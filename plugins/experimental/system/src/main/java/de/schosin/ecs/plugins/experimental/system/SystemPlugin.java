package de.schosin.ecs.plugins.experimental.system;

import de.schosin.ecs.api.Plugin;
import de.schosin.ecs.api.World;
import de.schosin.ecs.plugins.experimental.system.systems.BaseSystem;

/**
 * Simple plugin that provides a sequential system invocaton strategy.
 * 
 * <p>
 * Use {@link SystemConfig#builder(BaseSystem...)} and {@link SystemConfig#add(BaseSystem...)}
 * to define the systems and pass the config to {@link World.Builder#configure(de.schosin.ecs.api.Plugin.PluginConfig...)}
 * when creating a world extending this plugin.
 * </p>
 * 
 * <p>
 * Alternatively use {@link SystemPlugin#standalone(World, SystemConfig)} or {@link SystemPlugin#standalone(World, BaseSystem...)} 
 * to create a standalone instance. 
 * </p>
 */
@Plugin(SystemManager.class)
public interface SystemPlugin {

    /**
     * Creates a standalone instance of the plugin.
     * 
     * @param world world to bind to
     * @param systems systems
     * @return standalone instance
     */
    static SystemPlugin standalone(World world, BaseSystem... systems) {
        return standalone(world, SystemConfig.builder(systems));
    }

    /**
     * Creates a standalone instance of the plugin.
     * 
     * @param world world to bind to
     * @param config system configuration
     * @return standalone instance
     */
    static SystemPlugin standalone(World world, SystemConfig config) {
        return new SystemManager(world, config);
    }

    /**
     * Processes the systems and invokes {@link World#process()} afterwards.
     * 
     * @param delta delta passed to {@link BaseSystem#process(float)}
     */
    void processSystems(float delta);

}
