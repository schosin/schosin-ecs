package de.schosin.ecs.utils.collections;

import java.util.PrimitiveIterator;
import java.util.PrimitiveIterator.OfInt;

import org.jspecify.annotations.NonNull;

public sealed interface ImmutableIntBag permits IntBag, ImmutableIntBagImpl {

    public static final ImmutableIntBag EMPTY = ImmutableIntBagImpl.EMPTY;

    static ImmutableIntBag create(IntBag bag) {
        return new ImmutableIntBagImpl(bag);
    }

    boolean isEmpty();

    int getSize();

    int getCapacity();

    int get(int index);

    boolean contains(int item);

    int indexOf(int item);

    PrimitiveIterator.OfInt iterator();

}

final class ImmutableIntBagImpl implements ImmutableIntBag {

    static final ImmutableIntBagImpl EMPTY = new ImmutableIntBagImpl(new IntBag(0));

    private final IntBag bag;

    ImmutableIntBagImpl(IntBag bag) {
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
    public int get(int index) {
        return this.bag.get(index);
    }

    @Override
    public boolean contains(int item) {
        return this.bag.contains(item);
    }

    @Override
    public int indexOf(@NonNull int item) {
        return this.bag.indexOf(item);
    }

    @Override
    public OfInt iterator() {
        return new ImmutableIntBagIterator(bag);
    }

    private static class ImmutableIntBagIterator implements PrimitiveIterator.OfInt {

        private final int[] data;
        private final int size;
        private int index;

        public ImmutableIntBagIterator(IntBag bag) {
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

    }

}