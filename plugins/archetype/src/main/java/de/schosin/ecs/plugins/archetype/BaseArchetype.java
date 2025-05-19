package de.schosin.ecs.plugins.archetype;

import org.jspecify.annotations.NullMarked;

import de.schosin.ecs.api.Pooled;
import de.schosin.ecs.api.components.Relation.ComponentRelation;
import de.schosin.ecs.api.components.Relation.EntityRelation;
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
     * <p>
     * <b>Attention:</b> If the instance is added to an entity, it will be returned 
     * to the underlying pool the entity is deleted or the relation removed from it.
     * As such it cannot be assigned to multiple entities, as the data might be reset
     * while still assigned to another entity.
     * </p>
     * 
     * @param <T> type of component
     * @param clazz class of component
     * @return pooled instance
     */
    <T extends Pooled> T getInstance(Class<T> clazz);

    /**
     * Returns a pooled instance of a component relation.
     * 
     * <p>
     * <b>Attention:</b> If the instance is added to an entity, it will be returned 
     * to the underlying pool the entity is deleted or the relation removed from it.
     * As such it cannot be assigned to multiple entities, as the data might be reset
     * while still assigned to another entity.
     * </p>
     * 
     * @param <R> type of relationship
     * @param <T> type of target
     * @param relationship relationship component 
     * @param target target component
     * @return pooled instance
     */
    <R, T> ComponentRelation<R, T> getRelation(R relationship, T target);

    /**
     * Returns a pooled instance of an entity relation.
     * 
     * <p>
     * <b>Attention:</b> If the instance is added to an entity, it will be returned 
     * to the underlying pool the entity is deleted or the relation removed from it.
     * As such it cannot be assigned to multiple entities, as the data might be reset
     * while still assigned to another entity.
     * </p>
     * 
     * @param <R> type of relationship
     * @param relationship relationship component 
     * @param target target entity
     * @return pooled instance
     */
    <R> EntityRelation<R> getRelation(R relationship, int target);

}
