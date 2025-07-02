package de.schosin.ecs.plugins.wildcards.types;

import java.lang.reflect.Modifier;

import de.schosin.ecs.api.components.Relation;
import de.schosin.ecs.api.components.Result;
import de.schosin.ecs.api.components.mappers.CustomComponentMapper;
import de.schosin.ecs.api.components.types.ComponentType;
import de.schosin.ecs.api.components.types.CustomComponentType;

/**
 * Describes relation components with wildcard types. Allows reading multiple types of relations that
 * match the given bounds. 
 * 
 * @param <R> type of relationship bound
 * @param <CT> maps to {@link ComponentType} {@code T} (write operations)
 * @param <CR> maps to {@link ComponentType} {@code R} (read operations)
 * @param <C> maps to {@link CustomComponentType} {@code C} (mapper type)
 */
public sealed interface WildcardRelationType<R, CT extends Relation<R>, CR extends Result<?>, C extends CustomComponentMapper<CT, CR>> extends WildcardType<CT, CR, C>
        permits WildcardComponentRelationType, WildcardEntityRelationType, WildcardEntityRelationFetchType {

    Class<R> relationshipBound();

}

class WildcardRelationTypeHelper extends WildcardHelper {

    static void validateWildcardComponentRelation(Class<?> relationshipBound, Class<?> targetBound) {
        validateWildcard(relationshipBound);
        validateWildcard(targetBound);

        if (Relation.Target.class.isAssignableFrom(relationshipBound)) {
            throw new IllegalArgumentException("Class '%s' cannot be used as a relationship bound. It is marked as a Target component.".formatted(relationshipBound.getName()));
        }
        if (Relation.EntityRelationship.class.isAssignableFrom(relationshipBound)) {
            throw new IllegalArgumentException("Class '%s' cannot be used for a component relation. It is marked as a entity relationship component.".formatted(relationshipBound.getName()));
        }

        if (Relation.Relationship.class.isAssignableFrom(targetBound)) {
            throw new IllegalArgumentException("Class '%s' cannot be used as a target bound. It is marked as a Relationship component.".formatted(targetBound.getName()));
        }

        if (Modifier.isFinal(relationshipBound.getModifiers()) && Modifier.isFinal(targetBound.getModifiers())) {
            throw new IllegalArgumentException("Relationship '%s' and target '%s' cannot be used as a component wildcard bounds. At one most may be final, but both were."
                    .formatted(relationshipBound.getName(), targetBound.getName()));
        }
    }

    static void validateWildcardEntityRelation(Class<?> relationshipBound) {
        validateWildcard(relationshipBound);

        if (Relation.Target.class.isAssignableFrom(relationshipBound)) {
            throw new IllegalArgumentException("Class '%s' cannot be used as a relationship bound. It is marked as a Target component.".formatted(relationshipBound.getName()));
        }

        if (Modifier.isFinal(relationshipBound.getModifiers())) {
            throw new IllegalArgumentException("Relationship '%s' cannot be used as a entity relationship bound. Must not be final.".formatted(relationshipBound.getName()));
        }
    }

    static void validateWildcard(Class<?> bound) {
        if (UNSUPPORTED_TYPES.contains(bound)) {
            throw new IllegalArgumentException("Class '%s' cannot be used as a wildcard.".formatted(bound.getName()));
        }
        if (bound.isArray()) {
            throw new IllegalArgumentException("Class '%s' cannot be used as a wildcard. Wildcards must not be arrays.".formatted(bound.getName()));
        }
        if (bound.isSynthetic()) {
            throw new IllegalArgumentException("Class '%s' cannot be used as a wildcard. Wildcards must not be synthetic.".formatted(bound.getName()));
        }
        if (bound.getTypeParameters().length > 0) {
            throw new IllegalArgumentException("Class '%s' cannot be used as a wildcard. Wildcards must not be generic.".formatted(bound.getName()));
        }
    }

}