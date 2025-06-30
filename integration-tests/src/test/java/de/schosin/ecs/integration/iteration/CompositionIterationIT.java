package de.schosin.ecs.integration.iteration;

import static de.schosin.ecs.api.components.types.ComponentType.WILDCARD;
import static de.schosin.ecs.api.components.types.ComponentType.wildcardRelation;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;

import de.schosin.ecs.api.components.ComponentSetConfig;
import de.schosin.ecs.api.components.Relation;
import de.schosin.ecs.api.components.Relation.EntityRelationData;
import de.schosin.ecs.api.components.Relations.ComponentRelations;
import de.schosin.ecs.api.components.Relations.EntityRelations;
import de.schosin.ecs.api.components.Result.ComponentResult;
import de.schosin.ecs.api.components.mappers.ComponentMapper;
import de.schosin.ecs.api.components.mappers.ComponentSetMapper;
import de.schosin.ecs.api.components.types.ComponentType;
import de.schosin.ecs.integration.AbstractEcsIT;
import de.schosin.ecs.integration.components.Favorite;
import de.schosin.ecs.integration.components.Hitbox;
import de.schosin.ecs.integration.components.Position;
import de.schosin.ecs.integration.components.Size;
import de.schosin.ecs.integration.components.Velocity;
import de.schosin.ecs.plugins.archetype.Archetype2;
import de.schosin.ecs.plugins.composition.Composition;
import de.schosin.ecs.plugins.composition.CompositionData;
import de.schosin.ecs.plugins.composition.CompositionData1;
import de.schosin.ecs.plugins.composition.CompositionData2;
import de.schosin.ecs.plugins.composition.CompositionSet;
import de.schosin.ecs.plugins.data.mappers.DataTypeMapper;
import de.schosin.ecs.plugins.data.types.Data3;
import de.schosin.ecs.plugins.data.types.DataType;
import de.schosin.ecs.plugins.data.types.DataType4.Processor4;
import de.schosin.ecs.plugins.experimental.system.SystemPlugin;
import de.schosin.ecs.plugins.experimental.system.systems.BaseSystem;
import de.schosin.ecs.worlds.DefaultWorld;

/**
 * Intended to test correct usage of pooling. 
 * Attach VisualVM memory profiler and it should have zero allocations during its {@link #runSimulation(SystemPlugin) runtime} in {@code de.schosin.**}.
 * 
 * <p>
 * Use {@link #runSimulation(SystemPlugin, Duration)} to increase duration. 
 */
public class CompositionIterationIT extends AbstractEcsIT {

    @Test
    void testIteration() {
        var systems = SystemPlugin.standalone(world);
        systems.addSystems(
                new ClassIterationSystem(world),
                new ComponentRelationsIterationSystem(world),
                new EntityRelationsIterationSystem(world),
                new EntityRelationFetchIterationSystem(world),
                new WildcardComponentRelationsIterationSystem(world),
                new WildcardEntityRelationsIterationSystem(world),
                new DataTypeIterationSystem(world),
                new CreateAndDeleteSystem(world));

        runSimulation(systems);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(ClassIterationSystem.ran).as("ClassIterationSystem ran").isTrue();
            softly.assertThat(ComponentRelationsIterationSystem.ran).as("ComponentRelationsIterationSystem ran").isTrue();
            softly.assertThat(EntityRelationsIterationSystem.ran).as("EntityRelationsIterationSystem ran").isTrue();
            softly.assertThat(EntityRelationFetchIterationSystem.ran).as("EntityRelationFetchIterationSystem ran").isTrue();
            softly.assertThat(WildcardComponentRelationsIterationSystem.ran).as("WildcardComponentRelationsIterationSystem ran").isTrue();
            softly.assertThat(WildcardEntityRelationsIterationSystem.ran).as("WildcardEntityRelationsIterationSystem ran").isTrue();
            softly.assertThat(DataTypeIterationSystem.ran).as("DataTypeIterationSystem ran").isTrue();
            softly.assertThat(CreateAndDeleteSystem.ran).as("CreateAndDeleteSystem ran").isTrue();
        });
    }

    static class ClassIterationSystem implements BaseSystem {
        static boolean ran = false;

        private enum Marker {
            INSTANCE
        }

        private final CompositionSet<ClassIterationComponents.Processor> composition;

        public ClassIterationSystem(DefaultWorld world) {
            this.composition = world.createComposition(Composition.all(Marker.class), ClassIterationComponents.TYPE);

            for (int i = 0; i < 10; i++) {
                world.createEntity(Marker.INSTANCE,
                        new Position().init(0, 0),
                        new Velocity().init(RNG.nextInt(1, 3), RNG.nextInt(1, 3)));
            }
        }

        @Override
        public void process() {
            this.composition.process(this::processEntity);
        }

        @ComponentSetConfig("ClassIterationComponents")
        private void processEntity(int entityId, Position pos, Velocity velocity) {
            pos.x += velocity.vx;
            pos.y += velocity.vy;

            ran = true;
        }

    }

    static class ComponentRelationsIterationSystem implements BaseSystem {
        static boolean ran = false;

        private enum Marker {
            INSTANCE
        }

        private final CompositionData1<ComponentRelations<Position, Velocity>> composition;

        public ComponentRelationsIterationSystem(DefaultWorld world) {
            this.composition = world.createComposition(Composition.all(Marker.class), relation(Position.class, Velocity.class));

            for (int i = 0; i < 10; i++) {
                world.createEntity(Marker.INSTANCE,
                        Relation.create(new Position().init(0, 0), new Velocity().init(RNG.nextInt(1, 3), RNG.nextInt(1, 3))),
                        Relation.create(new Position().init(0, 0), new Velocity().init(-RNG.nextInt(1, 3), -RNG.nextInt(1, 3))));
            }
        }

        @Override
        public void process() {
            this.composition.process(this::processEntity);
        }

        private void processEntity(int entityId, ComponentRelations<Position, Velocity> relations) {
            for (int i = 0, s = relations.size(); i < s; i++) {
                var relation = relations.get(i);

                var pos = relation.relationship();
                var velocity = relation.target();

                pos.x += velocity.vx;
                pos.y += velocity.vy;
            }

            ran = true;
        }

    }

    static class EntityRelationsIterationSystem implements BaseSystem {
        static boolean ran = false;

        private enum Marker {
            INSTANCE
        }

        private final CompositionData1<EntityRelations<Position>> composition;

        public EntityRelationsIterationSystem(DefaultWorld world) {
            this.composition = world.createComposition(Composition.all(Marker.class), relation(Position.class));

            var target1 = world.createEntity();
            var target2 = world.createEntity();

            for (int i = 0; i < 10; i++) {
                world.createEntity(Marker.INSTANCE,
                        Relation.create(new Position().init(0, 0), target1),
                        Relation.create(new Position().init(0, 0), target2));
            }
        }

        @Override
        public void process() {
            this.composition.process(this::processEntity);
        }

        private void processEntity(int entityId, EntityRelations<Position> relations) {
            for (int i = 0, s = relations.size(); i < s; i++) {
                var relation = relations.get(i);

                var pos = relation.relationship();
                var target = relation.target();

                pos.x += target;
                pos.y -= target;
            }

            ran = true;
        }

    }

    static class EntityRelationFetchIterationSystem implements BaseSystem {
        static boolean ran = false;

        private enum Marker {
            INSTANCE
        }

        private final CompositionData2<Position, EntityRelationData<Favorite, Position>> composition;

        public EntityRelationFetchIterationSystem(DefaultWorld world) {
            this.composition = world.createComposition(Composition.all(Marker.class), component(Position.class), ComponentType.exclusiveRelation(Favorite.class, component(Position.class)));

            var target1 = world.createEntity(new Position().init(10, 10));
            var target2 = world.createEntity();

            for (int i = 0; i < 10; i++) {
                world.createEntity(Marker.INSTANCE,
                        new Position().init(-1, -1),
                        Relation.create(new Favorite(), i % 2 == 0 ? target1 : target2));
            }
        }

        @Override
        public void process() {
            this.composition.process(this::processEntity);
        }

        private void processEntity(int entityId, Position pos, EntityRelationData<Favorite, Position> relation) {
            var targetPos = relation.data();
            if (targetPos != null) {
                pos.x = targetPos.x;
                pos.y = targetPos.y;
            }

            ran = true;
        }

    }

    static class WildcardComponentRelationsIterationSystem implements BaseSystem {
        static boolean ran = false;

        private enum Marker {
            INSTANCE
        }

        private final CompositionData1<ComponentRelations<Object, Position>> composition;

        public WildcardComponentRelationsIterationSystem(DefaultWorld world) {
            this.composition = world.createComposition(Composition.all(Marker.class), wildcardRelation(Object.class, Position.class));

            for (int i = 0; i < 10; i++) {
                world.createEntity(Marker.INSTANCE,
                        Relation.create(new Velocity().init(RNG.nextInt(1, 3), RNG.nextInt(1, 3)), new Position().init(0, 0)),
                        Relation.create(new Velocity().init(-RNG.nextInt(1, 3), -RNG.nextInt(1, 3)), new Position().init(0, 0)),
                        Relation.create(new Favorite().init("home"), RNG.nextInt(1, 3)), new Position().init(0, 0));
            }
        }

        @Override
        public void process() {
            this.composition.process(this::processEntity);
        }

        private void processEntity(int entityId, ComponentRelations<Object, Position> relations) {
            for (int i = 0, s = relations.size(); i < s; i++) {
                var relation = relations.get(i);

                if (!(relation.relationship() instanceof Velocity velocity)) {
                    continue;
                }

                var pos = relation.target();

                pos.x += velocity.vx;
                pos.y += velocity.vy;
            }

            ran = true;
        }
    }

    static class WildcardEntityRelationsIterationSystem implements BaseSystem {
        static boolean ran = false;

        private enum Marker {
            INSTANCE
        }

        private final CompositionData2<Position, EntityRelations<Object>> composition;

        public WildcardEntityRelationsIterationSystem(DefaultWorld world) {
            this.composition = world.createComposition(Composition.all(Marker.class), component(Position.class), wildcardRelation(Object.class));

            var target1 = world.createEntity();
            var target2 = world.createEntity();
            var target3 = world.createEntity();

            for (int i = 0; i < 10; i++) {
                world.createEntity(Marker.INSTANCE,
                        new Position().init(0, 0),
                        Relation.create(new Velocity().init(RNG.nextInt(1, 3), -RNG.nextInt(1, 3)), target1),
                        Relation.create(new Velocity().init(-RNG.nextInt(1, 3), -RNG.nextInt(1, 3)), target2),
                        Relation.create(new Favorite().init("home"), target3));
            }
        }

        @Override
        public void process() {
            this.composition.process(this::processEntity);
        }

        private void processEntity(int entityId, Position pos, EntityRelations<Object> relations) {
            for (int i = 0, s = relations.size(); i < s; i++) {
                var relation = relations.get(i);

                if (relation.relationship() instanceof Velocity velocity) {
                    pos.x += velocity.vx;
                    pos.y += velocity.vy;

                    continue;
                }

                var target = relation.target();
                pos.x += target;
                pos.y -= target;
            }

            ran = true;
        }
    }

    static class DataTypeIterationSystem implements BaseSystem {
        static boolean ran = false;

        private enum Marker {
            INSTANCE
        }

        private final CompositionData<Processor4<Data3<Position, Velocity, ComponentResult<Object>>, Size, Hitbox, ClassIterationComponents>> composition;

        private final DataTypeMapper<Data3<Position, Velocity, Object>, Data3<Position, Velocity, ComponentResult<Object>>> dataM;
        private final ComponentMapper<Size> sizeM;
        private final ComponentMapper<Hitbox> hitboxM;
        private final ComponentSetMapper<ClassIterationComponents> classComponentsM;

        public DataTypeIterationSystem(DefaultWorld world) {
            var dataType = DataType.get(component(Position.class), component(Velocity.class), WILDCARD);

            this.composition = world.createComposition(Composition.all(Marker.class), dataType, component(Size.class), component(Hitbox.class), ClassIterationComponents.TYPE);

            this.dataM = world.getComponents(dataType);
            this.sizeM = world.getComponents(Size.class);
            this.hitboxM = world.getComponents(Hitbox.class);
            this.classComponentsM = world.getComponents(ClassIterationComponents.TYPE);

            for (int i = 0; i < 10; i++) {
                world.createEntity(Marker.INSTANCE,
                        new Position().init(0, 0),
                        new Velocity().init(RNG.nextInt(1, 3), RNG.nextInt(1, 3)),
                        new Size().init(10 + i, 20 - i),
                        new Hitbox());
            }
        }

        @Override
        public void process() {
            this.composition.process(this::processEntity);
            this.composition.process(this::processEntityMappers);
        }

        private void processEntity(int entityId, Data3<Position, Velocity, ComponentResult<Object>> data, Size size, Hitbox hitbox, ClassIterationComponents classComponents) {
            var pos = data.component1();
            var velocity = data.component2();

            pos.x += velocity.vx + size.width;
            pos.y += velocity.vy + size.height;

            hitbox.x1 = pos.x - size.width / 2;
            hitbox.y1 = pos.y - size.height / 2;
            hitbox.x2 = pos.x + size.width / 2;
            hitbox.y2 = pos.y + size.height / 2;

            var merged = 0;
            var components = data.component3();
            for (int i = 0, s = components.size(); i < s; i++) {
                merged += components.get(i).hashCode();
            }

            if (merged % 10 == 0) {
                pos.x = -pos.x;
            }

            assertThat(classComponents.pos()).isSameAs(pos);
            assertThat(classComponents.velocity()).isSameAs(velocity);

            ran = true;
        }

        private void processEntityMappers(int entityId) {
            processEntity(entityId, dataM.get(entityId), sizeM.get(entityId), hitboxM.get(entityId), classComponentsM.get(entityId));
        }

    }

    static class CreateAndDeleteSystem implements BaseSystem {
        static boolean ran = false;

        private static final int TARGET = 10;

        private enum Marker {
            INSTANCE
        }

        private final DefaultWorld world;
        private final Composition composition;
        private final Archetype2<Position, Velocity> archetype;

        private int counter = RNG.nextInt(5, 10);

        public CreateAndDeleteSystem(DefaultWorld world) {
            this.world = world;
            this.composition = world.createComposition(Composition.all(Marker.class));
            this.archetype = world.createArchetype(Position.class, Velocity.class).with(Marker.INSTANCE);

            this.archetype.createBatch(TARGET, init -> init.create(
                    archetype.getInstance(Position.class).init(0, 0),
                    archetype.getInstance(Velocity.class).init(RNG.nextInt(1, 3), RNG.nextInt(1, 3))));
        }

        @Override
        public void process() {
            this.composition.process(this::processEntity);

            if (composition.getCount() == 0) {
                this.archetype.createBatch(TARGET, init -> init.create(
                        archetype.getInstance(Position.class).init(0, 0),
                        archetype.getInstance(Velocity.class).init(RNG.nextInt(1, 3), RNG.nextInt(1, 3))));
            }
        }

        private void processEntity(int entityId) {
            if (--counter > 0) {
                return;
            }
            counter = RNG.nextInt(5, 10);

            if (RNG.nextInt(TARGET) >= composition.getCount()) {
                this.archetype.create(
                        archetype.getInstance(Position.class).init(0, 0),
                        archetype.getInstance(Velocity.class).init(RNG.nextInt(1, 3), RNG.nextInt(1, 3)));

                return;
            }

            world.deleteEntity(entityId);

            ran = true;
        }

    }

}
