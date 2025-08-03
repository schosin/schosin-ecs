package de.schosin.ecs.engine.events.builtin;

import de.schosin.ecs.engine.events.builtin.EntitiesEvent.EntitiesInsertedEvent;
import de.schosin.ecs.engine.events.builtin.EntityEvent.BeforeEntityUpdateEvent;
import de.schosin.ecs.engine.events.builtin.EntityEvent.EntityInsertedEvent;
import de.schosin.ecs.engine.events.builtin.EntityEvent.EntityRemovedEvent;
import de.schosin.ecs.engine.events.builtin.EntityEvent.EntityUpdatedEvent;
import de.schosin.ecs.storage.api.entities.Archetype;

public sealed interface EntityEvent extends Event {

    int entityId();

    Archetype archetype();

    /**
     * This event will be dispatched when a single entity is created.
     * 
     * <p>
     * <b>Attention:</b> When a a batch of entities are created, 
     * {@link EntitiesInsertedEvent} will be dispatched instead.
     * </p>
     */
    sealed interface EntityInsertedEvent extends EntityEvent {

        static EntityInsertedEvent get() {
            return new EntityInsertedEventImpl();
        }

        EntityInsertedEvent with(int entityId, Archetype archetype);

    }

    sealed interface BeforeEntityUpdateEvent extends EntityEvent {

        static BeforeEntityUpdateEvent get() {
            return new BeforeEntityUpdateEventImpl();
        }

        Archetype newArchetype();

        BeforeEntityUpdateEvent with(int entityId, Archetype archetype, Archetype newArchetype);

    }

    sealed interface EntityUpdatedEvent extends EntityEvent {

        static EntityUpdatedEvent get() {
            return new EntityUpdatedEventImpl();
        }

        Archetype previousArchetype();

        EntityUpdatedEvent with(int entityId, Archetype previousArchetype, Archetype archetype);

    }

    sealed interface EntityRemovedEvent extends EntityEvent {

        static EntityRemovedEvent get() {
            return new EntityRemovedEventImpl();
        }

        EntityRemovedEvent with(int entityId, Archetype archetype);

    }

}

abstract sealed class AbstractEntityEvent implements EntityEvent {

    protected int entityId = -1;
    protected Archetype archetype;

    @Override
    public int entityId() {
        return entityId;
    }

    @Override
    public Archetype archetype() {
        return archetype;
    }

}

final class EntityInsertedEventImpl extends AbstractEntityEvent implements EntityInsertedEvent {

    @Override
    public EntityInsertedEvent with(int entityId, Archetype archetype) {
        this.entityId = entityId;
        this.archetype = archetype;

        return this;
    }

}

final class BeforeEntityUpdateEventImpl extends AbstractEntityEvent implements BeforeEntityUpdateEvent {

    private Archetype newArchetype;

    @Override
    public Archetype newArchetype() {
        return newArchetype;
    }

    @Override
    public BeforeEntityUpdateEvent with(int entityId, Archetype archetype, Archetype newArchetype) {
        this.entityId = entityId;
        this.archetype = archetype;
        this.newArchetype = newArchetype;

        return this;
    }

}

final class EntityUpdatedEventImpl extends AbstractEntityEvent implements EntityUpdatedEvent {

    private Archetype previousArchetype;

    @Override
    public Archetype previousArchetype() {
        return previousArchetype;
    }

    @Override
    public EntityUpdatedEvent with(int entityId, Archetype previousArchetype, Archetype archetype) {
        this.entityId = entityId;
        this.previousArchetype = previousArchetype;
        this.archetype = archetype;

        return this;
    }

}

final class EntityRemovedEventImpl extends AbstractEntityEvent implements EntityRemovedEvent {

    @Override
    public EntityRemovedEvent with(int entityId, Archetype archetype) {
        this.entityId = entityId;
        this.archetype = archetype;

        return this;
    }

}
