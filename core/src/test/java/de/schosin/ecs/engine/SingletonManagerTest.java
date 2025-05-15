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
import de.schosin.ecs.engine.components.ComponentManager;
import de.schosin.ecs.engine.components.ComponentMapperManager;
import de.schosin.ecs.engine.components.ComponentMaskManager;
import de.schosin.ecs.engine.components.TransmutationManager;
import de.schosin.ecs.engine.entities.EntityManager;
import de.schosin.ecs.engine.events.EventManager;
import de.schosin.ecs.storage.api.StorageEngine;
import de.schosin.ecs.utils.collections.IntBag;

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
            var storageEngine = StorageEngine.load();

            var eventManager = new EventManager();
            var singletonManager = new SingletonManager(null);
            var bagManager = new BagManager();
            var idManager = new IdManager(bagManager);
            var componentManager = new ComponentManager(storageEngine, null);
            var componentMaskManager = new ComponentMaskManager(bagManager, componentManager);
            var entityManager = new EntityManager(null, idManager, componentManager, componentMaskManager);
            var changeManager = new ChangeManager(eventManager, bagManager, componentManager, componentMaskManager, entityManager);
            var transmutationManager = new TransmutationManager(changeManager, componentManager, componentMaskManager, entityManager);
            var componentMapperManager = new ComponentMapperManager(bagManager, componentManager, transmutationManager);

            return Stream.of(
                    Arguments.of(Named.of("eventManager", eventManager)),
                    Arguments.of(Named.of("singletonManager", singletonManager)),
                    Arguments.of(Named.of("bagManager", bagManager)),
                    Arguments.of(Named.of("idManager", idManager)),
                    Arguments.of(Named.of("componentManager", componentManager)),
                    Arguments.of(Named.of("componentMaskManager", componentMaskManager)),
                    Arguments.of(Named.of("entityManager", entityManager)),
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

    @Test
    void testNestedSingletonCreation() {
        var world = World.builder().build();

        var outer = world.getSingleton(Outer.class);
        assertThat(outer.inner.world).isSameAs(world);
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

    public static class Outer {
        public final Inner inner;

        public Outer(World world) {
            this.inner = world.getSingleton(Inner.class);
        }
    }

    public static class Inner {
        public final World world;

        public Inner(World world) {
            this.world = world;
        }
    }

}
