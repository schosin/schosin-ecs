package de.schosin.ecs.storage.testsuite.entities;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assumptions.assumeThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.ObjIntConsumer;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import de.schosin.ecs.api.components.Relation;
import de.schosin.ecs.api.components.Relation.Exclusive;
import de.schosin.ecs.api.components.Relations;
import de.schosin.ecs.api.components.types.ComponentType;
import de.schosin.ecs.api.components.types.ComponentType.RegularComponentType;
import de.schosin.ecs.storage.api.StorageEngineException;
import de.schosin.ecs.storage.api.entities.Archetype;
import de.schosin.ecs.storage.testsuite.AbstractStorageEngineTest;
import de.schosin.ecs.utils.collections.ImmutableBag;

public class ArchetypeTest extends AbstractStorageEngineTest {

    @Nested
    class CreateEntityTest {

        @Test
        void testEmptyArchetype() {
            var archetype = engine.getArchetype();

            // Call
            archetype.createEntity(42, new Object[0]);

            // Verify
            assertThat(archetype.contains(42)).as("archetype.contains must return true after creation").isTrue();
            assertThat(engine.getArchetypeForEntity(42)).as("getArchetypeForEntity must return same archetype after creation").isSameAs(archetype);
        }

        @Test
        void testEmptyArchetype_NonMatchingComponents() {
            var archetype = engine.getArchetype();

            assertThatThrownBy(() -> archetype.createEntity(42, new Object[] { new C1() }))
                    .as("Empty archetype must throw when components not empty").isInstanceOf(StorageEngineException.class)
                    .as("Empty archetype must throw when components not empty").hasMessage("Expected 0 components, but got 1");
        }

        @Test
        void testExistingEntity() {
            var archetype = engine.getArchetype();

            archetype.createEntity(42, new Object[0]);

            assertThatThrownBy(() -> archetype.createEntity(42, new Object[0]))
                    .isInstanceOf(StorageEngineException.class)
                    .hasMessageContainingAll("entity 42", "already present in storage");
        }

        @Nested
        class SingleComponentArchetypeTest extends AbstractTest {

            @Override
            protected Archetype getArchetype(RegularComponentType<?, ?> componentType) {
                return engine.getArchetype(componentType);
            }

            @Override
            protected Object[] createComponents(Archetype archetype, Object component) {
                return new Object[] { component };
            }

        }

        @Nested
        class MultiComponentArchetypeTest extends AbstractTest {

            @Override
            protected Archetype getArchetype(RegularComponentType<?, ?> componentType) {
                return engine.getArchetype(component(C3.class), componentType);
            }

            @Override
            protected Object[] createComponents(Archetype archetype, Object component) {
                return new Object[] { new C3(), component };
            }

            @Test
            void testMismatchingOrder() {
                var archetype = engine.getArchetype(component(C1.class), component(C2.class));
                var components = new Object[] { new C2(2), new C1(1) };

                assertThatThrownBy(() -> archetype.createEntity(42, components))
                        .as("Archetype must throw when component types are not ordered").isInstanceOf(StorageEngineException.class)
                        .as("Archetype must throw when component types are not ordered").hasMessageContainingAll(
                                "Expected component type '%s'".formatted(component(C1.class)),
                                "was '%s'".formatted(new C2(2)));
            }

        }

        abstract class AbstractTest {

            protected abstract Archetype getArchetype(RegularComponentType<?, ?> componentType);

            protected abstract Object[] createComponents(Archetype archetype, Object component);

            @ParameterizedTest
            @MethodSource("components")
            void testCreateEntity(Object component) {
                var componentType = ComponentType.detectComponentType(component);

                var archetype = getArchetype(componentType);
                var components = createComponents(archetype, component);

                var relations = component instanceof Relations<?> rel
                        ? StreamSupport.stream(rel.spliterator(), false).toList()
                        : null;

                archetype.createEntity(42, components);

                // Verify
                assertThat(archetype.contains(42)).as("archetype.contains must return true after creation").isTrue();
                assertThat(engine.getArchetypeForEntity(42)).as("getArchetypeForEntity must return same archetype after creation").isSameAs(archetype);

                var accessor = archetype.getEntityData().getAccessor(42);
                var componentId = engine.getComponent(componentType).id();

                var result = accessor.getComponent(componentId);
                if (result instanceof Relations<?>) {
                    if (relations != null) {
                        assertThat(result)
                                .as("Accessor return relations with instance after creation").asInstanceOf(InstanceOfAssertFactories.ITERABLE)
                                .as("Accessor return relations with instance after creation").containsExactlyElementsOf(relations);
                    } else {
                        assertThat(result)
                                .as("Accessor return relations with instance after creation").asInstanceOf(InstanceOfAssertFactories.ITERABLE)
                                .as("Accessor return relations with instance after creation").containsExactly(component);
                    }
                } else {
                    assertThat(result).as("Accessor must return same instance after creation").isSameAs(component);
                }
            }

            static Stream<Object> components() {
                var relation1 = Relation.create(new C1(1), new C1(1));
                var relation2 = Relation.create(new C1(1), new C1(2));

                var entityRelation1 = Relation.create(new C1(1), 1);
                var entityRelation2 = Relation.create(new C1(1), 2);

                return Stream.of(
                        new C1(1),

                        relation1,
                        Relations.of(relation1, relation2),
                        Relation.create(E1.INSTANCE, new C1()),

                        entityRelation1,
                        Relations.of(entityRelation1, entityRelation2),
                        Relation.create(E1.INSTANCE, 3));
            }

            @Test
            void testDeleteCreatedEntity() {
                var archetype = getArchetype(component(C1.class));
                var components = createComponents(archetype, new C1(1));

                archetype.createEntity(42, components);
                assertThat(archetype.contains(42)).as("archetype.contains must return true after creation").isTrue();

                assertThatCode(() -> engine.delete(42)).as("Deleting an entity created via Archetype must work").doesNotThrowAnyException();
                assertThat(archetype.contains(42)).as("archetype.contains must return false after deletion").isFalse();
            }

            @Test
            void testEmptyComponents() {
                var archetype = getArchetype(component(C1.class));
                var expected = archetype.getComponentMask().getComponentTypes().getSize();

                assertThatThrownBy(() -> archetype.createEntity(42, new Object[0]))
                        .as("Archetype must throw when components is empty").isInstanceOf(StorageEngineException.class)
                        .as("Archetype must throw when components is empty").hasMessage("Expected %d components, but got 0".formatted(expected));
            }

            @Test
            void testMismatchingSize() {
                var archetype = getArchetype(component(C1.class));
                var expected = archetype.getComponentMask().getComponentTypes().getSize();

                var components = expected == 1 ? new Object[] { new C1(1), new C3() } : new Object[] { new C1(1) };

                assertThatThrownBy(() -> archetype.createEntity(42, components))
                        .as("Archetype must throw when size of components does not match").isInstanceOf(StorageEngineException.class)
                        .as("Archetype must throw when size of components does not match").hasMessage("Expected %d components, but got %d".formatted(expected, components.length));
            }

            @ParameterizedTest(name = "{0} != {1}")
            @MethodSource("mismatchingTypes")
            void testMismatchingTypes(RegularComponentType<?, ?> componentType, Object component) {
                var actualType = ComponentType.detectComponentType(component);
                assertThat(actualType).as("broken test suite").isNotEqualTo(componentType);

                var archetype = getArchetype(componentType);
                var components = createComponents(archetype, component);

                assertThatThrownBy(() -> archetype.createEntity(42, components))
                        .as("Archetype must throw when component types do not match").isInstanceOf(StorageEngineException.class)
                        .as("Archetype must throw when component types do not match").hasMessageContainingAll(
                                "Expected component type '%s'".formatted(componentType),
                                "was '%s'".formatted(component));
            }

            static Stream<Arguments> mismatchingTypes() {
                var relation1 = Relation.create(new C1(), new C1());
                var relation2 = Relation.create(new C2(), new C1());

                var exclusive1 = Relation.create(E1.INSTANCE, new C1());
                var exclusive2 = Relation.create(E2.INSTANCE, new C1());

                var entityRelation1 = Relation.create(new C1(), 1);
                var entityRelation2 = Relation.create(new C2(), 2);

                var exclusiveEntity1 = Relation.create(E1.INSTANCE, 2);
                var exclusiveEntity2 = Relation.create(E2.INSTANCE, 3);

                return Stream.of(
                        Arguments.arguments(component(C1.class), new C2()),
                        Arguments.arguments(component(C1.class), relation1),
                        Arguments.arguments(component(C1.class), exclusive1),
                        Arguments.arguments(component(C1.class), entityRelation1),
                        Arguments.arguments(component(C1.class), exclusiveEntity1),
                        Arguments.arguments(component(C1.class), Relations.of(relation1)),
                        Arguments.arguments(component(C1.class), Relations.of(entityRelation1)),

                        Arguments.arguments(relation(C1.class, C1.class), new C2()),
                        Arguments.arguments(relation(C1.class, C1.class), relation2),
                        Arguments.arguments(relation(C1.class, C1.class), exclusive2),
                        Arguments.arguments(relation(C1.class, C1.class), entityRelation1),
                        Arguments.arguments(relation(C1.class, C1.class), exclusiveEntity2),
                        Arguments.arguments(relation(C1.class, C1.class), Relations.of(relation2)),
                        Arguments.arguments(relation(C1.class, C1.class), Relations.of(entityRelation1)),

                        Arguments.arguments(exclusiveRelation(E1.class, C1.class), new C2()),
                        Arguments.arguments(exclusiveRelation(E1.class, C1.class), relation1),
                        Arguments.arguments(exclusiveRelation(E1.class, C1.class), exclusive2),
                        Arguments.arguments(exclusiveRelation(E1.class, C1.class), entityRelation1),
                        Arguments.arguments(exclusiveRelation(E1.class, C1.class), exclusiveEntity2),
                        Arguments.arguments(exclusiveRelation(E1.class, C1.class), Relations.of(relation1)),
                        Arguments.arguments(exclusiveRelation(E1.class, C1.class), Relations.of(entityRelation1)),

                        Arguments.arguments(relation(C1.class), new C2()),
                        Arguments.arguments(relation(C1.class), relation1),
                        Arguments.arguments(relation(C1.class), exclusive2),
                        Arguments.arguments(relation(C1.class), entityRelation2),
                        Arguments.arguments(relation(C1.class), exclusiveEntity2),
                        Arguments.arguments(relation(C1.class), Relations.of(relation1)),
                        Arguments.arguments(relation(C1.class), Relations.of(entityRelation2)),

                        Arguments.arguments(exclusiveRelation(E1.class), new C2()),
                        Arguments.arguments(exclusiveRelation(E1.class), relation1),
                        Arguments.arguments(exclusiveRelation(E1.class), exclusive2),
                        Arguments.arguments(exclusiveRelation(E1.class), entityRelation1),
                        Arguments.arguments(exclusiveRelation(E1.class), exclusiveEntity2),
                        Arguments.arguments(exclusiveRelation(E1.class), Relations.of(relation1)),
                        Arguments.arguments(exclusiveRelation(E1.class), Relations.of(entityRelation1))

                );
            }

        }

    }

    @Nested
    class CreateEntitiesTest {

        @Nested
        class SingleComponentArchetypeTest extends AbstractTest {

            @Override
            protected Archetype getArchetype(RegularComponentType<?, ?> componentType) {
                return engine.getArchetype(componentType);
            }

            @Override
            protected Object[] createComponents(Archetype archetype, int i, Object component) {
                return new Object[] { component };
            }

        }

        @Nested
        class MultiComponentArchetypeTest extends AbstractTest {

            @Override
            protected Archetype getArchetype(RegularComponentType<?, ?> componentType) {
                return engine.getArchetype(component(C3.class), componentType);
            }

            @Override
            protected Object[] createComponents(Archetype archetype, int i, Object component) {
                return new Object[] { new C3(i), component };
            }

        }

        abstract class AbstractTest {

            protected abstract Archetype getArchetype(RegularComponentType<?, ?> componentType);

            protected abstract Object[] createComponents(Archetype archetype, int i, Object component);

            @ParameterizedTest
            @MethodSource("testCreateEntitiesArguments")
            void testCreateEntities(int count, IntFunction<Object> componentFunction) {
                var componentType = ComponentType.detectComponentType(componentFunction.apply(0));
                var archetype = getArchetype(componentType);

                var componentArrays = IntStream.range(1, count + 2)
                        .mapToObj(i -> createComponents(archetype, i, componentFunction.apply(i)))
                        .toArray(Object[][]::new);

                var ids = new AtomicInteger(1);
                var provider = new ComponentProvider(componentArrays);

                // Call
                archetype.createEntities(count, ids::getAndIncrement, provider);

                // Verify
                assertThat(ids.get()).as("createEntities must request ids matching the count parameter").isEqualTo(count + 1);

                assertThat(archetype.getCount()).as("createEntities must create as many entities as the count parameter").isEqualTo(count);

                for (int i = 0; i < count; i++) {
                    var entityId = 1 + i;
                    var component = componentFunction.apply(entityId);

                    var relations = component instanceof Relations<?> rel
                            ? StreamSupport.stream(rel.spliterator(), false).toList()
                            : null;

                    assertThat(archetype.contains(entityId)).as("archetype.contains must return true after creation").isTrue();
                    assertThat(engine.getArchetypeForEntity(entityId)).as("getArchetypeForEntity must return same archetype after creation").isSameAs(archetype);

                    var accessor = archetype.getEntityData().getAccessor(entityId);

                    var componentId = engine.getComponent(componentType).id();

                    var result = accessor.getComponent(componentId);
                    if (result instanceof Relations<?>) {
                        if (relations != null) {
                            assertThat(result)
                                    .as("Accessor return relations with instance after creation").asInstanceOf(InstanceOfAssertFactories.ITERABLE)
                                    .as("Accessor return relations with instance after creation").containsExactlyElementsOf(relations);
                        } else {
                            assertThat(result)
                                    .as("Accessor return relations with instance after creation").asInstanceOf(InstanceOfAssertFactories.ITERABLE)
                                    .as("Accessor return relations with instance after creation").containsExactly(component);
                        }
                    } else {
                        assertThat(result).as("Accessor must return same instance after creation").isEqualTo(component);
                    }
                }
            }

            static Stream<Arguments> testCreateEntitiesArguments() {
                var components = List.<IntFunction<Object>>of(
                        i -> new C1(i),

                        i -> Relation.create(new C1(i), new C1(i)),
                        i -> Relations.of(Relation.create(new C1(i), new C1(i)), Relation.create(new C1(i * 10), new C1(i * 10))),
                        i -> Relation.create(E1.INSTANCE, new C1(i)),

                        i -> Relation.create(new C1(i), i),
                        i -> Relations.of(Relation.create(new C1(i), i), Relation.create(new C1(i * 10), i * 10)),
                        i -> Relation.create(E1.INSTANCE, i));

                return IntStream.of(10, 15, 42, 512)
                        .mapToObj(i -> components.stream()
                                .map(args -> Arguments.argumentSet("%d: %s".formatted(i, args.apply(0)), i, args)))
                        .flatMap(Function.identity());
            }

            @Test
            void testDeleteCreatedEntities() {
                var archetype = getArchetype(component(C1.class));

                var count = 5;
                var componentArrays = IntStream.range(1, count + 1)
                        .mapToObj(i -> createComponents(archetype, i, new C1(i)))
                        .toArray(Object[][]::new);

                var ids = new AtomicInteger(1);
                var provider = new ComponentProvider(componentArrays);

                // Call
                archetype.createEntities(count, ids::getAndIncrement, provider);

                // Verify
                for (int i = 1; i <= count; i++) {
                    var entityId = i;

                    assertThat(archetype.contains(entityId)).as("archetype.contains must return false after creation").isTrue();

                    assertThatCode(() -> engine.delete(entityId)).as("Deleting an entity created via Archetype must work").doesNotThrowAnyException();
                    assertThat(archetype.contains(entityId)).as("archetype.contains must return false after deletion").isFalse();
                }
            }

            @Test
            void testEmptyComponents() {
                var archetype = getArchetype(component(C1.class));

                var count = 5;
                var componentArrays = IntStream.range(1, count + 1)
                        .mapToObj(i -> i == count
                                ? new Object[0]
                                : createComponents(archetype, i, new C1(i)))
                        .toArray(Object[][]::new);

                var ids = new AtomicInteger(1);
                var provider = new ComponentProvider(componentArrays);

                assertThatThrownBy(() -> archetype.createEntities(count, ids::getAndIncrement, provider), "Archetype must throw when components is empty");
            }

            @Test
            void testTooFewComponents() {
                var archetype = getArchetype(component(C1.class));

                var expected = archetype.getComponentMask().getComponentTypes().getSize();
                assumeThat(expected).isGreaterThan(1);

                var count = 5;
                var componentArrays = IntStream.range(1, count + 1)
                        .mapToObj(i -> i == count
                                ? new Object[] { new C1(1) }
                                : createComponents(archetype, i, new C1(i)))
                        .toArray(Object[][]::new);

                var ids = new AtomicInteger(1);
                var provider = new ComponentProvider(componentArrays);

                assertThatThrownBy(() -> archetype.createEntities(count, ids::getAndIncrement, provider), "Archetype must throw when size of components lower than expected");
            }

            @ParameterizedTest(name = "{0} != {1}")
            @MethodSource("mismatchingTypes")
            void testMismatchingTypes(RegularComponentType<?, ?> componentType, Object component) {
                var archetype = getArchetype(componentType);

                var expected = archetype.getComponentMask().getComponentTypes().getSize();
                assumeThat(expected).isGreaterThan(1);

                var count = 5;
                var componentArrays = IntStream.range(1, count + 1)
                        .mapToObj(i -> createComponents(archetype, i, component))
                        .toArray(Object[][]::new);

                var ids = new AtomicInteger(1);
                var provider = new ComponentProvider(componentArrays);

                assertThatThrownBy(() -> archetype.createEntities(count, ids::getAndIncrement, provider))
                        .as("Archetype must throw when component types do not match").isInstanceOf(StorageEngineException.class)
                        .as("Archetype must throw when component types do not match").hasMessageContainingAll(
                                "Expected component type '%s'".formatted(componentType),
                                "was '%s'".formatted(component));
            }

            static Stream<Arguments> mismatchingTypes() {
                return CreateEntityTest.AbstractTest.mismatchingTypes();
            }

            private class ComponentProvider implements ObjIntConsumer<Object[]> {

                private final List<Object[]> list;

                private ComponentProvider(Object[]... components) {
                    this.list = Arrays.asList(components);
                }

                @Override
                public void accept(Object[] components, int idx) {
                    var data = list.get(idx);

                    for (int i = 0, s = components.length; i < s; i++) {
                        components[i] = data[i];
                    }
                }
            }

        }

    }

    @Nested
    class EntityCreationTest {

        @Test
        void testEmptyArchetype() {
            var archetype = engine.getArchetype();

            var entity = world.createEntity();
            var entity1 = world.createEntity(new C1());

            assertThat(archetype.contains(entity)).as("empty archetype must contain entity with no components").isTrue();
            assertThat(archetype.contains(entity1)).as("empty archetype must not contain entity with components").isFalse();

            assertThat(archetype.getCount()).as("getCount returns number of matching entities").isEqualTo(1);
            assertThat(archetype.getEntities().iterator()).toIterable().as("getEntities contains only matching entities").containsExactly(entity);
        }

        @Test
        void testArchetype() {
            var archetype = engine.getArchetype(component(C1.class));

            var entity = world.createEntity();
            var entity1 = world.createEntity(new C1());
            var entity2 = world.createEntity(new C2());
            var entity12 = world.createEntity(new C1(), new C2());

            assertThat(archetype.contains(entity)).as("non-empty archetype must not contain entity with no components").isFalse();
            assertThat(archetype.contains(entity1)).as("archetype must contain entity with exactly matching components").isTrue();
            assertThat(archetype.contains(entity2)).as("archetype must not contain entity with different components").isFalse();
            assertThat(archetype.contains(entity12)).as("archetype must not contain entity with overlapping components").isFalse();

            assertThat(archetype.getCount()).as("getCount returns number of matching entities").isEqualTo(1);
            assertThat(archetype.getEntities().iterator()).toIterable().as("getEntities contains only matching entities").containsExactly(entity1);
        }

        @Test
        void testEntityComponentOrder() {
            var archetype = engine.getArchetype(component(C1.class), component(C2.class));

            var entity12 = world.createEntity(new C1(), new C2());
            var entity21 = world.createEntity(new C2(), new C1());

            assertThat(archetype.contains(entity12)).as("archetype must not contain matching entity regardless of order of components").isTrue();
            assertThat(archetype.contains(entity21)).as("archetype must not contain matching entity regardless of order of components").isTrue();

            assertThat(archetype.getCount()).as("getCount returns number of matching entities").isEqualTo(2);
            assertThat(archetype.getEntities().iterator()).toIterable().as("getEntities contains only matching entities").containsExactlyInAnyOrder(entity12, entity21);
        }

    }

    @Nested
    class EntityUpdatesTest {

        @Test
        void testFlushUnknownEntity() {
            assertThatThrownBy(() -> engine.flushChanges(42), "must throw when flushing an unknown entity")
                    .as("must throw when flushing an unknown entity").isInstanceOf(StorageEngineException.class)
                    .message()
                    .as("must throw when flushing an unchanged entity").containsIgnoringCase("entity 42")
                    .as("must throw when flushing an unknown entity").containsIgnoringCase("not present in storage");
        }

        @Test
        void testFlushUnchangedEntity() {
            var archetype = engine.getArchetype();
            archetype.createEntity(42, new Object[0]);

            assertThatThrownBy(() -> engine.flushChanges(42), "must throw when flushing an unchanged entity")
                    .as("must throw when flushing an unchanged entity").isInstanceOf(StorageEngineException.class)
                    .message()
                    .as("must throw when flushing an unchanged entity").containsIgnoringCase("entity 42")
                    .as("must throw when flushing an unchanged entity").containsIgnoringCase("no pending changes");
        }

        @Test
        void testAddComponent() {
            var archetype = engine.getArchetype();
            var archetype1 = engine.getArchetype(component(C1.class));

            var entity1 = world.createEntity();
            var entity2 = world.createEntity();

            assertThat(archetype.contains(entity1)).as("archetype contains entity with matching components").isTrue();
            assertThat(archetype.contains(entity2)).as("archetype contains entity with matching components").isTrue();
            assertThat(archetype.getCount()).as("getCount returns number of matching entities").isEqualTo(2);
            assertThat(archetype.getEntities().iterator()).toIterable().as("getEntities contains only matching entities").containsExactlyInAnyOrder(entity1, entity2);

            assertThat(archetype1.contains(entity1)).as("archetype does not contain entity with non-matching components").isFalse();
            assertThat(archetype1.contains(entity2)).as("archetype does not contain entity with non-matching components").isFalse();
            assertThat(archetype1.getCount()).as("getCount returns number of matching entities").isEqualTo(0);
            assertThat(archetype1.getEntities().iterator()).toIterable().as("getEntities contains only matching entities").isEmpty();

            // Call
            engine.add(entity1, new Object[] { new C1() });

            // Verify
            assertThat(archetype.contains(entity1)).as("archetype contains entity with matching components").isTrue();
            assertThat(archetype.contains(entity2)).as("archetype contains entity with matching components").isTrue();
            assertThat(archetype.getCount()).as("getCount returns number of matching entities").isEqualTo(2);
            assertThat(archetype.getEntities().iterator()).toIterable().as("getEntities contains only matching entities").containsExactlyInAnyOrder(entity1, entity2);

            assertThat(archetype1.contains(entity1)).as("archetype does not contain entity with non-matching components").isFalse();
            assertThat(archetype1.contains(entity2)).as("archetype does not contain entity with non-matching components").isFalse();
            assertThat(archetype1.getCount()).as("getCount returns number of matching entities").isEqualTo(0);
            assertThat(archetype1.getEntities().iterator()).toIterable().as("getEntities contains only matching entities").isEmpty();
        }

        @Test
        void testAddComponent_FlushedChanges() {
            var archetype = engine.getArchetype();
            var archetype1 = engine.getArchetype(component(C1.class));

            var entity1 = world.createEntity();
            var entity2 = world.createEntity();

            assertThat(archetype.contains(entity1)).as("archetype contains entity with matching components").isTrue();
            assertThat(archetype.contains(entity2)).as("archetype contains entity with matching components").isTrue();
            assertThat(archetype.getCount()).as("getCount returns number of matching entities").isEqualTo(2);
            assertThat(archetype.getEntities().iterator()).toIterable().as("getEntities contains only matching entities").containsExactlyInAnyOrder(entity1, entity2);

            assertThat(archetype1.contains(entity1)).as("archetype does not contain entity with non-matching components").isFalse();
            assertThat(archetype1.contains(entity2)).as("archetype does not contain entity with non-matching components").isFalse();
            assertThat(archetype1.getCount()).as("getCount returns number of matching entities").isEqualTo(0);
            assertThat(archetype1.getEntities().iterator()).toIterable().as("getEntities contains only matching entities").isEmpty();

            // Call
            engine.add(entity1, new Object[] { new C1() });
            engine.flushChanges(entity1);

            // Verify
            assertThat(archetype.contains(entity1)).as("archetype does not contain entity with non-matching components").isFalse();
            assertThat(archetype.contains(entity2)).as("archetype contains entity with matching components").isTrue();
            assertThat(archetype.getCount()).as("getCount returns number of matching entities").isEqualTo(1);
            assertThat(archetype.getEntities().iterator()).toIterable().as("getEntities contains only matching entities").containsExactly(entity2);

            assertThat(archetype1.contains(entity1)).as("archetype contains entity with matching components").isTrue();
            assertThat(archetype1.contains(entity2)).as("archetype does not contain entity with non-matching components").isFalse();
            assertThat(archetype1.getCount()).as("getCount returns number of matching entities").isEqualTo(1);
            assertThat(archetype1.getEntities().iterator()).toIterable().as("getEntities contains only matching entities").containsExactlyInAnyOrder(entity1);
        }

        @Test
        void testRemoveComponent() {
            var archetype = engine.getArchetype();
            var archetype1 = engine.getArchetype(component(C1.class));

            var entity1 = world.createEntity(new C1());
            var entity2 = world.createEntity(new C1());

            assertThat(archetype.contains(entity1)).as("archetype does not contain entity with non-matching components").isFalse();
            assertThat(archetype.contains(entity2)).as("archetype does not contain entity with non-matching components").isFalse();
            assertThat(archetype.getCount()).as("getCount returns number of matching entities").isEqualTo(0);
            assertThat(archetype.getEntities().iterator()).toIterable().as("getEntities contains only matching entities").isEmpty();

            assertThat(archetype1.contains(entity1)).as("archetype contains entity with matching components").isTrue();
            assertThat(archetype1.contains(entity2)).as("archetype contains entity with matching components").isTrue();
            assertThat(archetype1.getCount()).as("getCount returns number of matching entities").isEqualTo(2);
            assertThat(archetype1.getEntities().iterator()).toIterable().as("getEntities contains only matching entities").containsExactlyInAnyOrder(entity1, entity2);

            // Call
            engine.remove(entity1, ImmutableBag.of(component(C1.class)));

            // Verify
            assertThat(archetype.contains(entity1)).as("archetype does not contain entity with non-matching components").isFalse();
            assertThat(archetype.contains(entity2)).as("archetype does not contain entity with non-matching components").isFalse();
            assertThat(archetype.getCount()).as("getCount returns number of matching entities").isEqualTo(0);
            assertThat(archetype.getEntities().iterator()).toIterable().as("getEntities contains only matching entities").isEmpty();

            assertThat(archetype1.contains(entity1)).as("archetype contains entity with matching components").isTrue();
            assertThat(archetype1.contains(entity2)).as("archetype contains entity with matching components").isTrue();
            assertThat(archetype1.getCount()).as("getCount returns number of matching entities").isEqualTo(2);
            assertThat(archetype1.getEntities().iterator()).toIterable().as("getEntities contains only matching entities").containsExactlyInAnyOrder(entity1, entity2);
        }

        @Test
        void testFlushedRemoveComponent() {
            var archetype = engine.getArchetype();
            var archetype1 = engine.getArchetype(component(C1.class));

            var entity1 = world.createEntity(new C1());
            var entity2 = world.createEntity(new C1());

            assertThat(archetype.contains(entity1)).as("archetype does not contain entity with non-matching components").isFalse();
            assertThat(archetype.contains(entity2)).as("archetype does not contain entity with non-matching components").isFalse();
            assertThat(archetype.getCount()).as("getCount returns number of matching entities").isEqualTo(0);
            assertThat(archetype.getEntities().iterator()).toIterable().as("getEntities contains only matching entities").isEmpty();

            assertThat(archetype1.contains(entity1)).as("archetype contains entity with matching components").isTrue();
            assertThat(archetype1.contains(entity2)).as("archetype contains entity with matching components").isTrue();
            assertThat(archetype1.getCount()).as("getCount returns number of matching entities").isEqualTo(2);
            assertThat(archetype1.getEntities().iterator()).toIterable().as("getEntities contains only matching entities").containsExactlyInAnyOrder(entity1, entity2);

            // Call
            engine.remove(entity1, ImmutableBag.of(component(C1.class)));
            engine.flushChanges(entity1);

            // Verify
            assertThat(archetype.contains(entity1)).as("archetype contains entity with matching components").isTrue();
            assertThat(archetype.contains(entity2)).as("archetype does not contain entity with non-matching components").isFalse();
            assertThat(archetype.getCount()).as("getCount returns number of matching entities").isEqualTo(1);
            assertThat(archetype.getEntities().iterator()).toIterable().as("getEntities contains only matching entities").containsExactlyInAnyOrder(entity1);

            assertThat(archetype1.contains(entity1)).as("archetype does not contain entity with non-matching components").isFalse();
            assertThat(archetype1.contains(entity2)).as("archetype contains entity with matching components").isTrue();
            assertThat(archetype1.getCount()).as("getCount returns number of matching entities").isEqualTo(1);
            assertThat(archetype1.getEntities().iterator()).toIterable().as("getEntities contains only matching entities").containsExactly(entity2);
        }

        @Test
        void testModifyComponents() {
            var archetype = engine.getArchetype();
            var archetype1 = engine.getArchetype(component(C1.class));
            var archetype2 = engine.getArchetype(component(C2.class));
            var archetype12 = engine.getArchetype(component(C1.class), component(C2.class));

            var entity1 = world.createEntity(new C1());
            var entity2 = world.createEntity(new C1());

            assertThat(archetype1.contains(entity1)).as("archetype contains entity with matching components").isTrue();
            assertThat(archetype1.contains(entity2)).as("archetype contains entity with matching components").isTrue();
            assertThat(archetype1.getCount()).as("getCount returns number of matching entities").isEqualTo(2);
            assertThat(archetype1.getEntities().iterator()).toIterable().as("getEntities contains only matching entities").containsExactlyInAnyOrder(entity1, entity2);

            assertThat(archetype2.contains(entity1)).as("archetype does not contain entity with non-matching components").isFalse();
            assertThat(archetype2.contains(entity2)).as("archetype does not contain entity with non-matching components").isFalse();
            assertThat(archetype2.getCount()).as("getCount returns number of matching entities").isEqualTo(0);
            assertThat(archetype2.getEntities().iterator()).toIterable().as("getEntities contains only matching entities").isEmpty();

            assertThat(archetype.getCount()).as("empty archetype does not contain any of these entities").isZero();
            assertThat(archetype12.getCount()).as("archetype with C1 and C2 does not contain any of these entities").isZero();

            // Call
            engine.modify(entity1, new Object[] { new C2() }, ImmutableBag.of(component(C1.class)));

            // Verify
            assertThat(archetype1.contains(entity1)).as("archetype contains entity with matching components").isTrue();
            assertThat(archetype1.contains(entity2)).as("archetype contains entity with matching components").isTrue();
            assertThat(archetype1.getCount()).as("getCount returns number of matching entities").isEqualTo(2);
            assertThat(archetype1.getEntities().iterator()).toIterable().as("getEntities contains only matching entities").containsExactlyInAnyOrder(entity1, entity2);

            assertThat(archetype2.contains(entity1)).as("archetype does not contain entity with non-matching components").isFalse();
            assertThat(archetype2.contains(entity2)).as("archetype does not contain entity with non-matching components").isFalse();
            assertThat(archetype2.getCount()).as("getCount returns number of matching entities").isEqualTo(0);
            assertThat(archetype2.getEntities().iterator()).toIterable().as("getEntities contains only matching entities").isEmpty();

            assertThat(archetype.getCount()).as("empty archetype does not contain any of these entities").isZero();
            assertThat(archetype12.getCount()).as("archetype with C1 and C2 does not contain any of these entities").isZero();
        }

        @Test
        void testFlushedModifyComponents() {
            var archetype = engine.getArchetype();
            var archetype1 = engine.getArchetype(component(C1.class));
            var archetype2 = engine.getArchetype(component(C2.class));
            var archetype12 = engine.getArchetype(component(C1.class), component(C2.class));

            var entity1 = world.createEntity(new C1());
            var entity2 = world.createEntity(new C1());

            assertThat(archetype1.contains(entity1)).as("archetype contains entity with matching components").isTrue();
            assertThat(archetype1.contains(entity2)).as("archetype contains entity with matching components").isTrue();
            assertThat(archetype1.getCount()).as("getCount returns number of matching entities").isEqualTo(2);
            assertThat(archetype1.getEntities().iterator()).toIterable().as("getEntities contains only matching entities").containsExactlyInAnyOrder(entity1, entity2);

            assertThat(archetype2.contains(entity1)).as("archetype does not contain entity with non-matching components").isFalse();
            assertThat(archetype2.contains(entity2)).as("archetype does not contain entity with non-matching components").isFalse();
            assertThat(archetype2.getCount()).as("getCount returns number of matching entities").isEqualTo(0);
            assertThat(archetype2.getEntities().iterator()).toIterable().as("getEntities contains only matching entities").isEmpty();

            assertThat(archetype.getCount()).as("empty archetype does not contain any of these entities").isZero();
            assertThat(archetype12.getCount()).as("archetype with C1 and C2 does not contain any of these entities").isZero();

            // Call
            engine.modify(entity1, new Object[] { new C2() }, ImmutableBag.of(component(C1.class)));
            engine.flushChanges(entity1);

            // Verify
            assertThat(archetype1.contains(entity1)).as("archetype does not contain entity with non-matching components").isFalse();
            assertThat(archetype1.contains(entity2)).as("archetype contains entity with matching components").isTrue();
            assertThat(archetype1.getCount()).as("getCount returns number of matching entities").isEqualTo(1);
            assertThat(archetype1.getEntities().iterator()).toIterable().as("getEntities contains only matching entities").containsExactly(entity2);

            assertThat(archetype2.contains(entity1)).as("archetype contains entity with matching components").isTrue();
            assertThat(archetype2.contains(entity2)).as("archetype does not contain entity with non-matching components").isFalse();
            assertThat(archetype2.getCount()).as("getCount returns number of matching entities").isEqualTo(1);
            assertThat(archetype2.getEntities().iterator()).toIterable().as("getEntities contains only matching entities").containsExactlyInAnyOrder(entity1);

            assertThat(archetype.getCount()).as("empty archetype does not contain any of these entities").isZero();
            assertThat(archetype12.getCount()).as("archetype with C1 and C2 does not contain any of these entities").isZero();
        }

    }

    @Nested
    class EntityRemovalTest {

        @Test
        void testDeleteEntity() {
            var archetype = engine.getArchetype();

            var entity1 = world.createEntity();
            var entity2 = world.createEntity();

            assertThat(archetype.contains(entity1)).as("archetype contains entity with matching components").isTrue();
            assertThat(archetype.contains(entity2)).as("archetype contains entity with matching components").isTrue();
            assertThat(archetype.getCount()).as("getCount returns number of matching entities").isEqualTo(2);
            assertThat(archetype.getEntities().iterator()).toIterable().as("getEntities contains only matching entities").containsExactlyInAnyOrder(entity1, entity2);

            // Call
            engine.delete(entity1);

            // Verify
            assertThat(archetype.contains(entity1)).as("archetype does not contain deleted entity").isFalse();
            assertThat(archetype.contains(entity2)).as("archetype contains entity with matching components").isTrue();
            assertThat(archetype.getCount()).as("getCount returns number of matching entities").isEqualTo(1);
            assertThat(archetype.getEntities().iterator()).toIterable().as("getEntities contains only matching entities").containsExactly(entity2);
        }

    }

    @Nested
    class GetComponentIndexTest {

        @Test
        void testGetComponentIndexById() {
            var type1 = engine.getComponent(component(C1.class)).id();
            var type2 = engine.getComponent(component(C2.class)).id();
            var relation12 = engine.getComponent(relation(C1.class, C2.class)).id();
            var exclusive12 = engine.getComponent(exclusiveRelation(E1.class, C2.class)).id();
            var relation1 = engine.getComponent(relation(C1.class)).id();
            var exclusive1 = engine.getComponent(exclusiveRelation(E1.class)).id();

            // Verify
            var archetype1 = engine.getArchetype(component(C1.class));
            assertThat(archetype1.getComponentIndex(type1)).as("getComponentIndex returns the index for componentIds part of the archetype").isEqualTo(0);
            assertThat(archetype1.getComponentIndex(type2)).as("getComponentIndex returns -1 for componentIds not part of the archetype").isEqualTo(-1);
            assertThat(archetype1.getComponentIndex(relation12)).as("getComponentIndex returns -1 for componentIds not part of the archetype").isEqualTo(-1);
            assertThat(archetype1.getComponentIndex(exclusive12)).as("getComponentIndex returns -1 for componentIds not part of the archetype").isEqualTo(-1);
            assertThat(archetype1.getComponentIndex(relation1)).as("getComponentIndex returns -1 for componentIds not part of the archetype").isEqualTo(-1);
            assertThat(archetype1.getComponentIndex(exclusive1)).as("getComponentIndex returns -1 for componentIds not part of the archetype").isEqualTo(-1);

            var archetype2 = engine.getArchetype(component(C2.class));
            assertThat(archetype2.getComponentIndex(type1)).as("getComponentIndex returns -1 for componentIds not part of the archetype").isEqualTo(-1);
            assertThat(archetype2.getComponentIndex(type2)).as("getComponentIndex returns the index for contained componentIds").isGreaterThan(-1);
            assertThat(archetype2.getComponentIndex(relation12)).as("getComponentIndex returns -1 for componentIds not part of the archetype").isEqualTo(-1);
            assertThat(archetype2.getComponentIndex(exclusive12)).as("getComponentIndex returns -1 for componentIds not part of the archetype").isEqualTo(-1);
            assertThat(archetype2.getComponentIndex(relation1)).as("getComponentIndex returns -1 for componentIds not part of the archetype").isEqualTo(-1);
            assertThat(archetype2.getComponentIndex(exclusive1)).as("getComponentIndex returns -1 for componentIds not part of the archetype").isEqualTo(-1);

            // refactor to isEqualTo(0) if default storage is removed 
            var archetype12 = engine.getArchetype(component(C1.class), component(C2.class));
            var i1 = assertThat(archetype12.getComponentIndex(type1)).as("getComponentIndex returns the index for contained componentIds").isGreaterThan(-1).actual();
            assertThat(archetype12.getComponentIndex(type2)).as("getComponentIndex returns the index for contained componentIds").isGreaterThan(i1);
            assertThat(archetype12.getComponentIndex(relation12)).as("getComponentIndex returns -1 for componentIds not part of the archetype").isEqualTo(-1);
            assertThat(archetype12.getComponentIndex(exclusive12)).as("getComponentIndex returns -1 for componentIds not part of the archetype").isEqualTo(-1);
            assertThat(archetype12.getComponentIndex(relation1)).as("getComponentIndex returns -1 for componentIds not part of the archetype").isEqualTo(-1);
            assertThat(archetype12.getComponentIndex(exclusive1)).as("getComponentIndex returns -1 for componentIds not part of the archetype").isEqualTo(-1);

            // refactor to isEqualTo(0), isEqualTo(1), ... if default storage is removed
            var archetype = engine.getArchetype(component(C2.class), relation(C1.class, C2.class), exclusiveRelation(E1.class, C2.class), relation(C1.class), exclusiveRelation(E1.class));
            assertThat(archetype.getComponentIndex(type1)).as("getComponentIndex returns -1 for componentIds not part of the archetype").isEqualTo(-1);
            i1 = assertThat(archetype.getComponentIndex(type2)).as("getComponentIndex returns the index for contained componentIds").isGreaterThan(-1).actual();
            var i2 = assertThat(archetype.getComponentIndex(relation12)).as("getComponentIndex returns the index for contained componentIds").isGreaterThan(i1).actual();
            var i3 = assertThat(archetype.getComponentIndex(exclusive12)).as("getComponentIndex returns the index for contained componentIds").isGreaterThan(i2).actual();
            var i4 = assertThat(archetype.getComponentIndex(relation1)).as("getComponentIndex returns the index for contained componentIds").isGreaterThan(i3).actual();
            assertThat(archetype.getComponentIndex(exclusive1)).as("getComponentIndex returns the index for contained componentIds").isGreaterThan(i4);
        }

        @Test
        void testGetComponentIndexByType() {
            var type1 = engine.getComponent(component(C1.class)).type();
            var type2 = engine.getComponent(component(C2.class)).type();
            var relation12 = engine.getComponent(relation(C1.class, C2.class)).type();
            var exclusive12 = engine.getComponent(exclusiveRelation(E1.class, C2.class)).type();
            var relation1 = engine.getComponent(relation(C1.class)).type();
            var exclusive1 = engine.getComponent(exclusiveRelation(E1.class)).type();

            // Verify
            var archetype1 = engine.getArchetype(component(C1.class));
            assertThat(archetype1.getComponentIndex(type1)).as("getComponentIndex returns the index for componentIds part of the archetype").isEqualTo(0);
            assertThat(archetype1.getComponentIndex(type2)).as("getComponentIndex returns -1 for componentIds not part of the archetype").isEqualTo(-1);
            assertThat(archetype1.getComponentIndex(relation12)).as("getComponentIndex returns -1 for componentIds not part of the archetype").isEqualTo(-1);
            assertThat(archetype1.getComponentIndex(exclusive12)).as("getComponentIndex returns -1 for componentIds not part of the archetype").isEqualTo(-1);
            assertThat(archetype1.getComponentIndex(relation1)).as("getComponentIndex returns -1 for componentIds not part of the archetype").isEqualTo(-1);
            assertThat(archetype1.getComponentIndex(exclusive1)).as("getComponentIndex returns -1 for componentIds not part of the archetype").isEqualTo(-1);

            var archetype2 = engine.getArchetype(component(C2.class));
            assertThat(archetype2.getComponentIndex(type1)).as("getComponentIndex returns -1 for componentIds not part of the archetype").isEqualTo(-1);
            assertThat(archetype2.getComponentIndex(type2)).as("getComponentIndex returns the index for contained componentIds").isGreaterThan(-1);
            assertThat(archetype2.getComponentIndex(relation12)).as("getComponentIndex returns -1 for componentIds not part of the archetype").isEqualTo(-1);
            assertThat(archetype2.getComponentIndex(exclusive12)).as("getComponentIndex returns -1 for componentIds not part of the archetype").isEqualTo(-1);
            assertThat(archetype2.getComponentIndex(relation1)).as("getComponentIndex returns -1 for componentIds not part of the archetype").isEqualTo(-1);
            assertThat(archetype2.getComponentIndex(exclusive1)).as("getComponentIndex returns -1 for componentIds not part of the archetype").isEqualTo(-1);

            // refactor to isEqualTo(0) if default storage is removed 
            var archetype12 = engine.getArchetype(component(C1.class), component(C2.class));
            var i1 = assertThat(archetype12.getComponentIndex(type1)).as("getComponentIndex returns the index for contained componentIds").isGreaterThan(-1).actual();
            assertThat(archetype12.getComponentIndex(type2)).as("getComponentIndex returns the index for contained componentIds").isGreaterThan(i1);
            assertThat(archetype12.getComponentIndex(relation12)).as("getComponentIndex returns -1 for componentIds not part of the archetype").isEqualTo(-1);
            assertThat(archetype12.getComponentIndex(exclusive12)).as("getComponentIndex returns -1 for componentIds not part of the archetype").isEqualTo(-1);
            assertThat(archetype12.getComponentIndex(relation1)).as("getComponentIndex returns -1 for componentIds not part of the archetype").isEqualTo(-1);
            assertThat(archetype12.getComponentIndex(exclusive1)).as("getComponentIndex returns -1 for componentIds not part of the archetype").isEqualTo(-1);

            // refactor to isEqualTo(0), isEqualTo(1), ... if default storage is removed
            var archetype = engine.getArchetype(component(C2.class), relation(C1.class, C2.class), exclusiveRelation(E1.class, C2.class), relation(C1.class), exclusiveRelation(E1.class));
            assertThat(archetype.getComponentIndex(type1)).as("getComponentIndex returns -1 for componentIds not part of the archetype").isEqualTo(-1);
            i1 = assertThat(archetype.getComponentIndex(type2)).as("getComponentIndex returns the index for contained componentIds").isGreaterThan(-1).actual();
            var i2 = assertThat(archetype.getComponentIndex(relation12)).as("getComponentIndex returns the index for contained componentIds").isGreaterThan(i1).actual();
            var i3 = assertThat(archetype.getComponentIndex(exclusive12)).as("getComponentIndex returns the index for contained componentIds").isGreaterThan(i2).actual();
            var i4 = assertThat(archetype.getComponentIndex(relation1)).as("getComponentIndex returns the index for contained componentIds").isGreaterThan(i3).actual();
            assertThat(archetype.getComponentIndex(exclusive1)).as("getComponentIndex returns the index for contained componentIds").isGreaterThan(i4);
        }

    }

    @Nested
    class EntityDataTest {

        @Test
        void testSingleComponent() {
            var componentId1 = engine.getComponent(component(C1.class)).id();

            var archetype = engine.getArchetype(component(C1.class));

            var component1 = new C1(1);
            var entity1 = world.createEntity(component1);

            var component2 = new C1(2);
            var entity2 = world.createEntity(component2);

            var entities = archetype.getEntityData(component(C1.class));
            var accessor = entities.getAccessor();

            // Iteration 1
            assertThat(accessor.hasNext()).as("first hasNext returns true for two entities").isTrue();
            var current = assertThat(accessor.next()).as("next returns entityId").isIn(entity1, entity2).actual();
            assertThat(accessor.<C1>getComponent(componentId1)).as("getComponent returns instance of current entity").isSameAs(current.equals(entity1) ? component1 : component2);

            // Iteration 2
            assertThat(accessor.hasNext()).as("second hasNext returns true for two entities").isTrue();
            current = assertThat(accessor.next()).as("next returns entityId").isIn(entity1, entity2).actual();
            assertThat(accessor.<C1>getComponent(componentId1)).as("getComponent returns instance of current entity").isSameAs(current.equals(entity1) ? component1 : component2);

            // Iteration 3
            assertThat(accessor.hasNext()).as("third hasNext returns false for two entities").isFalse();
        }

        @Test
        void testMultipleComponents() {
            var componentId1 = engine.getComponent(component(C1.class)).id();
            var componentId2 = engine.getComponent(component(C2.class)).id();

            var archetype = engine.getArchetype(component(C1.class), component(C2.class));

            var component11 = new C1(11);
            var component12 = new C2(12);
            var entity1 = world.createEntity(component11, component12);

            var component21 = new C1(21);
            var component22 = new C2(22);
            var entity2 = world.createEntity(component21, component22);

            var entities = archetype.getEntityData(component(C1.class), component(C2.class));
            var accessor = entities.getAccessor();

            // Iteration 1
            assertThat(accessor.hasNext()).as("first hasNext returns true for two entities").isTrue();
            var current = assertThat(accessor.next()).as("next returns entityId").isIn(entity1, entity2).actual();
            assertThat(accessor.<C1>getComponent(componentId1)).as("getComponent returns instance of current entity").isSameAs(current.equals(entity1) ? component11 : component21);
            assertThat(accessor.<C2>getComponent(componentId2)).as("getComponent returns instance of current entity").isSameAs(current.equals(entity1) ? component12 : component22);

            // Iteration 2
            assertThat(accessor.hasNext()).as("second hasNext returns true for two entities").isTrue();
            current = assertThat(accessor.next()).as("next returns entityId").isIn(entity1, entity2).actual();
            assertThat(accessor.<C1>getComponent(componentId1)).as("getComponent returns instance of current entity").isSameAs(current.equals(entity1) ? component11 : component21);
            assertThat(accessor.<C2>getComponent(componentId2)).as("getComponent returns instance of current entity").isSameAs(current.equals(entity1) ? component12 : component22);

            // Iteration 3
            assertThat(accessor.hasNext()).as("third hasNext returns false for two entities").isFalse();
        }

        @Test
        void testMultipleComponents_AccessFewerComponents() {
            var componentId1 = engine.getComponent(component(C1.class)).id();

            var archetype = engine.getArchetype(component(C1.class), component(C2.class));

            var component11 = new C1(11);
            var component12 = new C2(12);
            var entity1 = world.createEntity(component11, component12);

            var component21 = new C1(21);
            var component22 = new C2(22);
            var entity2 = world.createEntity(component21, component22);

            var entities = archetype.getEntityData(component(C1.class));
            var accessor = entities.getAccessor();

            // Iteration 1
            assertThat(accessor.hasNext()).as("first hasNext returns true for two entities").isTrue();
            var current = assertThat(accessor.next()).as("next returns entityId").isIn(entity1, entity2).actual();
            assertThat(accessor.<C1>getComponent(componentId1)).as("getComponent returns instance of current entity").isSameAs(current.equals(entity1) ? component11 : component21);

            // Iteration 2
            assertThat(accessor.hasNext()).as("second hasNext returns true for two entities").isTrue();
            current = assertThat(accessor.next()).as("next returns entityId").isIn(entity1, entity2).actual();
            assertThat(accessor.<C1>getComponent(componentId1)).as("getComponent returns instance of current entity").isSameAs(current.equals(entity1) ? component11 : component21);

            // Iteration 3
            assertThat(accessor.hasNext()).as("third hasNext returns false for two entities").isFalse();
        }

        @Test
        void testMultipleComponents_AccessMoreComponents() {
            var componentId1 = engine.getComponent(component(C1.class)).id();
            var componentId3 = engine.getComponent(component(C3.class)).id();

            var archetype = engine.getArchetype(component(C1.class), component(C2.class));

            var component11 = new C1(11);
            var component12 = new C2(12);
            var entity1 = world.createEntity(component11, component12);

            var component21 = new C1(21);
            var component22 = new C2(22);
            var entity2 = world.createEntity(component21, component22);

            var entities = archetype.getEntityData(component(C1.class), component(C3.class));
            var accessor = entities.getAccessor();

            // Iteration 1
            assertThat(accessor.hasNext()).as("first hasNext returns true for two entities").isTrue();
            var current = assertThat(accessor.next()).as("next returns entityId").isIn(entity1, entity2).actual();
            assertThat(accessor.<C1>getComponent(componentId1)).as("getComponent returns instance of current entity").isSameAs(current.equals(entity1) ? component11 : component21);
            assertThat(accessor.<C3>getComponent(componentId3)).as("getComponent returns instance of current entity").isNull();

            // Iteration 2
            assertThat(accessor.hasNext()).as("second hasNext returns true for two entities").isTrue();
            current = assertThat(accessor.next()).as("next returns entityId").isIn(entity1, entity2).actual();
            assertThat(accessor.<C1>getComponent(componentId1)).as("getComponent returns instance of current entity").isSameAs(current.equals(entity1) ? component11 : component21);
            assertThat(accessor.<C3>getComponent(componentId3)).as("getComponent returns instance of current entity").isNull();

            // Iteration 3
            assertThat(accessor.hasNext()).as("third hasNext returns false for two entities").isFalse();
        }

        @Test
        void testGetPendingComponents() {
            var componentId1 = engine.getComponent(component(C1.class)).id();
            var componentId2 = engine.getComponent(component(C2.class)).id();

            var archetype = engine.getArchetype();
            var archetype1 = engine.getArchetype(component(C1.class));

            var entity = world.createEntity();
            var entity1 = world.createEntity(new C1(11));

            engine.add(entity, new Object[] { new C2(20) });
            engine.add(entity1, new Object[] { new C2(21) });

            // Test empty archetype, access C1, C2
            var entities12 = archetype.getEntityData(component(C1.class), component(C2.class));
            var accessor12 = entities12.getAccessor();

            assertThat(accessor12.next()).isEqualTo(entity);
            assertThat(accessor12.<C1>getComponent(componentId1)).as("TODO").isNull();
            assertThat(accessor12.<C2>getComponent(componentId2)).as("TODO").isEqualTo(new C2(20));

            // Test empty archetype, access C2, C1
            var entities21 = archetype.getEntityData(component(C2.class), component(C1.class));
            var accessor21 = entities21.getAccessor();

            assertThat(accessor21.next()).isEqualTo(entity);
            assertThat(accessor21.<C1>getComponent(componentId1)).as("TODO").isNull();
            assertThat(accessor21.<C2>getComponent(componentId2)).as("TODO").isEqualTo(new C2(20));

            // Test C1 archetype, access C1, C2
            entities12 = archetype1.getEntityData(component(C1.class), component(C2.class));
            accessor12 = entities12.getAccessor();

            assertThat(accessor12.next()).isEqualTo(entity1);
            assertThat(accessor12.<C1>getComponent(componentId1)).as("TODO").isEqualTo(new C1(11));
            assertThat(accessor12.<C2>getComponent(componentId2)).as("TODO").isEqualTo(new C2(21));

            // Test C1 archetype, access C2, C1
            entities21 = archetype1.getEntityData(component(C2.class), component(C1.class));
            accessor21 = entities21.getAccessor();

            assertThat(accessor21.next()).isEqualTo(entity1);
            assertThat(accessor21.<C1>getComponent(componentId1)).as("TODO").isEqualTo(new C1(11));
            assertThat(accessor21.<C2>getComponent(componentId2)).as("TODO").isEqualTo(new C2(21));
        }

        @Test
        void testReset() {
            var componentId1 = engine.getComponent(component(C1.class)).id();

            var archetype = engine.getArchetype(component(C1.class));

            var component1 = new C1(1);
            var entity1 = world.createEntity(component1);

            var component2 = new C1(2);
            var entity2 = world.createEntity(component2);

            var entities = archetype.getEntityData(component(C1.class));
            var accessor = entities.getAccessor();

            // Iterate once
            accessor.hasNext();
            accessor.next();
            accessor.hasNext();
            accessor.next();

            // Free accessor, retrieve same accessor again
            assertThat(entities.getAccessor()).as("retrieving a new accessor returns a different instance").isNotSameAs(accessor);

            accessor.free();
            assertThat(entities.getAccessor()).as("retrieving a new accessor after free returns a same instance").isSameAs(accessor);

            // Iteration 1
            assertThat(accessor.hasNext()).as("first hasNext returns true for two entities").isTrue();
            var current = assertThat(accessor.next()).as("next returns entityId").isIn(entity1, entity2).actual();
            assertThat(accessor.<C1>getComponent(componentId1)).as("getComponent returns instance of current entity").isSameAs(current.equals(entity1) ? component1 : component2);

            // Iteration 2
            assertThat(accessor.hasNext()).as("second hasNext returns true for two entities").isTrue();
            current = assertThat(accessor.next()).as("next returns entityId").isIn(entity1, entity2).actual();
            assertThat(accessor.<C1>getComponent(componentId1)).as("getComponent returns instance of current entity").isSameAs(current.equals(entity1) ? component1 : component2);

            // Iteration 3
            assertThat(accessor.hasNext()).as("third hasNext returns false for two entities").isFalse();
        }

        @Nested
        class SizeTest {

            @ParameterizedTest
            @ValueSource(ints = { 0, 1, 5 })
            void testSize_DefaultEntityData_BeforeCreation(int count) {
                var archetype = engine.getArchetype(component(C1.class));
                var entities = archetype.getEntityData();

                for (int i = 0; i < count; i++) {
                    world.createEntity(new C1());
                }

                // Verify
                assertThat(entities.getSize()).as("EntityData#getSize returns number of entities in archetype").isEqualTo(count);
            }

            @ParameterizedTest
            @ValueSource(ints = { 0, 1, 5 })
            void testSize_DefaultEntityData_AfterCreation(int count) {
                var archetype = engine.getArchetype(component(C1.class));

                for (int i = 0; i < count; i++) {
                    world.createEntity(new C1());
                }

                // Verify
                var entities = archetype.getEntityData();
                assertThat(entities.getSize()).as("EntityData#getSize returns number of entities in archetype").isEqualTo(count);
            }

            @ParameterizedTest
            @ValueSource(ints = { 0, 1, 5 })
            void testSize_MatchingEntityData_BeforeCreation(int count) {
                var archetype = engine.getArchetype(component(C1.class));
                var entities = archetype.getEntityData(component(C1.class));

                for (int i = 0; i < count; i++) {
                    world.createEntity(new C1());
                }

                // Verify
                assertThat(entities.getSize()).as("EntityData#getSize returns number of entities in archetype").isEqualTo(count);
            }

            @ParameterizedTest
            @ValueSource(ints = { 0, 1, 5 })
            void testSize_MatchingEntityData_AfterCreation(int count) {
                var archetype = engine.getArchetype(component(C1.class));

                for (int i = 0; i < count; i++) {
                    world.createEntity(new C1());
                }

                // Verify
                var entities = archetype.getEntityData(component(C1.class));
                assertThat(entities.getSize()).as("EntityData#getSize returns number of entities in archetype").isEqualTo(count);
            }

            @ParameterizedTest
            @ValueSource(ints = { 0, 1, 5 })
            void testSize_MismatchingEntityData_BeforeCreation(int count) {
                var archetype = engine.getArchetype(component(C1.class));
                var entities = archetype.getEntityData(component(C2.class));

                for (int i = 0; i < count; i++) {
                    world.createEntity(new C1());
                }

                // Verify
                assertThat(entities.getSize()).as("EntityData#getSize returns number of entities in archetype").isEqualTo(count);
            }

            @ParameterizedTest
            @ValueSource(ints = { 0, 1, 5 })
            void testSize_MismatchingEntityData_AfterCreation(int count) {
                var archetype = engine.getArchetype(component(C1.class));

                for (int i = 0; i < count; i++) {
                    world.createEntity(new C1());
                }

                // Verify
                var entities = archetype.getEntityData(component(C2.class));
                assertThat(entities.getSize()).as("EntityData#getSize returns number of entities in archetype").isEqualTo(count);
            }

        }

        @Nested
        class GetIdTest {

            @ParameterizedTest
            @ValueSource(ints = { 1, 5 })
            void testGetId(int count) {
                var archetype = engine.getArchetype(component(C1.class));
                var entities = archetype.getEntityData();

                var entityIds = new ArrayList<Integer>();
                for (int i = 0; i < count; i++) {
                    entityIds.add(world.createEntity(new C1()));
                    world.createEntity(new C2());
                }

                // Verify
                for (int i = 0; i < count; i++) {
                    assertThat(entities.getId(i)).as("getId returns contained entities").isIn(entityIds);
                }
            }

        }

        @Nested
        class GetComponentTest {

            @ParameterizedTest
            @ValueSource(ints = { 1, 5 })
            void testEmptyArchetype(int count) {
                var archetype = engine.getArchetype();
                var entities = archetype.getEntityData();

                var entityIds = new ArrayList<Integer>();
                for (int i = 0; i < count; i++) {
                    entityIds.add(world.createEntity());
                }

                // Verify
                for (int i = 0; i < count; i++) {
                    assertThat(entities.<C1>getComponent(i)).as("getComponent returns null for empty archetype").isNull();
                }
            }

            @ParameterizedTest
            @ValueSource(ints = { 1, 5 })
            void testSingleComponentArchetype(int count) {
                var archetype = engine.getArchetype(component(C1.class));
                var entities = archetype.getEntityData();

                var entityIds = new ArrayList<Integer>();
                var components = new ArrayList<C1>();

                for (int i = 0; i < count; i++) {
                    var component = new C1(i);

                    entityIds.add(world.createEntity(component));
                    components.add(component);
                }

                // Verify
                for (int i = 0; i < count; i++) {
                    assertThat(entities.<C1>getComponent(i)).as("getComponent returns component by entity index").isSameAs(components.get(i));
                }
            }

            @ParameterizedTest
            @ValueSource(ints = { 1, 5 })
            void testSingleComponentArchetype_PendingComponent(int count) {
                var archetype = engine.getArchetype(component(C1.class));
                var entities = archetype.getEntityData(component(C2.class));

                var mapper2 = world.getComponents(C2.class);

                var entityIds = new ArrayList<Integer>();
                var components = new ArrayList<C2>();

                for (int i = 0; i < count; i++) {
                    var entityId = world.createEntity(new C1(i));
                    entityIds.add(entityId);

                    var component2 = new C2(i);
                    components.add(component2);

                    mapper2.add(entityId, component2);
                }

                // Verify
                for (int i = 0; i < count; i++) {
                    assertThat(entities.<C2>getComponent(i)).as("getComponent returns pending component by entity index").isSameAs(components.get(i));
                }
            }

            @ParameterizedTest
            @ValueSource(ints = { 1, 5 })
            void testMultipleComponentsArchetype(int count) {
                var archetype = engine.getArchetype(component(C1.class), component(C2.class));
                var entities = archetype.getEntityData();

                var entityIds = new ArrayList<Integer>();
                var components = new ArrayList<C1>();

                for (int i = 0; i < count; i++) {
                    var component1 = new C1(i);
                    entityIds.add(world.createEntity(component1, new C2()));
                    components.add(component1);

                    var component2 = new C1(i);
                    entityIds.add(world.createEntity(new C2(), component2));
                    components.add(component2);
                }

                // Verify
                for (int i = 0; i < count; i++) {
                    assertThat(entities.<C1>getComponent(i)).as("getComponent returns first component defined by archetype").isSameAs(components.get(i));
                }
            }

        }

        @Nested
        class AccessorTest {

            @Nested
            class AccessorForEntityTest {

                @Test
                void testGetAccessorForEntity() {
                    var entities1 = engine.getArchetype(component(C1.class)).getEntityData();
                    var entities2 = engine.getArchetype(component(C2.class)).getEntityData();

                    var entity1 = world.createEntity(new C1());
                    var entity2 = world.createEntity(new C2());

                    // Verify
                    assertThat(entities1.getAccessor(entity1)).as("EntityData#getAccessor(int) must return instance for contained entities").isNotNull();
                    assertThat(entities1.getAccessor(entity2)).as("EntityData#getAccessor(int) must return null for other entities").isNull();

                    assertThat(entities2.getAccessor(entity1)).as("EntityData#getAccessor(int) must return null for other entities").isNull();
                    assertThat(entities2.getAccessor(entity2)).as("EntityData#getAccessor(int) must return instance for contained entities").isNotNull();
                }

                @Test
                void testAccessor() {
                    var componentId1 = engine.getComponent(component(C1.class)).id();
                    var componentId2 = engine.getComponent(component(C2.class)).id();

                    var component = new C1(42);
                    var entityId = world.createEntity(component);

                    var entities = engine.getArchetype(component(C1.class)).getEntityData();
                    var accessor = entities.getAccessor(entityId);

                    // Verify
                    assertThat(accessor).as("EntityData#getAccessor(int) must return instance for contained entities").isNotNull();

                    assertThat(accessor.hasComponent(componentId1)).as("hasComponent returns true for contained components").isTrue();
                    assertThat(accessor.hasComponent(componentId2)).as("hasComponent returns false for other components if not pending").isFalse();

                    assertThat(accessor.<C1>getComponent(componentId1)).as("getComponent returns instance for contained components").isSameAs(component);
                    assertThat(accessor.<C2>getComponent(componentId2)).as("getComponent returns null for other components if not pending").isNull();

                    assertThat(accessor.<C1>getComponentByIndex(0)).as("getComponentByIndex returns instance").isSameAs(component);

                    assertThat(accessor.<C1>getPendingComponent(componentId1)).as("getPendingComponent returns null for contained components").isNull();
                    assertThat(accessor.<C2>getPendingComponent(componentId2)).as("getPendingComponent returns null for other components if not pending").isNull();
                }

                @Test
                void testAccessor_PendingComponents() {
                    var componentId2 = engine.getComponent(component(C2.class)).id();

                    var entityId = world.createEntity(new C1(42));

                    var entities = engine.getArchetype(component(C1.class)).getEntityData();
                    var accessor = entities.getAccessor(entityId);

                    // Add component
                    var component2 = new C2(9001);
                    world.getComponents(C2.class).add(entityId, component2);

                    // Verify
                    assertThat(accessor).as("EntityData#getAccessor(int) must return instance for contained entities").isNotNull();

                    assertThat(accessor.hasComponent(componentId2)).as("hasComponent returns true for other components if pending").isTrue();
                    assertThat(accessor.<C2>getComponent(componentId2)).as("getComponent returns instance for other components if pending").isSameAs(component2);
                    assertThat(accessor.<C2>getPendingComponent(componentId2)).as("getPendingComponent returns instance for other components if pending").isSameAs(component2);
                }

            }

            @Nested
            class GetComponentTest {

                @Test
                void testGetComponent() {
                    // Introduce components, generate ids
                    var id1 = engine.getComponent(component(C1.class)).id();
                    var id2 = engine.getComponent(component(C2.class)).id();
                    var id3 = engine.getComponent(component(C3.class)).id();

                    var archetype1 = engine.getArchetype(component(C1.class));
                    var archetype2 = engine.getArchetype(component(C2.class));
                    var archetype12 = engine.getArchetype(component(C3.class));

                    // Create entities
                    var entity1 = world.createEntity(new C1(1));
                    var entity2 = world.createEntity(new C2(2));
                    var entity12 = world.createEntity(new C1(10), new C2(20));

                    var pending12 = world.createEntity(new C1(100));
                    engine.add(pending12, new Object[] { new C2(200) });

                    // Verify C1 accessor 
                    var accessor1 = archetype1.getEntityData().getAccessor();
                    while (accessor1.hasNext()) {
                        var entityId = accessor1.next();
                        assertThat(entityId).as("accessor must only iterate entities of archetype").isIn(entity1, pending12);

                        if (entityId == entity1) {
                            assertThat(accessor1.<C1>getComponent(id1)).as("accessor must return component by componentId").isEqualTo(new C1(1));
                            assertThat(accessor1.<C2>getComponent(id2)).as("accessor must return null for componentId not part of archetype").isNull();
                            assertThat(accessor1.<C3>getComponent(id3)).as("accessor must return null for componentId not part of archetype").isNull();

                            continue;
                        }

                        assertThat(accessor1.<C1>getComponent(id1)).as("accessor must return component by componentId").isEqualTo(new C1(100));
                        assertThat(accessor1.<C2>getComponent(id2)).as("accessor must return pending component by componentId").isEqualTo(new C2(200));
                        assertThat(accessor1.<C3>getComponent(id3)).as("accessor must return null for componentId not part of archetype").isNull();
                    }

                    // Verify C2 accessor 
                    var accessor2 = archetype2.getEntityData().getAccessor();
                    while (accessor2.hasNext()) {
                        assertThat(accessor2.next()).as("accessor must only iterate entities of archetype").isEqualTo(entity2);

                        assertThat(accessor2.<C1>getComponent(id1)).as("accessor must return null for componentId not part of archetype").isNull();
                        assertThat(accessor2.<C2>getComponent(id2)).as("accessor must return component by componentId").isEqualTo(new C2(2));
                        assertThat(accessor2.<C3>getComponent(id3)).as("accessor must return null for componentId not part of archetype").isNull();
                    }

                    // Verify C1, C2 accessor 
                    var accessor12 = archetype12.getEntityData().getAccessor();
                    while (accessor12.hasNext()) {
                        assertThat(accessor1.next()).as("accessor must only iterate entities of archetype").isEqualTo(entity12);

                        assertThat(accessor12.<C1>getComponent(id1)).as("accessor must return component by componentId").isEqualTo(new C1(10));
                        assertThat(accessor12.<C2>getComponent(id2)).as("accessor must return component by componentId").isEqualTo(new C2(20));
                        assertThat(accessor12.<C3>getComponent(id3)).as("accessor must return null for componentId not part of archetype").isNull();
                    }
                }

            }

            @Nested
            class GetComponentByIndexTest {

                @ParameterizedTest
                @MethodSource("simpleComponents")
                void testSimpleComponents(Object component) {
                    var componentType = ComponentType.detectComponentType(component);
                    var archetype = engine.getArchetype(componentType);

                    archetype.createEntity(7, new Object[] { component });

                    // Verify
                    var result = archetype.getEntityData().getAccessor(7).getComponentByIndex(0);
                    assertThat(result).as("getComponentByIndex must return component").isSameAs(component);
                }

                @ParameterizedTest
                @MethodSource("nonExclusiveRelations")
                void testNonExclusiveRelations(Relation<?> component) {
                    var componentType = ComponentType.detectComponentType(component);
                    var archetype = engine.getArchetype(componentType);

                    archetype.createEntity(7, new Object[] { component });

                    // Verify
                    var result = archetype.getEntityData().getAccessor(7).getComponentByIndex(0);
                    assertThat(result)
                            .as("getComponentByIndex must return relations with relation").asInstanceOf(InstanceOfAssertFactories.ITERABLE)
                            .as("getComponentByIndex must return relations with relation").containsExactly(component);
                }

                @ParameterizedTest
                @MethodSource("simpleComponents")
                void testSimpleComponents_MultipleComponentsArchetype(Object component) {
                    var otherComponent = new C3();

                    var componentType = ComponentType.detectComponentType(component);
                    var archetype = engine.getArchetype(component(C3.class), componentType);

                    var componentIndex = archetype.getComponentIndex(engine.getComponent(componentType).id());
                    var otherIndex = archetype.getComponentIndex(engine.getComponent(component(C3.class)).id());

                    assertThat(componentIndex).isIn(0, 1);
                    assertThat(otherIndex).isIn(0, 1).isNotEqualTo(componentIndex);

                    archetype.createEntity(7, new Object[] { otherComponent, component });

                    // Verify
                    var accessor = archetype.getEntityData().getAccessor(7);

                    var result = accessor.getComponentByIndex(componentIndex);
                    assertThat(result).as("getComponentByIndex must return component").isSameAs(component);

                    var otherResult = accessor.getComponentByIndex(otherIndex);
                    assertThat(otherResult).as("getComponentByIndex must return component").isSameAs(otherComponent);
                }

                @ParameterizedTest
                @MethodSource("nonExclusiveRelations")
                void testNonExclusiveRelations_MultipleComponentsArchetype(Relation<?> component) {
                    var otherComponent = new C3();

                    var componentType = ComponentType.detectComponentType(component);
                    var archetype = engine.getArchetype(component(C3.class), componentType);

                    var relationIndex = archetype.getComponentIndex(engine.getComponent(componentType).id());
                    var otherIndex = archetype.getComponentIndex(engine.getComponent(component(C3.class)).id());

                    assertThat(relationIndex).isIn(0, 1);
                    assertThat(otherIndex).isIn(0, 1).isNotEqualTo(relationIndex);

                    archetype.createEntity(7, new Object[] { otherComponent, component });

                    // Verify
                    var accessor = archetype.getEntityData().getAccessor(7);

                    var result = accessor.getComponentByIndex(relationIndex);
                    assertThat(result)
                            .as("getComponentByIndex must return relations with relation").asInstanceOf(InstanceOfAssertFactories.ITERABLE)
                            .as("getComponentByIndex must return relations with relation").containsExactly(component);

                    var otherResult = accessor.getComponentByIndex(otherIndex);
                    assertThat(otherResult).as("getComponentByIndex must return component").isSameAs(otherComponent);
                }

                private static Stream<Object> simpleComponents() {
                    return Stream.of(
                            new C1(),
                            Relation.create(E1.INSTANCE, new C2()),
                            Relation.create(E1.INSTANCE, 42));
                }

                private static Stream<Object> nonExclusiveRelations() {
                    return Stream.of(
                            Relation.create(new C1(), new C2()),
                            Relation.create(new C1(), 42));
                }

            }

            @Nested
            class GetPendingComponentTest {

                @ParameterizedTest
                @MethodSource("simpleComponents")
                void testSimpleComponents_EmptyArchetype(Object component) {
                    var archetype = engine.getArchetype();

                    var componentType = ComponentType.detectComponentType(component);
                    var componentId = engine.getComponent(componentType).id();
                    var type2 = engine.getComponent(component(C2.class)).id();
                    var type3 = engine.getComponent(component(C3.class)).id();

                    archetype.createEntity(42, new Object[0]);

                    var accessor = archetype.getEntityData().getAccessor(42);

                    // Verify before add
                    assertThat(accessor.<Object>getPendingComponent(componentId)).as("getPendingComponent must return null if no pending components").isNull();
                    assertThat(accessor.<C2>getPendingComponent(type2)).as("getPendingComponent must return null if no pending components").isNull();
                    assertThat(accessor.<C3>getPendingComponent(type3)).as("getPendingComponent must return null if no pending components").isNull();

                    // Call
                    engine.add(42, new Object[] { component });

                    // Verify
                    assertThat(accessor.<Object>getPendingComponent(componentId)).as("getPendingComponent must return pending component").isSameAs(component);
                    assertThat(accessor.<C2>getPendingComponent(type2)).as("getPendingComponent must return null if no pending components").isNull();
                    assertThat(accessor.<C3>getPendingComponent(type3)).as("getPendingComponent must return null if no pending components").isNull();
                }

                @ParameterizedTest
                @MethodSource("nonExclusiveRelations")
                void testNonExclusiveRelations_EmptyArchetype(Relation<?> component) {
                    var archetype = engine.getArchetype();

                    var componentType = ComponentType.detectComponentType(component);
                    var componentId = engine.getComponent(componentType).id();
                    var type1 = engine.getComponent(relation(C1.class, C3.class)).id();

                    archetype.createEntity(42, new Object[0]);

                    var accessor = archetype.getEntityData().getAccessor(42);

                    // Verify before add
                    assertThat(accessor.<Object>getPendingComponent(componentId)).as("getPendingComponent must return null if no pending components").isNull();
                    assertThat(accessor.<C1>getPendingComponent(type1)).as("getPendingComponent must return null if no pending components").isNull();

                    // Call
                    engine.add(42, new Object[] { component });

                    // Verify
                    assertThat(accessor.<Object>getPendingComponent(componentId))
                            .as("getPendingComponent must return relations with pending relation").asInstanceOf(InstanceOfAssertFactories.ITERABLE)
                            .as("getPendingComponent must return relations with pending relation").containsExactly(component);

                    assertThat(accessor.<C1>getPendingComponent(type1)).as("getPendingComponent must return null for other components").isNull();
                }

                @ParameterizedTest
                @MethodSource("simpleComponents")
                void testSimpleComponents_OneComponentArchtype(Object component) {
                    var archetype = engine.getArchetype(component(C3.class));

                    var componentType = ComponentType.detectComponentType(component);
                    var componentId = engine.getComponent(componentType).id();
                    var type2 = engine.getComponent(component(C2.class)).id();
                    var type3 = engine.getComponent(component(C3.class)).id();

                    archetype.createEntity(42, new Object[] { new C3() });

                    var accessor = archetype.getEntityData().getAccessor(42);

                    // Verify before add
                    assertThat(accessor.<Object>getPendingComponent(componentId)).as("getPendingComponent must return null if no pending components").isNull();
                    assertThat(accessor.<C2>getPendingComponent(type2)).as("getPendingComponent must return null if no pending components").isNull();
                    assertThat(accessor.<C3>getPendingComponent(type3)).as("getPendingComponent must return null if component part of archetype").isNull();

                    // Call
                    engine.add(42, new Object[] { component });

                    // Verify
                    assertThat(accessor.<Object>getPendingComponent(componentId)).as("getPendingComponent must return pending component").isSameAs(component);
                    assertThat(accessor.<C2>getPendingComponent(type2)).as("getPendingComponent must return null if no pending components").isNull();
                    assertThat(accessor.<C3>getPendingComponent(type3)).as("getPendingComponent must return null if component part of archetype").isNull();
                }

                @ParameterizedTest
                @MethodSource("nonExclusiveRelations")
                void testNonExclusiveRelations_OneComponentArchetype(Relation<?> component) {
                    var archetype = engine.getArchetype(relation(C3.class, C3.class));

                    var componentType = ComponentType.detectComponentType(component);
                    var componentId = engine.getComponent(componentType).id();
                    var componentRelationType = engine.getComponent(relation(C3.class, C3.class)).id();

                    archetype.createEntity(42, new Object[] { Relation.create(new C3(), new C3()) });

                    var accessor = archetype.getEntityData().getAccessor(42);

                    // Verify before add
                    assertThat(accessor.<Object>getPendingComponent(componentId)).as("getPendingComponent must return null if no pending components").isNull();
                    assertThat(accessor.<Object>getPendingComponent(componentRelationType)).as("getPendingComponent must return null if relation part of archetype").isNull();

                    // Call
                    engine.add(42, new Object[] { component, new C3() });

                    // Verify
                    assertThat(accessor.<Object>getPendingComponent(componentId))
                            .as("getPendingComponent must return relations with pending relation").asInstanceOf(InstanceOfAssertFactories.ITERABLE)
                            .as("getPendingComponent must return relations with pending relation").containsExactly(component);

                    assertThat(accessor.<Object>getPendingComponent(componentRelationType)).as("getPendingComponent must return null if relation part of archetype").isNull();
                }

                private static Stream<Object> simpleComponents() {
                    return Stream.of(
                            new C1(),
                            Relation.create(E1.INSTANCE, new C2()),
                            Relation.create(E1.INSTANCE, 42));
                }

                private static Stream<Object> nonExclusiveRelations() {
                    return Stream.of(
                            Relation.create(new C1(), new C2()),
                            Relation.create(new C1(), 42));
                }

            }

        }

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

    record C3(int value) {
        public C3() {
            this(0);
        }
    }

    enum E1 implements Exclusive {
        INSTANCE
    }

    enum E2 implements Exclusive {
        INSTANCE
    }

}
