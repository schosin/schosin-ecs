package de.schosin.ecs.api.components.mappers;

import static de.schosin.ecs.api.components.types.ComponentType.exclusiveRelation;
import static de.schosin.ecs.api.components.types.ComponentType.relation;

import de.schosin.ecs.api.components.Relation;
import de.schosin.ecs.api.components.Relation.EntityRelation;
import de.schosin.ecs.api.components.Relation.Exclusive;
import de.schosin.ecs.api.components.Result.EntityRelationResult;
import de.schosin.ecs.api.components.mappers.Components.RegularComponents;
import de.schosin.ecs.api.components.types.ComponentType;
import de.schosin.ecs.api.components.types.RelationComponentType.ComponentRelationType;
import de.schosin.ecs.api.components.types.RelationComponentType.EntityRelationType;
import de.schosin.ecs.api.components.types.RelationComponentType.ExclusiveComponentRelationType;
import de.schosin.ecs.api.components.types.RelationComponentType.ExclusiveEntityRelationType;
import de.schosin.ecs.api.components.types.RelationComponentType.RegularEntityRelationType;

/**
 * {@link Components Component mapper} for {@link RegularEntityRelationType} components. Contains subtypes
 * for both non-exclusive and exclusive variants.
 * 
 * @param <R> type of relationship component
 * @param <X> maps to {@link ComponentType} {@code R} (read operations)
 */
public sealed interface EntityRelations<R, X> extends RegularComponents<EntityRelation<R>, X> {

    default EntityRelation<R> add(int entityId, R relationship, int target) {
        return add(entityId, Relation.create(relationship, target));
    }

    non-sealed interface EntityRelationMapper<R> extends EntityRelations<R, EntityRelationResult<R>> {

        R getRelationship(int entityId, int target);

    }

    non-sealed interface ExclusiveEntityRelationMapper<R extends Exclusive> extends EntityRelations<R, EntityRelation<R>> {

        R getRelationship(int entityId);

        /**
         * @return target or -1 if no relation
         */
        int getTarget(int entityId);

    }

    interface Creator {

        /**
         * Retrieves the mapper for a {@link EntityRelationType}. This can be used to access the relations
         * and to add or remove them from entities.
         * 
         * @param <R> type of relationship component
         * @param relationship class of relationship component
         * @return class to manage the relations defined by the relationship class
         */
        default <R> EntityRelationMapper<R> getEntityRelations(Class<R> relationship) {
            return getComponents(relation(relationship));
        }

        /**
         * Retrieves the mapper for a {@link EntityRelationType}. This can be used to access the relations
         * and to add or remove them from entities.
         * 
         * @param <R> type of relationship component
         * @param relation {@link ComponentRelationType} of the relation
         * @return class to manage the relations defined by the type
         */
        <R> EntityRelationMapper<R> getComponents(EntityRelationType<R> relation);

        /**
         * Retrieves the mapper for a {@link ExclusiveEntityRelationType}. This can be used to access the relations
         * and to add or remove them from entities.
         * 
         * @param <R> type of relationship component
         * @param relationship class of relationship component
         * @return class to manage the relations defined by the relationship class
         */
        default <R extends Exclusive> ExclusiveEntityRelationMapper<R> getExclusiveEntityRelations(Class<R> relationship) {
            return getComponents(exclusiveRelation(relationship));
        }

        /**
         * Retrieves the mapper for a {@link ExclusiveEntityRelationType}. This can be used to access the relations
         * and to add or remove them from entities.
         * 
         * @param <R> type of relationship component
         * @param relation {@link ExclusiveComponentRelationType} of the relation
         * @return class to manage the relations defined by the relationship type
         */
        <R extends Exclusive> ExclusiveEntityRelationMapper<R> getComponents(ExclusiveEntityRelationType<R> relation);

    }

}
