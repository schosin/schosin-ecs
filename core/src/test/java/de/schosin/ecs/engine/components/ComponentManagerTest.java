package de.schosin.ecs.engine.components;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import de.schosin.ecs.api.components.ComponentSetConfig;
import de.schosin.ecs.api.components.Relation.ComponentRelation;
import de.schosin.ecs.api.components.Relation.EntityRelation;
import de.schosin.ecs.api.components.Relation.Exclusive;
import de.schosin.ecs.api.components.Relations.ComponentRelations;
import de.schosin.ecs.api.components.Relations.EntityRelations;
import de.schosin.ecs.api.components.types.ClassType;
import de.schosin.ecs.api.components.types.ComponentType.RegularComponentType;
import de.schosin.ecs.api.components.types.RelationComponentType.EntityRelationType;
import de.schosin.ecs.api.components.types.RelationFetchType.EntityRelationFetchType;
import de.schosin.ecs.engine.AbstractWorldTest;

public class ComponentManagerTest extends AbstractWorldTest {

    @Nested
    class GetRegularComponentTypesTest {

        @ParameterizedTest
        @MethodSource("regularComponentTypes")
        void testRegularComponentType(RegularComponentType<?, ?> componentType) {
            var componentTypes = componentManager.getRegularComponentTypes(componentType);
            assertThat(componentTypes).containsExactly(componentType);
        }

        @Test
        void testEntityRelationFetchType() {
            var relation1 = componentManager.getComponent(new EntityRelationType<>(C1.class)).type();
            componentManager.getComponent(new EntityRelationType<>(C2.class)).type();

            var componentTypes = componentManager.getRegularComponentTypes(new EntityRelationFetchType<>(C1.class, component(C2.class)));
            assertThat(componentTypes).containsExactly(relation1);
        }

        @Test
        void testComponentSet() {
            var componentTypes = componentManager.getRegularComponentTypes(SimpleComponentSet.TYPE);
            assertThat(componentTypes).containsExactlyInAnyOrder(
                    component(C3.class),
                    exclusiveRelation(Exclusive1.class, C2.class), relation(C1.class, C2.class),
                    exclusiveRelation(Exclusive1.class), relation(C1.class));
        }

        @Test
        void testNestedComponentSet() {
            componentManager.getComponent(new ClassType<>(C3.class)).type();

            var componentTypes = componentManager.getRegularComponentTypes(NestedComponentSet.TYPE);
            assertThat(componentTypes).containsExactlyInAnyOrder(
                    component(C3.class),
                    exclusiveRelation(Exclusive1.class, C2.class), relation(C1.class, C2.class),
                    exclusiveRelation(Exclusive1.class), relation(C1.class));
        }

        private static Stream<RegularComponentType<?, ?>> regularComponentTypes() {
            return Stream.of(
                    component(C1.class),
                    exclusiveRelation(Exclusive1.class, C2.class),
                    relation(C1.class, C2.class),
                    exclusiveRelation(Exclusive1.class),
                    relation(C1.class));
        }

        @SuppressWarnings("unused")
        @ComponentSetConfig("SimpleComponentSet")
        private void simpleComponentSet(int entityId, C3 c3,
                ComponentRelation<Exclusive1, C2> componentRelation, ComponentRelations<C1, C2> componentRelations,
                EntityRelation<Exclusive1> entityRelation, EntityRelations<C1> entityRelations) {
        }

        @SuppressWarnings("unused")
        @ComponentSetConfig("NestedComponentSet")
        private void nestedComponentSet(int entityId, SimpleComponentSet simpleSet) {
        }

    }

    public interface Bound {
    }

    public record C1() implements Bound {
    }

    public record C2() implements Bound {
    }

    public record C3() {
    }

    public record Exclusive1() implements Exclusive, Bound {
    }

    record Exclusive2() implements Exclusive {
    }

}
