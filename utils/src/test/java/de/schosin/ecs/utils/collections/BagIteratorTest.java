package de.schosin.ecs.utils.collections;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class BagIteratorTest {

    @Test
    void testIterator() {
        var bag = bag(1, 2, 3, 4);

        var iter = new BagIterator<>(bag);

        assertThat(iter.hasNext()).isTrue();
        assertThat(iter.next()).isEqualTo(1);

        assertThat(iter.hasNext()).isTrue();
        assertThat(iter.next()).isEqualTo(2);

        assertThat(iter.hasNext()).isTrue();
        assertThat(iter.next()).isEqualTo(3);

        assertThat(iter.hasNext()).isTrue();
        assertThat(iter.next()).isEqualTo(4);
    }

    @Test
    void testIteration() {
        var bag = bag(1, 2, 3, 4);

        var iter = new BagIterator<>(bag);

        var count = 0;
        for (; iter.hasNext();) {
            assertThat(iter.next()).isIn(1, 2, 3, 4);
            count++;
        }

        assertThat(count).isEqualTo(4);
    }

    @Test
    void testRemoveFirst() {
        var bag = bag(1, 2, 3, 4);

        var iter = new BagIterator<>(bag);

        assertThat(iter.hasNext()).isTrue();
        assertThat(iter.next()).isEqualTo(1);
        iter.remove();

        assertThat(iter.hasNext()).isTrue();
        assertThat(iter.next()).isIn(2, 3, 4);

        assertThat(iter.hasNext()).isTrue();
        assertThat(iter.next()).isIn(2, 3, 4);

        assertThat(iter.hasNext()).isTrue();
        assertThat(iter.next()).isIn(2, 3, 4);

        assertThat(bag.getSize()).isEqualTo(3);
        assertThat(bag.getData()).doesNotContain(1).containsOnlyOnce(2, 3, 4);
    }

    @Test
    void testRemoveMiddle() {
        var bag = bag(1, 2, 3, 4);

        var iter = new BagIterator<>(bag);

        assertThat(iter.hasNext()).isTrue();
        assertThat(iter.next()).isEqualTo(1);

        assertThat(iter.hasNext()).isTrue();
        assertThat(iter.next()).isEqualTo(2);
        iter.remove();

        assertThat(iter.hasNext()).isTrue();
        assertThat(iter.next()).isIn(3, 4);

        assertThat(iter.hasNext()).isTrue();
        assertThat(iter.next()).isIn(3, 4);

        assertThat(bag.getSize()).isEqualTo(3);
        assertThat(bag.getData()).doesNotContain(2).containsOnlyOnce(1, 3, 4);
    }

    @Test
    void testRemoveLast() {
        var bag = bag(1, 2, 3, 4);

        var iter = new BagIterator<>(bag);

        assertThat(iter.hasNext()).isTrue();
        assertThat(iter.next()).isEqualTo(1);

        assertThat(iter.hasNext()).isTrue();
        assertThat(iter.next()).isEqualTo(2);

        assertThat(iter.hasNext()).isTrue();
        assertThat(iter.next()).isEqualTo(3);

        assertThat(iter.hasNext()).isTrue();
        assertThat(iter.next()).isEqualTo(4);
        iter.remove();

        assertThat(bag.getSize()).isEqualTo(3);
        assertThat(bag.getData()).doesNotContain(4).containsOnlyOnce(1, 2, 3);
    }

    @Test
    void testRemoveAll() {
        var bag = bag(1, 2, 3, 4);

        var iter = new BagIterator<>(bag);

        assertThat(iter.hasNext()).isTrue();
        iter.next();
        iter.remove();

        assertThat(iter.hasNext()).isTrue();
        iter.next();
        iter.remove();

        assertThat(iter.hasNext()).isTrue();
        iter.next();
        iter.remove();

        assertThat(iter.hasNext()).isTrue();
        iter.next();
        iter.remove();

        assertThat(bag.getSize()).isEqualTo(0);
        assertThat(bag.getData()).doesNotContain(1, 2, 3, 4);
    }

    @Test
    void testRemoveAll_Loop() {
        var bag = bag(1, 2, 3, 4);

        var iter = new BagIterator<>(bag);

        for (; iter.hasNext();) {
            iter.next();
            iter.remove();
        }

        assertThat(bag.getSize()).isEqualTo(0);
        assertThat(bag.getData()).doesNotContain(1, 2, 3, 4);
    }

    private Bag<Integer> bag(int... values) {
        var bag = new Bag<>(Integer.class, values.length);

        for (var value : values) {
            bag.add(value);
        }

        return bag;
    }

}
