package de.schosin.ecs.plugins.composition;

import java.util.HashSet;
import java.util.Set;
import java.util.function.IntConsumer;
import java.util.function.UnaryOperator;
import java.util.stream.IntStream;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;

import de.schosin.ecs.api.World;
import de.schosin.ecs.api.components.mappers.Components;
import de.schosin.ecs.api.components.types.ComponentType;

/**
 * A composition describes the component composition for entities. Entities can be limited by the following aspects:
 * 
 * <ul>
 *  <li>all: An entity must have all components of a given set</li>
 *  <li>one: An entity must have atleast one component of a given set</li>
 *  <li>none: An entity must not have any components of a given set</li>
 * </ul>
 * 
 * Note that calls to {@link #all(Class...)} and {@link #none(Class...)} append further classes, but calls to
 * {@link #one(Class...)} will be independently checked.
 * 
 * The composition can then be used to listen to lifecycle events of entities matching the composition.
 * Use {@link #inserted(IntConsumer)} to be notified when a relevant entity is created or modified,
 * and {@link #removed(IntConsumer)} when an earlier {@link #inserted(IntConsumer) inserted} entity is
 * removed from the world or its component composition is changed such that it does not match this composition
 * anymore.
 * 
 * <p>
 * The method {@link #process(IntConsumer)} can be used to process all entities matching this composition.
 * Note that this will still return {@link World#deleteEntity(int) deleted} or {@link Components#remove(int) modified} 
 * entities until the {@link World#process() world is processed}.
 * </p>
 */
@NullMarked
public interface Composition extends Spec {

    /**
     * Creates a new empty builder that will match all entities.
     * 
     * @return builder
     */
    static Builder all() {
        return builder();
    }

    /**
     * Adds the components to a new builder, limiting the composition to entities that have
     * all of the passed components.
     *
     * @param components types of components
     * @return builder
     */
    static Builder all(ComponentType<?, ?>... components) {
        return builder().all(components);
    }

    /**
     * Adds the classes to a new builder, limiting the composition to entities that have
     * all of the passed components.
     *
     * @param classes classes of components
     * @return builder
     */
    static Builder all(Class<?>... classes) {
        return builder().all(classes);
    }

    /**
     * Adds the nested builders to a new builder, limiting the composition to entities that match
     * all of the passed builders.
     *
     * @param builders nested builders
     * @return builder
     */
    static Builder all(Builder... builders) {
        return builder().all(builders);
    }

    /**
     * Applies the {@code operator} to a new builder, modifying the composition to only match
     * entities that match all of the requirements set on the {@link BaseComposition.Group Group}.
     *
     * @param operator callback
     * @return builder
     */
    static Builder all(UnaryOperator<Group> operator) {
        return builder().all(operator);
    }

    /**
     * Adds the components to a new builder as a new group, limiting the composition to entities that have
     * any of the passed components.
     *
     * @param components types of components
     * @return builder
     */
    static Builder one(ComponentType<?, ?>... components) {
        return builder().one(components);
    }

    /**
     * Adds the classes to a new builder as a new group, limiting the composition to entities that have
     * any of the passed components.
     *
     * @param classes classes of components
     * @return builder
     */
    static Builder one(Class<?>... classes) {
        return builder().one(classes);
    }

    /**
     * Adds the nested builders to a new builder as a new group, limiting the composition to entities
     * that match any of the passed builders.
     *
     * @param builders nested builders
     * @return builder
     */
    static Builder one(Builder... builders) {
        return builder().one(builders);
    }

    /**
     * Applies the {@code operator} to a new {@link BaseComposition.Group Group} that is added a new builder,
     * modifying the composition to only match entities that match any of the requirements
     * set on the {@link BaseComposition.Group Group}.
     *
     * @param operator callback
     * @return builder
     */
    static Builder one(UnaryOperator<Group> operator) {
        return builder().one(operator);
    }

    /**
     * Adds the components to a new builder, limiting the composition to entities that have
     * none of the passed components.
     *
     * @param components types of components
     * @return builder
     */
    static Builder none(ComponentType<?, ?>... components) {
        return builder().none(components);
    }

    /**
     * Adds the classes to a new builder, limiting the composition to entities that have
     * none of the passed components.
     *
     * @param classes classes of components
     * @return builder
     */
    static Builder none(Class<?>... classes) {
        return builder().none(classes);
    }

    /**
     * Adds the nested builders to a new builder, limiting the composition to entities that match
     * none of the passed builders.
     *
     * @param builders nested builders
     * @return builder
     */
    static Builder none(Builder... builders) {
        return builder().none(builders);
    }

    /**
     * Applies the {@code operator} to a new builder, modifying the composition to only match
     * entities that match none of the requirements set on the {@link BaseComposition.Group Group}.
     *
     * @param operator callback
     * @return builder
     */
    static Builder none(UnaryOperator<Group> operator) {
        return builder().none(operator);
    }

    private static Builder builder() {
        return new Builder();
    }

    /**
     * Returns true if this composition is atleast as strict as the {@link Spec spec}.
     * The following criteria must be met:
     * 
     * <ul>
     * <li>If {@code spec} defines {@code all}, {@code this} must define the same components as {@code all}</li>
     * <li>If {@code spec} defines {@code one}, {@code this} must define the same components as {@code one}</li>
     * <li>If {@code spec} defines {@code none}, {@code this} must define the same components as {@code none}</li>
     * </ul>
     * 
     * <p>
     * This composition may be more strict and contain additional restrictings in each aspect (all, one, none).
     * Note that restrictions for one means fewer types, whereas for all and none it means more types.
     * </p>
     * 
     * <p>
     * <b>Note:</b> {@link Composition} extends {@link Spec} and can be used as well.
     * </p>
     * 
     * @param spec spec to test against
     * @return true if this composition is atleast as strict as the spec
     */
    boolean matches(Spec spec);

    /**
     * Callback for whenever an entity matching this composition is created
     * or modified in such a way that it matches this composition.
     * 
     * The callback will only be called once for an entities lifecycle unless
     * it has been {@link #remove() removed} due to composition changes.
     * 
     * *Attention:* Since entity ids may be reused after an entity was removed
     * from the world, any caching of entity ids (e.g. lookup maps) should be 
     * cleaned up via {@link #removed(IntConsumer)}
     * 
     * @param inserted callback
     */
    void inserted(@NonNull IntConsumer inserted);

    /**
     * Callback for whenever an entity matching this composition is removed from
     * the world or modified in such a way that it no longer matches this composition.
     * 
     * @param removed callback
     */
    void removed(@NonNull IntConsumer removed);

    /**
     * Returns the number of entities matching this composition.
     * 
     * @return number of entities
     */
    int getCount();

    /**
     * Returns true if this composition does not contain any entities.
     * 
     * @return true if no entities with this composition exist
     */
    boolean isEmpty();

    /**
     * Process all entities matching this composition.
     * 
     * <p>
     * <b>Attention:</b> If this method is running while another thread is {@link World#process() processing the world},
     * entities might be skipped or the callback might see zeros (0) for the entity id.
     * </p>
     * 
     * @param process callback
     */
    void process(@NonNull IntConsumer process);

    /**
     * Returns a stream containing all entity ids for this composition.
     * 
     * <p>
     * Prefer {@link #process(IntConsumer)} as that method is allocation free.
     * </p>
     * 
     * <p>
     * <b>Attention:</b> If this method is running while another thread is {@link World#process() processing the world},
     * entities might be skipped or the callback might see zeros (0) for the entity id.
     * </p>
     * 
     * @return stream for processing entities
     */
    IntStream stream();

    /**
     * Returns a parallel stream containing all entity ids for this composition.
     * 
     * <p>
     * Prefer {@link #process(IntConsumer)} as that method is allocation free.
     * </p>
     * 
     * <p>
     * <b>Attention:</b> If this method is running while another thread is {@link World#process() processing the world},
     * entities might be skipped or the callback might see zeros (0) for the entity id.
     * </p>
     * 
     * @return parallel stream for processing entities
     */
    IntStream parallelStream();

    final class Builder {

        private final Group all = new Group();
        private final Set<Group> ones = new HashSet<>();
        private final Group none = new Group();

        /**
         * Adds the components to this builder, limiting the composition to entities that have
         * all of the passed components.
         *
         * @param components types of components
         * @return this builder
         */
        public Builder all(ComponentType<?, ?>... components) {
            all.add(components);
            return this;
        }

        /**
         * Adds the classes to this builder, limiting the composition to entities that have
         * all of the passed components.
         *
         * @param classes classes of components
         * @return this builder
         */
        public Builder all(Class<?>... classes) {
            all.add(classes);
            return this;
        }

        /**
         * Adds the nested builders to this builder, limiting the composition to entities that match
         * all of the passed builders.
         *
         * @param builders nested builders
         * @return this builder
         */
        public Builder all(Builder... builders) {
            all.add(builders);
            return this;
        }

        /**
         * Applies the {@code operator} to this builder, modifying the composition to only match
         * entities that match all of the requirements set on the {@link BaseComposition.Group Group}.
         *
         * @param operator callback
         * @return this builder
         */
        public Builder all(UnaryOperator<Group> operator) {
            operator.apply(all);
            return this;
        }

        /**
         * Adds the components to this builder as a new group, limiting the composition to entities that have
         * any of the passed components.
         *
         * @param components types of components
         * @return this builder
         */
        public Builder one(ComponentType<?, ?>... components) {
            if (components.length == 0) {
                return this;
            }
            return one(group -> group.add(components));
        }

        /**
         * Adds the classes to this builder as a new group, limiting the composition to entities that have
         * any of the passed components.
         *
         * @param classes classes of components
         * @return this builder
         */
        public Builder one(Class<?>... classes) {
            if (classes.length == 0) {
                return this;
            }
            return one(group -> group.add(classes));
        }

        /**
         * Adds the nested builders to this builder as a new group, limiting the composition to entities
         * that match any of the passed builders.
         *
         * @param builders nested builders
         * @return this builder
         */
        public Builder one(Builder... builders) {
            if (builders.length == 0) {
                return this;
            }
            return one(group -> group.add(builders));
        }

        /**
         * Applies the {@code operator} to a new {@link BaseComposition.Group Group} that is added this builder,
         * modifying the composition to only match entities that match any of the requirements
         * set on the {@link BaseComposition.Group Group}.
         *
         * @param operator callback
         * @return this builder
         */
        public Builder one(UnaryOperator<Group> operator) {
            var group = new Group();
            operator.apply(group);
            if (group.isEmpty()) {
                throw new IllegalStateException("Group is empty, add atleast one class or builder.");
            }
            this.ones.add(group);
            return this;
        }

        /**
         * Adds the components to this builder, limiting the composition to entities that have
         * none of the passed components.
         *
         * @param components types of components
         * @return this builder
         */
        public Builder none(ComponentType<?, ?>... components) {
            none.add(components);
            return this;
        }

        /**
         * Adds the classes to this builder, limiting the composition to entities that have
         * none of the passed components.
         *
         * @param classes classes of components
         * @return this builder
         */
        public Builder none(Class<?>... classes) {
            none.add(classes);
            return this;
        }

        /**
         * Adds the nested builders to this builder, limiting the composition to entities that match
         * none of the passed builders.
         *
         * @param builders nested builders
         * @return this builder
         */
        public Builder none(Builder... builders) {
            none.add(builders);
            return this;
        }

        /**
         * Applies the {@code operator} to this builder, modifying the composition to only match
         * entities that match none of the requirements set on the {@link BaseComposition.Group Group}.
         *
         * @param operator callback
         * @return this builder
         */
        public Builder none(UnaryOperator<Group> operator) {
            operator.apply(none);
            return this;
        }

        public final Group getAll() {
            return this.all;
        }

        public final Set<Group> getOnes() {
            return this.ones;
        }

        public final Group getNone() {
            return this.none;
        }

        @Override
        public String toString() {
            var comma = false;
            var builder = new StringBuilder();
            builder.append(this.getClass().getSimpleName()).append("(");
            if (!this.all.isEmpty()) {
                builder.append(this.all);
                comma = true;
            }
            if (!this.ones.isEmpty()) {
                if (comma) {
                    builder.append(", ");
                }
                builder.append(this.ones);
                comma = true;
            }
            if (!this.none.isEmpty()) {
                if (comma) {
                    builder.append(", ");
                }
                builder.append(this.none);
            }
            builder.append(")");
            return builder.toString();
        }
    }

    record Group(Set<ComponentType<?, ?>> components, Set<Builder> builders) {

        Group() {
            this(new HashSet<>(), new HashSet<>());
        }

        public Group copy() {
            var copy = new Group();
            copy.components.addAll(this.components);
            copy.builders.addAll(this.builders);

            return copy;
        }

        public Group add(Class<?>... classes) {
            for (var clazz : classes) {
                this.components.add(ComponentType.component(clazz));
            }

            return this;
        }

        public Group add(ComponentType<?, ?>... components) {
            for (var component : components) {
                this.components.add(component);
            }

            return this;
        }

        public Group add(Builder... builders) {
            for (var builder : builders) {
                this.builders.add(builder);
            }

            return this;
        }

        public boolean isEmpty() {
            return components.isEmpty() && builders.isEmpty();
        }

    }

}
