package de.schosin.ecs.api.components.mappers;

import static de.schosin.ecs.api.components.types.ComponentType.componentSet;

import org.jspecify.annotations.NonNull;

import de.schosin.ecs.api.components.ComponentSet;
import de.schosin.ecs.api.components.types.ComponentSetType;

/**
 * {@link Components Component mapper} for {@link ComponentSetType} components. 
 * 
 * @param <T> type of component set
 */
public non-sealed interface ComponentSetMapper<T extends ComponentSet> extends Components<T, T> {

    /**
     * Adds the components contained in the component set to the entity. 
     * Overwrites any existing components of the same class. 
     * 
     * @param entityId id of entity
     * @param components component set instance
     */
    void add(int entityId, @NonNull T components);

    /**
     * Returns true if the entity has all of the components defined by this set.
     * 
     * @param entityId id of entity
     * @return true if entity has any component
     */
    boolean hasAll(int entityId);

    interface Creator {

        /**
         * Retrieves the mapper for a {@link ComponentSet} class. This can be used to acces the component set
         * and to add or remove the components of a set from entities.
         *  
         * @param <T> type of component set
         * @param class of the set
         * @return class to manage the component sets defined by the type
         */
        default <T extends ComponentSet> ComponentSetMapper<T> getComponentSets(Class<T> type) {
            return getComponents(componentSet(type));
        }

        /**
         * Retrieves the mapper for a {@link ComponentSetType}. This can be used to acces the component set
         * and to add or remove the components of a set from entities.
         *  
         * @param <T> type of component set
         * @param type {@link ComponentSetType} of the set
         * @return class to manage the component sets defined by the type
         */
        <T extends ComponentSet> ComponentSetMapper<T> getComponents(ComponentSetType<T> type);

    }

}
