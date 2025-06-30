package de.schosin.ecs.engine.utils.components;

import static de.schosin.ecs.api.components.types.ComponentType.component;
import static de.schosin.ecs.api.components.types.ComponentType.exclusiveRelation;
import static de.schosin.ecs.api.components.types.ComponentType.relation;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

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
