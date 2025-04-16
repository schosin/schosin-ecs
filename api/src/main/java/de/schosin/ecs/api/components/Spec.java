package de.schosin.ecs.api.components;

import de.schosin.ecs.api.World;
import de.schosin.ecs.api.archetype.Transmuter.Builder.AbstractBuilder;
import de.schosin.ecs.api.components.Composition.Builder;

public interface Spec {

    /**
     * Creates a composition builder. 
     * 
     * @see AbstractBuilder#all(Class...)
     * @see World#createComposition(AbstractBuilder)
     */
    static Builder all(Class<?>... classes) {
        return Composition.all(classes);
    }

    /**
     * Creates a composition builder.
     * 
     * @see AbstractBuilder#one(Class...)
     * @see World#createComposition(AbstractBuilder)
     */
    static Builder one(Class<?>... classes) {
        return Composition.one(classes);
    }

    /**
     * Creates a composition builder.
     * 
     * @see AbstractBuilder#none(Class...)
     * @see World#createComposition(AbstractBuilder)
     */
    static Builder none(Class<?>... classes) {
        return Composition.none(classes);
    }

    boolean isInterested(int entityId);

    interface Creator {

        /**
         * Creates a {@link Spec} from a builder.
         * Intended to be used instead of {@link Composition} if the additional
         * functionality and overhead is not needed.
         * 
         * <p>
         * {@link Spec2#isInterested(int)} can be faster than {@link Composition#}
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
