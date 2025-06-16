package de.schosin.ecs.storage.api.entities;

import java.util.function.IntSupplier;
import java.util.function.ObjIntConsumer;

import de.schosin.ecs.api.components.types.ComponentType.RegularComponentType;
import de.schosin.ecs.utils.collections.ImmutableIntBag;

public interface Archetype {

    int getId();

    ComponentMask getComponentMask();

    int getCount();

    boolean contains(int entityId);

    ImmutableIntBag getEntities();

    /**
     * Returns the index of the component in this archetype, or -1 if not contained
     */
    int getComponentIndex(int componentId);

    /**
     * Returns the index of the component in this archetype, or -1 if not contained
     */
    int getComponentIndex(RegularComponentType<?, ?> componentType);

    EntityData getEntityData();

    EntityData getEntityData(RegularComponentType<?, ?>... componentTypes);

    void createEntity(int entityId, Object[] components);

    void createEntities(int count, IntSupplier entityIdSupplier, ObjIntConsumer<Object[]> componentsConsumer);

}
