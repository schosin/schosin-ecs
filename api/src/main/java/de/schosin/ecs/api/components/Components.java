package de.schosin.ecs.api.components;

import org.jspecify.annotations.NonNull;

import de.schosin.ecs.api.Pooled;
import de.schosin.ecs.api.World;

/**
 *  Component mapper for accessing and modifying components of an entity.
 *  Use {@link World#getComponents(Class)} and {@link World#getPooledComponents(Class)}
 *  to create instances of this interface.
 *  
 *  <p>
 *  The component of an entity contains the data systems interact with.
 *  These are managed by the {@link World} and can be queried and modified by this 
 *  class. 
 *  </p>
 *  
 *  <p>
 *  For advanced querying {@link Composition Compositions} can be used. 
 *  These allow querying entities having a specific component composition. 
 *  </p>
 *  
 *  <p>
 *  When adding and removing multiple components for an entity, consider
 *  using Transmuters. These reduce the number of 
 *  calculations for the composition changes when compared to adding
 *  and removing components sequentially.
 *  </p> 
 *  
 *  <p>
 *  When state is required for an entity that is not needed by any 
 *  {@link Composition compositions}, use State from the state plugin instead. 
 *  While such state could be managed as components, adding and removing
 *  components causes an overhead in notifying compositions.
 *  State does not incur this overhead.
 *  </p>
 * 
 * @param <T> component type
 */
public interface Components<T> {

    /**
     * Specialized variant of {@link Components} for {@link Enum enums} supporting
     * adding a default instance defined at {@link Creator#getEnumComponents(Enum) creation}.
     * 
     * @param <T> type of enum
     */
    interface EnumComponents<T extends Enum<T>> extends Components<T> {

        /**
         * Returns the current component or adds the enum instance defined
         * at {@link Creator#getEnumComponents(Enum) creation}.
         * 
         * @param entityId
         * @return existing or predefined component instance
         */
        @NonNull
        T add(int entityId);

        /**
         * Returns the default instance defined at {@link Creator#getEnumComponents(Enum) creation}.
         * 
         * @return default instance
         */
        @NonNull
        T getDefault();

    }

    interface PooledComponents<T extends Pooled> extends Components<T> {

        /**
         * Returns the current component or adds a new component from a pool. 
         * 
         * <p>
         * <b>Attention:</b> Using this method requires a public default constructor.
         * </p>
         * 
         * <p>
         * <b>Note:</b> Components implementing {@link Pooled} will be pooled and reused to reduce GC pressure before 
         * creating an instance via reflection.
         * </p>
         * 
         * @param entityId id of entity
         * @return existing, new, or reused component
         */
        @NonNull
        T add(int entityId);

        /**
         * Returns an unused component from a pool or creates a new instance. 
         * Can be used for {@link Components#add(int, Object)},
         * Archetype, {@link Transmuter}, or {@link World#createEntity(Object...)}.
         * 
         * <p>
         * <b>Attention:</b> Using this method requires a public default constructor.
         * </p>
         * 
         * <p>
         * If the component is given to an entity, the instance will
         * be returned to the pool if the component is removed from
         * the entity or the entity is {@link World#deleteEntity(int) deleted}.
         * </p>
         * 
         * <p>
         * Components implementing {@link Pooled} will be {@link Pooled#reset() reset}
         * when reusing instances from the pool. Newly created instances won't be
         * reset.
         * </p>
         *         
         * @return 
         */
        @NonNull
        T getInstance();

    }

    /**
     * Checks whether the entity has the given component.
     * 
     * @param entityId id of entity
     * @return true, if the entity has this component
     */
    boolean has(int entityId);

    /**
     * Retrieves the component for the entity.
     * 
     * @param entityId id of entity
     * @return component instance, may be null
     */
    T get(int entityId);

    /**
     * Adds the component to the entity. Overwrites any existing component of the same class. 
     * 
     * @param entityId id of entity
     * @param component component instance
     * @return same component instance as the parameter
     */
    @NonNull
    T add(int entityId, @NonNull T component);

    /**
     * Marks the component for removal. The component will be removed during the {@link #process()} call.
     * 
     * <p>
     * <b>Attention:</b> {@link Composition Compositions} won't be updated until {@link World#process()} is called
     * </p>
     * 
     * @param entityId id of entity
     * @return true, if the entity had the component
     */
    boolean remove(int entityId);

    interface Creator {

        /**
         * Retrieves the mapper of the given component class. This can be used to access components and 
         * to add or remove components from entities.
         * 
         * <p>
         * <b>Note:</b> If T extends Pooled, the returned instance will also implement and
         * can be cast to {@link PooledComponents}. Prefer using {@link #getPooledComponents(Class)}
         * for pooled components.
         * </p>
         *  
         * @param clazz {@link Class} of the component
         * @return class to manage the components defined by the clazz argument 
         */
        @NonNull
        <T> Components<T> getComponents(@NonNull Class<T> clazz);

        /**
         * Retrieves the mapper of the given enum class defined by the {@literal defaultComponent}.
         * This can be used to access components and to add or remove components from entities.
         * 
         * <p>
         * {@link EnumComponents#add(int)} will add the default component to the entity an can be
         * used for marker components (singletons) that require no starte.
         * </p>
         * 
         * @param defaultComponent default component for {@link EnumComponents#add(int)}
         * @return class to manage the components defined by the class of {@literal defaultComponent}
         */
        @NonNull
        <T extends Enum<T>> EnumComponents<T> getEnumComponents(@NonNull T defaultComponent);

        /**
         * Retrieves the mapper of the given component class. This can be used to access components and 
         * to add or remove components from entities.
         * 
         * @param clazz {@link Class} of the component
         * @return class to manage the components defined by the clazz argument 
         */
        <T extends Pooled> PooledComponents<T> getPooledComponents(@NonNull Class<T> clazz);

    }

}
