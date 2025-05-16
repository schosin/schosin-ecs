package de.schosin.ecs.api.components;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.stream.Stream;

import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import de.schosin.ecs.api.components.ComponentType.ClassType;
import de.schosin.ecs.api.components.ComponentType.Wildcard;

class ComponentTypeTest {

    private static final Class<?> SYNTHETIC_CLASS = ((Runnable) () -> {
    }).getClass();

    @Nested
    class ClassTypeTest {

        @ParameterizedTest
        @ValueSource(classes = { Component.class, NonFinalComponent.class, FinalComponent.class })
        void testValidClassTypes(Class<?> clazz) {
            assertThatCode(() -> new ClassType<>(clazz)).doesNotThrowAnyException();
        }

        @ParameterizedTest
        @ValueSource(classes = { Component.class, NonFinalComponent.class, FinalComponent.class })
        void testClassTypeReturnsArgument(Class<?> clazz) {
            var classType = new ClassType<>(clazz);

            assertThat(classType.clazz()).isSameAs(clazz);
        }

        @ParameterizedTest
        @ValueSource(classes = { GenericComponent.class, ComponentInterface.class, AbstractComponent.class })
        void testInvalidClassType(Class<?> clazz) {
            assertThatThrownBy(() -> new ClassType<>(clazz))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContainingAll(clazz.getName(), "cannot be used as a component");
        }

        @ParameterizedTest
        @MethodSource("de.schosin.ecs.api.components.ComponentTypeTest#unsupportedTypes")
        void testUnsupportedClassType(Class<?> clazz) {
            assertThatThrownBy(() -> new ClassType<>(clazz))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContainingAll(clazz.getName(), "cannot be used as a component");
        }

        @Test
        void testInvalidSyntheticClass() {
            assertThatThrownBy(() -> new ClassType<>(SYNTHETIC_CLASS))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContainingAll(SYNTHETIC_CLASS.getName(), "must not be synthetic");
        }

    }

    @Nested
    class WildcardTest {

        @ParameterizedTest
        @ValueSource(classes = { ComponentInterface.class, AbstractComponent.class, NonFinalComponent.class })
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
        @ValueSource(classes = { Component.class, GenericComponent.class, GenericComponent.class, GenericComponentInterface.class, FinalComponent.class })
        void testInvalidWildcardType(Class<?> clazz) {
            assertThatThrownBy(() -> new Wildcard<>(clazz))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContainingAll(clazz.getName(), "cannot be used as a wildcard");
        }

        @ParameterizedTest
        @MethodSource("de.schosin.ecs.api.components.ComponentTypeTest#unsupportedTypes")
        void testUnsupportedWildcardType(Class<?> clazz) {
            assertThatThrownBy(() -> new Wildcard<>(clazz))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContainingAll(clazz.getName(), "cannot be used as a wildcard");
        }

        @Test
        void testInvalidSyntheticWildcard() {
            assertThatThrownBy(() -> new Wildcard<>(SYNTHETIC_CLASS))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(SYNTHETIC_CLASS.getName(), "must not be synthetic");
        }

    }

    static Stream<Arguments> unsupportedTypes() {
        return ComponentTypeHelper.UNSUPPORTED_TYPES.stream()
                .map(clazz -> Arguments.of(Named.of(clazz.getSimpleName(), clazz)));
    }

    record Component(String value) {
    }

    record GenericComponent<T>(T value) {
    }

    interface ComponentInterface {
    }

    @SuppressWarnings("unused")
    interface GenericComponentInterface<T> {
    }

    abstract class AbstractComponent {
    }

    class NonFinalComponent {
    }

    final class FinalComponent {
    }

}
