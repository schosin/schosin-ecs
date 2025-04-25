package de.schosin.ecs.engine;

import org.jspecify.annotations.NonNull;

import de.schosin.ecs.engine.components.ComponentData;
import de.schosin.ecs.engine.components.ComponentManager;
import de.schosin.ecs.engine.compositions.CompositionManager;
import de.schosin.ecs.engine.entities.EntityManager;
import de.schosin.ecs.engine.utils.collections.BitVector;

public class ChangeManager {

    private final ComponentManager componentManager;
    private final CompositionManager compositionManager;
    private final EntityManager entityManager;

    private BitVector deletedEntities;
    private BitVector deletedEntitiesOverflow;

    private BitVector updatedEntities;
    private BitVector updatedEntitiesOverflow;

    private BitVector removedComponents;
    private BitVector removedComponentsOverflow;

    public ChangeManager(ComponentManager componentManager, CompositionManager compositionManager, EntityManager entityManager) {
        this.componentManager = componentManager;
        this.compositionManager = compositionManager;
        this.entityManager = entityManager;

        this.deletedEntities = new BitVector(64);
        this.deletedEntitiesOverflow = new BitVector(64);

        this.updatedEntities = new BitVector(64);
        this.updatedEntitiesOverflow = new BitVector(64);

        this.removedComponents = new BitVector(64);
        this.removedComponentsOverflow = new BitVector(64);
    }

    public boolean process(int loops) {
        do {
            if (!processInternal()) {
                return false;
            }
        } while (--loops > 0);

        return true;
    }

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

        // Process changes
        deleted.iterate(this::processDeletedEntity);
        deleted.clear();

        updated.iterate(this::processUpdatedEntities);
        updated.clear();

        removed.iterate(this::processRemovedComponent);
        removed.clear();

        return isDirty();
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

    private void processUpdatedEntities(int entityId) {
        // Get new mask, return early if null (entity removed)
        var componentMask = entityManager.getComponentMask(entityId);
        if (componentMask == null) {
            return;
        }

        // Update entity
        var previousComponentMask = entityManager.getPreviousComponentMask(entityId);
        compositionManager.updated(entityId, previousComponentMask, componentMask);
    }

    public void deleteEntity(int entityId) {
        this.deletedEntities.set(entityId);
    }

    public void updateEntity(int entityId) {
        this.updatedEntities.set(entityId);
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

}
