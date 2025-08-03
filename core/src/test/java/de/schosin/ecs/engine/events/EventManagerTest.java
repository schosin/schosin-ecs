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
import de.schosin.ecs.engine.events.builtin.EntityEvent;
import de.schosin.ecs.engine.events.builtin.EntityEvent.EntityInsertedEvent;
import de.schosin.ecs.engine.events.builtin.EntityEvent.EntityRemovedEvent;
import de.schosin.ecs.engine.events.builtin.EntityEvent.EntityUpdatedEvent;
import de.schosin.ecs.engine.events.builtin.Event;
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
            assertThatCode(() -> eventManager.registerEventHandler(new EntityEventHandlerImpl())).doesNotThrowAnyException();
        }

        @Test
        void testClass_WhenNonGenericAbstractSuperclassExtendsEventHandler_TypeDetectedAutomatically() {
            assertThatCode(() -> eventManager.registerEventHandler(new AbstractEntityEventHandlerImpl())).doesNotThrowAnyException();
        }

        @Test
        void testClass_WhenEventGeneric_DoesNotDetectType() {
            assertThatThrownBy(() -> eventManager.registerEventHandler(new GenericEventHandler<EntityInsertedEvent>()))
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
            assertThatThrownBy(() -> eventManager.registerEventHandler((EntityInsertedEvent event) -> System.out.println(event)))
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
        private static void methodReference(EntityInsertedEvent event) {
        }

        static class Handler implements EventHandler<EntityEvent> {
            @Override
            public void handle(EntityEvent event) {
            }
        }

        @SuppressWarnings("unused")
        static class GenericHandler<T> implements EventHandler<EntityEvent> {
            @Override
            public void handle(EntityEvent event) {
            }
        }

        static class GenericEventHandler<T extends EntityEvent> implements EventHandler<T> {
            @Override
            public void handle(T event) {
            }
        }

        static class ConcreteGenericEventHandler extends GenericEventHandler<EntityInsertedEvent> implements EventHandler<EntityInsertedEvent> {
            @Override
            public void handle(EntityInsertedEvent event) {
            }
        }

        static class EntityEventHandlerImpl implements EntityEventHandler {
            @Override
            public void handle(EntityEvent event) {
            }
        }

        interface EntityEventHandler extends EventHandler<EntityEvent> {
        }

        interface UnrelatedInterface {
        }

        @SuppressWarnings("unused")
        interface UnrelatedGenericInterface<T> {
        }

        static class AbstractEntityEventHandlerImpl extends AbstractEntityEventHandler {
            @Override
            public void handle(EntityEvent event) {
            }
        }

        abstract static class AbstractEntityEventHandler implements UnrelatedInterface, UnrelatedGenericInterface<String>, EventHandler<EntityEvent> {
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
        @MethodSource("getEntityEventHandlers")
        @SuppressWarnings({ "unchecked", "rawtypes" })
        void testEntityEventHandlers(Class<?> eventType, EventHandler<?> handler, Set<Class<?>> expectedEvents) {
            // Setup
            eventManager.registerEventHandler((Class) eventType, handler);

            // Call
            eventManager.dispatchEvent(EntityInsertedEvent.get().with(0, null));
            eventManager.dispatchEvent(EntityUpdatedEvent.get().with(0, null, null));
            eventManager.dispatchEvent(EntityRemovedEvent.get().with(0, null));

            // Verify
            assertThat(this.events).hasSameSizeAs(expectedEvents);
            assertThat(this.events).as("is expected").allSatisfy(event -> assertThat(expectedEvents).anySatisfy(expected -> assertThat(expected).isAssignableFrom(event)));
        }

        @ParameterizedTest
        @MethodSource("getEntityEventHandlers")
        @SuppressWarnings({ "unchecked", "rawtypes" })
        void testEntityEventHandlers_WhenRegisteredAfterFirstDispatch(Class<?> eventType, EventHandler<?> handler, Set<Class<?>> expectedEvents) {
            // Setup
            eventManager.dispatchEvent(EntityInsertedEvent.get().with(0, null));
            eventManager.dispatchEvent(EntityInsertedEvent.get().with(0, null));

            eventManager.dispatchEvent(EntityUpdatedEvent.get().with(0, null, null));
            eventManager.dispatchEvent(EntityUpdatedEvent.get().with(0, null, null));

            eventManager.dispatchEvent(EntityRemovedEvent.get().with(0, null));
            eventManager.dispatchEvent(EntityRemovedEvent.get().with(0, null));

            eventManager.registerEventHandler((Class) eventType, handler);

            // Call
            eventManager.dispatchEvent(EntityInsertedEvent.get().with(0, null));
            eventManager.dispatchEvent(EntityUpdatedEvent.get().with(0, null, null));
            eventManager.dispatchEvent(EntityRemovedEvent.get().with(0, null));

            // Verify
            assertThat(this.events).hasSameSizeAs(expectedEvents);
            assertThat(this.events).as("is expected").allSatisfy(event -> assertThat(expectedEvents).anySatisfy(expected -> assertThat(expected).isAssignableFrom(event)));
        }

        Stream<Arguments> getEntityEventHandlers() {
            return Stream.of(
                    handler("AllEventHandler", Event.class, new AllEventHandler(), EntityInsertedEvent.class, EntityUpdatedEvent.class, EntityRemovedEvent.class),
                    handler("EntityEventHandler", EntityEvent.class, new EntityEventHandler(), EntityInsertedEvent.class, EntityUpdatedEvent.class, EntityRemovedEvent.class),
                    handler("EntityInsertedEventHandler", EntityInsertedEvent.class, new EntityInsertedEventHandler(), EntityInsertedEvent.class),
                    handler("EntityUpdatedEventHandler", EntityUpdatedEvent.class, new EntityUpdatedEventHandler(), EntityUpdatedEvent.class),
                    handler("EntityRemovedEventHandler", EntityRemovedEvent.class, new EntityRemovedEventHandler(), EntityRemovedEvent.class),
                    handler("handleEvent", Event.class, this::handleEvent, EntityInsertedEvent.class, EntityUpdatedEvent.class, EntityRemovedEvent.class),
                    handler("handleEntityEvent", EntityEvent.class, this::handleEntityEvent, EntityInsertedEvent.class, EntityUpdatedEvent.class, EntityRemovedEvent.class),
                    handler("handleEntityInsertedEvent", EntityInsertedEvent.class, this::handleEntityInsertedEvent, EntityInsertedEvent.class),
                    handler("handleEntityUpdatedEvent", EntityUpdatedEvent.class, this::handleEntityUpdatedEvent, EntityUpdatedEvent.class),
                    handler("handleEntityRemovedEvent", EntityRemovedEvent.class, this::handleEntityRemovedEvent, EntityRemovedEvent.class)

            );

        }

        private static <T> Arguments handler(String name, Class<?> eventType, EventHandler<T> handler, Class<?>... expectedEvents) {
            return Arguments.of(Named.of(eventType.getSimpleName(), eventType), Named.of(name, handler), Set.of(expectedEvents));
        }

        private void handleEvent(Event event) {
            events.add(event.getClass());
        }

        private void handleEntityEvent(EntityEvent event) {
            events.add(event.getClass());
        }

        private void handleEntityInsertedEvent(EntityInsertedEvent event) {
            events.add(event.getClass());
        }

        private void handleEntityUpdatedEvent(EntityUpdatedEvent event) {
            events.add(event.getClass());
        }

        private void handleEntityRemovedEvent(EntityRemovedEvent event) {
            events.add(event.getClass());
        }

        class AllEventHandler implements EventHandler<Event> {
            @Override
            public void handle(Event event) {
                events.add(event.getClass());
            }
        }

        class EntityEventHandler implements EventHandler<EntityEvent> {
            @Override
            public void handle(EntityEvent event) {
                events.add(event.getClass());
            }
        }

        class EntityInsertedEventHandler implements EventHandler<EntityInsertedEvent> {
            @Override
            public void handle(EntityInsertedEvent event) {
                events.add(event.getClass());
            }
        }

        class EntityUpdatedEventHandler implements EventHandler<EntityUpdatedEvent> {
            @Override
            public void handle(EntityUpdatedEvent event) {
                events.add(event.getClass());
            }
        }

        class EntityRemovedEventHandler implements EventHandler<EntityRemovedEvent> {
            @Override
            public void handle(EntityRemovedEvent event) {
                events.add(event.getClass());
            }
        }

    }

}
