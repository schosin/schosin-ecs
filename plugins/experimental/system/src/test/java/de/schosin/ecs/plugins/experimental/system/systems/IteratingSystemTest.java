package de.schosin.ecs.plugins.experimental.system.systems;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import de.schosin.ecs.plugins.composition.Composition;
import de.schosin.ecs.plugins.experimental.system.AbstractSystemPluginTest;
import de.schosin.ecs.plugins.experimental.system.CompositionWorld;
import de.schosin.ecs.plugins.experimental.system.SystemPlugin;
import de.schosin.ecs.plugins.experimental.system.SystemWorld;
import de.schosin.ecs.utils.collections.IntBag;

class IteratingSystemTest {

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

        @Test
        void testNoEntities() {
            var system = new TestSystem(world);
            plugin.addSystems(system);

            plugin.processSystems();

            assertThat(system.processed.getSize()).isZero();
        }

        @Test
        void testEntities() {
            var system = new TestSystem(world);
            plugin.addSystems(system);

            var entity1 = world.createEntity();
            var entity2 = world.createEntity();

            plugin.processSystems();

            assertThat(system.processed.getSize()).isEqualTo(2);
            assertThat(system.processed.getData()).contains(entity1, entity2);
        }

        @Test
        void testInserted() {
            var entity1 = world.createEntity();

            var system = new InsertedTestSystem(world);
            plugin.addSystems(system);

            var entity2 = world.createEntity();
            var entity3 = world.createEntity();

            assertThat(system.inserted.getSize()).isEqualTo(2);
            assertThat(system.inserted.getData()).contains(entity2, entity3).doesNotContain(entity1);
        }

        @Test
        void testRemoved() {
            var entity1 = world.createEntity();
            var entity2 = world.createEntity();
            var entity3 = world.createEntity();

            var system = new RemovedTestSystem(world);
            plugin.addSystems(system);

            world.deleteEntity(entity2);
            world.deleteEntity(entity3);
            plugin.processSystems();

            assertThat(system.removed.getSize()).isEqualTo(2);
            assertThat(system.removed.getData()).contains(entity2, entity3).doesNotContain(entity1);
        }

        @Test
        void testInsertedRemoved_NotOverridden() {
            var system = new TestSystem(world);

            // these should not be called when overridden, so they must throw an error
            assertThatThrownBy(() -> system.inserted(42)).isInstanceOf(UnsupportedOperationException.class);
            assertThatThrownBy(() -> system.removed(42)).isInstanceOf(UnsupportedOperationException.class);
        }

        private class TestSystem extends IteratingSystem {

            private final IntBag processed = new IntBag(8);

            public TestSystem(CompositionWorld world) {
                super(world, Composition.all());
            }

            @Override
            protected void processEntity(int entityId) {
                this.processed.add(entityId);
            }

        }

        private class InsertedTestSystem extends IteratingSystem {

            private final IntBag inserted = new IntBag(8);

            public InsertedTestSystem(CompositionWorld world) {
                super(world, Composition.all());
            }

            @Override
            protected void inserted(int entityId) {
                inserted.add(entityId);
            }

            @Override
            protected void processEntity(int entityId) {
            }

        }

        private class RemovedTestSystem extends IteratingSystem {

            private final IntBag removed = new IntBag(8);

            public RemovedTestSystem(CompositionWorld world) {
                super(world, Composition.all());
            }

            @Override
            protected void removed(int entityId) {
                removed.add(entityId);
            }

            @Override
            protected void processEntity(int entityId) {
            }

        }

    }

}
