package de.schosin.ecs.api.components.types;

import de.schosin.ecs.api.components.Relation;
import de.schosin.ecs.api.components.Relation.ComponentRelation;
import de.schosin.ecs.api.components.Relation.EntityRelation;
import de.schosin.ecs.api.components.Result.ComponentRelationResult;
import de.schosin.ecs.api.components.Result.EntityRelationResult;
import de.schosin.ecs.api.components.types.ComponentType.RegularComponentType;

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
     * as long as the relationship component differs.
     * </p>
     * 
     * @param <R> type of relationship component, must not extend {@link Relation.Exclusive}
     * @param <T> type of target component
     */
    record ComponentRelationType<R, T>(Class<R> relationship, Class<T> target) implements RegularComponentRelationType<R, T, ComponentRelationResult<R, T>> {
        public ComponentRelationType {
            RelationComponentTypeHelper.validateNonExclusiveComponentRelationship(relationship);
            RelationComponentTypeHelper.validateComponentTarget(target);
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
            RelationComponentTypeHelper.validateComponentRelationship(relationship);
            RelationComponentTypeHelper.validateComponentTarget(target);
        }

        @Override
        public final String toString() {
            return "ExclusiveComponentRelationType(%s / %s)".formatted(relationship.getSimpleName(), target.getSimpleName());
        }
    }

    record EntityRelationType<R>(Class<R> relationship) implements RegularEntityRelationType<R, EntityRelationResult<R>> {
        public EntityRelationType {
            RelationComponentTypeHelper.validateNonExclusiveEntityRelationship(relationship);
        }

        @Override
        public final String toString() {
            return "EntityRelationType(%s)".formatted(relationship.getSimpleName());
        }
    }

    record ExclusiveEntityRelationType<R extends Relation.Exclusive>(Class<R> relationship) implements RegularEntityRelationType<R, EntityRelation<R>> {
        public ExclusiveEntityRelationType {
            RelationComponentTypeHelper.validateEntityRelationship(relationship);
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