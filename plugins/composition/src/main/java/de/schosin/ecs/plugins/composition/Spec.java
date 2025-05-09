package de.schosin.ecs.plugins.composition;

import de.schosin.ecs.plugins.composition.Composition.Builder;

public interface Spec {

    /**
     * Creates a composition builder. 
     * 
     * @see Builder#all(Class...)
     * @see CompositionPlugin#createComposition(Builder)
     */
    static Builder all(Class<?>... classes) {
        return Composition.all(classes);
    }

    /**
     * Creates a composition builder.
     * 
     * @see Builder#one(Class...)
     * @see CompositionPlugin#createComposition(Builder)
     */
    static Builder one(Class<?>... classes) {
        return Composition.one(classes);
    }

    /**
     * Creates a composition builder.
     * 
     * @see Builder#none(Class...)
     * @see CompositionPlugin#createComposition(Builder)
     */
    static Builder none(Class<?>... classes) {
        return Composition.none(classes);
    }

    boolean isInterested(int entityId);

    interface SpecCreator {

        /**
         * Creates a {@link Spec} from a builder.
         * Intended to be used instead of {@link Composition} if the additional
         * functionality and overhead is not needed.
         * 
         * <p>
         * {@link Spec#isInterested(int)} can be faster than {@link Composition#}
         * </p>
         * 
         * Can be used for testing entities
         * and other Specs and {@link Composition Compositions}
         * 
         * @param builder
         * @return
         */
        Spec createSpec(Composition.Builder builder);

    }

}
