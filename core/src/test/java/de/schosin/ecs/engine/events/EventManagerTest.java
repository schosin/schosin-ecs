package de.schosin.ecs.engine.events;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import de.schosin.ecs.engine.AbstractWorldTest;
import de.schosin.ecs.engine.utils.exceptions.EcsEventHandlerException;

class EventManagerTest extends AbstractWorldTest {

    @Nested
    class RegisterEventHandlerTest {

        @Test
        void testClass_TypeDetectedAutomatically() {
            assertThatCode(() -> eventManager.registerEventHandler(new Handler())).doesNotThrowAnyException();
        }

        @Test
        void testClass_WhenUnrelatedGeneric_TypeDetectedAutomatically() {
            assertThatCode(() -> eventManager.registerEventHandler(new GenericHandler<String>())).doesNotThrowAnyException();
        }

        @Test
        void testClass_WhenNonGenericInterfaceExtendsEventHandler_TypeDetectedAutomatically() {
            assertThatCode(() -> eventManager.registerEventHandler(new BaseEventHandlerImpl())).doesNotThrowAnyException();
        }

        @Test
        void testClass_WhenNonGenericAbstractSuperclassExtendsEventHandler_TypeDetectedAutomatically() {
            assertThatCode(() -> eventManager.registerEventHandler(new AbstractBaseEventHandlerImpl())).doesNotThrowAnyException();
        }

        @Test
        void testClass_WhenEventGeneric_DoesNotDetectType() {
            assertThatThrownBy(() -> eventManager.registerEventHandler(new GenericEventHandler<Event1>()))
                    .isInstanceOf(EcsEventHandlerException.class)
                    .hasMessageContaining("Could not determine event type for handler");
        }

        @Test
        void testClass_WhenTypeExtendsAbstractEventGeneric_TypeDetectedAutomatically() {
            assertThatCode(() -> eventManager.registerEventHandler(new ConcreteGenericEventHandler())).doesNotThrowAnyException();
        }

        @Test
        void testMethodReference_DoesNotDetectType() {
            assertThatThrownBy(() -> eventManager.registerEventHandler(RegisterEventHandlerTest::methodReference))
                    .isInstanceOf(EcsEventHandlerException.class)
                    .hasMessageContaining("synthetic handlers");
        }

        @Test
        void testLambda_DoesNotDetectType() {
            assertThatThrownBy(() -> eventManager.registerEventHandler((Event1 event) -> System.out.println(event)))
                    .isInstanceOf(EcsEventHandlerException.class)
                    .hasMessageContaining("synthetic handlers");
        }

        @Test
        void testClass_WhenGenericEventType() {
            assertThatCode(() -> eventManager.registerEventHandler(new GenericEventTypeHandler()))
                    .isInstanceOf(EcsEventHandlerException.class)
                    .hasMessageContaining("Could not determine event type for handler");
        }

        @Test
        void testClass_WhenArrayEventType() {
            assertThatCode(() -> eventManager.registerEventHandler(new ArrayEventTypeHandler())).doesNotThrowAnyException();
        }

        @SuppressWarnings("unused")
        private static void methodReference(Event1 event) {
        }

        static class Handler implements EventHandler<BaseEvent> {
            @Override
            public void handle(BaseEvent event) {
            }
        }

        @SuppressWarnings("unused")
        static class GenericHandler<T> implements EventHandler<BaseEvent> {
            @Override
            public void handle(BaseEvent event) {
            }
        }

        static class GenericEventHandler<T extends BaseEvent> implements EventHandler<T> {
            @Override
            public void handle(T event) {
            }
        }

        static class ConcreteGenericEventHandler extends GenericEventHandler<Event1> implements EventHandler<Event1> {
            @Override
            public void handle(Event1 event) {
            }
        }

        static class BaseEventHandlerImpl implements BaseEventHandler {
            @Override
            public void handle(BaseEvent event) {
            }
        }

        interface BaseEventHandler extends EventHandler<BaseEvent> {
        }

        interface UnrelatedInterface {
        }

        @SuppressWarnings("unused")
        interface UnrelatedGenericInterface<T> {
        }

        static class AbstractBaseEventHandlerImpl extends AbstractBaseEventHandler {
            @Override
            public void handle(BaseEvent event) {
            }
        }

        abstract static class AbstractBaseEventHandler implements UnrelatedInterface, UnrelatedGenericInterface<String>, EventHandler<BaseEvent> {
        }

        class GenericEventTypeHandler implements EventHandler<List<String>> {
            @Override
            public void handle(List<String> event) {
            }
        }

        class ArrayEventTypeHandler implements EventHandler<String[]> {
            @Override
            public void handle(String[] event) {
            }
        }

    }

    @Nested
    @TestInstance(Lifecycle.PER_CLASS)
    class EventRoutingTest {

        final Set<Class<?>> events = new HashSet<>();

        @AfterEach
        void clearEvents() {
            this.events.clear();
        }

        @Test
        void testCustomEvent() {
            // Setup
            var handled = new ArrayList<String>();
            eventManager.registerEventHandler(String.class, handled::add);

            // Call
            eventManager.dispatchEvent("first event");
            eventManager.dispatchEvent("second event");

            // Verify
            assertThat(handled).containsExactly("first event", "second event");
        }

        @Test
        void testCustomEvent_WhenRegisteredAfterFirstDispatch() {
            // Setup
            eventManager.dispatchEvent("before register 1");
            eventManager.dispatchEvent("before register 2");

            var handled = new ArrayList<String>();
            eventManager.registerEventHandler(String.class, handled::add);

            // Call
            eventManager.dispatchEvent("first event");
            eventManager.dispatchEvent("second event");

            // Verify
            assertThat(handled).containsExactly("first event", "second event");
        }

        @Test
        void testCustomEvent_WhenHandlerForSupertype() {
            // Setup
            var handled = new ArrayList<Object>();
            eventManager.registerEventHandler(Object.class, handled::add);

            // Call
            var arrayEvent = new String[] { "third", "fourth" };

            eventManager.dispatchEvent("first event");
            eventManager.dispatchEvent("second event");
            eventManager.dispatchEvent(arrayEvent);

            // Verify
            assertThat(handled).containsExactly("first event", "second event", arrayEvent);
        }

        @Test
        void testMultipleHandlers() {
            // Setup
            var handled1 = new ArrayList<String>();
            eventManager.registerEventHandler(String.class, handled1::add);

            var handled2 = new ArrayList<Object>();
            eventManager.registerEventHandler(Object.class, handled2::add);

            // Call
            eventManager.dispatchEvent("first event");
            eventManager.dispatchEvent("second event");
            eventManager.dispatchEvent(handled1);

            // Verify
            assertThat(handled1).containsExactly("first event", "second event");
            assertThat(handled2).containsExactly("first event", "second event", handled1);

        }

        @ParameterizedTest
        @MethodSource("getBaseEventHandlers")
        @SuppressWarnings({ "unchecked", "rawtypes" })
        void testBaseEventHandlers(Class<?> eventType, EventHandler<?> handler, Set<Class<?>> expectedEvents) {
            // Setup
            eventManager.registerEventHandler((Class) eventType, handler);

            // Call
            eventManager.dispatchEvent(Event1.INSTANCE);
            eventManager.dispatchEvent(Event2.INSTANCE);

            // Verify
            assertThat(this.events).hasSameSizeAs(expectedEvents);
            assertThat(this.events).as("is expected").allSatisfy(event -> assertThat(expectedEvents).anySatisfy(expected -> assertThat(expected).isAssignableFrom(event)));
        }

        @ParameterizedTest
        @MethodSource("getBaseEventHandlers")
        @SuppressWarnings({ "unchecked", "rawtypes" })
        void testBaseEventHandlers_WhenRegisteredAfterFirstDispatch(Class<?> eventType, EventHandler<?> handler, Set<Class<?>> expectedEvents) {
            // Setup
            eventManager.dispatchEvent(Event1.INSTANCE);
            eventManager.dispatchEvent(Event1.INSTANCE);

            eventManager.dispatchEvent(Event2.INSTANCE);
            eventManager.dispatchEvent(Event2.INSTANCE);

            eventManager.registerEventHandler((Class) eventType, handler);

            // Call
            eventManager.dispatchEvent(Event1.INSTANCE);
            eventManager.dispatchEvent(Event2.INSTANCE);

            // Verify
            assertThat(this.events).hasSameSizeAs(expectedEvents);
            assertThat(this.events).as("is expected").allSatisfy(event -> assertThat(expectedEvents).anySatisfy(expected -> assertThat(expected).isAssignableFrom(event)));
        }

        Stream<Arguments> getBaseEventHandlers() {
            return Stream.of(
                    handler("BaseEventHandler", BaseEvent.class, new BaseEventHandler(), Event1.class, Event2.class),
                    handler("Event1Handler", Event1.class, new Event1Handler(), Event1.class),
                    handler("Event2Handler", Event2.class, new Event2Handler(), Event2.class),
                    handler("handleBaseEvent", BaseEvent.class, this::handleBaseEvent, Event1.class, Event2.class),
                    handler("handleEvent1", Event1.class, this::handleEvent1, Event1.class),
                    handler("handleEvent2", Event2.class, this::handleEvent2, Event2.class)

            );

        }

        private static <T> Arguments handler(String name, Class<?> eventType, EventHandler<T> handler, Class<?>... expectedEvents) {
            return Arguments.of(Named.of(eventType.getSimpleName(), eventType), Named.of(name, handler), Set.of(expectedEvents));
        }

        private void handleBaseEvent(BaseEvent event) {
            events.add(event.getClass());
        }

        private void handleEvent1(Event1 event) {
            events.add(event.getClass());
        }

        private void handleEvent2(Event2 event) {
            events.add(event.getClass());
        }

        class BaseEventHandler implements EventHandler<BaseEvent> {
            @Override
            public void handle(BaseEvent event) {
                events.add(event.getClass());
            }
        }

        class Event1Handler implements EventHandler<Event1> {
            @Override
            public void handle(Event1 event) {
                events.add(event.getClass());
            }
        }

        class Event2Handler implements EventHandler<Event2> {
            @Override
            public void handle(Event2 event) {
                events.add(event.getClass());
            }
        }

    }

    interface BaseEvent {
    }

    enum Event1 implements BaseEvent {
        INSTANCE
    }

    enum Event2 implements BaseEvent {
        INSTANCE
    }

}
