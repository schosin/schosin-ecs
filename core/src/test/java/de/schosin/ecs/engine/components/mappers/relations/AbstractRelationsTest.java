package de.schosin.ecs.engine.components.mappers.relations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThatCode;

import java.util.ArrayList;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.schosin.ecs.api.components.Relation;
import de.schosin.ecs.api.components.mappers.Components;
import de.schosin.ecs.api.components.types.ComponentType;
import de.schosin.ecs.api.components.types.RelationComponentType;
import de.schosin.ecs.api.components.types.RelationComponentType.ExclusiveComponentRelationType;
import de.schosin.ecs.engine.AbstractWorldTest;
import de.schosin.ecs.engine.events.builtin.EntityEvent.EntityUpdatedEvent;

public abstract class AbstractRelationsTest extends AbstractWorldTest {

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
        void testAddRelationship_UpdatesArchetype() {
            var entityId = world.createEntity();
            var archetype = entityManager.getArchetype(entityId);

            // Call
            add1(mapper1, entityId, relationship1);
            world.process();

            // Verify
            var updatedArchetype = entityManager.getArchetype(entityId);
            assertThat(updatedArchetype).isNotSameAs(archetype);
            assertThat(updatedArchetype.getComponents()).hasSize(1);
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
        void testMultipleRelations_WhenNotProcessed_DoesNotUpdateArchetype() {
            var entityId = world.createEntity();

            verify(verify -> {
                verify.expectNoMoreUpdated();

                add1(mapper1, entityId, relationship1);
                add2(mapper2, entityId, relationship2);

                if (type1 instanceof ExclusiveComponentRelationType<?, ?>) {
                    verifyDoesNotHaveComponents(entityId, type1);
                    verifyHasComponents(entityId, type2);
                } else {
                    verifyHasComponents(entityId, type1, type2);
                }
                verifyArchetypeDoesNotHaveComponents(entityId, type1, type2);
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
                verifyArchetypeDoesNotHaveComponents(entityId, type1);
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
                verifyArchetypeHasComponents(entityId, type1);
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
