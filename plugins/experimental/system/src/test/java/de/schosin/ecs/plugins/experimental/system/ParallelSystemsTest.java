package de.schosin.ecs.plugins.experimental.system;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import de.schosin.ecs.api.World;
import de.schosin.ecs.plugins.experimental.system.systems.BaseSystem;
import de.schosin.ecs.utils.collections.IntBag;

public class ParallelSystemsTest {

    @Nested
    class StandaloneTest extends AbstractTest<CompositionWorld> {
        @Override
        protected SystemPlugin plugin() {
            return SystemPlugin.standalone(world, Executors.newFixedThreadPool(10));
        }
    }

    @Nested
    class WorldTest extends AbstractTest<SystemWorld> {

        @Override
        protected SystemWorld createWorld() {
            return World.builder(SystemWorld.class)
                    .configure(new SystemConfig(Executors.newFixedThreadPool(10)))
                    .build();
        }

        @Override
        protected SystemPlugin plugin() {
            return world;
        }
    }

    @Timeout(3)
    abstract class AbstractTest<T extends CompositionWorld> extends AbstractSystemPluginTest<T> {

        private final IntBag processed = new IntBag(4);

        @BeforeEach
        void resetProcessed() {
            processed.clear();
        }

        @Test
        void testRootSystems() {
            var latch1 = new CountDownLatch(1);
            var latch2 = new CountDownLatch(1);

            var system1 = new TestSystem(1, null, latch1);
            var system2 = new TestSystem(2, latch1, latch2);

            plugin.addParallelSystemGroup("parallel", group -> group.add(system1, system2));
            assertThat(processed.getSize()).isZero();

            plugin.processSystems();
            assertThat(processed.getSize()).isEqualTo(2);
            assertThat(processed.getData()).contains(1, 2);

            assertThat(latch1.getCount()).isZero();
            assertThat(latch2.getCount()).isZero();
        }

        @ParameterizedTest
        @CsvSource({
                "0, 100", "100, 0"
        })
        void testSystemGroups(long sleep1, long sleep4) {
            var system1 = new TestSystem(1, sleep1);
            var system2 = new TestSystem(2, 50);
            var system3 = new TestSystem(3, 50);
            var system4 = new TestSystem(4, sleep4);

            // system1+system2 run in parallel before system3+system4 run in parallel
            plugin.addParallelSystemGroup("group1", group -> group.add(system1, system2));
            plugin.addParallelSystemGroup("group2", group -> group.add(system3, system4));

            assertThat(processed.getSize()).isZero();

            plugin.processSystems();
            assertThat(processed.getSize()).isEqualTo(4);
            assertThat(processed.getData()).as("system1 must run before system3 and system4").containsSubsequence(1, 3);
            assertThat(processed.getData()).as("system1 must run before system3 and system4").containsSubsequence(1, 4);
            assertThat(processed.getData()).as("system2 must run before system3 and system4").containsSubsequence(2, 3);
            assertThat(processed.getData()).as("system2 must run before system3 and system4").containsSubsequence(2, 4);
        }

        @ParameterizedTest
        @CsvSource({
                "50, 100, 150, 200, 250",
                "250, 200, 150, 100, 50",
        })
        void testNestedSystemGroups(long sleep1, long sleep2, long sleep3, long sleep4, long sleep5) {
            var system1 = new TestSystem(1, sleep1);
            var system2 = new TestSystem(2, sleep2);
            var system3 = new TestSystem(3, sleep3);
            var system4 = new TestSystem(4, sleep4);
            var system5 = new TestSystem(5, sleep5);

            plugin.addParallelSystemGroup("group1", group -> group
                    .add(system1)
                    .add("nested1", nested -> nested.add(system2))
                    .add("nested2", nested -> nested.add(system3, system4))
                    .add(system5));

            assertThat(processed.getSize()).isZero();

            plugin.processSystems();
            assertThat(processed.getSize()).isEqualTo(5);
            assertThat(processed.getData()).contains(1, 2, 3, 4, 5);
            assertThat(processed.getData()).as("system3 and system4 in sequential system group must run sequentially").containsSubsequence(3, 4);
        }

        @Test
        void testDisabledGroup() {
            var system1 = new TestSystem(1);
            var system2 = new TestSystem(2);
            var system3 = new TestSystem(3);
            var system4 = new TestSystem(4);
            var system5 = new TestSystem(5);

            plugin.addParallelSystemGroup("group1", group -> group
                    .add(system1)
                    .add("nested1", nested -> nested.add(system2))
                    .add("nested2", nested -> nested.add(system3, system4).disable())
                    .add(system5));

            assertThat(processed.getSize()).isZero();

            plugin.processSystems();
            assertThat(processed.getSize()).isEqualTo(3);
            assertThat(processed.getData()).contains(1, 2, 5);
        }

        @Test
        void testDisabledGroup_DisableAfterAdd() {
            var system1 = new TestSystem(1);
            var system2 = new TestSystem(2);
            var system3 = new TestSystem(3);
            var system4 = new TestSystem(4);
            var system5 = new TestSystem(5);

            plugin.addParallelSystemGroup("group1", group -> group
                    .add(system1)
                    .add("nested1", nested -> nested.add(system2))
                    .add("nested2", nested -> nested.add(system3, system4))
                    .add(system5));

            plugin.disable("nested2");

            assertThat(processed.getSize()).isZero();

            plugin.processSystems();
            assertThat(processed.getSize()).isEqualTo(3);
            assertThat(processed.getData()).contains(1, 2, 5);
        }

        @Test
        void testDisabledGroup_EnableAfterAdd() {
            var system1 = new TestSystem(1);
            var system2 = new TestSystem(2);
            var system3 = new TestSystem(3, 500);
            var system4 = new TestSystem(4);
            var system5 = new TestSystem(5);

            plugin.addParallelSystemGroup("group1", group -> group
                    .add(system1)
                    .add("nested1", nested -> nested.add(system2))
                    .add("nested2", nested -> nested.add(system3, system4).disable())
                    .add(system5));

            plugin.enable("nested2");

            assertThat(processed.getSize()).isZero();

            plugin.processSystems();
            assertThat(processed.getSize()).isEqualTo(5);
            assertThat(processed.getData()).contains(1, 2, 3, 4, 5);
            assertThat(processed.getData()).as("system3 and system4 in sequential system group must run sequentially").containsSubsequence(3, 4);
        }

        private class TestSystem implements BaseSystem {

            private final int id;
            private final long sleep;

            private final CountDownLatch latch;
            private final CountDownLatch countdown;

            public TestSystem(int id) {
                this(id, 0L, null, null);
            }

            public TestSystem(int id, long sleep) {
                this(id, sleep, null, null);
            }

            public TestSystem(int id, CountDownLatch latch, CountDownLatch countdown) {
                this(id, 0L, latch, countdown);
            }

            public TestSystem(int id, long sleep, CountDownLatch latch, CountDownLatch countdown) {
                this.id = id;
                this.sleep = sleep;
                this.latch = latch;
                this.countdown = countdown;
            }

            @Override
            public void process() {
                if (sleep > 0L) {
                    try {
                        Thread.sleep(sleep);
                    } catch (InterruptedException ex) {
                        fail("%d: Thread.sleep(%d) failed".formatted(id, sleep), ex);
                    }
                }

                if (latch != null) {
                    try {
                        latch.await();
                    } catch (InterruptedException ex) {
                        fail("%d: latch.await() failed".formatted(id), ex);
                    }
                }
                if (countdown != null) {
                    countdown.countDown();
                }

                synchronized (processed) {
                    processed.add(id);
                }
            }
        }

    }

}
