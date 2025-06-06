package de.schosin.ecs.api.components.types;

import java.lang.reflect.Modifier;
import java.util.Set;

import de.schosin.ecs.api.components.ComponentSet;
import de.schosin.ecs.api.components.Relation;
import de.schosin.ecs.api.components.Relation.ComponentRelation;
import de.schosin.ecs.api.components.Relation.EntityRelation;
import de.schosin.ecs.api.components.Relation.Exclusive;
import de.schosin.ecs.api.components.Result.ComponentResult;
import de.schosin.ecs.api.components.mappers.Components;
import de.schosin.ecs.api.components.types.ComponentType.RegularComponentType;
import de.schosin.ecs.api.components.types.RelationComponentType.ComponentRelationType;
import de.schosin.ecs.api.components.types.RelationComponentType.EntityRelationType;
import de.schosin.ecs.api.components.types.RelationComponentType.ExclusiveComponentRelationType;
import de.schosin.ecs.api.components.types.RelationComponentType.ExclusiveEntityRelationType;
import de.schosin.ecs.api.components.types.RelationFetchType.EntityRelationFetchType;
import de.schosin.ecs.api.components.types.RelationFetchType.ExclusiveEntityRelationFetchType;
import de.schosin.ecs.api.components.types.WildcardRelationType.WildcardComponentRelationType;
import de.schosin.ecs.api.components.types.WildcardRelationType.WildcardEntityRelationFetchType;
import de.schosin.ecs.api.components.types.WildcardRelationType.WildcardEntityRelationType;
import de.schosin.ecs.api.data.DataProcessor;

/**
 * Interface to describe the supported component types.
 * 
 * <p>
 * A component type describes both the type of a single instance ({@code T}), as well as the type
 * when retrieving the component data for an entity ({@code R}).
 * </p>
 * 
 * <p>
 * By default only {@link RegularComponentType} components can be directly added to entities.
 * These types are intended to read, add and remove components from entities.
 * </p>
 * 
 * <p>
 * {@link ComponentType ComponentTypes} not extending {@link RegularComponentType} are intended to
 * provide additional capabilities when reading component data. {@link Wildcard} supports reading
 * a {@link ComponentResult} of components matching the {@link Wildcard#bound()}, {@link ComponentSetType}
 * supports retrieves a set of components as if it were a single component, and {@link RelationFetchType}
 * allows to fetch components of the target entity of the relation.
 * </p>
 * 
 * <p>
 * {@link ComponentType ComponentTypes} are bound to their {@link Components} counterpart. 
 * See the return type of a method in {@link Components.Creator} that accepts a particular {@link ComponentType}
 * to see the supported operations beyond what {@link Components} provide.
 * </p>
 * 
 * @param <T> type of a single component instance (write operations)
 * @param <R> type of component data when reading (read operations)
 */
public sealed interface ComponentType<T, R> permits RegularComponentType, Wildcard, ComponentSetType, RelationFetchType, WildcardRelationType, CustomComponentType {

    /**
     * Describes component types that can be directly assigned to entities.
     * 
     * @param <T> type of a single component instance
     * @param <R> type of component data when reading
     */
    sealed interface RegularComponentType<T, R> extends ComponentType<T, R> permits ClassType, RelationComponentType {

        /**
         * Check whether the component is a single instance of this type, matching the type parameter {@code T}.
         * 
         * @param component component to check
         * @return true, if the component is an instance of this type
         */
        boolean isInstance(Object component);

    }

    /**
     * Wildcard matching all {@link ClassType} components.
     */
    static Wildcard<Object> WILDCARD = Wildcard.WILDCARD;

    @SuppressWarnings({ "unchecked", "rawtypes" })
    static <T> RegularComponentType<T, ?> detectComponentType(T component) {
        return switch (component) {
            case null -> throw new IllegalArgumentException("Cannot get component type for null instance");
            case ComponentRelation<?, ?> relation -> Exclusive.class.isAssignableFrom(relation.relationship().getClass())
                    ? exclusiveRelation((Class) relation.relationship().getClass(), relation.target().getClass())
                    : relation((Class) relation.relationship().getClass(), relation.target().getClass());
            case EntityRelation<?> relation -> Exclusive.class.isAssignableFrom(relation.relationship().getClass())
                    ? exclusiveRelation((Class) relation.relationship().getClass())
                    : relation((Class) relation.relationship().getClass());
            default -> component((Class<T>) component.getClass());
        };
    }

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

    static <T extends ComponentSet<P>, P extends DataProcessor<T>> ComponentSetType<T, P> componentSet(Class<T> set, Class<P> processor) {
        return new ComponentSetType<>(set, processor);
    }

    static <T> Wildcard<T> wildcard(Class<T> bound) {
        return new Wildcard<>(bound);
    }

    static <R, T> WildcardComponentRelationType<R, T> wildcardRelation(Class<R> relationshipBound, Class<T> targetBound) {
        return new WildcardComponentRelationType<>(relationshipBound, targetBound);
    }

    static <R> WildcardEntityRelationType<R> wildcardRelation(Class<R> relationshipBound) {
        return new WildcardEntityRelationType<>(relationshipBound);
    }

    static <R, T> WildcardEntityRelationFetchType<R, T> wildcardRelation(Class<R> relationship, ComponentType<?, T> fetch) {
        return new WildcardEntityRelationFetchType<>(relationship, fetch);
    }

    /**
     * Returns whether this component type is equal to the other component type,
     * or is interested in the other component type.
     * 
     * <p>
     * {@link RegularComponentType Regular component types} should only implement this
     * by {@link Object#equals(Object)}. Wildcard types should return true for all
     * component types they match against.
     * </p>
     * 
     * @param otherType other component type
     * @return true if this is equal to or is interested in the other component type 
     */
    boolean matches(ComponentType<?, ?> otherType);

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