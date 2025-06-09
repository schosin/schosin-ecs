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
        var componentMask = storageEngine.create(42, new Object[] {
                new C1(), new P1(),
                Relation.create(new C1(), new C2()), Relation.create(E1.INSTANCE, new C2()),
                Relation.create(new C1(), 2), Relation.create(E1.INSTANCE, 2)
        });

        assertThat(storageEngine.getComponentMaskForEntity(42)).as("getComponentMaskForEntity returns mask before delete").isSameAs(componentMask);

        for (var component : componentMask.getComponents()) {
            assertThat(component.hasComponent(42)).as("hasComponent returns true before delete").isTrue();
            assertThat(component.getComponent(42)).as("getComponent returns not null before delete").isNotNull();
        }

        // Call
        storageEngine.delete(42);

        // Verify
        assertThat(storageEngine.getComponentMaskForEntity(42)).as("getComponentMaskForEntity returns null after delete").isNull();

        for (var component : componentMask.getComponents()) {
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
        storageEngine.create(42, new Object[0]);

        storageEngine.add(42, new Object[] { new C1() });
        assertThat(storageEngine.getPendingComponentMask(42)).as("getPendingComponentMask must return value after add").isNotNull();

        storageEngine.delete(42);

        assertThatThrownBy(() -> storageEngine.getPendingComponentMask(42), "getPendingComponentMask throws for deleted entities")
                .as("getPendingComponentMask throws for deleted entities").isInstanceOf(StorageEngineException.class)
                .as("getPendingComponentMask throws for deleted entities").hasMessageContainingAll("entity 42", "not present in storage");

        // Create other entity with same id
        storageEngine.create(42, new Object[0]);

        assertThat(storageEngine.getPendingComponentMask(42)).as("getPendingComponentMask must be cleared after delete with changes").isNull();
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
