package de.schosin.ecs.api.components.mappers;

import static de.schosin.ecs.api.components.types.ComponentType.wildcardRelation;

import de.schosin.ecs.api.components.Relation;
import de.schosin.ecs.api.components.Relation.ComponentRelation;
import de.schosin.ecs.api.components.Result;
import de.schosin.ecs.api.components.types.WildcardRelationType.WildcardComponentRelationType;

public sealed interface WildcardRelations<R extends Relation<?>, T extends Result<?>> extends Components<R, T> {

    non-sealed interface WildcardComponentRelations<R, T> extends WildcardRelations<ComponentRelation<? extends R, ? extends T>, Result<ComponentRelation<? extends R, ? extends T>>> {
    }

    interface Creator {

        default <R, T> WildcardComponentRelations<R, T> getWildcardComponentRelations(Class<R> relationshipBound, Class<T> targetBound) {
            return getWildcardComponentRelations(wildcardRelation(relationshipBound, targetBound));
        }

        <R, T> WildcardComponentRelations<R, T> getWildcardComponentRelations(WildcardComponentRelationType<R, T> wildcardRelation);

    }

}
