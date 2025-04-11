package de.schosin.ecs.engine.utils.collections;

import java.util.Arrays;

public class IntBag {

    private int[] data;
    private int size;

    public IntBag(int capacity) {
        this.data = new int[capacity];
        this.size = 0;
    }

    public int[] getData() {
        return data;
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

}
