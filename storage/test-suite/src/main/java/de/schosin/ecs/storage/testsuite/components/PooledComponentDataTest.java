package de.schosin.ecs.storage.testsuite.components;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.IdentityHashMap;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import de.schosin.ecs.api.Pooled;
import de.schosin.ecs.api.components.types.ClassType;
import de.schosin.ecs.storage.api.components.Component.PooledComponentData;
import de.schosin.ecs.storage.testsuite.components.PooledComponentDataTest.P1;
import de.schosin.ecs.storage.testsuite.components.PooledComponentDataTest.P2;
import de.schosin.ecs.storage.testsuite.components.PooledComponentDataTest.P3;
import de.schosin.ecs.utils.collections.ImmutableBag;

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
        void testGetInstance_InstanceNotYetReusedWhenOnlyRemoved(ClassType<? extends Pooled> classType) {
            var component = getComponent(classType);

            var instance = component.getInstance();
            var entityId = world.createEntity(instance);

            storageEngine.remove(entityId, ImmutableBag.of(classType));

            assertThat(component.getInstance()).as("getInstance returns new instance if not flushed").isNotSameAs(instance);
        }

        @ParameterizedTest
        @MethodSource(TYPES)
        void testGetInstance_InstanceReusedWhenRemovedAndFlushed(ClassType<? extends Pooled> classType) {
            var component = getComponent(classType);

            var instance = component.getInstance();
            var entityId = world.createEntity(instance);

            storageEngine.remove(entityId, ImmutableBag.of(classType));
            storageEngine.flushChanges(entityId);

            assertThat(component.getInstance()).as("getInstance reuses removed instances").isSameAs(instance);
        }

        @Test
        void testGetInstance_InstanceNotYetResetWhenComponentOnlyRemoved() {
            var type = component(PooledClass.class);
            var component = getComponent(type);

            var instance = component.getInstance().init(42);
            var entityId = world.createEntity(instance);

            storageEngine.remove(entityId, ImmutableBag.of(type));

            assertThat(instance.data).as("component must be reset when removed from entity").isEqualTo(42);
        }

        @Test
        void testGetInstance_InstanceResetWhenComponentRemovedAndFlushed() {
            var type = component(PooledClass.class);
            var component = getComponent(type);

            var instance = component.getInstance().init(42);
            var entityId = world.createEntity(instance);

            storageEngine.remove(entityId, ImmutableBag.of(type));
            storageEngine.flushChanges(entityId);

            assertThat(instance.data).as("component must be reset when removed from entity").isEqualTo(-1);
        }

        @Test
        void testGetInstance_InstanceResetWhenEntityDeleted() {
            var type = component(PooledClass.class);
            var component = getComponent(type);

            var instance = component.getInstance().init(42);
            var entityId = world.createEntity(instance);

            storageEngine.delete(entityId);

            assertThat(instance.data).as("component must be reset when removed from entity").isEqualTo(-1);
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
        return engine.getPooledComponent(classType);
    }

    public record P1() implements Pooled {
    }

    public record P2() implements Pooled {
    }

    public record P3() implements Pooled {
    }

    public static class PooledClass implements Pooled {
        private int data = -1;

        public PooledClass init(int data) {
            this.data = data;
            return this;
        }

        @Override
        public void reset() {
            this.data = -1;
        }
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
