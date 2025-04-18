package de.schosin.ecs.api.components;

import org.jspecify.annotations.NonNull;

import de.schosin.ecs.api.Pooled;
import de.schosin.ecs.api.World;
import de.schosin.ecs.api.archetype.Archetype;
import de.schosin.ecs.api.archetype.Transmuter;
import de.schosin.ecs.api.state.State;

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
 *  using {@link Transmuter Transmuters}. These reduce the number of 
 *  calculations for the composition changes when compared to adding
 *  and removing components sequentially.
 *  </p> 
 *  
 *  <p>
 *  When state is required for an entity that is not needed by any 
 *  {@link Composition compositions}, use {@link State} instead. 
 *  While such state could be managed as components, adding and removing
 *  components causes an overhead in notifying compositions.
 *  {@link State} does not incur this overhead.
 *  </p>
 * 
 * @param <T> component type
 */
public interface Components<T> {

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
         * @param entityId
         * @return existing, new, or reused component
         */
        @NonNull
        T add(int entityId);

        /**
         * Returns an unused component from a pool or creates a new instance. 
         * Can be used for {@link Components#add(int, Object)},
         * {@link Archetype}, {@link Transmuter}, or {@link World#createEntity(Object...)}.
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

}
