package de.schosin.ecs.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import de.schosin.ecs.api.Plugin;
import de.schosin.ecs.api.World;
import de.schosin.ecs.engine.WorldBuilderTest.MyPluginTest.MyPlugin;
import de.schosin.ecs.engine.WorldBuilderTest.MyPluginTest.MyPluginImpl;
import de.schosin.ecs.engine.WorldBuilderTest.SimplePluginTest.SimplePlugin;
import de.schosin.ecs.engine.components.ComponentManager;
import de.schosin.ecs.engine.utils.collections.ReflectionUtils;
import de.schosin.ecs.engine.utils.exceptions.EcsPluginException;
import de.schosin.ecs.engine.utils.exceptions.EcsWorldCreationException;

public class WorldBuilderTest {

    @Nested
    static class DynamicWorldTest {

        @Test
        void testCreateDynamicWorld_WhenNoPlugins_DefaultMethodsWork() {
            var world = World.builder(DynamicWorld.class).build();

            assertThat(world.test()).isEqualTo(42);
        }

        @Test
        void testCreateDynamicWorld_WhenNoPlugins_WorldMethodsWork() {
            var world = World.builder(DynamicWorld.class).build();

            // Call
            var entityId = world.createEntity(new C1());

            var mapper1 = world.getComponents(C1.class);
            assertThat(mapper1.has(entityId)).isTrue();

            assertThat(world.process()).isTrue();
        }

        public interface DynamicWorld extends World {
            default int test() {
                return 42;
            }
        }

    }

    @Nested
    static class MyPluginTest {

        @Test
        void testCreateDynamicWorld_PluginMethodsWork() {
            var world = World.builder(MyWorld.class).build();

            assertThat(world.getComponentCount()).isEqualTo(0);

            world.createEntity(new C1());
            assertThat(world.getComponentCount()).isEqualTo(1);
        }

        @Test
        void testCreateDynamicWorld_WorldMethodsWork() {
            var world = World.builder(MyWorld.class).build();

            // Call
            var entityId = world.createEntity(new C1());

            var mapper1 = world.getComponents(C1.class);
            assertThat(mapper1.has(entityId)).isTrue();

            assertThat(world.process()).isTrue();
        }

        @Test
        void testReflectionUtilsCreateInstance_WhenInvokedOnProxy_PassesProxyToConstructor() {
            var world = World.builder(MyWorld.class).build();

            var singleton = ReflectionUtils.createInstance(world, Singleton.class);
            assertThat(singleton.plugin).isSameAs(world);
        }

        @Test
        void testReflectionUtilsCreateInstance_WhenInvokedWithinPlugin_PassesProxyToConstructor() {
            var world = World.builder(MyWorld.class).build();

            var singleton = world.getSingleton();
            assertThat(singleton.plugin).isSameAs(world);
        }

        public interface MyWorld extends World, MyPlugin {
        }

        @Plugin(MyPluginImpl.class)
        public interface MyPlugin {

            int getComponentCount();

            Singleton getSingleton();

        }

        public static class MyPluginImpl implements MyPlugin {

            private final World world;
            private final ComponentManager componentManager;

            public MyPluginImpl(EngineWorld world) {
                this.world = world;
                this.componentManager = world.getSingleton(ComponentManager.class);
            }

            @Override
            public int getComponentCount() {
                return componentManager.getComponents().size();
            }

            @Override
            public Singleton getSingleton() {
                return world.getSingleton(Singleton.class);
            }

        }

        public record Singleton(MyPlugin plugin) {
        }

    }

    @Nested
    static class SimplePluginTest {

        @Test
        void testCreateDynamicWorld_WhenWithPlugin_PluginMethodsWork() {
            var world = World.builder(SimpleWorld.class).build();

            assertThat(world.getAnswer()).isEqualTo(42);
        }

        @Test
        void testCreateDynamicWorld_WhenWithPlugin_WorldMethodsWork() {
            var world = World.builder(SimpleWorld.class).build();

            // Call
            var entityId = world.createEntity(new C1());

            var mapper1 = world.getComponents(C1.class);
            assertThat(mapper1.has(entityId)).isTrue();

            assertThat(world.process()).isTrue();
        }

        public interface SimpleWorld extends World, SimplePlugin {
        }

        @Plugin(SimplePluginImpl.class)
        public interface SimplePlugin {
            int getAnswer();
        }

        public static class SimplePluginImpl implements SimplePlugin {
            @Override
            public int getAnswer() {
                return 42;
            }
        }

    }

    @Nested
    static class CombinedTypes {

        @Test
        void testCreateDynamicWorld_WhenMultiplePlugins_PluginMethodsWork() {
            var world = World.builder(MySimpleWorld.class).build();

            // MyPlugin
            assertThat(world.getComponentCount()).isEqualTo(0);

            world.createEntity(new C1());
            assertThat(world.getComponentCount()).isEqualTo(1);

            // SimplePlugin
            assertThat(world.getAnswer()).isEqualTo(42);
        }

        @Test
        void testCreateDynamicWorld_WhenMultiplePlugins_WorldMethodsWork() {
            var world = World.builder(MySimpleWorld.class).build();

            // Call
            var entityId = world.createEntity(new C1());

            var mapper1 = world.getComponents(C1.class);
            assertThat(mapper1.has(entityId)).isTrue();

            assertThat(world.process()).isTrue();
        }

        @Test
        void testCreateDynamicWorld_WhenMethodCollision_PrioritizesFirst() {
            var firstSecondWorld = World.builder(FirstSecondPluginWorld.class).build();
            assertThat(firstSecondWorld.getAnswer()).isEqualTo(42);

            var secondFirstWorld = World.builder(SecondFirstPluginWorld.class).build();
            assertThat(secondFirstWorld.getAnswer()).isEqualTo(9001);
        }

        public interface MySimpleWorld extends World, MyPlugin, SimplePlugin {
        }

        public interface FirstSecondPluginWorld extends World, SimplePlugin, SecondSimplePlugin {
        }

        public interface SecondFirstPluginWorld extends World, SecondSimplePlugin, SimplePlugin {
        }

        @Plugin(SecondSimplePluginImpl.class)
        public interface SecondSimplePlugin {
            int getAnswer();
        }

        public static class SecondSimplePluginImpl implements SecondSimplePlugin {
            @Override
            public int getAnswer() {
                return 9001;
            }
        }

    }

    @Nested
    static class InvalidTypesTest {

        @Test
        void testCreateDynamicWorld_WhenPluginDefinesIncorrectImplementation_Throws() {
            assertThatThrownBy(() -> World.builder(ImplementationDoesNotImplementWorld.class).build())
                    .isInstanceOf(EcsPluginException.class)
                    .hasMessageContainingAll(MyPluginImpl.class.getName(), ImplementationDoesNotImplement.class.getName(), "does not implement plugin");
        }

        @Test
        void testCreateDynamicWorld_WhenWorldWithAbstractMethod_Throws() {
            assertThatThrownBy(() -> World.builder(AbstractMethodWorld.class))
                    .isInstanceOf(EcsWorldCreationException.class)
                    .hasMessageContainingAll(AbstractMethodWorld.class.getName(), "must not declare abstract methods", "abstractTest");
        }

        @Test
        void testCreateDynamicWorld_WhenPluginImplPrivateConstructor_Throws() {
            assertThatThrownBy(() -> World.builder(UnaccessibleConstructorWorld.class).build())
                    .isInstanceOf(EcsWorldCreationException.class)
                    .hasMessageContainingAll(UnaccessibleConstructorPlugin.class.getName(), UnaccessibleConstructorPluginImpl.class.getName(), "cannot access", "private");
        }

        @Test
        void testCreateDynamicWorld_NoMatchingConstructor_Throws() {
            assertThatThrownBy(() -> World.builder(NoMatchingConstructorWorld.class).build())
                    .isInstanceOf(EcsWorldCreationException.class)
                    .hasMessageContainingAll(NoMatchingConstructorPlugin.class.getName(), NoMatchingConstructorPluginImpl.class.getName(),
                            "must have either a constructor accepting World", "default constructor");
        }

        @Test
        void testCreateDynamicWorld_ConstructorThrowsException_Throws() {
            assertThatThrownBy(() -> World.builder(ThrowingConstructorWorld.class).build())
                    .isInstanceOf(EcsWorldCreationException.class)
                    .hasCauseInstanceOf(CustomPluginException.class)
                    .hasMessageContainingAll(ThrowingConstructorPlugin.class.getName(), ThrowingConstructorPluginImpl.class.getName(), "Failed to instantiate plugin");
        }

        public interface AbstractMethodWorld extends World {
            int abstractTest();
        }

        public interface ImplementationDoesNotImplementWorld extends World, ImplementationDoesNotImplement {
        }

        @Plugin(MyPluginImpl.class)
        public interface ImplementationDoesNotImplement {
        }

        public interface UnaccessibleConstructorWorld extends World, UnaccessibleConstructorPlugin {
        }

        @Plugin(UnaccessibleConstructorPluginImpl.class)
        public interface UnaccessibleConstructorPlugin {
        }

        public static class UnaccessibleConstructorPluginImpl implements UnaccessibleConstructorPlugin {
            private UnaccessibleConstructorPluginImpl() {
            }
        }

        public interface NoMatchingConstructorWorld extends World, NoMatchingConstructorPlugin {
        }

        @Plugin(NoMatchingConstructorPluginImpl.class)
        public interface NoMatchingConstructorPlugin {
        }

        public static class NoMatchingConstructorPluginImpl implements NoMatchingConstructorPlugin {
            @SuppressWarnings("unused")
            public NoMatchingConstructorPluginImpl(int value) {
            }
        }

        public interface ThrowingConstructorWorld extends World, ThrowingConstructorPlugin {
        }

        @Plugin(ThrowingConstructorPluginImpl.class)
        public interface ThrowingConstructorPlugin {
        }

        public static class ThrowingConstructorPluginImpl implements ThrowingConstructorPlugin {
            public ThrowingConstructorPluginImpl() {
                throw new CustomPluginException();
            }
        }

        static class CustomPluginException extends RuntimeException {
            private static final long serialVersionUID = 1L;
        }

    }

    private record C1() {
    }

}
