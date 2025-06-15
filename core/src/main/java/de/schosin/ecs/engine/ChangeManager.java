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
import de.schosin.ecs.storage.api.entities.ComponentMask;
import de.schosin.ecs.utils.collections.BitVector;
import de.schosin.ecs.utils.collections.ImmutableBag;
import de.schosin.ecs.utils.collections.ImmutableIntBag;

public class ChangeManager {

    private static final int MAX_PROCESS_REPITITIONS = 10;

    private final StorageEngine storageEngine;

    private final EventManager eventManager;
    private final EntityManager entityManager;

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

    public void inserted(int entityId, ComponentMask componentMask) {
        // Dispatch event, return early if no handlers
        if (!eventManager.dispatchEvent(EntityInsertedEvent.get(entityId, componentMask))) {
            return;
        }

        // Process composition updates
        processEntityCreation(entityId, componentMask);
    }

    public void inserted(ImmutableIntBag entityIds, ComponentMask componentMask) {
        // Dispatch event, skip if no handlers
        if (!eventManager.dispatchEvent(EntitiesInsertedEvent.get(entityIds, componentMask))) {
            return;
        }

        // Process composition updates
        for (int i = 0, s = entityIds.getSize(); i < s; i++) {
            var entityId = entityIds.get(i);

            processEntityCreation(entityId, componentMask);
        }
    }

    private void processEntityCreation(int entityId, ComponentMask componentMask) {
        var tries = MAX_PROCESS_REPITITIONS;
        while (!processEntityInCreation(entityId) && --tries > 0) {
            // just repeat until done
        }

        if (tries == 0) {
            System.err.println("Creating entity %d caused too many recursive updates while processing compositions. Initial component mask: %s".formatted(entityId, componentMask));
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
        processUpdatedEntity(entityId);
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

        updated.iterate(this::processUpdatedEntity);
        updated.clear();

        deleted.clear();

        return !isDirty();
    }

    private boolean isDirty() {
        return !this.deletedEntities.isEmpty() || !this.updatedEntities.isEmpty();
    }

    private void processDeletedEntity(int entityId) {
        // Get component mask, return early if null (entity not active)
        var componentMask = entityManager.getComponentMask(entityId);
        if (componentMask == null) {
            return;
        }

        // Notify handlers
        eventManager.dispatchEvent(EntityRemovedEvent.get(entityId, componentMask));

        // Delete entity
        entityManager.deleteEntity(entityId);
    }

    private void processUpdatedEntity(int entityId) {
        var pendingComponentMask = storageEngine.getPendingComponentMask(entityId);
        if (pendingComponentMask == null) {
            return;
        }

        // Dispatch before event
        var previousComponentMask = entityManager.getComponentMask(entityId);
        eventManager.dispatchEvent(BeforeEntityUpdateEvent.get(entityId, previousComponentMask, pendingComponentMask));

        // Apply changes
        storageEngine.flushChanges(entityId);
        entityManager.updateComponentMask(entityId, pendingComponentMask);

        // Dispatch after event
        eventManager.dispatchEvent(EntityUpdatedEvent.get(entityId, previousComponentMask, pendingComponentMask));
    }

    public void deleteEntity(int entityId) {
        this.updatedEntities.clear(entityId);
        this.deletedEntities.set(entityId);
    }

    public boolean updateEntity(int entityId, ImmutableBag<RegularComponentType<?, ?>> addTypes, Object[] add, ImmutableBag<ComponentType<?, ?>> removeTypes) {
        // Skip deleted entities
        if (this.deletedEntities.get(entityId)) {
            return false;
        }

        // Modify components in storage
        var previousComponentMask = entityManager.getComponentMask(entityId);
        var componentMask = storageEngine.modify(entityId, addTypes, add, removeTypes);

        // Track changed entity
        var changed = previousComponentMask.getId() != componentMask.getId();
        if (changed) {
            this.updatedEntities.set(entityId);
        }

        return changed;
    }

}
