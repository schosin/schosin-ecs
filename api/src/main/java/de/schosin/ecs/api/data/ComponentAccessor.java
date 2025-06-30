package de.schosin.ecs.api.data;

public interface ComponentAccessor<R> {

    R getComponent(DataAccessor accessor);

    default void free() {
    }

}
