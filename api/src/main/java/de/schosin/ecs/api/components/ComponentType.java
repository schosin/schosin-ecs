package de.schosin.ecs.api.components;

import de.schosin.ecs.api.components.ComponentType.RegularComponentType;

/**
 * Interface to describe the supported component types.
 * 
 * @param <T> type of component data
 */
public sealed interface ComponentType<T> permits RegularComponentType {

    sealed interface RegularComponentType<T> extends ComponentType<T> {
    }

    static <T> ClassType<T> component(Class<T> clazz) {
        return new ClassType<>(clazz);
    }

    /**
     * Describes a regular component based on a non-generic class.
     * 
     * @param <T> type of component
     */
    record ClassType<T>(Class<T> clazz) implements RegularComponentType<T> {
        public ClassType {
            if (clazz.getTypeParameters().length > 0) {
                throw new IllegalArgumentException("Class '%s' cannot be used as a component. Components must not be generic.".formatted(clazz.getName()));
            }
        }

        @Override
        public final String toString() {
            return "ClassType(%s)".formatted(clazz.getName());
        }
    }

}
