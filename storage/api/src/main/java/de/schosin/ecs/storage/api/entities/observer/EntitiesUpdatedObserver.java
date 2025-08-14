package de.schosin.ecs.storage.api.entities.observer;

import de.schosin.ecs.storage.api.entities.Archetype;
import de.schosin.ecs.utils.collections.ImmutableIntBag;

/**
 * Observer to be called after the entities have been moved to the new archetype. 
 * Can be used to access components of the new archetype.
 * 
 * <p>
 * Observers will be called after {@link EntitiesDeletedObserver} and {@link EntitiesCreatedObserver}.
 * 
 * <p>
 * To receive events for all entity updates, {@link EntityUpdatedObserver} must also be implemented
 * to receive callbacks for updates of a single entity.
 */
@FunctionalInterface
public interface EntitiesUpdatedObserver {

    void handleEntitiesUpdated(Archetype archetype, Archetype previousArchetype, ImmutableIntBag entities);

}
