package de.schosin.ecs.examples.simple.example2_entitylifecycle;

import de.schosin.ecs.api.components.mappers.ComponentMapper;
import de.schosin.ecs.examples.simple.AbstractExample;
import de.schosin.ecs.examples.simple.compositions.Acceleration;
import de.schosin.ecs.examples.simple.compositions.Position;
import de.schosin.ecs.examples.simple.compositions.Velocity;
import de.schosin.ecs.plugins.composition.Composition;

public class EntityLifecycleExample extends AbstractExample {

    // Simulated delta
    static final int delta = 1;

    // Create component mapper for Acceleration
    static final ComponentMapper<Acceleration> accelerationM = world.getComponents(Acceleration.class);

    public static void main(String[] args) throws InterruptedException {
        // Create a composition to query entities
        var physics = world.createComposition(Composition.all(Position.class, Velocity.class), Position.class, Velocity.class, Acceleration.class);
        var unaccelerated = world.createComposition(Composition.all(Position.class, Velocity.class).none(Acceleration.class));
        var accelerated = world.createComposition(Composition.all(Position.class, Velocity.class, Acceleration.class));
        var all = world.createComposition(Composition.all(), Position.class);

        // Create inserted and removed listeners
        physics.inserted((entityId, pos, vel, accel) -> System.out.println("Physics: Entity %d inserted: %s / %s / %s".formatted(entityId, pos != null, vel != null, accel != null)));
        physics.removed((entityId, pos, vel, accel) -> System.out.println("Physics: Entity %d removed: %s / %s / %s".formatted(entityId, pos != null, vel != null, accel != null)));

        unaccelerated.inserted(entityId -> System.out.println("Unaccelerated: Entity %d inserted".formatted(entityId)));
        unaccelerated.removed(entityId -> System.out.println("Unaccelerated: Entity %d removed".formatted(entityId)));

        accelerated.inserted(entityId -> System.out.println("Accelerated: Entity %d inserted".formatted(entityId)));
        accelerated.removed(entityId -> System.out.println("Accelerated: Entity %d removed".formatted(entityId)));

        all.inserted(entityId -> System.out.println("Render: Entity %d inserted".formatted(entityId)));
        all.removed(entityId -> System.out.println("Render: Entity %d removed".formatted(entityId)));

        // Create a few entities
        for (int i = 0; i < 5; i++) {
            var position = new Position(rng.nextInt(-20, 20), rng.nextInt(-20, 20));
            var velocity = new Velocity(rng.nextInt(-20, 20), rng.nextInt(-20, 20));

            if (i % 2 == 0) {
                world.createEntity(position, velocity);
            } else {
                var acceleration = new Acceleration(rng.nextInt(-20, 20), rng.nextInt(-20, 20));

                world.createEntity(position, velocity, acceleration);
            }
        }

        // Simulated game loop
        var loop = 0;
        while (++loop <= 10) {
            System.out.println("-- Loop " + loop);

            // Run "systems"
            all.process(EntityLifecycleExample::maybeRemove);
            unaccelerated.process(EntityLifecycleExample::maybeAccelerate);
            maybeSpawn();

            // Process world to flush deletions
            System.out.println("Processing world");
            world.process();

            // Simulate frame
            Thread.sleep(1000 * delta);
        }
    }

    static void maybeRemove(int entityId) {
        if (rng.nextFloat() < 0.1f) {
            // Deleting an entity invokes inserted/removed during world.process()

            System.err.println("Deleting %d".formatted(entityId));
            world.deleteEntity(entityId);
        }
    }

    static void maybeAccelerate(int entityId) {
        if (rng.nextFloat() < 0.1f) {
            // Altering the composition of an entity invokes inserted/removed during world.process()
            // Adding Acceleration to the entity will trigger unaccelerated.removed and accelerated.inserted

            System.err.println("Accelerating %d".formatted(entityId));
            accelerationM.add(entityId, new Acceleration(10, 0));
        }
    }

    private static void maybeSpawn() {
        if (rng.nextFloat() < 0.2f) {
            var position = new Position(rng.nextInt(-20, 20), rng.nextInt(-20, 20));
            var velocity = new Velocity(rng.nextInt(-20, 20), rng.nextInt(-20, 20));

            // Creating an entity immediately invokes inserted of interested compositions
            var entityId = world.createEntity(position, velocity);
            System.err.println("Spawned %d".formatted(entityId));
        }
    }

}
