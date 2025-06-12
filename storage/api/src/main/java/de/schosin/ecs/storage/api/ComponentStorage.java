package de.schosin.ecs.storage.api;

import javax.management.openmbean.CompositeData;

import de.schosin.ecs.api.Pooled;
import de.schosin.ecs.api.components.Relation.Exclusive;
import de.schosin.ecs.api.components.types.ClassType;
import de.schosin.ecs.api.components.types.ComponentType;
import de.schosin.ecs.api.components.types.ComponentType.RegularComponentType;
import de.schosin.ecs.api.components.types.RelationComponentType;
import de.schosin.ecs.api.components.types.RelationComponentType.ComponentRelationType;
import de.schosin.ecs.api.components.types.RelationComponentType.EntityRelationType;
import de.schosin.ecs.api.components.types.RelationComponentType.ExclusiveComponentRelationType;
import de.schosin.ecs.api.components.types.RelationComponentType.ExclusiveEntityRelationType;
import de.schosin.ecs.api.components.types.RelationComponentType.RegularComponentRelationType;
import de.schosin.ecs.api.components.types.RelationComponentType.RegularEntityRelationType;
import de.schosin.ecs.storage.api.components.Component;
import de.schosin.ecs.storage.api.components.Component.ClassComponent;
import de.schosin.ecs.storage.api.components.Component.ComponentData;
import de.schosin.ecs.storage.api.components.Component.ComponentRelationComponent;
import de.schosin.ecs.storage.api.components.Component.ComponentRelationData;
import de.schosin.ecs.storage.api.components.Component.EntityRelationComponent;
import de.schosin.ecs.storage.api.components.Component.EntityRelationData;
import de.schosin.ecs.storage.api.components.Component.ExclusiveComponentRelationData;
import de.schosin.ecs.storage.api.components.Component.ExclusiveEntityRelationData;
import de.schosin.ecs.storage.api.components.Component.PooledComponentData;
import de.schosin.ecs.utils.collections.Bag;
import de.schosin.ecs.utils.collections.ImmutableBag;

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
    Component<?, ?> getComponent(int componentId);

    /**
     * Returns the {@link Component} instance for the {@link RegularComponentType type}.
     * 
     * <p>
     * Implementation must return the same instance for multiple calls
     * with equal types.
     * </p>
     * 
     * <p>
     * Implementation must pass {@literal type} to the {@literal validate} exactly once
     * when this is the first time the storage sees this type and before the {@link Component}
     * instance is created. No exceptions shall be caught when that {@literal validate} might
     * throw. 
     * </p>
     * 
     * <p>
     * When the type describes a {@link Pooled} {@link ClassType}, the implementation must return a 
     * {@link PooledComponentData}.
     * When the type is an instance of {@link ComponentRelationType}, the implementation must return
     * a {@link ComponentRelationData}.
     * Otherwise the implementation must return a {@link CompositeData}.
     * </p>
     * 
     * @param <T> type of component
     * @param type component type of component
     * @return component instance
     */
    @SuppressWarnings("unchecked")
    default <T, R> Component<T, R> getComponent(RegularComponentType<T, R> type) {
        return (Component<T, R>) switch (type) {
            case ClassType<?> classType -> getComponent(classType);
            case RelationComponentType<?, ?, ?> relation -> switch (relation) {
                case ComponentRelationType<?, ?> componentRelation -> getComponent(componentRelation);
                case ExclusiveComponentRelationType<?, ?> componentRelation -> getComponent(componentRelation);
                case EntityRelationType<?> entityRelation -> getComponent(entityRelation);
                case ExclusiveEntityRelationType<?> entityRelation -> getComponent(entityRelation);
            };
        };
    }

    /**
     * Returns the {@link ComponentData} instance for the {@link ClassType type}.
     * 
     * <p>
     * Implementation must return the same instance for multiple calls
     * with equal types.
     * </p>
     * 
     * <p>
     * Implementation must pass {@literal type} to the {@literal validate} exactly once
     * when this is the first time the storage sees this type and before the {@link Component}
     * instance is created. No exceptions shall be caught when that {@literal validate} might
     * throw. 
     * </p>
     * 
     * @param <T> type of component
     * @param type component type of component
     * @return component instance
     */
    <T> ClassComponent<T> getComponent(ClassType<T> type);

    /**
     * Returns the {@link PooledComponentData} instance for the {@link ClassType type}.
     * 
     * <p>
     * Implementation must return the same instance for multiple calls
     * with equal types.
     * </p>
     * 
     * <p>
     * Implementation must pass {@literal type} to the {@literal validate} exactly once
     * when this is the first time the storage sees this type and before the {@link Component}
     * instance is created. No exceptions shall be caught when that {@literal validate} might
     * throw. 
     * </p>
     * 
     * @param <T> type of component
     * @param type component type of component
     * @return component instance
     */
    <T extends Pooled> PooledComponentData<T> getPooledComponent(ClassType<T> type);

    @SuppressWarnings("unchecked")
    default <R, T, X> ComponentRelationComponent<R, T, X> getComponent(RegularComponentRelationType<R, T, X> type) {
        return (ComponentRelationComponent<R, T, X>) switch (type) {
            case ComponentRelationType<?, ?> relation -> getComponent(relation);
            case ExclusiveComponentRelationType<?, ?> relation -> getComponent(relation);
        };
    }

    <R, T> ComponentRelationData<R, T> getComponent(ComponentRelationType<R, T> type);

    <R extends Exclusive, T> ExclusiveComponentRelationData<R, T> getComponent(ExclusiveComponentRelationType<R, T> type);

    @SuppressWarnings("unchecked")
    default <R, X> EntityRelationComponent<R, X> getComponent(RegularEntityRelationType<R, X> type) {
        return (EntityRelationComponent<R, X>) switch (type) {
            case EntityRelationType<?> relation -> getComponent(relation);
            case ExclusiveEntityRelationType<?> relation -> getComponent(relation);
        };
    }

    <R> EntityRelationData<R> getComponent(EntityRelationType<R> type);

    <R extends Exclusive> ExclusiveEntityRelationData<R> getComponent(ExclusiveEntityRelationType<R> type);

    /**
     * Returns a bag of the known components. 
     * 
     * <p>
     * This must be a live bag, meaning that it will be updated automatically when new components are created.
     * </p>
     * 
     * <p>
     * Implementation should not be affected by modifying the returned bag by using 
     * {@link ImmutableBag#create(de.schosin.ecs.utils.collections.Bag)} or implementing
     * their own class. Implementations must not return a {@link Bag}.
     * </p>
     * 
     * @return known components
     */
    ImmutableBag<Component<?, ?>> getComponents();

    /**
     * Returns a bag of the known components that are {@link Class#isAssignableFrom(Class) assignable too}
     * the bound argument.
     * 
     * <p>
     * This must be a live bag, meaning that it will be updated automatically when new components are created.
     * </p>
     * 
     * <p>
     * Implementation should not be affected by modifying the returned bag by using 
     * {@link ImmutableBag#create(de.schosin.ecs.utils.collections.Bag)} or implementing
     * their own class. Implementations must not return a {@link Bag}.
     * </p>
     * 
     * @param bound component type bound
     * @return known components matching the bound
     */
    <T> ImmutableBag<Component<? extends T, ?>> getComponents(ComponentType<T, ?> bound);

    /**
     * Returns an array of all known component types that match the bound.
     * 
     * <p>
     * Matches the types of the result of {@link #getComponents(ComponentType)}.
     * </p>
     * 
     * @param componentType component type bound
     * @return known regular component types matching the bound.
     */
    RegularComponentType<?, ?>[] getRegularComponentTypes(ComponentType<?, ?> bound);

}
