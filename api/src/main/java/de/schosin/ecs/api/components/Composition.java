package de.schosin.ecs.api.components;

import java.util.HashSet;
import java.util.Set;
import java.util.function.IntConsumer;
import java.util.stream.IntStream;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;

import de.schosin.ecs.api.World;
import de.schosin.ecs.api.archetype.Transmuter.Builder.AbstractBuilder;

/**
 * A composition describes the component composition for entities. Entities can be limited by the following aspects:
 * 
 * <ul>
 *  <li>all: An entity must have all components of a given set</li>
 *  <li>one: An entity must have atleast one component of a given set</li>
 *  <li>none: An entity must not have any components of a given set</li>
 * </ul>
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
public interface Composition {

    sealed interface Of<C> {
        void process(int entityId, @NonNull C process);

        void process(@NonNull C process);

        void inserted(@NonNull C callback);

        void removed(@NonNull C callback);
    }

    non-sealed interface Of1<T1> extends Composition, Of<Of1.Consumer<T1>> {
        interface Consumer<T1> {
            void consume(int entityId, T1 component1);
        }
    }

    non-sealed interface Of2<T1, T2> extends Composition, Of<Of2.Consumer<T1, T2>> {
        interface Consumer<T1, T2> {
            void consume(int entityId, T1 component1, T2 component2);
        }
    }

    non-sealed interface Of3<T1, T2, T3> extends Composition, Of<Of3.Consumer<T1, T2, T3>> {
        interface Consumer<T1, T2, T3> {
            void consume(int entityId, T1 component1, T2 component2, T3 component3);
        }
    }

    non-sealed interface Of4<T1, T2, T3, T4> extends Composition, Of<Of4.Consumer<T1, T2, T3, T4>> {
        interface Consumer<T1, T2, T3, T4> {
            void consume(int entityId, T1 component1, T2 component2, T3 component3, T4 component4);
        }
    }

    non-sealed interface Of5<T1, T2, T3, T4, T5> extends Composition, Of<Of5.Consumer<T1, T2, T3, T4, T5>> {
        interface Consumer<T1, T2, T3, T4, T5> {
            void consume(int entityId, T1 component1, T2 component2, T3 component3, T4 component4, T5 component5);
        }
    }

    non-sealed interface Of6<T1, T2, T3, T4, T5, T6> extends Composition, Of<Of6.Consumer<T1, T2, T3, T4, T5, T6>> {
        interface Consumer<T1, T2, T3, T4, T5, T6> {
            void consume(int entityId, T1 component1, T2 component2, T3 component3, T4 component4, T5 component5, T6 component6);
        }
    }

    non-sealed interface Of7<T1, T2, T3, T4, T5, T6, T7> extends Composition, Of<Of7.Consumer<T1, T2, T3, T4, T5, T6, T7>> {
        interface Consumer<T1, T2, T3, T4, T5, T6, T7> {
            void consume(int entityId, T1 component1, T2 component2, T3 component3, T4 component4, T5 component5, T6 component6, T7 component7);
        }
    }

    non-sealed interface Of8<T1, T2, T3, T4, T5, T6, T7, T8> extends Composition, Of<Of8.Consumer<T1, T2, T3, T4, T5, T6, T7, T8>> {
        interface Consumer<T1, T2, T3, T4, T5, T6, T7, T8> {
            void consume(int entityId, T1 component1, T2 component2, T3 component3, T4 component4, T5 component5, T6 component6, T7 component7, T8 component8);
        }
    }

    /**
     * Creates a composition builder. 
     * 
     * @see AbstractBuilder#all(Class...)
     * @see World#createComposition(AbstractBuilder)
     */
    static Builder all(Class<?>... classes) {
        return builder().all(classes);
    }

    /**
     * Creates a composition builder.
     * 
     * @see AbstractBuilder#one(Class...)
     * @see World#createComposition(AbstractBuilder)
     */
    static Builder one(Class<?>... classes) {
        return builder().one(classes);
    }

    /**
     * Creates a composition builder.
     * 
     * @see AbstractBuilder#none(Class...)
     * @see World#createComposition(AbstractBuilder)
     */
    static Builder none(Class<?>... classes) {
        return builder().none(classes);
    }

    private static Builder builder() {
        return new Builder();
    }

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

    class Builder {

        private final Set<Class<?>> all = new HashSet<>();
        private final Set<Class<?>> one = new HashSet<>();
        private final Set<Class<?>> none = new HashSet<>();

        /**
         * Limits this composition to entities having all of the given components.
         * 
         * @param classes components an entity have to pocess all of
         * @return this builder instance
         */
        public Builder all(Class<?>... classes) {
            for (var clazz : classes) {
                this.all.add(clazz);
            }

            return this;
        }

        /**
         * Limits this composition to entities having atleast one of the given components.
         * 
         * @param classes components an entity have to pocess atleast one of
         * @return this builder instance
         */
        public Builder one(Class<?>... classes) {
            for (var clazz : classes) {
                this.one.add(clazz);
            }

            return this;
        }

        /**
         * Limits this composition to entities having none of the given components.
         * 
         * @param classes components an entity should not pocess
         * @return this builder instance
         */
        public Builder none(Class<?>... classes) {
            for (var clazz : classes) {
                this.none.add(clazz);
            }

            return this;
        }

        public Set<Class<?>> getAll() {
            return all;
        }

        public Set<Class<?>> getOne() {
            return one;
        }

        public Set<Class<?>> getNone() {
            return none;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("Composition [");
            if (!all.isEmpty()) {
                builder.append("all=").append(all).append(", ");
            }
            if (!one.isEmpty()) {
                builder.append("one=").append(one).append(", ");
            }
            if (!none.isEmpty()) {
                builder.append("none=").append(none);
            }
            builder.append("]");
            return builder.toString();
        }

    }

    interface Creator {

        /**
         * Creates a {@link Composition} from a builder.
         * 
         * <p>
         * <b>Hint:</b> Compositions are not meant to be created on the fly. Create all
         * required compositions before the game loop starts.
         * </p>
         * 
         * @param builder composition builder
         * @return the builder
         */
        Composition createComposition(Composition.Builder builder);

        /**
         * Creates a {@link Composition} from a builder.
         * 
         * <p>
         * <b>Hint:</b> Compositions are not meant to be created on the fly. Create all
         * required compositions before the game loop starts.
         * </p>
         * 
         * @param builder composition builder
         * @return the builder
         */
        <T1> Composition.Of1<T1> createComposition(Composition.Builder builder, Class<T1> component1);

        /**
         * Creates a {@link Composition} from a builder.
         * 
         * <p>
         * <b>Hint:</b> Compositions are not meant to be created on the fly. Create all
         * required compositions before the game loop starts.
         * </p>
         * 
         * @param builder composition builder
         * @return the builder
         */
        <T1, T2> Composition.Of2<T1, T2> createComposition(Composition.Builder builder, Class<T1> component1, Class<T2> component2);

        /**
         * Creates a {@link Composition} from a builder.
         * 
         * <p>
         * <b>Hint:</b> Compositions are not meant to be created on the fly. Create all
         * required compositions before the game loop starts.
         * </p>
         * 
         * @param builder composition builder
         * @return the builder
         */
        <T1, T2, T3> Composition.Of3<T1, T2, T3> createComposition(Composition.Builder builder, Class<T1> component1, Class<T2> component2, Class<T3> component3);

        /**
         * Creates a {@link Composition} from a builder.
         * 
         * <p>
         * <b>Hint:</b> Compositions are not meant to be created on the fly. Create all
         * required compositions before the game loop starts.
         * </p>
         * 
         * @param builder composition builder
         * @return the builder
         */
        <T1, T2, T3, T4> Composition.Of4<T1, T2, T3, T4> createComposition(Composition.Builder builder, Class<T1> component1, Class<T2> component2, Class<T3> component3, Class<T4> component4);

        /**
         * Creates a {@link Composition} from a builder.
         * 
         * <p>
         * <b>Hint:</b> Compositions are not meant to be created on the fly. Create all
         * required compositions before the game loop starts.
         * </p>
         * 
         * @param builder composition builder
         * @return the builder
         */
        <T1, T2, T3, T4, T5> Composition.Of5<T1, T2, T3, T4, T5> createComposition(Composition.Builder builder, Class<T1> component1, Class<T2> component2, Class<T3> component3, Class<T4> component4,
                Class<T5> component5);

        /**
         * Creates a {@link Composition} from a builder.
         * 
         * <p>
         * <b>Hint:</b> Compositions are not meant to be created on the fly. Create all
         * required compositions before the game loop starts.
         * </p>
         * 
         * @param builder composition builder
         * @return the builder
         */
        <T1, T2, T3, T4, T5, T6> Composition.Of6<T1, T2, T3, T4, T5, T6> createComposition(Composition.Builder builder, Class<T1> component1, Class<T2> component2, Class<T3> component3,
                Class<T4> component4, Class<T5> component5, Class<T6> component6);

        /**
         * Creates a {@link Composition} from a builder.
         * 
         * <p>
         * <b>Hint:</b> Compositions are not meant to be created on the fly. Create all
         * required compositions before the game loop starts.
         * </p>
         * 
         * @param builder composition builder
         * @return the builder
         */
        <T1, T2, T3, T4, T5, T6, T7> Composition.Of7<T1, T2, T3, T4, T5, T6, T7> createComposition(Composition.Builder builder, Class<T1> component1, Class<T2> component2, Class<T3> component3,
                Class<T4> component4, Class<T5> component5, Class<T6> component6, Class<T7> component7);

        /**
         * Creates a {@link Composition} from a builder.
         * 
         * <p>
         * <b>Hint:</b> Compositions are not meant to be created on the fly. Create all
         * required compositions before the game loop starts.
         * </p>
         * 
         * @param builder composition builder
         * @return the builder
         */
        <T1, T2, T3, T4, T5, T6, T7, T8> Composition.Of8<T1, T2, T3, T4, T5, T6, T7, T8> createComposition(Composition.Builder builder, Class<T1> component1, Class<T2> component2,
                Class<T3> component3, Class<T4> component4, Class<T5> component5, Class<T6> component6, Class<T7> component7, Class<T8> component8);

    }

}
