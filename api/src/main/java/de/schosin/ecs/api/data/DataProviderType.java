package de.schosin.ecs.api.data;

import de.schosin.ecs.api.components.types.ComponentType;

/**
 * Interface for {@link ComponentType ComponentTypes} to extend.
 * 
 * <p>
 * APIs accepting a {@link DataProvider} may support component types
 * implementing this interface by not only accepting a {@link DataProvider},
 * but also the specialized tpe {@code P}.
 * </p>
 * 
 * @param <T> matches {@link ComponentType} {@code T}
 * @param <P> specialized {@link DataProvider} type for {@code T}
 */
public interface DataProviderType<T, P extends DataProvider<T>> {
}
