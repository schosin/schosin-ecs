package de.schosin.ecs.engine.components.mappers.relations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import de.schosin.ecs.api.components.Relation.EntityRelation;
import de.schosin.ecs.api.components.mappers.Components;
import de.schosin.ecs.api.components.mappers.EntityRelationMappers.ExclusiveEntityRelationMapper;

class ExclusiveEntityRelationMapperImplTest extends AbstractEntityRelationsTest {

    @Nested
    class ExclusiveEntityRelationMapperTest extends AbstractEntityRelationTest<Loves, Loves2, ExclusiveEntityRelationMapper<Loves>, ExclusiveEntityRelationMapper<Loves2>> {

        public ExclusiveEntityRelationMapperTest() {
            super(exclusiveRelation(Loves.class), exclusiveRelation(Loves2.class));
        }

        @Override
        protected ExclusiveEntityRelationMapper<Loves> createMapper1() {
            return world.getExclusiveEntityRelations(Loves.class);
        }

        @Override
        protected ExclusiveEntityRelationMapper<Loves2> createMapper2() {
            return world.getExclusiveEntityRelations(Loves2.class);
        }

        @Override
        @SuppressWarnings({ "unchecked", "rawtypes" })
        protected <RR> void add(Components<EntityRelation<RR>, ?> mapper, int entityId, RR relationship, int target) {
            ((ExclusiveEntityRelationMapper) mapper).add(entityId, relationship, target);
        }

        @Test
        void testCreateEntity_SameRelationship_Throws() {
            assertThatThrownBy(() -> world.createEntity(relation(relationship1, target1), relation(relationship1, target2)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContainingAll("EntityRelations", relationship1.getClass().getSimpleName());
        }

        @Test
        void testGetRelationshipTarget() {
            var entityId = world.createEntity(relation(Loves.ADORES, target2));

            assertThat(mapper1.getRelationship(entityId)).isSameAs(Loves.ADORES);
            assertThat(mapper1.getTarget(entityId)).isSameAs(target2);
        }

        @Test
        void testGetRelationshipTarget_WhenNoRelations() {
            var entityId = world.createEntity();

            assertThat(mapper1.getRelationship(entityId)).isNull();
            assertThat(mapper1.getTarget(entityId)).isEqualTo(-1);
        }

        @Test
        void testExclusiveTrait_OverridesExistingRelation() {
            var entityId = world.createEntity(relation(Loves.LOVES, target1));

            verify(verify -> {
                verify.expectNoMoreUpdated();

                mapper1.add(entityId, Loves.LOVES, target2);

                world.process();

                assertThat(mapper1.get(entityId))
                        .extracting("relationship", "target")
                        .contains(Loves.LOVES, target2);
            });
        }

        @Test
        void testExclusiveTrait_WhenAddedMultipleTimes_KeepsOnlyLast() {
            var entityId = world.createEntity();

            verify(verify -> {
                verify.expectUpdated(entityId, type1);
                verify.expectNoMoreUpdated();

                mapper1.add(entityId, Loves.LOVES, target1);
                mapper1.add(entityId, Loves.LOVES, target2);

                world.process();

                assertThat(mapper1.get(entityId))
                        .extracting("relationship", "target")
                        .contains(Loves.LOVES, target2);
            });
        }

        @Test
        void testRelationsRemoved_IfTargetDeleted() {
            var entityId = world.createEntity(relation(Loves.LOVES, target1));

            // Delete target1
            world.deleteEntity(target1);
            assertThat(mapper1.get(entityId)).extracting("relationship", "target").contains(Loves.LOVES, target1);

            // Process
            world.process();
            assertThat(mapper1.get(entityId)).isNull();
        }

        @Test
        void testArchetypeUpdated_IfRelationRemovedByDeletedTarget() {
            var entityId = world.createEntity(new RegularComponent(), relation(Loves.LOVES, target1));

            verify(verify -> {
                verify.expectNoMoreUpdated();

                world.deleteEntity(target1);
            });

            verify(verify -> {
                verify.expectUpdated(entityId, RegularComponent.class);
                verify.expectNoMoreUpdated();

                world.process();

                verifyHasComponents(entityId, RegularComponent.class);
                verifyDoesNotHaveComponents(entityId, type1);

                verifyArchetypeHasComponents(entityId, RegularComponent.class);
                verifyArchetypeDoesNotHaveComponents(entityId, type1);
            });
        }

    }

}
