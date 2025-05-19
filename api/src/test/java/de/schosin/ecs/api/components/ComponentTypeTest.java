package de.schosin.ecs.api.components;

import static de.schosin.ecs.api.components.ComponentType.WILDCARD;
import static de.schosin.ecs.api.components.ComponentType.component;
import static de.schosin.ecs.api.components.ComponentType.exclusiveRelation;
import static de.schosin.ecs.api.components.ComponentType.relation;
import static de.schosin.ecs.api.components.ComponentType.wildcard;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.stream.Stream;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import de.schosin.ecs.api.components.ComponentType.ClassType;
import de.schosin.ecs.api.components.ComponentType.ComponentRelationType;
import de.schosin.ecs.api.components.ComponentType.EntityRelationType;
import de.schosin.ecs.api.components.ComponentType.ExclusiveComponentRelationType;
import de.schosin.ecs.api.components.ComponentType.ExclusiveEntityRelationType;
import de.schosin.ecs.api.components.ComponentType.Wildcard;

class ComponentTypeTest {

    private static final Class<?> SYNTHETIC_CLASS = ((Runnable) () -> {
    }).getClass();

    @Nested
    class FactoryMethodsTest {

        @Test
        void testComponent() {
            assertThat(component(Component.class)).as("must not be refactored to something else").isInstanceOf(ClassType.class);
        }

        @Test
        void testRelation() {
            assertThat(relation(RelationshipComponent.class, TargetComponent.class)).as("must not be refactored to something else").isInstanceOf(ComponentRelationType.class);
        }

        @Test
        void testExclusiveRelation() {
            assertThat(exclusiveRelation(ExclusiveComponent.class, TargetComponent.class)).as("must not be refactored to something else").isInstanceOf(ExclusiveComponentRelationType.class);
        }

        @Test
        void testEntityRelation() {
            assertThat(relation(RelationshipComponent.class)).as("must not be refactored to something else").isInstanceOf(EntityRelationType.class);
        }

        @Test
        void testEntityExclusiveRelation() {
            assertThat(exclusiveRelation(ExclusiveComponent.class)).as("must not be refactored to something else").isInstanceOf(ExclusiveEntityRelationType.class);
        }

        @Test
        void testWildcard() {
            assertThat(wildcard(ComponentInterface.class)).as("must not be refactored to something else").isInstanceOf(Wildcard.class);
        }

        @Test
        void testWildcardConstant() {
            assertThat(WILDCARD).as("must not be refactored to something else").isInstanceOf(Wildcard.class);
        }

    }

    @Nested
    class ToStringTest {

        @Test
        void testComponent() {
            assertThat(component(Component.class))
                    .extracting(Object::toString, InstanceOfAssertFactories.STRING)
                    .containsSubsequence("ClassType", Component.class.getSimpleName());
        }

        @Test
        void testRelation() {
            assertThat(relation(RelationshipComponent.class, TargetComponent.class))
                    .extracting(Object::toString, InstanceOfAssertFactories.STRING)
                    .containsSubsequence("ComponentRelationType", RelationshipComponent.class.getSimpleName(), TargetComponent.class.getSimpleName());
        }

        @Test
        void testExclusiveRelation() {
            assertThat(exclusiveRelation(ExclusiveComponent.class, TargetComponent.class))
                    .extracting(Object::toString, InstanceOfAssertFactories.STRING)
                    .containsSubsequence("ExclusiveComponentRelationType", ExclusiveComponent.class.getSimpleName(), TargetComponent.class.getSimpleName());
        }

        @Test
        void testEntityRelation() {
            assertThat(relation(RelationshipComponent.class))
                    .extracting(Object::toString, InstanceOfAssertFactories.STRING)
                    .containsSubsequence("EntityRelationType", RelationshipComponent.class.getSimpleName());
        }

        @Test
        void testExclusiveEntityRelation() {
            assertThat(exclusiveRelation(ExclusiveComponent.class))
                    .extracting(Object::toString, InstanceOfAssertFactories.STRING)
                    .containsSubsequence("ExclusiveEntityRelationType", ExclusiveComponent.class.getSimpleName());
        }

        @Test
        void testWildcard() {
            assertThat(wildcard(ComponentInterface.class)).as("must not be refactored to something else")
                    .extracting(Object::toString, InstanceOfAssertFactories.STRING)
                    .containsSubsequence("Wildcard", ComponentInterface.class.getSimpleName());
        }

    }

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
        @ValueSource(classes = { GenericComponent.class, ComponentInterface.class, AbstractComponent.class, Object.class, int[].class, Integer[].class, Object[].class })
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
    class ComponentRelationTypeTest {

        @Nested
        class RelationshipComponentTest {

            @Test
            void testRelationshipTrait_DoesNotThrow() {
                assertThatCode(() -> new ComponentRelationType<>(RelationshipComponent.class, EnumComponent.class)).doesNotThrowAnyException();
            }

            @Test
            void testExclusiveTrait_DoesNotThrow() {
                assertThatThrownBy(() -> new ComponentRelationType<>(ExclusiveComponent.class, EnumComponent.class))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContainingAll(ExclusiveComponent.class.getName(), "cannot be used as a non-exclusive relationship component", "marked as a Exclusive");
            }

            @Test
            void testTargetTrait_DoesThrow() {
                assertThatThrownBy(() -> new ComponentRelationType<>(TargetComponent.class, EnumComponent.class))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContainingAll(TargetComponent.class.getName(), "cannot be used as a relationship component", "marked as a Target");
            }

        }

        @Nested
        class TargetComponentTest {

            @ParameterizedTest
            @ValueSource(classes = { GenericComponent.class, ComponentInterface.class, AbstractComponent.class })
            void testInvalidClassType(Class<?> clazz) {
                assertThatThrownBy(() -> new ComponentRelationType<>(EnumComponent.class, clazz))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContainingAll(clazz.getName(), "cannot be used as a component");
            }

            @ParameterizedTest
            @MethodSource("de.schosin.ecs.api.components.ComponentTypeTest#unsupportedTypes")
            void testUnsupportedClassType(Class<?> clazz) {
                assertThatThrownBy(() -> new ComponentRelationType<>(EnumComponent.class, clazz))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContainingAll(clazz.getName(), "cannot be used as a component");
            }

            @Test
            void testInvalidSyntheticClass() {
                assertThatThrownBy(() -> new ComponentRelationType<>(EnumComponent.class, SYNTHETIC_CLASS))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContainingAll(SYNTHETIC_CLASS.getName(), "must not be synthetic");
            }

            @Test
            void testRelationshipTrait_DoesThrow() {
                assertThatThrownBy(() -> new ComponentRelationType<>(EnumComponent.class, RelationshipComponent.class))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContainingAll(RelationshipComponent.class.getName(), "cannot be used as a target component", "marked as a Relationship");
            }

            @Test
            void testExclusiveTrait_DoesThrow() {
                assertThatThrownBy(() -> new ComponentRelationType<>(EnumComponent.class, ExclusiveComponent.class))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContainingAll(ExclusiveComponent.class.getName(), "cannot be used as a target component", "marked as a Relationship");
            }

            @Test
            void testTargetTrait_DoesNotThrow() {
                assertThatCode(() -> new ComponentRelationType<>(EnumComponent.class, TargetComponent.class)).doesNotThrowAnyException();
            }

        }

    }

    @Nested
    class ExclusiveComponentRelationTypeTest {

        @Nested
        class RelationshipComponentTest {

            @Test
            void testRelationshipTrait_DoesNotThrow() {
                assertThatCode(() -> new ExclusiveComponentRelationType<>(ExclusiveComponent.class, EnumComponent.class)).doesNotThrowAnyException();
            }

        }

        @Nested
        class TargetComponentTest {

            @ParameterizedTest
            @ValueSource(classes = { GenericComponent.class, ComponentInterface.class, AbstractComponent.class })
            void testInvalidClassType(Class<?> clazz) {
                assertThatThrownBy(() -> new ExclusiveComponentRelationType<>(ExclusiveComponent.class, clazz))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContainingAll(clazz.getName(), "cannot be used as a component");
            }

            @ParameterizedTest
            @MethodSource("de.schosin.ecs.api.components.ComponentTypeTest#unsupportedTypes")
            void testUnsupportedClassType(Class<?> clazz) {
                assertThatThrownBy(() -> new ExclusiveComponentRelationType<>(ExclusiveComponent.class, clazz))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContainingAll(clazz.getName(), "cannot be used as a component");
            }

            @Test
            void testInvalidSyntheticClass() {
                assertThatThrownBy(() -> new ExclusiveComponentRelationType<>(ExclusiveComponent.class, SYNTHETIC_CLASS))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContainingAll(SYNTHETIC_CLASS.getName(), "must not be synthetic");
            }

            @Test
            void testRelationshipTrait_DoesThrow() {
                assertThatThrownBy(() -> new ExclusiveComponentRelationType<>(ExclusiveComponent.class, RelationshipComponent.class))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContainingAll(RelationshipComponent.class.getName(), "cannot be used as a target component", "marked as a Relationship");
            }

            @Test
            void testExclusiveTrait_DoesThrow() {
                assertThatThrownBy(() -> new ExclusiveComponentRelationType<>(ExclusiveComponent.class, ExclusiveComponent.class))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContainingAll(ExclusiveComponent.class.getName(), "cannot be used as a target component", "marked as a Relationship");
            }

            @Test
            void testTargetTrait_DoesNotThrow() {
                assertThatCode(() -> new ExclusiveComponentRelationType<>(ExclusiveComponent.class, TargetComponent.class)).doesNotThrowAnyException();
            }

        }

    }

    @Nested
    class WildcardTest {

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

    enum EnumComponent {
        A, B
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

    enum RelationshipComponent implements Relation.Relationship {
        INSTANCE
    }

    enum ExclusiveComponent implements Relation.Exclusive {
        INSTANCE
    }

    enum TargetComponent implements Relation.Target {
        INSTANCE
    }

}
