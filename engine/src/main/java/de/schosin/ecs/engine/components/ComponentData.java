package de.schosin.ecs.engine.components;

import java.util.Objects;

import de.schosin.ecs.engine.utils.collections.Bag;
import de.schosin.ecs.engine.utils.collections.BitVector;
import de.schosin.ecs.engine.utils.collections.Pool;

public sealed interface ComponentData<T> {

    int id();

    Class<T> clazz();

    boolean hasComponent(int entityId);

    T getComponent(int entityId);

    void addComponent(int entityId, T component);

    void markRemoved(int entityId);

    void unmarkRemoved(int entityId);

    /**
     * Applies pending removals of components.
     */
    void applyRemovals();

    T getInstance();

}

record ComponentDataImpl<T>(int id, Class<T> clazz, Bag<T> components, BitVector removals, Pool<T> pool) implements ComponentData<T> {

    @Override
    public boolean hasComponent(int entityId) {
        return this.components.get(entityId) != null;
    }

    @Override
    public T getComponent(int entityId) {
        return this.components.get(entityId);
    }

    @Override
    public void addComponent(int entityId, T component) {
        if (component == null) {
            return;
        }
        this.components.set(entityId, component);
    }

    @Override
    public void markRemoved(int entityId) {
        this.removals.set(entityId);
    }

    @Override
    public void unmarkRemoved(int entityId) {
        this.removals.clear(entityId);
    }

    @Override
    public void applyRemovals() {
        if (removals.isEmpty()) {
            return;
        }

        this.removals.iterate(this::removeComponent);
        this.removals.clear();
    }

    void removeComponent(int entityId) {
        // Non-pooled
        if (pool == null) {
            this.components.set(entityId, null);

            return;
        }

        // Pooled
        var component = this.components.get(entityId);
        if (component != null) {
            this.components.set(entityId, null);
            this.pool.free(component);
        }
    }

    @Override
    public T getInstance() {
        return pool.getInstance();
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    @SuppressWarnings("rawtypes")
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ComponentDataImpl other = (ComponentDataImpl) obj;
        return id == other.id;
    }

}
