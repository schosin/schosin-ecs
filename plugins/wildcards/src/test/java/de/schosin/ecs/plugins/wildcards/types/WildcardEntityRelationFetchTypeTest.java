package de.schosin.ecs.plugins.wildcards.types;

import static de.schosin.ecs.api.components.types.ComponentType.component;
import static de.schosin.ecs.api.components.types.ComponentType.exclusiveRelation;
import static de.schosin.ecs.api.components.types.ComponentType.relation;
import static de.schosin.ecs.plugins.wildcards.types.WildcardType.wildcardRelation;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import de.schosin.ecs.api.components.Relation.EntityRelationship;
import de.schosin.ecs.api.components.types.ComponentType;
import de.schosin.ecs.api.components.types.ComponentType.RegularComponentType;

class WildcardEntityRelationFetchTypeTest extends AbstractWildcardTypeTest<WildcardEntityRelationFetchTypeTest.MatchesTestCases> {

    public WildcardEntityRelationFetchTypeTest() {
        super(MatchesTestCases.class);
    }

    enum MatchesTestCases implements AbstractWildcardTypeTest.MatchesTestCase {

        wildcardEntityFetch_objectWildcard(wildcardRelation(EntityRelationship.class, FETCH), component(RegularComponent.class), false),
        wildcardEntityFetch_componentRelation(wildcardRelation(EntityRelationship.class, FETCH), relation(RelationshipComponent.class, TargetComponent.class), false),
        wildcardEntityFetch_exclusiveComponentRelation(wildcardRelation(EntityRelationship.class, FETCH), exclusiveRelation(ExclusiveComponent.class, TargetComponent.class), false),
        wildcardEntityFetch_entityRelation(wildcardRelation(EntityRelationship.class, FETCH), relation(EntityRelationshipComponent.class), true),
        wildcardEntityFetch_exclusiveEntityRelation(wildcardRelation(EntityRelationship.class, FETCH), exclusiveRelation(ExclusiveEntityRelationship.class), true);

        private final WildcardEntityRelationFetchType<?, ?> type;
        private final RegularComponentType<?, ?> otherType;
        private final boolean matches;

        private MatchesTestCases(WildcardEntityRelationFetchType<?, ?> type, RegularComponentType<?, ?> otherType, boolean matches) {
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
        assertThat(wildcardRelation(relationshipBound, FETCH))
                .extracting(Object::toString, InstanceOfAssertFactories.STRING)
                .containsSubsequence("WildcardEntityRelationFetchType", relationshipBound.getSimpleName(), FETCH.toString());
    }

    @ParameterizedTest
    @ValueSource(classes = { ComponentInterface.class, AbstractComponent.class, NonFinalComponent.class })
    void testWildcard(Class<?> relationshipBound) {
        var wildcard = new WildcardEntityRelationFetchType<>(relationshipBound, FETCH);

        assertThat(wildcard.relationshipBound()).isSameAs(relationshipBound);
        assertThat(wildcard.fetch()).isSameAs(FETCH);
    }

    @ParameterizedTest
    @ValueSource(classes = { GenericComponent.class, GenericComponent.class, GenericComponentInterface.class, int[].class, Integer[].class, Object[].class })
    void testInvalidRelationshipBound(Class<?> relationshipBound) {
        assertThatThrownBy(() -> new WildcardEntityRelationFetchType<>(relationshipBound, FETCH))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContainingAll(relationshipBound.getName(), "cannot be used as a wildcard");
    }

    @ParameterizedTest
    @ValueSource(classes = { RegularComponent.class, FinalComponent.class })
    void testInvalidRelationshipBound_Final(Class<?> relationshipBound) {
        assertThatThrownBy(() -> new WildcardEntityRelationFetchType<>(relationshipBound, FETCH))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContainingAll(relationshipBound.getName(), "cannot be used as a entity relationship bound", "final");
    }

    @Test
    void testRelationshipTrait() {
        assertThatCode(() -> new WildcardEntityRelationFetchType<>(RelationshipWildcard.class, FETCH)).doesNotThrowAnyException();
    }

    @Test
    void testTargetTrait() {
        assertThatThrownBy(() -> new WildcardEntityRelationFetchType<>(TargetWildcard.class, FETCH))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContainingAll(TargetWildcard.class.getName(), "cannot be used as a relationship bound", "marked as a Target");
    }

    @Test
    void testExclusiveTrait() {
        assertThatCode(() -> new WildcardEntityRelationFetchType<>(ExclusiveWildcard.class, FETCH)).doesNotThrowAnyException();
    }

    @Test
    void testEntityRelationshipTrait() {
        assertThatCode(() -> new WildcardEntityRelationFetchType<>(EntityRelationshipWildcard.class, FETCH)).doesNotThrowAnyException();
    }

    @Test
    void testExclusiveEntityRelationshipTrait() {
        assertThatCode(() -> new WildcardEntityRelationFetchType<>(ExclusiveEntityRelationshipWildcard.class, FETCH)).doesNotThrowAnyException();
    }

}
