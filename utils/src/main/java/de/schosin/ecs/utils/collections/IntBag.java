package de.schosin.ecs.utils.collections;

import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.PrimitiveIterator;
import java.util.PrimitiveIterator.OfInt;

public final class IntBag implements ImmutableIntBag {

    private int[] data;
    private int size;

    public IntBag(int capacity) {
        this.data = new int[capacity];
        this.size = 0;
    }

    public IntBag(ImmutableIntBag other) {
        this.data = new int[other.getSize()];
        this.size = other.getSize();

        for (int i = 0; i < size; i++) {
            this.data[i] = other.get(i);
        }
    }

    public int[] getData() {
        return data;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public int getSize() {
        return size;
    }

    public int getCapacity() {
        return data.length;
    }

    public int get(int index) {
        return data[index];
    }

    public int getSafe(int index) {
        if (index >= data.length) {
            return 0;
        }

        return data[index];
    }

    public void add(int item) {
        if (data.length == size) {
            setCapacity(data.length * 2);
        }

        this.data[size++] = item;
    }

    public void set(int index, int item) {
        if (index >= data.length) {
            var newSize = Math.max(data.length * 2, index + 1);
            setCapacity(newSize);
        }

        this.size = index < size ? size : index + 1;
        this.data[index] = item;
    }

    public int removeIndex(int index) {
        var item = data[index];

        // move last item to cleared slot
        this.data[index] = data[--size];
        this.data[size] = 0;

        return item;
    }

    public boolean removeValue(int item) {
        var index = indexOf(item);
        if (index > -1) {
            removeIndex(index);
            return true;
        }

        return false;
    }

    public int removeLast() {
        var item = data[--size];
        this.data[size] = 0;

        return item;
    }

    public void clear() {
        Arrays.fill(data, 0, size, 0);
        this.size = 0;
    }

    public boolean contains(int item) {
        return indexOf(item) != -1;
    }

    public int indexOf(int item) {
        for (int i = 0; i < size; i++) {
            if (item == data[i]) {
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
    public OfInt iterator() {
        return new IntBagIterator(this);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder().append("IntBag(");
        for (int i = 0; i < size; i++) {
            if (i > 0) {
                builder.append(", ");
            }
            builder.append(data[i]);
        }
        return builder.append(")").toString();
    }

    private static class IntBagIterator implements PrimitiveIterator.OfInt {

        private final IntBag bag;
        private final int[] data;

        private int size;
        private int index;

        public IntBagIterator(IntBag bag) {
            this.bag = bag;
            this.data = bag.getData();
            this.size = bag.getSize();
        }

        @Override
        public boolean hasNext() {
            return index < size;
        }

        @Override
        public int nextInt() {
            return data[index++];
        }

        @Override
        public void remove() {
            if (bag.getData() != data) {
                throw new ConcurrentModificationException("Backing array replaced by concurrent operation");
            }

            this.bag.removeIndex(--index);
            this.size--;
        }

    }

}
