package de.schosin.ecs.plugins.experimental.system;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import de.schosin.ecs.plugins.experimental.system.systems.BaseSystem;
import de.schosin.ecs.utils.collections.IntBag;

public class SequentialSystemsTest {

    @Nested
    class StandaloneTest extends AbstractTest<CompositionWorld> {
        @Override
        protected SystemPlugin plugin() {
            return SystemPlugin.standalone(world);
        }
    }

    @Nested
    class WorldTest extends AbstractTest<SystemWorld> {
        @Override
        protected SystemPlugin plugin() {
            return world;
        }
    }

    abstract class AbstractTest<T extends CompositionWorld> extends AbstractSystemPluginTest<T> {

        private final IntBag processed = new IntBag(4);

        @BeforeEach
        void resetProcessed() {
            processed.clear();
        }

        @Test
        void testRootSystems() {
            var system1 = new TestSystem(1);
            var system2 = new TestSystem(2);

            plugin.addSystems(system1, system2);
            assertThat(processed.getSize()).isZero();

            plugin.processSystems();
            assertThat(processed.getSize()).isEqualTo(2);
            assertThat(processed.getData()).startsWith(1, 2);
        }

        @Test
        void testSystemGroups() {
            var system1 = new TestSystem(1);
            var system2 = new TestSystem(2);
            var system3 = new TestSystem(3);
            var system4 = new TestSystem(4);

            plugin.addSystemGroup("group1", group -> group.add(system1, system2));
            plugin.addSystemGroup("group2", group -> group.add(system3, system4));

            assertThat(processed.getSize()).isZero();

            plugin.processSystems();
            assertThat(processed.getSize()).isEqualTo(4);
            assertThat(processed.getData()).startsWith(1, 2, 3, 4);
        }

        @Test
        void testNestedSystemGroups() {
            var system1 = new TestSystem(1);
            var system2 = new TestSystem(2);
            var system3 = new TestSystem(3);
            var system4 = new TestSystem(4);
            var system5 = new TestSystem(5);

            plugin.addSystemGroup("group1", group -> group
                    .add(system1)
                    .add("nested1", nested -> nested.add(system2))
                    .add("nested2", nested -> nested.add(system3, system4))
                    .add(system5));

            assertThat(processed.getSize()).isZero();

            plugin.processSystems();
            assertThat(processed.getSize()).isEqualTo(5);
            assertThat(processed.getData()).startsWith(1, 2, 3, 4, 5);
        }

        @Test
        void testDisabledGroup() {
            var system1 = new TestSystem(1);
            var system2 = new TestSystem(2);
            var system3 = new TestSystem(3);
            var system4 = new TestSystem(4);
            var system5 = new TestSystem(5);

            plugin.addSystemGroup("group1", group -> group
                    .add(system1)
                    .add("nested1", nested -> nested.add(system2))
                    .add("nested2", nested -> nested.add(system3, system4).disable())
                    .add(system5));

            assertThat(processed.getSize()).isZero();

            plugin.processSystems();
            assertThat(processed.getSize()).isEqualTo(3);
            assertThat(processed.getData()).startsWith(1, 2, 5);
        }

        @Test
        void testDisabledGroup_DisableAfterAdd() {
            var system1 = new TestSystem(1);
            var system2 = new TestSystem(2);
            var system3 = new TestSystem(3);
            var system4 = new TestSystem(4);
            var system5 = new TestSystem(5);

            plugin.addSystemGroup("group1", group -> group
                    .add(system1)
                    .add("nested1", nested -> nested.add(system2))
                    .add("nested2", nested -> nested.add(system3, system4))
                    .add(system5));

            plugin.disable("nested2");

            assertThat(processed.getSize()).isZero();

            plugin.processSystems();
            assertThat(processed.getSize()).isEqualTo(3);
            assertThat(processed.getData()).startsWith(1, 2, 5);
        }

        @Test
        void testDisabledGroup_EnableAfterAdd() {
            var system1 = new TestSystem(1);
            var system2 = new TestSystem(2);
            var system3 = new TestSystem(3);
            var system4 = new TestSystem(4);
            var system5 = new TestSystem(5);

            plugin.addSystemGroup("group1", group -> group
                    .add(system1)
                    .add("nested1", nested -> nested.add(system2))
                    .add("nested2", nested -> nested.add(system3, system4).disable())
                    .add(system5));

            plugin.enable("nested2");

            assertThat(processed.getSize()).isZero();

            plugin.processSystems();
            assertThat(processed.getSize()).isEqualTo(5);
            assertThat(processed.getData()).startsWith(1, 2, 3, 4, 5);
        }

        private class TestSystem implements BaseSystem {

            private final int id;

            public TestSystem(int id) {
                this.id = id;
            }

            @Override
            public void process() {
                processed.add(id);
            }
        }

    }

}
