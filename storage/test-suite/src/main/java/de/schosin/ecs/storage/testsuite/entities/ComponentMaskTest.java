package de.schosin.ecs.storage.testsuite.entities;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import de.schosin.ecs.api.Pooled;
import de.schosin.ecs.api.components.Relation.Exclusive;
import de.schosin.ecs.api.components.types.ComponentType.RegularComponentType;
import de.schosin.ecs.storage.api.StorageEngineException;
import de.schosin.ecs.storage.api.entities.ComponentMask;
import de.schosin.ecs.storage.testsuite.AbstractStorageEngineTest;
import de.schosin.ecs.utils.collections.Bag;
import de.schosin.ecs.utils.collections.ImmutableBag;

public class ComponentMaskTest extends AbstractStorageEngineTest {

    @Nested
    class GetComponentMaskTest {

        @Nested
        class EmptyComponentMask {

            @Test
            void testNoVarargs() {
                var componentMask = engine.getComponentMask();
                assertThat(componentMask).as("empty var args must not return null").isNotNull();

                assertThat(componentMask.getId()).as("id must not be negative").isNotNegative();
                assertThat(componentMask.getComponentTypes()).as("getComponentTypes must return non-null and empty bag").isEmpty();
                assertThat(componentMask.getComponents()).as("getComponents must return non-null and empty bag").isEmpty();
            }

            @Test
            void testEmptyArray() {
                var componentMask = engine.getComponentMask(new RegularComponentType<?, ?>[0]);
                assertThat(componentMask).as("empty array must not return null").isNotNull();

                assertThat(componentMask.getId()).as("id must not be negative").isNotNegative();
                assertThat(componentMask.getComponentTypes()).as("getComponentTypes must return non-null and empty bag").isEmpty();
                assertThat(componentMask.getComponents()).as("getComponents must return non-null and empty bag").isEmpty();
            }

            @Test
            void testSameInstanceReturned() {
                var componentMask = engine.getComponentMask();

                assertThat(engine.getComponentMask()).as("must return same component mask if queried again").isSameAs(componentMask);
                assertThat(engine.getComponentMask(new RegularComponentType<?, ?>[0])).as("must return same component mask if queried again").isSameAs(componentMask);
            }

            @Test
            void testNullArray() {
                assertThatThrownBy(() -> engine.getComponentMask((RegularComponentType<?, ?>[]) null), "null array should throw an exception");
            }

            @Test
            void testNullTypes() {
                assertThatThrownBy(() -> engine.getComponentMask(new RegularComponentType<?, ?>[] { null }), "null types should throw an exception");
                assertThatThrownBy(() -> engine.getComponentMask(new RegularComponentType<?, ?>[] { component(C1.class), null }), "null types should throw an exception");
            }

            @Test
            void testGetByComponentId() {
                var componentMask = engine.getComponentMask();

                assertThat(engine.getComponentMaskById(componentMask.getId())).as("getComponentMaskById must return same instance").isSameAs(componentMask);
            }

        }

        @Nested
        class OneComponentType {

            @Test
            void testClassType() {
                var componentMask = engine.getComponentMask(component(C1.class));

                // Verify
                assertThat(componentMask.getComponentTypes()).as("getComponentTypes must contain only passed type by equality").containsExactly(component(C1.class));
                assertThat(componentMask.getComponents()).extracting("type").as("getComponents must match getComponentTypes").containsExactly(component(C1.class));

                assertThat(engine.getComponentMaskById(componentMask.getId())).as("getComponentMaskById must return same instance").isSameAs(componentMask);
            }

            @Test
            void testPooledClassType() {
                var componentMask = engine.getComponentMask(component(P1.class));

                // Verify
                assertThat(componentMask.getComponentTypes()).as("getComponentTypes must contain only passed component type by equality").containsExactly(component(P1.class));
                assertThat(componentMask.getComponents()).extracting("type").as("getComponents must match getComponentTypes").containsExactly(component(P1.class));

                assertThat(engine.getComponentMaskById(componentMask.getId())).as("getComponentMaskById must return same instance").isSameAs(componentMask);
            }

            @Test
            void testComponentRelation() {
                var componentMask = engine.getComponentMask(relation(C1.class, C2.class));

                // Verify
                assertThat(componentMask.getComponentTypes()).as("getComponentTypes must contain only passed type by equality").containsExactly(relation(C1.class, C2.class));
                assertThat(componentMask.getComponents()).extracting("type").as("getComponents must match getComponentTypes").containsExactly(relation(C1.class, C2.class));

                assertThat(engine.getComponentMaskById(componentMask.getId())).as("getComponentMaskById must return same instance").isSameAs(componentMask);
            }

            @Test
            void testExclusiveComponentRelation() {
                var componentMask = engine.getComponentMask(exclusiveRelation(E1.class, C2.class));

                // Verify
                assertThat(componentMask.getComponentTypes()).as("getComponentTypes must contain only passed type by equality").containsExactly(exclusiveRelation(E1.class, C2.class));
                assertThat(componentMask.getComponents()).extracting("type").as("getComponents must match getComponentTypes").containsExactly(exclusiveRelation(E1.class, C2.class));

                assertThat(engine.getComponentMaskById(componentMask.getId())).as("getComponentMaskById must return same instance").isSameAs(componentMask);
            }

            @Test
            void testEntityRelation() {
                var componentMask = engine.getComponentMask(relation(C1.class));

                // Verify
                assertThat(componentMask.getComponentTypes()).as("getComponentTypes must contain only passed type by equality").containsExactly(relation(C1.class));
                assertThat(componentMask.getComponents()).extracting("type").as("getComponents must match getComponentTypes").containsExactly(relation(C1.class));

                assertThat(engine.getComponentMaskById(componentMask.getId())).as("getComponentMaskById must return same instance").isSameAs(componentMask);
            }

            @Test
            void testExclusiveEntityRelation() {
                var componentMask = engine.getComponentMask(exclusiveRelation(E1.class));

                // Verify
                assertThat(componentMask.getComponentTypes()).as("getComponentTypes must contain only passed type by equality").containsExactly(exclusiveRelation(E1.class));
                assertThat(componentMask.getComponents()).extracting("type").as("getComponents must match getComponentTypes").containsExactly(exclusiveRelation(E1.class));

                assertThat(engine.getComponentMaskById(componentMask.getId())).as("getComponentMaskById must return same instance").isSameAs(componentMask);
            }

        }

        @Nested
        class MultipleComponentType {

            @Test
            void testClassTypes() {
                var componentMask = engine.getComponentMask(component(C1.class), component(C2.class));

                // Verify
                assertThat(componentMask.getComponentTypes())
                        .as("getComponentTypes must contain only passed types by equality").containsExactlyInAnyOrder(component(C1.class), component(C2.class));

                assertThat(componentMask.getComponents()).extracting("type")
                        .as("getComponents must match getComponentTypes").containsExactlyInAnyOrder(component(C1.class), component(C2.class));

                assertThat(engine.getComponentMask(component(C2.class), component(C1.class))).as("order of types does not matter").isSameAs(componentMask);

                assertThat(engine.getComponentMaskById(componentMask.getId())).as("getComponentMaskById must return same instance").isSameAs(componentMask);
            }

            @Test
            void testPooledClassTypes() {
                var componentMask = engine.getComponentMask(component(P1.class), component(P2.class));

                // Verify
                assertThat(componentMask.getComponentTypes())
                        .as("getComponentTypes must contain only passed component types by equality").containsExactlyInAnyOrder(component(P1.class), component(P2.class));

                assertThat(componentMask.getComponents()).extracting("type")
                        .as("getComponents must match getComponentTypes").containsExactlyInAnyOrder(component(P1.class), component(P2.class));

                assertThat(engine.getComponentMask(component(P2.class), component(P1.class))).as("order of types does not matter").isSameAs(componentMask);

                assertThat(engine.getComponentMaskById(componentMask.getId())).as("getComponentMaskById must return same instance").isSameAs(componentMask);
            }

            @Test
            void testComponentRelations() {
                var componentMask = engine.getComponentMask(relation(C1.class, C2.class), relation(C2.class, C1.class));

                // Verify
                assertThat(componentMask.getComponentTypes())
                        .as("getComponentTypes must contain only passed types by equality").containsExactlyInAnyOrder(relation(C1.class, C2.class), relation(C2.class, C1.class));

                assertThat(componentMask.getComponents()).extracting("type")
                        .as("getComponents must match getComponentTypes").containsExactlyInAnyOrder(relation(C1.class, C2.class), relation(C2.class, C1.class));

                assertThat(engine.getComponentMask(relation(C2.class, C1.class), relation(C1.class, C2.class))).as("order of types does not matter").isSameAs(componentMask);

                assertThat(engine.getComponentMaskById(componentMask.getId())).as("getComponentMaskById must return same instance").isSameAs(componentMask);
            }

            @Test
            void testExclusiveComponentRelations() {
                var componentMask = engine.getComponentMask(exclusiveRelation(E1.class, C2.class), exclusiveRelation(E2.class, C2.class));

                // Verify
                assertThat(componentMask.getComponentTypes())
                        .as("getComponentTypes must contain only passed types by equality").containsExactlyInAnyOrder(exclusiveRelation(E1.class, C2.class), exclusiveRelation(E2.class, C2.class));

                assertThat(componentMask.getComponents()).extracting("type")
                        .as("getComponents must match getComponentTypes").containsExactlyInAnyOrder(exclusiveRelation(E1.class, C2.class), exclusiveRelation(E2.class, C2.class));

                assertThat(engine.getComponentMask(exclusiveRelation(E2.class, C2.class), exclusiveRelation(E1.class, C2.class))).as("order of types does not matter").isSameAs(componentMask);

                assertThat(engine.getComponentMaskById(componentMask.getId())).as("getComponentMaskById must return same instance").isSameAs(componentMask);
            }

            @Test
            void testEntityRelations() {
                var componentMask = engine.getComponentMask(relation(C1.class), relation(C2.class));

                // Verify
                assertThat(componentMask.getComponentTypes())
                        .as("getComponentTypes must contain only passed types by equality").containsExactlyInAnyOrder(relation(C1.class), relation(C2.class));

                assertThat(componentMask.getComponents()).extracting("type")
                        .as("getComponents must match getComponentTypes").containsExactlyInAnyOrder(relation(C1.class), relation(C2.class));

                assertThat(engine.getComponentMask(relation(C2.class), relation(C1.class))).as("order of types does not matter").isSameAs(componentMask);

                assertThat(engine.getComponentMaskById(componentMask.getId())).as("getComponentMaskById must return same instance").isSameAs(componentMask);
            }

            @Test
            void testExclusiveEntityRelations() {
                var componentMask = engine.getComponentMask(exclusiveRelation(E1.class), exclusiveRelation(E2.class));

                // Verify
                assertThat(componentMask.getComponentTypes())
                        .as("getComponentTypes must contain only passed types by equality").containsExactlyInAnyOrder(exclusiveRelation(E1.class), exclusiveRelation(E2.class));

                assertThat(componentMask.getComponents()).extracting("type")
                        .as("getComponents must match getComponentTypes").containsExactlyInAnyOrder(exclusiveRelation(E1.class), exclusiveRelation(E2.class));

                assertThat(engine.getComponentMask(exclusiveRelation(E2.class), exclusiveRelation(E1.class))).as("order of types does not matter").isSameAs(componentMask);

                assertThat(engine.getComponentMaskById(componentMask.getId())).as("getComponentMaskById must return same instance").isSameAs(componentMask);
            }

            @Test
            void testMixedTypes() {
                var componentMask = engine.getComponentMask(
                        component(C1.class),
                        component(P1.class),
                        relation(C1.class, C2.class),
                        exclusiveRelation(E1.class, C2.class),
                        relation(C1.class),
                        exclusiveRelation(E1.class));

                assertThat(componentMask.getComponentTypes())
                        .as("getComponentTypes must contain only passed types by equality").containsExactlyInAnyOrder(
                                component(C1.class),
                                component(P1.class),
                                relation(C1.class, C2.class),
                                exclusiveRelation(E1.class, C2.class),
                                relation(C1.class),
                                exclusiveRelation(E1.class));

                assertThat(componentMask.getComponents()).extracting("type")
                        .as("getComponents must match getComponentTypes").containsExactlyInAnyOrder(
                                component(C1.class),
                                component(P1.class),
                                relation(C1.class, C2.class),
                                exclusiveRelation(E1.class, C2.class),
                                relation(C1.class),
                                exclusiveRelation(E1.class));

                var differentOrder = engine.getComponentMask(
                        component(P1.class),
                        component(C1.class),
                        relation(C1.class, C2.class),
                        exclusiveRelation(E1.class, C2.class),
                        relation(C1.class),
                        exclusiveRelation(E1.class));

                assertThat(differentOrder).as("order of types does not matter").isSameAs(componentMask);

                assertThat(engine.getComponentMaskById(componentMask.getId())).as("getComponentMaskById must return same instance").isSameAs(componentMask);
            }

            @Test
            void testDuplicateClassTypes() {
                assertThatThrownBy(() -> engine.getComponentMask(component(C1.class), component(C1.class)))
                        .isInstanceOf(StorageEngineException.class)
                        .hasMessageContainingAll("duplicate component types", C1.class.getSimpleName());
            }

        }

        @Test
        void testGetComponentMasks() {
            var type1 = component(C1.class);
            var type2 = component(P1.class);
            var type3 = relation(C1.class, C2.class);
            var type4 = exclusiveRelation(E1.class, C2.class);
            var type5 = relation(C1.class);
            var type6 = exclusiveRelation(E1.class);

            assertThat(engine.getComponentMasks()).as("is empty if no types known").isEmpty();

            engine.getComponentMask(type1);
            assertThat(engine.getComponentMasks()).as("returns all known types").hasSize(1);

            engine.getComponentMask(type2);
            assertThat(engine.getComponentMasks()).as("returns all known types").hasSize(2);

            engine.getComponentMask(type3);
            assertThat(engine.getComponentMasks()).as("returns all known types").hasSize(3);

            engine.getComponentMask(type4);
            assertThat(engine.getComponentMasks()).as("returns all known types").hasSize(4);

            engine.getComponentMask(type5);
            assertThat(engine.getComponentMasks()).as("returns all known types").hasSize(5);

            engine.getComponentMask(type6);
            assertThat(engine.getComponentMasks()).as("returns all known types").hasSize(6);
        }

        @Test
        void testGetComponentMasksByPredicate() {
            var type1 = component(C1.class);
            var type2 = component(P1.class);
            var type3 = relation(C1.class, C2.class);
            var type4 = exclusiveRelation(E1.class, C2.class);
            var type5 = relation(C1.class);
            var type6 = exclusiveRelation(E1.class);

            engine.getComponentMask(type1);
            engine.getComponentMask(type2);
            var mask3 = engine.getComponentMask(type3);
            engine.getComponentMask(type4);
            engine.getComponentMask(type5);
            engine.getComponentMask(type6);
            var mask135 = engine.getComponentMask(type1, type3, type5);

            var result = new Bag<>(ComponentMask.class);
            engine.getComponentMasks(mask -> mask.getComponentTypes().contains(type3), result);

            assertThat(result).containsExactlyInAnyOrder(mask3, mask135);
        }

        @Test
        void testGetComponentMaskAfterAdd() {
            var emptyComponentMask = storageEngine.getComponentMask();
            var componentMask1 = storageEngine.getComponentMask(component(C1.class));

            var entityId = world.createEntity();
            assertThat(storageEngine.getComponentMaskForEntity(entityId)).as("getComponentMaskForEntity returns empty component mask for empty entity").isSameAs(emptyComponentMask);

            // Call
            var updatedComponentMask = storageEngine.add(entityId, new Object[] { new C1() });
            assertThat(updatedComponentMask).as("add returns updated component mask").isSameAs(componentMask1);

            // Verify
            assertThat(storageEngine.getComponentMaskForEntity(entityId)).as("getComponentMaskForEntity returns old component mask after add").isSameAs(emptyComponentMask);
            assertThat(storageEngine.getPendingComponentMask(entityId)).as("getPendingComponentMask returns updated component mask after add").isSameAs(updatedComponentMask);
        }

        @Test
        void testGetComponentMaskAfterAddFlushed() {
            var emptyComponentMask = storageEngine.getComponentMask();
            var componentMask1 = storageEngine.getComponentMask(component(C1.class));

            var entityId = world.createEntity();
            assertThat(storageEngine.getComponentMaskForEntity(entityId)).as("getComponentMaskForEntity returns empty component mask for empty entity").isSameAs(emptyComponentMask);

            // Call
            var updatedComponentMask = storageEngine.add(entityId, new Object[] { new C1() });
            assertThat(updatedComponentMask).as("add returns updated component mask").isSameAs(componentMask1);

            assertThat(storageEngine.flushChanges(entityId)).as("flushChanges returns updated component mask").isSameAs(componentMask1);

            // Verify
            assertThat(storageEngine.getComponentMaskForEntity(entityId)).as("getComponentMaskForEntity returns old component mask after flushed add").isSameAs(componentMask1);
            assertThat(storageEngine.getPendingComponentMask(entityId)).as("getPendingComponentMask returns null if no changes").isNull();
        }

        @Test
        void testGetComponentMaskAfterRemove() {
            var emptyComponentMask = storageEngine.getComponentMask();
            var componentMask1 = storageEngine.getComponentMask(component(C1.class));

            var entityId = world.createEntity(new C1());
            assertThat(storageEngine.getComponentMaskForEntity(entityId)).as("getComponentMaskForEntity returns correct component mask for entity").isSameAs(componentMask1);

            // Call
            var updatedComponentMask = storageEngine.remove(entityId, ImmutableBag.of(component(C1.class)));
            assertThat(updatedComponentMask).as("add returns updated component mask").isSameAs(emptyComponentMask);

            // Verify
            assertThat(storageEngine.getComponentMaskForEntity(entityId)).as("getComponentMaskForEntity returns old component mask after add").isSameAs(componentMask1);
            assertThat(storageEngine.getPendingComponentMask(entityId)).as("getPendingComponentMask returns updated component mask after add").isSameAs(updatedComponentMask);
        }

        @Test
        void testGetComponentMaskAfterRemoveFlushed() {
            var emptyComponentMask = storageEngine.getComponentMask();
            var componentMask1 = storageEngine.getComponentMask(component(C1.class));

            var entityId = world.createEntity(new C1());
            assertThat(storageEngine.getComponentMaskForEntity(entityId)).as("getComponentMaskForEntity returns correct component mask for entity").isSameAs(componentMask1);

            // Call
            var updatedComponentMask = storageEngine.remove(entityId, ImmutableBag.of(component(C1.class)));
            assertThat(updatedComponentMask).as("add returns updated component mask").isSameAs(emptyComponentMask);

            assertThat(storageEngine.flushChanges(entityId)).as("flushChanges returns updated component mask").isSameAs(emptyComponentMask);

            // Verify
            assertThat(storageEngine.getComponentMaskForEntity(entityId)).as("getComponentMaskForEntity returns old component mask after flushed remove").isSameAs(updatedComponentMask);
            assertThat(storageEngine.getPendingComponentMask(entityId)).as("getPendingComponentMask returns null if no changes").isNull();
        }

        @Test
        void testGetComponentMaskAfterModify() {
            var componentMask1 = storageEngine.getComponentMask(component(C1.class));
            var componentMask2 = storageEngine.getComponentMask(component(C2.class));

            var entityId = world.createEntity(new C1());
            assertThat(storageEngine.getComponentMaskForEntity(entityId)).as("getComponentMaskForEntity returns correct component mask for entity").isSameAs(componentMask1);

            // Call
            var updatedComponentMask = storageEngine.modify(entityId, new Object[] { new C2() }, ImmutableBag.of(component(C1.class)));
            assertThat(updatedComponentMask).as("add returns updated component mask").isSameAs(componentMask2);

            // Verify
            assertThat(storageEngine.getComponentMaskForEntity(entityId)).as("getComponentMaskForEntity returns old component mask after modify").isSameAs(componentMask1);
            assertThat(storageEngine.getPendingComponentMask(entityId)).as("getPendingComponentMask returns updated component mask after modify").isSameAs(componentMask2);
        }

        @Test
        void testGetComponentMaskAfterModifyFlushed() {
            var componentMask1 = storageEngine.getComponentMask(component(C1.class));
            var componentMask2 = storageEngine.getComponentMask(component(C2.class));

            var entityId = world.createEntity(new C1());
            assertThat(storageEngine.getComponentMaskForEntity(entityId)).as("getComponentMaskForEntity returns correct component mask for entity").isSameAs(componentMask1);

            // Call
            var updatedComponentMask = storageEngine.modify(entityId, new Object[] { new C2() }, ImmutableBag.of(component(C1.class)));
            assertThat(updatedComponentMask).as("add returns updated component mask").isSameAs(componentMask2);

            assertThat(storageEngine.flushChanges(entityId)).as("flushChanges returns updated component mask").isSameAs(componentMask2);

            // Verify
            assertThat(storageEngine.getComponentMaskForEntity(entityId)).as("getComponentMaskForEntity returns updated component mask after flushed modify").isSameAs(componentMask2);
            assertThat(storageEngine.getPendingComponentMask(entityId)).as("getPendingComponentMask returns null if no changes").isNull();
        }

        @Test
        void testGetPendingComponentMask_NoChanges() {
            var entityId = world.createEntity();

            assertThat(storageEngine.getPendingComponentMask(entityId)).as("getPendingComponentMask returns null if no changes").isNull();
        }

        @Test
        void testGetPendingComponentMask_ChangesFlushed() {
            var entityId = world.createEntity();

            storageEngine.add(entityId, new Object[] { new C1() });
            storageEngine.flushChanges(entityId);

            assertThat(storageEngine.getPendingComponentMask(entityId)).as("getPendingComponentMask returns null if no changes").isNull();
        }

        @Test
        void testGetPendingComponentMask_DeletedEntity() {
            var entityId = world.createEntity();

            storageEngine.delete(entityId);

            assertThatThrownBy(() -> storageEngine.getPendingComponentMask(entityId), "getPendingComponentMask throws for deleted entities")
                    .as("getPendingComponentMask throws for deleted entities").isInstanceOf(StorageEngineException.class)
                    .as("getPendingComponentMask throws for deleted entities").hasMessageContainingAll("entity %d".formatted(entityId), "not present in storage");
        }

        @Test
        void testGetPendingComponentMask_UnknownEntity() {
            assertThatThrownBy(() -> storageEngine.getPendingComponentMask(42), "getPendingComponentMask throws for unknown entities")
                    .as("getPendingComponentMask throws for unknown entities").isInstanceOf(StorageEngineException.class)
                    .as("getPendingComponentMask throws for unknown entities").hasMessageContainingAll("entity 42", "not present in storage");
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
