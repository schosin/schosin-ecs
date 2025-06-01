package de.schosin.ecs.utils.collections;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Fast unordered data structure for fast iteration, lookup, add, and remove.
 * 
 * <p>
 * The data structure is backed by an array. When removing an item from a bag,
 * the last item of the bag will be moved to the removed index. This reordering
 * allows for removal in {@code O(1)}, but causes the data structure to be unordered.
 * </p>
 * 
 * <p>
 * To reduce memory allocations, it is recommended to iterate using a regular for loop. 
 * 
 * {@snippet:
 *     var data = bag.getData();
 *     for (int i = 0, s = bag.getSize(); i < s; i++) {
 *         var item = data[i];
 *     }
 * }
 * 
 * Iteration using the enhanced for loop ({@code for (var item : bag)} is possible, but 
 * causes an allocation of the {@link Iterator} instance. For maximum performance, use
 * the regular for loop.
 * </p>
 * 
 * <p>
 * If removal during iteration is required, either iterate the bag in reverse order, starting
 * at {@code size - 1}, or use {@link Iterator#remove()}.
 * </p>
 * 
 * @param <T> element type
 */
public final class Bag<T> implements ImmutableBag<T> {

    private T[] data;
    private int size;

    public Bag(Class<T> clazz) {
        this(clazz, 64);
    }

    @SuppressWarnings("unchecked")
    public Bag(Class<? super T> clazz, int size) {
        this.data = (T[]) Array.newInstance(clazz, size);
        this.size = 0;
    }

    @SafeVarargs
    public Bag(T... items) {
        this.data = Arrays.copyOf(items, Math.max(items.length, 64));
        this.size = items.length;
    }

    public Bag(ImmutableBag<T> other) {
        this(other instanceof Bag<T> bag ? bag : ((ImmutableBagImpl<T>) other).bag);
    }

    public Bag(Bag<T> other) {
        this.data = Arrays.copyOf(other.data, Math.max(other.data.length, 64));
        this.size = other.size;
    }

    public T[] getData() {
        return data;
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    @Override
    public int getSize() {
        return size;
    }

    @Override
    public int getCapacity() {
        return data.length;
    }

    @Override
    public T get(int index) {
        return data[index];
    }

    public void addAll(ImmutableBag<? extends T> components) {
        for (int i = 0, s = components.getSize(); i < s; i++) {
            add(components.get(i));
        }
    }

    public void add(@NonNull T item) {
        if (data.length == size) {
            setCapacity(data.length * 2);
        }

        this.data[size++] = item;
    }

    public void set(int index, @Nullable T item) {
        if (index >= data.length) {
            var newSize = Math.max(data.length * 2, index + 1);
            setCapacity(newSize);
        }

        this.data[index] = item;
        this.size = index < size ? size : index + 1;
    }

    public T remove(int index) {
        var item = data[index];

        // move last item to cleared slot
        this.data[index] = data[--size];
        this.data[size] = null;

        return item;
    }

    public boolean remove(@NonNull T item) {
        for (int i = 0; i < size; i++) {
            var test = data[i];

            if (item.equals(test)) {
                // move last item to cleared slot
                this.data[i] = data[--size];
                this.data[size] = null;

                return true;
            }
        }

        return false;
    }

    public boolean removeIdentity(@NonNull T item) {
        for (int i = 0; i < size; i++) {
            var test = data[i];

            if (item == test) {
                // move last item to cleared slot
                this.data[i] = data[--size];
                this.data[size] = null;

                return true;
            }
        }

        return false;
    }

    public T removeLast() {
        var item = data[--size];
        this.data[size] = null;

        return item;
    }

    public void clear() {
        Arrays.fill(this.data, 0, this.size, null);
        this.size = 0;
    }

    @Override
    public boolean contains(@NonNull T item) {
        return indexOf(item) > -1;
    }

    @Override
    public boolean containsIdentity(@NonNull T item) {
        return indexOfIdentity(item) > -1;
    }

    @Override
    public int indexOf(@NonNull T item) {
        for (int i = 0; i < size; i++) {
            if (data[i].equals(item)) {
                return i;
            }
        }

        return -1;
    }

    @Override
    public int indexOfIdentity(@NonNull T item) {
        for (int i = 0; i < size; i++) {
            if (data[i] == item) {
                return i;
            }
        }

        return -1;
    }

    public void ensureCapacity(int index) {
        if (index >= data.length) {
            setCapacity(index + 1);
        }
    }

    private void setCapacity(int length) {
        if (length > this.data.length) {
            synchronized (this) {
                if (length > this.data.length) {
                    this.data = Arrays.copyOf(data, length);
                }
            }
        }
    }

    @Override
    public Stream<T> stream() {
        return StreamSupport.stream(Spliterators.spliterator(iterator(), size, 0), false);
    }

    @Override
    public Iterator<T> iterator() {
        return new BagIterator<>(this);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder().append("Bag(");
        for (int i = 0; i < size; i++) {
            if (i > 0) {
                builder.append(", ");
            }
            builder.append(data[i]);
        }
        return builder.append(")").toString();
    }

    private static class BagIterator<T> implements Iterator<T> {

        private final Bag<T> bag;
        private final T[] data;

        private int size;
        private int index;

        public BagIterator(Bag<T> bag) {
            this.bag = bag;
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

        @Override
        public void remove() {
            if (bag.getData() != data) {
                throw new ConcurrentModificationException("Backing array replaced by concurrent operation");
            }

            this.bag.remove(--index);
            this.size--;
        }

    }

}
