package de.schosin.ecs.storage.api.entities;

import java.util.function.IntSupplier;
import java.util.function.ObjIntConsumer;

import de.schosin.ecs.api.components.types.ComponentType.RegularComponentType;
import de.schosin.ecs.api.data.IterableAccessor;
import de.schosin.ecs.storage.api.components.Component;
import de.schosin.ecs.storage.api.entities.observer.EntitiesCreatedObserver;
import de.schosin.ecs.storage.api.entities.observer.EntityCreatedObserver;
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

    /**
     * Create an entity given the components.
     * 
     * <p>
     * The actual archetype of the entity might differ from this archetype if initialization
     * logic adds or removes components. The return type matches the archetype after that initialization. 
     * Further modifications during {@link EntityCreatedObserver} or {@link EntitiesCreatedObserver}
     * will be immediately flushed, which can cause major performance regressions. Prefer using 
     * {@link de.schosin.ecs.plugins.composition.EntityInitializer EntityInitializer} from the composition plugin if possible.
     * 
     * @param entityId
     * @param components components in order according to {@link #getComponentTypes()}
     * @return archetype of created entity
     */
    Archetype createEntity(int entityId, Object[] components);

    /**
     * Create a batch of entities in this archetype. Will create {@code count} entities and call
     * {@code entityIdSupplier} and {@code componentsConsumer} that many times.
     * 
     * <p>
     * The actual archetype of the entities might differ from this archetype if initialization
     * logic adds or removes components. The return type matches the archetype after that initialization. 
     * Further modifications during {@link EntityCreatedObserver} or {@link EntitiesCreatedObserver}
     * will be immediately flushed, which can cause major performance regressions. Prefer using 
     * {@link de.schosin.ecs.plugins.composition.EntityInitializer EntityInitializer} from the composition plugin if possible.
     * 
     * @param count number of entities to create
     * @param entityIdSupplier supplier of entity ids
     * @param initializer consumer for filling component data for the entity by its index
     * @return archetype of created entites
     */
    Archetype createEntities(int count, IntSupplier entityIdSupplier, ComponentsInitializer initializer);

}
