package de.schosin.ecs.api.data;

public interface ArchetypeComponentAccessor<T> {

    /**
     * Retrieves the component for the entity at the given index.
     * 
     * @param index index of entity
     * @return component instance or null if not assigned
     */
    T getComponent(int index);

}
