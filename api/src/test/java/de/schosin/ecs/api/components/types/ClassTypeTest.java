package de.schosin.ecs.api.components.types;

import static de.schosin.ecs.api.components.types.ComponentType.component;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ClassTypeTest extends AbstractComponentTypeTest {

    @Nested
    class CommonClassTypeTest extends CommonComponentTest {

        @Override
        protected ComponentType<?, ?> type(Class<?> clazz) {
            return new ClassType<>(clazz);
        }

    }

    @Test
    void testToString() {
        assertThat(component(Component.class))
                .extracting(Object::toString, InstanceOfAssertFactories.STRING)
                .containsSubsequence("ClassType", Component.class.getSimpleName());
    }

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
    @ValueSource(classes = { RelationshipComponent.class, ExclusiveComponent.class, EntityRelationshipComponent.class, ExclusiveEntityRelationship.class, TargetComponent.class })
    void testInvalidTraits(Class<?> clazz) {
        assertThatThrownBy(() -> new ClassType<>(clazz))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContainingAll(clazz.getName(), "cannot be used as a class component", "It is marked as ");
    }

}
