package de.schosin.ecs.api.components.types;

import static de.schosin.ecs.api.components.types.ComponentType.component;
import static de.schosin.ecs.api.components.types.ComponentType.componentSet;
import static de.schosin.ecs.api.components.types.ComponentType.exclusiveRelation;
import static de.schosin.ecs.api.components.types.ComponentType.relation;
import static de.schosin.ecs.api.components.types.ComponentType.wildcard;
import static de.schosin.ecs.api.components.types.ComponentType.wildcardRelation;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import de.schosin.ecs.api.components.types.RelationFetchType.EntityRelationFetchType;
import de.schosin.ecs.api.components.types.RelationFetchType.ExclusiveEntityRelationFetchType;

class RelationFetchTypeTest extends AbstractComponentTypeTest<RelationFetchTypeTest.MatchesTestCases> {

    public RelationFetchTypeTest() {
        super(MatchesTestCases.class);
    }

    enum MatchesTestCases implements AbstractComponentTypeTest.MatchesTestCase {

        classType(relation(EntityRelationshipComponent.class, FETCH), component(Component.class), false),
        componentRelation(relation(EntityRelationshipComponent.class, FETCH), relation(RelationshipComponent.class, TargetComponent.class), false),
        exclusiveComponentRelation(relation(EntityRelationshipComponent.class, FETCH), exclusiveRelation(ExclusiveComponent.class, TargetComponent.class), false),
        entityRelation(relation(EntityRelationshipComponent.class, FETCH), relation(EntityRelationshipComponent.class), false),
        exclusiveEntityRelation(relation(EntityRelationshipComponent.class, FETCH), exclusiveRelation(ExclusiveEntityRelationship.class), false),
        wildcardObject(relation(EntityRelationshipComponent.class, FETCH), wildcard(Object.class), false),
        componentSet(relation(EntityRelationshipComponent.class, FETCH), componentSet(MyComponentSet.class, MyComponentSet.Processor.class), false),
        equalEntityFetch(relation(EntityRelationshipComponent.class, FETCH), relation(EntityRelationshipComponent.class, FETCH), true),
        equalExclusiveEntityFetch(relation(EntityRelationshipComponent.class, FETCH), exclusiveRelation(ExclusiveEntityRelationship.class, FETCH), false),
        wildcardComponentRelation(relation(EntityRelationshipComponent.class, FETCH), wildcardRelation(Object.class, Object.class), false),
        wildcardEntityRelation(relation(EntityRelationshipComponent.class, FETCH), wildcardRelation(Object.class), false),
        wildcardEntityFetchRelation(relation(EntityRelationshipComponent.class, FETCH), wildcardRelation(Object.class, component(Component.class)), false),

        exclusive_classType(exclusiveRelation(ExclusiveEntityRelationship.class, FETCH), component(Component.class), false),
        exclusive_componentRelation(exclusiveRelation(ExclusiveEntityRelationship.class, FETCH), relation(RelationshipComponent.class, TargetComponent.class), false),
        exclusive_exclusiveComponentRelation(exclusiveRelation(ExclusiveEntityRelationship.class, FETCH), exclusiveRelation(ExclusiveComponent.class, TargetComponent.class), false),
        exclusive_entityRelation(exclusiveRelation(ExclusiveEntityRelationship.class, FETCH), relation(EntityRelationshipComponent.class), false),
        exclusive_exclusiveEntityRelation(exclusiveRelation(ExclusiveEntityRelationship.class, FETCH), exclusiveRelation(ExclusiveEntityRelationship.class), false),
        exclusive_wildcardObject(exclusiveRelation(ExclusiveEntityRelationship.class, FETCH), wildcard(Object.class), false),
        exclusive_componentSet(exclusiveRelation(ExclusiveEntityRelationship.class, FETCH), componentSet(MyComponentSet.class, MyComponentSet.Processor.class), false),
        exclusive_equalEntityFetch(exclusiveRelation(ExclusiveEntityRelationship.class, FETCH), relation(EntityRelationshipComponent.class, FETCH), false),
        exclusive_equalExclusiveEntityFetch(exclusiveRelation(ExclusiveEntityRelationship.class, FETCH), exclusiveRelation(ExclusiveEntityRelationship.class, FETCH), true),
        exclusive_wildcardComponentRelation(exclusiveRelation(ExclusiveEntityRelationship.class, FETCH), wildcardRelation(Object.class, Object.class), false),
        exclusive_wildcardEntityRelation(exclusiveRelation(ExclusiveEntityRelationship.class, FETCH), wildcardRelation(Object.class), false),
        exclusive_wildcardEntityFetchRelation(exclusiveRelation(ExclusiveEntityRelationship.class, FETCH), wildcardRelation(Object.class, component(Component.class)), false);

        private final RelationFetchType<?, ?, ?> type;
        private final ComponentType<?, ?> otherType;
        private final boolean matches;

        private MatchesTestCases(RelationFetchType<?, ?, ?> type, ComponentType<?, ?> otherType, boolean matches) {
            this.type = type;
            this.otherType = otherType;
            this.matches = matches;
        }

        @Override
        public ComponentType<?, ?> type() {
            return type;
        }

        @Override
        public ComponentType<?, ?> otherType() {
            return otherType;
        }

        @Override
        public boolean matches() {
            return matches;
        }

    }

    @Nested
    class EntityRelationFetchTypeTest extends CommonComponentTest {

        @Override
        protected ComponentType<?, ?> type(Class<?> clazz) {
            return new EntityRelationFetchType<>(clazz, FETCH);
        }

        @Test
        void testToString() {
            assertThat(relation(RelationshipComponent.class, FETCH))
                    .extracting(Object::toString, InstanceOfAssertFactories.STRING)
                    .containsSubsequence("EntityRelationFetchType", RelationshipComponent.class.getSimpleName(), FETCH.toString());
        }

        @Test
        void testRelationshipTrait_DoesNotThrow() {
            assertThatCode(() -> new EntityRelationFetchType<>(RelationshipComponent.class, FETCH)).doesNotThrowAnyException();
        }

        @Test
        void testExclusiveTrait() {
            assertThatThrownBy(() -> new EntityRelationFetchType<>(ExclusiveComponent.class, FETCH))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContainingAll(ExclusiveComponent.class.getName(), "cannot be used as a non-exclusive relationship component", "marked as a Exclusive");
        }

        @Test
        void testTargetTrait() {
            assertThatThrownBy(() -> new EntityRelationFetchType<>(TargetComponent.class, FETCH))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContainingAll(TargetComponent.class.getName(), "cannot be used as a relationship component", "marked as a Target");
        }

        @Test
        void testEntityRelationshipTrait_DoesNotThrow() {
            assertThatCode(() -> new EntityRelationFetchType<>(EntityRelationshipComponent.class, FETCH)).doesNotThrowAnyException();
        }

        @Test
        void testExclusiveEntityRelationshipTrait() {
            assertThatThrownBy(() -> new EntityRelationFetchType<>(ExclusiveEntityRelationship.class, FETCH))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContainingAll(ExclusiveEntityRelationship.class.getName(), "cannot be used as a non-exclusive relationship component", "marked as a Exclusive");
        }

    }

    @Nested
    class ExclusiveEntityRelationFetchTypeTest {

        @Test
        void testToString() {
            assertThat(exclusiveRelation(ExclusiveComponent.class, FETCH))
                    .extracting(Object::toString, InstanceOfAssertFactories.STRING)
                    .containsSubsequence("ExclusiveEntityRelationFetchType", ExclusiveComponent.class.getSimpleName(), FETCH.toString());
        }

        @Test
        void testExclusiveTrait_DoesNotThrow() {
            assertThatCode(() -> new ExclusiveEntityRelationFetchType<>(ExclusiveComponent.class, FETCH)).doesNotThrowAnyException();
        }

        @Test
        void testExclusiveEntityRelationshipTrait() {
            assertThatCode(() -> new ExclusiveEntityRelationFetchType<>(ExclusiveEntityRelationship.class, FETCH)).doesNotThrowAnyException();
        }

    }

}
