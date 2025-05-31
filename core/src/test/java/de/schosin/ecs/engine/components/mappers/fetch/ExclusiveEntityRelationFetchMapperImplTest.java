package de.schosin.ecs.engine.components.mappers.fetch;

import static de.schosin.ecs.api.components.types.ComponentType.componentSet;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import de.schosin.ecs.api.components.Relation;
import de.schosin.ecs.api.components.mappers.EntityFetchRelations.ExclusiveEntityRelationFetchMapper;
import de.schosin.ecs.api.components.types.ComponentType;
import de.schosin.ecs.engine.components.mappers.AbstractMapperTest;
import de.schosin.ecs.engine.utils.components.ComponentSetsHelperTest.MyComponentSet;
import de.schosin.ecs.engine.utils.components.ComponentSetsHelperTest.Position;
import de.schosin.ecs.engine.utils.components.ComponentSetsHelperTest.Velocity;

class ExclusiveEntityRelationFetchMapperImplTest extends AbstractMapperTest {

    ExclusiveEntityRelationFetchMapper<Parent, MyComponentSet> mapper;

    @BeforeEach
    void setupMapper() {
        this.mapper = world.getComponents(ComponentType.exclusiveRelation(Parent.class, componentSet(MyComponentSet.class)));
    }

    @Nested
    class HasTest {

        @Test
        void testHas() {
            var parentPos = new Position();
            var parentVelocity = new Velocity();
            var parent1 = world.createEntity(parentPos, parentVelocity);
            var parent2 = world.createEntity();

            var entity1 = world.createEntity(Relation.create(Parent.Parent, parent1));
            var entity2 = world.createEntity(Relation.create(Parent.Parent, parent2));
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
        void testNoParent() {
            var entityId = world.createEntity();

            // Verify
            var relation = mapper.get(entityId);
            assertThat(relation).isNull();
        }

        @Test
        void testParentHasAllComponents() {
            var parentPos = new Position();
            var parentVelocity = new Velocity();
            var parentId = world.createEntity(parentPos, parentVelocity);

            var entityId = world.createEntity(Relation.create(Parent.Parent, parentId));

            // Verify
            var relation = mapper.get(entityId);
            assertThat(relation).isNotNull();
            assertThat(relation.target()).isEqualTo(parentId);

            var data = relation.data();
            assertThat(data).isNotNull();
            assertThat(data.pos()).isSameAs(parentPos);
            assertThat(data.velocity()).isSameAs(parentVelocity);
        }

        @Test
        void testParentHasSomeComponents() {
            var parentVelocity = new Velocity();
            var parentId = world.createEntity(parentVelocity);

            var entityId = world.createEntity(Relation.create(Parent.Parent, parentId));

            // Verify
            var relation = mapper.get(entityId);
            assertThat(relation).isNotNull();
            assertThat(relation.target()).isEqualTo(parentId);

            var data = relation.data();
            assertThat(data).isNotNull();
            assertThat(data.pos()).isNull();
            assertThat(data.velocity()).isSameAs(parentVelocity);
        }

        @Test
        void testParentHasNoComponents() {
            var parentId = world.createEntity();

            var entityId = world.createEntity(Relation.create(Parent.Parent, parentId));

            // Verify
            var relation = mapper.get(entityId);
            assertThat(relation).isNotNull();
            assertThat(relation.target()).isEqualTo(parentId);

            var data = relation.data();
            assertThat(data).isNull();
        }

    }

    @Nested
    class RemoveTest {

        @Test
        void testRemoveRelation_EntityChanged() {
            var parentPos = new Position();
            var parentVelocity = new Velocity();
            var parentId = world.createEntity(parentPos, parentVelocity);

            var entityId = world.createEntity(new Position(), Relation.create(Parent.Parent, parentId));

            verify(verify -> {
                verify.expectUpdated(entityId, Position.class);
                verify.expectNoMoreUpdated();
                verify.expectNoMoreRemoved();

                // Call
                mapper.remove(entityId);
                world.process();

                // Verify
                verifyHasComponents(entityId, Position.class);
                verifyDoesNotHaveComponents(entityId, exclusiveRelation(Parent.class));

                verifyComponentMaskHasComponents(entityId, Position.class);
                verifyComponentMaskDoesNotHaveComponents(entityId, exclusiveRelation(Parent.class));
            });
        }

        @Test
        void testRemoveRelation_ParentUnchanged() {
            var parentPos = new Position();
            var parentVelocity = new Velocity();
            var parentId = world.createEntity(parentPos, parentVelocity);

            var entityId = world.createEntity(new Position(), Relation.create(Parent.Parent, parentId));

            verify(verify -> {
                verify.expectUpdated(entityId, Position.class);
                verify.expectNoMoreUpdated();
                verify.expectNoMoreRemoved();

                // Call
                mapper.remove(entityId);
                world.process();

                // Verify
                verifyHasComponents(parentId, Position.class, Velocity.class);
                verifyComponentMaskHasComponents(parentId, Position.class, Velocity.class);
            });
        }

    }

}
