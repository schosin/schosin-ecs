package de.schosin.ecs.api.components.mappers;

import org.jspecify.annotations.NonNull;

import de.schosin.ecs.api.World;
import de.schosin.ecs.api.components.Relation.ComponentRelation;
import de.schosin.ecs.api.components.Relation.EntityRelation;
import de.schosin.ecs.api.components.mappers.ComponentMapper.PooledComponentMapper;
import de.schosin.ecs.api.components.mappers.Components.RegularComponents;
import de.schosin.ecs.api.components.mappers.CustomComponents.Factory;
import de.schosin.ecs.api.components.mappers.EntityFetchRelations.EntityRelationFetchMapper;
import de.schosin.ecs.api.components.types.ClassType;
import de.schosin.ecs.api.components.types.ComponentType;
import de.schosin.ecs.api.components.types.ComponentType.RegularComponentType;
import de.schosin.ecs.api.components.types.CustomComponentType;
import de.schosin.ecs.api.components.types.RelationFetchType.EntityRelationFetchType;
import de.schosin.ecs.api.data.DataAccessor;

/**
 * This interface allows for accessing and removing a single type of component from entities.
 * Every instance of this interface will be extending one of the types defined by the sealed
 * hierarchy.
 * 
 * <ul>
 *  <li>{@link ComponentMapper} allows working with regular POJO components and enums</li>
 *  <li>{@link ComponentRelations} allows working with {@link ComponentRelation ComponentRelations}</li> 
 *  <li>{@link EntityRelations} allows working with {@link EntityRelation EntityRelations}</li>
 *  <li>
 *      {@link EntityFetchRelations} allows working with {@link EntityRelation EntityRelations} 
 *      and fetching components of the target entity
 *  </li>
 *  <li>{@link ComponentSetMapper} allows working with a set of components</li>
 *  <li>{@link WildcardComponents} allows working with a group of components extending a lower bound type</li> 
 * </ul>
 * 
 * <p>
 * Every implementation will be bound to a {@link ComponentType}. The relation between {@link ComponentType} and
 * implementation is defined by the methods in {@link Creator}. <br />
 * {@link ClassType} maps to {@link ComponentMapper} because of {@link Creator#getComponents(ClassType)}, and
 * {@link EntityRelationFetchType} maps to {@link EntityRelationFetchMapper} because of 
 * {@link Creator#getEntityRelations(EntityRelationFetchType)}.
 * </p>
 * 
 * <p>
 * Some implementations support adding components to entities as well. 
 * </p>
 * 
 * Component mapper for accessing and modifying components of an entity.
 * Use {@link World#getComponents(Class)} and {@link World#getPooledComponents(Class)}
 * to create instances of this interface.
 * 
 * <p>
 * The component of an entity contains the data systems interact with.
 * These are managed by the {@link World} and can be queried and modified by this 
 * class. 
 * </p>
 * 
 * <p>
 * For advanced querying {@link Composition Compositions} can be used. 
 * These allow querying entities having a specific component composition. 
 * </p>
 * 
 * <p>
 * When adding and removing multiple components for an entity, consider
 * using Transmuters. These reduce the number of 
 * calculations for the composition changes when compared to adding
 * and removing components sequentially.
 * </p> 
 * 
 * <p>
 * When state is required for an entity that is not needed by any 
 * {@link Composition compositions}, use State from the state plugin instead. 
 * While such state could be managed as components, adding and removing
 * components causes an overhead in notifying compositions.
 * State does not incur this overhead.
 * </p>
 * 
 * @param <T> component type
 */
public sealed interface Components<T, R> permits RegularComponents, ComponentSetMapper, WildcardComponents, EntityFetchRelations, WildcardRelations, CustomComponents {

    sealed interface RegularComponents<T, R> extends Components<T, R> permits ComponentMapper, ComponentRelations, EntityRelations {

        /**
         * Returns the id of the component managed by this mapper.
         * 
         * @return id of component
         */
        int componentId();

        /**
         * Returns the component type managed by this mapper.
         * 
         * @return component type
         */
        RegularComponentType<T, R> componentType();

        /**
         * Adds the component to the entity. Overwrites any existing component of the same class unless
         * the particular implementation states otherwise. 
         * 
         * @param entityId id of entity
         * @param component component instance
         * @return same component instance as the parameter
         */
        @NonNull
        T add(int entityId, @NonNull T component);

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
     * Retrieves the component given the {@link DataAccessor}.
     *  
     * @param accessor data accessor
     * @return component instance, may be null
     */
    R get(DataAccessor accessor);

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

    interface Creator extends ComponentMapper.Creator, ComponentSetMapper.Creator, ComponentRelations.Creator, EntityRelations.Creator, EntityFetchRelations.Creator, WildcardRelations.Creator {

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
        <T, R> RegularComponents<T, R> getComponents(@NonNull RegularComponentType<T, R> type);

        /**
         * Retrieves the mapper for the custom component type.
         * 
         * @param type {@link CustomComponentType} of the component
         * @return class to manage the components defined by the type argument 
         * @throws IllegalArgumentException if no {@link Factory} has been registered for the type
         */
        @NonNull
        <T, R, X extends CustomComponentType<T, R, C>, C extends CustomComponents<T, R>> C getComponents(X type);

    }

}
