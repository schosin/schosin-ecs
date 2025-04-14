package de.schosin.ecs.engine.entities;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import de.schosin.ecs.api.components.Composition;
import de.schosin.ecs.engine.AbstractWorldTest;
import de.schosin.ecs.engine.components.ComponentData;
import de.schosin.ecs.engine.compositions.Spec;
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
            var entityId = entityManager.createEntity(Component1.class);

            world.deleteEntity(entityId);
            world.process();

            assertThat(entityManager.createEntity(Component2.class)).isEqualTo(entityId);
            assertThat(entityManager.createEntity(Component2.class)).isGreaterThan(entityId);
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

            assertThatThrownBy(() -> world.createEntity(component1, component2, otherComponent1, component2))
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

            var spec = Spec.create(null, null, null);

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

            var spec = Spec.create(all, null, null);

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

            var spec = Spec.create(null, one, null);

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

            var spec = Spec.create(null, null, none);

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

            var spec = Spec.create(all, null, none);

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
            var spec = Spec.create(null, null, null);
            var entities = entityManager.getEntities(spec);

            assertThat(entityManager.getEntities(spec)).isNotSameAs(entities);
        }

        @Test
        void testResultSizeSynchronized() {
            var spec = Spec.create(null, null, null);
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

    private record Component1() {
    }

    private record Component2() {
    }

    private record Component3() {
    }

}
