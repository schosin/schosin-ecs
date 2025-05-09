package de.schosin.ecs.plugins.state;

import org.jspecify.annotations.NonNull;

import de.schosin.ecs.api.Plugin;
import de.schosin.ecs.api.Pooled;
import de.schosin.ecs.plugins.state.State.PooledState;

@Plugin(StateManager.class)
public interface StatePlugin {

    /**
     * Retrieves the mapper of the given state class. This can be used to access state and
     * to add or remove state from entities.
     * 
     * <p>
     * <b>Note:</b> If T extends Pooled, the returned instance will also implement and
     * can be cast to {@link PooledState}. Prefer using {@link #getPooledState(Class)}
     * for pooled state.
     * </p>
     *  
     * @param clazz {@link Class} of the state
     * @return class to manage the state defined by the clazz argument 
     */
    <T> State<T> getState(@NonNull Class<T> clazz);

    /**
     * Retrieves the mapper of the given state class. This can be used to access state and 
     * to add or remove state from entities.
     * 
     * @param clazz {@link Class} of the state
     * @return class to manage the state defined by the clazz argument 
     */
    <T extends Pooled> PooledState<T> getPooledState(@NonNull Class<T> clazz);

}
