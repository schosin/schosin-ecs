package de.schosin.ecs.storage.api;

import de.schosin.ecs.storage.api.entities.observer.EntitiesBeforeUpdateObserver;
import de.schosin.ecs.storage.api.entities.observer.EntitiesCreatedObserver;
import de.schosin.ecs.storage.api.entities.observer.EntitiesDeletedObserver;
import de.schosin.ecs.storage.api.entities.observer.EntitiesUpdatedObserver;
import de.schosin.ecs.storage.api.entities.observer.EntityBeforeUpdateObserver;
import de.schosin.ecs.storage.api.entities.observer.EntityCreatedObserver;
import de.schosin.ecs.storage.api.entities.observer.EntityUpdatedObserver;

public interface ObservableStorage {

    void registerCreated(EntityCreatedObserver observer);

    void registerBatchCreated(EntitiesCreatedObserver observer);

    void registerBeforeUpdate(EntityBeforeUpdateObserver observer);

    void registerUpdated(EntityUpdatedObserver observer);

    void registerBatchBeforeUpdate(EntitiesBeforeUpdateObserver observer);

    void registerBatchUpdated(EntitiesUpdatedObserver observer);

    void registerDeleted(EntitiesDeletedObserver observer);

}
