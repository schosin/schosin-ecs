package de.schosin.ecs.engine.utils.components;

import static de.schosin.ecs.api.components.types.ComponentType.component;
import static de.schosin.ecs.api.components.types.ComponentType.exclusiveRelation;
import static de.schosin.ecs.api.components.types.ComponentType.relation;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import de.schosin.ecs.api.components.Relation;
import de.schosin.ecs.api.components.Relation.Exclusive;
import de.schosin.ecs.engine.components.mappers.MyComponentSet;
import de.schosin.ecs.engine.components.mappers.RelationComponentSet;

public class ComponentSetsHelperTest {

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

}
