package de.schosin.ecs.storage.testsuite.components;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import de.schosin.ecs.api.Pooled;
import de.schosin.ecs.api.components.ComponentType.ClassType;
import de.schosin.ecs.storage.api.components.Component.ComponentData;
import de.schosin.ecs.storage.api.components.Component.PooledComponentData;
import de.schosin.ecs.storage.testsuite.AbstractStorageEngineTest;

public class ComponentStorageTest extends AbstractStorageEngineTest {

    @Nested
    class GetComponentTest {

        @Test
        void testByClassType() {
            var instance = engine.getComponent(new ClassType<>(C1.class), NO_OP);

            assertThat(instance).as("returned instance is not null").isNotNull();
            assertThat(instance).as("non-pooled ClassType must return ComponentData").isInstanceOf(ComponentData.class);
        }

        @Test
        void testByClassType_PooledComponent() {
            var instance = engine.getComponent(new ClassType<>(P1.class), NO_OP);

            assertThat(instance).as("returned instance is not null").isNotNull();
            assertThat(instance).as("pooled ClassType must return PooledComponentData").isInstanceOf(PooledComponentData.class);
        }

        @Test
        void testById() {
            var instance = engine.getComponent(new ClassType<>(C1.class), NO_OP);

            var result = engine.getComponent(instance.id());
            assertThat(result).as("returned instance is same as previously created instance").isSameAs(instance);
        }

    }

    @Nested
    class GetPooledComponentTest {

        @Test
        void testByClassType() {
            var instance = engine.getPooledComponent(new ClassType<>(P1.class), NO_OP);

            assertThat(instance).as("returned instance is not null").isNotNull();
        }

        @Test
        void testById() {
            var instance = engine.getPooledComponent(new ClassType<>(P1.class), NO_OP);

            var result = engine.getComponent(instance.id());
            assertThat(result).as("returned instance is same as previously created instance").isSameAs(instance);
        }

    }

    @Nested
    class ComponentInstanceReusedTest {

        @Test
        void testInstanceReused_SameComponentTypes() {
            var componentType = new ClassType<>(C1.class);
            var instance1 = engine.getComponent(componentType, NO_OP);
            var instance2 = engine.getComponent(componentType, NO_OP);

            assertThat(instance2).as("getComponent returns same instance for same componentType").isSameAs(instance1);
        }

        @Test
        void testInstanceReused_EqualComponentTypes() {
            var instance1 = engine.getComponent(new ClassType<>(C1.class), NO_OP);
            var instance2 = engine.getComponent(new ClassType<>(C1.class), NO_OP);

            assertThat(instance2).as("getComponent returns same instance for equal componentTypes").isSameAs(instance1);
        }

        @Test
        void testPooledInstanceReused_SameComponentTypes() {
            var componentType = new ClassType<>(P1.class);
            var instance1 = engine.getComponent(componentType, NO_OP);
            var instance2 = engine.getComponent(componentType, NO_OP);
            var instance3 = engine.getPooledComponent(componentType, NO_OP);

            assertThat(instance2).as("getComponent returns same instance for same componentType").isSameAs(instance1);
            assertThat(instance3).as("getPooledComponent returns same instance for same componentType").isSameAs(instance1);
        }

        @Test
        void testPooledInstanceReused_EqualComponentTypes() {
            var instance1 = engine.getComponent(new ClassType<>(P1.class), NO_OP);
            var instance2 = engine.getComponent(new ClassType<>(P1.class), NO_OP);
            var instance3 = engine.getPooledComponent(new ClassType<>(P1.class), NO_OP);

            assertThat(instance2).as("getComponent returns same instance for equal componentTypes").isSameAs(instance1);
            assertThat(instance3).as("getPooledComponent returns same instance for equal componentTypes").isSameAs(instance1);
        }

    }

    @Nested
    class GetComponentsTest {

        @Test
        void testInitiallyEmpty() {
            var components = engine.getComponents();

            assertThat(components).as("result must never be null").isNotNull();
            assertThat(components).as("result must be empty if no components added").isEmpty();
        }

        @Test
        void testSameInstances() {
            var instance1 = engine.getComponent(new ClassType<>(C1.class), NO_OP);
            var instance2 = engine.getComponent(new ClassType<>(C2.class), NO_OP);

            var components = engine.getComponents();
            assertThat(components).as("must return all previously added components in any order").containsExactlyInAnyOrder(instance1, instance2);
        }

        @Test
        void testDoesNotContainDuplicates() {
            var instance1 = engine.getComponent(new ClassType<>(C1.class), NO_OP);
            engine.getComponent(new ClassType<>(C1.class), NO_OP);

            var components = engine.getComponents();
            assertThat(components).as("must not return duplicates if same component retrieved multiple times").containsExactlyInAnyOrder(instance1);
        }

        @Test
        void testLiveCollection() {
            var components = engine.getComponents();

            var instance1 = engine.getComponent(new ClassType<>(C1.class), NO_OP);
            var instance2 = engine.getComponent(new ClassType<>(C2.class), NO_OP);

            assertThat(components).as("must be updated when components added afterwards").containsExactlyInAnyOrder(instance1, instance2);
        }

        @Test
        void testAdd() {
            var instance1 = engine.getComponent(new ClassType<>(C1.class), NO_OP);
            var instance2 = engine.getComponent(new ClassType<>(C2.class), NO_OP);

            var components1 = engine.getComponents();
            var components2 = engine.getComponents();

            try {
                components1.add(instance1);

                assertThat(components1).as("add must not change collection").containsExactlyInAnyOrder(instance1, instance2);
                assertThat(components2).as("add must not change other retrieved collections").containsExactlyInAnyOrder(instance1, instance2);
            } catch (RuntimeException ex) {
                // acceptable behaviour
            }
        }

        @Test
        void testRemove() {
            var instance1 = engine.getComponent(new ClassType<>(C1.class), NO_OP);
            var instance2 = engine.getComponent(new ClassType<>(C2.class), NO_OP);

            var components1 = engine.getComponents();
            var components2 = engine.getComponents();

            try {
                assertThat(components1.remove(instance1)).as("remove must return false").isFalse();

                assertThat(components1).as("remove must not change collection").containsExactlyInAnyOrder(instance1, instance2);
                assertThat(components2).as("remove must not change other retrieved collections").containsExactlyInAnyOrder(instance1, instance2);
            } catch (RuntimeException ex) {
                // acceptable behaviour
            }
        }

        @Test
        void testClear() {
            var instance1 = engine.getComponent(new ClassType<>(C1.class), NO_OP);
            var instance2 = engine.getComponent(new ClassType<>(C2.class), NO_OP);

            var components1 = engine.getComponents();
            var components2 = engine.getComponents();

            try {
                components1.clear();

                assertThat(components1).as("clear must not change collection").containsExactlyInAnyOrder(instance1, instance2);
                assertThat(components2).as("clear must not change other retrieved collections").containsExactlyInAnyOrder(instance1, instance2);
            } catch (RuntimeException ex) {
                // acceptable behaviour
            }
        }

    }

    record C1() {
    }

    record C2() {
    }

    record P1() implements Pooled {
    }

}
