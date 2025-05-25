package de.schosin.ecs.utils.collections;

import java.lang.reflect.Array;
import java.util.Arrays;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class Bag<T> implements ImmutableBag<T> {

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

    public Bag(Bag<T> other) {
        this.data = Arrays.copyOf(other.data, other.data.length);
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

}
