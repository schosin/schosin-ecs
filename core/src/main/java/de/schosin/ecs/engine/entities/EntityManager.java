package de.schosin.ecs.engine.entities;

import de.schosin.ecs.api.Pooled;
import de.schosin.ecs.api.World;
import de.schosin.ecs.engine.ChangeManager;
import de.schosin.ecs.engine.IdManager;
import de.schosin.ecs.engine.IdManager.Id.EntityId;
import de.schosin.ecs.engine.components.ComponentManager;
import de.schosin.ecs.engine.components.ComponentMask;
import de.schosin.ecs.engine.components.ComponentMaskManager;
import de.schosin.ecs.storage.api.components.Component;
import de.schosin.ecs.utils.collections.Bag;
import de.schosin.ecs.utils.collections.BitVector;
import de.schosin.ecs.utils.collections.IntBag;
import de.schosin.ecs.utils.collections.Pool;

public class EntityManager {

    public interface ComponentsPredicate {
        boolean isInterested(BitVector components);
    }

    private final World world;

    private final IdManager idManager;
    private final ComponentManager componentManager;
    private final ComponentMaskManager componentMaskManager;

    private final Bag<Entity> entities = new Bag<>(Entity.class, 64);
    private final Pool<Entity> pool = Pool.unbounded(Entity.class, () -> new Entity(createEntityId()), Entity::reset);

    private ChangeManager changeManager;

    public EntityManager(World world, IdManager idManager, ComponentManager componentManager, ComponentMaskManager componentMaskManager) {
        this.world = world;

        this.idManager = idManager;
        this.componentManager = componentManager;
        this.componentMaskManager = componentMaskManager;
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

        for (var component : components) {
            var metadata = (Component) componentManager.getComponent(component);
            metadata.addComponentUnsafe(entity.id, component);
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
    public int[] createEntities(ComponentMask componentMask, Object[][] data, Component<?>[] lookup) {
        var componentSize = data.length;
        var count = data[0].length;

        // Create entities
        var entityIds = new int[count];
        for (int i = 0; i < count; i++) {
            var entity = createEntity(componentMask);
            entityIds[i] = entity.id;

            this.entities.set(entity.id, entity);
        }

        // Add components
        for (int c = 0; c < componentSize; c++) {
            var metadata = lookup[c];
            var components = data[c];

            for (int i = 0; i < count; i++) {
                var entityId = entityIds[i];
                ((Component) metadata).addComponent(entityId, components[i]);
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

    private void inserted(int[] entityIds, ComponentMask componentMask) {
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

    private EntityId createEntityId() {
        return idManager.createEntityId();
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

                if (predicate.isInterested(entity.componentMask.getMask())) {
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

        private Entity(EntityId entityId) {
            this.id = entityId.id();
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