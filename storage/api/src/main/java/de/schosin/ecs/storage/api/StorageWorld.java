package de.schosin.ecs.storage.api;

import de.schosin.ecs.api.World;
import de.schosin.ecs.storage.api.events.StorageEvent;
import de.schosin.ecs.utils.collections.Bag;
import de.schosin.ecs.utils.collections.IntBag;

public interface StorageWorld extends World {

    /**
     * Returns a new bag that is synchronized with the number of
     * entities in this word. Allows working on indexes without
     * size checks.
     * 
     * @param <T> type of element
     * @param clazz class of element
     * @return synchronized bag
     */
    <T> Bag<T> createEntityBag(Class<? super T> clazz);

    /**
     * Returns a new int bag that is synchronized with the number of
     * entities in this word. Allows working on indexes without
     * size checks.
     * 
     * @return synchronized int bag
     */
    IntBag createEntityIntBag();

    void dispatchEvent(StorageEvent event);

}
