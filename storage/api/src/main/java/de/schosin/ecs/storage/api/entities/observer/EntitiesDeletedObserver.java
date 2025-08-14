package de.schosin.ecs.storage.api.entities.observer;

import de.schosin.ecs.api.World;
import de.schosin.ecs.storage.api.entities.Archetype;
import de.schosin.ecs.utils.collections.ImmutableIntBag;

/**
 * Observer for when entities are deleted. 
 * 
 * <p>
 * Observers will be called during {@link World#process()} before {@link EntitiesBeforeUpdateObserver}
 * and {@link EntitiesUpdatedObserver} are called.
 */
@FunctionalInterface
public interface EntitiesDeletedObserver {

    void handleEntitiesDeleted(Archetype archetype, ImmutableIntBag entities);

}
