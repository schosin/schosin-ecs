package de.schosin.ecs.plugins.data.types;

import de.schosin.ecs.api.components.types.ComponentType;

/**
 * Base functional interface for providing data for entities.
 * 
 * @param <T> type of data, usually maps to {@link ComponentType} {@code T}
 * 
 * @see DataProviderType
 * @see DataProcessor
 * @see DataProcessorType
 */
@FunctionalInterface
public interface DataProvider<T> {

    T getData();

}
