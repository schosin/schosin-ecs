package de.schosin.ecs.storage.api;

import java.util.Collection;
import java.util.function.Consumer;

import javax.management.openmbean.CompositeData;

import de.schosin.ecs.api.Pooled;
import de.schosin.ecs.api.components.ComponentType.ClassType;
import de.schosin.ecs.api.components.ComponentType.RegularComponentType;
import de.schosin.ecs.storage.api.components.Component;
import de.schosin.ecs.storage.api.components.Component.PooledComponentData;

public interface ComponentStorage {

    /**
     * Returns the component assigned to that id.
     * 
     * <p>
     * Implementation may return {@literal null} or throw a {@link RuntimeException} when
     * the id is not known or invalid.
     * </p>
     * 
     * @param componentId id of component
     * @return {@link Component} instance
     */
    Component<?> getComponent(int componentId);

    /**
     * Returns the {@link Component} instance for the {@link RegularComponentType type}.
     * 
     * <p>
     * Implementation must return the same instance for multiple calls
     * with equal types.
     * </p>
     * 
     * <p>
     * Implementation must pass {@liter type} to the {@literal validate} exactly once
     * when this is the first time the storage sees this type and before the {@link Component}
     * instance is created. No exceptions shall be caught when that {@literal validate} might
     * throw. 
     * </p>
     * 
     * <p>
     * When the type describes a {@link Pooled} {@link ClassType}, the implementation must return a 
     * {@link PooledComponentData}.
     * Otherwise the implementation must return a {@link CompositeData}.
     * </p>
     * 
     * @param <T> type of component
     * @param type component type of component
     * @param validate callback for component creation
     * @return component instance
     */
    <T> Component<T> getComponent(RegularComponentType<T> type, Consumer<RegularComponentType<?>> validate);

    /**
     * Returns the {@link PooledComponentData} instance for the {@link RegularComponentType type}.
     * 
     * <p>
     * Implementation must return the same instance for multiple calls
     * with equal types.
     * </p>
     * 
     * <p>
     * Implementation must pass {@liter type} to the {@literal validate} exactly once
     * when this is the first time the storage sees this type and before the {@link Component}
     * instance is created. No exceptions shall be caught when that {@literal validate} might
     * throw. 
     * </p>
     * 
     * @param <T> type of component
     * @param type component type of component
     * @param validate callback for component creation
     * @return component instance
     */
    <T extends Pooled> PooledComponentData<T> getPooledComponent(RegularComponentType<T> type, Consumer<RegularComponentType<?>> validate);

    /**
     * Returns a collection of the known components. This must be a live collection,
     * meaning that it will be updated automatically when new components are created.
     * 
     * <p>
     * Implementation should not be affected by modifying the returned collection. Either by returning a defensive
     * copy, or by throws {@link UnsupportedOperationException}.
     * </p>
     * 
     * @return known components
     */
    Collection<Component<?>> getComponents();

}
