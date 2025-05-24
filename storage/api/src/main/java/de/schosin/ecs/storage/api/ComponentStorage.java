package de.schosin.ecs.storage.api;

import java.util.function.Consumer;

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
     * @param validate callback for component creation
     * @return component instance
     */
    @SuppressWarnings("unchecked")
    default <T, R> Component<T, R> getComponent(RegularComponentType<T, R> type, Consumer<RegularComponentType<?, ?>> validate) {
        return (Component<T, R>) switch (type) {
            case ClassType<?> classType -> getComponent(classType, validate);
            case RelationComponentType<?, ?, ?> relation -> switch (relation) {
                case ComponentRelationType<?, ?> componentRelation -> getComponent(componentRelation, validate);
                case ExclusiveComponentRelationType<?, ?> componentRelation -> getComponent(componentRelation, validate);
                case EntityRelationType<?> entityRelation -> getComponent(entityRelation, validate);
                case ExclusiveEntityRelationType<?> entityRelation -> getComponent(entityRelation, validate);
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
     * @param validate callback for component creation
     * @return component instance
     */
    <T> ClassComponent<T> getComponent(ClassType<T> type, Consumer<RegularComponentType<?, ?>> validate);

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
     * @param validate callback for component creation
     * @return component instance
     */
    <T extends Pooled> PooledComponentData<T> getPooledComponent(ClassType<T> type, Consumer<RegularComponentType<?, ?>> validate);

    @SuppressWarnings("unchecked")
    default <R, T, X> ComponentRelationComponent<R, T, X> getComponent(RegularComponentRelationType<R, T, X> type, Consumer<RegularComponentType<?, ?>> validate) {
        return (ComponentRelationComponent<R, T, X>) switch (type) {
            case ComponentRelationType<?, ?> relation -> getComponent(relation, validate);
            case ExclusiveComponentRelationType<?, ?> relation -> getComponent(relation, validate);
        };
    }

    <R, T> ComponentRelationData<R, T> getComponent(ComponentRelationType<R, T> type, Consumer<RegularComponentType<?, ?>> validate);

    <R extends Exclusive, T> ExclusiveComponentRelationData<R, T> getComponent(ExclusiveComponentRelationType<R, T> type, Consumer<RegularComponentType<?, ?>> validate);

    @SuppressWarnings("unchecked")
    default <R, X> EntityRelationComponent<R, X> getComponent(RegularEntityRelationType<R, X> type, Consumer<RegularComponentType<?, ?>> validate) {
        return (EntityRelationComponent<R, X>) switch (type) {
            case EntityRelationType<?> relation -> getComponent(relation, validate);
            case ExclusiveEntityRelationType<?> relation -> getComponent(relation, validate);
        };
    }

    <R> EntityRelationData<R> getComponent(EntityRelationType<R> type, Consumer<RegularComponentType<?, ?>> validate);

    <R extends Exclusive> ExclusiveEntityRelationData<R> getComponent(ExclusiveEntityRelationType<R> type, Consumer<RegularComponentType<?, ?>> validate);

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

}
