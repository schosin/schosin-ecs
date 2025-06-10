package de.schosin.ecs.storage.archetype.entities.archetypes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import de.schosin.ecs.api.Pooled;
import de.schosin.ecs.api.components.Relation.ComponentRelation;
import de.schosin.ecs.api.components.Relation.EntityRelation;
import de.schosin.ecs.api.components.types.ClassType;
import de.schosin.ecs.api.components.types.ComponentType.RegularComponentType;
import de.schosin.ecs.api.components.types.RelationComponentType.ComponentRelationType;
import de.schosin.ecs.api.components.types.RelationComponentType.EntityRelationType;
import de.schosin.ecs.api.components.types.RelationComponentType.ExclusiveComponentRelationType;
import de.schosin.ecs.api.components.types.RelationComponentType.ExclusiveEntityRelationType;
import de.schosin.ecs.api.components.types.RelationComponentType.RegularComponentRelationType;
import de.schosin.ecs.api.components.types.RelationComponentType.RegularEntityRelationType;
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
import de.schosin.ecs.utils.collections.BitVector;
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
    private final List<Bag<Object>> data;
    private final IntBag entities;
    private final int size;

    private final Bag<PendingChanges> pendingChanges;

    private final Map<BitVector, EntityDataImpl> entityDataMap = new ConcurrentHashMap<>();
    private final Pool<IntBag> intBagPool = Pool.unbounded(IntBag.class, () -> new IntBag(16), IntBag::clear);

    private int alive;
    // TODO optimize singleton enums (tags) to not be included in data (take component id into account -> mapping required)

    @SuppressWarnings("unchecked")
    public ArchetypeDataSoaImpl(ComponentIndex componentIndex, EntityRelationIndex relationIndex, EntityIndex entityIndex, ComponentMaskImpl componentMask, StorageWorld storageWorld) {
        this.componentIndex = componentIndex;
        this.relationIndex = relationIndex;
        this.entityIndex = entityIndex;

        this.componentMask = componentMask;

        this.componentTypes = new Bag<>(componentMask.getComponentTypes());
        this.entityRelationTypes = new Bag<>(RegularEntityRelationType.class, componentMask.getComponentTypes().getSize());

        this.componentTypeIds = new IntBag(64);
        for (int i = 0, s = componentTypes.getSize(); i < s; i++) {
            var componentType = componentTypes.get(i);

            var componentId = componentIndex.getId(componentType);
            this.componentTypeIds.set(componentId, i == 0 ? -1 : i);

            if (componentType instanceof RegularEntityRelationType<?, ?> relationType) {
                this.entityRelationTypes.set(i, relationType);
            }
        }

        this.entities = new IntBag(64);
        this.size = componentTypes.getSize();

        this.pendingChanges = storageWorld.createEntityBag(PendingChanges.class);

        var data = new ArrayList<Bag<Object>>(size);
        for (int i = 0; i < size; i++) {
            var clazz = switch (componentTypes.get(i)) {
                case ClassType<?> type -> type.clazz();
                case ComponentRelationType<?, ?> type -> ComponentRelationResultImpl.class;
                case ExclusiveComponentRelationType<?, ?> type -> ComponentRelation.class;
                case EntityRelationType<?> type -> EntityRelationResultImpl.class;
                case ExclusiveEntityRelationType<?> type -> EntityRelation.class;
            };

            data.add(storageWorld.createEntityBag((Class<Object>) clazz));
        }

        this.data = List.copyOf(data);
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
    public EntityData getEntityData(RegularComponentType<?, ?>... componentTypes) {
        var vector = new BitVector();

        for (var type : componentTypes) {
            var componentId = componentIndex.getId(type);

            vector.set(componentId);
        }

        return entityDataMap.computeIfAbsent(vector, key -> new EntityDataImpl(componentTypes));
    }

    @Override
    public ComponentMask getComponentMask() {
        return componentMask;
    }

    @Override
    public boolean contains(long index, RegularComponentType<?, ?> type) {
        if (componentTypes.contains(type)) {
            return true;
        }

        var changes = getPendingChanges(index);
        return changes != null && changes.getAddedTypes().contains(type);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R> R getComponent(long indexL, RegularComponentType<?, R> componentType) {
        var index = (int) indexL;
        if (index >= alive) {
            return null;
        }

        var componentId = componentIndex.getId(componentType);
        if (componentId >= this.componentTypeIds.getSize()) {
            return getPendingComponent(index, componentType);
        }

        var componentIndex = this.componentTypeIds.getSafe(componentId);
        if (componentIndex == 0) {
            return getPendingComponent(index, componentType);
        }

        return (R) data.get(componentIndex == -1 ? 0 : componentIndex).get(index);
    }

    /**
     * Add an entity, returning its index.
     *
     * @param id of entity
     * @param componentTypes component types matching components
     * @param components components to add
     * @return index of entity
     */
    @Override
    public long addEntity(int entityId, ImmutableBag<? extends RegularComponentType<?, ?>> componentTypes, ImmutableBag<Object> components) {
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

            var componentIndex = this.componentTypeIds.getSafe(componentId);
            if (componentIndex == 0) {
                entityIndex.freeComponent(components.get(i));
                continue; // removed comonent
            }

            if (componentIndex == -1) {
                componentIndex = 0;
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
    public long addEntity(int entityId, ImmutableBag<RegularComponentType<?, ?>> componentTypes, Bag<Object> components,
            ImmutableBag<? extends RegularComponentType<?, ?>> componentTypes2, Object[] components2) {

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

            var componentIndex = this.componentTypeIds.getSafe(componentId);
            if (componentIndex == 0) {
                entityIndex.freeComponent(components.get(i));
                continue; // removed comonent
            }

            if (componentIndex == -1) {
                componentIndex = 0;
            }

            var component = addComponent(index, componentIndex, componentType, components.get(i));

            // Track entity relations
            var relationType = this.entityRelationTypes.get(componentIndex);
            if (relationType != null) {
                relationIndex.add(entityId, relationType, component);
            }
        }

        // Fill data array (components 2)
        for (int i = 0, s = components2.length; i < s; i++) {
            // Add component to data
            var componentType = componentTypes2.get(i);
            var componentId = componentIds2.get(i);

            var componentIndex = this.componentTypeIds.getSafe(componentId);
            if (componentIndex == 0) {
                entityIndex.freeComponent(components.get(i));
                continue; // removed comonent
            }

            if (componentIndex == -1) {
                componentIndex = 0;
            }

            var component = addComponent(index, componentIndex, componentType, components2[i]);

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
    public long addEntity(int entityId, ImmutableBag<RegularComponentType<?, ?>> componentTypes, Bag<Object> components,
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

            var componentIndex = this.componentTypeIds.getSafe(componentId);
            if (componentIndex == 0) {
                entityIndex.freeComponent(components.get(i));
                continue; // removed comonent
            }

            if (componentIndex == -1) {
                componentIndex = 0;
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

            var componentIndex = this.componentTypeIds.getSafe(componentId);
            if (componentIndex == 0) {
                entityIndex.freeComponent(components.get(i));
                continue; // removed comonent
            }

            if (componentIndex == -1) {
                componentIndex = 0;
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
    public void addComponents(int entityId, long indexL, ImmutableBag<? extends RegularComponentType<?, ?>> componentTypes, Object[] components) {
        var index = (int) indexL;

        var changes = pendingChanges.get(index);
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
                if (componentIndex == -1) {
                    componentIndex = 0;
                }

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
    public void removeComponents(int entityId, long indexL, ImmutableBag<? extends RegularComponentType<?, ?>> componentTypes) {
        var index = (int) indexL;

        var changes = pendingChanges.get(index);
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
            case ComponentRelationResultImpl relations -> addRelations(index, componentIndex, relations);
            case ComponentRelation<?, ?> relation -> addRelation(index, componentIndex, (RegularComponentRelationType<?, ?, ?>) componentType, relation);
            case EntityRelationResultImpl relations -> addRelations(index, componentIndex, relations);
            case EntityRelation<?> relation -> addRelation(index, componentIndex, (RegularEntityRelationType<?, ?>) componentType, relation);
            default -> {
                data.get(componentIndex).set(index, component);
                yield component;
            }
        };
    }

    private Object addRelations(int index, int componentIndex, ComponentRelationResultImpl relations) {
        var componentData = data.get(componentIndex);

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

    private Object addRelation(int index, int componentIndex, RegularComponentRelationType<?, ?, ?> relationType, ComponentRelation<?, ?> relation) {
        var componentData = data.get(componentIndex);

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

    private Object addRelations(int index, int componentIndex, EntityRelationResultImpl relations) {
        var componentData = data.get(componentIndex);

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

    private Object addRelation(int index, int componentIndex, RegularEntityRelationType<?, ?> relationType, EntityRelation<?> relation) {
        var componentData = data.get(componentIndex);

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

    /**
     * Delete the entity at the given index.
     *
     * @param entityId id of entity
     * @param index index of entity
     * @param fill bag that will contain components of deleted entity
     * @return id of entity swapped to index position, or -1 if no swap
     */
    @Override
    public int removeEntity(int entityId, long indexL, Bag<Object> fill) {
        int index = (int) indexL;

        if (entities.get(index) != entityId) {
            return -1;
        }

        synchronized (this.data) {
            if (entities.get(index) != entityId) {
                return -1;
            }

            // Process components
            for (int i = 0; i < size; i++) {
                var component = data.get(i).get(index);

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
                    var components = data.get(i);
                    components.set(index, components.get(lastIndex));
                    components.set(lastIndex, null);
                }

                // Swap entity lookup
                var swappedEntityId = this.entities.get(lastIndex);

                this.entities.set(index, swappedEntityId);
                this.entities.removeLast();

                var pending = this.pendingChanges.get(lastIndex);
                this.pendingChanges.set(lastIndex, this.pendingChanges.get(index));
                this.pendingChanges.set(index, pending);

                // Return id of swapped entity
                return swappedEntityId;
            }

            // Last element removed, no swap required
            for (int i = 0; i < size; i++) {
                data.get(i).set(lastIndex, null);
            }

            this.entities.removeLast();

            return -1;
        }
    }

    @Override
    public PendingChanges getPendingChanges(long indexL) {
        var index = (int) indexL;

        var changes = pendingChanges.get(index);
        return changes == null || changes.isEmpty() ? null : changes;
    }

    private <R> R getPendingComponent(int index, RegularComponentType<?, R> componentType) {
        var changes = pendingChanges.get(index);
        if (changes == null) {
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

    private class EntityDataImpl implements EntityData {

        private final RegularComponentType<?, ?>[] componentTypes;
        private final IntBag componentIds;
        private final int[] mapping;
        private final int size;

        private final Pool<AccessorImpl> accessors = Pool.unbounded(AccessorImpl.class, AccessorImpl::new);

        public EntityDataImpl(RegularComponentType<?, ?>[] componentTypes) {
            this.componentTypes = componentTypes;
            this.componentIds = new IntBag(componentTypes.length);
            this.mapping = new int[componentTypes.length];

            for (int i = 0, s = componentTypes.length; i < s; i++) {
                var type = componentTypes[i];
                var componentId = componentIndex.getId(type);
                var id = componentTypeIds.getSafe(componentId);

                componentIds.add(componentId);
                mapping[i] = id == 0 ? -1 : id == -1 ? 0 : id;
            }

            this.size = componentTypes.length;
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

            return (R) data.get(0).get(index);
        }

        @Override
        public Accessor getAccessor() {
            return accessors.getInstance();
        }

        @Override
        public void freeAccessor(Accessor accessor) {
            accessors.free((AccessorImpl) accessor);
        }

        private class AccessorImpl implements Accessor, Pooled {

            private int index = -1;

            @Override
            public boolean hasNext() {
                return ++index < entities.getSize();
            }

            @Override
            public int next() {
                return entities.get(index);
            }

            @Override
            public Object getComponent(int componentIndex) {
                var id = mapping[componentIndex];
                if (id > -1) {
                    return data.get(id).get(index);
                }

                return getPendingComponent(componentIndex, componentTypes[componentIndex]);
            }

            @Override
            public void reset() {
                this.index = -1;
            }

        }

    }

}
