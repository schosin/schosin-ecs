package de.schosin.ecs.engine.events.builtin;

import de.schosin.ecs.engine.events.builtin.EntitiesEvent.EntitiesInsertedEvent;
import de.schosin.ecs.engine.events.builtin.EntityEvent.EntityInsertedEvent;
import de.schosin.ecs.engine.events.builtin.EntityEvent.EntityRemovedEvent;
import de.schosin.ecs.engine.events.builtin.EntityEvent.EntityUpdatedEvent;
import de.schosin.ecs.storage.api.entities.ComponentMask;
import de.schosin.ecs.utils.collections.Pool;

public sealed interface EntityEvent extends Event {

    int entityId();

    ComponentMask componentMask();

    /**
     * This event will be dispatched when a single entity is created.
     * 
     * <p>
     * <b>Attention:</b> When a a batch of entities are created, 
     * {@link EntitiesInsertedEvent} will be dispatched instead.
     * </p>
     */
    sealed interface EntityInsertedEvent extends EntityEvent {

        static EntityInsertedEvent get(int entityId, ComponentMask componentMask) {
            return EntityInsertedEventImpl.get(entityId, componentMask);
        }

    }

    sealed interface EntityUpdatedEvent extends EntityEvent {

        static EntityUpdatedEvent get(int entityId, ComponentMask previousComponentMask, ComponentMask componentMask) {
            return EntityUpdatedEventImpl.get(entityId, previousComponentMask, componentMask);
        }

        ComponentMask previousComponentMask();

    }

    sealed interface EntityRemovedEvent extends EntityEvent {

        static EntityRemovedEvent get(int entityId, ComponentMask componentMask) {
            return EntityRemovedEventImpl.get(entityId, componentMask);
        }

    }

}

abstract sealed class AbstractEntityEvent implements EntityEvent {

    protected int entityId = -1;
    protected ComponentMask componentMask;

    @Override
    public int entityId() {
        return entityId;
    }

    @Override
    public ComponentMask componentMask() {
        return componentMask;
    }

    @Override
    public void reset() {
        this.entityId = -1;
        this.componentMask = null;
    }

}

final class EntityInsertedEventImpl extends AbstractEntityEvent implements EntityInsertedEvent {

    private static final Pool<EntityInsertedEventImpl> POOL = Pool.unbounded(EntityInsertedEventImpl.class, EntityInsertedEventImpl::new);

    static EntityInsertedEvent get(int entityId, ComponentMask componentMask) {
        var instance = POOL.getInstance();
        instance.entityId = entityId;
        instance.componentMask = componentMask;

        return instance;
    }

    @Override
    public void free() {
        POOL.free(this);
    }

}

final class EntityUpdatedEventImpl extends AbstractEntityEvent implements EntityUpdatedEvent {

    private static final Pool<EntityUpdatedEventImpl> POOL = Pool.unbounded(EntityUpdatedEventImpl.class, EntityUpdatedEventImpl::new);

    static EntityUpdatedEvent get(int entityId, ComponentMask previousComponentMask, ComponentMask componentMask) {
        var instance = POOL.getInstance();
        instance.entityId = entityId;
        instance.previousComponentMask = previousComponentMask;
        instance.componentMask = componentMask;

        return instance;
    }

    private ComponentMask previousComponentMask;

    @Override
    public ComponentMask previousComponentMask() {
        return previousComponentMask;
    }

    @Override
    public void free() {
        POOL.free(this);
    }

    @Override
    public void reset() {
        super.reset();

        this.previousComponentMask = null;
    }

}

final class EntityRemovedEventImpl extends AbstractEntityEvent implements EntityRemovedEvent {

    private static final Pool<EntityRemovedEventImpl> POOL = Pool.unbounded(EntityRemovedEventImpl.class, EntityRemovedEventImpl::new);

    static EntityRemovedEvent get(int entityId, ComponentMask componentMask) {
        var instance = POOL.getInstance();
        instance.entityId = entityId;
        instance.componentMask = componentMask;

        return instance;
    }

    @Override
    public void free() {
        POOL.free(this);
    }

}
