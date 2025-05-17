package de.schosin.ecs.utils;

import org.jspecify.annotations.NullMarked;

import de.schosin.ecs.api.components.ComponentType;
import de.schosin.ecs.api.components.ComponentType.ClassType;
import de.schosin.ecs.api.components.ComponentType.ComponentRelationType;
import de.schosin.ecs.api.components.ComponentType.ExclusiveComponentRelationType;
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
        return switch (otherType) {
            case RegularComponentType<?, ?> otherRegular -> matches(type, otherRegular);
            case Wildcard<?> otherWildcard -> switch (type) {
                case RegularComponentType<?, ?> regular -> switch (regular) {
                    case ClassType<?> classType -> classType.clazz().isAssignableFrom(otherWildcard.bound());
                    case ComponentRelationType<?, ?> relation -> false;
                    case ExclusiveComponentRelationType<?, ?> relation -> false;
                };
                case Wildcard<?> wildcard -> wildcard.bound().isAssignableFrom(otherWildcard.bound());
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
        return switch (type) {
            case RegularComponentType<?, ?> regular -> switch (regular) {
                case ClassType<?> classType -> switch (otherType) {
                    case ClassType<?> otherClassType -> classType.clazz() == otherClassType.clazz();
                    case ComponentRelationType<?, ?> otherRelation -> false;
                    case ExclusiveComponentRelationType<?, ?> otherRelation -> false;
                };
                case ComponentRelationType<?, ?> relation -> switch (otherType) {
                    case ClassType<?> otherClass -> false;
                    case ComponentRelationType<?, ?> otherRelation -> relation.relationship() == otherRelation.relationship() && relation.target() == otherRelation.target();
                    case ExclusiveComponentRelationType<?, ?> otherRelation -> false;
                };
                case ExclusiveComponentRelationType<?, ?> relation -> switch (otherType) {
                    case ClassType<?> otherClass -> false;
                    case ComponentRelationType<?, ?> otherRelation -> false;
                    case ExclusiveComponentRelationType<?, ?> otherRelation -> relation.relationship() == otherRelation.relationship() && relation.target() == otherRelation.target();
                };
            };
            // No record pattern for Wildcard: https://github.com/eclipse-jdt/eclipse.jdt.core/issues/4002
            case Wildcard<?> wildcard -> switch (otherType) {
                case ClassType<?> classType -> wildcard.bound().isAssignableFrom(classType.clazz());
                case ComponentRelationType<?, ?> otherRelation -> false;
                case ExclusiveComponentRelationType<?, ?> otherRelation -> false;
            };
        };
    }

}
