package de.schosin.ecs.storage.api.components;

import java.util.Objects;

import org.jspecify.annotations.NonNull;

import de.schosin.ecs.api.components.ComponentType.ClassType;
import de.schosin.ecs.api.components.ComponentType.RegularComponentType;

public sealed interface Component<T> {

    non-sealed interface ComponentData<T> extends Component<T> {

        @Override
        ClassType<T> type();

        /**
         * Returns the {@link Class} of this component. Must be the same
         * as <tt>this.{@link #type() type()}.{@link ClassType#clazz() clazz()}</tt>.
         * 
         * @return class of component
         */
        Class<T> clazz();

    }

    interface PooledComponentData<T> extends ComponentData<T> {

        /**
         * Returns a pooled instance of the component. The implementation should
         * reuse instances when a component is removed from an entity, either via
         * {@link Component#removeComponent(int)}, {@link Component#applyRemoval(int)},
         * or {@link Component#applyRemovals()}.
         * 
         * <p>
         * An implementation may throw a {@link RuntimeException} when a new instance
         * could not be instantiated. See ReflectionUtils#createComponentInstance from 
         * the utils module, which can create instances for public types with a public
         * default constructor.
         * </p>
         * 
         * @return instance
         */
        T getInstance();

    }

    /**
     * Returns the id of the component. The id must be unique for every component
     * and should be increasing in creation order. Implementations may choose a different
     * strategy for assigning ids if it allows for performance improvements.
     * 
     * <p>
     * <b>Note:</b> The ids are used for indexing into arrays (Bag and IntBag from utils module),
     * choosing large values should be avoided to keep the arrays small. Reserving small id spaces
     * depending on on the {@link RegularComponentType} is acceptable.
     * </p>
     * 
     * @return id of component
     */
    int id();

    /**
     * Returns the {@link RegularComponentType} of this component.
     * 
     * @return type of component
     */
    RegularComponentType<T> type();

    /**
     * Returns a short string for displaying information about this component. 
     * Must include the {@link Class#getSimpleName()} of the underlying types.
     * 
     * @return short display
     */
    String display();

    /**
     * Returns true if the entity has the component. 
     * 
     * <p>
     * This must return true if {@link #addComponent(int, Object)} or 
     * {@link #addComponentUnsafe(int, Object)} was called for the same
     * entity beforehand.
     * Calls to {@link #markRemoved(int)} should not alter the outcome of this
     * method until either {@link #applyRemovals()} or {@link #applyRemoval(int)}
     * for the same entity was called. 
     * </p>
     * 
     * @param entityId id of entity
     * @return true if the entity has the component
     */
    boolean hasComponent(int entityId);

    /**
     * Returns the component instance if the entity has the component. 
     * 
     * <p>
     * This must return the instance passed to {@link #addComponent(int, Object)} or 
     * {@link #addComponentUnsafe(int, Object)}.
     * Calls to {@link #markRemoved(int)} should not alter the outcome of this
     * method until either {@link #applyRemovals()} or {@link #applyRemoval(int)}
     * for the same entity was called. 
     * </p>
     * 
     * @param entityId id of entity
     * @return component instance or null 
     */
    T getComponent(int entityId);

    default void addComponent(int entityId, @NonNull T component) {
        addComponentUnsafe(entityId, Objects.requireNonNull(component, "component cannot be null"));
    }

    /**
     * Adds the non-null component to the entity.
     * 
     * @param entityId id of entity
     * @param component non-null instance
     */
    void addComponentUnsafe(int entityId, T component);

    void removeComponent(int entityId);

    /**
     * Must be overriden based on {@link #id()} only.
     */
    @Override
    int hashCode();

    /**
     * Must be overriden based on {@link #id()} only.
     */
    @Override
    boolean equals(Object obj);

}
