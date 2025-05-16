package de.schosin.ecs.api.components;

import java.util.Iterator;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

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
public interface Result<T> extends Iterable<T> {

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
     * Retrieves a component that has the given {@code clazz}. The components
     * will be checked by {@code component.getClass() == clazz}.
     *  
     * @param <R> type of component
     * @param clazz class of component
     * @return matching component, or null if not present
     */
    @Nullable
    <R extends T> R get(Class<R> clazz);

    /**
     * @return number of components in this result
     */
    int size();

    /**
     * @return true if this result contains no components
     */
    boolean isEmpty();

    /**
     * Returns an iterator. The iterator may be implemented by this instance 
     * and as such cannot be shared.
     */
    @Override
    Iterator<T> iterator();

}
