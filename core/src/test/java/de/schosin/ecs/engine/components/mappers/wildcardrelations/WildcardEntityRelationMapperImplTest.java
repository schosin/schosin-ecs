package de.schosin.ecs.engine.components.mappers.wildcardrelations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Objects;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import de.schosin.ecs.api.components.Relation;
import de.schosin.ecs.api.components.Relation.Exclusive;
import de.schosin.ecs.engine.AbstractWorldTest;
import de.schosin.ecs.engine.components.ComponentMapperManager.PoolingComponents;

class WildcardEntityRelationMapperImplTest extends AbstractWorldTest {

    int target1;
    int target2;
    int target3;

    @BeforeEach
    void setupTargets() {
        this.target1 = world.createEntity();
        this.target2 = world.createEntity();
        this.target3 = world.createEntity();
    }

    @Nested
    class HasTest {

        @Test
        void testEmptyEntity() {
            var entityId = world.createEntity();

            var mapper1 = world.getWildcardEntityRelations(Object.class);
            var mapper2 = world.getWildcardEntityRelations(Relationship12.class);

            // Verify
            assertThat(mapper1.has(entityId)).isFalse();
            assertThat(mapper2.has(entityId)).isFalse();
        }

        @Test
        void testHas() {
            var relation1 = Relation.create(new Relationship1(1), target1);
            var relation2 = Relation.create(new Relationship2(2), target2);
            var relation3 = Relation.create(new Relationship3(3), target3);

            var entity1 = world.createEntity(relation1);
            var entity2 = world.createEntity(relation2);
            var entity3 = world.createEntity(relation3);

            var mapper1 = world.getWildcardEntityRelations(Object.class);
            var mapper2 = world.getWildcardEntityRelations(Relationship12.class);
            var mapper3 = world.getWildcardEntityRelations(Relationship3.class);

            // Verify
            assertThat(mapper1.has(entity1)).isTrue();
            assertThat(mapper2.has(entity1)).isTrue();
            assertThat(mapper3.has(entity1)).isFalse();

            assertThat(mapper1.has(entity2)).isTrue();
            assertThat(mapper2.has(entity2)).isTrue();
            assertThat(mapper3.has(entity2)).isFalse();

            assertThat(mapper1.has(entity3)).isTrue();
            assertThat(mapper2.has(entity3)).isFalse();
            assertThat(mapper3.has(entity3)).isTrue();
        }

    }

    @Nested
    class GetTest {

        @Test
        void testResultInstanceReused() {
            var entityId = world.createEntity();

            var mapper = world.getWildcardEntityRelations(Object.class);

            // Call
            var result1 = mapper.get(entityId);

            world.process(); // reclaim

            // Verify
            assertThat(mapper.get(entityId)).isSameAs(result1);
        }

        @Test
        @SuppressWarnings("unchecked")
        void testFree_ResultInstanceReused() {
            var entityId = world.createEntity();

            var mapper = world.getWildcardEntityRelations(Object.class);

            // Call
            var result1 = mapper.get(entityId);

            var poolingMapper = assertThat(mapper).asInstanceOf(InstanceOfAssertFactories.type(PoolingComponents.class)).actual();
            poolingMapper.free(result1);

            // Verify
            assertThat(mapper.get(entityId)).isSameAs(result1);

        }

        @Test
        void testEmptyEntity() {
            var entityId = world.createEntity();

            var mapper1 = world.getWildcardEntityRelations(Object.class);
            var mapper2 = world.getWildcardEntityRelations(Relationship12.class);

            // Verify
            var result1 = mapper1.get(entityId);
            assertThat(result1).isEmpty();
            assertThat(result1.size()).isZero();
            assertThat(result1.isEmpty()).isTrue();

            var result2 = mapper2.get(entityId);
            assertThat(result2).isEmpty();
            assertThat(result2.size()).isZero();
            assertThat(result2.isEmpty()).isTrue();
        }

        @Test
        void testGet() {
            var relation1 = Relation.create(new Relationship1(1), target1);
            var relation2 = Relation.create(new Relationship2(2), target2);
            var relation3 = Relation.create(new Relationship3(3), target3);

            var entity1 = world.createEntity(relation1);
            var entity2 = world.createEntity(relation2);
            var entity3 = world.createEntity(relation3);

            var mapper1 = world.getWildcardEntityRelations(Object.class);
            var mapper2 = world.getWildcardEntityRelations(Relationship12.class);
            var mapper3 = world.getWildcardEntityRelations(Relationship3.class);

            // Verify
            assertThat(mapper1.get(entity1)).asInstanceOf(InstanceOfAssertFactories.ITERABLE).containsExactlyInAnyOrder(relation1);
            assertThat(mapper2.get(entity1)).asInstanceOf(InstanceOfAssertFactories.ITERABLE).containsExactlyInAnyOrder(relation1);
            assertThat(mapper3.get(entity1)).isEmpty();

            assertThat(mapper1.get(entity2)).asInstanceOf(InstanceOfAssertFactories.ITERABLE).containsExactlyInAnyOrder(relation2);
            assertThat(mapper2.get(entity2)).asInstanceOf(InstanceOfAssertFactories.ITERABLE).containsExactlyInAnyOrder(relation2);
            assertThat(mapper3.get(entity2)).isEmpty();

            assertThat(mapper1.get(entity3)).asInstanceOf(InstanceOfAssertFactories.ITERABLE).containsExactlyInAnyOrder(relation3);
            assertThat(mapper2.get(entity3)).isEmpty();
            assertThat(mapper3.get(entity3)).asInstanceOf(InstanceOfAssertFactories.ITERABLE).containsExactlyInAnyOrder(relation3);
        }

        @Test
        void testGet_MultipleRelations() {
            var relation1 = Relation.create(new Relationship1(1), target1);
            var relation2 = Relation.create(new Relationship2(2), target2);
            var relation3 = Relation.create(new Relationship3(3), target3);

            var entityId = world.createEntity(relation1, relation2, relation3);

            var mapper1 = world.getWildcardEntityRelations(Object.class);
            var mapper2 = world.getWildcardEntityRelations(Relationship12.class);
            var mapper3 = world.getWildcardEntityRelations(Relationship3.class);

            // Verify
            assertThat(mapper1.get(entityId)).asInstanceOf(InstanceOfAssertFactories.ITERABLE).containsExactlyInAnyOrder(relation1, relation2, relation3);
            assertThat(mapper2.get(entityId)).asInstanceOf(InstanceOfAssertFactories.ITERABLE).containsExactlyInAnyOrder(relation1, relation2);
            assertThat(mapper3.get(entityId)).asInstanceOf(InstanceOfAssertFactories.ITERABLE).containsExactlyInAnyOrder(relation3);
        }

        @Test
        void testGetByIndex() {
            var relation1 = Relation.create(new Relationship1(1), target1);
            var relation2 = Relation.create(new Relationship2(2), target2);
            var relation3 = Relation.create(new Relationship3(3), target3);

            var entityId = world.createEntity(relation1, relation2, relation3);

            var mapper = world.getWildcardEntityRelations(Object.class);

            // Verify
            var result = mapper.get(entityId);
            assertThat(result.isEmpty()).isFalse();
            assertThat(result.size()).isEqualTo(3);
            assertThat(result.get(0)).isIn(relation1, relation2, relation3);
            assertThat(result.get(1)).isIn(relation1, relation2, relation3);
            assertThat(result.get(2)).isIn(relation1, relation2, relation3);

            assertThatThrownBy(() -> result.get(3)).isInstanceOf(ArrayIndexOutOfBoundsException.class);
        }

    }

    @Nested
    class RemoveTest {

        @Test
        void testEmptyEntity() {
            var entityId = world.createEntity();

            var mapper1 = world.getWildcardEntityRelations(Object.class);
            var mapper2 = world.getWildcardEntityRelations(Relationship12.class);

            // Verify
            assertThat(mapper1.remove(entityId)).isFalse();
            assertThat(mapper2.remove(entityId)).isFalse();
        }

        @Test
        void testGet_MultipleRelations() {
            var type1 = relation(Relationship1.class);
            var type2 = relation(Relationship2.class);
            var type3 = exclusiveRelation(Relationship3.class);

            var entity1 = world.createEntity(
                    Relation.create(new Relationship1(1), target1),
                    Relation.create(new Relationship2(2), target2),
                    Relation.create(new Relationship3(3), target3));

            var entity2 = world.createEntity(
                    Relation.create(new Relationship1(1), target1),
                    Relation.create(new Relationship2(2), target2),
                    Relation.create(new Relationship3(3), target3));

            var entity3 = world.createEntity(
                    Relation.create(new Relationship1(1), target1),
                    Relation.create(new Relationship2(2), target2),
                    Relation.create(new Relationship3(3), target3));

            var mapper1 = world.getWildcardEntityRelations(Object.class);
            var mapper2 = world.getWildcardEntityRelations(Relationship12.class);
            var mapper3 = world.getWildcardEntityRelations(Relationship3.class);

            verify(verify -> {
                verify.expectUpdated(entity1, NO_COMPONENTS);
                verify.expectUpdated(entity2, type3);
                verify.expectUpdated(entity3, type1, type2);
                verify.expectNoMoreUpdated();

                // Call
                mapper1.remove(entity1);
                mapper2.remove(entity2);
                mapper3.remove(entity3);

                world.process();

                // Verify
                verifyDoesNotHaveComponents(entity1, type1, type2, type3);
                verifyComponentMaskDoesNotHaveComponents(entity1, type1, type2, type3);

                verifyHasComponents(entity2, type3);
                verifyDoesNotHaveComponents(entity2, type1, type2);
                verifyComponentMaskHasComponents(entity2, type3);
                verifyComponentMaskDoesNotHaveComponents(entity2, type1, type2);

                verifyHasComponents(entity3, type1, type2);
                verifyDoesNotHaveComponents(entity3, type3);
                verifyComponentMaskHasComponents(entity3, type1, type2);
                verifyComponentMaskDoesNotHaveComponents(entity3, type3);
            });
        }

    }

    interface Relationship12 {
    }

    record Relationship1(int value) implements Relationship12 {
    }

    record Relationship2(int value) implements Relationship12 {
    }

    class Relationship3 implements Exclusive {
        private int value;

        public Relationship3(int value) {
            this.value = value;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + getEnclosingInstance().hashCode();
            result = prime * result + Objects.hash(this.value);
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            Relationship3 other = (Relationship3) obj;
            if (!getEnclosingInstance().equals(other.getEnclosingInstance()))
                return false;
            return this.value == other.value;
        }

        private WildcardEntityRelationMapperImplTest getEnclosingInstance() {
            return WildcardEntityRelationMapperImplTest.this;
        }
    }

    record Target1(int value) {
    }

}
