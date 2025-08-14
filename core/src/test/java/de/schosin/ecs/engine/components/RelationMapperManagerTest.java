package de.schosin.ecs.engine.components;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.api.Test;

import de.schosin.ecs.api.components.Relation;
import de.schosin.ecs.engine.AbstractWorldTest;

class RelationMapperManagerTest extends AbstractWorldTest {

    @Test
    void testRemoveTarget_MapperCreated() {
        world.getComponents(relation(C1.class));

        var targetId = world.createEntity();
        var entityId = world.createEntity(Relation.create(C1.A, targetId));

        // Call
        world.deleteEntity(targetId);
        world.process();

        // Verify
        verifyDoesNotHaveComponents(entityId, relation(C1.class));
        verifyArchetypeDoesNotHaveComponents(entityId, relation(C1.class));
    }

    @Test
    void testRemoveTarget_MapperNotCreated() {
        var targetId = world.createEntity();
        var entityId = world.createEntity(Relation.create(C1.A, targetId));

        // Call
        world.deleteEntity(targetId);
        world.process();

        // Verify
        verifyDoesNotHaveComponents(entityId, relation(C1.class));
        verifyArchetypeDoesNotHaveComponents(entityId, relation(C1.class));
    }

    @Test
    void testRemoveTarget_MultipleTimes() {
        // Call 1
        var target1 = world.createEntity();
        var entity1 = world.createEntity(Relation.create(C1.A, target1));

        world.deleteEntity(target1);
        world.process();

        // Call 2
        var target2 = world.createEntity();
        var entity2 = world.createEntity(Relation.create(C2.A, target2));

        world.deleteEntity(target2);
        world.process();

        // Verify
        verifyDoesNotHaveComponents(entity1, relation(C1.class));
        verifyArchetypeDoesNotHaveComponents(entity1, relation(C1.class));

        verifyDoesNotHaveComponents(entity2, relation(C2.class));
        verifyArchetypeDoesNotHaveComponents(entity2, relation(C2.class));
    }

    @Test
    void testDeleteEntityAndRelated_EntityFirst() {
        var targetId = world.createEntity();
        var entityId = world.createEntity(Relation.create(C1.A, targetId));

        world.deleteEntity(entityId);
        world.deleteEntity(targetId);

        // Call
        assertThatCode(world::process).doesNotThrowAnyException();

        // Verify
        assertThat(entityManager.isActive(targetId)).isFalse();
        assertThat(entityManager.isActive(entityId)).isFalse();
    }

    @Test
    void testDeleteEntityAndRelated_TargetFirst() {
        var targetId = world.createEntity();
        var entityId = world.createEntity(Relation.create(C1.A, targetId));

        world.deleteEntity(targetId);
        world.deleteEntity(entityId);

        // Call
        assertThatCode(world::process).doesNotThrowAnyException();

        // Verify
        assertThat(entityManager.isActive(targetId)).isFalse();
        assertThat(entityManager.isActive(entityId)).isFalse();
    }

    @Test
    void testDeleteEntityAndMultipleRelated_EntityFirst() {
        var target1 = world.createEntity();
        var target2 = world.createEntity();
        var entityId = world.createEntity(Relation.create(C1.A, target1), Relation.create(C1.A, target2));

        world.deleteEntity(entityId);
        world.deleteEntity(target1);
        world.deleteEntity(target2);

        // Call
        world.process();
        assertThatCode(world::process).doesNotThrowAnyException();

        // Verify
        assertThat(entityManager.isActive(target1)).isFalse();
        assertThat(entityManager.isActive(target2)).isFalse();
        assertThat(entityManager.isActive(entityId)).isFalse();
    }

    @Test
    void testDeleteEntityAndMultipleRelated_EntityMiddle() {
        var target1 = world.createEntity();
        var target2 = world.createEntity();
        var entityId = world.createEntity(Relation.create(C1.A, target1), Relation.create(C1.A, target2));

        world.deleteEntity(target1);
        world.deleteEntity(entityId);
        world.deleteEntity(target2);

        // Call
        assertThatCode(world::process).doesNotThrowAnyException();

        // Verify
        assertThat(entityManager.isActive(target1)).isFalse();
        assertThat(entityManager.isActive(target2)).isFalse();
        assertThat(entityManager.isActive(entityId)).isFalse();
    }

    @Test
    void testDeleteEntityAndMultipleRelated_EntityLast() {
        var target1 = world.createEntity();
        var target2 = world.createEntity();
        var entityId = world.createEntity(Relation.create(C1.A, target1), Relation.create(C1.A, target2));

        world.deleteEntity(target1);
        world.deleteEntity(target2);
        world.deleteEntity(entityId);

        // Call
        assertThatCode(world::process).doesNotThrowAnyException();

        // Verify
        assertThat(entityManager.isActive(target1)).isFalse();
        assertThat(entityManager.isActive(target2)).isFalse();
        assertThat(entityManager.isActive(entityId)).isFalse();
    }

    enum C1 {
        A
    }

    enum C2 {
        A
    }

}
