package de.schosin.ecs.api.archetype;

import org.jspecify.annotations.NullMarked;

import de.schosin.ecs.api.Pooled;

@NullMarked
public interface Archetype {

    /**
     * Create a new archetype that extends this archetype by adding the passed components to
     * every created entity. The old archetype is not modified.
     * 
     * <p>
     * Intended to be used with marker or singleton components (e.g. enums).
     * </p>
     * 
     * <p>
     * <b>Note:</b> No duplicate components are allowed. If {@link #with(Object...)} adds components
     * defined when creating the archetype, or a previous {@link #with(Object...)} call, an error
     * is thrown.
     * </p>
     * 
     * @param components components to add to every entity
     * @return new archetype
     */
    Archetype with(Object... components);

    /**
     * Returns a pooled instance of the component.
     * 
     * @param <T> type of component
     * @param clazz class of component
     * @return pooled instance
     */
    <T extends Pooled> T getInstance(Class<T> clazz);

    interface Of1<T1> extends Archetype {
        interface Init<T1> {
            <T> T get(Class<T> component);

            void initialize(T1 component1);
        }

        Of1<T1> with(Object... components);

        int create(T1 component1);

        /**
         * Create the given number of entities and initialize their components with
         * the function init.
         * 
         * @param count number of entities to create
         * @param init initialization function
         * @return array of entity ids
         */
        int[] createBatch(int count, Initialize<Init<T1>> init);
    }

    interface Of2<T1, T2> extends Archetype {
        interface Init<T1, T2> {
            <T> T get(Class<T> component);

            void initialize(T1 component1, T2 component2);
        }

        @Override
        Of2<T1, T2> with(Object... components);

        int create(T1 component1, T2 component2);

        /**
         * Create the given number of entities and initialize their components with
         * the function init.
         * 
         * @param count number of entities to create
         * @param init initialization function
         * @return array of entity ids
         */
        int[] createBatch(int count, Initialize<Init<T1, T2>> init);
    }

    interface Of3<T1, T2, T3> extends Archetype {
        interface Init<T1, T2, T3> {
            <T> T get(Class<T> component);

            void initialize(T1 component1, T2 component2, T3 component3);
        }

        @Override
        Of3<T1, T2, T3> with(Object... components);

        int create(T1 component1, T2 component2, T3 component3);

        /**
         * Create the given number of entities and initialize their components with
         * the function init.
         * 
         * @param count number of entities to create
         * @param init initialization function
         * @return array of entity ids
         */
        int[] createBatch(int count, Initialize<Init<T1, T2, T3>> init);
    }

    interface Of4<T1, T2, T3, T4> extends Archetype {
        interface Init<T1, T2, T3, T4> {
            <T> T get(Class<T> component);

            void initialize(T1 component1, T2 component2, T3 component3, T4 component4);
        }

        @Override
        Of4<T1, T2, T3, T4> with(Object... components);

        int create(T1 component1, T2 component2, T3 component3, T4 component4);

        /**
         * Create the given number of entities and initialize their components with
         * the function init.
         * 
         * @param count number of entities to create
         * @param init initialization function
         * @return array of entity ids
         */
        int[] createBatch(int count, Initialize<Init<T1, T2, T3, T4>> init);
    }

    interface Of5<T1, T2, T3, T4, T5> extends Archetype {
        interface Init<T1, T2, T3, T4, T5> {
            <T> T get(Class<T> component);

            void initialize(T1 component1, T2 component2, T3 component3, T4 component4, T5 component5);
        }

        @Override
        Of5<T1, T2, T3, T4, T5> with(Object... components);

        int create(T1 component1, T2 component2, T3 component3, T4 component4, T5 component5);

        /**
         * Create the given number of entities and initialize their components with
         * the function init.
         * 
         * @param count number of entities to create
         * @param init initialization function
         * @return array of entity ids
         */
        int[] createBatch(int count, Initialize<Init<T1, T2, T3, T4, T5>> init);
    }

    interface Of6<T1, T2, T3, T4, T5, T6> extends Archetype {
        interface Init<T1, T2, T3, T4, T5, T6> {
            <T> T get(Class<T> component);

            void initialize(T1 component1, T2 component2, T3 component3, T4 component4, T5 component5, T6 component6);
        }

        @Override
        Of6<T1, T2, T3, T4, T5, T6> with(Object... components);

        int create(T1 component1, T2 component2, T3 component3, T4 component4, T5 component5, T6 component6);

        /**
         * Create the given number of entities and initialize their components with
         * the function init.
         * 
         * @param count number of entities to create
         * @param init initialization function
         * @return array of entity ids
         */
        int[] createBatch(int count, Initialize<Init<T1, T2, T3, T4, T5, T6>> init);
    }

    interface Of7<T1, T2, T3, T4, T5, T6, T7> extends Archetype {
        interface Init<T1, T2, T3, T4, T5, T6, T7> {
            <T> T get(Class<T> component);

            void initialize(T1 component1, T2 component2, T3 component3, T4 component4, T5 component5, T6 component6, T7 component7);
        }

        @Override
        Of7<T1, T2, T3, T4, T5, T6, T7> with(Object... components);

        int create(T1 component1, T2 component2, T3 component3, T4 component4, T5 component5, T6 component6, T7 component7);

        /**
         * Create the given number of entities and initialize their components with
         * the function init.
         * 
         * @param count number of entities to create
         * @param init initialization function
         * @return array of entity ids
         */
        int[] createBatch(int count, Initialize<Init<T1, T2, T3, T4, T5, T6, T7>> init);
    }

    interface Of8<T1, T2, T3, T4, T5, T6, T7, T8> extends Archetype {
        interface Init<T1, T2, T3, T4, T5, T6, T7, T8> {
            <T> T get(Class<T> component);

            void initialize(T1 component1, T2 component2, T3 component3, T4 component4, T5 component5, T6 component6, T7 component7, T8 component8);
        }

        @Override
        Of8<T1, T2, T3, T4, T5, T6, T7, T8> with(Object... components);

        int create(T1 component1, T2 component2, T3 component3, T4 component4, T5 component5, T6 component6, T7 component7, T8 component8);

        /**
         * Create the given number of entities and initialize their components with
         * the function init.
         * 
         * @param count number of entities to create
         * @param init initialization function
         * @return array of entity ids
         */
        int[] createBatch(int count, Initialize<Init<T1, T2, T3, T4, T5, T6, T7, T8>> init);
    }

    interface OfN<T1, T2, T3, T4, T5, T6, T7, T8> extends Archetype {
        interface Init<T1, T2, T3, T4, T5, T6, T7, T8> {
            <T> T get(Class<T> component);

            void initialize(T1 component1, T2 component2, T3 component3, T4 component4, T5 component5, T6 component6, T7 component7, T8 component8, Object... components);
        }

        @Override
        OfN<T1, T2, T3, T4, T5, T6, T7, T8> with(Object... components);

        int create(T1 component1, T2 component2, T3 component3, T4 component4, T5 component5, T6 component6, T7 component7, T8 component8, Object... others);

        /**
         * Create the given number of entities and initialize their components with
         * the function init.
         * 
         * <p>
         * <b>Attention:</b> Initializing entities with varying amounts of 
         * {@link Init#initialize(Object, Object, Object, Object, Object, Object, Object, Object, Object...) components}
         * may cause {@link ArrayIndexOutOfBoundsException ArrayIndexOutOfBoundsExceptions} or other issues when processing
         * these entities. Make sure to give each entity the components defined when this archetype was created.
         * </p>
         * 
         * @param count number of entities to create
         * @param init initialization function
         * @return array of entity ids
         */
        int[] createBatch(int count, Initialize<Init<T1, T2, T3, T4, T5, T6, T7, T8>> init);
    }

    interface Initialize<T> {
        /**
         * Function to initialize the entity with its components. The init function
         * must be called. 
         * 
         * @param index index of the entity, from 0 to count-1
         * @param init initialization callback for adding components to the entity
         */
        void initialize(int index, T init);
    }

    interface Creator {

        <T1> Archetype.Of1<T1> createArchetype(Class<T1> component1);

        <T1, T2> Archetype.Of2<T1, T2> createArchetype(Class<T1> component1, Class<T2> component2);

        <T1, T2, T3> Archetype.Of3<T1, T2, T3> createArchetype(Class<T1> component1, Class<T2> component2, Class<T3> component3);

        <T1, T2, T3, T4> Archetype.Of4<T1, T2, T3, T4> createArchetype(Class<T1> component1, Class<T2> component2, Class<T3> component3, Class<T4> component4);

        <T1, T2, T3, T4, T5> Archetype.Of5<T1, T2, T3, T4, T5> createArchetype(Class<T1> component1, Class<T2> component2, Class<T3> component3, Class<T4> component4, Class<T5> component5);

        <T1, T2, T3, T4, T5, T6> Archetype.Of6<T1, T2, T3, T4, T5, T6> createArchetype(Class<T1> component1, Class<T2> component2, Class<T3> component3, Class<T4> component4, Class<T5> component5,
                Class<T6> component6);

        <T1, T2, T3, T4, T5, T6, T7> Archetype.Of7<T1, T2, T3, T4, T5, T6, T7> createArchetype(Class<T1> component1, Class<T2> component2, Class<T3> component3, Class<T4> component4,
                Class<T5> component5, Class<T6> component6, Class<T7> component7);

        <T1, T2, T3, T4, T5, T6, T7, T8> Archetype.Of8<T1, T2, T3, T4, T5, T6, T7, T8> createArchetype(Class<T1> component1, Class<T2> component2, Class<T3> component3, Class<T4> component4,
                Class<T5> component5, Class<T6> component6, Class<T7> component7, Class<T8> component8);

        <T1, T2, T3, T4, T5, T6, T7, T8> Archetype.OfN<T1, T2, T3, T4, T5, T6, T7, T8> createArchetype(Class<T1> component1, Class<T2> component2, Class<T3> component3, Class<T4> component4,
                Class<T5> component5, Class<T6> component6, Class<T7> component7, Class<T8> component8, Class<?>... others);

    }

}
