package de.schosin.ecs.api.components;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import de.schosin.ecs.api.components.Relation.ComponentRelation;
import de.schosin.ecs.api.components.Relation.EntityRelation;
import de.schosin.ecs.api.components.Relation.Exclusive;

class RelationsTest {

    @Nested
    class ComponentRelationsTest {
        @Test
        void testCreate() {
            var relation1 = Relation.create(new C1(11), new C2(21));
            var relation2 = Relation.create(new C1(12), new C2(22));

            // Call
            var relations = Relations.create(relation1, relation2);

            // Verify
            assertThat(relations).containsExactly(relation1, relation2);
            assertThat(relations.size()).isEqualTo(2);
            assertThat(relations.get(0)).isSameAs(relation1);
            assertThat(relations.get(1)).isSameAs(relation2);
        }

        @Test
        void testEclusiveRelation() {
            var relation1 = Relation.create(E1.A, new C2(21));
            var relation2 = Relation.create(E1.B, new C2(22));

            assertThatThrownBy(() -> Relations.create(relation1, relation2))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContainingAll(E1.class.getSimpleName(), "exclusive relationship");
        }

        @Test
        void testCreateEmpty() {
            assertThatThrownBy(() -> Relations.<ComponentRelation<C1, C2>>create())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("must not be empty");
        }

        @Test
        void testFree() {
            var relation1 = Relation.create(new C1(11), new C2(21));
            var relation2 = Relation.create(new C1(12), new C2(22));

            var relations = Relations.create(relation1, relation2);

            // Call
            Relations.free(relations);

            // Verify
            assertThat(relations).isEmpty();
            assertThat(relations.size()).isEqualTo(0);

            assertThat(Relations.create(Relation.create(new C1(13), new C2(23)))).isSameAs(relations);

            assertThat(relation1).as("relation not reset").extracting("relationship.value", "target.value").contains(11, 21);
            assertThat(relation2).as("relation not reset").extracting("relationship.value", "target.value").contains(12, 22);
        }

    }

    @Nested
    class EntityRelationsTest {

        @Test
        void testCreate() {
            var relation1 = Relation.create(new C1(11), 42);
            var relation2 = Relation.create(new C1(12), 9001);

            // Call
            var relations = Relations.create(relation1, relation2);

            // Verify
            assertThat(relations).containsExactly(relation1, relation2);
            assertThat(relations.size()).isEqualTo(2);
            assertThat(relations.get(0)).isSameAs(relation1);
            assertThat(relations.get(1)).isSameAs(relation2);
        }

        @Test
        void testEclusiveRelation() {
            var relation1 = Relation.create(E1.A, 42);
            var relation2 = Relation.create(E1.B, 9001);

            assertThatThrownBy(() -> Relations.create(relation1, relation2))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContainingAll(E1.class.getSimpleName(), "exclusive relationship");
        }

        @Test
        void testCreateEmpty() {
            assertThatThrownBy(() -> Relations.<EntityRelation<C1>>create())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("must not be empty");
        }

        @Test
        void testFree() {
            var relation1 = Relation.create(new C1(11), 42);
            var relation2 = Relation.create(new C1(12), 9001);

            var relations = Relations.create(relation1, relation2);

            // Call
            Relations.free(relations);

            // Verify
            assertThat(relations).isEmpty();
            assertThat(relations.size()).isEqualTo(0);

            assertThat(Relations.create(Relation.create(new C1(13), 1337))).isSameAs(relations);

            assertThat(relation1).as("relation not reset").extracting("relationship.value", "target").contains(11, 42);
            assertThat(relation2).as("relation not reset").extracting("relationship.value", "target").contains(12, 9001);
        }

    }

    record C1(int value) {
    }

    record C2(int value) {
    }

    enum E1 implements Exclusive {
        A, B
    }

}
