package de.schosin.ecs.examples.simple.example5_worlds;

import java.util.function.IntConsumer;

import de.schosin.ecs.api.Plugin;
import de.schosin.ecs.api.World;
import de.schosin.ecs.examples.simple.AbstractExample;
import de.schosin.ecs.examples.simple.components.Acceleration;
import de.schosin.ecs.examples.simple.components.Position;
import de.schosin.ecs.examples.simple.components.Velocity;
import de.schosin.ecs.plugins.composition.Composition;
import de.schosin.ecs.plugins.composition.CompositionPlugin;
import de.schosin.ecs.worlds.DefaultWorld;

/**
 * This example with delve into details on creating, configuring,
 * and extending the {@link World} instance, the main entry point
 * into the library.
 * 
 * You will learn how to create a simple world and how the plugin system
 * works.
 */
@SuppressWarnings("unused")
public class WorldsExample {

    public static void main(String[] args) throws InterruptedException {
        world();
        defaultWorld();
        customWorld();
        processLoops();
    }

    /**
     * Unlike the previous examples, this example does not extends {@link AbstractExample},
     * which already provides an instatiated world. This time we will do it by hand.
     * 
     * We will start simple and just work with the standard {@link World}.
     * @throws InterruptedException 
     */
    private static void world() {
        System.out.println();
        System.out.println("-- World");

        /*
         * To instantiate a world, World provides a static builder method. Calling build()
         * on that builer creates a world ready for use.
         */
        var world1 = World.builder().build();

        var entityId = world1.createEntity(new Position(42, 9001));
        var mapper = world1.getComponents(Position.class);
        System.out.println("World 1 - Entity %d: %s".formatted(entityId, mapper.get(entityId)));

        /*
         * The builder provides a couple of configuration options. An important one for performance
         * is expectedEntities. 
         * 
         * It accepts an int and should be configured to the maximum expected number 
         * of alive entities at the peak. This allows some internal data structures to be
         * laid out more efficiently in memory and avoids copying of data when the number
         * of entities increases.
         * 
         * To achieve that the framework will require more memory at startup, but avoids some
         * garbage collection and stutters from growing internal data structures.
         */
        var world2 = World.builder()
                .expectedEntities(1_000_000)
                .build();

        /*
         * Unless there is an off-by-one error somewhere, this will avoid most of the
         * allocations of Object[] and int[] that would happen if this was done on world1.
         * 
         * <p>Note:
         * Don't create large numbers of entities using "world.createEntity(...)"!
         * This method is quite slow for larger component counts as it has to do a lot of work 
         * to store these components in memory.
         * 
         * Example 6 will go into a more efficient way to create predefined
         * entities, including batching.
         */
        for (int i = 0; i < 1_000_000; i++) {
            world2.createEntity(new Position(i, i));
        }

        System.out.println("World 2 - Created 1000000 entities. Entity 1 (alive %b), Entity 1000000 (alive %b), Entity 1000001 (alive %b)".formatted(
                world2.isActive(1),
                world2.isActive(1_000_000),
                world2.isActive(1_000_001)));
    }

    private static void defaultWorld() {
        System.out.println();
        System.out.println("-- DefaultWorld");

        /*
         * Let's create a World and a composition. 
         * 
         * Oops, I've left the composition commented. Please uncomment the line.
         */
        World world1 = World.builder().build();

        // var composition1 = world1.createComposition(Composition.all(Position.class), Position.class);

        /*
         * A compilation error you say? Well, leave it commented then. We didn't need it anyway.
         * 
         * We have been creating compositions with that method several times now, and there is a 
         * good reason why it doesn't work this time.
         * 
         * When you look at the type of "world1", you will see that it is World.
         * In all the previous examples we've not been using World, but DefaultWorld.
         * 
         * So let's create a DefaultWorld and try again. 
         */
        DefaultWorld world2 = DefaultWorld.create();

        var composition2 = world2.createComposition(Composition.all(Position.class), Position.class);

        /*
         * If you take a look at what DefaultWorld exactly is, you will find that it is an interface
         * that extends World, along with a number plugins. One among them is CompositionPlugin.
         * 
         * You can take a closer look at CompositionPlugin if you want, but be warnded that you will
         * come across ugly, generated code. But you will find all the "createComposition" methods 
         * and overloads we've been using so far, and more.
         * 
         * If you want you can also explore some of the other plugins, which have not been introduced
         * yet. ArchetypePlugin for example with be explained in example 7 and provides a fast way
         * to create predefined entities, both single entities or whole batches at once.
         */
    }

    /**
     * For the next example we will need a new type. If you've taken a look at {@link DefaultWorld},
     * you will note that this is just a simplified version, reduced to only {@link World} and {@link CompositionPlugin}.
     */
    public interface CustomWorld extends World, CompositionPlugin {
        default int getAnswer() {
            return 42;
        }
    }

    /**
     * This is an implementation of out {@link CustomWorld}.
     * 
     * It is your task to remove the abstract keyword and implement it :-P
     * 
     * Or better yet, just jump ahead to {@link WorldsExample#customWorld()}.
     */
    public abstract class CustomWorldImpl implements CustomWorld {
    }

    private static void customWorld() {
        System.out.println();
        System.out.println("-- CustomWorld");

        /*
         * So we've declared a new CustomWorld. You might even have tried implementing CustomWorldImpl,
         * but I'm sure it wasn't fun. I hope you didn't actually do it.
         * 
         * So let's just create an instance and use it.
         * Maybe create a composition, add a couple entities with positions, remove it from one, delete the other, process the world.
         */
        CustomWorld customWorld = World.builder(CustomWorld.class).build();

        var composition = customWorld.createComposition(Composition.all(Position.class), Position.class);
        composition.inserted((entityId, pos) -> System.out.println("Entity %d inserted: %s".formatted(entityId, pos)));
        composition.removed((entityId, pos) -> System.out.println("Entity %d removed: %s".formatted(entityId, pos)));

        var entity1 = customWorld.createEntity(new Position(42, 42));
        System.out.println("Created entity %d with position".formatted(entity1));

        var entity2 = customWorld.createEntity(new Position(0, 0));
        System.out.println("Created entity %d with position".formatted(entity2));

        var posM = customWorld.getComponents(Position.class);
        posM.remove(entity1);

        customWorld.deleteEntity(entity2);
        customWorld.process();

        /*
         * You will see that our CustomWorld works just like our DefaultWorld.
         * 
         * Heres a defaultWorld. If you look at the methods both provide, you will
         * see that DefaultWorld has a lot more to offer. But maybe you are not interested
         * in all those archetype and transmuter methods and you want to get rid of them.
         * 
         * If so, just craft your own world and only take what you want!
         */
        DefaultWorld defaultWorld = DefaultWorld.create();

        var defaultArchetype = defaultWorld.createArchetype(Position.class, Velocity.class);
        // var customArchetype = customWorld.createArchetype(Position.class, Velocity.class); // won't compile, does not extend ArchetypePlugin

        /*
         * Remember the default method on CustomWorld? We can even invoke that and get an answer!
         * 
         * But don't get too excited, there aren't a lot of uses for that. Try making the method abstract
         * or add an abstract method like "int getPowerLevel();" and you will see the limitations of this appraoch.
         * 
         * If you want a power level from your custom world, you'd have to write your own plugin.
         * (It's below this method, try adding it to CustomWorld and see if it works!)
         */
        System.out.println("Answer: " + customWorld.getAnswer());

        /*
         * If you want a power level from your custom world, you'd have to write your own plugin.
         * (It's below this method, try adding it to CustomWorld and see if it works!)
         */
        // System.out.println("Power level: " + customWorld.getPowerLevel());
    }

    /**
     * Creating a plugin is out of scope, but the basic rundown is this:
     * 
     *      A plugin interface points to an implementation with the {@link Plugin} annotation.
     *      
     * That's basically it. There are a lot more details, for example you can see that the constructor
     * takes a world as well as a {@link CompositionPlugin} as an argument. This is somewhat similar to
     * dependency injection. There's also a way to configure plugins with {@link World.Builder#configure(Plugin.PluginConfig...)}.
     */
    @Plugin(PowerLevelPluginImpl.class)
    public interface PowerLevelPlugin {
        int getPowerLevel();
    }

    public static class PowerLevelPluginImpl implements PowerLevelPlugin {
        public PowerLevelPluginImpl(World world, CompositionPlugin compositionPlugin) {
            System.out.println("Initializing PowerLevelPluginImpl: " + world);
            System.out.println("Initializing PowerLevelPluginImpl: " + compositionPlugin);
        }

        @Override
        public int getPowerLevel() {
            return 9001;
        }
    }

    /**
     * This is just a note for those interested in how {@link WorldsExample#customWorld()}
     * actually works.
     * 
     * When we create an instance, we pass the interface to {@link World#builder(Class)} and then 
     * we build a world. What is happening in {@link World.Builder#build()} is roughly the following:
     * 
     *      - The implementation WorldBuilder in the core module will analyze the interface
     *      - It will detect interfaces that are annotated with {@link Plugin @Plugin} and retrieves their implementation classes
     *      - It will construct a regular {@link World}
     *      - For every plugin, it will instantiate its implementation, passing in dependencies declared as parameters to its sole public constructor
     *      - It uses ByteBuddy to create a proxy class that implements CustomWorld and uses method delegation to dispatch method calls to the regular world or plugin implementations
     *      - It creates an instance of that proxy class and returns it
     */
    private static void customWorldImplementation() {
    }

    /**
     * The configuration "processLoops" is a very technical one and is only relevant when certain patterns
     * are used. This will explain how world.process() works, and go into more detail on the entity lifecycle,
     * in particular the inserted callbacks. 
     * 
     * If you plan to use {@link Composition#inserted(IntConsumer)} to initialize entities,
     * this config might become relevant. If not, you can skip this. 
     * 
     * If you decide to read this, please:
     *      
     *      This is just an example to showcase it. No sane person should write such code.
     *      Read the last part about the take-away.
     */
    private static void processLoops() {
        // we'll split this in multiple parts as it is a bit more involved. The first one acts as a small recap.
        processLoopsPart1();
        processLoopsPart2();
        processLoopsPart3();
    }

    private static void processLoopsPart1() {
        System.out.println();
        System.out.println("-- Process loops #1");

        // Let's create a world and get some component mappers to use later.
        DefaultWorld world1 = DefaultWorld.create();

        var posMapper = world1.getComponents(Position.class);
        var velocityMapper = world1.getComponents(Velocity.class);
        var accelerationMapper = world1.getComponents(Acceleration.class);

        /*
         * Remember the delayed changes to components of entities from example 2?
         * 
         * When adding a component to a entity, the component will be added immediately, but any compositions
         * will only be notified (inserted, removed) on the next world.process() call.
         * 
         * When running this example, you will see the output of composition1.inserted in between 
         * "Before process" and "After process" for that very reason, even though the actual "add" happened before.
         */
        var composition1 = world1.createComposition(Composition.all(Position.class), Position.class);
        composition1.inserted((entityId, pos) -> System.out.println("World 1 - Entity %d inserted: %s".formatted(entityId, pos)));

        var entity1 = world1.createEntity();

        posMapper.add(entity1, new Position(42, 9001));
        System.out.println("Added Position to entity %d".formatted(entity1));

        System.out.println();
        System.out.println("Before process");
        world1.process();
        System.out.println("After process");

        /*
         * Let's see what happens when we modify an entity within an inserted callback.
         * For that we create two new composition that match entities that do not have a component.
         * 
         * "addVelocity" will match any entity without a velocity and just adds one.
         * "addAcceleration" will match any entity with a velocity, but no acceleration, and just adds the acceleration.
         * 
         * Additionally we add an additional inserted callback before these operations that logs whenever an entity
         * is inserted for that composition. These will be run in the same sequence as they were added.
         * 
         * One thing to note: 
         * When another system calls "createComposition" with an equal builder, they will
         * work on the same instance. Even when they use different components afterwards, the base composition
         * will be the same and any inserted or removed callbacks registered will be in the registration order,
         * event across systems!
         */
        var addVelocity = world1.createComposition(Composition.none(Velocity.class));
        addVelocity.inserted(entityId -> System.out.println("addVelocity: %d inserted".formatted(entityId)));
        addVelocity.inserted(entityId -> velocityMapper.add(entityId, new Velocity(1, -1)));

        var addAcceleration = world1.createComposition(Composition.all(Velocity.class).none(Acceleration.class));
        addAcceleration.inserted(entityId -> System.out.println("addAcceleration: %d inserted".formatted(entityId)));
        addAcceleration.inserted(entityId -> accelerationMapper.add(entityId, new Acceleration(-1, 1)));

        /*
         * When we now create an empty entity, we will see the output from "composition1" 
         * and the two logs from "addVelocity" and "addAcceleration".
         * 
         * This is at odds with the statement that composition changes are delayed. The reason for that is
         * that this statement only applies to changes to an existing entity, not when an entity is being created.
         * 
         * When an entity is created, updates triggered in inserted callbacks will be applied "recursively". 
         * Well, up to a certain point ...
         */
        var emptyEntity = world1.createEntity();

        System.out.println("Empty entity %d, velocity: %s".formatted(emptyEntity, velocityMapper.get(emptyEntity)));
        System.out.println("Empty entity %d, acceleration: %s".formatted(emptyEntity, accelerationMapper.get(emptyEntity)));
    }

    private static void processLoopsPart2() {
        System.out.println();
        System.out.println("-- Process loops #2");

        /*
         * Let's create a fresh world. We'll also reuse "addVelocity" from the previous part.
         * 
         * We just also add a "removed" callback to log when that gets invoked.
         */
        var world = DefaultWorld.create();
        var velocityMapper = world.getComponents(Velocity.class);

        var addVelocity = world.createComposition(Composition.none(Velocity.class));
        addVelocity.inserted(entityId -> System.out.println("addVelocity: %d inserted".formatted(entityId)));
        addVelocity.removed(entityId -> System.out.println("addVelocity: %d removed".formatted(entityId)));
        addVelocity.inserted(entityId -> velocityMapper.add(entityId, new Velocity(1, -1)));

        /*
         * Now let's add a "removeVelocity". It is the same as "addVelocity", but it removes the component instead.
         * 
         * We learned that when an entity is created, "inserted" callbacks will be applied "recursively". 
         * What this setup means then is the following:
         * 
         *      1. We create an empty entity
         *      2. addVelocity.inserted prints to sysout and adds a velocity component
         *      3. addVelocity.removed prints to sysout
         *      4. removeVelocity.inserted prints to sysout and removes the velocity component
         *      5. removeVelocity.removed prints to sysout
         *      6. go back to step 2
         *      
         * This would continue endlessly. And since this is not even a true recursion, it would not even cause
         * a stack overflow, but hang endlessly.
         * 
         * One thing to note:
         * This hasn't been talked about before, but when the entity composition changes, the world will
         * invoke all removed callbacks of previously interested compositions, and then invoke the inserted
         * callbacks of all newly interested compositions.
         * 
         * Try adding a composition with Composition.all() and log its inserted and removed callbacks.
         * You will observe that only the inserted callback will be invoked once. That is because it is interested
         * in all entities and adding or removing components does not alter its "interest" in the entity.
         */
        var removeVelocity = world.createComposition(Composition.all(Velocity.class));
        removeVelocity.inserted(entityId -> System.out.println("removeVelocity: %d inserted".formatted(entityId)));
        removeVelocity.removed(entityId -> System.out.println("removeVelocity: %d removed".formatted(entityId)));
        removeVelocity.inserted(entityId -> velocityMapper.remove(entityId));

        /*
         * When we actually add an entity now, we can observe the back and forth between both compositions,
         * but it will eventually stop with an error log.
         * 
         * Unless the configuration has changes, it should leave the entity in a state where it has the velocity,
         * but leave the entity in the "delayed change" state.
         */
        var emptyEntity = world.createEntity();

        System.out.println("Empty entity %d, velocity: %s".formatted(emptyEntity, velocityMapper.get(emptyEntity)));

        /*
         * Since the entity is in that state, world.process() should resolve that, right?
         * Well, it continues that atleast.
         * 
         * This time we will see less back and forth. This time we will see it match the "processLoops" 
         * configuration ...
         */
        System.out.println();
        System.out.println("- Before process");
        world.process();
        System.out.println("- After process");
    }

    private static void processLoopsPart3() {
        System.out.println();
        System.out.println("-- Process loops #3");

        /*
         * The previous part showed a setup where two compositions went back and forth
         * and added and removed a component from an entity "endlessly". 
         * 
         * In that part, the last world.process() should produce 3 "inserted" events,
         * because the default value for this "processLoops" is 3 if not otherwise specified.
         * 
         * This time we configure it explicitly. For that we use the builder so we can set 
         * the value.
         */
        DefaultWorld world3 = DefaultWorld.builder()
                .processLoops(5)
                .build();

        /*
         * Let's recreate the same setup as in part two and create the entity again.
         * We'll just leave out the removed callbacks to reduce the noise.
         */
        var world = DefaultWorld.create();
        var velocityMapper = world.getComponents(Velocity.class);

        var addVelocity = world.createComposition(Composition.none(Velocity.class));
        addVelocity.inserted(entityId -> System.out.println("addVelocity: %d inserted".formatted(entityId)));
        addVelocity.inserted(entityId -> velocityMapper.add(entityId, new Velocity(1, -1)));

        var removeVelocity = world.createComposition(Composition.all(Velocity.class));
        removeVelocity.inserted(entityId -> System.out.println("removeVelocity: %d inserted".formatted(entityId)));
        removeVelocity.inserted(entityId -> velocityMapper.remove(entityId));

        var emptyEntity = world.createEntity();

        System.out.println("Empty entity %d, velocity: %s".formatted(emptyEntity, velocityMapper.get(emptyEntity)));

        /*
         * When we run "world.process()" this time around, we will not see 3 "inserted" calls, but the configured 5 times.
         * Try changing the number to see for yourself.
         */
        System.out.println();
        System.out.println("- Before process #1");
        world.process();
        System.out.println("- After process #1");

        /*
         * An important take-away from this whole excersive:
         * 
         * This example was just to showcase the configuration parameter. Nobody should ever write these kinds of
         * recursion. In fact, it might even be better if the library threw an error if "processLoops" is reached, 
         * as this example should be considered a "user error".
         * 
         * But this whole example still has a very important point:
         * 
         * !!! When writing systems and managers that dynamically add or remove components in inserted callbacks,
         * !!! take good care that you do not end up with these endless callback loops.
         * 
         * If your setup is particuarly complex and creating an entity requires multiple such inserted callbacks in succession,
         * you have the option to increase that parameter so an entity is not left in a half-baked state.
         * 
         * Maybe you create an entity that has a position and sprite. 
         * A system reacts to such entities and calculates a bounding box component.
         * When an entity with a bounding box is inserted, a collision system might want to add it to a quadtree.
         * To track some state for that entity, it adds some collisions component.
         * Maybe you had some issues with collisions and created a debug system that wants to track entities in a field that have
         * a collisions component and does some debugging work.
         * ...
         * 
         * As you can see, there might be use-cases for these deeper inserted chains that start simply 
         * with creating an entity with two components.
         */
        var importantTakeaway = "Please read this.";
    }

}
