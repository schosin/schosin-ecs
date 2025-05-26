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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import de.schosin.ecs.api.components.types.RelationComponentType.ComponentRelationType;
import de.schosin.ecs.api.components.types.RelationComponentType.EntityRelationType;
import de.schosin.ecs.api.components.types.RelationComponentType.ExclusiveComponentRelationType;
import de.schosin.ecs.api.components.types.RelationComponentType.ExclusiveEntityRelationType;

class RelationComponentTypeTest extends AbstractComponentTypeTest<RelationComponentTypeTest.MatchesTestCases> {

    public RelationComponentTypeTest() {
        super(MatchesTestCases.class);
    }

    enum MatchesTestCases implements AbstractComponentTypeTest.MatchesTestCase {

        component_classType(relation(RelationshipComponent.class, TargetComponent.class), component(Component.class), false),
        component_componentRelation(relation(RelationshipComponent.class, TargetComponent.class), relation(RelationshipComponent.class, TargetComponent.class), true),
        component_otherComponentRelation(relation(RelationshipComponent.class, TargetComponent.class), relation(RelationshipComponent.class, Component.class), false),
        component_exclusiveComponentRelation(relation(RelationshipComponent.class, TargetComponent.class), exclusiveRelation(ExclusiveComponent.class, TargetComponent.class), false),
        component_entityRelation(relation(RelationshipComponent.class, TargetComponent.class), relation(EntityRelationshipComponent.class), false),
        component_exclusiveEntityRelation(relation(RelationshipComponent.class, TargetComponent.class), exclusiveRelation(ExclusiveEntityRelationship.class), false),
        component_wildcardObject(relation(RelationshipComponent.class, TargetComponent.class), wildcard(Object.class), false),
        component_componentSet(relation(RelationshipComponent.class, TargetComponent.class), componentSet(MyComponentSet.class), false),
        component_equalEntityFetch(relation(RelationshipComponent.class, TargetComponent.class), relation(EntityRelationshipComponent.class, FETCH), false),
        component_equalExclusiveEntityFetch(relation(RelationshipComponent.class, TargetComponent.class), exclusiveRelation(ExclusiveEntityRelationship.class, FETCH), false),
        component_wildcardComponentRelation(relation(RelationshipComponent.class, TargetComponent.class), wildcardRelation(Object.class, Object.class), false),

        exclusiveComponent_classType(exclusiveRelation(ExclusiveComponent.class, TargetComponent.class), component(Component.class), false),
        exclusiveComponent_componentRelation(exclusiveRelation(ExclusiveComponent.class, TargetComponent.class), relation(RelationshipComponent.class, TargetComponent.class), false),
        exclusiveComponent_exclusiveComponentRelation(exclusiveRelation(ExclusiveComponent.class, TargetComponent.class), exclusiveRelation(ExclusiveComponent.class, TargetComponent.class), true),
        exclusiveComponent_otherExclusiveComponentRelation(exclusiveRelation(ExclusiveComponent.class, TargetComponent.class), exclusiveRelation(ExclusiveComponent.class, Component.class), false),
        exclusiveComponent_entityRelation(exclusiveRelation(ExclusiveComponent.class, TargetComponent.class), relation(EntityRelationshipComponent.class), false),
        exclusiveComponent_exclusiveEntityRelation(exclusiveRelation(ExclusiveComponent.class, TargetComponent.class), exclusiveRelation(ExclusiveEntityRelationship.class), false),
        exclusiveComponent_wildcardObject(exclusiveRelation(ExclusiveComponent.class, TargetComponent.class), wildcard(Object.class), false),
        exclusiveComponent_componentSet(exclusiveRelation(ExclusiveComponent.class, TargetComponent.class), componentSet(MyComponentSet.class), false),
        exclusiveComponent_equalEntityFetch(exclusiveRelation(ExclusiveComponent.class, TargetComponent.class), relation(EntityRelationshipComponent.class, FETCH), false),
        exclusiveComponent_equalExclusiveEntityFetch(exclusiveRelation(ExclusiveComponent.class, TargetComponent.class), exclusiveRelation(ExclusiveEntityRelationship.class, FETCH), false),
        exclusiveComponent_wildcardComponentRelation(exclusiveRelation(ExclusiveComponent.class, TargetComponent.class), wildcardRelation(Object.class, Object.class), false),

        entity_classType(relation(EntityRelationshipComponent.class), component(Component.class), false),
        entity_componentRelation(relation(EntityRelationshipComponent.class), relation(RelationshipComponent.class, TargetComponent.class), false),
        entity_exclusiveComponentRelation(relation(EntityRelationshipComponent.class), exclusiveRelation(ExclusiveComponent.class, TargetComponent.class), false),
        entity_entityRelation(relation(EntityRelationshipComponent.class), relation(EntityRelationshipComponent.class), true),
        entity_otherEntityRelation(relation(EntityRelationshipComponent.class), relation(Component.class), false),
        entity_exclusiveEntityRelation(relation(EntityRelationshipComponent.class), exclusiveRelation(ExclusiveEntityRelationship.class), false),
        entity_wildcardObject(relation(EntityRelationshipComponent.class), wildcard(Object.class), false),
        entity_componentSet(relation(EntityRelationshipComponent.class), componentSet(MyComponentSet.class), false),
        entity_equalEntityFetch(relation(EntityRelationshipComponent.class), relation(EntityRelationshipComponent.class, FETCH), false),
        entity_equalExclusiveEntityFetch(relation(EntityRelationshipComponent.class), exclusiveRelation(ExclusiveEntityRelationship.class, FETCH), false),
        entity_wildcardComponentRelation(relation(EntityRelationshipComponent.class), wildcardRelation(Object.class, Object.class), false),

        exclusiveEntity_classType(exclusiveRelation(ExclusiveEntityRelationship.class), component(Component.class), false),
        exclusiveEntity_componentRelation(exclusiveRelation(ExclusiveEntityRelationship.class), relation(RelationshipComponent.class, TargetComponent.class), false),
        exclusiveEntity_exclusiveComponentRelation(exclusiveRelation(ExclusiveEntityRelationship.class), exclusiveRelation(ExclusiveComponent.class, TargetComponent.class), false),
        exclusiveEntity_entityRelation(exclusiveRelation(ExclusiveEntityRelationship.class), relation(EntityRelationshipComponent.class), false),
        exclusiveEntity_exclusiveEntityRelation(exclusiveRelation(ExclusiveEntityRelationship.class), exclusiveRelation(ExclusiveEntityRelationship.class), true),
        exclusiveEntity_otherExclusiveEntityRelation(exclusiveRelation(ExclusiveEntityRelationship.class), exclusiveRelation(ExclusiveComponent.class), false),
        exclusiveEntity_wildcardObject(exclusiveRelation(ExclusiveEntityRelationship.class), wildcard(Object.class), false),
        exclusiveEntity_componentSet(exclusiveRelation(ExclusiveEntityRelationship.class), componentSet(MyComponentSet.class), false),
        exclusiveEntity_equalEntityFetch(exclusiveRelation(ExclusiveEntityRelationship.class), relation(EntityRelationshipComponent.class, FETCH), false),
        exclusiveEntity_equalExclusiveEntityFetch(exclusiveRelation(ExclusiveEntityRelationship.class), exclusiveRelation(ExclusiveEntityRelationship.class, FETCH), false),
        exclusiveEntity_wildcardComponentRelation(exclusiveRelation(ExclusiveEntityRelationship.class), wildcardRelation(Object.class, Object.class), false);

        private final RelationComponentType<?, ?, ?> type;
        private final ComponentType<?, ?> otherType;
        private final boolean matches;

        private MatchesTestCases(RelationComponentType<?, ?, ?> type, ComponentType<?, ?> otherType, boolean matches) {
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
    class ComponentRelationTypeTest {

        @Test
        void testToString() {
            assertThat(relation(RelationshipComponent.class, TargetComponent.class))
                    .extracting(Object::toString, InstanceOfAssertFactories.STRING)
                    .containsSubsequence("ComponentRelationType", RelationshipComponent.class.getSimpleName(), TargetComponent.class.getSimpleName());
        }

        @Nested
        class RelationshipComponentTest extends CommonComponentTest {

            @Override
            protected ComponentType<?, ?> type(Class<?> clazz) {
                return new ComponentRelationType<>(clazz, EnumComponent.class);
            }

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

            @Test
            void testEntityRelationshipTrait() {
                assertThatThrownBy(() -> new ComponentRelationType<>(EntityRelationshipComponent.class, EnumComponent.class))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContainingAll(EntityRelationshipComponent.class.getName(), "cannot be used for a component relation", "marked as a entity relationship");
            }

            @Test
            void testExclusiveEntityRelationshipTrait() {
                assertThatThrownBy(() -> new ComponentRelationType<>(ExclusiveEntityRelationship.class, EnumComponent.class))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContainingAll(ExclusiveEntityRelationship.class.getName(), "cannot be used for a component relation", "marked as a entity relationship");
            }

        }

        @Nested
        class TargetComponentTest extends CommonComponentTest {

            @Override
            protected ComponentType<?, ?> type(Class<?> clazz) {
                return new ComponentRelationType<>(EnumComponent.class, clazz);
            }

            @ParameterizedTest
            @ValueSource(classes = { GenericComponent.class, ComponentInterface.class, AbstractComponent.class })
            void testInvalidClassType(Class<?> clazz) {
                assertThatThrownBy(() -> new ComponentRelationType<>(EnumComponent.class, clazz))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContainingAll(clazz.getName(), "cannot be used as a component");
            }

            @ParameterizedTest
            @MethodSource("de.schosin.ecs.api.components.types.AbstractComponentTypeTest#unsupportedTypes")
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

            @Test
            void testEntityRelationshipTrait() {
                assertThatThrownBy(() -> new ComponentRelationType<>(EntityRelationshipComponent.class, EnumComponent.class))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContainingAll(EntityRelationshipComponent.class.getName(), "cannot be used for a component relation", "marked as a entity relationship");
            }

            @Test
            void testExclusiveEntityRelationshipTrait() {
                assertThatThrownBy(() -> new ComponentRelationType<>(ExclusiveEntityRelationship.class, EnumComponent.class))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContainingAll(ExclusiveEntityRelationship.class.getName(), "cannot be used for a component relation", "marked as a entity relationship");
            }

        }

    }

    @Nested
    class ExclusiveComponentRelationTypeTest {

        @Test
        void testToString() {
            assertThat(exclusiveRelation(ExclusiveComponent.class, TargetComponent.class))
                    .extracting(Object::toString, InstanceOfAssertFactories.STRING)
                    .containsSubsequence("ExclusiveComponentRelationType", ExclusiveComponent.class.getSimpleName(), TargetComponent.class.getSimpleName());
        }

        @Nested
        class RelationshipComponentTest {

            @Test
            void testRelationshipTrait_DoesNotThrow() {
                assertThatCode(() -> new ExclusiveComponentRelationType<>(ExclusiveComponent.class, EnumComponent.class)).doesNotThrowAnyException();
            }

            @Test
            void testExclusiveEntityRelationshipTrait() {
                assertThatThrownBy(() -> new ExclusiveComponentRelationType<>(ExclusiveEntityRelationship.class, EnumComponent.class))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContainingAll(ExclusiveEntityRelationship.class.getName(), "cannot be used for a component relation", "marked as a entity relationship");
            }

        }

        @Nested
        class TargetComponentTest extends CommonComponentTest {

            @Override
            protected ComponentType<?, ?> type(Class<?> clazz) {
                return new ExclusiveComponentRelationType<>(ExclusiveComponent.class, clazz);
            }

            @ParameterizedTest
            @ValueSource(classes = { GenericComponent.class, ComponentInterface.class, AbstractComponent.class })
            void testInvalidClassType(Class<?> clazz) {
                assertThatThrownBy(() -> new ExclusiveComponentRelationType<>(ExclusiveComponent.class, clazz))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContainingAll(clazz.getName(), "cannot be used as a component");
            }

            @ParameterizedTest
            @MethodSource("de.schosin.ecs.api.components.types.AbstractComponentTypeTest#unsupportedTypes")
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

            @Test
            void testEntityRelationshipTrait() {
                assertThatThrownBy(() -> new ExclusiveComponentRelationType<>(ExclusiveComponent.class, EntityRelationshipComponent.class))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContainingAll(EntityRelationshipComponent.class.getName(), "cannot be used as a target component", "marked as a Relationship");
            }

            @Test
            void testExclusiveEntityRelationshipTrait() {
                assertThatThrownBy(() -> new ExclusiveComponentRelationType<>(ExclusiveComponent.class, ExclusiveEntityRelationship.class))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContainingAll(ExclusiveEntityRelationship.class.getName(), "cannot be used as a target component", "marked as a Relationship");
            }

        }

    }

    @Nested
    class EntityRelationTypeTest extends CommonComponentTest {

        @Test
        void testToString() {
            assertThat(relation(RelationshipComponent.class))
                    .extracting(Object::toString, InstanceOfAssertFactories.STRING)
                    .containsSubsequence("EntityRelationType", RelationshipComponent.class.getSimpleName());
        }

        @Override
        protected ComponentType<?, ?> type(Class<?> clazz) {
            return new EntityRelationType<>(clazz);
        }

        @Test
        void testRelationshipTrait_DoesNotThrow() {
            assertThatCode(() -> new EntityRelationType<>(RelationshipComponent.class)).doesNotThrowAnyException();
        }

        @Test
        void testExclusiveTrait() {
            assertThatThrownBy(() -> new EntityRelationType<>(ExclusiveComponent.class))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContainingAll(ExclusiveComponent.class.getName(), "cannot be used as a non-exclusive relationship component", "marked as a Exclusive");
        }

        @Test
        void testTargetTrait() {
            assertThatThrownBy(() -> new EntityRelationType<>(TargetComponent.class))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContainingAll(TargetComponent.class.getName(), "cannot be used as a relationship component", "marked as a Target");
        }

        @Test
        void testEntityRelationshipTrait_DoesNotThrow() {
            assertThatCode(() -> new EntityRelationType<>(EntityRelationshipComponent.class)).doesNotThrowAnyException();
        }

        @Test
        void testExclusiveEntityRelationshipTrait() {
            assertThatThrownBy(() -> new EntityRelationType<>(ExclusiveEntityRelationship.class))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContainingAll(ExclusiveEntityRelationship.class.getName(), "cannot be used as a non-exclusive relationship component", "marked as a Exclusive");
        }

    }

    @Nested
    class ExclusiveEntityRelationTypeTest {

        @Test
        void testToString() {
            assertThat(exclusiveRelation(ExclusiveComponent.class))
                    .extracting(Object::toString, InstanceOfAssertFactories.STRING)
                    .containsSubsequence("ExclusiveEntityRelationType", ExclusiveComponent.class.getSimpleName());
        }

        @Test
        void testExclusiveTrait_DoesNotThrow() {
            assertThatCode(() -> new ExclusiveEntityRelationType<>(ExclusiveComponent.class)).doesNotThrowAnyException();
        }

        @Test
        void testExclusiveEntityRelationshipTrait() {
            assertThatCode(() -> new ExclusiveEntityRelationType<>(ExclusiveEntityRelationship.class)).doesNotThrowAnyException();
        }

    }

}
