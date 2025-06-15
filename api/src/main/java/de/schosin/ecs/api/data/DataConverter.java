package de.schosin.ecs.api.data;

public interface DataConverter<R> {

    R getComponent(DataAccessor accessor);

    default void free(R component) {
    }

}
