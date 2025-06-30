package de.schosin.ecs.engine.components.mappers.fetch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import de.schosin.ecs.api.components.Relation;
import de.schosin.ecs.api.components.Relation.EntityRelationData;
import de.schosin.ecs.api.components.mappers.EntityFetchRelationMappers.EntityRelationFetchMapper;
import de.schosin.ecs.api.components.types.ComponentType;
import de.schosin.ecs.engine.components.ComponentMapperManager.ReclaimingComponents;
import de.schosin.ecs.engine.components.mappers.AbstractMapperTest;
import de.schosin.ecs.engine.components.mappers.MyComponentSet;
import de.schosin.ecs.engine.utils.components.ComponentSetsHelperTest.Position;
import de.schosin.ecs.engine.utils.components.ComponentSetsHelperTest.Velocity;

class EntityRelationFetchMapperImplTest extends AbstractMapperTest {

    EntityRelationFetchMapper<Related, MyComponentSet> mapper;

    @BeforeEach
    void setupMapper() {
        this.mapper = world.getComponents(ComponentType.relation(Related.class, MyComponentSet.TYPE));
    }

    @Nested
    class HasTest {

        @Test
        void testHas() {
            var relatedPos = new Position();
            var relatedVelocity = new Velocity();
            var related1 = world.createEntity(relatedPos, relatedVelocity);
            var related2 = world.createEntity();
            var related3 = world.createEntity();

            var entity1 = world.createEntity(Relation.create(Related.Related, related1));
            var entity2 = world.createEntity(Relation.create(Related.Related, related2), Relation.create(Related.Related, related3));
            var entity3 = world.createEntity();

            // Verify
            assertThat(mapper.has(entity1)).isTrue();
            assertThat(mapper.has(entity2)).isTrue();
            assertThat(mapper.has(entity3)).isFalse();
        }

    }

    @Nested
    class GetTest {

        @Test
        void testNoRelated() {
            var entityId = world.createEntity();

            // Verify
            var relation = mapper.get(entityId);
            assertThat(relation).isNotNull().isEmpty();
        }

        @Test
        void testRelatedHasAllComponents() {
            var relatedPos = new Position();
            var relatedVelocity = new Velocity();
            var relatedId = world.createEntity(relatedPos, relatedVelocity);

            var entityId = world.createEntity(Relation.create(Related.Related, relatedId));

            // Verify
            var relations = mapper.get(entityId);
            assertThat(relations).isNotNull();
            assertThat(relations).hasSize(1);

            var relation = relations.get(0);
            assertThat(relation.target()).isEqualTo(relatedId);

            var data = relation.data();
            assertThat(data).isNotNull();
            assertThat(data.pos()).isSameAs(relatedPos);
            assertThat(data.velocity()).isSameAs(relatedVelocity);

            data.process((id, pos, velocity) -> {
                assertThat(id).isEqualTo(relatedId);
                assertThat(pos).isSameAs(relatedPos);
                assertThat(velocity).isSameAs(relatedVelocity);
            });
        }

        @Test
        void testRelatedHasSomeComponents() {
            var relatedVelocity = new Velocity();
            var relatedId = world.createEntity(relatedVelocity);

            var entityId = world.createEntity(Relation.create(Related.Related, relatedId));

            // Verify
            var relations = mapper.get(entityId);
            assertThat(relations).isNotNull();
            assertThat(relations).hasSize(1);

            var relation = relations.get(0);
            assertThat(relation.target()).isEqualTo(relatedId);

            var data = relation.data();
            assertThat(data).isNotNull();
            assertThat(data.pos()).isNull();
            assertThat(data.velocity()).isSameAs(relatedVelocity);
        }

        @Test
        void testRelatedHasNoComponents() {
            var relatedId = world.createEntity();

            var entityId = world.createEntity(Relation.create(Related.Related, relatedId));

            // Verify
            var relations = mapper.get(entityId);
            assertThat(relations).isNotNull();
            assertThat(relations).hasSize(1);

            var relation = relations.get(0);
            assertThat(relation.target()).isEqualTo(relatedId);

            var data = relation.data();
            assertThat(data).isNotNull();
            assertThat(data.pos()).isNull();
            assertThat(data.velocity()).isNull();
        }

        @Test
        void testMultipleRelations() {
            var relatedPos1 = new Position();
            var relatedVelocity1 = new Velocity();
            var related1 = world.createEntity(relatedPos1, relatedVelocity1);

            var relatedPos2 = new Position();
            var relatedVelocity2 = new Velocity();
            var related2 = world.createEntity(relatedPos2, relatedVelocity2);

            var entityId = world.createEntity(Relation.create(Related.Related, related1), Relation.create(Related.Related, related2));

            // Verify
            var relations = mapper.get(entityId);
            assertThat(relations).isNotNull();
            assertThat(relations).hasSize(2);

            assertThat(relations)
                    .extracting(EntityRelationData::target, data -> data.data().pos(), data -> data.data().velocity())
                    .containsExactlyInAnyOrder(
                            tuple(related1, relatedPos1, relatedVelocity1),
                            tuple(related2, relatedPos2, relatedVelocity2));
        }

    }

    @Nested
    class GetRelationshipTest {

        @Test
        void testMultipleRelations() {
            var related1 = world.createEntity();
            var related2 = world.createEntity();
            var unrelated = world.createEntity();

            var entityId = world.createEntity(Relation.create(Related.Related, related1), Relation.create(Related.Related, related2));

            // Verify
            var relations = mapper.get(entityId);
            assertThat(relations).isNotNull();
            assertThat(relations.getRelationship(related1)).isNotNull();
            assertThat(relations.getRelationship(related2)).isNotNull();
            assertThat(relations.getRelationship(unrelated)).isNull();
        }

    }

    @Nested
    class GetDataTest {

        @Test
        void testMultipleRelations() {
            var relatedPos1 = new Position();
            var relatedVelocity1 = new Velocity();
            var related1 = world.createEntity(relatedPos1, relatedVelocity1);

            var relatedPos2 = new Position();
            var relatedVelocity2 = new Velocity();
            var related2 = world.createEntity(relatedPos2, relatedVelocity2);

            var unrelated = world.createEntity();

            var entityId = world.createEntity(Relation.create(Related.Related, related1), Relation.create(Related.Related, related2));

            // Verify
            var relations = mapper.get(entityId);
            assertThat(relations).isNotNull();
            assertThat(relations.getData(related1)).extracting(MyComponentSet::pos, MyComponentSet::velocity).contains(relatedPos1, relatedVelocity1);
            assertThat(relations.getData(related2)).extracting(MyComponentSet::pos, MyComponentSet::velocity).contains(relatedPos2, relatedVelocity2);
            assertThat(relations.getData(unrelated)).isNull();
        }

    }

    @Nested
    class RemoveTest {

        @Test
        void testRemoveRelation_EntityChanged() {
            var relatedPos = new Position();
            var relatedVelocity = new Velocity();
            var relatedId = world.createEntity(relatedPos, relatedVelocity);

            var entityId = world.createEntity(new Position(), Relation.create(Related.Related, relatedId));

            verify(verify -> {
                verify.expectUpdated(entityId, Position.class);
                verify.expectNoMoreUpdated();
                verify.expectNoMoreRemoved();

                // Call
                mapper.remove(entityId);
                world.process();

                // Verify
                verifyHasComponents(entityId, Position.class);
                verifyDoesNotHaveComponents(entityId, relation(Related.class));

                verifyComponentMaskHasComponents(entityId, Position.class);
                verifyComponentMaskDoesNotHaveComponents(entityId, relation(Related.class));
            });
        }

        @Test
        void testRemoveRelation_RelatedUnchanged() {
            var relatedPos = new Position();
            var relatedVelocity = new Velocity();
            var relatedId = world.createEntity(relatedPos, relatedVelocity);

            var entityId = world.createEntity(new Position(), Relation.create(Related.Related, relatedId));

            verify(verify -> {
                verify.expectUpdated(entityId, Position.class);
                verify.expectNoMoreUpdated();
                verify.expectNoMoreRemoved();

                // Call
                mapper.remove(entityId);
                world.process();

                // Verify
                verifyHasComponents(relatedId, Position.class, Velocity.class);
                verifyComponentMaskHasComponents(relatedId, Position.class, Velocity.class);
            });
        }

    }

    @Nested
    class ReclaimTest {

        @Test
        void testReclaim() {
            var relatedPos1 = new Position();
            var relatedVelocity1 = new Velocity();
            var related1 = world.createEntity(relatedPos1, relatedVelocity1);

            var relatedPos2 = new Position();
            var relatedVelocity2 = new Velocity();
            var related2 = world.createEntity(relatedPos2, relatedVelocity2);

            var entityId = world.createEntity(Relation.create(Related.Related, related1), Relation.create(Related.Related, related2));

            // Call
            var result = mapper.get(entityId);
            assertThat(result).isNotNull();
            assertThat(result.toString())
                    .containsSubsequence(Related.Related.toString(), Integer.toString(related1), relatedPos1.toString(), relatedVelocity1.toString())
                    .containsSubsequence(Related.Related.toString(), Integer.toString(related2), relatedPos2.toString(), relatedVelocity2.toString());

            var reclaimingMapper = assertThat(mapper).asInstanceOf(InstanceOfAssertFactories.type(ReclaimingComponents.class)).actual();
            reclaimingMapper.reclaim();

            // Verify
            assertThat(result.toString()).contains("invalidated");
            assertThat(mapper.get(entityId)).isSameAs(result);
        }

        @Test
        void testReclaim_WorldProcess() {
            var relatedPos1 = new Position();
            var relatedVelocity1 = new Velocity();
            var related1 = world.createEntity(relatedPos1, relatedVelocity1);

            var relatedPos2 = new Position();
            var relatedVelocity2 = new Velocity();
            var related2 = world.createEntity(relatedPos2, relatedVelocity2);

            var entityId = world.createEntity(Relation.create(Related.Related, related1), Relation.create(Related.Related, related2));

            // Call
            var result = mapper.get(entityId);
            assertThat(result).isNotNull();
            assertThat(result.toString())
                    .containsSubsequence(Related.Related.toString(), Integer.toString(related1), relatedPos1.toString(), relatedVelocity1.toString())
                    .containsSubsequence(Related.Related.toString(), Integer.toString(related2), relatedPos2.toString(), relatedVelocity2.toString());

            world.process();

            // Verify
            assertThat(result.toString()).contains("invalidated");
            assertThat(mapper.get(entityId)).isSameAs(result);
        }

    }

}
