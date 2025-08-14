package de.schosin.ecs.storage.api;

import org.jspecify.annotations.Nullable;

import de.schosin.ecs.api.components.types.ComponentType;
import de.schosin.ecs.api.components.types.ComponentType.RegularComponentType;
import de.schosin.ecs.api.data.DataAccessor;
import de.schosin.ecs.storage.api.entities.Archetype;
import de.schosin.ecs.utils.collections.ImmutableBag;

public interface EntityStorage {

    DataAccessor getAccessor(int entityId);

    /**
     * Modify an existing entity by adding the components, overwriting existing values in case of collisions.
     * 
     * <p>
     * If the addition changes the {@link Archetype} of the entity, the addition of components that caused
     * the change are delayed. They are still accessible when retrieving them with the exception of exclusive
     * component relations that replace an existing one.
     * </p>
     * 
     * @param entityId id of entity
     * @param components components to add to the entity
     * @return updated archetype of entity
     */
    Archetype add(int entityId, Object[] components);

    /**
     * Modify an existing entity by adding the components, overwriting existing values in case of collisions.
     * The component types must match the components by index.
     * 
     * <p>
     * If the addition changes the {@link Archetype} of the entity, the addition of components that caused
     * the change are delayed. They are still accessible when retrieving them with the exception of exclusive
     * component relations that replace an existing one.
     * </p>
     * 
     * @param entityId id of entity
     * @param components components to add to the entity
     * @param componentTypes component types of components
     * @return updated archetype of entity
     */
    Archetype add(int entityId, ImmutableBag<? extends RegularComponentType<?, ?>> componentTypes, Object[] components);

    /**
     * Modify an existing entity by removing the components. The actual removal is delayed.
     * 
     * @param entityId id of entity
     * @param componentTypes component types to remove from the entity
     * @return updated archetype of entity
     */
    Archetype remove(int entityId, ImmutableBag<? extends ComponentType<?, ?>> componentTypes);

    /**
     * Marks the entity for deletion during the next {@link StorageEngine#process()} call.
     * 
     * @param entityId id of entity
     */
    void markDeleted(int entityId);

    /**
     * Modify an existing entity by adding and removing components.
     * 
     * <p>
     * If the addition changes the {@link Archetype} of the entity, the addition of components that caused
     * the change are delayed. They are still accessible when retrieving them with the exception of exclusive
     * component relations that replace an existing one.
     * </p>
     * 
     * @param entityId id of entity
     * @param add added components
     * @param removeTypes component types of removed components
     * @return updated archetype of entity
     */
    default Archetype modify(int entityId, Object[] add, ImmutableBag<? extends ComponentType<?, ?>> removeTypes) {
        add(entityId, add);
        return remove(entityId, removeTypes);
    }

    /**
     * Modify an existing entity by adding and removing components.
     * 
     * <p>
     * If the addition changes the {@link Archetype} of the entity, the addition of components that caused
     * the change are delayed. They are still accessible when retrieving them with the exception of exclusive
     * component relations that replace an existing one.
     * </p>
     * 
     * @param entityId id of entity
     * @param addTypes component types of added components
     * @param add added components
     * @param removeTypes component types of removed components
     * @return updated archetype of entity
     */
    default Archetype modify(int entityId, ImmutableBag<? extends RegularComponentType<?, ?>> addTypes, Object[] add, ImmutableBag<? extends ComponentType<?, ?>> removeTypes) {
        add(entityId, addTypes, add);
        return remove(entityId, removeTypes);
    }

    /**
     * Returns the archetype for pending changes.
     * 
     * @param entityId id of entity
     * @return archetype for pending changes or null if none
     */
    @Nullable
    Archetype getPendingArchetype(int entityId);

}
