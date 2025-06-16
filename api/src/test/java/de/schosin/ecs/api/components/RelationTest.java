package de.schosin.ecs.api.components;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.stream.Stream;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import de.schosin.ecs.api.components.Relation.Exclusive;
import de.schosin.ecs.api.components.Relation.Relationship;
import de.schosin.ecs.api.components.Relation.Target;
import de.schosin.ecs.api.components.types.RelationComponentType.ComponentRelationType;
import de.schosin.ecs.api.components.types.RelationComponentType.EntityRelationType;
import de.schosin.ecs.api.components.types.RelationComponentType.ExclusiveComponentRelationType;
import de.schosin.ecs.api.components.types.RelationComponentType.ExclusiveEntityRelationType;

class RelationTest {

    @Nested
    class ComponentRelationTest {

        @ParameterizedTest
        @MethodSource("relations")
        void testCreate(Class<?> relationshipClass, Class<?> targetClass) {
            var relationship = relationshipClass.getEnumConstants()[0];
            var target = targetClass.getEnumConstants()[0];

            var relation = Relation.create(relationship, target);

            // Verify
            var type = relation.type();
            assertThat(type).isInstanceOf(ComponentRelationType.class);
            assertThat(type.relationship()).isSameAs(relationshipClass);
            assertThat(type.target()).isSameAs(targetClass);

            assertThat(relation.relationship()).isSameAs(relationship);
            assertThat(relation.target()).isSameAs(target);
        }

        @ParameterizedTest
        @MethodSource("invalidRelations")
        void testCreateInvalidRelation(Class<?> relationshipClass, Class<?> targetClass) {
            var relationship = relationshipClass.getEnumConstants()[0];
            var target = targetClass.getEnumConstants()[0];

            assertThatThrownBy(() -> Relation.create(relationship, target)).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void testCreateExclusive() {
            var relation = Relation.create(ExclusiveRelationshipComponent.A, Component2.A);

            // Verify
            var type = relation.type();
            assertThat(type).isInstanceOf(ExclusiveComponentRelationType.class);
            assertThat(type.relationship()).isSameAs(ExclusiveRelationshipComponent.class);
            assertThat(type.target()).isSameAs(Component2.class);

            assertThat(relation.relationship()).isSameAs(ExclusiveRelationshipComponent.A);
            assertThat(relation.target()).isSameAs(Component2.A);
        }

        @Test
        void testRelationRelationship() {
            var inner1 = Relation.create(Component1.A, Component2.A);
            var inner2 = Relation.create(Component1.A, 42);

            assertThatThrownBy(() -> Relation.create(inner1, Component2.A))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContainingAll("Cannot use Relation as relationship", inner1.toString());

            assertThatThrownBy(() -> Relation.create(inner2, Component2.A))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContainingAll("Cannot use Relation as relationship", inner2.toString());
        }

        @Test
        void testRelationsRelationship() {
            var inner1 = Relations.create(Component1.A, Component2.A);
            var inner2 = Relations.create(Component1.A, 42);

            assertThatThrownBy(() -> Relation.create(inner1, Component2.A))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContainingAll("Cannot use Relations as relationship", inner1.toString());

            assertThatThrownBy(() -> Relation.create(inner2, Component2.A))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContainingAll("Cannot use Relations as relationship", inner2.toString());
        }

        @Test
        void testRelationTarget() {
            var inner1 = Relation.create(Component1.A, Component2.A);
            var inner2 = Relation.create(Component1.A, 42);

            assertThatThrownBy(() -> Relation.create(Component2.A, inner1))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContainingAll("Cannot use Relation as target", inner1.toString());

            assertThatThrownBy(() -> Relation.create(Component2.A, inner2))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContainingAll("Cannot use Relation as target", inner2.toString());
        }

        @Test
        void testRelationsTarget() {
            var inner1 = Relations.create(Component1.A, Component2.A);
            var inner2 = Relations.create(Component1.A, 42);

            assertThatThrownBy(() -> Relation.create(Component2.A, inner1))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContainingAll("Cannot use Relations as target", inner1.toString());

            assertThatThrownBy(() -> Relation.create(Component2.A, inner2))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContainingAll("Cannot use Relations as target", inner2.toString());
        }

        @Test
        void testFree() {
            var relation = Relation.create(Component1.A, Component2.A);

            // Call
            Relation.free(relation);

            // Verify
            assertThat(relation.type()).isNull();
            assertThat(relation.relationship()).isNull();
            assertThat(relation.target()).isNull();
        }

        static Stream<Arguments> relations() {
            return Stream.of(
                    Arguments.of(Component1.class, Component2.class),
                    Arguments.of(Component1.class, TargetComponent.class),
                    Arguments.of(RelationshipComponent.class, Component2.class),
                    Arguments.of(RelationshipComponent.class, TargetComponent.class));
        }

        static Stream<Arguments> invalidRelations() {
            return Stream.of(
                    Arguments.of(TargetComponent.class, Component2.class),
                    Arguments.of(Component1.class, RelationshipComponent.class));
        }

    }

    @Nested
    class EntityRelationTest {

        @ParameterizedTest
        @MethodSource("relations")
        void testCreate(Class<?> relationshipClass) {
            var relationship = relationshipClass.getEnumConstants()[0];
            var relation = Relation.create(relationship, 42);

            // Verify
            var type = relation.type();
            assertThat(type).isInstanceOf(EntityRelationType.class);
            assertThat(type.relationship()).isSameAs(relationshipClass);

            assertThat(relation.relationship()).isSameAs(relationship);
            assertThat(relation.target()).isEqualTo(42);
        }

        @Test
        void testCreateInvalidRelation() {
            assertThatThrownBy(() -> Relation.create(TargetComponent.A, 42)).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void testCreateExclusive() {
            var relation = Relation.create(ExclusiveRelationshipComponent.A, 42);

            // Verify
            var type = relation.type();
            assertThat(type).isInstanceOf(ExclusiveEntityRelationType.class);
            assertThat(type.relationship()).isSameAs(ExclusiveRelationshipComponent.class);

            assertThat(relation.relationship()).isSameAs(ExclusiveRelationshipComponent.A);
            assertThat(relation.target()).isEqualTo(42);
        }

        @Test
        void testRelationRelationship() {
            var inner1 = Relation.create(Component1.A, Component2.A);
            var inner2 = Relation.create(Component1.A, 42);

            assertThatThrownBy(() -> Relation.create(inner1, 42))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContainingAll("Cannot use Relation as relationship", inner1.toString());

            assertThatThrownBy(() -> Relation.create(inner2, 42))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContainingAll("Cannot use Relation as relationship", inner2.toString());
        }

        @Test
        void testRelationsRelationship() {
            var inner1 = Relations.create(Component1.A, Component2.A);
            var inner2 = Relations.create(Component1.A, 42);

            assertThatThrownBy(() -> Relation.create(inner1, 42))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContainingAll("Cannot use Relations as relationship", inner1.toString());

            assertThatThrownBy(() -> Relation.create(inner2, 42))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContainingAll("Cannot use Relations as relationship", inner2.toString());
        }

        @Test
        void testFree() {
            var relation = Relation.create(Component1.A, 42);

            // Call
            Relation.free(relation);

            // Verify
            assertThat(relation.type()).isNull();
            assertThat(relation.relationship()).isNull();
            assertThat(relation.target()).isEqualTo(-1);
        }

        static Stream<Arguments> relations() {
            return Stream.of(
                    Arguments.of(Component1.class, 42),
                    Arguments.of(RelationshipComponent.class, 9001));
        }

    }

    enum Component1 {
        A, B
    }

    enum Component2 {
        A, B
    }

    enum ExclusiveRelationshipComponent implements Exclusive {
        A, B
    }

    enum RelationshipComponent implements Relationship {
        A
    }

    enum TargetComponent implements Target {
        A
    }

}
