package de.schosin.ecs.utils;

import java.util.Objects;

import org.jspecify.annotations.NullMarked;

import de.schosin.ecs.api.components.ComponentType;
import de.schosin.ecs.api.components.ComponentType.ClassType;
import de.schosin.ecs.api.components.ComponentType.ComponentRelationType;
import de.schosin.ecs.api.components.ComponentType.EntityRelationType;
import de.schosin.ecs.api.components.ComponentType.ExclusiveComponentRelationType;
import de.schosin.ecs.api.components.ComponentType.ExclusiveEntityRelationType;
import de.schosin.ecs.api.components.ComponentType.RegularComponentType;
import de.schosin.ecs.api.components.ComponentType.Wildcard;

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
    public static boolean matches(ComponentType<?, ?> type, ComponentType<?, ?> otherType) {
        Objects.requireNonNull(type, "type cannot be null");
        Objects.requireNonNull(otherType, "otherType cannot be null");

        return switch (otherType) {
            case RegularComponentType<?, ?> otherRegular -> matches(type, otherRegular);
            case Wildcard<?> otherWildcard -> switch (type) {
                case ClassType<?> classType -> classType.clazz().isAssignableFrom(otherWildcard.bound());
                case Wildcard<?> wildcard -> wildcard.bound().isAssignableFrom(otherWildcard.bound());
                default -> false;
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
    public static boolean matches(ComponentType<?, ?> type, RegularComponentType<?, ?> otherType) {
        Objects.requireNonNull(type, "type cannot be null");
        Objects.requireNonNull(otherType, "otherType cannot be null");

        return switch (type) {
            case RegularComponentType<?, ?> regular -> regular.equals(otherType);
            // No record pattern for Wildcard: https://github.com/eclipse-jdt/eclipse.jdt.core/issues/4002
            case Wildcard<?> wildcard -> switch (otherType) {
                case ClassType<?> classType -> wildcard.bound().isAssignableFrom(classType.clazz());
                case ComponentRelationType<?, ?> otherRelation -> false;
                case ExclusiveComponentRelationType<?, ?> otherRelation -> false;
                case EntityRelationType<?> otherRelation -> false;
                case ExclusiveEntityRelationType<?> otherRelation -> false;
            };
        };
    }

}
