package de.schosin.ecs.storage.defaultimpl.components;

import org.jspecify.annotations.NonNull;

import de.schosin.ecs.api.components.types.ClassType;
import de.schosin.ecs.storage.api.components.Component.ComponentData;
import de.schosin.ecs.storage.common.PendingChanges;
import de.schosin.ecs.utils.collections.Bag;

public record ComponentDataImpl<T>(int id, ClassType<T> type, Bag<T> components, Bag<PendingChanges> changes) implements DefaultComponent<T>, ComponentData<T> {

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
