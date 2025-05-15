package de.schosin.ecs.storage.testsuite.components;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assumptions.assumeThat;

import java.util.ArrayList;
import java.util.stream.Stream;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import de.schosin.ecs.api.World;
import de.schosin.ecs.api.components.ComponentType.ClassType;
import de.schosin.ecs.api.components.ComponentType.RegularComponentType;
import de.schosin.ecs.engine.events.builtin.ComponentAddedEvent.RegularComponentAddedEvent.ClassComponentAddedEvent;
import de.schosin.ecs.engine.events.builtin.Event;
import de.schosin.ecs.storage.api.StorageEngine;
import de.schosin.ecs.storage.api.components.Component;
import de.schosin.ecs.storage.api.components.Component.ComponentData;
import de.schosin.ecs.storage.testsuite.AbstractStorageEngineTest;

public abstract class CommonComponentTest<T, T1 extends T, T2 extends T, T3 extends T> extends AbstractStorageEngineTest {

    protected abstract T getInstance(ClassType<? extends T> classType);

    protected abstract T1 getInstance1();

    protected abstract T2 getInstance2();

    protected abstract T3 getInstance3();

    protected abstract <TT extends T> Component<TT> getComponent(ClassType<TT> classType);

    @Nested
    class IdTest {

        @Test
        void testDifferentIds() {
            var id1 = getComponent(type1()).id();
            var id2 = getComponent(type2()).id();
            var id3 = getComponent(type3()).id();

            assertThat(id1).as("component.id() for different types must be unique").isNotIn(id2, id3);
            assertThat(id2).as("component.id() for different types must be unique").isNotEqualTo(id3);
        }

    }

    @Nested
    class TypeTest extends AbstractTypeTest {

        @ParameterizedTest
        @MethodSource(TYPES)
        void testTypeEqualToArgument(ClassType<? extends T> type) {
            assertThat(getComponent(type).type()).as("component.type() must be equal to argument").isEqualTo(type);
        }

    }

    @Nested
    class DisplayTest extends AbstractTypeTest {

        @ParameterizedTest
        @MethodSource(TYPES)
        void testDisplayIncludesSimpleName(ClassType<? extends T> type) {
            assertThat(getComponent(type).display()).as("component.display() must contain clazz.getSimpleName()").contains(type.clazz().getSimpleName());
        }

        @ParameterizedTest
        @MethodSource(TYPES)
        void testDisplayIncludesId(ClassType<? extends T> type) {
            var component = getComponent(type);

            assertThat(component.display()).as("component.display() must contain the id").contains(Integer.toString(component.id()));
        }

    }

    @Nested
    class HasComponentTest extends AbstractTypeTest {

        @ParameterizedTest
        @MethodSource(TYPES)
        void testNotAddedBefore(ClassType<? extends T> type) {
            var entityId = world.createEntity();

            assertThat(getComponent(type).hasComponent(entityId)).as("hasComponent returns false if not added before").isFalse();
        }

        @ParameterizedTest
        @MethodSource(TYPES)
        void testHasComponent(ClassType<? extends T> type) {
            var entityId = world.createEntity(getInstance(type));

            assertThat(getComponent(type).hasComponent(entityId)).as("hasComponent returns true if entity created with component").isTrue();
        }

        @ParameterizedTest
        @MethodSource(TYPES)
        @SuppressWarnings({ "unchecked", "rawtypes" })
        void testHasComponent_WhenEntityModified(ClassType<? extends T> type) {
            var component = getComponent(type);

            var entityId = world.createEntity();
            ((Component) component).addComponent(entityId, getInstance(type));

            assertThat(component.hasComponent(entityId)).as("hasComponent returns true if component added to existing entity and world process").isTrue();
        }

        @ParameterizedTest
        @MethodSource(TYPES)
        void testNoOutOfBounds(ClassType<? extends T> type) {
            var entitySize = bagManager.getEntitySize();

            // Implementation note: Implementations should either use BagManager#createEntityBag, do a range check, or auto-grow the underlying data structure
            assertThat(getComponent(type).hasComponent(entitySize - 1)).as("hasComponent does not throw any index related exception if within entitySize").isFalse();

            for (int i = 0, s = bagManager.getEntitySize() + 10; i < s; i++) {
                world.createEntity();
            }

            assertThat(getComponent(type).hasComponent(entitySize + 9)).as("hasComponent does not throw any index related exception when entitySize expanded").isFalse();
        }

    }

    @Nested
    class GetComponentTest extends AbstractTypeTest {

        @ParameterizedTest
        @MethodSource(TYPES)
        void testNotAddedBefore(ClassType<? extends T> type) {
            var entityId = world.createEntity();

            assertThat(getComponent(type).getComponent(entityId)).as("getComponent returns false if not added before").isNull();
        }

        @ParameterizedTest
        @MethodSource(TYPES)
        void testGetComponent(ClassType<? extends T> type) {
            var instance = getInstance(type);
            var entityId = world.createEntity(instance);

            assertThat(getComponent(type).getComponent(entityId)).as("getComponent returns same instance if entity created with component").isSameAs(instance);
        }

        @ParameterizedTest
        @MethodSource(TYPES)
        @SuppressWarnings({ "unchecked", "rawtypes" })
        void testHasComponent_WhenEntityModified(ClassType<? extends T> type) {
            var component = getComponent(type);

            var instance = getInstance(type);
            var entityId = world.createEntity();
            ((Component) component).addComponent(entityId, instance);

            assertThat(component.getComponent(entityId)).as("getComponent returns same instance if component added to existing entity and world process").isSameAs(instance);
        }

        @ParameterizedTest
        @MethodSource(TYPES)
        void testNoOutOfBounds(ClassType<? extends T> type) {
            var entitySize = bagManager.getEntitySize();

            // Implementation note: Implementations should either use BagManager#createEntityBag, do a range check, or auto-grow the underlying data structure
            assertThat(getComponent(type).getComponent(entitySize - 1)).as("getComponent does not throw any index related exception if within entitySize").isNull();

            for (int i = 0, s = bagManager.getEntitySize() + 10; i < s; i++) {
                world.createEntity();
            }

            assertThat(getComponent(type).getComponent(entitySize + 9)).as("getComponent does not throw any index related exception when entitySize expanded").isNull();
        }

    }

    @Nested
    class RemovalTest {

        @Test
        void testRemoveComponent() {
            var component = getComponent(type1());

            var entityId = world.createEntity(getInstance1());

            component.removeComponent(entityId);

            assertThat(component.hasComponent(entityId)).as("hasComponent returns false when removeComponent called").isFalse();
            assertThat(component.getComponent(entityId)).as("getComponent returns null when removeComponent called").isNull();
        }

        @Test
        void testRemoveComponent_DoesNothingIfComponentNotPresent() {
            var component = getComponent(type1());

            var entityId = world.createEntity();

            assertThatCode(() -> component.removeComponent(entityId)).doesNotThrowAnyException();

            assertThat(component.hasComponent(entityId)).as("hasComponent returns false when removeComponent called").isFalse();
            assertThat(component.getComponent(entityId)).as("getComponent returns null when removeComponent called").isNull();
        }

    }

    @Nested
    class EqualsHashCodeTest extends AbstractTypeTest {

        @ParameterizedTest
        @MethodSource(TYPES)
        void testHashCode_EqualsId(ClassType<? extends T> type) {
            var component = getComponent(type);

            assertThat(component.hashCode()).as("has hashCode equal to id").isEqualTo(component.id());
        }

        @ParameterizedTest
        @MethodSource(TYPES)
        void testEquals_EqualsItself(ClassType<? extends T> type) {
            var component = getComponent(type);

            assertThat(component).as("component equals itself").isEqualTo(component);
        }

        @ParameterizedTest
        @MethodSource(TYPES)
        void testEquals_EqualsOtherInstanceWithSameId(ClassType<? extends T> type) {
            var component = getComponent(type);

            var otherWorld = World.builder().build();
            var otherEngine = otherWorld.getSingleton(StorageEngine.class);

            var otherComponent = otherEngine.getComponent(type, NO_OP);

            assumeThat(otherComponent.id()).as("assumption that component ids are in strict ascending order").isSameAs(component.id());

            assertThat(component).as("component equals other instance of same class with same id").isEqualTo(otherComponent);
        }

        @Test
        void testEquals_DoesNotEqualInstanceWithDifferentId() {
            var component = getComponent(type1());
            var otherComponent = getComponent(type2());

            assertThat(component).as("component does not equal other instance of same class with different id").isNotEqualTo(otherComponent);
        }

        @ParameterizedTest
        @MethodSource(TYPES)
        void testEquals_DoesNotEqualOtherClassWithSameId(ClassType<? extends T> type) {
            var component = getComponent(type);

            var otherComponent = new TestComponent<>(component.id());

            assertThat(component).as("component equals other instance with same id").isNotEqualTo(otherComponent);
        }

    }

    @Nested
    class ComponentAddedEventTest extends AbstractTypeTest {

        private record EventData(RegularComponentType<?> type, Component<?> component) {
            private EventData(Event event) {
                this(assertThat(event).as("Must pass same ClassType as argument").asInstanceOf(InstanceOfAssertFactories.type(ClassComponentAddedEvent.class)).actual());
            }

            private EventData(ClassComponentAddedEvent event) {
                this(event.type(), event.component());
            }
        }

        @ParameterizedTest
        @MethodSource(TYPES)
        void testClassComponentAddedEvent_WhenGetComponentsCalled(ClassType<? extends T> type) {
            var events = new ArrayList<EventData>();
            eventManager.registerEventHandler(Event.class, event -> events.add(new EventData(event)));

            var component = getComponent(type);

            assertThat(events).as("getComponents must call CoreWorld#dispatchComponentAddedEvent when component created").hasSize(1);

            var event = events.get(0);
            assertThat(event.type()).as("Must pass same ClassType as argument").isSameAs(type);
            assertThat(event.component()).as("Must pass created Components instance as argument").isSameAs(component);
        }

        @ParameterizedTest
        @MethodSource(TYPES)
        void testClassComponentAddedEvent_WhenGetComponentsCalled_DispatchesOnlyOnCreation(ClassType<? extends T> type) {
            var events = new ArrayList<EventData>();
            eventManager.registerEventHandler(Event.class, event -> events.add(new EventData(event)));

            getComponent(type);
            getComponent(type);

            assertThat(events).as("getComponents must call CoreWorld#dispatchComponentAddedEvent only when component created").hasSize(1);
        }

    }

    @SuppressWarnings("unchecked")
    protected ClassType<T1> type1() {
        return new ClassType<>((Class<T1>) getInstance1().getClass());
    }

    @SuppressWarnings("unchecked")
    protected ClassType<T2> type2() {
        return new ClassType<>((Class<T2>) getInstance2().getClass());
    }

    @SuppressWarnings("unchecked")
    protected ClassType<T3> type3() {
        return new ClassType<>((Class<T3>) getInstance3().getClass());
    }

    @TestInstance(Lifecycle.PER_CLASS)
    abstract class AbstractTypeTest {

        interface ComponentSupplier<T> {
            T getInstance();
        }

        static final String TYPES = "types";

        Stream<Arguments> types() {
            var type1 = type1();
            var type2 = type2();
            var type3 = type3();

            return Stream.of(
                    Arguments.of(Named.of(type1.clazz().getSimpleName(), type1)),
                    Arguments.of(Named.of(type2.clazz().getSimpleName(), type2)),
                    Arguments.of(Named.of(type3.clazz().getSimpleName(), type3)));
        }

    }

}

class TestComponent<T> implements ComponentData<T> {

    private final int id;

    TestComponent(int id) {
        this.id = id;
    }

    @Override
    public int id() {
        return id;
    }

    @Override
    public ClassType<T> type() {
        return null;
    }

    @Override
    public String display() {
        return null;
    }

    @Override
    public boolean hasComponent(int entityId) {
        return false;
    }

    @Override
    public T getComponent(int entityId) {
        return null;
    }

    @Override
    public void addComponentUnsafe(int id, T component) {
    }

    @Override
    public void removeComponent(int entityId) {
    }

    @Override
    public Class<T> clazz() {
        return null;
    }

}