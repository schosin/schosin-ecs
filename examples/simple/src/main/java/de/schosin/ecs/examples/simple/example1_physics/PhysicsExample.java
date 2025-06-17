package de.schosin.ecs.examples.simple.example1_physics;

import de.schosin.ecs.examples.simple.AbstractExample;
import de.schosin.ecs.examples.simple.components.Acceleration;
import de.schosin.ecs.examples.simple.components.Position;
import de.schosin.ecs.examples.simple.components.Velocity;
import de.schosin.ecs.plugins.composition.Composition;

/**
 * This example will explain how to create entities and how to iterate 
 * over entities and accessing their components.
 * 
 * <p>
 * It implements a simple simulation that moves entities on a 2D space
 * by updating its position during the "game loop".
 */
public class PhysicsExample extends AbstractExample {

    // Simulated delta
    static final int delta = 1;

    public static void main(String[] args) throws InterruptedException {
        /*
         * The simplest way to create an entity is to call World#createEntity and pass
         * in the components.
         * 
         * Here we create a couple of entities in a loop and assign a position and velocity
         * to each, and every other entity also gets an acceleration assigned.
         */
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

        /*
         * Compositions are the main way to implement systems. 
         * While there is an experimental system plugin, we keep it simple for these examples. 
         * 
         * When creating a composition the first argument is a Composition.Builder. This acts
         * as the "WHERE" clause for the entities.
         * 
         *      "Composition.all(Position.class, Velocity.class)" means that the composition will
         *      contain only entities that have a position and velocity component.
         *      
         *      You can read it as "must have all of ...". 
         * 
         * After the first arguments comes the "SELECT" clause. Here you can define which
         * components you want to load when processing the entities.
         * 
         *      Look at the actual type physics and render and you will see that they are
         *      CompositionData3<...> and CompositionData1<...>, where the type parameters
         *      match the components passed after the builder.
         *      
         *      Ignore the remaining overloads for now. They are explained in examples 3 and 4 
         *      in more detail.
         */
        var physics = world.createComposition(Composition.all(Position.class, Velocity.class), Position.class, Velocity.class, Acceleration.class);
        var render = world.createComposition(Composition.all(Position.class), Position.class);

        // Simulated game loop
        var loop = 0;
        while (++loop <= 10) {
            System.out.println();
            System.out.println("-- Loop " + loop);

            /*
             * Here we run the "systems" in our game loop.
             * 
             * A CompositionDataN provides two ways to iterate over all the entities.
             * Here we are using a method that is defined by the "SELECT" clause mentioned
             * earlier. 
             * 
             *      If you look at the signature of physics#process, you will see a
             *      Processor3<...> once again matching the type parameters when creating
             *      the composition.
             *      
             *      Try swapping Position.class and Velocity.class and see how the call won't
             *      compile until the method "physicsSystem" reflects that swap as well.
             */
            physics.process(PhysicsExample::physicsSystem);
            render.process(PhysicsExample::renderSystem);

            /*
             * This processes the world itself.
             * 
             * To ensure a consistent state and no errors during iteration, a couple of operations
             * performed with the world will be delayed. These include adding and removing components
             * from entities, and deleting entities.
             * Additionally there are other maintenance tasks that are performed here that require a
             * call to this method.
             * 
             * This method **MUST** always be called at the end of a game loop and must not overlap with
             * any system processing.
             * 
             * Example 2 will go into a bit more detail on these delayed operations.
             */
            world.process();

            // Simulate frame
            Thread.sleep(1000 * delta);
        }
    }

    /**
     * A simple implementation of a physics system. The actual logic is not that important.
     */
    @SuppressWarnings("unused")
    public static void physicsSystem(int entityId, Position position, Velocity velocity, Acceleration acceleration) {
        if (acceleration != null) {
            velocity.vx += delta * acceleration.ax;
            velocity.vy += delta * acceleration.ay;
        }

        position.x += delta * velocity.vx;
        position.y += delta * velocity.vy;
    }

    /**
     * A simple representation of a render system. For a graphical version of this example, see the libgdx module. 
     */
    static void renderSystem(int entityId, Position position) {
        System.out.println("Entity %d: (%4d, %4d)".formatted(entityId, position.x, position.y));
    }

}
