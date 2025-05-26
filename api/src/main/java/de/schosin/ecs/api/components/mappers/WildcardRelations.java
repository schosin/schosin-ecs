package de.schosin.ecs.api.components.mappers;

import static de.schosin.ecs.api.components.types.ComponentType.wildcardRelation;

import de.schosin.ecs.api.components.Relation;
import de.schosin.ecs.api.components.Relation.ComponentRelation;
import de.schosin.ecs.api.components.Relation.EntityRelation;
import de.schosin.ecs.api.components.Relation.EntityRelationData;
import de.schosin.ecs.api.components.Result;
import de.schosin.ecs.api.components.types.ComponentType;
import de.schosin.ecs.api.components.types.WildcardRelationType.WildcardComponentRelationType;
import de.schosin.ecs.api.components.types.WildcardRelationType.WildcardEntityRelationFetchType;
import de.schosin.ecs.api.components.types.WildcardRelationType.WildcardEntityRelationType;

public sealed interface WildcardRelations<R extends Relation<?>, T extends Result<?>> extends Components<R, T> {

    non-sealed interface WildcardComponentRelations<R, T> extends WildcardRelations<ComponentRelation<? extends R, ? extends T>, Result<ComponentRelation<? extends R, ? extends T>>> {
    }

    non-sealed interface WildcardEntityRelations<R> extends WildcardRelations<EntityRelation<R>, Result<EntityRelation<? extends R>>> {
    }

    non-sealed interface WildcardEntityFetchRelations<R, T> extends WildcardRelations<EntityRelationData<? extends R, T>, Result<EntityRelationData<? extends R, T>>> {
    }

    interface Creator {

        default <R, T> WildcardComponentRelations<R, T> getWildcardComponentRelations(Class<R> relationshipBound, Class<T> targetBound) {
            return getWildcardComponentRelations(wildcardRelation(relationshipBound, targetBound));
        }

        <R, T> WildcardComponentRelations<R, T> getWildcardComponentRelations(WildcardComponentRelationType<R, T> wildcardRelation);

        default <R> WildcardEntityRelations<R> getWildcardEntityRelations(Class<R> relationshipBound) {
            return getWildcardEntityRelations(wildcardRelation(relationshipBound));
        }

        <R> WildcardEntityRelations<R> getWildcardEntityRelations(WildcardEntityRelationType<R> wildcardRelation);

        default <R, T> WildcardEntityFetchRelations<R, T> getWildcardEntityFetchRelations(Class<R> relationshipBound, ComponentType<?, T> fetch) {
            return getWildcardEntityFetchRelations(wildcardRelation(relationshipBound, fetch));
        }

        <R, T> WildcardEntityFetchRelations<R, T> getWildcardEntityFetchRelations(WildcardEntityRelationFetchType<R, T> wildcardRelation);

    }

}
