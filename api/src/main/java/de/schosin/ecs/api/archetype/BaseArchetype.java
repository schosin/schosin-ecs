package de.schosin.ecs.api.archetype;

import org.jspecify.annotations.NullMarked;

import de.schosin.ecs.api.Pooled;
import de.schosin.ecs.codegen.EcsCodegen;

@NullMarked
@EcsCodegen
public interface BaseArchetype {

    /**
     * Create a new archetype that extends this archetype by adding the passed components to
     * every created entity. The old archetype is not modified.
     * 
     * <p>
     * Intended to be used with marker or singleton components (e.g. enums).
     * </p>
     * 
     * <p>
     * <b>Note:</b> No duplicate components are allowed. If {@link #with(Object...)} adds components
     * defined when creating the archetype, or a previous {@link #with(Object...)} call, an error
     * is thrown.
     * </p>
     * 
     * @param components components to add to every entity
     * @return new archetype
     */
    Archetype with(Object... components);

    /**
     * Returns a pooled instance of the component.
     * 
     * @param <T> type of component
     * @param clazz class of component
     * @return pooled instance
     */
    <T extends Pooled> T getInstance(Class<T> clazz);

}
