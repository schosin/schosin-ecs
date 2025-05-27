package de.schosin.ecs.plugins.experimental.system;

import de.schosin.ecs.api.World;
import de.schosin.ecs.plugins.experimental.system.systems.BaseSystem;
import de.schosin.ecs.utils.collections.Bag;

public class SystemManager implements SystemPlugin {

    private final World world;
    private final Bag<BaseSystem> systems;

    public SystemManager(World world, SystemConfig config) {
        this.world = world;
        this.systems = new Bag<>(config.getSystems());
    }

    @Override
    public void processSystems(float delta) {
        var data = systems.getData();
        for (int i = 0, s = systems.getSize(); i < s; i++) {
            var system = data[i];
            system.process(delta);
        }

        world.process();
    }

}
