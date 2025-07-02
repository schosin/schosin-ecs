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

class WildcardComponentRelationTypeTest extends AbstractWildcardTypeTest<WildcardComponentRelationTypeTest.MatchesTestCases> {

    public WildcardComponentRelationTypeTest() {
        super(MatchesTestCases.class);
    }

    enum MatchesTestCases implements AbstractWildcardTypeTest.MatchesTestCase {

        wildcardComponent_objectWildcard(wildcardRelation(Object.class, Object.class), component(RegularComponent.class), false),
        wildcardComponent_matchingInterface(wildcardRelation(Object.class, Object.class), component(FinalComponent.class), false),
        wildcardComponent_mismatchingInterface(wildcardRelation(Object.class, Object.class), component(RegularComponent.class), false),
        wildcardComponent_componentRelation(wildcardRelation(Object.class, Object.class), relation(RelationshipComponent.class, TargetComponent.class), true),
        wildcardComponent_componentRelation_mismatchingRelationship(wildcardRelation(RelationshipWildcard.class, Object.class), relation(RelationshipComponent.class, TargetComponent.class), false),
        wildcardComponent_componentRelation_mismatchingTarget(wildcardRelation(Object.class, TargetWildcard.class), relation(RelationshipComponent.class, TargetComponent.class), false),
        wildcardComponent_exclusiveComponentRelation(wildcardRelation(Object.class, Object.class), exclusiveRelation(ExclusiveComponent.class, TargetComponent.class), true),
        wildcardComponent_exclusiveComponentRelation_mismatchingRelationship(wildcardRelation(RelationshipWildcard.class, Object.class),
                exclusiveRelation(ExclusiveComponent.class, TargetComponent.class), false),
        wildcardComponent_exclusiveComponentRelation_mismatchingTarget(wildcardRelation(Object.class, TargetWildcard.class), exclusiveRelation(ExclusiveComponent.class, TargetComponent.class), false),
        wildcardComponent_entityRelation(wildcardRelation(Object.class, Object.class), relation(EntityRelationshipComponent.class), false),
        wildcardComponent_exclusiveEntityRelation(wildcardRelation(Object.class, Object.class), exclusiveRelation(ExclusiveEntityRelationship.class), false);

        private final WildcardComponentRelationType<?, ?> type;
        private final RegularComponentType<?, ?> otherType;
        private final boolean matches;

        private MatchesTestCases(WildcardComponentRelationType<?, ?> type, RegularComponentType<?, ?> otherType, boolean matches) {
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
        assertThat(wildcardRelation(relationshipBound, ComponentInterface.class))
                .extracting(Object::toString, InstanceOfAssertFactories.STRING)
                .containsSubsequence("WildcardComponentRelationType", relationshipBound.getSimpleName(), ComponentInterface.class.getSimpleName());
    }

    @ParameterizedTest
    @ValueSource(classes = { EnumComponent.class, RegularComponent.class, FinalComponent.class })
    void testRelationshipFinal(Class<?> finalBound) {
        assertThatCode(() -> new WildcardComponentRelationType<>(finalBound, ComponentInterface.class)).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(classes = { EnumComponent.class, RegularComponent.class, FinalComponent.class })
    void testTargetFinal(Class<?> finalBound) {
        assertThatCode(() -> new WildcardComponentRelationType<>(ComponentInterface.class, finalBound)).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(classes = { EnumComponent.class, RegularComponent.class, FinalComponent.class })
    void testBothFinal(Class<?> finalBound) {
        assertThatThrownBy(() -> new WildcardComponentRelationType<>(finalBound, RegularComponent.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContainingAll(finalBound.getName(), RegularComponent.class.getName(), "cannot be used as a component wildcard bounds", "At one most may be final");
    }

    @Nested
    class RelationshipTest {

        @ParameterizedTest
        @ValueSource(classes = { ComponentInterface.class, AbstractComponent.class, NonFinalComponent.class })
        void testRelationshipBound(Class<?> relationshipBound) {
            var wildcard = new WildcardComponentRelationType<>(relationshipBound, ComponentInterface.class);

            assertThat(wildcard.relationshipBound()).isSameAs(relationshipBound);
            assertThat(wildcard.targetBound()).isSameAs(ComponentInterface.class);
        }

        @ParameterizedTest
        @ValueSource(classes = { GenericComponent.class, GenericComponent.class, GenericComponentInterface.class, int.class, int[].class, Integer[].class, Object[].class })
        void testInvalidRelationshipBound(Class<?> relationshipBound) {
            assertThatThrownBy(() -> new WildcardComponentRelationType<>(relationshipBound, ComponentInterface.class))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContainingAll(relationshipBound.getName(), "cannot be used as a wildcard");
        }

        @Test
        void testInvalidSyntheticClass() {
            assertThatThrownBy(() -> new WildcardComponentRelationType<>(SYNTHETIC_CLASS, ComponentInterface.class))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContainingAll(SYNTHETIC_CLASS.getName(), "must not be synthetic");
        }

        @Test
        void testRelationshipTrait() {
            assertThatCode(() -> new WildcardComponentRelationType<>(RelationshipWildcard.class, ComponentInterface.class)).doesNotThrowAnyException();
        }

        @Test
        void testTargetTrait() {
            assertThatThrownBy(() -> new WildcardComponentRelationType<>(TargetWildcard.class, ComponentInterface.class))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContainingAll(TargetWildcard.class.getName(), "cannot be used as a relationship bound", "marked as a Target");
        }

        @Test
        void testExclusiveTrait() {
            assertThatCode(() -> new WildcardComponentRelationType<>(ExclusiveWildcard.class, ComponentInterface.class)).doesNotThrowAnyException();
        }

        @Test
        void testEntityRelationshipTrait() {
            assertThatThrownBy(() -> new WildcardComponentRelationType<>(EntityRelationshipWildcard.class, ComponentInterface.class))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContainingAll(EntityRelationshipWildcard.class.getName(), "cannot be used for a component relation", "marked as a entity relationship component");
        }

        @Test
        void testExclusiveEntityRelationshipTrait() {
            assertThatThrownBy(() -> new WildcardComponentRelationType<>(ExclusiveEntityRelationshipWildcard.class, ComponentInterface.class))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContainingAll(ExclusiveEntityRelationshipWildcard.class.getName(), "cannot be used for a component relation", "marked as a entity relationship component");
        }

    }

    @Nested
    class TargetTest {

        @ParameterizedTest
        @ValueSource(classes = { ComponentInterface.class, AbstractComponent.class, NonFinalComponent.class })
        void testTargetBound(Class<?> relationshipBound) {
            var wildcard = new WildcardComponentRelationType<>(ComponentInterface.class, relationshipBound);

            assertThat(wildcard.relationshipBound()).isSameAs(ComponentInterface.class);
            assertThat(wildcard.targetBound()).isSameAs(relationshipBound);
        }

        @ParameterizedTest
        @ValueSource(classes = { GenericComponent.class, GenericComponent.class, GenericComponentInterface.class, int.class, int[].class, Integer[].class, Object[].class })
        void testInvalidTargetBound(Class<?> relationshipBound) {
            assertThatThrownBy(() -> new WildcardComponentRelationType<>(ComponentInterface.class, relationshipBound))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContainingAll(relationshipBound.getName(), "cannot be used as a wildcard");
        }

        @Test
        void testInvalidSyntheticClass() {
            assertThatThrownBy(() -> new WildcardComponentRelationType<>(ComponentInterface.class, SYNTHETIC_CLASS))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContainingAll(SYNTHETIC_CLASS.getName(), "must not be synthetic");
        }

        @Test
        void testTargetTrait() {
            assertThatCode(() -> new WildcardComponentRelationType<>(ComponentInterface.class, TargetWildcard.class)).doesNotThrowAnyException();
        }

        @Test
        void testExclusiveTrait() {
            assertThatCode(() -> new WildcardComponentRelationType<>(ComponentInterface.class, ExclusiveWildcard.class))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContainingAll(ExclusiveWildcard.class.getName(), "cannot be used as a target bound", "marked as a Relationship");
        }

        @Test
        void testEntityRelationshipTrait() {
            assertThatCode(() -> new WildcardComponentRelationType<>(ComponentInterface.class, EntityRelationshipWildcard.class)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContainingAll(EntityRelationshipWildcard.class.getName(), "cannot be used as a target bound", "marked as a Relationship");
        }

        @Test
        void testExclusiveEntityRelationshipTrait() {
            assertThatCode(() -> new WildcardComponentRelationType<>(ComponentInterface.class, ExclusiveEntityRelationshipWildcard.class)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContainingAll(ExclusiveEntityRelationshipWildcard.class.getName(), "cannot be used as a target bound", "marked as a Relationship");
        }

    }

}
