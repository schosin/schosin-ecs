package de.schosin.ecs.api.components.types;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import de.schosin.ecs.api.components.Relation;
import de.schosin.ecs.api.components.Relation.ComponentRelation;
import de.schosin.ecs.api.components.Relation.EntityRelation;
import de.schosin.ecs.api.components.Relation.Exclusive;
import de.schosin.ecs.api.components.Relations.ComponentRelations;
import de.schosin.ecs.api.components.Relations.EntityRelations;
import de.schosin.ecs.api.components.types.ComponentType.RegularComponentType;

/**
 * Describes relation components.
 * 
 * <ol>
 * <li><b>{@link ComponentRelationType}:</b> Describes a non-exclusive component relation</li>
 * <li><b>{@link ExclusiveComponentRelationType}:</b> Describes an exclusive component relation</li>
 * <li><b>{@link EntityRelationType}:</b> Describes a non-exclusive entity relation</li>
 * <li><b>{@link ExclusiveEntityRelationType}:</b> Describes an exclusive entity relation</li>
 * </ol>
 * 
 * @param <R> type of relationship component
 * @param <T> maps to {@link ComponentType} {@code T} (write operations)
 * @param <X> maps to {@link ComponentType} {@code R} (read operations)
 */
public sealed interface RelationComponentType<R, T extends Relation<R>, X> extends RegularComponentType<T, X> {

    Class<R> relationship();

    sealed interface RegularComponentRelationType<R, T, X> extends RelationComponentType<R, ComponentRelation<R, T>, X> {
        Class<T> target();
    }

    sealed interface RegularEntityRelationType<R, X> extends RelationComponentType<R, EntityRelation<R>, X> {
    }

    /**
     * Describes a component relation, consisting of a {@link ComponentRelationType#relationship relationship component}, 
     * and a {@link ComponentRelationType#target target component}.
     * 
     * <p>
     * An entity can have more than one instance of the same {@link ComponentRelationType}
     * as long as the relationship or target component differs.
     * </p>
     * 
     * @param <R> type of relationship component, must not extend {@link Relation.Exclusive}
     * @param <T> type of target component
     */
    record ComponentRelationType<R, T>(Class<R> relationship, Class<T> target) implements RegularComponentRelationType<R, T, ComponentRelations<R, T>> {

        private static final Map<Class<?>, Map<Class<?>, ComponentRelationType<?, ?>>> LOOKUP = new ConcurrentHashMap<>();

        @SuppressWarnings("unchecked")
        public static <R, T> ComponentRelationType<R, T> getInstance(Class<R> relationship, Class<T> target) {
            return (ComponentRelationType<R, T>) LOOKUP.computeIfAbsent(relationship, r -> new ConcurrentHashMap<>())
                    .computeIfAbsent(target, t -> new ComponentRelationType<>(relationship, target));
        }

        public ComponentRelationType {
            RelationComponentTypeHelper.validateNonExclusiveComponentRelationship(relationship);
            RelationComponentTypeHelper.validateComponentTarget(target);
        }

        @Override
        public boolean matches(RegularComponentType<?, ?> otherType) {
            return this.equals(otherType);
        }

        @Override
        public boolean isInstance(Object component) {
            return switch (component) {
                case ComponentRelation<?, ?> relation -> this.relationship.isInstance(relation.relationship()) && this.target.isInstance(relation.target());
                case ComponentRelations<?, ?> relations -> this.relationship.isInstance(relations.get(0).relationship()) && this.target.isInstance(relations.get(0).target());
                case null, default -> false;
            };
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

        private static final Map<Class<?>, Map<Class<?>, ExclusiveComponentRelationType<?, ?>>> LOOKUP = new ConcurrentHashMap<>();

        @SuppressWarnings("unchecked")
        public static <R extends Exclusive, T> ExclusiveComponentRelationType<R, T> getInstance(Class<R> relationship, Class<T> target) {
            return (ExclusiveComponentRelationType<R, T>) LOOKUP.computeIfAbsent(relationship, r -> new ConcurrentHashMap<>())
                    .computeIfAbsent(target, t -> new ExclusiveComponentRelationType<>(relationship, target));
        }

        public ExclusiveComponentRelationType {
            RelationComponentTypeHelper.validateComponentRelationship(relationship);
            RelationComponentTypeHelper.validateComponentTarget(target);
        }

        @Override
        public boolean matches(RegularComponentType<?, ?> otherType) {
            return this.equals(otherType);
        }

        @Override
        public boolean isInstance(Object component) {
            return component instanceof ComponentRelation<?, ?> relation && this.relationship.isInstance(relation.relationship()) && this.target.isInstance(relation.target());
        }

        @Override
        public final String toString() {
            return "ExclusiveComponentRelationType(%s / %s)".formatted(relationship.getSimpleName(), target.getSimpleName());
        }

    }

    /**
     * Describes an entity relation, consisting of a {@link EntityRelationType#relationship relationship component}.
     * 
     * <p>
     * An entity can have more than one instance of the same {@link EntityRelationType}
     * as long as the relationship component or target entity differs.
     * </p>
     * 
     * @param <R> type of relationship component, must not extend {@link Relation.Exclusive}
     */
    record EntityRelationType<R>(Class<R> relationship) implements RegularEntityRelationType<R, EntityRelations<R>> {

        private static final Map<Class<?>, EntityRelationType<?>> LOOKUP = new ConcurrentHashMap<>();

        @SuppressWarnings("unchecked")
        public static <R> EntityRelationType<R> getInstance(Class<R> relationship) {
            return (EntityRelationType<R>) LOOKUP.computeIfAbsent(relationship, r -> new EntityRelationType<>(relationship));
        }

        public EntityRelationType {
            RelationComponentTypeHelper.validateNonExclusiveEntityRelationship(relationship);
        }

        @Override
        public boolean matches(RegularComponentType<?, ?> otherType) {
            return this.equals(otherType);
        }

        @Override
        public boolean isInstance(Object component) {
            return switch (component) {
                case EntityRelation<?> relation -> this.relationship.isInstance(relation.relationship());
                case EntityRelations<?> relations -> this.relationship.isInstance(relations.get(0).relationship());
                case null, default -> false;
            };
        }

        @Override
        public final String toString() {
            return "EntityRelationType(%s)".formatted(relationship.getSimpleName());
        }

    }

    /**
     * Describes an exclusive entity relation, consisting of a relationship component.
     * 
     * @param <R> type of relationship component
     */
    record ExclusiveEntityRelationType<R extends Relation.Exclusive>(Class<R> relationship) implements RegularEntityRelationType<R, EntityRelation<R>> {

        private static final Map<Class<?>, ExclusiveEntityRelationType<?>> LOOKUP = new ConcurrentHashMap<>();

        @SuppressWarnings("unchecked")
        public static <R extends Exclusive> ExclusiveEntityRelationType<R> getInstance(Class<R> relationship) {
            return (ExclusiveEntityRelationType<R>) LOOKUP.computeIfAbsent(relationship, r -> new ExclusiveEntityRelationType<>(relationship));
        }

        public ExclusiveEntityRelationType {
            RelationComponentTypeHelper.validateEntityRelationship(relationship);
        }

        @Override
        public boolean matches(RegularComponentType<?, ?> otherType) {
            return this.equals(otherType);
        }

        @Override
        public boolean isInstance(Object component) {
            return component instanceof EntityRelation<?> relation && this.relationship.isInstance(relation.relationship());
        }

        @Override
        public final String toString() {
            return "ExclusiveEntityRelationType(%s)".formatted(relationship.getSimpleName());
        }

    }

}

class RelationComponentTypeHelper extends ComponentTypeHelper {

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

    static void validateTarget(Class<?> target) {
        validateComponent(target);

        if (Relation.Relationship.class.isAssignableFrom(target)) {
            throw new IllegalArgumentException("Class '%s' cannot be used as a target component. It is marked as a Relationship component.".formatted(target.getName()));
        }
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

}