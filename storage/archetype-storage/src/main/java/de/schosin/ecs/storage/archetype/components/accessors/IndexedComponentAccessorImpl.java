package de.schosin.ecs.storage.archetype.components.accessors;

import de.schosin.ecs.api.data.ArchetypeComponentAccessor;
import de.schosin.ecs.utils.collections.Bag;

public record IndexedComponentAccessorImpl<T>(Bag<T> components) implements ArchetypeComponentAccessor<T> {

    @Override
    public T getComponent(int index) {
        return components.get(index);
    }

}
