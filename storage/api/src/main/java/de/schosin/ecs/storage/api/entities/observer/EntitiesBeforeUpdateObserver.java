package de.schosin.ecs.storage.api.entities.observer;

import de.schosin.ecs.storage.api.entities.Archetype;
import de.schosin.ecs.utils.collections.ImmutableIntBag;

/**
 * Observer to be called before the entities are moved to the new archetype. 
 * Can be used to access components of the old archetype.
 * 
 * <p>
 * Observers will be called after {@link EntitiesDeletedObserver} and before {@link EntitiesCreatedObserver}.
 * 
 * <p>
 * To receive events for all entity updates, {@link EntityBeforeUpdateObserver} must also be implemented
 * to receive callbacks for updates of a single entity.
 */
@FunctionalInterface
public interface EntitiesBeforeUpdateObserver {

    void handleEntitiesBeforeUpdate(Archetype archetype, Archetype newArchetype, ImmutableIntBag entities);

}
