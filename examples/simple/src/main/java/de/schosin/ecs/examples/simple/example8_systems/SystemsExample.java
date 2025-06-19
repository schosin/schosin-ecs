package de.schosin.ecs.examples.simple.example8_systems;

import static de.schosin.ecs.examples.simple.example8_systems.SystemsExample.RNG;

import java.lang.StackWalker.Option;
import java.util.Random;
import java.util.Set;
import java.util.function.IntConsumer;
import java.util.stream.Collectors;

import de.schosin.ecs.api.components.ComponentSet;
import de.schosin.ecs.api.components.ComponentSetConfig;
import de.schosin.ecs.api.components.mappers.ComponentMapper;
import de.schosin.ecs.api.components.mappers.Components;
import de.schosin.ecs.api.components.types.ComponentSetType;
import de.schosin.ecs.api.components.types.ComponentType;
import de.schosin.ecs.api.data.DataProcessor;
import de.schosin.ecs.engine.utils.components.ComponentSetsHelper;
import de.schosin.ecs.examples.simple.components.Acceleration;
import de.schosin.ecs.examples.simple.components.Position;
import de.schosin.ecs.examples.simple.components.Velocity;
import de.schosin.ecs.plugins.composition.Composition;
import de.schosin.ecs.plugins.composition.CompositionData;
import de.schosin.ecs.plugins.composition.CompositionSet;
import de.schosin.ecs.plugins.data.types.DataType2;
import de.schosin.ecs.worlds.DefaultWorld;

/**
 * This example will explain the current stance of this library on the topic of systems.
 * 
 * First things first: 
 * This library does not have an opinion (yet) on how a system should be implemented, 
 * or how "system invocations" must work.
 * 
 * Nonetheless this example will delve into how systems can be built with this library.
 * It does so by building progressively more complex systems and system invocation strategies.
 * 
 * You can look at them in order from top to bottom, or just jump straight to the last one.
 * 
 * Note: 
 * The systems in this example would never be an inner class, and would most likely
 * not be records. This is done for brevity. 
 * In an actual project they'd be regular classes in their own files and within an organized package hierarchy. 
 */
public class SystemsExample {
    static final Random RNG = new Random();

    /**
     * Every example has its own main method, so you can also just run a single one.
     */
    public static void main(String[] args) {
        SimpleSystems.main(args);
        CompositionSystems.main(args);
        SystemHierarchy.main(args);
    }

}

/**
 * This example will quickly recap what has been called "system" in previous examples
 * and then build up to a very simple system invocation strategy based on Runnable.
 * 
 * The goal is to show that implementing systems yourself must not be difficult.
 * 
 * We will work our way up to more and more sophisticated systems. If you want to,
 * you can jump straight to the {@link SystemHierarchy last examples}.
 */
@SuppressWarnings("unused")
class SimpleSystems {

    public static void main(String[] args) {
        System.out.println();
        System.out.println("-- Simple systems");

        var world = DefaultWorld.create();

        /*
         * In previous examples this was often labelled as a "system". 
         * 
         * While this could be true for stateless systems, it will break down very quickly
         * once more complex systems are required.
         * 
         * Additionally when it comes to system invocation, passing around "Helper::physicsSystem" 
         * will simply not work due to different signatures.
         */
        var composition = world.createComposition(Composition.all(Position.class, Velocity.class), Position.class, Velocity.class);
        composition.process(SimpleSystems::physicsSystem); // this won't do anything, we have no entities

        /*
         * What we actually need is a base system that can be invoked by some construct, 
         * which will accept a bunch of these and can then just iterate over them each tick,
         * or pass them to an ExecutorService, or however else it wants to invoke a system.
         * 
         * Let's start simple and just define a system as a Runnable. They are stateless and they
         * don't do a lot yet.
         */
        Runnable[] systems = new Runnable[] {
                () -> System.out.println("System 1"),
                () -> System.out.println("System 2"),
                new System3()
        };

        /*
         * Let's implement a quick and dirty invocation strategy that just iterates over the systems,
         * calling run() on each and then calling world.process() to apply any delayed changes or deletions.
         */
        var invocationStrategy = new RunnableSystemInvocation(world, systems);

        System.out.println("- Frame 1");
        invocationStrategy.process();

        System.out.println("- Frame 2");
        invocationStrategy.process();
    }

    static void physicsSystem(int entityId, Position pos, Velocity velocity) {
        // impl
    }

    static class System3 implements Runnable {
        @Override
        public void run() {
            System.out.println("System 3");
        }
    }

    /**
     * Simple, sequential invocation strategy.
     * 
     * record is used for its brevity.
     */
    record RunnableSystemInvocation(DefaultWorld world, Runnable[] systems) {
        void process() {
            for (var system : systems) {
                system.run();
            }

            world.process();
        }
    }

}

/**
 * In this example we will explore how systems can work with the {@link DefaultWorld},
 * accessing components or creating and using {@link Composition compositions}.
 * 
 * We will still stick to {@link Runnable} as the base type of a system and use the same
 * invocation strategy. We're just interested in making systems more interesting in this example.
 */
@SuppressWarnings("unused")
class CompositionSystems {

    // ignore for now
    static boolean showStackTrace2 = false;
    static boolean showStackTrace3 = false;

    public static void main(String[] args) {
        System.out.println();
        System.out.println("-- Composition systems");

        var world = DefaultWorld.create();

        // Let's create some entities to work with:
        var archetype = world.createArchetype(Position.class, Velocity.class);
        archetype.createBatch(10, (i, factory) -> factory.create(new Position(i, i), new Velocity(i, i)));

        /*
         * Once again we're creating an array of runnables to pass to our invocation strategy.
         */
        Runnable[] systems = new Runnable[] {
                new System1(world),
                new System2(world),
                new System3(world)
        };

        /*
         * Same as in SimpleSystems.
         */
        var invocationStrategy = new RunnableSystemInvocation(world, systems);

        System.out.println("- Frame 1");
        invocationStrategy.process();

        System.out.println("- Frame 2");
        invocationStrategy.process();

        /*
         * One note on the difference of System2 and System3 can be observed
         * when looking at the stacktraces inside their processing methods.
         * 
         * When running this example you can see that System2 (method reference)
         * will include one more stack frame:
         * 
         *      de.schosin.ecs.examples.simple.example8_systems.CompositionSystems$System2$$Lambda/0x0000029146154a00.process(Unknown Source)
         * 
         * This might not be much in terms of performance, but System3 will be easier
         * to debug when stepping into the composition. 
         * As such it might be benefical to implement the processor interface of a composition 
         * when a system only works on a single composition. 
         */
        showStackTrace2 = true;
        showStackTrace3 = true;

        System.out.println("- Frame 3 (Stacktraces)");
        invocationStrategy.process();
    }

    /**
     * Once again, record for brevity. This would most likely be an actual class in a separate file,
     * in a proper package hierarchy.
     */
    record System1(DefaultWorld world) implements Runnable {
        @Override
        public void run() {
            var entityId = RNG.nextInt(1, 20);
            if (world.isActive(entityId)) {
                System.out.println("System1: Entity %d is active".formatted(entityId));
            } else {
                System.out.println("System1: Entity %d is not active".formatted(entityId));
            }
        }
    }

    /**
     * This is a bit more sophisticated. Here we use the world to retrieve a composition that
     * will be used in {@link #run()}.
     * 
     * Using a component set here allows us to the keep it brief and makes for nice, clean code. 
     * 
     * Once again, "composition" would be a final field in a regular class.
     */
    record System2(CompositionSet<CompositionSystemsSet.Processor> composition) implements Runnable {

        public System2(DefaultWorld world) {
            this(world.createComposition(Composition.all(), CompositionSystemsSet.TYPE));
        }

        @Override
        public void run() {
            composition.process(this::process);
        }

        @ComponentSetConfig("CompositionSystemsSet")
        private void process(int entityId, Position pos, Velocity velocity) {
            pos.x += velocity.vx;
            pos.y += velocity.vy;

            if (showStackTrace2) {
                showStackTrace2 = false;

                var stackframes = StackWalker.getInstance(Set.of(Option.RETAIN_CLASS_REFERENCE, Option.SHOW_HIDDEN_FRAMES)).walk(frames -> frames
                        .map(Object::toString)
                        .map(frame -> frame.contains("Lambda") ? " > " + frame : "   " + frame)
                        .collect(Collectors.joining(System.lineSeparator())));

                System.out.println("System2: %s%s".formatted(System.lineSeparator(), stackframes));
            }
        }
    }

    /**
     * Same as {@link System2}, but this time we also implement the generated {@link CompositionSystemsSet.Processor}.
     * 
     * That allows us to pass "this" to {@link ComponentSet#process(DataProcessor)}, removing the need
     * for the JVM to create and invoke a lambda. You can see the difference when running this example in the
     * printed stack traces.
     */
    record System3(CompositionSet<CompositionSystemsSet.Processor> composition) implements Runnable, CompositionSystemsSet.Processor {

        public System3(DefaultWorld world) {
            this(world.createComposition(Composition.all(), CompositionSystemsSet.TYPE));
        }

        @Override
        public void run() {
            composition.process(this);
        }

        @Override
        public void process(int entityId, Position pos, Velocity velocity) {
            // let's undo System2's work
            pos.x -= velocity.vx;
            pos.y -= velocity.vy;

            if (showStackTrace3) {
                showStackTrace3 = false;

                var stackframes = StackWalker.getInstance(Set.of(Option.RETAIN_CLASS_REFERENCE, Option.SHOW_HIDDEN_FRAMES)).walk(frames -> frames
                        .map(frame -> "   " + frame)
                        .collect(Collectors.joining(System.lineSeparator())));

                System.out.println("System3: %s%s".formatted(System.lineSeparator(), stackframes));
            }
        }
    }

    /*
     * Nothing changed here.
     */
    record RunnableSystemInvocation(DefaultWorld world, Runnable[] systems) {
        void process() {
            for (var system : systems) {
                system.run();
            }

            world.process();
        }
    }

}

/**
 * In this example we will introduce a system hierarchy, starting with a simple BaseSystem 
 * and building our way up to a framework specific ComponentSetSystem.
 *  
 * We're not quite at artemis-odb levels of systems, but it'll be start.
 * 
 * Implementing base systems like IntervalSystem will require dealing with a
 * delta time. The invocation strategy could just accept it as a parameter and pass
 * it along. But that is out of scope for this example.
 */
class SystemHierarchy {

    public static void main(String[] args) {
        System.out.println();
        System.out.println("-- Composition systems");

        var world = DefaultWorld.create();

        // Let's create some entities to work with:
        var archetype = world.createArchetype(Position.class, Velocity.class);
        archetype.createBatch(10, (i, factory) -> factory.create(new Position(i, i), new Velocity(i, i)));

        /*
         * This time our base type is BaseSystem.
         */
        BaseSystem[] systems = new BaseSystem[] {
                new System1(world),
                new System2(world),
                new System3(world),
                new System4(world)
        };

        /*
         * Same as in SimpleSystems.
         */
        var invocationStrategy = new BaseSystemInvocation(world, systems);

        System.out.println("- Frame 1");
        invocationStrategy.process();

        System.out.println("- Frame 2");
        invocationStrategy.process();
    }

    /**
     * The first system will extends {@link BaseSystem} directly. It has to pass a world to it and can access it by its field.
     * 
     * It has to implement the abstract method {@link #process()}, which will be called every "frame" by {@link BaseSystemInvocation}.
     */
    static class System1 extends BaseSystem {

        System1(DefaultWorld world) {
            super(world);
        }

        @Override
        void process() {
            var entityId = RNG.nextInt(1, 20);
            if (world.isActive(entityId)) {
                System.out.println("System1: Entity %d is active".formatted(entityId));
            } else {
                System.out.println("System1: Entity %d is not active".formatted(entityId));
            }
        }

    }

    /**
     * This is our base system. Users of artemis-odb might recognize it already.
     * 
     * In addition to {@link #process()} it also has the methods {@link #begin()}
     * and {@link #end()}. These will be called around process and will be relevant
     * for {@link System2}.
     * 
     * Note:
     * You could also use a generic <T extends World> to work with arbitrary worlds.
     */
    protected abstract static class BaseSystem {

        final DefaultWorld world;

        BaseSystem(DefaultWorld world) {
            this.world = world;
        }

        void begin() {
        }

        abstract void process();

        void end() {
        }

    }

    /**
     * This is probably the closest to how a system can be implemented in artemis-odb.
     * 
     * There is no injection (1) of component mappers or the world, everything works by constructor,
     * but the way a single entity is processed is very similar to how I used artemis-odb:
     * 
     *      - Get the components of the entity using the component mappers
     *      - Perform the system logic on the components
     * 
     * In addition to processing entities in {@link #accept(int)}, we also override {@link #begin()}
     * and {@link #end()} just to print some stuff. These will be around the actual entity processing.
     * Try adding a sysout to accept.
     * 
     * (1) On the topic of injection:
     * if someone is actually interested in the injection part, that would be just an implementation
     * detail of the system invocation strategy. It's a lot more involved, but in the end its mostly
     * just reflection.
     */
    static class System2 extends IteratingSystem {

        private final ComponentMapper<Position> posM;
        private final ComponentMapper<Velocity> velocityM;

        System2(DefaultWorld world) {
            super(world, Composition.all(Position.class, Velocity.class));

            this.posM = world.getComponents(Position.class);
            this.velocityM = world.getComponents(Velocity.class);
        }

        @Override
        void begin() {
            System.out.println("System2 begin: Processing %d entities".formatted(composition.getCount()));
        }

        @Override
        public void accept(int entityId) {
            var pos = posM.get(entityId);
            var velocity = velocityM.get(entityId);

            pos.x += velocity.vx;
            pos.y += velocity.vy;
        }

        @Override
        void end() {
            System.out.println("System2 end");
        }

    }

    /**
     * A simple iterating system similar to the system by the same name of artemis-odb.
     * 
     * It's implementation is based on a {@link Composition}. The implementing system
     * has to pass in a {@link Composition.Builder} that describes the entities its 
     * interested in. Think artemis-odb Aspect (@All, @One, @None).
     * 
     * Since {@link Composition#process(IntConsumer)} wants an {@link IntConsumer},
     * the process method will be called "accept" instead of "process". 
     * 
     * If that bothers anyone, a method reference to an abstract method could be used, 
     * or just implement "accept" in here and call an abstract method "process(int)".
     */
    abstract static class IteratingSystem extends BaseSystem implements IntConsumer {

        protected final Composition composition;

        IteratingSystem(DefaultWorld world, Composition.Builder builder) {
            super(world);

            this.composition = world.createComposition(builder);
        }

        @Override
        final void process() {
            composition.process(this);
        }

    }

    /**
     * This does the same work as {@link System2} (in reverse), but implemented based on
     * {@link CompositionSystem}. There is no equivalent in artemis-odb, so let's start at the top.
     * 
     * The first thing is the type parameter of the super class. It needs to be a "DataProcessor<?>".
     * Here we are using a generated {@link ComponentSet} that is defined by {@link #process()}. 
     * It comes with a nested type "Processor" that extends DataProcessor.
     * 
     * In addition to passing the generated {@link SystemHierarchySet.Processor} as a type parameter, 
     * we also implement the interface. This just happens to be exactly the method used to define
     * the component set.
     * 
     * {@link CompositionSystemNotes At the end} there are some notes on how to implement this system
     * using a component set. It's just a tiny bit more involved as the component set won't exist before process is defined.
     * 
     * But it's just an order one has to get used to. A small price to pay for the convenience of
     * just adding "{@link Acceleration} acceleration" as a parameter and it just works.
     */
    static class System3 extends CompositionSystem<SystemHierarchySet.Processor> implements SystemHierarchySet.Processor {

        System3(DefaultWorld world) {
            super(world, world.createComposition(Composition.all(Position.class, Velocity.class), SystemHierarchySet.TYPE));
        }

        @Override
        @ComponentSetConfig("SystemHierarchySet")
        public void process(int entityId, Position pos, Velocity velocity) {
            pos.x -= velocity.vx;
            pos.y -= velocity.vy;
        }

    }

    /**
     * A little bit more involved than {@link IteratingSystem}, but the approach is the same.
     * 
     * The type parameter P will replace the {@link IntConsumer} in {@link IteratingSystem}.
     * 
     * The implementing system will then have to pass a {@link CompositionData CompositionData&lt;P&gt}.
     * This is a composition that accepts a P in its {@link CompositionData#process(DataProcessor)}.
     * 
     * By casting "this" to P and assigning it to the field "processor", we force the implementing
     * system to not just pass a valid P, but also implement it. An alternative would be to require
     * an additional parameter "P processor".
     * 
     * This little detail makes implementing systems that work on a single composition quite a lot
     * nicer than using {@link IteratingSystem} and {@link Components component mappers}. 
     * 
     * It's also a lot more performant since {@link DataProcessor} is a lot more optimized than
     * accessing components by hand with {@link Components#get(int)}.
     */
    abstract static class CompositionSystem<P extends DataProcessor<?>> extends BaseSystem {

        protected final CompositionData<P> composition;
        private final P processor;

        /**
         * Note that {@link #composition} does not require a component set. 
         * The implementing system could also just pass a 
         * {@link DefaultWorld#createComposition(Composition.Builder, Class, Class) world.createComposition(Position.class, Velocity.class)}
         * and the signature of the process method in {@link System3} would stay the same:
         * 
         * <p>
         *      class System3 extends CompositionSystem<DataType2.Processor2<Position, Velocity>>
         *      super(world, world.createComposition(Composition.all(), Position.class, Velocity.class));
         * 
         * I'll also reference {@link DataType2} so the import is already present.
         */
        @SuppressWarnings("unchecked")
        CompositionSystem(DefaultWorld world, CompositionData<P> composition) {
            super(world);

            this.composition = composition;
            this.processor = (P) this;
        }

        @Override
        final void process() {
            composition.process(processor);
        }

    }

    /**
     * This one goes one step further than {@link System3} and {@link CompositionSystem} and uses a {@link ComponentSetSystem}.
     * 
     * The type signature has to be longer to satisfy the type system, but the super call is just passing world and SystemHierarchySet.TYPE.
     * Other than that everything else stays the same.
     */
    static class System4 extends ComponentSetSystem<SystemHierarchySet, SystemHierarchySet.Processor> implements SystemHierarchySet.Processor {

        System4(DefaultWorld world) {
            super(world, SystemHierarchySet.TYPE);
        }

        @Override
        public void process(int entityId, Position pos, Velocity velocity) {
            pos.x -= velocity.vx;
            pos.y -= velocity.vy;
        }

    }

    /**
     * The implementation is basically the same as {@link CompositionSystem}. 
     * 
     * The only difference is that it uses the same methods as the core and plugin modules 
     * to access the component types for a component set and passes it to 
     * {@link Composition.Builder#all(ComponentType...)}.
     * 
     * As such systems extending this class will iterate entities that have all the
     * components present in the component set.
     */
    abstract static class ComponentSetSystem<T extends ComponentSet<P>, P extends DataProcessor<T>> extends BaseSystem {

        protected final CompositionSet<P> composition;
        private final P processor;

        @SuppressWarnings("unchecked")
        ComponentSetSystem(DefaultWorld world, ComponentSetType<T, P> componentType) {
            super(world);

            this.composition = buildComposition(world, componentType);
            this.processor = (P) this;
        }

        private CompositionSet<P> buildComposition(DefaultWorld world, ComponentSetType<T, P> componentType) {
            /*
             * ComponentSetsHelper is actually a type in core module and not intended to be used in user code.
             * 
             * These base classes would optimally be provided by a Plugin along with a method like 
             * "MySystemPlugin#addSystem" and "MySystemPlugin#processSystems()" or "MySystemPlugin#processSystems(float delta)".
             * 
             * The experimental plugin "SystemsPlugin" is basically that, just not yet with any special base types.
             */
            var componentTypes = ComponentSetsHelper.getData(componentType.componentSet()).components().stream()
                    .<ComponentType<?, ?>>map(ComponentSet.ComponentData::type)
                    .toArray(ComponentType<?, ?>[]::new);

            return world.createComposition(Composition.all(componentTypes), componentType);
        }

        @Override
        final void process() {
            composition.process(processor);
        }

    }

    /**
     * This time we don't have an array of {@link Runnable}, but of {@link BaseSystem}.
     * 
     * Iteration stays the same, this time we just call begin, process and end on every system.
     */
    record BaseSystemInvocation(DefaultWorld world, BaseSystem[] systems) {
        void process() {
            for (var system : systems) {
                system.begin();
                system.process();
                system.end();
            }

            world.process();
        }
    }

    /**
     * When writing a new system extending {@link CompositionSystem} with a {@link ComponentSet}, the order is important.
     * 
     * Using {@link ComponentSetSystem} instead simplifies step 7 and 8.
     */
    static class CompositionSystemNotes {

        // Step 1: 
        // public void process(int entityId) {}

        // Step 2: Add the components you want
        // public void process(int entityId, Position pos, Velocity velocity) {}

        // Step 3: Annotate
        // @ComponentSetConfig("MyComponentSet")
        // public void process(int entityId, Position pos, Velocity velocity) {}

        // Step 4: Implement interface, optionally add @Override to process 
        // CompositionSystemNotes implements MyComponentSet.Processor

        // Step 5: Empty constructor accepting World, DefaultWord or CustomWorld
        // public CompositionSystemNotes(DefaultWorld) {}

        // Step 6: extend CompositionSystem<MyComponentSet.Processor>

        // Step 7: super(world, world.createComposition(Composition.all(), MyComponentSet.TYPE);

        // Step 8: Adjust Composition.all() or maybe move it to a constant so the super call isn't too long.

    }

}
