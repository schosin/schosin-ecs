package de.schosin.ecs.api.archetype;

import org.jspecify.annotations.NullMarked;

import de.schosin.ecs.api.Pooled;

@NullMarked
public interface Archetype {

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
            void initialize(T1 component1);
        }

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
            void initialize(T1 component1, T2 component2);
        }

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
            void initialize(T1 component1, T2 component2, T3 component3);
        }

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
            void initialize(T1 component1, T2 component2, T3 component3, T4 component4);
        }

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
            void initialize(T1 component1, T2 component2, T3 component3, T4 component4, T5 component5);
        }

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
            void initialize(T1 component1, T2 component2, T3 component3, T4 component4, T5 component5, T6 component6);
        }

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
            void initialize(T1 component1, T2 component2, T3 component3, T4 component4, T5 component5, T6 component6, T7 component7);
        }

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
            void initialize(T1 component1, T2 component2, T3 component3, T4 component4, T5 component5, T6 component6, T7 component7, T8 component8);
        }

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
            void initialize(T1 component1, T2 component2, T3 component3, T4 component4, T5 component5, T6 component6, T7 component7, T8 component8, Object... components);
        }

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
