package de.schosin.ecs.api.components.mappers;

import de.schosin.ecs.api.components.Relation.EntityRelationData;
import de.schosin.ecs.api.components.Relation.Exclusive;
import de.schosin.ecs.api.components.Result.EntityRelationDataResult;
import de.schosin.ecs.api.components.types.ComponentType;
import de.schosin.ecs.api.components.types.RelationFetchType;
import de.schosin.ecs.api.components.types.RelationFetchType.EntityRelationFetchType;
import de.schosin.ecs.api.components.types.RelationFetchType.ExclusiveEntityRelationFetchType;

/**
 * {@link Components Component mapper} for {@link RelationFetchType} components. Contains subtypes
 * for both non-exclusive and exclusive variants.
 * 
 * @param <R> type of relationship component
 * @param <T> type of fetched data for target entity
 * @param <X> maps to {@link ComponentType} {@code R} (read operations)
 */
public sealed interface EntityFetchRelationMappers<R, T, F> extends Components<EntityRelationData<R, T>, F> {

    non-sealed interface EntityRelationFetchMapper<R, T> extends EntityFetchRelationMappers<R, T, EntityRelationDataResult<R, T>> {
    }

    non-sealed interface ExclusiveEntityRelationFetchMapper<R extends Exclusive, T> extends EntityFetchRelationMappers<R, T, EntityRelationData<R, T>> {
    }

    interface Creator {

        <R, T> EntityRelationFetchMapper<R, T> getComponents(EntityRelationFetchType<R, T> relation);

        <R extends Exclusive, T> ExclusiveEntityRelationFetchMapper<R, T> getComponents(ExclusiveEntityRelationFetchType<R, T> relation);

    }

}
