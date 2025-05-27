package de.schosin.ecs.engine;

import static de.schosin.ecs.api.components.types.ComponentType.component;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import de.schosin.ecs.api.Plugin;
import de.schosin.ecs.api.Plugin.PluginConfig;
import de.schosin.ecs.api.World;
import de.schosin.ecs.api.components.mappers.ComponentMapper;
import de.schosin.ecs.engine.WorldBuilderTest.ComponentAccessingPluginTest.ComponentAccessingPlugin;
import de.schosin.ecs.engine.WorldBuilderTest.ComponentAccessingPluginTest.ComponentAccessingPluginImpl;
import de.schosin.ecs.engine.WorldBuilderTest.MyPluginTest.MyPlugin;
import de.schosin.ecs.engine.WorldBuilderTest.MyPluginTest.MyPluginImpl;
import de.schosin.ecs.engine.WorldBuilderTest.SimplePluginTest.SimplePlugin;
import de.schosin.ecs.engine.components.ComponentManager;
import de.schosin.ecs.engine.utils.exceptions.EcsPluginException;
import de.schosin.ecs.engine.utils.exceptions.EcsWorldCreationException;
import de.schosin.ecs.storage.api.StorageWorld;
import de.schosin.ecs.storage.api.components.Component.ClassComponent;
import de.schosin.ecs.storage.defaultimpl.DefaultStorageEngine;
import de.schosin.ecs.utils.ReflectionUtils;

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
                world.addSingleton(this);

                this.world = world;
                this.componentManager = world.getSingleton(ComponentManager.class);
            }

            @Override
            public int getComponentCount() {
                return componentManager.getComponents().getSize();
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
    static class ComponentAccessingPluginTest {

        @Test
        void testPluginAccessingStorageEngineInConstructor() {
            var world = World.builder(ComponentAccessingWorld.class).build();

            var pluginImpl = world.getSingleton(ComponentAccessingPluginImpl.class);

            assertThat(pluginImpl.component).isNotNull();
            assertThat(pluginImpl.mapper).isNotNull();
        }

        public interface ComponentAccessingWorld extends World, ComponentAccessingPlugin {
        }

        @Plugin(ComponentAccessingPluginImpl.class)
        public interface ComponentAccessingPlugin {
        }

        public static class ComponentAccessingPluginImpl implements ComponentAccessingPlugin {

            private final ClassComponent<PluginComponent> component;
            private final ComponentMapper<PluginComponent> mapper;

            public ComponentAccessingPluginImpl(World world) {
                world.addSingleton(this);

                var type = component(PluginComponent.class);

                this.component = world.getSingleton(ComponentManager.class).getComponent(type);
                this.mapper = world.getComponents(type);
            }

        }

        private record PluginComponent() {
        }

    }

    @Nested
    static class PluginDependencyTest {

        @ParameterizedTest
        @ValueSource(classes = { DependentWorld.class, DependentWorldExtendsDependencyBefore.class, DependentWorldExtendsDependencyAfter.class })
        void testPluginDependencyConstructorInjection(Class<? extends World> clazz) {
            var world = World.builder(clazz).build();

            var pluginImpl = world.getSingleton(DependentPluginImpl.class);
            assertThat(pluginImpl.plugin).isNotNull();

            assertThat(world.getSingleton(MyPluginImpl.class)).isSameAs(pluginImpl.plugin);
        }

        @Test
        void testMultiplePluginDependenciesConstructorInjection() {
            var world = World.builder(MultiDependencyWorld.class).build();

            var pluginImpl = world.getSingleton(MultiDependencyPluginImpl.class);
            assertThat(pluginImpl.myPlugin).isNotNull();
            assertThat(pluginImpl.componentAccessingPlugin).isNotNull();

            assertThat(world.getSingleton(MyPluginImpl.class)).isSameAs(pluginImpl.myPlugin);
            assertThat(world.getSingleton(ComponentAccessingPluginImpl.class)).isSameAs(pluginImpl.componentAccessingPlugin);
        }

        @Test
        void testNestedPluginDependenciesConstructorInjection() {
            var world = World.builder(NestedDependencyWorld.class).build();

            var pluginImpl = world.getSingleton(NestedDependencyPluginImpl.class);
            assertThat(pluginImpl.multiPlugin).isNotNull();
            assertThat(pluginImpl.myPlugin).isNotNull();
            assertThat(pluginImpl.componentAccessingPlugin).isNotNull();

            assertThat(world.getSingleton(MultiDependencyPluginImpl.class)).isSameAs(pluginImpl.multiPlugin);
            assertThat(world.getSingleton(MyPluginImpl.class)).isSameAs(pluginImpl.myPlugin);
            assertThat(world.getSingleton(ComponentAccessingPluginImpl.class)).isSameAs(pluginImpl.componentAccessingPlugin);
        }

        @Test
        void testPluginDependencyDoesNotAlterProxiedWorld() {
            var world = World.builder(DependentWorld.class).build();

            assertThat(world).isNotInstanceOf(MyPlugin.class);
        }

        public interface DependentWorld extends World, DependentPlugin {
        }

        public interface DependentWorldExtendsDependencyBefore extends World, MyPlugin, DependentPlugin {
        }

        public interface DependentWorldExtendsDependencyAfter extends World, DependentPlugin, MyPlugin {
        }

        @Plugin(DependentPluginImpl.class)
        public interface DependentPlugin {
        }

        public static class DependentPluginImpl implements DependentPlugin {

            private final MyPlugin plugin;

            public DependentPluginImpl(World world, MyPlugin plugin) {
                world.addSingleton(this);

                this.plugin = plugin;
            }

        }

        public interface MultiDependencyWorld extends World, MultiDependencyPlugin {
        }

        @Plugin(MultiDependencyPluginImpl.class)
        public interface MultiDependencyPlugin {
        }

        public static class MultiDependencyPluginImpl implements MultiDependencyPlugin {

            private final MyPlugin myPlugin;
            private final ComponentAccessingPlugin componentAccessingPlugin;

            public MultiDependencyPluginImpl(MyPlugin myPlugin, ComponentAccessingPlugin componentAccessingPlugin, World world) {
                world.addSingleton(this);

                this.myPlugin = myPlugin;
                this.componentAccessingPlugin = componentAccessingPlugin;
            }

        }

        public interface NestedDependencyWorld extends World, NestedDependencyPlugin {
        }

        @Plugin(NestedDependencyPluginImpl.class)
        public interface NestedDependencyPlugin {
        }

        public static class NestedDependencyPluginImpl implements NestedDependencyPlugin {

            private final MultiDependencyPluginImpl multiPlugin;
            private final MyPlugin myPlugin;
            private final ComponentAccessingPlugin componentAccessingPlugin;

            public NestedDependencyPluginImpl(World world, MultiDependencyPlugin multiPlugin) {
                world.addSingleton(this);

                this.multiPlugin = (MultiDependencyPluginImpl) multiPlugin;
                this.myPlugin = this.multiPlugin.myPlugin;
                this.componentAccessingPlugin = this.multiPlugin.componentAccessingPlugin;
            }

        }

    }

    @Nested
    static class CustomStorageEngineTest {

        @Test
        void testCustomStorageEngineAccessingPlugin() {
            var world = World.builder(CustomWorld.class).storageEngine(CustomStorageEngine.class).build();

            var plugin = world.getSingleton(CustomPluginImpl.class);
            var engine = world.getSingleton(CustomStorageEngine.class);

            assertThat(engine.plugin).isSameAs(plugin);
        }

        public interface CustomWorld extends World, CustomPlugin {
        }

        @Plugin(CustomPluginImpl.class)
        public interface CustomPlugin {
        }

        public static class CustomPluginImpl implements CustomPlugin {
            public CustomPluginImpl(World world) {
                world.addSingleton(this);
            }
        }

        public static class CustomStorageEngine extends DefaultStorageEngine {

            private CustomPluginImpl plugin;

            @Override
            public void setWorld(StorageWorld world) {
                world.addSingleton(this);
            }

            @Override
            public void setProxiedWorld(StorageWorld world) {
                this.plugin = world.getSingleton(CustomPluginImpl.class);
            }
        }

    }

    @Nested
    static class ConfigurablePluginTest {

        @Nested
        class RequiredPluginConfigTest {

            @Test
            void testPluginConfig() {
                var config = new ConfigurablePluginConfig();

                // Create
                var world = World.builder(ConfigurableWorld.class)
                        .configure(config)
                        .build();

                // Verify
                var plugin = world.getSingleton(ConfigurablePluginImpl.class);
                assertThat(plugin.config).isSameAs(config);
            }

            @Test
            void testMissingPluginConfig() {
                assertThatThrownBy(() -> World.builder(ConfigurableWorld.class).build())
                        .isInstanceOf(EcsWorldCreationException.class)
                        .hasMessageContainingAll(ConfigurablePlugin.class.getName(), ConfigurablePluginConfig.class.getName(), "required config parameter");
            }

            public interface ConfigurableWorld extends World, ConfigurablePlugin {
            }

            @Plugin(ConfigurablePluginImpl.class)
            public interface ConfigurablePlugin {
            }

            public static class ConfigurablePluginConfig implements PluginConfig {
            }

            public static class ConfigurablePluginImpl implements ConfigurablePlugin {

                private final ConfigurablePluginConfig config;

                public ConfigurablePluginImpl(World world, ConfigurablePluginConfig config) {
                    world.addSingleton(this);

                    this.config = config;
                }

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
            assertThatThrownBy(() -> World.builder(InaccessibleConstructorWorld.class).build())
                    .isInstanceOf(EcsWorldCreationException.class)
                    .hasMessageContainingAll(UnaccessibleConstructorPlugin.class.getName(), UnaccessibleConstructorPluginImpl.class.getName(), "Declares 0 public constructors");
        }

        @Test
        void testCreateDynamicWorld_NoMatchingConstructor_Throws() {
            assertThatThrownBy(() -> World.builder(NoMatchingConstructorWorld.class).build())
                    .isInstanceOf(EcsWorldCreationException.class)
                    .hasMessageContainingAll(NoMatchingConstructorPlugin.class.getName(), NoMatchingConstructorPluginImpl.class.getName(), "parameter of unsupported type 'int'");
        }

        @Test
        void testCreateDynamicWorld_MultipleMatchingConstructors_Throws() {
            assertThatThrownBy(() -> World.builder(MultipleConstructorWorld.class).build())
                    .isInstanceOf(EcsWorldCreationException.class)
                    .hasMessageContainingAll(MultipleConstructorPlugin.class.getName(), MultipleConstructorPluginImpl.class.getName(), "Declares 3 public constructors");
        }

        @Test
        void testCreateDynamicWorld_ConstructorThrowsException_Throws() {
            assertThatThrownBy(() -> World.builder(ThrowingConstructorWorld.class).build())
                    .isInstanceOf(EcsWorldCreationException.class)
                    .hasCauseInstanceOf(CustomPluginException.class)
                    .hasMessageContainingAll(ThrowingConstructorPlugin.class.getName(), ThrowingConstructorPluginImpl.class.getName(), CustomPluginException.class.getSimpleName(),
                            "Failed to instantiate plugin", "custom message");
        }

        public interface AbstractMethodWorld extends World {
            int abstractTest();
        }

        public interface ImplementationDoesNotImplementWorld extends World, ImplementationDoesNotImplement {
        }

        @Plugin(MyPluginImpl.class)
        public interface ImplementationDoesNotImplement {
        }

        public interface InaccessibleConstructorWorld extends World, UnaccessibleConstructorPlugin {
        }

        @Plugin(UnaccessibleConstructorPluginImpl.class)
        public interface UnaccessibleConstructorPlugin {
        }

        public static class UnaccessibleConstructorPluginImpl implements UnaccessibleConstructorPlugin {
            private UnaccessibleConstructorPluginImpl() {
            }
        }

        public interface MultipleConstructorWorld extends World, MultipleConstructorPlugin {
        }

        @Plugin(MultipleConstructorPluginImpl.class)
        public interface MultipleConstructorPlugin {
        }

        @SuppressWarnings("unused")
        public static class MultipleConstructorPluginImpl implements MultipleConstructorPlugin {
            public MultipleConstructorPluginImpl() {
            }

            public MultipleConstructorPluginImpl(World world) {
            }

            public MultipleConstructorPluginImpl(MyPlugin plugin) {
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

            public CustomPluginException() {
                super("custom message");
            }

        }

    }

    private record C1() {
    }

}
