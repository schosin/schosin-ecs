package de.schosin.ecs.storage.testsuite.components;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.IdentityHashMap;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import de.schosin.ecs.api.Pooled;
import de.schosin.ecs.api.components.types.ClassType;
import de.schosin.ecs.storage.api.components.Component.PooledComponentData;
import de.schosin.ecs.storage.testsuite.components.PooledComponentDataTest.P1;
import de.schosin.ecs.storage.testsuite.components.PooledComponentDataTest.P2;
import de.schosin.ecs.storage.testsuite.components.PooledComponentDataTest.P3;

public class PooledComponentDataTest extends CommonClassTypeTest<P1, P2, P3> {

    @Nested
    class ClazzTest extends AbstractTypeTest {

        @ParameterizedTest
        @MethodSource(TYPES)
        void testClazzMatchesClassType(ClassType<? extends Pooled> type) {
            assertThat(getComponent(type).clazz()).as("component.clazz() is same as argument clazz").isSameAs(type.clazz());
        }

    }

    @Nested
    class PooledTest extends AbstractTypeTest {

        @ParameterizedTest
        @MethodSource(TYPES)
        void testGetInstance_DoesNotReturnNull(ClassType<? extends Pooled> classType) {
            var component = getComponent(classType);

            assertThat(component.getInstance()).as("getInstance does not return null").isNotNull();
        }

        @ParameterizedTest
        @MethodSource(TYPES)
        void testGetInstance_ReturnsNewInstancesForMultipleCalls(ClassType<? extends Pooled> classType) {
            var component = getComponent(classType);

            var instances = new IdentityHashMap<>();

            var instance = component.getInstance();
            assertThat(instances.put(instance, "foo")).as("sanity check").isNull();
            assertThat(instances.put(instance, "bar")).as("sanity check").isEqualTo("foo");

            for (int i = 0, s = 10; i < s; i++) {
                assertThat(instances.put(component.getInstance(), "test")).as("getInstance does not return duplicates").isNull();
            }
        }

        @ParameterizedTest
        @MethodSource(TYPES)
        void testGetInstance_ReturnsInstanceOfMatchingType(ClassType<? extends Pooled> classType) {
            var component = getComponent(classType);

            assertThat(component.getInstance()).as("getInstance returns instance of same class").isExactlyInstanceOf(classType.clazz());
        }

        @ParameterizedTest
        @ValueSource(classes = {
                PackagePrivateComponent.class,
                PrivateComponent.class,
                NoDefaultConstructorComponent.class,
                PackagePrivateConstructorComponent.class,
                PrivateConstructorComponent.class
        })
        void testGetInstance_MayThrowIfConstructorInaccessible(Class<? extends Pooled> clazz) {
            var component = getComponent(new ClassType<>(clazz));

            try {
                var instance = component.getInstance();

                assertThat(instance).as("getInstance must not return null").isNotNull();
                assertThat(instance).as("getInstance returns instance of same class").isExactlyInstanceOf(clazz);
            } catch (RuntimeException ex) {
                // this is okay
            }
        }

        @ParameterizedTest
        @MethodSource(TYPES)
        void testGetInstance_InstanceReusedWhenRemoved(ClassType<? extends Pooled> classType) {
            var component = getComponent(classType);

            var instance = component.getInstance();
            var entityId = world.createEntity(instance);

            component.removeComponent(entityId);

            assertThat(component.getInstance()).as("getInstance reuses removed instances").isSameAs(instance);
        }

    }

    @Override
    protected P1 getInstance1() {
        return new P1();
    }

    @Override
    protected P2 getInstance2() {
        return new P2();
    }

    @Override
    protected P3 getInstance3() {
        return new P3();
    }

    protected <TT extends Pooled> PooledComponentData<TT> getComponent(ClassType<TT> classType) {
        return engine.getPooledComponent(classType, NO_OP);
    }

    public record P1() implements Pooled {
    }

    public record P2() implements Pooled {
    }

    public record P3() implements Pooled {
    }

    record PackagePrivateComponent() implements Pooled {
    }

    private record PrivateComponent() implements Pooled {
    }

    public record NoDefaultConstructorComponent(int data) implements Pooled {
    }

    public class PackagePrivateConstructorComponent implements Pooled {
        PackagePrivateConstructorComponent() {
        }
    }

    public class PrivateConstructorComponent implements Pooled {
        private PrivateConstructorComponent() {
        }
    }

}
