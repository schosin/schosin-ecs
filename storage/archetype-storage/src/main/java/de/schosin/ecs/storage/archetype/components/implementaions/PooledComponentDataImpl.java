package de.schosin.ecs.storage.archetype.components.implementaions;

import de.schosin.ecs.api.Pooled;
import de.schosin.ecs.api.components.types.ClassType;
import de.schosin.ecs.storage.api.components.Component.PooledComponentData;
import de.schosin.ecs.storage.archetype.entities.EntityIndex;
import de.schosin.ecs.utils.collections.Pool;

public record PooledComponentDataImpl<T extends Pooled>(int id, ClassType<T> type, EntityIndex index, Pool<T> pool) implements PooledComponentData<T> {

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
        return index.hasComponent(entityId, type);
    }

    @Override
    public T getComponent(int entityId) {
        return index.getComponent(entityId, type);
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
