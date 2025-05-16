package de.schosin.ecs.engine.entities;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import de.schosin.ecs.api.Pooled;
import de.schosin.ecs.api.components.Components.PooledComponentMapper;
import de.schosin.ecs.engine.AbstractWorldTest;
import de.schosin.ecs.engine.events.builtin.EntityEvent.EntityInsertedEvent;
import de.schosin.ecs.engine.events.builtin.EntityEvent.EntityUpdatedEvent;
import de.schosin.ecs.storage.api.components.Component;
import de.schosin.ecs.utils.collections.BitVector;

class EntityManagerTest extends AbstractWorldTest {

    Component<Component1> component1;
    Component<Component2> component2;
    Component<Component3> component3;

    @BeforeEach
    void setupComponents() {
        this.component1 = componentManager.getComponent(component(Component1.class));
        this.component2 = componentManager.getComponent(component(Component2.class));
        this.component3 = componentManager.getComponent(component(Component3.class));
    }

    @Test
    void testIsActive() {
        var entity1 = entityManager.createEntity();
        var entity2 = entityManager.createEntity();

        assertThat(world.isActive(entity1)).isTrue();
        assertThat(world.isActive(entity2)).isTrue();

        world.deleteEntity(entity1);
        assertThat(world.isActive(entity1)).isTrue();
        assertThat(world.isActive(entity2)).isTrue();

        world.process();
        assertThat(world.isActive(entity1)).isFalse();
        assertThat(world.isActive(entity2)).isTrue();
    }

    @Nested
    class CreateDynamicEntity {

        @Test
        void testDynamicEntitiy() {
            var entityId = entityManager.createEntity(new Component1(), new Component2());

            // Verify
            verifyHasComponents(entityId, Component1.class, Component2.class);
            verifyComponentMaskHasComponents(entityId, Component1.class, Component2.class);
        }

        @Test
        void testReusedEntityId() {
            var entityId = entityManager.createEntity(new Component1());

            world.deleteEntity(entityId);
            world.process();

            assertThat(entityManager.createEntity(new Component2())).isEqualTo(entityId);
            assertThat(entityManager.createEntity(new Component2())).isGreaterThan(entityId);
        }

        @Test
        void testInsertedHandler() {
            verify(verify -> {
                verify.expectInserted(Component1.class, Component2.class);
                verify.expectInserted(Component1.class, Component3.class);
                verify.expectNoMoreInserted();

                entityManager.createEntity(new Component1(), new Component2());
                entityManager.createEntity(new Component1(), new Component3());
            });
        }

        @Test
        void testDuplicateTypes() {
            var otherComponent1 = new Component1();

            assertThatThrownBy(() -> world.createEntity(new Component1(), new Component2(), otherComponent1, new Component2()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("duplicate component types");
        }

    }

    @Nested
    class GetEntitiesTest {

        @Test
        void testAcceptAll() {
            var entity123 = world.createEntity(new Component1(), new Component2(), new Component3());
            var entity12 = world.createEntity(new Component1(), new Component2());
            var entity1 = world.createEntity(new Component1());
            var entity2 = world.createEntity(new Component2());
            var entity23 = world.createEntity(new Component2(), new Component3());

            // Call
            var entities = entityManager.getEntities(mask -> true);

            // Verify
            assertThat(entities.getSize()).as("size").isEqualTo(5);
            assertThat(entities.getData()).as("entity123").contains(entity123);
            assertThat(entities.getData()).as("entity12").contains(entity12);
            assertThat(entities.getData()).as("entity1").contains(entity1);
            assertThat(entities.getData()).as("entity2").contains(entity2);
            assertThat(entities.getData()).as("entity23").contains(entity23);
        }

        @Test
        void testAllOf() {
            var entity123 = world.createEntity(new Component1(), new Component2(), new Component3());
            var entity12 = world.createEntity(new Component1(), new Component2());
            var entity1 = world.createEntity(new Component1());
            var entity2 = world.createEntity(new Component2());
            var entity23 = world.createEntity(new Component2(), new Component3());

            var all = new BitVector();
            all.set(component1.id());
            all.set(component2.id());

            // Call
            var entities = entityManager.getEntities(mask -> mask.containsAll(all));

            // Verify
            assertThat(entities.getSize()).as("size").isEqualTo(2);
            assertThat(entities.getData()).as("entity123").contains(entity123);
            assertThat(entities.getData()).as("entity12").contains(entity12);
            assertThat(entities.getData()).as("entity1").doesNotContain(entity1);
            assertThat(entities.getData()).as("entity2").doesNotContain(entity2);
            assertThat(entities.getData()).as("entity23").doesNotContain(entity23);
        }

        @Test
        void testOneOf() {
            var entity123 = world.createEntity(new Component1(), new Component2(), new Component3());
            var entity12 = world.createEntity(new Component1(), new Component2());
            var entity1 = world.createEntity(new Component1());
            var entity2 = world.createEntity(new Component2());
            var entity23 = world.createEntity(new Component2(), new Component3());

            var one = new BitVector();
            one.set(component1.id());
            one.set(component3.id());

            // Call
            var entities = entityManager.getEntities(mask -> mask.containsSome(one));

            // Verify
            assertThat(entities.getSize()).as("size").isEqualTo(4);
            assertThat(entities.getData()).as("entity123").contains(entity123);
            assertThat(entities.getData()).as("entity12").contains(entity12);
            assertThat(entities.getData()).as("entity1").contains(entity1);
            assertThat(entities.getData()).as("entity2").doesNotContain(entity2);
            assertThat(entities.getData()).as("entity23").contains(entity23);

        }

        @Test
        void testNoneOf() {
            var entity123 = world.createEntity(new Component1(), new Component2(), new Component3());
            var entity12 = world.createEntity(new Component1(), new Component2());
            var entity1 = world.createEntity(new Component1());
            var entity2 = world.createEntity(new Component2());
            var entity23 = world.createEntity(new Component2(), new Component3());

            var none = new BitVector();
            none.set(component1.id());
            none.set(component3.id());

            // Call
            var entities = entityManager.getEntities(mask -> mask.containsNone(none));

            // Verify
            assertThat(entities.getSize()).as("size").isEqualTo(1);
            assertThat(entities.getData()).as("entity123").doesNotContain(entity123);
            assertThat(entities.getData()).as("entity12").doesNotContain(entity12);
            assertThat(entities.getData()).as("entity1").doesNotContain(entity1);
            assertThat(entities.getData()).as("entity2").contains(entity2);
            assertThat(entities.getData()).as("entity23").doesNotContain(entity23);
        }

        @Test
        void testComplexSpec() {
            var entity123 = world.createEntity(new Component1(), new Component2(), new Component3());
            var entity12 = world.createEntity(new Component1(), new Component2());
            var entity1 = world.createEntity(new Component1());
            var entity2 = world.createEntity(new Component2());
            var entity23 = world.createEntity(new Component2(), new Component3());

            var all = new BitVector();
            all.set(component1.id());
            all.set(component2.id());

            var none = new BitVector();
            none.set(component3.id());

            // Call
            var entities = entityManager.getEntities(mask -> mask.containsAll(all) && mask.containsNone(none));

            // Verify
            assertThat(entities.getSize()).as("size").isEqualTo(1);
            assertThat(entities.getData()).as("entity123").doesNotContain(entity123);
            assertThat(entities.getData()).as("entity12").contains(entity12);
            assertThat(entities.getData()).as("entity1").doesNotContain(entity1);
            assertThat(entities.getData()).as("entity2").doesNotContain(entity2);
            assertThat(entities.getData()).as("entity23").doesNotContain(entity23);
        }

        @Test
        void testResultInstanceNotReused() {
            var entities = entityManager.getEntities(mask -> true);

            assertThat(entityManager.getEntities(mask -> true)).isNotSameAs(entities);
        }

    }

    @Nested
    class ComponentMaskTest {

        @Test
        void testComponentOrderDoesNotAffectComponentMask() {
            // Setup
            var entity12 = world.createEntity(new Component1(), new Component2());
            var entity21 = world.createEntity(new Component1(), new Component2());

            // Call
            var componentMask12 = entityManager.getComponentMask(entity12);
            var componentMask21 = entityManager.getComponentMask(entity21);

            // Verify
            assertThat(componentMask12).isSameAs(componentMask21);
        }

        @Test
        void testGetUnknownEntity() {
            // Call
            var componentMask = entityManager.getComponentMask(42);

            // Verify
            assertThat(componentMask).isNull();
        }

        @Test
        void testUpdateComponentMask() {
            // Setup
            var entityId = world.createEntity(new Component1(), new Component2());

            var componentMask = entityManager.getComponentMask(entityId);
            assertThat(componentMask.contains(component1.id())).isTrue();
            assertThat(componentMask.contains(component2.id())).isTrue();

            var otherComponentMask = componentMaskManager.getComponentMask(component(Component3.class));
            assertThat(otherComponentMask).isNotSameAs(componentMask).isNotEqualTo(componentMask);

            // Call
            assertThat(entityManager.updateComponentMask(entityId, otherComponentMask)).isTrue();

            // Verify
            assertThat(entityManager.getComponentMask(entityId)).isSameAs(otherComponentMask);
        }

        @Test
        void testUpdateComponentMask_NoChange() {
            // Setup
            var entityId = world.createEntity(new Component1(), new Component2());

            var componentMask = entityManager.getComponentMask(entityId);
            assertThat(componentMask.contains(component1.id())).isTrue();
            assertThat(componentMask.contains(component2.id())).isTrue();

            var sameComponentMask = componentMaskManager.getComponentMask(component(Component2.class), component(Component1.class));
            assertThat(sameComponentMask).isSameAs(componentMask);

            // Call
            assertThat(entityManager.updateComponentMask(entityId, sameComponentMask)).isFalse();

            // Verify
            assertThat(entityManager.getComponentMask(entityId)).isSameAs(componentMask);
        }

        @Test
        void testUpdateComponentMask_UnknownEntity() {
            var componentMask = componentMaskManager.getComponentMask(component(Component1.class));

            assertThat(entityManager.updateComponentMask(42, null)).isFalse();
            assertThat(entityManager.updateComponentMask(42, componentMask)).isFalse();
        }

    }

    @Nested
    class DeleteEntityTest {

        @Test
        void testDeleteUnknownEntity() {
            assertThatCode(() -> entityManager.deleteEntity(42)).doesNotThrowAnyExceptionExcept(ArrayIndexOutOfBoundsException.class);
            assertThatCode(() -> entityManager.deleteEntity(31337)).doesNotThrowAnyExceptionExcept(ArrayIndexOutOfBoundsException.class);
        }

        @Test
        void testDeleteDelayed() {
            // Setup
            var entityId = world.createEntity(new Component1());
            verifyHasComponents(entityId, Component1.class);
            verifyComponentMaskHasComponents(entityId, Component1.class);

            // Call
            verify(verify -> {
                verify.expectNoMoreRemoved();

                world.deleteEntity(entityId);
            });

            // Verify
            assertThat(world.isActive(entityId)).as("is active").isTrue();

            verifyHasComponents(entityId, Component1.class);
            verifyComponentMaskHasComponents(entityId, Component1.class);
        }

        @Test
        void testDeleteProcessed() {
            // Setup
            var entityId = world.createEntity(new Component1());

            // Call
            verify(verify -> {
                verify.expectRemoved(entityId);
                verify.expectNoMoreRemoved();

                world.deleteEntity(entityId);
                world.process();
            });

            // Verify
            assertThat(world.isActive(entityId)).as("is active").isFalse();
        }

    }

    @Nested
    class CreateEntityMutationsTest {

        PooledComponentMapper<C1> pooled1;
        PooledComponentMapper<C2> pooled2;
        PooledComponentMapper<C3> pooled3;

        int id1;
        int id2;
        int id3;

        @BeforeEach
        void setupMappers() {
            this.pooled1 = world.getPooledComponents(C1.class);
            this.pooled2 = world.getPooledComponents(C2.class);
            this.pooled3 = world.getPooledComponents(C3.class);

            this.id1 = componentManager.getComponent(component(C1.class)).id();
            this.id2 = componentManager.getComponent(component(C2.class)).id();
            this.id3 = componentManager.getComponent(component(C3.class)).id();
        }

        @Test
        void testDeletionDuringCreation() {
            // Setup listeners
            eventManager.registerEventHandler(EntityInsertedEvent.class, event -> {
                if (event.componentMask().getMask().get(id1)) {
                    pooled2.add(event.entityId());
                }
            });

            eventManager.registerEventHandler(EntityUpdatedEvent.class, event -> {
                var mask = event.componentMask();
                var entityId = event.entityId();

                if (mask.getMask().get(id2)) {
                    pooled3.add(entityId);
                }

                if (mask.getMask().get(id3)) {
                    world.deleteEntity(entityId);
                }
            });

            // Call
            var c1 = pooled1.getInstance();

            assertThatThrownBy(() -> world.createEntity(c1))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("deleted during creation");
        }

        @Test
        void testMutationDuringCreation_WhenListenersModifyComponents_AlsoWorksIfWorldIsProcessed() {
            verify(verify -> {
                verify.expectInserted(C1.class);
                verify.expectUpdated(C1.class, C2.class);
                verify.expectUpdated(C1.class, C2.class, C3.class);
                verify.expectUpdated(C2.class, C3.class);
                verify.expectNoMoreInserted();
                verify.expectNoMoreUpdated();

                // Setup listeners
                eventManager.registerEventHandler(EntityInsertedEvent.class, event -> {
                    pooled2.add(event.entityId());
                });

                eventManager.registerEventHandler(EntityUpdatedEvent.class, event -> {
                    var mask = event.componentMask();
                    var prevMask = event.previousComponentMask();
                    var entityId = event.entityId();

                    if (!prevMask.getMask().get(id2) && mask.getMask().get(id2)) {
                        pooled3.add(entityId);
                    }

                    if (!prevMask.getMask().get(id3) && mask.getMask().get(id3)) {
                        pooled1.remove(entityId);
                    }
                });

                // Call
                var entityId = world.createEntity(pooled1.getInstance());
                world.process();

                // Verify
                verifyHasComponents(entityId, C2.class, C3.class);
                verifyDoesNotHaveComponents(entityId, C1.class);

                verifyComponentMaskHasComponents(entityId, C2.class, C3.class);
                verifyComponentMaskDoesNotHaveComponents(entityId, C1.class);
            });
        }

        @Test
        void testMutationDuringCreation_WhenListenersModifyComponents_ListenersCalledRecursivly() {
            verify(verify -> {
                verify.expectInserted(C1.class);
                verify.expectUpdated(C1.class, C2.class);
                verify.expectUpdated(C1.class, C2.class, C3.class);
                verify.expectUpdated(C2.class, C3.class);
                verify.expectNoMoreInserted();
                verify.expectNoMoreUpdated();

                // Setup listeners
                eventManager.registerEventHandler(EntityInsertedEvent.class, event -> {
                    pooled2.add(event.entityId());
                });

                eventManager.registerEventHandler(EntityUpdatedEvent.class, event -> {
                    var mask = event.componentMask();
                    var prevMask = event.previousComponentMask();
                    var entityId = event.entityId();

                    if (!prevMask.getMask().get(id2) && mask.getMask().get(id2)) {
                        pooled3.add(entityId);
                    }

                    if (!prevMask.getMask().get(id3) && mask.getMask().get(id3)) {
                        pooled1.remove(entityId);
                    }
                });

                // Call
                var entityId = world.createEntity(pooled1.getInstance());

                // Verify
                verifyHasComponents(entityId, C2.class, C3.class);
                verifyDoesNotHaveComponents(entityId, C1.class);

                verifyComponentMaskHasComponents(entityId, C2.class, C3.class);
                verifyComponentMaskDoesNotHaveComponents(entityId, C1.class);
            });
        }

        @Test
        void testMutationAfterCreation_WhenListenersModifyComponents_WorksIfWorldIsProcessed() {
            var entityId = world.createEntity();

            verify(verify -> {
                verify.expectUpdated(C1.class);
                verify.expectUpdated(C1.class, C2.class);
                verify.expectUpdated(C1.class, C2.class, C3.class);
                verify.expectUpdated(C2.class, C3.class);
                verify.expectNoMoreUpdated();

                // Setup listeners
                eventManager.registerEventHandler(EntityUpdatedEvent.class, event -> {
                    var mask = event.componentMask();
                    var prevMask = event.previousComponentMask();
                    var id = event.entityId();

                    if (!prevMask.getMask().get(id1) && mask.getMask().get(id1)) {
                        pooled2.add(id);
                    }
                    if (!prevMask.getMask().get(id2) && mask.getMask().get(id2)) {
                        pooled3.add(id);
                    }

                    if (!prevMask.getMask().get(id3) && mask.getMask().get(id3)) {
                        pooled1.remove(id);
                    }
                });

                // Call
                pooled1.add(entityId);

                // Process 1
                world.process(1);
                verifyHasComponents(entityId, C1.class, C2.class);
                verifyDoesNotHaveComponents(entityId, C3.class);

                verifyComponentMaskHasComponents(entityId, C1.class);
                verifyComponentMaskDoesNotHaveComponents(entityId, C2.class, C3.class);

                // Process 2
                world.process(1);
                verifyHasComponents(entityId, C1.class, C2.class, C3.class);

                verifyComponentMaskHasComponents(entityId, C1.class, C2.class);
                verifyComponentMaskDoesNotHaveComponents(entityId, C3.class);

                // Process 3
                world.process(1);
                verifyHasComponents(entityId, C1.class, C2.class, C3.class);
                verifyComponentMaskHasComponents(entityId, C1.class, C2.class, C3.class);

                // Process 4
                world.process(1);
                verifyHasComponents(entityId, C2.class, C3.class);
                verifyDoesNotHaveComponents(entityId, C1.class);

                verifyComponentMaskHasComponents(entityId, C2.class, C3.class);
                verifyComponentMaskDoesNotHaveComponents(entityId, C1.class);
            });
        }

        @Test
        void testMutationAfterCreation_WhenListenersModifyComponents_ListenersNotCalledRecirsuvly() {
            var entityId = world.createEntity();

            verify(verify -> {
                verify.expectNoMoreUpdated();

                // Setup listeners
                eventManager.registerEventHandler(EntityInsertedEvent.class, event -> {
                    pooled2.add(event.entityId());
                });

                eventManager.registerEventHandler(EntityUpdatedEvent.class, event -> {
                    var mask = event.componentMask();
                    var prevMask = event.previousComponentMask();
                    var id = event.entityId();

                    if (!prevMask.getMask().get(id2) && mask.getMask().get(id2)) {
                        pooled3.add(id);
                    }

                    if (!prevMask.getMask().get(id3) && mask.getMask().get(id3)) {
                        pooled1.remove(id);
                    }
                });

                // Call
                pooled1.add(entityId);

                // Verify
                verifyHasComponents(entityId, C1.class);
                verifyDoesNotHaveComponents(entityId, C2.class, C3.class);

                verifyComponentMaskDoesNotHaveComponents(entityId, C1.class, C2.class, C3.class);
            });
        }

    }

    private record Component1() {
    }

    private record Component2() {
    }

    private record Component3() {
    }

    public record C1() implements Pooled {
    }

    public record C2() implements Pooled {
    }

    public record C3() implements Pooled {
    }

}
