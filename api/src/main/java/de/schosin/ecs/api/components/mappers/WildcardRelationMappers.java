package de.schosin.ecs.api.components.mappers;

import static de.schosin.ecs.api.components.types.ComponentType.wildcardRelation;

import de.schosin.ecs.api.components.Relation;
import de.schosin.ecs.api.components.Relation.ComponentRelation;
import de.schosin.ecs.api.components.Relation.EntityRelation;
import de.schosin.ecs.api.components.Relation.EntityRelationData;
import de.schosin.ecs.api.components.Relations.ComponentRelations;
import de.schosin.ecs.api.components.Relations.EntityRelations;
import de.schosin.ecs.api.components.Relations.EntityRelationsData;
import de.schosin.ecs.api.components.Result;
import de.schosin.ecs.api.components.types.ComponentType;
import de.schosin.ecs.api.components.types.WildcardRelationType.WildcardComponentRelationType;
import de.schosin.ecs.api.components.types.WildcardRelationType.WildcardEntityRelationFetchType;
import de.schosin.ecs.api.components.types.WildcardRelationType.WildcardEntityRelationType;

public sealed interface WildcardRelationMappers<R extends Relation<?>, T extends Result<?>> extends Components<R, T> {

    non-sealed interface WildcardComponentRelationMapper<R, T> extends WildcardRelationMappers<ComponentRelation<? extends R, ? extends T>, ComponentRelations<? extends R, ? extends T>> {
    }

    non-sealed interface WildcardEntityRelationMapper<R> extends WildcardRelationMappers<EntityRelation<? extends R>, EntityRelations<? extends R>> {
    }

    non-sealed interface WildcardEntityFetchRelationMapper<R, T> extends WildcardRelationMappers<EntityRelationData<? extends R, T>, EntityRelationsData<? extends R, T>> {
    }

    interface Creator {

        default <R, T> WildcardComponentRelationMapper<R, T> getWildcardComponentRelations(Class<R> relationshipBound, Class<T> targetBound) {
            return getComponents(wildcardRelation(relationshipBound, targetBound));
        }

        <R, T> WildcardComponentRelationMapper<R, T> getComponents(WildcardComponentRelationType<R, T> wildcardRelation);

        default <R> WildcardEntityRelationMapper<R> getWildcardEntityRelations(Class<R> relationshipBound) {
            return getComponents(wildcardRelation(relationshipBound));
        }

        <R> WildcardEntityRelationMapper<R> getComponents(WildcardEntityRelationType<R> wildcardRelation);

        default <R, T> WildcardEntityFetchRelationMapper<R, T> getWildcardEntityFetchRelations(Class<R> relationshipBound, ComponentType<?, T> fetch) {
            return getComponents(wildcardRelation(relationshipBound, fetch));
        }

        <R, T> WildcardEntityFetchRelationMapper<R, T> getComponents(WildcardEntityRelationFetchType<R, T> wildcardRelation);

    }

}
