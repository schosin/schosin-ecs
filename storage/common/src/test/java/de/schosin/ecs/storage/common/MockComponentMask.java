package de.schosin.ecs.storage.common;

import de.schosin.ecs.api.components.types.ComponentType.RegularComponentType;
import de.schosin.ecs.storage.api.components.Component;
import de.schosin.ecs.storage.api.entities.ComponentMask;
import de.schosin.ecs.utils.collections.ImmutableBag;

public record MockComponentMask(ImmutableBag<RegularComponentType<?, ?>> componentTypes) implements ComponentMask {

    @Override
    public int getId() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean containsComponent(int componentId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ImmutableBag<Component<?, ?>> getComponents() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ImmutableBag<RegularComponentType<?, ?>> getComponentTypes() {
        return componentTypes;
    }

}
