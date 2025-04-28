package de.schosin.ecs.engine;

import org.jspecify.annotations.NonNull;

import de.schosin.ecs.engine.components.ComponentData;
import de.schosin.ecs.engine.components.ComponentManager;
import de.schosin.ecs.engine.components.ComponentMask;
import de.schosin.ecs.engine.components.ComponentMaskManager;
import de.schosin.ecs.engine.compositions.CompositionManager;
import de.schosin.ecs.engine.entities.EntityManager;
import de.schosin.ecs.engine.utils.collections.BitVector;
import de.schosin.ecs.engine.utils.collections.IntBag;

public class ChangeManager {

    private final ComponentManager componentManager;
    private final ComponentMaskManager componentMaskManager;
    private final CompositionManager compositionManager;
    private final EntityManager entityManager;

    private BitVector deletedEntities;
    private BitVector deletedEntitiesOverflow;

    private BitVector updatedEntities;
    private BitVector updatedEntitiesOverflow;

    private IntBag updatedEntityMasks;
    private IntBag updatedEntityMasksOverflow;

    private BitVector removedComponents;
    private BitVector removedComponentsOverflow;

    public ChangeManager(BagManager bagManager, ComponentManager componentManager, ComponentMaskManager componentMaskManager, CompositionManager compositionManager, EntityManager entityManager) {
        this.componentManager = componentManager;
        this.componentMaskManager = componentMaskManager;
        this.compositionManager = compositionManager;
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

    /**
     * @return processing finished
     */
    public boolean processEntityInCreation(int entityId) {
        // Throw if deleted during creation
        if (deletedEntities.get(entityId)) {
            throw new IllegalStateException("Entity %d deleted during creation.".formatted(entityId));
        }

        // Handle entity updates
        if (updatedEntities.get(entityId)) {
            var previousComponentMask = entityManager.getComponentMask(entityId);

            var componentMaskId = fromLookup(updatedEntityMasks.get(entityId));
            var componentMask = componentMaskManager.getComponentMask(componentMaskId);

            // Cleanup
            updatedEntities.clear(entityId);
            updatedEntityMasks.set(entityId, 0);

            // Process updated entity
            processUpdatedEntity(entityId, componentMaskId);

            // Flush component removals
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

        return !updatedEntities.get(entityId) && !deletedEntities.get(entityId);
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
        entityManager.deleteEntity(entityId);
    }

    private void processRemovedComponent(int componentId) {
        var metadata = componentManager.getData(componentId);
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
            // Notify compositions
            compositionManager.updated(entityId, previousComponentMask, componentMask);
        }
    }

    public void deleteEntity(int entityId) {
        this.deletedEntities.set(entityId);
    }

    public void updateEntity(int entityId, ComponentMask componentMask) {
        this.updatedEntities.set(entityId);
        this.updatedEntityMasks.set(entityId, fromLookup(componentMask.getId()));
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public boolean addComponent(int entityId, ComponentData component, @NonNull Object instance) {
        if (component.hasComponent(entityId)) {
            return false;
        }

        component.addComponent(entityId, instance);
        component.unmarkRemoved(entityId);

        this.updatedEntities.set(entityId);

        return true;
    }

    public boolean removeComponent(int entityId, ComponentData<?> component) {
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
