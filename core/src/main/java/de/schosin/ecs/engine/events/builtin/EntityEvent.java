package de.schosin.ecs.engine.events.builtin;

import de.schosin.ecs.engine.events.builtin.EntitiesEvent.EntitiesInsertedEvent;
import de.schosin.ecs.engine.events.builtin.EntityEvent.BeforeEntityUpdateEvent;
import de.schosin.ecs.engine.events.builtin.EntityEvent.EntityInsertedEvent;
import de.schosin.ecs.engine.events.builtin.EntityEvent.EntityRemovedEvent;
import de.schosin.ecs.engine.events.builtin.EntityEvent.EntityUpdatedEvent;
import de.schosin.ecs.storage.api.entities.Archetype;
import de.schosin.ecs.utils.collections.Pool;

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

        static EntityInsertedEvent get(int entityId, Archetype archetype) {
            return EntityInsertedEventImpl.get(entityId, archetype);
        }

    }

    sealed interface BeforeEntityUpdateEvent extends EntityEvent {

        static BeforeEntityUpdateEvent get(int entityId, Archetype archetype, Archetype newArchetype) {
            return BeforeEntityUpdateEventImpl.get(entityId, archetype, newArchetype);
        }

        Archetype newArchetype();

    }

    sealed interface EntityUpdatedEvent extends EntityEvent {

        static EntityUpdatedEvent get(int entityId, Archetype previousArchetype, Archetype archetype) {
            return EntityUpdatedEventImpl.get(entityId, previousArchetype, archetype);
        }

        Archetype previousArchetype();

    }

    sealed interface EntityRemovedEvent extends EntityEvent {

        static EntityRemovedEvent get(int entityId, Archetype archetype) {
            return EntityRemovedEventImpl.get(entityId, archetype);
        }

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

    @Override
    public void reset() {
        this.entityId = -1;
        this.archetype = null;
    }

}

final class EntityInsertedEventImpl extends AbstractEntityEvent implements EntityInsertedEvent {

    private static final Pool<EntityInsertedEventImpl> POOL = Pool.unbounded(EntityInsertedEventImpl.class, EntityInsertedEventImpl::new);

    static EntityInsertedEvent get(int entityId, Archetype archetype) {
        var instance = POOL.getInstance();
        instance.entityId = entityId;
        instance.archetype = archetype;

        return instance;
    }

    @Override
    public void free() {
        POOL.free(this);
    }

}

final class BeforeEntityUpdateEventImpl extends AbstractEntityEvent implements BeforeEntityUpdateEvent {

    private static final Pool<BeforeEntityUpdateEventImpl> POOL = Pool.unbounded(BeforeEntityUpdateEventImpl.class, BeforeEntityUpdateEventImpl::new);

    static BeforeEntityUpdateEvent get(int entityId, Archetype archetype, Archetype newArchetype) {
        var instance = POOL.getInstance();
        instance.entityId = entityId;
        instance.archetype = archetype;
        instance.newArchetype = newArchetype;

        return instance;
    }

    private Archetype newArchetype;

    @Override
    public Archetype newArchetype() {
        return newArchetype;
    }

    @Override
    public void free() {
        POOL.free(this);
    }

    @Override
    public void reset() {
        super.reset();

        this.newArchetype = null;
    }

}

final class EntityUpdatedEventImpl extends AbstractEntityEvent implements EntityUpdatedEvent {

    private static final Pool<EntityUpdatedEventImpl> POOL = Pool.unbounded(EntityUpdatedEventImpl.class, EntityUpdatedEventImpl::new);

    static EntityUpdatedEvent get(int entityId, Archetype previousArchetype, Archetype archetype) {
        var instance = POOL.getInstance();
        instance.entityId = entityId;
        instance.previousArchetype = previousArchetype;
        instance.archetype = archetype;

        return instance;
    }

    private Archetype previousArchetype;

    @Override
    public Archetype previousArchetype() {
        return previousArchetype;
    }

    @Override
    public void free() {
        POOL.free(this);
    }

    @Override
    public void reset() {
        super.reset();

        this.previousArchetype = null;
    }

}

final class EntityRemovedEventImpl extends AbstractEntityEvent implements EntityRemovedEvent {

    private static final Pool<EntityRemovedEventImpl> POOL = Pool.unbounded(EntityRemovedEventImpl.class, EntityRemovedEventImpl::new);

    static EntityRemovedEvent get(int entityId, Archetype archetype) {
        var instance = POOL.getInstance();
        instance.entityId = entityId;
        instance.archetype = archetype;

        return instance;
    }

    @Override
    public void free() {
        POOL.free(this);
    }

}
