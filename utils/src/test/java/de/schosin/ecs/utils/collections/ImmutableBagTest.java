package de.schosin.ecs.utils.collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ImmutableBagTest {

    @Nested
    class IteratorTest {

        @Test
        void testIterator() {
            var bag = bag(1, 2, 3, 4);
            var iter = bag.iterator();

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
            var iter = bag.iterator();

            var count = 0;
            for (; iter.hasNext();) {
                assertThat(iter.next()).isIn(1, 2, 3, 4);
                count++;
            }

            assertThat(count).isEqualTo(4);
        }

        @Test
        void testRemove() {
            var bag = bag(1, 2, 3, 4);
            var iter = bag.iterator();

            assertThat(iter.hasNext()).isTrue();
            assertThat(iter.next()).isEqualTo(1);

            assertThatThrownBy(iter::remove).isInstanceOf(UnsupportedOperationException.class);
        }

        private ImmutableBag<Integer> bag(int... values) {
            var bag = new Bag<>(Integer.class, values.length);

            for (var value : values) {
                bag.add(value);
            }

            return ImmutableBag.create(bag);
        }

    }

}
