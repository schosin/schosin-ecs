package de.schosin.ecs.api.components.types;

import de.schosin.ecs.api.components.mappers.CustomComponentMapper;

/**
 * Declares a custom component type that can be used to extend the
 * supported component types.
 * 
 * <p>
 * A custom component type must declare its {@link CustomComponentMapper}
 * implementation. To use a custom component type, a 
 * {@link CustomComponentMapper.Factory Factory} has to be registered with
 * {@link de.schosin.ecs.engine.components.ComponentMapperManager ComponentMapperManager}.
 * Custom components are usually added by plugins, which should register the matching
 * component mapper upon initialization.
 * </p>
 * 
 * @param <T> type of a single component instance
 * @param <R> type of component data when reading
 * @param <C> type of {@link CustomComponentMapper} implementation
 */
public non-sealed interface CustomComponentType<T, R, C extends CustomComponentMapper<T, R>> extends ComponentType<T, R> {

    @Override
    default boolean matches(RegularComponentType<?, ?> otherType) {
        return false;
    }

}
