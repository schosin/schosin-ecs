package de.schosin.ecs.storage.testsuite.entities;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import de.schosin.ecs.api.Pooled;
import de.schosin.ecs.api.components.Relation.Exclusive;
import de.schosin.ecs.api.components.types.ComponentType.RegularComponentType;
import de.schosin.ecs.storage.api.StorageEngineException;
import de.schosin.ecs.storage.testsuite.AbstractStorageEngineTest;
import de.schosin.ecs.utils.collections.ImmutableBag;

public class GetArchetypeTest extends AbstractStorageEngineTest {

    @Nested
    class EmptyArchetype {

        @Test
        void testNoVarargs() {
            var archetype = engine.getArchetype();
            assertThat(archetype).as("empty var args must not return null").isNotNull();

            assertThat(archetype.getId()).as("id must not be negative").isNotNegative();
            assertThat(archetype.getComponentTypes()).as("getComponentTypes must return non-null and empty bag").isEmpty();
            assertThat(archetype.getComponents()).as("getComponents must return non-null and empty bag").isEmpty();
        }

        @Test
        void testEmptyArray() {
            var archetype = engine.getArchetype(new RegularComponentType<?, ?>[0]);
            assertThat(archetype).as("empty array must not return null").isNotNull();

            assertThat(archetype.getId()).as("id must not be negative").isNotNegative();
            assertThat(archetype.getComponentTypes()).as("getComponentTypes must return non-null and empty bag").isEmpty();
            assertThat(archetype.getComponents()).as("getComponents must return non-null and empty bag").isEmpty();
        }

        @Test
        void testSameInstanceReturned() {
            var archetype = engine.getArchetype();

            assertThat(engine.getArchetype()).as("must return same archetype if queried again").isSameAs(archetype);
            assertThat(engine.getArchetype(new RegularComponentType<?, ?>[0])).as("must return same archetype if queried again").isSameAs(archetype);
        }

        @Test
        void testNullArray() {
            assertThatThrownBy(() -> engine.getArchetype((RegularComponentType<?, ?>[]) null), "null array should throw an exception");
        }

        @Test
        void testNullTypes() {
            assertThatThrownBy(() -> engine.getArchetype(new RegularComponentType<?, ?>[] { null }), "null types should throw an exception");
            assertThatThrownBy(() -> engine.getArchetype(new RegularComponentType<?, ?>[] { component(C1.class), null }), "null types should throw an exception");
        }

        @Test
        void testGetByComponentId() {
            var archetype = engine.getArchetype();

            assertThat(engine.getArchetypeById(archetype.getId())).as("getArchetypeById must return same instance").isSameAs(archetype);
        }

    }

    @Nested
    class OneComponentType {

        @Test
        void testClassType() {
            var archetype = engine.getArchetype(component(C1.class));

            // Verify
            assertThat(archetype.getComponentTypes()).as("getComponentTypes must contain only passed type by equality").containsExactly(component(C1.class));
            assertThat(archetype.getComponents()).extracting("type").as("getComponents must match getComponentTypes").containsExactly(component(C1.class));

            assertThat(engine.getArchetypeById(archetype.getId())).as("getArchetypeById must return same instance").isSameAs(archetype);
        }

        @Test
        void testPooledClassType() {
            var archetype = engine.getArchetype(component(P1.class));

            // Verify
            assertThat(archetype.getComponentTypes()).as("getComponentTypes must contain only passed component type by equality").containsExactly(component(P1.class));
            assertThat(archetype.getComponents()).extracting("type").as("getComponents must match getComponentTypes").containsExactly(component(P1.class));

            assertThat(engine.getArchetypeById(archetype.getId())).as("getArchetypeById must return same instance").isSameAs(archetype);
        }

        @Test
        void testComponentRelation() {
            var archetype = engine.getArchetype(relation(C1.class, C2.class));

            // Verify
            assertThat(archetype.getComponentTypes()).as("getComponentTypes must contain only passed type by equality").containsExactly(relation(C1.class, C2.class));
            assertThat(archetype.getComponents()).extracting("type").as("getComponents must match getComponentTypes").containsExactly(relation(C1.class, C2.class));

            assertThat(engine.getArchetypeById(archetype.getId())).as("getArchetypeById must return same instance").isSameAs(archetype);
        }

        @Test
        void testExclusiveComponentRelation() {
            var archetype = engine.getArchetype(exclusiveRelation(E1.class, C2.class));

            // Verify
            assertThat(archetype.getComponentTypes()).as("getComponentTypes must contain only passed type by equality").containsExactly(exclusiveRelation(E1.class, C2.class));
            assertThat(archetype.getComponents()).extracting("type").as("getComponents must match getComponentTypes").containsExactly(exclusiveRelation(E1.class, C2.class));

            assertThat(engine.getArchetypeById(archetype.getId())).as("getArchetypeById must return same instance").isSameAs(archetype);
        }

        @Test
        void testEntityRelation() {
            var archetype = engine.getArchetype(relation(C1.class));

            // Verify
            assertThat(archetype.getComponentTypes()).as("getComponentTypes must contain only passed type by equality").containsExactly(relation(C1.class));
            assertThat(archetype.getComponents()).extracting("type").as("getComponents must match getComponentTypes").containsExactly(relation(C1.class));

            assertThat(engine.getArchetypeById(archetype.getId())).as("getArchetypeById must return same instance").isSameAs(archetype);
        }

        @Test
        void testExclusiveEntityRelation() {
            var archetype = engine.getArchetype(exclusiveRelation(E1.class));

            // Verify
            assertThat(archetype.getComponentTypes()).as("getComponentTypes must contain only passed type by equality").containsExactly(exclusiveRelation(E1.class));
            assertThat(archetype.getComponents()).extracting("type").as("getComponents must match getComponentTypes").containsExactly(exclusiveRelation(E1.class));

            assertThat(engine.getArchetypeById(archetype.getId())).as("getArchetypeById must return same instance").isSameAs(archetype);
        }

    }

    @Nested
    class MultipleComponentType {

        @Test
        void testClassTypes() {
            var archetype = engine.getArchetype(component(C1.class), component(C2.class));

            // Verify
            assertThat(archetype.getComponentTypes())
                    .as("getComponentTypes must contain only passed types by equality").containsExactlyInAnyOrder(component(C1.class), component(C2.class));

            assertThat(archetype.getComponents()).extracting("type")
                    .as("getComponents must match getComponentTypes").containsExactlyInAnyOrder(component(C1.class), component(C2.class));

            assertThat(engine.getArchetype(component(C2.class), component(C1.class))).as("order of types does not matter").isSameAs(archetype);

            assertThat(engine.getArchetypeById(archetype.getId())).as("getArchetypeById must return same instance").isSameAs(archetype);
        }

        @Test
        void testPooledClassTypes() {
            var archetype = engine.getArchetype(component(P1.class), component(P2.class));

            // Verify
            assertThat(archetype.getComponentTypes())
                    .as("getComponentTypes must contain only passed component types by equality").containsExactlyInAnyOrder(component(P1.class), component(P2.class));

            assertThat(archetype.getComponents()).extracting("type")
                    .as("getComponents must match getComponentTypes").containsExactlyInAnyOrder(component(P1.class), component(P2.class));

            assertThat(engine.getArchetype(component(P2.class), component(P1.class))).as("order of types does not matter").isSameAs(archetype);

            assertThat(engine.getArchetypeById(archetype.getId())).as("getArchetypeById must return same instance").isSameAs(archetype);
        }

        @Test
        void testComponentRelations() {
            var archetype = engine.getArchetype(relation(C1.class, C2.class), relation(C2.class, C1.class));

            // Verify
            assertThat(archetype.getComponentTypes())
                    .as("getComponentTypes must contain only passed types by equality").containsExactlyInAnyOrder(relation(C1.class, C2.class), relation(C2.class, C1.class));

            assertThat(archetype.getComponents()).extracting("type")
                    .as("getComponents must match getComponentTypes").containsExactlyInAnyOrder(relation(C1.class, C2.class), relation(C2.class, C1.class));

            assertThat(engine.getArchetype(relation(C2.class, C1.class), relation(C1.class, C2.class))).as("order of types does not matter").isSameAs(archetype);

            assertThat(engine.getArchetypeById(archetype.getId())).as("getArchetypeById must return same instance").isSameAs(archetype);
        }

        @Test
        void testExclusiveComponentRelations() {
            var archetype = engine.getArchetype(exclusiveRelation(E1.class, C2.class), exclusiveRelation(E2.class, C2.class));

            // Verify
            assertThat(archetype.getComponentTypes())
                    .as("getComponentTypes must contain only passed types by equality").containsExactlyInAnyOrder(exclusiveRelation(E1.class, C2.class), exclusiveRelation(E2.class, C2.class));

            assertThat(archetype.getComponents()).extracting("type")
                    .as("getComponents must match getComponentTypes").containsExactlyInAnyOrder(exclusiveRelation(E1.class, C2.class), exclusiveRelation(E2.class, C2.class));

            assertThat(engine.getArchetype(exclusiveRelation(E2.class, C2.class), exclusiveRelation(E1.class, C2.class))).as("order of types does not matter").isSameAs(archetype);

            assertThat(engine.getArchetypeById(archetype.getId())).as("getArchetypeById must return same instance").isSameAs(archetype);
        }

        @Test
        void testEntityRelations() {
            var archetype = engine.getArchetype(relation(C1.class), relation(C2.class));

            // Verify
            assertThat(archetype.getComponentTypes())
                    .as("getComponentTypes must contain only passed types by equality").containsExactlyInAnyOrder(relation(C1.class), relation(C2.class));

            assertThat(archetype.getComponents()).extracting("type")
                    .as("getComponents must match getComponentTypes").containsExactlyInAnyOrder(relation(C1.class), relation(C2.class));

            assertThat(engine.getArchetype(relation(C2.class), relation(C1.class))).as("order of types does not matter").isSameAs(archetype);

            assertThat(engine.getArchetypeById(archetype.getId())).as("getArchetypeById must return same instance").isSameAs(archetype);
        }

        @Test
        void testExclusiveEntityRelations() {
            var archetype = engine.getArchetype(exclusiveRelation(E1.class), exclusiveRelation(E2.class));

            // Verify
            assertThat(archetype.getComponentTypes())
                    .as("getComponentTypes must contain only passed types by equality").containsExactlyInAnyOrder(exclusiveRelation(E1.class), exclusiveRelation(E2.class));

            assertThat(archetype.getComponents()).extracting("type")
                    .as("getComponents must match getComponentTypes").containsExactlyInAnyOrder(exclusiveRelation(E1.class), exclusiveRelation(E2.class));

            assertThat(engine.getArchetype(exclusiveRelation(E2.class), exclusiveRelation(E1.class))).as("order of types does not matter").isSameAs(archetype);

            assertThat(engine.getArchetypeById(archetype.getId())).as("getArchetypeById must return same instance").isSameAs(archetype);
        }

        @Test
        void testMixedTypes() {
            var archetype = engine.getArchetype(
                    component(C1.class),
                    component(P1.class),
                    relation(C1.class, C2.class),
                    exclusiveRelation(E1.class, C2.class),
                    relation(C1.class),
                    exclusiveRelation(E1.class));

            assertThat(archetype.getComponentTypes())
                    .as("getComponentTypes must contain only passed types by equality").containsExactlyInAnyOrder(
                            component(C1.class),
                            component(P1.class),
                            relation(C1.class, C2.class),
                            exclusiveRelation(E1.class, C2.class),
                            relation(C1.class),
                            exclusiveRelation(E1.class));

            assertThat(archetype.getComponents()).extracting("type")
                    .as("getComponents must match getComponentTypes").containsExactlyInAnyOrder(
                            component(C1.class),
                            component(P1.class),
                            relation(C1.class, C2.class),
                            exclusiveRelation(E1.class, C2.class),
                            relation(C1.class),
                            exclusiveRelation(E1.class));

            var differentOrder = engine.getArchetype(
                    component(P1.class),
                    component(C1.class),
                    relation(C1.class, C2.class),
                    exclusiveRelation(E1.class, C2.class),
                    relation(C1.class),
                    exclusiveRelation(E1.class));

            assertThat(differentOrder).as("order of types does not matter").isSameAs(archetype);

            assertThat(engine.getArchetypeById(archetype.getId())).as("getArchetypeById must return same instance").isSameAs(archetype);
        }

        @Test
        void testDuplicateClassTypes() {
            assertThatThrownBy(() -> engine.getArchetype(component(C1.class), component(C1.class)))
                    .isInstanceOf(StorageEngineException.class)
                    .hasMessageContainingAll("duplicate component type", C1.class.getSimpleName());
        }

    }

    @Test
    void testGetArchetypes() {
        var type1 = component(C1.class);
        var type2 = component(P1.class);
        var type3 = relation(C1.class, C2.class);
        var type4 = exclusiveRelation(E1.class, C2.class);
        var type5 = relation(C1.class);
        var type6 = exclusiveRelation(E1.class);

        assertThat(engine.getArchetypes()).as("only contains empty archetype if no types known").hasSize(1);

        var emptyArchetype = engine.getArchetype();
        assertThat(engine.getArchetypes()).as("only contains empty archetype if no types known").containsExactly(emptyArchetype);

        engine.getArchetype(type1);
        assertThat(engine.getArchetypes()).as("returns all known types").hasSize(2);

        engine.getArchetype(type2);
        assertThat(engine.getArchetypes()).as("returns all known types").hasSize(3);

        engine.getArchetype(type3);
        assertThat(engine.getArchetypes()).as("returns all known types").hasSize(4);

        engine.getArchetype(type4);
        assertThat(engine.getArchetypes()).as("returns all known types").hasSize(5);

        engine.getArchetype(type5);
        assertThat(engine.getArchetypes()).as("returns all known types").hasSize(6);

        engine.getArchetype(type6);
        assertThat(engine.getArchetypes()).as("returns all known types").hasSize(7);
    }

    @Test
    void testGetArchetypeAfterAdd() {
        var emptyArchetype = storageEngine.getArchetype();
        var archetype1 = storageEngine.getArchetype(component(C1.class));

        var entityId = world.createEntity();
        assertThat(storageEngine.getArchetypeForEntity(entityId)).as("getArchetypeForEntity returns empty archetype for empty entity").isSameAs(emptyArchetype);

        // Call
        var updatedArchetype = storageEngine.add(entityId, new Object[] { new C1() });
        assertThat(updatedArchetype).as("add returns updated archetype").isSameAs(archetype1);

        // Verify
        assertThat(storageEngine.getArchetypeForEntity(entityId)).as("getArchetypeForEntity returns old archetype after add").isSameAs(emptyArchetype);
        assertThat(storageEngine.getPendingArchetype(entityId)).as("getPendingArchetype returns updated archetype after add").isSameAs(updatedArchetype);
    }

    @Test
    void testGetArchetypeAfterAddFlushed() {
        var emptyArchetype = storageEngine.getArchetype();
        var archetype1 = storageEngine.getArchetype(component(C1.class));

        var entityId = world.createEntity();
        assertThat(storageEngine.getArchetypeForEntity(entityId)).as("getArchetypeForEntity returns empty archetype for empty entity").isSameAs(emptyArchetype);

        // Call
        var updatedArchetype = storageEngine.add(entityId, new Object[] { new C1() });
        assertThat(updatedArchetype).as("add returns updated archetype").isSameAs(archetype1);

        assertThat(storageEngine.flushChanges(entityId)).as("flushChanges returns updated archetype").isSameAs(archetype1);

        // Verify
        assertThat(storageEngine.getArchetypeForEntity(entityId)).as("getArchetypeForEntity returns old archetype after flushed add").isSameAs(archetype1);
        assertThat(storageEngine.getPendingArchetype(entityId)).as("getPendingArchetype returns null if no changes").isNull();
    }

    @Test
    void testGetArchetypeAfterRemove() {
        var emptyArchetype = storageEngine.getArchetype();
        var archetype1 = storageEngine.getArchetype(component(C1.class));

        var entityId = world.createEntity(new C1());
        assertThat(storageEngine.getArchetypeForEntity(entityId)).as("getArchetypeForEntity returns correct archetype for entity").isSameAs(archetype1);

        // Call
        var updatedArchetype = storageEngine.remove(entityId, ImmutableBag.of(component(C1.class)));
        assertThat(updatedArchetype).as("add returns updated archetype").isSameAs(emptyArchetype);

        // Verify
        assertThat(storageEngine.getArchetypeForEntity(entityId)).as("getArchetypeForEntity returns old archetype after add").isSameAs(archetype1);
        assertThat(storageEngine.getPendingArchetype(entityId)).as("getPendingArchetype returns updated archetype after add").isSameAs(updatedArchetype);
    }

    @Test
    void testGetArchetypeAfterRemoveFlushed() {
        var emptyArchetype = storageEngine.getArchetype();
        var archetype1 = storageEngine.getArchetype(component(C1.class));

        var entityId = world.createEntity(new C1());
        assertThat(storageEngine.getArchetypeForEntity(entityId)).as("getArchetypeForEntity returns correct archetype for entity").isSameAs(archetype1);

        // Call
        var updatedArchetype = storageEngine.remove(entityId, ImmutableBag.of(component(C1.class)));
        assertThat(updatedArchetype).as("add returns updated archetype").isSameAs(emptyArchetype);

        assertThat(storageEngine.flushChanges(entityId)).as("flushChanges returns updated archetype").isSameAs(emptyArchetype);

        // Verify
        assertThat(storageEngine.getArchetypeForEntity(entityId)).as("getArchetypeForEntity returns old archetype after flushed remove").isSameAs(updatedArchetype);
        assertThat(storageEngine.getPendingArchetype(entityId)).as("getPendingArchetype returns null if no changes").isNull();
    }

    @Test
    void testGetArchetypeAfterModify() {
        var archetype1 = storageEngine.getArchetype(component(C1.class));
        var archetype2 = storageEngine.getArchetype(component(C2.class));

        var entityId = world.createEntity(new C1());
        assertThat(storageEngine.getArchetypeForEntity(entityId)).as("getArchetypeForEntity returns correct archetype for entity").isSameAs(archetype1);

        // Call
        var updatedArchetype = storageEngine.modify(entityId, new Object[] { new C2() }, ImmutableBag.of(component(C1.class)));
        assertThat(updatedArchetype).as("add returns updated archetype").isSameAs(archetype2);

        // Verify
        assertThat(storageEngine.getArchetypeForEntity(entityId)).as("getArchetypeForEntity returns old archetype after modify").isSameAs(archetype1);
        assertThat(storageEngine.getPendingArchetype(entityId)).as("getPendingArchetype returns updated archetype after modify").isSameAs(archetype2);
    }

    @Test
    void testGetArchetypeAfterModifyFlushed() {
        var archetype1 = storageEngine.getArchetype(component(C1.class));
        var archetype2 = storageEngine.getArchetype(component(C2.class));

        var entityId = world.createEntity(new C1());
        assertThat(storageEngine.getArchetypeForEntity(entityId)).as("getArchetypeForEntity returns correct archetype for entity").isSameAs(archetype1);

        // Call
        var updatedArchetype = storageEngine.modify(entityId, new Object[] { new C2() }, ImmutableBag.of(component(C1.class)));
        assertThat(updatedArchetype).as("add returns updated archetype").isSameAs(archetype2);

        assertThat(storageEngine.flushChanges(entityId)).as("flushChanges returns updated archetype").isSameAs(archetype2);

        // Verify
        assertThat(storageEngine.getArchetypeForEntity(entityId)).as("getArchetypeForEntity returns updated archetype after flushed modify").isSameAs(archetype2);
        assertThat(storageEngine.getPendingArchetype(entityId)).as("getPendingArchetype returns null if no changes").isNull();
    }

    @Test
    void testGetPendingArchetype_NoChanges() {
        var entityId = world.createEntity();

        assertThat(storageEngine.getPendingArchetype(entityId)).as("getPendingArchetype returns null if no changes").isNull();
    }

    @Test
    void testGetPendingArchetype_ChangesFlushed() {
        var entityId = world.createEntity();

        storageEngine.add(entityId, new Object[] { new C1() });
        storageEngine.flushChanges(entityId);

        assertThat(storageEngine.getPendingArchetype(entityId)).as("getPendingArchetype returns null if no changes").isNull();
    }

    @Test
    void testGetPendingArchetype_DeletedEntity() {
        var entityId = world.createEntity();

        storageEngine.delete(entityId);

        assertThatThrownBy(() -> storageEngine.getPendingArchetype(entityId), "getPendingArchetype throws for deleted entities")
                .as("getPendingArchetype throws for deleted entities").isInstanceOf(StorageEngineException.class)
                .as("getPendingArchetype throws for deleted entities").hasMessageContainingAll("entity %d".formatted(entityId), "not present in storage");
    }

    @Test
    void testGetPendingArchetype_UnknownEntity() {
        assertThatThrownBy(() -> storageEngine.getPendingArchetype(42), "getPendingArchetype throws for unknown entities")
                .as("getPendingArchetype throws for unknown entities").isInstanceOf(StorageEngineException.class)
                .as("getPendingArchetype throws for unknown entities").hasMessageContainingAll("entity 42", "not present in storage");
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
