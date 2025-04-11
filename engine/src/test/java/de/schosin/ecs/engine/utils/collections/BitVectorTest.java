package de.schosin.ecs.engine.utils.collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.stream.IntStream;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class BitVectorTest {

    @Test
    void testCopy() {
        var vector = new BitVector();
        vector.set(42);

        var copy = new BitVector(vector);
        assertThat(copy).isEqualTo(vector);
        assertThat(copy).hasSameHashCodeAs(vector);

        vector.set(1);
        assertThat(copy).isNotEqualTo(vector);
        assertThat(copy).doesNotHaveSameHashCodeAs(vector);
    }

    @Test
    void testIsEmpty() {
        var vector = new BitVector();
        assertThat(vector.isEmpty()).isTrue();

        vector.set(1);
        assertThat(vector.isEmpty()).isFalse();

        vector.set(2);
        assertThat(vector.isEmpty()).isFalse();

        vector.clear(1);
        assertThat(vector.isEmpty()).isFalse();

        vector.clear(2);
        assertThat(vector.isEmpty()).isTrue();
    }

    @Nested
    class EqualsHashCodeTest {

        @Test
        void testEmptyVectors() {
            assertThat(new BitVector()).isEqualTo(new BitVector());
        }

        @Test
        void testEqualVectors() {
            var vector1 = new BitVector();
            vector1.set(1);
            vector1.set(4);

            var vector2 = new BitVector();
            vector2.set(1);
            vector2.set(4);

            assertThat(vector2)
                    .isEqualTo(vector1)
                    .hasSameHashCodeAs(vector1);
        }

        @Test
        void testEqualBitsDifferentSizes() {
            var vector1 = new BitVector();
            vector1.set(1);
            vector1.set(4);
            vector1.set(1000);
            vector1.clear(1000);

            var vector2 = new BitVector();
            vector2.set(1);
            vector2.set(4);

            assertThat(vector2)
                    .isEqualTo(vector1)
                    .hasSameHashCodeAs(vector1);
        }

    }

    @Nested
    class ContainsTest {

        @Test
        void testEmptyVectors() {
            var vector1 = new BitVector();
            var vector2 = new BitVector();

            assertThat(vector1.containsAll(vector2)).isTrue();
            assertThat(vector1.containsNone(vector2)).isTrue();
            assertThat(vector1.containsSome(vector2)).isFalse();

            assertThat(vector2.containsAll(vector1)).isTrue();
            assertThat(vector2.containsNone(vector1)).isTrue();
            assertThat(vector2.containsSome(vector1)).isFalse();
        }

        @Test
        void testEmptyVectorWithOther() {
            var vector1 = new BitVector();
            var vector2 = new BitVector();
            vector2.set(1);

            assertThat(vector1.containsAll(vector2)).isTrue();
            assertThat(vector1.containsNone(vector2)).isTrue();
            assertThat(vector1.containsSome(vector2)).isFalse();

            assertThat(vector2.containsAll(vector1)).isFalse();
            assertThat(vector2.containsNone(vector1)).isTrue();
            assertThat(vector2.containsSome(vector1)).isFalse();
        }

        @Test
        void testEqualVectors() {
            var vector1 = new BitVector();
            vector1.set(1);
            var vector2 = new BitVector();
            vector2.set(1);

            assertThat(vector1.containsAll(vector2)).isTrue();
            assertThat(vector1.containsNone(vector2)).isFalse();
            assertThat(vector1.containsSome(vector2)).isTrue();

            assertThat(vector2.containsAll(vector1)).isTrue();
            assertThat(vector2.containsNone(vector1)).isFalse();
            assertThat(vector2.containsSome(vector1)).isTrue();
        }

        @Test
        void testOverlappingVectors() {
            var vector1 = new BitVector();
            vector1.set(1);
            vector1.set(2);
            var vector2 = new BitVector();
            vector2.set(1);
            vector2.set(3);

            assertThat(vector1.containsAll(vector2)).isFalse();
            assertThat(vector1.containsNone(vector2)).isFalse();
            assertThat(vector1.containsSome(vector2)).isTrue();

            assertThat(vector2.containsAll(vector1)).isFalse();
            assertThat(vector2.containsNone(vector1)).isFalse();
            assertThat(vector2.containsSome(vector1)).isTrue();
        }

        @Test
        void testDisjunctVectors() {
            var vector1 = new BitVector();
            vector1.set(2);
            var vector2 = new BitVector();
            vector2.set(3);

            assertThat(vector1.containsAll(vector2)).isFalse();
            assertThat(vector1.containsNone(vector2)).isTrue();
            assertThat(vector1.containsSome(vector2)).isFalse();

            assertThat(vector2.containsAll(vector1)).isFalse();
            assertThat(vector2.containsNone(vector1)).isTrue();
            assertThat(vector2.containsSome(vector1)).isFalse();
        }

        @Test
        void testShorterVector() {
            var vector1 = new BitVector();
            vector1.set(2);
            var vector2 = new BitVector();
            vector2.set(2);
            vector2.set(100);

            assertThat(vector1.containsAll(vector2)).isTrue();
            assertThat(vector1.containsNone(vector2)).isFalse();
            assertThat(vector1.containsSome(vector2)).isTrue();

            assertThat(vector2.containsAll(vector1)).isFalse();
            assertThat(vector2.containsNone(vector1)).isFalse();
            assertThat(vector2.containsSome(vector1)).isTrue();
        }

    }

    @Nested
    class GetSetTest {

        @Test
        void testGet() {
            var vector = new BitVector();
            vector.set(1);
            vector.set(42);

            assertThat(vector.get(1)).isTrue();
            assertThat(vector.get(2)).isFalse();
            assertThat(vector.get(42)).isTrue();
            assertThat(vector.get(9001)).isFalse();
        }

        @Test
        void testUnsafeGet() {
            var vector = new BitVector();
            vector.set(1);
            vector.set(42);

            assertThat(vector.unsafeGet(1)).isTrue();
            assertThat(vector.unsafeGet(2)).isFalse();
            assertThat(vector.unsafeGet(42)).isTrue();
            assertThatThrownBy(() -> vector.unsafeGet(9001)).isInstanceOf(ArrayIndexOutOfBoundsException.class);
        }

        @Test
        void testUnsafeSet() {
            var vector = new BitVector();
            vector.unsafeSet(1);
            vector.unsafeSet(42);
            assertThatThrownBy(() -> vector.unsafeSet(9001)).isInstanceOf(ArrayIndexOutOfBoundsException.class);

            assertThat(vector.get(1)).isTrue();
            assertThat(vector.get(2)).isFalse();
            assertThat(vector.get(42)).isTrue();
            assertThat(vector.get(9001)).isFalse();

            assertThat(vector.unsafeGet(1)).isTrue();
            assertThat(vector.unsafeGet(2)).isFalse();
            assertThat(vector.unsafeGet(42)).isTrue();
            assertThatThrownBy(() -> vector.unsafeGet(9001)).isInstanceOf(ArrayIndexOutOfBoundsException.class);
        }

        @Test
        void testClear() {
            var vector = new BitVector();
            vector.set(1);
            vector.set(42);
            vector.set(100);
            vector.set(200);

            vector.clear(0);
            assertThat(vector.unsafeGet(1)).isTrue();
            assertThat(vector.unsafeGet(2)).isFalse();
            assertThat(vector.unsafeGet(42)).isTrue();
            assertThat(vector.unsafeGet(100)).isTrue();
            assertThat(vector.unsafeGet(200)).isTrue();

            vector.clear(2);
            assertThat(vector.unsafeGet(1)).isTrue();
            assertThat(vector.unsafeGet(2)).isFalse();
            assertThat(vector.unsafeGet(42)).isTrue();
            assertThat(vector.unsafeGet(100)).isTrue();
            assertThat(vector.unsafeGet(200)).isTrue();

            vector.clear(42);
            assertThat(vector.unsafeGet(1)).isTrue();
            assertThat(vector.unsafeGet(2)).isFalse();
            assertThat(vector.unsafeGet(42)).isFalse();
            assertThat(vector.unsafeGet(100)).isTrue();
            assertThat(vector.unsafeGet(200)).isTrue();

            vector.clear(200);
            assertThat(vector.unsafeGet(1)).isTrue();
            assertThat(vector.unsafeGet(2)).isFalse();
            assertThat(vector.unsafeGet(42)).isFalse();
            assertThat(vector.unsafeGet(100)).isTrue();
            assertThat(vector.unsafeGet(200)).isFalse();

            vector.clear(100);
            assertThat(vector.unsafeGet(1)).isTrue();
            assertThat(vector.unsafeGet(2)).isFalse();
            assertThat(vector.unsafeGet(42)).isFalse();
            assertThat(vector.unsafeGet(100)).isFalse();
            assertThat(vector.unsafeGet(200)).isFalse();

            vector.clear(9001);
            assertThat(vector.unsafeGet(1)).isTrue();
            assertThat(vector.unsafeGet(2)).isFalse();
            assertThat(vector.unsafeGet(42)).isFalse();
            assertThat(vector.unsafeGet(100)).isFalse();
            assertThat(vector.unsafeGet(200)).isFalse();

            vector.clear(1);
            assertThat(vector.unsafeGet(1)).isFalse();
            assertThat(vector.unsafeGet(2)).isFalse();
            assertThat(vector.unsafeGet(42)).isFalse();
            assertThat(vector.unsafeGet(100)).isFalse();
            assertThat(vector.unsafeGet(200)).isFalse();

            vector.clear(7);
            assertThat(vector.unsafeGet(1)).isFalse();
            assertThat(vector.unsafeGet(2)).isFalse();
            assertThat(vector.unsafeGet(7)).isFalse();
            assertThat(vector.unsafeGet(42)).isFalse();
            assertThat(vector.unsafeGet(100)).isFalse();
            assertThat(vector.unsafeGet(200)).isFalse();
        }

        @Test
        void testClear_WordIndexBeforeCurrentWord() {
            // zeroing a word before the current word should not change current word
            var vector = new BitVector();
            vector.set(1);
            vector.set(9001);

            assertThat(vector.get(9001)).isTrue();

            vector.clear(1);
            assertThat(vector.get(9001)).isTrue();
        }

        @Test
        void testUnsafeClear() {
            var vector = new BitVector();
            vector.set(1);
            vector.set(42);
            vector.set(100);
            vector.set(200);

            vector.unsafeClear(0);
            assertThat(vector.unsafeGet(1)).isTrue();
            assertThat(vector.unsafeGet(2)).isFalse();
            assertThat(vector.unsafeGet(42)).isTrue();
            assertThat(vector.unsafeGet(100)).isTrue();
            assertThat(vector.unsafeGet(200)).isTrue();

            vector.unsafeClear(2);
            assertThat(vector.unsafeGet(1)).isTrue();
            assertThat(vector.unsafeGet(2)).isFalse();
            assertThat(vector.unsafeGet(42)).isTrue();
            assertThat(vector.unsafeGet(100)).isTrue();
            assertThat(vector.unsafeGet(200)).isTrue();

            vector.unsafeClear(42);
            assertThat(vector.unsafeGet(1)).isTrue();
            assertThat(vector.unsafeGet(2)).isFalse();
            assertThat(vector.unsafeGet(42)).isFalse();
            assertThat(vector.unsafeGet(100)).isTrue();
            assertThat(vector.unsafeGet(200)).isTrue();

            vector.unsafeClear(200);
            assertThat(vector.unsafeGet(1)).isTrue();
            assertThat(vector.unsafeGet(2)).isFalse();
            assertThat(vector.unsafeGet(42)).isFalse();
            assertThat(vector.unsafeGet(100)).isTrue();
            assertThat(vector.unsafeGet(200)).isFalse();

            vector.unsafeClear(100);
            assertThat(vector.unsafeGet(1)).isTrue();
            assertThat(vector.unsafeGet(2)).isFalse();
            assertThat(vector.unsafeGet(42)).isFalse();
            assertThat(vector.unsafeGet(100)).isFalse();
            assertThat(vector.unsafeGet(200)).isFalse();

            assertThatThrownBy(() -> vector.unsafeClear(9001)).isInstanceOf(ArrayIndexOutOfBoundsException.class);
            assertThat(vector.unsafeGet(1)).isTrue();
            assertThat(vector.unsafeGet(2)).isFalse();
            assertThat(vector.unsafeGet(42)).isFalse();
            assertThat(vector.unsafeGet(100)).isFalse();
            assertThat(vector.unsafeGet(200)).isFalse();

            vector.unsafeClear(1);
            assertThat(vector.unsafeGet(1)).isFalse();
            assertThat(vector.unsafeGet(2)).isFalse();
            assertThat(vector.unsafeGet(42)).isFalse();
            assertThat(vector.unsafeGet(100)).isFalse();
            assertThat(vector.unsafeGet(200)).isFalse();

            vector.unsafeClear(7);
            assertThat(vector.unsafeGet(1)).isFalse();
            assertThat(vector.unsafeGet(2)).isFalse();
            assertThat(vector.unsafeGet(7)).isFalse();
            assertThat(vector.unsafeGet(42)).isFalse();
            assertThat(vector.unsafeGet(100)).isFalse();
            assertThat(vector.unsafeGet(200)).isFalse();
        }

        @Test
        void testUnsafeClear_WordIndexBeforeCurrentWord() {
            // zeroing a word before the current word should not change current word
            var vector = new BitVector();
            vector.set(1);
            vector.set(9001);

            assertThat(vector.get(9001)).isTrue();

            vector.unsafeClear(1);
            assertThat(vector.get(9001)).isTrue();
        }

    }

    @Nested
    class NextSetBitTest {

        @Test
        void testEmptyVector() {
            var vector = new BitVector();

            assertThat(vector.nextSetBit(0)).isEqualTo(-1);
            assertThat(vector.nextSetBit(1)).isEqualTo(-1);
            assertThat(vector.nextSetBit(42)).isEqualTo(-1);
            assertThat(vector.nextSetBit(9001)).isEqualTo(-1);
        }

        @Test
        void testNextSetBit() {
            var vector = new BitVector();
            vector.set(1);
            vector.set(42);
            vector.set(9001);

            assertThat(vector.nextSetBit(0)).isEqualTo(1);
            assertThat(vector.nextSetBit(1)).isEqualTo(1);

            assertThat(vector.nextSetBit(2)).isEqualTo(42);
            assertThat(vector.nextSetBit(41)).isEqualTo(42);
            assertThat(vector.nextSetBit(42)).isEqualTo(42);

            assertThat(vector.nextSetBit(43)).isEqualTo(9001);
            assertThat(vector.nextSetBit(9000)).isEqualTo(9001);
            assertThat(vector.nextSetBit(9001)).isEqualTo(9001);

            assertThat(vector.nextSetBit(9002)).isEqualTo(-1);
            assertThat(vector.nextSetBit(31337)).isEqualTo(-1);
        }

        @Test
        void testIteration() {
            var vector = new BitVector();
            vector.set(1);
            vector.set(42);
            vector.set(9001);

            var result = new IntBag(3);

            var idx = vector.nextSetBit(0);
            while (idx > -1) {
                result.add(idx);
                idx = vector.nextSetBit(idx + 1);
            }

            assertThat(result.getSize()).as("size").isEqualTo(3);
            assertThat(result.getData()).as("value 1").contains(1);
            assertThat(result.getData()).as("value 42").contains(42);
            assertThat(result.getData()).as("value 9001").contains(9001);
        }

    }

    @Nested
    class IterateTest {

        @ParameterizedTest
        @ValueSource(ints = { 1, 2, 4, 63, 64, 65 })
        void testFilledBitVector(int size) {
            var vector = new BitVector();
            for (int i = 0; i < size; i++) {
                vector.set(i);
            }

            var result = new IntBag(size);
            vector.iterate(result::add);

            assertThat(result.getData())
                    .hasSize(size)
                    .containsSequence(IntStream.range(0, size).toArray());
        }

        @ParameterizedTest
        @ValueSource(ints = { 4, 12, 18, 24, 63, 64, 65 })
        void testSparseBitVector(int size) {
            var vector = new BitVector();
            for (int i = 0; i < size; i += 4) {
                vector.set(i);
            }

            var result = new IntBag(size);
            vector.iterate(result::add);

            assertThat(result.getData()).hasSize(size);
        }

    }

}
