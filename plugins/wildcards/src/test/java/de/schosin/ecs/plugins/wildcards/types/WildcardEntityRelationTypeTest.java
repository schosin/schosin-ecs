package de.schosin.ecs.plugins.wildcards.types;

import static de.schosin.ecs.api.components.types.ComponentType.component;
import static de.schosin.ecs.api.components.types.ComponentType.exclusiveRelation;
import static de.schosin.ecs.api.components.types.ComponentType.relation;
import static de.schosin.ecs.plugins.wildcards.types.WildcardType.wildcardRelation;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import de.schosin.ecs.api.components.types.ComponentType;
import de.schosin.ecs.api.components.types.ComponentType.RegularComponentType;

class WildcardEntityRelationTypeTest extends AbstractWildcardTypeTest<WildcardEntityRelationTypeTest.MatchesTestCases> {

    public WildcardEntityRelationTypeTest() {
        super(MatchesTestCases.class);
    }

    enum MatchesTestCases implements AbstractWildcardTypeTest.MatchesTestCase {

        wildcardEntity_objectWildcard(wildcardRelation(Object.class), component(RegularComponent.class), false),
        wildcardEntity_matchingInterface(wildcardRelation(Object.class), component(FinalComponent.class), false),
        wildcardEntity_mismatchingInterface(wildcardRelation(Object.class), component(RegularComponent.class), false),
        wildcardEntity_entityRelation(wildcardRelation(Object.class), relation(RelationshipComponent.class), true),
        wildcardEntity_entityRelation_mismatchingRelationship(wildcardRelation(RelationshipWildcard.class), relation(RelationshipComponent.class), false),
        wildcardEntity_exclusiveEntityRelation(wildcardRelation(Object.class), exclusiveRelation(ExclusiveComponent.class), true),
        wildcardEntity_exclusiveEntityRelation_mismatchingRelationship(wildcardRelation(RelationshipWildcard.class), exclusiveRelation(ExclusiveComponent.class), false),
        wildcardEntity_componentRelation(wildcardRelation(Object.class), relation(RelationshipComponent.class, TargetComponent.class), false),
        wildcardEntity_exclusiveComponentRelation(wildcardRelation(Object.class), exclusiveRelation(ExclusiveComponent.class, TargetComponent.class), false);

        private final WildcardEntityRelationType<?> type;
        private final RegularComponentType<?, ?> otherType;
        private final boolean matches;

        private MatchesTestCases(WildcardEntityRelationType<?> type, RegularComponentType<?, ?> otherType, boolean matches) {
            this.type = type;
            this.otherType = otherType;
            this.matches = matches;
        }

        @Override
        public ComponentType<?, ?> type() {
            return type;
        }

        @Override
        public RegularComponentType<?, ?> otherType() {
            return otherType;
        }

        @Override
        public boolean matches() {
            return matches;
        }

    }

    @ParameterizedTest
    @ValueSource(classes = { ComponentInterface.class, AbstractComponent.class, NonFinalComponent.class })
    void testToString(Class<?> relationshipBound) {
        assertThat(wildcardRelation(relationshipBound))
                .extracting(Object::toString, InstanceOfAssertFactories.STRING)
                .containsSubsequence("WildcardEntityRelationType", relationshipBound.getSimpleName());
    }

    @ParameterizedTest
    @ValueSource(classes = { EnumComponent.class, RegularComponent.class, FinalComponent.class })
    void testRelationshipFinal(Class<?> finalBound) {
        assertThatThrownBy(() -> new WildcardEntityRelationType<>(finalBound))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContainingAll(finalBound.getName(), "cannot be used as a entity relationship bound", "Must not be final");
    }

    @Nested
    class RelationshipTest {

        @ParameterizedTest
        @ValueSource(classes = { ComponentInterface.class, AbstractComponent.class, NonFinalComponent.class })
        void testRelationshipBound(Class<?> relationshipBound) {
            var wildcard = new WildcardEntityRelationType<>(relationshipBound);

            assertThat(wildcard.relationshipBound()).isSameAs(relationshipBound);
        }

        @ParameterizedTest
        @ValueSource(classes = { GenericComponent.class, GenericComponent.class, GenericComponentInterface.class, int.class, int[].class, Integer[].class, Object[].class })
        void testInvalidRelationshipBound(Class<?> relationshipBound) {
            assertThatThrownBy(() -> new WildcardEntityRelationType<>(relationshipBound))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContainingAll(relationshipBound.getName(), "cannot be used as a wildcard");
        }

        @Test
        void testInvalidSyntheticClass() {
            assertThatThrownBy(() -> new WildcardEntityRelationType<>(SYNTHETIC_CLASS))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContainingAll(SYNTHETIC_CLASS.getName(), "must not be synthetic");
        }

        @Test
        void testRelationshipTrait() {
            assertThatCode(() -> new WildcardEntityRelationType<>(RelationshipWildcard.class)).doesNotThrowAnyException();
        }

        @Test
        void testTargetTrait() {
            assertThatThrownBy(() -> new WildcardEntityRelationType<>(TargetWildcard.class))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContainingAll(TargetWildcard.class.getName(), "cannot be used as a relationship bound", "marked as a Target");
        }

        @Test
        void testExclusiveTrait() {
            assertThatCode(() -> new WildcardEntityRelationType<>(ExclusiveWildcard.class)).doesNotThrowAnyException();
        }

        @Test
        void testEntityRelationshipTrait() {
            assertThatCode(() -> new WildcardEntityRelationType<>(EntityRelationshipWildcard.class)).doesNotThrowAnyException();
        }

        @Test
        void testExclusiveEntityRelationshipTrait() {
            assertThatCode(() -> new WildcardEntityRelationType<>(ExclusiveEntityRelationshipWildcard.class)).doesNotThrowAnyException();
        }

    }

}
