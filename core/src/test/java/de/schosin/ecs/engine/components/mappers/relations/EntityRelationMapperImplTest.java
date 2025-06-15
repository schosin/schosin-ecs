package de.schosin.ecs.engine.components.mappers.relations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import de.schosin.ecs.api.components.Relation.EntityRelation;
import de.schosin.ecs.api.components.mappers.Components;
import de.schosin.ecs.api.components.mappers.EntityRelationMappers.EntityRelationMapper;

class EntityRelationMapperImplTest extends AbstractEntityRelationsTest {

    @Nested
    class EntityRelationMapperTest extends AbstractEntityRelationTest<Hates, Hates2, EntityRelationMapper<Hates>, EntityRelationMapper<Hates2>> {

        public EntityRelationMapperTest() {
            super(relation(Hates.class), relation(Hates2.class));
        }

        @Override
        protected EntityRelationMapper<Hates> createMapper1() {
            return world.getEntityRelations(Hates.class);
        }

        @Override
        protected EntityRelationMapper<Hates2> createMapper2() {
            return world.getEntityRelations(Hates2.class);
        }

        @Override
        @SuppressWarnings({ "unchecked", "rawtypes" })
        protected <RR> void add(Components<EntityRelation<RR>, ?> mapper, int entityId, RR relationship, int target) {
            ((EntityRelationMapper) mapper).add(entityId, relationship, target);
        }

        @Test
        void testMultipleRelations() {
            var entityId = world.createEntity();

            verify(verify -> {
                verify.expectUpdated(entityId, type1, type2);
                verify.expectNoMoreUpdated();

                add(mapper1, entityId, relationship1, target1);
                add(mapper2, entityId, relationship2, target2);

                world.process();

                verifyHasComponents(entityId, type1, type2);
                verifyComponentMaskHasComponents(entityId, type1, type2);
            });
        }

        @Test
        void testGet() {
            var entityId = world.createEntity(relation(Hates.HATES, target1), relation(Hates.DESPISES, target2));

            var relations = mapper1.get(entityId);
            assertThat(relations)
                    .extracting("relationship", "target")
                    .containsExactlyInAnyOrder(
                            tuple(Hates.HATES, target1),
                            tuple(Hates.DESPISES, target2));
        }

        @Test
        void testGetRelationship() {
            var target3 = world.createEntity();
            var entityId = world.createEntity(relation(Hates.HATES, target1), relation(Hates.DESPISES, target2));

            assertThat(mapper1.getRelationship(entityId, target1)).isSameAs(Hates.HATES);
            assertThat(mapper1.getRelationship(entityId, target2)).isSameAs(Hates.DESPISES);
            assertThat(mapper1.getRelationship(entityId, target3)).isNull();
        }

        @Test
        void testGetRelationship_WhenNoRelations() {
            var entityId = world.createEntity();

            assertThat(mapper1.getRelationship(entityId, target1)).isNull();
            assertThat(mapper1.getRelationship(entityId, target2)).isNull();
        }

        @Test
        void testRelationsRemoved_IfTargetDeleted() {
            var entityId = world.createEntity(relation(Hates.HATES, target1), relation(Hates.DESPISES, target2));

            // Delete target1
            world.deleteEntity(target1);

            assertThat(mapper1.get(entityId))
                    .extracting("relationship", "target")
                    .containsExactlyInAnyOrder(
                            tuple(Hates.HATES, target1),
                            tuple(Hates.DESPISES, target2));

            // Process
            world.process();

            assertThat(mapper1.get(entityId))
                    .extracting("relationship", "target")
                    .containsExactlyInAnyOrder(tuple(Hates.DESPISES, target2));

            // Delete target2 & process
            world.deleteEntity(target2);
            world.process();

            assertThat(mapper1.get(entityId)).isNull();
        }

        @Test
        void testComponentMaskUpdated_IfRelationRemovedByDeletedTarget() {
            var entityId = world.createEntity(new RegularComponent(), relation(Hates.HATES, target1), relation(Hates.DESPISES, target2));

            verify(verify -> {
                verify.expectNoMoreUpdated();

                world.deleteEntity(target1);
                world.process();
            });

            verify(verify -> {
                verify.expectNoMoreUpdated();

                world.deleteEntity(target2);
            });

            verify(verify -> {
                verify.expectUpdated(entityId, RegularComponent.class);
                verify.expectNoMoreUpdated();

                world.process();

                verifyHasComponents(entityId, RegularComponent.class);
                verifyDoesNotHaveComponents(entityId, type1);

                verifyComponentMaskHasComponents(entityId, RegularComponent.class);
                verifyComponentMaskDoesNotHaveComponents(entityId, type1);
            });
        }

    }

}
