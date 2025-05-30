package de.schosin.ecs.api.components.mappers;

import static de.schosin.ecs.api.components.types.ComponentType.exclusiveRelation;
import static de.schosin.ecs.api.components.types.ComponentType.relation;

import de.schosin.ecs.api.components.Relation;
import de.schosin.ecs.api.components.Relation.ComponentRelation;
import de.schosin.ecs.api.components.Relation.Exclusive;
import de.schosin.ecs.api.components.Result.ComponentRelationResult;
import de.schosin.ecs.api.components.mappers.Components.RegularComponents;
import de.schosin.ecs.api.components.types.ComponentType;
import de.schosin.ecs.api.components.types.RelationComponentType.ComponentRelationType;
import de.schosin.ecs.api.components.types.RelationComponentType.ExclusiveComponentRelationType;
import de.schosin.ecs.api.components.types.RelationComponentType.RegularComponentRelationType;

/**
 * {@link Components Component mapper} for {@link RegularComponentRelationType} components. Contains subtypes
 * for both non-exclusive and exclusive variants.
 * 
 * @param <R> type of relationship component
 * @param <T> type of target component
 * @param <X> maps to {@link ComponentType} {@code R} (read operations)
 */
public sealed interface ComponentRelations<R, T, X> extends RegularComponents<ComponentRelation<R, T>, X> {

    default ComponentRelation<R, T> add(int entityId, R relationship, T target) {
        return add(entityId, Relation.create(relationship, target));
    }

    non-sealed interface ComponentRelationMapper<R, T> extends ComponentRelations<R, T, ComponentRelationResult<R, T>> {

        R getRelationship(int entityId, T target);

    }

    non-sealed interface ExclusiveComponentRelationMapper<R extends Exclusive, T> extends ComponentRelations<R, T, ComponentRelation<R, T>> {

        R getRelationship(int entityId);

        T getTarget(int entityId);

    }

    interface Creator {

        /**
         * Retrieves the mapper for a {@link ComponentRelationType}. This can be used to access the relations
         * and to add or remove them from entities.
         * 
         * @param <R> type of relationship component
         * @param <T> type of target component
         * @param relationship class of relationship component
         * @param target class of target component
         * @return class to manage the relations defined by the relationship and target class
         */
        default <R, T> ComponentRelationMapper<R, T> getComponentRelations(Class<R> relationship, Class<T> target) {
            return getComponentRelations(relation(relationship, target));
        }

        /**
         * Retrieves the mapper for a {@link ComponentRelationType}. This can be used to access the relations
         * and to add or remove them from entities.
         * 
         * @param <R> type of relationship component
         * @param <T> type of target component
         * @param relation {@link ComponentRelationType} of the relation
         * @return class to manage the relations defined by the relationship and target class
         */
        <R, T> ComponentRelationMapper<R, T> getComponentRelations(ComponentRelationType<R, T> relation);

        /**
         * Retrieves the mapper for a {@link ExclusiveComponentRelationType}. This can be used to access the relations
         * and to add or remove them from entities.
         * 
         * @param <R> type of relationship component
         * @param <T> type of target component
         * @param relationship class of relationship component
         * @param target class of target component
         * @return class to manage the relations defined by the relationship and target class
         */
        default <R extends Exclusive, T> ExclusiveComponentRelationMapper<R, T> getExclusiveComponentRelations(Class<R> relationship, Class<T> target) {
            return getComponentRelations(exclusiveRelation(relationship, target));
        }

        /**
         * Retrieves the mapper for a {@link ExclusiveComponentRelationType}. This can be used to access the relations
         * and to add or remove them from entities.
         * 
         * @param <R> type of relationship component
         * @param <T> type of target component
         * @param relation {@link ExclusiveComponentRelationType} of the relation
         * @return class to manage the relations defined by the relationship and target class
         */
        <R extends Exclusive, T> ExclusiveComponentRelationMapper<R, T> getComponentRelations(ExclusiveComponentRelationType<R, T> relation);

    }

}
