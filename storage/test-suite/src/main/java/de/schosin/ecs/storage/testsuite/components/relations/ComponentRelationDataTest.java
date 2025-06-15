package de.schosin.ecs.storage.testsuite.components.relations;

import static org.assertj.core.api.Assertions.assertThat;
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
import de.schosin.ecs.api.components.Relation.ComponentRelation;
import de.schosin.ecs.api.components.Relations.ComponentRelations;
import de.schosin.ecs.api.components.types.RelationComponentType.ComponentRelationType;
import de.schosin.ecs.storage.api.components.Component.ComponentRelationData;
import de.schosin.ecs.storage.testsuite.components.relations.ComponentRelationDataTest.Relationship1;
import de.schosin.ecs.storage.testsuite.components.relations.ComponentRelationDataTest.Relationship2;
import de.schosin.ecs.storage.testsuite.components.relations.ComponentRelationDataTest.Relationship3;
import de.schosin.ecs.storage.testsuite.components.relations.ComponentRelationDataTest.Target1;
import de.schosin.ecs.storage.testsuite.components.relations.ComponentRelationDataTest.Target2;
import de.schosin.ecs.storage.testsuite.components.relations.ComponentRelationDataTest.Target3;
import de.schosin.ecs.utils.collections.ImmutableBag;

public class ComponentRelationDataTest extends
        CommonComponentRelationTest<Relationship1, Target1, ComponentRelations<Relationship1, Target1>, Relationship2, Target2, ComponentRelations<Relationship2, Target2>, Relationship3, Target3, ComponentRelations<Relationship3, Target3>> {

    @Test
    void testAddWithExistionRelation() {
        var type = type1();

        var instance1 = Relation.create(Relationship1.A, Target1.FIRST);
        var instance2 = Relation.create(Relationship1.A, Target1.SECOND);

        assertThat(instance2).as("must be different instances (test suite broken if this fails)").isNotSameAs(instance1);

        var entityId = world.createEntity(instance1);
        assertThat(getComponent(entityId, type)).as("returns instance passed at creation")
                .as("returns result with relation").asInstanceOf(InstanceOfAssertFactories.iterable(ComponentRelation.class))
                .as("returns result with relation").containsExactlyInAnyOrder(instance1);

        assertThat(storageEngine.getPendingComponentMask(entityId)).as("getPendingComponentMask returns null after creation").isNull();

        // Call
        storageEngine.add(entityId, ImmutableBag.of(type), new Object[] { instance2 });

        // Verify
        assertThat(getComponent(entityId, type))
                .as("returns result with both relations").asInstanceOf(InstanceOfAssertFactories.iterable(ComponentRelation.class))
                .as("returns result with both relations").containsExactlyInAnyOrder(instance1, instance2);

        assertThat(storageEngine.getPendingComponentMask(entityId)).as("getPendingComponentMask returns null if add caused no component mask change").isNull();
    }

    @Test
    void testAddMultipleWithExistionRelation() {
        var type = type1();

        var instance1 = Relation.create(Relationship1.A, Target1.FIRST);
        var instance2 = Relation.create(Relationship1.A, Target1.SECOND);
        var instance3 = Relation.create(Relationship1.B, Target1.THIRD);

        assertThat(instance2).as("must be different instances (test suite broken if this fails)").isNotSameAs(instance1);

        var entityId = world.createEntity(instance1);
        assertThat(getComponent(entityId, type)).as("returns instance passed at creation")
                .as("returns result with relation").asInstanceOf(InstanceOfAssertFactories.iterable(ComponentRelation.class))
                .as("returns result with relation").containsExactlyInAnyOrder(instance1);

        assertThat(storageEngine.getPendingComponentMask(entityId)).as("getPendingComponentMask returns null after creation").isNull();

        // Call
        storageEngine.add(entityId, ImmutableBag.of(type, type), new Object[] { instance2, instance3 });

        // Verify
        assertThat(getComponent(entityId, type))
                .as("returns result with all relations").asInstanceOf(InstanceOfAssertFactories.iterable(ComponentRelation.class))
                .as("returns result with all relations").containsExactlyInAnyOrder(instance1, instance2, instance3);

        assertThat(storageEngine.getPendingComponentMask(entityId)).as("getPendingComponentMask returns null if add caused no component mask change").isNull();
    }

    @Test
    void testAddMultipleWithExistionRelation_EqualTargetReplaced() {
        var type = type1();

        var instance1 = Relation.create(Relationship1.A, Target1.FIRST);
        var instance2 = Relation.create(Relationship1.A, Target1.SECOND);
        var instance3 = Relation.create(Relationship1.B, Target1.FIRST);

        assertThat(instance2).as("must be different instances (test suite broken if this fails)").isNotSameAs(instance1);

        var entityId = world.createEntity(instance1);
        assertThat(getComponent(entityId, type)).as("returns instance passed at creation")
                .as("returns result with relation").asInstanceOf(InstanceOfAssertFactories.iterable(ComponentRelation.class))
                .as("returns result with relation").containsExactlyInAnyOrder(instance1);

        assertThat(storageEngine.getPendingComponentMask(entityId)).as("getPendingComponentMask returns null after creation").isNull();

        // Call
        storageEngine.add(entityId, ImmutableBag.of(type, type), new Object[] { instance2, instance3 });

        // Verify
        assertThat(getComponent(entityId, type))
                .as("returns result with added relations only").asInstanceOf(InstanceOfAssertFactories.iterable(ComponentRelation.class))
                .as("returns result with added relations only").containsExactlyInAnyOrder(instance2, instance3);

        assertThat(storageEngine.getPendingComponentMask(entityId)).as("getPendingComponentMask returns null if add caused no component mask change").isNull();
    }

    @Test
    void testAddThenRemove_FreesRelationsOnlyOnce() {
        var type = type1();

        var instance1 = Relation.create(Relationship1.A, Target1.FIRST);
        var instance2 = Relation.create(Relationship1.A, Target1.SECOND);
        assertThat(instance2).as("must be different instances (test suite broken if this fails)").isNotSameAs(instance1);

        var entityId = world.createEntity();

        // Add relations
        storageEngine.add(entityId, ImmutableBag.of(type, type), new Object[] { instance1, instance2 });

        assertThat(instance1.target()).as("relation not reset after add").isEqualTo(Target1.FIRST);
        assertThat(instance2.target()).as("relation not reset after add").isEqualTo(Target1.SECOND);

        // Flush add
        storageEngine.flushChanges(entityId);

        assertThat(instance1.target()).as("relation not reset after flush of add").isEqualTo(Target1.FIRST);
        assertThat(instance2.target()).as("relation not reset after flush of add").isEqualTo(Target1.SECOND);

        // Remove relations
        storageEngine.remove(entityId, ImmutableBag.of(type));

        assertThat(instance1.target()).as("relation not reset before flush of removal").isEqualTo(Target1.FIRST);
        assertThat(instance2.target()).as("relation not reset before flush of removal").isEqualTo(Target1.SECOND);

        // Flush removal
        storageEngine.flushChanges(entityId);

        assertThat(instance1.target()).as("relation reset after removal flushed").isNull();
        assertThat(instance2.target()).as("relation reset after removal flushed").isNull();

        // Verify returned to pool once
        var relations = List.of(Relation.create(Relationship2.FOO, Target1.FIRST), Relation.create(Relationship2.BAR, Target1.SECOND), Relation.create(Relationship2.BAZ, Target1.THIRD));
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
                    .extracting(ComponentRelation::relationship, ComponentRelation::target)
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
                    .extracting(ComponentRelation::relationship, ComponentRelation::target)
                    .as("relations added if target different from existing")
                    .containsExactlyInAnyOrder(
                            tuple(relation2.relationship(), relation2.target()),
                            tuple(relation3.relationship(), relation3.target()));
        }

    }

    @Nested
    class ReplaceExistingRelationTest extends AbstractTypeTest {

        @ParameterizedTest
        @MethodSource(TYPES)
        void testAddSameTarget_ReplacesExistingRelation(ComponentRelationType<?, ?> type) {
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
        void testAddEqualTarget_ReplacesExistingRelation(ComponentRelationType<?, ?> type) {
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
        void testAddEqualBoth_ReplacesExistingRelation(ComponentRelationType<?, ?> type) {
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
        void testSameEqualBoth_ReplacesExistingRelation(ComponentRelationType<?, ?> type) {
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
    class GetRelationshipTest {

        @Nested
        class CreateEntityTest extends AbstractTest {

            @Override
            @SuppressWarnings("unchecked")
            protected int getEntity(ComponentRelationData<Relationship3, Target3> component, ComponentRelation<Relationship3, Target3>... relations) {
                return world.createEntity((Object[]) relations);
            }

        }

        @Nested
        class AddRelationsTest extends AbstractTest {

            @Override
            @SuppressWarnings("unchecked")
            protected int getEntity(ComponentRelationData<Relationship3, Target3> component, ComponentRelation<Relationship3, Target3>... relations) {
                var entityId = world.createEntity();

                storageEngine.add(entityId, relations);

                return entityId;
            }

        }

        @SuppressWarnings("unchecked")
        abstract class AbstractTest {

            protected abstract int getEntity(ComponentRelationData<Relationship3, Target3> component, ComponentRelation<Relationship3, Target3>... relations);

            @Test
            void testGetRelationship_SameTarget() {
                var component = getComponent(type3());

                var instance1 = getInstance3(4, 1);
                var instance2 = getInstance3(5, 2);
                var instance3 = getInstance3(6, 3);

                var entityId = getEntity(component, instance1, instance2, instance3);

                // Call
                var relations = component.getComponent(entityId);

                // Verify
                assertThat(relations.getRelationship(instance1.target())).as("returns same relationship for same target").isSameAs(instance1.relationship());
                assertThat(relations.getRelationship(instance2.target())).as("returns same relationship for same target").isSameAs(instance2.relationship());
                assertThat(relations.getRelationship(instance3.target())).as("returns same relationship for same target").isSameAs(instance3.relationship());
            }

            @Test
            void testGetRelationship_EqualTarget() {
                var component = getComponent(type3());

                var instance1 = getInstance3(4, 1);
                var instance2 = getInstance3(5, 2);
                var instance3 = getInstance3(6, 3);

                var target1 = getInstance3(4, 1).target();
                assertThat(target1).as("sanity check").isEqualTo(instance1.target()).as("sanityCheck").isNotSameAs(instance1.target());

                var target2 = getInstance3(5, 2).target();
                assertThat(target2).as("sanity check").isEqualTo(instance2.target()).as("sanityCheck").isNotSameAs(instance2.target());

                var target3 = getInstance3(6, 3).target();
                assertThat(target3).as("sanity check").isEqualTo(instance3.target()).as("sanityCheck").isNotSameAs(instance3.target());

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

    private <R, T> ComponentRelationData<R, T> getComponent(ComponentRelationType<R, T> type) {
        return engine.getComponent(type);
    }

    @Override
    protected ComponentRelationType<Relationship1, Target1> type1() {
        return new ComponentRelationType<>(Relationship1.class, Target1.class);
    }

    @Override
    protected ComponentRelationType<Relationship2, Target2> type2() {
        return new ComponentRelationType<>(Relationship2.class, Target2.class);
    }

    @Override
    protected ComponentRelationType<Relationship3, Target3> type3() {
        return new ComponentRelationType<>(Relationship3.class, Target3.class);
    }

    @Override
    protected ComponentRelation<Relationship1, Target1> getInstance1(int relationship, int target) {
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
                switch (target) {
                    case 1 -> Target1.FIRST;
                    case 2 -> Target1.SECOND;
                    case 3 -> Target1.THIRD;
                    default -> throw new IllegalArgumentException("target value %d unsupported, should have been caught by assumeThat".formatted(target));
                });
    }

    @Override
    protected ComponentRelation<Relationship2, Target2> getInstance2(int relationship, int target) {
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
                switch (target) {
                    case 1 -> Target2.ONE;
                    case 2 -> Target2.TWO;
                    case 3 -> Target2.THREE;
                    default -> throw new IllegalArgumentException("target value %d unsupported, should have been caught by assumeThat".formatted(target));
                });
    }

    @Override
    protected ComponentRelation<Relationship3, Target3> getInstance3(int relationship, int target) {
        assertThat(relationship).as("relationship").isPositive();
        assertThat(target).as("target").isPositive();

        return relation(new Relationship3(relationship), new Target3(target));
    }

    @Override
    protected BiConsumer<ObjectAssert<?>, Object> verifyComponentInstance() {
        return (objectAssert, instance) -> objectAssert.asInstanceOf(InstanceOfAssertFactories.ITERABLE).contains(instance);
    }

    @Override
    protected BiConsumer<ObjectAssert<?>, ComponentRelation<?, ?>> verifyRelationInstance() {
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

    enum Target1 {
        FIRST, SECOND, THIRD
    }

    enum Target2 {
        ONE, TWO, THREE
    }

    static final class Target3 {

        private final int value;

        public Target3(int value) {
            this.value = value;
        }

        @Override
        public int hashCode() {
            return value;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof Target3 other && this.value == other.value;
        }

    }

}
