package de.schosin.ecs.utils;

import org.jspecify.annotations.NullMarked;

import de.schosin.ecs.api.components.ComponentType;
import de.schosin.ecs.api.components.ComponentType.RegularComponentType;

@NullMarked
public class ComponentUtils {

    /**
     * Tests whether the given {@link ComponentType} {@code other} matches
     * {@code type}. 
     * 
     * <p>
     * In the case of {@link RegularComponentType} they will be tested for equality, 
     * when {@code type} contains wildcards, the bounds of the wildcards will be tested
     * via {@link Class#isAssignableFrom(Class)} against the {@code other} component.
     * </p>
     *  
     * @param type type to test
     * @param otherType possible match
     * @return true if {@code other} matches {@code type}
     */
    public static boolean matches(ComponentType<?> type, ComponentType<?> otherType) {
        return switch (otherType) {
            case ComponentType.RegularComponentType<?> otherRegular -> matches(type, otherRegular);
            case ComponentType.Wildcard<?> otherWildcard -> switch (type) {
                case ComponentType.RegularComponentType<?> regular -> switch (regular) {
                    case ComponentType.ClassType(var clazz) -> clazz.isAssignableFrom(otherWildcard.bound());
                };
                case ComponentType.Wildcard<?> wildcard -> wildcard.bound().isAssignableFrom(otherWildcard.bound());
            };
        };
    }

    /**
     * Tests whether the given {@link ComponentType} {@code other} matches
     * {@code type}. 
     * 
     * <p>
     * In the case of {@link RegularComponentType} they will be tested for equality, 
     * when {@code type} contains wildcards, the bounds of the wildcards will test 
     * whether they include {@code otherType}
     * </p>
     *  
     * @param type type to test
     * @param otherType possible match
     * @return true if {@code other} matches {@code type}
     */
    public static boolean matches(ComponentType<?> type, RegularComponentType<?> otherType) {
        return switch (type) {
            case ComponentType.RegularComponentType<?> regular -> switch (regular) {
                case ComponentType.ClassType(var clazz) -> switch (otherType) {
                    case ComponentType.ClassType(var otherClazz) -> clazz == otherClazz;
                };
            };
            // No record pattern for Wildcard: https://github.com/eclipse-jdt/eclipse.jdt.core/issues/4002
            case ComponentType.Wildcard<?> wildcard -> switch (otherType) {
                case ComponentType.ClassType(var otherClazz) -> wildcard.bound().isAssignableFrom(otherClazz);
            };
        };
    }

}
