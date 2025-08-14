package de.schosin.ecs.storage.testsuite.entities;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import de.schosin.ecs.api.Pooled;
import de.schosin.ecs.api.components.Relation;
import de.schosin.ecs.api.components.Relation.Exclusive;
import de.schosin.ecs.api.components.Relations;
import de.schosin.ecs.storage.api.StorageEngineException;
import de.schosin.ecs.storage.testsuite.AbstractStorageEngineTest;

public class CreateEntityTest extends AbstractStorageEngineTest {

    @Test
    void testNoComponents() {
        var archetype = storageEngine.getArchetype();

        assertThatCode(() -> archetype.createEntity(1, new Object[0])).as("empty archetype must not throw errors").doesNotThrowAnyException();
    }

    @Test
    void testNullComponents() {
        var archetype1 = storageEngine.getArchetype(component(C1.class));
        assertThatThrownBy(() -> archetype1.createEntity(1, new Object[] { null })).isInstanceOf(StorageEngineException.class);

        var archetype12 = storageEngine.getArchetype(component(C1.class), component(C2.class));
        assertThatThrownBy(() -> archetype12.createEntity(1, new Object[] { new C1(), null })).isInstanceOf(StorageEngineException.class);
    }

    @Test
    void testEntityAlreadyPresentInStore() {
        var archetype = storageEngine.getArchetype();
        archetype.createEntity(42, new Object[0]);

        assertThatThrownBy(() -> archetype.createEntity(42, new Object[0]))
                .isInstanceOf(StorageEngineException.class)
                .hasMessageContaining("already present in storage", "42");
    }

    /*
     * Affected archetype storage: Implementation failed to track entityId lookup based on index in certain scenarios
     */
    @Test
    void testReuseDataStructuresFromAlteredEntity() {
        var archetype = storageEngine.getArchetype(component(C1.class));

        // Create entities
        for (int i = 1; i <= 11; i++) {
            archetype.createEntity(i, new Object[] { new C1() });
        }

        // Alter their composition
        for (int i = 1; i <= 11; i++) {
            storageEngine.add(i, new Object[] { new C2() });
        }

        var mapper1 = storageEngine.getComponent(component(C1.class));

        // Reuse entity ids, creating a new with the archetype before alteration
        for (int i = 1; i <= 11; i++) {
            storageEngine.markDeleted(i);
            storageEngine.process();

            var component1 = new C1();
            archetype.createEntity(i, new Object[] { component1 });

            // Verify
            assertThat(mapper1.getComponent(i)).as("returns added components").isSameAs(component1);
        }

        for (int i = 1; i <= 11; i++) {
            var id = i;

            assertThatCode(() -> {
                storageEngine.markDeleted(id);
                storageEngine.process();

                var component1 = new C1();
                archetype.createEntity(id, new Object[] { component1 });

                // Verify
                assertThat(mapper1.getComponent(id)).as("returns added components").isSameAs(component1);
            }).doesNotThrowAnyException();
        }
    }

    @Test
    void testReuseEntityIdForSamePurpose() {
        // Create entity
        var archetype = storageEngine.getArchetype(component(C1.class));
        archetype.createEntity(1, new Object[] { new C1() });

        verifyHasComponents(1, C1.class);
        verifyDoesNotHaveComponents(1, C2.class);

        // Add C2
        storageEngine.add(1, new Object[] { new C2() });
        storageEngine.process();

        verifyHasComponents(1, C1.class, C2.class);

        // Delete entity
        storageEngine.markDeleted(1);
        storageEngine.process();

        verifyDoesNotHaveComponents(1, C1.class, C2.class);

        // Create entity with reused id
        archetype.createEntity(1, new Object[] { new C1() });

        verifyHasComponents(1, C1.class);
        verifyDoesNotHaveComponents(1, C2.class);

        // Add C2
        storageEngine.add(1, new Object[] { new C2() });
        storageEngine.process();

        verifyHasComponents(1, C1.class, C2.class);

        // Delete entity
        storageEngine.markDeleted(1);
        storageEngine.process();

        verifyDoesNotHaveComponents(1, C1.class, C2.class);
    }

    @Nested
    class ArchetypeTest {

        @Test
        void testClassTypes() {
            var archetype = storageEngine.getArchetype(component(C1.class), component(C2.class));
            archetype.createEntity(1, new Object[] { new C1(), new C2() });

            // Verify
            assertThat(archetype.getComponentTypes()).as("must contain matching component types").containsExactlyInAnyOrder(component(C1.class), component(C2.class));
            assertThat(archetype.getComponents()).extracting("type").as("must contain matching component types").containsExactlyInAnyOrder(component(C1.class), component(C2.class));

            assertThat(storageEngine.getArchetypeForEntity(1)).as("getArchetypeForEntity returns same instance").isSameAs(archetype);
        }

        @Test
        void testPooledClassTypes() {
            var archetype = storageEngine.getArchetype(component(P1.class), component(P2.class));
            archetype.createEntity(1, new Object[] { new P1(), new P2() });

            // Verify
            assertThat(archetype.getComponentTypes()).as("must contain matching component types").containsExactlyInAnyOrder(component(P1.class), component(P2.class));
            assertThat(archetype.getComponents()).extracting("type").as("must contain matching component types").containsExactlyInAnyOrder(component(P1.class), component(P2.class));

            assertThat(storageEngine.getArchetypeForEntity(1)).as("getArchetypeForEntity returns same instance").isSameAs(archetype);
        }

        @Test
        void testComponentRelations() {
            var archetype = storageEngine.getArchetype(relation(C1.class, C2.class), relation(C2.class, C1.class));
            archetype.createEntity(1, new Object[] { Relation.create(new C1(), new C2()), Relation.create(new C2(), new C1()) });

            // Verify
            assertThat(archetype.getComponentTypes()).as("must contain matching component types").containsExactlyInAnyOrder(relation(C1.class, C2.class), relation(C2.class, C1.class));
            assertThat(archetype.getComponents()).extracting("type").as("must contain matching component types").containsExactlyInAnyOrder(relation(C1.class, C2.class), relation(C2.class, C1.class));

            assertThat(storageEngine.getArchetypeForEntity(1)).as("getArchetypeForEntity returns same instance").isSameAs(archetype);
        }

        @Test
        void testExclusiveComponentRelations() {
            var archetype = storageEngine.getArchetype(exclusiveRelation(E1.class, C2.class), exclusiveRelation(E2.class, C2.class));
            archetype.createEntity(1, new Object[] { Relation.create(E1.INSTANCE, new C2()), Relation.create(E2.INSTANCE, new C2()) });

            // Verify
            assertThat(archetype.getComponentTypes())
                    .as("must contain matching component types").containsExactlyInAnyOrder(exclusiveRelation(E1.class, C2.class), exclusiveRelation(E2.class, C2.class));
            assertThat(archetype.getComponents()).extracting("type")
                    .as("must contain matching component types").containsExactlyInAnyOrder(exclusiveRelation(E1.class, C2.class), exclusiveRelation(E2.class, C2.class));

            assertThat(storageEngine.getArchetypeForEntity(1)).as("getArchetypeForEntity returns same instance").isSameAs(archetype);
        }

        @Test
        void testEntityRelations() {
            var archetype = storageEngine.getArchetype(relation(C1.class), relation(C2.class));
            archetype.createEntity(1, new Object[] { Relation.create(new C1(), 2), Relation.create(new C2(), 2) });

            // Verify
            assertThat(archetype.getComponentTypes()).as("must contain matching component types").containsExactlyInAnyOrder(relation(C1.class), relation(C2.class));
            assertThat(archetype.getComponents()).extracting("type").as("must contain matching component types").containsExactlyInAnyOrder(relation(C1.class), relation(C2.class));

            assertThat(storageEngine.getArchetypeForEntity(1)).as("getArchetypeForEntity returns same instance").isSameAs(archetype);
        }

        @Test
        void testExclusiveEntityRelations() {
            var archetype = storageEngine.getArchetype(exclusiveRelation(E1.class), exclusiveRelation(E2.class));
            archetype.createEntity(1, new Object[] { Relation.create(E1.INSTANCE, 2), Relation.create(E2.INSTANCE, 2) });

            // Verify
            assertThat(archetype.getComponentTypes()).as("must contain matching component types").containsExactlyInAnyOrder(exclusiveRelation(E1.class), exclusiveRelation(E2.class));
            assertThat(archetype.getComponents()).extracting("type").as("must contain matching component types").containsExactlyInAnyOrder(exclusiveRelation(E1.class), exclusiveRelation(E2.class));

            assertThat(storageEngine.getArchetypeForEntity(1)).as("getArchetypeForEntity returns same instance").isSameAs(archetype);
        }

    }

    @Nested
    class ComponentDataTest {

        @Test
        void testClassTypes() {
            var component1 = new C1();
            var component2 = new C2();

            var archetype = storageEngine.getArchetype(component(C1.class), component(C2.class));
            archetype.createEntity(1, new Object[] { component1, component2 });

            // Verify
            assertThat(storageEngine.getComponent(component(C1.class)).getComponent(1)).as("Must store component instance").isSameAs(component1);
            assertThat(storageEngine.getComponent(component(C2.class)).getComponent(1)).as("Must store component instance").isSameAs(component2);
        }

        @Test
        void testPooledClassTypes() {
            var component1 = new P1();
            var component2 = new P2();

            var archetype = storageEngine.getArchetype(component(P1.class), component(P2.class));
            archetype.createEntity(1, new Object[] { component1, component2 });

            // Verify
            assertThat(storageEngine.getComponent(component(P1.class)).getComponent(1)).as("Must store component instance").isSameAs(component1);
            assertThat(storageEngine.getComponent(component(P2.class)).getComponent(1)).as("Must store component instance").isSameAs(component2);
        }

        @Test
        void testMultipleComponentRelationInstaces() {
            var component1 = Relation.create(new C1(), new C2());
            var component2 = Relation.create(new C2(), new C1());

            var archetype = storageEngine.getArchetype(relation(C1.class, C2.class), relation(C2.class, C1.class));
            archetype.createEntity(1, new Object[] { component1, component2 });

            // Verify
            assertThat(storageEngine.getComponent(relation(C1.class, C2.class)).getComponent(1)).as("Must store component instance").containsExactly(component1);
            assertThat(storageEngine.getComponent(relation(C2.class, C1.class)).getComponent(1)).as("Must store component instance").containsExactly(component2);
        }

        @Test
        void testComponentRelations() {
            var relation1 = Relation.create(new C1(11), new C2(12));
            var relation2 = Relation.create(new C1(12), new C2(22));

            var relations = Relations.of(relation1, relation2);

            var archetype = storageEngine.getArchetype(relation(C1.class, C2.class));
            archetype.createEntity(1, new Object[] { relations });

            // Verify
            assertThat(storageEngine.getComponent(relation(C1.class, C2.class)).getComponent(1)).as("Must store relations").containsExactlyInAnyOrder(relation1, relation2);

            assertThat(relations).as("must return relations back to the pool").isEmpty();
            assertThat(Relations.of(Relation.create(new C1(13), new C2(13)))).as("must return relations back to the pool").isSameAs(relations);
        }

        @Test
        void testComponentRelations_EqualTargets() {
            var relation1 = Relation.create(new C1(11), new C2(12));
            var relation2 = Relation.create(new C1(12), new C2(12));

            var relations = Relations.of(relation1, relation2);

            var archetype = storageEngine.getArchetype(relation(C1.class, C2.class));
            archetype.createEntity(1, new Object[] { relations });

            // Verify
            assertThat(storageEngine.getComponent(relation(C1.class, C2.class)).getComponent(1)).as("Must only store last relation for a given target").containsExactly(relation2);

            assertThat(relation1.relationship()).as("must return replaced relation back to pool").isNull();
            assertThat(relation1.target()).as("must return replaced relation back to pool").isNull();
            assertThat(Relation.create(new C1(4), new C2(4))).as("must return replaced relation back to pool").isSameAs(relation1);

            assertThat(relations).as("must return relations back to the pool").isEmpty();
            assertThat(Relations.of(Relation.create(new C1(13), new C2(13)))).as("must return relations back to the pool").isSameAs(relations);
        }

        @Test
        void testExclusiveComponentRelations() {
            var component1 = Relation.create(E1.INSTANCE, new C2());
            var component2 = Relation.create(E2.INSTANCE, new C2());

            var archetype = storageEngine.getArchetype(exclusiveRelation(E1.class, C2.class), exclusiveRelation(E2.class, C2.class));
            archetype.createEntity(1, new Object[] { component1, component2 });

            // Verify
            assertThat(storageEngine.getComponent(exclusiveRelation(E1.class, C2.class)).getComponent(1)).as("Must store component instance").isSameAs(component1);
            assertThat(storageEngine.getComponent(exclusiveRelation(E2.class, C2.class)).getComponent(1)).as("Must store component instance").isSameAs(component2);
        }

        @Test
        void testEntityRelations() {
            var relation1 = Relation.create(new C1(1), 2);
            var relation2 = Relation.create(new C1(2), 3);

            var relations = Relations.of(relation1, relation2);

            var archetype = storageEngine.getArchetype(relation(C1.class));
            archetype.createEntity(1, new Object[] { relations });

            // Verify
            assertThat(storageEngine.getComponent(relation(C1.class)).getComponent(1)).as("Must store relations").containsExactlyInAnyOrder(relation1, relation2);

            assertThat(relations).as("must return relations back to the pool").isEmpty();
            assertThat(Relations.of(Relation.create(new C1(3), 4))).as("must return relations back to the pool").isSameAs(relations);
        }

        @Test
        void testEntityRelations_EqualTargets() {
            var relation1 = Relation.create(new C1(1), 2);
            var relation2 = Relation.create(new C1(2), 2);

            var relations = Relations.of(relation1, relation2);

            var archetype = storageEngine.getArchetype(relation(C1.class));
            archetype.createEntity(1, new Object[] { relations });

            // Verify
            assertThat(storageEngine.getComponent(relation(C1.class)).getComponent(1)).as("Must only store last relation for a given target").containsExactly(relation2);

            assertThat(relation1.relationship()).as("must return replaced relation back to pool").isNull();
            assertThat(relation1.target()).as("must return replaced relation back to pool").isEqualTo(-1);
            assertThat(Relation.create(new C1(4), 4)).as("must return replaced relation back to pool").isSameAs(relation1);

            assertThat(relations).as("must return relations back to the pool").isEmpty();
            assertThat(Relations.of(Relation.create(new C1(3), 4))).as("must return relations back to the pool").isSameAs(relations);
        }

        @Test
        void testExclusiveEntityRelations() {
            var component1 = Relation.create(E1.INSTANCE, 2);
            var component2 = Relation.create(E2.INSTANCE, 2);

            var archetype = storageEngine.getArchetype(exclusiveRelation(E1.class), exclusiveRelation(E2.class));
            archetype.createEntity(1, new Object[] { component1, component2 });

            // Verify
            assertThat(storageEngine.getComponent(exclusiveRelation(E1.class)).getComponent(1)).as("Must store component instance").isSameAs(component1);
            assertThat(storageEngine.getComponent(exclusiveRelation(E2.class)).getComponent(1)).as("Must store component instance").isSameAs(component2);
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
