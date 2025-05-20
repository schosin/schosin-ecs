package de.schosin.ecs.storage.testsuite.initialization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import org.junit.jupiter.api.Test;

import de.schosin.ecs.api.World;
import de.schosin.ecs.api.components.ComponentType;
import de.schosin.ecs.api.components.ComponentType.ClassType;
import de.schosin.ecs.engine.components.ComponentManager;
import de.schosin.ecs.storage.api.components.Component.ClassComponent;

public class EarlyComponentAccessTest {

    private static final ClassType<PluginComponent> TYPE = ComponentType.component(PluginComponent.class);

    @Test
    void testPluginAccessingStorageEngineInConstructor() {
        ComponentAccessingWorld world;
        try {
            world = World.builder(ComponentAccessingWorld.class).build();
        } catch (RuntimeException ex) {
            fail("Plugins must be able to access components after StorageEngine#setWorld", ex);
            return;
        }

        var pluginImpl = world.getSingleton(ComponentAccessingPluginImpl.class);
        assertThat(pluginImpl.component).as("Plugins must be able to access components after StorageEngine#setWorld").isNotNull();
    }

    @Test
    void testComponentsAccessedByPluginInConstructorMustBeSameAs() {
        ComponentAccessingWorld world;
        try {
            world = World.builder(ComponentAccessingWorld.class).build();
        } catch (RuntimeException ex) {
            fail("Plugins must be able to access components after StorageEngine#setWorld", ex);
            return;
        }

        var pluginImpl = world.getSingleton(ComponentAccessingPluginImpl.class);
        var componentManager = world.getSingleton(ComponentManager.class);

        assertThat(componentManager.getComponent(TYPE)).as("Components returned in plugin constructor must be same as returned after StorageEngine#setProxiedWorld").isSameAs(pluginImpl.component);
    }

    public interface ComponentAccessingWorld extends World, ComponentAccessingPlugin {
    }

    public interface ComponentAccessingPlugin {
    }

    public static class ComponentAccessingPluginImpl implements ComponentAccessingPlugin {

        private final ClassComponent<PluginComponent> component;

        public ComponentAccessingPluginImpl(World world) {
            world.addSingleton(this);

            var componentManager = world.getSingleton(ComponentManager.class);
            this.component = componentManager.getComponent(TYPE);
        }

    }

    private record PluginComponent() {
    }

}
