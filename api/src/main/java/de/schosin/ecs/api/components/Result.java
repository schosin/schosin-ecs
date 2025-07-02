package de.schosin.ecs.api.components;

import java.util.Iterator;

import org.jspecify.annotations.NonNull;

import de.schosin.ecs.api.components.types.ComponentType;

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

    @SuppressWarnings("unchecked")
    static <T> Result<T> empty() {
        return EmptyResult.INSTANCE;
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

@SuppressWarnings("rawtypes")
enum EmptyResult implements Result {

    INSTANCE;

    private static final Iterator EMPTY = new Iterator() {
        @Override
        public boolean hasNext() {
            return false;
        }

        @Override
        public Object next() {
            throw new UnsupportedOperationException();
        }
    };

    @Override
    public Iterator iterator() {
        return EMPTY;
    }

    @NonNull
    @Override
    public Object get(int i) {
        return null;
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public boolean isEmpty() {
        return true;
    }

}