package de.schosin.ecs.storage.defaultimpl.components;

import java.util.Objects;

import org.jspecify.annotations.NonNull;

public interface DefaultComponent<T> {

    default void addComponent(int entityId, @NonNull T component) {
        addComponentUnsafe(entityId, Objects.requireNonNull(component, "component cannot be null"));
    }

    /**
     * Adds the non-null component to the entity.
     * 
     * @param entityId id of entity
     * @param component non-null instance
     */
    void addComponentUnsafe(int entityId, T component);

    void removeComponent(int entityId);

}
