package de.schosin.ecs.storage.testsuite.entities;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assumptions.assumeThat;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import de.schosin.ecs.api.components.Relation;
import de.schosin.ecs.api.components.Relation.Exclusive;
import de.schosin.ecs.api.data.DataAccessor;
import de.schosin.ecs.storage.api.StorageEngineException;
import de.schosin.ecs.storage.testsuite.AbstractStorageEngineTest;

public class AccessorTest extends AbstractStorageEngineTest {

    @Test
    void testGetAccessorOfUnknownEntity() {
        assertThatThrownBy(() -> engine.getAccessor(42))
                .isInstanceOf(StorageEngineException.class)
                .hasMessageContainingAll("entity 42", "not present in storage");
    }

    @Test
    void testEntityId() {
        var archetype = engine.getArchetype();
        archetype.createEntity(42, new Object[0]);

        var accessor = engine.getAccessor(42);
        assertThat(accessor.entityId()).as("accessor.entityId() must return id when creating for entity").isEqualTo(42);
    }

    @Nested
    class IterableAccessorTest {

        @Test
        void testFree() {
            var archetype = engine.getArchetype();
            archetype.createEntity(42, new Object[0]);

            var accessor = archetype.getAccessor();
            assertThat(accessor.isValid()).as("accessor.isValid() must return true before free()").isTrue();

            accessor.free();
            assertThat(accessor.isValid()).as("accessor.isValid() must return false after free()").isFalse();
        }

        @Test
        void testClose() {
            var archetype = engine.getArchetype();
            archetype.createEntity(42, new Object[0]);

            var accessor = archetype.getAccessor();
            assertThat(accessor.isValid()).as("accessor.isValid() must return true before close()").isTrue();

            accessor.close();
            assertThat(accessor.isValid()).as("accessor.isValid() must return false after close()").isFalse();
        }

        @Test
        void testTryWithResources() {
            var archetype = engine.getArchetype();
            archetype.createEntity(42, new Object[0]);

            var accessor = archetype.getAccessor();
            try (accessor) {
                assertThat(accessor.isValid()).as("accessor.isValid() must return true when within try-with-resources").isTrue();
            }

            assertThat(accessor.isValid()).as("accessor.isValid() must return false after try-with-resources").isFalse();
        }

    }

    @Nested
    class HasComponentTest {

        @Test
        void testHasComponent_NoComponentsAssigned() {
            var type1 = engine.getComponent(component(C1.class)).id();
            var type2 = engine.getComponent(component(C2.class)).id();
            var relation12 = engine.getComponent(relation(C1.class, C2.class)).id();
            var exclusive12 = engine.getComponent(exclusiveRelation(E1.class, C2.class)).id();
            var relation1 = engine.getComponent(relation(C1.class)).id();
            var exclusive1 = engine.getComponent(exclusiveRelation(E1.class)).id();

            var archetype = engine.getArchetype();
            archetype.createEntity(42, new Object[0]);

            var accessor = engine.getAccessor(42);

            // Verify
            assertThat(accessor.hasComponent(type1)).as("hasComponent must return false if not assigned to entity").isFalse();
            assertThat(accessor.hasComponent(type2)).as("hasComponent must return false if not assigned to entity").isFalse();
            assertThat(accessor.hasComponent(relation12)).as("hasComponent must return false if not assigned to entity").isFalse();
            assertThat(accessor.hasComponent(exclusive12)).as("hasComponent must return false if not assigned to entity").isFalse();
            assertThat(accessor.hasComponent(relation1)).as("hasComponent must return false if not assigned to entity").isFalse();
            assertThat(accessor.hasComponent(exclusive1)).as("hasComponent must return false if not assigned to entity").isFalse();
        }

        @Test
        void testHasComponent() {
            var type1 = engine.getComponent(component(C1.class));
            var type2 = engine.getComponent(component(C2.class));
            var relation12 = engine.getComponent(relation(C1.class, C2.class));
            var exclusive12 = engine.getComponent(exclusiveRelation(E1.class, C2.class));
            var relation1 = engine.getComponent(relation(C1.class));
            var exclusive1 = engine.getComponent(exclusiveRelation(E1.class));

            var archetype = engine.getArchetype(type1.type(), type2.type(), relation12.type(), exclusive12.type(), relation1.type(), exclusive1.type());
            archetype.createEntity(42, new Object[] {
                    new C1(), new C2(),
                    Relation.create(new C1(), new C2()), Relation.create(E1.INSTANCE, new C2()),
                    Relation.create(new C1(), 7), Relation.create(E1.INSTANCE, 7)
            });

            var accessor = engine.getAccessor(42);

            // Verify
            assertThat(accessor.hasComponent(type1.id())).as("hasComponent must return true if assigned to entity").isTrue();
            assertThat(accessor.hasComponent(type2.id())).as("hasComponent must return true if assigned to entity").isTrue();
            assertThat(accessor.hasComponent(relation12.id())).as("hasComponent must return true if assigned to entity").isTrue();
            assertThat(accessor.hasComponent(exclusive12.id())).as("hasComponent must return true if assigned to entity").isTrue();
            assertThat(accessor.hasComponent(relation1.id())).as("hasComponent must return true if assigned to entity").isTrue();
            assertThat(accessor.hasComponent(exclusive1.id())).as("hasComponent must return true if assigned to entity").isTrue();
        }

        @Test
        void testHasPendingComponent() {
            var type1 = engine.getComponent(component(C1.class)).id();
            var type2 = engine.getComponent(component(C2.class)).id();
            var relation12 = engine.getComponent(relation(C1.class, C2.class)).id();
            var exclusive12 = engine.getComponent(exclusiveRelation(E1.class, C2.class)).id();
            var relation1 = engine.getComponent(relation(C1.class)).id();
            var exclusive1 = engine.getComponent(exclusiveRelation(E1.class)).id();

            var archetype = engine.getArchetype();
            archetype.createEntity(42, new Object[0]);

            engine.add(42, new Object[] {
                    new C1(), new C2(),
                    Relation.create(new C1(), new C2()), Relation.create(E1.INSTANCE, new C2()),
                    Relation.create(new C1(), 7), Relation.create(E1.INSTANCE, 7)
            });

            var accessor = engine.getAccessor(42);

            // Verify
            assertThat(accessor.hasComponent(type1)).as("hasComponent must return true if added to entity").isTrue();
            assertThat(accessor.hasComponent(type2)).as("hasComponent must return true if added to entity").isTrue();
            assertThat(accessor.hasComponent(relation12)).as("hasComponent must return true if added to entity").isTrue();
            assertThat(accessor.hasComponent(exclusive12)).as("hasComponent must return true if added to entity").isTrue();
            assertThat(accessor.hasComponent(relation1)).as("hasComponent must return true if added to entity").isTrue();
            assertThat(accessor.hasComponent(exclusive1)).as("hasComponent must return true if added to entity").isTrue();
        }

    }

    @Nested
    class GetByComponentId extends AbstractGetComponentTest {

        protected Object getComponent(int entityId, int componentId, DataAccessor accessor) {
            return accessor.getComponent(componentId);
        }

    }

    @Nested
    class GetByComponentIndex extends AbstractGetComponentTest {

        @Override
        protected Object getComponent(int entityId, int componentId, DataAccessor accessor) {
            var archetype = engine.getArchetypeForEntity(entityId);

            var componentIndex = archetype.getComponentIndex(componentId);
            assumeThat(componentIndex).as("can only get component by index if present in archetype").isGreaterThan(-1);

            return accessor.getComponentByIndex(componentIndex);
        }

    }

    @Nested
    class GetPendingComponent extends AbstractGetComponentTest {

        @Override
        protected Object getComponent(int entityId, int componentId, DataAccessor accessor) {
            var archetype = engine.getArchetypeForEntity(entityId);

            var componentIndex = archetype.getComponentIndex(componentId);
            assumeThat(componentIndex).as("can only get pending component if not present in archetype").isEqualTo(-1);

            return accessor.getPendingComponent(componentId);
        }

    }

    abstract class AbstractGetComponentTest {

        protected abstract Object getComponent(int entityId, int componentId, DataAccessor accessor);

        @Test
        void testGetComponent_NoComponentsAssigned() {
            var type1 = engine.getComponent(component(C1.class)).id();
            var type2 = engine.getComponent(component(C2.class)).id();
            var relation12 = engine.getComponent(relation(C1.class, C2.class)).id();
            var exclusive12 = engine.getComponent(exclusiveRelation(E1.class, C2.class)).id();
            var relation1 = engine.getComponent(relation(C1.class)).id();
            var exclusive1 = engine.getComponent(exclusiveRelation(E1.class)).id();

            var archetype = engine.getArchetype();
            archetype.createEntity(42, new Object[0]);

            var accessor = engine.getAccessor(42);

            // Verify
            assertThat(getComponent(42, type1, accessor)).as("getComponent must return null if not assigned to entity").isNull();
            assertThat(getComponent(42, type2, accessor)).as("getComponent must return null if not assigned to entity").isNull();
            assertThat(getComponent(42, relation12, accessor)).as("getComponent must return null if not assigned to entity").isNull();
            assertThat(getComponent(42, exclusive12, accessor)).as("getComponent must return null if not assigned to entity").isNull();
            assertThat(getComponent(42, relation1, accessor)).as("getComponent must return null if not assigned to entity").isNull();
            assertThat(getComponent(42, exclusive1, accessor)).as("getComponent must return null if not assigned to entity").isNull();
        }

        @Test
        void testGetComponent() {
            var type1 = engine.getComponent(component(C1.class));
            var type2 = engine.getComponent(component(C2.class));
            var relation12 = engine.getComponent(relation(C1.class, C2.class));
            var exclusive12 = engine.getComponent(exclusiveRelation(E1.class, C2.class));
            var relation1 = engine.getComponent(relation(C1.class));
            var exclusive1 = engine.getComponent(exclusiveRelation(E1.class));

            var c1 = new C1();
            var c2 = new C2();
            var componentRelation12 = Relation.create(new C1(), new C2());
            var exclusiveComponent12 = Relation.create(E1.INSTANCE, new C2());
            var entityRelation1 = Relation.create(new C1(), 7);
            var exclusiveEntity1 = Relation.create(E1.INSTANCE, 7);

            var archetype = engine.getArchetype(type1.type(), type2.type(), relation12.type(), exclusive12.type(), relation1.type(), exclusive1.type());
            archetype.createEntity(42, new Object[] {
                    c1, c2,
                    componentRelation12, exclusiveComponent12,
                    entityRelation1, exclusiveEntity1
            });

            var accessor = engine.getAccessor(42);

            // Verify
            assertThat(getComponent(42, type1.id(), accessor)).as("getComponent must return instance if assigned to entity").isSameAs(c1);
            assertThat(getComponent(42, type2.id(), accessor)).as("getComponent must return instance if assigned to entity").isSameAs(c2);
            assertThat(getComponent(42, relation12.id(), accessor))
                    .as("getComponent must return instance if assigned to entity").asInstanceOf(InstanceOfAssertFactories.ITERABLE)
                    .as("getComponent must return instance if assigned to entity").containsExactly(componentRelation12);

            assertThat(getComponent(42, exclusive12.id(), accessor)).as("getComponent must return instance if assigned to entity").isSameAs(exclusiveComponent12);
            assertThat(getComponent(42, relation1.id(), accessor))
                    .as("getComponent must return instance if assigned to entity").asInstanceOf(InstanceOfAssertFactories.ITERABLE)
                    .as("getComponent must return instance if assigned to entity").containsExactly(entityRelation1);

            assertThat(getComponent(42, exclusive1.id(), accessor)).as("getComponent must return instance if assigned to entity").isSameAs(exclusiveEntity1);
        }

        @Test
        void testGetPendingComponent() {
            var type1 = engine.getComponent(component(C1.class)).id();
            var type2 = engine.getComponent(component(C2.class)).id();
            var relation12 = engine.getComponent(relation(C1.class, C2.class)).id();
            var exclusive12 = engine.getComponent(exclusiveRelation(E1.class, C2.class)).id();
            var relation1 = engine.getComponent(relation(C1.class)).id();
            var exclusive1 = engine.getComponent(exclusiveRelation(E1.class)).id();

            var c1 = new C1();
            var c2 = new C2();
            var componentRelation12 = Relation.create(new C1(), new C2());
            var exclusiveComponent12 = Relation.create(E1.INSTANCE, new C2());
            var entityRelation1 = Relation.create(new C1(), 7);
            var exclusiveEntity1 = Relation.create(E1.INSTANCE, 7);

            var archetype = engine.getArchetype();
            archetype.createEntity(42, new Object[0]);

            engine.add(42, new Object[] {
                    c1, c2,
                    componentRelation12, exclusiveComponent12,
                    entityRelation1, exclusiveEntity1
            });

            var accessor = engine.getAccessor(42);

            // Verify
            assertThat(getComponent(42, type1, accessor)).as("getComponent must return instance if added to entity").isSameAs(c1);
            assertThat(getComponent(42, type2, accessor)).as("getComponent must return instance if added to entity").isSameAs(c2);
            assertThat(getComponent(42, relation12, accessor))
                    .as("getComponent must return instance if added to entity").asInstanceOf(InstanceOfAssertFactories.ITERABLE)
                    .as("getComponent must return instance if added to entity").containsExactly(componentRelation12);

            assertThat(getComponent(42, exclusive12, accessor)).as("getComponent must return instance if added to entity").isSameAs(exclusiveComponent12);
            assertThat(getComponent(42, relation1, accessor))
                    .as("getComponent must return instance if added to entity").asInstanceOf(InstanceOfAssertFactories.ITERABLE)
                    .as("getComponent must return instance if added to entity").containsExactly(entityRelation1);

            assertThat(getComponent(42, exclusive1, accessor)).as("getComponent must return instance if added to entity").isSameAs(exclusiveEntity1);
        }

        @Test
        void testNonExclusiveRelationComponents_MergesIfAlreadyExisting() {
            var relation12 = engine.getComponent(relation(C1.class, C2.class));
            var relation1 = engine.getComponent(relation(C1.class));

            var componentRelation1 = Relation.create(new C1(), new C2(42));
            var componentRelation2 = Relation.create(new C1(), new C2(9001));
            var entityRelation1 = Relation.create(new C1(), 10);
            var entityRelation2 = Relation.create(new C1(), 20);

            var archetype = engine.getArchetype(relation12.type(), relation1.type());
            archetype.createEntity(42, new Object[] {
                    componentRelation1, entityRelation1
            });

            engine.add(42, new Object[] {
                    componentRelation2, entityRelation2
            });

            var accessor = engine.getAccessor(42);

            // Verify
            assertThat(getComponent(42, relation12.id(), accessor))
                    .as("getComponent must return all relations if previously assigned to entity").asInstanceOf(InstanceOfAssertFactories.ITERABLE)
                    .as("getComponent must return all relations if previously assigned to entity").containsExactlyInAnyOrder(componentRelation1, componentRelation2);

            assertThat(getComponent(42, relation1.id(), accessor))
                    .as("getComponent must return all relations if previously assigned to entity").asInstanceOf(InstanceOfAssertFactories.ITERABLE)
                    .as("getComponent must return all relations if previously assigned to entity").containsExactlyInAnyOrder(entityRelation1, entityRelation2);
        }

    }

    record C1(int value) {
        C1() {
            this(0);
        }
    }

    record C2(int value) {
        C2() {
            this(0);
        }
    }

    enum E1 implements Exclusive {
        INSTANCE
    }

}
