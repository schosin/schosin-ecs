package de.schosin.ecs.storage.archetype.components.accessors;

import de.schosin.ecs.api.data.ArchetypeComponentAccessor;

public record EnumComponentAccessorImpl<T extends Enum<T>>(T value) implements ArchetypeComponentAccessor<T> {

    @Override
    public T getComponent(int index) {
        return value;
    }

}
