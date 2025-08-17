package de.schosin.ecs.storage.api.entities;

import java.util.function.IntSupplier;
import java.util.function.ObjIntConsumer;

import de.schosin.ecs.api.components.types.ComponentType.RegularComponentType;
import de.schosin.ecs.api.data.IterableAccessor;
import de.schosin.ecs.storage.api.components.Component;
import de.schosin.ecs.utils.collections.ImmutableBag;
import de.schosin.ecs.utils.collections.ImmutableIntBag;

public interface Archetype {

    @FunctionalInterface
    interface ComponentsInitializer {
        void accept(ObjIntConsumer<Object> components, int index);
    }

    int getId();

    ImmutableBag<Component<?, ?>> getComponents();

    ImmutableBag<RegularComponentType<?, ?>> getComponentTypes();

    int getCount();

    ImmutableIntBag getEntities();

    /**
     * Returns the index of the component in this archetype, or -1 if not contained
     */
    int getComponentIndex(int componentId);

    IterableAccessor getAccessor();

    void createEntity(int entityId, Object[] components);

    /**
     * Create a batch of entities in this archetype. Will create {@code count} entities and call
     * {@code entityIdSupplier} and {@code componentsConsumer} that many times.
     * 
     * @param count number of entities to create
     * @param entityIdSupplier supplier of entity ids
     * @param initializer consumer for filling component data for the entity by its index
     */
    void createEntities(int count, IntSupplier entityIdSupplier, ComponentsInitializer initializer);

}
