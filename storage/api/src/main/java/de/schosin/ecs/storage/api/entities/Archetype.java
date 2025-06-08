package de.schosin.ecs.storage.api.entities;

import de.schosin.ecs.api.components.types.ComponentType.RegularComponentType;
import de.schosin.ecs.utils.collections.ImmutableIntBag;

public interface Archetype {

    int getId();

    ComponentMask getComponentMask();

    int getCount();

    boolean contains(int entityId);

    ImmutableIntBag getEntities();

    EntityData getEntityData(RegularComponentType<?, ?>... componentTypes);

}
