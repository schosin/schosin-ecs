package de.schosin.ecs.utils.collections;

import java.util.Iterator;
import java.util.stream.Stream;

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
public sealed interface ImmutableBag<T> extends Iterable<T> permits Bag, ImmutableBagImpl {

    @SafeVarargs
    static <T> ImmutableBag<T> of(T... items) {
        if (items.length == 0) {
            return emptyBag();
        }

        return create(new Bag<>(items));
    }

    @SuppressWarnings("unchecked")
    static <T> ImmutableBag<T> emptyBag() {
        return ImmutableBagImpl.EMPTY;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    static <T> ImmutableBag<T> create(Bag<? extends T> bag) {
        return new ImmutableBagImpl<>((Bag) bag);
    }

    static <T> ImmutableBag<T> copyOf(ImmutableBag<? extends T> other) {
        return create(new Bag<>(other));
    }

    boolean isEmpty();

    int getSize();

    int getCapacity();

    T get(int index);

    T getSafe(int index);

    boolean contains(@NonNull T item);

    boolean containsIdentity(@NonNull T item);

    int indexOf(@NonNull T item);

    int indexOfIdentity(@NonNull T item);

    Stream<T> stream();

}

final class ImmutableBagImpl<T> implements ImmutableBag<T> {

    @SuppressWarnings({ "unchecked", "rawtypes" })
    static final ImmutableBagImpl EMPTY = new ImmutableBagImpl(new Bag(Object.class, 0));

    final Bag<T> bag;

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
    public T getSafe(int index) {
        return this.bag.getSafe(index);
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
    public Stream<T> stream() {
        return bag.stream();
    }

    @Override
    public Iterator<T> iterator() {
        return new ImmutableBagIterator<>(bag);
    }

    @Override
    public String toString() {
        return this.bag.toString();
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
