package de.schosin.ecs.api.components;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import de.schosin.ecs.api.Pooled;
import de.schosin.ecs.api.World;
import de.schosin.ecs.api.components.ComponentSet.ComponentAccessor;
import de.schosin.ecs.api.components.ComponentSet.ComponentData;
import de.schosin.ecs.api.components.ComponentSet.ComponentSetData;
import de.schosin.ecs.api.components.ComponentSet.ComponentSetDataBuilder;
import de.schosin.ecs.api.components.ComponentSet.Factory;
import de.schosin.ecs.api.components.Relation.ComponentRelation;
import de.schosin.ecs.api.components.Relation.EntityRelation;
import de.schosin.ecs.api.components.Relation.Exclusive;
import de.schosin.ecs.api.components.Result.ComponentRelationResult;
import de.schosin.ecs.api.components.Result.ComponentResult;
import de.schosin.ecs.api.components.Result.EntityRelationResult;

/**
 * Base interface for declaring a component set. A component set consists of one or more
 * components and can be used to access, add and remove a set of components.
 * 
 * <p>
 * There a multiple ways to declare a component set. The easiest one is to use the annotation
 * processor "{@code de.schosin.ecs.buildtools:codegen-apt}" that can generate the necessary
 * classes. Alternativly a component set can be implemented manually, which will has additional
 * requirements and recommendations beyond what implementing this interface can enforce. 
 * </p>
 * 
 * <h3>Code generation</h3>
 * 
 * <p>
 * When using code generation, "{@code de.schosin.ecs.buildtools:codegen-apt}" has to be added
 * as an optional dependency and any class has to be annotated with {@code @DiscoverComponentSets}.
 * This can be the main class starting the application or the class instantiating the {@link World}.
 * </p>
 * 
 * <p>
 * The annotation processor will look for all interfaces extending {@link ComponentSet}, as well as records
 * annotated with {@code @ComponentSetConfig}. It will generate the necessary classes and helper functions
 * in a class {@link de.schosin.ecs.api.components.ComponentSets ComponentSets}.
 * </p>
 * 
 * <h4>Interface declaration</h4>
 * 
 * <p>
 * To declare a component set using an interface, just extend {@link ComponentSet} and create accessor
 * methods for the components, {@link Relation relations} or {@link Result component and relation results}.
 * </p>
 * 
 * <p>
 * <b>Example</b>
 * {@snippet:
 * interface PhysicsComponents extends ComponentSet {
 *     Position pos();
 *     Velocity velocity();
 *     ComponentRelation<Birthplace, Position> birthplace();
 *     ComponentRelationResult<Location, Position> locations();
 *     ComponentResult<Object> all();
 * }
 * }
 * </p>
 * 
 * The annotation processor will create an implementation {@code PhysicsComponentsImpl} and create a static
 * method in {@code ComponentSets} for creating an instance of this component set.
 * 
 * <h4>Record declaration</h4>
 * 
 * <p>
 * To declare a component set using a record, just declare the components as the record components and annotate
 * the record with {@link de.schosin.ecs.buildtools.codegen.ComponentSetConfig @ComponentSetConfig}.
 * </p>
 * 
 * <p>
 * <b>Example</b>
 * {@snippet:
 * @ComponentSetConfig
 * record PhysicsComponents(
 *     Position pos(),
 *     Velocity velocity();
 *     ComponentRelation<Birthplace, Position> birthplace(),
 *     ComponentRelationResult<Location, Position> locations(),
 *     ComponentResult<Object> all) {}
 * }
 * </p>
 * 
 * The annotation processor will create an interface {@code PhysicsComponentsSet}, an implementation
 * {@code PhysicsComponentsImpl} and create a static method in {@code ComponentSets} for creating 
 * an instance of this component set. The name for the interface and implementation can be altered
 * using {@code @ComponentSetConfig("MyPhysicsComponents")}.
 * 
 * <h3>Manual implementation</h3>
 * 
 * </p>
 * If code generation is not used, a component set must extend {@link ComponentSet} just like described
 * in "Interface declaration". In addition to implementing {@link ComponentSet}, the interface must 
 * provide an instance of {@link ComponentSetData} as a constant "{@code DATA}":
 * </p>
 * 
 * <p>
 * <b>Example</b>
 * {@snippet:
 * interface PhysicsComponents extends ComponentSet {
 *     ComponentSetData<PhysicsComponents> DATA = ...;
 * 
 *     Position pos();
 *     Velocity velocity();
 *     ComponentRelation<Birthplace, Position> birthplace();
 *     ComponentRelationResult<Location, Position> locations();
 *     ComponentResult<Object> all();
 * }
 * }
 * </p>
 * 
 * <p>
 * To create an instance of {@link ComponentSetData}, use {@link ComponentSet#builder(Factory)}
 * and pass a factory method as an implementation of the functional interface {@link Factory}.
 * It is suggested to use a method reference to a private static method in the interface or implementation,
 * as this factory is only useful for the core library and should not be used in regular code.
 * </p>
 * 
 * <p>
 * A useful pattern for creating that factory method is to first create a type-safe factory method in
 * the implementation class that accepts an "{@code int entityId}" and all components in a predefined
 * order. After that add a private factory method matching the signature of {@link Factory<PhysicsComponents>}
 * and implement it based on the type-safe factory method.
 * </p>
 * 
 * </p>
 * That factory method can then be used for creating the {@link ComponentSetData}. Additionally every
 * component used in the factory method must be added using {@link ComponentSetDataBuilder#add(ComponentAccessor)}
 * or {@link ComponentSetDataBuilder#add(ComponentType, Function)} in the same order as they are read from the 
 * {@code components} parameter. The correct order ensures that the core library will call the method with the
 * correct components. 
 * </p>
 * 
 * <p>
 * <b>Example implementation</b>
 * {@snippet:
 * public interface PhysicsComponents extends ComponentSet {
 *     ComponentSetData<PhysicsComponents> DATA = PhysicsComponentsImpl.DATA;

 *     static PhysicsComponents get(Position pos, ..., ComponentResult<Object> all) {
 *         return PhysicsComponentsImpl.get(-1, pos, ..., all);
 *     }
 *     
 *     // accessors
 * }
 * 
 * class PhysicsComponentsImpl implements PhysicsComponents {
 *     static final ComponentSetData<PhysicsComponents> DATA = ComponentSet.builder(PhysicsComponentsImpl::factory)
 *             .add(new ComponentAccessor<>(PhysicsComponents::pos) {})
 *             // ...
 *             .add(new ComponentAccessor<>(PhysicsComponents::all) {})
 *             .build();
 *             
 *     static PhysicsComponents get(int entityId, Position pos, ..., ComponentResult<Object> all) {
 *         // implement rest
 *     }
 *     
 *     private static PhysicsComponents factory(int entityId, Object... components) {
 *         return get(entityId, (Position) components[0], ..., (ComponentResult<Object>) components[4]);
 *     }
 *     
 *     // fields, accessors
 * }
 * }
 * </p>
 * 
 * <h3>Implementation notes:</h2>
 * 
 * <p>
 * Following are additional notes on using or implementing a {@link ComponentSet}.
 * </p>
 * 
 * <h3>Sealed component sets<h3>
 * 
 * The {@code sealed} keyword can be utilized to use code generation and implement component sets
 * manually. The code generation will not create an implementation for any sealed 
 * {@link ComponentSet ComponentSets} it discovers. 
 * 
 * <h3>Pooling</h3>
 * 
 * <p>
 * {@link ComponentSet} does extend {@link Pooled} to reduce GC pressure by reusing instances.
 * When manually implementing a {@link ComponentSet} it is highly recommended to implement pooling
 * as well. For a proper implementation the following points have to be followed:
 * </p>
 * 
 * <ul>
 *  <li>The implementation must have a constant field to hold pooled instances 
 *  (see {@link de.schosin.ecs.utils.collections.Pool Pool} from the utils module as an option)</li>
 *  <li>The implementation must override {@link ComponentSet#free() void free()} and return itself to the pool</li>
 *  <li>The implementation should override {@link Pooled#reset() void reset()} and set all fields to {@code null}</li>
 *  <li>The factory method {@code get} must use the pool to retrieve an instance and initialize it before returning</li>
 * </ul>
 * 
 * <h3>Code generation</h3>
 * 
 * <p>
 * When using code generation, an implementation will be generated automatically and will have
 * the same name as the component set with {@literal Impl} appended 
 * (e.g. "{@code MyComponentSetImpl implements MyComponentSet}").
 * When these are only used within the same project, the interface does not have to use
 * {@link Implementation @Implementation}, as the generated ComponentSets type will handle
 * that.
 * </p>
 * 
 * <p>
 * When using the generated classes as a dependency for another project that uses code generation
 * itself, the intefaces must be annotated with {@link Implementation @Implementation}, as the
 * newly generated ComponentSets will not contain that information. 
 * </p>
 */
public interface ComponentSet extends Pooled {

    @FunctionalInterface
    interface Factory<S extends ComponentSet> {
        S create(int entityId, Object... components);
    }

    record ComponentData<S extends ComponentSet, T, R>(ComponentType<T, R> type, Function<S, R> accessor) {
    }

    sealed interface ComponentSetData<S extends ComponentSet> {
        Factory<S> factory();

        List<ComponentData<S, ?, ?>> components();
    }

    sealed interface ComponentSetDataBuilder<S extends ComponentSet> {
        <T, R> ComponentSetDataBuilder<S> add(ComponentType<T, R> componentType, Function<S, R> accessor);

        <R> ComponentSetDataBuilder<S> add(ComponentAccessor<S, R> data);

        ComponentSetData<S> build();
    }

    abstract class ComponentAccessor<S extends ComponentSet, R> extends AbstractComponent<S, R> {
        public ComponentAccessor(Function<S, R> accessor) {
            super(accessor);
        }
    }

    static <S extends ComponentSet> ComponentSetDataBuilder<S> builder(Factory<S> factory) {
        return new ComponentSetDataBuilderImpl<>(factory);
    }

    /**
     * Returns the id of the entity that owns the components. Must be overriden if implemented manually.
     * 
     * @return id of entity
     */
    int entityId();

    /**
     * Called when the API does not need this instance anymore.
     * Can be used to return it to a pool to be reused later.
     */
    default void free() {
    }

}

final class ComponentSetDataBuilderImpl<S extends ComponentSet> implements ComponentSetDataBuilder<S> {

    private final Factory<S> factory;
    private final List<ComponentData<S, ?, ?>> components = new ArrayList<>();

    public ComponentSetDataBuilderImpl(Factory<S> factory) {
        this.factory = factory;
    }

    @Override
    public <T, R> ComponentSetDataBuilder<S> add(ComponentType<T, R> componentType, Function<S, R> accessor) {
        this.components.add(new ComponentData<>(componentType, accessor));

        return this;
    }

    @Override
    public <R> ComponentSetDataBuilder<S> add(ComponentAccessor<S, R> data) {
        this.components.add(new ComponentData<>(data.componentType, data.accessor));

        return this;
    }

    @Override
    public ComponentSetData<S> build() {
        return new ComponentSetDataImpl<>(factory, components);
    }

}

final class ComponentSetDataImpl<S extends ComponentSet> implements ComponentSetData<S> {

    private final Factory<S> factory;
    private final List<ComponentData<S, ?, ?>> components;

    ComponentSetDataImpl(Factory<S> factory, List<ComponentData<S, ?, ?>> components) {
        this.factory = factory;
        this.components = components;
    }

    @Override
    public Factory<S> factory() {
        return factory;
    }

    @Override
    public List<ComponentData<S, ?, ?>> components() {
        return components;
    }
}

abstract class AbstractComponent<S extends ComponentSet, R> {

    final Function<S, R> accessor;
    final ComponentType<?, R> componentType;

    public AbstractComponent(Function<S, R> accessor) {
        this.accessor = Objects.requireNonNull(accessor, "accessor cannot be null");
        this.componentType = resolveComponentType();
    }

    @SuppressWarnings("unchecked")
    private ComponentType<?, R> resolveComponentType() {
        var component = getClass().getGenericSuperclass() instanceof ParameterizedType type ? type.getActualTypeArguments()[1] : null;
        if (component == null) {
            throw new IllegalStateException("Failed to determine component type from '%s': Use #add(ComponentType, Function) instead.".formatted(this.getClass()));
        }

        if (component instanceof TypeVariable<?>) {
            throw new IllegalArgumentException("When using the short constructor, a new class has to be created: new ComponentData<>(MySet::myComponent) {}");
        }

        if (component instanceof Class<?> clazz) {
            return (ComponentType<?, R>) ComponentType.component(clazz);
        }

        if (component instanceof ParameterizedType parameterized) {
            var rawType = parameterized.getRawType();

            if (rawType == ComponentRelation.class) {
                var relationship = (Class<?>) parameterized.getActualTypeArguments()[0];
                var target = (Class<?>) parameterized.getActualTypeArguments()[1];

                return (ComponentType<?, R>) ComponentType.exclusiveRelation(relationship.asSubclass(Exclusive.class), target);
            }

            if (rawType == ComponentRelationResult.class) {
                var relationship = (Class<?>) parameterized.getActualTypeArguments()[0];
                var target = (Class<?>) parameterized.getActualTypeArguments()[1];

                return (ComponentType<?, R>) ComponentType.relation(relationship, target);
            }

            if (rawType == EntityRelation.class) {
                var relationship = (Class<?>) parameterized.getActualTypeArguments()[0];

                return (ComponentType<?, R>) ComponentType.exclusiveRelation(relationship.asSubclass(Exclusive.class));
            }

            if (rawType == EntityRelationResult.class) {
                var relationship = (Class<?>) parameterized.getActualTypeArguments()[0];

                return (ComponentType<?, R>) ComponentType.relation(relationship);
            }

            if (rawType == ComponentResult.class) {
                var bound = (Class<?>) parameterized.getActualTypeArguments()[0];

                return (ComponentType<?, R>) ComponentType.wildcard(bound);
            }
        }

        throw new IllegalStateException("Failed to determine component type from '%s': Use #add(ComponentType, Function) instead.".formatted(component));
    }

}