package de.schosin.ecs.engine.entities;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import de.schosin.ecs.api.Pooled;
import de.schosin.ecs.api.components.Components.PooledComponents;
import de.schosin.ecs.api.components.Composition;
import de.schosin.ecs.engine.AbstractWorldTest;
import de.schosin.ecs.engine.components.ComponentData;
import de.schosin.ecs.engine.compositions.EngineSpec;
import de.schosin.ecs.engine.utils.collections.BitVector;
import de.schosin.ecs.engine.utils.collections.IntBag;

class EntityManagerTest extends AbstractWorldTest {

    ComponentData<Component1> component1;
    ComponentData<Component2> component2;
    ComponentData<Component3> component3;

    @BeforeEach
    void setupComponents() {
        this.component1 = componentManager.getData(Component1.class);
        this.component2 = componentManager.getData(Component2.class);
        this.component3 = componentManager.getData(Component3.class);
    }

    @Test
    void testIsActive() {
        var entityId = entityManager.createEntity();
        assertThat(world.isActive(entityId)).isTrue();
        assertThat(world.isActive(entityId + 1)).isFalse();

        world.deleteEntity(entityId);
        assertThat(world.isActive(entityId)).isTrue();
        assertThat(world.isActive(entityId + 1)).isFalse();

        world.process();
        assertThat(world.isActive(entityId)).isFalse();
        assertThat(world.isActive(entityId + 1)).isFalse();
    }

    @Nested
    class CreateDynamicEntity {

        @Test
        void testDynamicEntitiy() {
            var entityId = entityManager.createEntity(new Component1(), new Component2());

            // Verify components
            verifyHasComponent(entityId, Component1.class);
            verifyHasComponent(entityId, Component2.class);

            // Verify composition
            verifyHasComposition(entityId, Composition.all(Component1.class, Component2.class));
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
        void testCompositionInserted() {
            var inserted = new IntBag(4);

            var composition = world.createComposition(Composition.all(Component1.class).none(Component3.class));
            composition.inserted(inserted::add);

            // Call
            var entity12 = entityManager.createEntity(new Component1(), new Component2());
            var entity13 = entityManager.createEntity(new Component1(), new Component3());

            // Verify
            assertThat(inserted.getSize()).as("inserted size").isOne();
            assertThat(inserted.getData()).as("inserted entity12").contains(entity12);
            assertThat(inserted.getData()).as("inserted entity13").doesNotContain(entity13);
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
        void testEmptySpec() {
            var entity123 = world.createEntity(new Component1(), new Component2(), new Component3());
            var entity12 = world.createEntity(new Component1(), new Component2());
            var entity1 = world.createEntity(new Component1());
            var entity2 = world.createEntity(new Component2());
            var entity23 = world.createEntity(new Component2(), new Component3());

            var spec = EngineSpec.create(null, null, null);

            // Call
            var entities = entityManager.getEntities(spec);

            // Verify
            assertThat(entities.getSize()).as("size").isEqualTo(5);
            assertThat(entities.getData()).as("entity123").contains(entity123);
            assertThat(entities.getData()).as("entity12").contains(entity12);
            assertThat(entities.getData()).as("entity1").contains(entity1);
            assertThat(entities.getData()).as("entity2").contains(entity2);
            assertThat(entities.getData()).as("entity23").contains(entity23);
        }

        @Test
        void testAllSpec() {
            var entity123 = world.createEntity(new Component1(), new Component2(), new Component3());
            var entity12 = world.createEntity(new Component1(), new Component2());
            var entity1 = world.createEntity(new Component1());
            var entity2 = world.createEntity(new Component2());
            var entity23 = world.createEntity(new Component2(), new Component3());

            var all = new BitVector();
            all.set(component1.id());
            all.set(component2.id());

            var spec = EngineSpec.create(all, null, null);

            // Call
            var entities = entityManager.getEntities(spec);

            // Verify
            assertThat(entities.getSize()).as("size").isEqualTo(2);
            assertThat(entities.getData()).as("entity123").contains(entity123);
            assertThat(entities.getData()).as("entity12").contains(entity12);
            assertThat(entities.getData()).as("entity1").doesNotContain(entity1);
            assertThat(entities.getData()).as("entity2").doesNotContain(entity2);
            assertThat(entities.getData()).as("entity23").doesNotContain(entity23);
        }

        @Test
        void testOneSpec() {
            var entity123 = world.createEntity(new Component1(), new Component2(), new Component3());
            var entity12 = world.createEntity(new Component1(), new Component2());
            var entity1 = world.createEntity(new Component1());
            var entity2 = world.createEntity(new Component2());
            var entity23 = world.createEntity(new Component2(), new Component3());

            var one = new BitVector();
            one.set(component1.id());
            one.set(component3.id());

            var spec = EngineSpec.create(null, Set.of(one), null);

            // Call
            var entities = entityManager.getEntities(spec);

            // Verify
            assertThat(entities.getSize()).as("size").isEqualTo(4);
            assertThat(entities.getData()).as("entity123").contains(entity123);
            assertThat(entities.getData()).as("entity12").contains(entity12);
            assertThat(entities.getData()).as("entity1").contains(entity1);
            assertThat(entities.getData()).as("entity2").doesNotContain(entity2);
            assertThat(entities.getData()).as("entity23").contains(entity23);

        }

        @Test
        void testNoneSpec() {
            var entity123 = world.createEntity(new Component1(), new Component2(), new Component3());
            var entity12 = world.createEntity(new Component1(), new Component2());
            var entity1 = world.createEntity(new Component1());
            var entity2 = world.createEntity(new Component2());
            var entity23 = world.createEntity(new Component2(), new Component3());

            var none = new BitVector();
            none.set(component1.id());
            none.set(component3.id());

            var spec = EngineSpec.create(null, null, none);

            // Call
            var entities = entityManager.getEntities(spec);

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

            var spec = EngineSpec.create(all, null, none);

            // Call
            var entities = entityManager.getEntities(spec);

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
            var spec = EngineSpec.create(null, null, null);
            var entities = entityManager.getEntities(spec);

            assertThat(entityManager.getEntities(spec)).isNotSameAs(entities);
        }

        @Test
        void testResultSizeSynchronized() {
            var spec = EngineSpec.create(null, null, null);
            var entities = entityManager.getEntities(spec);

            var capacity = entities.getCapacity();

            // Call
            bagManager.ensureEntitySize(capacity * 2);

            // Verify
            assertThat(entities.getCapacity()).isGreaterThanOrEqualTo(capacity * 2);
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

            var otherComponentMask = componentMaskManager.getComponentMask(Component3.class);
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

            var sameComponentMask = componentMaskManager.getComponentMask(Component2.class, Component1.class);
            assertThat(sameComponentMask).isSameAs(componentMask);

            // Call
            assertThat(entityManager.updateComponentMask(entityId, sameComponentMask)).isFalse();

            // Verify
            assertThat(entityManager.getComponentMask(entityId)).isSameAs(componentMask);
        }

        @Test
        void testUpdateComponentMask_UnknownEntity() {
            var componentMask = componentMaskManager.getComponentMask(Component1.class);

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

    }

    @Nested
    class WorldDeleteTest {

        @Test
        void testDeleteDelayed() {
            // Setup
            var removed = new IntBag(4);
            var processed = new IntBag(4);

            var composition = world.createComposition(Composition.all(Component1.class));
            composition.removed(removed::add);

            var entityId = world.createEntity(new Component1());
            verifyHasComposition(entityId, Composition.all(Component1.class));
            getComponent(entityId, Component1.class);

            // Call
            world.deleteEntity(entityId);

            // Verify
            assertThat(removed.getSize()).as("removed size").isZero();
            assertThat(removed.getData()).as("entity not removed").doesNotContain(entityId);

            verifyHasComposition(entityId, Composition.all(Component1.class));
            verifyHasComponent(entityId, Component1.class);

            composition.process(processed::add);
            assertThat(processed.getSize()).as("processed size").isOne();
            assertThat(processed.getData()).as("entity not processed").contains(entityId);
        }

        @Test
        void testDeleteProcessed() {
            var removed = new IntBag(4);
            var processed = new IntBag(4);

            var composition = world.createComposition(Composition.all(Component1.class));
            composition.removed(removed::add);

            var entityId = world.createEntity(new Component1());
            verifyHasComposition(entityId, Composition.all(Component1.class));
            getComponent(entityId, Component1.class);

            // Call
            world.deleteEntity(entityId);
            world.process();

            // Verify
            assertThat(removed.getSize()).as("removed size").isOne();
            assertThat(removed.getData()).as("entity removed").contains(entityId);

            verifyDoesNotHaveComposition(entityId, Composition.all(Component1.class));
            verifyDoesNotHaveComponent(entityId, Component1.class);

            composition.process(processed::add);
            assertThat(processed.getSize()).as("processed size").isZero();
            assertThat(processed.getData()).as("entity not processed").doesNotContain(entityId);
        }

    }

    @Nested
    class CreateEntityMutationsTest {

        PooledComponents<C1> pooled1;
        PooledComponents<C2> pooled2;
        PooledComponents<C3> pooled3;

        @BeforeEach
        void setupMappers() {
            this.pooled1 = world.getPooledComponents(C1.class);
            this.pooled2 = world.getPooledComponents(C2.class);
            this.pooled3 = world.getPooledComponents(C3.class);
        }

        @Test
        void testDeletionDuringCreation() {
            // Setup composition listeners
            var composition1 = world.createComposition(Composition.all(C1.class));
            composition1.inserted(pooled2::add);

            var composition2 = world.createComposition(Composition.all(C2.class));
            composition2.inserted(pooled3::add);

            var composition3 = world.createComposition(Composition.all(C3.class));
            composition3.inserted(world::deleteEntity);

            // Call
            var c1 = pooled1.getInstance();

            assertThatThrownBy(() -> world.createEntity(c1))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("deleted during creation");
        }

        @Test
        void testMutationDuringCreation_WhenListenersModifyComponents_WorksIfWorldIsProcessed() {
            // Setup composition listeners
            var composition1 = world.createComposition(Composition.all(C1.class));
            composition1.inserted(pooled2::add);

            var composition2 = world.createComposition(Composition.all(C2.class));
            composition2.inserted(pooled3::add);

            var composition3 = world.createComposition(Composition.all(C3.class));
            composition3.inserted(pooled1::remove);

            // Call
            var entityId = world.createEntity(pooled1.getInstance());

            assertThat(world.process(1)).isTrue();

            // Verify
            verifyHasComposition(entityId, Composition.all(C2.class, C3.class).none(C1.class));

            verifyDoesNotHaveComponent(entityId, C1.class);
            verifyHasComponent(entityId, C2.class);
            verifyHasComponent(entityId, C3.class);
        }

        @Test
        void testMutationDuringCreation_WhenListenersModifyComponents_ListenersCalledRecursivly() {
            // Setup composition listeners
            var composition1 = world.createComposition(Composition.all(C1.class));
            composition1.inserted(pooled2::add);

            var composition2 = world.createComposition(Composition.all(C2.class));
            composition2.inserted(pooled3::add);

            var composition3 = world.createComposition(Composition.all(C3.class));
            composition3.inserted(pooled1::remove);

            // Call
            var entityId = world.createEntity(pooled1.getInstance());

            // Verify
            verifyHasComposition(entityId, Composition.all(C2.class, C3.class).none(C1.class));

            verifyDoesNotHaveComponent(entityId, C1.class);
            verifyHasComponent(entityId, C2.class);
            verifyHasComponent(entityId, C3.class);
        }

        @Test
        void testMutationAfterCreation_WhenListenersModifyComponents_WorksIfWorldIsProcessed() {
            // Setup composition listeners
            var composition1 = world.createComposition(Composition.all(C1.class));
            composition1.inserted(pooled2::add);

            var composition2 = world.createComposition(Composition.all(C2.class));
            composition2.inserted(pooled3::add);

            var composition3 = world.createComposition(Composition.all(C3.class));
            composition3.inserted(pooled1::remove);

            var entityId = world.createEntity();

            // Call
            pooled1.add(entityId);

            // Verify first process
            assertThat(world.process(1)).isFalse();
            verifyHasComposition(entityId, Composition.all(C1.class).none(C2.class, C3.class));

            verifyHasComponent(entityId, C1.class);
            verifyHasComponent(entityId, C2.class);
            verifyDoesNotHaveComponent(entityId, C3.class);

            // Verify second process
            assertThat(world.process(1)).isFalse();
            verifyHasComposition(entityId, Composition.all(C1.class, C2.class).none(C3.class));

            verifyHasComponent(entityId, C1.class);
            verifyHasComponent(entityId, C2.class);
            verifyHasComponent(entityId, C3.class);

            // Verify third process
            assertThat(world.process(1)).isFalse();
            verifyHasComposition(entityId, Composition.all(C1.class, C2.class, C3.class));

            verifyHasComponent(entityId, C1.class);
            verifyHasComponent(entityId, C2.class);
            verifyHasComponent(entityId, C3.class);

            // Verify last process
            assertThat(world.process(1)).isTrue();
            verifyHasComposition(entityId, Composition.all(C2.class, C3.class).none(C1.class));

            verifyDoesNotHaveComponent(entityId, C1.class);
            verifyHasComponent(entityId, C2.class);
            verifyHasComponent(entityId, C3.class);
        }

        @Test
        void testMutationAfterCreation_WhenListenersModifyComponents_ListenersNotCalledRecirsuvly() {
            // Setup composition listeners
            var composition1 = world.createComposition(Composition.all(C1.class));
            composition1.inserted(pooled2::add);

            var composition2 = world.createComposition(Composition.all(C2.class));
            composition2.inserted(pooled3::add);

            var composition3 = world.createComposition(Composition.all(C3.class));
            composition3.inserted(pooled1::remove);

            var entityId = world.createEntity();

            // Call
            pooled1.add(entityId);

            // Verify
            verifyHasComposition(entityId, Composition.none(C1.class, C2.class, C3.class));

            verifyHasComponent(entityId, C1.class);
            verifyDoesNotHaveComponent(entityId, C2.class);
            verifyDoesNotHaveComponent(entityId, C3.class);
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
