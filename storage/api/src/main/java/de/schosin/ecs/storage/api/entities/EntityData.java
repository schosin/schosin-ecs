package de.schosin.ecs.storage.api.entities;

public interface EntityData {

    interface Accessor {

        void reset();

        boolean hasNext();

        int next();

        Object getComponent(int componentIndex);

    }

    int getSize();

    int getId(int index);

    <R> R getComponent(int index);

    Accessor getAccessor();

    void freeAccessor(Accessor accessor);

}
