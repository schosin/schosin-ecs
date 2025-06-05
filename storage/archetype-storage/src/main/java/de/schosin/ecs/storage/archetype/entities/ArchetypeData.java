package de.schosin.ecs.storage.archetype.entities;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Objects;

import org.jspecify.annotations.NonNull;

import de.schosin.ecs.api.Pooled;
import de.schosin.ecs.api.components.Relation;
import de.schosin.ecs.api.components.Relation.ComponentRelation;
import de.schosin.ecs.api.components.Relation.EntityRelation;
import de.schosin.ecs.api.components.Result.ComponentRelationResult;
import de.schosin.ecs.api.components.Result.EntityRelationResult;
import de.schosin.ecs.api.components.types.ComponentType.RegularComponentType;
import de.schosin.ecs.api.components.types.RelationComponentType.ComponentRelationType;
import de.schosin.ecs.api.components.types.RelationComponentType.EntityRelationType;
import de.schosin.ecs.api.components.types.RelationComponentType.ExclusiveComponentRelationType;
import de.schosin.ecs.api.components.types.RelationComponentType.ExclusiveEntityRelationType;
import de.schosin.ecs.api.components.types.RelationComponentType.RegularComponentRelationType;
import de.schosin.ecs.api.components.types.RelationComponentType.RegularEntityRelationType;
import de.schosin.ecs.storage.api.StorageWorld;
import de.schosin.ecs.storage.api.entities.ComponentMask;
import de.schosin.ecs.storage.archetype.components.ComponentIndex;
import de.schosin.ecs.utils.collections.Bag;
import de.schosin.ecs.utils.collections.ImmutableBag;
import de.schosin.ecs.utils.collections.IntBag;
import de.schosin.ecs.utils.collections.Pool;

public class ArchetypeData {

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
    private final int size;

    private int alive;
    // TODO optimize singleton enums (tags) to not be included in data (take component id into account -> mapping required)

    public ArchetypeData(ComponentIndex componentIndex, EntityRelationIndex relationIndex, EntityIndex entityIndex, ComponentMaskImpl componentMask, StorageWorld storageWorld) {
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
        this.size = componentTypes.getSize();
    }

    public ComponentMask getComponentMask() {
        return componentMask;
    }

    public boolean contains(RegularComponentType<?, ?> type) {
        return componentTypes.contains(type);
    }

    @SuppressWarnings("unchecked")
    public <R> R getComponent(int index, RegularComponentType<?, R> componentType) {
        if (index >= alive) {
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

    /**
     * Add an entity, returning its index.
     * 
     * @param id of entity
     * @param componentTypes component types matching components
     * @param components components to add
     * @return index of entity
     */
    public int addEntity(int entityId, ImmutableBag<? extends RegularComponentType<?, ?>> componentTypes, ImmutableBag<Object> components) {
        // Track entity index
        var index = alive++;
        this.entities.set(index, entityId);

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

    public int addEntity(int entityId, ImmutableBag<RegularComponentType<?, ?>> componentTypes, Bag<Object> components,
            ImmutableBag<? extends RegularComponentType<?, ?>> componentTypes2, Object[] components2) {

        // Track entity index
        var index = alive++;
        this.entities.set(index, entityId);

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

    /**
     * Delete the entity at the given index.
     * 
     * @param entityId id of entity
     * @param index index of entity
     * @param fill bag that will contain components of deleted entity
     * @return id of entity swapped to index position, or -1 if no swap
     */
    public int deleteEntity(int entityId, int index, Bag<Object> fill) {
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
            alive--;
            if (index < alive) {
                // Move components of last row to removed entity's row
                var lastComponents = this.data.get(alive);
                for (int i = 0; i < size; i++) {
                    components[i] = lastComponents[i];
                    lastComponents[i] = null;
                }

                // Swap entity lookup
                var swappedEntityId = this.entities.get(alive);

                this.entities.set(alive, -1);
                this.entities.set(index, swappedEntityId);

                // Return id of swapped entity
                return swappedEntityId;
            }

            // Last element removed, no swap required
            Arrays.fill(components, null);

            this.entities.set(alive, -1);

            return -1;
        }
    }

    @Override
    public String toString() {
        return new StringBuilder()
                .append("ArchetypeData(")
                .append("count = ").append(this.alive).append(", ")
                .append("componentMask = ").append(this.componentMask).append(")")
                .toString();
    }

}

@SuppressWarnings("rawtypes")
class ComponentRelationResultImpl implements ComponentRelationResult, Pooled {

    private static final Pool<ComponentRelationResultImpl> POOL = Pool.unbounded(ComponentRelationResultImpl.class, ComponentRelationResultImpl::new);

    private final Bag<ComponentRelation<?, ?>> relations = new Bag<>(ComponentRelation.class, 4);

    private ComponentRelationResultImpl() {
    }

    public static ComponentRelationResultImpl getInstance() {
        return POOL.getInstance();
    }

    public void free() {
        POOL.free(this);
    }

    public synchronized void add(ComponentRelation<?, ?> relation) {
        var data = relations.getData();
        for (int i = 0, s = relations.getSize(); i < s; i++) {
            var existing = data[i];
            if (Objects.equals(existing.target(), relation.target())) {
                relations.set(i, relation);
                return;
            }
        }

        this.relations.add(relation);
    }

    @NonNull
    @Override
    public ComponentRelation<?, ?> get(int i) {
        return this.relations.get(i);
    }

    @Override
    public int size() {
        return relations.getSize();
    }

    @Override
    public boolean isEmpty() {
        return relations.isEmpty();
    }

    @Override
    public Object getRelationship(Object target) {
        var data = relations.getData();
        for (int i = 0, s = relations.getSize(); i < s; i++) {
            var relation = data[i];
            if (Objects.equals(relation.target(), target)) {
                return relation.relationship();
            }
        }

        return null;
    }

    @Override
    public Iterator<? extends ComponentRelation<?, ?>> iterator() {
        return relations.iterator();
    }

    @Override
    public void reset() {
        for (int i = 0, s = this.relations.getSize(); i < s; i++) {
            Relation.free(this.relations.get(i));
        }

        this.relations.clear();
    }

}

@SuppressWarnings("rawtypes")
class EntityRelationResultImpl implements EntityRelationResult, Pooled {

    private static final Pool<EntityRelationResultImpl> POOL = Pool.unbounded(EntityRelationResultImpl.class, EntityRelationResultImpl::new);

    private final Bag<EntityRelation<?>> relations = new Bag<>(EntityRelation.class, 4);
    private final Bag<EntityRelation<?>> targetLookup = new Bag<>(EntityRelation.class, 4);

    private EntityRelationResultImpl() {
    }

    public static EntityRelationResultImpl getInstance() {
        return POOL.getInstance();
    }

    public void free() {
        POOL.free(this);
    }

    public synchronized void add(EntityRelation<?> relation) {
        targetLookup.set(relation.target(), relation);

        var data = relations.getData();
        for (int i = 0, s = relations.getSize(); i < s; i++) {
            var existing = data[i];
            if (Objects.equals(existing.target(), relation.target())) {
                relations.set(i, relation);

                return;
            }
        }

        this.relations.add(relation);
    }

    public void removeTarget(int target) {
        var relation = this.targetLookup.get(target);
        if (relation == null) {
            return;
        }

        this.targetLookup.set(target, null);
        this.relations.remove(relation);
    }

    @NonNull
    @Override
    public EntityRelation<?> get(int i) {
        return this.relations.get(i);
    }

    @Override
    public int size() {
        return relations.getSize();
    }

    @Override
    public boolean isEmpty() {
        return relations.isEmpty();
    }

    @Override
    public Object getRelationship(int target) {
        var data = relations.getData();
        for (int i = 0, s = relations.getSize(); i < s; i++) {
            var relation = data[i];
            if (relation.target() == target) {
                return relation.relationship();
            }
        }

        return null;
    }

    @Override
    public Iterator<? extends EntityRelation<?>> iterator() {
        return relations.iterator();
    }

    @Override
    public void reset() {
        for (int i = 0, s = relations.getSize(); i < s; i++) {
            Relation.free(relations.get(i));
        }

        this.relations.clear();
        this.targetLookup.clear();
    }

}
