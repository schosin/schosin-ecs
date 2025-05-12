package de.schosin.ecs.engine;

import org.jspecify.annotations.NonNull;

import de.schosin.ecs.engine.components.Component;
import de.schosin.ecs.engine.components.ComponentManager;
import de.schosin.ecs.engine.components.ComponentMask;
import de.schosin.ecs.engine.components.ComponentMaskManager;
import de.schosin.ecs.engine.entities.EntityManager;
import de.schosin.ecs.engine.utils.collections.Bag;
import de.schosin.ecs.engine.utils.collections.BitVector;
import de.schosin.ecs.engine.utils.collections.IntBag;

public class ChangeManager {

    public interface EntityInsertedHandler {
        default void handleInserted(int[] entityIds, ComponentMask componentMask) {
            for (var entityId : entityIds) {
                handleInserted(entityId, componentMask);
            }
        }

        void handleInserted(int entityId, ComponentMask componentMask);
    }

    public interface EntityUpdatedHandler {
        void handleUpdated(int entityId, ComponentMask previousComponentMask, ComponentMask componentMask);
    }

    public interface EntityRemovedHandler {
        void handleRemoved(int entityId, ComponentMask componentMask);
    }

    private static final int MAX_PROCESS_REPITITIONS = 10;

    private final ComponentManager componentManager;
    private final ComponentMaskManager componentMaskManager;
    private final EntityManager entityManager;

    private Bag<EntityInsertedHandler> entityInsertedHandlers;
    private Bag<EntityUpdatedHandler> entityUpdatedHandlers;
    private Bag<EntityRemovedHandler> entityRemovedHandlers;

    private BitVector deletedEntities;
    private BitVector deletedEntitiesOverflow;

    private BitVector updatedEntities;
    private BitVector updatedEntitiesOverflow;

    private IntBag updatedEntityMasks;
    private IntBag updatedEntityMasksOverflow;

    private BitVector removedComponents;
    private BitVector removedComponentsOverflow;

    public ChangeManager(BagManager bagManager, ComponentManager componentManager, ComponentMaskManager componentMaskManager, EntityManager entityManager) {
        this.componentManager = componentManager;
        this.componentMaskManager = componentMaskManager;
        this.entityManager = entityManager;

        this.deletedEntities = new BitVector(64);
        this.deletedEntitiesOverflow = new BitVector(64);

        this.updatedEntities = new BitVector(64);
        this.updatedEntitiesOverflow = new BitVector(64);

        this.updatedEntityMasks = bagManager.createEntityIntBag();
        this.updatedEntityMasksOverflow = bagManager.createEntityIntBag();

        this.removedComponents = new BitVector(64);
        this.removedComponentsOverflow = new BitVector(64);
    }

    public void registerInserted(EntityInsertedHandler handler) {
        if (entityInsertedHandlers == null) {
            this.entityInsertedHandlers = new Bag<>(EntityInsertedHandler.class, 4);
        }

        this.entityInsertedHandlers.add(handler);
    }

    public void registerUpdated(EntityUpdatedHandler handler) {
        if (entityUpdatedHandlers == null) {
            this.entityUpdatedHandlers = new Bag<>(EntityUpdatedHandler.class, 4);
        }

        this.entityUpdatedHandlers.add(handler);
    }

    public void registerRemoved(EntityRemovedHandler handler) {
        if (entityRemovedHandlers == null) {
            this.entityRemovedHandlers = new Bag<>(EntityRemovedHandler.class, 4);
        }

        this.entityRemovedHandlers.add(handler);
    }

    public void inserted(int entityId, ComponentMask componentMask) {
        if (entityInsertedHandlers == null) {
            return;
        }

        // Notify handlers
        var data = entityInsertedHandlers.getData();
        for (int i = 0, s = entityInsertedHandlers.getSize(); i < s; i++) {
            data[i].handleInserted(entityId, componentMask);
        }

        // Process composition updates
        processEntityCreation(entityId, componentMask);
    }

    public void inserted(int[] entityIds, ComponentMask componentMask) {
        if (entityInsertedHandlers == null) {
            return;
        }

        // Notify handlers
        var data = entityInsertedHandlers.getData();

        for (int i = 0, s = entityInsertedHandlers.getSize(); i < s; i++) {
            data[i].handleInserted(entityIds, componentMask);
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

            // Apply removal if component not in new component mask
            component.applyRemoval(entityId);
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

        removed.iterate(this::processRemovedComponent);
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
        if (entityRemovedHandlers != null) {
            var data = entityRemovedHandlers.getData();
            for (int i = 0, s = entityRemovedHandlers.getSize(); i < s; i++) {
                data[i].handleRemoved(entityId, componentMask);
            }
        }

        // Delete entity
        entityManager.deleteEntity(entityId);
    }

    private void processRemovedComponent(int componentId) {
        var metadata = componentManager.getComponent(componentId);
        metadata.applyRemovals();
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
            // Notify handlers
            if (entityUpdatedHandlers != null) {
                var data = entityUpdatedHandlers.getData();
                for (int i = 0, s = entityUpdatedHandlers.getSize(); i < s; i++) {
                    data[i].handleUpdated(entityId, previousComponentMask, componentMask);
                }
            }
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
        component.unmarkRemoved(entityId);

        if (changed) {
            this.updatedEntities.set(entityId);
        }

        return changed;
    }

    public boolean removeComponent(int entityId, Component<?> component) {
        if (!component.hasComponent(entityId)) {
            return false;
        }

        component.markRemoved(entityId);

        this.updatedEntities.set(entityId);
        this.removedComponents.set(component.id());

        return true;
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
