package de.schosin.ecs.engine.components.mappers;

import static de.schosin.ecs.api.components.types.ComponentType.componentSet;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import de.schosin.ecs.api.components.Relation;
import de.schosin.ecs.api.components.mappers.ComponentSetMapper;
import de.schosin.ecs.engine.utils.components.ComponentSetsHelperTest.Birthplace;
import de.schosin.ecs.engine.utils.components.ComponentSetsHelperTest.MyComponentSet;
import de.schosin.ecs.engine.utils.components.ComponentSetsHelperTest.Position;
import de.schosin.ecs.engine.utils.components.ComponentSetsHelperTest.RelationComponentSet;
import de.schosin.ecs.engine.utils.components.ComponentSetsHelperTest.Velocity;

class ComponentSetComponentsImplTest extends AbstractMapperTest {

    ComponentSetMapper<MyComponentSet> mapper;

    @BeforeEach
    void setupMapper() {
        this.mapper = world.getComponents(componentSet(MyComponentSet.class));
    }

    @Nested
    class HasTest {

        @Test
        void testEmptyEntity() {
            var entityId = world.createEntity();

            assertThat(mapper.has(entityId)).isFalse();
        }

        @Test
        void testAllComponents() {
            var entityId = world.createEntity(new Position(), new Velocity());

            assertThat(mapper.has(entityId)).isTrue();
        }

        @Test
        void testOnlyFirst() {
            var entityId = world.createEntity(new Position());

            assertThat(mapper.has(entityId)).isTrue();
        }

        @Test
        void testOnlySecond() {
            var entityId = world.createEntity(new Velocity());

            assertThat(mapper.has(entityId)).isTrue();
        }

    }

    @Nested
    class HasAllTest {

        @Test
        void testEmptyEntity() {
            var entityId = world.createEntity();

            assertThat(mapper.hasAll(entityId)).isFalse();
        }

        @Test
        void testAllComponents() {
            var entityId = world.createEntity(new Position(), new Velocity());

            assertThat(mapper.hasAll(entityId)).isTrue();
        }

        @Test
        void testOnlyFirst() {
            var entityId = world.createEntity(new Position());

            assertThat(mapper.hasAll(entityId)).isFalse();
        }

        @Test
        void testOnlySecond() {
            var entityId = world.createEntity(new Velocity());

            assertThat(mapper.hasAll(entityId)).isFalse();
        }

    }

    @Nested
    class GetTest {

        @Test
        void testEmptyEntity() {
            var entityId = world.createEntity();

            assertThat(mapper.get(entityId)).isNull();
        }

        @Test
        void testAllComponents() {
            var pos = new Position();
            var velocity = new Velocity();

            var entityId = world.createEntity(pos, velocity);

            // Verify
            var result = mapper.get(entityId);
            assertThat(result).isNotNull();
            assertThat(result.pos()).isSameAs(pos);
            assertThat(result.velocity()).isSameAs(velocity);
        }

        @Test
        void testOnlyFirst() {
            var pos = new Position();

            var entityId = world.createEntity(pos);

            // Verify
            var result = mapper.get(entityId);
            assertThat(result).isNotNull();
            assertThat(result.pos()).isSameAs(pos);
            assertThat(result.velocity()).isNull();
        }

        @Test
        void testOnlySecond() {
            var velocity = new Velocity();

            var entityId = world.createEntity(velocity);

            // Verify
            var result = mapper.get(entityId);
            assertThat(result).isNotNull();
            assertThat(result.pos()).isNull();
            assertThat(result.velocity()).isSameAs(velocity);
        }

    }

    @Nested
    class AddTest {

        @Test
        void testEmptySet() {
            var entityId = world.createEntity(new Component1());
            var components = MyComponentSet.get(-1, null, null);

            verify(verify -> {
                verify.expectNoMoreUpdated();

                // Call
                mapper.add(entityId, components);
                world.process();

                // Verify
                verifyHasComponents(entityId, Component1.class);
                verifyDoesNotHaveComponents(entityId, Position.class, Velocity.class);

                verifyComponentMaskHasComponents(entityId, Component1.class);
                verifyComponentMaskDoesNotHaveComponents(entityId, Position.class, Velocity.class);
            });
        }

        @Test
        void testAllComponents() {
            var pos = new Position();
            var velocity = new Velocity();

            var entityId = world.createEntity(new Component1());
            var components = MyComponentSet.get(-1, pos, velocity);

            verify(verify -> {
                verify.expectUpdated(entityId, Position.class, Velocity.class, Component1.class);
                verify.expectNoMoreUpdated();

                // Call
                mapper.add(entityId, components);
                world.process();

                // Verify
                verifyHasComponents(entityId, Position.class, Velocity.class, Component1.class);
                verifyComponentMaskHasComponents(entityId, Position.class, Velocity.class, Component1.class);
            });
        }

        @Test
        void testOnlyRequiredComponents() {
            var pos = new Position();

            var entityId = world.createEntity(new Component1());
            var components = MyComponentSet.get(-1, pos, null);

            verify(verify -> {
                verify.expectUpdated(entityId, Position.class, Component1.class);
                verify.expectNoMoreUpdated();

                // Call
                mapper.add(entityId, components);
                world.process();

                // Verify
                verifyHasComponents(entityId, Position.class, Component1.class);
                verifyDoesNotHaveComponents(entityId, Velocity.class);

                verifyComponentMaskHasComponents(entityId, Position.class, Component1.class);
                verifyComponentMaskDoesNotHaveComponents(entityId, Velocity.class);
            });
        }

        @Test
        void testOnlyOptionalComponents() {
            var velocity = new Velocity();

            var entityId = world.createEntity(new Component1());
            var components = MyComponentSet.get(-1, null, velocity);

            verify(verify -> {
                verify.expectUpdated(entityId, Velocity.class, Component1.class);
                verify.expectNoMoreUpdated();

                // Call
                mapper.add(entityId, components);
                world.process();

                // Verify
                verifyHasComponents(entityId, Velocity.class, Component1.class);
                verifyDoesNotHaveComponents(entityId, Position.class);

                verifyComponentMaskHasComponents(entityId, Velocity.class, Component1.class);
                verifyComponentMaskDoesNotHaveComponents(entityId, Position.class);
            });
        }

        @Test
        void testRelationComponentSet() {
            var mapper = world.getComponentSets(RelationComponentSet.class);

            var birthplace = Relation.create(Birthplace.Birthplace, new Position());
            var birthplaceId = Relation.create(Birthplace.Birthplace, 1);

            var entityId = world.createEntity(new Component1());
            var components = RelationComponentSet.get(entityId, birthplace, null, birthplaceId, null);

            verify(verify -> {
                verify.expectUpdated(entityId, exclusiveRelation(Birthplace.class, Position.class), exclusiveRelation(Birthplace.class), component(Component1.class));
                verify.expectNoMoreUpdated();

                // Call
                mapper.add(entityId, components);
                world.process();

                // Verify
                verifyHasComponents(entityId, exclusiveRelation(Birthplace.class, Position.class), exclusiveRelation(Birthplace.class), component(Component1.class));
                verifyComponentMaskHasComponents(entityId, exclusiveRelation(Birthplace.class, Position.class), exclusiveRelation(Birthplace.class), component(Component1.class));
            });
        }

    }

    @Nested
    class RemoveTest {

        @Test
        void testEmptyEntity() {
            var entityId = world.createEntity();

            verify(verify -> {
                verify.expectNoMoreUpdated();

                assertThat(mapper.remove(entityId)).isFalse();
                world.process();

                verifyDoesNotHaveComponents(entityId, Position.class, Velocity.class, Component1.class);
                verifyComponentMaskDoesNotHaveComponents(entityId, Position.class, Velocity.class, Component1.class);
            });
        }

        @Test
        void testAllComponents() {
            var entityId = world.createEntity(new Position(), new Velocity());

            verify(verify -> {
                verify.expectUpdated(entityId, NO_COMPONENTS);
                verify.expectNoMoreUpdated();

                assertThat(mapper.remove(entityId)).isTrue();
                world.process();

                verifyDoesNotHaveComponents(entityId, Position.class, Velocity.class, Component1.class);
                verifyComponentMaskDoesNotHaveComponents(entityId, Position.class, Velocity.class, Component1.class);
            });
        }

        @Test
        void testOnlyRequiredComponents() {
            var entityId = world.createEntity(new Position());

            verify(verify -> {
                verify.expectUpdated(entityId, NO_COMPONENTS);
                verify.expectNoMoreUpdated();

                assertThat(mapper.remove(entityId)).isTrue();
                world.process();

                verifyDoesNotHaveComponents(entityId, Position.class, Velocity.class, Component1.class);
                verifyComponentMaskDoesNotHaveComponents(entityId, Position.class, Velocity.class, Component1.class);
            });
        }

        @Test
        void testOnlyOptionalComponents() {
            var entityId = world.createEntity(new Velocity());

            verify(verify -> {
                verify.expectUpdated(entityId, NO_COMPONENTS);
                verify.expectNoMoreUpdated();

                assertThat(mapper.remove(entityId)).isTrue();
                world.process();

                verifyDoesNotHaveComponents(entityId, Position.class, Velocity.class, Component1.class);
                verifyComponentMaskDoesNotHaveComponents(entityId, Position.class, Velocity.class, Component1.class);
            });
        }

        @Test
        void testOtherComponents() {
            var entityId = world.createEntity(new Position(), new Velocity(), new Component1());

            verify(verify -> {
                verify.expectUpdated(entityId, Component1.class);
                verify.expectNoMoreUpdated();

                assertThat(mapper.remove(entityId)).isTrue();
                world.process();

                verifyHasComponents(entityId, Component1.class);
                verifyDoesNotHaveComponents(entityId, Position.class, Velocity.class);

                verifyComponentMaskHasComponents(entityId, Component1.class);
                verifyComponentMaskDoesNotHaveComponents(entityId, Position.class, Velocity.class);
            });
        }

        @Test
        void testDoesNotRemoveWithoutProcess() {
            var entityId = world.createEntity(new Position(), new Velocity(), new Component1());

            verify(verify -> {
                verify.expectNoMoreUpdated();

                assertThat(mapper.remove(entityId)).isTrue();

                verifyHasComponents(entityId, Position.class, Velocity.class, Component1.class);
                verifyComponentMaskHasComponents(entityId, Position.class, Velocity.class, Component1.class);
            });
        }

    }

}
