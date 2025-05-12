package de.schosin.ecs.engine.components;

import java.util.Objects;

import org.jspecify.annotations.NonNull;

import de.schosin.ecs.engine.IdManager.Id.ComponentId;
import de.schosin.ecs.engine.components.Component.PooledComponent;
import de.schosin.ecs.engine.utils.collections.Bag;
import de.schosin.ecs.engine.utils.collections.BitVector;
import de.schosin.ecs.engine.utils.collections.Pool;

sealed interface ComponentData<T> extends Component<T>, PooledComponent<T> {

    Class<T> clazz();

}

record ComponentDataImpl<T>(ComponentId componentId, Class<T> clazz, Bag<T> components, BitVector removals, Pool<T> pool) implements ComponentData<T> {

    @Override
    public int id() {
        return componentId.id();
    }

    @Override
    public String display() {
        return "%s(%d)".formatted(clazz.getSimpleName(), id());
    }

    @Override
    public boolean hasComponent(int entityId) {
        return this.components.get(entityId) != null;
    }

    @Override
    public T getComponent(int entityId) {
        return this.components.get(entityId);
    }

    @Override
    public void addComponentUnsafe(int entityId, @NonNull T component) {
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

    @Override
    public void applyRemoval(int entityId) {
        removeComponent(entityId);
    }

    @Override
    public void removeComponent(int entityId) {
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
        return Objects.hash(componentId.id());
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
        return componentId.id() == other.componentId.id();
    }

}
