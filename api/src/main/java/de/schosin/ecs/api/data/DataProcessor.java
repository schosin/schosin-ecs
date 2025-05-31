package de.schosin.ecs.api.data;

import de.schosin.ecs.api.components.types.ComponentType;

/**
 * Base functional interface for processing data of entities.
 * 
 * @param <R> type of data, usually maps to {@link ComponentType} {@code R}
 * 
 * @see DataProvider
 * @see DataProviderType
 * @see DataProcessorType
 */
@FunctionalInterface
public interface DataProcessor<R> {

    void process(int entityId, R data);

}
