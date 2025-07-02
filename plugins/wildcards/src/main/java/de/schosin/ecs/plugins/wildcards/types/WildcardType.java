package de.schosin.ecs.plugins.wildcards.types;

import de.schosin.ecs.api.components.mappers.CustomComponentMapper;
import de.schosin.ecs.api.components.types.ComponentType;
import de.schosin.ecs.api.components.types.CustomComponentType;

public sealed interface WildcardType<T, R, C extends CustomComponentMapper<T, R>> extends CustomComponentType<T, R, C> permits WildcardClassType, WildcardRelationType {

    static WildcardClassType<Object> WILDCARD = WildcardClassType.WILDCARD;

    static <T> WildcardClassType<T> wildcard(Class<T> bound) {
        return WildcardClassType.getInstance(bound);
    }

    static <R, T> WildcardComponentRelationType<R, T> wildcardRelation(Class<R> relationshipBound, Class<T> targetBound) {
        return WildcardComponentRelationType.getInstance(relationshipBound, targetBound);
    }

    static <R> WildcardEntityRelationType<R> wildcardRelation(Class<R> relationshipBound) {
        return WildcardEntityRelationType.getInstance(relationshipBound);
    }

    static <R, T> WildcardEntityRelationFetchType<R, T> wildcardRelation(Class<R> relationshipBound, ComponentType<?, T> fetch) {
        return WildcardEntityRelationFetchType.getInstance(relationshipBound, fetch);
    }

}
