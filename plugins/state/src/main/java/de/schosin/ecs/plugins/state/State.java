package de.schosin.ecs.plugins.state;

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

}
