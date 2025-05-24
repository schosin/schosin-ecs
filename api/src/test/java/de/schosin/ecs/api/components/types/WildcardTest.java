package de.schosin.ecs.api.components.types;

import static de.schosin.ecs.api.components.types.ComponentType.wildcard;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class WildcardTest extends AbstractComponentTypeTest {

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
