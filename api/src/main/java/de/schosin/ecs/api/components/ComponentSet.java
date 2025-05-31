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
import de.schosin.ecs.api.components.mappers.ComponentSetMapper;
import de.schosin.ecs.api.components.types.ComponentSetType;
import de.schosin.ecs.api.components.types.ComponentType;
import de.schosin.ecs.api.data.DataProcessor;

/**
 * Base interface for component sets. A component set consists of one or more
 * components and can be used to access, add and remove a set of components at once.
 * 
 * <p>
 * Concrete component sets are generated using an annotation processor (APT), which requires
 * the artifact "{@code de.schosin.ecs.buildtools:ecs-build-tools-codegen-apt}" to be added as an APT.
 * </p>
 * 
 * <p>
 * A component set is intended to be used in combination with the composition plugin and
 * {@link de.schosin.ecs.plugins.composition.CompositionSet CompositionSet}. As such a 
 * component set is defined by the method that can process it. To create the requires types,
 * such a method must be annotated with {@link ComponentSetConfig @ComponentSetConfig} and 
 * given a name for the generated component set.
 * 
 * {@snippet:
 * @ComponentSetConfig("PhysicsComponentSet")
 * void processEntities(int entityId, Position position, Velocity velocity) {
 *     // system logic
 * } 
 * }
 * 
 * When the APT runs at compile time, this will generate an interface {@code PhysicsComponentSet}
 * along with an implementation. Additionally, a type {@code ComponentSets} is generated that
 * contains static factory methods for creating an instance of a component set.
 * 
 * {@snippet:
 * public interface PhysicsComponentSet {
 *     ComponentSetType<PhysicsComponentSet, Processor> TYPE = ...;
 *     
 *     static PhysicsComponentSet get(int entityId, Position position, Velocity velocity) { ... }
 *     static PhysicsComponentSet get(Position position, Velocity velocity) { ... }
 *     
 *     Position position();
 *     Velocity velocity();
 *     
 *     // additional types and fields
 * }
 * }
 * </p>
 * 
 * <p>
 * The generated field {@code PhysicsComponentSet.TYPE} can be used to retrieve a {@link ComponentSetMapper}
 * from {@link World#getComponents(ComponentSetType)}, which can be used to access the components defined by
 * the component set. Additionally the composition plugin provides direct support for component sets.
 * </p>
 */
public interface ComponentSet<P extends DataProcessor<?>> extends Pooled {

    /**
     * Functional interface for the factory method to instantiate the component set by 
     * the world.
     * 
     * @param <S> type of component set
     */
    @FunctionalInterface
    interface Factory<S extends ComponentSet<?>> {
        S create(int entityId, Object... components);
    }

    /**
     * Record holding the {@link ComponentType} and getter for a component of the set.
     * 
     * @param <S> type of set
     * @param <T> maps to {@link ComponentType} {@code T} (write operations)
     * @param <R> maps to {@link ComponentType} {@code R} (read operations)
     */
    record ComponentData<S extends ComponentSet<?>, T, R>(ComponentType<T, R> type, Function<S, R> accessor) {
    }

    /**
     * Interface for providing the factory and list of components of a component set.
     * 
     * @param <S> type of component set
     */
    sealed interface ComponentSetData<S extends ComponentSet<?>> {
        Factory<S> factory();

        List<ComponentData<S, ?, ?>> components();
    }

    /**
     * Builder for creating an instance of {@link ComponentSetData}. Components must be described
     * in the same order that the passed {@link Factory} evaluates the varargs array.
     * 
     * <p>
     * Use {@link ComponentSet#builder(Factory)} to obtain a builder instance.
     * </p>
     * 
     * @param <S> type of component set
     */
    sealed interface ComponentSetDataBuilder<S extends ComponentSet<?>> {

        /**
         * Adds a component by creating an anonymous implementation of {@link ComponentAccessor}. Allows
         * to just pass the accessor.
         * 
         * {@snippet:
         * builder.add(new ComponentAccessor<>(MyCompoentSet::getPosition) {})
         * }
         */
        <R> ComponentSetDataBuilder<S> add(ComponentAccessor<S, R> data);

        /**
         * Creates the {@link ComponentSetData} instance.
         */
        ComponentSetData<S> build();

    }

    /**
     * Abstract class used by {@link ComponentSetDataBuilder#add(ComponentAccessor)} to obtain the
     * {@link ComponentType} from a method reference to the accessor of a component.
     * 
     * @param <S> type of set
     * @param <R> type of component
     */
    abstract class ComponentAccessor<S extends ComponentSet<?>, R> extends AbstractComponent<S, R> {
        public ComponentAccessor(Function<S, R> accessor) {
            super(accessor);
        }
    }

    /**
     * Creates a {@link ComponentSetDataBuilder} instance given the factory method.
     */
    static <S extends ComponentSet<?>> ComponentSetDataBuilder<S> builder(Factory<S> factory) {
        return new ComponentSetDataBuilderImpl<>(factory);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    default void process(P processor) {
        ((DataProcessor) processor).process(entityId(), this);
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

final class ComponentSetDataBuilderImpl<S extends ComponentSet<?>> implements ComponentSetDataBuilder<S> {

    private final Factory<S> factory;
    private final List<ComponentData<S, ?, ?>> components = new ArrayList<>();

    public ComponentSetDataBuilderImpl(Factory<S> factory) {
        this.factory = factory;
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

final class ComponentSetDataImpl<S extends ComponentSet<?>> implements ComponentSetData<S> {

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

abstract class AbstractComponent<S extends ComponentSet<?>, R> {

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