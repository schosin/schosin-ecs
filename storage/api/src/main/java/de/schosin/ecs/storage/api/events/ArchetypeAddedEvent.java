package de.schosin.ecs.storage.api.events;

import de.schosin.ecs.storage.api.entities.Archetype;

public record ArchetypeAddedEvent(Archetype archetype) implements StorageEvent {

    @Override
    public void free() {
    }

}
