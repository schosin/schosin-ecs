package de.schosin.ecs.integration.threadsafety;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.time.Duration;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Random;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import de.schosin.ecs.api.components.ComponentSetConfig;
import de.schosin.ecs.api.components.mappers.ComponentMapper.PooledComponentMapper;
import de.schosin.ecs.api.data.DataProcessor;
import de.schosin.ecs.integration.AbstractEcsIT;
import de.schosin.ecs.integration.components.Acceleration;
import de.schosin.ecs.integration.components.EntityId;
import de.schosin.ecs.integration.components.Position;
import de.schosin.ecs.integration.components.Velocity;
import de.schosin.ecs.plugins.archetype.Archetype2;
import de.schosin.ecs.plugins.archetype.Archetype2.Factory2;
import de.schosin.ecs.plugins.composition.Composition;
import de.schosin.ecs.plugins.composition.CompositionData1;
import de.schosin.ecs.plugins.composition.CompositionData4;
import de.schosin.ecs.plugins.experimental.system.SystemPlugin;
import de.schosin.ecs.plugins.experimental.system.systems.BaseSystem;
import de.schosin.ecs.plugins.experimental.system.systems.ComponentSetSystem;
import de.schosin.ecs.plugins.experimental.system.systems.CompositionSystem;
import de.schosin.ecs.worlds.DefaultWorld;

public class ThreadSafetyIT extends AbstractEcsIT {
    static final Random rng = new Random();

    record Params(int consumers, int spawners, int entityCount, float accelerateChance) {
    }

    Params params;

    @ParameterizedTest
    @MethodSource("params")
    void testThreadSafety(Params params) {
        this.params = params;

        var systems = SystemPlugin.standalone(world);
        systems.addParallelSystemGroup("main", group -> group.add(buildSystems(world)));

        runSimulation(systems, Duration.ofMinutes(10));
    }

    static Stream<Params> params() {
        return Stream.of(
                new Params(1, 5, 1000, 0.2f),
                new Params(10, 5, 1000, 0.2f));
    }

    private BaseSystem[] buildSystems(DefaultWorld world) {
        var system = 0;
        var systems = new BaseSystem[2 + params.consumers + params.spawners];

        new AcceleratorSystem();

        systems[system++] = new OutOfBoundsSystem();
        systems[system++] = new TrackingSystem();

        for (int i = 0; i < params.consumers; i++) {
            systems[system++] = new ConsumerSystem(world);
        }

        for (int i = 0; i < params.spawners; i++) {
            systems[system++] = new SpawnerSystem();
        }

        assertThat(systems).doesNotContainNull();
        return systems;
    }

    class OutOfBoundsSystem implements BaseSystem {

        static final int BOUNDS = 200;

        private final CompositionData1<Position> composition;

        public OutOfBoundsSystem() {
            this.composition = world.createComposition(Composition.all(Position.class), Position.class);
        }

        @Override
        public void process() {
            composition.process(this::checkBounds);
        }

        private void checkBounds(int entityId, Position pos) {
            if (pos.x < -BOUNDS || pos.x > BOUNDS || pos.y < -BOUNDS || pos.y > BOUNDS) {
                world.deleteEntity(entityId);
            }
        }

    }

    class TrackingSystem extends CompositionSystem<DataProcessor<EntityId>> implements DataProcessor<EntityId> {

        private final CompositionData4<Position, Velocity, Acceleration, EntityId> all;
        private final PooledComponentMapper<EntityId> mapper;

        private final Map<Object, Integer> components = new IdentityHashMap<Object, Integer>();

        public TrackingSystem() {
            super(world.createComposition(Composition.all(EntityId.class), EntityId.class));

            this.all = world.createComposition(Composition.all(), Position.class, Velocity.class, Acceleration.class, EntityId.class);
            this.mapper = world.getPooledComponents(EntityId.class);

            var init = world.createComposition(Composition.none(EntityId.class));
            init.inserted(this::addTracking);
        }

        private void addTracking(int entityId) {
            var component = mapper.getInstance().init(entityId);
            System.out.println("Tracking %d: %d".formatted(entityId, System.identityHashCode(components)));

            mapper.add(entityId, component);
        }

        @Override
        public void begin() {
            all.process((entityId, pos, vel, accel, track) -> {
                var existing = components.put(pos, entityId);
                if (existing != null) {
                    fail("Entity %d and %d use same position instance %d: %s", entityId, existing, System.identityHashCode(pos), pos);
                }

                existing = components.put(vel, entityId);
                if (existing != null) {
                    fail("Entity %d and %d use same velocity instance %d: %s", entityId, existing, System.identityHashCode(vel), vel);
                }

                if (accel != null) {
                    existing = components.put(accel, entityId);
                    if (existing != null) {
                        fail("Entity %d and %d use same acceleration instance %d: %s", entityId, existing, System.identityHashCode(accel), accel);
                    }
                }
                if (track != null) {
                    existing = components.put(track, entityId);
                    if (existing != null) {
                        fail("Entity %d and %d use same entityId instance %d: %s", entityId, existing, System.identityHashCode(track), track);
                    }
                }
            });

            components.clear();
        }

        @Override
        public void process(int entityId, EntityId component) {
            if (entityId != component.entityId) {
                System.err.println("Tracking %d: %d".formatted(entityId, System.identityHashCode(components)));
            }

            assertThat(component).as("tracking: EntityId component must not be null").isNotNull();
            assertThat(component.entityId).as("tracking: EntityId component must contain id of entity").isEqualTo(entityId);
        }

    }

    class AcceleratorSystem {

        private final PooledComponentMapper<Acceleration> mapper;

        public AcceleratorSystem() {
            this.mapper = world.getPooledComponents(Acceleration.class);

            var composition = world.createComposition(Composition.all(Position.class, Velocity.class).none(Acceleration.class));
            composition.inserted(this::handleInserted);
        }

        private void handleInserted(int entityId) {
            if (rng.nextFloat() < params.accelerateChance) {
                var angle = rng.nextDouble(Math.PI * 2);
                var acceleration = mapper.getInstance().init(50f * (float) Math.sin(angle), 50 * (float) Math.cos(angle));

                mapper.add(entityId, acceleration);
            }
        }

    }

    class SpawnerSystem implements BaseSystem {

        private static final float SPEED = 50f;

        private final Archetype2<Position, Velocity> archetype;
        private final Composition composition;

        public SpawnerSystem() {
            this.archetype = world.createArchetype(Position.class, Velocity.class);
            this.composition = world.createComposition(Composition.all(Position.class, Velocity.class));
        }

        @Override
        public void process() {
            var missing = params.entityCount - composition.getCount();
            if (missing <= 0) {
                return;
            }

            var spawn = missing > 10 ? 10 : missing;
            archetype.createBatch(spawn, this::spawnEntity);
        }

        private void spawnEntity(Factory2<Position, Velocity> factory) {
            var position = archetype.getInstance(Position.class).init(0, 0);

            var angle = rng.nextDouble(Math.PI * 2);
            var velocity = archetype.getInstance(Velocity.class).init(SPEED * (float) Math.sin(angle), SPEED * (float) Math.cos(angle));

            factory.create(position, velocity);
        }

    }

    class ConsumerSystem extends ComponentSetSystem<ConsumerSystemSet, ConsumerSystemSet.Processor> implements ConsumerSystemSet.Processor {

        public ConsumerSystem(DefaultWorld world) {
            super(world, ConsumerSystemSet.TYPE, Composition.all(Position.class, Velocity.class));
        }

        @ComponentSetConfig("ConsumerSystemSet")
        public void process(int entityId, Position pos, Velocity velocity, Acceleration acceleration) {
            if (acceleration != null) {
                assertThat(acceleration.ax + acceleration.ay).as("acceleration: ax + ay").isNotZero();

                velocity.vx += acceleration.ax;
                velocity.vy += acceleration.ay;
            } else {
                assertThat(velocity.vx + velocity.vy).as("velocity: vx + vy").isNotZero();
            }

            pos.x += velocity.vx;
            pos.y += velocity.vy;
        }

    }

}
