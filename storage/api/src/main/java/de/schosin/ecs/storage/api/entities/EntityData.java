package de.schosin.ecs.storage.api.entities;

import de.schosin.ecs.api.data.IterableAccessor;

public interface EntityData {

    int getSize();

    int getId(int index);

    <R> R getComponent(int index);

    IterableAccessor getAccessor();

    ArchetypeAccessor getAccessor(int entityId);
    
    ArchetypeAccessor getAccessorByIndex(int index);

}
