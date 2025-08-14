package de.schosin.ecs.storage.testsuite.entities;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import de.schosin.ecs.api.Pooled;
import de.schosin.ecs.api.components.Relation;
import de.schosin.ecs.api.components.Relation.Exclusive;
import de.schosin.ecs.storage.api.StorageEngineException;
import de.schosin.ecs.storage.testsuite.AbstractStorageEngineTest;

public class DeleteEntityTest extends AbstractStorageEngineTest {

    @Test
    void testUnknownEntity() {
        assertThatThrownBy(() -> storageEngine.markDeleted(42))
                .isInstanceOf(StorageEngineException.class)
                .hasMessageContainingAll("entity 42", "not present in storage");
    }

    @Test
    void testDelete() {
        var archetype = storageEngine.getArchetype(
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
        storageEngine.markDeleted(42);
        storageEngine.process();

        // Verify
        assertThat(storageEngine.getArchetypeForEntity(42)).as("getArchetypeForEntity returns null after delete").isNull();

        for (var component : archetype.getComponents()) {
            assertThat(component.hasComponent(42)).as("hasComponent returns false after delete").isFalse();
            assertThat(component.getComponent(42)).as("getComponent returns null after delete").isNull();
        }

        assertThat(archetype.getEntities().iterator()).toIterable().as("getEntities returns empty bag after delete").isEmpty();
    }

    @Test
    void testDelete_ComponentRelationInstanceFreed() {
        var relation = Relation.create(new C1(), new C2());
        var entityId = world.createEntity(relation);

        // Call
        storageEngine.markDeleted(entityId);
        storageEngine.process();

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
        storageEngine.markDeleted(entityId);
        storageEngine.process();

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
        storageEngine.markDeleted(entityId);
        storageEngine.process();

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
        storageEngine.markDeleted(entityId);
        storageEngine.process();

        // Verify
        assertThat(relation.type()).as("removed relation must be returned to Relation.free").isNull();
        assertThat(relation.relationship()).as("removed relation must be returned to Relation.free").isNull();
        assertThat(relation.target()).as("removed relation must be returned to Relation.free").isEqualTo(-1);

        assertThat(Relation.create(E2.INSTANCE, world.createEntity())).as("relation instance freed").isSameAs(relation);
    }

    @Test
    void testDelete_DiscardsPendingChanges() {
        // Create empty entity, add C1, delete
        var archetype = storageEngine.getArchetype();
        archetype.createEntity(42, new Object[0]);

        storageEngine.add(42, new Object[] { new C1() });
        assertThat(storageEngine.getPendingArchetype(42)).as("getPendingArchetype must return value after add").isNotNull();

        storageEngine.markDeleted(42);
        storageEngine.process();

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

        var archetype = storageEngine.getArchetype(component.type());
        archetype.createEntity(1, new Object[] { component1 });
        archetype.createEntity(2, new Object[] { component2 });

        // Access after creation
        assertThat(storageEngine.getAccessor(1).<C1>getComponent(component.id())).as("accessor returns instance before delete").isSameAs(component1);
        assertThat(storageEngine.getAccessor(2).<C1>getComponent(component.id())).as("accessor returns instance before delete").isSameAs(component2);

        // Delete first
        storageEngine.markDeleted(1);
        storageEngine.process();

        assertThatThrownBy(() -> storageEngine.getAccessor(1))
                .as("accessor not available after delete").isInstanceOf(StorageEngineException.class)
                .as("accessor not available after delete").hasMessageContainingAll("entity 1", "not present in storage");

        assertThat(storageEngine.getAccessor(2).<C1>getComponent(component.id())).as("accessor returns instance before delete").isSameAs(component2);

        // Delete second
        storageEngine.markDeleted(2);
        storageEngine.process();

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

        var archetype = storageEngine.getArchetype(component.type());
        archetype.createEntity(1, new Object[] { component1 });
        archetype.createEntity(2, new Object[] { component2 });

        // Access after creation
        assertThat(storageEngine.getAccessor(1).<C1>getComponent(component.id())).as("accessor returns instance before delete").isSameAs(component1);
        assertThat(storageEngine.getAccessor(2).<C1>getComponent(component.id())).as("accessor returns instance before delete").isSameAs(component2);

        // Delete first
        storageEngine.markDeleted(2);
        storageEngine.process();

        assertThat(storageEngine.getAccessor(1).<C1>getComponent(component.id())).as("accessor returns instance before delete").isSameAs(component1);

        assertThatThrownBy(() -> storageEngine.getAccessor(2))
                .as("accessor not available after delete").isInstanceOf(StorageEngineException.class)
                .as("accessor not available after delete").hasMessageContainingAll("entity 2", "not present in storage");

        // Delete second
        storageEngine.markDeleted(1);
        storageEngine.process();

        assertThatThrownBy(() -> storageEngine.getAccessor(1))
                .as("accessor not available after delete").isInstanceOf(StorageEngineException.class)
                .as("accessor not available after delete").hasMessageContainingAll("entity 1", "not present in storage");

        assertThatThrownBy(() -> storageEngine.getAccessor(2))
                .as("accessor not available after delete").isInstanceOf(StorageEngineException.class)
                .as("accessor not available after delete").hasMessageContainingAll("entity 2", "not present in storage");
    }

    @Test
    void testDeleteEntityInRemovedObserver() {
        var entity1 = world.createEntity();
        var entity2 = world.createEntity();

        onEntityDeleted((archetype, id) -> {
            if (id == entity1) {
                world.deleteEntity(entity2);
            }
        });

        // Call
        world.deleteEntity(entity1);
        world.process();

        // Verify
        assertThat(world.isActive(entity1)).as("isActive must return false after delete of entity").isFalse();
        assertThat(world.isActive(entity2)).as("isActive must return false after delete of entity in deleted observer").isFalse();
    }

    @Test
    void testCreateEntityInRemovedObserver() {
        var archetype = storageEngine.getArchetype(component(C1.class));

        var entity1 = world.createEntity(new C1());

        var entityId = new AtomicInteger(-1);
        onEntityDeleted((__, id) -> {
            if (id == entity1) {
                entityId.set(world.createEntity(new C1()));
            }
        });

        // Call
        world.deleteEntity(entity1);
        world.process();

        // Verify
        assertThat(world.isActive(entity1)).as("isActive must return false after delete of entity").isFalse();

        assertThat(world.isActive(entityId.get())).as("isActive must return true after create in deleted observer").isTrue();
        assertThat(storageEngine.getArchetypeForEntity(entityId.get())).as("getArchetypeForEntity must return archetype").isSameAs(archetype);
    }

    @Test
    void testDeleteAfterMultipleModifications() {
        var mapper1 = world.getComponents(C1.class);
        var mapper2 = world.getComponents(C2.class);

        var entityId = world.createEntity();

        // Call
        mapper1.add(entityId, new C1());
        mapper2.add(entityId, new C2());
        world.deleteEntity(entityId);

        world.process();

        // Verify
        assertThat(world.isActive(entityId)).as("isActive must return false after deletion processed").isFalse();
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
