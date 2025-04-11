package de.schosin.ecs.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import de.schosin.ecs.api.World;
import de.schosin.ecs.api.World.Builder;
import de.schosin.ecs.api.components.Composition;
import de.schosin.ecs.engine.utils.collections.IntBag;

public class EngineWorldTest extends AbstractWorldTest {

    @Nested
    class WorldCreationTest {

        @Test
        void testDefault() {
            assertThat(World.builder()).isInstanceOf(WorldBuilder.class);
            assertThat(World.builder().build()).isInstanceOf(EngineWorld.class);
        }

        @Test
        void testCustomBuilder() {
            assertThat(World.builder(CustomBuilder.class.getName())).isInstanceOf(CustomBuilder.class);
        }

        @Test
        void testUnknownBuilderClass() {
            assertThatThrownBy(() -> World.builder("foo.bar.Builder")).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void testConfiguration() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
            var world = (EngineWorld) World.builder()
                    .processLoops(42)
                    .build();

            var config = getField(world, "config");
            assertThat(config)
                    .hasOnlyFields("processLoops") // add assertions for new fields
                    .hasFieldOrPropertyWithValue("processLoops", 42);
        }

        @Test
        void testPassedSingletons() {
            var sharedBag = new IntBag(1);

            var world = (EngineWorld) World.builder()
                    .processLoops(42)
                    .singletons("foobar", sharedBag)
                    .build();

            assertThat(world.getSingleton(String.class)).isEqualTo("foobar");
            assertThat(world.getSingleton(IntBag.class)).isSameAs(sharedBag);
        }

        @Test
        void testSingletonCreation() {
            var world = World.builder().build();

            var shared = assertThat(world.getSingleton(PublicShared.class)).isNotNull().actual();
            assertThat(world.getSingleton(PublicShared.class)).isSameAs(shared);

            assertThatThrownBy(() -> world.getSingleton(PublicSharedNoDefault.class)).isExactlyInstanceOf(UnsupportedOperationException.class);
            assertThatThrownBy(() -> world.getSingleton(PrivateShared.class)).isExactlyInstanceOf(UnsupportedOperationException.class);
        }

        public static class PublicShared {
        }

        public static class PublicSharedNoDefault {
            @SuppressWarnings("unused")
            public PublicSharedNoDefault(int foo) {
            }
        }

        private static class PrivateShared {
        }

        public static class CustomBuilder implements World.Builder {

            @Override
            public Builder processLoops(int loops) {
                return this;
            }

            @Override
            public Builder singletons(Object... singletons) {
                return this;
            }

            @Override
            public World build() {
                return new EngineWorld(new WorldBuilder());
            }

        }

    }

    @Nested
    class CompositionTest {

        // TODO test entities processed/inserted/removed due to composition change of entity

        @Nested
        class AllSpecTest extends AbstractSpecTest {

            @Override
            Composition createComposition() {
                return world.createComposition(Composition.all(Component1.class, Component2.class));
            }

            @Override
            int createInterestedEntity() {
                return world.createEntity(new Component1(1), new Component2(1));
            }

            @Override
            int createUninterestedEntity() {
                return world.createEntity(new Component1(1), new Component3(1));
            }

        }

        @Nested
        class OneSpecTest extends AbstractSpecTest {

            @Override
            Composition createComposition() {
                return world.createComposition(Composition.one(Component1.class, Component2.class));
            }

            @Override
            int createInterestedEntity() {
                return world.createEntity(new Component1(1), new Component3(1));
            }

            @Override
            int createUninterestedEntity() {
                return world.createEntity(new Component3(1));
            }

        }

        @Nested
        class NoneSpecTest extends AbstractSpecTest {

            @Override
            Composition createComposition() {
                return world.createComposition(Composition.one(Component1.class));
            }

            @Override
            int createInterestedEntity() {
                return world.createEntity(new Component1(1), new Component3(1));
            }

            @Override
            int createUninterestedEntity() {
                return world.createEntity(new Component3(1));
            }

        }

        @Nested
        class ComplexSpecTest extends AbstractSpecTest {

            @Override
            Composition createComposition() {
                return world.createComposition(Composition.one(Component1.class, Component2.class).none(Component3.class));
            }

            @Override
            int createInterestedEntity() {
                return world.createEntity(new Component1(1));
            }

            @Override
            int createUninterestedEntity() {
                return world.createEntity(new Component1(1), new Component3(1));
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

    private record Component1(int data) {
    }

    private record Component2(int data) {
    }

    private record Component3(int data) {
    }

}
