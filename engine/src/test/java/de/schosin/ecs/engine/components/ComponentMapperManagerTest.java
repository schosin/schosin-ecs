package de.schosin.ecs.engine.components;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import de.schosin.ecs.api.Pooled;
import de.schosin.ecs.api.World;
import de.schosin.ecs.api.components.Components;
import de.schosin.ecs.api.components.Components.PooledComponents;
import de.schosin.ecs.api.components.Composition;
import de.schosin.ecs.engine.AbstractWorldTest;
import de.schosin.ecs.engine.EngineWorld;
import de.schosin.ecs.engine.WorldBuilder;

class ComponentMapperManagerTest extends AbstractWorldTest {

    @Test
    void testComponentsReused() {
        var component1 = world.getComponents(Component1.class);

        assertThat(world.getComponents(Component1.class)).isSameAs(component1);
    }

    @Nested
    class ComponentsTest {

        Components<Component1> component1;
        Components<Component2> component2;
        PooledComponents<PooledComponent> pooledComponent;

        @BeforeEach
        void setup() {
            this.component1 = world.getComponents(Component1.class);
            this.component2 = world.getComponents(Component2.class);
            this.pooledComponent = world.getPooledComponents(PooledComponent.class);
        }

        @Test
        void testComponentsDetectsPooled() {
            var world = (EngineWorld) World.builder(WorldBuilder.class.getName()).build(); // code coverage requires new world due to caching

            var components = world.getComponents(PooledComponent.class);
            assertThat(components).isInstanceOf(PooledComponents.class);

            var pooledComponents = world.getPooledComponents(PooledComponent.class);
            assertThat(pooledComponents).isSameAs(components);
        }

        @Test
        void testPooledIntanceReused() {
            var entityId = world.createEntity();
            verifyDoesNotHaveComponent(entityId, PooledComponent.class);
            verifyDoesNotHaveComposition(entityId, Composition.all(PooledComponent.class)); // init composition

            // Add
            var instance1 = pooledComponent.add(entityId);
            assertThat(instance1).isNotNull();

            world.process();
            verifyHasComponent(entityId, PooledComponent.class);
            verifyHasComposition(entityId, Composition.all(PooledComponent.class));

            // Remove
            pooledComponent.remove(entityId);

            world.process();
            verifyDoesNotHaveComponent(entityId, PooledComponent.class);
            verifyDoesNotHaveComposition(entityId, Composition.all(PooledComponent.class));

            // Reuse
            var reusedInstance = pooledComponent.add(entityId);
            assertThat(reusedInstance).isSameAs(instance1);

            world.process();
            verifyHasComponent(entityId, PooledComponent.class);
            verifyHasComposition(entityId, Composition.all(PooledComponent.class));
        }

        @Nested
        class HasComponent {

            @Test
            void testHasComponent() {
                var entity1 = world.createEntity(new Component1());
                var entity2p = world.createEntity(new Component2("data"));

                // Call
                assertThat(component1.has(entity1)).isTrue();
                assertThat(component2.has(entity1)).isFalse();
                assertThat(pooledComponent.has(entity1)).isFalse();

                assertThat(component1.has(entity2p)).isFalse();
                assertThat(component2.has(entity2p)).isTrue();
                assertThat(pooledComponent.has(entity2p)).isFalse();
            }

        }

        @Nested
        class AddTest {

            @Test
            void testAdd() {
                var entityId = world.createEntity();

                var instance1 = new Component1();
                var instance2 = new Component2("foo");
                var pooledInstance = new PooledComponent();
                pooledInstance.data = "bar";

                // Call
                assertThat(component1.add(entityId, instance1)).isSameAs(instance1);
                assertThat(component2.add(entityId, instance2)).isSameAs(instance2);
                assertThat(pooledComponent.add(entityId, pooledInstance)).isSameAs(pooledInstance);

                // Verify
                assertThat(pooledInstance.data).isEqualTo("bar");

                verifyHasComponent(entityId, Component1.class);
                verifyHasComponent(entityId, Component2.class);
                verifyHasComponent(entityId, PooledComponent.class);
                verifyHasComposition(entityId, Composition.all(Component1.class, Component2.class, PooledComponent.class));
            }

        }

        @Nested
        class AddDefaultConstructorTest {

            @Test
            void testPooledIntance() {
                var entityId = world.createEntity();

                // Call
                var instance1 = pooledComponent.add(entityId);
                assertThat(instance1).isNotNull();

                // Verify
                verifyHasComponent(entityId, PooledComponent.class);
                verifyHasComposition(entityId, Composition.all(PooledComponent.class));
            }

        }

        @Nested
        class RemoveComponentTest {

            @Test
            void testRemoveComponent() {
                var entityId = world.createEntity(new Component1(), new Component2("data"));

                // Call
                assertThat(component1.remove(entityId)).isTrue();
                assertThat(component2.remove(entityId)).isTrue();
                assertThat(pooledComponent.remove(entityId)).isFalse();
            }

        }

    }

    public record Component1() {
    }

    public record Component2(String data) {
    }

    public static class PooledComponent implements Pooled {

        private String data;

        @Override
        public void reset() {
            this.data = null;
        }
    }

}
