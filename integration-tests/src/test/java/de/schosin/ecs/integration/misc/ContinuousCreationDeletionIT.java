package de.schosin.ecs.integration.misc;

import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import de.schosin.ecs.api.components.ComponentSetConfig;
import de.schosin.ecs.api.components.mappers.ComponentMapper.PooledComponentMapper;
import de.schosin.ecs.integration.AbstractEcsIT;
import de.schosin.ecs.integration.components.Hitbox;
import de.schosin.ecs.integration.components.Position;
import de.schosin.ecs.integration.components.Size;
import de.schosin.ecs.integration.components.Velocity;
import de.schosin.ecs.plugins.archetype.Archetype3;
import de.schosin.ecs.plugins.composition.Composition;
import de.schosin.ecs.plugins.composition.CompositionSet;
import de.schosin.ecs.plugins.experimental.system.SystemPlugin;
import de.schosin.ecs.plugins.experimental.system.systems.BaseSystem;
import de.schosin.ecs.worlds.DefaultWorld;

/*
 * Affected: Archetype storage implementation
 * Purpose: Verifies that the archetype storage engine properly tracks entityId lookups
 * 
 * The archetype storage mishandled the tracking of the indexes for entities by their id.
 * When a component array could be reused, it failed to set the lookup IntBag `entities`.
 */
@SuppressWarnings("unused")
public class ContinuousCreationDeletionIT extends AbstractEcsIT {

    @Test
    void integrationTest() {
        var systems = SystemPlugin.standalone(world);
        systems.addSystems(
                new CreationSystem(world),
                new HitboxSystem(world),
                new MovementSystem(world),
                new DeletionSystem(world));

        runSimulation(systems);
    }

    static class CreationSystem implements BaseSystem {
        private static final Random RNG = new Random();

        private final Composition composition;
        private final Archetype3<Position, Velocity, Size> archetype;

        public CreationSystem(DefaultWorld world) {
            this.composition = world.createComposition(Composition.all());
            this.archetype = world.createArchetype(Position.class, Velocity.class, Size.class);
        }

        @Override
        public void process() {
            var missing = 100 - composition.getCount();
            if (missing <= 0) {
                return;
            }

            var vx = -20 + 40 * RNG.nextFloat();
            var vy = -20 + 40 * RNG.nextFloat();

            archetype.createBatch(missing, init -> init.create(
                    archetype.getInstance(Position.class).init(0f, 0f),
                    archetype.getInstance(Velocity.class).init(vx, vy),
                    archetype.getInstance(Size.class).init(10, 10)));
        }

    }

    static class HitboxSystem implements BaseSystem {

        private final CompositionSet<HitboxComponents.Processor> composition;
        private final PooledComponentMapper<Hitbox> hitboxM;

        public HitboxSystem(DefaultWorld world) {
            this.composition = world.createComposition(Composition.all(Position.class, Hitbox.class), HitboxComponents.TYPE);
            this.hitboxM = world.getPooledComponents(Hitbox.class);

            var init = world.createComposition(Composition.all(Position.class, Size.class).none(Hitbox.class));
            init.inserted(this::addHitbox);
        }

        private void addHitbox(int entityId) {
            hitboxM.add(entityId);
        }

        @Override
        public void process() {
            this.composition.process(this::processEntity);
        }

        @ComponentSetConfig("HitboxComponents")
        private void processEntity(int entityId, Position pos, Size size, Hitbox hitbox) {
            var hw = 0.5f * size.width;
            var hh = 0.5f * size.height;

            hitbox.x1 = pos.x - hw;
            hitbox.y1 = pos.y - hh;
            hitbox.x2 = pos.x + hw;
            hitbox.y2 = pos.y + hh;
        }

    }

    static class MovementSystem implements BaseSystem {

        private CompositionSet<MovementComponents.Processor> composition;

        public MovementSystem(DefaultWorld world) {
            this.composition = world.createComposition(Composition.all(Position.class, Velocity.class), MovementComponents.TYPE);
        }

        @Override
        public void process() {
            composition.process(this::processEntity);
        }

        @ComponentSetConfig("MovementComponents")
        private void processEntity(int entityId, Position pos, Velocity velocity) {
            pos.x += 1f * velocity.vx;
            pos.y += 1f * velocity.vy;
        }

    }

    static class DeletionSystem implements BaseSystem {
        private static final Random RNG = new Random();

        private final DefaultWorld world;
        private final Composition composition;

        private final AtomicInteger counter = new AtomicInteger();

        public DeletionSystem(DefaultWorld world) {
            this.world = world;
            this.composition = world.createComposition(Composition.all());
        }

        @Override
        public void process() {
            var count = this.composition.getCount();

            var delete = RNG.nextInt(count);
            if (delete == 0) {
                return;
            }

            counter.set(delete);
            this.composition.process(entityId -> {
                if (counter.get() == 0) {
                    return;
                }

                counter.getAndDecrement();
                world.deleteEntity(entityId);
            });
        }

    }

}