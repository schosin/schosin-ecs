package de.schosin.ecs.examples.simple.example1_physics;

import java.util.Random;

import de.schosin.ecs.examples.simple.AbstractExample;
import de.schosin.ecs.examples.simple.compositions.Acceleration;
import de.schosin.ecs.examples.simple.compositions.Position;
import de.schosin.ecs.examples.simple.compositions.Velocity;
import de.schosin.ecs.plugins.composition.Composition;
import de.schosin.ecs.plugins.data.types.DataType3.Processor3;

public class PhysicsExample extends AbstractExample {

    // Simulated delta
    static final int delta = 1;

    public static void main(String[] args) throws InterruptedException {
        var rng = new Random();

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

        // Create a composition to query entities
        var physics = world.createComposition(Composition.all(Position.class, Velocity.class), Position.class, Velocity.class, Acceleration.class);
        var render = world.createComposition(Composition.all(Position.class), Position.class);

        // Simulated game loop
        var loop = 0;
        while (++loop <= 10) {
            System.out.println("-- Loop " + loop);

            // Run "systems"
            physics.process(PhysicsExample::physicsSystem);
            render.process(PhysicsExample::renderSystem);

            // Process world (not really needed in this example)
            world.process();

            // Simulate frame
            Thread.sleep(1000 * delta);
        }
    }

    static void physicsSystem(int entityId, Position position, Velocity velocity, Acceleration acceleration) {
        if (acceleration != null) {
            velocity.vx += delta * acceleration.ax;
            velocity.vy += delta * acceleration.ay;
        }

        position.x += delta * velocity.vx;
        position.y += delta * velocity.vy;
    }

    static void renderSystem(int entityId, Position position) {
        System.out.println("Entity %d: (%4d, %4d)".formatted(entityId, position.x, position.y));
    }

}
