package de.schosin.ecs.api.components.types;

import de.schosin.ecs.api.components.mappers.CustomComponents;

/**
 * Declares a custom component type that can be used to extend the
 * supported component types.
 * 
 * <p>
 * A custom component type must declare its {@link CustomComponents}
 * implementation. To use a custom component type, a 
 * {@link CustomComponents.Factory Factory} has to be registered with
 * {@link de.schosin.ecs.engine.components.ComponentMapperManager ComponentMapperManager}.
 * </p>
 * 
 * @param <T> type of a single component instance
 * @param <R> type of component data when reading
 * @param <C> type of {@link CustomComponents} implementation
 */
public non-sealed interface CustomComponentType<T, R, C extends CustomComponents<T, R>> extends ComponentType<T, R> {

    @Override
    default boolean matches(ComponentType<?, ?> otherType) {
        return this.equals(otherType);
    }

}
