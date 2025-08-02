package de.schosin.ecs.storage.archetype.entities.archetypes;

import java.util.function.IntSupplier;
import java.util.function.ObjIntConsumer;

import de.schosin.ecs.api.Pooled;
import de.schosin.ecs.api.components.Relation;
import de.schosin.ecs.api.components.Relation.ComponentRelation;
import de.schosin.ecs.api.components.Relation.EntityRelation;
import de.schosin.ecs.api.components.Relations;
import de.schosin.ecs.api.components.types.ClassType;
import de.schosin.ecs.api.components.types.ComponentType.RegularComponentType;
import de.schosin.ecs.api.components.types.RelationComponentType;
import de.schosin.ecs.api.components.types.RelationComponentType.ComponentRelationType;
import de.schosin.ecs.api.components.types.RelationComponentType.EntityRelationType;
import de.schosin.ecs.api.components.types.RelationComponentType.ExclusiveComponentRelationType;
import de.schosin.ecs.api.components.types.RelationComponentType.ExclusiveEntityRelationType;
import de.schosin.ecs.api.components.types.RelationComponentType.RegularEntityRelationType;
import de.schosin.ecs.api.data.IterableAccessor;
import de.schosin.ecs.storage.api.StorageEngineException;
import de.schosin.ecs.storage.api.StorageWorld;
import de.schosin.ecs.storage.api.components.Component;
import de.schosin.ecs.storage.api.entities.Archetype;
import de.schosin.ecs.storage.api.entities.ArchetypeAccessor;
import de.schosin.ecs.storage.archetype.ArchetypeStorageConfig;
import de.schosin.ecs.storage.archetype.components.ComponentIndex;
import de.schosin.ecs.storage.archetype.entities.EntityIndex;
import de.schosin.ecs.storage.archetype.entities.EntityRelationIndex;
import de.schosin.ecs.storage.archetype.utils.results.ComponentRelationResultImpl;
import de.schosin.ecs.storage.archetype.utils.results.EntityRelationResultImpl;
import de.schosin.ecs.storage.archetype.utils.results.StorageRelationResult;
import de.schosin.ecs.utils.collections.Bag;
import de.schosin.ecs.utils.collections.ImmutableBag;
import de.schosin.ecs.utils.collections.ImmutableIntBag;
import de.schosin.ecs.utils.collections.IntBag;
import de.schosin.ecs.utils.collections.Pool;

/**
 * Auto-growing "Struct of arrays" implementation of {@link ArchetypeData}.
 */
public final class ArchetypeDataSoaImpl implements ArchetypeData {

    private final int creationBatchSize;

    private final int id;
    private final ArchetypeGraphNode node;

    private final ComponentIndex componentIndex;
    private final EntityRelationIndex relationIndex;
    private final EntityIndex entityIndex;

    private final Bag<RegularComponentType<?, ?>> componentTypes;
    private final IntBag componentTypeIds;

    private final ImmutableBag<Component<?, ?>> components;
    private final ImmutableBag<RegularComponentType<?, ?>> immutableComponentTypes;

    private final ComponentAdder[] adders;
    private final Bag<RegularEntityRelationType<?, ?>> entityRelationTypes;

    // data[componentId].get(index) // index tracked by EntityIndex
    private final Bag<Object>[] data;
    private final IntBag entities;
    private final int size;

    private final Object[] zeroSizedTypes;

    private final Bag<PendingChanges> pendingChanges;

    private final Bag<ArchetypeMover> movers = new Bag<>(ArchetypeMover.class, 4);
    private final Pool<AccessorImpl> accessors = Pool.unbounded(AccessorImpl.class, AccessorImpl::new);

    private int alive;

    @SuppressWarnings("unchecked")
    public ArchetypeDataSoaImpl(int id, ArchetypeGraphNode node, ComponentIndex componentIndex, EntityRelationIndex relationIndex, EntityIndex entityIndex, ArchetypeStorageConfig config,
            StorageWorld world) {

        this.creationBatchSize = config.creationBatchSize();

        this.id = id;
        this.node = node;

        this.componentIndex = componentIndex;
        this.relationIndex = relationIndex;
        this.entityIndex = entityIndex;

        this.components = node.getComponents();
        this.size = components.getSize();

        this.componentTypes = new Bag<>(RegularComponentType.class, this.size);

        this.adders = new ComponentAdder[size];
        this.entityRelationTypes = new Bag<>(RegularEntityRelationType.class, size);

        this.componentTypeIds = componentIndex.createIntBag();

        this.zeroSizedTypes = new Object[size];

        for (int i = 0; i < size; i++) {
            var component = components.get(i);
            var componentId = component.id();

            var componentType = component.type();
            this.componentTypes.add(componentType);

            // Handle zero-sized components (marker components)
            if (componentType instanceof ClassType<?> classType && Enum.class.isAssignableFrom(classType.clazz())) {
                var enumConstants = classType.clazz().getEnumConstants();
                if (enumConstants != null && enumConstants.length == 1) {
                    this.zeroSizedTypes[i] = enumConstants[0];

                    this.componentTypeIds.set(componentId, -2 - i);
                    this.adders[i] = NoOpAdder.INSTANCE;

                    continue;
                }
            }

            this.componentTypeIds.set(componentId, i);

            if (componentType instanceof RegularEntityRelationType<?, ?> relationType) {
                this.entityRelationTypes.set(i, relationType);
            }

            this.adders[i] = switch (componentType) {
                case ClassType<?> type -> new ClassTypeAdder(type, i);
                case ComponentRelationType<?, ?> type -> new RelationsAdder(type, i);
                case ExclusiveComponentRelationType<?, ?> type -> new RelationAdder(type, i);
                case EntityRelationType<?> type -> new RelationsAdder(type, i);
                case ExclusiveEntityRelationType<?> type -> new RelationAdder(type, i);
            };
        }

        this.immutableComponentTypes = ImmutableBag.create(this.componentTypes);

        this.entities = new IntBag(64);

        this.pendingChanges = new Bag<>(PendingChanges.class, 1024);

        this.data = new Bag[size];
        for (int i = 0; i < size; i++) {
            // skip zero-sized components
            if (this.adders[i] == NoOpAdder.INSTANCE) {
                continue;
            }

            var clazz = switch (componentTypes.get(i)) {
                case ClassType<?> type -> type.clazz();
                case ComponentRelationType<?, ?> type -> ComponentRelationResultImpl.class;
                case ExclusiveComponentRelationType<?, ?> type -> ComponentRelation.class;
                case EntityRelationType<?> type -> EntityRelationResultImpl.class;
                case ExclusiveEntityRelationType<?> type -> EntityRelation.class;
            };

            data[i] = world.createEntityBag((Class<Object>) clazz);
        }
    }

    @Override
    public int getCount() {
        return alive;
    }

    @Override
    public ImmutableIntBag getEntities() {
        return entities;
    }

    @Override
    public int getComponentIndex(int componentId) {
        return componentTypeIds.get(componentId);
    }

    @Override
    public ImmutableBag<Component<?, ?>> getComponents() {
        return components;
    }

    @Override
    public ImmutableBag<RegularComponentType<?, ?>> getComponentTypes() {
        return immutableComponentTypes;
    }

    @Override
    public ArchetypeAccessor getAccessor(int entityId) {
        var index = entityIndex.getEntityIndex(ArchetypeDataSoaImpl.this, entityId);
        if (index == -1) {
            return null;
        }

        var accessor = accessors.getInstance();
        accessor.index = index;

        return accessor;
    }

    @Override
    public IterableAccessor getAccessor() {
        var accessor = accessors.getInstance();
        accessor.index = -1;

        return accessor;
    }

    @Override
    public void createEntity(int entityId, Object[] components) {
        // Validate entity not already in storage
        var existing = entityIndex.getArchetypeDataForEntity(entityId);
        if (existing != null) {
            throw new StorageEngineException("Cannot create entity %d, already present in storage: %s".formatted(entityId, existing));
        }

        // Validate matching length
        if (size != components.length) {
            throw new StorageEngineException("Expected %d components, but got %d".formatted(size, components.length));
        }

        // Add components
        int index;
        synchronized (this.entities) {
            index = alive++;

            for (int i = 0; i < size; i++) {
                adders[i].add(entityId, index, components[i]);
            }

            // Track entity
            this.entities.add(entityId);
        }

        // Add to EntityIndex
        entityIndex.add(this, entityId, index);
    }

    @Override
    public void createEntities(int count, IntSupplier entityIdSupplier, ObjIntConsumer<Object[]> componentsConsumer) {
        var batchSize = creationBatchSize < count ? creationBatchSize : count;

        var entityIds = new int[batchSize];
        var components = new Object[batchSize][size];

        var idx = 0;
        while (count > 0) {
            var batch = batchSize < count ? batchSize : count;
            count -= batch;

            // Fill batch
            for (int i = 0; i < batch; i++) {
                entityIds[i] = entityIdSupplier.getAsInt();
                componentsConsumer.accept(components[i], idx++);
            }

            // Process batch
            createEntities(entityIds, components, batch);
        }
    }

    private void createEntities(int[] entityIds, Object[][] components, int count) {
        synchronized (entities) {
            for (int i = 0, s = count; i < s; i++) {
                var index = alive++;

                var entityId = entityIds[i];

                // Validate entity not already in storage
                var existing = entityIndex.getArchetypeDataForEntity(entityId);
                if (existing != null) {
                    throw new StorageEngineException("Cannot create entity %d, already present in storage: %s".formatted(entityId, existing));
                }

                var entityComponents = components[i];

                // Add components
                for (int c = 0; c < size; c++) {
                    adders[c].add(entityId, index, entityComponents[c]);
                }

                // Track entity
                this.entities.add(entityId);

                // Add to EntityIndex
                entityIndex.add(this, entityId, index);

                // Clear first component to cause error if user does not fill array
                entityComponents[0] = null;
            }
        }
    }

    @Override
    public void addComponents(int entityId, int index, ImmutableBag<? extends RegularComponentType<?, ?>> componentTypes, Object[] components) {
        var changes = pendingChanges.getSafe(index);
        if (changes == null) {
            changes = new PendingChanges(componentIndex, node);
            pendingChanges.set(index, changes);
        }

        for (int i = 0, s = components.length; i < s; i++) {
            var componentType = componentTypes.get(i);
            var component = components[i];

            // Add to pending changes
            if (!changes.add(componentType, component)) {
                // No archetype change, apply directly
                var componentId = componentIndex.getId(componentType);
                var componentIndex = this.componentTypeIds.get(componentId);

                adders[componentIndex].add(entityId, index, component);
            }
        }
    }

    @Override
    public void removeComponents(int entityId, int index, ImmutableBag<? extends RegularComponentType<?, ?>> componentTypes) {
        var changes = pendingChanges.getSafe(index);
        if (changes == null) {
            changes = new PendingChanges(componentIndex, node);
            pendingChanges.set(index, changes);
        }

        for (int i = 0, s = componentTypes.getSize(); i < s; i++) {
            var componentType = componentTypes.get(i);
            changes.remove(componentType);
        }
    }

    @Override
    public int moveEntity(int entityId, int index) {
        // Get pending changes (must not be null if this is called)
        var changes = pendingChanges.get(index);

        // Handle archetype implementations
        return switch (changes.getPendingArchetypeNode().getArchetype()) {
            case ArchetypeDataSoaImpl archetype -> moveEntity(entityId, index, changes, archetype);
        };
    }

    private int moveEntity(int entityId, int index, PendingChanges changes, ArchetypeDataSoaImpl targetArchetype) {
        // Resolve archetype mover
        var mover = this.movers.getSafe(targetArchetype.id);
        if (mover == null) {
            // Initialize mover
            mover = createMover(targetArchetype);
            this.movers.set(targetArchetype.id, mover);
        }

        // Move entity data
        return mover.moveComponent(entityId, index, changes);
    }

    private ArchetypeMover createMover(ArchetypeDataSoaImpl targetArchetype) {
        // Generate mapping for component indices
        var mapping = new int[size];
        for (int i = 0; i < size; i++) {
            var componentId = this.components.get(i).id();
            var componentIndex = targetArchetype.getComponentIndex(componentId);

            mapping[i] = componentIndex > -1 ? componentIndex : -1; // convert zero-sized (-2 and lower) to -1
        }

        return new ArchetypeMover(entityIndex, this, targetArchetype, mapping);
    }

    @Override
    public int removeEntity(int entityId, int index) {
        if (entities.get(index) != entityId) {
            return -1;
        }

        synchronized (this.data) {
            if (entities.get(index) != entityId) {
                return -1;
            }

            // Process components
            for (int i = 0; i < size; i++) {
                var componentData = this.data[i];

                // Free component
                if (componentData != null) {
                    entityIndex.freeComponent(componentData.get(index));
                }
            }

            // Remove row (decrement alive, move last row to removed index if needed)
            var lastIndex = --alive;
            if (index < lastIndex) {
                // Move components of last row to removed entity's row
                for (int i = 0; i < size; i++) {
                    var components = data[i];
                    if (components != null) {
                        components.set(index, components.get(lastIndex));
                        components.set(lastIndex, null);
                    }
                }

                // Swap entity lookup
                var swappedEntityId = this.entities.get(lastIndex);

                this.entities.set(index, swappedEntityId);
                this.entities.removeLast();

                var pending = this.pendingChanges.getSafe(lastIndex);
                this.pendingChanges.set(lastIndex, this.pendingChanges.getSafe(index));
                this.pendingChanges.set(index, pending);

                // Return id of swapped entity
                return swappedEntityId;
            }

            // Last element removed, no swap required
            for (int i = 0; i < size; i++) {
                var componentData = this.data[i];
                if (componentData != null) {
                    componentData.set(lastIndex, null);
                }
            }

            this.entities.removeLast();

            return -1;
        }
    }

    @Override
    public PendingChanges getPendingChanges(int index) {
        var changes = pendingChanges.getSafe(index);
        return changes == null || changes.isEmpty() ? null : changes;
    }

    private boolean hasPendingComponent(int index, int componentId) {
        var changes = pendingChanges.getSafe(index);
        if (changes == null || changes.isNoAdded()) {
            return false;
        }

        var componentType = componentIndex.getType(componentId);
        if (componentType == null) {
            return false;
        }

        return changes.containsComponent(componentType);

    }

    private <R> R retrievePendingComponent(int index, int componentId) {
        var changes = pendingChanges.getSafe(index);
        if (changes == null || changes.isNoAdded()) {
            return null;
        }

        var componentType = componentIndex.<R>getType(componentId);
        if (componentType == null) {
            return null;
        }

        return changes.getComponent(componentType);
    }

    private <R> R retrievePendingComponent(int index, RegularComponentType<?, R> componentType) {
        var changes = pendingChanges.getSafe(index);
        if (changes == null || changes.isNoAdded()) {
            return null;
        }

        return changes.getComponent(componentType);
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public String toString() {
        return new StringBuilder()
                .append("ArchetypeData(")
                .append("id = ").append(this.id).append(", ")
                .append("count = ").append(this.alive).append(", ")
                .append("componentTypes = ").append(this.componentTypes).append(")")
                .toString();
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this;
    }

    /**
     * Helper class for moving entities between {@link ArchetypeDataSoaImpl ArchetypeDataSoaImpl archetypes}. 
     */
    private final record ArchetypeMover(EntityIndex entityIndex, ArchetypeDataSoaImpl source, ArchetypeDataSoaImpl target, int[] mapping) {

        /**
         * Moves the entity at "sourceIndex" to the end of the target archetype.
         * 
         * Moves the components of the source archetype according to the mapping (index in target archetype)
         * to the target archetype and overwrites the source data. 
         * Any components not moved to the new archetype will be freed.  
         * Afterwards it adds the pending (added) components.
         * 
         * When "sourceIndex" does not point to the last entity of the archetype, the component data of
         * the last entity will be moved to "sourceIndex" after its data has been moved. This ensures a
         * dense packing of entities. In that case the return value of this function will be the id
         * of the source entity that had its component data moved to "sourceIndex". 
         * 
         * @param entityId id of entity
         * @param sourceIndex index of entity in source archetype
         * @param changes pending changes
         * @return -1 or id of source entity moved to sourceIndex
         */
        private int moveComponent(int entityId, int sourceIndex, PendingChanges changes) {
            var sourceData = source.data;
            var targetData = target.data;

            // Update alive (entity count) fields of source and target
            var previouslyLastIndex = --source.alive;
            var targetIndex = target.alive++;

            // Resolve index of source entity moved to freed up slot
            var movedIndex = sourceIndex == source.alive ? -1 : source.alive;

            // Move components of this archetype over
            for (int i = 0, s = source.size; i < s; i++) {
                var sourceComponents = sourceData[i];

                // Skip zero-sized components
                if (sourceComponents == null) {
                    continue;
                }

                var targetComponentIndex = mapping[i];

                // Free removed components (not in target archetype) 
                if (targetComponentIndex == -1) {
                    entityIndex.freeComponent(sourceComponents.get(sourceIndex));
                    continue;
                }

                // Move component to new archetype 
                targetData[targetComponentIndex].set(targetIndex, sourceComponents.get(sourceIndex));

                if (movedIndex != -1) {
                    // Move component data from moved entity to source index
                    sourceComponents.set(sourceIndex, sourceComponents.get(movedIndex));
                }

                // Clear previously last index data
                sourceComponents.set(previouslyLastIndex, null);
            }

            // Add pending components
            var added = changes.getAdded();
            var addedIds = changes.getAddedIds();

            for (int i = 0, s = added.getSize(); i < s; i++) {
                // Retrieve target component index
                var targetComponentIndex = target.getComponentIndex(addedIds.get(i));

                // Only skip zero-sized components (-1 at this point is an error somewhere -> AIOOBE is correct) 
                if (targetComponentIndex > -2) {
                    // Set component data (no index check, must be valid at this point)
                    targetData[targetComponentIndex].set(targetIndex, added.get(i));
                }
            }

            // Add entity to target
            target.entities.add(entityId);

            // Reset pending changes
            changes.reset();

            // Return -1 if no source entity was moved
            if (movedIndex == -1) {
                source.entities.removeLast();
                return -1;
            }

            // Update source tracking tracking
            var movedEntityId = source.entities.removeLast();
            source.entities.set(sourceIndex, movedEntityId);

            var movedPending = source.pendingChanges.get(movedIndex);
            source.pendingChanges.set(movedIndex, source.pendingChanges.get(sourceIndex));
            source.pendingChanges.set(sourceIndex, movedPending);

            // Return id of moved source entity  
            return movedEntityId;
        }

    }

    private final class AccessorImpl implements ArchetypeAccessor, IterableAccessor, Pooled {

        private int index = -2;

        @Override
        public Archetype getArchetype() {
            return ArchetypeDataSoaImpl.this;
        }

        @Override
        public boolean hasNext() {
            return index < entities.getSize() - 1;
        }

        @Override
        public int next() {
            return entities.get(++index);
        }

        @Override
        public int entityId() {
            return entities.get(index);
        }

        @Override
        public boolean hasComponent(int componentId) {
            var componentIndex = componentTypeIds.get(componentId);
            if (componentIndex != -1) {
                return true;
            }

            return hasPendingComponent(index, componentId);
        }

        @Override
        @SuppressWarnings("unchecked")
        public <R> R getComponent(int componentId) {
            var componentIndex = componentTypeIds.get(componentId);
            if (componentIndex > -1) {
                return (R) data[componentIndex].get(index);
            }
            if (componentIndex < -1) {
                return (R) zeroSizedTypes[-componentIndex - 2];
            }

            return retrievePendingComponent(index, componentId);
        }

        @Override
        @SuppressWarnings("unchecked")
        public <R> R getComponent(RegularComponentType<?, R> componentType, int componentId) {
            var componentIndex = componentTypeIds.get(componentId);
            if (componentIndex > -1) {
                return (R) data[componentIndex].get(index);
            }
            if (componentIndex < -1) {
                return (R) zeroSizedTypes[-componentIndex - 2];
            }

            return retrievePendingComponent(index, componentType);
        }

        @Override
        public <R> R getPendingComponent(int componentId) {
            return retrievePendingComponent(index, componentId);
        }

        @Override
        @SuppressWarnings("unchecked")
        public <R> R getComponentByIndex(int componentIndex) {
            return (R) (componentIndex > -1
                    ? data[componentIndex].get(index)
                    : zeroSizedTypes[-componentIndex - 2]);
        }

        @Override
        public boolean isValid() {
            return this.index > -2;
        }

        @Override
        public void free() {
            accessors.free(this);
        }

        @Override
        public void reset() {
            this.index = -2;
        }

        @Override
        public String toString() {
            return new StringBuilder()
                    .append("AccessorImpl(index = ").append(index)
                    .append(", size = ").append(size)
                    .append(", archetype = ").append(ArchetypeDataSoaImpl.this)
                    .append(")").toString();
        }

    }

    private sealed interface ComponentAdder {

        void add(int entityId, int index, Object component);

    }

    private enum NoOpAdder implements ComponentAdder {

        INSTANCE;

        @Override
        public final void add(int entityId, int index, Object component) {
            // nothing to do
        }

    }

    private sealed abstract class AbstractComponentAdder<T extends RegularComponentType<?, ?>> implements ComponentAdder {

        protected final T componentType;
        protected final int componentIndex;

        protected AbstractComponentAdder(T componentType, int componentIndex) {
            this.componentType = componentType;
            this.componentIndex = componentIndex;
        }

        @Override
        public final void add(int entityId, int index, Object component) {
            if (!componentType.isInstance(component)) {
                throw new StorageEngineException("Expected component type '%s' at index %d, but was '%s'".formatted(componentType, componentIndex, component));
            }

            store(entityId, index, component);
        }

        protected abstract void store(int entityId, int index, Object component);
    }

    private final class ClassTypeAdder extends AbstractComponentAdder<RegularComponentType<?, ?>> {

        protected ClassTypeAdder(RegularComponentType<?, ?> componentType, int componentIndex) {
            super(componentType, componentIndex);
        }

        @Override
        protected void store(int entityId, int index, Object component) {
            data[componentIndex].set(index, component);
        }

    }

    private final class RelationAdder extends AbstractComponentAdder<RelationComponentType<?, ?, ?>> {

        private final boolean entityType;

        protected RelationAdder(RelationComponentType<?, ?, ?> componentType, int componentIndex) {
            super(componentType, componentIndex);

            this.entityType = componentType instanceof ExclusiveEntityRelationType<?>;
        }

        @Override
        protected void store(int entityId, int index, Object component) {
            data[componentIndex].set(index, component);

            if (entityType) {
                relationIndex.add(entityId, (EntityRelation<?>) component);
            }
        }

    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private final class RelationsAdder extends AbstractComponentAdder<RelationComponentType<?, ?, ?>> {

        private final boolean entityType;

        protected RelationsAdder(RelationComponentType<?, ?, ?> componentType, int componentIndex) {
            super(componentType, componentIndex);

            this.entityType = componentType instanceof EntityRelationType<?>;
        }

        @Override
        protected void store(int entityId, int index, Object component) {
            // Prepare storage data
            var relations = (StorageRelationResult) data[componentIndex].getSafe(index);
            if (relations == null) {
                relations = StorageRelationResult.getInstance(componentType);
                data[componentIndex].set(index, relations);
            }

            switch (component) {
                case StorageRelationResult<?> other -> store(entityId, relations, other);
                case Relations<?> other -> store(entityId, relations, other);
                case Relation<?> relation -> store(entityId, relations, relation);
                case null -> throw new IllegalArgumentException("Cannot add null component to relations");
                default -> throw new IllegalArgumentException("Cannot add component of type '%s' to relations: %s".formatted(component.getClass().getName(), component));
            }
        }

        private void store(int entityId, StorageRelationResult relations, StorageRelationResult<?> other) {
            // Move relations over
            while (!other.isEmpty()) {
                store(entityId, relations, other.removeLast());
            }
        }

        private void store(int entityId, StorageRelationResult relations, Relations<?> other) {
            // Add relations
            for (int i = 0, s = other.size(); i < s; i++) {
                store(entityId, relations, other.get(i));
            }

            // Free relations
            Relations.free(other);
        }

        private void store(int entityId, StorageRelationResult relations, Relation<?> relation) {
            relations.add(relation);

            if (entityType) {
                relationIndex.add(entityId, (EntityRelation<?>) relation);
            }
        }

    }

}
