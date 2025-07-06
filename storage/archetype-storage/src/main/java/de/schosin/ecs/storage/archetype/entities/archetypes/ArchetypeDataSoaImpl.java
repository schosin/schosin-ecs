package de.schosin.ecs.storage.archetype.entities.archetypes;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
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
import de.schosin.ecs.storage.api.entities.Archetype;
import de.schosin.ecs.storage.api.entities.ArchetypeAccessor;
import de.schosin.ecs.storage.api.entities.ComponentMask;
import de.schosin.ecs.storage.api.entities.EntityData;
import de.schosin.ecs.storage.archetype.ArchetypeStorageConfig;
import de.schosin.ecs.storage.archetype.components.ComponentIndex;
import de.schosin.ecs.storage.archetype.entities.ComponentMaskImpl;
import de.schosin.ecs.storage.archetype.entities.EntityIndex;
import de.schosin.ecs.storage.archetype.entities.EntityRelationIndex;
import de.schosin.ecs.storage.common.PendingChanges;
import de.schosin.ecs.storage.common.results.ComponentRelationResultImpl;
import de.schosin.ecs.storage.common.results.EntityRelationResultImpl;
import de.schosin.ecs.storage.common.results.StorageRelationResult;
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

    private final ComponentIndex componentIndex;
    private final EntityRelationIndex relationIndex;
    private final EntityIndex entityIndex;

    private final ComponentMaskImpl componentMask;

    private final Bag<RegularComponentType<?, ?>> componentTypes;
    private final IntBag componentTypeIds;

    private final ComponentAdder<?>[] adders;
    private final Bag<RegularEntityRelationType<?, ?>> entityRelationTypes;

    // data.get(index)[componentId] // index tracked by EntityIndex
    private final Bag<Object>[] data;
    private final IntBag entities;
    private final int size;

    private final Bag<PendingChanges> pendingChanges;
    private final EntityDataImpl entityData;

    private final Map<ImmutableIntBag, EntityDataImpl> entityDataMap = new HashMap<>();
    private final Pool<IntBag> intBagPool = Pool.unbounded(IntBag.class, () -> new IntBag(16), IntBag::clear);

    private int alive;

    @SuppressWarnings("unchecked")
    public ArchetypeDataSoaImpl(ComponentIndex componentIndex, EntityRelationIndex relationIndex, EntityIndex entityIndex, ComponentMaskImpl componentMask, ArchetypeStorageConfig config,
            StorageWorld world) {

        this.creationBatchSize = config.creationBatchSize();

        this.componentIndex = componentIndex;
        this.relationIndex = relationIndex;
        this.entityIndex = entityIndex;

        this.componentMask = componentMask;
        this.componentTypes = new Bag<>(componentMask.getComponentTypes());
        this.size = componentTypes.getSize();

        this.adders = new ComponentAdder<?>[size];
        this.entityRelationTypes = new Bag<>(RegularEntityRelationType.class, size);

        this.componentTypeIds = componentIndex.createIntBag();

        for (int i = 0; i < size; i++) {
            var componentType = componentTypes.get(i);

            var componentId = componentIndex.getId(componentType);
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

        this.entities = new IntBag(64);

        this.pendingChanges = new Bag<>(PendingChanges.class, 1024);
        this.entityData = getEntityData(Arrays.copyOf(componentTypes.getData(), componentTypes.getSize()));

        this.data = new Bag[size];
        for (int i = 0; i < size; i++) {
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
    public boolean containsEntity(int entityId) {
        return entityIndex.getComponentMask(entityId) == componentMask;
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
    public int getComponentIndex(RegularComponentType<?, ?> componentType) {
        for (int i = 0; i < size; i++) {
            if (componentTypes.get(i).equals(componentType)) {
                return i;
            }
        }

        return -1;
    }

    @Override
    public EntityData getEntityData() {
        return entityData;
    }

    @Override
    public EntityDataImpl getEntityData(RegularComponentType<?, ?>... componentTypes) {
        // Lookup key must be ordered
        var componentIds = intBagPool.getInstance();
        for (var type : componentTypes) {
            componentIds.add(componentIndex.getId(type));
        }

        // Retrieve cached value
        var result = entityDataMap.get(componentIds);
        if (result != null) {
            intBagPool.free(componentIds);
            return result;
        }

        synchronized (entityDataMap) {
            // Retrieve cached value
            result = entityDataMap.get(componentIds);
            if (result != null) {
                intBagPool.free(componentIds);
                return result;
            }

            // Create new instance
            result = new EntityDataImpl(componentTypes);

            var key = ImmutableIntBag.copyOf(componentIds);
            entityDataMap.put(key, result);

            intBagPool.free(componentIds);
            return result;
        }
    }

    @Override
    public ComponentMask getComponentMask() {
        return componentMask;
    }

    @Override
    public boolean contains(int index, RegularComponentType<?, ?> type) {
        if (componentTypes.contains(type)) {
            return true;
        }

        var changes = getPendingChanges(index);
        return changes != null && changes.getAddedTypes().contains(type);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R> R getComponent(int index, RegularComponentType<?, R> componentType, int componentId) {
        if (index >= alive) {
            return null;
        }

        var componentIndex = this.componentTypeIds.get(componentId);
        if (componentIndex == -1) {
            return retrievePendingComponent(index, componentType);
        }

        return (R) data[componentIndex].get(index);
    }

    @Override
    public ArchetypeAccessor getAccessor(int entityId) {
        return entityData.getAccessor(entityId);
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
    public int addEntity(int entityId, ImmutableBag<RegularComponentType<?, ?>> componentTypes, Bag<Object> components,
            ImmutableBag<? extends RegularComponentType<?, ?>> componentTypes2, ImmutableBag<Object> components2) {

        // Track entity index
        var index = alive++;
        this.entities.add(entityId);

        var componentIds = intBagPool.getInstance();
        for (int i = 0, s = componentTypes.getSize(); i < s; i++) {
            componentIds.set(i, componentIndex.getId(componentTypes.get(i)));
        }

        var componentIds2 = intBagPool.getInstance();
        for (int i = 0, s = componentTypes2.getSize(); i < s; i++) {
            componentIds2.set(i, componentIndex.getId(componentTypes2.get(i)));
        }

        // Fill data array (components 1)
        for (int i = 0, s = components.getSize(); i < s; i++) {
            // Add component to data
            var componentId = componentIds.get(i);

            var componentIndex = this.componentTypeIds.get(componentId);
            if (componentIndex == -1) {
                entityIndex.freeComponent(components.get(i));
                continue; // removed comonent
            }

            adders[componentIndex].add(entityId, index, components.get(i));
        }

        // Fill data array (components 2)
        for (int i = 0, s = components2.getSize(); i < s; i++) {
            // Add component to data
            var componentId = componentIds2.get(i);

            var componentIndex = this.componentTypeIds.get(componentId);
            if (componentIndex == -1) {
                entityIndex.freeComponent(components2.get(i));
                continue; // removed comonent
            }

            adders[componentIndex].add(entityId, index, components2.get(i));
        }

        intBagPool.free(componentIds);
        intBagPool.free(componentIds2);

        return index;

    }

    @Override
    public void addComponents(int entityId, int index, ImmutableBag<? extends RegularComponentType<?, ?>> componentTypes, Object[] components) {
        var changes = pendingChanges.getSafe(index);
        if (changes == null) {
            changes = new PendingChanges(componentMask);
            pendingChanges.set(index, changes);
        }

        for (int i = 0, s = componentTypes.getSize(); i < s; i++) {
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
            changes = new PendingChanges(componentMask);
            pendingChanges.set(index, changes);
        }

        for (int i = 0, s = componentTypes.getSize(); i < s; i++) {
            var componentType = componentTypes.get(i);
            changes.remove(componentType);
        }
    }

    @Override
    public int removeEntity(int entityId, int index, Bag<Object> fill) {
        if (entities.get(index) != entityId) {
            return -1;
        }

        synchronized (this.data) {
            if (entities.get(index) != entityId) {
                return -1;
            }

            // Process components
            for (int i = 0; i < size; i++) {
                var component = data[i].get(index);

                if (fill != null) {
                    // Put component into fill bag, required from caller
                    fill.add(component);
                } else {
                    entityIndex.freeComponent(component);
                }
            }

            // Remove row (decrement alive, move last row to removed index if needed)
            var lastIndex = --alive;
            if (index < lastIndex) {
                // Move components of last row to removed entity's row
                for (int i = 0; i < size; i++) {
                    var components = data[i];
                    components.set(index, components.get(lastIndex));
                    components.set(lastIndex, null);
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
                data[i].set(lastIndex, null);
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
        return componentMask.getId();
    }

    @Override
    public String toString() {
        return new StringBuilder()
                .append("ArchetypeData(")
                .append("count = ").append(this.alive).append(", ")
                .append("componentMask = ").append(this.componentMask).append(")")
                .toString();
    }

    private final class EntityDataImpl implements EntityData {

        private final RegularComponentType<?, ?>[] componentTypes;
        private final IntBag componentIds;
        private final IntBag componentLookup;

        private final int[] mapping;
        private final int size;

        private final Pool<AccessorImpl> accessors = Pool.unbounded(AccessorImpl.class, AccessorImpl::new);

        public EntityDataImpl(RegularComponentType<?, ?>[] componentTypes) {
            this.size = componentTypes.length;

            this.componentTypes = componentTypes;
            this.componentIds = new IntBag(componentTypes.length);
            this.mapping = new int[componentTypes.length];

            var largestComponentId = 0;
            for (int i = 0, s = size; i < s; i++) {
                var type = componentTypes[i];
                var componentId = componentIndex.getId(type);
                var id = componentTypeIds.get(componentId);

                componentIds.add(componentId);
                mapping[i] = id;
                if (componentId > largestComponentId) {
                    largestComponentId = componentId;
                }
            }

            this.componentLookup = new IntBag(largestComponentId);
            for (int i = 0; i < largestComponentId; i++) {
                this.componentLookup.set(i, -1);
            }

            for (int i = 0; i < size; i++) {
                this.componentLookup.set(componentIds.get(i), i);
            }
        }

        @Override
        public int getSize() {
            return entities.getSize();
        }

        @Override
        public int getId(int index) {
            return entities.get(index);
        }

        @Override
        @SuppressWarnings("unchecked")
        public <R> R getComponent(int index) {
            if (size == 0) {
                return null;
            }

            var id = mapping[0];
            if (id > -1) {
                return (R) data[id].get(index);
            }

            return (R) retrievePendingComponent(index, componentTypes[0]);
        }

        @Override
        public IterableAccessor getAccessor() {
            var accessor = accessors.getInstance();
            accessor.index = -1;

            return accessor;
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
        public String toString() {
            return new StringBuilder()
                    .append("EntityDataImpl(archetype = ").append(ArchetypeDataSoaImpl.this).append(")")
                    .toString();
        }

        private class AccessorImpl implements ArchetypeAccessor, IterableAccessor, Pooled {

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
                if (componentIndex > -1) {
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

                return retrievePendingComponent(index, componentId);
            }

            @Override
            @SuppressWarnings("unchecked")
            public <R> R getComponent(RegularComponentType<?, R> componentType, int componentId) {
                var componentIndex = componentTypeIds.get(componentId);
                if (componentIndex > -1) {
                    return (R) data[componentIndex].get(index);
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
                return (R) data[componentIndex].get(index);
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

    }

    private sealed abstract class ComponentAdder<T extends RegularComponentType<?, ?>> {

        protected final T componentType;
        protected final int componentIndex;

        protected ComponentAdder(T componentType, int componentIndex) {
            this.componentType = componentType;
            this.componentIndex = componentIndex;
        }

        public final void add(int entityId, int index, Object component) {
            if (!componentType.isInstance(component)) {
                throw new StorageEngineException("Expected component type '%s' at index %d, but was '%s'".formatted(componentType, componentIndex, component));
            }

            store(entityId, index, component);
        }

        protected abstract void store(int entityId, int index, Object component);
    }

    private final class ClassTypeAdder extends ComponentAdder<RegularComponentType<?, ?>> {

        protected ClassTypeAdder(RegularComponentType<?, ?> componentType, int componentIndex) {
            super(componentType, componentIndex);
        }

        @Override
        protected void store(int entityId, int index, Object component) {
            data[componentIndex].set(index, component);
        }

    }

    private final class RelationAdder extends ComponentAdder<RelationComponentType<?, ?, ?>> {

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
    private final class RelationsAdder extends ComponentAdder<RelationComponentType<?, ?, ?>> {

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
