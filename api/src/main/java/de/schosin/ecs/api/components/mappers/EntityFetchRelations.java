package de.schosin.ecs.api.components.mappers;

import de.schosin.ecs.api.components.Relation.EntityRelationData;
import de.schosin.ecs.api.components.Relation.Exclusive;
import de.schosin.ecs.api.components.Result.EntityRelationDataResult;
import de.schosin.ecs.api.components.types.RelationFetchType.EntityRelationFetchType;
import de.schosin.ecs.api.components.types.RelationFetchType.ExclusiveEntityRelationFetchType;

public sealed interface EntityFetchRelations<R, T, F> extends Components<EntityRelationData<R, T>, F> {

    non-sealed interface EntityRelationFetchMapper<R, T> extends EntityFetchRelations<R, T, EntityRelationDataResult<R, T>> {
    }

    non-sealed interface ExclusiveEntityRelationFetchMapper<R extends Exclusive, T> extends EntityFetchRelations<R, T, EntityRelationData<R, T>> {
    }

    interface Creator {

        <R, T> EntityRelationFetchMapper<R, T> getEntityFetchRelations(EntityRelationFetchType<R, T> relation);

        <R extends Exclusive, T> ExclusiveEntityRelationFetchMapper<R, T> getEntityFetchRelations(ExclusiveEntityRelationFetchType<R, T> relation);

    }

}
