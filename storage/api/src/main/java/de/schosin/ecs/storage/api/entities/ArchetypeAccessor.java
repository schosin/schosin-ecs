package de.schosin.ecs.storage.api.entities;

import de.schosin.ecs.api.components.types.ComponentType.RegularComponentType;
import de.schosin.ecs.api.data.DataAccessor;
import de.schosin.ecs.api.entities.Entity;

public interface ArchetypeAccessor extends DataAccessor, Entity {

    Archetype getArchetype();

    <R> R getComponent(RegularComponentType<?, R> componentType);

    <R> R getComponent(RegularComponentType<?, R> componentType, int componentId);

    @Override
    default int id() {
        return entityId();
    }

    @Override
    default boolean isAlive() {
        return isValid();
    }

    @Override
    default <R> R get(RegularComponentType<?, R> componentType) {
        if (!isValid()) {
            throw new IllegalStateException("Entity %d is not alive".formatted(entityId()));
        }

        return getComponent(componentType);
    }

}
