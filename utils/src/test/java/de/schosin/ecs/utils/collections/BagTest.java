package de.schosin.ecs.engine.utils.collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class BagTest {

    @Nested
    class SizeTest {

        @Test
        void testEmptySize() {
            var bag = new Bag<>(String.class);
            assertThat(bag.getSize()).isZero();
        }

        @Test
        void testAdd() {
            var bag = new Bag<>(String.class);

            bag.add("data");
            assertThat(bag.getSize()).isEqualTo(1);

            bag.add("data"); // no set semantics
            assertThat(bag.getSize()).isEqualTo(2);
        }

        @Test
        void testSet() {
            var bag = new Bag<>(String.class);

            bag.set(0, "data");
            assertThat(bag.getSize()).isEqualTo(1);

            bag.set(0, "data");
            assertThat(bag.getSize()).isEqualTo(1);

            bag.set(1, "data");
            assertThat(bag.getSize()).isEqualTo(2);

            bag.set(5, "data");
            assertThat(bag.getSize()).isEqualTo(6);
        }

        @Test
        void testEnsureCapacity() {
            var bag = new Bag<>(String.class);
            var capacity = bag.getCapacity();

            bag.ensureCapacity(capacity - 1);
            assertThat(bag.getCapacity()).isEqualTo(capacity);

            bag.ensureCapacity(capacity);
            assertThat(bag.getCapacity()).isEqualTo(capacity + 1);

            bag.ensureCapacity(capacity + 5);
            assertThat(bag.getCapacity()).isGreaterThanOrEqualTo(capacity + 6);

            bag.ensureCapacity(capacity + 2);
            assertThat(bag.getCapacity()).isGreaterThanOrEqualTo(capacity + 6);
        }

    }

    @Nested
    class AddTest {

        @Test
        void testCapacityGrows() {
            var bag = new Bag<>(String.class);
            var capacity = bag.getCapacity();

            bag.set(capacity - 1, "data");
            assertThat(bag.getCapacity()).isEqualTo(capacity);

            bag.add("data");
            assertThat(bag.getCapacity()).isGreaterThan(capacity);
        }

    }

    @Nested
    class SetTest {

        @Test
        void testSet() {
            var bag = new Bag<>(String.class);

            bag.set(5, "data");
            assertThat(bag.getSize()).isEqualTo(6);
            assertThat(bag.get(0)).isNull();
            assertThat(bag.get(1)).isNull();
            assertThat(bag.get(2)).isNull();
            assertThat(bag.get(3)).isNull();
            assertThat(bag.get(4)).isNull();
            assertThat(bag.get(5)).isEqualTo("data");
        }

        @Test
        void testCapacityGrows() {
            var bag = new Bag<>(String.class);
            var capacity = bag.getCapacity();

            bag.set(capacity - 1, "data");
            assertThat(bag.getCapacity()).isEqualTo(capacity);

            bag.set(capacity, "data");
            assertThat(bag.getCapacity()).isGreaterThan(capacity);
        }

    }

    @Nested
    class RemoveTest {

        @Test
        void testRemoveIndex() {
            var bag = new Bag<>(String.class);

            bag.set(2, "data");
            assertThat(bag.getSize()).isEqualTo(3);

            bag.remove(0);
            assertThat(bag.getSize()).isEqualTo(2);

            bag.remove(1);
            assertThat(bag.getSize()).isEqualTo(1);

            bag.remove(0);
            assertThat(bag.getSize()).isZero();
        }

        @Test
        void testRemoveItem() {
            var bag = new Bag<>(String.class);
            bag.add("1");
            bag.add("2");
            bag.add("2");

            assertThat(bag.getSize()).isEqualTo(3);

            bag.remove("1");
            assertThat(bag.getSize()).isEqualTo(2);

            bag.remove("1");
            assertThat(bag.getSize()).isEqualTo(2);

            bag.remove("2");
            assertThat(bag.getSize()).isEqualTo(1);

            bag.remove("2");
            assertThat(bag.getSize()).isZero();
        }

        @Test
        void testRemoveLast() {
            var bag = new Bag<>(String.class);
            bag.add("1");
            bag.add("2");
            bag.add("2");

            assertThat(bag.getSize()).isEqualTo(3);

            bag.removeLast();
            assertThat(bag.getSize()).isEqualTo(2);

            bag.removeLast();
            assertThat(bag.getSize()).isEqualTo(1);

            bag.removeLast();
            assertThat(bag.getSize()).isZero();
        }

        @Test
        void testClear() {
            var bag = new Bag<>(String.class);
            bag.add("1");
            bag.add("2");
            bag.add("2");

            assertThat(bag.getSize()).isEqualTo(3);

            bag.clear();
            assertThat(bag.getSize()).isZero();
        }

        @Test
        void testClearResetsData() {
            var bag = new Bag<>(String.class);
            bag.add("1");
            bag.add("2");
            bag.add("2");

            assertThat(bag.getSize()).isEqualTo(3);
            assertThat(bag.get(0)).isEqualTo("1");
            assertThat(bag.get(1)).isEqualTo("2");
            assertThat(bag.get(2)).isEqualTo("2");

            bag.clear();
            assertThat(bag.getSize()).isZero();

            bag.set(2, "new");
            assertThat(bag.getSize()).isEqualTo(3);
            assertThat(bag.get(0)).isNull();
            assertThat(bag.get(1)).isNull();
            assertThat(bag.get(2)).isEqualTo("new");
        }

    }

    @Nested
    class ConcurrencyTest {

        @ParameterizedTest
        @ValueSource(ints = { 1, 2, 5, 10, 20 })
        void testConcurrentReads(int threads) throws InterruptedException {
            var bag = new Bag<>(String.class);
            bag.add("1");
            bag.add("2");
            bag.add("3");

            try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
                var tasks = IntStream.of(threads).mapToObj(i -> new Iterate(bag)).toList();
                executor.invokeAll(tasks);

                executor.shutdown();
                if (!executor.awaitTermination(1L, TimeUnit.SECONDS)) {
                    fail("timeout");
                }
            }
        }

        @ParameterizedTest
        @ValueSource(ints = { 1, 2, 5, 10, 20 })
        void testConcurrentOverwrites(int threads) throws InterruptedException {
            var bag = new Bag<>(String.class);
            bag.add("1");
            bag.add("2");
            bag.add("3");

            try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
                var tasks = Stream.concat(
                        IntStream.of(threads).mapToObj(i -> new Iterate(bag)),
                        IntStream.of(threads).mapToObj(i -> new Overwrite(bag, i % 3, "value " + i)))
                        .toList();

                executor.invokeAll(tasks);

                executor.shutdown();
                if (!executor.awaitTermination(1L, TimeUnit.SECONDS)) {
                    fail("timeout");
                }
            }
        }

        @ParameterizedTest
        @ValueSource(ints = { 1, 2, 5, 10, 20 })
        void testConcurrentRemoves(int threads) throws InterruptedException {
            var bag = new Bag<>(String.class);
            bag.add("1");
            bag.add("2");
            bag.add("3");

            try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
                var tasks = Stream.concat(
                        IntStream.of(threads).mapToObj(i -> new Iterate(bag)),
                        IntStream.of(threads).mapToObj(i -> new Remove(bag, i % 3)))
                        .toList();

                executor.invokeAll(tasks);

                executor.shutdown();
                if (!executor.awaitTermination(1L, TimeUnit.SECONDS)) {
                    fail("timeout");
                }
            }
        }

        private record Iterate(Bag<String> bag) implements Callable<String> {
            @Override
            public String call() throws Exception {
                var result = new StringBuilder();
                for (int i = 0, s = bag.getSize(); i < s; i++) {
                    result.append(bag.get(i)).append(", ");
                }

                return result.toString();
            }
        }

        private record Overwrite(Bag<String> bag, int index, String value) implements Callable<String> {
            @Override
            public String call() throws Exception {
                bag.set(index, value);
                return null;
            }
        }

        private record Remove(Bag<String> bag, int index) implements Callable<String> {
            @Override
            public String call() throws Exception {
                if (index < bag.getSize()) {
                    bag.remove(index);
                }

                return null;
            }
        }

    }

}
