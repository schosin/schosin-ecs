package de.schosin.ecs.plugins.wildcards.types;

import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import de.schosin.ecs.api.components.mappers.Components;
import de.schosin.ecs.api.components.types.ClassType;
import de.schosin.ecs.plugins.wildcards.mappers.WildcardClassMapper;
import de.schosin.ecs.plugins.wildcards.result.WildcardResult;

/**
 * Describes a wildcard component type. When reading components (e.g. with {@link Components#get(int)}) with this
 * type, the {@link WildcardResult} will contain all components assigned to the entity that are an instance of, 
 * or are assignable to {@link WildcardClassType#bound}. As such, using {@code Object.class} as a bound would match every
 * {@link ClassType} based component and allows to iterate through all these components of an entity.
 * 
 * @param <T> type of bound
 */
public record WildcardClassType<T>(Class<T> bound) implements WildcardType<T, WildcardResult<T>, WildcardClassMapper<T>> {

    public static final WildcardClassType<Object> WILDCARD = new WildcardClassType<>(Object.class);

    private static final Map<Class<?>, WildcardClassType<?>> LOOKUP = new ConcurrentHashMap<>();

    @SuppressWarnings("unchecked")
    public static <T> WildcardClassType<T> getInstance(Class<T> bound) {
        return (WildcardClassType<T>) LOOKUP.computeIfAbsent(bound, WildcardClassType::new);
    }

    public WildcardClassType {
        WildcardHelper.validateWildcard(bound);
    }

    @Override
    public boolean matches(RegularComponentType<?, ?> otherType) {
        return otherType instanceof ClassType<?> classType && this.bound.isAssignableFrom(classType.clazz());
    }

    @Override
    public final String toString() {
        return "WildcardClassType(%s)".formatted(bound.getSimpleName());
    }

}

class WildcardHelper {

    // matches package-prive ComponentTypeHelper from api module
    static final Set<Class<?>> UNSUPPORTED_TYPES = Set.of(
            boolean.class, byte.class, char.class, short.class, int.class, long.class, float.class, double.class,
            Boolean.class, Byte.class, Character.class, Short.class, Integer.class, Long.class, Float.class, Double.class,
            String.class);

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