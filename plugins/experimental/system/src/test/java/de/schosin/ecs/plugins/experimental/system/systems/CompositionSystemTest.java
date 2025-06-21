package de.schosin.ecs.plugins.experimental.system.systems;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import de.schosin.ecs.plugins.composition.Composition;
import de.schosin.ecs.plugins.data.types.DataType2;
import de.schosin.ecs.plugins.experimental.system.AbstractSystemPluginTest;
import de.schosin.ecs.plugins.experimental.system.CompositionWorld;
import de.schosin.ecs.plugins.experimental.system.SystemPlugin;
import de.schosin.ecs.plugins.experimental.system.SystemWorld;
import de.schosin.ecs.utils.collections.IntBag;

class CompositionSystemTest {

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

            var entity1 = world.createEntity(new C1(), new C2());
            var entity2 = world.createEntity(new C1(), new C2());

            plugin.processSystems();

            assertThat(system.processed.getSize()).isEqualTo(2);
            assertThat(system.processed.getData()).contains(entity1, entity2);
        }

        @Test
        void testInserted() {
            var entity1 = world.createEntity();

            var system = new InsertedTestSystem(world);
            plugin.addSystems(system);

            var entity2 = world.createEntity(new C1(), new C2());
            var entity3 = world.createEntity(new C1(), new C2());

            assertThat(system.inserted.getSize()).isEqualTo(2);
            assertThat(system.inserted.getData()).contains(entity2, entity3).doesNotContain(entity1);
        }

        @Test
        void testRemoved() {
            var entity1 = world.createEntity(new C1(), new C2());
            var entity2 = world.createEntity(new C1(), new C2());
            var entity3 = world.createEntity(new C1(), new C2());

            var system = new RemovedTestSystem(world);
            plugin.addSystems(system);

            world.deleteEntity(entity2);
            world.deleteEntity(entity3);
            plugin.processSystems();

            assertThat(system.removed.getSize()).isEqualTo(2);
            assertThat(system.removed.getData()).contains(entity2, entity3).doesNotContain(entity1);
        }

        @Test
        void testNotImplementingProcessor() {
            assertThatThrownBy(() -> new InvalidTestSystem())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("must implement");
        }

        private class TestSystem extends CompositionSystem<DataType2.Processor2<C1, C2>> implements DataType2.Processor2<C1, C2> {

            private final IntBag processed = new IntBag(8);

            public TestSystem(CompositionWorld world) {
                super(world.createComposition(Composition.all(), C1.class, C2.class));
            }

            @Override
            public void process(int entityId, C1 c1, C2 c2) {
                assertThat(c1).isNotNull();
                assertThat(c2).isNotNull();

                processed.add(entityId);
            }

        }

        private class InsertedTestSystem extends CompositionSystem<DataType2.Processor2<C1, C2>> implements DataType2.Processor2<C1, C2> {

            private final IntBag inserted = new IntBag(8);

            public InsertedTestSystem(CompositionWorld world) {
                super(world.createComposition(Composition.all(), C1.class, C2.class));

                this.composition.inserted(this::handleInserted);
            }

            private void handleInserted(int entityId, C1 c1, C2 c2) {
                assertThat(c1).isNotNull();
                assertThat(c2).isNotNull();

                inserted.add(entityId);
            }

            @Override
            public void process(int entityId, C1 c1, C2 c2) {
            }

        }

        private class RemovedTestSystem extends CompositionSystem<DataType2.Processor2<C1, C2>> implements DataType2.Processor2<C1, C2> {

            private final IntBag removed = new IntBag(8);

            public RemovedTestSystem(CompositionWorld world) {
                super(world.createComposition(Composition.all(), C1.class, C2.class));

                this.composition.removed(this::handleRemoved);
            }

            private void handleRemoved(int entityId, C1 c1, C2 c2) {
                assertThat(c1).isNotNull();
                assertThat(c2).isNotNull();

                removed.add(entityId);
            }

            @Override
            public void process(int entityId, C1 c1, C2 c2) {
            }

        }

        private class InvalidTestSystem extends CompositionSystem<DataType2.Processor2<C1, C2>> {
            public InvalidTestSystem() {
                super(world.createComposition(Composition.all(), C1.class, C2.class));
            }
        }

    }

    private record C1() {
    }

    private record C2() {
    }

}
