package de.schosin.ecs.api.components.types;

import de.schosin.ecs.api.components.mappers.CustomComponentMapper;
import de.schosin.ecs.api.data.DataAccessor;
import de.schosin.ecs.api.data.DataProcessor;
import de.schosin.ecs.api.data.IterableComponentProcessor;

/**
 * Interface for {@link ComponentType ComponentTypes} to extend. Provides additional type information
 * for a specialized {@link DataProcessor} implementation.
 * 
 * <p>
 * This is primarily used for {@link ComponentSetType} and DataType (ecs-plugins-data-types) to
 * optimize entity iteration and component access. This is primarly achieved by unpacking the components
 * and passing them directly to the specialized {@link DataProcessor}, removing the need for intermediate objects.
 * </p>
 * 
 * <p>
 * Custom component types can choose to implement {@link CustomComponentType} if they wish 
 * to provide custom {@link DataProcessor} types. If you do, the corresponding {@link CustomComponentMapper}
 * MUST return an {@link IterableComponentProcessor} whenever {@link CustomComponentMapper#getComponentAccessor(DataAccessor)}
 * is called.
 * </p>
 *  
 * @param <T> matches {@link ComponentType} {@code T}
 * @param <R> matches {@link ComponentType} {@code R}
 * @param <P> specialized {@link DataProcessor} type for {@code R}
 */
public sealed interface DataProcessorType<T, R, P extends DataProcessor<R>> extends ComponentType<T, R> permits ComponentSetType, CustomDataProcessorType {
}
