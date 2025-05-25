package de.schosin.ecs.buildtools.codegen.it;

import static de.schosin.ecs.api.components.types.ComponentType.componentSet;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import de.schosin.ecs.api.World;
import de.schosin.ecs.api.components.ComponentSet;
import de.schosin.ecs.api.components.Relation;
import de.schosin.ecs.buildtools.codegen.it.components.Acceleration;
import de.schosin.ecs.buildtools.codegen.it.components.DockedTo;
import de.schosin.ecs.buildtools.codegen.it.components.Location;
import de.schosin.ecs.buildtools.codegen.it.components.Position;
import de.schosin.ecs.buildtools.codegen.it.components.Velocity;
import de.schosin.ecs.test.AbstractEcsTest;

public class ComponentSetComponentsTest extends AbstractEcsTest<World> {

    @Nested
    class ComponentMapperTest {

        @ParameterizedTest
        @ValueSource(classes = {
                AlliedWithComponentSet.class, DockedToComponentSet.class, LocationComponentSet.class,
                PhysicsComponentSet.class, RecordPhysicsComponentsSet.class, RelationRecordComponentsSet.class
        })
        void testSameInstanceReturned(Class<? extends ComponentSet> clazz) {
            var type = componentSet(clazz);
            var mapper = componentMapperManager.getComponentSets(type);

            assertThat(componentMapperManager.getComponentSets(type)).isSameAs(mapper);
        }

        @Nested
        class PhysicsComponentsSetTest {

            @Test
            void testHasAny() {
                var type = componentSet(PhysicsComponentSet.class);
                var mapper = componentMapperManager.getComponentSets(type);

                // Create entities
                var entity1 = world.createEntity();
                var entity2 = world.createEntity(new Position());
                var entity3 = world.createEntity(new Velocity());
                var entity4 = world.createEntity(new Position(), new Velocity());

                // Call
                assertThat(mapper.has(entity1)).isFalse();
                assertThat(mapper.has(entity2)).isTrue();
                assertThat(mapper.has(entity3)).isTrue();
                assertThat(mapper.has(entity4)).isTrue();
            }

            @Test
            void testHasAll() {
                var type = componentSet(PhysicsComponentSet.class);
                var mapper = componentMapperManager.getComponentSets(type);

                // Create entities
                var entity1 = world.createEntity();
                var entity2 = world.createEntity(new Position());
                var entity3 = world.createEntity(new Velocity());
                var entity4 = world.createEntity(new Position(), new Velocity());

                // Call
                assertThat(mapper.hasAll(entity1)).isFalse();
                assertThat(mapper.hasAll(entity2)).isFalse();
                assertThat(mapper.hasAll(entity3)).isFalse();
                assertThat(mapper.hasAll(entity4)).isTrue();
            }

            @Test
            void testGet() {
                var type = componentSet(PhysicsComponentSet.class);
                var mapper = componentMapperManager.getComponentSets(type);

                // Create entity
                var pos = new Position();
                var velocity = new Velocity();

                // Create entities
                var entity1 = world.createEntity();
                var entity2 = world.createEntity(new Velocity());
                var entity3 = world.createEntity(new Position());
                var entity4 = world.createEntity(pos, velocity);

                // Verify
                assertThat(mapper.get(entity1)).isNull();

                var components2 = mapper.get(entity2);
                assertThat(components2);
                assertThat(components2.entityId()).isEqualTo(entity2);
                assertThat(components2.pos()).isNull();
                assertThat(components2.velocity()).isNotNull();

                var components3 = mapper.get(entity3);
                assertThat(components3);
                assertThat(components3.entityId()).isEqualTo(entity3);
                assertThat(components3.pos()).isNotNull();
                assertThat(components3.velocity()).isNull();

                var components4 = mapper.get(entity4);
                assertThat(components4);
                assertThat(components4.entityId()).isEqualTo(entity4);
                assertThat(components4.pos()).isSameAs(pos);
                assertThat(components4.velocity()).isSameAs(velocity);
            }

            @Test
            void testGet_InstanceReusedAfterProcess() {
                var type = componentSet(PhysicsComponentSet.class);
                var mapper = componentMapperManager.getComponentSets(type);

                // Create entities
                var entity1 = world.createEntity(new Position(), new Velocity());
                var entity2 = world.createEntity(new Position(), new Velocity());

                // Get first
                var components1 = mapper.get(entity1);
                assertThat(components1);
                assertThat(components1.entityId()).isEqualTo(entity1);
                assertThat(components1.pos()).isNotNull();
                assertThat(components1.velocity()).isNotNull();

                // Process
                world.process();

                assertThat(components1.entityId()).isEqualTo(-1);
                assertThat(components1.pos()).isNull();
                assertThat(components1.velocity()).isNull();

                // Get second
                var components2 = mapper.get(entity2);
                assertThat(components2).isSameAs(components1);
                assertThat(components2.entityId()).isEqualTo(entity2);
                assertThat(components2.pos()).isNotNull();
                assertThat(components2.velocity()).isNotNull();
            }

            @Test
            void testRemove() {
                var type = componentSet(PhysicsComponentSet.class);
                var mapper = componentMapperManager.getComponentSets(type);

                // Create entities
                var entity1 = world.createEntity(new Acceleration());
                var entity2 = world.createEntity(new Position(), new Acceleration());
                var entity3 = world.createEntity(new Velocity());
                var entity4 = world.createEntity(new Position(), new Velocity());

                verify(verify -> {
                    verify.expectUpdated(entity2, Acceleration.class);
                    verify.expectUpdated(entity3, NO_COMPONENTS);
                    verify.expectUpdated(entity4, NO_COMPONENTS);
                    verify.expectNoMoreUpdated();

                    // Call
                    assertThat(mapper.remove(entity1)).isFalse();
                    assertThat(mapper.remove(entity2)).isTrue();
                    assertThat(mapper.remove(entity3)).isTrue();
                    assertThat(mapper.remove(entity4)).isTrue();

                    world.process();

                    // Verify
                    verifyDoesNotHaveComponents(entity1, Position.class, Velocity.class);
                    verifyDoesNotHaveComponents(entity2, Position.class, Velocity.class);
                    verifyDoesNotHaveComponents(entity3, Position.class, Velocity.class);
                    verifyDoesNotHaveComponents(entity4, Position.class, Velocity.class);
                });
            }

        }

        @Nested
        class RecordPhysicsComponentsSetTest {

            @Test
            void testHasAny() {
                var type = componentSet(RecordPhysicsComponentsSet.class);
                var mapper = componentMapperManager.getComponentSets(type);

                // Create entities
                var entity1 = world.createEntity();
                var entity2 = world.createEntity(new Position());
                var entity3 = world.createEntity(new Velocity());
                var entity4 = world.createEntity(new Position(), new Velocity());

                // Call
                assertThat(mapper.has(entity1)).isFalse();
                assertThat(mapper.has(entity2)).isTrue();
                assertThat(mapper.has(entity3)).isTrue();
                assertThat(mapper.has(entity4)).isTrue();
            }

            @Test
            void testHasAll() {
                var type = componentSet(RecordPhysicsComponentsSet.class);
                var mapper = componentMapperManager.getComponentSets(type);

                // Create entities
                var entity1 = world.createEntity();
                var entity2 = world.createEntity(new Position());
                var entity3 = world.createEntity(new Velocity());
                var entity4 = world.createEntity(new Position(), new Velocity());

                // Call
                assertThat(mapper.hasAll(entity1)).isFalse();
                assertThat(mapper.hasAll(entity2)).isFalse();
                assertThat(mapper.hasAll(entity3)).isFalse();
                assertThat(mapper.hasAll(entity4)).isTrue();
            }

            @Test
            void testGet() {
                var type = componentSet(RecordPhysicsComponentsSet.class);
                var mapper = componentMapperManager.getComponentSets(type);

                // Create entity
                var pos = new Position();
                var velocity = new Velocity();

                // Create entities
                var entity1 = world.createEntity();
                var entity2 = world.createEntity(new Velocity());
                var entity3 = world.createEntity(new Position());
                var entity4 = world.createEntity(pos, velocity);

                // Verify
                assertThat(mapper.get(entity1)).isNull();

                var components2 = mapper.get(entity2);
                assertThat(components2);
                assertThat(components2.entityId()).isEqualTo(entity2);
                assertThat(components2.pos()).isNull();
                assertThat(components2.velocity()).isNotNull();

                var components3 = mapper.get(entity3);
                assertThat(components3);
                assertThat(components3.entityId()).isEqualTo(entity3);
                assertThat(components3.pos()).isNotNull();
                assertThat(components3.velocity()).isNull();

                var components4 = mapper.get(entity4);
                assertThat(components4);
                assertThat(components4.entityId()).isEqualTo(entity4);
                assertThat(components4.pos()).isSameAs(pos);
                assertThat(components4.velocity()).isSameAs(velocity);
            }

            @Test
            void testGet_InstanceReusedAfterProcess() {
                var type = componentSet(RecordPhysicsComponentsSet.class);
                var mapper = componentMapperManager.getComponentSets(type);

                // Create entities
                var entity1 = world.createEntity(new Position(), new Velocity());
                var entity2 = world.createEntity(new Position(), new Velocity());

                // Get first
                var components1 = mapper.get(entity1);
                assertThat(components1);
                assertThat(components1.entityId()).isEqualTo(entity1);
                assertThat(components1.pos()).isNotNull();
                assertThat(components1.velocity()).isNotNull();

                // Process
                world.process();

                assertThat(components1.entityId()).isEqualTo(-1);
                assertThat(components1.pos()).isNull();
                assertThat(components1.velocity()).isNull();

                // Get second
                var components2 = mapper.get(entity2);
                assertThat(components2).isSameAs(components1);
                assertThat(components2.entityId()).isEqualTo(entity2);
                assertThat(components2.pos()).isNotNull();
                assertThat(components2.velocity()).isNotNull();
            }

            @Test
            void testRemove() {
                var type = componentSet(RecordPhysicsComponentsSet.class);
                var mapper = componentMapperManager.getComponentSets(type);

                // Create entities
                var entity1 = world.createEntity(new Acceleration());
                var entity2 = world.createEntity(new Position(), new Acceleration());
                var entity3 = world.createEntity(new Velocity());
                var entity4 = world.createEntity(new Position(), new Velocity());

                verify(verify -> {
                    verify.expectUpdated(entity2, Acceleration.class);
                    verify.expectUpdated(entity3, NO_COMPONENTS);
                    verify.expectUpdated(entity4, NO_COMPONENTS);
                    verify.expectNoMoreUpdated();

                    // Call
                    assertThat(mapper.remove(entity1)).isFalse();
                    assertThat(mapper.remove(entity2)).isTrue();
                    assertThat(mapper.remove(entity3)).isTrue();
                    assertThat(mapper.remove(entity4)).isTrue();

                    world.process();

                    // Verify
                    verifyDoesNotHaveComponents(entity1, Position.class, Velocity.class);
                    verifyDoesNotHaveComponents(entity2, Position.class, Velocity.class);
                    verifyDoesNotHaveComponents(entity3, Position.class, Velocity.class);
                    verifyDoesNotHaveComponents(entity4, Position.class, Velocity.class);
                });
            }

        }

        @Nested
        class RelationRecordComponentsTest {

            @Test
            void testGet() {
                var type = componentSet(RelationRecordComponentsSet.class);
                var mapper = world.getComponents(type);

                var earth = world.createEntity();

                var dockedTo = Relation.create(DockedTo.DockerTo, earth);
                var startPos = Relation.create(Location.Start.Start, new Position(1, 2));
                var endPos = Relation.create(Location.End.End, new Position(10, 20));

                // Create entities
                var entity1 = world.createEntity(dockedTo, startPos, endPos);

                // Verify
                var components1 = mapper.get(entity1);
                assertThat(components1);
                assertThat(components1.entityId()).isEqualTo(entity1);
                assertThat(components1.dockedTo()).isSameAs(dockedTo);
                assertThat(components1.startPos()).isSameAs(startPos);
                assertThat(components1.endPos()).isSameAs(endPos);
            }

        }

    }

}
