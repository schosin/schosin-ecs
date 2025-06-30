package de.schosin.ecs.storage.api.entities;

import de.schosin.ecs.api.components.types.ComponentType.RegularComponentType;
import de.schosin.ecs.api.data.DataAccessor;

public interface ArchetypeAccessor extends DataAccessor {

    Archetype getArchetype();

    <R> R getComponent(RegularComponentType<?, R> componentType, int componentId);

}
