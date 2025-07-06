package de.schosin.ecs.engine.components.mappers.relations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import de.schosin.ecs.api.components.Relation.ComponentRelation;
import de.schosin.ecs.api.components.mappers.ComponentRelationMappers.ExclusiveComponentRelationMapper;
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
            return world.getComponents(exclusiveRelation(Loves.class, Player.class));
        }

        @Override
        protected ExclusiveComponentRelationMapper<Loves, Enemy> createMapper2() {
            return world.getComponents(exclusiveRelation(Loves.class, Enemy.class));
        }

        @Override
        @SuppressWarnings({ "unchecked", "rawtypes" })
        protected <RR, T> void add(Components<ComponentRelation<RR, T>, ?> mapper, int entityId, RR relationship, T target) {
            ((ExclusiveComponentRelationMapper) mapper).add(entityId, relationship, target);
        }

        @Test
        void testCreateEntity_SameRelationship_Throws() {
            assertThatThrownBy(() -> world.createEntity(relation(relationship1, Player.PLAYER), relation(relationship1, Player.PLAYER2)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContainingAll("ComponentRelations", relationship1.getClass().getSimpleName());
        }

        @Test
        void testGetRelationshipTarget() {
            var entityId = world.createEntity(relation(Loves.ADORES, Player.PLAYER2));

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

                verifyArchetypeDoesNotHaveComponents(entityId, type1);
                verifyArchetypeHasComponents(entityId, type2);
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

                verifyArchetypeDoesNotHaveComponents(entityId, type1);
                verifyArchetypeHasComponents(entityId, type2);
            });
        }

    }

}
