package de.schosin.ecs.plugins.wildcards.types;

import static de.schosin.ecs.api.components.types.ComponentType.component;
import static de.schosin.ecs.api.components.types.ComponentType.exclusiveRelation;
import static de.schosin.ecs.api.components.types.ComponentType.relation;
import static de.schosin.ecs.plugins.wildcards.types.WildcardType.WILDCARD;
import static de.schosin.ecs.plugins.wildcards.types.WildcardType.wildcard;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import de.schosin.ecs.api.components.types.ClassType;
import de.schosin.ecs.api.components.types.ComponentType;
import de.schosin.ecs.api.components.types.ComponentType.RegularComponentType;

class WildcardClassTypeTest extends AbstractWildcardTypeTest<WildcardClassTypeTest.MatchesTestCases> {

    public WildcardClassTypeTest() {
        super(MatchesTestCases.class);
    }

    enum MatchesTestCases implements AbstractWildcardTypeTest.MatchesTestCase {

        wildcardObject_component(WILDCARD, component(RegularComponent.class), true),
        wildcardObject_otherComponent(WILDCARD, component(FinalComponent.class), true),
        wildcardObject_componentRelation(WILDCARD, relation(RelationshipComponent.class, TargetComponent.class), false),
        wildcardObject_exclusiveComponentRelation(WILDCARD, exclusiveRelation(ExclusiveComponent.class, TargetComponent.class), false),
        wildcardObject_exclusiveEntityRelation(WILDCARD, exclusiveRelation(ExclusiveComponent.class, TargetComponent.class), false),
        wildcardObject_entityRelation(WILDCARD, relation(EntityRelationshipComponent.class), false),

        wildcardInterface_matchingInterface(wildcard(ComponentInterface.class), component(FinalComponent.class), true),
        wildcardInterface_mismatchingInterface(wildcard(ComponentInterface.class), component(RegularComponent.class), false),
        wildcardInterface_componentRelation(wildcard(ComponentInterface.class), relation(RelationshipComponent.class, TargetComponent.class), false),
        wildcardInterface_exclusiveComponentRelation(wildcard(ComponentInterface.class), exclusiveRelation(ExclusiveComponent.class, TargetComponent.class), false),
        wildcardInterface_exclusiveEntityRelation(wildcard(ComponentInterface.class), exclusiveRelation(ExclusiveComponent.class), false),
        wildcardInterface_entityRelation(wildcard(ComponentInterface.class), relation(EntityRelationshipComponent.class), false);

        private final WildcardClassType<?> type;
        private final RegularComponentType<?, ?> otherType;
        private final boolean matches;

        private MatchesTestCases(WildcardClassType<?> type, RegularComponentType<?, ?> otherType, boolean matches) {
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
    void testToString(Class<?> wildcard) {
        assertThat(wildcard(wildcard))
                .extracting(Object::toString, InstanceOfAssertFactories.STRING)
                .containsSubsequence("WildcardClassType", wildcard.getSimpleName());
    }

    @ParameterizedTest
    @ValueSource(classes = { ComponentInterface.class, AbstractComponent.class, NonFinalComponent.class, Object.class })
    void testValidWildcardTypes(Class<?> clazz) {
        assertThatCode(() -> wildcard(clazz)).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(classes = { ComponentInterface.class, AbstractComponent.class, NonFinalComponent.class })
    void testWildcardReturnsArgument(Class<?> bound) {
        var wildcard = wildcard(bound);

        assertThat(wildcard.bound()).isSameAs(bound);
    }

    @ParameterizedTest
    @ValueSource(classes = {
            RegularComponent.class, GenericComponent.class, GenericComponent.class, GenericComponentInterface.class, FinalComponent.class,
            int[].class, Integer[].class, Object[].class
    })
    void testInvalidWildcardType(Class<?> clazz) {
        assertThatThrownBy(() -> wildcard(clazz))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContainingAll(clazz.getName(), "cannot be used as a wildcard");
    }

    @ParameterizedTest
    @MethodSource("de.schosin.ecs.plugins.wildcards.types.AbstractWildcardTypeTest#unsupportedTypes")
    void testUnsupportedWildcardType(Class<?> clazz) {
        assertThatThrownBy(() -> wildcard(clazz))
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
        assertThatThrownBy(() -> wildcard(SYNTHETIC_CLASS))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(SYNTHETIC_CLASS.getName(), "must not be synthetic");
    }

}
