package de.schosin.ecs.engine.components;

public sealed interface Component permits ComponentData {

    int id();

    String display();

    boolean hasComponent(int entityId);

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
