package de.schosin.ecs.storage.archetype.entities.archetypes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.schosin.ecs.api.Pooled;
import de.schosin.ecs.api.components.Relation;
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
import de.schosin.ecs.api.data.DataAccessor;
import de.schosin.ecs.api.data.IterableAccessor;
import de.schosin.ecs.storage.api.StorageEngineException;
import de.schosin.ecs.storage.api.StorageWorld;
import de.schosin.ecs.storage.api.entities.ComponentMask;
import de.schosin.ecs.storage.api.entities.EntityData;
import de.schosin.ecs.storage.archetype.components.ComponentIndex;
import de.schosin.ecs.storage.archetype.entities.ComponentMaskImpl;
import de.schosin.ecs.storage.archetype.entities.EntityIndex;
import de.schosin.ecs.storage.archetype.entities.EntityRelationIndex;
import de.schosin.ecs.storage.common.PendingChanges;
import de.schosin.ecs.storage.common.results.StorageRelationResult;
import de.schosin.ecs.utils.collections.Bag;
import de.schosin.ecs.utils.collections.ImmutableBag;
import de.schosin.ecs.utils.collections.ImmutableIntBag;
import de.schosin.ecs.utils.collections.IntBag;
import de.schosin.ecs.utils.collections.Pool;

public final class ArchetypeDataImpl implements ArchetypeData {

    private final ComponentIndex componentIndex;
    private final EntityRelationIndex relationIndex;
    private final EntityIndex entityIndex;

    private final ComponentMaskImpl componentMask;

    private final Bag<RegularComponentType<?, ?>> componentTypes;
    private final IntBag componentTypeIds;

    private final ComponentAdder<?>[] adders;
    private final Bag<RegularEntityRelationType<?, ?>> entityRelationTypes;

    // data.get(index)[componentId] // index tracked by EntityIndex
    private final Bag<Object[]> data;
    private final IntBag entities;
    private final ImmutableIntBag immutableEntities;
    private final int size;

    private final Bag<PendingChanges> pendingChanges;
    private final EntityDataImpl entityData;

    private final Map<List<RegularComponentType<?, ?>>, EntityDataImpl> entityDataMap = new HashMap<>();
    private final Pool<List<RegularComponentType<?, ?>>> typesPool = Pool.unbounded(List.class, ArrayList::new, List::clear);

    public ArchetypeDataImpl(ComponentIndex componentIndex, EntityRelationIndex relationIndex, EntityIndex entityIndex, ComponentMaskImpl componentMask, StorageWorld storageWorld) {
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

        this.data = storageWorld.createEntityBag(Object[].class);
        this.entities = new IntBag(64);
        this.immutableEntities = ImmutableIntBag.create(entities);

        this.pendingChanges = new Bag<>(PendingChanges.class, 1024);
        this.entityData = getEntityData(Arrays.copyOf(componentTypes.getData(), size));

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
    public <R> R getComponent(int index, RegularComponentType<?, R> componentType) {
        if (index >= entities.getSize()) {
            return null;
        }

        var componentId = componentIndex.getId(componentType);
        var componentIndex = this.componentTypeIds.get(componentId);
        if (componentIndex == -1) {
            return retrievePendingComponent(index, componentType);
        }

        return (R) data.get(index)[componentIndex == -1 ? 0 : componentIndex];
    }

    @Override
    public DataAccessor getAccessor(int entityId) {
        return entityData.getAccessor(entityId);
    }

    @Override
    public int addEntity(int entityId, ImmutableBag<? extends RegularComponentType<?, ?>> componentTypes, ImmutableBag<Object> components) {
        // Track entity index
        var index = entities.getSize();
        this.entities.add(entityId);

        // Get data array
        var data = this.data.getSafe(index);
        if (data == null) {
            data = new Object[size];
            this.data.set(index, data);
        }

        // Fill data array
        for (int i = 0, s = components.getSize(); i < s; i++) {
            // Add component to data
            var componentType = componentTypes.get(i);
            var componentId = componentIndex.getId(componentType);

            var componentIndex = this.componentTypeIds.get(componentId);
            if (componentIndex == -1) {
                entityIndex.freeComponent(components.get(i));
                continue; // removed comonent
            }

            adders[componentIndex].add(entityId, data, components.get(i));
        }

        return index;
    }

    @Override
    public int addEntity(int entityId, ImmutableBag<RegularComponentType<?, ?>> componentTypes, Bag<Object> components,
            ImmutableBag<? extends RegularComponentType<?, ?>> componentTypes2, ImmutableBag<Object> components2) {

        // Track entity index
        var index = entities.getSize();
        this.entities.add(entityId);

        // Get data array
        var data = this.data.getSafe(index);
        if (data == null) {
            data = new Object[size];
            this.data.set(index, data);
        }

        // Fill data array (components 1)
        for (int i = 0, s = components.getSize(); i < s; i++) {
            // Add component to data
            var componentType = componentTypes.get(i);
            var componentId = componentIndex.getId(componentType);

            var componentIndex = this.componentTypeIds.get(componentId);
            if (componentIndex == -1) {
                entityIndex.freeComponent(components.get(i));
                continue; // removed comonent
            }

            adders[componentIndex].add(entityId, data, components.get(i));
        }

        // Fill data array (components 2)
        for (int i = 0, s = components2.getSize(); i < s; i++) {
            // Add component to data
            var componentType = componentTypes2.get(i);
            var componentId = componentIndex.getId(componentType);

            var componentIndex = this.componentTypeIds.get(componentId);
            if (componentIndex == -1) {
                entityIndex.freeComponent(components2.get(i));
                continue; // removed comonent
            }

            adders[componentIndex].add(entityId, data, components2.get(i));
        }

        return index;
    }

    @Override
    public void addComponents(int entityId, int index, ImmutableBag<? extends RegularComponentType<?, ?>> componentTypes, Object[] components) {
        var changes = pendingChanges.getSafe(index);
        if (changes == null) {
            changes = new PendingChanges(componentMask);
            pendingChanges.set(index, changes);
        }

        // Get data array
        var data = this.data.getSafe(index);
        if (data == null) {
            data = new Object[size];
            this.data.set(index, data);
        }

        for (int i = 0, s = componentTypes.getSize(); i < s; i++) {
            var componentType = componentTypes.get(i);
            var component = components[i];

            // Add to pending changes
            if (!changes.add(componentType, component)) {
                // No archetype change, apply directly
                var componentId = componentIndex.getId(componentType);
                var componentIndex = this.componentTypeIds.get(componentId);

                adders[componentIndex].add(entityId, data, component);
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
        var components = data.get(index);
        if (components == null) {
            return -1;
        }

        synchronized (this.data) {
            components = data.get(index);
            if (components == null) {
                return -1;
            }

            // Process components
            for (int i = 0; i < size; i++) {
                var component = components[i];

                if (fill != null) {
                    // Put component into fill bag, required from caller
                    fill.add(component);
                } else {
                    entityIndex.freeComponent(component);
                }
            }

            // Remove row (decrement alive, move last row to removed index if needed)
            var lastIndex = entities.getSize() - 1;
            if (index < lastIndex) {
                // Move components of last row to removed entity's row
                var lastComponents = this.data.get(lastIndex);
                for (int i = 0; i < size; i++) {
                    components[i] = lastComponents[i];
                    lastComponents[i] = null;
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
            Arrays.fill(components, null);

            this.entities.removeLast();

            return -1;
        }
    }

    @Override
    public PendingChanges getPendingChanges(int index) {
        var changes = pendingChanges.getSafe(index);
        return changes == null || changes.isEmpty() ? null : changes;
    }

    private <R> R retrievePendingComponent(int index, int componentId) {
        var componentType = componentIndex.<R>getType(componentId);
        if (componentType == null) {
            return null;
        }

        return retrievePendingComponent(index, componentType);
    }

    private <R> R retrievePendingComponent(int index, RegularComponentType<?, R> componentType) {
        var changes = pendingChanges.getSafe(index);
        if (changes == null) {
            return null;
        }

        return changes.getComponent(componentType);
    }

    @Override
    public String toString() {
        return new StringBuilder()
                .append("ArchetypeData(")
                .append("count = ").append(this.entities.getSize()).append(", ")
                .append("componentMask = ").append(this.componentMask).append(")")
                .toString();
    }

    @Override
    public int getId() {
        return componentMask.getId();
    }

    @Override
    public int getCount() {
        return entities.getSize();
    }

    @Override
    public boolean contains(int entityId) {
        return entityIndex.getComponentMask(entityId) == componentMask;
    }

    @Override
    public ImmutableIntBag getEntities() {
        return immutableEntities;
    }

    @Override
    public int getComponentIndex(int componentId) {
        return componentTypeIds.get(componentId);
    }

    @Override
    public EntityData getEntityData() {
        return entityData;
    }

    @Override
    public EntityDataImpl getEntityData(RegularComponentType<?, ?>... componentTypes) {
        var key = typesPool.getInstance();
        for (var type : componentTypes) {
            key.add(type);
        }

        var result = entityDataMap.get(key);
        if (result != null) {
            typesPool.free(key);
            return result;
        }

        synchronized (entityDataMap) {
            result = entityDataMap.get(key);
            if (result != null) {
                typesPool.free(key);
                return result;
            }

            result = new EntityDataImpl(componentTypes);
            entityDataMap.put(List.copyOf(key), result);

            typesPool.free(key);
            return result;
        }
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
                return (R) data.get(index)[id];
            }

            return (R) retrievePendingComponent(index, componentTypes[0]);
        }

        @Override
        public IterableAccessor getAccessor() {
            return accessors.getInstance();
        }

        @Override
        public DataAccessor getAccessor(int entityId) {
            var index = entityIndex.getEntityIndex(ArchetypeDataImpl.this, entityId);
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
                    .append("EntityDataImpl(archetype = ").append(ArchetypeDataImpl.this).append(")")
                    .toString();
        }

        private class AccessorImpl implements IterableAccessor, Pooled {

            private int index = -1;

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

                return retrievePendingComponent(index, componentId) != null;
            }

            @Override
            @SuppressWarnings("unchecked")
            public <R> R getComponent(int componentId) {
                var componentIndex = componentTypeIds.get(componentId);
                if (componentIndex > -1) {
                    return (R) data.get(index)[componentIndex];
                }

                return retrievePendingComponent(index, componentId);
            }

            @Override
            public <R> R getPendingComponent(int componentId) {
                return retrievePendingComponent(index, componentId);
            }

            @Override
            @SuppressWarnings("unchecked")
            public <R> R getComponentByIndex(int componentIndex) {
                return (R) data.get(index)[componentIndex];
            }

            @Override
            public void free() {
                accessors.free(this);
            }

            @Override
            public void reset() {
                this.index = -1;
            }

            @Override
            public String toString() {
                return new StringBuilder()
                        .append("AccessorImpl(index = ").append(index)
                        .append(", size = ").append(size)
                        .append(", archetype = ").append(ArchetypeDataImpl.this)
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

        public final void add(int entityId, Object[] data, Object component) {
            if (!componentType.isInstance(component)) {
                throw new StorageEngineException("Expected component type '%s' at index %d, but was '%s'".formatted(componentType, componentIndex, component));
            }

            store(entityId, data, component);
        }

        protected abstract void store(int entityId, Object[] data, Object component);
    }

    private final class ClassTypeAdder extends ComponentAdder<RegularComponentType<?, ?>> {

        protected ClassTypeAdder(RegularComponentType<?, ?> componentType, int componentIndex) {
            super(componentType, componentIndex);
        }

        @Override
        protected void store(int entityId, Object[] data, Object component) {
            data[componentIndex] = component;
        }

    }

    private final class RelationAdder extends ComponentAdder<RelationComponentType<?, ?, ?>> {

        private final boolean entityType;

        protected RelationAdder(RelationComponentType<?, ?, ?> componentType, int componentIndex) {
            super(componentType, componentIndex);

            this.entityType = componentType instanceof ExclusiveEntityRelationType<?>;
        }

        @Override
        protected void store(int entityId, Object[] data, Object component) {
            data[componentIndex] = component;

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
        protected void store(int entityId, Object[] data, Object component) {
            // Prepare storage data
            var relations = (StorageRelationResult) data[componentIndex];
            if (relations == null) {
                relations = StorageRelationResult.getInstance(componentType);
                data[componentIndex] = relations;
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
