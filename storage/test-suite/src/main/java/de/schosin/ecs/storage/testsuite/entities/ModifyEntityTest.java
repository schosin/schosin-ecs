package de.schosin.ecs.storage.testsuite.entities;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;

import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import de.schosin.ecs.api.Pooled;
import de.schosin.ecs.api.components.Relation;
import de.schosin.ecs.api.components.Relation.Exclusive;
import de.schosin.ecs.api.components.types.ComponentType;
import de.schosin.ecs.api.components.types.ComponentType.RegularComponentType;
import de.schosin.ecs.storage.api.StorageEngineException;
import de.schosin.ecs.storage.api.entities.ComponentMask;
import de.schosin.ecs.storage.testsuite.AbstractStorageEngineTest;
import de.schosin.ecs.utils.collections.ImmutableBag;

public class ModifyEntityTest {

    private static final String COMPONENTS_SOURCE = "de.schosin.ecs.storage.testsuite.entities.ModifyEntityTest#components";

    @Nested
    class AddOnlyTest extends AddComponentsTest {

        @Override
        public ComponentMask addComponents(int entityId, ImmutableBag<? extends RegularComponentType<?, ?>> componentTypes, Object[] components) {
            return storageEngine.modify(entityId, componentTypes, components, ImmutableBag.emptyBag());
        }

    }

    @Nested
    class RemoveOnlyTest extends RemoveComponentsTest {

        @Override
        public ComponentMask removeComponents(int entityId, ImmutableBag<? extends ComponentType<?, ?>> componentTypes) {
            return storageEngine.modify(entityId, new Object[0], componentTypes);
        }

    }

    @Nested
    class AddAndRemoveTest extends AbstractStorageEngineTest {

        @ParameterizedTest
        @MethodSource(COMPONENTS_SOURCE)
        void testAddAndRemoveSameComponent(Object component) {
            var componentType = ComponentType.detectComponentType(component);

            var entityId = world.createEntity();
            var expectedComponentMask = storageEngine.getComponentMask();

            try {
                // Call
                var componentMask = storageEngine.modify(entityId, new Object[] { component }, ImmutableBag.of(componentType));

                // Verify
                assertThat(componentMask).as("removes component if added and removed at the same time").isSameAs(expectedComponentMask);
            } catch (StorageEngineException ex) {
                // throwing StorageEngineException is fine
            }
        }

        @ParameterizedTest
        @MethodSource(COMPONENTS_SOURCE)
        void testAddAndRemoveSameComponent_PredefinedAddTypes(Object component) {
            var componentType = ComponentType.detectComponentType(component);

            var entityId = world.createEntity();
            var expectedComponentMask = storageEngine.getComponentMask();

            try {
                // Call
                var componentMask = storageEngine.modify(entityId, ImmutableBag.of(componentType), new Object[] { component }, ImmutableBag.of(componentType));

                // Verify
                assertThat(componentMask).as("removes component if added and removed at the same time").isSameAs(expectedComponentMask);
            } catch (StorageEngineException ex) {
                // throwing StorageEngineException is fine
            }
        }

    }

    static Stream<Arguments> components() {
        return Stream.of(
                Arguments.of(Named.of("C1", new C1())),
                Arguments.of(Named.of("P1", new P1())),
                Arguments.of(Named.of("relation(C1, P1)", Relation.create(new C1(), new P1()))),
                Arguments.of(Named.of("relation(E1, P1)", Relation.create(E1.INSTANCE, new P1()))),
                Arguments.of(Named.of("relation(C1, int)", Relation.create(new C1(), 42))),
                Arguments.of(Named.of("relation(E1, int)", Relation.create(E1.INSTANCE, 42))));
    }

    record C1() {
    }

    record C2() {
    }

    record C3() {
    }

    record P1() implements Pooled {
    }

    enum E1 implements Exclusive {
        INSTANCE
    }

}
