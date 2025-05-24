package de.schosin.ecs.storage.defaultimpl.components;

import org.jspecify.annotations.NonNull;

import de.schosin.ecs.api.components.types.ClassType;
import de.schosin.ecs.storage.api.components.Component.ComponentData;
import de.schosin.ecs.utils.collections.Bag;

public record ComponentDataImpl<T>(int id, ClassType<T> type, Bag<T> components) implements ComponentData<T> {

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
    public void removeComponent(int entityId) {
        this.components.set(entityId, null);
    }

    @Override
    public int hashCode() {
        return id;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof ComponentDataImpl<?> data && data.id() == id;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("ComponentDataImpl [id=").append(this.id).append(", type=").append(this.type).append("]");
        return builder.toString();
    }

}
