package de.schosin.ecs.storage.testsuite.entities;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.stream.Stream;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import de.schosin.ecs.api.Pooled;
import de.schosin.ecs.api.components.Relation;
import de.schosin.ecs.api.components.Relation.Exclusive;
import de.schosin.ecs.api.components.mappers.ComponentMapper;
import de.schosin.ecs.api.components.mappers.ComponentRelations.ComponentRelationMapper;
import de.schosin.ecs.api.components.mappers.ComponentRelations.ExclusiveComponentRelationMapper;
import de.schosin.ecs.api.components.mappers.EntityRelations.EntityRelationMapper;
import de.schosin.ecs.api.components.mappers.EntityRelations.ExclusiveEntityRelationMapper;
import de.schosin.ecs.api.components.types.ComponentType;
import de.schosin.ecs.api.components.types.ComponentType.RegularComponentType;
import de.schosin.ecs.storage.api.StorageEngineException;
import de.schosin.ecs.storage.api.entities.ComponentMask;
import de.schosin.ecs.storage.testsuite.AbstractStorageEngineTest;
import de.schosin.ecs.utils.collections.ImmutableBag;

public class AddComponentsTest extends AbstractStorageEngineTest {

    private static final String COMPONENTS_SOURCE = "de.schosin.ecs.storage.testsuite.entities.AddComponentsTest#components";

    public ComponentMask addComponents(int entityId, ImmutableBag<? extends RegularComponentType<?, ?>> componentTypes, Object[] components) {
        return storageEngine.add(entityId, componentTypes, components);
    }

    @ParameterizedTest
    @MethodSource(COMPONENTS_SOURCE)
    void testAddToUnknownEntity(Object component) {
        var componentType = ComponentType.detectComponentType(component);

        assertThatThrownBy(() -> addComponents(42, ImmutableBag.of(componentType), new Object[] { component }))
                .isInstanceOf(StorageEngineException.class)
                .hasMessageContainingAll("Cannot add components to entity 42", "not present in storage");
    }

    @Nested
    class PredefinedComponentTypesTest {

        @SuppressWarnings("rawtypes")
        @ParameterizedTest
        @MethodSource(COMPONENTS_SOURCE)
        void testAdd(Object component) {
            var componentType = ComponentType.detectComponentType(component);
            var entityId = world.createEntity();

            // Call
            var componentMask = addComponents(entityId, ImmutableBag.of(componentType), new Object[] { component });

            // Verify
            var components = world.getComponents(componentType);

            switch (components) {
                case ComponentMapper mapper -> assertThat(mapper.get(entityId)).as("must store component").isSameAs(component);
                case ComponentRelationMapper mapper -> assertThat(mapper.get(entityId)).asInstanceOf(InstanceOfAssertFactories.ITERABLE).as("must store component").containsExactly(component);
                case ExclusiveComponentRelationMapper mapper -> assertThat(mapper.get(entityId)).as("must store component").isSameAs(component);
                case EntityRelationMapper mapper -> assertThat(mapper.get(entityId)).asInstanceOf(InstanceOfAssertFactories.ITERABLE).as("must store component").containsExactly(component);
                case ExclusiveEntityRelationMapper mapper -> assertThat(mapper.get(entityId)).as("must store component").isSameAs(component);
            }

            assertThat(componentMask.getComponentTypes()).as("component mask must only contain type of passed component").containsExactly(componentType);
        }

        @SuppressWarnings("rawtypes")
        @ParameterizedTest
        @MethodSource(COMPONENTS_SOURCE)
        void testAddMultiple(Object component) {
            var componentType = ComponentType.detectComponentType(component);
            var entityId = world.createEntity();

            // Call
            var component2 = new C2();
            var componentMask = addComponents(entityId, ImmutableBag.of(componentType, component(C2.class)), new Object[] { component, component2 });

            // Verify
            var components = world.getComponents(componentType);

            switch (components) {
                case ComponentMapper mapper -> assertThat(mapper.get(entityId)).as("must store component").isSameAs(component);
                case ComponentRelationMapper mapper -> assertThat(mapper.get(entityId)).asInstanceOf(InstanceOfAssertFactories.ITERABLE).as("must store component").containsExactly(component);
                case ExclusiveComponentRelationMapper mapper -> assertThat(mapper.get(entityId)).as("must store component").isSameAs(component);
                case EntityRelationMapper mapper -> assertThat(mapper.get(entityId)).asInstanceOf(InstanceOfAssertFactories.ITERABLE).as("must store component").containsExactly(component);
                case ExclusiveEntityRelationMapper mapper -> assertThat(mapper.get(entityId)).as("must store component").isSameAs(component);
            }

            assertThat(world.getComponents(component(C2.class)).get(entityId)).as("must store component").isSameAs(component2);

            assertThat(componentMask.getComponentTypes()).as("component mask must only contain type of passed component").containsExactlyInAnyOrder(componentType, component(C2.class));
        }

        @ParameterizedTest
        @MethodSource(COMPONENTS_SOURCE)
        void testAddMultiple_MismatchingTypes(Object component) {
            var componentType = ComponentType.detectComponentType(component);
            var entityId = world.createEntity();

            // Call
            var component2 = new C2();
            assertThatThrownBy(() -> addComponents(entityId, ImmutableBag.of(componentType, component(C3.class)), new Object[] { component, component2 }))
                    .isInstanceOf(StorageEngineException.class)
                    .hasMessageContainingAll("entity %d".formatted(entityId), "Expected component type '%s' at index 1".formatted(component(C3.class)));
        }

        @ParameterizedTest
        @MethodSource(COMPONENTS_SOURCE)
        void testAddMultiple_MisorderedTypes(Object component) {
            var componentType = ComponentType.detectComponentType(component);
            var entityId = world.createEntity();

            // Call
            var component1 = new C3();
            assertThatThrownBy(() -> addComponents(entityId, ImmutableBag.of(componentType, component(C3.class)), new Object[] { component1, component }))
                    .isInstanceOf(StorageEngineException.class)
                    .hasMessageContainingAll("entity %d".formatted(entityId),
                            "Expected component type '%s' at index 0".formatted(componentType),
                            "Expected component type '%s' at index 1".formatted(component(C3.class)));
        }

        @ParameterizedTest
        @MethodSource(COMPONENTS_SOURCE)
        void testAddMultiple_MissingTypes(Object component) {
            var componentType = ComponentType.detectComponentType(component);
            var entityId = world.createEntity();

            // Call
            var component2 = new C2();
            assertThatThrownBy(() -> addComponents(entityId, ImmutableBag.of(componentType), new Object[] { component, component2 }))
                    .isInstanceOf(StorageEngineException.class)
                    .hasMessageContainingAll("entity %d".formatted(entityId), "Unexpected component '%s' at index 1".formatted(component2));
        }

        @ParameterizedTest
        @MethodSource(COMPONENTS_SOURCE)
        void testAddMultiple_UnexpectedTypes(Object component) {
            var componentType = ComponentType.detectComponentType(component);
            var entityId = world.createEntity();

            // Call
            var component2 = new C2();
            assertThatThrownBy(() -> addComponents(entityId, ImmutableBag.of(componentType, component(C2.class), component(C3.class)), new Object[] { component, component2 }))
                    .isInstanceOf(StorageEngineException.class)
                    .hasMessageContainingAll("entity %d".formatted(entityId), "The following component types were unexpected", component(C3.class).toString());
        }

    }

    @Nested
    class AddToComponentMaskTest {

        @ParameterizedTest
        @MethodSource(COMPONENTS_SOURCE)
        void testMatchesAddOperation(Object component) {
            var componentType = ComponentType.detectComponentType(component);

            var entityId = world.createEntity();
            var componentMask = storageEngine.getComponentMaskForEntity(entityId);
            var updatedComponentMask = storageEngine.add(entityId, new Object[] { component });

            // Call
            var result = storageEngine.addToComponentMask(componentMask, ImmutableBag.of(componentType));

            // Verify
            assertThat(result).as("addToComponentMask must match storageEngine.add").isSameAs(updatedComponentMask);
        }

        @ParameterizedTest
        @MethodSource(COMPONENTS_SOURCE)
        void testMatchesAddOperation_MultipleTypes(Object component) {
            var componentType = ComponentType.detectComponentType(component);

            var entityId = world.createEntity();
            var componentMask = storageEngine.getComponentMaskForEntity(entityId);
            var updatedComponentMask = storageEngine.add(entityId, new Object[] { component, new C2() });

            // Call
            var result = storageEngine.addToComponentMask(componentMask, ImmutableBag.of(componentType, component(C2.class)));

            // Verify
            assertThat(result).as("addToComponentMask must match storageEngine.add").isSameAs(updatedComponentMask);
        }

        @ParameterizedTest
        @MethodSource(COMPONENTS_SOURCE)
        void testAddPresentType(Object component) {
            var componentType = ComponentType.detectComponentType(component);
            var componentMask = storageEngine.getComponentMask(componentType);

            // Call
            var result = storageEngine.addToComponentMask(componentMask, ImmutableBag.of(componentType));

            // Verify
            assertThat(result).as("addToComponentMask returns same instance if type already present").isSameAs(componentMask);
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
