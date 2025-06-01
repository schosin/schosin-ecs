package de.schosin.ecs.storage.testsuite.components;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assumptions.assumeThat;

import java.util.ArrayList;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.assertj.core.api.ObjectAssert;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import de.schosin.ecs.api.World;
import de.schosin.ecs.api.components.types.ComponentType.RegularComponentType;
import de.schosin.ecs.engine.events.builtin.ComponentAddedEvent;
import de.schosin.ecs.engine.events.builtin.Event;
import de.schosin.ecs.storage.api.StorageEngine;
import de.schosin.ecs.storage.api.components.Component;
import de.schosin.ecs.storage.testsuite.AbstractStorageEngineTest;

public abstract class CommonComponentTest<T1, R1, T2, R2, T3, R3> extends AbstractStorageEngineTest {

    protected final BiConsumer<ObjectAssert<?>, Object> verifyComponentInstance = verifyComponentInstance();

    protected abstract RegularComponentType<T1, R1> type1();

    protected abstract RegularComponentType<T2, R2> type2();

    protected abstract RegularComponentType<T3, R3> type3();

    protected abstract <T> T getInstance(RegularComponentType<T, ?> type);

    protected BiConsumer<ObjectAssert<?>, Object> verifyComponentInstance() {
        return ObjectAssert::isSameAs;
    }

    @Test
    void verifyTestSetup() {
        var type1 = type1();
        var type2 = type2();
        var type3 = type3();

        assertThat(type1).as("sanity check").isIn(type1).as("sanity check").isIn(type1());
        assertThat(type2).as("sanity check").isIn(type2).as("sanity check").isIn(type2());
        assertThat(type3).as("sanity check").isIn(type3).as("sanity check").isIn(type3());

        assertThat(type1).as("type1 must not be equal to type2 or type3").isNotIn(type2, type3);
        assertThat(type2).as("type2 must not be equal to type1 or type2").isNotIn(type1, type3);
        assertThat(type3).as("type3 must not be equal to type2 or type3").isNotIn(type1, type2);

        assertThat(type1()).as("type1() must return a new instance").isNotSameAs(type1);
        assertThat(type2()).as("type1() must return a new instance").isNotSameAs(type2);
        assertThat(type3()).as("type1() must return a new instance").isNotSameAs(type3);
    }

    @Nested
    class CommonInstanceTest {

        @Test
        void testDifferentTypes_DifferentInstances() {
            var component1 = getComponent(type1());
            var component2 = getComponent(type2());
            var component3 = getComponent(type3());

            assertThat(component1).as("different type must return differnt component").isNotIn(component2, component3);
            assertThat(component2).as("different type must return differnt component").isNotIn(component1, component3);
            assertThat(component3).as("different type must return differnt component").isNotIn(component1, component2);
        }

        @Test
        void testSameType_SameInstance() {
            var type = type1();
            var component = getComponent(type);

            assertThat(getComponent(type)).as("same type argument must return same instance").isSameAs(component);
        }

        @Test
        void testEqualType_SameInstance() {
            var type = type1();
            var component = getComponent(type);

            assertThat(getComponent(type1())).as("equal type argument must return same instance").isSameAs(component);
        }

    }

    @Nested
    class CommonIdTest {

        @Test
        void testDifferentTypes_DifferentIds() {
            var id1 = getComponent(type1()).id();
            var id2 = getComponent(type2()).id();
            var id3 = getComponent(type3()).id();

            assertThat(id1).as("component.id() for different types must be unique").isNotIn(id2, id3);
            assertThat(id2).as("component.id() for different types must be unique").isNotEqualTo(id3);
        }

    }

    @Nested
    class CommonTypeTest extends AbstractTypeTest {

        @ParameterizedTest
        @MethodSource(TYPES)
        void testTypeEqualToArgument(RegularComponentType<?, ?> type) {
            assertThat(getComponent(type).type()).as("component.type() must be equal to argument").isEqualTo(type);
        }

    }

    @Nested
    class CommonHasComponentTest extends AbstractTypeTest {

        @ParameterizedTest
        @MethodSource(TYPES)
        void testNotAddedBefore(RegularComponentType<?, ?> type) {
            var entityId = world.createEntity();

            assertThat(getComponent(type).hasComponent(entityId)).as("hasComponent returns false if not added before").isFalse();
        }

        @ParameterizedTest
        @MethodSource(TYPES)
        void testHasComponent(RegularComponentType<?, ?> type) {
            var entityId = world.createEntity(getInstance(type));

            assertThat(getComponent(type).hasComponent(entityId)).as("hasComponent returns true if entity created with component").isTrue();
        }

        @ParameterizedTest
        @MethodSource(TYPES)
        @SuppressWarnings({ "unchecked", "rawtypes" })
        void testHasComponent_WhenEntityModified(RegularComponentType<?, ?> type) {
            var component = getComponent(type);

            var entityId = world.createEntity();
            ((Component) component).addComponent(entityId, getInstance(type));

            assertThat(component.hasComponent(entityId)).as("hasComponent returns true if component added to existing entity and world process").isTrue();
        }

        @ParameterizedTest
        @MethodSource(TYPES)
        void testNoOutOfBounds(RegularComponentType<?, ?> type) {
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
    class CommonGetComponentTest extends AbstractTypeTest {

        @ParameterizedTest
        @MethodSource(TYPES)
        void testNotAddedBefore(RegularComponentType<?, ?> type) {
            var entityId = world.createEntity();

            assertThat(getComponent(type).getComponent(entityId)).as("getComponent returns false if not added before").isNull();
        }

        @ParameterizedTest
        @MethodSource(TYPES)
        void testGetComponent(RegularComponentType<?, ?> type) {
            var instance = getInstance(type);
            var entityId = world.createEntity(instance);

            verifyComponentInstance.accept(assertThat(getComponent(type).getComponent(entityId)).as("getComponent returns same instance if entity created with component"), instance);
        }

        @ParameterizedTest
        @MethodSource(TYPES)
        @SuppressWarnings({ "unchecked", "rawtypes" })
        void testGetComponent_WhenEntityModified(RegularComponentType<?, ?> type) {
            var component = getComponent(type);

            var instance = getInstance(type);
            var entityId = world.createEntity();
            ((Component) component).addComponent(entityId, instance);

            verifyComponentInstance.accept(assertThat(component.getComponent(entityId)).as("getComponent returns same instance if component added to existing entity and world process"), instance);
        }

        @ParameterizedTest
        @MethodSource(TYPES)
        void testNoOutOfBounds(RegularComponentType<?, ?> type) {
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
    class CommonRemovalTest {

        @Test
        void testRemoveComponent() {
            var component = getComponent(type1());

            var entityId = world.createEntity(getInstance(type1()));

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
    class CommonEqualsHashCodeTest extends AbstractTypeTest {

        @ParameterizedTest
        @MethodSource(TYPES)
        void testHashCode_EqualsId(RegularComponentType<?, ?> type) {
            var component = getComponent(type);

            assertThat(component.hashCode()).as("has hashCode equal to id").isEqualTo(component.id());
        }

        @ParameterizedTest
        @MethodSource(TYPES)
        void testEquals_EqualsItself(RegularComponentType<?, ?> type) {
            var component = getComponent(type);

            assertThat(component).as("component equals itself").isEqualTo(component);
        }

        @ParameterizedTest
        @MethodSource(TYPES)
        void testEquals_EqualsOtherInstanceWithSameId(RegularComponentType<?, ?> type) {
            var component = getComponent(type);

            var otherWorld = World.builder().build();
            var otherEngine = otherWorld.getSingleton(StorageEngine.class);

            var otherComponent = otherEngine.getComponent(type);

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
        void testEquals_DoesNotEqualOtherClassWithSameId(RegularComponentType<?, ?> type) {
            var component = getComponent(type);

            var otherComponent = new TestComponent<>(component.id());

            assertThat(component).as("component equals other instance with same id").isNotEqualTo(otherComponent);
        }

    }

    @Nested
    class CommonComponentAddedEventTest extends AbstractTypeTest {

        private record EventData(RegularComponentType<?, ?> type, Component<?, ?> component) {
            private EventData(Event event) {
                this(assertThat(event).as("Must pass same ClassType as argument").asInstanceOf(InstanceOfAssertFactories.type(ComponentAddedEvent.class)).actual());
            }

            private EventData(ComponentAddedEvent event) {
                this(event.type(), event.component());
            }
        }

        @ParameterizedTest
        @MethodSource(TYPES)
        void testComponentAddedEvent_WhenGetComponentsCalled(RegularComponentType<?, ?> type) {
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
        void testComponentAddedEvent_WhenGetComponentsCalled_DispatchesOnlyOnCreation(RegularComponentType<?, ?> type) {
            var events = new ArrayList<EventData>();
            eventManager.registerEventHandler(Event.class, event -> events.add(new EventData(event)));

            getComponent(type);
            getComponent(type);

            assertThat(events).as("getComponents must call CoreWorld#dispatchComponentAddedEvent only when component created").hasSize(1);
        }

    }

    protected final <T, R> Component<T, R> getComponent(RegularComponentType<T, R> type) {
        return engine.getComponent(type);
    }

    private Stream<Arguments> types() {
        return Stream.of(
                Arguments.of(type1()),
                Arguments.of(type2()),
                Arguments.of(type3()));
    }

    @TestInstance(Lifecycle.PER_CLASS)
    protected abstract class AbstractTypeTest {

        interface ComponentSupplier<T> {
            T getInstance();
        }

        protected static final String TYPES = "types";

        protected Stream<Arguments> types() {
            return CommonComponentTest.this.types();
        }

    }

}
