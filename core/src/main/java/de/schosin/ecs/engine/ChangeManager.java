package de.schosin.ecs.engine;

import de.schosin.ecs.api.components.types.ComponentType;
import de.schosin.ecs.api.components.types.ComponentType.RegularComponentType;
import de.schosin.ecs.engine.components.ComponentManager;
import de.schosin.ecs.engine.entities.EntityManager;
import de.schosin.ecs.engine.events.EventManager;
import de.schosin.ecs.engine.events.builtin.EntitiesEvent.EntitiesInsertedEvent;
import de.schosin.ecs.engine.events.builtin.EntityEvent.EntityInsertedEvent;
import de.schosin.ecs.engine.events.builtin.EntityEvent.EntityRemovedEvent;
import de.schosin.ecs.engine.events.builtin.EntityEvent.EntityUpdatedEvent;
import de.schosin.ecs.storage.api.StorageEngine;
import de.schosin.ecs.storage.api.components.Component;
import de.schosin.ecs.storage.api.entities.ComponentMask;
import de.schosin.ecs.utils.collections.Bag;
import de.schosin.ecs.utils.collections.BitVector;
import de.schosin.ecs.utils.collections.ImmutableBag;
import de.schosin.ecs.utils.collections.ImmutableIntBag;
import de.schosin.ecs.utils.collections.IntBag;
import de.schosin.ecs.utils.collections.Pool;

public class ChangeManager {

    private static final int MAX_PROCESS_REPITITIONS = 10;

    private final StorageEngine storageEngine;

    private final EventManager eventManager;
    private final ComponentManager componentManager;
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

    private final Pool<Bag<RegularComponentType<?, ?>>> componentTypesPool = Pool.unbounded(Bag.class, () -> new Bag<>(RegularComponentType.class, 8), Bag::clear);

    public ChangeManager(StorageEngine storageEngine, EventManager eventManager, BagManager bagManager, ComponentManager componentManager, EntityManager entityManager) {
        this.storageEngine = storageEngine;

        this.eventManager = eventManager;
        this.componentManager = componentManager;
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
            var previousComponentMask = entityManager.getComponentMask(entityId);

            var componentMaskId = fromLookup(updatedEntityMasks.get(entityId));
            var componentMask = storageEngine.getComponentMaskById(componentMaskId);

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
            var componentMask = storageEngine.getComponentMaskById(componentMaskId);

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
        componentTypesPool.withInstanceNoResult(componentTypes -> {
            // Gather removed component types
            componentTypes.addAll(previousComponentMask.getComponentTypes());
            componentTypes.removeAll(componentMask.getComponentTypes());

            // Remove components
            if (!componentTypes.isEmpty()) {
                storageEngine.remove(entityId, componentTypes);
            }
        });
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

        updated.iterate(entityId -> processUpdatedEntity(entityId, fromLookup(masks.get(entityId))));
        updated.clear();
        masks.clear();

        var removedData = removed.getData();
        for (int i = 0, s = removed.getSize(); i < s; i++) {
            var entities = removedData[i];

            var componentId = this.removedComponentsBags.indexOfIdentity(entities);
            processRemovedComponent(componentId, entities, deleted);

            entities.clear();
        }

        removed.clear();
        deleted.clear();

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

    private void processRemovedComponent(int componentId, IntBag entities, BitVector deleted) {
        var metadata = componentManager.getComponent(componentId);

        var componentTypes = componentTypesPool.getInstance();
        componentTypes.add(metadata.type());

        var data = entities.getData();
        for (int i = 0, s = entities.getSize(); i < s; i++) {
            var entityId = data[i];

            // Skip if deleted this process
            if (deleted.get(entityId)) {
                continue;
            }

            // If this throws "not in storage", user code is altering deleted entities in composition callbacks
            storageEngine.remove(entityId, componentTypes);
        }

        componentTypesPool.free(componentTypes);
    }

    private void processUpdatedEntity(int entityId, int componentMaskId) {
        // Get current mask, return early if null (entity removed)
        var previousComponentMask = entityManager.getComponentMask(entityId);
        if (previousComponentMask == null) {
            return;
        }

        // Update entity
        var componentMask = storageEngine.getComponentMaskById(componentMaskId);
        if (entityManager.updateComponentMask(entityId, componentMask)) {
            eventManager.dispatchEvent(EntityUpdatedEvent.get(entityId, previousComponentMask, componentMask));
        }
    }

    public void deleteEntity(int entityId) {
        this.deletedEntities.set(entityId);
    }

    public boolean updateEntity(int entityId, ImmutableBag<RegularComponentType<?, ?>> addTypes, Object[] add, ImmutableBag<ComponentType<?, ?>> removeTypes) {
        // Retrieve current component mask
        var previousComponentMask = entityManager.getComponentMask(entityId);

        // Retrieve pending component mask change if present
        var pendingComponentMaskId = getPendingComponentMask(entityId);
        if (pendingComponentMaskId > -1) {
            previousComponentMask = storageEngine.getComponentMaskById(pendingComponentMaskId);
        }

        // Add components to storage
        storageEngine.add(entityId, addTypes, add);

        // Track new component mask separately due to delayed removal
        var newComponentMask = storageEngine.addToComponentMask(previousComponentMask, addTypes);

        if (!addTypes.isEmpty()) {
            for (int i = 0, s = addTypes.getSize(); i < s; i++) {
                var component = componentManager.getComponent(addTypes.get(i));
                unmarkRemoved(entityId, component);
            }
        }

        if (!removeTypes.isEmpty()) {
            var removeComponentMask = storageEngine.removeFromComponentMask(newComponentMask, removeTypes);

            // Remove previous componets no longer in component mask
            var previousComponents = previousComponentMask.getComponents();
            for (int i = 0, s = previousComponents.getSize(); i < s; i++) {
                var component = previousComponents.get(i);

                if (!removeComponentMask.contains(component.id())) {
                    markRemoved(entityId, component);
                }
            }

            // Remove added components no longer in component mask (e.g. add and remove in same operation)
            // hard to detect properly because wildcards can match components discovered at a later time
            var newComponents = newComponentMask.getComponents();
            for (int i = 0, s = newComponents.getSize(); i < s; i++) {
                var component = newComponents.get(i);

                if (!removeComponentMask.contains(component.id())) {
                    markRemoved(entityId, component);
                }
            }

            newComponentMask = removeComponentMask;
        }

        if (previousComponentMask.getId() != newComponentMask.getId()) {
            this.updatedEntities.set(entityId);
            this.updatedEntityMasks.set(entityId, fromLookup(newComponentMask.getId()));

            return true;
        }

        return false;
    }

    private void markRemoved(int entityId, Component<?, ?> component) {
        if (!component.hasComponent(entityId)) {
            return;
        }

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

    private void unmarkRemoved(int entityId, Component<?, ?> component) {
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
