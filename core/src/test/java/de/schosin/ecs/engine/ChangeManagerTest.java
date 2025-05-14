package de.schosin.ecs.engine;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import de.schosin.ecs.api.Pooled;
import de.schosin.ecs.api.components.Components.PooledComponents;
import de.schosin.ecs.engine.events.builtin.EntityEvent.EntityInsertedEvent;
import de.schosin.ecs.engine.events.builtin.EntityEvent.EntityUpdatedEvent;
import de.schosin.ecs.engine.utils.collections.IntBag;

class ChangeManagerTest extends AbstractWorldTest {

    PooledComponents<C1> component1;
    PooledComponents<C2> component2;

    int id1;
    int id2;

    @BeforeEach
    void setup() {
        this.component1 = world.getPooledComponents(C1.class);
        this.component2 = world.getPooledComponents(C2.class);

        this.id1 = componentManager.getComponent(component(C1.class)).id();
        this.id2 = componentManager.getComponent(component(C2.class)).id();
    }

    @Nested
    class InsertedRemovedDependenciesTest {

        @Nested
        class EntityCreationRemoval extends AbstractTest {
            @Override
            protected int add1(int entityId) {
                return world.createEntity(new C1());
            }

            @Override
            protected int add2(int entityId) {
                return world.createEntity(new C2());
            }

            @Override
            protected void remove1(int entityId) {
                world.deleteEntity(entityId);
            }

            @Override
            protected void remove2(int entityId) {
                world.deleteEntity(entityId);
            }
        }

        @Nested
        class ComponentCompositionChange extends AbstractTest {
            @Override
            protected int add1(int entityId) {
                component1.add(entityId);
                return entityId;
            }

            @Override
            protected int add2(int entityId) {
                component2.add(entityId);
                return entityId;
            }

            @Override
            protected void remove1(int entityId) {
                component1.remove(entityId);
            }

            @Override
            protected void remove2(int entityId) {
                component2.remove(entityId);
            }
        }

        abstract class AbstractTest {

            protected abstract int add1(int entityId);

            protected abstract int add2(int entityId);

            protected abstract void remove1(int entityId);

            protected abstract void remove2(int entityId);

            @Test
            void testUpdated2DependsOnUpdated1() {
                // Setup
                var entityId = world.createEntity();
                var updated = new IntBag(1);

                eventManager.registerEventHandler(EntityInsertedEvent.class, event -> {
                    if (event.componentMask().contains(id1)) {
                        component2.add(event.entityId());
                    }
                });

                eventManager.registerEventHandler(EntityUpdatedEvent.class, event -> {
                    var mask = event.componentMask();
                    var prevMask = event.previousComponentMask();
                    var id = event.entityId();

                    if (!prevMask.contains(id1) && mask.contains(id1)) {
                        component2.add(id);
                    }

                    if (!prevMask.contains(id2) && mask.contains(id2)) {
                        updated.add(id);
                    }
                });

                // Call
                var otherEntityId = add1(entityId);
                world.process();

                // Verify
                assertThat(updated.getSize()).isEqualTo(1);
                assertThat(updated.getData()).containsExactly(otherEntityId);
            }

            @Test
            void testUpdated1DependsOnUpdated2() {
                // Setup
                var entityId = world.createEntity();
                var updated = new IntBag(1);

                eventManager.registerEventHandler(EntityInsertedEvent.class, event -> {
                    if (event.componentMask().contains(id2)) {
                        component1.add(event.entityId());
                    }
                });

                eventManager.registerEventHandler(EntityUpdatedEvent.class, event -> {
                    var mask = event.componentMask();
                    var prevMask = event.previousComponentMask();
                    var id = event.entityId();

                    if (!prevMask.contains(id1) && mask.contains(id1)) {
                        updated.add(id);
                    }

                    if (!prevMask.contains(id2) && mask.contains(id2)) {
                        component1.add(id);
                    }
                });

                // Call
                var otherEntityId = add2(entityId);
                world.process();

                // Verify
                assertThat(updated.getSize()).isEqualTo(1);
                assertThat(updated.getData()).containsExactly(otherEntityId);
            }

            @Test
            void testCompositionUpdateBeforeComponentRemoval() {
                var resetting = new Resetting().init("foobar");

                var entityId = world.createEntity(resetting);
                var removed = new IntBag(1);

                var mapper = world.getComponents(Resetting.class);

                eventManager.registerEventHandler(EntityUpdatedEvent.class, event -> {
                    var component = mapper.get(event.entityId());
                    removed.add(component.data.length());
                });

                // Call
                world.getComponents(Resetting.class).remove(entityId);
                world.process();

                // Verify
                assertThat(removed.getSize()).isEqualTo(1);
                assertThat(removed.getData()).containsExactly("foobar".length());
            }

        }

    }

    @Nested
    class FlushEntityUpdatesTest {

        @Test
        void testFlushEntityUpdates_WhenNoPendingUpdates() {
            var entityId = world.createEntity();

            // Call
            assertThat(world.flushEntityUpdates(entityId)).isTrue();
        }

        @Test
        void testFlushEntityUpdates_WhenComponentAdded_AddsComponentAndUpdatesComposition() {
            // Setup            
            var entityId = world.createEntity();
            verifyDoesNotHaveComponents(entityId, C1.class);
            verifyComponentMaskDoesNotHaveComponents(entityId, C1.class);

            // Add component
            component1.add(entityId);
            verifyHasComponents(entityId, C1.class);
            verifyComponentMaskDoesNotHaveComponents(entityId, C1.class);

            // Call
            assertThat(world.flushEntityUpdates(entityId)).isTrue();

            // Verify
            verifyHasComponents(entityId, C1.class);
            verifyComponentMaskHasComponents(entityId, C1.class);
        }

        @Test
        void testFlushEntityUpdates_WhenComponentRemoved_UpdatesOnlyComposition() {
            // Setup            
            var entityId = world.createEntity(new C1());
            verifyHasComponents(entityId, C1.class);
            verifyComponentMaskHasComponents(entityId, C1.class);

            // Add component
            component1.remove(entityId);
            verifyHasComponents(entityId, C1.class);
            verifyComponentMaskHasComponents(entityId, C1.class);

            // Call
            assertThat(world.flushEntityUpdates(entityId)).isTrue();

            // Verify
            verifyHasComponents(entityId, C1.class);
            verifyComponentMaskDoesNotHaveComponents(entityId, C1.class);
        }

        @Test
        void testProcess_WhenRemovedComponentFlushed_RemovesComponent() {
            // Setup            
            var entityId = world.createEntity(new C1());
            verifyHasComponents(entityId, C1.class);
            verifyComponentMaskHasComponents(entityId, C1.class);

            // Add component
            component1.remove(entityId);
            verifyHasComponents(entityId, C1.class);
            verifyComponentMaskHasComponents(entityId, C1.class);

            // Flush updates
            assertThat(world.flushEntityUpdates(entityId)).isTrue();
            verifyHasComponents(entityId, C1.class);
            verifyComponentMaskDoesNotHaveComponents(entityId, C1.class);

            // Call
            world.process();

            // Verify
            verifyDoesNotHaveComponents(entityId, C1.class);
            verifyComponentMaskDoesNotHaveComponents(entityId, C1.class);
        }

    }

    public record C1() implements Pooled {
    }

    public record C2() implements Pooled {
    }

    public class Resetting implements Pooled {

        public String data;

        public Resetting init(String data) {
            this.data = data;

            return this;
        }

        @Override
        public void reset() {
            this.data = null;
        }

    }
}
