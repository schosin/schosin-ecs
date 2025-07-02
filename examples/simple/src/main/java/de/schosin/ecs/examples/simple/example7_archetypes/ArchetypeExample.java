package de.schosin.ecs.examples.simple.example7_archetypes;

import static de.schosin.ecs.plugins.wildcards.types.WildcardType.WILDCARD;
import static de.schosin.ecs.plugins.wildcards.types.WildcardType.wildcard;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import de.schosin.ecs.api.World;
import de.schosin.ecs.examples.simple.components.Position;
import de.schosin.ecs.examples.simple.components.Velocity;
import de.schosin.ecs.plugins.archetype.Archetype2;
import de.schosin.ecs.plugins.archetype.ArchetypePlugin;
import de.schosin.ecs.plugins.composition.Composition;
import de.schosin.ecs.plugins.wildcards.result.WildcardResult;
import de.schosin.ecs.worlds.DefaultWorld;

/**
 * Archetypes are a fast way to create predefined entities in a type-safe manner.
 * 
 * When compared to world.createEntity(...), even using an archetype for creating a single
 * entity now and then will provide performance benefits. 
 * 
 * On top of that archetype also supports batch creation of entities, which provide another 
 * performance boost compared to creating entities in a loop.
 * 
 * Archetypes are implemented by {@link ArchetypePlugin} and are part of {@link DefaultWorld}.
 * The concept of plugins has been introduced in example 5.
 */
@SuppressWarnings("unused")
public class ArchetypeExample {

    protected static final ArchetypeExampleWorld world = createWorld();

    public static void main(String[] args) {
        archetype();
        removeEntities();

        markerComponents();
        removeEntities();

        benchmark();
        removeEntities();
    }

    private static void archetype() {
        System.out.println();
        System.out.println("-- Archetype");

        /*
         * Let's create a composition so we can track whenever an entity is inserted.
         * 
         * Ignore WILDCARD for now. Just know that it provides us with a way to iterate all
         * non-relation components that are assigned to an entity.
         */
        var composition = world.createComposition(Composition.all(), WILDCARD);
        composition.inserted((entityId, components) -> System.out.println("Created entity %d: %s%s".formatted(entityId, System.lineSeparator(), formatComponents(components))));

        /*
         * We can create an archetype with world.createArchetype(...), adding in one or more components.
         * 
         * The actual number of components predefined at build time of this library and is currently configured as 8.
         * That means we can create up to Archetype8.
         * 
         * Note:
         * There are plans to utilize component sets (example 4) for this as well, which would allow to create
         * archetypes for arbitrary sizes.
         */
        Archetype2<Position, Velocity> archetype = world.createArchetype(Position.class, Velocity.class);

        /*
         * Creating a single entity can be done by just calling create and passing in the components.
         */
        var entity1 = archetype.create(new Position(1, 1), new Velocity(1, 1));

        /*
         * There are also other variants that accept a "factory", which is actually just a custom consumer.
         * 
         * These variants are not that useful when creating single entities, but in combination with a method references
         * it might come in handy at some point. The second one in particular is quite useless as i will always be zero.
         */
        var entity2 = archetype.create(factory -> factory.create(new Position(2, 2), new Velocity(2, 2)));
        var entity3 = archetype.create((i, factory) -> factory.create(new Position(3, 3), new Velocity(3, 3)));

        /*
         * Where it gets interesting when we start using createBatch. This method allows us to define the amount
         * of entities we want to create and the callback will be invoked that many times. 
         * 
         * This is also where that "i" comes into play. It indexes the entities from 0 to count-1. Useful if you
         * want some variant, like every other entity will have double the velocity!
         * 
         * You can also just use the method that only uses "factory".
         */
        System.out.println();

        System.out.println("- Before createBatch");
        var entities1 = archetype.createBatch(5, (i, factory) -> factory.create(new Position(4 + i, 4 + i), new Velocity(4 + i, 4 + i)));
        System.out.println("Created %d entities: %s".formatted(entities1.getSize(), entities1));
        System.out.println("- After createBatch");
    }

    private static void markerComponents() {
        System.out.println();
        System.out.println("-- Marker components");

        /*
         * We'll use a clean world so no compositions of the previous method will do anything.
         */
        var world = createWorld();

        /*
         * Once again we'll use WILDCARD to iterate all components of created entities. This time just the explicit form.
         * 
         * Hint: It also supports interfaces or abstract types. Create an interface "DebugComponent" and implement a debug system
         * that just works on all components implementing that interface.
         */
        var composition = world.createComposition(Composition.all(), wildcard(Object.class));
        composition.inserted((entityId, components) -> System.out.println("Created entity %d: %s%s".formatted(entityId, System.lineSeparator(), formatComponents(components))));

        /*        
         * One limitation of archetypes is the limited number of components it can manage.
         * 
         * Eight components can be gone quite quickly. As mentioned before, a way to have arbitrarily sized
         * archetypes based on component sets is planned.
         * 
         * One possible type of component that can be used are "marker components", 
         * which is just a fancy name for enums. Let's create an archetype with a couple of those.
         */
        var units = world.createArchetype(Position.class, Velocity.class, Enemy.class, UnitType.class);

        System.out.println("- Before units");
        units.createBatch(3, (i, factory) -> factory.create(new Position(i, i), new Velocity(i, i), Enemy.ENEMY, UnitType.values()[i % UnitType.count]));
        System.out.println("- After units");

        /*
         * Now we're up to Archetype4. I've gone ahead and hid the signature with var.
         * And the worst part is that Enemy isn't even that interesting to begin with, it'll always be the same value!
         * 
         * But we can do better and eliminate those enum components. We'll start with the same base archetype as before.
         * This time I won't hide the signature.
         */
        Archetype2<Position, Velocity> baseArchetype = world.createArchetype(Position.class, Velocity.class);

        /*
         * Let's start with Enemy, since it only has a single value.
         * 
         * When the composition above logs the components for these entities you will see that 
         * they indeed have the Enemy component, even though we did not have to include it!
         */
        Archetype2<Position, Velocity> enemies = baseArchetype.with(Enemy.ENEMY);

        System.out.println("- Before enemies");
        enemies.createBatch(3, (i, factory) -> factory.create(new Position(i, i), new Velocity(i, i)));
        System.out.println("- After enemies");

        /*
         * UnitType is a bit more annyoing because we want to be able to use all values.
         * One way to achieve that is to just use an archetype for every variant.
         * 
         * Note that we use "enemies" as the base this time. When the entities are logged,
         * they will have both ENEMY and WARRIOR on top of Position and Velocity.
         */
        Archetype2<Position, Velocity> enemyWarrios = enemies.with(UnitType.WARRIOR);

        System.out.println("- Before enemyWarrios");
        enemyWarrios.createBatch(3, (i, factory) -> factory.create(new Position(i, i), new Velocity(i, i)));
        System.out.println("- After enemyWarrios");

        /*
         * We could just implement the other variants by hand, but that'd be boring.
         * Let's use a stream and just create a map from UnitType to its archetype.
         */
        Map<UnitType, Archetype2<Position, Velocity>> enemyUnits = Arrays.stream(UnitType.values())
                .collect(Collectors.toMap(Function.identity(), enemies::with));

        for (var entry : enemyUnits.entrySet()) {
            var unitType = entry.getKey();
            var archetype = entry.getValue();

            System.out.println("- Before enemyUnits, " + unitType);
            archetype.createBatch(3, (i, factory) -> factory.create(new Position(i, i), new Velocity(i, i)));
            System.out.println("- After enemyUnits, " + unitType);
        }
    }

    enum Enemy {
        ENEMY
    }

    enum UnitType {
        WARRIOR, RANGER, MAGE;

        static final int count = values().length;
    }

    private static void benchmark() {
        System.out.println();
        System.out.println("-- Benchmark");

        /*
         * The main selling point of an archetype is the performance benefits it provides
         * over creating entities with world.createEntity(...).
         * 
         * Let's create a very sophisticated benchmark. "count" will be the number of entities we create.
         * You can play around with it. Depending on your system you may want to increase or decrease the value 
         * to see the difference. "warmups" is the number of warmups we'll perform before the benchmark.
         */
        var count = 1_000_000; // try to keep it even
        var warmups = 3;

        /*
         * Before we run any "benchmarks", we'll make it a little bit more fair:
         * 
         *      - We create a world and provide the expected number of entities to avoid array copying
         *      - We create a composition that matches all entities so we can easily delete them in between runs
         *      - We perform a couple of warmup runs to get the library warmed up
         */
        var world = DefaultWorld.builder().expectedEntities(count).build();
        var all = world.createComposition(Composition.all());
        var archetype = world.createArchetype(Position.class, Velocity.class);

        for (int w = 0; w < warmups; w++) {
            // Create entities (archetype)
            archetype.createBatch(count, (i, factory) -> factory.create(new Position(i, i), new Velocity(i, i)));

            // Delete entities and process deletions
            all.process(world::deleteEntity);
            world.process();

            // Create entities (world)
            for (int i = 0; i < count; i++) {
                world.createEntity(new Position(i, i), new Velocity(i, i));
            }

            // Delete entities and process deletions
            all.process(world::deleteEntity);
            world.process();
        }

        {
            /*
             * We'll start with world.createEntity.
             */
            var start = System.currentTimeMillis(); // yes, I know. There is the benchmark module for JMH!

            for (int i = 0; i < count; i++) {
                world.createEntity(new Position(i, i), new Velocity(i, i));
            }

            var duration = System.currentTimeMillis() - start;
            System.out.println("world.createEntity:    %5d ms".formatted(duration));

            // Delete entities and process deletions
            all.process(world::deleteEntity);
            world.process();
        }

        {
            /*
             * Now let's see how archetype performs. Let's begin with archetype.create(...)
             */
            var start = System.currentTimeMillis();

            for (int i = 0; i < count; i++) {
                archetype.create(new Position(i, i), new Velocity(i, i));
            }

            var duration = System.currentTimeMillis() - start;
            System.out.println("archetype.create:      %5d ms".formatted(duration));

            // Delete entities and process deletions
            all.process(world::deleteEntity);
            world.process();
        }

        {
            /*
             * Lastly, we'll try archetype.createBatch(count, ...).
             */
            var start = System.currentTimeMillis();

            archetype.createBatch(count, (i, factory) -> factory.create(new Position(i, i), new Velocity(i, i)));

            var duration = System.currentTimeMillis() - start;
            System.out.println("archetype.createBatch: %5d ms".formatted(duration));

            // Delete entities and process deletions
            all.process(world::deleteEntity);
            world.process();
        }

        /*
         * On an i5-8400 and 2400 MHz DIMM RAM this results in the following times:
         * 
         *      world.createEntity:      723 ms
         *      archetype.create:        318 ms
         *      archetype.createBatch:    96 ms
         * 
         * So even when creating only single entities every now and then, there is a good reason 
         * to use archetypes!
         * 
         * Disclaimer: 
         * This is not a real benchmark, do not trust these numbers. These are at best indicators.
         * See the benchmark module for actual JMH benchmarks.
         */
    }

    private static String formatComponents(WildcardResult<Object> components) {
        return StreamSupport.stream(components.spliterator(), false)
                .map(component -> "  " + component)
                .collect(Collectors.joining(System.lineSeparator()));
    }

    private static ArchetypeExampleWorld createWorld() {
        return World.builder(ArchetypeExampleWorld.class).build();
    }

    private static void removeEntities() {
        // Composition matching all entities to pass each to World#deleteEntity
        world.createComposition(Composition.all()).process(world::deleteEntity);

        // Flush deletions
        world.process();
    }

}
