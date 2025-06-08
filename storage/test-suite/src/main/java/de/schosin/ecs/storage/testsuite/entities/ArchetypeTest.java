package de.schosin.ecs.storage.testsuite.entities;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import de.schosin.ecs.storage.testsuite.AbstractStorageEngineTest;
import de.schosin.ecs.utils.collections.ImmutableBag;

public class ArchetypeTest extends AbstractStorageEngineTest {

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
    class EntityDataTest {

        @Test
        void testSingleComponent() {
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
            assertThat(accessor.getComponent(0)).as("getComponent returns instance of current entity").isSameAs(current.equals(entity1) ? component1 : component2);

            // Iteration 2
            assertThat(accessor.hasNext()).as("second hasNext returns true for two entities").isTrue();
            current = assertThat(accessor.next()).as("next returns entityId").isIn(entity1, entity2).actual();
            assertThat(accessor.getComponent(0)).as("getComponent returns instance of current entity").isSameAs(current.equals(entity1) ? component1 : component2);

            // Iteration 3
            assertThat(accessor.hasNext()).as("third hasNext returns false for two entities").isFalse();
        }

        @Test
        void testMultipleComponents() {
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
            assertThat(accessor.getComponent(0)).as("getComponent returns instance of current entity").isSameAs(current.equals(entity1) ? component11 : component21);
            assertThat(accessor.getComponent(1)).as("getComponent returns instance of current entity").isSameAs(current.equals(entity1) ? component12 : component22);

            // Iteration 2
            assertThat(accessor.hasNext()).as("second hasNext returns true for two entities").isTrue();
            current = assertThat(accessor.next()).as("next returns entityId").isIn(entity1, entity2).actual();
            assertThat(accessor.getComponent(0)).as("getComponent returns instance of current entity").isSameAs(current.equals(entity1) ? component11 : component21);
            assertThat(accessor.getComponent(1)).as("getComponent returns instance of current entity").isSameAs(current.equals(entity1) ? component12 : component22);

            // Iteration 3
            assertThat(accessor.hasNext()).as("third hasNext returns false for two entities").isFalse();
        }

        @Test
        void testMultipleComponents_AccessInDifferentOrder() {
            var archetype = engine.getArchetype(component(C1.class), component(C2.class));

            var component11 = new C1(11);
            var component12 = new C2(12);
            var entity1 = world.createEntity(component11, component12);

            var component21 = new C1(21);
            var component22 = new C2(22);
            var entity2 = world.createEntity(component21, component22);

            var entities = archetype.getEntityData(component(C2.class), component(C1.class));
            var accessor = entities.getAccessor();

            // Iteration 1
            assertThat(accessor.hasNext()).as("first hasNext returns true for two entities").isTrue();
            var current = assertThat(accessor.next()).as("next returns entityId").isIn(entity1, entity2).actual();
            assertThat(accessor.getComponent(0)).as("getComponent returns instance of current entity").isSameAs(current.equals(entity1) ? component12 : component22);
            assertThat(accessor.getComponent(1)).as("getComponent returns instance of current entity").isSameAs(current.equals(entity1) ? component11 : component21);

            // Iteration 2
            assertThat(accessor.hasNext()).as("second hasNext returns true for two entities").isTrue();
            current = assertThat(accessor.next()).as("next returns entityId").isIn(entity1, entity2).actual();
            assertThat(accessor.getComponent(0)).as("getComponent returns instance of current entity").isSameAs(current.equals(entity1) ? component12 : component22);
            assertThat(accessor.getComponent(1)).as("getComponent returns instance of current entity").isSameAs(current.equals(entity1) ? component11 : component21);

            // Iteration 3
            assertThat(accessor.hasNext()).as("third hasNext returns false for two entities").isFalse();
        }

        @Test
        void testMultipleComponents_AccessFewerComponents() {
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
            assertThat(accessor.getComponent(0)).as("getComponent returns instance of current entity").isSameAs(current.equals(entity1) ? component11 : component21);

            // Iteration 2
            assertThat(accessor.hasNext()).as("second hasNext returns true for two entities").isTrue();
            current = assertThat(accessor.next()).as("next returns entityId").isIn(entity1, entity2).actual();
            assertThat(accessor.getComponent(0)).as("getComponent returns instance of current entity").isSameAs(current.equals(entity1) ? component11 : component21);

            // Iteration 3
            assertThat(accessor.hasNext()).as("third hasNext returns false for two entities").isFalse();
        }

        @Test
        void testMultipleComponents_AccessMoreComponents() {
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
            assertThat(accessor.getComponent(0)).as("getComponent returns instance of current entity").isSameAs(current.equals(entity1) ? component11 : component21);
            assertThat(accessor.getComponent(1)).as("getComponent returns instance of current entity").isNull();

            // Iteration 2
            assertThat(accessor.hasNext()).as("second hasNext returns true for two entities").isTrue();
            current = assertThat(accessor.next()).as("next returns entityId").isIn(entity1, entity2).actual();
            assertThat(accessor.getComponent(0)).as("getComponent returns instance of current entity").isSameAs(current.equals(entity1) ? component11 : component21);
            assertThat(accessor.getComponent(1)).as("getComponent returns instance of current entity").isNull();

            // Iteration 3
            assertThat(accessor.hasNext()).as("third hasNext returns false for two entities").isFalse();
        }

        @Test
        void testReset() {
            var archetype = engine.getArchetype(component(C1.class));

            var component1 = new C1(1);
            var entity1 = world.createEntity(component1);

            var component2 = new C1(2);
            var entity2 = world.createEntity(component2);

            var entities = archetype.getEntityData(component(C1.class));
            var accessor = entities.getAccessor();

            // Iterate once, reset
            accessor.hasNext();
            accessor.next();
            accessor.hasNext();
            accessor.next();

            accessor.reset();

            // Iteration 1
            assertThat(accessor.hasNext()).as("first hasNext returns true for two entities").isTrue();
            var current = assertThat(accessor.next()).as("next returns entityId").isIn(entity1, entity2).actual();
            assertThat(accessor.getComponent(0)).as("getComponent returns instance of current entity").isSameAs(current.equals(entity1) ? component1 : component2);

            // Iteration 2
            assertThat(accessor.hasNext()).as("second hasNext returns true for two entities").isTrue();
            current = assertThat(accessor.next()).as("next returns entityId").isIn(entity1, entity2).actual();
            assertThat(accessor.getComponent(0)).as("getComponent returns instance of current entity").isSameAs(current.equals(entity1) ? component1 : component2);

            // Iteration 3
            assertThat(accessor.hasNext()).as("third hasNext returns false for two entities").isFalse();
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

    record C3() {
    }

}
