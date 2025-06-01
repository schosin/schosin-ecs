package de.schosin.ecs.storage.api.entities;

import de.schosin.ecs.api.components.types.ComponentType.RegularComponentType;
import de.schosin.ecs.storage.api.components.Component;
import de.schosin.ecs.utils.collections.ImmutableBag;

public interface ComponentMask {

    int getId();

    boolean contains(int componentId);

    ImmutableBag<Component<?, ?>> getComponents();

    ImmutableBag<RegularComponentType<?, ?>> getComponentTypes();

    @Override
    int hashCode();

    @Override
    boolean equals(Object obj);

}
