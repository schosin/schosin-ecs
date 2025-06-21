package de.schosin.ecs.plugins.experimental.system.systems;

import java.util.concurrent.Callable;

import de.schosin.ecs.plugins.experimental.system.SystemPlugin;

/**
 * Base system to use with {@link SystemPlugin}.
 * 
 * <p>
 * Supports method references ({@code world.addSystems(this::mySystem))} and lambdas ({@code world.addSystems(() -> mySystem(someVariable)}). <br />
 * For more advanced systems, see {@link AbstractSystem} and its abstract subtypes.
 * </p>
 */
@FunctionalInterface
public interface BaseSystem extends Callable<Void> {

    void process();

    @Override
    default Void call() {
        process();
        return null;
    }

}
