package de.schosin.ecs.engine.events.builtin;

import de.schosin.ecs.engine.events.builtin.EntitiesEvent.EntitiesInsertedEvent;
import de.schosin.ecs.engine.events.builtin.EntityEvent.EntityInsertedEvent;
import de.schosin.ecs.storage.api.entities.Archetype;
import de.schosin.ecs.utils.collections.ImmutableIntBag;

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

        static EntitiesInsertedEvent get() {
            return new EntitiesInsertedEventImpl();
        }

        EntitiesInsertedEvent with(ImmutableIntBag entityIds, Archetype archetype);

    }

}

final class EntitiesInsertedEventImpl implements EntitiesInsertedEvent {

    private ImmutableIntBag entityIds;
    private Archetype archetype;

    @Override
    public ImmutableIntBag entityIds() {
        return entityIds;
    }

    @Override
    public Archetype archetype() {
        return archetype;
    }

    @Override
    public EntitiesInsertedEvent with(ImmutableIntBag entityIds, Archetype archetype) {
        this.entityIds = entityIds;
        this.archetype = archetype;

        return this;
    }

}
