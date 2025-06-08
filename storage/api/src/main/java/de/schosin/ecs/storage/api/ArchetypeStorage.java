package de.schosin.ecs.storage.api;

import de.schosin.ecs.api.components.types.ComponentType.RegularComponentType;
import de.schosin.ecs.storage.api.entities.Archetype;

public interface ArchetypeStorage {

    Archetype getArchetypeForEntity(int entityId);

    Archetype getArchetypeById(int archetypeId);

    Archetype getArchetype(RegularComponentType<?, ?>... componentTypes);

}
