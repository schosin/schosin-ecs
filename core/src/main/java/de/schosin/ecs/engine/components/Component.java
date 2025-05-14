package de.schosin.ecs.engine.components;

import java.util.Objects;

import org.jspecify.annotations.NonNull;

import de.schosin.ecs.api.components.ComponentType.RegularComponentType;

public sealed interface Component<T> permits ComponentData {

    interface PooledComponent<T> {
        T getInstance();
    }

    int id();
    
    RegularComponentType<T> type();

    String display();

    boolean hasComponent(int entityId);

    T getComponent(int entityId);

    default void addComponent(int entityId, @NonNull T component) {
        addComponentUnsafe(entityId, Objects.requireNonNull(component, "component cannot be null"));
    }

    void addComponentUnsafe(int id, T component);

    void markRemoved(int entityId);

    void unmarkRemoved(int entityId);

    void removeComponent(int entityId);

    void applyRemovals();

    void applyRemoval(int entityId);

    /**
     * Must be overriden based on {@link #id()} only.
     */
    @Override
    int hashCode();

    /**
     * Must be overriden based on {@link #id()} only.
     */
    @Override
    boolean equals(Object obj);

}
