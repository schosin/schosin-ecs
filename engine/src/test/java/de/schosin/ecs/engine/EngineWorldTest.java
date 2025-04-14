package de.schosin.ecs.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.stream.Stream;

import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import de.schosin.ecs.api.World;
import de.schosin.ecs.api.World.Builder;
import de.schosin.ecs.api.components.Composition;
import de.schosin.ecs.engine.EngineWorldTest.SingletonTest.CustomBuilder;
import de.schosin.ecs.engine.components.ComponentManager;
import de.schosin.ecs.engine.components.ComponentMapperManager;
import de.schosin.ecs.engine.components.ComponentMaskManager;
import de.schosin.ecs.engine.components.TransmutationManager;
import de.schosin.ecs.engine.compositions.CompositionManager;
import de.schosin.ecs.engine.entities.ArchetypeManager;
import de.schosin.ecs.engine.entities.EntityManager;
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

    }

    @Nested
    class SingletonTest {

        @Nested
        class ManagerOerwriteNotAllowedTest {

            @ParameterizedTest
            @MethodSource("managers")
            void testBuilder(Object manager) {
                var builder = World.builder().singletons(manager);

                assertThatThrownBy(builder::build)
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContainingAll("This world already contains a singleton of type", manager.getClass().getName());
            }

            @ParameterizedTest
            @MethodSource("managers")
            void testAddSingleton(Object manager) {
                var world = World.builder().build();

                assertThatThrownBy(() -> world.addSingleton(manager))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContainingAll("This world already contains a singleton of type", manager.getClass().getName());
            }

            static Stream<Arguments> managers() {
                var bagManager = new BagManager();
                var componentManager = new ComponentManager(bagManager);
                var componentMaskManager = new ComponentMaskManager(bagManager, componentManager);
                var compositionManager = new CompositionManager(bagManager, componentManager);
                var entityManager = new EntityManager(bagManager, componentManager, componentMaskManager, compositionManager);
                var archetypeManager = new ArchetypeManager(componentManager, componentMaskManager, entityManager);
                var changeManager = new ChangeManager(componentManager, compositionManager, entityManager);
                var transmutationManager = new TransmutationManager(changeManager, componentManager, componentMaskManager, entityManager);
                var componentMapperManager = new ComponentMapperManager(bagManager, componentManager, transmutationManager);

                return Stream.of(
                        Arguments.of(Named.of("bagManager", bagManager)),
                        Arguments.of(Named.of("componentManager", componentManager)),
                        Arguments.of(Named.of("componentMaskManager", componentMaskManager)),
                        Arguments.of(Named.of("compositionManager", compositionManager)),
                        Arguments.of(Named.of("entityManager", entityManager)),
                        Arguments.of(Named.of("archetypeManager", archetypeManager)),
                        Arguments.of(Named.of("changeManager", changeManager)),
                        Arguments.of(Named.of("transmutationManager", transmutationManager)),
                        Arguments.of(Named.of("componentMapperManager", componentMapperManager)));
            }

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
        }

        @Test
        void testSingletonWorldConstructor() {
            var world = World.builder().build();

            var shared = assertThat(world.getSingleton(PublicWorld.class)).isNotNull().actual();
            assertThat(shared.world).isSameAs(world);

            assertThat(world.getSingleton(PublicWorld.class)).isSameAs(shared);
        }

        @Test
        void testSingletonCreationErrors() {
            var world = World.builder().build();

            assertThatThrownBy(() -> world.getSingleton(PublicSharedNoDefault.class)).isExactlyInstanceOf(UnsupportedOperationException.class);
            assertThatThrownBy(() -> world.getSingleton(PrivateShared.class)).isExactlyInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        void testAddSingleton() {
            var sharedNoDefault = new PublicSharedNoDefault(1);

            var world = World.builder().build();
            world.addSingleton(sharedNoDefault);

            assertThat(world.getSingleton(PublicSharedNoDefault.class)).isSameAs(sharedNoDefault);
        }

        @Test
        void testAddDuplicateSingleton() {
            var world = World.builder().build();
            world.addSingleton(new PublicSharedNoDefault(1));

            var sharedNoDefault = new PublicSharedNoDefault(2);
            assertThatThrownBy(() -> world.addSingleton(sharedNoDefault)).isInstanceOf(IllegalArgumentException.class);
        }

        public record PublicWorld(World world) {
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
