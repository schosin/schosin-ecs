package de.schosin.ecs.engine.components;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import de.schosin.ecs.api.Pooled;
import de.schosin.ecs.api.World;
import de.schosin.ecs.api.components.Components;
import de.schosin.ecs.api.components.Components.EnumComponents;
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
        EnumComponents<EnumComponent> firstEnum;
        EnumComponents<EnumComponent> secondEnum;

        @BeforeEach
        void setup() {
            this.component1 = world.getComponents(Component1.class);
            this.component2 = world.getComponents(Component2.class);
            this.pooledComponent = world.getPooledComponents(PooledComponent.class);
            this.firstEnum = world.getEnumComponents(EnumComponent.FIRST);
            this.secondEnum = world.getEnumComponents(EnumComponent.SECOND);
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
                var entityId = world.createEntity(new Component1());
                var entityIdPooled = world.createEntity(new Component2("data"));
                var entityIdEnum = world.createEntity(EnumComponent.FIRST);

                // Call
                assertThat(component1.has(entityId)).isTrue();
                assertThat(component2.has(entityId)).isFalse();
                assertThat(pooledComponent.has(entityId)).isFalse();
                assertThat(firstEnum.has(entityId)).isFalse();
                assertThat(secondEnum.has(entityId)).isFalse();

                assertThat(component1.has(entityIdPooled)).isFalse();
                assertThat(component2.has(entityIdPooled)).isTrue();
                assertThat(pooledComponent.has(entityIdPooled)).isFalse();
                assertThat(firstEnum.has(entityIdPooled)).isFalse();
                assertThat(secondEnum.has(entityIdPooled)).isFalse();

                assertThat(component1.has(entityIdEnum)).isFalse();
                assertThat(component2.has(entityIdEnum)).isFalse();
                assertThat(pooledComponent.has(entityIdEnum)).isFalse();
                assertThat(firstEnum.has(entityIdEnum)).isTrue();
                assertThat(secondEnum.has(entityIdEnum)).isTrue();
            }

        }

        @Nested
        class GetComponent {

            @Test
            void testGetComponent() {
                var component = new Component1();
                var entityId = world.createEntity(component);

                var componentPooled = new Component2("data");
                var entityIdPooled = world.createEntity(componentPooled);

                var entityIdEnum = world.createEntity(EnumComponent.FIRST);

                // Call
                assertThat(component1.get(entityId)).isEqualTo(component);
                assertThat(component2.get(entityId)).isNull();
                assertThat(pooledComponent.get(entityId)).isNull();
                assertThat(firstEnum.get(entityId)).isNull();
                assertThat(secondEnum.get(entityId)).isNull();

                assertThat(component1.get(entityIdPooled)).isNull();
                assertThat(component2.get(entityIdPooled)).isEqualTo(componentPooled);
                assertThat(pooledComponent.get(entityIdPooled)).isNull();
                assertThat(firstEnum.get(entityIdPooled)).isNull();
                assertThat(secondEnum.get(entityIdPooled)).isNull();

                assertThat(component1.get(entityIdEnum)).isNull();
                assertThat(component2.get(entityIdEnum)).isNull();
                assertThat(pooledComponent.get(entityIdEnum)).isNull();
                assertThat(firstEnum.get(entityIdEnum)).isEqualTo(EnumComponent.FIRST);
                assertThat(secondEnum.get(entityIdEnum)).isEqualTo(EnumComponent.FIRST);
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

                assertThat(firstEnum.add(entityId, EnumComponent.FIRST)).isSameAs(EnumComponent.FIRST);
                assertThat(secondEnum.add(entityId, EnumComponent.FIRST)).isSameAs(EnumComponent.FIRST);

                assertThat(firstEnum.add(entityId, EnumComponent.SECOND)).isSameAs(EnumComponent.SECOND);
                assertThat(secondEnum.add(entityId, EnumComponent.SECOND)).isSameAs(EnumComponent.SECOND);

                // Verify
                assertThat(pooledInstance.data).isEqualTo("bar");

                verifyHasComponent(entityId, Component1.class);
                verifyHasComponent(entityId, Component2.class);
                verifyHasComponent(entityId, PooledComponent.class);
                verifyHasComponent(entityId, EnumComponent.class);
                verifyHasComposition(entityId, Composition.all(Component1.class, Component2.class, PooledComponent.class, EnumComponent.class));
            }

            @Test
            void testAdd_WhenAlreadyContained_ReturnsExisting() {
                var pooled = new PooledComponent();
                var entityId = world.createEntity(pooled);

                // Call
                assertThat(pooledComponent.add(entityId)).isSameAs(pooled);
            }

        }

        @Nested
        class EnumComponentsTest {

            @Test
            void testGetDefault_WhenFirstEnum() {
                assertThat(firstEnum.getDefault()).isSameAs(EnumComponent.FIRST);
            }

            @Test
            void testGetDefault_WhenSecondEnum() {
                assertThat(secondEnum.getDefault()).isSameAs(EnumComponent.SECOND);
            }

            @Test
            void testAdd_WhenFirstEnum() {
                var entityId = world.createEntity();

                // Call
                var instance1 = firstEnum.add(entityId);
                assertThat(instance1).isSameAs(EnumComponent.FIRST);

                // Verify
                verifyHasComponent(entityId, EnumComponent.class);
                verifyHasComposition(entityId, Composition.all(EnumComponent.class));
            }

            @Test
            void testAdd_WhenSecondEnum() {
                var entityId = world.createEntity();

                // Call
                var instance1 = secondEnum.add(entityId);
                assertThat(instance1).isSameAs(EnumComponent.SECOND);

                // Verify
                verifyHasComponent(entityId, EnumComponent.class);
                verifyHasComposition(entityId, Composition.all(EnumComponent.class));
            }

        }

        @Nested
        class PooledComponentsTest {

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
                var entityId = world.createEntity(new Component1(), new Component2("data"), EnumComponent.SECOND);

                // Call
                assertThat(component1.remove(entityId)).isTrue();
                assertThat(component2.remove(entityId)).isTrue();
                assertThat(pooledComponent.remove(entityId)).isFalse();
                assertThat(firstEnum.remove(entityId)).isTrue();
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

    enum EnumComponent {
        FIRST, SECOND
    }

}
