package de.schosin.ecs.engine.entities;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jspecify.annotations.NonNull;

import de.schosin.ecs.api.Pooled;
import de.schosin.ecs.api.state.State;
import de.schosin.ecs.api.state.State.PooledState;
import de.schosin.ecs.engine.BagManager;
import de.schosin.ecs.engine.EngineWorld.Classes;
import de.schosin.ecs.engine.utils.collections.Bag;
import de.schosin.ecs.engine.utils.collections.Pool;
import de.schosin.ecs.engine.utils.collections.ReflectionUtils;

public class StateManager {

    private static final int POOL_LIMIT = 1000000; // TODO configuration or per-class (default method in interface? Annotation? config per-class?)

    private final BagManager bagManager;
    private final Classes classes;

    private final Map<Class<?>, State<?>> byClass = new ConcurrentHashMap<>();

    public StateManager(BagManager bagManager, Classes classes) {
        this.bagManager = bagManager;
        this.classes = classes;
    }

    @SuppressWarnings("unchecked")
    public <T> State<T> getState(@NonNull Class<T> clazz) {
        var result = (State<T>) byClass.get(clazz);
        if (result != null) {
            return result;
        }

        synchronized (classes) {
            if (classes.components().contains(clazz)) {
                throw new IllegalArgumentException("Class %s is already used as a component.".formatted(clazz.getName()));
            }

            classes.states().add(clazz);
            return (State<T>) this.byClass.computeIfAbsent(clazz, this::createState);
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private <T> State<T> createState(Class<T> clazz) {
        return Pooled.class.isAssignableFrom(clazz)
                ? createPooledState((Class) clazz)
                : new StateImpl<>(clazz, bagManager.createEntityBag(clazz));
    }

    @SuppressWarnings("unchecked")
    public <T extends Pooled> PooledState<T> getPooledState(Class<T> clazz) {
        var result = (PooledState<T>) byClass.get(clazz);
        if (result != null) {
            return result;
        }

        synchronized (classes) {
            if (classes.components().contains(clazz)) {
                throw new IllegalArgumentException("Class %s is already used as a component.".formatted(clazz.getName()));
            }

            classes.states().add(clazz);
            return (PooledState<T>) this.byClass.computeIfAbsent(clazz, ignore -> createPooledState(clazz));
        }
    }

    private <T extends Pooled> PooledStateImpl<T> createPooledState(Class<T> clazz) {
        return new PooledStateImpl<>(clazz, bagManager.createEntityBag(clazz), Pool.bounded(POOL_LIMIT, clazz, () -> ReflectionUtils.createComponentInstance(clazz)));
    }

    private record StateImpl<T>(Class<T> clazz, Bag<T> state) implements State<T> {

        @Override
        public T add(int entityId, T state) {
            var existing = get(entityId);
            if (existing != null) {
                throw new IllegalStateException("Cannot override existing state for entity %d: %s".formatted(entityId, state));
            }

            this.state.set(entityId, state);
            return state;
        }

        @Override
        public T get(int entityId) {
            return this.state.get(entityId);
        }

        @Override
        public void remove(int entityId) {
            this.state.set(entityId, null);
        }

    }

    private record PooledStateImpl<T extends Pooled>(Class<T> clazz, Bag<T> state, Pool<T> pool) implements PooledState<T> {

        @Override
        public @NonNull T add(int entityId) {
            var existing = get(entityId);
            if (existing != null) {
                return existing;
            }

            return add(entityId, getInstance());
        }

        @Override
        public T add(int entityId, T state) {
            var existing = get(entityId);
            if (existing != null) {
                throw new IllegalStateException("Cannot override existing state for entity %d: %s".formatted(entityId, state));
            }

            this.state.set(entityId, state);
            return state;
        }

        @Override
        public T get(int entityId) {
            return this.state.get(entityId);
        }

        @Override
        public void remove(int entityId) {
            var state = this.state.get(entityId);
            if (state != null) {
                this.state.set(entityId, null);
                this.pool.free(state);
            }
        }

        @Override
        public @NonNull T getInstance() {
            return pool.getInstance();
        }

    }

}
