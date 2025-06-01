package de.schosin.ecs.utils.collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ImmutableIntBagTest {

    @Nested
    class IteratorTest {

        @Test
        void testIterator() {
            var bag = bag(1, 2, 3, 4);
            var iter = bag.iterator();

            assertThat(iter.hasNext()).isTrue();
            assertThat(iter.nextInt()).isEqualTo(1);

            assertThat(iter.hasNext()).isTrue();
            assertThat(iter.nextInt()).isEqualTo(2);

            assertThat(iter.hasNext()).isTrue();
            assertThat(iter.nextInt()).isEqualTo(3);

            assertThat(iter.hasNext()).isTrue();
            assertThat(iter.nextInt()).isEqualTo(4);
        }

        @Test
        void testIteration() {
            var bag = bag(1, 2, 3, 4);
            var iter = bag.iterator();

            var count = 0;
            for (; iter.hasNext();) {
                assertThat(iter.nextInt()).isIn(1, 2, 3, 4);
                count++;
            }

            assertThat(count).isEqualTo(4);
        }

        @Test
        void testRemove() {
            var bag = bag(1, 2, 3, 4);
            var iter = bag.iterator();

            assertThat(iter.hasNext()).isTrue();
            assertThat(iter.nextInt()).isEqualTo(1);

            assertThatThrownBy(iter::remove).isInstanceOf(UnsupportedOperationException.class);
        }

        private ImmutableIntBag bag(int... values) {
            var bag = new IntBag(values.length);

            for (var value : values) {
                bag.add(value);
            }

            return ImmutableIntBag.create(bag);
        }

    }

}
