package de.schosin.ecs.engine.utils.collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class IntBagTest {

    @Nested
    class AddTest {

        @Test
        void testCapacityIncrease() {
            var bag = new IntBag(64);
            var capacity = bag.getCapacity();

            while (bag.getCapacity() == capacity) {
                bag.add(1);
            }

            assertThat(bag.getCapacity()).isGreaterThanOrEqualTo(2 * capacity);
        }

    }

    @Nested
    class SetTest {

        @Test
        void testSet() {
            var bag = new IntBag(64);

            bag.set(0, 1);
            assertThat(bag.getSize()).isEqualTo(1);

            bag.set(63, 1);
            assertThat(bag.getSize()).isEqualTo(64);

            bag.set(64, 1);
            assertThat(bag.getSize()).isEqualTo(65);
        }

        @Test
        void testCapacityIncrease() {
            var bag = new IntBag(64);

            bag.set(0, 1);
            assertThat(bag.getCapacity()).isEqualTo(64);

            bag.set(63, 1);
            assertThat(bag.getCapacity()).isEqualTo(64);

            bag.set(64, 1);
            assertThat(bag.getCapacity()).isGreaterThanOrEqualTo(128);
        }

    }

    @Nested
    class RemoveIndexTest {

        @Test
        void testEmptyBag() {
            var bag = new IntBag(64);
            assertThatThrownBy(() -> bag.removeIndex(0)).isInstanceOf(ArrayIndexOutOfBoundsException.class);
        }

        @Test
        void testRemove() {
            var bag = new IntBag(64);
            bag.add(1);
            bag.add(1);
            bag.add(4);

            assertThat(bag.removeIndex(0)).isEqualTo(1);
            assertThat(bag.getSize()).isEqualTo(2);
            assertThat(bag.getData()).containsOnly(0, 1, 4);

            assertThat(bag.removeIndex(0)).isNotEqualTo(-1); // actually 4, but no assertions on implementation detail!
            assertThat(bag.getSize()).isEqualTo(1);
            assertThat(bag.getData()).containsOnly(0, 1);

            assertThat(bag.removeIndex(0)).isNotEqualTo(-1); // actually 1, but no assertions on implementation detail!
            assertThat(bag.getSize()).isZero();
            assertThat(bag.getData()).containsOnly(0);
        }

    }

    @Nested
    class RemoveValueTest {

        @Test
        void testEmptyBag() {
            var bag = new IntBag(64);
            assertThat(bag.removeValue(0)).isFalse();
            assertThat(bag.removeValue(1)).isFalse();
            assertThat(bag.removeValue(63)).isFalse();
            assertThat(bag.removeValue(64)).isFalse();
            assertThat(bag.removeValue(65)).isFalse();
        }

        @Test
        void testDuplicateValue() {
            var bag = new IntBag(64);
            bag.add(1);
            bag.add(1);
            bag.add(4);

            assertThat(bag.removeValue(1)).isTrue();
            assertThat(bag.getSize()).isEqualTo(2);
            assertThat(bag.getData()).containsOnly(0, 1, 4);

            assertThat(bag.removeValue(1)).isTrue();
            assertThat(bag.getSize()).isEqualTo(1);
            assertThat(bag.getData()).containsOnly(0, 4);

            assertThat(bag.removeValue(1)).isFalse();
            assertThat(bag.getSize()).isEqualTo(1);
            assertThat(bag.getData()).containsOnly(0, 4);
        }

    }

    @Nested
    class RemoveLastTest {

        @Test
        void testEmptyBag() {
            var bag = new IntBag(64);
            assertThatThrownBy(() -> bag.removeLast()).isInstanceOf(ArrayIndexOutOfBoundsException.class);
        }

        @Test
        void testRemoveLast() {
            var bag = new IntBag(64);
            bag.add(1);
            bag.add(2);
            bag.add(4);

            assertThat(bag.removeLast()).isEqualTo(4);
            assertThat(bag.getSize()).isEqualTo(2);
            assertThat(bag.getData()).containsOnly(0, 1, 2);

            assertThat(bag.removeLast()).isEqualTo(2); // actually 4, but no assertions on implementation detail!
            assertThat(bag.getSize()).isEqualTo(1);
            assertThat(bag.getData()).containsOnly(0, 1);

            assertThat(bag.removeLast()).isEqualTo(1); // actually 1, but no assertions on implementation detail!
            assertThat(bag.getSize()).isZero();
            assertThat(bag.getData()).containsOnly(0);
        }

    }

    @Nested
    class ClearTest {

        @Test
        void testEmptyBag() {
            var bag = new IntBag(64);
            var capacity = bag.getCapacity();

            bag.clear();
            assertThat(bag.getCapacity()).isEqualTo(capacity);
            assertThat(bag.getData()).containsOnly(0);
        }

        @Test
        void testFilledBag() {
            var bag = new IntBag(64);
            bag.add(1);
            bag.add(2);
            bag.add(4);

            bag.clear();
            assertThat(bag.getData()).containsOnly(0);
        }

        @Test
        void testCapacityUnchanged() {
            var bag = new IntBag(64);
            bag.set(0, 1);
            bag.set(127, 1);

            var capacity = bag.getCapacity();

            bag.clear();
            assertThat(bag.getCapacity()).isEqualTo(capacity);

        }

    }

    @Nested
    class ContainsTest {

        @Test
        void testEmptyBag() {
            var bag = new IntBag(64);

            assertThat(bag.contains(1)).isFalse();
        }

        @Test
        void testContains() {
            var bag = new IntBag(64);
            bag.add(1);
            bag.add(42);

            assertThat(bag.contains(0)).isFalse();
            assertThat(bag.contains(1)).isTrue();
            assertThat(bag.contains(42)).isTrue();
            assertThat(bag.contains(64)).isFalse();
        }

    }

    @Nested
    class TestEnsureCapacity {

        @Test
        void testUnchangedCapacity() {
            var bag = new IntBag(64);

            bag.ensureCapacity(1);
            assertThat(bag.getCapacity()).isEqualTo(64);

            bag.ensureCapacity(63);
            assertThat(bag.getCapacity()).isEqualTo(64);
        }

        @Test
        void testIncreasedCapacity() {
            var bag = new IntBag(64);

            bag.ensureCapacity(64);
            assertThat(bag.getCapacity()).isEqualTo(65);

            bag.ensureCapacity(65);
            assertThat(bag.getCapacity()).isEqualTo(66);

        }

    }

}
