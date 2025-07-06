package de.schosin.ecs.engine.events.builtin;

import de.schosin.ecs.engine.events.builtin.EntitiesEvent.EntitiesInsertedEvent;
import de.schosin.ecs.engine.events.builtin.EntityEvent.EntityInsertedEvent;
import de.schosin.ecs.storage.api.entities.Archetype;
import de.schosin.ecs.utils.collections.ImmutableIntBag;
import de.schosin.ecs.utils.collections.Pool;

public sealed interface EntitiesEvent extends Event {

    ImmutableIntBag entityIds();

    Archetype archetype();

    /**
     * This event will be dispatched when a batch of entities is created.
     * 
     * <p>
     * <b>Attention:</b> When a single entity is created, 
     * {@link EntityInsertedEvent} will be dispatched instead.
     * </p>
     */
    sealed interface EntitiesInsertedEvent extends EntitiesEvent {

        static EntitiesInsertedEvent get(ImmutableIntBag entityIds, Archetype archetype) {
            return EntitiesInsertedEventImpl.get(entityIds, archetype);
        }

    }

}

abstract sealed class AbstractEntitiesEvent implements EntitiesEvent {

    protected ImmutableIntBag entityIds;
    protected Archetype archetype;

    @Override
    public ImmutableIntBag entityIds() {
        return entityIds;
    }

    @Override
    public Archetype archetype() {
        return archetype;
    }

    @Override
    public void reset() {
        this.entityIds = null;
        this.archetype = null;
    }

}

final class EntitiesInsertedEventImpl extends AbstractEntitiesEvent implements EntitiesInsertedEvent {

    private static final Pool<EntitiesInsertedEventImpl> POOL = Pool.unbounded(EntitiesInsertedEventImpl.class, EntitiesInsertedEventImpl::new);

    static EntitiesInsertedEvent get(ImmutableIntBag entityIds, Archetype archetype) {
        var instance = POOL.getInstance();
        instance.entityIds = entityIds;
        instance.archetype = archetype;

        return instance;
    }

    @Override
    public void free() {
        POOL.free(this);
    }

}
