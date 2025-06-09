package de.schosin.ecs.storage.testsuite.components.relations;

import static de.schosin.ecs.api.components.types.ComponentType.detectComponentType;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;

import org.junit.jupiter.api.Test;

import de.schosin.ecs.api.components.Relation;
import de.schosin.ecs.api.components.Relation.ComponentRelation;
import de.schosin.ecs.api.components.Relation.Exclusive;
import de.schosin.ecs.api.components.types.RelationComponentType.ExclusiveComponentRelationType;
import de.schosin.ecs.storage.testsuite.components.relations.ExclusiveComponentRelationDataTest.Relationship1;
import de.schosin.ecs.storage.testsuite.components.relations.ExclusiveComponentRelationDataTest.Relationship2;
import de.schosin.ecs.storage.testsuite.components.relations.ExclusiveComponentRelationDataTest.Relationship3;
import de.schosin.ecs.storage.testsuite.components.relations.ExclusiveComponentRelationDataTest.Target1;
import de.schosin.ecs.storage.testsuite.components.relations.ExclusiveComponentRelationDataTest.Target2;
import de.schosin.ecs.storage.testsuite.components.relations.ExclusiveComponentRelationDataTest.Target3;
import de.schosin.ecs.utils.collections.ImmutableBag;

public class ExclusiveComponentRelationDataTest extends
        CommonComponentRelationTest<Relationship1, Target1, ComponentRelation<Relationship1, Target1>, Relationship2, Target2, ComponentRelation<Relationship2, Target2>, Relationship3, Target3, ComponentRelation<Relationship3, Target3>> {

    @Test
    void testOverrideExistingComponent_AppliedImmediately() {
        var type = type1();

        var instance1 = getInstance(type);
        var instance2 = getInstance(type);
        assertThat(instance2).as("must be different instances (test suite broken if this fails)").isNotSameAs(instance1);

        var entityId = world.createEntity(instance1);
        assertThat(getComponent(entityId, type)).as("returns instance passed at creation").isSameAs(instance1);
        assertThat(storageEngine.getPendingComponentMask(entityId)).as("getPendingComponentMask returns null after creation").isNull();

        // Call
        storageEngine.add(entityId, ImmutableBag.of(type), new Object[] { instance2 });

        // Verify
        assertThat(getComponent(entityId, type)).as("returns instance passed at creation").isSameAs(instance2);
        assertThat(storageEngine.getPendingComponentMask(entityId)).as("getPendingComponentMask returns null if add caused no component mask change").isNull();
    }

    @Test
    void testOverrideExistingComponent_DelayedIfSameRelationship() {
        var instance1 = Relation.create(Relationship1.A, Target1.FIRST);
        var type1 = detectComponentType(instance1);

        var instance2 = Relation.create(Relationship1.A, Target2.ONE);
        var type2 = detectComponentType(instance2);

        var entityId = world.createEntity(instance1);
        var componentMask = storageEngine.getComponentMaskForEntity(entityId);
        
        assertThat(getComponent(entityId, type1)).as("returns instance passed at creation").isSameAs(instance1);
        assertThat(storageEngine.getPendingComponentMask(entityId)).as("getPendingComponentMask returns null after creation").isNull();

        // Call
        var updatedComponentMask = storageEngine.add(entityId, ImmutableBag.of(type2), new Object[] { instance2 });
        assertThat(updatedComponentMask).as("adding exclusive relation with different target returns different component mask").isNotEqualTo(componentMask);
        assertThat(updatedComponentMask.getComponentTypes()).as("adding exclusive relation with different target discards old exclusive relation type").containsExactly(type2);

        // Verify
        assertThat(getComponent(entityId, type1)).as("returns instance passed at creation").isSameAs(instance1);
        assertThat(getComponent(entityId, type2)).as("returns pending instance passed at add").isSameAs(instance2);
        assertThat(storageEngine.getComponentMaskForEntity(entityId)).as("getComponentMaskForEntity returns old component mask").isSameAs(componentMask);
        assertThat(storageEngine.getPendingComponentMask(entityId)).as("getPendingComponentMask returns updated component mask").isSameAs(updatedComponentMask);
        
        // Flush
        storageEngine.flushChanges(entityId);
        
        assertThat(getComponent(entityId, type1)).as("returns instance passed at creation").isNull();
        assertThat(getComponent(entityId, type2)).as("returns pending instance passed at add").isSameAs(instance2);
        assertThat(storageEngine.getComponentMaskForEntity(entityId)).as("getComponentMaskForEntity returns updated component mask after flush").isSameAs(updatedComponentMask);
        assertThat(storageEngine.getPendingComponentMask(entityId)).as("getPendingComponentMask returns null after flush").isNull();
        
    }

    @Override
    protected ExclusiveComponentRelationType<Relationship1, Target1> type1() {
        return new ExclusiveComponentRelationType<>(Relationship1.class, Target1.class);
    }

    @Override
    protected ExclusiveComponentRelationType<Relationship2, Target2> type2() {
        return new ExclusiveComponentRelationType<>(Relationship2.class, Target2.class);
    }

    @Override
    protected ExclusiveComponentRelationType<Relationship3, Target3> type3() {
        return new ExclusiveComponentRelationType<>(Relationship3.class, Target3.class);
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
