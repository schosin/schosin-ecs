package de.schosin.ecs.engine.entities;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import de.schosin.ecs.api.Pooled;
import de.schosin.ecs.api.archetype.Archetype;
import de.schosin.ecs.api.components.Composition;
import de.schosin.ecs.codegen.EcsCodegen;

@EcsCodegen
class ArchetypeManagerTest extends BaseArchetypeManagerTest {

    @SuppressWarnings("rawtypes")
    static final Archetype.Initialize NO_OP = (i, init) -> {
    };

    @Nested
    class Archetype1Test {

        @Nested
        class SimpleArchetypeTest extends AbstractTest {
            @Override
            protected <T1> Archetype.Of1<T1> createArchetype(Class<T1> component1) {
                return world.createArchetype(component1);
            }
        }

        @Nested
        class WithComponentsTest extends AbstractTest {

            @Override
            protected <T1> Archetype.Of1<T1> createArchetype(Class<T1> component1) {
                return world.createArchetype(component1).with(E1.INSTANCE, E2.INSTANCE);
            }

            protected void verifyHasComposition(int entityId, Composition.Builder builder) {
                var composition = world.createComposition(builder.all(E1.class, E2.class));

                ArchetypeManagerTest.this.verifyHasComposition(entityId, composition);
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

            protected abstract <T1> Archetype.Of1<T1> createArchetype(Class<T1> component1);

            protected void verifyHasComposition(int entityId, Composition.Builder builder) {
                var composition = world.createComposition(builder);

                ArchetypeManagerTest.this.verifyHasComposition(entityId, composition);
            }

            @Test
            void testArchetype() {
                var archetype = createArchetype(C1.class);

                var entityId = archetype.create(new C1());
                verifyHasComponent(entityId, C1.class);
                verifyHasComposition(entityId, Composition.all(C1.class));
            }

            @Test
            void testNullInstance() {
                var archetype = createArchetype(C1.class);

                assertThatThrownBy(() -> archetype.create(null)).isNotNull();
            }

            @Test
            void testBatch() {
                var archetype = createArchetype(C1.class);

                var entityIds = archetype.createBatch(10, (i, init) -> init.initialize(new C1()));
                assertThat(entityIds).hasSize(10);

                for (int i = 0; i < 10; i++) {
                    var entityId = entityIds[i];
                    verifyHasComponent(entityId, C1.class);
                    verifyHasComposition(entityId, Composition.all(C1.class));
                }
            }

            @Test
            void testBatchComposition() {
                var archetype = createArchetype(C1.class);

                var inserted = new ArrayList<Integer>();
                var composition = world.createComposition(Composition.all(C1.class));
                composition.inserted(inserted::add);

                var entityIds = archetype.createBatch(10, (i, init) -> init.initialize(new C1()));

                assertThat(inserted).hasSize(10);
                for (var entityId : entityIds) {
                    assertThat(inserted).contains(entityId);
                }
            }

            @Test
            void testBatch_NullInstances() {
                var archetype = createArchetype(C1.class);

                assertThatThrownBy(() -> archetype.createBatch(10, (i, init) -> init.initialize(null)))
                        .isInstanceOf(NullPointerException.class);
            }

            @Test
            @SuppressWarnings("unchecked")
            void testBatchInitializeNotCalled() {
                var archetype = createArchetype(C1.class);

                assertThatThrownBy(() -> archetype.createBatch(10, NO_OP))
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessageContaining("callback not called for entity");
            }

            @Test
            void testGetInstance() {
                var archetype = createArchetype(C1.class);
                assertThat(archetype.getInstance(D1.class)).isNotNull();
                assertThat(archetype.getInstance(D2.class)).isNotNull();
            }

        }

    }

    @Nested
    class Archetype2Test {

        @Nested
        class SimpleArchetypeTest extends AbstractTest {
            @Override
            protected <T1, T2> Archetype.Of2<T1, T2> createArchetype(Class<T1> component1, Class<T2> component2) {

                return world.createArchetype(component1, component2);
            }
        }

        @Nested
        class WithComponentsTest extends AbstractTest {

            @Override
            protected <T1, T2> Archetype.Of2<T1, T2> createArchetype(Class<T1> component1, Class<T2> component2) {

                return world.createArchetype(component1, component2).with(E1.INSTANCE, E2.INSTANCE);
            }

            protected void verifyHasComposition(int entityId, Composition.Builder builder) {
                var composition = world.createComposition(builder.all(E1.class, E2.class));

                ArchetypeManagerTest.this.verifyHasComposition(entityId, composition);
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

            protected abstract <T1, T2> Archetype.Of2<T1, T2> createArchetype(Class<T1> component1, Class<T2> component2);

            protected void verifyHasComposition(int entityId, Composition.Builder builder) {
                var composition = world.createComposition(builder);

                ArchetypeManagerTest.this.verifyHasComposition(entityId, composition);
            }

            @Test
            void testArchetype() {
                var archetype = createArchetype(C1.class, C2.class);

                var entityId = archetype.create(new C1(), new C2());
                verifyHasComponent(entityId, C1.class);
                verifyHasComponent(entityId, C2.class);
                verifyHasComposition(entityId, Composition.all(C1.class, C2.class));
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

                var entityIds = archetype.createBatch(10, (i, init) -> init.initialize(new C1(), new C2()));
                assertThat(entityIds).hasSize(10);

                for (int i = 0; i < 10; i++) {
                    var entityId = entityIds[i];
                    verifyHasComponent(entityId, C1.class);
                    verifyHasComponent(entityId, C2.class);
                    verifyHasComposition(entityId, Composition.all(C1.class, C2.class));
                }
            }

            @Test
            void testBatchComposition() {
                var archetype = createArchetype(C1.class, C2.class);

                var inserted = new ArrayList<Integer>();
                var composition = world.createComposition(Composition.all(C1.class, C2.class));
                composition.inserted(inserted::add);

                var entityIds = archetype.createBatch(10, (i, init) -> init.initialize(new C1(), new C2()));

                assertThat(inserted).hasSize(10);
                for (var entityId : entityIds) {
                    assertThat(inserted).contains(entityId);
                }
            }

            @Test
            void testBatch_NullInstances() {
                var archetype = createArchetype(C1.class, C2.class);

                assertThatThrownBy(() -> archetype.createBatch(10, (i, init) -> init.initialize(null, new C2())))
                        .isInstanceOf(NullPointerException.class);
                assertThatThrownBy(() -> archetype.createBatch(10, (i, init) -> init.initialize(new C1(), null)))
                        .isInstanceOf(NullPointerException.class);
            }

            @Test
            @SuppressWarnings("unchecked")
            void testBatchInitializeNotCalled() {
                var archetype = createArchetype(C1.class, C2.class);

                assertThatThrownBy(() -> archetype.createBatch(10, NO_OP))
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessageContaining("callback not called for entity");
            }

            @Test
            void testGetInstance() {
                var archetype = createArchetype(C1.class, C2.class);
                assertThat(archetype.getInstance(D1.class)).isNotNull();
                assertThat(archetype.getInstance(D2.class)).isNotNull();
            }

        }

    }

    @Nested
    class Archetype3Test {

        @Nested
        class SimpleArchetypeTest extends AbstractTest {
            @Override
            protected <T1, T2, T3> Archetype.Of3<T1, T2, T3> createArchetype(Class<T1> component1, Class<T2> component2, Class<T3> component3) {

                return world.createArchetype(component1, component2, component3);
            }
        }

        @Nested
        class WithComponentsTest extends AbstractTest {

            @Override
            protected <T1, T2, T3> Archetype.Of3<T1, T2, T3> createArchetype(Class<T1> component1, Class<T2> component2, Class<T3> component3) {

                return world.createArchetype(component1, component2, component3).with(E1.INSTANCE, E2.INSTANCE);
            }

            protected void verifyHasComposition(int entityId, Composition.Builder builder) {
                var composition = world.createComposition(builder.all(E1.class, E2.class));

                ArchetypeManagerTest.this.verifyHasComposition(entityId, composition);
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

            protected abstract <T1, T2, T3> Archetype.Of3<T1, T2, T3> createArchetype(Class<T1> component1, Class<T2> component2, Class<T3> component3);

            protected void verifyHasComposition(int entityId, Composition.Builder builder) {
                var composition = world.createComposition(builder);

                ArchetypeManagerTest.this.verifyHasComposition(entityId, composition);
            }

            @Test
            void testArchetype() {
                var archetype = createArchetype(C1.class, C2.class, C3.class);

                var entityId = archetype.create(new C1(), new C2(), new C3());
                verifyHasComponent(entityId, C1.class);
                verifyHasComponent(entityId, C2.class);
                verifyHasComponent(entityId, C3.class);
                verifyHasComposition(entityId, Composition.all(C1.class, C2.class, C3.class));
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

                var entityIds = archetype.createBatch(10, (i, init) -> init.initialize(new C1(), new C2(), new C3()));
                assertThat(entityIds).hasSize(10);

                for (int i = 0; i < 10; i++) {
                    var entityId = entityIds[i];
                    verifyHasComponent(entityId, C1.class);
                    verifyHasComponent(entityId, C2.class);
                    verifyHasComponent(entityId, C3.class);
                    verifyHasComposition(entityId, Composition.all(C1.class, C2.class, C3.class));
                }
            }

            @Test
            void testBatchComposition() {
                var archetype = createArchetype(C1.class, C2.class, C3.class);

                var inserted = new ArrayList<Integer>();
                var composition = world.createComposition(Composition.all(C1.class, C2.class, C3.class));
                composition.inserted(inserted::add);

                var entityIds = archetype.createBatch(10, (i, init) -> init.initialize(new C1(), new C2(), new C3()));

                assertThat(inserted).hasSize(10);
                for (var entityId : entityIds) {
                    assertThat(inserted).contains(entityId);
                }
            }

            @Test
            void testBatch_NullInstances() {
                var archetype = createArchetype(C1.class, C2.class, C3.class);

                assertThatThrownBy(() -> archetype.createBatch(10, (i, init) -> init.initialize(null, new C2(), new C3())))
                        .isInstanceOf(NullPointerException.class);
                assertThatThrownBy(() -> archetype.createBatch(10, (i, init) -> init.initialize(new C1(), null, new C3())))
                        .isInstanceOf(NullPointerException.class);
                assertThatThrownBy(() -> archetype.createBatch(10, (i, init) -> init.initialize(new C1(), new C2(), null)))
                        .isInstanceOf(NullPointerException.class);
            }

            @Test
            @SuppressWarnings("unchecked")
            void testBatchInitializeNotCalled() {
                var archetype = createArchetype(C1.class, C2.class, C3.class);

                assertThatThrownBy(() -> archetype.createBatch(10, NO_OP))
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessageContaining("callback not called for entity");
            }

            @Test
            void testGetInstance() {
                var archetype = createArchetype(C1.class, C2.class, C3.class);
                assertThat(archetype.getInstance(D1.class)).isNotNull();
                assertThat(archetype.getInstance(D2.class)).isNotNull();
            }

        }

    }

    @Nested
    class Archetype4Test {

        @Nested
        class SimpleArchetypeTest extends AbstractTest {
            @Override
            protected <T1, T2, T3, T4> Archetype.Of4<T1, T2, T3, T4> createArchetype(Class<T1> component1, Class<T2> component2, Class<T3> component3, Class<T4> component4) {

                return world.createArchetype(component1, component2, component3, component4);
            }
        }

        @Nested
        class WithComponentsTest extends AbstractTest {

            @Override
            protected <T1, T2, T3, T4> Archetype.Of4<T1, T2, T3, T4> createArchetype(Class<T1> component1, Class<T2> component2, Class<T3> component3, Class<T4> component4) {

                return world.createArchetype(component1, component2, component3, component4).with(E1.INSTANCE, E2.INSTANCE);
            }

            protected void verifyHasComposition(int entityId, Composition.Builder builder) {
                var composition = world.createComposition(builder.all(E1.class, E2.class));

                ArchetypeManagerTest.this.verifyHasComposition(entityId, composition);
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

            protected abstract <T1, T2, T3, T4> Archetype.Of4<T1, T2, T3, T4> createArchetype(Class<T1> component1, Class<T2> component2, Class<T3> component3, Class<T4> component4);

            protected void verifyHasComposition(int entityId, Composition.Builder builder) {
                var composition = world.createComposition(builder);

                ArchetypeManagerTest.this.verifyHasComposition(entityId, composition);
            }

            @Test
            void testArchetype() {
                var archetype = createArchetype(C1.class, C2.class, C3.class, C4.class);

                var entityId = archetype.create(new C1(), new C2(), new C3(), new C4());
                verifyHasComponent(entityId, C1.class);
                verifyHasComponent(entityId, C2.class);
                verifyHasComponent(entityId, C3.class);
                verifyHasComponent(entityId, C4.class);
                verifyHasComposition(entityId, Composition.all(C1.class, C2.class, C3.class, C4.class));
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

                var entityIds = archetype.createBatch(10, (i, init) -> init.initialize(new C1(), new C2(), new C3(), new C4()));
                assertThat(entityIds).hasSize(10);

                for (int i = 0; i < 10; i++) {
                    var entityId = entityIds[i];
                    verifyHasComponent(entityId, C1.class);
                    verifyHasComponent(entityId, C2.class);
                    verifyHasComponent(entityId, C3.class);
                    verifyHasComponent(entityId, C4.class);
                    verifyHasComposition(entityId, Composition.all(C1.class, C2.class, C3.class, C4.class));
                }
            }

            @Test
            void testBatchComposition() {
                var archetype = createArchetype(C1.class, C2.class, C3.class, C4.class);

                var inserted = new ArrayList<Integer>();
                var composition = world.createComposition(Composition.all(C1.class, C2.class, C3.class, C4.class));
                composition.inserted(inserted::add);

                var entityIds = archetype.createBatch(10, (i, init) -> init.initialize(new C1(), new C2(), new C3(), new C4()));

                assertThat(inserted).hasSize(10);
                for (var entityId : entityIds) {
                    assertThat(inserted).contains(entityId);
                }
            }

            @Test
            void testBatch_NullInstances() {
                var archetype = createArchetype(C1.class, C2.class, C3.class, C4.class);

                assertThatThrownBy(() -> archetype.createBatch(10, (i, init) -> init.initialize(null, new C2(), new C3(), new C4())))
                        .isInstanceOf(NullPointerException.class);
                assertThatThrownBy(() -> archetype.createBatch(10, (i, init) -> init.initialize(new C1(), null, new C3(), new C4())))
                        .isInstanceOf(NullPointerException.class);
                assertThatThrownBy(() -> archetype.createBatch(10, (i, init) -> init.initialize(new C1(), new C2(), null, new C4())))
                        .isInstanceOf(NullPointerException.class);
                assertThatThrownBy(() -> archetype.createBatch(10, (i, init) -> init.initialize(new C1(), new C2(), new C3(), null)))
                        .isInstanceOf(NullPointerException.class);
            }

            @Test
            @SuppressWarnings("unchecked")
            void testBatchInitializeNotCalled() {
                var archetype = createArchetype(C1.class, C2.class, C3.class, C4.class);

                assertThatThrownBy(() -> archetype.createBatch(10, NO_OP))
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessageContaining("callback not called for entity");
            }

            @Test
            void testGetInstance() {
                var archetype = createArchetype(C1.class, C2.class, C3.class, C4.class);
                assertThat(archetype.getInstance(D1.class)).isNotNull();
                assertThat(archetype.getInstance(D2.class)).isNotNull();
            }

        }

    }

    @Nested
    class Archetype5Test {

        @Nested
        class SimpleArchetypeTest extends AbstractTest {
            @Override
            protected <T1, T2, T3, T4, T5> Archetype.Of5<T1, T2, T3, T4, T5> createArchetype(Class<T1> component1, Class<T2> component2, Class<T3> component3, Class<T4> component4,
                    Class<T5> component5) {

                return world.createArchetype(component1, component2, component3, component4, component5);
            }
        }

        @Nested
        class WithComponentsTest extends AbstractTest {

            @Override
            protected <T1, T2, T3, T4, T5> Archetype.Of5<T1, T2, T3, T4, T5> createArchetype(Class<T1> component1, Class<T2> component2, Class<T3> component3, Class<T4> component4,
                    Class<T5> component5) {

                return world.createArchetype(component1, component2, component3, component4, component5).with(E1.INSTANCE, E2.INSTANCE);
            }

            protected void verifyHasComposition(int entityId, Composition.Builder builder) {
                var composition = world.createComposition(builder.all(E1.class, E2.class));

                ArchetypeManagerTest.this.verifyHasComposition(entityId, composition);
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

            protected abstract <T1, T2, T3, T4, T5> Archetype.Of5<T1, T2, T3, T4, T5> createArchetype(Class<T1> component1, Class<T2> component2, Class<T3> component3, Class<T4> component4,
                    Class<T5> component5);

            protected void verifyHasComposition(int entityId, Composition.Builder builder) {
                var composition = world.createComposition(builder);

                ArchetypeManagerTest.this.verifyHasComposition(entityId, composition);
            }

            @Test
            void testArchetype() {
                var archetype = createArchetype(C1.class, C2.class, C3.class, C4.class, C5.class);

                var entityId = archetype.create(new C1(), new C2(), new C3(), new C4(), new C5());
                verifyHasComponent(entityId, C1.class);
                verifyHasComponent(entityId, C2.class);
                verifyHasComponent(entityId, C3.class);
                verifyHasComponent(entityId, C4.class);
                verifyHasComponent(entityId, C5.class);
                verifyHasComposition(entityId, Composition.all(C1.class, C2.class, C3.class, C4.class, C5.class));
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

                var entityIds = archetype.createBatch(10, (i, init) -> init.initialize(new C1(), new C2(), new C3(), new C4(), new C5()));
                assertThat(entityIds).hasSize(10);

                for (int i = 0; i < 10; i++) {
                    var entityId = entityIds[i];
                    verifyHasComponent(entityId, C1.class);
                    verifyHasComponent(entityId, C2.class);
                    verifyHasComponent(entityId, C3.class);
                    verifyHasComponent(entityId, C4.class);
                    verifyHasComponent(entityId, C5.class);
                    verifyHasComposition(entityId, Composition.all(C1.class, C2.class, C3.class, C4.class, C5.class));
                }
            }

            @Test
            void testBatchComposition() {
                var archetype = createArchetype(C1.class, C2.class, C3.class, C4.class, C5.class);

                var inserted = new ArrayList<Integer>();
                var composition = world.createComposition(Composition.all(C1.class, C2.class, C3.class, C4.class, C5.class));
                composition.inserted(inserted::add);

                var entityIds = archetype.createBatch(10, (i, init) -> init.initialize(new C1(), new C2(), new C3(), new C4(), new C5()));

                assertThat(inserted).hasSize(10);
                for (var entityId : entityIds) {
                    assertThat(inserted).contains(entityId);
                }
            }

            @Test
            void testBatch_NullInstances() {
                var archetype = createArchetype(C1.class, C2.class, C3.class, C4.class, C5.class);

                assertThatThrownBy(() -> archetype.createBatch(10, (i, init) -> init.initialize(null, new C2(), new C3(), new C4(), new C5())))
                        .isInstanceOf(NullPointerException.class);
                assertThatThrownBy(() -> archetype.createBatch(10, (i, init) -> init.initialize(new C1(), null, new C3(), new C4(), new C5())))
                        .isInstanceOf(NullPointerException.class);
                assertThatThrownBy(() -> archetype.createBatch(10, (i, init) -> init.initialize(new C1(), new C2(), null, new C4(), new C5())))
                        .isInstanceOf(NullPointerException.class);
                assertThatThrownBy(() -> archetype.createBatch(10, (i, init) -> init.initialize(new C1(), new C2(), new C3(), null, new C5())))
                        .isInstanceOf(NullPointerException.class);
                assertThatThrownBy(() -> archetype.createBatch(10, (i, init) -> init.initialize(new C1(), new C2(), new C3(), new C4(), null)))
                        .isInstanceOf(NullPointerException.class);
            }

            @Test
            @SuppressWarnings("unchecked")
            void testBatchInitializeNotCalled() {
                var archetype = createArchetype(C1.class, C2.class, C3.class, C4.class, C5.class);

                assertThatThrownBy(() -> archetype.createBatch(10, NO_OP))
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessageContaining("callback not called for entity");
            }

            @Test
            void testGetInstance() {
                var archetype = createArchetype(C1.class, C2.class, C3.class, C4.class, C5.class);
                assertThat(archetype.getInstance(D1.class)).isNotNull();
                assertThat(archetype.getInstance(D2.class)).isNotNull();
            }

        }

    }

    @Nested
    class Archetype6Test {

        @Nested
        class SimpleArchetypeTest extends AbstractTest {
            @Override
            protected <T1, T2, T3, T4, T5, T6> Archetype.Of6<T1, T2, T3, T4, T5, T6> createArchetype(Class<T1> component1, Class<T2> component2, Class<T3> component3, Class<T4> component4,
                    Class<T5> component5, Class<T6> component6) {

                return world.createArchetype(component1, component2, component3, component4, component5, component6);
            }
        }

        @Nested
        class WithComponentsTest extends AbstractTest {

            @Override
            protected <T1, T2, T3, T4, T5, T6> Archetype.Of6<T1, T2, T3, T4, T5, T6> createArchetype(Class<T1> component1, Class<T2> component2, Class<T3> component3, Class<T4> component4,
                    Class<T5> component5, Class<T6> component6) {

                return world.createArchetype(component1, component2, component3, component4, component5, component6).with(E1.INSTANCE, E2.INSTANCE);
            }

            protected void verifyHasComposition(int entityId, Composition.Builder builder) {
                var composition = world.createComposition(builder.all(E1.class, E2.class));

                ArchetypeManagerTest.this.verifyHasComposition(entityId, composition);
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

            protected abstract <T1, T2, T3, T4, T5, T6> Archetype.Of6<T1, T2, T3, T4, T5, T6> createArchetype(Class<T1> component1, Class<T2> component2, Class<T3> component3, Class<T4> component4,
                    Class<T5> component5, Class<T6> component6);

            protected void verifyHasComposition(int entityId, Composition.Builder builder) {
                var composition = world.createComposition(builder);

                ArchetypeManagerTest.this.verifyHasComposition(entityId, composition);
            }

            @Test
            void testArchetype() {
                var archetype = createArchetype(C1.class, C2.class, C3.class, C4.class, C5.class, C6.class);

                var entityId = archetype.create(new C1(), new C2(), new C3(), new C4(), new C5(), new C6());
                verifyHasComponent(entityId, C1.class);
                verifyHasComponent(entityId, C2.class);
                verifyHasComponent(entityId, C3.class);
                verifyHasComponent(entityId, C4.class);
                verifyHasComponent(entityId, C5.class);
                verifyHasComponent(entityId, C6.class);
                verifyHasComposition(entityId, Composition.all(C1.class, C2.class, C3.class, C4.class, C5.class, C6.class));
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

                var entityIds = archetype.createBatch(10, (i, init) -> init.initialize(new C1(), new C2(), new C3(), new C4(), new C5(), new C6()));
                assertThat(entityIds).hasSize(10);

                for (int i = 0; i < 10; i++) {
                    var entityId = entityIds[i];
                    verifyHasComponent(entityId, C1.class);
                    verifyHasComponent(entityId, C2.class);
                    verifyHasComponent(entityId, C3.class);
                    verifyHasComponent(entityId, C4.class);
                    verifyHasComponent(entityId, C5.class);
                    verifyHasComponent(entityId, C6.class);
                    verifyHasComposition(entityId, Composition.all(C1.class, C2.class, C3.class, C4.class, C5.class, C6.class));
                }
            }

            @Test
            void testBatchComposition() {
                var archetype = createArchetype(C1.class, C2.class, C3.class, C4.class, C5.class, C6.class);

                var inserted = new ArrayList<Integer>();
                var composition = world.createComposition(Composition.all(C1.class, C2.class, C3.class, C4.class, C5.class, C6.class));
                composition.inserted(inserted::add);

                var entityIds = archetype.createBatch(10, (i, init) -> init.initialize(new C1(), new C2(), new C3(), new C4(), new C5(), new C6()));

                assertThat(inserted).hasSize(10);
                for (var entityId : entityIds) {
                    assertThat(inserted).contains(entityId);
                }
            }

            @Test
            void testBatch_NullInstances() {
                var archetype = createArchetype(C1.class, C2.class, C3.class, C4.class, C5.class, C6.class);

                assertThatThrownBy(() -> archetype.createBatch(10, (i, init) -> init.initialize(null, new C2(), new C3(), new C4(), new C5(), new C6())))
                        .isInstanceOf(NullPointerException.class);
                assertThatThrownBy(() -> archetype.createBatch(10, (i, init) -> init.initialize(new C1(), null, new C3(), new C4(), new C5(), new C6())))
                        .isInstanceOf(NullPointerException.class);
                assertThatThrownBy(() -> archetype.createBatch(10, (i, init) -> init.initialize(new C1(), new C2(), null, new C4(), new C5(), new C6())))
                        .isInstanceOf(NullPointerException.class);
                assertThatThrownBy(() -> archetype.createBatch(10, (i, init) -> init.initialize(new C1(), new C2(), new C3(), null, new C5(), new C6())))
                        .isInstanceOf(NullPointerException.class);
                assertThatThrownBy(() -> archetype.createBatch(10, (i, init) -> init.initialize(new C1(), new C2(), new C3(), new C4(), null, new C6())))
                        .isInstanceOf(NullPointerException.class);
                assertThatThrownBy(() -> archetype.createBatch(10, (i, init) -> init.initialize(new C1(), new C2(), new C3(), new C4(), new C5(), null)))
                        .isInstanceOf(NullPointerException.class);
            }

            @Test
            @SuppressWarnings("unchecked")
            void testBatchInitializeNotCalled() {
                var archetype = createArchetype(C1.class, C2.class, C3.class, C4.class, C5.class, C6.class);

                assertThatThrownBy(() -> archetype.createBatch(10, NO_OP))
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessageContaining("callback not called for entity");
            }

            @Test
            void testGetInstance() {
                var archetype = createArchetype(C1.class, C2.class, C3.class, C4.class, C5.class, C6.class);
                assertThat(archetype.getInstance(D1.class)).isNotNull();
                assertThat(archetype.getInstance(D2.class)).isNotNull();
            }

        }

    }

    @Nested
    class Archetype7Test {

        @Nested
        class SimpleArchetypeTest extends AbstractTest {
            @Override
            protected <T1, T2, T3, T4, T5, T6, T7> Archetype.Of7<T1, T2, T3, T4, T5, T6, T7> createArchetype(Class<T1> component1, Class<T2> component2, Class<T3> component3, Class<T4> component4,
                    Class<T5> component5, Class<T6> component6, Class<T7> component7) {

                return world.createArchetype(component1, component2, component3, component4, component5, component6, component7);
            }
        }

        @Nested
        class WithComponentsTest extends AbstractTest {

            @Override
            protected <T1, T2, T3, T4, T5, T6, T7> Archetype.Of7<T1, T2, T3, T4, T5, T6, T7> createArchetype(Class<T1> component1, Class<T2> component2, Class<T3> component3, Class<T4> component4,
                    Class<T5> component5, Class<T6> component6, Class<T7> component7) {

                return world.createArchetype(component1, component2, component3, component4, component5, component6, component7).with(E1.INSTANCE, E2.INSTANCE);
            }

            protected void verifyHasComposition(int entityId, Composition.Builder builder) {
                var composition = world.createComposition(builder.all(E1.class, E2.class));

                ArchetypeManagerTest.this.verifyHasComposition(entityId, composition);
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

            protected abstract <T1, T2, T3, T4, T5, T6, T7> Archetype.Of7<T1, T2, T3, T4, T5, T6, T7> createArchetype(Class<T1> component1, Class<T2> component2, Class<T3> component3,
                    Class<T4> component4, Class<T5> component5, Class<T6> component6, Class<T7> component7);

            protected void verifyHasComposition(int entityId, Composition.Builder builder) {
                var composition = world.createComposition(builder);

                ArchetypeManagerTest.this.verifyHasComposition(entityId, composition);
            }

            @Test
            void testArchetype() {
                var archetype = createArchetype(C1.class, C2.class, C3.class, C4.class, C5.class, C6.class, C7.class);

                var entityId = archetype.create(new C1(), new C2(), new C3(), new C4(), new C5(), new C6(), new C7());
                verifyHasComponent(entityId, C1.class);
                verifyHasComponent(entityId, C2.class);
                verifyHasComponent(entityId, C3.class);
                verifyHasComponent(entityId, C4.class);
                verifyHasComponent(entityId, C5.class);
                verifyHasComponent(entityId, C6.class);
                verifyHasComponent(entityId, C7.class);
                verifyHasComposition(entityId, Composition.all(C1.class, C2.class, C3.class, C4.class, C5.class, C6.class, C7.class));
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

                var entityIds = archetype.createBatch(10, (i, init) -> init.initialize(new C1(), new C2(), new C3(), new C4(), new C5(), new C6(), new C7()));
                assertThat(entityIds).hasSize(10);

                for (int i = 0; i < 10; i++) {
                    var entityId = entityIds[i];
                    verifyHasComponent(entityId, C1.class);
                    verifyHasComponent(entityId, C2.class);
                    verifyHasComponent(entityId, C3.class);
                    verifyHasComponent(entityId, C4.class);
                    verifyHasComponent(entityId, C5.class);
                    verifyHasComponent(entityId, C6.class);
                    verifyHasComponent(entityId, C7.class);
                    verifyHasComposition(entityId, Composition.all(C1.class, C2.class, C3.class, C4.class, C5.class, C6.class, C7.class));
                }
            }

            @Test
            void testBatchComposition() {
                var archetype = createArchetype(C1.class, C2.class, C3.class, C4.class, C5.class, C6.class, C7.class);

                var inserted = new ArrayList<Integer>();
                var composition = world.createComposition(Composition.all(C1.class, C2.class, C3.class, C4.class, C5.class, C6.class, C7.class));
                composition.inserted(inserted::add);

                var entityIds = archetype.createBatch(10, (i, init) -> init.initialize(new C1(), new C2(), new C3(), new C4(), new C5(), new C6(), new C7()));

                assertThat(inserted).hasSize(10);
                for (var entityId : entityIds) {
                    assertThat(inserted).contains(entityId);
                }
            }

            @Test
            void testBatch_NullInstances() {
                var archetype = createArchetype(C1.class, C2.class, C3.class, C4.class, C5.class, C6.class, C7.class);

                assertThatThrownBy(() -> archetype.createBatch(10, (i, init) -> init.initialize(null, new C2(), new C3(), new C4(), new C5(), new C6(), new C7())))
                        .isInstanceOf(NullPointerException.class);
                assertThatThrownBy(() -> archetype.createBatch(10, (i, init) -> init.initialize(new C1(), null, new C3(), new C4(), new C5(), new C6(), new C7())))
                        .isInstanceOf(NullPointerException.class);
                assertThatThrownBy(() -> archetype.createBatch(10, (i, init) -> init.initialize(new C1(), new C2(), null, new C4(), new C5(), new C6(), new C7())))
                        .isInstanceOf(NullPointerException.class);
                assertThatThrownBy(() -> archetype.createBatch(10, (i, init) -> init.initialize(new C1(), new C2(), new C3(), null, new C5(), new C6(), new C7())))
                        .isInstanceOf(NullPointerException.class);
                assertThatThrownBy(() -> archetype.createBatch(10, (i, init) -> init.initialize(new C1(), new C2(), new C3(), new C4(), null, new C6(), new C7())))
                        .isInstanceOf(NullPointerException.class);
                assertThatThrownBy(() -> archetype.createBatch(10, (i, init) -> init.initialize(new C1(), new C2(), new C3(), new C4(), new C5(), null, new C7())))
                        .isInstanceOf(NullPointerException.class);
                assertThatThrownBy(() -> archetype.createBatch(10, (i, init) -> init.initialize(new C1(), new C2(), new C3(), new C4(), new C5(), new C6(), null)))
                        .isInstanceOf(NullPointerException.class);
            }

            @Test
            @SuppressWarnings("unchecked")
            void testBatchInitializeNotCalled() {
                var archetype = createArchetype(C1.class, C2.class, C3.class, C4.class, C5.class, C6.class, C7.class);

                assertThatThrownBy(() -> archetype.createBatch(10, NO_OP))
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessageContaining("callback not called for entity");
            }

            @Test
            void testGetInstance() {
                var archetype = createArchetype(C1.class, C2.class, C3.class, C4.class, C5.class, C6.class, C7.class);
                assertThat(archetype.getInstance(D1.class)).isNotNull();
                assertThat(archetype.getInstance(D2.class)).isNotNull();
            }

        }

    }

    @Nested
    class Archetype8Test {

        @Nested
        class SimpleArchetypeTest extends AbstractTest {
            @Override
            protected <T1, T2, T3, T4, T5, T6, T7, T8> Archetype.Of8<T1, T2, T3, T4, T5, T6, T7, T8> createArchetype(Class<T1> component1, Class<T2> component2, Class<T3> component3,
                    Class<T4> component4, Class<T5> component5, Class<T6> component6, Class<T7> component7, Class<T8> component8) {

                return world.createArchetype(component1, component2, component3, component4, component5, component6, component7, component8);
            }
        }

        @Nested
        class WithComponentsTest extends AbstractTest {

            @Override
            protected <T1, T2, T3, T4, T5, T6, T7, T8> Archetype.Of8<T1, T2, T3, T4, T5, T6, T7, T8> createArchetype(Class<T1> component1, Class<T2> component2, Class<T3> component3,
                    Class<T4> component4, Class<T5> component5, Class<T6> component6, Class<T7> component7, Class<T8> component8) {

                return world.createArchetype(component1, component2, component3, component4, component5, component6, component7, component8).with(E1.INSTANCE, E2.INSTANCE);
            }

            protected void verifyHasComposition(int entityId, Composition.Builder builder) {
                var composition = world.createComposition(builder.all(E1.class, E2.class));

                ArchetypeManagerTest.this.verifyHasComposition(entityId, composition);
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

            protected abstract <T1, T2, T3, T4, T5, T6, T7, T8> Archetype.Of8<T1, T2, T3, T4, T5, T6, T7, T8> createArchetype(Class<T1> component1, Class<T2> component2, Class<T3> component3,
                    Class<T4> component4, Class<T5> component5, Class<T6> component6, Class<T7> component7, Class<T8> component8);

            protected void verifyHasComposition(int entityId, Composition.Builder builder) {
                var composition = world.createComposition(builder);

                ArchetypeManagerTest.this.verifyHasComposition(entityId, composition);
            }

            @Test
            void testArchetype() {
                var archetype = createArchetype(C1.class, C2.class, C3.class, C4.class, C5.class, C6.class, C7.class, C8.class);

                var entityId = archetype.create(new C1(), new C2(), new C3(), new C4(), new C5(), new C6(), new C7(), new C8());
                verifyHasComponent(entityId, C1.class);
                verifyHasComponent(entityId, C2.class);
                verifyHasComponent(entityId, C3.class);
                verifyHasComponent(entityId, C4.class);
                verifyHasComponent(entityId, C5.class);
                verifyHasComponent(entityId, C6.class);
                verifyHasComponent(entityId, C7.class);
                verifyHasComponent(entityId, C8.class);
                verifyHasComposition(entityId, Composition.all(C1.class, C2.class, C3.class, C4.class, C5.class, C6.class, C7.class, C8.class));
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

                var entityIds = archetype.createBatch(10, (i, init) -> init.initialize(new C1(), new C2(), new C3(), new C4(), new C5(), new C6(), new C7(), new C8()));
                assertThat(entityIds).hasSize(10);

                for (int i = 0; i < 10; i++) {
                    var entityId = entityIds[i];
                    verifyHasComponent(entityId, C1.class);
                    verifyHasComponent(entityId, C2.class);
                    verifyHasComponent(entityId, C3.class);
                    verifyHasComponent(entityId, C4.class);
                    verifyHasComponent(entityId, C5.class);
                    verifyHasComponent(entityId, C6.class);
                    verifyHasComponent(entityId, C7.class);
                    verifyHasComponent(entityId, C8.class);
                    verifyHasComposition(entityId, Composition.all(C1.class, C2.class, C3.class, C4.class, C5.class, C6.class, C7.class, C8.class));
                }
            }

            @Test
            void testBatchComposition() {
                var archetype = createArchetype(C1.class, C2.class, C3.class, C4.class, C5.class, C6.class, C7.class, C8.class);

                var inserted = new ArrayList<Integer>();
                var composition = world.createComposition(Composition.all(C1.class, C2.class, C3.class, C4.class, C5.class, C6.class, C7.class, C8.class));
                composition.inserted(inserted::add);

                var entityIds = archetype.createBatch(10, (i, init) -> init.initialize(new C1(), new C2(), new C3(), new C4(), new C5(), new C6(), new C7(), new C8()));

                assertThat(inserted).hasSize(10);
                for (var entityId : entityIds) {
                    assertThat(inserted).contains(entityId);
                }
            }

            @Test
            void testBatch_NullInstances() {
                var archetype = createArchetype(C1.class, C2.class, C3.class, C4.class, C5.class, C6.class, C7.class, C8.class);

                assertThatThrownBy(() -> archetype.createBatch(10, (i, init) -> init.initialize(null, new C2(), new C3(), new C4(), new C5(), new C6(), new C7(), new C8())))
                        .isInstanceOf(NullPointerException.class);
                assertThatThrownBy(() -> archetype.createBatch(10, (i, init) -> init.initialize(new C1(), null, new C3(), new C4(), new C5(), new C6(), new C7(), new C8())))
                        .isInstanceOf(NullPointerException.class);
                assertThatThrownBy(() -> archetype.createBatch(10, (i, init) -> init.initialize(new C1(), new C2(), null, new C4(), new C5(), new C6(), new C7(), new C8())))
                        .isInstanceOf(NullPointerException.class);
                assertThatThrownBy(() -> archetype.createBatch(10, (i, init) -> init.initialize(new C1(), new C2(), new C3(), null, new C5(), new C6(), new C7(), new C8())))
                        .isInstanceOf(NullPointerException.class);
                assertThatThrownBy(() -> archetype.createBatch(10, (i, init) -> init.initialize(new C1(), new C2(), new C3(), new C4(), null, new C6(), new C7(), new C8())))
                        .isInstanceOf(NullPointerException.class);
                assertThatThrownBy(() -> archetype.createBatch(10, (i, init) -> init.initialize(new C1(), new C2(), new C3(), new C4(), new C5(), null, new C7(), new C8())))
                        .isInstanceOf(NullPointerException.class);
                assertThatThrownBy(() -> archetype.createBatch(10, (i, init) -> init.initialize(new C1(), new C2(), new C3(), new C4(), new C5(), new C6(), null, new C8())))
                        .isInstanceOf(NullPointerException.class);
                assertThatThrownBy(() -> archetype.createBatch(10, (i, init) -> init.initialize(new C1(), new C2(), new C3(), new C4(), new C5(), new C6(), new C7(), null)))
                        .isInstanceOf(NullPointerException.class);
            }

            @Test
            @SuppressWarnings("unchecked")
            void testBatchInitializeNotCalled() {
                var archetype = createArchetype(C1.class, C2.class, C3.class, C4.class, C5.class, C6.class, C7.class, C8.class);

                assertThatThrownBy(() -> archetype.createBatch(10, NO_OP))
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessageContaining("callback not called for entity");
            }

            @Test
            void testGetInstance() {
                var archetype = createArchetype(C1.class, C2.class, C3.class, C4.class, C5.class, C6.class, C7.class, C8.class);
                assertThat(archetype.getInstance(D1.class)).isNotNull();
                assertThat(archetype.getInstance(D2.class)).isNotNull();
            }

        }

    }

    @Nested
    class ArchetypeNTest extends BaseArchetypeNTest {

        @Nested
        class SimpleArchetypeTest extends AbstractTest {
            @Override
            protected ArchetypeData createArchetype(Class<?>... others) {
                return createArchetypeN(others);
            }
        }

        @Nested
        class WithComponentsTest extends AbstractTest {

            @Override
            protected ArchetypeData createArchetype(Class<?>... others) {
                return createArchetypeN(new Object[] { E1.INSTANCE, E2.INSTANCE }, others);
            }

            protected void verifyHasComposition(int entityId, Composition.Builder builder) {
                var composition = world.createComposition(builder.all(E1.class, E2.class));

                ArchetypeManagerTest.this.verifyHasComposition(entityId, composition);
            }

            @Test
            void testPooledComponents_Throws() {
                var archetype = createArchetype(D1.class).archetype();

                assertThatThrownBy(() -> archetype.with(new D2()))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContainingAll("D2", "cannot implement Pooled");
            }

            @Test
            void testDuplicateComponents_Throws() {
                var archetype = createArchetype(D1.class).archetype();

                assertThatThrownBy(() -> archetype.with(new C1()))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContainingAll("C1", "already defined");

                assertThatThrownBy(() -> archetype.with(E1.INSTANCE, E2.INSTANCE, E1.INSTANCE))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContainingAll("E1", "already defined");
            }

        }

        abstract class AbstractTest {

            protected abstract ArchetypeData createArchetype(Class<?>... others);

            protected void verifyHasComposition(int entityId, Composition.Builder builder) {
                var composition = world.createComposition(builder);

                ArchetypeManagerTest.this.verifyHasComposition(entityId, composition);
            }

            @Test
            void testArchetype() {
                var archetype = createArchetype(D1.class, D2.class);
                var entityId = createEntity(archetype, new D1(), new D2());

                // Verify
                for (var component : archetype.components()) {
                    verifyHasComponent(entityId, component);
                }
                verifyHasComponent(entityId, D1.class);
                verifyHasComponent(entityId, D2.class);
                verifyHasComposition(entityId, Composition.all(archetype.components()));
                verifyHasComposition(entityId, Composition.all(D1.class, D2.class));
            }

            @Test
            void testOtherComponentsNotValidated() {
                var archetype = createArchetype(D1.class, D2.class);
                var entityId = createEntity(archetype);

                for (var component : archetype.components()) {
                    if (D1.class.equals(component) || D2.class.equals(component)) {
                        continue;
                    }

                    verifyHasComponent(entityId, component);
                }
                verifyDoesNotHaveComponent(entityId, D1.class);
                verifyDoesNotHaveComponent(entityId, D2.class);
                verifyHasComposition(entityId, Composition.all(archetype.components()));
                verifyHasComposition(entityId, Composition.all(D1.class, D2.class));
            }

            @Test
            void testNullInstance() {
                var archetype = createArchetype(D1.class, D2.class);

                for (var i = 0; i < archetype.components().length - 2; i++) {
                    var idx = i;
                    assertThatThrownBy(() -> createEntityNull(archetype, idx, new D1(), new D2())).isNotNull();
                }
                assertThatThrownBy(() -> createEntity(archetype, null, new D2())).isNotNull();
                assertThatThrownBy(() -> createEntity(archetype, new D1(), null)).isNotNull();
            }

            @Test
            void testDuplicateInstances() {
                assertThatThrownBy(() -> createArchetype(C1.class))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContainingAll("C1", "already defined");

                assertThatThrownBy(() -> createArchetype(D1.class, D1.class))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContainingAll("D1", "already defined");

                assertThatThrownBy(() -> world.createArchetype(C1.class, C2.class, C3.class, C4.class, C5.class, C6.class, C7.class, C8.class, C1.class))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContainingAll("C1", "already defined");

                assertThatThrownBy(() -> world.createArchetype(C1.class, C2.class, C3.class, C4.class, C5.class, C6.class, C7.class, C8.class, D1.class, D1.class))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContainingAll("D1", "already defined");
            }

            @Test
            void testBatch() {
                var archetype = createArchetype(D1.class, D2.class);

                var entityIds = createBatch(archetype, 10, i -> new Object[] { new D1(), new D2() });
                assertThat(entityIds).hasSize(10);

                for (int i = 0; i < 10; i++) {
                    var entityId = entityIds[i];

                    for (var component : archetype.components()) {
                        verifyHasComponent(entityId, component);
                    }
                    verifyHasComponent(entityId, D1.class);
                    verifyHasComponent(entityId, D2.class);
                    verifyHasComposition(entityId, Composition.all(archetype.components()));
                    verifyHasComposition(entityId, Composition.all(D1.class, D2.class));
                }
            }

            @Test
            void testBatchComposition() {
                var archetype = createArchetype(D1.class, D2.class);

                var inserted = new ArrayList<Integer>();
                var composition = world.createComposition(Composition.all(C1.class, C2.class, C3.class, C4.class, C5.class, C6.class, C7.class, C8.class, D1.class, D2.class));
                composition.inserted(inserted::add);

                var entityIds = createBatch(archetype, 10, i -> new Object[] { new D1(), new D2() });

                assertThat(inserted).hasSize(10);
                for (var entityId : entityIds) {
                    assertThat(inserted).contains(entityId);
                }
            }

            @Test
            void testBatch_NullInstances() {
                var archetype = createArchetype(D1.class, D2.class);

                for (var i = 0; i < archetype.components().length - 2; i++) {
                    var idx = i;
                    assertThatThrownBy(() -> createBatchNull(archetype, idx, 10, n -> new Object[] { new D1(), new D2() })).isInstanceOf(NullPointerException.class);
                }
                assertThatThrownBy(() -> createBatch(archetype, 10, n -> new Object[] { null, new D2() })).isInstanceOf(NullPointerException.class);
                assertThatThrownBy(() -> createBatch(archetype, 10, n -> new Object[] { new D1(), null })).isInstanceOf(NullPointerException.class);
            }

            @Test
            void testBatch_WhenLessAdditionalComponents_ThrowsBecauseOfNull() {
                var archetype = createArchetype(D1.class, D2.class);

                assertThatThrownBy(() -> createBatchNull(archetype, 0, 10, i -> new Object[] { new D1() })).isInstanceOf(NullPointerException.class);
                assertThatThrownBy(() -> createBatchNull(archetype, 0, 10, i -> new Object[0])).isInstanceOf(NullPointerException.class);
            }

            @Test
            void testBatchMoreAdditionalComponents_LargestFirst() {
                var archetype = createArchetype(D1.class);

                assertThatThrownBy(() -> createBatchNull(archetype, 0, 10, i -> i == 0 ? new Object[] { new D1(), new D2() } : new Object[] { new D1() }))
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessageStartingWith("Expected %d added components, but got %d".formatted(archetype.components().length, archetype.components().length + 1));
            }

            @Test
            void testBatchMoreAdditionalComponents_LargestLast() {
                var archetype = createArchetype(D1.class);

                assertThatThrownBy(() -> createBatchNull(archetype, 0, 10, i -> i == 0 ? new Object[] { new D1() } : new Object[] { new D1(), new D2() }))
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessageStartingWith("Expected %d added components, but got %d".formatted(archetype.components().length, archetype.components().length + 1));
            }

            @Test
            @SuppressWarnings({ "unchecked", "rawtypes" })
            void testBatchInitializeNotCalled() {
                var archetype = createArchetype(D1.class, D2.class);

                assertThatThrownBy(() -> ((Archetype.OfN) archetype.archetype()).createBatch(10, NO_OP))
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessageContaining("callback not called for entity");
            }

            @Test
            void testGetInstance() {
                var archetype = createArchetype(D1.class, D2.class);
                assertThat(archetype.archetype().getInstance(D1.class)).isNotNull();
                assertThat(archetype.archetype().getInstance(D2.class)).isNotNull();
            }

        }

    }

    public record D1() implements Pooled {
    }

    public record D2() implements Pooled {
    }

    public enum E1 {
        INSTANCE
    }

    public enum E2 {
        INSTANCE
    }

}
