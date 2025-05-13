package de.schosin.ecs.engine.events;

public interface EventHandler<T> {

    void handle(T event);

}
