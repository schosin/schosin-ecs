package de.schosin.ecs.api.components.types;

import java.lang.reflect.Modifier;

import de.schosin.ecs.api.components.Result.ComponentResult;

public record Wildcard<T>(Class<T> bound) implements ComponentType<T, ComponentResult<T>> {

    public static final Wildcard<Object> WILDCARD = new Wildcard<>(Object.class);

    public Wildcard {
        WildcardHelper.validateWildcard(bound);
    }

    @Override
    public final String toString() {
        return "Wildcard(%s)".formatted(bound.getSimpleName());
    }

}

class WildcardHelper extends ComponentTypeHelper {

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

}