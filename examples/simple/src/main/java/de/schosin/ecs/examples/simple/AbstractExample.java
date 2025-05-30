package de.schosin.ecs.examples.simple;

import java.util.Random;

import de.schosin.ecs.plugins.composition.Composition;
import de.schosin.ecs.worlds.DefaultWorld;

public abstract class AbstractExample {
    protected static final Random rng = new Random();

    // Create default world instance
    protected static final DefaultWorld world = DefaultWorld.create();

    protected static void removeEntities() {
        // Composition matching all entities to pass each to World#deleteEntity
        world.createComposition(Composition.all()).process(world::deleteEntity);

        // Flush deletions
        world.process();
    }

}
