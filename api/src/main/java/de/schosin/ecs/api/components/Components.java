package de.schosin.ecs.api.components;

import static de.schosin.ecs.api.components.ComponentType.exclusiveRelation;
import static de.schosin.ecs.api.components.ComponentType.relation;

import org.jspecify.annotations.NonNull;

import de.schosin.ecs.api.Pooled;
import de.schosin.ecs.api.World;
import de.schosin.ecs.api.components.ComponentType.ClassType;
import de.schosin.ecs.api.components.ComponentType.ComponentRelationType;
import de.schosin.ecs.api.components.ComponentType.ExclusiveComponentRelationType;
import de.schosin.ecs.api.components.ComponentType.RegularComponentType;
import de.schosin.ecs.api.components.Relation.ComponentRelation;
import de.schosin.ecs.api.components.Relation.Exclusive;
import de.schosin.ecs.api.components.Result.ComponentRelationResult;

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
public interface Components<T, R> {

    interface ComponentMapper<T> extends Components<T, T> {

        /**
         * Adds the component to the entity. Overwrites any existing component of the same class. 
         * 
         * @param entityId id of entity
         * @param component component instance
         * @return same component instance as the parameter
         */
        @NonNull
        T add(int entityId, @NonNull T component);

    }

    /**
     * Specialized variant of {@link Components} for {@link Enum enums} supporting
     * adding a default instance defined at {@link Creator#getEnumComponents(Enum) creation}.
     * 
     * @param <T> type of enum
     */
    interface EnumComponentMapper<T extends Enum<T>> extends ComponentMapper<T> {

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

    interface PooledComponentMapper<T extends Pooled> extends ComponentMapper<T> {

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

    sealed interface ComponentRelations<R, T, X> extends Components<ComponentRelation<R, T>, X> {

        ComponentRelation<R, T> add(int entityId, R relationship, T target);

        ComponentRelation<R, T> getInstance(R relationship, T target);

    }

    non-sealed interface ComponentRelationMapper<R, T> extends ComponentRelations<R, T, ComponentRelationResult<R, T>> {

        R getRelationship(int entityId, T target);

    }

    non-sealed interface ExclusiveComponentRelationMapper<R extends Exclusive, T> extends ComponentRelations<R, T, ComponentRelation<R, T>> {

        R getRelationship(int entityId);

        T getTarget(int entityId);

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
    R get(int entityId);

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
         * Retrieves the {@link Components} instance for the given {@link ComponentType}.
         * 
         * @param <T> type of result
         * @param type component type
         * @return class to access components defined by the type argument
         */
        @NonNull
        <T, R> Components<T, R> getComponents(@NonNull ComponentType<T, R> type);

        /**
         * Retrieves the mapper of the given component class. This can be used to access components and 
         * to add or remove components from entities.
         * 
         * <p>
         * <b>Note:</b> If T extends Pooled, the returned instance will also implement and
         * can be cast to {@link PooledComponentMapper}. Prefer using {@link #getPooledComponents(Class)}
         * for pooled components.
         * </p>
         *  
         * @param clazz {@link Class} of the component
         * @return class to manage the components defined by the clazz argument 
         */
        default <T> ComponentMapper<T> getComponents(@NonNull Class<T> clazz) {
            return getComponents(ComponentType.component(clazz));
        }

        /**
         * Retrieves the mapper of the given component type. This can be used to access components and 
         * to add or remove components from entities.
         * 
         * <p>
         * <b>Note:</b> If T extends Pooled, the returned instance will also implement and
         * can be cast to {@link PooledComponentMapper}. Prefer using {@link #getPooledComponents(Class)}
         * for pooled components.
         * </p>
         *  
         * @param type {@link RegularComponentType} of the component
         * @return class to manage the components defined by the type argument 
         */
        @NonNull
        <T, R> Components<T, R> getComponents(@NonNull RegularComponentType<T, R> type);

        /**
         * Retrieves the mapper of the given component type. This can be used to access components and 
         * to add or remove components from entities.
         * 
         * @param type {@link RegularComponentType} of the component
         * @return class to manage the components defined by the type argument
         */
        <T> ComponentMapper<T> getComponents(@NonNull ClassType<T> type);

        /**
         * Retrieves the mapper of the given enum class defined by the {@literal defaultComponent}.
         * This can be used to access components and to add or remove components from entities.
         * 
         * <p>
         * {@link EnumComponentMapper#add(int)} will add the default component to the entity an can be
         * used for marker components (singletons) that require no starte.
         * </p>
         * 
         * @param defaultComponent default component for {@link EnumComponentMapper#add(int)}
         * @return class to manage the components defined by the class of {@literal defaultComponent}
         */
        @NonNull
        <T extends Enum<T>> EnumComponentMapper<T> getEnumComponents(@NonNull T defaultComponent);

        /**
         * Retrieves the mapper of the given component class. This can be used to access components and 
         * to add or remove components from entities.
         * 
         * @param clazz {@link Class} of the component
         * @return class to manage the components defined by the clazz argument 
         */
        default <T extends Pooled> PooledComponentMapper<T> getPooledComponents(@NonNull Class<T> clazz) {
            return getPooledComponents(ComponentType.component(clazz));
        }

        /**
         * Retrieves the mapper of the given component type. This can be used to access components and 
         * to add or remove components from entities.
         * 
         * @param type {@link RegularComponentType} of the component
         * @return class to manage the components defined by the type argument
         */
        <T extends Pooled> PooledComponentMapper<T> getPooledComponents(@NonNull ClassType<T> type);

        /**
         * Retrieves the mapper for a {@link ComponentRelation}. This can be used to access the relations
         * and to add or remove them from entities.
         * 
         * @param <R> type of relationship component
         * @param <T> type of target component
         * @param relationship class of relationship component
         * @param target class of target component
         * @return class to manage the relations defined by the relationship and target class
         */
        default <R, T> ComponentRelationMapper<R, T> getComponentRelations(Class<R> relationship, Class<T> target) {
            return getComponentRelations(relation(relationship, target));
        }

        /**
         * Retrieves the mapper for a {@link ComponentRelationType}. This can be used to access the relations
         * and to add or remove them from entities.
         * 
         * @param <R> type of relationship component
         * @param <T> type of target component
         * @param type {@link ComponentRelationType} of the relation
         * @return class to manage the relations defined by the relationship and target class
         */
        <R, T> ComponentRelationMapper<R, T> getComponentRelations(ComponentRelationType<R, T> relation);

        /**
         * Retrieves the mapper for a {@link ExclusiveComponentRelationType}. This can be used to access the relations
         * and to add or remove them from entities.
         * 
         * @param <R> type of relationship component
         * @param <T> type of target component
         * @param relationship class of relationship component
         * @param target class of target component
         * @return class to manage the relations defined by the relationship and target class
         */
        default <R extends Exclusive, T> ExclusiveComponentRelationMapper<R, T> getExclusiveComponentRelations(Class<R> relationship, Class<T> target) {
            return getComponentRelations(exclusiveRelation(relationship, target));
        }

        /**
         * Retrieves the mapper for a {@link ExclusiveComponentRelationType}. This can be used to access the relations
         * and to add or remove them from entities.
         * 
         * @param <R> type of relationship component
         * @param <T> type of target component
         * @param type {@link ExclusiveComponentRelationType} of the relation
         * @return class to manage the relations defined by the relationship and target class
         */
        <R extends Exclusive, T> ExclusiveComponentRelationMapper<R, T> getComponentRelations(ExclusiveComponentRelationType<R, T> relation);

    }

}
