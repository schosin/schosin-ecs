package de.schosin.ecs.api.components.types;

import java.lang.reflect.Modifier;
import java.util.Set;

import de.schosin.ecs.api.components.ComponentSet;
import de.schosin.ecs.api.components.Relation;
import de.schosin.ecs.api.components.types.ComponentType.RegularComponentType;
import de.schosin.ecs.api.components.types.RelationComponentType.ComponentRelationType;
import de.schosin.ecs.api.components.types.RelationComponentType.EntityRelationType;
import de.schosin.ecs.api.components.types.RelationComponentType.ExclusiveComponentRelationType;
import de.schosin.ecs.api.components.types.RelationComponentType.ExclusiveEntityRelationType;
import de.schosin.ecs.api.components.types.RelationFetchType.EntityRelationFetchType;
import de.schosin.ecs.api.components.types.RelationFetchType.ExclusiveEntityRelationFetchType;

/**
 * Interface to describe the supported component types.
 * 
 * @param <T> type of component data
 * @param <R> type of result when retrieving component data for an entity
 */
public sealed interface ComponentType<T, R> permits RegularComponentType, Wildcard, ComponentSetType, RelationFetchType {

    sealed interface RegularComponentType<T, R> extends ComponentType<T, R> permits ClassType, RelationComponentType {
    }

    static Wildcard<Object> WILDCARD = Wildcard.WILDCARD;

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

    static <R, T> EntityRelationFetchType<R, T> relation(Class<R> relationship, ComponentType<?, T> fetch) {
        return new EntityRelationFetchType<>(relationship, fetch);
    }

    static <R extends Relation.Exclusive, T> ExclusiveEntityRelationFetchType<R, T> exclusiveRelation(Class<R> relationship, ComponentType<?, T> fetch) {
        return new ExclusiveEntityRelationFetchType<>(relationship, fetch);
    }

    static <T extends ComponentSet> ComponentSetType<T> componentSet(Class<T> set) {
        return new ComponentSetType<>(set);
    }

    static <T> Wildcard<T> wildcard(Class<T> bound) {
        return new Wildcard<>(bound);
    }

}

class ComponentTypeHelper {

    static final Set<Class<?>> UNSUPPORTED_TYPES = Set.of(
            boolean.class, byte.class, char.class, short.class, int.class, long.class, float.class, double.class,
            Boolean.class, Byte.class, Character.class, Short.class, Integer.class, Long.class, Float.class, Double.class,
            String.class);

    static void validateComponent(Class<?> clazz) {
        if (UNSUPPORTED_TYPES.contains(clazz) || Object.class == clazz) {
            throw new IllegalArgumentException("Class '%s' cannot be used as a component.".formatted(clazz.getName()));
        }
        if (ComponentSet.class.isAssignableFrom(clazz)) {
            throw new IllegalArgumentException("Class '%s' cannot be used as a component. Components must not be component sets.".formatted(clazz.getName()));
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