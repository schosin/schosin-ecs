package de.schosin.ecs.utils.collections;

import java.util.Iterator;

public class BagIterator<T> implements Iterator<T> {

    private Bag<T> bag;
    private int index;

    public static <T> Iterable<T> iterable(Bag<T> bag) {
        return () -> new BagIterator<>(bag);
    }

    public BagIterator(Bag<T> bag) {
        init(bag);
    }

    public BagIterator() {
    }

    public BagIterator<T> init(Bag<T> bag) {
        this.bag = bag;
        this.index = 0;

        return this;
    }

    @Override
    public boolean hasNext() {
        return index < bag.getSize();
    }

    @Override
    public T next() {
        return bag.get(index++);
    }

    @Override
    public void remove() {
        this.bag.remove(--index);
    }

}
