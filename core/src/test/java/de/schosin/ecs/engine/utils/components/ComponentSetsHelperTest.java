package de.schosin.ecs.engine.utils.components;

import static de.schosin.ecs.api.components.ComponentType.component;
import static de.schosin.ecs.api.components.ComponentType.exclusiveRelation;
import static de.schosin.ecs.api.components.ComponentType.relation;
import static org.assertj.core.api.Assertions.assertThat;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import de.schosin.ecs.api.components.ComponentSet;
import de.schosin.ecs.api.components.Relation;
import de.schosin.ecs.api.components.Relation.ComponentRelation;
import de.schosin.ecs.api.components.Relation.EntityRelation;
import de.schosin.ecs.api.components.Relation.Exclusive;
import de.schosin.ecs.api.components.Result.ComponentRelationResult;
import de.schosin.ecs.api.components.Result.EntityRelationResult;
import de.schosin.ecs.engine.utils.components.ComponentSetsHelperTest.Birthplace;
import de.schosin.ecs.engine.utils.components.ComponentSetsHelperTest.Location;
import de.schosin.ecs.engine.utils.components.ComponentSetsHelperTest.MyComponentSet;
import de.schosin.ecs.engine.utils.components.ComponentSetsHelperTest.Position;
import de.schosin.ecs.engine.utils.components.ComponentSetsHelperTest.RelationComponentSet;
import de.schosin.ecs.engine.utils.components.ComponentSetsHelperTest.Velocity;

public class ComponentSetsHelperTest {

    public interface CustomComponentSet extends ComponentSet {
    }

    @Nested
    class GetComponentTypesTest {

        @Test
        void testSimpleComponents() {
            var factory = ComponentSetsHelper.getFactory(MyComponentSet.class);
            assertThat(factory.getComponents())
                    .extracting("type")
                    .containsExactly(
                            component(Position.class),
                            component(Velocity.class));
        }

        @Test
        void testRelationComponentSet() {
            var factory = ComponentSetsHelper.getFactory(RelationComponentSet.class);
            assertThat(factory.getComponents())
                    .extracting("type")
                    .containsExactly(
                            exclusiveRelation(Birthplace.class, Position.class),
                            relation(Location.class, Position.class),
                            exclusiveRelation(Birthplace.class),
                            relation(Location.class));
        }

    }

    @Nested
    class GetTest {

        @Test
        void testSimpleComponents() {
            var factory = ComponentSetsHelper.getFactory(MyComponentSet.class);

            var pos = new Position();
            var velocity = new Velocity();

            var instance = factory.getInstance(42, pos, velocity);
            assertThat(instance).isInstanceOf(MyComponentSetImpl.class);
            assertThat(instance.entityId()).isEqualTo(42);
            assertThat(instance.pos()).isSameAs(pos);
            assertThat(instance.velocity()).isSameAs(velocity);
        }

        @Test
        void testRelationComponentSet() {
            var factory = ComponentSetsHelper.getFactory(RelationComponentSet.class);

            var birthplace = Relation.create(Birthplace.Birthplace, new Position());
            var birthplaceId = Relation.create(Birthplace.Birthplace, 1);

            var instance = factory.getInstance(42, birthplace, null, birthplaceId, null);
            assertThat(instance).isInstanceOf(RelationComponentSet.class);
            assertThat(instance.entityId()).isEqualTo(42);
            assertThat(instance.birthplace()).isSameAs(birthplace);
            assertThat(instance.birthplaceId()).isSameAs(birthplaceId);
        }

    }

    public record Position() {
    }

    public record Velocity() {
    }

    public enum Location {
        Home, Work
    }

    public enum Birthplace implements Exclusive {
        Birthplace
    }

    public interface MyComponentSet extends ComponentSet {

        ComponentSetData<MyComponentSet> DATA = ComponentSet.builder(MyComponentSet::factory)
                .add(new ComponentAccessor<>(MyComponentSet::pos) {})
                .add(new ComponentAccessor<>(MyComponentSet::velocity) {})
                .build();

        private static MyComponentSet factory(int entityId, Object[] components) {
            return get(entityId, (Position) components[0], (Velocity) components[1]);
        }

        static MyComponentSet get(int entityId, Position pos, Velocity velocity) {
            return new MyComponentSetImpl(entityId, pos, velocity);
        }

        Position pos();

        Velocity velocity();

    }

    public interface RelationComponentSet extends ComponentSet {

        ComponentSetData<RelationComponentSet> DATA = ComponentSet.builder(RelationComponentSet::factory)
                .add(new ComponentAccessor<>(RelationComponentSet::birthplace) {})
                .add(new ComponentAccessor<>(RelationComponentSet::locations) {})
                .add(new ComponentAccessor<>(RelationComponentSet::birthplaceId) {})
                .add(new ComponentAccessor<>(RelationComponentSet::locationIds) {})
                .build();

        @SuppressWarnings("unchecked")
        private static RelationComponentSet factory(int entityId, Object[] components) {
            return get(entityId,
                    (ComponentRelation<Birthplace, Position>) components[0],
                    (ComponentRelationResult<Location, Position>) components[1],
                    (EntityRelation<Birthplace>) components[2],
                    (EntityRelationResult<Location>) components[3]);
        }

        static RelationComponentSet get(int entityId,
                ComponentRelation<Birthplace, Position> birthplace,
                ComponentRelationResult<Location, Position> locations,
                EntityRelation<Birthplace> birthplaceId,
                EntityRelationResult<Location> locationIds) {

            return new RelationComponentSetImpl(entityId, birthplace, locations, birthplaceId, locationIds);
        }

        ComponentRelation<Birthplace, Position> birthplace();

        ComponentRelationResult<Location, Position> locations();

        EntityRelation<Birthplace> birthplaceId();

        EntityRelationResult<Location> locationIds();

    }

}

class MyComponentSetImpl implements MyComponentSet {

    private final int entityId;
    private final Position pos;
    private final Velocity velocity;

    public MyComponentSetImpl(int entityId, Position pos, Velocity velocity) {
        this.entityId = entityId;
        this.pos = pos;
        this.velocity = velocity;
    }

    @Override
    public int entityId() {
        return entityId;
    }

    @Override
    public Position pos() {
        return pos;
    }

    @Override
    public @Nullable Velocity velocity() {
        return velocity;
    }
}

class RelationComponentSetImpl implements RelationComponentSet {

    private final int entityId;
    private final ComponentRelation<Birthplace, Position> birthplace;
    private final ComponentRelationResult<Location, Position> locations;
    private final EntityRelation<Birthplace> birthplaceId;
    private final EntityRelationResult<Location> locationIds;

    public RelationComponentSetImpl(int entityId, ComponentRelation<Birthplace, Position> birthplace, ComponentRelationResult<Location, Position> locations, EntityRelation<Birthplace> birthplaceId,
            EntityRelationResult<Location> locationIds) {

        this.entityId = entityId;
        this.birthplace = birthplace;
        this.locations = locations;
        this.birthplaceId = birthplaceId;
        this.locationIds = locationIds;
    }

    @Override
    public int entityId() {
        return entityId;
    }

    @Override
    public ComponentRelation<Birthplace, Position> birthplace() {
        return birthplace;
    }

    @Override
    public ComponentRelationResult<Location, Position> locations() {
        return locations;
    }

    @Override
    public EntityRelation<Birthplace> birthplaceId() {
        return birthplaceId;
    }

    @Override
    public EntityRelationResult<Location> locationIds() {
        return locationIds;
    }

}
