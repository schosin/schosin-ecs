package de.schosin.ecs.utils.collections;

import org.jspecify.annotations.NonNull;

public interface ImmutableBag<T> {

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

}