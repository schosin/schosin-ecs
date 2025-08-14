package de.schosin.ecs.storage.api.entities.observer;

import de.schosin.ecs.storage.api.entities.Archetype;

/**
 * Observer to be called after the entities have been moved to the new archetype. 
 * Can be used to access components of the new archetype.
 * 
 * <p>
 * Observers will be called after {@link EntitiesDeletedObserver} and {@link EntitiesCreatedObserver}.
 * 
 * <p>
 * To receive events for all entity updates, {@link EntitiesUpdatedObserver} must also be implemented
 * to receive callbacks for batched updates of entities. 
 */
@FunctionalInterface
public interface EntityUpdatedObserver {

    void handleEntityUpdated(Archetype archetype, Archetype previousArchetype, int entityId);

}
