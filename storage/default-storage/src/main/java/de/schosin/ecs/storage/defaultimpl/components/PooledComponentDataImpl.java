package de.schosin.ecs.storage.defaultimpl.components;

import org.jspecify.annotations.NonNull;

import de.schosin.ecs.api.Pooled;
import de.schosin.ecs.api.components.types.ClassType;
import de.schosin.ecs.storage.api.components.Component.PooledComponentData;
import de.schosin.ecs.storage.common.PendingChanges;
import de.schosin.ecs.utils.collections.Bag;
import de.schosin.ecs.utils.collections.Pool;

public record PooledComponentDataImpl<T extends Pooled>(int id, ClassType<T> type, Bag<T> components, Bag<PendingChanges> changes, Pool<T> pool)
        implements DefaultComponent<T>, PooledComponentData<T> {

    @Override
    public Class<T> clazz() {
        return type.clazz();
    }

    @Override
    public String display() {
        return "%s(%d)".formatted(type.clazz().getSimpleName(), id());
    }

    @Override
    public boolean hasComponent(int entityId) {
        return getComponent(entityId) != null;
    }

    @Override
    public T getComponent(int entityId) {
        var result = this.components.get(entityId);
        if (result != null) {
            return result;
        }

        var changes = this.changes.get(entityId);
        if (changes != null) {
            return changes.getComponent(type);
        }

        return null;
    }

    @Override
    public void addComponentUnsafe(int entityId, @NonNull T component) {
        this.components.set(entityId, component);
    }

    @Override
    public void removeComponent(int entityId) {
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
        return id;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof PooledComponentDataImpl<?> data && data.id() == id;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("PooledComponentDataImpl [id=").append(this.id).append(", type=").append(this.type).append("]");
        return builder.toString();
    }

}
