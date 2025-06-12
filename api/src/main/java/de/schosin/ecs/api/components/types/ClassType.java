package de.schosin.ecs.api.components.types;

import de.schosin.ecs.api.components.Relation;
import de.schosin.ecs.api.components.types.ComponentType.RegularComponentType;

/**
 * Describes a regular POJO component given its {@link Class} object.
 * 
 * <ol>
 * <li>The component must be a non-generic, non-abstract POJO</li>
 * <li>The component must not be {@link String}, a primitive type or their wrapper types</li>
 * <li>The component must not implement {@link Relation.Relationship}</li>
 * <li>The component must not implement {@link Relation.Target}</li>
 * </ol>
 * 
 * @param <T> type of component
 */
public record ClassType<T>(Class<T> clazz) implements RegularComponentType<T, T> {

    public ClassType {
        ClassTypeHelper.validateClassType(clazz);
    }

    @Override
    public boolean matches(RegularComponentType<?, ?> otherType) {
        return this.equals(otherType);
    }

    @Override
    public boolean isInstance(Object component) {
        return clazz.isInstance(component);
    }

    @Override
    public final String toString() {
        return "ClassType(%s)".formatted(clazz.getSimpleName());
    }

}

class ClassTypeHelper extends ComponentTypeHelper {

    static void validateClassType(Class<?> clazz) {
        validateComponent(clazz);

        if (Relation.Relationship.class.isAssignableFrom(clazz)) {
            throw new IllegalArgumentException("Class '%s' cannot be used as a class component. It is marked as a relationship component.".formatted(clazz.getName()));
        }
        if (Relation.Target.class.isAssignableFrom(clazz)) {
            throw new IllegalArgumentException("Class '%s' cannot be used as a class component. It is marked as a target component.".formatted(clazz.getName()));
        }
    }

}
