package de.schosin.ecs.engine.entities;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import de.schosin.ecs.api.Pooled;
import de.schosin.ecs.api.World;
import de.schosin.ecs.api.components.Relation;
import de.schosin.ecs.api.components.Relation.ComponentRelation;
import de.schosin.ecs.api.components.Relation.EntityRelation;
import de.schosin.ecs.api.components.Relations;
import de.schosin.ecs.api.components.types.ComponentType;
import de.schosin.ecs.api.components.types.ComponentType.RegularComponentType;
import de.schosin.ecs.api.components.types.RelationComponentType;
import de.schosin.ecs.api.data.DataAccessor;
import de.schosin.ecs.engine.BagManager;
import de.schosin.ecs.engine.ChangeManager;
import de.schosin.ecs.storage.api.StorageEngine;
import de.schosin.ecs.storage.api.components.Component;
import de.schosin.ecs.storage.api.entities.Archetype;
import de.schosin.ecs.storage.api.entities.ComponentMask;
import de.schosin.ecs.utils.collections.Bag;
import de.schosin.ecs.utils.collections.ImmutableBag;
import de.schosin.ecs.utils.collections.ImmutableIntBag;
import de.schosin.ecs.utils.collections.IntBag;
import de.schosin.ecs.utils.collections.Pool;

public class EntityManager {

    public interface ComponentsPredicate {
        boolean isInterested(ComponentMask componentMask);
    }

    private static final RegularComponentType<?, ?>[] EMPTY_COMPONENT_TYPES = new RegularComponentType<?, ?>[0];

    private final World world;
    private final StorageEngine storageEngine;

    private final BagManager bagManager;

    private final AtomicInteger entityId = new AtomicInteger(1);
    private final Bag<Entity> entities = new Bag<>(Entity.class, 64);
    private final Pool<Entity> pool = Pool.unbounded(Entity.class, () -> new Entity(createEntityId()), Entity::reset);

    private final Pool<IntBag> intBagPool = Pool.unbounded(IntBag.class, () -> new IntBag(1000), IntBag::clear);
    private final Bag<IntBag> lentIntBags = new Bag<>(IntBag.class, 8);

    private ChangeManager changeManager;

    public EntityManager(World world, StorageEngine storageEngine, BagManager bagManager) {
        this.world = world;
        this.storageEngine = storageEngine;

        this.bagManager = bagManager;
    }

    public void process() {
        var data = this.lentIntBags.getData();
        for (int i = 0, s = this.lentIntBags.getSize(); i < s; i++) {
            intBagPool.free(data[i]);
        }

        this.lentIntBags.clear();
    }

    public int createEntity(Object... components) {
        // Determine component types and merge relations
        var componentTypes = determineComponentTypes(components);
        var merged = mergeRelations(components, componentTypes);
        if (merged != components) {
            components = merged;
            componentTypes = determineComponentTypes(components);
        }

        // Get archetype, sort components in-place
        var archetype = storageEngine.getArchetype(componentTypes);
        if (components.length > 1) {
            sortComponentsInPlace(components, componentTypes, archetype);
        }

        // Create entity
        var entity = createEntityInstance();
        entity.componentMask = archetype.getComponentMask();

        archetype.createEntity(entity.id, components);

        // Add entity
        this.entities.set(entity.id, entity);

        // Notify handlers
        inserted(entity.id, entity.componentMask);

        return entity.id;
    }

    private RegularComponentType<?, ?>[] determineComponentTypes(Object[] components) {
        if (components.length == 0) {
            return EMPTY_COMPONENT_TYPES;
        }

        var componentTypes = new RegularComponentType<?, ?>[components.length];
        for (int i = 0, s = components.length; i < s; i++) {
            componentTypes[i] = ComponentType.detectComponentType(components[i]);
        }

        return componentTypes;
    }

    @SuppressWarnings("unchecked")
    private Object[] mergeRelations(Object[] components, RegularComponentType<?, ?>[] componentTypes) {
        if (components.length < 2) {
            return components;
        }

        // See if any non-exclusive relations are present
        var indices = intBagPool.getInstance();
        for (int i = 0, s = componentTypes.length; i < s; i++) {
            if (componentTypes[i] instanceof RelationComponentType<?, ?, ?>) {
                indices.add(i);
            }
        }

        if (indices.isEmpty()) {
            intBagPool.free(indices);
            return components;
        }

        // Create lookup map based on type to either a single relation, or a list of relations
        var lookup = new HashMap<RegularComponentType<?, ?>, Object>();
        for (int i = 0, s = indices.getSize(); i < s; i++) {
            var index = indices.get(i);
            var relation = components[index];

            lookup.merge(componentTypes[index], relation, (existing, item) -> {
                return switch (existing) {
                    case Relation<?> r -> {
                        yield new ArrayList<Object>(List.of(r, item));
                    }
                    default -> {
                        var list = (List<Object>) existing;

                        list.add(item);
                        yield list;
                    }
                };
            });
        }

        // Return early if only unique relations found
        var newSize = components.length - indices.getSize() + lookup.size();
        if (newSize == components.length) {
            intBagPool.free(indices);
            return components;
        }

        // Build new components array
        var result = new Object[newSize];

        var index = 0;
        for (int i = 0, s = components.length; i < s; i++) {
            if (indices.contains(i)) {
                continue;
            }

            result[index++] = components[i];
        }

        for (var value : lookup.values()) {
            var component = value instanceof List<?> list
                    ? list.get(0) instanceof ComponentRelation<?, ?>
                            ? Relations.create(list.toArray(ComponentRelation[]::new))
                            : Relations.create(list.toArray(EntityRelation[]::new))
                    : value;

            result[index++] = component;
        }

        intBagPool.free(indices);
        return result;
    }

    private static void sortComponentsInPlace(Object[] components, RegularComponentType<?, ?>[] componentTypes, Archetype archetype) {
        for (int i = 0, s = components.length; i < s; i++) {
            var index = archetype.getComponentIndex(componentTypes[i]);
            if (index != i) {
                swap(components, i, index);
                swap(componentTypes, i, index);

                i--;
            }
        }
    }

    private static void swap(Object[] array, int oldIndex, int newIndex) {
        var item = array[newIndex];
        array[newIndex] = array[oldIndex];
        array[oldIndex] = item;
    }

    public int create(ComponentMask componentMask, ImmutableBag<RegularComponentType<?, ?>> componentTypes, Object... components) {
        // Create entity
        var entity = createEntityInstance();
        entity.componentMask = storageEngine.create(entity.id, componentMask, componentTypes, components);

        // Add entity
        this.entities.set(entity.id, entity);

        // Notify handlers
        inserted(entity.id, componentMask);

        return entity.id;
    }

    /**
     * Creates the number of entities described by the length of the inner arrays of data.
     * 
     * @param componentMask component mask for the entities
     * @param data component data for the entities (outer bag for entities, inner bag for components) 
     * @param lookup lookup for {@link Component} matching the index of the outer array of data (component index)
     * @return array of entity ids
     */
    public ImmutableIntBag createEntities(ComponentMask componentMask, Bag<Bag<Object>> data, ImmutableBag<RegularComponentType<?, ?>> componentTypes) {
        var count = data.getSize();

        // Create result bag
        var entityIds = intBagPool.getInstance();
        this.lentIntBags.add(entityIds);

        // Create entities
        for (int i = 0; i < count; i++) {
            var components = data.get(i);

            // Create entity
            var entity = createEntityInstance();
            entity.componentMask = storageEngine.create(entity.id, componentMask, componentTypes, components);

            // Add entity
            this.entities.set(entity.id, entity);
            entityIds.add(entity.id);
        }

        // Notify listeners
        inserted(entityIds, componentMask);

        return entityIds;
    }

    private void inserted(int entityId, ComponentMask componentMask) {
        if (changeManager == null) {
            this.changeManager = world.getSingleton(ChangeManager.class);
        }

        changeManager.inserted(entityId, componentMask);
    }

    private void inserted(ImmutableIntBag entityIds, ComponentMask componentMask) {
        if (changeManager == null) {
            this.changeManager = world.getSingleton(ChangeManager.class);
        }

        changeManager.inserted(entityIds, componentMask);
    }

    private Entity createEntityInstance() {
        return pool.getInstance();
    }

    private int createEntityId() {
        var entityId = this.entityId.getAndIncrement();
        this.bagManager.ensureEntitySize(entityId);

        return entityId;
    }

    public DataAccessor getAccessor(int entityId) {
        return storageEngine.getAccessor(entityId);
    }

    public boolean isActive(int entityId) {
        return this.entities.get(entityId) != null;
    }

    public IntBag getEntities(ComponentsPredicate predicate) {
        var result = new IntBag(1024);

        synchronized (this.entities) {
            for (int i = 0, s = this.entities.getSize(); i < s; i++) {
                var entity = this.entities.get(i);
                if (entity == null) {
                    continue;
                }

                // TODO iterate over component masks instead, checking this only once
                if (predicate.isInterested(entity.componentMask)) {
                    result.add(entity.id);
                }
            }

        }

        return result;
    }

    public void deleteEntity(int entityId) {
        var entity = this.entities.get(entityId);
        if (entity == null) {
            return;
        }

        synchronized (this.entities) {
            entity = this.entities.get(entityId);
            if (entity == null) {
                return;
            }

            this.entities.set(entityId, null);
            this.storageEngine.delete(entityId);
        }

        // Add entity to pool for reuse
        this.pool.free(entity);
    }

    /**
     * @return component mask for the entity or null if entity does not exist
     */
    public ComponentMask getComponentMask(int entityId) {
        var entity = this.entities.get(entityId);
        if (entity == null) {
            return null;
        }

        return entity.componentMask;
    }

    /**
     * Sets the component mask
     * 
     * @param entityId id of the entity
     * @param componentMask new component mask
     * @return true if the entity exists and the mask was updated (e.g. not unchanged)
     */
    public boolean updateComponentMask(int entityId, ComponentMask componentMask) {
        // Retrieve entity, return early if not found or no changes
        var entity = this.entities.get(entityId);
        if (entity == null) {
            return false;
        }

        // Update component mask
        return entity.setComponentMask(componentMask);
    }

    private class Entity implements Pooled {

        private final int id;

        private ComponentMask componentMask;

        private Entity(int entityId) {
            this.id = entityId;
        }

        /**
         * Sets the component mask unless the entity already has the same mask. 
         * 
         * @param componentMask component mask to set
         * @return true if the component mask differs from the current one
         */
        private boolean setComponentMask(ComponentMask componentMask) {
            if (componentMask.getId() == this.componentMask.getId()) {
                return false;
            }

            this.componentMask = componentMask;
            return true;
        }

        @Override
        public void reset() {
            this.componentMask = null;
        }

    }

}