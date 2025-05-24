package de.schosin.ecs.api.components;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import de.schosin.ecs.api.components.Relation.Exclusive;
import de.schosin.ecs.api.components.types.RelationComponentType.ComponentRelationType;
import de.schosin.ecs.api.components.types.RelationComponentType.EntityRelationType;
import de.schosin.ecs.api.components.types.RelationComponentType.ExclusiveComponentRelationType;
import de.schosin.ecs.api.components.types.RelationComponentType.ExclusiveEntityRelationType;

class RelationTest {

    @Nested
    class ComponentRelationTest {

        @Test
        void testCreate() {
            var relation = Relation.create(RelationshipComponent.A, TargetComponent.A);

            // Verify
            var type = relation.type();
            assertThat(type).isInstanceOf(ComponentRelationType.class);
            assertThat(type.relationship()).isSameAs(RelationshipComponent.class);
            assertThat(type.target()).isSameAs(TargetComponent.class);

            assertThat(relation.relationship()).isSameAs(RelationshipComponent.A);
            assertThat(relation.target()).isSameAs(TargetComponent.A);
        }

        @Test
        void testCreateExclusive() {
            var relation = Relation.create(ExclusiveRelationshipComponent.A, TargetComponent.A);

            // Verify
            var type = relation.type();
            assertThat(type).isInstanceOf(ExclusiveComponentRelationType.class);
            assertThat(type.relationship()).isSameAs(ExclusiveRelationshipComponent.class);
            assertThat(type.target()).isSameAs(TargetComponent.class);

            assertThat(relation.relationship()).isSameAs(ExclusiveRelationshipComponent.A);
            assertThat(relation.target()).isSameAs(TargetComponent.A);
        }

        @Test
        void testFree() {
            var relation = Relation.create(RelationshipComponent.A, TargetComponent.A);

            // Call
            Relation.free(relation);

            // Verify
            assertThat(relation.type()).isNull();
            assertThat(relation.relationship()).isNull();
            assertThat(relation.target()).isNull();
        }

    }

    @Nested
    class EntityRelationTest {

        @Test
        void testCreate() {
            var relation = Relation.create(RelationshipComponent.A, 42);

            // Verify
            var type = relation.type();
            assertThat(type).isInstanceOf(EntityRelationType.class);
            assertThat(type.relationship()).isSameAs(RelationshipComponent.class);

            assertThat(relation.relationship()).isSameAs(RelationshipComponent.A);
            assertThat(relation.target()).isEqualTo(42);
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
        void testFree() {
            var relation = Relation.create(RelationshipComponent.A, 42);

            // Call
            Relation.free(relation);

            // Verify
            assertThat(relation.type()).isNull();
            assertThat(relation.relationship()).isNull();
            assertThat(relation.target()).isEqualTo(-1);
        }

    }

    enum RelationshipComponent {
        A, B
    }

    enum ExclusiveRelationshipComponent implements Exclusive {
        A, B
    }

    enum TargetComponent {
        A, B
    }

}
