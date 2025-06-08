package de.schosin.ecs.storage.archetype.entities.archetypes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.schosin.ecs.api.Pooled;
import de.schosin.ecs.api.components.Relation.ComponentRelation;
import de.schosin.ecs.api.components.Relation.EntityRelation;
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
import de.schosin.ecs.storage.archetype.results.ComponentRelationResultImpl;
import de.schosin.ecs.storage.archetype.results.EntityRelationResultImpl;
import de.schosin.ecs.utils.collections.Bag;
import de.schosin.ecs.utils.collections.ImmutableBag;
import de.schosin.ecs.utils.collections.ImmutableIntBag;
import de.schosin.ecs.utils.collections.IntBag;
import de.schosin.ecs.utils.collections.Pool;

public class ArchetypeDataImpl implements ArchetypeData {

    private final ComponentIndex componentIndex;
    private final EntityRelationIndex relationIndex;
    private final EntityIndex entityIndex;

    private final ComponentMaskImpl componentMask;

    private final Bag<RegularComponentType<?, ?>> componentTypes;
    private final IntBag componentTypeIds;

    private final Bag<RegularEntityRelationType<?, ?>> entityRelationTypes;

    // data.get(index)[componentId] // index tracked by EntityIndex
    private final Bag<Object[]> data;
    private final IntBag entities;
    private final ImmutableIntBag immutableEntities;
    private final int size;

    private final Map<List<RegularComponentType<?, ?>>, EntityDataImpl> entityDataMap = new HashMap<>();
    private final Pool<List<RegularComponentType<?, ?>>> typesPool = Pool.unbounded(List.class, ArrayList::new, List::clear);

    public ArchetypeDataImpl(ComponentIndex componentIndex, EntityRelationIndex relationIndex, EntityIndex entityIndex, ComponentMaskImpl componentMask, StorageWorld storageWorld) {
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

        this.data = storageWorld.createEntityBag(Object[].class);
        this.entities = new IntBag(64);
        this.immutableEntities = ImmutableIntBag.create(entities);
        this.size = componentTypes.getSize();
    }

    @Override
    public ComponentMask getComponentMask() {
        return componentMask;
    }

    @Override
    public boolean contains(RegularComponentType<?, ?> type) {
        return componentTypes.contains(type);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R> R getComponent(int index, RegularComponentType<?, R> componentType) {
        if (index >= entities.getSize()) {
            return null;
        }

        var componentId = componentIndex.getId(componentType);
        if (componentId >= this.componentTypeIds.getSize()) {
            return null;
        }

        var componentIndex = this.componentTypeIds.get(componentId);
        if (componentIndex == 0) {
            return null;
        }

        return (R) data.get(index)[componentIndex == -1 ? 0 : componentIndex];
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
                componentIndex = 0;
            }

            var component = addComponent(data, componentIndex, componentType, components.get(i));

            // Track entity relations
            var relationType = this.entityRelationTypes.get(componentIndex);
            if (relationType != null) {
                relationIndex.add(entityId, relationType, component);
            }
        }

        return index;
    }

    @Override
    public int addEntity(int entityId, ImmutableBag<RegularComponentType<?, ?>> componentTypes, Bag<Object> components,
            ImmutableBag<? extends RegularComponentType<?, ?>> componentTypes2, Object[] components2) {

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
                componentIndex = 0;
            }

            var component = addComponent(data, componentIndex, componentType, components.get(i));

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
            var componentId = componentIndex.getId(componentType);

            var componentIndex = this.componentTypeIds.get(componentId);
            if (componentIndex == -1) {
                componentIndex = 0;
            }

            var component = addComponent(data, componentIndex, componentType, components2[i]);

            // Track entity relations
            var relationType = this.entityRelationTypes.get(componentIndex);
            if (relationType != null) {
                relationIndex.add(entityId, relationType, component);
            }
        }

        return index;
    }

    @Override
    public void updateComponents(int entityId, int index, ImmutableBag<? extends RegularComponentType<?, ?>> componentTypes, Object[] components) {
        // Get data array
        var data = this.data.get(index);

        // Update components
        for (int i = 0, s = components.length; i < s; i++) {
            // Add component to data
            var componentType = componentTypes.get(i);
            var componentId = componentIndex.getId(componentType);

            var componentIndex = this.componentTypeIds.get(componentId);
            if (componentIndex == -1) {
                componentIndex = 0;
            }

            var component = addComponent(data, componentIndex, componentType, components[i]);

            // Track entity relations
            var relationType = this.entityRelationTypes.get(componentIndex);
            if (relationType != null) {
                relationIndex.add(entityId, relationType, component);
            }
        }
    }

    private Object addComponent(Object[] data, int index, RegularComponentType<?, ?> componentType, Object component) {
        return switch (component) {
            case ComponentRelation<?, ?> relation -> addRelation(data, index, (RegularComponentRelationType<?, ?, ?>) componentType, relation);
            case EntityRelation<?> relation -> addRelation(data, index, (RegularEntityRelationType<?, ?>) componentType, relation);
            default -> data[index] = component;
        };
    }

    private Object addRelation(Object[] data, int index, RegularComponentRelationType<?, ?, ?> relationType, ComponentRelation<?, ?> relation) {
        return switch (relationType) {
            case ComponentRelationType<?, ?> type -> {
                var relations = (ComponentRelationResultImpl) data[index];
                if (relations == null) {
                    relations = ComponentRelationResultImpl.getInstance();
                    data[index] = relations;
                }

                relations.add(relation);

                yield relations;
            }
            case ExclusiveComponentRelationType<?, ?> type -> data[index] = relation;
        };
    }

    private Object addRelation(Object[] data, int index, RegularEntityRelationType<?, ?> relationType, EntityRelation<?> relation) {
        return switch (relationType) {
            case EntityRelationType<?> type -> {
                var relations = (EntityRelationResultImpl) data[index];
                if (relations == null) {
                    relations = EntityRelationResultImpl.getInstance();
                    data[index] = relations;
                }

                relations.add(relation);

                yield relations;
            }
            case ExclusiveEntityRelationType<?> type -> data[index] = relation;
        };
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
    public EntityData getEntityData(RegularComponentType<?, ?>... componentTypes) {
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

    private class EntityDataImpl implements EntityData {

        private final IntBag componentIds;
        private final int[] mapping;
        private final int size;

        private final Pool<AccessorImpl> accessors = Pool.unbounded(AccessorImpl.class, AccessorImpl::new);

        public EntityDataImpl(RegularComponentType<?, ?>[] componentTypes) {
            this.componentIds = new IntBag(componentTypes.length);
            this.mapping = new int[componentTypes.length];

            for (int i = 0, s = componentTypes.length; i < s; i++) {
                var type = componentTypes[i];
                var componentId = componentIndex.getId(type);
                var id = componentTypeIds.get(componentId);

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

            return (R) data.get(index)[0];
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
                if (id == -1) {
                    return null;
                }

                return data.get(index)[id];
            }

            @Override
            public void reset() {
                this.index = -1;
            }

        }

    }

}
