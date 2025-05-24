package de.schosin.ecs.api.components.types;

import de.schosin.ecs.api.components.Relation;
import de.schosin.ecs.api.components.types.ComponentType.RegularComponentType;

public record ClassType<T>(Class<T> clazz) implements RegularComponentType<T, T> {

    public ClassType {
        ClassTypeHelper.validateClassType(clazz);
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
