package de.schosin.ecs.engine.entities;

import java.util.concurrent.atomic.AtomicInteger;

import de.schosin.ecs.api.Pooled;
import de.schosin.ecs.api.World;
import de.schosin.ecs.engine.BagManager;
import de.schosin.ecs.engine.ChangeManager;
import de.schosin.ecs.engine.components.ComponentManager;
import de.schosin.ecs.engine.components.ComponentMask;
import de.schosin.ecs.engine.components.ComponentMaskManager;
import de.schosin.ecs.storage.api.components.Component;
import de.schosin.ecs.utils.collections.Bag;
import de.schosin.ecs.utils.collections.ImmutableIntBag;
import de.schosin.ecs.utils.collections.IntBag;
import de.schosin.ecs.utils.collections.Pool;

public class EntityManager {

    public interface ComponentsPredicate {
        boolean isInterested(ComponentMask componentMask);
    }

    private final World world;

    private final BagManager bagManager;
    private final ComponentManager componentManager;
    private final ComponentMaskManager componentMaskManager;

    private final AtomicInteger entityId = new AtomicInteger(1);
    private final Bag<Entity> entities = new Bag<>(Entity.class, 64);
    private final Pool<Entity> pool = Pool.unbounded(Entity.class, () -> new Entity(createEntityId()), Entity::reset);

    private final Pool<IntBag> intBagPool = Pool.unbounded(IntBag.class, () -> new IntBag(1000), IntBag::clear);
    private final Bag<IntBag> lentIntBags = new Bag<>(IntBag.class, 8);

    private ChangeManager changeManager;

    public EntityManager(World world, BagManager bagManager, ComponentManager componentManager, ComponentMaskManager componentMaskManager) {
        this.world = world;

        this.bagManager = bagManager;
        this.componentManager = componentManager;
        this.componentMaskManager = componentMaskManager;
    }

    public void process() {
        var data = this.lentIntBags.getData();
        for (int i = 0, s = this.lentIntBags.getSize(); i < s; i++) {
            intBagPool.free(data[i]);
        }
    }

    public int createEntity(Object... components) {
        // Retrieve component mask
        var componentMask = componentMaskManager.getComponentMask(components);

        // Create entity
        return create(componentMask, components);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public int create(ComponentMask componentMask, Object... components) {
        // Create entity
        var entity = createEntity(componentMask);

        for (int i = 0, s = components.length; i < s; i++) {
            var component = components[i];
            var metadata = (Component) componentManager.getComponent(component);

            metadata.addComponentUnsafe(entity.id, component);
        }

        // Add entity
        this.entities.set(entity.id, entity);

        // Notify handlers
        inserted(entity.id, componentMask);

        return entity.id;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public int create(ComponentMask componentMask, Component<?, ?>[] lookup, Object... components) {
        // Create entity
        var entity = createEntity(componentMask);

        for (int i = 0, s = components.length; i < s; i++) {
            var component = components[i];
            var metadata = (Component) lookup[i];

            metadata.addComponent(entity.id, component);
        }

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
     * @param data component data for the entities (outer array for components, inner array for entities) 
     * @param lookup lookup for {@link Component} matching the index of the outer array of data (component index)
     * @return array of entity ids
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public ImmutableIntBag createEntities(ComponentMask componentMask, Bag<Bag<Object>> data, Component<?, ?>[] lookup) {
        var componentSize = data.getSize();
        var count = data.get(0).getSize();

        // Create result bag
        var entityIds = intBagPool.getInstance();
        this.lentIntBags.add(entityIds);

        // Create entities
        for (int i = 0; i < count; i++) {
            var entity = createEntity(componentMask);
            entityIds.add(entity.id);

            this.entities.set(entity.id, entity);
        }

        // Add components
        for (int c = 0; c < componentSize; c++) {
            var metadata = lookup[c];
            var components = data.get(c);

            for (int i = 0; i < count; i++) {
                var entityId = entityIds.get(i);
                ((Component) metadata).addComponent(entityId, components.get(i));
            }
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

    private Entity createEntity(ComponentMask componentMask) {
        var entity = pool.getInstance();
        entity.componentMask = componentMask;

        return entity;
    }

    private int createEntityId() {
        var entityId = this.entityId.getAndIncrement();
        this.bagManager.ensureEntitySize(entityId);

        return entityId;
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
        }

        // Notify managers
        var componentMask = entity.componentMask;

        componentManager.removed(entityId, componentMask);

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