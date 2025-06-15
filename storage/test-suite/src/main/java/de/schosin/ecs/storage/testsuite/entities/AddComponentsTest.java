package de.schosin.ecs.storage.testsuite.entities;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.stream.Stream;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import de.schosin.ecs.api.Pooled;
import de.schosin.ecs.api.components.Relation;
import de.schosin.ecs.api.components.Relation.Exclusive;
import de.schosin.ecs.api.components.Relations;
import de.schosin.ecs.api.components.mappers.ComponentMapper;
import de.schosin.ecs.api.components.mappers.ComponentRelationMappers.ComponentRelationMapper;
import de.schosin.ecs.api.components.mappers.ComponentRelationMappers.ExclusiveComponentRelationMapper;
import de.schosin.ecs.api.components.mappers.EntityRelationMappers.EntityRelationMapper;
import de.schosin.ecs.api.components.mappers.EntityRelationMappers.ExclusiveEntityRelationMapper;
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

        @SuppressWarnings("rawtypes")
        @ParameterizedTest
        @MethodSource(COMPONENTS_SOURCE)
        void testAddToMultipleEntities_FlushChanges(Object component) {
            var emptyComponentMask = engine.getComponentMask();

            var componentType = ComponentType.detectComponentType(component);
            var entity1 = world.createEntity();
            var entity2 = world.createEntity();

            // Call
            var component2 = new C2();
            addComponents(entity1, ImmutableBag.of(componentType), new Object[] { component });
            var componentMask2 = addComponents(entity2, ImmutableBag.of(component(C2.class)), new Object[] { component2 });

            storageEngine.flushChanges(entity1);

            // Verify
            var components = world.getComponents(componentType);

            switch (components) {
                case ComponentMapper mapper -> assertThat(mapper.get(entity1)).as("must store component").isSameAs(component);
                case ComponentRelationMapper mapper -> assertThat(mapper.get(entity1)).asInstanceOf(InstanceOfAssertFactories.ITERABLE).as("must store component").containsExactly(component);
                case ExclusiveComponentRelationMapper mapper -> assertThat(mapper.get(entity1)).as("must store component").isSameAs(component);
                case EntityRelationMapper mapper -> assertThat(mapper.get(entity1)).asInstanceOf(InstanceOfAssertFactories.ITERABLE).as("must store component").containsExactly(component);
                case ExclusiveEntityRelationMapper mapper -> assertThat(mapper.get(entity1)).as("must store component").isSameAs(component);
            }

            assertThat(storageEngine.getComponentMaskForEntity(entity2)).as("flushing changes must not affect pending changes for other entities").isSameAs(emptyComponentMask);
            assertThat(storageEngine.getPendingComponentMask(entity2)).as("flushing changes must not affect pending changes for other entities").isSameAs(componentMask2);
            assertThat(getComponent(entity2, C2.class)).as("flushing changes must not affect pending changes for other entities").isSameAs(component2);

            // Flush second entity
            storageEngine.flushChanges(entity2);

            assertThat(storageEngine.getComponentMaskForEntity(entity2)).as("flushing changes must not affect pending changes for other entities").isSameAs(componentMask2);
            assertThat(storageEngine.getPendingComponentMask(entity2)).as("flushing changes must not affect pending changes for other entities").isNull();
            assertThat(getComponent(entity2, C2.class)).as("flushing changes must not affect pending changes for other entities").isSameAs(component2);
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

        @Test
        void testComponentRelations() {
            var relation1 = Relation.create(new C1(11), new C2(12));
            var relation2 = Relation.create(new C1(12), new C2(22));

            var relations = Relations.create(relation1, relation2);

            var entityId = world.createEntity();

            // Call
            addComponents(entityId, ImmutableBag.of(relation(C1.class, C2.class)), new Object[] { relations });

            // Verify
            assertThat(storageEngine.getComponent(relation(C1.class, C2.class)).getComponent(1)).as("Must store relations").containsExactlyInAnyOrder(relation1, relation2);

            assertThat(relations).as("must return relations back to the pool").isEmpty();
            assertThat(Relations.create(Relation.create(new C1(13), new C2(13)))).as("must return relations back to the pool").isSameAs(relations);
        }

        @Test
        void testComponentRelations_EqualTarget() {
            var relation1 = Relation.create(new C1(11), new C2(12));
            var relation2 = Relation.create(new C1(12), new C2(12));
            var relation3 = Relation.create(new C1(13), new C2(33));

            var relations = Relations.create(relation2, relation3);

            var entityId = world.createEntity(relation1);

            // Call
            addComponents(entityId, ImmutableBag.of(relation(C1.class, C2.class)), new Object[] { relations });

            // Verify
            assertThat(storageEngine.getComponent(relation(C1.class, C2.class)).getComponent(1)).as("Must store relations").containsExactlyInAnyOrder(relation2, relation3);

            assertThat(relation1.relationship()).as("must return replaced relation back to pool").isNull();
            assertThat(relation1.target()).as("must return replaced relation back to pool").isNull();
            assertThat(Relation.create(new C1(4), new C2(4))).as("must return replaced relation back to pool").isSameAs(relation1);

            assertThat(relations).as("must return relations back to the pool").isEmpty();
            assertThat(Relations.create(Relation.create(new C1(13), new C2(13)))).as("must return relations back to the pool").isSameAs(relations);
        }

        @Test
        void testEntityRelations() {
            var relation1 = Relation.create(new C1(1), 2);
            var relation2 = Relation.create(new C1(2), 3);

            var relations = Relations.create(relation1, relation2);

            var entityId = world.createEntity();

            // Call
            addComponents(entityId, ImmutableBag.of(relation(C1.class)), new Object[] { relations });

            // Verify
            assertThat(storageEngine.getComponent(relation(C1.class)).getComponent(1)).as("Must store relations").containsExactlyInAnyOrder(relation1, relation2);

            assertThat(relations).as("must return relations back to the pool").isEmpty();
            assertThat(Relations.create(Relation.create(new C1(13), 3))).as("must return relations back to the pool").isSameAs(relations);
        }

        @Test
        void testEntityRelations_EqualTarget() {
            var relation1 = Relation.create(new C1(1), 2);
            var relation2 = Relation.create(new C1(2), 2);
            var relation3 = Relation.create(new C1(13), 3);

            var relations = Relations.create(relation2, relation3);

            var entityId = world.createEntity(relation1);

            // Call
            addComponents(entityId, ImmutableBag.of(relation(C1.class)), new Object[] { relations });

            // Verify
            assertThat(storageEngine.getComponent(relation(C1.class)).getComponent(1)).as("Must store relations").containsExactlyInAnyOrder(relation2, relation3);

            assertThat(relation1.relationship()).as("must return replaced relation back to pool").isNull();
            assertThat(relation1.target()).as("must return replaced relation back to pool").isEqualTo(-1);
            assertThat(Relation.create(new C1(4), 4)).as("must return replaced relation back to pool").isSameAs(relation1);

            assertThat(relations).as("must return relations back to the pool").isEmpty();
            assertThat(Relations.create(Relation.create(new C1(14), 4))).as("must return relations back to the pool").isSameAs(relations);
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

    record C1(int value) {
        public C1() {
            this(0);
        }
    }

    record C2(int value) {
        public C2() {
            this(0);
        }
    }

    record C3() {
    }

    record P1() implements Pooled {
    }

    enum E1 implements Exclusive {
        INSTANCE
    }

}
