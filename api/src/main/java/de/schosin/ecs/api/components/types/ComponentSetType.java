package de.schosin.ecs.api.components.types;

import de.schosin.ecs.api.components.ComponentSet;

public record ComponentSetType<T extends ComponentSet>(Class<T> componentSet) implements ComponentType<T, T> {

    public ComponentSetType {
        ComponentSetTypeHelper.validateComponentSet(componentSet);
    }

    @Override
    public boolean matches(ComponentType<?, ?> otherType) {
        return this.equals(otherType);
    }

    @Override
    public final String toString() {
        return "ComponentSetType(%s)".formatted(componentSet.getSimpleName());
    }

}

class ComponentSetTypeHelper extends ComponentTypeHelper {

    public static void validateComponentSet(Class<?> componentSet) {
        if (!componentSet.isInterface()) {
            throw new IllegalArgumentException("Class '%s' cannot be used as a component set. Component sets must be interfaces.".formatted(componentSet.getName()));
        }
    }

}
