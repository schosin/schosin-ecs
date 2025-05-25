package de.schosin.ecs.api.components.types;

import static de.schosin.ecs.api.components.types.ComponentType.component;
import static de.schosin.ecs.api.components.types.ComponentType.componentSet;
import static de.schosin.ecs.api.components.types.ComponentType.exclusiveRelation;
import static de.schosin.ecs.api.components.types.ComponentType.relation;
import static de.schosin.ecs.api.components.types.ComponentType.wildcard;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ClassTypeTest extends AbstractComponentTypeTest<ClassTypeTest.MatchesTestCases> {

    public ClassTypeTest() {
        super(MatchesTestCases.class);
    }

    enum MatchesTestCases implements AbstractComponentTypeTest.MatchesTestCase {

        equalClassType(component(Component.class), component(Component.class), true),
        otherClassType(component(Component.class), component(FinalComponent.class), false),
        componentRelation(component(Component.class), relation(RelationshipComponent.class, TargetComponent.class), false),
        exclusiveComponentRelation(component(Component.class), exclusiveRelation(ExclusiveComponent.class, TargetComponent.class), false),
        entityRelation(component(Component.class), relation(EntityRelationshipComponent.class), false),
        exclusiveEntityRelation(component(Component.class), exclusiveRelation(ExclusiveEntityRelationship.class), false),
        wildcardObject(component(Component.class), wildcard(Object.class), false),
        wildcardSameClass(component(NonFinalComponent.class), wildcard(NonFinalComponent.class), false),
        wildcardParentInterface(component(FinalComponent.class), wildcard(ComponentInterface.class), false),
        componentSet(component(Component.class), componentSet(MyComponentSet.class), false),
        entityFetch(component(Component.class), relation(EntityRelationshipComponent.class, FETCH), false),
        exclusiveEntityFetch(component(Component.class), exclusiveRelation(ExclusiveEntityRelationship.class, FETCH), false);

        private final ClassType<?> type;
        private final ComponentType<?, ?> otherType;
        private final boolean matches;

        private MatchesTestCases(ClassType<?> type, ComponentType<?, ?> otherType, boolean matches) {
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
