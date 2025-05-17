package de.schosin.ecs.engine.components;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.util.ArrayList;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import de.schosin.ecs.api.components.ComponentType.RegularComponentRelationType;
import de.schosin.ecs.api.components.Components;
import de.schosin.ecs.api.components.Components.ComponentRelationMapper;
import de.schosin.ecs.api.components.Components.ExclusiveComponentRelationMapper;
import de.schosin.ecs.api.components.Relation;
import de.schosin.ecs.api.components.Relation.ComponentRelation;
import de.schosin.ecs.api.components.Relation.Exclusive;
import de.schosin.ecs.engine.AbstractWorldTest;
import de.schosin.ecs.engine.components.RelationMapperManagerTest.Faction.Enemy;
import de.schosin.ecs.engine.components.RelationMapperManagerTest.Faction.Player;
import de.schosin.ecs.engine.events.builtin.EntityEvent.EntityUpdatedEvent;

class RelationMapperManagerTest extends AbstractWorldTest {

    @Nested
    class ComponentRelationMapperTest extends AbstractTest<Hates, Player, Enemy, ComponentRelationMapper<Hates, Player>, ComponentRelationMapper<Hates, Enemy>> {

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

        @Override
        @SuppressWarnings({ "unchecked", "rawtypes" })
        protected <RR, T> ComponentRelation<RR, T> getInstance(Components<ComponentRelation<RR, T>, ?> mapper, RR relationship, T target) {
            return ((ComponentRelationMapper) mapper).getInstance(relationship, target);
        }

        @Test
        void testMultipleRelations() {
            var entityId = world.createEntity();

            verify(verify -> {
                verify.expectUpdated(entityId, type1, type2);
                verify.expectNoMoreUpdated();

                add(mapper1, entityId, relationship, target1);
                add(mapper2, entityId, relationship, target2);

                world.process();

                verifyHasComponents(entityId, type1, type2);
                verifyComponentMaskHasComponents(entityId, type1, type2);
            });
        }

        @Test
        void testGet() {
            var entityId = world.createEntity(mapper1.getInstance(Hates.HATES, Player.PLAYER), mapper1.getInstance(Hates.DESPISES, Player.PLAYER2));

            var relations = mapper1.get(entityId);
            assertThat(relations)
                    .extracting("relationship", "target")
                    .containsExactlyInAnyOrder(
                            tuple(Hates.HATES, Player.PLAYER),
                            tuple(Hates.DESPISES, Player.PLAYER2));
        }

        @Test
        void testGetRelationship() {
            var entityId = world.createEntity(mapper1.getInstance(Hates.HATES, Player.PLAYER), mapper1.getInstance(Hates.DESPISES, Player.PLAYER2));

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

    @Nested
    class ExclusiveComponentRelationMapperTest extends AbstractTest<Loves, Player, Enemy, ExclusiveComponentRelationMapper<Loves, Player>, ExclusiveComponentRelationMapper<Loves, Enemy>> {

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

        @Override
        @SuppressWarnings({ "unchecked", "rawtypes" })
        protected <RR, T> ComponentRelation<RR, T> getInstance(Components<ComponentRelation<RR, T>, ?> mapper, RR relationship, T target) {
            return ((ExclusiveComponentRelationMapper) mapper).getInstance((Exclusive) relationship, target);
        }

        @Test
        void testGet() {
            var entityId = world.createEntity(mapper1.getInstance(relationship, Player.PLAYER), mapper1.getInstance(relationship, Player.PLAYER2));

            var relation = mapper1.get(entityId);
            assertThat(relation)
                    .extracting("relationship", "target")
                    .contains(relationship, Player.PLAYER2);
        }

        @Test
        void testGet_WhenMultiple_KeepsLast() {
            var entity1 = world.createEntity(mapper1.getInstance(relationship, Player.PLAYER), mapper1.getInstance(relationship, Player.PLAYER2));
            var entity2 = world.createEntity(mapper1.getInstance(relationship, Player.PLAYER2), mapper1.getInstance(relationship, Player.PLAYER));

            assertThat(mapper1.get(entity1))
                    .extracting("relationship", "target")
                    .contains(relationship, Player.PLAYER2);

            assertThat(mapper1.get(entity2))
                    .extracting("relationship", "target")
                    .contains(relationship, Player.PLAYER);
        }

        @Test
        void testGetRelationshipTarget() {
            var entityId = world.createEntity(mapper1.getInstance(Loves.LOVES, Player.PLAYER), mapper1.getInstance(Loves.ADORES, Player.PLAYER2));

            assertThat(mapper1.getRelationship(entityId)).isSameAs(Loves.ADORES);
            assertThat(mapper1.getTarget(entityId)).isSameAs(Player.PLAYER2);
        }

        @Test
        void testGetRelation_WhenNoRelations() {
            var entityId = world.createEntity();

            assertThat(mapper1.getRelationship(entityId)).isNull();
            assertThat(mapper1.getTarget(entityId)).isNull();
        }

        @Test
        void testExclusiveTrait_OverridesExistingRelation() {
            var entityId = world.createEntity(mapper1.getInstance(Loves.LOVES, Faction.Player.PLAYER));

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

    abstract class AbstractTest<R extends Enum<R>, T1 extends Enum<T1>, T2 extends Enum<T2>, M1 extends Components<ComponentRelation<R, T1>, ?>, M2 extends Components<ComponentRelation<R, T2>, ?>> {

        protected final RegularComponentRelationType<R, T1, ?> type1;
        protected final RegularComponentRelationType<R, T2, ?> type2;

        protected final Class<R> relationshipClass;
        protected final Class<T1> target1Class;
        protected final Class<T2> target2Class;

        protected R relationship;
        protected T1 target1;
        protected T2 target2;

        protected M1 mapper1;
        protected M2 mapper2;

        protected AbstractTest(RegularComponentRelationType<R, T1, ?> type1, RegularComponentRelationType<R, T2, ?> type2) {
            this.type1 = type1;
            this.type2 = type2;

            this.relationshipClass = type1.relationship();
            this.target1Class = type1.target();
            this.target2Class = type2.target();

            this.relationship = relationshipClass.getEnumConstants()[0];
            this.target1 = target1Class.getEnumConstants()[0];
            this.target2 = target2Class.getEnumConstants()[0];
        }

        @BeforeEach
        void setupMappers() {
            this.mapper1 = createMapper1();
            this.mapper2 = createMapper2();
        }

        protected abstract M1 createMapper1();

        protected abstract M2 createMapper2();

        protected abstract <RR, T> void add(Components<ComponentRelation<RR, T>, ?> mapper, int entityId, RR relationship, T target);

        protected abstract <RR, T> ComponentRelation<RR, T> getInstance(Components<ComponentRelation<RR, T>, ?> mapper, RR relationship, T target);

        @Test
        void testCaching() {
            // Verify
            assertThat(createMapper1()).isSameAs(mapper1);
            assertThat(createMapper1()).isNotSameAs(mapper2);

            assertThat(createMapper2()).isNotSameAs(mapper1);
            assertThat(createMapper2()).isSameAs(mapper2);
        }

        @Test
        void testAddRelationship_UpdatesComponentMask() {
            var entityId = world.createEntity();
            var componentMask = entityManager.getComponentMask(entityId);

            // Call
            add(mapper1, entityId, relationship, target1);
            world.process();

            // Verify
            var updatedComponentMask = entityManager.getComponentMask(entityId);
            assertThat(updatedComponentMask).isNotSameAs(componentMask);
            assertThat(updatedComponentMask.getComponents()).hasSize(1);
        }

        @Test
        void testAddRelationship_DoesNotAddRelationOrTargetAsRegularComponents() {
            var entityId = world.createEntity();

            // Call
            add(mapper1, entityId, relationship, target1);
            world.process();

            // Verify
            verifyDoesNotHaveComponents(entityId, relationshipClass);
            verifyDoesNotHaveComponents(entityId, target1Class);
        }

        @Test
        void testAddRelationship() {
            var entityId = world.createEntity();

            verify(verify -> {
                verify.expectUpdated(entityId, type1);
                verify.expectNoMoreUpdated();

                add(mapper1, entityId, relationship, target1);

                world.process();
            });
        }

        @Test
        void testAddRelationship_TriggersUpdate() {
            var entityId = world.createEntity();

            var updated = new ArrayList<Integer>();
            eventManager.registerEventHandler(EntityUpdatedEvent.class, event -> updated.add(event.entityId()));

            // Call
            add(mapper1, entityId, relationship, target1);
            world.process();

            // Verify
            assertThat(updated).containsExactly(entityId);
        }

        @Test
        void testAddRelationship_WhenAlreadyPresent_DoesNotTriggerUpdate() {
            var entityId = world.createEntity(getInstance(mapper1, relationship, target1));

            var updated = new ArrayList<Integer>();
            eventManager.registerEventHandler(EntityUpdatedEvent.class, event -> updated.add(event.entityId()));

            // Call
            add(mapper1, entityId, relationship, target1);
            world.process();

            // Verify
            assertThat(updated).isEmpty();
        }

        @Test
        void testRemoveRelationship_TriggersUpdate() {
            var entityId = world.createEntity(getInstance(mapper1, relationship, target1));

            var updated = new ArrayList<Integer>();
            eventManager.registerEventHandler(EntityUpdatedEvent.class, event -> updated.add(event.entityId()));

            // Call
            mapper1.remove(entityId);
            world.process();

            // Verify
            assertThat(updated).containsExactly(entityId);
        }

        @Test
        void testRemoveRelationship_WhenNotAddedBefore_DoesNotTriggerCompositionRemoved() {
            var entityId = world.createEntity();

            var updated = new ArrayList<Integer>();
            eventManager.registerEventHandler(EntityUpdatedEvent.class, event -> updated.add(event.entityId()));

            // Call
            mapper1.remove(entityId);
            world.process();

            // Verify
            assertThat(updated).isEmpty();
        }

        @Test
        void testMultipleRelations_WhenNotProcessed_DoesNotUpdateMask() {
            var entityId = world.createEntity();

            verify(verify -> {
                verify.expectNoMoreUpdated();

                add(mapper1, entityId, relationship, target1);
                add(mapper2, entityId, relationship, target2);

                verifyHasComponents(entityId, type1, type2);
                verifyComponentMaskDoesNotHaveComponents(entityId, type1, type2);
            });
        }

        @Test
        void testHasComponentRelation_WhenEntityCreatedWithRelation() {
            var relation = getInstance(mapper1, relationship, target1);

            verify(verify -> {
                verify.expectInserted(type1);
                verify.expectNoMoreInserted();

                var entityId = world.createEntity(relation);

                assertThat(mapper1.has(entityId)).isTrue();
            });

        }

        @Test
        void testHasComponentRelation_WhenEntityModified_DoesNotHaveIfNotProcessed() {
            var entityId = world.createEntity();

            verify(verify -> {
                verify.expectNoMoreUpdated();

                add(mapper1, entityId, relationship, target1);

                assertThat(mapper1.has(entityId)).isTrue();
                verifyHasComponents(entityId, type1);
                verifyComponentMaskDoesNotHaveComponents(entityId, type1);
            });
        }

        @Test
        void testHasComponentRelation_WhenEntityModified_HasWhenProcessed() {
            var entityId = world.createEntity();

            verify(verify -> {
                verify.expectUpdated(entityId, type1);
                verify.expectNoMoreUpdated();

                add(mapper1, entityId, relationship, target1);
                world.process();

                assertThat(mapper1.has(entityId)).isTrue();
                verifyHasComponents(entityId, type1);
                verifyComponentMaskHasComponents(entityId, type1);
            });
        }

    }

    interface EnumComponent {
    }

    public enum Hates implements EnumComponent {
        HATES, DESPISES
    }

    public enum Loves implements Relation.Exclusive, EnumComponent {
        LOVES, ADORES
    }

    public sealed interface Faction {
        enum Player implements Faction {
            PLAYER, PLAYER2, PLAYER3
        }

        enum Enemy implements Faction {
            ENEMY, ENEMY2
        }
    }

    class RegularComponent {
    }

    class RelationshipComponent implements Relation.Relationship {
    }

    class TargetComponent implements Relation.Target {
    }

}
