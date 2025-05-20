package de.schosin.ecs.engine.components;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.assertj.core.api.Assumptions.assumeThatCode;

import java.util.ArrayList;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import de.schosin.ecs.api.components.ComponentType;
import de.schosin.ecs.api.components.ComponentType.RegularComponentRelationType;
import de.schosin.ecs.api.components.ComponentType.RegularEntityRelationType;
import de.schosin.ecs.api.components.ComponentType.RelationComponentType;
import de.schosin.ecs.api.components.Components;
import de.schosin.ecs.api.components.Components.ComponentRelationMapper;
import de.schosin.ecs.api.components.Components.EntityRelationMapper;
import de.schosin.ecs.api.components.Components.ExclusiveComponentRelationMapper;
import de.schosin.ecs.api.components.Components.ExclusiveEntityRelationMapper;
import de.schosin.ecs.api.components.Relation;
import de.schosin.ecs.api.components.Relation.ComponentRelation;
import de.schosin.ecs.api.components.Relation.EntityRelation;
import de.schosin.ecs.api.components.Relation.Exclusive;
import de.schosin.ecs.engine.AbstractWorldTest;
import de.schosin.ecs.engine.components.RelationMapperManagerTest.Faction.Enemy;
import de.schosin.ecs.engine.components.RelationMapperManagerTest.Faction.Player;
import de.schosin.ecs.engine.events.builtin.EntityEvent.EntityUpdatedEvent;

class RelationMapperManagerTest extends AbstractWorldTest {

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

    abstract class AbstractComponentRelationTest<R extends Enum<R>, T1 extends Enum<T1>, T2 extends Enum<T2>, M1 extends Components<ComponentRelation<R, T1>, ?>, M2 extends Components<ComponentRelation<R, T2>, ?>>
            extends AbstractRelationTest<R, R, RegularComponentRelationType<R, T1, ?>, RegularComponentRelationType<R, T2, ?>, ComponentRelation<R, T1>, M1, ComponentRelation<R, T2>, M2> {

        protected final Class<T1> target1Class;
        protected final Class<T2> target2Class;

        protected T1 target1;
        protected T2 target2;

        protected AbstractComponentRelationTest(RegularComponentRelationType<R, T1, ?> type1, RegularComponentRelationType<R, T2, ?> type2) {
            super(type1, type2);

            this.target1Class = type1.target();
            this.target2Class = type2.target();

            this.target1 = target1Class.getEnumConstants()[0];
            this.target2 = target2Class.getEnumConstants()[0];
        }

        @Override
        protected void add1(M1 mapper, int entityId, R relationship) {
            add(mapper1, entityId, relationship, target1);
        }

        @Override
        protected void add2(M2 mapper, int entityId, R relationship) {
            add(mapper2, entityId, relationship, target2);
        }

        protected abstract <RR, T> void add(Components<ComponentRelation<RR, T>, ?> mapper, int entityId, RR relationship, T target);

        @Override
        protected ComponentRelation<R, T1> getInstance1(R relationship) {
            return Relation.create(relationship, target1);
        }

        @Override
        protected ComponentRelation<R, T2> getInstance2(R relationship) {
            return Relation.create(relationship, target2);
        }

    }

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
        void testGet() {
            var entityId = world.createEntity(relation(relationship1, target1), relation(relationship1, target2));

            var relation = mapper1.get(entityId);
            assertThat(relation)
                    .extracting("relationship", "target")
                    .contains(relationship1, target2);
        }

        @Test
        void testGet_WhenMultiple_KeepsLast() {
            var entity1 = world.createEntity(relation(relationship1, target1), relation(relationship1, target2));
            var entity2 = world.createEntity(relation(relationship1, target2), relation(relationship1, target1));

            assertThat(mapper1.get(entity1))
                    .extracting("relationship", "target")
                    .contains(relationship1, target2);

            assertThat(mapper1.get(entity2))
                    .extracting("relationship", "target")
                    .contains(relationship1, target1);
        }

        @Test
        void testGetRelationshipTarget() {
            var entityId = world.createEntity(relation(Loves.LOVES, target1), relation(Loves.ADORES, target2));

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
        void testComponentMaskUpdated_IfRelationRemovedByDeletedTarget() {
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

                verifyComponentMaskHasComponents(entityId, RegularComponent.class);
                verifyComponentMaskDoesNotHaveComponents(entityId, type1);
            });
        }

    }

    abstract class AbstractEntityRelationTest<R1 extends Enum<R1>, R2 extends Enum<R2>, M1 extends Components<EntityRelation<R1>, ?>, M2 extends Components<EntityRelation<R2>, ?>>
            extends AbstractRelationTest<R1, R2, RegularEntityRelationType<R1, ?>, RegularEntityRelationType<R2, ?>, EntityRelation<R1>, M1, EntityRelation<R2>, M2> {

        protected int target1;
        protected int target2;

        protected AbstractEntityRelationTest(RegularEntityRelationType<R1, ?> type1, RegularEntityRelationType<R2, ?> type2) {
            super(type1, type2);
        }

        @BeforeEach
        void setupTargets() {
            this.target1 = world.createEntity();
            this.target2 = world.createEntity();
        }

        @Override
        protected void add1(M1 mapper, int entityId, R1 relationship) {
            add(mapper1, entityId, relationship, target1);
        }

        @Override
        protected void add2(M2 mapper, int entityId, R2 relationship) {
            add(mapper2, entityId, relationship, target2);
        }

        protected abstract <RR> void add(Components<EntityRelation<RR>, ?> mapper, int entityId, RR relationship, int target);

        @Override
        protected EntityRelation<R1> getInstance1(R1 relationship) {
            return Relation.create(relationship, target1);
        }

        @Override
        protected EntityRelation<R2> getInstance2(R2 relationship) {
            return Relation.create(relationship, target2);
        }

    }

    abstract class AbstractRelationTest<R1 extends Enum<R1>, R2 extends Enum<R2>, TYPE1 extends RelationComponentType<R1, ?, ?>, TYPE2 extends RelationComponentType<R2, ?, ?>, X1, M1 extends Components<X1, ?>, X2, M2 extends Components<X2, ?>> {

        protected final TYPE1 type1;
        protected final TYPE2 type2;

        protected final Class<R1> relationship1Class;
        protected final R1 relationship1;

        protected final Class<R2> relationship2Class;
        protected final R2 relationship2;

        protected M1 mapper1;
        protected M2 mapper2;

        protected AbstractRelationTest(TYPE1 type1, TYPE2 type2) {
            this.type1 = type1;
            this.type2 = type2;

            this.relationship1Class = type1.relationship();
            this.relationship1 = relationship1Class.getEnumConstants()[0];

            this.relationship2Class = type2.relationship();
            this.relationship2 = relationship2Class.getEnumConstants()[0];
        }

        @BeforeEach
        void setupMappers() {
            this.mapper1 = createMapper1();
            this.mapper2 = createMapper2();
        }

        protected abstract M1 createMapper1();

        protected abstract M2 createMapper2();

        protected abstract void add1(M1 mapper, int entityId, R1 relationship);

        protected abstract void add2(M2 mapper, int entityId, R2 relationship);

        protected abstract X1 getInstance1(R1 relationship);

        protected abstract X2 getInstance2(R2 relationship);

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
            add1(mapper1, entityId, relationship1);
            world.process();

            // Verify
            var updatedComponentMask = entityManager.getComponentMask(entityId);
            assertThat(updatedComponentMask).isNotSameAs(componentMask);
            assertThat(updatedComponentMask.getComponents()).hasSize(1);
        }

        @Test
        void testAddRelationship_DoesNotAddRelationshipAsRegularComponents() {
            assumeThatCode(() -> ComponentType.component(relationship1Class)).doesNotThrowAnyException();

            var entityId = world.createEntity();

            // Call
            add1(mapper1, entityId, relationship1);
            world.process();

            // Verify
            verifyDoesNotHaveComponents(entityId, relationship1Class);
        }

        @Test
        void testAddRelationship() {
            var entityId = world.createEntity();

            verify(verify -> {
                verify.expectUpdated(entityId, type1);
                verify.expectNoMoreUpdated();

                add1(mapper1, entityId, relationship1);

                world.process();
            });
        }

        @Test
        void testAddRelationship_TriggersUpdate() {
            var entityId = world.createEntity();

            var updated = new ArrayList<Integer>();
            eventManager.registerEventHandler(EntityUpdatedEvent.class, event -> updated.add(event.entityId()));

            // Call
            add1(mapper1, entityId, relationship1);
            world.process();

            // Verify
            assertThat(updated).containsExactly(entityId);
        }

        @Test
        void testAddRelationship_WhenAlreadyPresent_DoesNotTriggerUpdate() {
            var entityId = world.createEntity(getInstance1(relationship1));

            var updated = new ArrayList<Integer>();
            eventManager.registerEventHandler(EntityUpdatedEvent.class, event -> updated.add(event.entityId()));

            // Call
            add1(mapper1, entityId, relationship1);
            world.process();

            // Verify
            assertThat(updated).isEmpty();
        }

        @Test
        void testRemoveRelationship_TriggersUpdate() {
            var entityId = world.createEntity(getInstance1(relationship1));

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

                add1(mapper1, entityId, relationship1);
                add2(mapper2, entityId, relationship2);

                verifyHasComponents(entityId, type1, type2);
                verifyComponentMaskDoesNotHaveComponents(entityId, type1, type2);
            });
        }

        @Test
        void testHasComponentRelation_WhenEntityCreatedWithRelation() {
            var relation = getInstance1(relationship1);

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

                add1(mapper1, entityId, relationship1);

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

                add1(mapper1, entityId, relationship1);
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

    public enum Hates2 implements EnumComponent {
        HATES, DESPISES
    }

    public enum Loves implements Relation.Exclusive, EnumComponent {
        LOVES, ADORES
    }

    public enum Loves2 implements Relation.Exclusive, EnumComponent {
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
