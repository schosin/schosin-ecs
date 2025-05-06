package de.schosin.ecs.engine.components;

public sealed interface Component permits ComponentData {

    int id();

    boolean hasComponent(int entityId);

    void markRemoved(int entityId);

    void unmarkRemoved(int entityId);

    void removeComponent(int entityId);

    void applyRemovals();

    void applyRemoval(int entityId);

}
