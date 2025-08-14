package de.schosin.ecs.storage.api.entities.observer;

import java.util.function.IntSupplier;
import java.util.function.ObjIntConsumer;

import de.schosin.ecs.storage.api.entities.Archetype;
import de.schosin.ecs.utils.collections.ImmutableIntBag;

/**
 * Observer for when a batch of entity is created using {@link Archetype#createEntities(int, IntSupplier, ObjIntConsumer)}.
 * 
 * <p>
 * To receive events for all entity creations, {@link EntityCreatedObserver} must also be implemented
 * to receive callbacks for creation of a single entity. 
 */
@FunctionalInterface
public interface EntitiesCreatedObserver {

    void handleEntitiesCreated(Archetype archetype, ImmutableIntBag entities);

}
