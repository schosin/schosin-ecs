package de.schosin.ecs.storage.api.entities.observer;

import de.schosin.ecs.storage.api.entities.Archetype;

/**
 * Observer for when a single entity is created using {@link Archetype#createEntity(int, Object[])}.
 * 
 * <p>
 * To receive events for all entity creations, {@link EntitiesCreatedObserver} must also be implemented
 * to receive callbacks for batch creation of entities. 
 */
@FunctionalInterface
public interface EntityCreatedObserver {

    void handleEntityCreated(Archetype archetype, int entityId);

}
