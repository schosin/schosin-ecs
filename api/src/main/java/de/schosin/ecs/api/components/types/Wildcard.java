package de.schosin.ecs.api.components.types;

import java.lang.reflect.Modifier;

import de.schosin.ecs.api.components.Result.ComponentResult;
import de.schosin.ecs.api.components.mappers.Components;

/**
 * Describes a wildcard component type. When reading components (e.g. with {@link Components#get(int)}) with this
 * type, the {@link ComponentResult} will contain all components assigned to the entity that are an instance of, 
 * or are assignable to {@link Wildcard#bound}. As such, using {@code Object.class} as a bound would match every
 * {@link ClassType} based component and allows to iterate through all these components of an entity.
 * 
 * @param <T> type of bound
 */
public record Wildcard<T>(Class<T> bound) implements ComponentType<T, ComponentResult<T>> {

    public static final Wildcard<Object> WILDCARD = new Wildcard<>(Object.class);

    public Wildcard {
        WildcardHelper.validateWildcard(bound);
    }

    @Override
    public boolean matches(ComponentType<?, ?> otherType) {
        return switch (otherType) {
            case ClassType<?> classType -> this.bound.isAssignableFrom(classType.clazz());
            case Wildcard<?> wildcard -> this.bound.isAssignableFrom(wildcard.bound());
            default -> false;
        };
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