package de.schosin.ecs.plugins.composition.manager;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import de.schosin.ecs.plugins.composition.Composition;
import de.schosin.ecs.plugins.composition.manager.components.records.C1;
import de.schosin.ecs.plugins.composition.manager.components.records.C2;
import de.schosin.ecs.plugins.composition.manager.components.records.C3;
import de.schosin.ecs.utils.collections.IntBag;

public class CompositionTest extends AbstractCompositionPluginTest {

    @Nested
    class AllSpecTest extends AbstractSpecTest {

        @Override
        Composition createComposition() {
            return world.createComposition(Composition.all(C1.class, C2.class));
        }

        @Override
        int createInterestedEntity() {
            return world.createEntity(new C1(), new C2());
        }

        @Override
        int createUninterestedEntity() {
            return world.createEntity(new C1(), new C3());
        }

    }

    @Nested
    class OneSpecTest extends AbstractSpecTest {

        @Override
        Composition createComposition() {
            return world.createComposition(Composition.one(C1.class, C2.class));
        }

        @Override
        int createInterestedEntity() {
            return world.createEntity(new C1(), new C3());
        }

        @Override
        int createUninterestedEntity() {
            return world.createEntity(new C3());
        }

    }

    @Nested
    class NoneSpecTest extends AbstractSpecTest {

        @Override
        Composition createComposition() {
            return world.createComposition(Composition.none(C1.class));
        }

        @Override
        int createInterestedEntity() {
            return world.createEntity(new C2(), new C3());
        }

        @Override
        int createUninterestedEntity() {
            return world.createEntity(new C1());
        }

    }

    @Nested
    class ComplexSpecTest extends AbstractSpecTest {

        @Override
        Composition createComposition() {
            return world.createComposition(Composition.one(C1.class, C2.class).none(C3.class));
        }

        @Override
        int createInterestedEntity() {
            return world.createEntity(new C1());
        }

        @Override
        int createUninterestedEntity() {
            return world.createEntity(new C1(), new C3());
        }

    }

    abstract class AbstractSpecTest {

        abstract Composition createComposition();

        abstract int createInterestedEntity();

        abstract int createUninterestedEntity();

        @Test
        void testProcess() {
            // Setup
            var entities = new IntBag(3);

            createUninterestedEntity();
            var interested1 = createInterestedEntity();
            var interested2 = createInterestedEntity();
            createUninterestedEntity();

            var composition = createComposition();

            // Call
            composition.process(entities::add);

            // Verify
            assertThat(entities.getSize()).as("size").isEqualTo(2);
            assertThat(entities.contains(interested1)).as("contains interested1").isTrue();
            assertThat(entities.contains(interested2)).as("contains interested2").isTrue();
        }

        @Test
        void testCreateEntity() {
            // Setup
            var entities = new IntBag(3);

            createUninterestedEntity();
            createInterestedEntity();
            createInterestedEntity();
            createUninterestedEntity();

            var composition = createComposition();

            // Call
            composition.inserted(entities::add);

            createUninterestedEntity();
            var interested3 = createInterestedEntity();
            var interested4 = createInterestedEntity();
            createUninterestedEntity();

            // Verify
            assertThat(entities.getSize()).as("size").isEqualTo(2);
            assertThat(entities.contains(interested3)).as("contains interested3").isTrue();
            assertThat(entities.contains(interested4)).as("contains interested4").isTrue();
        }

        @Test
        void testRemoved() {
            // Setup
            var entities = new IntBag(3);

            var uninterested1 = createUninterestedEntity();
            var interested1 = createInterestedEntity();
            createInterestedEntity();
            createUninterestedEntity();

            var composition = createComposition();

            // Call
            composition.removed(entities::add);

            var interested3 = createInterestedEntity();

            world.deleteEntity(uninterested1);
            world.deleteEntity(interested1);
            world.deleteEntity(interested3);
            world.process();

            // Verify
            assertThat(entities.getSize()).as("size").isEqualTo(2);
            assertThat(entities.contains(interested1)).as("contains interested1").isTrue();
            assertThat(entities.contains(interested3)).as("contains interested3").isTrue();
        }

        @Test
        void testProcessWithRemovedEntity_BeforeWorldProcess() {
            // Setup
            var entities = new IntBag(3);

            createUninterestedEntity();
            var interested1 = createInterestedEntity();
            var interested2 = createInterestedEntity();
            createUninterestedEntity();

            var composition = createComposition();

            // Call
            world.deleteEntity(interested1);
            composition.process(entities::add);

            // Verify
            assertThat(entities.getSize()).as("size").isEqualTo(2);
            assertThat(entities.contains(interested1)).as("contains interested1").isTrue();
            assertThat(entities.contains(interested2)).as("contains interested2").isTrue();
        }

        @Test
        void testProcessWithRemovedEntity_AfterWorldProcess() {
            // Setup
            var entities = new IntBag(3);

            createUninterestedEntity();
            var interested1 = createInterestedEntity();
            var interested2 = createInterestedEntity();
            createUninterestedEntity();

            var composition = createComposition();

            // Call
            world.deleteEntity(interested1);
            world.process();

            composition.process(entities::add);

            // Verify
            assertThat(entities.getSize()).as("size").isEqualTo(1);
            assertThat(entities.contains(interested2)).as("contains interested2").isTrue();
        }

    }

}
