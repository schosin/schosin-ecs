package de.schosin.ecs.api.components;

import java.lang.reflect.Modifier;
import java.util.Set;

import de.schosin.ecs.api.components.Relation.ComponentRelation;
import de.schosin.ecs.api.components.Relation.EntityRelation;
import de.schosin.ecs.api.components.Result.ComponentRelationResult;
import de.schosin.ecs.api.components.Result.ComponentResult;
import de.schosin.ecs.api.components.Result.EntityRelationResult;

/**
 * Interface to describe the supported component types.
 * 
 * @param <T> type of component data
 * @param <R> type of result when retrieving component data for an entity
 */
public sealed interface ComponentType<T, R> {

    sealed interface RegularComponentType<T, R> extends ComponentType<T, R> {
    }

    sealed interface RelationComponentType<R, T extends Relation<R>, X> extends RegularComponentType<T, X> {
        Class<R> relationship();
    }

    sealed interface RegularComponentRelationType<R, T, X> extends RelationComponentType<R, ComponentRelation<R, T>, X> {
        Class<T> target();
    }

    sealed interface RegularEntityRelationType<R, X> extends RelationComponentType<R, EntityRelation<R>, X> {
    }

    static Wildcard<Object> WILDCARD = wildcard(Object.class);

    static <T> ClassType<T> component(Class<T> clazz) {
        return new ClassType<>(clazz);
    }

    static <R, T> ComponentRelationType<R, T> relation(Class<R> relationship, Class<T> target) {
        return new ComponentRelationType<>(relationship, target);
    }

    static <R extends Relation.Exclusive, T> ExclusiveComponentRelationType<R, T> exclusiveRelation(Class<R> relationship, Class<T> target) {
        return new ExclusiveComponentRelationType<>(relationship, target);
    }

    static <R> EntityRelationType<R> relation(Class<R> relationship) {
        return new EntityRelationType<>(relationship);
    }

    static <R extends Relation.Exclusive> ExclusiveEntityRelationType<R> exclusiveRelation(Class<R> relationship) {
        return new ExclusiveEntityRelationType<>(relationship);
    }

    static <T> Wildcard<T> wildcard(Class<T> bound) {
        return new Wildcard<>(bound);
    }

    /**
     * Describes a regular component based on a non-generic class.
     * 
     * @param <T> type of component
     */
    record ClassType<T>(Class<T> clazz) implements RegularComponentType<T, T> {
        public ClassType {
            ComponentTypeHelper.validateClassType(clazz);
        }

        @Override
        public final String toString() {
            return "ClassType(%s)".formatted(clazz.getSimpleName());
        }
    }

    /**
     * Describes a component relation, consisting of a {@link ComponentRelationType#relationship relationship component}, 
     * and a {@link ComponentRelationType#target target component}.
     * 
     * <p>
     * An entity can have more than one instance of the same {@link ComponentRelationType}
     * as long as the relationship component differs.
     * </p>
     * 
     * @param <R> type of relationship component, must not extend {@link Relation.Exclusive}
     * @param <T> type of target component
     */
    record ComponentRelationType<R, T>(Class<R> relationship, Class<T> target) implements RegularComponentRelationType<R, T, ComponentRelationResult<R, T>> {
        public ComponentRelationType {
            ComponentTypeHelper.validateNonExclusiveComponentRelationship(relationship);
            ComponentTypeHelper.validateComponentTarget(target);
        }

        @Override
        public final String toString() {
            return "ComponentRelationType(%s / %s)".formatted(relationship.getSimpleName(), target.getSimpleName());
        }
    }

    /**
     * Describes an exclusive component relation, consisting of a relationship component, and a target component.
     * 
     * @param <R> type of relationship component
     * @param <T> type of target component
     */
    record ExclusiveComponentRelationType<R extends Relation.Exclusive, T>(Class<R> relationship, Class<T> target) implements RegularComponentRelationType<R, T, ComponentRelation<R, T>> {
        public ExclusiveComponentRelationType {
            ComponentTypeHelper.validateComponentRelationship(relationship);
            ComponentTypeHelper.validateComponentTarget(target);
        }

        @Override
        public final String toString() {
            return "ExclusiveComponentRelationType(%s / %s)".formatted(relationship.getSimpleName(), target.getSimpleName());
        }
    }

    record EntityRelationType<R>(Class<R> relationship) implements RegularEntityRelationType<R, EntityRelationResult<R>> {
        public EntityRelationType {
            ComponentTypeHelper.validateNonExclusiveEntityRelationship(relationship);
        }

        @Override
        public final String toString() {
            return "EntityRelationType(%s)".formatted(relationship.getSimpleName());
        }
    }

    record ExclusiveEntityRelationType<R extends Relation.Exclusive>(Class<R> relationship) implements RegularEntityRelationType<R, EntityRelation<R>> {
        public ExclusiveEntityRelationType {
            ComponentTypeHelper.validateEntityRelationship(relationship);
        }

        @Override
        public final String toString() {
            return "ExclusiveEntityRelationType(%s)".formatted(relationship.getSimpleName());
        }
    }

    record Wildcard<T>(Class<T> bound) implements ComponentType<T, ComponentResult<T>> {
        public Wildcard {
            ComponentTypeHelper.validateWildcard(bound);
        }

        @Override
        public final String toString() {
            return "Wildcard(%s)".formatted(bound.getSimpleName());
        }
    }

}

class ComponentTypeHelper {

    static final Set<Class<?>> UNSUPPORTED_TYPES = Set.of(
            boolean.class, byte.class, char.class, short.class, int.class, long.class, float.class, double.class,
            Boolean.class, Byte.class, Character.class, Short.class, Integer.class, Long.class, Float.class, Double.class,
            String.class);

    static void validateClassType(Class<?> clazz) {
        validateComponent(clazz);

        if (Relation.Relationship.class.isAssignableFrom(clazz)) {
            throw new IllegalArgumentException("Class '%s' cannot be used as a class component. It is marked as a relationship component.".formatted(clazz.getName()));
        }
        if (Relation.Target.class.isAssignableFrom(clazz)) {
            throw new IllegalArgumentException("Class '%s' cannot be used as a class component. It is marked as a target component.".formatted(clazz.getName()));
        }
    }

    static void validateNonExclusiveComponentRelationship(Class<?> relationship) {
        validateComponentRelationship(relationship);

        if (Relation.Exclusive.class.isAssignableFrom(relationship)) {
            throw new IllegalArgumentException("Class '%s' cannot be used as a non-exclusive relationship component. It is marked as a Exclusive component. Use #exclusiveRelation instead."
                    .formatted(relationship.getName()));
        }
    }

    static void validateComponentRelationship(Class<?> relationship) {
        validateRelationship(relationship);

        if (Relation.EntityRelationship.class.isAssignableFrom(relationship)) {
            throw new IllegalArgumentException("Class '%s' cannot be used for a component relation. It is marked as a entity relationship component.".formatted(relationship.getName()));
        }
    }

    static void validateComponentTarget(Class<?> target) {
        validateTarget(target);
    }

    static void validateNonExclusiveEntityRelationship(Class<?> relationship) {
        validateEntityRelationship(relationship);

        if (Relation.Exclusive.class.isAssignableFrom(relationship)) {
            throw new IllegalArgumentException("Class '%s' cannot be used as a non-exclusive relationship component. It is marked as a Exclusive component. Use #exclusiveRelation instead."
                    .formatted(relationship.getName()));
        }
    }

    static void validateEntityRelationship(Class<?> relationship) {
        validateRelationship(relationship);
    }

    static void validateRelationship(Class<?> relationship) {
        validateComponent(relationship);

        if (Relation.Target.class.isAssignableFrom(relationship)) {
            throw new IllegalArgumentException("Class '%s' cannot be used as a relationship component. It is marked as a Target component.".formatted(relationship.getName()));
        }
    }

    static void validateTarget(Class<?> target) {
        validateComponent(target);

        if (Relation.Relationship.class.isAssignableFrom(target)) {
            throw new IllegalArgumentException("Class '%s' cannot be used as a target component. It is marked as a Relationship component.".formatted(target.getName()));
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
        if (Modifier.isFinal(bound.getModifiers())) {
            throw new IllegalArgumentException("Class '%s' cannot be used as a wildcard. Wildcards must not be final.".formatted(bound.getName()));
        }
        if (bound.getTypeParameters().length > 0) {
            throw new IllegalArgumentException("Class '%s' cannot be used as a wildcard. Wildcards must not be generic.".formatted(bound.getName()));
        }
    }

    static void validateComponent(Class<?> clazz) {
        if (UNSUPPORTED_TYPES.contains(clazz) || Object.class == clazz) {
            throw new IllegalArgumentException("Class '%s' cannot be used as a component.".formatted(clazz.getName()));
        }
        if (clazz.isArray()) {
            throw new IllegalArgumentException("Class '%s' cannot be used as a component. Components must not be arrays.".formatted(clazz.getName()));
        }
        if (clazz.isSynthetic()) {
            throw new IllegalArgumentException("Class '%s' cannot be used as a component. Components must not be synthetic.".formatted(clazz.getName()));
        }
        if (clazz.isInterface() || Modifier.isAbstract(clazz.getModifiers())) {
            throw new IllegalArgumentException("Class '%s' cannot be used as a component. Components must not be abstract.".formatted(clazz.getName()));
        }
        if (clazz.getTypeParameters().length > 0) {
            throw new IllegalArgumentException("Class '%s' cannot be used as a component. Components must not be generic.".formatted(clazz.getName()));
        }
    }

}