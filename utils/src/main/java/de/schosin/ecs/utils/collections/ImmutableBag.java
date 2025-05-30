package de.schosin.ecs.utils.collections;

import java.util.Iterator;

import org.jspecify.annotations.NonNull;

/**
 * Fast unordered data structure for fast iteration and index lookups.
 * 
 * <p>
 * To reduce memory allocations, it is recommended to iterate using a regular for loop. 
 * 
 * {@snippet:
 *     for (int i = 0, s = bag.getSize(); i < s; i++) {
 *         var item = bag.get(i);
 *     }
 * }
 * 
 * Iteration using the enhanced for loop ({@code for (var item : bag)} is possible, but 
 * causes an allocation of the {@link Iterator} instance. For maximum performance, use
 * the regular for loop.
 * </p>
 * 
 * @param <T> element type
 */
public interface ImmutableBag<T> extends Iterable<T> {

    @SuppressWarnings("unchecked")
    static <T> ImmutableBag<T> emptyBag() {
        return ImmutableBagImpl.EMPTY;
    }

    static <T> ImmutableBag<T> create(Bag<T> bag) {
        return new ImmutableBagImpl<>(bag);
    }

    boolean isEmpty();

    int getSize();

    int getCapacity();

    T get(int index);

    boolean contains(@NonNull T item);

    boolean containsIdentity(@NonNull T item);

    int indexOf(@NonNull T item);

    int indexOfIdentity(@NonNull T item);

}

class ImmutableBagImpl<T> implements ImmutableBag<T> {

    @SuppressWarnings({ "unchecked", "rawtypes" })
    static final ImmutableBagImpl EMPTY = new ImmutableBagImpl(new Bag(Object.class, 0));

    private final Bag<T> bag;

    ImmutableBagImpl(Bag<T> bag) {
        this.bag = bag;
    }

    @Override
    public boolean isEmpty() {
        return this.bag.isEmpty();
    }

    @Override
    public int getSize() {
        return this.bag.getSize();
    }

    @Override
    public int getCapacity() {
        return this.bag.getCapacity();
    }

    @Override
    public T get(int index) {
        return this.bag.get(index);
    }

    @Override
    public boolean contains(@NonNull T item) {
        return this.bag.contains(item);
    }

    @Override
    public boolean containsIdentity(@NonNull T item) {
        return this.bag.containsIdentity(item);
    }

    @Override
    public int indexOf(@NonNull T item) {
        return this.bag.indexOf(item);
    }

    @Override
    public int indexOfIdentity(@NonNull T item) {
        return this.bag.indexOfIdentity(item);
    }

    @Override
    public Iterator<T> iterator() {
        return new ImmutableBagIterator<>(bag);
    }

    private static class ImmutableBagIterator<T> implements Iterator<T> {

        private final T[] data;
        private final int size;
        private int index;

        public ImmutableBagIterator(Bag<T> bag) {
            this.data = bag.getData();
            this.size = bag.getSize();
        }

        @Override
        public boolean hasNext() {
            return index < size;
        }

        @Override
        public T next() {
            return data[index++];
        }

    }

}
