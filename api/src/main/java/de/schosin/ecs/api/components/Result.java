package de.schosin.ecs.api.components;

import java.util.Iterator;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import de.schosin.ecs.api.components.Relation.ComponentRelation;
import de.schosin.ecs.api.components.Relation.EntityRelation;

/**
 * Represents a result containing no, one or more components matching
 * the type bound {@code T}. 
 * 
 * <p>
 * <b>Note:</b> This interface extends both {@link Iterable} and {@link Iterator}.
 * When using it as an interable (e.g. in an enhanced for loop), it resets and
 * returns itself.
 * </p>
 * 
 * <p>
 * <b>Attention:</b> APIs may choose to pool these objects. Do not hold onto 
 * instances of Result, as they may be reused. See the documentation of APIs
 * accepting {@link ComponentType} for details on how long an instance of this
 * type can be used and when it will be reclaimed.
 * </p>
 * 
 * @param <T> type bound
 */
public sealed interface Result<T> extends Iterable<T> {

    non-sealed interface ComponentResult<T> extends Result<T> {

        /**
         * Retrieves a component that has the given {@code clazz}. The components
         * will be checked by {@code component.getClass() == clazz}.
         *  
         * @param <R> type of component
         * @param clazz class of component
         * @return matching component, or null if not present
         */
        @Nullable
        <R extends T> R get(Class<R> clazz);

    }

    non-sealed interface ComponentRelationResult<R, T> extends Result<ComponentRelation<R, T>> {

        /**
         * Retrieves the relationship given the target component. The target will be
         * checked by {@link #equals(Object)} against existing relations, returning 
         * its relationship if it is equal.
         * 
         * @param target target component
         * @return relationship component, or null if not present
         */
        R getRelationship(T target);

    }

    non-sealed interface EntityRelationResult<R> extends Result<EntityRelation<R>> {

        /**
         * Retrieves the relationship given the target entity.
         * 
         * @param target target entity
         * @return relationship component, or null if not present
         */
        R getRelationship(int target);

    }

    /**
     * Retrieves the component by its zero-based index.
     * 
     * @param i zero-based index
     * @return component instance
     * @throws ArrayIndexOutOfBoundsException if {@literal i >= size()}
     */
    @NonNull
    T get(int i);

    /**
     * @return number of components in this result
     */
    int size();

    /**
     * @return true if this result contains no components
     */
    boolean isEmpty();

}
