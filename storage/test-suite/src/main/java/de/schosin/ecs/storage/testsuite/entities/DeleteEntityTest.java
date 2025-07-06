package de.schosin.ecs.storage.testsuite.entities;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import de.schosin.ecs.api.Pooled;
import de.schosin.ecs.api.components.Relation;
import de.schosin.ecs.api.components.Relation.Exclusive;
import de.schosin.ecs.storage.api.StorageEngineException;
import de.schosin.ecs.storage.testsuite.AbstractStorageEngineTest;

public class DeleteEntityTest extends AbstractStorageEngineTest {

    @Test
    void testUnknownEntity() {
        assertThatThrownBy(() -> storageEngine.delete(42))
                .isInstanceOf(StorageEngineException.class)
                .hasMessageContainingAll("entity 42", "not present in storage");
    }

    @Test
    void testDelete() {
        var archetype = engine.getArchetype(
                component(C1.class), component(P1.class),
                relation(C1.class, C2.class), exclusiveRelation(E1.class, C2.class),
                relation(C1.class), exclusiveRelation(E1.class));

        archetype.createEntity(42, new Object[] {
                new C1(), new P1(),
                Relation.create(new C1(), new C2()), Relation.create(E1.INSTANCE, new C2()),
                Relation.create(new C1(), 2), Relation.create(E1.INSTANCE, 2)
        });

        assertThat(storageEngine.getArchetypeForEntity(42)).as("getArchetypeEntity returns archetype before delete").isSameAs(archetype);

        for (var component : archetype.getComponents()) {
            assertThat(component.hasComponent(42)).as("hasComponent returns true before delete").isTrue();
            assertThat(component.getComponent(42)).as("getComponent returns not null before delete").isNotNull();
        }

        // Call
        storageEngine.delete(42);

        // Verify
        assertThat(storageEngine.getArchetypeForEntity(42)).as("getArchetypeForEntity returns null after delete").isNull();

        for (var component : archetype.getComponents()) {
            assertThat(component.hasComponent(42)).as("hasComponent returns false after delete").isFalse();
            assertThat(component.getComponent(42)).as("getComponent returns null after delete").isNull();
        }
    }

    @Test
    void testDelete_ComponentRelationInstanceFreed() {
        var relation = Relation.create(new C1(), new C2());
        var entityId = world.createEntity(relation);

        // Call
        storageEngine.delete(entityId);

        // Verify
        assertThat(relation.type()).as("removed relation must be returned to Relation.free").isNull();
        assertThat(relation.relationship()).as("removed relation must be returned to Relation.free").isNull();
        assertThat(relation.target()).as("removed relation must be returned to Relation.free").isNull();

        assertThat(Relation.create(new C2(), new C1())).as("relation instance freed").isSameAs(relation);
    }

    @Test
    void testDelete_ExclusiveComponentRelationInstanceFreed() {
        var relation = Relation.create(E1.INSTANCE, new C1());
        var entityId = world.createEntity(relation);

        // Call
        storageEngine.delete(entityId);

        // Verify
        assertThat(relation.type()).as("removed relation must be returned to Relation.free").isNull();
        assertThat(relation.relationship()).as("removed relation must be returned to Relation.free").isNull();
        assertThat(relation.target()).as("removed relation must be returned to Relation.free").isNull();

        assertThat(Relation.create(E2.INSTANCE, new C2())).as("relation instance freed").isSameAs(relation);
    }

    @Test
    void testDelete_EntityRelationInstanceFreed() {
        var target = world.createEntity();

        var relation = Relation.create(new C1(), target);
        var entityId = world.createEntity(relation);

        // Call
        storageEngine.delete(entityId);

        // Verify
        assertThat(relation.type()).as("removed relation must be returned to Relation.free").isNull();
        assertThat(relation.relationship()).as("removed relation must be returned to Relation.free").isNull();
        assertThat(relation.target()).as("removed relation must be returned to Relation.free").isEqualTo(-1);

        assertThat(Relation.create(new C2(), world.createEntity())).as("relation instance freed").isSameAs(relation);
    }

    @Test
    void testDelete_ExclusiveEntityRelationInstanceFreed() {
        var target = world.createEntity();

        var relation = Relation.create(E1.INSTANCE, target);
        var entityId = world.createEntity(relation);

        // Call
        storageEngine.delete(entityId);

        // Verify
        assertThat(relation.type()).as("removed relation must be returned to Relation.free").isNull();
        assertThat(relation.relationship()).as("removed relation must be returned to Relation.free").isNull();
        assertThat(relation.target()).as("removed relation must be returned to Relation.free").isEqualTo(-1);

        assertThat(Relation.create(E2.INSTANCE, world.createEntity())).as("relation instance freed").isSameAs(relation);
    }

    @Test
    void testDelete_DiscardsPendingChanges() {
        // Create empty entity, add C1, delete
        var archetype = engine.getArchetype();
        archetype.createEntity(42, new Object[0]);

        storageEngine.add(42, new Object[] { new C1() });
        assertThat(storageEngine.getPendingArchetype(42)).as("getPendingArchetype must return value after add").isNotNull();

        storageEngine.delete(42);

        assertThatThrownBy(() -> storageEngine.getPendingArchetype(42), "getPendingArchetype throws for deleted entities")
                .as("getPendingArchetype throws for deleted entities").isInstanceOf(StorageEngineException.class)
                .as("getPendingArchetype throws for deleted entities").hasMessageContainingAll("entity 42", "not present in storage");

        // Create other entity with same id
        archetype.createEntity(42, new Object[0]);

        assertThat(storageEngine.getPendingArchetype(42)).as("getPendingArchetype must be cleared after delete with changes").isNull();
    }

    @Test
    void testDeleteMultipleEntities_AccessComponentsBeforehand() {
        var component = storageEngine.getComponent(component(C1.class));

        var component1 = new C1();
        var component2 = new C1();

        var archetype = engine.getArchetype(component.type());
        archetype.createEntity(1, new Object[] { component1 });
        archetype.createEntity(2, new Object[] { component2 });

        // Access after creation
        assertThat(storageEngine.getAccessor(1).<C1>getComponent(component.id())).as("accessor returns instance before delete").isSameAs(component1);
        assertThat(storageEngine.getAccessor(2).<C1>getComponent(component.id())).as("accessor returns instance before delete").isSameAs(component2);

        // Delete first
        storageEngine.delete(1);

        assertThatThrownBy(() -> storageEngine.getAccessor(1))
                .as("accessor not available after delete").isInstanceOf(StorageEngineException.class)
                .as("accessor not available after delete").hasMessageContainingAll("entity 1", "not present in storage");

        assertThat(storageEngine.getAccessor(2).<C1>getComponent(component.id())).as("accessor returns instance before delete").isSameAs(component2);

        // Delete second
        storageEngine.delete(2);

        assertThatThrownBy(() -> storageEngine.getAccessor(1))
                .as("accessor not available after delete").isInstanceOf(StorageEngineException.class)
                .as("accessor not available after delete").hasMessageContainingAll("entity 1", "not present in storage");

        assertThatThrownBy(() -> storageEngine.getAccessor(2))
                .as("accessor not available after delete").isInstanceOf(StorageEngineException.class)
                .as("accessor not available after delete").hasMessageContainingAll("entity 2", "not present in storage");
    }

    @Test
    void testDeleteMultipleEntities_AccessComponentsBeforehand_ReverseDeleteOrder() {
        var component = storageEngine.getComponent(component(C1.class));

        var component1 = new C1();
        var component2 = new C1();

        var archetype = engine.getArchetype(component.type());
        archetype.createEntity(1, new Object[] { component1 });
        archetype.createEntity(2, new Object[] { component2 });

        // Access after creation
        assertThat(storageEngine.getAccessor(1).<C1>getComponent(component.id())).as("accessor returns instance before delete").isSameAs(component1);
        assertThat(storageEngine.getAccessor(2).<C1>getComponent(component.id())).as("accessor returns instance before delete").isSameAs(component2);

        // Delete first
        storageEngine.delete(2);

        assertThat(storageEngine.getAccessor(1).<C1>getComponent(component.id())).as("accessor returns instance before delete").isSameAs(component1);

        assertThatThrownBy(() -> storageEngine.getAccessor(2))
                .as("accessor not available after delete").isInstanceOf(StorageEngineException.class)
                .as("accessor not available after delete").hasMessageContainingAll("entity 2", "not present in storage");

        // Delete second
        storageEngine.delete(1);

        assertThatThrownBy(() -> storageEngine.getAccessor(1))
                .as("accessor not available after delete").isInstanceOf(StorageEngineException.class)
                .as("accessor not available after delete").hasMessageContainingAll("entity 1", "not present in storage");

        assertThatThrownBy(() -> storageEngine.getAccessor(2))
                .as("accessor not available after delete").isInstanceOf(StorageEngineException.class)
                .as("accessor not available after delete").hasMessageContainingAll("entity 2", "not present in storage");
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
