package de.schosin.ecs.plugins.archetype;

import java.util.function.IntFunction;

import org.jspecify.annotations.NullMarked;

import de.schosin.ecs.api.Pooled;
import de.schosin.ecs.codegen.EcsCodegen;
import de.schosin.ecs.plugins.data.types.DataProvider;

@NullMarked
@EcsCodegen
public interface BaseArchetype<P extends DataProvider<?>> {

    /**
     * Creates an entity. All components must be non-null.
     * 
     * <p>
     * Example:
     * {@snippet:
     * var entityId = archetype.create(factory -> factory.create(component1, component2));
     * }
     * </p>
     * 
     * @param provider method accepting a factory, returning the result of invoking the {@code create} method.
     * @return id of entity
     */
    int create(P provider);

    /**
     * Creates a batch of entities. All components must be non-null.
     * 
     * <p>
     * Example (Lambda):
     * {@snippet:
     * var entityIds = archetype.createBatch(10, factory -> factory.create(component1, component2));
     * }
     * </p>
     * 
     * @param count number of entities
     * @param provider method accepting a factory, returning the result of invoking the {@code create} method
     * @return array of length {@code count} containing the ids of created entities
     */
    default int[] createBatch(int count, P provider) {
        return createIndexed(count, idx -> provider);
    }

    /**
     * Creates a batch of entities. All components must be non-null.
     * 
     * <p>
     * Example (Lambda):
     * {@snippet:
     * var entityIds = archetype.createBatch(10, idx -> factory -> factory.create(
     *         createComponent1(idx), 
     *         createComponent2(idx)));
     * }
     * </p>
     * 
     * @param count
     * @param provider method accepting a zero-based index, returning a method accepting a factory, returning the result of invoking the {@code create} method.
     * @return array of length {@code count} containing the ids of created entities
     */
    int[] createIndexed(int count, IntFunction<P> provider);

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
    BaseArchetype<P> with(Object... components);

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
