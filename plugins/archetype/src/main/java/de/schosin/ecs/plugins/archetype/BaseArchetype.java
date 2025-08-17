package de.schosin.ecs.plugins.archetype;

import java.util.function.ObjIntConsumer;

import org.jspecify.annotations.NullMarked;

import de.schosin.ecs.api.Pooled;
import de.schosin.ecs.api.World;
import de.schosin.ecs.codegen.EcsCodegen;
import de.schosin.ecs.plugins.archetype.BaseArchetype.ArchetypeConsumer;
import de.schosin.ecs.utils.collections.ImmutableIntBag;

@NullMarked
@EcsCodegen
public interface BaseArchetype<C extends ArchetypeConsumer> {

    @FunctionalInterface
    interface ArchetypeConsumer {
        void accept(ObjIntConsumer<Object> components, int index, int[] mapping);
    }
    
    interface ArchetypeBatch {
        ImmutableIntBag createBatch(int count);
    }

    /**
     * Creates a single entity. All components passed to the factory must be non-null.
     * 
     * <p>
     * Example (Lambda):
     * {@snippet:
     * var entityId = archetype.create(factory -> factory.create(component1, component2));
     * }
     * </p>
     * 
     * @param consumer callback, must invoke {@code factory.create(...)}
     * @return id of entity
     */
    int create(C consumer);

    /**
     * Creates a batch of entities. All components passed to the factory must be non-null.
     * 
     * <p>
     * Example (Lambda):
     * {@snippet:
     * var entityIds = archetype.createBatch(10, (index, factory) -> factory.create(component1, component2));
     * }
     * </p>
     * 
     * @param count number of entities to create, {@code consumer} will be invoked that many times
     * @param consumer callback, must invoke {@code factory.create(...)}, {@code index} will range from 0 to {@code count - 1} 
     * @return id of entity
     * @return bag of entity ids, instance usable until the next {@link World#process()}
     */
    ImmutableIntBag createBatch(int count, C consumer);

    /**
     * Returns a {@link ArchetypeBatch} with the bound consumer.
     * 
     * <p>
     * Using this over {@link #createBatch(int, ArchetypeConsumer)} allows for further optimizations 
     * by moving calculations from the runtime to the initialization (bind method call).
     * 
     * @param consumer invoked consumer when {@link ArchetypeBatch#createBatch(int)} is used
     * @return archetype batch
     */
    ArchetypeBatch bind(C consumer);

    /**
     * Create a new archetype that extends this archetype by adding the passed components to
     * every created entity. The old archetype is not modified.
     * 
     * <p>
     * Intended to be used with marker or singleton components (e.g. enums).
     * </p>
     * 
     * <p>
     * <b>Note:</b> No duplicate components are allowed. If {@link #with(Object...)} adds components
     * defined when creating the archetype, or a previous {@link #with(Object...)} call, an error
     * is thrown.
     * </p>
     * 
     * @param components components to add to every entity
     * @return new archetype
     */
    BaseArchetype<C> with(Object... components);

    /**
     * Returns a pooled instance of the component.
     * 
     * <p>
     * <b>Attention:</b> If the instance is added to an entity, it will be returned 
     * to the underlying pool the entity is deleted or the relation removed from it.
     * As such it cannot be assigned to multiple entities, as the data might be reset
     * while still assigned to another entity.
     * </p>
     * 
     * @param <T> type of component
     * @param clazz class of component
     * @return pooled instance
     */
    <T extends Pooled> T getInstance(Class<T> clazz);

}
