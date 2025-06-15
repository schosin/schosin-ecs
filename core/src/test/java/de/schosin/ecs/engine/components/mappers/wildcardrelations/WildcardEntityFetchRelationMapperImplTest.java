package de.schosin.ecs.engine.components.mappers.wildcardrelations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import de.schosin.ecs.api.components.Relation;
import de.schosin.ecs.api.components.Relation.Exclusive;
import de.schosin.ecs.engine.AbstractWorldTest;
import de.schosin.ecs.engine.components.ComponentMapperManager.PoolingComponents;

class WildcardEntityFetchRelationMapperImplTest extends AbstractWorldTest {

    @Nested
    class HasTest {

        @Test
        void testEmptyEntity() {
            var entityId = world.createEntity();

            var mapper1 = world.getWildcardEntityFetchRelations(Object.class, component(Position.class));
            var mapper2 = world.getWildcardEntityFetchRelations(Exclusive.class, component(Position.class));

            // Verify
            assertThat(mapper1.has(entityId)).isFalse();
            assertThat(mapper2.has(entityId)).isFalse();
        }

        @Test
        void testHas() {
            var target1 = world.createEntity();
            var target2 = world.createEntity();

            var relation1 = Relation.create(new Relationship1(1), target1);
            var relation2 = Relation.create(new Relationship2(2), target2);

            var entity1 = world.createEntity(relation1);
            var entity2 = world.createEntity(relation2);

            var mapper1 = world.getWildcardEntityFetchRelations(Object.class, component(Position.class));
            var mapper2 = world.getWildcardEntityFetchRelations(Exclusive.class, component(Position.class));

            // Verify
            assertThat(mapper1.has(entity1)).isTrue();
            assertThat(mapper1.has(entity2)).isTrue();

            assertThat(mapper2.has(entity1)).isFalse();
            assertThat(mapper2.has(entity2)).isTrue();
        }

    }

    @Nested
    class GetTest {

        @Test
        void testResultInstanceReused() {
            var relation = Relation.create(new Relationship1(1), world.createEntity());
            var entityId = world.createEntity(relation);

            var mapper = world.getWildcardEntityFetchRelations(Object.class, component(Position.class));

            // Call
            var result1 = mapper.get(entityId);

            world.process(); // reclaim

            // Verify
            assertThat(mapper.get(entityId)).isSameAs(result1);
        }

        @Test
        @SuppressWarnings("unchecked")
        void testFree_ResultInstanceReused() {
            var relation = Relation.create(new Relationship1(1), world.createEntity());
            var entityId = world.createEntity(relation);

            var mapper = world.getWildcardEntityFetchRelations(Object.class, component(Position.class));

            // Call
            var result1 = mapper.get(entityId);

            var poolingMapper = assertThat(mapper).asInstanceOf(InstanceOfAssertFactories.type(PoolingComponents.class)).actual();
            poolingMapper.free(result1);

            // Verify
            assertThat(mapper.get(entityId)).isSameAs(result1);

        }

        @Test
        void testNoRelations() {
            var entityId = world.createEntity();

            var mapper = world.getWildcardEntityFetchRelations(Object.class, component(Position.class));

            // Call
            var result = mapper.get(entityId);

            // Verify
            assertThat(result).isNotNull().isEmpty();
        }

        @Test
        void testRelationTargetData() {
            var target1 = world.createEntity(new Position(1, 10));
            var target2 = world.createEntity(new Position(2, 20));
            var target3 = world.createEntity(new Position(3, 30));

            var relation1 = Relation.create(new Relationship1(1), target1);
            var relation2 = Relation.create(new Relationship1(2), target2);
            var relation3 = Relation.create(new Relationship2(3), target3);

            var entityId = world.createEntity(relation1, relation2, relation3);

            var mapper = world.getWildcardEntityFetchRelations(Object.class, component(Position.class));

            // Call
            var result = mapper.get(entityId);

            // Verify
            assertThat(result).isNotEmpty();
            assertThat(result.isEmpty()).isFalse();
            assertThat(result.size()).isEqualTo(3);

            assertThat(result)
                    .extracting("target", "data.x", "data.y")
                    .containsExactlyInAnyOrder(
                            tuple(target1, 1, 10),
                            tuple(target2, 2, 20),
                            tuple(target3, 3, 30));
        }

        @Test
        void testRelationTargetDataByIndex() {
            var target1 = world.createEntity(new Position(1, 10));
            var target2 = world.createEntity(new Position(2, 20));
            var target3 = world.createEntity(new Position(3, 30));

            var relation1 = Relation.create(new Relationship1(1), target1);
            var relation2 = Relation.create(new Relationship1(2), target2);
            var relation3 = Relation.create(new Relationship2(3), target3);

            var entityId = world.createEntity(relation1, relation2, relation3);

            var mapper = world.getWildcardEntityFetchRelations(Object.class, component(Position.class));

            // Call
            var result = mapper.get(entityId);

            // Verify
            assertThat(result).isNotEmpty();
            assertThat(result.isEmpty()).isFalse();
            assertThat(result.size()).isEqualTo(3);

            var result1 = result.get(0);
            assertThat(result1.target()).isIn(target1, target2, target3);
            assertThat(result1.data().x).isIn(1, 2, 3);

            var result2 = result.get(1);
            assertThat(result2.target()).isIn(target1, target2, target3).isNotEqualTo(result1.target());
            assertThat(result2.data().x).isIn(1, 2, 3).isNotEqualTo(result1.data().x);

            var result3 = result.get(2);
            assertThat(result3.target()).isIn(target1, target2, target3).isNotIn(result1.target(), result2.target());
            assertThat(result3.data().x).isIn(1, 2, 3).isNotIn(result1.data().x, result2.data().x);

            assertThatThrownBy(() -> result.get(3)).isInstanceOf(ArrayIndexOutOfBoundsException.class);
        }

    }

    @Nested
    class RemoveTest {

        @Test
        void testRemove() {
            var type1 = relation(Relationship1.class);
            var type2 = exclusiveRelation(Relationship2.class);

            var target1 = world.createEntity();
            var target2 = world.createEntity();

            var entity1 = world.createEntity(Relation.create(new Relationship1(1), target1));
            var entity2 = world.createEntity(Relation.create(new Relationship2(2), target2));
            var entity3 = world.createEntity(Relation.create(new Relationship1(1), target1));
            var entity4 = world.createEntity(Relation.create(new Relationship2(2), target2));

            var mapper1 = world.getWildcardEntityFetchRelations(Object.class, component(Position.class));
            var mapper2 = world.getWildcardEntityFetchRelations(Exclusive.class, component(Position.class));

            verify(verify -> {
                verify.expectUpdated(entity1, NO_COMPONENTS);
                verify.expectUpdated(entity2, NO_COMPONENTS);
                verify.expectUpdated(entity4, NO_COMPONENTS);
                verify.expectNoMoreUpdated();

                // Call
                assertThat(mapper1.remove(entity1)).isTrue();
                assertThat(mapper1.remove(entity2)).isTrue();

                assertThat(mapper2.remove(entity3)).isFalse();
                assertThat(mapper2.remove(entity4)).isTrue();

                world.process();

                // Verify
                verifyDoesNotHaveComponents(entity1, type1);
                verifyComponentMaskDoesNotHaveComponents(entity1, type1);

                verifyDoesNotHaveComponents(entity2, type2);
                verifyComponentMaskDoesNotHaveComponents(entity2, type2);

                verifyHasComponents(entity3, type1);
                verifyComponentMaskHasComponents(entity3, type1);

                verifyDoesNotHaveComponents(entity4, type2);
                verifyComponentMaskDoesNotHaveComponents(entity4, type2);
            });
        }

    }

    record Relationship1(int value) {
    }

    record Relationship2(int value) implements Exclusive {
    }

    record Position(int x, int y) {
    }

}
