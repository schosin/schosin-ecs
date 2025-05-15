package de.schosin.ecs.engine.events.builtin;

import de.schosin.ecs.engine.components.ComponentMask;
import de.schosin.ecs.engine.events.builtin.EntitiesEvent.EntitiesInsertedEvent;
import de.schosin.ecs.engine.events.builtin.EntityEvent.EntityInsertedEvent;
import de.schosin.ecs.utils.collections.Pool;

public sealed interface EntitiesEvent extends Event {

    int[] entityIds();

    ComponentMask componentMask();

    /**
     * This event will be dispatched when a batch of entities is created.
     * 
     * <p>
     * <b>Attention:</b> When a single entity is created, 
     * {@link EntityInsertedEvent} will be dispatched instead.
     * </p>
     */
    sealed interface EntitiesInsertedEvent extends EntitiesEvent {

        static EntitiesInsertedEvent get(int[] entityIds, ComponentMask componentMask) {
            return EntitiesInsertedEventImpl.get(entityIds, componentMask);
        }

    }

}

abstract sealed class AbstractEntitiesEvent implements EntitiesEvent {

    protected int[] entityIds;
    protected ComponentMask componentMask;

    @Override
    public int[] entityIds() {
        return entityIds;
    }

    @Override
    public ComponentMask componentMask() {
        return componentMask;
    }

    @Override
    public void reset() {
        this.entityIds = null;
        this.componentMask = null;
    }

}

final class EntitiesInsertedEventImpl extends AbstractEntitiesEvent implements EntitiesInsertedEvent {

    private static final Pool<EntitiesInsertedEventImpl> POOL = Pool.unbounded(EntitiesInsertedEventImpl.class, EntitiesInsertedEventImpl::new);

    static EntitiesInsertedEvent get(int[] entityIds, ComponentMask componentMask) {
        var instance = POOL.getInstance();
        instance.entityIds = entityIds;
        instance.componentMask = componentMask;

        return instance;
    }

    @Override
    public void free() {
        POOL.free(this);
    }

}
