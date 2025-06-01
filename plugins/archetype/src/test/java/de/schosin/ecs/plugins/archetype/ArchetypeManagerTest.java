package de.schosin.ecs.plugins.archetype;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import de.schosin.ecs.api.Pooled;
import de.schosin.ecs.api.components.Relation;
import de.schosin.ecs.api.components.types.ComponentType.RegularComponentType;
import de.schosin.ecs.engine.utils.ArrayUtils;
import de.schosin.ecs.test.AbstractEcsTest;

class ArchetypeManagerTest extends AbstractEcsTest<ArchetypeWorld> {

    @Nested
    class Archetype1Test {

        @Nested
        class SimpleArchetypeTest extends AbstractTest {
            @Override
            protected <T1> Archetype1<T1> createArchetype(RegularComponentType<T1, ?> component1) {
                return world.createArchetype(component1);
            }
        }

        @Nested
        class WithComponentsTest extends AbstractTest {

            @Override
            protected <T1> Archetype1<T1> createArchetype(RegularComponentType<T1, ?> component1) {
                return world.createArchetype(component1).with(E1.INSTANCE, E2.INSTANCE);
            }

            protected void verifyComponentMaskHasComponents(int entityId, Class<?>... components) {
                var expected = ArrayUtils.concat(Class.class, components, E1.class, E2.class);

                ArchetypeManagerTest.this.verifyComponentMaskHasComponents(entityId, expected);
            }

            @Test
            void testPooledComponents_Throws() {
                assertThatThrownBy(() -> world.createArchetype(C1.class).with(new D1()))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContainingAll("D1", "cannot implement Pooled");
            }

            @Test
            void testDuplicateComponents_Throws() {
                assertThatThrownBy(() -> world.createArchetype(C1.class).with(new C1()))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContainingAll("C1", "already defined");

                assertThatThrownBy(() -> world.createArchetype(C1.class).with(E1.INSTANCE, E2.INSTANCE, E1.INSTANCE))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContainingAll("E1", "already defined");
            }

        }

        abstract class AbstractTest {

            protected final <T1> Archetype1<T1> createArchetype(Class<T1> component1) {
                return createArchetype(component(component1));
            }

            protected abstract <T1> Archetype1<T1> createArchetype(RegularComponentType<T1, ?> component1);

            @Test
            void testArchetype() {
                var archetype = createArchetype(C1.class);

                var entityId = archetype.create(new C1());
                verifyHasComponents(entityId, C1.class);
                verifyComponentMaskHasComponents(entityId, C1.class);
            }

            @Test
            void testNullInstance() {
                var archetype = createArchetype(C1.class);

                assertThatThrownBy(() -> archetype.create((C1) null)).isNotNull();
            }

            @Test
            void testBatch() {
                var archetype = createArchetype(C1.class);

                var entityIds = archetype.createBatch(10, () -> new C1());
                assertThat(entityIds.getSize()).isEqualTo(10);

                for (var iter = entityIds.iterator(); iter.hasNext();) {
                    var entityId = iter.nextInt();
                    verifyHasComponents(entityId, C1.class);
                    verifyComponentMaskHasComponents(entityId, C1.class);
                }
            }

            @Test
            void testBatchChangeHandler() {
                var count = 10;
                var archetype = createArchetype(C1.class);

                verify(verify -> {
                    for (int i = 0; i < count; i++) {
                        verify.expectInserted(C1.class);
                    }

                    archetype.createBatch(count, () -> new C1());
                });
            }

            @Test
            void testBatch_NullInstances() {
                var archetype = createArchetype(C1.class);

                assertThatThrownBy(() -> archetype.createBatch(10, () -> null))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContaining("cannot be null");
            }

            @Test
            void testGetInstance() {
                var archetype = createArchetype(C1.class);
                assertThat(archetype.getInstance(D1.class)).isNotNull();
                assertThat(archetype.getInstance(D2.class)).isNotNull();
            }

            @Nested
            class ComponentRelationTest {

                @Test
                void testComponentRelation() {
                    var type = relation(C1.class, Target.class);
                    var archetype = createArchetype(type);

                    verify(verify -> {
                        verify.expectInserted(type);
                        verify.expectNoMoreInserted();

                        var relation1 = Relation.create(new C1(), new Target(1));

                        var entityId = archetype.create(relation1);
                        verifyHasComponents(entityId, type);
                        verifyComponentMaskHasComponents(entityId, type);
                    });
                }

            }

            @Nested
            class EntityRelationTest {

                @Test
                void testEntityRelation() {
                    var type = relation(C1.class);
                    var archetype = createArchetype(type);

                    var target = world.createEntity();

                    verify(verify -> {
                        verify.expectInserted(type);
                        verify.expectNoMoreInserted();

                        var relation1 = Relation.create(new C1(), target);

                        var entityId = archetype.create(relation1);
                        verifyHasComponents(entityId, type);
                        verifyComponentMaskHasComponents(entityId, type);
                    });
                }

            }

        }

    }

    @Nested
    class Archetype2Test {

        @Nested
        class SimpleArchetypeTest extends AbstractTest {
            @Override
            protected <T1, T2> Archetype2<T1, T2> createArchetype(RegularComponentType<T1, ?> component1, RegularComponentType<T2, ?> component2) {
                return world.createArchetype(component1, component2);
            }
        }

        @Nested
        class WithComponentsTest extends AbstractTest {

            @Override
            protected <T1, T2> Archetype2<T1, T2> createArchetype(RegularComponentType<T1, ?> component1, RegularComponentType<T2, ?> component2) {
                return world.createArchetype(component1, component2).with(E1.INSTANCE, E2.INSTANCE);
            }

            protected void verifyComponentMaskHasComponents(int entityId, Class<?>... components) {
                var expected = ArrayUtils.concat(Class.class, components, E1.class, E2.class);

                ArchetypeManagerTest.this.verifyComponentMaskHasComponents(entityId, expected);
            }

            @Test
            void testPooledComponents_Throws() {
                assertThatThrownBy(() -> world.createArchetype(C1.class, C2.class).with(new D1()))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContainingAll("D1", "cannot implement Pooled");
            }

            @Test
            void testDuplicateComponents_Throws() {
                assertThatThrownBy(() -> world.createArchetype(C1.class, C2.class).with(new C1()))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContainingAll("C1", "already defined");

                assertThatThrownBy(() -> world.createArchetype(C1.class, C2.class).with(E1.INSTANCE, E2.INSTANCE, E1.INSTANCE))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContainingAll("E1", "already defined");
            }

        }

        abstract class AbstractTest {

            protected final <T1, T2> Archetype2<T1, T2> createArchetype(Class<T1> component1, Class<T2> component2) {
                return createArchetype(component(component1), component(component2));
            }

            protected abstract <T1, T2> Archetype2<T1, T2> createArchetype(RegularComponentType<T1, ?> component1, RegularComponentType<T2, ?> component2);

            @Test
            void testArchetype() {
                var archetype = createArchetype(C1.class, C2.class);

                var entityId = archetype.create(new C1(), new C2());
                verifyHasComponents(entityId, C1.class, C2.class);
                verifyComponentMaskHasComponents(entityId, C1.class, C2.class);
            }

            @Test
            void testNullInstance() {
                var archetype = createArchetype(C1.class, C2.class);

                assertThatThrownBy(() -> archetype.create(null, new C2())).isNotNull();
                assertThatThrownBy(() -> archetype.create(new C1(), null)).isNotNull();
            }

            @Test
            void testBatch() {
                var archetype = createArchetype(C1.class, C2.class);

                var entityIds = archetype.createBatch(10, factory -> factory.create(new C1(), new C2()));
                assertThat(entityIds.getSize()).isEqualTo(10);

                for (var iter = entityIds.iterator(); iter.hasNext();) {
                    var entityId = iter.nextInt();
                    verifyHasComponents(entityId, C1.class, C2.class);
                    verifyComponentMaskHasComponents(entityId, C1.class, C2.class);
                }
            }

            @Test
            void testBatchChangeHandler() {
                var count = 10;
                var archetype = createArchetype(C1.class, C2.class);

                verify(verify -> {
                    for (int i = 0; i < count; i++) {
                        verify.expectInserted(C1.class, C2.class);
                    }

                    archetype.createBatch(count, factory -> factory.create(new C1(), new C2()));
                });
            }

            @Test
            void testBatch_NullInstances() {
                var archetype = createArchetype(C1.class, C2.class);

                assertThatThrownBy(() -> archetype.createBatch(10, factory -> factory.create(null, new C2())))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContainingAll("Component 1", "cannot be null");
                assertThatThrownBy(() -> archetype.createBatch(10, factory -> factory.create(new C1(), null)))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContainingAll("Component 2", "cannot be null");
            }

            @Test
            void testBatchInitializeNotCalled() {
                var archetype = createArchetype(C1.class, C2.class);

                assertThatThrownBy(() -> archetype.createBatch(10, factory -> null))
                        .isInstanceOf(NullPointerException.class)
                        .hasMessageContaining("return value cannot be null");
            }

            @Test
            void testGetInstance() {
                var archetype = createArchetype(C1.class, C2.class);
                assertThat(archetype.getInstance(D1.class)).isNotNull();
                assertThat(archetype.getInstance(D2.class)).isNotNull();
            }

            @Nested
            class ComponentRelationTest {

                @Test
                void testComponentRelation() {
                    var type = relation(C1.class, Target.class);
                    var archetype = createArchetype(type, type(C2.class));

                    var expected = new RegularComponentType<?, ?>[] { type, type(C2.class) };

                    verify(verify -> {
                        verify.expectInserted(expected);
                        verify.expectNoMoreInserted();

                        var relation1 = Relation.create(new C1(), new Target(1));

                        var entityId = archetype.create(relation1, new C2());
                        verifyHasComponents(entityId, expected);
                        verifyComponentMaskHasComponents(entityId, expected);
                    });
                }

                @Test
                void testMultipleComponentRelations_DifferentRelationships() {
                    var type1 = relation(C1.class, Target.class);
                    var type2 = relation(C2.class, Target.class);

                    var archetype = createArchetype(type1, type2);

                    var expected = new RegularComponentType<?, ?>[] { type1, type2 };

                    verify(verify -> {
                        verify.expectInserted(expected);
                        verify.expectNoMoreInserted();

                        var relation1 = Relation.create(new C1(), new Target(1));
                        var relation2 = Relation.create(new C2(), new Target(2));

                        var entityId = archetype.create(relation1, relation2);
                        verifyHasComponents(entityId, expected);
                        verifyComponentMaskHasComponents(entityId, expected);
                    });
                }

                @Test
                void testMultipleComponentRelations_DifferentTargets() {
                    var type1 = relation(C1.class, Target.class);
                    var type2 = relation(C1.class, Target2.class);

                    var archetype = createArchetype(type1, type2);

                    var expected = new RegularComponentType<?, ?>[] { type1, type2 };

                    verify(verify -> {
                        verify.expectInserted(expected);
                        verify.expectNoMoreInserted();

                        var relation1 = Relation.create(new C1(), new Target(1));
                        var relation2 = Relation.create(new C1(), new Target2(2));

                        var entityId = archetype.create(relation1, relation2);
                        verifyHasComponents(entityId, expected);
                        verifyComponentMaskHasComponents(entityId, expected);
                    });
                }

                @Test
                void testMultipleComponentRelations_SameType_TargetsEqual() {
                    var type = relation(C1.class, Target.class);
                    var archetype = createArchetype(type, type);

                    var expected = new RegularComponentType<?, ?>[] { type };

                    verify(verify -> {
                        verify.expectInserted(expected);
                        verify.expectNoMoreInserted();

                        var relationship = new C1();
                        var target1 = new Target(1);
                        var target2 = new Target(1);

                        var relation1 = Relation.create(relationship, target1);
                        var relation2 = Relation.create(relationship, target2);

                        var entityId = archetype.create(relation1, relation2);
                        verifyHasComponents(entityId, expected);
                        verifyComponentMaskHasComponents(entityId, expected);

                        var relations = relationMapperManager.getComponentRelationMapper(type).get(entityId);
                        assertThat(relations)
                                .extracting("relationship", "target")
                                .containsExactlyInAnyOrder(
                                        tuple(relationship, target2));
                    });
                }

                @Test
                void testMultipleComponentRelations_SameType_TargetsNotEqual() {
                    var type = relation(C1.class, Target.class);
                    var archetype = createArchetype(type, type);

                    var expected = new RegularComponentType<?, ?>[] { type };

                    verify(verify -> {
                        verify.expectInserted(expected);
                        verify.expectNoMoreInserted();

                        var relationship = new C1();
                        var target1 = new Target(1);
                        var target2 = new Target(2);

                        var relation1 = Relation.create(relationship, target1);
                        var relation2 = Relation.create(relationship, target2);

                        var entityId = archetype.create(relation1, relation2);
                        verifyHasComponents(entityId, expected);
                        verifyComponentMaskHasComponents(entityId, expected);

                        var relations = relationMapperManager.getComponentRelationMapper(type).get(entityId);
                        assertThat(relations)
                                .extracting("relationship", "target")
                                .containsExactlyInAnyOrder(
                                        tuple(relationship, target1),
                                        tuple(relationship, target2));
                    });
                }

            }

            @Nested
            class EntityRelationTest {

                @Test
                void testComponentRelation() {
                    var type = relation(C1.class);
                    var archetype = createArchetype(type, type(C2.class));

                    var expected = new RegularComponentType<?, ?>[] { type, type(C2.class) };

                    var target = world.createEntity();

                    verify(verify -> {
                        verify.expectInserted(expected);
                        verify.expectNoMoreInserted();

                        var relation1 = Relation.create(new C1(), target);

                        var entityId = archetype.create(relation1, new C2());
                        verifyHasComponents(entityId, expected);
                        verifyComponentMaskHasComponents(entityId, expected);
                    });
                }

                @Test
                void testMultipleComponentRelations_DifferentRelationships() {
                    var type1 = relation(C1.class);
                    var type2 = relation(C2.class);

                    var archetype = createArchetype(type1, type2);

                    var expected = new RegularComponentType<?, ?>[] { type1, type2 };

                    var target1 = world.createEntity();
                    var target2 = world.createEntity();

                    verify(verify -> {
                        verify.expectInserted(expected);
                        verify.expectNoMoreInserted();

                        var relation1 = Relation.create(new C1(), target1);
                        var relation2 = Relation.create(new C2(), target2);

                        var entityId = archetype.create(relation1, relation2);
                        verifyHasComponents(entityId, expected);
                        verifyComponentMaskHasComponents(entityId, expected);
                    });
                }

                @Test
                void testMultipleComponentRelations_SameType_TargetsEqual() {
                    var type = relation(C1.class);
                    var archetype = createArchetype(type, type);

                    var expected = new RegularComponentType<?, ?>[] { type };

                    var target = world.createEntity();

                    verify(verify -> {
                        verify.expectInserted(expected);
                        verify.expectNoMoreInserted();

                        var relationship = new C1();

                        var relation1 = Relation.create(relationship, target);
                        var relation2 = Relation.create(relationship, target);

                        var entityId = archetype.create(relation1, relation2);
                        verifyHasComponents(entityId, expected);
                        verifyComponentMaskHasComponents(entityId, expected);

                        var relations = relationMapperManager.getEntityRelationMapper(type).get(entityId);
                        assertThat(relations)
                                .extracting("relationship", "target")
                                .containsExactlyInAnyOrder(
                                        tuple(relationship, target));
                    });
                }

                @Test
                void testMultipleComponentRelations_SameType_TargetsNotEqual() {
                    var type = relation(C1.class);
                    var archetype = createArchetype(type, type);

                    var expected = new RegularComponentType<?, ?>[] { type };

                    var target1 = world.createEntity();
                    var target2 = world.createEntity();

                    verify(verify -> {
                        verify.expectInserted(expected);
                        verify.expectNoMoreInserted();

                        var relationship = new C1();

                        var relation1 = Relation.create(relationship, target1);
                        var relation2 = Relation.create(relationship, target2);

                        var entityId = archetype.create(relation1, relation2);
                        verifyHasComponents(entityId, expected);
                        verifyComponentMaskHasComponents(entityId, expected);

                        var relations = relationMapperManager.getEntityRelationMapper(type).get(entityId);
                        assertThat(relations)
                                .extracting("relationship", "target")
                                .containsExactlyInAnyOrder(
                                        tuple(relationship, target1),
                                        tuple(relationship, target2));
                    });
                }

            }

        }

    }

    @Nested
    class Archetype3Test {

        @Nested
        class SimpleArchetypeTest extends AbstractTest {
            @Override
            protected <T1, T2, T3> Archetype3<T1, T2, T3> createArchetype(RegularComponentType<T1, ?> component1, RegularComponentType<T2, ?> component2, RegularComponentType<T3, ?> component3) {
                return world.createArchetype(component1, component2, component3);
            }
        }

        @Nested
        class WithComponentsTest extends AbstractTest {

            @Override
            protected <T1, T2, T3> Archetype3<T1, T2, T3> createArchetype(RegularComponentType<T1, ?> component1, RegularComponentType<T2, ?> component2, RegularComponentType<T3, ?> component3) {
                return world.createArchetype(component1, component2, component3).with(E1.INSTANCE, E2.INSTANCE);
            }

            protected void verifyComponentMaskHasComponents(int entityId, Class<?>... components) {
                var expected = ArrayUtils.concat(Class.class, components, E1.class, E2.class);

                ArchetypeManagerTest.this.verifyComponentMaskHasComponents(entityId, expected);
            }

            @Test
            void testPooledComponents_Throws() {
                assertThatThrownBy(() -> world.createArchetype(C1.class, C2.class, C3.class).with(new D1()))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContainingAll("D1", "cannot implement Pooled");
            }

            @Test
            void testDuplicateComponents_Throws() {
                assertThatThrownBy(() -> world.createArchetype(C1.class, C2.class, C3.class).with(new C1()))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContainingAll("C1", "already defined");

                assertThatThrownBy(() -> world.createArchetype(C1.class, C2.class, C3.class).with(E1.INSTANCE, E2.INSTANCE, E1.INSTANCE))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContainingAll("E1", "already defined");
            }

        }

        abstract class AbstractTest {

            protected final <T1, T2, T3> Archetype3<T1, T2, T3> createArchetype(Class<T1> component1, Class<T2> component2, Class<T3> component3) {
                return createArchetype(component(component1), component(component2), component(component3));
            }

            protected abstract <T1, T2, T3> Archetype3<T1, T2, T3> createArchetype(RegularComponentType<T1, ?> component1, RegularComponentType<T2, ?> component2,
                    RegularComponentType<T3, ?> component3);

            @Test
            void testArchetype() {
                var archetype = createArchetype(C1.class, C2.class, C3.class);

                var entityId = archetype.create(new C1(), new C2(), new C3());
                verifyHasComponents(entityId, C1.class, C2.class, C3.class);
                verifyComponentMaskHasComponents(entityId, C1.class, C2.class, C3.class);
            }

            @Test
            void testNullInstance() {
                var archetype = createArchetype(C1.class, C2.class, C3.class);

                assertThatThrownBy(() -> archetype.create(null, new C2(), new C3())).isNotNull();
                assertThatThrownBy(() -> archetype.create(new C1(), null, new C3())).isNotNull();
                assertThatThrownBy(() -> archetype.create(new C1(), new C2(), null)).isNotNull();
            }

            @Test
            void testBatch() {
                var archetype = createArchetype(C1.class, C2.class, C3.class);

                var entityIds = archetype.createBatch(10, factory -> factory.create(new C1(), new C2(), new C3()));
                assertThat(entityIds.getSize()).isEqualTo(10);

                for (var iter = entityIds.iterator(); iter.hasNext();) {
                    var entityId = iter.nextInt();
                    verifyHasComponents(entityId, C1.class, C2.class, C3.class);
                    verifyComponentMaskHasComponents(entityId, C1.class, C2.class, C3.class);
                }
            }

            @Test
            void testBatchChangeHandler() {
                var count = 10;
                var archetype = createArchetype(C1.class, C2.class, C3.class);

                verify(verify -> {
                    for (int i = 0; i < count; i++) {
                        verify.expectInserted(C1.class, C2.class, C3.class);
                    }

                    archetype.createBatch(count, factory -> factory.create(new C1(), new C2(), new C3()));
                });
            }

            @Test
            void testBatch_NullInstances() {
                var archetype = createArchetype(C1.class, C2.class, C3.class);

                assertThatThrownBy(() -> archetype.createBatch(10, factory -> factory.create(null, new C2(), new C3())))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContainingAll("Component 1", "cannot be null");
                assertThatThrownBy(() -> archetype.createBatch(10, factory -> factory.create(new C1(), null, new C3())))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContainingAll("Component 2", "cannot be null");
                assertThatThrownBy(() -> archetype.createBatch(10, factory -> factory.create(new C1(), new C2(), null)))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContainingAll("Component 3", "cannot be null");
            }

            @Test
            void testBatchInitializeNotCalled() {
                var archetype = createArchetype(C1.class, C2.class, C3.class);

                assertThatThrownBy(() -> archetype.createBatch(10, factory -> null))
                        .isInstanceOf(NullPointerException.class)
                        .hasMessageContaining("return value cannot be null");
            }

            @Test
            void testGetInstance() {
                var archetype = createArchetype(C1.class, C2.class, C3.class);
                assertThat(archetype.getInstance(D1.class)).isNotNull();
                assertThat(archetype.getInstance(D2.class)).isNotNull();
            }

            @Nested
            class ComponentRelationTest {

                @Test
                void testComponentRelation() {
                    var type = relation(C1.class, Target.class);
                    var archetype = createArchetype(type, type(C2.class), type(C3.class));

                    var expected = new RegularComponentType<?, ?>[] { type, type(C2.class), type(C3.class) };

                    verify(verify -> {
                        verify.expectInserted(expected);
                        verify.expectNoMoreInserted();

                        var relation1 = Relation.create(new C1(), new Target(1));

                        var entityId = archetype.create(relation1, new C2(), new C3());
                        verifyHasComponents(entityId, expected);
                        verifyComponentMaskHasComponents(entityId, expected);
                    });
                }

                @Test
                void testMultipleComponentRelations_DifferentRelationships() {
                    var type1 = relation(C1.class, Target.class);
                    var type2 = relation(C2.class, Target.class);

                    var archetype = createArchetype(type1, type2, type(C3.class));

                    var expected = new RegularComponentType<?, ?>[] { type1, type2, type(C3.class) };

                    verify(verify -> {
                        verify.expectInserted(expected);
                        verify.expectNoMoreInserted();

                        var relation1 = Relation.create(new C1(), new Target(1));
                        var relation2 = Relation.create(new C2(), new Target(2));

                        var entityId = archetype.create(relation1, relation2, new C3());
                        verifyHasComponents(entityId, expected);
                        verifyComponentMaskHasComponents(entityId, expected);
                    });
                }

                @Test
                void testMultipleComponentRelations_DifferentTargets() {
                    var type1 = relation(C1.class, Target.class);
                    var type2 = relation(C1.class, Target2.class);

                    var archetype = createArchetype(type1, type2, type(C3.class));

                    var expected = new RegularComponentType<?, ?>[] { type1, type2, type(C3.class) };

                    verify(verify -> {
                        verify.expectInserted(expected);
                        verify.expectNoMoreInserted();

                        var relation1 = Relation.create(new C1(), new Target(1));
                        var relation2 = Relation.create(new C1(), new Target2(2));

                        var entityId = archetype.create(relation1, relation2, new C3());
                        verifyHasComponents(entityId, expected);
                        verifyComponentMaskHasComponents(entityId, expected);
                    });
                }

                @Test
                void testMultipleComponentRelations_SameType_TargetsEqual() {
                    var type = relation(C1.class, Target.class);
                    var archetype = createArchetype(type, type, type(C3.class));

                    var expected = new RegularComponentType<?, ?>[] { type, type(C3.class) };

                    verify(verify -> {
                        verify.expectInserted(expected);
                        verify.expectNoMoreInserted();

                        var relationship = new C1();
                        var target1 = new Target(1);
                        var target2 = new Target(1);

                        var relation1 = Relation.create(relationship, target1);
                        var relation2 = Relation.create(relationship, target2);

                        var entityId = archetype.create(relation1, relation2, new C3());
                        verifyHasComponents(entityId, expected);
                        verifyComponentMaskHasComponents(entityId, expected);

                        var relations = relationMapperManager.getComponentRelationMapper(type).get(entityId);
                        assertThat(relations)
                                .extracting("relationship", "target")
                                .containsExactlyInAnyOrder(
                                        tuple(relationship, target2));
                    });
                }

                @Test
                void testMultipleComponentRelations_SameType_TargetsNotEqual() {
                    var type = relation(C1.class, Target.class);
                    var archetype = createArchetype(type, type, type(C3.class));

                    var expected = new RegularComponentType<?, ?>[] { type, type(C3.class) };

                    verify(verify -> {
                        verify.expectInserted(expected);
                        verify.expectNoMoreInserted();

                        var relationship = new C1();
                        var target1 = new Target(1);
                        var target2 = new Target(2);

                        var relation1 = Relation.create(relationship, target1);
                        var relation2 = Relation.create(relationship, target2);

                        var entityId = archetype.create(relation1, relation2, new C3());
                        verifyHasComponents(entityId, expected);
                        verifyComponentMaskHasComponents(entityId, expected);

                        var relations = relationMapperManager.getComponentRelationMapper(type).get(entityId);
                        assertThat(relations)
                                .extracting("relationship", "target")
                                .containsExactlyInAnyOrder(
                                        tuple(relationship, target1),
                                        tuple(relationship, target2));
                    });
                }

            }

            @Nested
            class EntityRelationTest {

                @Test
                void testComponentRelation() {
                    var type = relation(C1.class);
                    var archetype = createArchetype(type, type(C2.class), type(C3.class));

                    var expected = new RegularComponentType<?, ?>[] { type, type(C2.class), type(C3.class) };

                    var target = world.createEntity();

                    verify(verify -> {
                        verify.expectInserted(expected);
                        verify.expectNoMoreInserted();

                        var relation1 = Relation.create(new C1(), target);

                        var entityId = archetype.create(relation1, new C2(), new C3());
                        verifyHasComponents(entityId, expected);
                        verifyComponentMaskHasComponents(entityId, expected);
                    });
                }

                @Test
                void testMultipleComponentRelations_DifferentRelationships() {
                    var type1 = relation(C1.class);
                    var type2 = relation(C2.class);

                    var archetype = createArchetype(type1, type2, type(C3.class));

                    var expected = new RegularComponentType<?, ?>[] { type1, type2, type(C3.class) };

                    var target1 = world.createEntity();
                    var target2 = world.createEntity();

                    verify(verify -> {
                        verify.expectInserted(expected);
                        verify.expectNoMoreInserted();

                        var relation1 = Relation.create(new C1(), target1);
                        var relation2 = Relation.create(new C2(), target2);

                        var entityId = archetype.create(relation1, relation2, new C3());
                        verifyHasComponents(entityId, expected);
                        verifyComponentMaskHasComponents(entityId, expected);
                    });
                }

                @Test
                void testMultipleComponentRelations_SameType_TargetsEqual() {
                    var type = relation(C1.class);
                    var archetype = createArchetype(type, type, type(C3.class));

                    var expected = new RegularComponentType<?, ?>[] { type, type(C3.class) };

                    var target = world.createEntity();

                    verify(verify -> {
                        verify.expectInserted(expected);
                        verify.expectNoMoreInserted();

                        var relationship = new C1();

                        var relation1 = Relation.create(relationship, target);
                        var relation2 = Relation.create(relationship, target);

                        var entityId = archetype.create(relation1, relation2, new C3());
                        verifyHasComponents(entityId, expected);
                        verifyComponentMaskHasComponents(entityId, expected);

                        var relations = relationMapperManager.getEntityRelationMapper(type).get(entityId);
                        assertThat(relations)
                                .extracting("relationship", "target")
                                .containsExactlyInAnyOrder(
                                        tuple(relationship, target));
                    });
                }

                @Test
                void testMultipleComponentRelations_SameType_TargetsNotEqual() {
                    var type = relation(C1.class);
                    var archetype = createArchetype(type, type, type(C3.class));

                    var expected = new RegularComponentType<?, ?>[] { type, type(C3.class) };

                    var target1 = world.createEntity();
                    var target2 = world.createEntity();

                    verify(verify -> {
                        verify.expectInserted(expected);
                        verify.expectNoMoreInserted();

                        var relationship = new C1();

                        var relation1 = Relation.create(relationship, target1);
                        var relation2 = Relation.create(relationship, target2);

                        var entityId = archetype.create(relation1, relation2, new C3());
                        verifyHasComponents(entityId, expected);
                        verifyComponentMaskHasComponents(entityId, expected);

                        var relations = relationMapperManager.getEntityRelationMapper(type).get(entityId);
                        assertThat(relations)
                                .extracting("relationship", "target")
                                .containsExactlyInAnyOrder(
                                        tuple(relationship, target1),
                                        tuple(relationship, target2));
                    });
                }

            }

        }

    }

    @Nested
    class Archetype4Test {

        @Nested
        class SimpleArchetypeTest extends AbstractTest {
            @Override
            protected <T1, T2, T3, T4> Archetype4<T1, T2, T3, T4> createArchetype(RegularComponentType<T1, ?> component1, RegularComponentType<T2, ?> component2,
                    RegularComponentType<T3, ?> component3, RegularComponentType<T4, ?> component4) {

                return world.createArchetype(component1, component2, component3, component4);
            }
        }

        @Nested
        class WithComponentsTest extends AbstractTest {

            @Override
            protected <T1, T2, T3, T4> Archetype4<T1, T2, T3, T4> createArchetype(RegularComponentType<T1, ?> component1, RegularComponentType<T2, ?> component2,
                    RegularComponentType<T3, ?> component3, RegularComponentType<T4, ?> component4) {

                return world.createArchetype(component1, component2, component3, component4).with(E1.INSTANCE, E2.INSTANCE);
            }

            protected void verifyComponentMaskHasComponents(int entityId, Class<?>... components) {
                var expected = ArrayUtils.concat(Class.class, components, E1.class, E2.class);

                ArchetypeManagerTest.this.verifyComponentMaskHasComponents(entityId, expected);
            }

            @Test
            void testPooledComponents_Throws() {
                assertThatThrownBy(() -> world.createArchetype(C1.class, C2.class, C3.class, C4.class).with(new D1()))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContainingAll("D1", "cannot implement Pooled");
            }

            @Test
            void testDuplicateComponents_Throws() {
                assertThatThrownBy(() -> world.createArchetype(C1.class, C2.class, C3.class, C4.class).with(new C1()))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContainingAll("C1", "already defined");

                assertThatThrownBy(() -> world.createArchetype(C1.class, C2.class, C3.class, C4.class).with(E1.INSTANCE, E2.INSTANCE, E1.INSTANCE))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContainingAll("E1", "already defined");
            }

        }

        abstract class AbstractTest {

            protected final <T1, T2, T3, T4> Archetype4<T1, T2, T3, T4> createArchetype(Class<T1> component1, Class<T2> component2, Class<T3> component3, Class<T4> component4) {
                return createArchetype(component(component1), component(component2), component(component3), component(component4));
            }

            protected abstract <T1, T2, T3, T4> Archetype4<T1, T2, T3, T4> createArchetype(RegularComponentType<T1, ?> component1, RegularComponentType<T2, ?> component2,
                    RegularComponentType<T3, ?> component3, RegularComponentType<T4, ?> component4);

            @Test
            void testArchetype() {
                var archetype = createArchetype(C1.class, C2.class, C3.class, C4.class);

                var entityId = archetype.create(new C1(), new C2(), new C3(), new C4());
                verifyHasComponents(entityId, C1.class, C2.class, C3.class, C4.class);
                verifyComponentMaskHasComponents(entityId, C1.class, C2.class, C3.class, C4.class);
            }

            @Test
            void testNullInstance() {
                var archetype = createArchetype(C1.class, C2.class, C3.class, C4.class);

                assertThatThrownBy(() -> archetype.create(null, new C2(), new C3(), new C4())).isNotNull();
                assertThatThrownBy(() -> archetype.create(new C1(), null, new C3(), new C4())).isNotNull();
                assertThatThrownBy(() -> archetype.create(new C1(), new C2(), null, new C4())).isNotNull();
                assertThatThrownBy(() -> archetype.create(new C1(), new C2(), new C3(), null)).isNotNull();
            }

            @Test
            void testBatch() {
                var archetype = createArchetype(C1.class, C2.class, C3.class, C4.class);

                var entityIds = archetype.createBatch(10, factory -> factory.create(new C1(), new C2(), new C3(), new C4()));
                assertThat(entityIds.getSize()).isEqualTo(10);

                for (var iter = entityIds.iterator(); iter.hasNext();) {
                    var entityId = iter.nextInt();
                    verifyHasComponents(entityId, C1.class, C2.class, C3.class, C4.class);
                    verifyComponentMaskHasComponents(entityId, C1.class, C2.class, C3.class, C4.class);
                }
            }

            @Test
            void testBatchChangeHandler() {
                var count = 10;
                var archetype = createArchetype(C1.class, C2.class, C3.class, C4.class);

                verify(verify -> {
                    for (int i = 0; i < count; i++) {
                        verify.expectInserted(C1.class, C2.class, C3.class, C4.class);
                    }

                    archetype.createBatch(count, factory -> factory.create(new C1(), new C2(), new C3(), new C4()));
                });
            }

            @Test
            void testBatch_NullInstances() {
                var archetype = createArchetype(C1.class, C2.class, C3.class, C4.class);

                assertThatThrownBy(() -> archetype.createBatch(10, factory -> factory.create(null, new C2(), new C3(), new C4())))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContainingAll("Component 1", "cannot be null");
                assertThatThrownBy(() -> archetype.createBatch(10, factory -> factory.create(new C1(), null, new C3(), new C4())))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContainingAll("Component 2", "cannot be null");
                assertThatThrownBy(() -> archetype.createBatch(10, factory -> factory.create(new C1(), new C2(), null, new C4())))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContainingAll("Component 3", "cannot be null");
                assertThatThrownBy(() -> archetype.createBatch(10, factory -> factory.create(new C1(), new C2(), new C3(), null)))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContainingAll("Component 4", "cannot be null");
            }

            @Test
            void testBatchInitializeNotCalled() {
                var archetype = createArchetype(C1.class, C2.class, C3.class, C4.class);

                assertThatThrownBy(() -> archetype.createBatch(10, factory -> null))
                        .isInstanceOf(NullPointerException.class)
                        .hasMessageContaining("return value cannot be null");
            }

            @Test
            void testGetInstance() {
                var archetype = createArchetype(C1.class, C2.class, C3.class, C4.class);
                assertThat(archetype.getInstance(D1.class)).isNotNull();
                assertThat(archetype.getInstance(D2.class)).isNotNull();
            }

            @Nested
            class ComponentRelationTest {

                @Test
                void testComponentRelation() {
                    var type = relation(C1.class, Target.class);
                    var archetype = createArchetype(type, type(C2.class), type(C3.class), type(C4.class));

                    var expected = new RegularComponentType<?, ?>[] { type, type(C2.class), type(C3.class), type(C4.class) };

                    verify(verify -> {
                        verify.expectInserted(expected);
                        verify.expectNoMoreInserted();

                        var relation1 = Relation.create(new C1(), new Target(1));

                        var entityId = archetype.create(relation1, new C2(), new C3(), new C4());
                        verifyHasComponents(entityId, expected);
                        verifyComponentMaskHasComponents(entityId, expected);
                    });
                }

                @Test
                void testMultipleComponentRelations_DifferentRelationships() {
                    var type1 = relation(C1.class, Target.class);
                    var type2 = relation(C2.class, Target.class);

                    var archetype = createArchetype(type1, type2, type(C3.class), type(C4.class));

                    var expected = new RegularComponentType<?, ?>[] { type1, type2, type(C3.class), type(C4.class) };

                    verify(verify -> {
                        verify.expectInserted(expected);
                        verify.expectNoMoreInserted();

                        var relation1 = Relation.create(new C1(), new Target(1));
                        var relation2 = Relation.create(new C2(), new Target(2));

                        var entityId = archetype.create(relation1, relation2, new C3(), new C4());
                        verifyHasComponents(entityId, expected);
                        verifyComponentMaskHasComponents(entityId, expected);
                    });
                }

                @Test
                void testMultipleComponentRelations_DifferentTargets() {
                    var type1 = relation(C1.class, Target.class);
                    var type2 = relation(C1.class, Target2.class);

                    var archetype = createArchetype(type1, type2, type(C3.class), type(C4.class));

                    var expected = new RegularComponentType<?, ?>[] { type1, type2, type(C3.class), type(C4.class) };

                    verify(verify -> {
                        verify.expectInserted(expected);
                        verify.expectNoMoreInserted();

                        var relation1 = Relation.create(new C1(), new Target(1));
                        var relation2 = Relation.create(new C1(), new Target2(2));

                        var entityId = archetype.create(relation1, relation2, new C3(), new C4());
                        verifyHasComponents(entityId, expected);
                        verifyComponentMaskHasComponents(entityId, expected);
                    });
                }

                @Test
                void testMultipleComponentRelations_SameType_TargetsEqual() {
                    var type = relation(C1.class, Target.class);
                    var archetype = createArchetype(type, type, type(C3.class), type(C4.class));

                    var expected = new RegularComponentType<?, ?>[] { type, type(C3.class), type(C4.class) };

                    verify(verify -> {
                        verify.expectInserted(expected);
                        verify.expectNoMoreInserted();

                        var relationship = new C1();
                        var target1 = new Target(1);
                        var target2 = new Target(1);

                        var relation1 = Relation.create(relationship, target1);
                        var relation2 = Relation.create(relationship, target2);

                        var entityId = archetype.create(relation1, relation2, new C3(), new C4());
                        verifyHasComponents(entityId, expected);
                        verifyComponentMaskHasComponents(entityId, expected);

                        var relations = relationMapperManager.getComponentRelationMapper(type).get(entityId);
                        assertThat(relations)
                                .extracting("relationship", "target")
                                .containsExactlyInAnyOrder(
                                        tuple(relationship, target2));
                    });
                }

                @Test
                void testMultipleComponentRelations_SameType_TargetsNotEqual() {
                    var type = relation(C1.class, Target.class);
                    var archetype = createArchetype(type, type, type(C3.class), type(C4.class));

                    var expected = new RegularComponentType<?, ?>[] { type, type(C3.class), type(C4.class) };

                    verify(verify -> {
                        verify.expectInserted(expected);
                        verify.expectNoMoreInserted();

                        var relationship = new C1();
                        var target1 = new Target(1);
                        var target2 = new Target(2);

                        var relation1 = Relation.create(relationship, target1);
                        var relation2 = Relation.create(relationship, target2);

                        var entityId = archetype.create(relation1, relation2, new C3(), new C4());
                        verifyHasComponents(entityId, expected);
                        verifyComponentMaskHasComponents(entityId, expected);

                        var relations = relationMapperManager.getComponentRelationMapper(type).get(entityId);
                        assertThat(relations)
                                .extracting("relationship", "target")
                                .containsExactlyInAnyOrder(
                                        tuple(relationship, target1),
                                        tuple(relationship, target2));
                    });
                }

            }

            @Nested
            class EntityRelationTest {

                @Test
                void testComponentRelation() {
                    var type = relation(C1.class);
                    var archetype = createArchetype(type, type(C2.class), type(C3.class), type(C4.class));

                    var expected = new RegularComponentType<?, ?>[] { type, type(C2.class), type(C3.class), type(C4.class) };

                    var target = world.createEntity();

                    verify(verify -> {
                        verify.expectInserted(expected);
                        verify.expectNoMoreInserted();

                        var relation1 = Relation.create(new C1(), target);

                        var entityId = archetype.create(relation1, new C2(), new C3(), new C4());
                        verifyHasComponents(entityId, expected);
                        verifyComponentMaskHasComponents(entityId, expected);
                    });
                }

                @Test
                void testMultipleComponentRelations_DifferentRelationships() {
                    var type1 = relation(C1.class);
                    var type2 = relation(C2.class);

                    var archetype = createArchetype(type1, type2, type(C3.class), type(C4.class));

                    var expected = new RegularComponentType<?, ?>[] { type1, type2, type(C3.class), type(C4.class) };

                    var target1 = world.createEntity();
                    var target2 = world.createEntity();

                    verify(verify -> {
                        verify.expectInserted(expected);
                        verify.expectNoMoreInserted();

                        var relation1 = Relation.create(new C1(), target1);
                        var relation2 = Relation.create(new C2(), target2);

                        var entityId = archetype.create(relation1, relation2, new C3(), new C4());
                        verifyHasComponents(entityId, expected);
                        verifyComponentMaskHasComponents(entityId, expected);
                    });
                }

                @Test
                void testMultipleComponentRelations_SameType_TargetsEqual() {
                    var type = relation(C1.class);
                    var archetype = createArchetype(type, type, type(C3.class), type(C4.class));

                    var expected = new RegularComponentType<?, ?>[] { type, type(C3.class), type(C4.class) };

                    var target = world.createEntity();

                    verify(verify -> {
                        verify.expectInserted(expected);
                        verify.expectNoMoreInserted();

                        var relationship = new C1();

                        var relation1 = Relation.create(relationship, target);
                        var relation2 = Relation.create(relationship, target);

                        var entityId = archetype.create(relation1, relation2, new C3(), new C4());
                        verifyHasComponents(entityId, expected);
                        verifyComponentMaskHasComponents(entityId, expected);

                        var relations = relationMapperManager.getEntityRelationMapper(type).get(entityId);
                        assertThat(relations)
                                .extracting("relationship", "target")
                                .containsExactlyInAnyOrder(
                                        tuple(relationship, target));
                    });
                }

                @Test
                void testMultipleComponentRelations_SameType_TargetsNotEqual() {
                    var type = relation(C1.class);
                    var archetype = createArchetype(type, type, type(C3.class), type(C4.class));

                    var expected = new RegularComponentType<?, ?>[] { type, type(C3.class), type(C4.class) };

                    var target1 = world.createEntity();
                    var target2 = world.createEntity();

                    verify(verify -> {
                        verify.expectInserted(expected);
                        verify.expectNoMoreInserted();

                        var relationship = new C1();

                        var relation1 = Relation.create(relationship, target1);
                        var relation2 = Relation.create(relationship, target2);

                        var entityId = archetype.create(relation1, relation2, new C3(), new C4());
                        verifyHasComponents(entityId, expected);
                        verifyComponentMaskHasComponents(entityId, expected);

                        var relations = relationMapperManager.getEntityRelationMapper(type).get(entityId);
                        assertThat(relations)
                                .extracting("relationship", "target")
                                .containsExactlyInAnyOrder(
                                        tuple(relationship, target1),
                                        tuple(relationship, target2));
                    });
                }

            }

        }

    }

    @Nested
    class Archetype5Test {

        @Nested
        class SimpleArchetypeTest extends AbstractTest {
            @Override
            protected <T1, T2, T3, T4, T5> Archetype5<T1, T2, T3, T4, T5> createArchetype(RegularComponentType<T1, ?> component1, RegularComponentType<T2, ?> component2,
                    RegularComponentType<T3, ?> component3, RegularComponentType<T4, ?> component4, RegularComponentType<T5, ?> component5) {

                return world.createArchetype(component1, component2, component3, component4, component5);
            }
        }

        @Nested
        class WithComponentsTest extends AbstractTest {

            @Override
            protected <T1, T2, T3, T4, T5> Archetype5<T1, T2, T3, T4, T5> createArchetype(RegularComponentType<T1, ?> component1, RegularComponentType<T2, ?> component2,
                    RegularComponentType<T3, ?> component3, RegularComponentType<T4, ?> component4, RegularComponentType<T5, ?> component5) {

                return world.createArchetype(component1, component2, component3, component4, component5).with(E1.INSTANCE, E2.INSTANCE);
            }

            protected void verifyComponentMaskHasComponents(int entityId, Class<?>... components) {
                var expected = ArrayUtils.concat(Class.class, components, E1.class, E2.class);

                ArchetypeManagerTest.this.verifyComponentMaskHasComponents(entityId, expected);
            }

            @Test
            void testPooledComponents_Throws() {
                assertThatThrownBy(() -> world.createArchetype(C1.class, C2.class, C3.class, C4.class, C5.class).with(new D1()))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContainingAll("D1", "cannot implement Pooled");
            }

            @Test
            void testDuplicateComponents_Throws() {
                assertThatThrownBy(() -> world.createArchetype(C1.class, C2.class, C3.class, C4.class, C5.class).with(new C1()))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContainingAll("C1", "already defined");

                assertThatThrownBy(() -> world.createArchetype(C1.class, C2.class, C3.class, C4.class, C5.class).with(E1.INSTANCE, E2.INSTANCE, E1.INSTANCE))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContainingAll("E1", "already defined");
            }

        }

        abstract class AbstractTest {

            protected final <T1, T2, T3, T4, T5> Archetype5<T1, T2, T3, T4, T5> createArchetype(Class<T1> component1, Class<T2> component2, Class<T3> component3, Class<T4> component4,
                    Class<T5> component5) {

                return createArchetype(component(component1), component(component2), component(component3), component(component4), component(component5));
            }

            protected abstract <T1, T2, T3, T4, T5> Archetype5<T1, T2, T3, T4, T5> createArchetype(RegularComponentType<T1, ?> component1, RegularComponentType<T2, ?> component2,
                    RegularComponentType<T3, ?> component3, RegularComponentType<T4, ?> component4, RegularComponentType<T5, ?> component5);

            @Test
            void testArchetype() {
                var archetype = createArchetype(C1.class, C2.class, C3.class, C4.class, C5.class);

                var entityId = archetype.create(new C1(), new C2(), new C3(), new C4(), new C5());
                verifyHasComponents(entityId, C1.class, C2.class, C3.class, C4.class, C5.class);
                verifyComponentMaskHasComponents(entityId, C1.class, C2.class, C3.class, C4.class, C5.class);
            }

            @Test
            void testNullInstance() {
                var archetype = createArchetype(C1.class, C2.class, C3.class, C4.class, C5.class);

                assertThatThrownBy(() -> archetype.create(null, new C2(), new C3(), new C4(), new C5())).isNotNull();
                assertThatThrownBy(() -> archetype.create(new C1(), null, new C3(), new C4(), new C5())).isNotNull();
                assertThatThrownBy(() -> archetype.create(new C1(), new C2(), null, new C4(), new C5())).isNotNull();
                assertThatThrownBy(() -> archetype.create(new C1(), new C2(), new C3(), null, new C5())).isNotNull();
                assertThatThrownBy(() -> archetype.create(new C1(), new C2(), new C3(), new C4(), null)).isNotNull();
            }

            @Test
            void testBatch() {
                var archetype = createArchetype(C1.class, C2.class, C3.class, C4.class, C5.class);

                var entityIds = archetype.createBatch(10, factory -> factory.create(new C1(), new C2(), new C3(), new C4(), new C5()));
                assertThat(entityIds.getSize()).isEqualTo(10);

                for (var iter = entityIds.iterator(); iter.hasNext();) {
                    var entityId = iter.nextInt();
                    verifyHasComponents(entityId, C1.class, C2.class, C3.class, C4.class, C5.class);
                    verifyComponentMaskHasComponents(entityId, C1.class, C2.class, C3.class, C4.class, C5.class);
                }
            }

            @Test
            void testBatchChangeHandler() {
                var count = 10;
                var archetype = createArchetype(C1.class, C2.class, C3.class, C4.class, C5.class);

                verify(verify -> {
                    for (int i = 0; i < count; i++) {
                        verify.expectInserted(C1.class, C2.class, C3.class, C4.class, C5.class);
                    }

                    archetype.createBatch(count, factory -> factory.create(new C1(), new C2(), new C3(), new C4(), new C5()));
                });
            }

            @Test
            void testBatch_NullInstances() {
                var archetype = createArchetype(C1.class, C2.class, C3.class, C4.class, C5.class);

                assertThatThrownBy(() -> archetype.createBatch(10, factory -> factory.create(null, new C2(), new C3(), new C4(), new C5())))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContainingAll("Component 1", "cannot be null");
                assertThatThrownBy(() -> archetype.createBatch(10, factory -> factory.create(new C1(), null, new C3(), new C4(), new C5())))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContainingAll("Component 2", "cannot be null");
                assertThatThrownBy(() -> archetype.createBatch(10, factory -> factory.create(new C1(), new C2(), null, new C4(), new C5())))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContainingAll("Component 3", "cannot be null");
                assertThatThrownBy(() -> archetype.createBatch(10, factory -> factory.create(new C1(), new C2(), new C3(), null, new C5())))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContainingAll("Component 4", "cannot be null");
                assertThatThrownBy(() -> archetype.createBatch(10, factory -> factory.create(new C1(), new C2(), new C3(), new C4(), null)))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContainingAll("Component 5", "cannot be null");
            }

            @Test
            void testBatchInitializeNotCalled() {
                var archetype = createArchetype(C1.class, C2.class, C3.class, C4.class, C5.class);

                assertThatThrownBy(() -> archetype.createBatch(10, factory -> null))
                        .isInstanceOf(NullPointerException.class)
                        .hasMessageContaining("return value cannot be null");
            }

            @Test
            void testGetInstance() {
                var archetype = createArchetype(C1.class, C2.class, C3.class, C4.class, C5.class);
                assertThat(archetype.getInstance(D1.class)).isNotNull();
                assertThat(archetype.getInstance(D2.class)).isNotNull();
            }

            @Nested
            class ComponentRelationTest {

                @Test
                void testComponentRelation() {
                    var type = relation(C1.class, Target.class);
                    var archetype = createArchetype(type, type(C2.class), type(C3.class), type(C4.class), type(C5.class));

                    var expected = new RegularComponentType<?, ?>[] { type, type(C2.class), type(C3.class), type(C4.class), type(C5.class) };

                    verify(verify -> {
                        verify.expectInserted(expected);
                        verify.expectNoMoreInserted();

                        var relation1 = Relation.create(new C1(), new Target(1));

                        var entityId = archetype.create(relation1, new C2(), new C3(), new C4(), new C5());
                        verifyHasComponents(entityId, expected);
                        verifyComponentMaskHasComponents(entityId, expected);
                    });
                }

                @Test
                void testMultipleComponentRelations_DifferentRelationships() {
                    var type1 = relation(C1.class, Target.class);
                    var type2 = relation(C2.class, Target.class);

                    var archetype = createArchetype(type1, type2, type(C3.class), type(C4.class), type(C5.class));

                    var expected = new RegularComponentType<?, ?>[] { type1, type2, type(C3.class), type(C4.class), type(C5.class) };

                    verify(verify -> {
                        verify.expectInserted(expected);
                        verify.expectNoMoreInserted();

                        var relation1 = Relation.create(new C1(), new Target(1));
                        var relation2 = Relation.create(new C2(), new Target(2));

                        var entityId = archetype.create(relation1, relation2, new C3(), new C4(), new C5());
                        verifyHasComponents(entityId, expected);
                        verifyComponentMaskHasComponents(entityId, expected);
                    });
                }

                @Test
                void testMultipleComponentRelations_DifferentTargets() {
                    var type1 = relation(C1.class, Target.class);
                    var type2 = relation(C1.class, Target2.class);

                    var archetype = createArchetype(type1, type2, type(C3.class), type(C4.class), type(C5.class));

                    var expected = new RegularComponentType<?, ?>[] { type1, type2, type(C3.class), type(C4.class), type(C5.class) };

                    verify(verify -> {
                        verify.expectInserted(expected);
                        verify.expectNoMoreInserted();

                        var relation1 = Relation.create(new C1(), new Target(1));
                        var relation2 = Relation.create(new C1(), new Target2(2));

                        var entityId = archetype.create(relation1, relation2, new C3(), new C4(), new C5());
                        verifyHasComponents(entityId, expected);
                        verifyComponentMaskHasComponents(entityId, expected);
                    });
                }

                @Test
                void testMultipleComponentRelations_SameType_TargetsEqual() {
                    var type = relation(C1.class, Target.class);
                    var archetype = createArchetype(type, type, type(C3.class), type(C4.class), type(C5.class));

                    var expected = new RegularComponentType<?, ?>[] { type, type(C3.class), type(C4.class), type(C5.class) };

                    verify(verify -> {
                        verify.expectInserted(expected);
                        verify.expectNoMoreInserted();

                        var relationship = new C1();
                        var target1 = new Target(1);
                        var target2 = new Target(1);

                        var relation1 = Relation.create(relationship, target1);
                        var relation2 = Relation.create(relationship, target2);

                        var entityId = archetype.create(relation1, relation2, new C3(), new C4(), new C5());
                        verifyHasComponents(entityId, expected);
                        verifyComponentMaskHasComponents(entityId, expected);

                        var relations = relationMapperManager.getComponentRelationMapper(type).get(entityId);
                        assertThat(relations)
                                .extracting("relationship", "target")
                                .containsExactlyInAnyOrder(
                                        tuple(relationship, target2));
                    });
                }

                @Test
                void testMultipleComponentRelations_SameType_TargetsNotEqual() {
                    var type = relation(C1.class, Target.class);
                    var archetype = createArchetype(type, type, type(C3.class), type(C4.class), type(C5.class));

                    var expected = new RegularComponentType<?, ?>[] { type, type(C3.class), type(C4.class), type(C5.class) };

                    verify(verify -> {
                        verify.expectInserted(expected);
                        verify.expectNoMoreInserted();

                        var relationship = new C1();
                        var target1 = new Target(1);
                        var target2 = new Target(2);

                        var relation1 = Relation.create(relationship, target1);
                        var relation2 = Relation.create(relationship, target2);

                        var entityId = archetype.create(relation1, relation2, new C3(), new C4(), new C5());
                        verifyHasComponents(entityId, expected);
                        verifyComponentMaskHasComponents(entityId, expected);

                        var relations = relationMapperManager.getComponentRelationMapper(type).get(entityId);
                        assertThat(relations)
                                .extracting("relationship", "target")
                                .containsExactlyInAnyOrder(
                                        tuple(relationship, target1),
                                        tuple(relationship, target2));
                    });
                }

            }

            @Nested
            class EntityRelationTest {

                @Test
                void testComponentRelation() {
                    var type = relation(C1.class);
                    var archetype = createArchetype(type, type(C2.class), type(C3.class), type(C4.class), type(C5.class));

                    var expected = new RegularComponentType<?, ?>[] { type, type(C2.class), type(C3.class), type(C4.class), type(C5.class) };

                    var target = world.createEntity();

                    verify(verify -> {
                        verify.expectInserted(expected);
                        verify.expectNoMoreInserted();

                        var relation1 = Relation.create(new C1(), target);

                        var entityId = archetype.create(relation1, new C2(), new C3(), new C4(), new C5());
                        verifyHasComponents(entityId, expected);
                        verifyComponentMaskHasComponents(entityId, expected);
                    });
                }

                @Test
                void testMultipleComponentRelations_DifferentRelationships() {
                    var type1 = relation(C1.class);
                    var type2 = relation(C2.class);

                    var archetype = createArchetype(type1, type2, type(C3.class), type(C4.class), type(C5.class));

                    var expected = new RegularComponentType<?, ?>[] { type1, type2, type(C3.class), type(C4.class), type(C5.class) };

                    var target1 = world.createEntity();
                    var target2 = world.createEntity();

                    verify(verify -> {
                        verify.expectInserted(expected);
                        verify.expectNoMoreInserted();

                        var relation1 = Relation.create(new C1(), target1);
                        var relation2 = Relation.create(new C2(), target2);

                        var entityId = archetype.create(relation1, relation2, new C3(), new C4(), new C5());
                        verifyHasComponents(entityId, expected);
                        verifyComponentMaskHasComponents(entityId, expected);
                    });
                }

                @Test
                void testMultipleComponentRelations_SameType_TargetsEqual() {
                    var type = relation(C1.class);
                    var archetype = createArchetype(type, type, type(C3.class), type(C4.class), type(C5.class));

                    var expected = new RegularComponentType<?, ?>[] { type, type(C3.class), type(C4.class), type(C5.class) };

                    var target = world.createEntity();

                    verify(verify -> {
                        verify.expectInserted(expected);
                        verify.expectNoMoreInserted();

                        var relationship = new C1();

                        var relation1 = Relation.create(relationship, target);
                        var relation2 = Relation.create(relationship, target);

                        var entityId = archetype.create(relation1, relation2, new C3(), new C4(), new C5());
                        verifyHasComponents(entityId, expected);
                        verifyComponentMaskHasComponents(entityId, expected);

                        var relations = relationMapperManager.getEntityRelationMapper(type).get(entityId);
                        assertThat(relations)
                                .extracting("relationship", "target")
                                .containsExactlyInAnyOrder(
                                        tuple(relationship, target));
                    });
                }

                @Test
                void testMultipleComponentRelations_SameType_TargetsNotEqual() {
                    var type = relation(C1.class);
                    var archetype = createArchetype(type, type, type(C3.class), type(C4.class), type(C5.class));

                    var expected = new RegularComponentType<?, ?>[] { type, type(C3.class), type(C4.class), type(C5.class) };

                    var target1 = world.createEntity();
                    var target2 = world.createEntity();

                    verify(verify -> {
                        verify.expectInserted(expected);
                        verify.expectNoMoreInserted();

                        var relationship = new C1();

                        var relation1 = Relation.create(relationship, target1);
                        var relation2 = Relation.create(relationship, target2);

                        var entityId = archetype.create(relation1, relation2, new C3(), new C4(), new C5());
                        verifyHasComponents(entityId, expected);
                        verifyComponentMaskHasComponents(entityId, expected);

                        var relations = relationMapperManager.getEntityRelationMapper(type).get(entityId);
                        assertThat(relations)
                                .extracting("relationship", "target")
                                .containsExactlyInAnyOrder(
                                        tuple(relationship, target1),
                                        tuple(relationship, target2));
                    });
                }

            }

        }

    }

    @Nested
    class Archetype6Test {

        @Nested
        class SimpleArchetypeTest extends AbstractTest {
            @Override
            protected <T1, T2, T3, T4, T5, T6> Archetype6<T1, T2, T3, T4, T5, T6> createArchetype(RegularComponentType<T1, ?> component1, RegularComponentType<T2, ?> component2,
                    RegularComponentType<T3, ?> component3, RegularComponentType<T4, ?> component4, RegularComponentType<T5, ?> component5, RegularComponentType<T6, ?> component6) {

                return world.createArchetype(component1, component2, component3, component4, component5, component6);
            }
        }

        @Nested
        class WithComponentsTest extends AbstractTest {

            @Override
            protected <T1, T2, T3, T4, T5, T6> Archetype6<T1, T2, T3, T4, T5, T6> createArchetype(RegularComponentType<T1, ?> component1, RegularComponentType<T2, ?> component2,
                    RegularComponentType<T3, ?> component3, RegularComponentType<T4, ?> component4, RegularComponentType<T5, ?> component5, RegularComponentType<T6, ?> component6) {

                return world.createArchetype(component1, component2, component3, component4, component5, component6).with(E1.INSTANCE, E2.INSTANCE);
            }

            protected void verifyComponentMaskHasComponents(int entityId, Class<?>... components) {
                var expected = ArrayUtils.concat(Class.class, components, E1.class, E2.class);

                ArchetypeManagerTest.this.verifyComponentMaskHasComponents(entityId, expected);
            }

            @Test
            void testPooledComponents_Throws() {
                assertThatThrownBy(() -> world.createArchetype(C1.class, C2.class, C3.class, C4.class, C5.class, C6.class).with(new D1()))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContainingAll("D1", "cannot implement Pooled");
            }

            @Test
            void testDuplicateComponents_Throws() {
                assertThatThrownBy(() -> world.createArchetype(C1.class, C2.class, C3.class, C4.class, C5.class, C6.class).with(new C1()))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContainingAll("C1", "already defined");

                assertThatThrownBy(() -> world.createArchetype(C1.class, C2.class, C3.class, C4.class, C5.class, C6.class).with(E1.INSTANCE, E2.INSTANCE, E1.INSTANCE))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContainingAll("E1", "already defined");
            }

        }

        abstract class AbstractTest {

            protected final <T1, T2, T3, T4, T5, T6> Archetype6<T1, T2, T3, T4, T5, T6> createArchetype(Class<T1> component1, Class<T2> component2, Class<T3> component3, Class<T4> component4,
                    Class<T5> component5, Class<T6> component6) {

                return createArchetype(component(component1), component(component2), component(component3), component(component4), component(component5), component(component6));
            }

            protected abstract <T1, T2, T3, T4, T5, T6> Archetype6<T1, T2, T3, T4, T5, T6> createArchetype(RegularComponentType<T1, ?> component1, RegularComponentType<T2, ?> component2,
                    RegularComponentType<T3, ?> component3, RegularComponentType<T4, ?> component4, RegularComponentType<T5, ?> component5, RegularComponentType<T6, ?> component6);

            @Test
            void testArchetype() {
                var archetype = createArchetype(C1.class, C2.class, C3.class, C4.class, C5.class, C6.class);

                var entityId = archetype.create(new C1(), new C2(), new C3(), new C4(), new C5(), new C6());
                verifyHasComponents(entityId, C1.class, C2.class, C3.class, C4.class, C5.class, C6.class);
                verifyComponentMaskHasComponents(entityId, C1.class, C2.class, C3.class, C4.class, C5.class, C6.class);
            }

            @Test
            void testNullInstance() {
                var archetype = createArchetype(C1.class, C2.class, C3.class, C4.class, C5.class, C6.class);

                assertThatThrownBy(() -> archetype.create(null, new C2(), new C3(), new C4(), new C5(), new C6())).isNotNull();
                assertThatThrownBy(() -> archetype.create(new C1(), null, new C3(), new C4(), new C5(), new C6())).isNotNull();
                assertThatThrownBy(() -> archetype.create(new C1(), new C2(), null, new C4(), new C5(), new C6())).isNotNull();
                assertThatThrownBy(() -> archetype.create(new C1(), new C2(), new C3(), null, new C5(), new C6())).isNotNull();
                assertThatThrownBy(() -> archetype.create(new C1(), new C2(), new C3(), new C4(), null, new C6())).isNotNull();
                assertThatThrownBy(() -> archetype.create(new C1(), new C2(), new C3(), new C4(), new C5(), null)).isNotNull();
            }

            @Test
            void testBatch() {
                var archetype = createArchetype(C1.class, C2.class, C3.class, C4.class, C5.class, C6.class);

                var entityIds = archetype.createBatch(10, factory -> factory.create(new C1(), new C2(), new C3(), new C4(), new C5(), new C6()));
                assertThat(entityIds.getSize()).isEqualTo(10);

                for (var iter = entityIds.iterator(); iter.hasNext();) {
                    var entityId = iter.nextInt();
                    verifyHasComponents(entityId, C1.class, C2.class, C3.class, C4.class, C5.class, C6.class);
                    verifyComponentMaskHasComponents(entityId, C1.class, C2.class, C3.class, C4.class, C5.class, C6.class);
                }
            }

            @Test
            void testBatchChangeHandler() {
                var count = 10;
                var archetype = createArchetype(C1.class, C2.class, C3.class, C4.class, C5.class, C6.class);

                verify(verify -> {
                    for (int i = 0; i < count; i++) {
                        verify.expectInserted(C1.class, C2.class, C3.class, C4.class, C5.class, C6.class);
                    }

                    archetype.createBatch(count, factory -> factory.create(new C1(), new C2(), new C3(), new C4(), new C5(), new C6()));
                });
            }

            @Test
            void testBatch_NullInstances() {
                var archetype = createArchetype(C1.class, C2.class, C3.class, C4.class, C5.class, C6.class);

                assertThatThrownBy(() -> archetype.createBatch(10, factory -> factory.create(null, new C2(), new C3(), new C4(), new C5(), new C6())))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContainingAll("Component 1", "cannot be null");
                assertThatThrownBy(() -> archetype.createBatch(10, factory -> factory.create(new C1(), null, new C3(), new C4(), new C5(), new C6())))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContainingAll("Component 2", "cannot be null");
                assertThatThrownBy(() -> archetype.createBatch(10, factory -> factory.create(new C1(), new C2(), null, new C4(), new C5(), new C6())))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContainingAll("Component 3", "cannot be null");
                assertThatThrownBy(() -> archetype.createBatch(10, factory -> factory.create(new C1(), new C2(), new C3(), null, new C5(), new C6())))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContainingAll("Component 4", "cannot be null");
                assertThatThrownBy(() -> archetype.createBatch(10, factory -> factory.create(new C1(), new C2(), new C3(), new C4(), null, new C6())))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContainingAll("Component 5", "cannot be null");
                assertThatThrownBy(() -> archetype.createBatch(10, factory -> factory.create(new C1(), new C2(), new C3(), new C4(), new C5(), null)))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContainingAll("Component 6", "cannot be null");
            }

            @Test
            void testBatchInitializeNotCalled() {
                var archetype = createArchetype(C1.class, C2.class, C3.class, C4.class, C5.class, C6.class);

                assertThatThrownBy(() -> archetype.createBatch(10, factory -> null))
                        .isInstanceOf(NullPointerException.class)
                        .hasMessageContaining("return value cannot be null");
            }

            @Test
            void testGetInstance() {
                var archetype = createArchetype(C1.class, C2.class, C3.class, C4.class, C5.class, C6.class);
                assertThat(archetype.getInstance(D1.class)).isNotNull();
                assertThat(archetype.getInstance(D2.class)).isNotNull();
            }

            @Nested
            class ComponentRelationTest {

                @Test
                void testComponentRelation() {
                    var type = relation(C1.class, Target.class);
                    var archetype = createArchetype(type, type(C2.class), type(C3.class), type(C4.class), type(C5.class), type(C6.class));

                    var expected = new RegularComponentType<?, ?>[] { type, type(C2.class), type(C3.class), type(C4.class), type(C5.class), type(C6.class) };

                    verify(verify -> {
                        verify.expectInserted(expected);
                        verify.expectNoMoreInserted();

                        var relation1 = Relation.create(new C1(), new Target(1));

                        var entityId = archetype.create(relation1, new C2(), new C3(), new C4(), new C5(), new C6());
                        verifyHasComponents(entityId, expected);
                        verifyComponentMaskHasComponents(entityId, expected);
                    });
                }

                @Test
                void testMultipleComponentRelations_DifferentRelationships() {
                    var type1 = relation(C1.class, Target.class);
                    var type2 = relation(C2.class, Target.class);

                    var archetype = createArchetype(type1, type2, type(C3.class), type(C4.class), type(C5.class), type(C6.class));

                    var expected = new RegularComponentType<?, ?>[] { type1, type2, type(C3.class), type(C4.class), type(C5.class), type(C6.class) };

                    verify(verify -> {
                        verify.expectInserted(expected);
                        verify.expectNoMoreInserted();

                        var relation1 = Relation.create(new C1(), new Target(1));
                        var relation2 = Relation.create(new C2(), new Target(2));

                        var entityId = archetype.create(relation1, relation2, new C3(), new C4(), new C5(), new C6());
                        verifyHasComponents(entityId, expected);
                        verifyComponentMaskHasComponents(entityId, expected);
                    });
                }

                @Test
                void testMultipleComponentRelations_DifferentTargets() {
                    var type1 = relation(C1.class, Target.class);
                    var type2 = relation(C1.class, Target2.class);

                    var archetype = createArchetype(type1, type2, type(C3.class), type(C4.class), type(C5.class), type(C6.class));

                    var expected = new RegularComponentType<?, ?>[] { type1, type2, type(C3.class), type(C4.class), type(C5.class), type(C6.class) };

                    verify(verify -> {
                        verify.expectInserted(expected);
                        verify.expectNoMoreInserted();

                        var relation1 = Relation.create(new C1(), new Target(1));
                        var relation2 = Relation.create(new C1(), new Target2(2));

                        var entityId = archetype.create(relation1, relation2, new C3(), new C4(), new C5(), new C6());
                        verifyHasComponents(entityId, expected);
                        verifyComponentMaskHasComponents(entityId, expected);
                    });
                }

                @Test
                void testMultipleComponentRelations_SameType_TargetsEqual() {
                    var type = relation(C1.class, Target.class);
                    var archetype = createArchetype(type, type, type(C3.class), type(C4.class), type(C5.class), type(C6.class));

                    var expected = new RegularComponentType<?, ?>[] { type, type(C3.class), type(C4.class), type(C5.class), type(C6.class) };

                    verify(verify -> {
                        verify.expectInserted(expected);
                        verify.expectNoMoreInserted();

                        var relationship = new C1();
                        var target1 = new Target(1);
                        var target2 = new Target(1);

                        var relation1 = Relation.create(relationship, target1);
                        var relation2 = Relation.create(relationship, target2);

                        var entityId = archetype.create(relation1, relation2, new C3(), new C4(), new C5(), new C6());
                        verifyHasComponents(entityId, expected);
                        verifyComponentMaskHasComponents(entityId, expected);

                        var relations = relationMapperManager.getComponentRelationMapper(type).get(entityId);
                        assertThat(relations)
                                .extracting("relationship", "target")
                                .containsExactlyInAnyOrder(
                                        tuple(relationship, target2));
                    });
                }

                @Test
                void testMultipleComponentRelations_SameType_TargetsNotEqual() {
                    var type = relation(C1.class, Target.class);
                    var archetype = createArchetype(type, type, type(C3.class), type(C4.class), type(C5.class), type(C6.class));

                    var expected = new RegularComponentType<?, ?>[] { type, type(C3.class), type(C4.class), type(C5.class), type(C6.class) };

                    verify(verify -> {
                        verify.expectInserted(expected);
                        verify.expectNoMoreInserted();

                        var relationship = new C1();
                        var target1 = new Target(1);
                        var target2 = new Target(2);

                        var relation1 = Relation.create(relationship, target1);
                        var relation2 = Relation.create(relationship, target2);

                        var entityId = archetype.create(relation1, relation2, new C3(), new C4(), new C5(), new C6());
                        verifyHasComponents(entityId, expected);
                        verifyComponentMaskHasComponents(entityId, expected);

                        var relations = relationMapperManager.getComponentRelationMapper(type).get(entityId);
                        assertThat(relations)
                                .extracting("relationship", "target")
                                .containsExactlyInAnyOrder(
                                        tuple(relationship, target1),
                                        tuple(relationship, target2));
                    });
                }

            }

            @Nested
            class EntityRelationTest {

                @Test
                void testComponentRelation() {
                    var type = relation(C1.class);
                    var archetype = createArchetype(type, type(C2.class), type(C3.class), type(C4.class), type(C5.class), type(C6.class));

                    var expected = new RegularComponentType<?, ?>[] { type, type(C2.class), type(C3.class), type(C4.class), type(C5.class), type(C6.class) };

                    var target = world.createEntity();

                    verify(verify -> {
                        verify.expectInserted(expected);
                        verify.expectNoMoreInserted();

                        var relation1 = Relation.create(new C1(), target);

                        var entityId = archetype.create(relation1, new C2(), new C3(), new C4(), new C5(), new C6());
                        verifyHasComponents(entityId, expected);
                        verifyComponentMaskHasComponents(entityId, expected);
                    });
                }

                @Test
                void testMultipleComponentRelations_DifferentRelationships() {
                    var type1 = relation(C1.class);
                    var type2 = relation(C2.class);

                    var archetype = createArchetype(type1, type2, type(C3.class), type(C4.class), type(C5.class), type(C6.class));

                    var expected = new RegularComponentType<?, ?>[] { type1, type2, type(C3.class), type(C4.class), type(C5.class), type(C6.class) };

                    var target1 = world.createEntity();
                    var target2 = world.createEntity();

                    verify(verify -> {
                        verify.expectInserted(expected);
                        verify.expectNoMoreInserted();

                        var relation1 = Relation.create(new C1(), target1);
                        var relation2 = Relation.create(new C2(), target2);

                        var entityId = archetype.create(relation1, relation2, new C3(), new C4(), new C5(), new C6());
                        verifyHasComponents(entityId, expected);
                        verifyComponentMaskHasComponents(entityId, expected);
                    });
                }

                @Test
                void testMultipleComponentRelations_SameType_TargetsEqual() {
                    var type = relation(C1.class);
                    var archetype = createArchetype(type, type, type(C3.class), type(C4.class), type(C5.class), type(C6.class));

                    var expected = new RegularComponentType<?, ?>[] { type, type(C3.class), type(C4.class), type(C5.class), type(C6.class) };

                    var target = world.createEntity();

                    verify(verify -> {
                        verify.expectInserted(expected);
                        verify.expectNoMoreInserted();

                        var relationship = new C1();

                        var relation1 = Relation.create(relationship, target);
                        var relation2 = Relation.create(relationship, target);

                        var entityId = archetype.create(relation1, relation2, new C3(), new C4(), new C5(), new C6());
                        verifyHasComponents(entityId, expected);
                        verifyComponentMaskHasComponents(entityId, expected);

                        var relations = relationMapperManager.getEntityRelationMapper(type).get(entityId);
                        assertThat(relations)
                                .extracting("relationship", "target")
                                .containsExactlyInAnyOrder(
                                        tuple(relationship, target));
                    });
                }

                @Test
                void testMultipleComponentRelations_SameType_TargetsNotEqual() {
                    var type = relation(C1.class);
                    var archetype = createArchetype(type, type, type(C3.class), type(C4.class), type(C5.class), type(C6.class));

                    var expected = new RegularComponentType<?, ?>[] { type, type(C3.class), type(C4.class), type(C5.class), type(C6.class) };

                    var target1 = world.createEntity();
                    var target2 = world.createEntity();

                    verify(verify -> {
                        verify.expectInserted(expected);
                        verify.expectNoMoreInserted();

                        var relationship = new C1();

                        var relation1 = Relation.create(relationship, target1);
                        var relation2 = Relation.create(relationship, target2);

                        var entityId = archetype.create(relation1, relation2, new C3(), new C4(), new C5(), new C6());
                        verifyHasComponents(entityId, expected);
                        verifyComponentMaskHasComponents(entityId, expected);

                        var relations = relationMapperManager.getEntityRelationMapper(type).get(entityId);
                        assertThat(relations)
                                .extracting("relationship", "target")
                                .containsExactlyInAnyOrder(
                                        tuple(relationship, target1),
                                        tuple(relationship, target2));
                    });
                }

            }

        }

    }

    @Nested
    class Archetype7Test {

        @Nested
        class SimpleArchetypeTest extends AbstractTest {
            @Override
            protected <T1, T2, T3, T4, T5, T6, T7> Archetype7<T1, T2, T3, T4, T5, T6, T7> createArchetype(RegularComponentType<T1, ?> component1, RegularComponentType<T2, ?> component2,
                    RegularComponentType<T3, ?> component3, RegularComponentType<T4, ?> component4,
                    RegularComponentType<T5, ?> component5, RegularComponentType<T6, ?> component6, RegularComponentType<T7, ?> component7) {

                return world.createArchetype(component1, component2, component3, component4, component5, component6, component7);
            }
        }

        @Nested
        class WithComponentsTest extends AbstractTest {

            @Override
            protected <T1, T2, T3, T4, T5, T6, T7> Archetype7<T1, T2, T3, T4, T5, T6, T7> createArchetype(RegularComponentType<T1, ?> component1, RegularComponentType<T2, ?> component2,
                    RegularComponentType<T3, ?> component3, RegularComponentType<T4, ?> component4,
                    RegularComponentType<T5, ?> component5, RegularComponentType<T6, ?> component6, RegularComponentType<T7, ?> component7) {

                return world.createArchetype(component1, component2, component3, component4, component5, component6, component7).with(E1.INSTANCE, E2.INSTANCE);
            }

            protected void verifyComponentMaskHasComponents(int entityId, Class<?>... components) {
                var expected = ArrayUtils.concat(Class.class, components, E1.class, E2.class);

                ArchetypeManagerTest.this.verifyComponentMaskHasComponents(entityId, expected);
            }

            @Test
            void testPooledComponents_Throws() {
                assertThatThrownBy(() -> world.createArchetype(C1.class, C2.class, C3.class, C4.class, C5.class, C6.class, C7.class).with(new D1()))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContainingAll("D1", "cannot implement Pooled");
            }

            @Test
            void testDuplicateComponents_Throws() {
                assertThatThrownBy(() -> world.createArchetype(C1.class, C2.class, C3.class, C4.class, C5.class, C6.class, C7.class).with(new C1()))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContainingAll("C1", "already defined");

                assertThatThrownBy(() -> world.createArchetype(C1.class, C2.class, C3.class, C4.class, C5.class, C6.class, C7.class).with(E1.INSTANCE, E2.INSTANCE, E1.INSTANCE))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContainingAll("E1", "already defined");
            }

        }

        abstract class AbstractTest {

            protected final <T1, T2, T3, T4, T5, T6, T7> Archetype7<T1, T2, T3, T4, T5, T6, T7> createArchetype(Class<T1> component1, Class<T2> component2, Class<T3> component3,
                    Class<T4> component4, Class<T5> component5, Class<T6> component6, Class<T7> component7) {

                return createArchetype(component(component1), component(component2), component(component3), component(component4), component(component5), component(component6), component(component7));
            }

            protected abstract <T1, T2, T3, T4, T5, T6, T7> Archetype7<T1, T2, T3, T4, T5, T6, T7> createArchetype(RegularComponentType<T1, ?> component1, RegularComponentType<T2, ?> component2,
                    RegularComponentType<T3, ?> component3, RegularComponentType<T4, ?> component4, RegularComponentType<T5, ?> component5, RegularComponentType<T6, ?> component6,
                    RegularComponentType<T7, ?> component7);

            @Test
            void testArchetype() {
                var archetype = createArchetype(C1.class, C2.class, C3.class, C4.class, C5.class, C6.class, C7.class);

                var entityId = archetype.create(new C1(), new C2(), new C3(), new C4(), new C5(), new C6(), new C7());
                verifyHasComponents(entityId, C1.class, C2.class, C3.class, C4.class, C5.class, C6.class, C7.class);
                verifyComponentMaskHasComponents(entityId, C1.class, C2.class, C3.class, C4.class, C5.class, C6.class, C7.class);
            }

            @Test
            void testNullInstance() {
                var archetype = createArchetype(C1.class, C2.class, C3.class, C4.class, C5.class, C6.class, C7.class);

                assertThatThrownBy(() -> archetype.create(null, new C2(), new C3(), new C4(), new C5(), new C6(), new C7())).isNotNull();
                assertThatThrownBy(() -> archetype.create(new C1(), null, new C3(), new C4(), new C5(), new C6(), new C7())).isNotNull();
                assertThatThrownBy(() -> archetype.create(new C1(), new C2(), null, new C4(), new C5(), new C6(), new C7())).isNotNull();
                assertThatThrownBy(() -> archetype.create(new C1(), new C2(), new C3(), null, new C5(), new C6(), new C7())).isNotNull();
                assertThatThrownBy(() -> archetype.create(new C1(), new C2(), new C3(), new C4(), null, new C6(), new C7())).isNotNull();
                assertThatThrownBy(() -> archetype.create(new C1(), new C2(), new C3(), new C4(), new C5(), null, new C7())).isNotNull();
                assertThatThrownBy(() -> archetype.create(new C1(), new C2(), new C3(), new C4(), new C5(), new C6(), null)).isNotNull();
            }

            @Test
            void testBatch() {
                var archetype = createArchetype(C1.class, C2.class, C3.class, C4.class, C5.class, C6.class, C7.class);

                var entityIds = archetype.createBatch(10, factory -> factory.create(new C1(), new C2(), new C3(), new C4(), new C5(), new C6(), new C7()));
                assertThat(entityIds.getSize()).isEqualTo(10);

                for (var iter = entityIds.iterator(); iter.hasNext();) {
                    var entityId = iter.nextInt();
                    verifyHasComponents(entityId, C1.class, C2.class, C3.class, C4.class, C5.class, C6.class, C7.class);
                    verifyComponentMaskHasComponents(entityId, C1.class, C2.class, C3.class, C4.class, C5.class, C6.class, C7.class);
                }
            }

            @Test
            void testBatchChangeHandler() {
                var count = 10;
                var archetype = createArchetype(C1.class, C2.class, C3.class, C4.class, C5.class, C6.class, C7.class);

                verify(verify -> {
                    for (int i = 0; i < count; i++) {
                        verify.expectInserted(C1.class, C2.class, C3.class, C4.class, C5.class, C6.class, C7.class);
                    }

                    archetype.createBatch(count, factory -> factory.create(new C1(), new C2(), new C3(), new C4(), new C5(), new C6(), new C7()));
                });
            }

            @Test
            void testBatch_NullInstances() {
                var archetype = createArchetype(C1.class, C2.class, C3.class, C4.class, C5.class, C6.class, C7.class);

                assertThatThrownBy(() -> archetype.createBatch(10, factory -> factory.create(null, new C2(), new C3(), new C4(), new C5(), new C6(), new C7())))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContainingAll("Component 1", "cannot be null");
                assertThatThrownBy(() -> archetype.createBatch(10, factory -> factory.create(new C1(), null, new C3(), new C4(), new C5(), new C6(), new C7())))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContainingAll("Component 2", "cannot be null");
                assertThatThrownBy(() -> archetype.createBatch(10, factory -> factory.create(new C1(), new C2(), null, new C4(), new C5(), new C6(), new C7())))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContainingAll("Component 3", "cannot be null");
                assertThatThrownBy(() -> archetype.createBatch(10, factory -> factory.create(new C1(), new C2(), new C3(), null, new C5(), new C6(), new C7())))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContainingAll("Component 4", "cannot be null");
                assertThatThrownBy(() -> archetype.createBatch(10, factory -> factory.create(new C1(), new C2(), new C3(), new C4(), null, new C6(), new C7())))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContainingAll("Component 5", "cannot be null");
                assertThatThrownBy(() -> archetype.createBatch(10, factory -> factory.create(new C1(), new C2(), new C3(), new C4(), new C5(), null, new C7())))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContainingAll("Component 6", "cannot be null");
                assertThatThrownBy(() -> archetype.createBatch(10, factory -> factory.create(new C1(), new C2(), new C3(), new C4(), new C5(), new C6(), null)))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContainingAll("Component 7", "cannot be null");
            }

            @Test
            void testBatchInitializeNotCalled() {
                var archetype = createArchetype(C1.class, C2.class, C3.class, C4.class, C5.class, C6.class, C7.class);

                assertThatThrownBy(() -> archetype.createBatch(10, factory -> null))
                        .isInstanceOf(NullPointerException.class)
                        .hasMessageContaining("return value cannot be null");
            }

            @Test
            void testGetInstance() {
                var archetype = createArchetype(C1.class, C2.class, C3.class, C4.class, C5.class, C6.class, C7.class);
                assertThat(archetype.getInstance(D1.class)).isNotNull();
                assertThat(archetype.getInstance(D2.class)).isNotNull();
            }

            @Nested
            class ComponentRelationTest {

                @Test
                void testComponentRelation() {
                    var type = relation(C1.class, Target.class);
                    var archetype = createArchetype(type, type(C2.class), type(C3.class), type(C4.class), type(C5.class), type(C6.class), type(C7.class));

                    var expected = new RegularComponentType<?, ?>[] { type, type(C2.class), type(C3.class), type(C4.class), type(C5.class), type(C6.class), type(C7.class) };

                    verify(verify -> {
                        verify.expectInserted(expected);
                        verify.expectNoMoreInserted();

                        var relation1 = Relation.create(new C1(), new Target(1));

                        var entityId = archetype.create(relation1, new C2(), new C3(), new C4(), new C5(), new C6(), new C7());
                        verifyHasComponents(entityId, expected);
                        verifyComponentMaskHasComponents(entityId, expected);
                    });
                }

                @Test
                void testMultipleComponentRelations_DifferentRelationships() {
                    var type1 = relation(C1.class, Target.class);
                    var type2 = relation(C2.class, Target.class);

                    var archetype = createArchetype(type1, type2, type(C3.class), type(C4.class), type(C5.class), type(C6.class), type(C7.class));

                    var expected = new RegularComponentType<?, ?>[] { type1, type2, type(C3.class), type(C4.class), type(C5.class), type(C6.class), type(C7.class) };

                    verify(verify -> {
                        verify.expectInserted(expected);
                        verify.expectNoMoreInserted();

                        var relation1 = Relation.create(new C1(), new Target(1));
                        var relation2 = Relation.create(new C2(), new Target(2));

                        var entityId = archetype.create(relation1, relation2, new C3(), new C4(), new C5(), new C6(), new C7());
                        verifyHasComponents(entityId, expected);
                        verifyComponentMaskHasComponents(entityId, expected);
                    });
                }

                @Test
                void testMultipleComponentRelations_DifferentTargets() {
                    var type1 = relation(C1.class, Target.class);
                    var type2 = relation(C1.class, Target2.class);

                    var archetype = createArchetype(type1, type2, type(C3.class), type(C4.class), type(C5.class), type(C6.class), type(C7.class));

                    var expected = new RegularComponentType<?, ?>[] { type1, type2, type(C3.class), type(C4.class), type(C5.class), type(C6.class), type(C7.class) };

                    verify(verify -> {
                        verify.expectInserted(expected);
                        verify.expectNoMoreInserted();

                        var relation1 = Relation.create(new C1(), new Target(1));
                        var relation2 = Relation.create(new C1(), new Target2(2));

                        var entityId = archetype.create(relation1, relation2, new C3(), new C4(), new C5(), new C6(), new C7());
                        verifyHasComponents(entityId, expected);
                        verifyComponentMaskHasComponents(entityId, expected);
                    });
                }

                @Test
                void testMultipleComponentRelations_SameType_TargetsEqual() {
                    var type = relation(C1.class, Target.class);
                    var archetype = createArchetype(type, type, type(C3.class), type(C4.class), type(C5.class), type(C6.class), type(C7.class));

                    var expected = new RegularComponentType<?, ?>[] { type, type(C3.class), type(C4.class), type(C5.class), type(C6.class), type(C7.class) };

                    verify(verify -> {
                        verify.expectInserted(expected);
                        verify.expectNoMoreInserted();

                        var relationship = new C1();
                        var target1 = new Target(1);
                        var target2 = new Target(1);

                        var relation1 = Relation.create(relationship, target1);
                        var relation2 = Relation.create(relationship, target2);

                        var entityId = archetype.create(relation1, relation2, new C3(), new C4(), new C5(), new C6(), new C7());
                        verifyHasComponents(entityId, expected);
                        verifyComponentMaskHasComponents(entityId, expected);

                        var relations = relationMapperManager.getComponentRelationMapper(type).get(entityId);
                        assertThat(relations)
                                .extracting("relationship", "target")
                                .containsExactlyInAnyOrder(
                                        tuple(relationship, target2));
                    });
                }

                @Test
                void testMultipleComponentRelations_SameType_TargetsNotEqual() {
                    var type = relation(C1.class, Target.class);
                    var archetype = createArchetype(type, type, type(C3.class), type(C4.class), type(C5.class), type(C6.class), type(C7.class));

                    var expected = new RegularComponentType<?, ?>[] { type, type(C3.class), type(C4.class), type(C5.class), type(C6.class), type(C7.class) };

                    verify(verify -> {
                        verify.expectInserted(expected);
                        verify.expectNoMoreInserted();

                        var relationship = new C1();
                        var target1 = new Target(1);
                        var target2 = new Target(2);

                        var relation1 = Relation.create(relationship, target1);
                        var relation2 = Relation.create(relationship, target2);

                        var entityId = archetype.create(relation1, relation2, new C3(), new C4(), new C5(), new C6(), new C7());
                        verifyHasComponents(entityId, expected);
                        verifyComponentMaskHasComponents(entityId, expected);

                        var relations = relationMapperManager.getComponentRelationMapper(type).get(entityId);
                        assertThat(relations)
                                .extracting("relationship", "target")
                                .containsExactlyInAnyOrder(
                                        tuple(relationship, target1),
                                        tuple(relationship, target2));
                    });
                }

            }

            @Nested
            class EntityRelationTest {

                @Test
                void testComponentRelation() {
                    var type = relation(C1.class);
                    var archetype = createArchetype(type, type(C2.class), type(C3.class), type(C4.class), type(C5.class), type(C6.class), type(C7.class));

                    var expected = new RegularComponentType<?, ?>[] { type, type(C2.class), type(C3.class), type(C4.class), type(C5.class), type(C6.class), type(C7.class) };

                    var target = world.createEntity();

                    verify(verify -> {
                        verify.expectInserted(expected);
                        verify.expectNoMoreInserted();

                        var relation1 = Relation.create(new C1(), target);

                        var entityId = archetype.create(relation1, new C2(), new C3(), new C4(), new C5(), new C6(), new C7());
                        verifyHasComponents(entityId, expected);
                        verifyComponentMaskHasComponents(entityId, expected);
                    });
                }

                @Test
                void testMultipleComponentRelations_DifferentRelationships() {
                    var type1 = relation(C1.class);
                    var type2 = relation(C2.class);

                    var archetype = createArchetype(type1, type2, type(C3.class), type(C4.class), type(C5.class), type(C6.class), type(C7.class));

                    var expected = new RegularComponentType<?, ?>[] { type1, type2, type(C3.class), type(C4.class), type(C5.class), type(C6.class), type(C7.class) };

                    var target1 = world.createEntity();
                    var target2 = world.createEntity();

                    verify(verify -> {
                        verify.expectInserted(expected);
                        verify.expectNoMoreInserted();

                        var relation1 = Relation.create(new C1(), target1);
                        var relation2 = Relation.create(new C2(), target2);

                        var entityId = archetype.create(relation1, relation2, new C3(), new C4(), new C5(), new C6(), new C7());
                        verifyHasComponents(entityId, expected);
                        verifyComponentMaskHasComponents(entityId, expected);
                    });
                }

                @Test
                void testMultipleComponentRelations_SameType_TargetsEqual() {
                    var type = relation(C1.class);
                    var archetype = createArchetype(type, type, type(C3.class), type(C4.class), type(C5.class), type(C6.class), type(C7.class));

                    var expected = new RegularComponentType<?, ?>[] { type, type(C3.class), type(C4.class), type(C5.class), type(C6.class), type(C7.class) };

                    var target = world.createEntity();

                    verify(verify -> {
                        verify.expectInserted(expected);
                        verify.expectNoMoreInserted();

                        var relationship = new C1();

                        var relation1 = Relation.create(relationship, target);
                        var relation2 = Relation.create(relationship, target);

                        var entityId = archetype.create(relation1, relation2, new C3(), new C4(), new C5(), new C6(), new C7());
                        verifyHasComponents(entityId, expected);
                        verifyComponentMaskHasComponents(entityId, expected);

                        var relations = relationMapperManager.getEntityRelationMapper(type).get(entityId);
                        assertThat(relations)
                                .extracting("relationship", "target")
                                .containsExactlyInAnyOrder(
                                        tuple(relationship, target));
                    });
                }

                @Test
                void testMultipleComponentRelations_SameType_TargetsNotEqual() {
                    var type = relation(C1.class);
                    var archetype = createArchetype(type, type, type(C3.class), type(C4.class), type(C5.class), type(C6.class), type(C7.class));

                    var expected = new RegularComponentType<?, ?>[] { type, type(C3.class), type(C4.class), type(C5.class), type(C6.class), type(C7.class) };

                    var target1 = world.createEntity();
                    var target2 = world.createEntity();

                    verify(verify -> {
                        verify.expectInserted(expected);
                        verify.expectNoMoreInserted();

                        var relationship = new C1();

                        var relation1 = Relation.create(relationship, target1);
                        var relation2 = Relation.create(relationship, target2);

                        var entityId = archetype.create(relation1, relation2, new C3(), new C4(), new C5(), new C6(), new C7());
                        verifyHasComponents(entityId, expected);
                        verifyComponentMaskHasComponents(entityId, expected);

                        var relations = relationMapperManager.getEntityRelationMapper(type).get(entityId);
                        assertThat(relations)
                                .extracting("relationship", "target")
                                .containsExactlyInAnyOrder(
                                        tuple(relationship, target1),
                                        tuple(relationship, target2));
                    });
                }

            }

        }

    }

    @Nested
    class Archetype8Test {

        @Nested
        class SimpleArchetypeTest extends AbstractTest {
            @Override
            protected <T1, T2, T3, T4, T5, T6, T7, T8> Archetype8<T1, T2, T3, T4, T5, T6, T7, T8> createArchetype(RegularComponentType<T1, ?> component1, RegularComponentType<T2, ?> component2,
                    RegularComponentType<T3, ?> component3, RegularComponentType<T4, ?> component4, RegularComponentType<T5, ?> component5, RegularComponentType<T6, ?> component6,
                    RegularComponentType<T7, ?> component7, RegularComponentType<T8, ?> component8) {

                return world.createArchetype(component1, component2, component3, component4, component5, component6, component7, component8);
            }
        }

        @Nested
        class WithComponentsTest extends AbstractTest {

            @Override
            protected <T1, T2, T3, T4, T5, T6, T7, T8> Archetype8<T1, T2, T3, T4, T5, T6, T7, T8> createArchetype(RegularComponentType<T1, ?> component1, RegularComponentType<T2, ?> component2,
                    RegularComponentType<T3, ?> component3, RegularComponentType<T4, ?> component4, RegularComponentType<T5, ?> component5, RegularComponentType<T6, ?> component6,
                    RegularComponentType<T7, ?> component7, RegularComponentType<T8, ?> component8) {

                return world.createArchetype(component1, component2, component3, component4, component5, component6, component7, component8).with(E1.INSTANCE, E2.INSTANCE);
            }

            protected void verifyComponentMaskHasComponents(int entityId, Class<?>... components) {
                var expected = ArrayUtils.concat(Class.class, components, E1.class, E2.class);

                ArchetypeManagerTest.this.verifyComponentMaskHasComponents(entityId, expected);
            }

            @Test
            void testPooledComponents_Throws() {
                assertThatThrownBy(() -> world.createArchetype(C1.class, C2.class, C3.class, C4.class, C5.class, C6.class, C7.class, C8.class).with(new D1()))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContainingAll("D1", "cannot implement Pooled");
            }

            @Test
            void testDuplicateComponents_Throws() {
                assertThatThrownBy(() -> world.createArchetype(C1.class, C2.class, C3.class, C4.class, C5.class, C6.class, C7.class, C8.class).with(new C1()))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContainingAll("C1", "already defined");

                assertThatThrownBy(() -> world.createArchetype(C1.class, C2.class, C3.class, C4.class, C5.class, C6.class, C7.class, C8.class).with(E1.INSTANCE, E2.INSTANCE, E1.INSTANCE))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContainingAll("E1", "already defined");
            }

        }

        abstract class AbstractTest {

            protected final <T1, T2, T3, T4, T5, T6, T7, T8> Archetype8<T1, T2, T3, T4, T5, T6, T7, T8> createArchetype(Class<T1> component1, Class<T2> component2, Class<T3> component3,
                    Class<T4> component4, Class<T5> component5, Class<T6> component6, Class<T7> component7, Class<T8> component8) {

                return createArchetype(component(component1), component(component2), component(component3), component(component4), component(component5), component(component6), component(component7),
                        component(component8));
            }

            protected abstract <T1, T2, T3, T4, T5, T6, T7, T8> Archetype8<T1, T2, T3, T4, T5, T6, T7, T8> createArchetype(RegularComponentType<T1, ?> component1,
                    RegularComponentType<T2, ?> component2, RegularComponentType<T3, ?> component3, RegularComponentType<T4, ?> component4, RegularComponentType<T5, ?> component5,
                    RegularComponentType<T6, ?> component6, RegularComponentType<T7, ?> component7, RegularComponentType<T8, ?> component8);

            @Test
            void testArchetype() {
                var archetype = createArchetype(C1.class, C2.class, C3.class, C4.class, C5.class, C6.class, C7.class, C8.class);

                var entityId = archetype.create(new C1(), new C2(), new C3(), new C4(), new C5(), new C6(), new C7(), new C8());
                verifyHasComponents(entityId, C1.class, C2.class, C3.class, C4.class, C5.class, C6.class, C7.class, C8.class);
                verifyComponentMaskHasComponents(entityId, C1.class, C2.class, C3.class, C4.class, C5.class, C6.class, C7.class, C8.class);
            }

            @Test
            void testNullInstance() {
                var archetype = createArchetype(C1.class, C2.class, C3.class, C4.class, C5.class, C6.class, C7.class, C8.class);

                assertThatThrownBy(() -> archetype.create(null, new C2(), new C3(), new C4(), new C5(), new C6(), new C7(), new C8())).isNotNull();
                assertThatThrownBy(() -> archetype.create(new C1(), null, new C3(), new C4(), new C5(), new C6(), new C7(), new C8())).isNotNull();
                assertThatThrownBy(() -> archetype.create(new C1(), new C2(), null, new C4(), new C5(), new C6(), new C7(), new C8())).isNotNull();
                assertThatThrownBy(() -> archetype.create(new C1(), new C2(), new C3(), null, new C5(), new C6(), new C7(), new C8())).isNotNull();
                assertThatThrownBy(() -> archetype.create(new C1(), new C2(), new C3(), new C4(), null, new C6(), new C7(), new C8())).isNotNull();
                assertThatThrownBy(() -> archetype.create(new C1(), new C2(), new C3(), new C4(), new C5(), null, new C7(), new C8())).isNotNull();
                assertThatThrownBy(() -> archetype.create(new C1(), new C2(), new C3(), new C4(), new C5(), new C6(), null, new C8())).isNotNull();
                assertThatThrownBy(() -> archetype.create(new C1(), new C2(), new C3(), new C4(), new C5(), new C6(), new C7(), null)).isNotNull();
            }

            @Test
            void testBatch() {
                var archetype = createArchetype(C1.class, C2.class, C3.class, C4.class, C5.class, C6.class, C7.class, C8.class);

                var entityIds = archetype.createBatch(10, factory -> factory.create(new C1(), new C2(), new C3(), new C4(), new C5(), new C6(), new C7(), new C8()));
                assertThat(entityIds.getSize()).isEqualTo(10);

                for (var iter = entityIds.iterator(); iter.hasNext();) {
                    var entityId = iter.nextInt();
                    verifyHasComponents(entityId, C1.class, C2.class, C3.class, C4.class, C5.class, C6.class, C7.class, C8.class);
                    verifyComponentMaskHasComponents(entityId, C1.class, C2.class, C3.class, C4.class, C5.class, C6.class, C7.class, C8.class);
                }
            }

            @Test
            void testBatchChangeHandler() {
                var count = 10;
                var archetype = createArchetype(C1.class, C2.class, C3.class, C4.class, C5.class, C6.class, C7.class, C8.class);

                verify(verify -> {
                    for (int i = 0; i < count; i++) {
                        verify.expectInserted(C1.class, C2.class, C3.class, C4.class, C5.class, C6.class, C7.class, C8.class);
                    }

                    archetype.createBatch(count, factory -> factory.create(new C1(), new C2(), new C3(), new C4(), new C5(), new C6(), new C7(), new C8()));
                });
            }

            @Test
            void testBatch_NullInstances() {
                var archetype = createArchetype(C1.class, C2.class, C3.class, C4.class, C5.class, C6.class, C7.class, C8.class);

                assertThatThrownBy(() -> archetype.createBatch(10, factory -> factory.create(null, new C2(), new C3(), new C4(), new C5(), new C6(), new C7(), new C8())))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContainingAll("Component 1", "cannot be null");
                assertThatThrownBy(() -> archetype.createBatch(10, factory -> factory.create(new C1(), null, new C3(), new C4(), new C5(), new C6(), new C7(), new C8())))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContainingAll("Component 2", "cannot be null");
                assertThatThrownBy(() -> archetype.createBatch(10, factory -> factory.create(new C1(), new C2(), null, new C4(), new C5(), new C6(), new C7(), new C8())))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContainingAll("Component 3", "cannot be null");
                assertThatThrownBy(() -> archetype.createBatch(10, factory -> factory.create(new C1(), new C2(), new C3(), null, new C5(), new C6(), new C7(), new C8())))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContainingAll("Component 4", "cannot be null");
                assertThatThrownBy(() -> archetype.createBatch(10, factory -> factory.create(new C1(), new C2(), new C3(), new C4(), null, new C6(), new C7(), new C8())))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContainingAll("Component 5", "cannot be null");
                assertThatThrownBy(() -> archetype.createBatch(10, factory -> factory.create(new C1(), new C2(), new C3(), new C4(), new C5(), null, new C7(), new C8())))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContainingAll("Component 6", "cannot be null");
                assertThatThrownBy(() -> archetype.createBatch(10, factory -> factory.create(new C1(), new C2(), new C3(), new C4(), new C5(), new C6(), null, new C8())))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContainingAll("Component 7", "cannot be null");
                assertThatThrownBy(() -> archetype.createBatch(10, factory -> factory.create(new C1(), new C2(), new C3(), new C4(), new C5(), new C6(), new C7(), null)))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContainingAll("Component 8", "cannot be null");
            }

            @Test
            void testBatchInitializeNotCalled() {
                var archetype = createArchetype(C1.class, C2.class, C3.class, C4.class, C5.class, C6.class, C7.class, C8.class);

                assertThatThrownBy(() -> archetype.createBatch(10, factory -> null))
                        .isInstanceOf(NullPointerException.class)
                        .hasMessageContaining("return value cannot be null");
            }

            @Test
            void testGetInstance() {
                var archetype = createArchetype(C1.class, C2.class, C3.class, C4.class, C5.class, C6.class, C7.class, C8.class);
                assertThat(archetype.getInstance(D1.class)).isNotNull();
                assertThat(archetype.getInstance(D2.class)).isNotNull();
            }

            @Nested
            class ComponentRelationTest {

                @Test
                void testComponentRelation() {
                    var type = relation(C1.class, Target.class);
                    var archetype = createArchetype(type, type(C2.class), type(C3.class), type(C4.class), type(C5.class), type(C6.class), type(C7.class), type(C8.class));

                    var expected = new RegularComponentType<?, ?>[] { type, type(C2.class), type(C3.class), type(C4.class), type(C5.class), type(C6.class), type(C7.class), type(C8.class) };

                    verify(verify -> {
                        verify.expectInserted(expected);
                        verify.expectNoMoreInserted();

                        var relation1 = Relation.create(new C1(), new Target(1));

                        var entityId = archetype.create(relation1, new C2(), new C3(), new C4(), new C5(), new C6(), new C7(), new C8());
                        verifyHasComponents(entityId, expected);
                        verifyComponentMaskHasComponents(entityId, expected);
                    });
                }

                @Test
                void testMultipleComponentRelations_DifferentRelationships() {
                    var type1 = relation(C1.class, Target.class);
                    var type2 = relation(C2.class, Target.class);

                    var archetype = createArchetype(type1, type2, type(C3.class), type(C4.class), type(C5.class), type(C6.class), type(C7.class), type(C8.class));

                    var expected = new RegularComponentType<?, ?>[] { type1, type2, type(C3.class), type(C4.class), type(C5.class), type(C6.class), type(C7.class), type(C8.class) };

                    verify(verify -> {
                        verify.expectInserted(expected);
                        verify.expectNoMoreInserted();

                        var relation1 = Relation.create(new C1(), new Target(1));
                        var relation2 = Relation.create(new C2(), new Target(2));

                        var entityId = archetype.create(relation1, relation2, new C3(), new C4(), new C5(), new C6(), new C7(), new C8());
                        verifyHasComponents(entityId, expected);
                        verifyComponentMaskHasComponents(entityId, expected);
                    });
                }

                @Test
                void testMultipleComponentRelations_DifferentTargets() {
                    var type1 = relation(C1.class, Target.class);
                    var type2 = relation(C1.class, Target2.class);

                    var archetype = createArchetype(type1, type2, type(C3.class), type(C4.class), type(C5.class), type(C6.class), type(C7.class), type(C8.class));

                    var expected = new RegularComponentType<?, ?>[] { type1, type2, type(C3.class), type(C4.class), type(C5.class), type(C6.class), type(C7.class), type(C8.class) };

                    verify(verify -> {
                        verify.expectInserted(expected);
                        verify.expectNoMoreInserted();

                        var relation1 = Relation.create(new C1(), new Target(1));
                        var relation2 = Relation.create(new C1(), new Target2(2));

                        var entityId = archetype.create(relation1, relation2, new C3(), new C4(), new C5(), new C6(), new C7(), new C8());
                        verifyHasComponents(entityId, expected);
                        verifyComponentMaskHasComponents(entityId, expected);
                    });
                }

                @Test
                void testMultipleComponentRelations_SameType_TargetsEqual() {
                    var type = relation(C1.class, Target.class);
                    var archetype = createArchetype(type, type, type(C3.class), type(C4.class), type(C5.class), type(C6.class), type(C7.class), type(C8.class));

                    var expected = new RegularComponentType<?, ?>[] { type, type(C3.class), type(C4.class), type(C5.class), type(C6.class), type(C7.class), type(C8.class) };

                    verify(verify -> {
                        verify.expectInserted(expected);
                        verify.expectNoMoreInserted();

                        var relationship = new C1();
                        var target1 = new Target(1);
                        var target2 = new Target(1);

                        var relation1 = Relation.create(relationship, target1);
                        var relation2 = Relation.create(relationship, target2);

                        var entityId = archetype.create(relation1, relation2, new C3(), new C4(), new C5(), new C6(), new C7(), new C8());
                        verifyHasComponents(entityId, expected);
                        verifyComponentMaskHasComponents(entityId, expected);

                        var relations = relationMapperManager.getComponentRelationMapper(type).get(entityId);
                        assertThat(relations)
                                .extracting("relationship", "target")
                                .containsExactlyInAnyOrder(
                                        tuple(relationship, target2));
                    });
                }

                @Test
                void testMultipleComponentRelations_SameType_TargetsNotEqual() {
                    var type = relation(C1.class, Target.class);
                    var archetype = createArchetype(type, type, type(C3.class), type(C4.class), type(C5.class), type(C6.class), type(C7.class), type(C8.class));

                    var expected = new RegularComponentType<?, ?>[] { type, type(C3.class), type(C4.class), type(C5.class), type(C6.class), type(C7.class), type(C8.class) };

                    verify(verify -> {
                        verify.expectInserted(expected);
                        verify.expectNoMoreInserted();

                        var relationship = new C1();
                        var target1 = new Target(1);
                        var target2 = new Target(2);

                        var relation1 = Relation.create(relationship, target1);
                        var relation2 = Relation.create(relationship, target2);

                        var entityId = archetype.create(relation1, relation2, new C3(), new C4(), new C5(), new C6(), new C7(), new C8());
                        verifyHasComponents(entityId, expected);
                        verifyComponentMaskHasComponents(entityId, expected);

                        var relations = relationMapperManager.getComponentRelationMapper(type).get(entityId);
                        assertThat(relations)
                                .extracting("relationship", "target")
                                .containsExactlyInAnyOrder(
                                        tuple(relationship, target1),
                                        tuple(relationship, target2));
                    });
                }

            }

            @Nested
            class EntityRelationTest {

                @Test
                void testComponentRelation() {
                    var type = relation(C1.class);
                    var archetype = createArchetype(type, type(C2.class), type(C3.class), type(C4.class), type(C5.class), type(C6.class), type(C7.class), type(C8.class));

                    var expected = new RegularComponentType<?, ?>[] { type, type(C2.class), type(C3.class), type(C4.class), type(C5.class), type(C6.class), type(C7.class), type(C8.class) };

                    var target = world.createEntity();

                    verify(verify -> {
                        verify.expectInserted(expected);
                        verify.expectNoMoreInserted();

                        var relation1 = Relation.create(new C1(), target);

                        var entityId = archetype.create(relation1, new C2(), new C3(), new C4(), new C5(), new C6(), new C7(), new C8());
                        verifyHasComponents(entityId, expected);
                        verifyComponentMaskHasComponents(entityId, expected);
                    });
                }

                @Test
                void testMultipleComponentRelations_DifferentRelationships() {
                    var type1 = relation(C1.class);
                    var type2 = relation(C2.class);

                    var archetype = createArchetype(type1, type2, type(C3.class), type(C4.class), type(C5.class), type(C6.class), type(C7.class), type(C8.class));

                    var expected = new RegularComponentType<?, ?>[] { type1, type2, type(C3.class), type(C4.class), type(C5.class), type(C6.class), type(C7.class), type(C8.class) };

                    var target1 = world.createEntity();
                    var target2 = world.createEntity();

                    verify(verify -> {
                        verify.expectInserted(expected);
                        verify.expectNoMoreInserted();

                        var relation1 = Relation.create(new C1(), target1);
                        var relation2 = Relation.create(new C2(), target2);

                        var entityId = archetype.create(relation1, relation2, new C3(), new C4(), new C5(), new C6(), new C7(), new C8());
                        verifyHasComponents(entityId, expected);
                        verifyComponentMaskHasComponents(entityId, expected);
                    });
                }

                @Test
                void testMultipleComponentRelations_SameType_TargetsEqual() {
                    var type = relation(C1.class);
                    var archetype = createArchetype(type, type, type(C3.class), type(C4.class), type(C5.class), type(C6.class), type(C7.class), type(C8.class));

                    var expected = new RegularComponentType<?, ?>[] { type, type(C3.class), type(C4.class), type(C5.class), type(C6.class), type(C7.class), type(C8.class) };

                    var target = world.createEntity();

                    verify(verify -> {
                        verify.expectInserted(expected);
                        verify.expectNoMoreInserted();

                        var relationship = new C1();

                        var relation1 = Relation.create(relationship, target);
                        var relation2 = Relation.create(relationship, target);

                        var entityId = archetype.create(relation1, relation2, new C3(), new C4(), new C5(), new C6(), new C7(), new C8());
                        verifyHasComponents(entityId, expected);
                        verifyComponentMaskHasComponents(entityId, expected);

                        var relations = relationMapperManager.getEntityRelationMapper(type).get(entityId);
                        assertThat(relations)
                                .extracting("relationship", "target")
                                .containsExactlyInAnyOrder(
                                        tuple(relationship, target));
                    });
                }

                @Test
                void testMultipleComponentRelations_SameType_TargetsNotEqual() {
                    var type = relation(C1.class);
                    var archetype = createArchetype(type, type, type(C3.class), type(C4.class), type(C5.class), type(C6.class), type(C7.class), type(C8.class));

                    var expected = new RegularComponentType<?, ?>[] { type, type(C3.class), type(C4.class), type(C5.class), type(C6.class), type(C7.class), type(C8.class) };

                    var target1 = world.createEntity();
                    var target2 = world.createEntity();

                    verify(verify -> {
                        verify.expectInserted(expected);
                        verify.expectNoMoreInserted();

                        var relationship = new C1();

                        var relation1 = Relation.create(relationship, target1);
                        var relation2 = Relation.create(relationship, target2);

                        var entityId = archetype.create(relation1, relation2, new C3(), new C4(), new C5(), new C6(), new C7(), new C8());
                        verifyHasComponents(entityId, expected);
                        verifyComponentMaskHasComponents(entityId, expected);

                        var relations = relationMapperManager.getEntityRelationMapper(type).get(entityId);
                        assertThat(relations)
                                .extracting("relationship", "target")
                                .containsExactlyInAnyOrder(
                                        tuple(relationship, target1),
                                        tuple(relationship, target2));
                    });
                }

            }

        }

    }

    public record C1() {
    }

    public record C2() {
    }

    public record C3() {
    }

    public record C4() {
    }

    public record C5() {
    }

    public record C6() {
    }

    public record C7() {
    }

    public record C8() {
    }

    public record D1() implements Pooled {
    }

    public record D2() implements Pooled {
    }

    public record D3() implements Pooled {
    }

    public enum E1 {
        INSTANCE
    }

    public enum E2 {
        INSTANCE
    }

    record Target(int value) {
    }

    record Target2(int value) {
    }

}
