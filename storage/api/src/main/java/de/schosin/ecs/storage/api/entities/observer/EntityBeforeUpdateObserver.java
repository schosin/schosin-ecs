package de.schosin.ecs.storage.api.entities.observer;

import de.schosin.ecs.storage.api.entities.Archetype;

/**
 * Observer to be called before the entity is moved to the new archetype.
 * Can be used to access components of the old archetype.
 * 
 * <p>
 * Observers will be called after {@link EntitiesDeletedObserver} and before {@link EntitiesCreatedObserver}.
 * 
 * <p>
 * To receive events for all entity updates, {@link EntitiesBeforeUpdateObserver} must also be implemented
 * to receive callbacks for batched updates of entities. 
 */
@FunctionalInterface
public interface EntityBeforeUpdateObserver {

    void handleEntityBeforeUpdate(Archetype archetype, Archetype newArchetype, int entityId);

}
