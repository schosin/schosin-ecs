package de.schosin.ecs.plugins.wildcards.mappers;

import static de.schosin.ecs.plugins.wildcards.types.WildcardType.wildcardRelation;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import de.schosin.ecs.api.components.Relation;
import de.schosin.ecs.api.components.Relation.Exclusive;
import de.schosin.ecs.api.components.types.RelationComponentType.ComponentRelationType;
import de.schosin.ecs.api.components.types.RelationComponentType.ExclusiveComponentRelationType;
import de.schosin.ecs.engine.components.ComponentMapperManager.ReclaimingComponents;

class WildcardComponentRelationMapperTest extends AbstractMapperTest {

    @Nested
    class HasTest {

        @Test
        void testEmptyEntity() {
            var entityId = world.createEntity();

            var mapper1 = world.getComponents(wildcardRelation(Object.class, Object.class));
            var mapper2 = world.getComponents(wildcardRelation(Relationship12.class, Object.class));

            // Verify
            assertThat(mapper1.has(entityId)).isFalse();
            assertThat(mapper2.has(entityId)).isFalse();
        }

        @Test
        void testHas() {
            var relation1 = Relation.create(new Relationship1(1), new Target1(10));
            var relation2 = Relation.create(new Relationship2(2), new Target1(20));
            var relation3 = Relation.create(new Relationship3(3), new Target1(30));

            var entity1 = world.createEntity(relation1);
            var entity2 = world.createEntity(relation2);
            var entity3 = world.createEntity(relation3);

            var mapper1 = world.getComponents(wildcardRelation(Object.class, Object.class));
            var mapper2 = world.getComponents(wildcardRelation(Relationship12.class, Object.class));
            var mapper3 = world.getComponents(wildcardRelation(Relationship3.class, Object.class));

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

            var mapper = world.getComponents(wildcardRelation(Object.class, Object.class));

            // Call
            var result1 = mapper.get(entityId);

            world.process(); // reclaim

            // Verify
            assertThat(mapper.get(entityId)).isSameAs(result1);
        }

        @Test
        void testEmptyEntity() {
            var entityId = world.createEntity();

            var mapper1 = world.getComponents(wildcardRelation(Object.class, Object.class));
            var mapper2 = world.getComponents(wildcardRelation(Relationship12.class, Object.class));

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
            var relation1 = Relation.create(new Relationship1(1), new Target1(10));
            var relation2 = Relation.create(new Relationship2(2), new Target1(20));
            var relation3 = Relation.create(new Relationship3(3), new Target1(30));

            var entity1 = world.createEntity(relation1);
            var entity2 = world.createEntity(relation2);
            var entity3 = world.createEntity(relation3);

            var mapper1 = world.getComponents(wildcardRelation(Object.class, Object.class));
            var mapper2 = world.getComponents(wildcardRelation(Relationship12.class, Object.class));
            var mapper3 = world.getComponents(wildcardRelation(Relationship3.class, Object.class));

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
            var relation1 = Relation.create(new Relationship1(1), new Target1(10));
            var relation2 = Relation.create(new Relationship2(2), new Target1(20));
            var relation3 = Relation.create(new Relationship3(3), new Target1(30));

            var entityId = world.createEntity(relation1, relation2, relation3);

            var mapper1 = world.getComponents(wildcardRelation(Object.class, Object.class));
            var mapper2 = world.getComponents(wildcardRelation(Relationship12.class, Object.class));
            var mapper3 = world.getComponents(wildcardRelation(Relationship3.class, Object.class));

            // Verify
            assertThat(mapper1.get(entityId)).asInstanceOf(InstanceOfAssertFactories.ITERABLE).containsExactlyInAnyOrder(relation1, relation2, relation3);
            assertThat(mapper2.get(entityId)).asInstanceOf(InstanceOfAssertFactories.ITERABLE).containsExactlyInAnyOrder(relation1, relation2);
            assertThat(mapper3.get(entityId)).asInstanceOf(InstanceOfAssertFactories.ITERABLE).containsExactlyInAnyOrder(relation3);
        }

        @Test
        void testGetByIndex() {
            var relation1 = Relation.create(new Relationship1(1), new Target1(10));
            var relation2 = Relation.create(new Relationship2(2), new Target1(20));
            var relation3 = Relation.create(new Relationship3(3), new Target1(30));

            var entityId = world.createEntity(relation1, relation2, relation3);

            var mapper = world.getComponents(wildcardRelation(Object.class, Object.class));

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

            var mapper1 = world.getComponents(wildcardRelation(Object.class, Object.class));
            var mapper2 = world.getComponents(wildcardRelation(Relationship12.class, Object.class));

            // Verify
            assertThat(mapper1.remove(entityId)).isFalse();
            assertThat(mapper2.remove(entityId)).isFalse();
        }

        @Test
        void testGet_MultipleRelations() {
            var type1 = relation(Relationship1.class, Target1.class);
            var type2 = relation(Relationship2.class, Target1.class);
            var type3 = exclusiveRelation(Relationship3.class, Target1.class);

            var entity1 = world.createEntity(
                    Relation.create(new Relationship1(1), new Target1(10)),
                    Relation.create(new Relationship2(2), new Target1(20)),
                    Relation.create(new Relationship3(3), new Target1(30)));

            var entity2 = world.createEntity(
                    Relation.create(new Relationship1(1), new Target1(10)),
                    Relation.create(new Relationship2(2), new Target1(20)),
                    Relation.create(new Relationship3(3), new Target1(30)));

            var entity3 = world.createEntity(
                    Relation.create(new Relationship1(1), new Target1(10)),
                    Relation.create(new Relationship2(2), new Target1(20)),
                    Relation.create(new Relationship3(3), new Target1(30)));

            var mapper1 = world.getComponents(wildcardRelation(Object.class, Object.class));
            var mapper2 = world.getComponents(wildcardRelation(Relationship12.class, Object.class));
            var mapper3 = world.getComponents(wildcardRelation(Relationship3.class, Object.class));

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

    @Nested
    class ReclaimTest {

        @Test
        void testReclaim() {
            var relation1 = Relation.create(new Relationship1(1), new Target1(10));
            var relation2 = Relation.create(new Relationship2(2), new Target1(20));
            var relation3 = Relation.create(new Relationship3(3), new Target1(30));

            var entityId = world.createEntity(relation1, relation2, relation3);

            var mapper = world.getComponents(wildcardRelation(Object.class, Object.class));

            // Call
            var result = mapper.get(entityId);
            assertThat(result).asInstanceOf(InstanceOfAssertFactories.ITERABLE).containsExactlyInAnyOrder(relation1, relation2, relation3);
            assertThat(result.toString()).contains(relation1.toString(), relation2.toString(), relation3.toString());

            var reclaimingMapper = assertThat(mapper).asInstanceOf(InstanceOfAssertFactories.type(ReclaimingComponents.class)).actual();
            reclaimingMapper.reclaim();

            // Verify
            assertThat(result.toString()).contains("invalidated");
            assertThat(mapper.get(entityId)).isSameAs(result);
        }

        @Test
        void testReclaim_WorldProcess() {
            var relation1 = Relation.create(new Relationship1(1), new Target1(10));
            var relation2 = Relation.create(new Relationship2(2), new Target1(20));
            var relation3 = Relation.create(new Relationship3(3), new Target1(30));

            var entityId = world.createEntity(relation1, relation2, relation3);

            var mapper = world.getComponents(wildcardRelation(Object.class, Object.class));

            // Call
            var result = mapper.get(entityId);
            assertThat(result).asInstanceOf(InstanceOfAssertFactories.ITERABLE).containsExactlyInAnyOrder(relation1, relation2, relation3);
            assertThat(result.toString()).contains(relation1.toString(), relation2.toString(), relation3.toString());

            world.process();

            // Verify
            assertThat(result.toString()).contains("invalidated");
            assertThat(mapper.get(entityId)).isSameAs(result);
        }

    }

    @Nested
    class GetRegularComponentTypesTest {

        @Test
        void testComponentRelationWildcards() {
            var relation11 = componentManager.getComponent(new ComponentRelationType<>(C1.class, C1.class)).type();
            var relation12 = componentManager.getComponent(new ComponentRelationType<>(C1.class, C2.class)).type();
            var exclusive11 = componentManager.getComponent(new ExclusiveComponentRelationType<>(Exclusive1.class, C1.class)).type();

            componentManager.getComponent(new ComponentRelationType<>(C1.class, C3.class));
            componentManager.getComponent(new ExclusiveComponentRelationType<>(Exclusive1.class, C3.class));
            componentManager.getComponent(new ExclusiveComponentRelationType<>(Exclusive2.class, C2.class));

            var componentTypes = componentManager.getRegularComponentTypes(wildcardRelation(Bound.class, Bound.class));
            assertThat(componentTypes).containsExactlyInAnyOrder(relation11, relation12, exclusive11);
        }

        @Test
        void testComponentRelationRelationshipWildcard() {
            var relation12 = componentManager.getComponent(new ComponentRelationType<>(C1.class, C2.class)).type();
            var exclusive22 = componentManager.getComponent(new ExclusiveComponentRelationType<>(Exclusive2.class, C2.class)).type();

            componentManager.getComponent(new ComponentRelationType<>(C1.class, C1.class));
            componentManager.getComponent(new ComponentRelationType<>(C1.class, C3.class));
            componentManager.getComponent(new ExclusiveComponentRelationType<>(Exclusive1.class, C1.class));
            componentManager.getComponent(new ExclusiveComponentRelationType<>(Exclusive1.class, C3.class));

            var componentTypes = componentManager.getRegularComponentTypes(wildcardRelation(Object.class, C2.class));
            assertThat(componentTypes).containsExactlyInAnyOrder(relation12, exclusive22);
        }

        @Test
        void testComponentRelationTargetWildcard() {
            var relation11 = componentManager.getComponent(new ComponentRelationType<>(C1.class, C1.class)).type();
            var relation12 = componentManager.getComponent(new ComponentRelationType<>(C1.class, C2.class)).type();

            componentManager.getComponent(new ComponentRelationType<>(C1.class, C3.class));
            componentManager.getComponent(new ExclusiveComponentRelationType<>(Exclusive2.class, C2.class));
            componentManager.getComponent(new ExclusiveComponentRelationType<>(Exclusive1.class, C1.class));
            componentManager.getComponent(new ExclusiveComponentRelationType<>(Exclusive1.class, C3.class));

            var componentTypes = componentManager.getRegularComponentTypes(wildcardRelation(C1.class, Bound.class));
            assertThat(componentTypes).containsExactlyInAnyOrder(relation11, relation12);
        }

    }

    interface Relationship12 {
    }

    record Relationship1(int value) implements Relationship12 {
    }

    record Relationship2(int value) implements Relationship12 {
    }

    record Relationship3(int value) implements Exclusive {
    }

    record Target1(int value) {
    }

    public interface Bound {
    }

    public record C1() implements Bound {
    }

    public record C2() implements Bound {
    }

    public record C3() {
    }

    public record Exclusive1() implements Exclusive, Bound {
    }

    record Exclusive2() implements Exclusive {
    }

}
