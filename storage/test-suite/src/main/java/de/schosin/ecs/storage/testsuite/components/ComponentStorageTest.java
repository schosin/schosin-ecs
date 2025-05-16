package de.schosin.ecs.storage.testsuite.components;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import de.schosin.ecs.api.Pooled;
import de.schosin.ecs.api.components.ComponentType;
import de.schosin.ecs.api.components.ComponentType.ClassType;
import de.schosin.ecs.api.components.ComponentType.Wildcard;
import de.schosin.ecs.storage.api.components.Component.ComponentData;
import de.schosin.ecs.storage.api.components.Component.PooledComponentData;
import de.schosin.ecs.storage.testsuite.AbstractStorageEngineTest;
import de.schosin.ecs.utils.collections.Bag;

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
            assertThat(components.isEmpty()).as("result must be empty if no components added").isTrue();
        }

        @Test
        void testSameInstances() {
            var instance1 = engine.getComponent(new ClassType<>(C1.class), NO_OP);
            var instance2 = engine.getComponent(new ClassType<>(C2.class), NO_OP);

            var components = engine.getComponents();

            assertThat(components.getSize()).as("must return all previously added components in any order").isEqualTo(2);
            assertThat(components.get(0)).as("must return all previously added components in any order").isIn(instance1, instance2);
            assertThat(components.get(1)).as("must return all previously added components in any order").isIn(instance1, instance2);
        }

        @Test
        void testDoesNotContainDuplicates() {
            var instance1 = engine.getComponent(new ClassType<>(C1.class), NO_OP);
            engine.getComponent(new ClassType<>(C1.class), NO_OP);

            var components = engine.getComponents();

            assertThat(components.getSize()).as("must not return duplicates if same component retrieved multiple times").isEqualTo(1);
            assertThat(components.get(0)).as("must not return duplicates if same component retrieved multiple times").isIn(instance1);
        }

        @Test
        void testLiveCollection() {
            var components = engine.getComponents();

            var instance1 = engine.getComponent(new ClassType<>(C1.class), NO_OP);
            var instance2 = engine.getComponent(new ClassType<>(C2.class), NO_OP);

            assertThat(components.getSize()).as("must be updated when components added afterwards").isEqualTo(2);
            assertThat(components.get(0)).as("must be updated when components added afterwards").isIn(instance1, instance2);
            assertThat(components.get(1)).as("must be updated when components added afterwards").isIn(instance1, instance2);
        }

        @Test
        void testImmutableBagType() {
            engine.getComponent(new ClassType<>(C1.class), NO_OP);
            engine.getComponent(new ClassType<>(C2.class), NO_OP);

            var components1 = engine.getComponents();
            assertThat(components1).as("must not be a regular Bag").isNotInstanceOf(Bag.class);
        }

        @Test
        void testImmutableBagType_WhenEmpty() {
            var components1 = engine.getComponents();
            assertThat(components1).as("must not be a regular Bag").isNotInstanceOf(Bag.class);
        }

    }

    @Nested
    class GetBoundComponentsTest {

        final Wildcard<Bound> bound = ComponentType.wildcard(Bound.class);

        @Test
        void testInitiallyEmpty() {
            var components = engine.getComponents(bound);

            assertThat(components).as("result must never be null").isNotNull();
            assertThat(components.isEmpty()).as("result must be empty if no components added").isTrue();
        }

        @Test
        void testResult_WhenEqualType_ReturnsSameInstance() {
            var components = engine.getComponents(ComponentType.wildcard(Bound.class));

            assertThat(engine.getComponents(ComponentType.wildcard(Bound.class))).as("must return same instance for equal bounds").isSameAs(components);
        }

        @Test
        void testResult_WhenSameType_ReturnsSameInstance() {
            var components = engine.getComponents(bound);

            assertThat(engine.getComponents(bound)).as("must return same instance for same bounds").isSameAs(components);
        }

        @Test
        void testResult_WhenDifferentType_ReturnsDifferentInstance() {
            var components = engine.getComponents(bound);

            assertThat(ComponentType.WILDCARD).as("must return different instance for different bounds").isNotSameAs(components);
        }

        @Test
        void testMultipleCallsReturnSameImmutableBag() {
            var components = engine.getComponents(ComponentType.wildcard(Bound.class));

            assertThat(engine.getComponents(ComponentType.wildcard(Bound.class))).as("must return same instance for equal wildcards").isSameAs(components);
        }

        @Test
        void testComponentsSameInstances() {
            var instance1 = engine.getComponent(new ClassType<>(C1.class), NO_OP);
            var instance2 = engine.getComponent(new ClassType<>(C2.class), NO_OP);
            engine.getComponent(new ClassType<>(C3.class), NO_OP);

            var components = engine.getComponents(bound);

            assertThat(components.getSize()).as("must return all previously added components in any order").isEqualTo(2);
            assertThat(components.get(0)).as("must return all previously added components in any order").isIn(instance1, instance2);
            assertThat(components.get(1)).as("must return all previously added components in any order").isIn(instance1, instance2);
        }

        @Test
        void testDoesNotContainDuplicates() {
            var instance1 = engine.getComponent(new ClassType<>(C1.class), NO_OP);
            engine.getComponent(new ClassType<>(C1.class), NO_OP);

            var components = engine.getComponents(bound);

            assertThat(components.getSize()).as("must not return duplicates if same component retrieved multiple times").isEqualTo(1);
            assertThat(components.get(0)).as("must not return duplicates if same component retrieved multiple times").isIn(instance1);
        }

        @Test
        void testLiveCollection() {
            var components = engine.getComponents(bound);

            var instance1 = engine.getComponent(new ClassType<>(C1.class), NO_OP);
            var instance2 = engine.getComponent(new ClassType<>(C2.class), NO_OP);
            engine.getComponent(new ClassType<>(C3.class), NO_OP);

            assertThat(components.getSize()).as("must be updated when components added afterwards").isEqualTo(2);
            assertThat(components.get(0)).as("must be updated when components added afterwards").isIn(instance1, instance2);
            assertThat(components.get(1)).as("must be updated when components added afterwards").isIn(instance1, instance2);
        }

        @Test
        void testLiveCollection_ObjectBound() {
            var components = engine.getComponents(ComponentType.wildcard(Object.class));

            var instance1 = engine.getComponent(new ClassType<>(C1.class), NO_OP);
            var instance2 = engine.getComponent(new ClassType<>(C2.class), NO_OP);
            var instance3 = engine.getComponent(new ClassType<>(C3.class), NO_OP);

            assertThat(components.getSize()).as("must be updated when components added afterwards").isEqualTo(3);
            assertThat(components.get(0)).as("must be updated when components added afterwards").isIn(instance1, instance2, instance3);
            assertThat(components.get(1)).as("must be updated when components added afterwards").isIn(instance1, instance2, instance3);
            assertThat(components.get(2)).as("must be updated when components added afterwards").isIn(instance1, instance2, instance3);
        }

        @Test
        void testLiveCollection_ObjectBound_Constant() {
            var components = engine.getComponents(ComponentType.WILDCARD);

            var instance1 = engine.getComponent(new ClassType<>(C1.class), NO_OP);
            var instance2 = engine.getComponent(new ClassType<>(C2.class), NO_OP);
            var instance3 = engine.getComponent(new ClassType<>(C3.class), NO_OP);

            assertThat(components.getSize()).as("must be updated when components added afterwards").isEqualTo(3);
            assertThat(components.get(0)).as("must be updated when components added afterwards").isIn(instance1, instance2, instance3);
            assertThat(components.get(1)).as("must be updated when components added afterwards").isIn(instance1, instance2, instance3);
            assertThat(components.get(2)).as("must be updated when components added afterwards").isIn(instance1, instance2, instance3);
        }

        @Test
        void testImmutableBagType() {
            engine.getComponent(new ClassType<>(C1.class), NO_OP);
            engine.getComponent(new ClassType<>(C2.class), NO_OP);
            engine.getComponent(new ClassType<>(C3.class), NO_OP);

            var components1 = engine.getComponents(bound);
            assertThat(components1).as("must not be a regular Bag").isNotInstanceOf(Bag.class);
        }

        @Test
        void testImmutableBagType_WhenEmpty() {
            var components1 = engine.getComponents(bound);
            assertThat(components1).as("must not be a regular Bag").isNotInstanceOf(Bag.class);
        }

    }

    interface Bound {
    }

    record C1() implements Bound {
    }

    record C2() implements Bound {
    }

    record C3() {
    }

    record P1() implements Pooled {
    }

}
