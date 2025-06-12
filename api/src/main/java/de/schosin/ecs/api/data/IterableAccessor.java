package de.schosin.ecs.api.data;

public interface IterableAccessor extends DataAccessor {

    boolean hasNext();

    int next();

}
