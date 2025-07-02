package de.schosin.ecs.api.components.types;

import static de.schosin.ecs.api.components.types.ComponentType.component;
import static de.schosin.ecs.api.components.types.ComponentType.exclusiveRelation;
import static de.schosin.ecs.api.components.types.ComponentType.relation;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import de.schosin.ecs.api.components.Relation;
import de.schosin.ecs.api.components.types.ComponentType.RegularComponentType;

class ClassTypeTest extends AbstractComponentTypeTest<ClassTypeTest.MatchesTestCases> {

    public ClassTypeTest() {
        super(MatchesTestCases.class);
    }

    enum MatchesTestCases implements AbstractComponentTypeTest.MatchesTestCase {

        equalClassType(component(RegularComponent.class), component(RegularComponent.class), true),
        otherClassType(component(RegularComponent.class), component(FinalComponent.class), false),
        componentRelation(component(RegularComponent.class), relation(RelationshipComponent.class, TargetComponent.class), false),
        exclusiveComponentRelation(component(RegularComponent.class), exclusiveRelation(ExclusiveComponent.class, TargetComponent.class), false),
        entityRelation(component(RegularComponent.class), relation(EntityRelationshipComponent.class), false),
        exclusiveEntityRelation(component(RegularComponent.class), exclusiveRelation(ExclusiveEntityRelationship.class), false);

        private final ClassType<?> type;
        private final RegularComponentType<?, ?> otherType;
        private final boolean matches;

        private MatchesTestCases(ClassType<?> type, RegularComponentType<?, ?> otherType, boolean matches) {
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

    @Nested
    class CommonClassTypeTest extends CommonComponentTest {

        @Override
        protected ComponentType<?, ?> type(Class<?> clazz) {
            return new ClassType<>(clazz);
        }

    }

    @Test
    void testToString() {
        assertThat(component(RegularComponent.class))
                .extracting(Object::toString, InstanceOfAssertFactories.STRING)
                .containsSubsequence("ClassType", RegularComponent.class.getSimpleName());
    }

    @ParameterizedTest
    @ValueSource(classes = { RegularComponent.class, NonFinalComponent.class, FinalComponent.class })
    void testValidClassTypes(Class<?> clazz) {
        assertThatCode(() -> new ClassType<>(clazz)).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(classes = { RegularComponent.class, NonFinalComponent.class, FinalComponent.class })
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

    @Test
    void testIsInstance() {
        var classType = new ClassType<>(RegularComponent.class);

        assertThat(classType.isInstance(null)).isFalse();
        assertThat(classType.isInstance(new RegularComponent("foo"))).isTrue();
        assertThat(classType.isInstance(new NonFinalComponent())).isFalse();
        assertThat(classType.isInstance(Relation.create(new RegularComponent("relationship"), new RegularComponent("target")))).isFalse();
        assertThat(classType.isInstance(Relation.create(new RegularComponent("relationship"), 42))).isFalse();
    }

}
