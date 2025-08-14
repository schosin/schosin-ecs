package de.schosin.ecs.storage.testsuite.components.relations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.assertj.core.api.Assumptions.assumeThat;

import java.util.List;
import java.util.function.BiConsumer;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.assertj.core.api.ObjectAssert;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import de.schosin.ecs.api.components.Relation;
import de.schosin.ecs.api.components.Relation.EntityRelation;
import de.schosin.ecs.api.components.Relations.EntityRelations;
import de.schosin.ecs.api.components.types.RelationComponentType.EntityRelationType;
import de.schosin.ecs.storage.api.components.Component.EntityRelationData;
import de.schosin.ecs.storage.testsuite.components.relations.EntityRelationDataTest.Relationship1;
import de.schosin.ecs.storage.testsuite.components.relations.EntityRelationDataTest.Relationship2;
import de.schosin.ecs.storage.testsuite.components.relations.EntityRelationDataTest.Relationship3;
import de.schosin.ecs.utils.collections.ImmutableBag;

public class EntityRelationDataTest
        extends CommonEntityRelationTest<Relationship1, EntityRelations<Relationship1>, Relationship2, EntityRelations<Relationship2>, Relationship3, EntityRelations<Relationship3>> {

    @Test
    void testAddWithExistionRelation() {
        var type = type1();

        var instance1 = Relation.create(Relationship1.A, 1);
        var instance2 = Relation.create(Relationship1.A, 2);
        assertThat(instance2).as("must be different instances (test suite broken if this fails)").isNotSameAs(instance1);

        var entityId = world.createEntity(instance1);
        assertThat(getComponent(entityId, type)).as("returns instance passed at creation")
                .as("returns result with relation").asInstanceOf(InstanceOfAssertFactories.iterable(EntityRelation.class))
                .as("returns result with relation").containsExactlyInAnyOrder(instance1);

        assertThat(storageEngine.getPendingArchetype(entityId)).as("getPendingArchetype returns null after creation").isNull();

        // Call
        storageEngine.add(entityId, ImmutableBag.of(type), new Object[] { instance2 });

        // Verify
        assertThat(getComponent(entityId, type))
                .as("returns result with both relations").asInstanceOf(InstanceOfAssertFactories.iterable(EntityRelation.class))
                .as("returns result with both relations").containsExactlyInAnyOrder(instance1, instance2);

        assertThat(storageEngine.getPendingArchetype(entityId)).as("getPendingArchetype returns null if add caused no archetype change").isNull();
    }

    @Test
    void testAddWithExistingDifferentRelation() {
        var type1 = type1();
        var type2 = type2();

        var instance1 = Relation.create(Relationship1.A, 1);
        var instance2 = Relation.create(Relationship2.FOO, 2);
        assertThat(instance2).as("must be different instances (test suite broken if this fails)").isNotSameAs(instance1);

        var entityId = world.createEntity(instance1);
        assertThat(getComponent(entityId, type1)).as("returns instance passed at creation")
                .as("returns result with relation").asInstanceOf(InstanceOfAssertFactories.iterable(EntityRelation.class))
                .as("returns result with relation").containsExactlyInAnyOrder(instance1);

        assertThat(storageEngine.getPendingArchetype(entityId)).as("getPendingArchetype returns null after creation").isNull();

        // Call
        storageEngine.add(entityId, ImmutableBag.of(type2), new Object[] { instance2 });
        storageEngine.process();

        // Verify
        assertThat(getComponent(entityId, type1)).as("returns instance passed at creation")
                .as("returns result with relation").asInstanceOf(InstanceOfAssertFactories.iterable(EntityRelation.class))
                .as("returns result with relation").containsExactlyInAnyOrder(instance1);

        assertThat(getComponent(entityId, type2)).as("returns added instance")
                .as("returns result with relation").asInstanceOf(InstanceOfAssertFactories.iterable(EntityRelation.class))
                .as("returns result with relation").containsExactlyInAnyOrder(instance2);

        assertThat(storageEngine.getPendingArchetype(entityId)).as("getPendingArchetype returns null if add caused no archetype change").isNull();
    }

    @Test
    void testAddMultipleWithExistionRelation() {
        var type = type1();

        var instance1 = Relation.create(Relationship1.A, 1);
        var instance2 = Relation.create(Relationship1.A, 2);
        var instance3 = Relation.create(Relationship1.B, 3);

        assertThat(instance2).as("must be different instances (test suite broken if this fails)").isNotSameAs(instance1);

        var entityId = world.createEntity(instance1);
        assertThat(getComponent(entityId, type)).as("returns instance passed at creation")
                .as("returns result with relation").asInstanceOf(InstanceOfAssertFactories.iterable(EntityRelation.class))
                .as("returns result with relation").containsExactlyInAnyOrder(instance1);

        assertThat(storageEngine.getPendingArchetype(entityId)).as("getPendingArchetype returns null after creation").isNull();

        // Call
        storageEngine.add(entityId, ImmutableBag.of(type, type), new Object[] { instance2, instance3 });

        // Verify
        assertThat(getComponent(entityId, type))
                .as("returns result with all relations").asInstanceOf(InstanceOfAssertFactories.iterable(EntityRelation.class))
                .as("returns result with all relations").containsExactlyInAnyOrder(instance1, instance2, instance3);

        assertThat(storageEngine.getPendingArchetype(entityId)).as("getPendingArchetype returns null if add caused no archetype change").isNull();
    }

    @Test
    void testAddMultipleWithExistionRelation_EqualTargetReplaced() {
        var type = type1();

        var instance1 = Relation.create(Relationship1.A, 1);
        var instance2 = Relation.create(Relationship1.A, 2);
        var instance3 = Relation.create(Relationship1.B, 1);

        assertThat(instance2).as("must be different instances (test suite broken if this fails)").isNotSameAs(instance1);

        var entityId = world.createEntity(instance1);
        assertThat(getComponent(entityId, type)).as("returns instance passed at creation")
                .as("returns result with relation").asInstanceOf(InstanceOfAssertFactories.iterable(EntityRelation.class))
                .as("returns result with relation").containsExactlyInAnyOrder(instance1);

        assertThat(storageEngine.getPendingArchetype(entityId)).as("getPendingArchetype returns null after creation").isNull();

        // Call
        storageEngine.add(entityId, ImmutableBag.of(type, type), new Object[] { instance2, instance3 });

        // Verify
        assertThat(getComponent(entityId, type))
                .as("returns result with added relations only").asInstanceOf(InstanceOfAssertFactories.iterable(EntityRelation.class))
                .as("returns result with added relations only").containsExactlyInAnyOrder(instance2, instance3);

        assertThat(storageEngine.getPendingArchetype(entityId)).as("getPendingArchetype returns null if add caused no archetype change").isNull();
    }

    @Test
    void testAddThenRemove_FreesRelationsOnlyOnce() {
        var type = type1();

        var instance1 = Relation.create(Relationship1.A, 1);
        var instance2 = Relation.create(Relationship1.A, 2);
        assertThat(instance2).as("must be different instances (test suite broken if this fails)").isNotSameAs(instance1);

        var entityId = world.createEntity();

        // Add relations
        storageEngine.add(entityId, ImmutableBag.of(type, type), new Object[] { instance1, instance2 });

        assertThat(instance1.target()).as("relation not reset after add").isEqualTo(1);
        assertThat(instance2.target()).as("relation not reset after add").isEqualTo(2);

        // Flush add
        storageEngine.process();

        assertThat(instance1.target()).as("relation not reset after flush of add").isEqualTo(1);
        assertThat(instance2.target()).as("relation not reset after flush of add").isEqualTo(2);

        // Remove relations
        storageEngine.remove(entityId, ImmutableBag.of(type));

        assertThat(instance1.target()).as("relation not reset before flush of removal").isEqualTo(1);
        assertThat(instance2.target()).as("relation not reset before flush of removal").isEqualTo(2);

        // Flush removal
        storageEngine.process();

        assertThat(instance1.target()).as("relation reset after removal flushed").isEqualTo(-1);
        assertThat(instance2.target()).as("relation reset after removal flushed").isEqualTo(-1);

        // Verify returned to pool once
        var relations = List.of(Relation.create(Relationship2.FOO, 3), Relation.create(Relationship2.BAR, 3), Relation.create(Relationship2.BAZ, 4));
        assertThat(relations).anySatisfy(relation -> assertThat(relation).as("relation returned to pool").isSameAs(instance1));
        assertThat(relations).anySatisfy(relation -> assertThat(relation).as("relation returned to pool").isSameAs(instance2));
        assertThat(relations).anySatisfy(relation -> assertThat(relation).as("each relation returned to pool only once").isNotSameAs(instance1).isNotSameAs(instance2));
    }

    @Nested
    class AddRelationTest {

        @Test
        void testAddRelation() {
            var component = getComponent(type3());

            var relation1 = getInstance3(1, 1);
            var relation2 = getInstance3(1, 2);
            var relation3 = getInstance3(1, 3);

            var entityId = world.createEntity(relation1);

            // Call
            storageEngine.add(entityId, new Object[] { relation2, relation3 });

            // Verify
            var relations = component.getComponent(entityId);
            assertThat(relations)
                    .extracting("relationship", "target")
                    .as("relations added if target different from existing")
                    .containsExactlyInAnyOrder(
                            tuple(relation1.relationship(), relation1.target()),
                            tuple(relation2.relationship(), relation2.target()),
                            tuple(relation3.relationship(), relation3.target()));
        }

        @Test
        void testAddRelation_ReplacesExistingIfSameTarget() {
            var component = getComponent(type3());

            var relation1 = getInstance3(1, 1);
            var relation2 = getInstance3(2, 1);
            var relation3 = getInstance3(1, 3);

            var entityId = world.createEntity(relation1);

            // Call
            storageEngine.add(entityId, new Object[] { relation2, relation3 });

            // Verify
            var relations = component.getComponent(entityId);
            assertThat(relations)
                    .extracting("relationship", "target")
                    .as("relations added if target different from existing")
                    .containsExactlyInAnyOrder(
                            tuple(relation2.relationship(), relation2.target()),
                            tuple(relation3.relationship(), relation3.target()));
        }

    }

    @Nested
    class RelationsReuseTest {

        @Test
        void testCreateEntity_UsedRelationsInstance_ThrowsIllegalArgumentException() {
            var type = type1();
            var component = getComponent(type);

            var instance1 = Relation.create(Relationship1.A, 1);
            var instance2 = Relation.create(Relationship1.A, 2);

            var entity1 = world.createEntity(instance1, instance2);

            var relations = component.getComponent(entity1);
            assertThat(relations).as("returns relations at creation").containsExactlyInAnyOrder(instance1, instance2);

            // Call
            assertThatThrownBy(() -> world.createEntity(relations))
                    .as("must throw an exception if relations obtained from entity added to another").isInstanceOf(IllegalArgumentException.class)
                    .as("must throw an exception if relations obtained from entity added to another").hasMessageContainingAll("Cannot add relations", "Relations.copyOf");
        }

    }

    @Nested
    class ReplaceExistingRelationTest extends AbstractTypeTest {

        @ParameterizedTest
        @MethodSource(TYPES)
        void testAddSameTarget_ReplacesExistingRelation(EntityRelationType<?> type) {
            var component = getComponent(type);

            var instance = getInstance(type, 1, 1);
            var entityId = world.createEntity(instance);

            // Call
            var newRelation = getInstance(type, 2, 1);
            assertThat(newRelation.relationship()).as("sanity check").isNotEqualTo(instance.relationship());
            assumeThat(newRelation.target()).isEqualTo(instance.target()).isNotSameAs(instance.target());

            storageEngine.add(entityId, new Object[] { newRelation });

            // Verify
            var relations = component.getComponent(entityId);
            assertThat(relations).as("no relation added if target same as im existing relation").hasSize(1);

            var relation = relations.get(0);
            assertThat(relation.relationship()).as("relationship must be same as passed relationship").isSameAs(newRelation.relationship());
            assertThat(relation.target()).as("target must be same as passed target").isSameAs(newRelation.target());
        }

        @ParameterizedTest
        @MethodSource(TYPES)
        void testAddEqualTarget_ReplacesExistingRelation(EntityRelationType<?> type) {
            var component = getComponent(type);

            var instance = getInstance(type, 1, 1);
            var entityId = world.createEntity(instance);

            // Call
            var newRelation = getInstance(type, 2, 1);
            assertThat(newRelation.relationship()).as("sanity check").isNotEqualTo(instance.relationship());
            assertThat(newRelation.target()).as("sanity check").isEqualTo(instance.target());

            storageEngine.add(entityId, new Object[] { newRelation });

            // Verify
            var relations = component.getComponent(entityId);
            assertThat(relations).as("no relation added if target equal to existing relation").hasSize(1);

            var relation = relations.get(0);
            assertThat(relation.relationship()).as("relationship must be same as passed relationship").isSameAs(newRelation.relationship());
            assertThat(relation.target()).as("target must be same as passed target").isSameAs(newRelation.target());
        }

        @ParameterizedTest
        @MethodSource(TYPES)
        void testAddEqualBoth_ReplacesExistingRelation(EntityRelationType<?> type) {
            var component = getComponent(type);

            var instance = getInstance(type, 1, 1);
            var entityId = world.createEntity(instance);

            // Call
            var newRelation = getInstance(type, 1, 1);
            assumeThat(newRelation.relationship()).isEqualTo(instance.relationship()).isNotSameAs(instance.relationship());
            assumeThat(newRelation.target()).isEqualTo(instance.target()).isNotSameAs(instance.target());

            storageEngine.add(entityId, new Object[] { newRelation });

            // Verify
            var relations = component.getComponent(entityId);
            assertThat(relations).as("no relation added if both same as im existing relation").hasSize(1);

            var relation = relations.get(0);
            assertThat(relation.relationship()).as("relationship must be same as passed relationship").isSameAs(newRelation.relationship());
            assertThat(relation.target()).as("target must be same as passed target").isSameAs(newRelation.target());
        }

        @ParameterizedTest
        @MethodSource(TYPES)
        void testSameEqualBoth_ReplacesExistingRelation(EntityRelationType<?> type) {
            var component = getComponent(type);

            var instance = getInstance(type, 1, 1);
            var entityId = world.createEntity(instance);

            // Call
            var newRelation = getInstance(type, 1, 1);
            assertThat(newRelation.relationship()).as("sanity check").isEqualTo(instance.relationship());
            assertThat(newRelation.target()).as("sanity check").isEqualTo(instance.target());

            storageEngine.add(entityId, new Object[] { newRelation });

            // Verify
            var relations = component.getComponent(entityId);
            assertThat(relations).as("no relation added if both equal to existing relation").hasSize(1);

            var relation = relations.get(0);
            assertThat(relation.relationship()).as("relationship must be same as passed relationship").isSameAs(newRelation.relationship());
            assertThat(relation.target()).as("target must be same as passed target").isSameAs(newRelation.target());
        }

    }

    @Nested
    class RemoveTargetTest {

        @Test
        void testRemoveTarget_NoRelations() {
            var target = world.createEntity();
            var component = getComponent(type1());

            // Call
            var handler = new Handler();
            assertThatCode(() -> component.removeTarget(target, handler)).as("removeTarget should do nothing if not used as target").doesNotThrowAnyException();

            // Verify
            assertThat(handler.data).as("removeTarget should do nothing if not used as target").isEmpty();
        }

        @Test
        void testRemoveTarget_SingleRelation() {
            var target = world.createEntity();

            var component = getComponent(type1());
            var relation = getInstance1(1, target);

            var entityId = world.createEntity(relation);
            assertThat(component.getComponent(entityId))
                    .extracting("relationship", "target")
                    .as("sanity check").containsExactlyInAnyOrder(tuple(relation.relationship(), target));

            // Call
            var handler = new Handler();
            component.removeTarget(target, handler);

            // Verify
            assertThat(handler.data).as("removeTarget should add entity to affectedEntities if all relations removed").containsExactly(new Handler.Data(entityId, type1()));

            assertThat(component.getComponent(entityId))
                    .as("removeTarget should not remove component").isNotNull()
                    .as("removeTarget should remove relation with target").isEmpty();
        }

        @Test
        void testRemoveTarget_MultipleRelations_DifferentEntities() {
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
            var handler = new Handler();
            component.removeTarget(target, handler);

            // Verify
            assertThat(handler.data).as("removeTarget should add entity to affectedEntities if all relations removed")
                    .containsExactlyInAnyOrder(new Handler.Data(entity1, type1()), new Handler.Data(entity2, type1()));

            assertThat(component.getComponent(entity1))
                    .as("removeTarget should not remove component").isNotNull()
                    .as("removeTarget should remove relation with target").isEmpty();

            assertThat(component.getComponent(entity2))
                    .as("removeTarget should not remove component").isNotNull()
                    .as("removeTarget should remove relation with target").isEmpty();

            assertThat(component.getComponent(entity3))
                    .extracting("relationship", "target")
                    .as("removeTarget should not remove relation with different target")
                    .containsExactlyInAnyOrder(tuple(relation3.relationship(), target2));
        }

        @Test
        void testRemoveTarget_MultipleRelations_SameEntity() {
            var target = world.createEntity();
            var target2 = world.createEntity();

            var component = getComponent(type1());
            var relation1 = getInstance1(1, target);
            var relation2 = getInstance1(2, target);
            var relation3 = getInstance1(3, target2);

            var entity1 = world.createEntity(relation1);
            var entity2 = world.createEntity(relation2, relation3);

            // Call
            var handler = new Handler();
            component.removeTarget(target, handler);

            // Verify
            assertThat(handler.data).as("removeTarget should add entity to affectedEntities if all relations removed").containsExactly(new Handler.Data(entity1, type1()));

            assertThat(component.getComponent(entity1))
                    .as("removeTarget should not remove component").isNotNull()
                    .as("removeTarget should remove relation with target").isEmpty();

            assertThat(component.getComponent(entity2))
                    .extracting("relationship", "target")
                    .as("removeTarget should only remove relation with target")
                    .containsExactlyInAnyOrder(tuple(relation3.relationship(), target2));
        }

    }

    @Nested
    class GetRelationshipTest {

        @Nested
        class CreateEntityTest extends AbstractTest {

            @Override
            @SuppressWarnings("unchecked")
            protected int getEntity(EntityRelationData<Relationship3> component, EntityRelation<Relationship3>... relations) {
                return world.createEntity((Object[]) relations);
            }

        }

        @Nested
        class AddRelationsTest extends AbstractTest {

            @Override
            @SuppressWarnings("unchecked")
            protected int getEntity(EntityRelationData<Relationship3> component, EntityRelation<Relationship3>... relations) {
                var entityId = world.createEntity();

                storageEngine.add(entityId, relations);
                storageEngine.process();

                return entityId;
            }

        }

        @SuppressWarnings("unchecked")
        abstract class AbstractTest {

            protected abstract int getEntity(EntityRelationData<Relationship3> component, EntityRelation<Relationship3>... relations);

            @Test
            void testGetRelationship() {
                var component = getComponent(type3());

                var instance1 = getInstance3(4, 1);
                var instance2 = getInstance3(5, 2);
                var instance3 = getInstance3(6, 3);

                var target1 = instance1.target();
                var target2 = instance2.target();
                var target3 = instance3.target();

                // Call
                var entityId = getEntity(component, instance1, instance2, instance3);

                var relations = component.getComponent(entityId);

                // Verify
                assertThat(relations.getRelationship(target1)).as("returns same relationship for equal target").isSameAs(instance1.relationship());
                assertThat(relations.getRelationship(target2)).as("returns same relationship for equal target").isSameAs(instance2.relationship());
                assertThat(relations.getRelationship(target3)).as("returns same relationship for equal target").isSameAs(instance3.relationship());
            }

        }

    }

    private <R> EntityRelationData<R> getComponent(EntityRelationType<R> type) {
        return storageEngine.getComponent(type);
    }

    @Override
    protected EntityRelationType<Relationship1> type1() {
        return new EntityRelationType<>(Relationship1.class);
    }

    @Override
    protected EntityRelationType<Relationship2> type2() {
        return new EntityRelationType<>(Relationship2.class);
    }

    @Override
    protected EntityRelationType<Relationship3> type3() {
        return new EntityRelationType<>(Relationship3.class);
    }

    @Override
    protected EntityRelation<Relationship1> getInstance1(int relationship, int target) {
        assumeThat(relationship).as("relationship").isBetween(1, 3);

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

    @Override
    protected BiConsumer<ObjectAssert<?>, Object> verifyComponentInstance() {
        return (objectAssert, instance) -> objectAssert.asInstanceOf(InstanceOfAssertFactories.ITERABLE).contains(instance);
    }

    @Override
    protected BiConsumer<ObjectAssert<?>, EntityRelation<?>> verifyRelationInstance() {
        return (objectAssert, instance) -> objectAssert.asInstanceOf(InstanceOfAssertFactories.ITERABLE).contains(instance);
    }

    enum Relationship1 {
        A, B, C
    }

    enum Relationship2 {
        FOO, BAR, BAZ
    }

    static final class Relationship3 {

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
