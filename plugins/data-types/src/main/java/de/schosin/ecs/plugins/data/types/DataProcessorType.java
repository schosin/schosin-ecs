package de.schosin.ecs.plugins.data.types;

import de.schosin.ecs.api.components.types.ComponentType;

/**
 * Interface for {@link ComponentType ComponentTypes} to extend.
 * 
 * <p>
 * APIs accepting a {@link DataProcessor} may support component types
 * implementing this interface by not only accepting a {@code DataProcessor<R>},
 * but also the specialized type {@code P}.
 * </p> 
 *  
 * @param <R> matches {@link ComponentType} {@code R}
 * @param <P> specialized {@link DataProcessor} type for {@code R}
 */
public interface DataProcessorType<R, P extends DataProcessor<R>> {
}
