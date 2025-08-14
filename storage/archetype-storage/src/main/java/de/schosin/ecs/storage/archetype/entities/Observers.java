package de.schosin.ecs.storage.archetype.entities;

import de.schosin.ecs.storage.api.entities.Archetype;
import de.schosin.ecs.storage.api.entities.observer.EntitiesBeforeUpdateObserver;
import de.schosin.ecs.storage.api.entities.observer.EntitiesCreatedObserver;
import de.schosin.ecs.storage.api.entities.observer.EntitiesDeletedObserver;
import de.schosin.ecs.storage.api.entities.observer.EntitiesUpdatedObserver;
import de.schosin.ecs.storage.api.entities.observer.EntityBeforeUpdateObserver;
import de.schosin.ecs.storage.api.entities.observer.EntityCreatedObserver;
import de.schosin.ecs.storage.api.entities.observer.EntityUpdatedObserver;
import de.schosin.ecs.utils.collections.Bag;
import de.schosin.ecs.utils.collections.ImmutableIntBag;

public class Observers {

    private final Bag<EntityCreatedObserver> createdObservers = new Bag<>(EntityCreatedObserver.class, 16);
    private final Bag<EntitiesCreatedObserver> batchCreatedObservers = new Bag<>(EntitiesCreatedObserver.class, 16);
    private final Bag<EntityBeforeUpdateObserver> beforeUpdateObservers = new Bag<>(EntityBeforeUpdateObserver.class, 16);
    private final Bag<EntityUpdatedObserver> updatedObservers = new Bag<>(EntityUpdatedObserver.class, 16);
    private final Bag<EntitiesBeforeUpdateObserver> batchBeforeUpdateObservers = new Bag<>(EntitiesBeforeUpdateObserver.class, 16);
    private final Bag<EntitiesUpdatedObserver> batchUpdatedObservers = new Bag<>(EntitiesUpdatedObserver.class, 16);
    private final Bag<EntitiesDeletedObserver> deletedObservers = new Bag<>(EntitiesDeletedObserver.class, 16);

    public void triggerEntityCreated(Archetype archetype, int entityId) {
        var data = this.createdObservers.getData();
        for (int i = 0, s = this.createdObservers.getSize(); i < s; i++) {
            data[i].handleEntityCreated(archetype, entityId);
        }
    }

    public void triggerEntitiesCreated(Archetype archetype, ImmutableIntBag entities) {
        var data = this.batchCreatedObservers.getData();
        for (int i = 0, s = this.batchCreatedObservers.getSize(); i < s; i++) {
            data[i].handleEntitiesCreated(archetype, entities);
        }
    }

    public void triggerEntityBeforeUpdate(Archetype archetype, Archetype newArchetype, int entityId) {
        var data = this.beforeUpdateObservers.getData();
        for (int i = 0, s = this.beforeUpdateObservers.getSize(); i < s; i++) {
            data[i].handleEntityBeforeUpdate(archetype, newArchetype, entityId);
        }
    }

    public void triggerEntityUpdated(Archetype archetype, Archetype previousArchetype, int entityId) {
        var data = this.updatedObservers.getData();
        for (int i = 0, s = this.updatedObservers.getSize(); i < s; i++) {
            data[i].handleEntityUpdated(archetype, previousArchetype, entityId);
        }
    }

    public void triggerEntitiesBeforeUpdate(Archetype archetype, Archetype newArchetype, ImmutableIntBag entities) {
        var data = this.batchBeforeUpdateObservers.getData();
        for (int i = 0, s = this.batchBeforeUpdateObservers.getSize(); i < s; i++) {
            data[i].handleEntitiesBeforeUpdate(archetype, newArchetype, entities);
        }
    }

    public void triggerEntitiesUpdated(Archetype archetype, Archetype previousArchetype, ImmutableIntBag entities) {
        var data = this.batchUpdatedObservers.getData();
        for (int i = 0, s = this.batchUpdatedObservers.getSize(); i < s; i++) {
            data[i].handleEntitiesUpdated(archetype, previousArchetype, entities);
        }
    }

    public void triggerEntitiesDeleted(Archetype archetype, ImmutableIntBag entities) {
        var data = this.deletedObservers.getData();
        for (int i = 0, s = this.deletedObservers.getSize(); i < s; i++) {
            data[i].handleEntitiesDeleted(archetype, entities);
        }
    }

    public void register(EntityCreatedObserver observer) {
        this.createdObservers.add(observer);
    }

    public void register(EntitiesCreatedObserver observer) {
        this.batchCreatedObservers.add(observer);
    }

    public void register(EntityBeforeUpdateObserver observer) {
        this.beforeUpdateObservers.add(observer);
    }

    public void register(EntityUpdatedObserver observer) {
        this.updatedObservers.add(observer);
    }

    public void register(EntitiesBeforeUpdateObserver observer) {
        this.batchBeforeUpdateObservers.add(observer);
    }

    public void register(EntitiesUpdatedObserver observer) {
        this.batchUpdatedObservers.add(observer);
    }

    public void register(EntitiesDeletedObserver observer) {
        this.deletedObservers.add(observer);
    }

}
