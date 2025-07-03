package de.schosin.ecs.api.components.types;

import de.schosin.ecs.api.components.mappers.CustomComponentMapper;
import de.schosin.ecs.api.data.DataProcessor;

/**
 * Extension point for {@link CustomComponentType} that also support custom {@link DataProcessor} types.
 * 
 * <p>
 * The main benefit 
 * </p>
 * 
 * @param <T> type of a single component instance
 * @param <R> type of component data when reading
 * @param <C> type of {@link CustomComponentMapper} implementation
 * @param <P> specialized {@link DataProcessor} type for {@code R}
 */
public non-sealed interface CustomDataProcessorType<T, R, C extends CustomComponentMapper<T, R>, P extends DataProcessor<R>> extends CustomComponentType<T, R, C>, DataProcessorType<T, R, P> {
}
