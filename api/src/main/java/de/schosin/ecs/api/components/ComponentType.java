package de.schosin.ecs.api.components;

import java.lang.reflect.Modifier;
import java.util.Set;

/**
 * Interface to describe the supported component types.
 * 
 * @param <T> type of component data
 */
public sealed interface ComponentType<T> {

    sealed interface RegularComponentType<T> extends ComponentType<T> {
    }

    static Wildcard<Object> WILDCARD = wildcard(Object.class);

    static <T> ClassType<T> component(Class<T> clazz) {
        return new ClassType<>(clazz);
    }

    static <T> Wildcard<T> wildcard(Class<T> bound) {
        return new Wildcard<>(bound);
    }

    /**
     * Describes a regular component based on a non-generic class.
     * 
     * @param <T> type of component
     */
    record ClassType<T>(Class<T> clazz) implements RegularComponentType<T> {
        public ClassType {
            if (ComponentTypeHelper.UNSUPPORTED_TYPES.contains(clazz) || Object.class == clazz) {
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

        @Override
        public final String toString() {
            return "ClassType(%s)".formatted(clazz.getName());
        }
    }

    record Wildcard<T>(Class<T> bound) implements ComponentType<Result<T>> {
        public Wildcard {
            if (ComponentTypeHelper.UNSUPPORTED_TYPES.contains(bound)) {
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

        @Override
        public final String toString() {
            return "Wildcard(%s)".formatted(bound.getName());
        }
    }

}

class ComponentTypeHelper {

    static final Set<Class<?>> UNSUPPORTED_TYPES = Set.of(
            boolean.class, byte.class, char.class, short.class, int.class, long.class, float.class, double.class,
            Boolean.class, Byte.class, Character.class, Short.class, Integer.class, Long.class, Float.class, Double.class,
            String.class);

}