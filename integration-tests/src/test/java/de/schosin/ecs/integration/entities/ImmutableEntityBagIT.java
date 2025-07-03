package de.schosin.ecs.integration.entities;

import static org.assertj.core.api.Assertions.assertThat;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;

import de.schosin.ecs.api.components.Relation;
import de.schosin.ecs.api.components.Relations;
import de.schosin.ecs.api.components.types.ClassType;
import de.schosin.ecs.api.components.types.RelationComponentType.ComponentRelationType;
import de.schosin.ecs.api.components.types.RelationComponentType.EntityRelationType;
import de.schosin.ecs.api.components.types.RelationComponentType.ExclusiveComponentRelationType;
import de.schosin.ecs.api.components.types.RelationComponentType.ExclusiveEntityRelationType;
import de.schosin.ecs.api.entities.ImmutableEntityBag;
import de.schosin.ecs.integration.AbstractEcsIT;
import de.schosin.ecs.integration.components.Favorite;
import de.schosin.ecs.integration.components.Position;
import de.schosin.ecs.integration.components.Velocity;
import de.schosin.ecs.plugins.experimental.system.SystemPlugin;
import de.schosin.ecs.plugins.experimental.system.systems.BaseSystem;
import de.schosin.ecs.worlds.DefaultWorld;

public class ImmutableEntityBagIT extends AbstractEcsIT {

    private static final ClassType<Position> POSITION = component(Position.class);
    private static final ComponentRelationType<Position, Velocity> POS_VELOCITY = relation(Position.class, Velocity.class);
    private static final ExclusiveComponentRelationType<Favorite, Position> FAVORITE_POS = exclusiveRelation(Favorite.class, Position.class);
    private static final EntityRelationType<Position> POS_ENTITY = relation(Position.class);
    private static final ExclusiveEntityRelationType<Favorite> FAVORITE_ENTITY = exclusiveRelation(Favorite.class);

    private static final int SPAWN_BEFORE = 6;
    private static final int SPAWN_AFTER = 7;

    @Test
    void testIteration() {
        spawnEntities(SPAWN_BEFORE);

        var systems = SystemPlugin.standalone(world);
        systems.addSystems(
                new ClassTypeSystem(world),
                new ComponentRelationTypeSystem(world),
                new ExclusiveComponentRelationTypeSystem(world),
                new EntityRelationTypeSystem(world),
                new ExclusiveEntityRelationTypeSystem(world));

        spawnEntities(SPAWN_AFTER);

        runSimulation(systems);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(ClassTypeSystem.ran).as("ClassTypeSystem ran").isTrue();
            softly.assertThat(ComponentRelationTypeSystem.ran).as("ComponentRelationTypeSystem ran").isTrue();
            softly.assertThat(ExclusiveComponentRelationTypeSystem.ran).as("ExclusiveComponentRelationTypeSystem ran").isTrue();
            softly.assertThat(EntityRelationTypeSystem.ran).as("EntityRelationTypeSystem ran").isTrue();
            softly.assertThat(ExclusiveEntityRelationTypeSystem.ran).as("ExclusiveEntityRelationTypeSystem ran").isTrue();
        });
    }

    private void spawnEntities(int count) {
        var target = world.createEntity();

        var archetype = world.createArchetype(POSITION, POS_VELOCITY, FAVORITE_POS, POS_ENTITY, FAVORITE_ENTITY);

        archetype.createBatch(count, (i, factory) -> {
            factory.create(
                    new Position().init(i, i),
                    Relations.create(new Position().init(i, i), new Velocity().init(i, i)),
                    Relation.create(new Favorite().init("" + i), new Position().init(0, 0)),
                    Relations.create(new Position().init(i, i), target),
                    Relation.create(new Favorite().init("" + i), target));
        });
    }

    static class ClassTypeSystem implements BaseSystem {
        static boolean ran = false;

        private final ImmutableEntityBag entities;

        public ClassTypeSystem(DefaultWorld world) {
            this.entities = world.getEntities(POSITION);
        }

        @Override
        public void process() {
            assertThat(this.entities.size()).isEqualTo(SPAWN_BEFORE + SPAWN_AFTER);

            for (int i = 0, s = this.entities.size(); i < s; i++) {
                var entity = entities.get(i);
                assertThat(entity.get(POSITION)).isNotNull();
            }

            ran = true;
        }

    }

    static class ComponentRelationTypeSystem implements BaseSystem {
        static boolean ran = false;

        private final ImmutableEntityBag entities;

        public ComponentRelationTypeSystem(DefaultWorld world) {
            this.entities = world.getEntities(POS_VELOCITY);
        }

        @Override
        public void process() {
            assertThat(this.entities.size()).isEqualTo(SPAWN_BEFORE + SPAWN_AFTER);

            for (int i = 0, s = this.entities.size(); i < s; i++) {
                var entity = entities.get(i);
                assertThat(entity.get(POS_VELOCITY).size()).isNotZero();
            }

            ran = true;
        }

    }

    static class ExclusiveComponentRelationTypeSystem implements BaseSystem {
        static boolean ran = false;

        private final ImmutableEntityBag entities;

        public ExclusiveComponentRelationTypeSystem(DefaultWorld world) {
            this.entities = world.getEntities(FAVORITE_POS);
        }

        @Override
        public void process() {
            assertThat(this.entities.size()).isEqualTo(SPAWN_BEFORE + SPAWN_AFTER);

            for (int i = 0, s = this.entities.size(); i < s; i++) {
                var entity = entities.get(i);
                assertThat(entity.get(FAVORITE_POS)).isNotNull();
            }

            ran = true;
        }

    }

    static class EntityRelationTypeSystem implements BaseSystem {
        static boolean ran = false;

        private final ImmutableEntityBag entities;

        public EntityRelationTypeSystem(DefaultWorld world) {
            this.entities = world.getEntities(POS_ENTITY);
        }

        @Override
        public void process() {
            assertThat(this.entities.size()).isEqualTo(SPAWN_BEFORE + SPAWN_AFTER);

            for (int i = 0, s = this.entities.size(); i < s; i++) {
                var entity = entities.get(i);
                assertThat(entity.get(POS_ENTITY).size()).isNotZero();
            }

            ran = true;
        }

    }

    static class ExclusiveEntityRelationTypeSystem implements BaseSystem {
        static boolean ran = false;

        private final ImmutableEntityBag entities;

        public ExclusiveEntityRelationTypeSystem(DefaultWorld world) {
            this.entities = world.getEntities(FAVORITE_ENTITY);
        }

        @Override
        public void process() {
            assertThat(this.entities.size()).isEqualTo(SPAWN_BEFORE + SPAWN_AFTER);

            for (int i = 0, s = this.entities.size(); i < s; i++) {
                var entity = entities.get(i);
                assertThat(entity.get(FAVORITE_ENTITY)).isNotNull();
            }

            ran = true;
        }

    }

}
