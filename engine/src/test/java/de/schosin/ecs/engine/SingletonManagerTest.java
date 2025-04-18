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
import de.schosin.ecs.engine.components.ComponentManager;
import de.schosin.ecs.engine.components.ComponentMapperManager;
import de.schosin.ecs.engine.components.ComponentMaskManager;
import de.schosin.ecs.engine.components.TransmutationManager;
import de.schosin.ecs.engine.compositions.CompositionManager;
import de.schosin.ecs.engine.entities.ArchetypeManager;
import de.schosin.ecs.engine.entities.EntityManager;
import de.schosin.ecs.engine.entities.StateManager;
import de.schosin.ecs.engine.utils.collections.IntBag;

class SingletonManagerTest extends AbstractWorldTest {

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
            var singletonManager = new SingletonManager(null);
            var bagManager = new BagManager();
            var stateManager = new StateManager(bagManager, null);
            var componentManager = new ComponentManager(bagManager, null);
            var componentMaskManager = new ComponentMaskManager(bagManager, componentManager);
            var compositionManager = new CompositionManager(bagManager, componentManager);
            var entityManager = new EntityManager(bagManager, componentManager, componentMaskManager, compositionManager);
            var archetypeManager = new ArchetypeManager(componentManager, componentMaskManager, entityManager);
            var changeManager = new ChangeManager(componentManager, compositionManager, entityManager);
            var transmutationManager = new TransmutationManager(changeManager, componentManager, componentMaskManager, entityManager);
            var componentMapperManager = new ComponentMapperManager(bagManager, componentManager, transmutationManager);

            return Stream.of(
                    Arguments.of(Named.of("singletonManager", singletonManager)),
                    Arguments.of(Named.of("bagManager", bagManager)),
                    Arguments.of(Named.of("stateManager", stateManager)),
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
