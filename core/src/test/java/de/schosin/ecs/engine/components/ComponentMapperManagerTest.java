package de.schosin.ecs.engine.components;

import static de.schosin.ecs.api.components.types.ComponentType.WILDCARD;
import static de.schosin.ecs.api.components.types.ComponentType.componentSet;
import static de.schosin.ecs.api.components.types.ComponentType.wildcard;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import de.schosin.ecs.api.Pooled;
import de.schosin.ecs.api.World;
import de.schosin.ecs.api.components.Relation;
import de.schosin.ecs.api.components.Relation.Exclusive;
import de.schosin.ecs.api.components.mappers.ComponentMapper;
import de.schosin.ecs.api.components.mappers.ComponentMapper.EnumComponentMapper;
import de.schosin.ecs.api.components.mappers.ComponentMapper.PooledComponentMapper;
import de.schosin.ecs.api.components.mappers.ComponentSetMapper;
import de.schosin.ecs.api.components.mappers.EntityFetchRelations.EntityRelationFetchMapper;
import de.schosin.ecs.api.components.mappers.EntityFetchRelations.ExclusiveEntityRelationFetchMapper;
import de.schosin.ecs.api.components.types.ComponentType;
import de.schosin.ecs.engine.AbstractWorldTest;
import de.schosin.ecs.engine.EngineWorld;
import de.schosin.ecs.engine.utils.components.ComponentSetsHelperTest.Birthplace;
import de.schosin.ecs.engine.utils.components.ComponentSetsHelperTest.MyComponentSet;
import de.schosin.ecs.engine.utils.components.ComponentSetsHelperTest.Position;
import de.schosin.ecs.engine.utils.components.ComponentSetsHelperTest.RelationComponentSet;
import de.schosin.ecs.engine.utils.components.ComponentSetsHelperTest.Velocity;

class ComponentMapperManagerTest extends AbstractWorldTest {

    @Test
    void testComponentsReused() {
        var component1 = world.getComponents(Component1.class);

        assertThat(world.getComponents(Component1.class)).isSameAs(component1);
    }

    @Nested
    class ComponentsTest {

        ComponentMapper<Component1> component1;
        ComponentMapper<Component2> component2;
        PooledComponentMapper<PooledComponent> pooledComponent;
        EnumComponentMapper<EnumComponent> firstEnum;
        EnumComponentMapper<EnumComponent> secondEnum;

        @BeforeEach
        void setup() {
            this.component1 = world.getComponents(Component1.class);
            this.component2 = world.getComponents(Component2.class);
            this.pooledComponent = world.getPooledComponents(PooledComponent.class);
            this.firstEnum = world.getEnumComponents(EnumComponent.FIRST);
            this.secondEnum = world.getEnumComponents(EnumComponent.SECOND);
        }

        @Test
        void testComponentsDetectsPooled() {
            var world = (EngineWorld) World.builder().build(); // code coverage requires new world due to caching

            var components = world.getComponents(PooledComponent.class);
            assertThat(components).isInstanceOf(PooledComponentMapper.class);

            var pooledComponents = world.getPooledComponents(PooledComponent.class);
            assertThat(pooledComponents).isSameAs(components);
        }

        @Test
        void testPooledIntanceReused() {
            var entityId = world.createEntity();
            verifyDoesNotHaveComponents(entityId, PooledComponent.class);
            verifyComponentMaskDoesNotHaveComponents(entityId, PooledComponent.class);

            // Add
            var instance1 = pooledComponent.add(entityId);
            assertThat(instance1).isNotNull();

            world.process();
            verifyHasComponents(entityId, PooledComponent.class);
            verifyComponentMaskHasComponents(entityId, PooledComponent.class);

            // Remove
            pooledComponent.remove(entityId);

            world.process();
            verifyDoesNotHaveComponents(entityId, PooledComponent.class);
            verifyComponentMaskDoesNotHaveComponents(entityId, PooledComponent.class);

            // Reuse
            var reusedInstance = pooledComponent.add(entityId);
            assertThat(reusedInstance).isSameAs(instance1);

            world.process();
            verifyHasComponents(entityId, PooledComponent.class);
            verifyComponentMaskHasComponents(entityId, PooledComponent.class);
        }

        @Nested
        class HasComponent {

            @Test
            void testHasComponent() {
                var entityId = world.createEntity(new Component1());
                var entityIdPooled = world.createEntity(new Component2("data"));
                var entityIdEnum = world.createEntity(EnumComponent.FIRST);

                // Call
                assertThat(component1.has(entityId)).isTrue();
                assertThat(component2.has(entityId)).isFalse();
                assertThat(pooledComponent.has(entityId)).isFalse();
                assertThat(firstEnum.has(entityId)).isFalse();
                assertThat(secondEnum.has(entityId)).isFalse();

                assertThat(component1.has(entityIdPooled)).isFalse();
                assertThat(component2.has(entityIdPooled)).isTrue();
                assertThat(pooledComponent.has(entityIdPooled)).isFalse();
                assertThat(firstEnum.has(entityIdPooled)).isFalse();
                assertThat(secondEnum.has(entityIdPooled)).isFalse();

                assertThat(component1.has(entityIdEnum)).isFalse();
                assertThat(component2.has(entityIdEnum)).isFalse();
                assertThat(pooledComponent.has(entityIdEnum)).isFalse();
                assertThat(firstEnum.has(entityIdEnum)).isTrue();
                assertThat(secondEnum.has(entityIdEnum)).isTrue();
            }

        }

        @Nested
        class GetComponent {

            @Test
            void testGetComponent() {
                var component = new Component1();
                var entityId = world.createEntity(component);

                var componentPooled = new Component2("data");
                var entityIdPooled = world.createEntity(componentPooled);

                var entityIdEnum = world.createEntity(EnumComponent.FIRST);

                // Call
                assertThat(component1.get(entityId)).isEqualTo(component);
                assertThat(component2.get(entityId)).isNull();
                assertThat(pooledComponent.get(entityId)).isNull();
                assertThat(firstEnum.get(entityId)).isNull();
                assertThat(secondEnum.get(entityId)).isNull();

                assertThat(component1.get(entityIdPooled)).isNull();
                assertThat(component2.get(entityIdPooled)).isEqualTo(componentPooled);
                assertThat(pooledComponent.get(entityIdPooled)).isNull();
                assertThat(firstEnum.get(entityIdPooled)).isNull();
                assertThat(secondEnum.get(entityIdPooled)).isNull();

                assertThat(component1.get(entityIdEnum)).isNull();
                assertThat(component2.get(entityIdEnum)).isNull();
                assertThat(pooledComponent.get(entityIdEnum)).isNull();
                assertThat(firstEnum.get(entityIdEnum)).isEqualTo(EnumComponent.FIRST);
                assertThat(secondEnum.get(entityIdEnum)).isEqualTo(EnumComponent.FIRST);
            }

        }

        @Nested
        class AddTest {

            @Test
            void testAdd() {
                var entityId = world.createEntity();

                var instance1 = new Component1();
                var instance2 = new Component2("foo");
                var pooledInstance = new PooledComponent();
                pooledInstance.data = "bar";

                // Call
                assertThat(component1.add(entityId, instance1)).isSameAs(instance1);
                assertThat(component2.add(entityId, instance2)).isSameAs(instance2);
                assertThat(pooledComponent.add(entityId, pooledInstance)).isSameAs(pooledInstance);

                assertThat(firstEnum.add(entityId, EnumComponent.FIRST)).isSameAs(EnumComponent.FIRST);
                assertThat(secondEnum.add(entityId, EnumComponent.FIRST)).isSameAs(EnumComponent.FIRST);

                assertThat(firstEnum.add(entityId, EnumComponent.SECOND)).isSameAs(EnumComponent.SECOND);
                assertThat(secondEnum.add(entityId, EnumComponent.SECOND)).isSameAs(EnumComponent.SECOND);

                world.process();

                // Verify
                assertThat(pooledInstance.data).isEqualTo("bar");

                verifyHasComponents(entityId, Component1.class, Component2.class, PooledComponent.class, EnumComponent.class);
                verifyComponentMaskHasComponents(entityId, Component1.class, Component2.class, PooledComponent.class, EnumComponent.class);
            }

            @Test
            void testAdd_WhenAlreadyContained_ReturnsExisting() {
                var pooled = new PooledComponent();
                var entityId = world.createEntity(pooled);

                // Call
                assertThat(pooledComponent.add(entityId)).isSameAs(pooled);
            }

        }

        @Nested
        class EnumComponentsTest {

            @Test
            void testGetDefault_WhenFirstEnum() {
                assertThat(firstEnum.getDefault()).isSameAs(EnumComponent.FIRST);
            }

            @Test
            void testGetDefault_WhenSecondEnum() {
                assertThat(secondEnum.getDefault()).isSameAs(EnumComponent.SECOND);
            }

            @Test
            void testAdd_WhenFirstEnum() {
                var entityId = world.createEntity();

                // Call
                var instance1 = firstEnum.add(entityId);
                assertThat(instance1).isSameAs(EnumComponent.FIRST);

                world.process();

                // Verify
                verifyHasComponents(entityId, EnumComponent.class);
                verifyComponentMaskHasComponents(entityId, EnumComponent.class);
            }

            @Test
            void testAdd_WhenSecondEnum() {
                var entityId = world.createEntity();

                // Call
                var instance1 = secondEnum.add(entityId);
                assertThat(instance1).isSameAs(EnumComponent.SECOND);

                world.process();

                // Verify
                verifyHasComponents(entityId, EnumComponent.class);
                verifyComponentMaskHasComponents(entityId, EnumComponent.class);
            }

        }

        @Nested
        class PooledComponentsTest {

            @Test
            void testPooledIntance() {
                var entityId = world.createEntity();

                // Call
                var instance1 = pooledComponent.add(entityId);
                assertThat(instance1).isNotNull();

                world.process();

                // Verify
                verifyHasComponents(entityId, PooledComponent.class);
                verifyComponentMaskHasComponents(entityId, PooledComponent.class);
            }

        }

        @Nested
        class RemoveComponentTest {

            @Test
            void testRemoveComponent() {
                var entityId = world.createEntity(new Component1(), new Component2("data"), EnumComponent.SECOND);

                // Call
                assertThat(component1.remove(entityId)).isTrue();
                assertThat(component2.remove(entityId)).isTrue();
                assertThat(pooledComponent.remove(entityId)).isFalse();
                assertThat(firstEnum.remove(entityId)).isTrue();
            }

        }

        @Nested
        class EntityRelationFetchMapperTest {

            EntityRelationFetchMapper<Related, MyComponentSet> mapper;

            @BeforeEach
            void setupMapper() {
                this.mapper = world.getEntityFetchRelations(ComponentType.relation(Related.class, componentSet(MyComponentSet.class)));
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
                    assertThat(relation).isNull();
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
                    assertThat(data).isNull();
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
                            .extracting("target", "data.pos", "data.velocity")
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
                    assertThat(relations.getData(related1)).extracting("pos", "velocity").contains(relatedPos1, relatedVelocity1);
                    assertThat(relations.getData(related2)).extracting("pos", "velocity").contains(relatedPos2, relatedVelocity2);
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

        }

        @Nested
        class ExclusiveEntityRelationFetchMapperTest {

            ExclusiveEntityRelationFetchMapper<Parent, MyComponentSet> mapper;

            @BeforeEach
            void setupMapper() {
                this.mapper = world.getEntityFetchRelations(ComponentType.exclusiveRelation(Parent.class, componentSet(MyComponentSet.class)));
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

        @Nested
        class ComponentSetMapperTest {

            ComponentSetMapper<MyComponentSet> mapper;

            @BeforeEach
            void setupMapper() {
                this.mapper = world.getComponentSets(componentSet(MyComponentSet.class));
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
                        verify.expectUpdated(entityId, new Class<?>[0]);
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
                        verify.expectUpdated(entityId, new Class<?>[0]);
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
                        verify.expectUpdated(entityId, new Class<?>[0]);
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

        @Nested
        class WildcardTypeComponentsTest {

            @Test
            void testHas() {
                var components = world.getComponents(WILDCARD);

                var component1 = new Component1();
                var component2 = new Component2("foo");
                var pooled = new PooledComponent();

                var entityId = world.createEntity(component1, component2, pooled, EnumComponent.FIRST);

                // Call
                assertThat(components.has(entityId)).isTrue();
            }

            @Test
            void testHas_ComponentsKnownBeforehand() {
                var component1 = new Component1();
                var component2 = new Component2("foo");
                var pooled = new PooledComponent();

                var entityId = world.createEntity(component1, component2, pooled, EnumComponent.FIRST);

                var components = world.getComponents(WILDCARD);

                // Call
                assertThat(components.has(entityId)).isTrue();
            }

            @Test
            void testRemove() {
                var components = world.getComponents(WILDCARD);

                var component1 = new Component1();
                var component2 = new Component2("foo");
                var pooled = new PooledComponent();

                var entityId = world.createEntity(component1, component2, pooled, EnumComponent.FIRST);
                verifyHasComponents(entityId, Component1.class, Component2.class, PooledComponent.class, EnumComponent.class);
                verifyComponentMaskHasComponents(entityId, Component1.class, Component2.class, PooledComponent.class, EnumComponent.class);

                // Call
                assertThat(components.remove(entityId)).isTrue();

                world.process();

                // Verify
                verifyDoesNotHaveComponents(entityId, Component1.class, Component2.class, PooledComponent.class, EnumComponent.class);
                verifyComponentMaskDoesNotHaveComponents(entityId, Component1.class, Component2.class, PooledComponent.class, EnumComponent.class);
            }

            @Test
            void testRemove_WhenWorldNotProcessed_DoesNotAlterYet() {
                var components = world.getComponents(WILDCARD);

                var component1 = new Component1();
                var component2 = new Component2("foo");
                var pooled = new PooledComponent();

                var entityId = world.createEntity(component1, component2, pooled, EnumComponent.FIRST);
                verifyHasComponents(entityId, Component1.class, Component2.class, PooledComponent.class, EnumComponent.class);
                verifyComponentMaskHasComponents(entityId, Component1.class, Component2.class, PooledComponent.class, EnumComponent.class);

                // Call
                assertThat(components.remove(entityId)).isTrue();

                // Verify
                verifyHasComponents(entityId, Component1.class, Component2.class, PooledComponent.class, EnumComponent.class);
                verifyComponentMaskHasComponents(entityId, Component1.class, Component2.class, PooledComponent.class, EnumComponent.class);
            }

            @Test
            void testRemove_ComponentsKnownBeforehand() {
                var component1 = new Component1();
                var component2 = new Component2("foo");
                var pooled = new PooledComponent();

                var entityId = world.createEntity(component1, component2, pooled, EnumComponent.FIRST);
                verifyHasComponents(entityId, Component1.class, Component2.class, PooledComponent.class, EnumComponent.class);
                verifyComponentMaskHasComponents(entityId, Component1.class, Component2.class, PooledComponent.class, EnumComponent.class);

                var components = world.getComponents(WILDCARD);

                // Call
                assertThat(components.remove(entityId)).isTrue();

                world.process();

                // Verify
                verifyDoesNotHaveComponents(entityId, Component1.class, Component2.class, PooledComponent.class, EnumComponent.class);
                verifyComponentMaskDoesNotHaveComponents(entityId, Component1.class, Component2.class, PooledComponent.class, EnumComponent.class);
            }

            @Test
            void testRemove_RegularOnly() {
                var components = world.getComponents(wildcard(Regular.class));

                var component1 = new Component1();
                var component2 = new Component2("foo");
                var pooled = new PooledComponent();

                var entityId = world.createEntity(component1, component2, pooled, EnumComponent.FIRST);
                verifyHasComponents(entityId, Component1.class, Component2.class, PooledComponent.class, EnumComponent.class);
                verifyComponentMaskHasComponents(entityId, Component1.class, Component2.class, PooledComponent.class, EnumComponent.class);

                // Call
                assertThat(components.remove(entityId)).isTrue();

                world.process();

                // Verify
                verifyHasComponents(entityId, PooledComponent.class, EnumComponent.class);
                verifyDoesNotHaveComponents(entityId, Component1.class, Component2.class);

                verifyComponentMaskHasComponents(entityId, PooledComponent.class, EnumComponent.class);
                verifyComponentMaskDoesNotHaveComponents(entityId, Component1.class, Component2.class);
            }

            @Test
            void testRemove_RegularOnly_ComponentsKnownBeforehand() {
                var component1 = new Component1();
                var component2 = new Component2("foo");
                var pooled = new PooledComponent();

                var entityId = world.createEntity(component1, component2, pooled, EnumComponent.FIRST);
                verifyHasComponents(entityId, Component1.class, Component2.class, PooledComponent.class, EnumComponent.class);
                verifyComponentMaskHasComponents(entityId, Component1.class, Component2.class, PooledComponent.class, EnumComponent.class);

                var components = world.getComponents(wildcard(Regular.class));

                // Call
                assertThat(components.remove(entityId)).isTrue();

                world.process();

                // Verify
                verifyHasComponents(entityId, PooledComponent.class, EnumComponent.class);
                verifyDoesNotHaveComponents(entityId, Component1.class, Component2.class);

                verifyComponentMaskHasComponents(entityId, PooledComponent.class, EnumComponent.class);
                verifyComponentMaskDoesNotHaveComponents(entityId, Component1.class, Component2.class);
            }

            @Nested
            class GetTest {

                @Test
                void testSize() {
                    var components = world.getComponents(WILDCARD);

                    var component1 = new Component1();
                    var component2 = new Component2("foo");
                    var pooled = new PooledComponent();

                    var entityId = world.createEntity(component1, component2, pooled, EnumComponent.FIRST);

                    // Call
                    var result = components.get(entityId);

                    // Verify
                    assertThat(result.size()).as("size").isEqualTo(4);
                }

                @Test
                void testSize_InterfaceWildcard() {
                    var components = world.getComponents(wildcard(Regular.class));

                    var component1 = new Component1();
                    var component2 = new Component2("foo");
                    var pooled = new PooledComponent();

                    var entityId = world.createEntity(component1, component2, pooled, EnumComponent.FIRST);

                    // Call
                    var result = components.get(entityId);

                    // Verify
                    assertThat(result.size()).as("size").isEqualTo(2);
                }

                @Test
                void testIsEmpty() {
                    var components = world.getComponents(WILDCARD);

                    var component1 = new Component1();
                    var component2 = new Component2("foo");
                    var pooled = new PooledComponent();

                    var entity1 = world.createEntity(component1, component2, pooled, EnumComponent.FIRST);
                    var entity2 = world.createEntity();

                    // Call
                    var result1 = components.get(entity1);
                    var result2 = components.get(entity2);

                    // Verify
                    assertThat(result1.isEmpty()).as("isEmpty").isFalse();
                    assertThat(result2.isEmpty()).as("isEmpty").isTrue();
                }

                @Test
                void testGetByIndex() {
                    var components = world.getComponents(WILDCARD);

                    var component1 = new Component1();
                    var component2 = new Component2("foo");
                    var pooled = new PooledComponent();

                    var entityId = world.createEntity(component1, component2, pooled, EnumComponent.FIRST);

                    // Call
                    var result = components.get(entityId);

                    // Verify
                    assertThat(result.get(0)).as("get(0)").isIn(component1, component2, pooled, EnumComponent.FIRST);
                    assertThat(result.get(1)).as("get(1)").isIn(component1, component2, pooled, EnumComponent.FIRST);
                    assertThat(result.get(2)).as("get(2)").isIn(component1, component2, pooled, EnumComponent.FIRST);
                    assertThat(result.get(3)).as("get(3)").isIn(component1, component2, pooled, EnumComponent.FIRST);

                    assertThatThrownBy(() -> result.get(4)).isInstanceOf(ArrayIndexOutOfBoundsException.class);
                }

                @Test
                void testEnhancedForLoop() {
                    var components = world.getComponents(WILDCARD);

                    var component1 = new Component1();
                    var component2 = new Component2("foo");
                    var pooled = new PooledComponent();

                    var entityId = world.createEntity(component1, component2, pooled, EnumComponent.FIRST);

                    // Call
                    var result = components.get(entityId);

                    // Verify
                    for (var component : result) {
                        assertThat(component).as("enhanced for loop").isIn(component1, component2, pooled, EnumComponent.FIRST);
                    }
                }

                @Test
                void testEnhancedForLoop_InterfaceWildcard() {
                    var components = world.getComponents(wildcard(Regular.class));

                    var component1 = new Component1();
                    var component2 = new Component2("foo");
                    var pooled = new PooledComponent();

                    var entityId = world.createEntity(component1, component2, pooled, EnumComponent.FIRST);

                    // Call
                    var result = components.get(entityId);

                    // Verify
                    for (var component : result) {
                        assertThat(component).as("enhanced for loop").isIn(component1, component2);
                    }
                }

                @Test
                void testIterator() {
                    var components = world.getComponents(WILDCARD);

                    var component1 = new Component1();
                    var component2 = new Component2("foo");
                    var pooled = new PooledComponent();

                    var entityId = world.createEntity(component1, component2, pooled, EnumComponent.FIRST);

                    // Call
                    var result = components.get(entityId);

                    // Verify
                    for (var iter = result.iterator(); iter.hasNext();) {
                        assertThat(iter.next()).as("iterator loop").isIn(component1, component2, pooled, EnumComponent.FIRST);
                    }
                    for (var iter = result.iterator(); iter.hasNext();) {
                        assertThat(iter.next()).as("iterator loop resets automatically").isIn(component1, component2, pooled, EnumComponent.FIRST);
                    }
                }

                @Test
                void testIterator_InterfaceWildcard() {
                    var components = world.getComponents(wildcard(Regular.class));

                    var component1 = new Component1();
                    var component2 = new Component2("foo");
                    var pooled = new PooledComponent();

                    var entityId = world.createEntity(component1, component2, pooled, EnumComponent.FIRST);

                    // Call
                    var result = components.get(entityId);

                    // Verify
                    for (var iter = result.iterator(); iter.hasNext();) {
                        assertThat(iter.next()).as("iterator loop").isIn(component1, component2);
                    }
                    for (var iter = result.iterator(); iter.hasNext();) {
                        assertThat(iter.next()).as("iterator loop resets automatically").isIn(component1, component2);
                    }
                }

                @Test
                void testGetByClass() {
                    var components = world.getComponents(WILDCARD);

                    var component1 = new Component1();
                    var component2 = new Component2("foo");
                    var pooled = new PooledComponent();

                    var entityId = world.createEntity(component1, component2, pooled, EnumComponent.FIRST);

                    // Call
                    var result = components.get(entityId);

                    // Verify
                    assertThat(result.get(Component1.class)).as("get(Component1.class)").isSameAs(component1);
                    assertThat(result.get(Component2.class)).as("get(Component2.class)").isSameAs(component2);
                    assertThat(result.get(PooledComponent.class)).as("get(PooledComponent.class)").isSameAs(pooled);
                    assertThat(result.get(EnumComponent.class)).as("get(EnumComponent.class)").isSameAs(EnumComponent.FIRST);
                }

                @Test
                void testGetByClass_InterfaceWildcard() {
                    world.getComponents(Component3.class); // register type so that Components detects component

                    var components = world.getComponents(wildcard(Regular.class));

                    var component1 = new Component1();
                    var component2 = new Component2("foo");
                    var pooled = new PooledComponent();

                    var entityId = world.createEntity(component1, component2, pooled, EnumComponent.FIRST);

                    // Call
                    var result = components.get(entityId);

                    // Verify
                    assertThat(result.get(Component1.class)).as("get(Component1.class)").isSameAs(component1);
                    assertThat(result.get(Component2.class)).as("get(Component2.class)").isSameAs(component2);
                    assertThat(result.get(Component3.class)).as("get(Component3.class)").isNull();
                }

                @Test
                void testResultReused_WhenWorldProcessedInBetween() {
                    var components = world.getComponents(wildcard(Regular.class));

                    var entityId = world.createEntity();

                    // Call
                    var result1 = components.get(entityId);

                    world.process();

                    // Verify
                    var result2 = components.get(entityId);
                    assertThat((Object) result2).isSameAs(result1);
                }

                @Test
                void testDifferentResult() {
                    var components = world.getComponents(wildcard(Regular.class));

                    var entityId = world.createEntity();

                    // Call
                    var result1 = components.get(entityId);
                    var result2 = components.get(entityId);

                    // Verify
                    assertThat((Object) result2).isNotSameAs(result1);
                }

            }

        }

    }

    interface Regular {
    }

    public record Component1() implements Regular {
    }

    public record Component2(String data) implements Regular {
    }

    public record Component3() implements Regular {
    }

    public enum Related {
        Related
    }

    public enum Parent implements Exclusive {
        Parent
    }

    public static class PooledComponent implements Pooled {

        private String data;

        @Override
        public void reset() {
            this.data = null;
        }
    }

    enum EnumComponent {
        FIRST, SECOND
    }

}
