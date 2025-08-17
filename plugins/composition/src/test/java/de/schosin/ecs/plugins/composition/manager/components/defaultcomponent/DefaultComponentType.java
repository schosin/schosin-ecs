package de.schosin.ecs.plugins.composition.manager.components.defaultcomponent;

import java.util.function.Supplier;

import de.schosin.ecs.api.components.types.CustomComponentType;

public record DefaultComponentType<T>(RegularComponentType<T, T> type, Supplier<T> defaultInstance) implements CustomComponentType<T, T, DefaultComponents<T>> {

    @Override
    public boolean matches(RegularComponentType<?, ?> otherType) {
        return this.type.equals(otherType);
    }

}
