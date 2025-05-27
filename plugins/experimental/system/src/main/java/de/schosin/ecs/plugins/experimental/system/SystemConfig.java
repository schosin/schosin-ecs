package de.schosin.ecs.plugins.experimental.system;

import de.schosin.ecs.api.Plugin.PluginConfig;
import de.schosin.ecs.plugins.experimental.system.systems.BaseSystem;
import de.schosin.ecs.utils.collections.Bag;

public class SystemConfig implements PluginConfig {

    private final Bag<BaseSystem> systems = new Bag<>(BaseSystem.class, 64);

    public static SystemConfig builder(BaseSystem... systems) {
        return new SystemConfig().add(systems);
    }

    private SystemConfig() {
    }

    public SystemConfig add(BaseSystem... systems) {
        for (var system : systems) {
            this.systems.add(system);
        }

        return this;
    }

    public Bag<BaseSystem> getSystems() {
        return this.systems;
    }

}
