package de.schosin.ecs.api.data;

public interface IterableAccessor extends DataAccessor {

    int size();

    int entityId(int index);

    @Deprecated(forRemoval = true)
    boolean hasNext();

    @Deprecated(forRemoval = true)
    int next();

    @Deprecated(forRemoval = true)
    void resetIterable();

}
