package de.schosin.ecs.storage.api.events;

import de.schosin.ecs.api.Pooled;

public sealed interface StorageEvent extends Pooled permits ComponentAddedEvent, ArchetypeAddedEvent {

    void free();

}
