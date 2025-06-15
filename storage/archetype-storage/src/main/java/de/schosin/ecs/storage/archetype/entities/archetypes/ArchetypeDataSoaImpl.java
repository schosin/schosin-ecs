package de.schosin.ecs.storage.archetype.entities.archetypes;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import de.schosin.ecs.api.Pooled;
import de.schosin.ecs.api.components.Relation.ComponentRelation;
import de.schosin.ecs.api.components.Relation.EntityRelation;
import de.schosin.ecs.api.components.Relations;
import de.schosin.ecs.api.components.Relations.ComponentRelations;
import de.schosin.ecs.api.components.Relations.EntityRelations;
import de.schosin.ecs.api.components.types.ClassType;
import de.schosin.ecs.api.components.types.ComponentType.RegularComponentType;
import de.schosin.ecs.api.components.types.RelationComponentType.ComponentRelationType;
import de.schosin.ecs.api.components.types.RelationComponentType.EntityRelationType;
import de.schosin.ecs.api.components.types.RelationComponentType.ExclusiveComponentRelationType;
import de.schosin.ecs.api.components.types.RelationComponentType.ExclusiveEntityRelationType;
import de.schosin.ecs.api.components.types.RelationComponentType.RegularComponentRelationType;
import de.schosin.ecs.api.components.types.RelationComponentType.RegularEntityRelationType;
import de.schosin.ecs.api.data.DataAccessor;
import de.schosin.ecs.api.data.IterableAccessor;
import de.schosin.ecs.storage.api.StorageWorld;
import de.schosin.ecs.storage.api.entities.ComponentMask;
import de.schosin.ecs.storage.api.entities.EntityData;
import de.schosin.ecs.storage.archetype.components.ComponentIndex;
import de.schosin.ecs.storage.archetype.entities.ComponentMaskImpl;
import de.schosin.ecs.storage.archetype.entities.EntityIndex;
import de.schosin.ecs.storage.archetype.entities.EntityRelationIndex;
import de.schosin.ecs.storage.common.PendingChanges;
import de.schosin.ecs.storage.common.results.ComponentRelationResultImpl;
import de.schosin.ecs.storage.common.results.EntityRelationResultImpl;
import de.schosin.ecs.utils.collections.Bag;
import de.schosin.ecs.utils.collections.ImmutableBag;
import de.schosin.ecs.utils.collections.ImmutableIntBag;
import de.schosin.ecs.utils.collections.IntBag;
import de.schosin.ecs.utils.collections.Pool;

/**
 * Auto-growing "Struct of arrays" implementation of {@link ArchetypeData}.
 */
public class ArchetypeDataSoaImpl implements ArchetypeData {

    private final ComponentIndex componentIndex;
    private final EntityRelationIndex relationIndex;
    private final EntityIndex entityIndex;

    private final ComponentMaskImpl componentMask;

    private final Bag<RegularComponentType<?, ?>> componentTypes;
    private final IntBag componentTypeIds;

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
    public ArchetypeDataSoaImpl(ComponentIndex componentIndex, EntityRelationIndex relationIndex, EntityIndex entityIndex, ComponentMaskImpl componentMask, StorageWorld storageWorld) {
        this.componentIndex = componentIndex;
        this.relationIndex = relationIndex;
        this.entityIndex = entityIndex;

        this.componentMask = componentMask;
        this.componentTypes = new Bag<>(componentMask.getComponentTypes());

        this.entityRelationTypes = new Bag<>(RegularEntityRelationType.class, componentMask.getComponentTypes().getSize());

        this.componentTypeIds = componentIndex.createIntBag();

        for (int i = 0, s = componentTypes.getSize(); i < s; i++) {
            var componentType = componentTypes.get(i);

            var componentId = componentIndex.getId(componentType);
            this.componentTypeIds.set(componentId, i);

            if (componentType instanceof RegularEntityRelationType<?, ?> relationType) {
                this.entityRelationTypes.set(i, relationType);
            }
        }

        this.entities = new IntBag(64);
        this.size = componentTypes.getSize();

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

            data[i] = storageWorld.createEntityBag((Class<Object>) clazz);
        }
    }

    @Override
    public int getCount() {
        return alive;
    }

    @Override
    public boolean contains(int entityId) {
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
    public <R> R getComponent(int index, RegularComponentType<?, R> componentType) {
        if (index >= alive) {
            return null;
        }

        var componentId = componentIndex.getId(componentType);
        var componentIndex = this.componentTypeIds.get(componentId);
        if (componentIndex == -1) {
            return retrievePendingComponent(index, componentType);
        }

        return (R) data[componentIndex].get(index);
    }

    @Override
    public DataAccessor getAccessor(int entityId) {
        return entityData.getAccessor(entityId);
    }

    @Override
    public int addEntity(int entityId, ImmutableBag<? extends RegularComponentType<?, ?>> componentTypes, ImmutableBag<Object> components) {
        // Track entity index
        var index = alive++;
        this.entities.add(entityId);

        var componentIds = intBagPool.getInstance();
        for (int i = 0, s = componentTypes.getSize(); i < s; i++) {
            componentIds.set(i, componentIndex.getId(componentTypes.get(i)));
        }

        // Fill data array
        for (int i = 0, s = components.getSize(); i < s; i++) {
            // Add component to data
            var componentType = componentTypes.get(i);
            var componentId = componentIds.get(i);

            var componentIndex = this.componentTypeIds.get(componentId);
            if (componentIndex == -1) {
                entityIndex.freeComponent(components.get(i));
                continue; // removed comonent
            }

            var component = addComponent(index, componentIndex, componentType, components.get(i));

            // Track entity relations
            var relationType = this.entityRelationTypes.get(componentIndex);
            if (relationType != null) {
                relationIndex.add(entityId, relationType, component);
            }
        }

        intBagPool.free(componentIds);

        return index;
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
            var componentType = componentTypes.get(i);
            var componentId = componentIds.get(i);

            var componentIndex = this.componentTypeIds.get(componentId);
            if (componentIndex == -1) {
                entityIndex.freeComponent(components.get(i));
                continue; // removed comonent
            }

            var component = addComponent(index, componentIndex, componentType, components.get(i));

            // Track entity relations
            var relationType = this.entityRelationTypes.get(componentIndex);
            if (relationType != null) {
                relationIndex.add(entityId, relationType, component);
            }
        }

        // Fill data array (components 2)
        for (int i = 0, s = components2.getSize(); i < s; i++) {
            // Add component to data
            var componentType = componentTypes2.get(i);
            var componentId = componentIds2.get(i);

            var componentIndex = this.componentTypeIds.get(componentId);
            if (componentIndex == -1) {
                entityIndex.freeComponent(components2.get(i));
                continue; // removed comonent
            }

            var component = addComponent(index, componentIndex, componentType, components2.get(i));

            // Track entity relations
            var relationType = this.entityRelationTypes.get(componentIndex);
            if (relationType != null) {
                relationIndex.add(entityId, relationType, component);
            }
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

                var result = addComponent(index, componentIndex, componentType, component);

                // Track entity relations
                var relationType = this.entityRelationTypes.get(componentIndex);
                if (relationType != null) {
                    relationIndex.add(entityId, relationType, result);
                }
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

    private Object addComponent(int index, int componentIndex, RegularComponentType<?, ?> componentType, Object component) {
        return switch (component) {
            case ComponentRelationResultImpl relations -> moveRelations(index, componentIndex, relations);
            case ComponentRelations<?, ?> relations -> addRelations(index, componentIndex, relations);
            case ComponentRelation<?, ?> relation -> addRelation(index, componentIndex, (RegularComponentRelationType<?, ?, ?>) componentType, relation);
            case EntityRelationResultImpl relations -> moveRelations(index, componentIndex, relations);
            case EntityRelations<?> relations -> addRelations(index, componentIndex, relations);
            case EntityRelation<?> relation -> addRelation(index, componentIndex, (RegularEntityRelationType<?, ?>) componentType, relation);
            default -> {
                data[componentIndex].set(index, component);
                yield component;
            }
        };
    }

    private Object moveRelations(int index, int componentIndex, ComponentRelationResultImpl relations) {
        var componentData = data[componentIndex];

        var result = (ComponentRelationResultImpl) componentData.get(index);
        if (result == null) {
            result = ComponentRelationResultImpl.getInstance();
            componentData.set(index, result);
        }

        // Copy data over as relations will be freed
        while (!relations.isEmpty()) {
            result.add(relations.removeLast());
        }

        return result;
    }

    private Object addRelations(int index, int componentIndex, ComponentRelations<?, ?> relations) {
        var componentData = data[componentIndex];

        var result = (ComponentRelationResultImpl) componentData.get(index);
        if (result == null) {
            result = ComponentRelationResultImpl.getInstance();
            componentData.set(index, result);
        }

        // Copy data over as relations will be freed
        for (int i = 0, s = relations.size(); i < s; i++) {
            result.add(relations.get(i));
        }

        // Free relations
        Relations.free(relations);

        return result;
    }

    private Object addRelation(int index, int componentIndex, RegularComponentRelationType<?, ?, ?> relationType, ComponentRelation<?, ?> relation) {
        var componentData = data[componentIndex];

        return switch (relationType) {
            case ComponentRelationType<?, ?> type -> {
                var relations = (ComponentRelationResultImpl) componentData.get(index);
                if (relations == null) {
                    relations = ComponentRelationResultImpl.getInstance();
                    componentData.set(index, relations);
                }

                relations.add(relation);

                yield relations;
            }
            case ExclusiveComponentRelationType<?, ?> type -> {
                componentData.set(index, relation);
                yield relation;
            }
        };
    }

    private Object moveRelations(int index, int componentIndex, EntityRelationResultImpl relations) {
        var componentData = data[componentIndex];

        var result = (EntityRelationResultImpl) componentData.get(index);
        if (result == null) {
            result = EntityRelationResultImpl.getInstance();
            componentData.set(index, result);
        }

        // Copy data over as relations will be freed
        while (!relations.isEmpty()) {
            result.add(relations.removeLast());
        }

        return result;
    }

    private Object addRelations(int index, int componentIndex, EntityRelations<?> relations) {
        var componentData = data[componentIndex];

        var result = (EntityRelationResultImpl) componentData.get(index);
        if (result == null) {
            result = EntityRelationResultImpl.getInstance();
            componentData.set(index, result);
        }

        // Copy data over as relations will be freed
        for (int i = 0, s = relations.size(); i < s; i++) {
            result.add(relations.get(i));
        }

        // Free relations
        Relations.free(relations);

        return result;
    }

    private Object addRelation(int index, int componentIndex, RegularEntityRelationType<?, ?> relationType, EntityRelation<?> relation) {
        var componentData = data[componentIndex];

        return switch (relationType) {
            case EntityRelationType<?> type -> {
                var relations = (EntityRelationResultImpl) componentData.get(index);
                if (relations == null) {
                    relations = EntityRelationResultImpl.getInstance();
                    componentData.set(index, relations);
                }

                relations.add(relation);

                yield relations;
            }
            case ExclusiveEntityRelationType<?> type -> {
                componentData.set(index, relation);
                yield relation;
            }
        };
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
            return accessors.getInstance();
        }

        @Override
        public DataAccessor getAccessor(int entityId) {
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
                    return (R) data[componentIndex].get(index);
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
                return (R) data[componentIndex].get(index);
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
                        .append(", archetype = ").append(ArchetypeDataSoaImpl.this)
                        .append(")").toString();
            }

        }

    }

}
