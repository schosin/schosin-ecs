package de.schosin.ecs.engine.components.mappers.relations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import de.schosin.ecs.api.components.Relation.ComponentRelation;
import de.schosin.ecs.api.components.mappers.ComponentRelationMappers.ComponentRelationMapper;
import de.schosin.ecs.api.components.mappers.Components;
import de.schosin.ecs.engine.components.mappers.relations.AbstractRelationsTest.Faction.Enemy;
import de.schosin.ecs.engine.components.mappers.relations.AbstractRelationsTest.Faction.Player;

class ComponentRelationMapperImplTest extends AbstractComponentRelationsTest {


    @Nested
    class ComponentRelationMapperTest extends AbstractComponentRelationTest<Hates, Player, Enemy, ComponentRelationMapper<Hates, Player>, ComponentRelationMapper<Hates, Enemy>> {

        public ComponentRelationMapperTest() {
            super(relation(Hates.class, Player.class), relation(Hates.class, Enemy.class));
        }

        @Override
        protected ComponentRelationMapper<Hates, Player> createMapper1() {
            return world.getComponentRelations(Hates.class, Player.class);
        }

        @Override
        protected ComponentRelationMapper<Hates, Enemy> createMapper2() {
            return world.getComponentRelations(Hates.class, Enemy.class);
        }

        @Override
        @SuppressWarnings({ "unchecked", "rawtypes" })
        protected <RR, T> void add(Components<ComponentRelation<RR, T>, ?> mapper, int entityId, RR relationship, T target) {
            ((ComponentRelationMapper) mapper).add(entityId, relationship, target);
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
            var entityId = world.createEntity(relation(Hates.HATES, Player.PLAYER), relation(Hates.DESPISES, Player.PLAYER2));

            var relations = mapper1.get(entityId);
            assertThat(relations)
                    .extracting("relationship", "target")
                    .containsExactlyInAnyOrder(
                            tuple(Hates.HATES, Player.PLAYER),
                            tuple(Hates.DESPISES, Player.PLAYER2));
        }

        @Test
        void testGetRelationship() {
            var entityId = world.createEntity(relation(Hates.HATES, Player.PLAYER), relation(Hates.DESPISES, Player.PLAYER2));

            assertThat(mapper1.getRelationship(entityId, Player.PLAYER)).isSameAs(Hates.HATES);
            assertThat(mapper1.getRelationship(entityId, Player.PLAYER2)).isSameAs(Hates.DESPISES);
            assertThat(mapper1.getRelationship(entityId, Player.PLAYER3)).isNull();
        }

        @Test
        void testGetRelationship_WhenNoRelations() {
            var entityId = world.createEntity();

            assertThat(mapper1.getRelationship(entityId, Player.PLAYER)).isNull();
            assertThat(mapper1.getRelationship(entityId, Player.PLAYER2)).isNull();
            assertThat(mapper1.getRelationship(entityId, Player.PLAYER3)).isNull();
        }

    }

}
