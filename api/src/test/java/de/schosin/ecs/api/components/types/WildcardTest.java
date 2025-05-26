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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class WildcardTest extends AbstractComponentTypeTest<WildcardTest.MatchesTestCases> {

    public WildcardTest() {
        super(MatchesTestCases.class);
    }

    enum MatchesTestCases implements AbstractComponentTypeTest.MatchesTestCase {

        objectWildcard(wildcard(Object.class), component(Component.class), true),
        matchingInterface(wildcard(ComponentInterface.class), component(FinalComponent.class), true),
        mismatchingInterface(wildcard(ComponentInterface.class), component(Component.class), false),
        componentRelation(wildcard(Object.class), relation(RelationshipComponent.class, TargetComponent.class), false),
        exclusiveComponentRelation(wildcard(Object.class), exclusiveRelation(ExclusiveComponent.class, TargetComponent.class), false),
        entityRelation(wildcard(Object.class), relation(EntityRelationshipComponent.class), false),
        exclusiveEntityRelation(wildcard(Object.class), exclusiveRelation(ExclusiveEntityRelationship.class), false),
        equalWildcards(wildcard(Object.class), wildcard(Object.class), true),
        supertypeMatchesSubtype(wildcard(Object.class), wildcard(ComponentInterface.class), true),
        subtypeDoesNotMatchSupertype(wildcard(ComponentInterface.class), wildcard(Object.class), false),
        componentSet(wildcard(Object.class), componentSet(MyComponentSet.class), false),
        entityFetch(wildcard(Object.class), relation(EntityRelationshipComponent.class, FETCH), false),
        exclusiveEntityFetch(wildcard(Object.class), exclusiveRelation(ExclusiveEntityRelationship.class, FETCH), false),
        wildcardComponentRelation(wildcard(Object.class), wildcardRelation(Object.class, Object.class), false);

        private final Wildcard<?> type;
        private final ComponentType<?, ?> otherType;
        private final boolean matches;

        private MatchesTestCases(Wildcard<?> type, ComponentType<?, ?> otherType, boolean matches) {
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

    @Test
    void testToString() {
        assertThat(wildcard(ComponentInterface.class)).as("must not be refactored to something else")
                .extracting(Object::toString, InstanceOfAssertFactories.STRING)
                .containsSubsequence("Wildcard", ComponentInterface.class.getSimpleName());
    }

    @ParameterizedTest
    @ValueSource(classes = { ComponentInterface.class, AbstractComponent.class, NonFinalComponent.class, Object.class })
    void testValidWildcardTypes(Class<?> clazz) {
        assertThatCode(() -> new Wildcard<>(clazz)).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(classes = { ComponentInterface.class, AbstractComponent.class, NonFinalComponent.class })
    void testWildcardReturnsArgument(Class<?> bound) {
        var wildcard = new Wildcard<>(bound);

        assertThat(wildcard.bound()).isSameAs(bound);
    }

    @ParameterizedTest
    @ValueSource(classes = { Component.class, GenericComponent.class, GenericComponent.class, GenericComponentInterface.class, FinalComponent.class, int[].class, Integer[].class, Object[].class })
    void testInvalidWildcardType(Class<?> clazz) {
        assertThatThrownBy(() -> new Wildcard<>(clazz))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContainingAll(clazz.getName(), "cannot be used as a wildcard");
    }

    @ParameterizedTest
    @MethodSource("de.schosin.ecs.api.components.types.AbstractComponentTypeTest#unsupportedTypes")
    void testUnsupportedWildcardType(Class<?> clazz) {
        assertThatThrownBy(() -> new Wildcard<>(clazz))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContainingAll(clazz.getName(), "cannot be used as a wildcard");
    }

    @ParameterizedTest
    @ValueSource(classes = { RelationshipComponent.class, ExclusiveComponent.class, EntityRelationshipComponent.class, ExclusiveEntityRelationship.class, TargetComponent.class })
    void testInvalidTraits(Class<?> clazz) {
        assertThatThrownBy(() -> new ClassType<>(clazz))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContainingAll(clazz.getName(), "cannot be used as a class component", "It is marked as ");
    }

    @Test
    void testInvalidSyntheticWildcard() {
        assertThatThrownBy(() -> new Wildcard<>(SYNTHETIC_CLASS))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(SYNTHETIC_CLASS.getName(), "must not be synthetic");
    }

}
