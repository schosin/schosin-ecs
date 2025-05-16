package de.schosin.ecs.engine;

import org.jspecify.annotations.NonNull;

import de.schosin.ecs.engine.components.ComponentManager;
import de.schosin.ecs.engine.components.ComponentMask;
import de.schosin.ecs.engine.components.ComponentMaskManager;
import de.schosin.ecs.engine.entities.EntityManager;
import de.schosin.ecs.engine.events.EventManager;
import de.schosin.ecs.engine.events.builtin.EntitiesEvent.EntitiesInsertedEvent;
import de.schosin.ecs.engine.events.builtin.EntityEvent.EntityInsertedEvent;
import de.schosin.ecs.engine.events.builtin.EntityEvent.EntityRemovedEvent;
import de.schosin.ecs.engine.events.builtin.EntityEvent.EntityUpdatedEvent;
import de.schosin.ecs.storage.api.components.Component;
import de.schosin.ecs.utils.collections.Bag;
import de.schosin.ecs.utils.collections.BitVector;
import de.schosin.ecs.utils.collections.IntBag;

public class ChangeManager {

    private static final int MAX_PROCESS_REPITITIONS = 10;

    private final EventManager eventManager;
    private final ComponentManager componentManager;
    private final ComponentMaskManager componentMaskManager;
    private final EntityManager entityManager;

    private final Bag<IntBag> removedComponentsBags;

    private BitVector deletedEntities;
    private BitVector deletedEntitiesOverflow;

    private BitVector updatedEntities;
    private BitVector updatedEntitiesOverflow;

    private IntBag updatedEntityMasks;
    private IntBag updatedEntityMasksOverflow;

    private Bag<IntBag> removedComponents;
    private Bag<IntBag> removedComponentsOverflow;

    public ChangeManager(EventManager eventManager, BagManager bagManager, ComponentManager componentManager, ComponentMaskManager componentMaskManager, EntityManager entityManager) {
        this.eventManager = eventManager;
        this.componentManager = componentManager;
        this.componentMaskManager = componentMaskManager;
        this.entityManager = entityManager;

        this.removedComponentsBags = bagManager.createComponentBag(IntBag.class);

        this.deletedEntities = new BitVector(64);
        this.deletedEntitiesOverflow = new BitVector(64);

        this.updatedEntities = new BitVector(64);
        this.updatedEntitiesOverflow = new BitVector(64);

        this.updatedEntityMasks = bagManager.createEntityIntBag();
        this.updatedEntityMasksOverflow = bagManager.createEntityIntBag();

        this.removedComponents = new Bag<>(IntBag.class);
        this.removedComponentsOverflow = new Bag<>(IntBag.class);
    }

    public void inserted(int entityId, ComponentMask componentMask) {
        // Dispatch event, return early if no handlers
        if (!eventManager.dispatchEvent(EntityInsertedEvent.get(entityId, componentMask))) {
            return;
        }

        // Process composition updates
        processEntityCreation(entityId, componentMask);
    }

    public void inserted(int[] entityIds, ComponentMask componentMask) {
        // Dispatch event, skip if no handlers
        if (!eventManager.dispatchEvent(EntitiesInsertedEvent.get(entityIds, componentMask))) {
            return;
        }

        // Process composition updates
        for (var entityId : entityIds) {
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
            var previousComponentMask = entityManager.getComponentMask(entityId);

            var componentMaskId = fromLookup(updatedEntityMasks.get(entityId));
            var componentMask = componentMaskManager.getComponentMask(componentMaskId);

            // Process updated entity
            flushCompositionUpdate(entityId, componentMask);

            // Flush component removals
            flushComponentRemovals(entityId, previousComponentMask, componentMask);
        }

        return !updatedEntities.get(entityId) && !deletedEntities.get(entityId);
    }

    public boolean flushEntityUpdates(int entityId, int loops) {
        while (updatedEntities.get(entityId) && --loops > 0) {
            var componentMaskId = fromLookup(updatedEntityMasks.get(entityId));
            var componentMask = componentMaskManager.getComponentMask(componentMaskId);

            flushCompositionUpdate(entityId, componentMask);
        }

        if (loops == 0) {
            System.err.println("Flushing updates for entity %d caused too many recursive updates while processing compositions.".formatted(entityId));
        }

        return !updatedEntities.get(entityId);
    }

    private void flushCompositionUpdate(int entityId, ComponentMask componentMask) {
        if (!updatedEntities.get(entityId)) {
            return;
        }

        // Cleanup
        updatedEntities.clear(entityId);
        updatedEntityMasks.set(entityId, 0);

        // Process updated entity
        processUpdatedEntity(entityId, componentMask.getId());
    }

    private void flushComponentRemovals(int entityId, ComponentMask previousComponentMask, ComponentMask componentMask) {
        outer: for (var component : previousComponentMask.getComponents()) {
            // Skip if new component mask still contains previous component
            for (var present : componentMask.getComponents()) {
                if (component == present) {
                    continue outer;
                }
            }

            // Remove component if component not in new component mask
            component.removeComponent(entityId);
        }
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

        var removed = this.removedComponents;
        this.removedComponents = this.removedComponentsOverflow;
        this.removedComponentsOverflow = removed;

        var updated = this.updatedEntities;
        this.updatedEntities = this.updatedEntitiesOverflow;
        this.updatedEntitiesOverflow = updated;

        var masks = this.updatedEntityMasks;
        this.updatedEntityMasks = this.updatedEntityMasksOverflow;
        this.updatedEntityMasksOverflow = masks;

        // Process changes
        deleted.iterate(this::processDeletedEntity);
        deleted.clear();

        updated.iterate(entityId -> processUpdatedEntity(entityId, fromLookup(masks.get(entityId))));
        updated.clear();
        masks.clear();

        var removedData = removed.getData();
        for (int i = 0, s = removed.getSize(); i < s; i++) {
            var entities = removedData[i];

            var componentId = this.removedComponentsBags.indexOfIdentity(entities);
            processRemovedComponent(componentId, removedData[i]);

            entities.clear();
        }
        removed.clear();

        return !isDirty();
    }

    private boolean isDirty() {
        return !this.deletedEntities.isEmpty() || !this.removedComponents.isEmpty() || !this.updatedEntities.isEmpty();
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

    private void processRemovedComponent(int componentId, IntBag entities) {
        var metadata = componentManager.getComponent(componentId);

        var data = entities.getData();
        for (int i = 0, s = entities.getSize(); i < s; i++) {
            metadata.removeComponent(data[i]);
        }
    }

    private void processUpdatedEntity(int entityId, int componentMaskId) {
        // Get current mask, return early if null (entity removed)
        var previousComponentMask = entityManager.getComponentMask(entityId);
        if (previousComponentMask == null) {
            return;
        }

        // Update entity
        var componentMask = componentMaskManager.getComponentMask(componentMaskId);
        if (entityManager.updateComponentMask(entityId, componentMask)) {
            eventManager.dispatchEvent(EntityUpdatedEvent.get(entityId, previousComponentMask, componentMask));
        }
    }

    public void deleteEntity(int entityId) {
        this.deletedEntities.set(entityId);
    }

    public void updateEntity(int entityId, ComponentMask componentMask) {
        this.updatedEntities.set(entityId);
        this.updatedEntityMasks.set(entityId, fromLookup(componentMask.getId()));
    }

    public <T> boolean addComponent(int entityId, Component<T> component, @NonNull T instance) {
        var changed = !component.hasComponent(entityId);

        component.addComponentUnsafe(entityId, instance);
        unmarkRemoved(entityId, component);

        if (changed) {
            this.updatedEntities.set(entityId);
        }

        return changed;
    }

    public boolean removeComponent(int entityId, Component<?> component) {
        if (!component.hasComponent(entityId)) {
            return false;
        }

        this.updatedEntities.set(entityId);
        markRemoved(entityId, component);

        return true;
    }

    private void markRemoved(int entityId, Component<?> component) {
        var removed = this.removedComponentsBags.get(component.id());
        if (removed == null) {
            synchronized (this.removedComponentsBags) {
                removed = this.removedComponentsBags.get(component.id());
                if (removed == null) {
                    removed = new IntBag(64);

                    this.removedComponentsBags.set(component.id(), removed);
                }
            }
        }

        if (!removed.contains(entityId)) {
            removed.add(entityId);
        }

        if (!this.removedComponents.containsIdentity(removed)) {
            this.removedComponents.add(removed);
        }
    }

    private void unmarkRemoved(int entityId, Component<?> component) {
        var removed = this.removedComponentsBags.get(component.id());
        if (removed == null || removed.isEmpty()) {
            return;
        }

        removed.removeValue(entityId);
    }

    /**
     * @return -1 if not set
     */
    public int getPendingComponentMask(int entityId) {
        return toLookup(this.updatedEntityMasks.get(entityId));
    }

    private int toLookup(int index) {
        return index == 0 ? -1 : index;
    }

    private int fromLookup(int index) {
        return index == -1 ? 0 : index;
    }

}
