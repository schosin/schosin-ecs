package de.schosin.ecs.engine;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import de.schosin.ecs.api.Pooled;
import de.schosin.ecs.api.components.Components.PooledComponents;
import de.schosin.ecs.api.components.Composition;
import de.schosin.ecs.engine.utils.collections.IntBag;

class ChangeManagerTest extends AbstractWorldTest {

    @Nested
    class InsertedRemovedDependenciesTest {

        PooledComponents<C1> component1;
        PooledComponents<C2> component2;

        @BeforeEach
        void setup() {
            this.component1 = world.getPooledComponents(C1.class);
            this.component2 = world.getPooledComponents(C2.class);
        }

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
            void testInserted2DependsOnInserted1() {
                // Setup
                var entityId = world.createEntity();
                var inserted = new IntBag(1);

                var composition1 = world.createComposition(Composition.all(C1.class));
                composition1.inserted(component2::add);

                var composition2 = world.createComposition(Composition.all(C2.class));
                composition2.inserted(inserted::add);

                // Call
                var otherEntityId = add1(entityId);
                world.process();

                // Verify
                assertThat(inserted.getSize()).isEqualTo(1);
                assertThat(inserted.getData()).containsExactly(otherEntityId);
            }

            @Test
            void testInserted1DependsOnInserted2() {
                // Setup
                var entityId = world.createEntity();
                var inserted = new IntBag(1);

                var composition1 = world.createComposition(Composition.all(C1.class));
                composition1.inserted(inserted::add);

                var composition2 = world.createComposition(Composition.all(C2.class));
                composition2.inserted(component1::add);

                // Call
                var otherEntityId = add2(entityId);
                world.process();

                // Verify
                assertThat(inserted.getSize()).isEqualTo(1);
                assertThat(inserted.getData()).containsExactly(otherEntityId);
            }

            @Test
            void testInsertedDependsOnRemoved() {
                // Setup
                var entityId = world.createEntity(new C1());
                var entityId2 = world.createEntity();
                var inserted = new IntBag(1);

                var composition1 = world.createComposition(Composition.all(C1.class));
                composition1.removed(ignore -> component2.add(entityId2));

                var composition2 = world.createComposition(Composition.all(C2.class));
                composition2.inserted(inserted::add);

                // Call
                remove1(entityId);
                world.process();

                // Verify
                assertThat(inserted.getSize()).isEqualTo(1);
                assertThat(inserted.getData()).containsExactly(entityId2);
            }

            @Test
            void testRemovedDependsOnInserted() {
                // Setup
                var entityId = world.createEntity();
                var entityId2 = world.createEntity(new C2());
                var removed = new IntBag(1);

                var composition1 = world.createComposition(Composition.all(C1.class));
                composition1.inserted(ignore -> component2.remove(entityId2));

                var composition2 = world.createComposition(Composition.all(C2.class));
                composition2.removed(removed::add);

                // Call
                add1(entityId);
                world.process();

                // Verify
                assertThat(removed.getSize()).isEqualTo(1);
                assertThat(removed.getData()).containsExactly(entityId2);
            }

            @Test
            void testRemoved2DependsOnRemoved1() {
                // Setup
                var entityId = world.createEntity(new C1(), new C2());
                var removed = new IntBag(1);

                var composition1 = world.createComposition(Composition.all(C1.class));
                composition1.removed(component2::remove);

                var composition2 = world.createComposition(Composition.all(C2.class));
                composition2.removed(removed::add);

                // Call
                remove1(entityId);
                world.process();

                // Verify
                assertThat(removed.getSize()).isEqualTo(1);
                assertThat(removed.getData()).containsExactly(entityId);
            }

            @Test
            void testRemoved1DependsOnRemoved2() {
                // Setup
                var entityId = world.createEntity(new C1(), new C2());
                var removed = new IntBag(1);

                var composition1 = world.createComposition(Composition.all(C1.class));
                composition1.removed(removed::add);

                var composition2 = world.createComposition(Composition.all(C2.class));
                composition2.removed(component1::remove);

                // Call
                remove2(entityId);
                world.process();

                // Verify
                assertThat(removed.getSize()).isEqualTo(1);
                assertThat(removed.getData()).containsExactly(entityId);
            }

        }

    }

    public record C1() implements Pooled {
    }

    public record C2() implements Pooled {
    }

}
