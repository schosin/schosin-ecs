package de.schosin.ecs.storage.testsuite.components.relations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assumptions.assumeThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import de.schosin.ecs.api.components.Relation.EntityRelation;
import de.schosin.ecs.api.components.Relation.Exclusive;
import de.schosin.ecs.api.components.types.RelationComponentType.ExclusiveEntityRelationType;
import de.schosin.ecs.storage.testsuite.components.relations.ExclusiveEntityRelationDataTest.Relationship1;
import de.schosin.ecs.storage.testsuite.components.relations.ExclusiveEntityRelationDataTest.Relationship2;
import de.schosin.ecs.storage.testsuite.components.relations.ExclusiveEntityRelationDataTest.Relationship3;

public class ExclusiveEntityRelationDataTest
        extends CommonEntityRelationTest<Relationship1, EntityRelation<Relationship1>, Relationship2, EntityRelation<Relationship2>, Relationship3, EntityRelation<Relationship3>> {

    @Nested
    class RemoveTargetTest {

        @Test
        void testRemoveTarget_NoRelations() {
            var target = world.createEntity();
            var component = getComponent(type1());

            // Call
            assertThatCode(() -> component.removeTarget(target, affectedEntities)).as("removeTarget should do nothing if not used as target").doesNotThrowAnyException();

            // Verify
            assertThat(affectedEntities.getSize()).as("removeTarget should do nothing if not used as target").isZero();
        }

        @Test
        void testRemoveTarget_SingleRelation() {
            var target = world.createEntity();

            var component = getComponent(type1());
            var relation = getInstance1(1, target);

            var entityId = world.createEntity(relation);
            assertThat(component.getComponent(entityId))
                    .extracting("relationship", "target")
                    .as("sanity check").contains(relation.relationship(), target);

            // Call
            component.removeTarget(target, affectedEntities);

            // Verify
            assertThat(affectedEntities.getSize()).as("removeTarget should add entity to affectedEntities if all relations removed").isEqualTo(1);
            assertThat(affectedEntities.getData()).as("removeTarget should add entity to affectedEntities if all relations removed").startsWith(entityId);

            assertThat(component.getComponent(entityId)).as("removeTarget should not remove relation with target").isNotNull();
        }

        @Test
        void testRemoveTarget_MultipleRelations() {
            var target = world.createEntity();
            var target2 = world.createEntity();

            var component = getComponent(type1());
            var relation1 = getInstance1(1, target);
            var relation2 = getInstance1(2, target);
            var relation3 = getInstance1(3, target2);

            var entity1 = world.createEntity(relation1);
            var entity2 = world.createEntity(relation2);
            var entity3 = world.createEntity(relation3);

            // Call
            component.removeTarget(target, affectedEntities);

            // Verify
            assertThat(affectedEntities.getSize()).as("removeTarget should add entity to affectedEntities if all relations removed").isEqualTo(2);
            assertThat(affectedEntities.getData()).as("removeTarget should add entity to affectedEntities if all relations removed").contains(entity1, entity2).doesNotContain(entity3);

            assertThat(component.getComponent(entity1)).as("removeTarget should not remove relation with target").isNotNull();
            assertThat(component.getComponent(entity2)).as("removeTarget should not remove relation with target").isNotNull();
            assertThat(component.getComponent(entity3))
                    .extracting("relationship", "target")
                    .as("removeTarget should not remove relation with different target").contains(relation3.relationship(), target2);
        }

    }

    @Override
    protected ExclusiveEntityRelationType<Relationship1> type1() {
        return new ExclusiveEntityRelationType<>(Relationship1.class);
    }

    @Override
    protected ExclusiveEntityRelationType<Relationship2> type2() {
        return new ExclusiveEntityRelationType<>(Relationship2.class);
    }

    @Override
    protected ExclusiveEntityRelationType<Relationship3> type3() {
        return new ExclusiveEntityRelationType<>(Relationship3.class);
    }

    @Override
    protected EntityRelation<Relationship1> getInstance1(int relationship, int target) {
        assertThat(relationship).as("relationship").isPositive();
        assumeThat(relationship).as("relationship").isBetween(1, 3);

        assertThat(target).as("target").isPositive();
        assumeThat(target).as("target").isBetween(1, 3);

        return relation(
                switch (relationship) {
                    case 1 -> Relationship1.A;
                    case 2 -> Relationship1.B;
                    case 3 -> Relationship1.C;
                    default -> throw new IllegalArgumentException("Relationship value %d unsupported, should have been caught by assumeThat".formatted(relationship));
                },
                target);
    }

    @Override
    protected EntityRelation<Relationship2> getInstance2(int relationship, int target) {
        assertThat(relationship).as("relationship").isPositive();
        assumeThat(relationship).as("relationship").isBetween(1, 3);

        assertThat(target).as("target").isPositive();
        assumeThat(target).as("target").isBetween(1, 3);

        return relation(
                switch (relationship) {
                    case 1 -> Relationship2.FOO;
                    case 2 -> Relationship2.BAR;
                    case 3 -> Relationship2.BAZ;
                    default -> throw new IllegalArgumentException("Relationship value %d unsupported, should have been caught by assumeThat".formatted(relationship));
                },
                target);
    }

    @Override
    protected EntityRelation<Relationship3> getInstance3(int relationship, int target) {
        assertThat(relationship).as("relationship").isPositive();
        assertThat(target).as("target").isPositive();

        return relation(new Relationship3(relationship), target);
    }

    enum Relationship1 implements Exclusive {
        A, B, C
    }

    enum Relationship2 implements Exclusive {
        FOO, BAR, BAZ
    }

    static final class Relationship3 implements Exclusive {

        private final int value;

        public Relationship3(int value) {
            this.value = value;
        }

        @Override
        public int hashCode() {
            return value;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof Relationship3 other && this.value == other.value;
        }

    }

}
