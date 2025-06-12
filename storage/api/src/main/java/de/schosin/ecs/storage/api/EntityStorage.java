package de.schosin.ecs.storage.api;

import java.util.function.Predicate;

import org.jspecify.annotations.Nullable;

import de.schosin.ecs.api.components.types.ComponentType;
import de.schosin.ecs.api.components.types.ComponentType.RegularComponentType;
import de.schosin.ecs.api.data.DataAccessor;
import de.schosin.ecs.storage.api.entities.ComponentMask;
import de.schosin.ecs.utils.collections.Bag;
import de.schosin.ecs.utils.collections.ImmutableBag;

public interface EntityStorage {

    DataAccessor getAccessor(int entityId);

    /**
     * Retrieve the component mask of the entity.
     * 
     * @param entityId id of component mask
     * @return component mask or null if entity not known
     */
    ComponentMask getComponentMaskForEntity(int entityId);

    ComponentMask getComponentMaskById(int componentMaskId);

    ComponentMask getComponentMask(RegularComponentType<?, ?>... componentTypes);

    ImmutableBag<ComponentMask> getComponentMasks();

    void getComponentMasks(Predicate<ComponentMask> predicate, Bag<ComponentMask> fill);

    /**
     * Create an entity with the passed components. The indexes of both bags must match.
     * 
     * @param entityId id of entity
     * @param components components to add to the entity
     * @return component mask of entity
     */
    ComponentMask create(int entityId, Object[] components);

    /**
     * Create an entity with the passed components. The types of the components must match the component mask.
     * 
     * @param entityId id of entity
     * @param componentMask component mask
     * @param components components to add to the entity
     * @return same as passed component mask
     */
    ComponentMask create(int entityId, ComponentMask componentMask, Object[] components);

    /**
     * Create an entity with the passed components. The component types must match the components by index,
     * and the component types must match the component mask exactly apart from the ordering.
     * 
     * @param entityId id of entity
     * @param componentMask component mask
     * @param componentTypes component types of components
     * @param components components to add to the entity
     * @return same as passed component mask
     */
    ComponentMask create(int entityId, ComponentMask componentMask, ImmutableBag<? extends RegularComponentType<?, ?>> componentTypes, Object[] components);

    ComponentMask create(int entityId, ComponentMask componentMask, ImmutableBag<? extends RegularComponentType<?, ?>> componentTypes, ImmutableBag<Object> components);

    /**
     * Modify an existing entity by adding the components, overwriting existing values in case of collisions.
     * 
     * <p>
     * If the addition changes the {@link ComponentMask} of the entity, the addition of components that caused
     * the change are delayed. They are still accessible when retrieving them with the exception of exclusive
     * component relations that replace an existing one.
     * </p>
     * 
     * @param entityId id of entity
     * @param components components to add to the entity
     * @return updated component mask of entity
     */
    ComponentMask add(int entityId, Object[] components);

    /**
     * Modify an existing entity by adding the components, overwriting existing values in case of collisions.
     * The component types must match the components by index.
     * 
     * <p>
     * If the addition changes the {@link ComponentMask} of the entity, the addition of components that caused
     * the change are delayed. They are still accessible when retrieving them with the exception of exclusive
     * component relations that replace an existing one.
     * </p>
     * 
     * @param entityId id of entity
     * @param components components to add to the entity
     * @param componentTypes component types of components
     * @return updated component mask of entity
     */
    ComponentMask add(int entityId, ImmutableBag<? extends RegularComponentType<?, ?>> componentTypes, Object[] components);

    /**
     * Modify an existing entity by removing the components. The actual removal is delayed.
     * 
     * @param entityId id of entity
     * @param componentTypes component types to remove from the entity
     * @return updated component mask of entity
     */
    ComponentMask remove(int entityId, ImmutableBag<? extends ComponentType<?, ?>> componentTypes);

    /**
     * Delete an entity, clearing all stored component data.
     * 
     * @param entityId id of entity
     * @return previous component mask or null if entity not known
     */
    ComponentMask delete(int entityId);

    /**
     * Modify an existing entity by adding and removing components.
     * 
     * <p>
     * If the addition changes the {@link ComponentMask} of the entity, the addition of components that caused
     * the change are delayed. They are still accessible when retrieving them with the exception of exclusive
     * component relations that replace an existing one.
     * </p>
     * 
     * @param entityId id of entity
     * @param add added components
     * @param removeTypes component types of removed components
     * @return updated component mask of entity
     */
    default ComponentMask modify(int entityId, Object[] add, ImmutableBag<? extends ComponentType<?, ?>> removeTypes) {
        add(entityId, add);
        return remove(entityId, removeTypes);
    }

    /**
     * Modify an existing entity by adding and removing components.
     * 
     * <p>
     * If the addition changes the {@link ComponentMask} of the entity, the addition of components that caused
     * the change are delayed. They are still accessible when retrieving them with the exception of exclusive
     * component relations that replace an existing one.
     * </p>
     * 
     * @param entityId id of entity
     * @param addTypes component types of added components
     * @param add added components
     * @param removeTypes component types of removed components
     * @return updated component mask of entity
     */
    default ComponentMask modify(int entityId, ImmutableBag<? extends RegularComponentType<?, ?>> addTypes, Object[] add, ImmutableBag<? extends ComponentType<?, ?>> removeTypes) {
        add(entityId, addTypes, add);
        return remove(entityId, removeTypes);
    }

    /**
     * Returns the component mask for pending changes.
     * 
     * @param entityId id of entity
     * @return component mask for pending changes or null if none
     */
    @Nullable
    ComponentMask getPendingComponentMask(int entityId);

    /**
     * Flushes pending changes by {@link #add(int, Object[])}, {@link #remove(int, ImmutableBag)} or
     * {@link #modify(int, Object[], ImmutableBag)} (and overloads) for the entity.
     * 
     * @param entityId id of entity
     * @return component mask after changes have been applied
     */
    ComponentMask flushChanges(int entityId);

}
