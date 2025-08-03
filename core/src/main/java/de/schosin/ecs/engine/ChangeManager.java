package de.schosin.ecs.engine;

import de.schosin.ecs.api.components.types.ComponentType;
import de.schosin.ecs.api.components.types.ComponentType.RegularComponentType;
import de.schosin.ecs.engine.entities.EntityManager;
import de.schosin.ecs.engine.events.EventManager;
import de.schosin.ecs.engine.events.builtin.EntitiesEvent.EntitiesInsertedEvent;
import de.schosin.ecs.engine.events.builtin.EntityEvent.BeforeEntityUpdateEvent;
import de.schosin.ecs.engine.events.builtin.EntityEvent.EntityInsertedEvent;
import de.schosin.ecs.engine.events.builtin.EntityEvent.EntityRemovedEvent;
import de.schosin.ecs.engine.events.builtin.EntityEvent.EntityUpdatedEvent;
import de.schosin.ecs.storage.api.StorageEngine;
import de.schosin.ecs.storage.api.entities.Archetype;
import de.schosin.ecs.utils.collections.BitVector;
import de.schosin.ecs.utils.collections.ImmutableBag;
import de.schosin.ecs.utils.collections.ImmutableIntBag;

public class ChangeManager {

    private static final int MAX_PROCESS_REPITITIONS = 10;

    private final StorageEngine storageEngine;

    private final EventManager eventManager;
    private final EntityManager entityManager;

    private final EntityInsertedEvent insertedEvent = EntityInsertedEvent.get();
    private final EntitiesInsertedEvent entitiesInsertedEvent = EntitiesInsertedEvent.get();
    private final BeforeEntityUpdateEvent beforeEvent = BeforeEntityUpdateEvent.get();
    private final EntityUpdatedEvent afterEvent = EntityUpdatedEvent.get();
    private final EntityRemovedEvent removedEvent = EntityRemovedEvent.get();

    private BitVector deletedEntities;
    private BitVector deletedEntitiesOverflow;

    private BitVector updatedEntities;
    private BitVector updatedEntitiesOverflow;

    public ChangeManager(StorageEngine storageEngine, EventManager eventManager, EntityManager entityManager) {
        this.storageEngine = storageEngine;

        this.eventManager = eventManager;
        this.entityManager = entityManager;

        this.deletedEntities = new BitVector(64);
        this.deletedEntitiesOverflow = new BitVector(64);

        this.updatedEntities = new BitVector(64);
        this.updatedEntitiesOverflow = new BitVector(64);
    }

    public void inserted(int entityId, Archetype archetype) {
        // Dispatch event, return early if no handlers
        if (!eventManager.dispatchEvent(insertedEvent.with(entityId, archetype))) {
            return;
        }

        // Process composition updates
        processEntityCreation(entityId, archetype);
    }

    public void inserted(ImmutableIntBag entityIds, Archetype archetype) {
        // Dispatch event, skip if no handlers
        if (!eventManager.dispatchEvent(entitiesInsertedEvent.with(entityIds, archetype))) {
            return;
        }

        // Process composition updates
        for (int i = 0, s = entityIds.getSize(); i < s; i++) {
            var entityId = entityIds.get(i);

            processEntityCreation(entityId, archetype);
        }
    }

    private void processEntityCreation(int entityId, Archetype archetype) {
        var tries = MAX_PROCESS_REPITITIONS;
        while (!processEntityInCreation(entityId) && --tries > 0) {
            // just repeat until done
        }

        if (tries == 0) {
            System.err.println("Creating entity %d caused too many recursive updates while processing compositions. Initial archetype: %s".formatted(entityId, archetype));
        }
    }

    /**
     * @return processing finished
     */
    private boolean processEntityInCreation(int entityId) {
        // Throw if deleted during creation
        if (deletedEntities.get(entityId)) {
            throw new IllegalStateException("Entity %d deleted during creation.".formatted(entityId));
        }

        // Handle entity updates
        if (updatedEntities.get(entityId)) {
            // Process updated entity
            flushCompositionUpdate(entityId);
        }

        return !updatedEntities.get(entityId) && !deletedEntities.get(entityId);
    }

    public boolean flushEntityUpdates(int entityId, int loops) {
        while (updatedEntities.get(entityId) && --loops > 0) {
            flushCompositionUpdate(entityId);
        }

        if (loops == 0) {
            System.err.println("Flushing updates for entity %d caused too many recursive updates while processing compositions.".formatted(entityId));
        }

        return !updatedEntities.get(entityId);
    }

    private void flushCompositionUpdate(int entityId) {
        if (!updatedEntities.get(entityId)) {
            return;
        }

        // Cleanup
        updatedEntities.clear(entityId);

        // Process updated entity
        processUpdatedEntity(entityId, deletedEntities);
    }

    /**
     * @return processing finished
     */
    public boolean process(int loops) {
        do {
            if (processInternal()) {
                return true;
            }
        } while (--loops > 0);

        return false;
    }

    /**
     * @return processing finished
     */
    private synchronized boolean processInternal() {
        // Prepare overflow
        var deleted = this.deletedEntities;
        this.deletedEntities = this.deletedEntitiesOverflow;
        this.deletedEntitiesOverflow = deleted;

        var updated = this.updatedEntities;
        this.updatedEntities = this.updatedEntitiesOverflow;
        this.updatedEntitiesOverflow = updated;

        // Process changes
        deleted.iterate(this::processDeletedEntity);

        updated.iterate(entityId -> processUpdatedEntity(entityId, deleted));
        updated.clear();

        deleted.clear();

        return !isDirty();
    }

    private boolean isDirty() {
        return !this.deletedEntities.isEmpty() || !this.updatedEntities.isEmpty();
    }

    private void processDeletedEntity(int entityId) {
        // Get archetype, return early if null (entity not active)
        var archetype = entityManager.getArchetype(entityId);
        if (archetype == null) {
            return;
        }

        // Notify handlers
        eventManager.dispatchEvent(removedEvent.with(entityId, archetype));

        // Delete entity
        entityManager.deleteEntity(entityId);
    }

    private void processUpdatedEntity(int entityId, BitVector deleted) {
        if (deleted.get(entityId)) {
            return;
        }

        var pendingArchetype = storageEngine.getPendingArchetype(entityId);
        if (pendingArchetype == null) {
            return;
        }

        // Dispatch before event
        var previousArchetype = entityManager.getArchetype(entityId);
        eventManager.dispatchEvent(beforeEvent.with(entityId, previousArchetype, pendingArchetype));

        // Apply changes
        storageEngine.flushChanges(entityId);
        entityManager.updateArchetype(entityId, pendingArchetype);

        // Dispatch after event
        eventManager.dispatchEvent(afterEvent.with(entityId, previousArchetype, pendingArchetype));
    }

    public void deleteEntity(int entityId) {
        this.updatedEntities.clear(entityId);
        this.deletedEntities.set(entityId);
    }

    public boolean updateEntity(int entityId, ImmutableBag<RegularComponentType<?, ?>> addTypes, Object[] add, ImmutableBag<ComponentType<?, ?>> removeTypes) {
        // Skip deleted entities
        if (this.deletedEntities.get(entityId) || this.deletedEntitiesOverflow.get(entityId)) {
            return false;
        }

        // Modify components in storage
        var previousArchetype = entityManager.getArchetype(entityId);
        var archetype = storageEngine.modify(entityId, addTypes, add, removeTypes);

        if (archetype == previousArchetype) {
            return false;
        }

        // Track changed entity
        this.updatedEntities.set(entityId);

        return true;
    }

}
