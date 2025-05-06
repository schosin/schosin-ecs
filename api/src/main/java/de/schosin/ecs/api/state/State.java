package de.schosin.ecs.api.state;

import org.jspecify.annotations.NonNull;

import de.schosin.ecs.api.Pooled;
import de.schosin.ecs.api.World;

/**
 * State mapper for accessing and modifying state of an entity.
 * Use {@link World#getState(Class)} and {@link World#getPooledState(Class)}
 * to create instances of this interface.
 * 
 * <p>
 * TODO document + usage example (behaviour tree?)
 * </p>
 * 
 * @param <T>
 */
public interface State<T> {

    interface PooledState<T extends Pooled> extends State<T> {

        @NonNull
        T add(int entityId);

        @NonNull
        T getInstance();

    }

    T add(int entityId, T state);

    T get(int entityId);

    void remove(int entityId);

    interface Creator {

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

}
