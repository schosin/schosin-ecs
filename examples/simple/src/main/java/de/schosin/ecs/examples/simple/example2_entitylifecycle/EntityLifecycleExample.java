package de.schosin.ecs.examples.simple.example2_entitylifecycle;

import de.schosin.ecs.api.World;
import de.schosin.ecs.api.components.mappers.ComponentMapper;
import de.schosin.ecs.examples.simple.AbstractExample;
import de.schosin.ecs.examples.simple.components.Acceleration;
import de.schosin.ecs.examples.simple.components.Position;
import de.schosin.ecs.examples.simple.components.Velocity;
import de.schosin.ecs.plugins.composition.Composition;

/**
 * This example explains the lifecycle of an entity and explains how
 * to add and remove components, and how to delete entities and when these operations 
 * actually happen.
 * 
 * <p>
 * Additionally it shows how to react to changes in the world using compositions.
 * 
 * <p>
 * Same as example 1 this is a simple simulation for moving entities, but this time
 * also includes adding and removing the acceleration component and deleting and creating
 * entities within the "game loop".
 */
public class EntityLifecycleExample extends AbstractExample {

    /**
     * Chance to remove a entity. 
     * 
     * Ignore for now, but keep it within 0..1f if you change it.
     */
    private static final float REMOVE_CHANCE = 0.1f;

    /**
     * Chance to accelerate a entity. 
     * 
     * Ignore for now, but keep it within 0..1f if you change it.
     */
    private static final float ACCELERATE_CHANCE = 0.1f;

    /**
     * Chance to spawn a entity. 
     * 
     * Ignore for now, but keep it within 0..1f if you change it.
     */
    private static final float SPAWN_CHANCE = 0.2f;

    // Simulated delta
    static final int delta = 1;

    /**
     * This is a component mapper that can be used to add, remove and access components for
     * entities given their id.
     * 
     * It is retrieved from the {@link World}. While the instances themselves are reused, it is
     * still suggested to store these in fields and not retrieve them within the game loop.
     * 
     * This particular component mapper is for the {@link Acceleration} component. 
     * 
     * There are many more overloads of {@link World#getComponents(Class)}, but we ignore these for now.
     * Example 3 will go into more detail on the "regular" components and their mappers.
     */
    static final ComponentMapper<Acceleration> accelerationM = world.getComponents(Acceleration.class);

    public static void main(String[] args) throws InterruptedException {
        /*
         * Same as in example 1 we create a few compositions to implement out "systems".
         *
         *      The composition "unaccelerated" calls both all(...) and none(...) for the same composition builder.
         *      This will select all entities that have a Position and Velocity, but no Acceleration.
         *      
         *      Note that unaccelerated and accelerated have no "SELECT" clause, only passing in the builder.
         *      The resulting types will be simple "Composition", which will only provide a way to iterate over
         *      the ids of the contained entities.
         *      
         *      For the composition "all" the special builder "Composition.all()" is used. This simply selects
         *      all active entities in the world, as "all entities have all of no components" ;-)
         * 
         */
        var physics = world.createComposition(Composition.all(Position.class, Velocity.class), Position.class, Velocity.class, Acceleration.class);
        var unaccelerated = world.createComposition(Composition.all(Position.class, Velocity.class).none(Acceleration.class));
        var accelerated = world.createComposition(Composition.all(Position.class, Velocity.class, Acceleration.class));
        var all = world.createComposition(Composition.all(), Position.class);

        /*
         * To explain the lifecycle of an entity, we are using the two new methods "inserted" and "removed" of Composition and CompositionDataN.
         * 
         * When hovering over their signature you will note that they match the signature of the "process" method used in the previous example.
         * This time we're just implementing them with a lambda instead of a method reference as in the first example.
         * 
         * The "inserted" method allows us to register a callback for whenever a matching entity is created, 
         * or an existing entity is changed such that it now matches the composition.
         *  
         * The "removed" method similarly allows us to react when a matching entity is deleted,
         * or when a matching entity is changed such that is no longer matches the composition.
         */
        physics.inserted((entityId, pos, vel, accel) -> System.out.println("Physics: Entity %d inserted: %s / %s / %s".formatted(entityId, pos, vel, accel)));
        physics.removed((entityId, pos, vel, accel) -> System.out.println("Physics: Entity %d removed: %s / %s / %s".formatted(entityId, pos, vel, accel)));

        /*
         * These two are a bit more interesting. They will show the order of how things happen when adding a component to an entity.
         * 
         * All entities will be created unaccelerated, but some of them will be accelerated during the game loop. 
         * The logic for this is implemented in "maybeAccelerate", which simply adds a acceleration component to the
         * entity using the component mapper (see the field above).
         * 
         * When the component is added, nothing much happens yet because these adding and removing components is delayed,
         * same as deleting entities. But the next time "world.process()" is called, these changes will be applied:
         * 
         *      Accelerating 3
         *      Unaccelerated: Entity 3 removed
         *      Accelerated: Entity 3 inserted
         * 
         * When running this example, you will be able to see the above example within a single game loop, possibly with
         * some extra lines in between. "Accelerating 3" is logged when the system actually adds the component.
         * 
         * When "world.process()" is called, the composition change will be applied. Since "unaccelerated" says its not 
         * interested in entities with an Acceleration, the removed callback is called. Afterwards the "accelerated"
         * compostion will have its inserted callback called because the entity now has all three components.
         */
        unaccelerated.inserted(entityId -> System.out.println("Unaccelerated: Entity %d inserted".formatted(entityId)));
        unaccelerated.removed(entityId -> System.out.println("Unaccelerated: Entity %d removed".formatted(entityId)));

        /*
         * Small detail on "removed": When removed is called, you still have access to all the components the entity
         * has.
         * 
         * This since example does not remove any components, we can only test it when an entity is removed, but try
         * adding "Acceleration.class" after the builder for "accelerated" and change the lambda to:
         * 
         *      (entityId, acceleration) -> System.out.println("Accelerated: Entity %d removed, %s".formatted(entityId, acceleration))
         * 
         * Any "removed" callback will be invoked before the actual removal of components, which allows a system to perform
         * clean up tasks if components hold some data that needs cleaning up (there's also "Pooled", but we'll get to that later)
         */
        accelerated.inserted(entityId -> System.out.println("Accelerated: Entity %d inserted".formatted(entityId)));
        accelerated.removed(entityId -> System.out.println("Accelerated: Entity %d removed".formatted(entityId)));

        all.inserted(entityId -> System.out.println("Render: Entity %d inserted".formatted(entityId)));
        all.removed(entityId -> System.out.println("Render: Entity %d removed".formatted(entityId)));

        /*
         * Same as in the last example we create a couple of entities, just none accelerated this time.
         * 
         * Since we have registered all those "inserted" callbacks beforehand, we will see a bunch of
         * logs before the first game loop:
         * 
         *      - Create entity 0
         *      Render: Entity 1 inserted
         *      Unaccelerated: Entity 1 inserted
         *      Physics: Entity 1 inserted: Position [x=-9, y=-17] / Velocity [vx=-15, vy=-16] / null
         * 
         * Since all entities will be created without an acceleration, that component will always be null.
         * 
         * When an entity is deleted though, the "removed" callback might contain the acceleration if the 
         * entity was accelerated before its deletion.
         */
        for (int i = 0; i < 5; i++) {
            var position = new Position(rng.nextInt(-20, 20), rng.nextInt(-20, 20));
            var velocity = new Velocity(rng.nextInt(-20, 20), rng.nextInt(-20, 20));

            System.out.println("- Create entity " + i);
            world.createEntity(position, velocity);
        }

        // Simulated game loop
        var loop = 0;
        while (++loop <= 10) {
            System.out.println();
            System.out.println("-- Loop %d, alive entities: %d".formatted(loop, all.getCount()));

            /*
             * Here we run the "systems" in our game loop.
             * 
             * First we run "maybeRemove" on every entity. This will remove an entity with
             * the chance defined by REMOVE_CHANCE.
             * 
             * After that we run "maybeAccelerate" on every entity that does not have the 
             * Acceleration component yet, adding it with the chance ACCELERATE_CHANCE.
             * 
             * Lastly "maybeSpawn" might spawn a fresh, unaccelerated entity 
             * with the change SPAWN_CHANCE.
             */
            all.process(EntityLifecycleExample::maybeRemove);
            unaccelerated.process(EntityLifecycleExample::maybeAccelerate);
            maybeSpawn();

            /*
             * This time around this call is more interesting. 
             * 
             * As "maybeRemove" might remove an entity and "maybeAccelerate" might change an entity,
             * this call will be responsible for performing these operations and cause most of those
             * inserted and removed logs.
             * 
             * Try commenting out the call. It will still work in this example, but you will note that
             * the number of alive entities will only increase because of "maybeSpawn"
             */
            System.out.println("- Processing world, alive entities: " + all.getCount());
            world.process();

            // Simulate frame
            Thread.sleep(1000 * delta);
        }
    }

    static void maybeRemove(int entityId) {
        if (rng.nextFloat() < REMOVE_CHANCE) {
            System.err.println("Deleting %d".formatted(entityId));

            /*
             * This only marks the entity for removal. 
             * 
             * The actual removal will be performed when world.process() is called.
             */
            world.deleteEntity(entityId);
        }
    }

    static void maybeAccelerate(int entityId) {
        if (rng.nextFloat() < ACCELERATE_CHANCE) {
            System.err.println("Accelerating %d".formatted(entityId));

            /*
             * This adds the component immediately. It will be available with accelerationM.get(entityId) 
             * and through other means, but the entity won't be part of the "accelerated" composition until
             * world.process() is called.
             */
            accelerationM.add(entityId, new Acceleration(10, 0));
        }
    }

    private static void maybeSpawn() {
        if (rng.nextFloat() < SPAWN_CHANCE) {
            var position = new Position(rng.nextInt(-20, 20), rng.nextInt(-20, 20));
            var velocity = new Velocity(rng.nextInt(-20, 20), rng.nextInt(-20, 20));

            /*
             * Only adding components, removing components and deleting entities are delayed.
             * 
             * As such this call will immediately trigger the "inserted" callbacks of interested 
             * compositions. In this case "physics", "unaccelerated" and "all":
             * 
             *      Render: Entity 3 inserted
             *      Unaccelerated: Entity 3 inserted
             *      Physics: Entity 3 inserted: Position [x=-11, y=-13] / Velocity [vx=11, vy=-9] / null#
             *      Spawned 3
             */
            var entityId = world.createEntity(position, velocity);
            System.err.println("Spawned %d".formatted(entityId));
        }
    }

}
