package de.schosin.ecs.api.components.types;

import de.schosin.ecs.api.components.ComponentSet;
import de.schosin.ecs.api.data.DataProcessor;

/**
 * Describes a {@link ComponentSet} component.
 * 
 * @param <T> type of component set
 */
public record ComponentSetType<T extends ComponentSet<P>, P extends DataProcessor<T>>(Class<T> componentSet, Class<P> processor) implements ComponentType<T, T> {

    public ComponentSetType {
        ComponentSetTypeHelper.validateComponentSet(componentSet);
    }

    @Override
    public boolean matches(RegularComponentType<?, ?> otherType) {
        return false;
    }

    @Override
    public final String toString() {
        return "ComponentSetType(%s)".formatted(componentSet.getSimpleName());
    }

}

class ComponentSetTypeHelper extends ComponentTypeHelper {

    public static void validateComponentSet(Class<?> componentSet) {
        if (!componentSet.isInterface()) {
            throw new IllegalArgumentException("Class '%s' cannot be used as a component set. RegularComponent sets must be interfaces.".formatted(componentSet.getName()));
        }
    }

}
