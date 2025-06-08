package de.schosin.ecs.storage.api.events;

import de.schosin.ecs.storage.api.entities.Archetype;
import de.schosin.ecs.storage.api.entities.ComponentMask;

public record ArchetypeAddedEvent(ComponentMask componentMask, Archetype archetype) implements StorageEvent {

    @Override
    public void free() {
    }

}
