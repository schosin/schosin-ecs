package de.schosin.ecs.api.components.types;

import static de.schosin.ecs.api.components.types.ComponentType.WILDCARD;
import static de.schosin.ecs.api.components.types.ComponentType.component;
import static de.schosin.ecs.api.components.types.ComponentType.componentSet;
import static de.schosin.ecs.api.components.types.ComponentType.exclusiveRelation;
import static de.schosin.ecs.api.components.types.ComponentType.relation;
import static de.schosin.ecs.api.components.types.ComponentType.wildcard;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import de.schosin.ecs.api.components.Relation;
import de.schosin.ecs.api.components.Relations;
import de.schosin.ecs.api.components.types.AbstractComponentTypeTest.Component;
import de.schosin.ecs.api.components.types.AbstractComponentTypeTest.ComponentInterface;
import de.schosin.ecs.api.components.types.AbstractComponentTypeTest.ExclusiveComponent;
import de.schosin.ecs.api.components.types.AbstractComponentTypeTest.MyComponentSet;
import de.schosin.ecs.api.components.types.AbstractComponentTypeTest.RelationshipComponent;
import de.schosin.ecs.api.components.types.AbstractComponentTypeTest.TargetComponent;
import de.schosin.ecs.api.components.types.ComponentType.RegularComponentType;
import de.schosin.ecs.api.components.types.RelationComponentType.ComponentRelationType;
import de.schosin.ecs.api.components.types.RelationComponentType.EntityRelationType;
import de.schosin.ecs.api.components.types.RelationComponentType.ExclusiveComponentRelationType;
import de.schosin.ecs.api.components.types.RelationComponentType.ExclusiveEntityRelationType;

class ComponentTypeTest {

    @Nested
    class FactoryMethodsTest {

        @Test
        void testComponent() {
            assertThat(component(Component.class)).as("must not be refactored to something else").isInstanceOf(ClassType.class);
        }

        @Test
        void testRelation() {
            assertThat(relation(RelationshipComponent.class, TargetComponent.class)).as("must not be refactored to something else").isInstanceOf(ComponentRelationType.class);
        }

        @Test
        void testExclusiveRelation() {
            assertThat(exclusiveRelation(ExclusiveComponent.class, TargetComponent.class)).as("must not be refactored to something else").isInstanceOf(ExclusiveComponentRelationType.class);
        }

        @Test
        void testEntityRelation() {
            assertThat(relation(RelationshipComponent.class)).as("must not be refactored to something else").isInstanceOf(EntityRelationType.class);
        }

        @Test
        void testEntityExclusiveRelation() {
            assertThat(exclusiveRelation(ExclusiveComponent.class)).as("must not be refactored to something else").isInstanceOf(ExclusiveEntityRelationType.class);
        }

        @Test
        void testComponentSet() {
            assertThat(componentSet(MyComponentSet.class, MyComponentSet.Processor.class)).as("must not be refactored to something else").isInstanceOf(ComponentSetType.class);
        }

        @Test
        void testWildcard() {
            assertThat(wildcard(ComponentInterface.class)).as("must not be refactored to something else").isInstanceOf(Wildcard.class);
        }

        @Test
        void testWildcardConstant() {
            assertThat(WILDCARD).as("must not be refactored to something else").isInstanceOf(Wildcard.class);
        }

    }

    @Nested
    class DetectTypeTest {

        @ParameterizedTest
        @MethodSource("tests")
        void testDetectComponentType(Object component, RegularComponentType<?, ?> expected) {
            assertThat(ComponentType.detectComponentType(component)).isEqualTo(expected);
        }

        static Stream<Arguments> tests() {
            return Stream.of(
                    Arguments.argumentSet("ClassType", new C1(), component(C1.class)),
                    Arguments.argumentSet("ComponentRelationType", Relation.create(new C1(), new C2(1)), relation(C1.class, C2.class)),
                    Arguments.argumentSet("ComponentRelations", Relations.of(Relation.create(new C1(), new C2(1)), Relation.create(new C1(), new C2(2))), relation(C1.class, C2.class)),
                    Arguments.argumentSet("ExclusiveComponentRelationType", Relation.create(ExclusiveComponent.A, new C2(1)), exclusiveRelation(ExclusiveComponent.class, C2.class)),
                    Arguments.argumentSet("EntityRelationType", Relation.create(new C1(), 42), relation(C1.class)),
                    Arguments.argumentSet("EntityRelations", Relations.of(Relation.create(new C1(), 42), Relation.create(new C1(), 9001)), relation(C1.class)),
                    Arguments.argumentSet("ExclusiveEntityRelationType", Relation.create(ExclusiveComponent.A, 42), exclusiveRelation(ExclusiveComponent.class)));
        }

        record C1() {
        }

        record C2(int value) {
        }

    }

}
