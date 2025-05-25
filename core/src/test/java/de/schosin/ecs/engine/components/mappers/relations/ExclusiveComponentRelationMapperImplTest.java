package de.schosin.ecs.engine.components.mappers.relations;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import de.schosin.ecs.api.components.Relation.ComponentRelation;
import de.schosin.ecs.api.components.Relation.Exclusive;
import de.schosin.ecs.api.components.mappers.ComponentRelations.ExclusiveComponentRelationMapper;
import de.schosin.ecs.api.components.mappers.Components;
import de.schosin.ecs.engine.components.mappers.relations.AbstractRelationsTest.Faction.Enemy;
import de.schosin.ecs.engine.components.mappers.relations.AbstractRelationsTest.Faction.Player;

class ExclusiveComponentRelationMapperImplTest extends AbstractComponentRelationsTest {

    @Nested
    class ExclusiveComponentRelationMapperTest
            extends AbstractComponentRelationTest<Loves, Player, Enemy, ExclusiveComponentRelationMapper<Loves, Player>, ExclusiveComponentRelationMapper<Loves, Enemy>> {

        public ExclusiveComponentRelationMapperTest() {
            super(exclusiveRelation(Loves.class, Player.class), exclusiveRelation(Loves.class, Enemy.class));
        }

        @Override
        protected ExclusiveComponentRelationMapper<Loves, Player> createMapper1() {
            return world.getComponentRelations(exclusiveRelation(Loves.class, Player.class));
        }

        @Override
        protected ExclusiveComponentRelationMapper<Loves, Enemy> createMapper2() {
            return world.getComponentRelations(exclusiveRelation(Loves.class, Enemy.class));
        }

        @Override
        @SuppressWarnings({ "unchecked", "rawtypes" })
        protected <RR, T> void add(Components<ComponentRelation<RR, T>, ?> mapper, int entityId, RR relationship, T target) {
            ((ExclusiveComponentRelationMapper) mapper).add(entityId, (Exclusive) relationship, target);
        }

        @Test
        void testGet() {
            var entityId = world.createEntity(relation(relationship1, Player.PLAYER), relation(relationship1, Player.PLAYER2));

            var relation = mapper1.get(entityId);
            assertThat(relation)
                    .extracting("relationship", "target")
                    .contains(relationship1, Player.PLAYER2);
        }

        @Test
        void testGet_WhenMultiple_KeepsLast() {
            var entity1 = world.createEntity(relation(relationship1, Player.PLAYER), relation(relationship1, Player.PLAYER2));
            var entity2 = world.createEntity(relation(relationship1, Player.PLAYER2), relation(relationship1, Player.PLAYER));

            assertThat(mapper1.get(entity1))
                    .extracting("relationship", "target")
                    .contains(relationship1, Player.PLAYER2);

            assertThat(mapper1.get(entity2))
                    .extracting("relationship", "target")
                    .contains(relationship1, Player.PLAYER);
        }

        @Test
        void testGetRelationshipTarget() {
            var entityId = world.createEntity(relation(Loves.LOVES, Player.PLAYER), relation(Loves.ADORES, Player.PLAYER2));

            assertThat(mapper1.getRelationship(entityId)).isSameAs(Loves.ADORES);
            assertThat(mapper1.getTarget(entityId)).isSameAs(Player.PLAYER2);
        }

        @Test
        void testGetRelationshipTarget_WhenNoRelations() {
            var entityId = world.createEntity();

            assertThat(mapper1.getRelationship(entityId)).isNull();
            assertThat(mapper1.getTarget(entityId)).isNull();
        }

        @Test
        void testExclusiveTrait_OverridesExistingRelation() {
            var entityId = world.createEntity(relation(Loves.LOVES, Faction.Player.PLAYER));

            verify(verify -> {
                verify.expectUpdated(entityId, type2);
                verify.expectNoMoreUpdated();

                mapper2.add(entityId, Loves.LOVES, Faction.Enemy.ENEMY);

                world.process();

                verifyDoesNotHaveComponents(entityId, type1);
                verifyHasComponents(entityId, type2);

                verifyComponentMaskDoesNotHaveComponents(entityId, type1);
                verifyComponentMaskHasComponents(entityId, type2);
            });
        }

        @Test
        void testExclusiveTrait_WhenAddedMultipleTimes_KeepsOnlyLast() {
            var entityId = world.createEntity();

            verify(verify -> {
                verify.expectUpdated(entityId, type2);
                verify.expectNoMoreUpdated();

                mapper1.add(entityId, Loves.LOVES, Faction.Player.PLAYER);
                mapper2.add(entityId, Loves.LOVES, Faction.Enemy.ENEMY);

                world.process();

                verifyDoesNotHaveComponents(entityId, type1);
                verifyHasComponents(entityId, type2);

                verifyComponentMaskDoesNotHaveComponents(entityId, type1);
                verifyComponentMaskHasComponents(entityId, type2);
            });
        }

    }

}
