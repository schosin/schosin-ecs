package de.schosin.ecs.api.components.types;

import java.lang.reflect.Modifier;

import de.schosin.ecs.api.components.Relation;
import de.schosin.ecs.api.components.Result;
import de.schosin.ecs.api.components.Result.ComponentRelationResult;
import de.schosin.ecs.api.components.Result.EntityRelationDataResult;
import de.schosin.ecs.api.components.Result.EntityRelationResult;
import de.schosin.ecs.api.components.mappers.EntityFetchRelations;
import de.schosin.ecs.api.components.types.RelationComponentType.ComponentRelationType;
import de.schosin.ecs.api.components.types.RelationComponentType.EntityRelationType;
import de.schosin.ecs.api.components.types.RelationComponentType.ExclusiveComponentRelationType;
import de.schosin.ecs.api.components.types.RelationComponentType.ExclusiveEntityRelationType;

/**
 * Describes relation components with wildcard types. Allows reading multiple types of relations that
 * match the given bounds. 
 * 
 * @param <R> type of relationship bound
 * @param <T> maps to {@link ComponentType} {@code R} (read operations)
 */
public sealed interface WildcardRelationType<R, T extends Result<?>> extends ComponentType<R, T> {

    Class<R> relationshipBound();

    /**
     * Describes a component relation wildcard type. Allows to read all component relations where both the relationship
     * and target are assignable to the respective bounds.
     * 
     * <p>
     * When using {@code Object.class} for both bounds, all component relations (non-exclusive and exclusive) of an entity can be read.
     * </p>
     * 
     * @param <R> type of relationship component bound
     * @param <T> type of target component bound
     */
    record WildcardComponentRelationType<R, T>(Class<R> relationshipBound, Class<T> targetBound) implements WildcardRelationType<R, ComponentRelationResult<R, T>> {
        public WildcardComponentRelationType {
            WildcardRelationTypeHelper.validateWildcardComponentRelation(relationshipBound, targetBound);
        }

        @Override
        public boolean matches(ComponentType<?, ?> otherType) {
            return switch (otherType) {
                case ComponentRelationType<?, ?> relation -> this.relationshipBound.isAssignableFrom(relation.relationship()) && this.targetBound.isAssignableFrom(relation.target());
                case ExclusiveComponentRelationType<?, ?> relation -> this.relationshipBound.isAssignableFrom(relation.relationship()) && this.targetBound.isAssignableFrom(relation.target());
                case WildcardComponentRelationType<?, ?> wildcard -> this.relationshipBound.isAssignableFrom(wildcard.relationshipBound()) && this.targetBound.isAssignableFrom(wildcard.targetBound());
                default -> false;
            };
        }

        @Override
        public final String toString() {
            return "WildcardComponentRelationType(%s / %s)".formatted(relationshipBound.getSimpleName(), targetBound.getSimpleName());
        }
    }

    /**
     * Describes an entity relation wildcard type. Allows to read all entity relations where the relationship
     * is assignable to the bound. 
     * 
     * <p>
     * When using {@code Object.class} as the bound, all entity relations (non-exclusive and exclusive) of an entity can be read.
     * </p>
     * 
     * @param <R> type of relationship bound
     */
    record WildcardEntityRelationType<R>(Class<R> relationshipBound) implements WildcardRelationType<R, EntityRelationResult<R>> {
        public WildcardEntityRelationType {
            WildcardRelationTypeHelper.validateWildcardEntityRelation(relationshipBound);
        }

        @Override
        public boolean matches(ComponentType<?, ?> otherType) {
            return switch (otherType) {
                case EntityRelationType<?> relation -> this.relationshipBound.isAssignableFrom(relation.relationship());
                case ExclusiveEntityRelationType<?> relation -> this.relationshipBound.isAssignableFrom(relation.relationship());
                case WildcardEntityRelationType<?> wildcard -> this.relationshipBound.isAssignableFrom(wildcard.relationshipBound());
                default -> false;
            };
        }

        @Override
        public final String toString() {
            return "WildcardEntityRelationType(%s)".formatted(relationshipBound.getSimpleName());
        }
    }

    /**
     * Describes an entity relation wildcard type similarly to {@link WildcardEntityRelationType}, but also allows to fetch data for
     * the target entities in the same way as {@link EntityFetchRelations} does.
     * 
     * @param <R> type of relationship bound
     * @param <T> type of fetched data for target entities
     */
    record WildcardEntityRelationFetchType<R, T>(Class<R> relationshipBound, ComponentType<?, T> fetch) implements WildcardRelationType<R, EntityRelationDataResult<R, T>> {
        public WildcardEntityRelationFetchType {
            WildcardRelationTypeHelper.validateWildcardEntityRelation(relationshipBound);
        }

        @Override
        public boolean matches(ComponentType<?, ?> otherType) {
            return switch (otherType) {
                case WildcardEntityRelationFetchType<?, ?> wildcard -> this.relationshipBound.isAssignableFrom(wildcard.relationshipBound()) & this.fetch.equals(wildcard.fetch);
                default -> false;
            };
        }

        @Override
        public final String toString() {
            return "WildcardEntityRelationFetchType(%s -> %s)".formatted(relationshipBound.getSimpleName(), fetch);
        }
    }

}

class WildcardRelationTypeHelper extends ComponentTypeHelper {

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

    static void validateRelationshipWildcard(Class<?> relationshipBound) {
        WildcardHelper.validateWildcard(relationshipBound);

        if (Relation.Target.class.isAssignableFrom(relationshipBound)) {
            throw new IllegalArgumentException("Class '%s' cannot be used as a relationship bound. It is marked as a Target component.".formatted(relationshipBound.getName()));
        }
    }

}