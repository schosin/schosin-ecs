package de.schosin.ecs.storage.archetype.entities.archetypes;

import java.util.BitSet;
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
import de.schosin.ecs.storage.archetype.entities.Observers;
import de.schosin.ecs.storage.archetype.utils.results.ComponentRelationResultImpl;
import de.schosin.ecs.storage.archetype.utils.results.EntityRelationResultImpl;
import de.schosin.ecs.storage.archetype.utils.results.StorageRelationResult;
import de.schosin.ecs.utils.collections.Bag;
import de.schosin.ecs.utils.collections.BitVector;
import de.schosin.ecs.utils.collections.ImmutableBag;
import de.schosin.ecs.utils.collections.ImmutableIntBag;
import de.schosin.ecs.utils.collections.IntBag;
import de.schosin.ecs.utils.collections.Pool;

/**
 * Auto-growing "Struct of arrays" implementation of {@link ArchetypeData}.
 */
public final class ArchetypeDataSoaImpl implements ArchetypeData {

    private final int id;
    private final ArchetypeGraphNode node;

    private final StorageWorld world;
    private final ComponentIndex componentIndex;
    private final EntityRelationIndex relationIndex;
    private final EntityIndex entityIndex;
    private final Observers observers;

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

    private final Bag<PendingArchetypeTransition> archetypeTransitionLookup = new Bag<>(PendingArchetypeTransition.class, 4);

    private final ProcessBuffer buffer = new ProcessBuffer();

    private final Bag<ArchetypeMover> movers = new Bag<>(ArchetypeMover.class, 4);
    private final Pool<AccessorImpl> accessors = Pool.unbounded(AccessorImpl.class, AccessorImpl::new);

    private final Pool<EntityCreator> creatorPool;

    private int alive;
    private boolean dirty;

    @SuppressWarnings("unchecked")
    public ArchetypeDataSoaImpl(int id, ArchetypeGraphNode node, ComponentIndex componentIndex, EntityRelationIndex relationIndex, EntityIndex entityIndex, Observers observers,
            ArchetypeStorageConfig config, StorageWorld world) {

        this.id = id;
        this.node = node;

        this.world = world;
        this.componentIndex = componentIndex;
        this.relationIndex = relationIndex;
        this.entityIndex = entityIndex;
        this.observers = observers;

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

        this.creatorPool = Pool.unbounded(EntityCreator.class, () -> new EntityCreator(this));
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
    public ArchetypeAccessor getAccessor(int index) {
        var accessor = accessors.getInstance();
        accessor.index = index;

        return accessor;
    }

    @Override
    public void updateAccessor(int index, ArchetypeAccessor previousAccessor) {
        if (!(previousAccessor instanceof AccessorImpl accessor)) {
            throw new StorageEngineException("Cannot update index accessor of unexpected type '%s': %s".formatted(previousAccessor.getClass(), previousAccessor));
        }

        accessor.index = index;
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

            this.pendingChanges.ensureCapacity(alive);
        }

        // Add to EntityIndex
        entityIndex.add(this, entityId, index);

        // Invoke observers
        observers.triggerEntityCreated(this, entityId);

        // Process changes by observers
        entityIndex.processCreatedEntity(entityId);
    }

    @Override
    public void createEntities(int count, IntSupplier entityIdSupplier, ComponentsInitializer componentsConsumer) {
        // Initialize creator
        var creator = creatorPool.getInstance();
        creator.entityIdSupplier = entityIdSupplier;
        creator.i = alive - 1;
        creator.end = alive + count;

        // Update alive
        this.alive += count;
        this.pendingChanges.ensureCapacity(alive);

        // Create entities
        var idx = 0;
        while (creator.next()) {
            componentsConsumer.accept(creator, idx++);
            creator.validate();
        }

        // Invoke observers
        this.observers.triggerEntitiesCreated(this, creator.entityIds);

        // Process changes by observers
        this.entityIndex.processCreatedEntities(creator.entityIds);

        // Free pooled creator
        this.creatorPool.free(creator);
    }

    @Override
    public void addComponents(int entityId, int index, ImmutableBag<? extends RegularComponentType<?, ?>> componentTypes, Object[] components) {
        // Skip if entity marked for deletion
        if (buffer.deletedEntitiesLookup.get(entityId)) {
            return;
        }

        // Changes in removed callbacks on deleted entities not allowed
        if (buffer.deletedEntitiesLookupOverflow.get(entityId)) {
            throw new StorageEngineException("Cannot add component to entity %d: Entity marked for deletion".formatted(entityId));
        }

        var changes = pendingChanges.get(index);
        if (changes == null) {
            changes = new PendingChanges(componentIndex, node);
            pendingChanges.set(index, changes);
        }

        var previousNode = changes.getPendingArchetypeNode();

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

        // Track archetype changes
        handlePendingArchetypes(entityId, previousNode, changes.getPendingArchetypeNode());

        // Mark archetype as dirty
        markDirty();
    }

    @Override
    public void removeComponents(int entityId, int index, ImmutableBag<? extends RegularComponentType<?, ?>> componentTypes) {
        // Skip if entity marked for deletion
        if (buffer.deletedEntitiesLookup.get(entityId)) {
            return;
        }

        // Changes in removed callbacks on deleted entities not allowed
        if (buffer.deletedEntitiesLookupOverflow.get(entityId)) {
            throw new StorageEngineException("Cannot remove component from entity %d: Entity marked for deletion".formatted(entityId));
        }

        var changes = pendingChanges.get(index);
        if (changes == null) {
            changes = new PendingChanges(componentIndex, node);
            pendingChanges.set(index, changes);
        }

        var previousNode = changes.getPendingArchetypeNode();

        for (int i = 0, s = componentTypes.getSize(); i < s; i++) {
            var componentType = componentTypes.get(i);
            changes.remove(componentType);
        }

        // Track archetype changes
        handlePendingArchetypes(entityId, previousNode, changes.getPendingArchetypeNode());

        // Mark archetype as dirty
        markDirty();
    }

    private void handlePendingArchetypes(int entityId, ArchetypeGraphNode previousTargetNode, ArchetypeGraphNode currentTargetNode) {
        // Pending archetype node unchanged -> return early
        if (previousTargetNode == currentTargetNode) {
            return;
        }

        // Remove entity from previous target node
        if (previousTargetNode != null) {
            var transition = getPendingArchetypeTransition(previousTargetNode);
            if (transition.removeEntity(entityId) && transition.entities.isEmpty()) {
                synchronized (buffer.archetypeTransitions) {
                    // no need for double-checked locking here
                    buffer.archetypeTransitions.remove(transition);
                }
            }
        }

        // Add to pending changes if entities empty
        if (currentTargetNode != null) {
            var change = getPendingArchetypeTransition(currentTargetNode);

            if (change.entities.isEmpty()) {
                synchronized (buffer.archetypeTransitions) {
                    // double-checked locking to avoid duplicates
                    if (change.entities.isEmpty()) {
                        buffer.archetypeTransitions.add(change);
                    }
                }
            }

            // Add entity to current target node
            change.addEntity(entityId);
        }
    }

    private PendingArchetypeTransition getPendingArchetypeTransition(ArchetypeGraphNode target) {
        var change = this.archetypeTransitionLookup.getSafe(target.getId());
        if (change != null) {
            return change;
        }

        synchronized (this.archetypeTransitionLookup) {
            change = this.archetypeTransitionLookup.getSafe(target.getId());
            if (change != null) {
                return change;
            }

            change = new PendingArchetypeTransition(this.observers, this.entityIndex, this.node, target);
            this.archetypeTransitionLookup.set(target.getId(), change);

            return change;
        }
    }

    @Override
    public void moveEntity(int entityId, ArchetypeGraphNode newArchetypeNode, int index) {
        // Get pending changes (must not be null if this is called)
        var changes = pendingChanges.get(index);

        // Remove entity from pending archetype change
        var transition = getPendingArchetypeTransition(newArchetypeNode);
        if (transition.removeEntity(entityId) && transition.entities.isEmpty()) {
            synchronized (buffer.archetypeTransitions) {
                // no need for double-checked locking here
                buffer.archetypeTransitions.remove(transition);
            }
        }

        // Handle archetype implementations
        switch (newArchetypeNode.getArchetype()) {
            case ArchetypeDataSoaImpl archetype -> moveEntity(entityId, index, changes, archetype);
        }
    }

    private void moveEntity(int entityId, int index, PendingChanges changes, ArchetypeDataSoaImpl targetArchetype) {
        // Resolve archetype mover
        var mover = this.movers.getSafe(targetArchetype.id);
        if (mover == null) {
            // Initialize mover
            mover = createMover(targetArchetype);
            this.movers.set(targetArchetype.id, mover);
        }

        // Move entity data
        mover.moveComponent(entityId, index, changes);
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
    public PendingChanges getPendingChanges(int index) {
        return pendingChanges.get(index);
    }

    private boolean hasPendingComponent(int index, int componentId) {
        var changes = pendingChanges.get(index);
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
        var changes = pendingChanges.get(index);
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
        var changes = pendingChanges.get(index);
        if (changes == null || changes.isNoAdded()) {
            return null;
        }

        return changes.getComponent(componentType);
    }

    @Override
    public void markDeleted(int entityId, int index) {
        var deletedEntitiesLookup = buffer.deletedEntitiesLookup;
        if (deletedEntitiesLookup.setAndReturn(entityId)) {
            buffer.deletedEntities.add(entityId);

            // Mark archetype as dirty
            markDirty();

            var changes = getPendingChanges(index);
            if (changes == null || changes.isEmpty()) {
                return;
            }

            // Discard pending changes
            var targetNode = changes.getPendingArchetypeNode();
            changes.reset();

            // Remove from archetype transition
            var transition = this.archetypeTransitionLookup.get(targetNode.getId());
            if (transition.removeEntity(entityId) && transition.entities.isEmpty()) {
                synchronized (buffer.archetypeTransitions) {
                    // no need for double-checked locking here
                    buffer.archetypeTransitions.remove(transition);
                }
            }
        }
    }

    private void markDirty() {
        if (dirty) {
            return;
        }

        this.dirty = true;
        this.entityIndex.markDirty(this);
    }

    @Override
    public void process() {
        // Clear dirty flag
        this.dirty = false;

        // Swap double buffer to avoid losing updates
        buffer.swap();

        // Process deleted and updated entities
        processDeletedEntities(buffer.deletedEntitiesOverflow);
        processUpdatedArchetypes(buffer.archetypeTransitionsOverflow);

        // Clear deleted lookup
        buffer.deletedEntitiesLookupOverflow.clear();
    }

    private void processDeletedEntities(IntBag removedEntities) {
        if (removedEntities.isEmpty()) {
            return;
        }

        // Call observers
        this.observers.triggerEntitiesDeleted(this, removedEntities);

        // Iterate indices in reverse order to reduce housekeeping (deleted entity moved in bag would change index)
        var data = removedEntities.getData();
        for (int i = 0, s = removedEntities.getSize(); i < s; i++) {
            var entityId = data[i];
            var index = this.entityIndex.removeEntityIndex(entityId);

            // Decrement alive count, only remove if last entity removed
            if (index == --alive) {
                // Remove entity
                this.entities.removeLast();

                // Remove component data
                for (int c = 0, cs = size; c < cs; c++) {
                    var components = this.data[c];

                    // Skip for zero-sized components
                    if (components == null) {
                        continue;
                    }

                    // Free component of removed entity
                    var component = components.get(index);
                    this.entityIndex.freeComponent(component);

                    // Remove reference
                    components.set(alive, null);
                }

                // Reset pending changes
                var changes = this.pendingChanges.get(index);
                if (changes != null) {
                    changes.reset();
                }

                continue;
            }

            // Remove entity by moving last entity to removed slot
            var movedEntityId = this.entities.removeLast();
            this.entities.set(index, movedEntityId);

            // Update swapped entity reference 
            this.entityIndex.setEntityIndex(movedEntityId, index);

            // Move last component data to removed index, clear reference to last slot 
            for (int c = 0, cs = size; c < cs; c++) {
                var components = this.data[c];

                // Skip for zero-sized components
                if (components == null) {
                    continue;
                }

                // Free component of removed entity
                var component = components.get(index);
                this.entityIndex.freeComponent(component);

                // Move last data to removed slot, clear reference at last index
                components.set(index, components.get(alive));
                components.set(alive, null);
            }

            // Swap pending changes
            var changes = pendingChanges.get(index);
            this.pendingChanges.set(index, pendingChanges.get(alive));

            if (changes != null) {
                changes.reset();
                this.pendingChanges.set(alive, changes);
            }
        }

        // Free entity ids
        world.freeEntityIds(removedEntities);

        // Clear deleted indices
        removedEntities.clear();
    }

    private void processUpdatedArchetypes(Bag<PendingArchetypeTransition> archetypeTransitions) {
        var s = archetypeTransitions.getSize();
        if (s == 0) {
            return;
        }

        // Process updated entities
        var data = archetypeTransitions.getData();
        for (int i = 0; i < s; i++) {
            data[i].process();
        }

        // Clear updated archetypes
        archetypeTransitions.clear();
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

    private static final class ProcessBuffer {

        private BitVector deletedEntitiesLookup = new BitVector(512);
        private BitVector deletedEntitiesLookupOverflow = new BitVector(512);

        private IntBag deletedEntities = new IntBag(512);
        private IntBag deletedEntitiesOverflow = new IntBag(512);

        private Bag<PendingArchetypeTransition> archetypeTransitions = new Bag<>(PendingArchetypeTransition.class, 4);
        private Bag<PendingArchetypeTransition> archetypeTransitionsOverflow = new Bag<>(PendingArchetypeTransition.class, 4);

        private void swap() {
            var deletedEntitiesLookup = this.deletedEntitiesLookup;
            this.deletedEntitiesLookup = this.deletedEntitiesLookupOverflow;
            this.deletedEntitiesLookupOverflow = deletedEntitiesLookup;

            var deletedEntities = this.deletedEntities;
            this.deletedEntities = this.deletedEntitiesOverflow;
            this.deletedEntitiesOverflow = deletedEntities;

            var archetypeTransitions = this.archetypeTransitions;
            this.archetypeTransitions = this.archetypeTransitionsOverflow;
            this.archetypeTransitionsOverflow = archetypeTransitions;
        }

    }

    private static final class EntityCreator implements ObjIntConsumer<Object>, Pooled {

        private final ArchetypeDataSoaImpl archetype;

        private final EntityIndex entityIndex;
        private final IntBag entities;
        private final ComponentAdder[] adders;
        private final int size;

        private final IntBag entityIds = new IntBag(64);
        private final BitSet calls;

        private IntSupplier entityIdSupplier;
        private int i = -2; // exclusive
        private int end = -2; // exclusive

        private int entityId;

        public EntityCreator(ArchetypeDataSoaImpl archetype) {
            this.archetype = archetype;

            this.entityIndex = archetype.entityIndex;
            this.entities = archetype.entities;
            this.adders = archetype.adders;
            this.size = archetype.size;

            this.calls = new BitSet(size);
        }

        private boolean next() {
            if (++i == end) {
                return false;
            }

            this.entityId = entityIdSupplier.getAsInt();
            this.entityIds.add(entityId);

            this.entities.add(entityId);
            this.entityIndex.add(archetype, entityId, i);

            this.calls.set(0, size);

            return true;
        }

        private void validate() {
            if (!calls.isEmpty()) {
                var missing = calls.stream()
                        .mapToObj(i -> archetype.componentTypes.get(i))
                        .toList();

                throw new StorageEngineException("%d/%d components not set for entity %d. %s".formatted(missing.size(), size, entityId, missing));
            }
        }

        @Override
        public void accept(Object component, int index) {
            // Add component
            this.adders[index].add(entityId, i, component);
            this.calls.clear(index);
        }

        @Override
        public void reset() {
            this.entityIdSupplier = null;
            this.i = -2;
            this.end = -2;
            
            this.entityIds.clear();
            this.calls.clear();
        }

    }

    private static final class PendingArchetypeTransition {

        private final Observers observers;
        private final EntityIndex entityIndex;

        private final ArchetypeGraphNode sourceNode;
        private final ArchetypeGraphNode targetNode;

        private IntBag entities = new IntBag(64);
        private IntBag entitiesOverflow = new IntBag(64);

        private IntBag entitiesLookup = new IntBag(64);
        private IntBag entitiesLookupOverflow = new IntBag(64);

        private ArchetypeUpdater updater;

        public PendingArchetypeTransition(Observers observers, EntityIndex entityIndex, ArchetypeGraphNode sourceNode, ArchetypeGraphNode targetNode) {
            this.observers = observers;
            this.entityIndex = entityIndex;

            this.sourceNode = sourceNode;
            this.targetNode = targetNode;
        }

        public void addEntity(int entityId) {
            this.entitiesLookup.set(entityId, this.entities.getSize());
            this.entities.add(entityId);
        }

        public boolean removeEntity(int entityId) {
            var index = this.entitiesLookup.get(entityId);
            if (index == 0 && this.entities.get(index) != entityId) {
                return false;
            }

            // Remove last index
            var lastIndex = this.entities.getSize() - 1;
            if (index == lastIndex) {
                this.entitiesLookup.set(entityId, 0);
                this.entities.removeLast();

                return true;
            }

            // Swap last index to removed index
            var moved = this.entities.get(lastIndex);

            this.entitiesLookup.set(moved, index);
            this.entities.set(index, moved);
            this.entities.removeLast();

            return true;
        }

        public void process() {
            // Double buffering
            var entities = this.entities;
            this.entities = this.entitiesOverflow;
            this.entitiesOverflow = entities;

            var entitiesLookup = this.entitiesLookup;
            this.entitiesLookup = this.entitiesLookupOverflow;
            this.entitiesLookupOverflow = entitiesLookup;

            // Instantiate updater lazily to avoid intermediate archetypes that will never contain entities
            var updater = this.updater;
            if (updater == null) {
                this.updater = updater = new ArchetypeUpdater(observers, entityIndex, sourceNode, targetNode);
            }

            // Run updater, moving entities to target archetype
            updater.process(entities);

            // Clear entities
            entitiesLookup.clear();
            entities.clear();
        }

    }

    private static final class ArchetypeUpdater {

        private final Observers observers;
        private final EntityIndex entityIndex;

        private final ArchetypeDataSoaImpl source;
        private final ArchetypeDataSoaImpl target;
        private final ArchetypeMover mover;

        private final int[] mapping;

        private ArchetypeUpdater(Observers observers, EntityIndex entityIndex, ArchetypeGraphNode sourceNode, ArchetypeGraphNode targetNode) {
            this.observers = observers;
            this.entityIndex = entityIndex;

            this.source = switch (sourceNode.getArchetype()) {
                case ArchetypeDataSoaImpl archetype -> archetype; // switch expression to force compilation error instead of unchecked cast
            };

            this.target = switch (targetNode.getArchetype()) {
                case ArchetypeDataSoaImpl archetype -> archetype; // switch expression to force compilation error instead of unchecked cast
            };

            this.mover = source.createMover(target);

            var components = sourceNode.getComponents();
            this.mapping = new int[components.getSize()];

            for (int i = 0, s = components.getSize(); i < s; i++) {
                this.mapping[i] = target.getComponentIndex(components.get(i).id());
            }
        }

        public void process(IntBag entities) {
            // Dispatch before update event
            observers.triggerEntitiesBeforeUpdate(source, target, entities);

            // Move entities and component data to new archetype
            var data = entities.getData();
            for (int i = 0, s = entities.getSize(); i < s; i++) {
                var entityId = data[i];

                var index = entityIndex.getEntityIndex(source, entityId);
                var changes = source.pendingChanges.get(index);

                mover.moveComponent(entityId, index, changes);
            }

            // Dispatch updated event
            observers.triggerEntitiesUpdated(target, source, entities);
        }

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
         */
        private void moveComponent(int entityId, int sourceIndex, PendingChanges changes) {
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
            target.pendingChanges.ensureCapacity(targetIndex);

            // Update entity index
            entityIndex.setEntityIndex(entityId, target, targetIndex);

            // Reset pending changes
            changes.reset();

            // Return -1 if no source entity was moved
            if (movedIndex == -1) {
                source.entities.removeLast();
                return;
            }

            // Update source tracking tracking
            var movedEntityId = source.entities.removeLast();
            source.entities.set(sourceIndex, movedEntityId);

            var movedPending = source.pendingChanges.get(movedIndex);
            source.pendingChanges.set(movedIndex, source.pendingChanges.get(sourceIndex));
            source.pendingChanges.set(sourceIndex, movedPending);

            // Update entity index for moved entity
            entityIndex.setEntityIndex(movedEntityId, sourceIndex);
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
            return index < alive - 1;
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
                case StorageRelationResult<?> other -> throw new IllegalArgumentException("""
                        Cannot add relations obtained from storage. \
                        Use Relations.copyOf to obtain a copy of an existing Relations object.
                        """);
                case Relations<?> other -> store(entityId, relations, other);
                case Relation<?> relation -> store(entityId, relations, relation);
                case null -> throw new IllegalArgumentException("Cannot add null component to relations");
                default -> throw new IllegalArgumentException("Cannot add component of type '%s' to relations: %s".formatted(component.getClass().getName(), component));
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
