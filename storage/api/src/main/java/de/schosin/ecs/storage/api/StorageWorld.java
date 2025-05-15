package de.schosin.ecs.storage.api;

import de.schosin.ecs.api.World;
import de.schosin.ecs.api.components.ComponentType.RegularComponentType;
import de.schosin.ecs.storage.api.components.Component;
import de.schosin.ecs.utils.collections.Bag;

public interface StorageWorld extends World {

    /**
     * Returns a new bag that is synchronized with the number of
     * entities in this word. Allows working on indexes without
     * size checks.
     * 
     * @param <T> type of lement
     * @param clazz class of element
     * @return synchronized bag
     */
    <T> Bag<T> createEntityBag(Class<? super T> clazz);

    <T> void dispatchComponentAddedEvent(RegularComponentType<T> type, Component<T> component);

}
