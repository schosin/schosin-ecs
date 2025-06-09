package de.schosin.ecs.storage.testsuite.entities;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import de.schosin.ecs.api.Pooled;
import de.schosin.ecs.api.components.Relation;
import de.schosin.ecs.api.components.Relation.Exclusive;
import de.schosin.ecs.api.components.types.ComponentType.RegularComponentType;
import de.schosin.ecs.storage.api.StorageEngineException;
import de.schosin.ecs.storage.api.entities.ComponentMask;
import de.schosin.ecs.storage.testsuite.AbstractStorageEngineTest;
import de.schosin.ecs.utils.collections.ImmutableBag;

public class CreateEntityTest extends AbstractStorageEngineTest {

    @Test
    void testNoComponents() {
        var componentMask = storageEngine.create(1, new Object[0]);

        // Verify
        assertThat(componentMask).as("must return empty component mask").isNotNull();
        assertThat(componentMask.getComponentTypes()).as("must return empty component mask").isEmpty();
        assertThat(componentMask.getComponents()).as("must return empty component mask").isEmpty();
    }

    @Test
    void testNullComponents() {
        assertThatThrownBy(() -> storageEngine.create(1, new Object[] { null })).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> storageEngine.create(1, new Object[] { new C1(), null })).isInstanceOf(IllegalArgumentException.class);

        // Verify
        assertThat(storageEngine.getComponentMaskById(1)).as("errors should not store a component mask").isNull();
        assertThat(world.getComponents(C1.class).get(1)).as("errors should not store a components").isNull();
    }

    @Test
    void testEntityAlreadyPresentInStore() {
        storageEngine.create(42, new Object[0]);

        assertThatThrownBy(() -> storageEngine.create(42, new Object[0]))
                .isInstanceOf(StorageEngineException.class)
                .hasMessageContaining("already present in storage", "42");
    }

    @Test
    void testEntityAlreadyPresentInStore_Predefined() {
        var componentMask = storageEngine.create(42, new Object[0]);

        assertThatThrownBy(() -> storageEngine.create(42, componentMask, componentMask.getComponentTypes(), new Object[0]))
                .isInstanceOf(StorageEngineException.class)
                .hasMessageContaining("already present in storage", "42");
    }

    @Test
    void testDuplicateClassComponent() {
        assertThatThrownBy(() -> storageEngine.create(1, new Object[] { new C1(), new C1() }))
                .isInstanceOf(StorageEngineException.class)
                .hasMessageContainingAll("duplicate component types", C1.class.getSimpleName());
    }

    /*
     * Affected archetype storage: Implementation failed to track entityId lookup based on index in certain scenarios
     */
    @Test
    void testReuseDataStructuresFromAlteredEntity() {
        // Create entities
        for (int i = 1; i <= 11; i++) {
            storageEngine.create(i, new Object[] { new C1() });
        }

        // Alter their composition
        for (int i = 1; i <= 11; i++) {
            storageEngine.add(i, new Object[] { new C2() });
        }

        var mapper1 = storageEngine.getComponent(component(C1.class));

        // Reuse entity ids, creating a new with the component mask before alteration
        for (int i = 1; i <= 11; i++) {
            storageEngine.delete(i);

            var component1 = new C1();
            storageEngine.create(i, new Object[] { component1 });

            // Verify
            assertThat(mapper1.getComponent(i)).as("returns added components").isSameAs(component1);
        }

        for (int i = 1; i <= 11; i++) {
            var id = i;

            assertThatCode(() -> {
                storageEngine.delete(id);

                var component1 = new C1();
                storageEngine.create(id, new Object[] { component1 });

                // Verify
                assertThat(mapper1.getComponent(id)).as("returns added components").isSameAs(component1);
            }).doesNotThrowAnyException();
        }
    }

    @Test
    void testReuseEntityIdForSamePurpose() {
        // Create entity
        storageEngine.create(1, new Object[] { new C1() });

        verifyHasComponents(1, C1.class);
        verifyDoesNotHaveComponents(1, C2.class);

        // Add C2
        storageEngine.add(1, new Object[] { new C2() });
        storageEngine.flushChanges(1);

        verifyHasComponents(1, C1.class, C2.class);

        // Delete entity
        storageEngine.delete(1);

        verifyDoesNotHaveComponents(1, C1.class, C2.class);

        // Create entity with reused id
        storageEngine.create(1, new Object[] { new C1() });

        verifyHasComponents(1, C1.class);
        verifyDoesNotHaveComponents(1, C2.class);

        // Add C2
        storageEngine.add(1, new Object[] { new C2() });
        storageEngine.flushChanges(1);

        verifyHasComponents(1, C1.class, C2.class);

        // Delete entity
        storageEngine.delete(1);

        verifyDoesNotHaveComponents(1, C1.class, C2.class);
    }

    @Nested
    class ComponentMaskTest {

        @Test
        void testClassTypes() {
            var componentMask = storageEngine.create(1, new Object[] { new C1(), new C2() });

            // Verify
            assertThat(componentMask).as("must return component mask").isNotNull();
            assertThat(componentMask.getComponentTypes()).as("must contain matching component types").containsExactlyInAnyOrder(component(C1.class), component(C2.class));
            assertThat(componentMask.getComponents()).extracting("type").as("must contain matching component types").containsExactlyInAnyOrder(component(C1.class), component(C2.class));

            var differentOrder = storageEngine.create(2, new Object[] { new C2(), new C1() });
            assertThat(differentOrder).as("order of components does not matter").isSameAs(componentMask);

            assertThat(storageEngine.getComponentMaskForEntity(1)).as("getComponentMaskForEntity returns same instance").isSameAs(componentMask);
        }

        @Test
        void testPooledClassTypes() {
            var componentMask = storageEngine.create(1, new Object[] { new P1(), new P2() });

            // Verify
            assertThat(componentMask).as("must return component mask").isNotNull();
            assertThat(componentMask.getComponentTypes()).as("must contain matching component types").containsExactlyInAnyOrder(component(P1.class), component(P2.class));
            assertThat(componentMask.getComponents()).extracting("type").as("must contain matching component types").containsExactlyInAnyOrder(component(P1.class), component(P2.class));

            var differentOrder = storageEngine.create(2, new Object[] { new P2(), new P1() });
            assertThat(differentOrder).as("order of components does not matter").isSameAs(componentMask);

            assertThat(storageEngine.getComponentMaskForEntity(1)).as("getComponentMaskForEntity returns same instance").isSameAs(componentMask);
        }

        @Test
        void testComponentRelations() {
            var componentMask = storageEngine.create(1, new Object[] { Relation.create(new C1(), new C2()), Relation.create(new C2(), new C1()) });

            // Verify
            assertThat(componentMask).as("must return component mask").isNotNull();
            assertThat(componentMask.getComponentTypes()).as("must contain matching component types").containsExactlyInAnyOrder(relation(C1.class, C2.class), relation(C2.class, C1.class));
            assertThat(componentMask.getComponents()).extracting("type").as("must contain matching component types").containsExactlyInAnyOrder(relation(C1.class, C2.class),
                    relation(C2.class, C1.class));

            var differentOrder = storageEngine.create(2, new Object[] { Relation.create(new C2(), new C1()), Relation.create(new C1(), new C2()) });
            assertThat(differentOrder).as("order of components does not matter").isSameAs(componentMask);

            assertThat(storageEngine.getComponentMaskForEntity(1)).as("getComponentMaskForEntity returns same instance").isSameAs(componentMask);
        }

        @Test
        void testExclusiveComponentRelations() {
            var componentMask = storageEngine.create(1, new Object[] { Relation.create(E1.INSTANCE, new C2()), Relation.create(E2.INSTANCE, new C2()) });

            // Verify
            assertThat(componentMask).as("must return component mask").isNotNull();
            assertThat(componentMask.getComponentTypes())
                    .as("must contain matching component types").containsExactlyInAnyOrder(exclusiveRelation(E1.class, C2.class), exclusiveRelation(E2.class, C2.class));
            assertThat(componentMask.getComponents()).extracting("type")
                    .as("must contain matching component types").containsExactlyInAnyOrder(exclusiveRelation(E1.class, C2.class), exclusiveRelation(E2.class, C2.class));

            var differentOrder = storageEngine.create(2, new Object[] { Relation.create(E2.INSTANCE, new C2()), Relation.create(E1.INSTANCE, new C2()) });
            assertThat(differentOrder).as("order of components does not matter").isSameAs(componentMask);

            assertThat(storageEngine.getComponentMaskForEntity(1)).as("getComponentMaskForEntity returns same instance").isSameAs(componentMask);
        }

        @Test
        void testEntityRelations() {
            var componentMask = storageEngine.create(1, new Object[] { Relation.create(new C1(), 2), Relation.create(new C2(), 2) });

            // Verify
            assertThat(componentMask).as("must return component mask").isNotNull();
            assertThat(componentMask.getComponentTypes()).as("must contain matching component types").containsExactlyInAnyOrder(relation(C1.class), relation(C2.class));
            assertThat(componentMask.getComponents()).extracting("type").as("must contain matching component types").containsExactlyInAnyOrder(relation(C1.class), relation(C2.class));

            var differentOrder = storageEngine.create(2, new Object[] { Relation.create(new C2(), 2), Relation.create(new C1(), 2) });
            assertThat(differentOrder).as("order of components does not matter").isSameAs(componentMask);

            assertThat(storageEngine.getComponentMaskForEntity(1)).as("getComponentMaskForEntity returns same instance").isSameAs(componentMask);
        }

        @Test
        void testExclusiveEntityRelations() {
            var componentMask = storageEngine.create(1, new Object[] { Relation.create(E1.INSTANCE, 2), Relation.create(E2.INSTANCE, 2) });

            // Verify
            assertThat(componentMask).as("must return component mask").isNotNull();
            assertThat(componentMask.getComponentTypes()).as("must contain matching component types").containsExactlyInAnyOrder(exclusiveRelation(E1.class), exclusiveRelation(E2.class));
            assertThat(componentMask.getComponents()).extracting("type").as("must contain matching component types").containsExactlyInAnyOrder(exclusiveRelation(E1.class),
                    exclusiveRelation(E2.class));

            var differentOrder = storageEngine.create(2, new Object[] { Relation.create(E2.INSTANCE, 2), Relation.create(E1.INSTANCE, 2) });
            assertThat(differentOrder).as("order of components does not matter").isSameAs(componentMask);

            assertThat(storageEngine.getComponentMaskForEntity(1)).as("getComponentMaskForEntity returns same instance").isSameAs(componentMask);
        }

    }

    @Nested
    class ComponentDataTest {

        @Test
        void testClassTypes() {
            var component1 = new C1();
            var component2 = new C2();

            storageEngine.create(1, new Object[] { component1, component2 });

            // Verify
            assertThat(storageEngine.getComponent(component(C1.class)).getComponent(1)).as("Must store component instance").isSameAs(component1);
            assertThat(storageEngine.getComponent(component(C2.class)).getComponent(1)).as("Must store component instance").isSameAs(component2);
        }

        @Test
        void testPooledClassTypes() {
            var component1 = new P1();
            var component2 = new P2();

            storageEngine.create(1, new Object[] { component1, component2 });

            // Verify
            assertThat(storageEngine.getComponent(component(P1.class)).getComponent(1)).as("Must store component instance").isSameAs(component1);
            assertThat(storageEngine.getComponent(component(P2.class)).getComponent(1)).as("Must store component instance").isSameAs(component2);
        }

        @Test
        void testComponentRelations() {
            var component1 = Relation.create(new C1(), new C2());
            var component2 = Relation.create(new C2(), new C1());

            storageEngine.create(1, new Object[] { component1, component2 });

            // Verify
            assertThat(storageEngine.getComponent(relation(C1.class, C2.class)).getComponent(1)).as("Must store component instance").containsExactly(component1);
            assertThat(storageEngine.getComponent(relation(C2.class, C1.class)).getComponent(1)).as("Must store component instance").containsExactly(component2);
        }

        @Test
        void testExclusiveComponentRelations() {
            var component1 = Relation.create(E1.INSTANCE, new C2());
            var component2 = Relation.create(E2.INSTANCE, new C2());

            storageEngine.create(1, new Object[] { component1, component2 });

            // Verify
            assertThat(storageEngine.getComponent(exclusiveRelation(E1.class, C2.class)).getComponent(1)).as("Must store component instance").isSameAs(component1);
            assertThat(storageEngine.getComponent(exclusiveRelation(E2.class, C2.class)).getComponent(1)).as("Must store component instance").isSameAs(component2);
        }

        @Test
        void testEntityRelations() {
            var component1 = Relation.create(new C1(), 2);
            var component2 = Relation.create(new C2(), 2);

            storageEngine.create(1, new Object[] { component1, component2 });

            // Verify
            assertThat(storageEngine.getComponent(relation(C1.class)).getComponent(1)).as("Must store component instance").containsExactly(component1);
            assertThat(storageEngine.getComponent(relation(C2.class)).getComponent(1)).as("Must store component instance").containsExactly(component2);
        }

        @Test
        void testExclusiveEntityRelations() {
            var component1 = Relation.create(E1.INSTANCE, 2);
            var component2 = Relation.create(E2.INSTANCE, 2);

            storageEngine.create(1, new Object[] { component1, component2 });

            // Verify
            assertThat(storageEngine.getComponent(exclusiveRelation(E1.class)).getComponent(1)).as("Must store component instance").isSameAs(component1);
            assertThat(storageEngine.getComponent(exclusiveRelation(E2.class)).getComponent(1)).as("Must store component instance").isSameAs(component2);
        }

        @Nested
        class PredefinedComponentMaskTest {

            @Test
            void testClassTypes() {
                var component1 = new C1();
                var component2 = new C2();
                var componentMask = storageEngine.getComponentMask(component(C1.class), component(C2.class));

                assertThat(storageEngine.create(1, componentMask, new Object[] { component1, component2 })).as("same component mask returned").isSameAs(componentMask);
                assertThat(storageEngine.create(2, componentMask, new Object[] { new C2(), new C1() })).as("same component mask returned").isSameAs(componentMask);

                // Verify
                assertThat(storageEngine.getComponent(component(C1.class)).getComponent(1)).as("Must store component instance").isSameAs(component1);
                assertThat(storageEngine.getComponent(component(C2.class)).getComponent(1)).as("Must store component instance").isSameAs(component2);

                assertThat(storageEngine.getComponent(component(C1.class)).getComponent(2)).as("Must store component instance").isNotNull().isNotSameAs(component1);
                assertThat(storageEngine.getComponent(component(C2.class)).getComponent(2)).as("Must store component instance").isNotNull().isNotSameAs(component2);
            }

            @Test
            void testPooledClassTypes() {
                var component1 = new P1();
                var component2 = new P2();
                var componentMask = storageEngine.getComponentMask(component(P1.class), component(P2.class));

                assertThat(storageEngine.create(1, componentMask, new Object[] { component1, component2 })).as("same component mask returned").isSameAs(componentMask);
                assertThat(storageEngine.create(2, componentMask, new Object[] { new P2(), new P1() })).as("same component mask returned").isSameAs(componentMask);

                // Verify
                assertThat(storageEngine.getComponent(component(P1.class)).getComponent(1)).as("Must store component instance").isSameAs(component1);
                assertThat(storageEngine.getComponent(component(P2.class)).getComponent(1)).as("Must store component instance").isSameAs(component2);

                assertThat(storageEngine.getComponent(component(P1.class)).getComponent(2)).as("Must store component instance").isNotNull().isNotSameAs(component1);
                assertThat(storageEngine.getComponent(component(P2.class)).getComponent(2)).as("Must store component instance").isNotNull().isNotSameAs(component2);
            }

            @Test
            void testComponentRelations() {
                var component1 = Relation.create(new C1(), new C2());
                var component2 = Relation.create(new C2(), new C1());
                var componentMask = storageEngine.getComponentMask(relation(C1.class, C2.class), relation(C2.class, C1.class));

                assertThat(storageEngine.create(1, componentMask, new Object[] { component1, component2 })).as("same component mask returned").isSameAs(componentMask);
                assertThat(storageEngine.create(2, componentMask, new Object[] { Relation.create(new C2(), new C1()), Relation.create(new C1(), new C2()) }))
                        .as("same component mask returned").isSameAs(componentMask);

                // Verify
                assertThat(storageEngine.getComponent(relation(C1.class, C2.class)).getComponent(1)).as("Must store component instance").containsExactly(component1);
                assertThat(storageEngine.getComponent(relation(C2.class, C1.class)).getComponent(1)).as("Must store component instance").containsExactly(component2);

                assertThat(storageEngine.getComponent(relation(C1.class, C2.class)).getComponent(2)).as("Must store component instance").isNotNull().isNotSameAs(component1);
                assertThat(storageEngine.getComponent(relation(C2.class, C1.class)).getComponent(2)).as("Must store component instance").isNotNull().isNotSameAs(component2);
            }

            @Test
            void testExclusiveComponentRelations() {
                var component1 = Relation.create(E1.INSTANCE, new C2());
                var component2 = Relation.create(E2.INSTANCE, new C2());
                var componentMask = storageEngine.getComponentMask(exclusiveRelation(E1.class, C2.class), exclusiveRelation(E2.class, C2.class));

                assertThat(storageEngine.create(1, componentMask, new Object[] { component1, component2 })).as("same component mask returned").isSameAs(componentMask);
                assertThat(storageEngine.create(2, componentMask, new Object[] { Relation.create(E2.INSTANCE, new C2()), Relation.create(E1.INSTANCE, new C2()) })).as("same component mask returned")
                        .isSameAs(componentMask);

                // Verify
                assertThat(storageEngine.getComponent(exclusiveRelation(E1.class, C2.class)).getComponent(1)).as("Must store component instance").isSameAs(component1);
                assertThat(storageEngine.getComponent(exclusiveRelation(E2.class, C2.class)).getComponent(1)).as("Must store component instance").isSameAs(component2);

                assertThat(storageEngine.getComponent(exclusiveRelation(E1.class, C2.class)).getComponent(2)).as("Must store component instance").isNotNull().isNotSameAs(component1);
                assertThat(storageEngine.getComponent(exclusiveRelation(E2.class, C2.class)).getComponent(2)).as("Must store component instance").isNotNull().isNotSameAs(component2);
            }

            @Test
            void testEntityRelations() {
                var component1 = Relation.create(new C1(), 2);
                var component2 = Relation.create(new C2(), 2);
                var componentMask = storageEngine.getComponentMask(relation(C1.class), relation(C2.class));

                assertThat(storageEngine.create(1, componentMask, new Object[] { component1, component2 })).as("same component mask returned").isSameAs(componentMask);
                assertThat(storageEngine.create(2, componentMask, new Object[] { Relation.create(new C2(), 2), Relation.create(new C1(), 2) })).as("same component mask returned")
                        .isSameAs(componentMask);

                // Verify
                assertThat(storageEngine.getComponent(relation(C1.class)).getComponent(1)).as("Must store component instance").containsExactly(component1);
                assertThat(storageEngine.getComponent(relation(C2.class)).getComponent(1)).as("Must store component instance").containsExactly(component2);

                assertThat(storageEngine.getComponent(relation(C1.class)).getComponent(2)).as("Must store component instance").isNotNull().isNotSameAs(component1);
                assertThat(storageEngine.getComponent(relation(C2.class)).getComponent(2)).as("Must store component instance").isNotNull().isNotSameAs(component2);
            }

            @Test
            void testExclusiveEntityRelations() {
                var component1 = Relation.create(E1.INSTANCE, 2);
                var component2 = Relation.create(E2.INSTANCE, 2);
                var componentMask = storageEngine.getComponentMask(exclusiveRelation(E1.class), exclusiveRelation(E2.class));

                assertThat(storageEngine.create(1, componentMask, new Object[] { component1, component2 })).as("same component mask returned").isSameAs(componentMask);
                assertThat(storageEngine.create(2, componentMask, new Object[] { Relation.create(E2.INSTANCE, 2), Relation.create(E1.INSTANCE, 2) }))
                        .as("same component mask returned").isSameAs(componentMask);

                // Verify
                assertThat(storageEngine.getComponent(exclusiveRelation(E1.class)).getComponent(1)).as("Must store component instance").isSameAs(component1);
                assertThat(storageEngine.getComponent(exclusiveRelation(E2.class)).getComponent(1)).as("Must store component instance").isSameAs(component2);

                assertThat(storageEngine.getComponent(exclusiveRelation(E1.class)).getComponent(2)).as("Must store component instance").isNotNull().isNotSameAs(component1);
                assertThat(storageEngine.getComponent(exclusiveRelation(E2.class)).getComponent(2)).as("Must store component instance").isNotNull().isNotSameAs(component2);
            }

            @Test
            void testMismatchingTypes() {
                var component1 = new C1();
                var component2 = new C2();
                var componentMask = storageEngine.getComponentMask(component(C1.class), component(P2.class));

                assertThatThrownBy(() -> storageEngine.create(1, componentMask, new Object[] { component1, component2 }))
                        .isInstanceOf(StorageEngineException.class)
                        .hasMessageContainingAll("component mask %d".formatted(componentMask.getId()), "The following component types are missing", component(P2.class).toString());

                // Verify (may store previous, valid components)
                assertThat(storageEngine.getComponent(component(C1.class)).getComponent(1)).as("Must not store component instance on error").isNull();
                assertThat(storageEngine.getComponent(component(C2.class)).getComponent(1)).as("Must not store component instance on error").isNull();
                assertThat(storageEngine.getComponent(component(P2.class)).getComponent(1)).as("Must not store component instance on error").isNull();
            }

            @Test
            void testMissingTypes() {
                var component1 = new C1();
                var componentMask = storageEngine.getComponentMask(component(C1.class), component(C2.class));

                assertThatThrownBy(() -> storageEngine.create(1, componentMask, new Object[] { component1 }))
                        .isInstanceOf(StorageEngineException.class)
                        .hasMessageContainingAll("component mask %d".formatted(componentMask.getId()), "The following component types are missing", component(C2.class).toString());

                // Verify (may store previous, valid components)
                assertThat(storageEngine.getComponent(component(C1.class)).getComponent(1)).as("Must not store component instance on error").isNull();
                assertThat(storageEngine.getComponent(component(C2.class)).getComponent(1)).as("Must not store component instance on error").isNull();
            }

            @Test
            void testUnexpectedTypes() {
                var component1 = new C1();
                var component2 = new C2();
                var componentMask = storageEngine.getComponentMask(component(C1.class), component(C2.class));

                assertThatThrownBy(() -> storageEngine.create(1, componentMask, new Object[] { component1, component2, new P1() }))
                        .isInstanceOf(StorageEngineException.class)
                        .hasMessageContainingAll("component mask %d".formatted(componentMask.getId()), "The following component types were unexpected", component(P1.class).toString());

                // Verify (may store previous, valid components)
                assertThat(storageEngine.getComponent(component(C1.class)).getComponent(1)).as("Must not store component instance on error").isNull();
                assertThat(storageEngine.getComponent(component(C2.class)).getComponent(1)).as("Must not store component instance on error").isNull();
                assertThat(storageEngine.getComponent(component(P1.class)).getComponent(1)).as("Must not store component instance on error").isNull();
            }

        }

        @Nested
        class PredefinedComponentMaskAndComponentTypesTest {

            @Nested
            class ObjectArrayTest extends AbstractPredefinedComponentMaskAndComponentTypesTest {

                @Override
                protected ComponentMask createEntity(int entityId, ComponentMask componentMask, ImmutableBag<? extends RegularComponentType<?, ?>> componentTypes, Object[] components) {
                    return storageEngine.create(1, componentMask, componentTypes, components);
                }

            }

            @Nested
            class ImmutableBagTest extends AbstractPredefinedComponentMaskAndComponentTypesTest {

                @Override
                protected ComponentMask createEntity(int entityId, ComponentMask componentMask, ImmutableBag<? extends RegularComponentType<?, ?>> componentTypes, Object[] components) {
                    return storageEngine.create(1, componentMask, componentTypes, ImmutableBag.of(components));
                }

            }

            abstract class AbstractPredefinedComponentMaskAndComponentTypesTest {

                protected abstract ComponentMask createEntity(int entityId, ComponentMask componentMask, ImmutableBag<? extends RegularComponentType<?, ?>> componentTypes, Object[] components);

                @Test
                void testEntityAlreadyPresentInStore() {
                    var component1 = new C1();
                    var componentMask = storageEngine.getComponentMask(component(C1.class));
                    var componentTypes = ImmutableBag.of(component(C1.class));

                    assertThat(createEntity(1, componentMask, componentTypes, new Object[] { component1 })).as("same component mask returned").isSameAs(componentMask);

                    assertThatThrownBy(() -> createEntity(1, componentMask, componentTypes, new Object[] { component1 }))
                            .isInstanceOf(StorageEngineException.class)
                            .hasMessageContaining("already present in storage", "42");
                }

                @Test
                void testClassTypes() {
                    var component1 = new C1();
                    var component2 = new C2();
                    var componentMask = storageEngine.getComponentMask(component(C1.class), component(C2.class));
                    var componentTypes = ImmutableBag.of(component(C1.class), component(C2.class));

                    assertThat(createEntity(1, componentMask, componentTypes, new Object[] { component1, component2 })).as("same component mask returned").isSameAs(componentMask);

                    // Verify
                    assertThat(storageEngine.getComponent(component(C1.class)).getComponent(1)).as("Must store component instance").isSameAs(component1);
                    assertThat(storageEngine.getComponent(component(C2.class)).getComponent(1)).as("Must store component instance").isSameAs(component2);
                }

                @Test
                void testPooledClassTypes() {
                    var component1 = new P1();
                    var component2 = new P2();
                    var componentMask = storageEngine.getComponentMask(component(P1.class), component(P2.class));
                    var componentTypes = ImmutableBag.of(component(P1.class), component(P2.class));

                    assertThat(createEntity(1, componentMask, componentTypes, new Object[] { component1, component2 })).as("same component mask returned").isSameAs(componentMask);

                    // Verify
                    assertThat(storageEngine.getComponent(component(P1.class)).getComponent(1)).as("Must store component instance").isSameAs(component1);
                    assertThat(storageEngine.getComponent(component(P2.class)).getComponent(1)).as("Must store component instance").isSameAs(component2);
                }

                @Test
                void testComponentRelations() {
                    var component1 = Relation.create(new C1(), new C2());
                    var component2 = Relation.create(new C2(), new C1());
                    var componentMask = storageEngine.getComponentMask(relation(C1.class, C2.class), relation(C2.class, C1.class));
                    var componentTypes = ImmutableBag.of(relation(C1.class, C2.class), relation(C2.class, C1.class));

                    assertThat(createEntity(1, componentMask, componentTypes, new Object[] { component1, component2 })).as("same component mask returned").isSameAs(componentMask);

                    // Verify
                    assertThat(storageEngine.getComponent(relation(C1.class, C2.class)).getComponent(1)).as("Must store component instance").containsExactly(component1);
                    assertThat(storageEngine.getComponent(relation(C2.class, C1.class)).getComponent(1)).as("Must store component instance").containsExactly(component2);
                }

                @Test
                void testExclusiveComponentRelations() {
                    var component1 = Relation.create(E1.INSTANCE, new C2());
                    var component2 = Relation.create(E2.INSTANCE, new C2());
                    var componentMask = storageEngine.getComponentMask(exclusiveRelation(E1.class, C2.class), exclusiveRelation(E2.class, C2.class));
                    var componentTypes = ImmutableBag.of(exclusiveRelation(E1.class, C2.class), exclusiveRelation(E2.class, C2.class));

                    assertThat(createEntity(1, componentMask, componentTypes, new Object[] { component1, component2 })).as("same component mask returned").isSameAs(componentMask);

                    // Verify
                    assertThat(storageEngine.getComponent(exclusiveRelation(E1.class, C2.class)).getComponent(1)).as("Must store component instance").isSameAs(component1);
                    assertThat(storageEngine.getComponent(exclusiveRelation(E2.class, C2.class)).getComponent(1)).as("Must store component instance").isSameAs(component2);
                }

                @Test
                void testEntityRelations() {
                    var component1 = Relation.create(new C1(), 2);
                    var component2 = Relation.create(new C2(), 2);
                    var componentMask = storageEngine.getComponentMask(relation(C1.class), relation(C2.class));
                    var componentTypes = ImmutableBag.of(relation(C1.class), relation(C2.class));

                    assertThat(createEntity(1, componentMask, componentTypes, new Object[] { component1, component2 })).as("same component mask returned").isSameAs(componentMask);

                    // Verify
                    assertThat(storageEngine.getComponent(relation(C1.class)).getComponent(1)).as("Must store component instance").containsExactly(component1);
                    assertThat(storageEngine.getComponent(relation(C2.class)).getComponent(1)).as("Must store component instance").containsExactly(component2);
                }

                @Test
                void testExclusiveEntityRelations() {
                    var component1 = Relation.create(E1.INSTANCE, 2);
                    var component2 = Relation.create(E2.INSTANCE, 2);
                    var componentMask = storageEngine.getComponentMask(exclusiveRelation(E1.class), exclusiveRelation(E2.class));
                    var componentTypes = ImmutableBag.of(exclusiveRelation(E1.class), exclusiveRelation(E2.class));

                    assertThat(createEntity(1, componentMask, componentTypes, new Object[] { component1, component2 })).as("same component mask returned").isSameAs(componentMask);

                    // Verify
                    assertThat(storageEngine.getComponent(exclusiveRelation(E1.class)).getComponent(1)).as("Must store component instance").isSameAs(component1);
                    assertThat(storageEngine.getComponent(exclusiveRelation(E2.class)).getComponent(1)).as("Must store component instance").isSameAs(component2);
                }

                @Test
                void testMismatchingTypes() {
                    var component1 = new C1();
                    var component2 = new C2();
                    var componentMask = storageEngine.getComponentMask(component(C1.class), component(P2.class));
                    var componentTypes = ImmutableBag.of(component(C1.class), component(C2.class));

                    assertThatThrownBy(() -> createEntity(1, componentMask, componentTypes, new Object[] { component1, component2 }))
                            .isInstanceOf(StorageEngineException.class)
                            .hasMessageContainingAll("component mask %d".formatted(componentMask.getId()), "The following component types are missing", component(P2.class).toString());

                    // Verify (may store previous, valid components)
                    assertThat(storageEngine.getComponent(component(C1.class)).getComponent(1)).as("Must not store component instance on error").isNull();
                    assertThat(storageEngine.getComponent(component(C2.class)).getComponent(1)).as("Must not store component instance on error").isNull();
                    assertThat(storageEngine.getComponent(component(P2.class)).getComponent(1)).as("Must not store component instance on error").isNull();
                }

                @Test
                void testMisorderedTypes() {
                    var component1 = new C1();
                    var component2 = new C2();
                    var componentMask = storageEngine.getComponentMask(component(C1.class), component(C2.class));
                    var componentTypes = ImmutableBag.of(component(C1.class), component(C2.class));

                    assertThatThrownBy(() -> storageEngine.create(1, componentMask, componentTypes, new Object[] { component2, component1 }))
                            .isInstanceOf(StorageEngineException.class)
                            .hasMessageContainingAll("component mask %d".formatted(componentMask.getId()),
                                    "Expected component type '%s'".formatted(component(C1.class)),
                                    "index 0",
                                    "but was '%s'".formatted(component2));

                    // Verify (may store previous, valid components)
                    assertThat(storageEngine.getComponent(component(C1.class)).getComponent(1)).as("Must not store component instance on error").isNull();
                    assertThat(storageEngine.getComponent(component(C2.class)).getComponent(1)).as("Must not store component instance on error").isNull();
                    assertThat(storageEngine.getComponent(component(P2.class)).getComponent(1)).as("Must not store component instance on error").isNull();
                }

                @Test
                void testMissingTypes() {
                    var component1 = new C1();
                    var componentMask = storageEngine.getComponentMask(component(C1.class), component(C2.class));
                    var componentTypes = ImmutableBag.of(component(C1.class));

                    assertThatThrownBy(() -> createEntity(1, componentMask, componentTypes, new Object[] { component1 }))
                            .isInstanceOf(StorageEngineException.class)
                            .hasMessageContainingAll("component mask %d".formatted(componentMask.getId()), "The following component types are missing", component(C2.class).toString());

                    // Verify (may store previous, valid components)
                    assertThat(storageEngine.getComponent(component(C1.class)).getComponent(1)).as("Must not store component instance on error").isNull();
                    assertThat(storageEngine.getComponent(component(C2.class)).getComponent(1)).as("Must not store component instance on error").isNull();
                }

                @Test
                void testUnexpectedTypes() {
                    var component1 = new C1();
                    var component2 = new C2();
                    var componentMask = storageEngine.getComponentMask(component(C1.class), component(C2.class));
                    var componentTypes = ImmutableBag.of(component(C1.class), component(C2.class), component(P1.class));

                    assertThatThrownBy(() -> createEntity(1, componentMask, componentTypes, new Object[] { component1, component2, new P1() }))
                            .isInstanceOf(StorageEngineException.class)
                            .hasMessageContainingAll("component mask %d".formatted(componentMask.getId()), "The following component types were unexpected", component(P1.class).toString());

                    // Verify (may store previous, valid components)
                    assertThat(storageEngine.getComponent(component(C1.class)).getComponent(1)).as("Must not store component instance on error").isNull();
                    assertThat(storageEngine.getComponent(component(C2.class)).getComponent(1)).as("Must not store component instance on error").isNull();
                    assertThat(storageEngine.getComponent(component(P1.class)).getComponent(1)).as("Must not store component instance on error").isNull();
                }

            }

        }

    }

    record C1() {
    }

    record C2() {
    }

    record P1() implements Pooled {
    }

    record P2() implements Pooled {
    }

    enum E1 implements Exclusive {
        INSTANCE
    }

    enum E2 implements Exclusive {
        INSTANCE
    }

}
