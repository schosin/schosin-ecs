package de.schosin.ecs.engine.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.assertj.core.api.SoftAssertions;

import de.schosin.ecs.engine.components.Component;
import de.schosin.ecs.engine.components.ComponentManager;
import de.schosin.ecs.engine.components.ComponentMask;
import de.schosin.ecs.engine.entities.EntityManager;
import de.schosin.ecs.engine.events.EventManager;
import de.schosin.ecs.engine.events.builtin.EntitiesEvent.EntitiesInsertedEvent;
import de.schosin.ecs.engine.events.builtin.EntityEvent.EntityInsertedEvent;
import de.schosin.ecs.engine.events.builtin.EntityEvent.EntityRemovedEvent;
import de.schosin.ecs.engine.events.builtin.EntityEvent.EntityUpdatedEvent;

/**
 * Abstract class providing assertions for testing engine functionality.
 * 
 * <p>
 * Should not be extended directly, use AbstractEcsWorld from test module instead.
 * </p>
 */
public abstract class AbstractEngineTest {

    protected ComponentManager componentManager;
    protected EntityManager entityManager;
    protected EventManager eventManager;

    protected final void initializeEngineTest(ComponentManager componentManager, EntityManager entityManager, EventManager eventManager) {
        this.componentManager = componentManager;
        this.entityManager = entityManager;
        this.eventManager = eventManager;
    }

    protected <T> T getComponent(int entityId, Class<T> clazz) {
        return componentManager.getComponent(clazz).getComponent(entityId);
    }

    protected <T> T verifyHasComponent(int entityId, Class<T> clazz) {
        var component = getComponent(entityId, clazz);
        assertThat(component).as("has %s", clazz.getSimpleName()).isNotNull();

        return component;
    }

    protected void verifyHasComponents(int entityId, Class<?>... classes) {
        for (var clazz : classes) {
            verifyHasComponent(entityId, clazz);
        }
    }

    protected void verifyDoesNotHaveComponent(int entityId, Class<?> clazz) {
        var components = componentManager.getComponent(clazz);
        assertThat(components.hasComponent(entityId)).as("does not have %s", clazz.getSimpleName()).isFalse();
    }

    protected void verifyDoesNotHaveComponents(int entityId, Class<?>... classes) {
        for (var clazz : classes) {
            verifyDoesNotHaveComponent(entityId, clazz);
        }
    }

    protected void verifyComponentMaskHasComponents(int entityId, Class<?>... classes) {
        var componentMask = entityManager.getComponentMask(entityId);

        for (var clazz : classes) {
            var component = componentManager.getComponent(clazz);
            assertThat(componentMask.getComponents()).as("has %s", clazz.getSimpleName()).contains(component);
        }
    }

    protected void verifyComponentMaskDoesNotHaveComponents(int entityId, Class<?>... classes) {
        var componentMask = entityManager.getComponentMask(entityId);

        for (var clazz : classes) {
            var component = componentManager.getComponent(clazz);
            assertThat(componentMask.getComponents()).as("does not have %s", clazz.getSimpleName()).doesNotContain(component);
        }
    }

    protected void verify(Consumer<Verify> consumer) {
        var verify = createVerify();
        consumer.accept(verify);

        verify.verify();
    }

    protected Verify createVerify() {
        return new VerifyImpl(componentManager, eventManager);
    }

    public interface Verify extends AutoCloseable {

        Verify expectInserted(Class<?>... components);

        Verify expectNoMoreInserted();

        default Verify expectUpdated(Class<?>... components) {
            return expectUpdated(-1, components);
        }

        Verify expectUpdated(int entityId, Class<?>... components);

        Verify expectNoMoreUpdated();

        Verify expectRemoved(int entityId);

        Verify expectNoMoreRemoved();

        void verify();

    }

    private record VerifyImpl(SoftAssertions softly, ComponentManager componentManager,
            List<Inserted> inserted, AtomicBoolean noMoreInserted, List<Updated> unexpectedInserted,
            List<Updated> updated, AtomicBoolean noMoreUpdated, List<Updated> unexpectedUpdated,
            List<Removed> removed, AtomicBoolean noMoreRemoved, List<Updated> unexpectedRemoved)
            implements Verify {

        private VerifyImpl(ComponentManager componentManager, EventManager eventManager) {
            this(new SoftAssertions(), componentManager,
                    new ArrayList<>(), new AtomicBoolean(false), new ArrayList<>(),
                    new ArrayList<>(), new AtomicBoolean(false), new ArrayList<>(),
                    new ArrayList<>(), new AtomicBoolean(false), new ArrayList<>());

            eventManager.registerEventHandler(EntityInsertedEvent.class, event -> handleInserted(event.entityId(), event.componentMask()));
            eventManager.registerEventHandler(EntitiesInsertedEvent.class, event -> handleInserted(event.entityIds(), event.componentMask()));
            eventManager.registerEventHandler(EntityUpdatedEvent.class, event -> handleUpdated(event.entityId(), event.componentMask()));
            eventManager.registerEventHandler(EntityRemovedEvent.class, event -> handleRemoved(event.entityId(), event.componentMask()));
        }

        @Override
        public Verify expectInserted(Class<?>... classes) {
            var components = Arrays.stream(classes).map(componentManager::getComponent).collect(Collectors.<Component<?>>toSet());

            this.inserted.add(new Inserted(components));
            return this;
        }

        @Override
        public Verify expectNoMoreInserted() {
            noMoreInserted.set(true);
            return this;
        }

        @Override
        public Verify expectUpdated(int entityId, Class<?>... classes) {
            var components = Arrays.stream(classes).map(componentManager::getComponent).collect(Collectors.<Component<?>>toSet());

            this.updated.add(new Updated(entityId, components));
            return this;
        }

        @Override
        public Verify expectNoMoreUpdated() {
            noMoreUpdated.set(true);
            return this;
        }

        @Override
        public Verify expectRemoved(int entityId) {
            this.removed.add(new Removed(entityId));
            return this;
        }

        @Override
        public Verify expectNoMoreRemoved() {
            noMoreRemoved.set(true);
            return this;
        }

        private void handleInserted(int[] entityIds, ComponentMask componentMask) {
            for (var entityId : entityIds) {
                handleInserted(entityId, componentMask);
            }
        }

        private void handleInserted(int entityId, ComponentMask componentMask) {
            var mask = componentMask.getMask();
            expected: for (var iter = inserted.iterator(); iter.hasNext();) {
                var expected = iter.next();

                for (var expectedComponent : expected.components) {
                    if (!mask.get(expectedComponent.id())) {
                        continue expected;
                    }
                }

                iter.remove();
                return;
            }

            unexpectedInserted.add(new Updated(entityId, set(componentMask)));
        }

        private void handleUpdated(int entityId, ComponentMask componentMask) {
            for (var iter = updated.iterator(); iter.hasNext();) {
                var expected = iter.next();

                if (expected.entityId != -1 && expected.entityId != entityId) {
                    continue;
                }

                iter.remove();

                var present = new HashSet<>(Set.of(componentMask.getComponents()));
                var missing = new ArrayList<String>();

                for (var expectedComponent : expected.components) {
                    if (present.remove(expectedComponent)) {
                        continue;
                    }

                    missing.add(expectedComponent.display());
                }

                if (!missing.isEmpty()) {
                    softly.assertThat(missing).as("Expected entity %d to have %d components, but some were missing.".formatted(entityId, expected.components.size())).isEmpty();
                }

                if (!present.isEmpty()) {
                    var display = present.stream().map(Component::display).toList();
                    softly.assertThat(display).as("Expected entity %d to have %d components, but some were unexpected.".formatted(entityId, expected.components.size())).isEmpty();
                }

                return;
            }

            unexpectedUpdated.add(new Updated(entityId, set(componentMask)));
        }

        private void handleRemoved(int entityId, ComponentMask componentMask) {
            for (var iter = removed.iterator(); iter.hasNext();) {
                var expected = iter.next();

                if (expected.entityId != entityId) {
                    continue;
                }

                iter.remove();
                return;
            }

            unexpectedRemoved.add(new Updated(entityId, set(componentMask)));
        }

        @Override
        public void verify() {
            try {
                close();
            } catch (Exception ex) {
                fail("Verify failed due to exception", ex);
            }
        }

        @Override
        public void close() throws Exception {
            for (var expected : inserted) {
                softly.fail("Expected entity to be inserted: %s".formatted(components(expected.components)));
            }
            for (var expected : updated) {
                softly.fail("Expected entity %d to be updated: %s".formatted(expected.entityId, components(expected.components)));
            }
            for (var expected : removed) {
                softly.fail("Expected entity %d to be removed".formatted(expected.entityId));
            }

            if (noMoreInserted.get()) {
                for (var unexpected : unexpectedInserted) {
                    softly.fail("Expected no more inserted, but got entity %d: %s".formatted(unexpected.entityId, components(unexpected.components)));
                }
            }
            if (noMoreUpdated.get()) {
                for (var unexpected : unexpectedUpdated) {
                    softly.fail("Expected no more updated, but got entity %d: %s".formatted(unexpected.entityId, components(unexpected.components)));
                }
            }
            if (noMoreRemoved.get()) {
                for (var unexpected : unexpectedRemoved) {
                    softly.fail("Expected no more removed, but got entity %d: %s".formatted(unexpected.entityId, components(unexpected.components)));
                }
            }

            softly.assertAll();
        }

        private static Set<Component<?>> set(ComponentMask componentMask) {
            return Arrays.stream(componentMask.getComponents()).collect(Collectors.toSet());
        }

        private static String components(Set<Component<?>> components) {
            if (components.isEmpty()) {
                return "<no components>";
            }

            return components.stream().map(Component::display).collect(Collectors.joining(", "));
        }

        private record Inserted(Set<Component<?>> components) {
        }

        private record Updated(int entityId, Set<Component<?>> components) {
        }

        private record Removed(int entityId) {
        }

    }

}
