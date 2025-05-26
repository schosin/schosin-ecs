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
import org.junit.jupiter.params.provider.ValueSource;

import de.schosin.ecs.api.components.Relation.EntityRelationship;
import de.schosin.ecs.api.components.types.WildcardRelationType.WildcardComponentRelationType;
import de.schosin.ecs.api.components.types.WildcardRelationType.WildcardEntityRelationType;

class WildcardRelationTypeTest extends AbstractComponentTypeTest<WildcardRelationTypeTest.MatchesTestCases> {

    public WildcardRelationTypeTest() {
        super(MatchesTestCases.class);
    }

    enum MatchesTestCases implements AbstractComponentTypeTest.MatchesTestCase {

        wildcardComponent_objectWildcard(wildcardRelation(Object.class, Object.class), component(Component.class), false),
        wildcardComponent_matchingInterface(wildcardRelation(Object.class, Object.class), component(FinalComponent.class), false),
        wildcardComponent_mismatchingInterface(wildcardRelation(Object.class, Object.class), component(Component.class), false),
        wildcardComponent_componentRelation(wildcardRelation(Object.class, Object.class), relation(RelationshipComponent.class, TargetComponent.class), true),
        wildcardComponent_componentRelation_mismatchingRelationship(wildcardRelation(RelationshipWildcard.class, Object.class), relation(RelationshipComponent.class, TargetComponent.class), false),
        wildcardComponent_componentRelation_mismatchingTarget(wildcardRelation(Object.class, TargetWildcard.class), relation(RelationshipComponent.class, TargetComponent.class), false),
        wildcardComponent_exclusiveComponentRelation(wildcardRelation(Object.class, Object.class), exclusiveRelation(ExclusiveComponent.class, TargetComponent.class), true),
        wildcardComponent_exclusiveComponentRelation_mismatchingRelationship(wildcardRelation(RelationshipWildcard.class, Object.class),
                exclusiveRelation(ExclusiveComponent.class, TargetComponent.class), false),
        wildcardComponent_exclusiveComponentRelation_mismatchingTarget(wildcardRelation(Object.class, TargetWildcard.class), exclusiveRelation(ExclusiveComponent.class, TargetComponent.class), false),
        wildcardComponent_entityRelation(wildcardRelation(Object.class, Object.class), relation(EntityRelationshipComponent.class), false),
        wildcardComponent_exclusiveEntityRelation(wildcardRelation(Object.class, Object.class), exclusiveRelation(ExclusiveEntityRelationship.class), false),
        wildcardComponent_wildcard(wildcardRelation(Object.class, Object.class), wildcard(Object.class), false),
        wildcardComponent_componentSet(wildcardRelation(Object.class, Object.class), componentSet(MyComponentSet.class), false),
        wildcardComponent_entityFetch(wildcardRelation(Object.class, Object.class), relation(EntityRelationshipComponent.class, FETCH), false),
        wildcardComponent_exclusiveEntityFetch(wildcardRelation(Object.class, Object.class), exclusiveRelation(ExclusiveEntityRelationship.class, FETCH), false),
        wildcardComponent_equal(wildcardRelation(Object.class, Object.class), wildcardRelation(Object.class, Object.class), true),
        wildcardComponent_relationshipSupertype(wildcardRelation(Object.class, Object.class), wildcardRelation(RelationshipWildcard.class, Object.class), true),
        wildcardComponent_targetSupertype(wildcardRelation(Object.class, Object.class), wildcardRelation(Object.class, TargetWildcard.class), true),
        wildcardComponent_relationshipSubtype(wildcardRelation(RelationshipWildcard.class, Object.class), wildcardRelation(Object.class, Object.class), false),
        wildcardComponent_targetSubtype(wildcardRelation(Object.class, TargetWildcard.class), wildcardRelation(Object.class, Object.class), false),
        wildcardComponent_wildcardEntityRelation(wildcardRelation(Object.class, Object.class), wildcardRelation(Object.class), false),
        wildcardComponent_wildcardEntityFetchRelation(wildcardRelation(Object.class, Object.class), wildcardRelation(Object.class, component(Component.class)), false),

        wildcardEntity_objectWildcard(wildcardRelation(Object.class), component(Component.class), false),
        wildcardEntity_matchingInterface(wildcardRelation(Object.class), component(FinalComponent.class), false),
        wildcardEntity_mismatchingInterface(wildcardRelation(Object.class), component(Component.class), false),
        wildcardEntity_entityRelation(wildcardRelation(Object.class), relation(RelationshipComponent.class), true),
        wildcardEntity_entityRelation_mismatchingRelationship(wildcardRelation(RelationshipWildcard.class), relation(RelationshipComponent.class), false),
        wildcardEntity_exclusiveEntityRelation(wildcardRelation(Object.class), exclusiveRelation(ExclusiveComponent.class), true),
        wildcardEntity_exclusiveEntityRelation_mismatchingRelationship(wildcardRelation(RelationshipWildcard.class), exclusiveRelation(ExclusiveComponent.class), false),
        wildcardEntity_componentRelation(wildcardRelation(Object.class), relation(RelationshipComponent.class, TargetComponent.class), false),
        wildcardEntity_exclusiveComponentRelation(wildcardRelation(Object.class), exclusiveRelation(ExclusiveComponent.class, TargetComponent.class), false),
        wildcardEntity_wildcard(wildcardRelation(Object.class), wildcard(Object.class), false),
        wildcardEntity_componentSet(wildcardRelation(Object.class), componentSet(MyComponentSet.class), false),
        wildcardEntity_entityFetch(wildcardRelation(Object.class), relation(EntityRelationshipComponent.class, FETCH), false),
        wildcardEntity_exclusiveEntityFetch(wildcardRelation(Object.class), exclusiveRelation(ExclusiveEntityRelationship.class, FETCH), false),
        wildcardEntity_equal(wildcardRelation(Object.class), wildcardRelation(Object.class), true),
        wildcardEntity_relationshipSupertype(wildcardRelation(Object.class), wildcardRelation(RelationshipWildcard.class), true),
        wildcardEntity_relationshipSubtype(wildcardRelation(RelationshipWildcard.class), wildcardRelation(Object.class), false),
        wildcardEntity_wildcardComponentRelation(wildcardRelation(Object.class), wildcardRelation(Object.class, Object.class), false),
        wildcardEntity_wildcardEntityFetchRelation(wildcardRelation(Object.class), wildcardRelation(Object.class, component(Component.class)), false),

        wildcardEntityFetch_objectWildcard(wildcardRelation(EntityRelationship.class, FETCH), component(Component.class), false),
        wildcardEntityFetch_componentRelation(wildcardRelation(EntityRelationship.class, FETCH), relation(RelationshipComponent.class, TargetComponent.class), false),
        wildcardEntityFetch_exclusiveComponentRelation(wildcardRelation(EntityRelationship.class, FETCH), exclusiveRelation(ExclusiveComponent.class, TargetComponent.class), false),
        wildcardEntityFetch_entityRelation(wildcardRelation(EntityRelationship.class, FETCH), relation(EntityRelationshipComponent.class), false),
        wildcardEntityFetch_exclusiveEntityRelation(wildcardRelation(EntityRelationship.class, FETCH), exclusiveRelation(ExclusiveEntityRelationship.class), false),
        wildcardEntityFetch_wildcardObject(wildcardRelation(EntityRelationship.class, FETCH), wildcard(Object.class), false),
        wildcardEntityFetch_componentSet(wildcardRelation(EntityRelationship.class, FETCH), componentSet(MyComponentSet.class), false),
        wildcardEntityFetch_entityFetch(wildcardRelation(EntityRelationship.class, FETCH), relation(EntityRelationshipComponent.class, FETCH), false),
        wildcardEntityFetch_exclusiveEntityFetch(wildcardRelation(EntityRelationship.class, FETCH), exclusiveRelation(ExclusiveEntityRelationship.class, FETCH), false),
        wildcardEntityFetch_wildcardComponentRelation(wildcardRelation(EntityRelationship.class, FETCH), wildcardRelation(Object.class, Object.class), false),
        wildcardEntityFetch_wildcardEntityRelation(wildcardRelation(EntityRelationship.class, FETCH), wildcardRelation(Object.class), false),
        wildcardEntityFetch_wildcardEntityFetchRelation_equal(wildcardRelation(EntityRelationship.class, FETCH), wildcardRelation(EntityRelationship.class, FETCH), true),
        wildcardEntityFetch_wildcardEntityFetchRelation_supertype(wildcardRelation(Object.class, FETCH), wildcardRelation(EntityRelationship.class, FETCH), true),
        wildcardEntityFetch_wildcardEntityFetchRelation_subtype(wildcardRelation(EntityRelationship.class, FETCH), wildcardRelation(Object.class, FETCH), false);

        private final WildcardRelationType<?, ?> type;
        private final ComponentType<?, ?> otherType;
        private final boolean matches;

        private MatchesTestCases(WildcardRelationType<?, ?> type, ComponentType<?, ?> otherType, boolean matches) {
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
    class WildcardComponentRelationTypeTest {

        @ParameterizedTest
        @ValueSource(classes = { ComponentInterface.class, AbstractComponent.class, NonFinalComponent.class })
        void testToString(Class<?> relationshipBound) {
            assertThat(wildcardRelation(relationshipBound, ComponentInterface.class))
                    .extracting(Object::toString, InstanceOfAssertFactories.STRING)
                    .containsSubsequence("WildcardComponentRelationType", relationshipBound.getSimpleName(), ComponentInterface.class.getSimpleName());
        }

        @ParameterizedTest
        @ValueSource(classes = { EnumComponent.class, Component.class, FinalComponent.class })
        void testRelationshipFinal(Class<?> finalBound) {
            assertThatCode(() -> new WildcardComponentRelationType<>(finalBound, ComponentInterface.class)).doesNotThrowAnyException();
        }

        @ParameterizedTest
        @ValueSource(classes = { EnumComponent.class, Component.class, FinalComponent.class })
        void testTargetFinal(Class<?> finalBound) {
            assertThatCode(() -> new WildcardComponentRelationType<>(ComponentInterface.class, finalBound)).doesNotThrowAnyException();
        }

        @ParameterizedTest
        @ValueSource(classes = { EnumComponent.class, Component.class, FinalComponent.class })
        void testBothFinal(Class<?> finalBound) {
            assertThatThrownBy(() -> new WildcardComponentRelationType<>(finalBound, Component.class))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContainingAll(finalBound.getName(), Component.class.getName(), "cannot be used as a component wildcard bounds", "At one most may be final");
        }

        @Nested
        class RelationshipTest {

            @ParameterizedTest
            @ValueSource(classes = { ComponentInterface.class, AbstractComponent.class, NonFinalComponent.class })
            void testRelationshipBound(Class<?> relationshipBound) {
                var wildcard = new WildcardComponentRelationType<>(relationshipBound, ComponentInterface.class);

                assertThat(wildcard.relationshipBound()).isSameAs(relationshipBound);
                assertThat(wildcard.targetBound()).isSameAs(ComponentInterface.class);
            }

            @ParameterizedTest
            @ValueSource(classes = { GenericComponent.class, GenericComponent.class, GenericComponentInterface.class, int.class, int[].class, Integer[].class, Object[].class })
            void testInvalidRelationshipBound(Class<?> relationshipBound) {
                assertThatThrownBy(() -> new WildcardComponentRelationType<>(relationshipBound, ComponentInterface.class))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContainingAll(relationshipBound.getName(), "cannot be used as a wildcard");
            }

            @Test
            void testInvalidSyntheticClass() {
                assertThatThrownBy(() -> new WildcardComponentRelationType<>(SYNTHETIC_CLASS, ComponentInterface.class))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContainingAll(SYNTHETIC_CLASS.getName(), "must not be synthetic");
            }

            @Test
            void testRelationshipTrait() {
                assertThatCode(() -> new WildcardComponentRelationType<>(RelationshipWildcard.class, ComponentInterface.class)).doesNotThrowAnyException();
            }

            @Test
            void testTargetTrait() {
                assertThatThrownBy(() -> new WildcardComponentRelationType<>(TargetWildcard.class, ComponentInterface.class))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContainingAll(TargetWildcard.class.getName(), "cannot be used as a relationship bound", "marked as a Target");
            }

            @Test
            void testExclusiveTrait() {
                assertThatCode(() -> new WildcardComponentRelationType<>(ExclusiveWildcard.class, ComponentInterface.class)).doesNotThrowAnyException();
            }

            @Test
            void testEntityRelationshipTrait() {
                assertThatThrownBy(() -> new WildcardComponentRelationType<>(EntityRelationshipWildcard.class, ComponentInterface.class))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContainingAll(EntityRelationshipWildcard.class.getName(), "cannot be used for a component relation", "marked as a entity relationship component");
            }

            @Test
            void testExclusiveEntityRelationshipTrait() {
                assertThatThrownBy(() -> new WildcardComponentRelationType<>(ExclusiveEntityRelationshipWildcard.class, ComponentInterface.class))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContainingAll(ExclusiveEntityRelationshipWildcard.class.getName(), "cannot be used for a component relation", "marked as a entity relationship component");
            }

        }

        @Nested
        class TargetTest {

            @ParameterizedTest
            @ValueSource(classes = { ComponentInterface.class, AbstractComponent.class, NonFinalComponent.class })
            void testTargetBound(Class<?> relationshipBound) {
                var wildcard = new WildcardComponentRelationType<>(ComponentInterface.class, relationshipBound);

                assertThat(wildcard.relationshipBound()).isSameAs(ComponentInterface.class);
                assertThat(wildcard.targetBound()).isSameAs(relationshipBound);
            }

            @ParameterizedTest
            @ValueSource(classes = { GenericComponent.class, GenericComponent.class, GenericComponentInterface.class, int.class, int[].class, Integer[].class, Object[].class })
            void testInvalidTargetBound(Class<?> relationshipBound) {
                assertThatThrownBy(() -> new WildcardComponentRelationType<>(ComponentInterface.class, relationshipBound))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContainingAll(relationshipBound.getName(), "cannot be used as a wildcard");
            }

            @Test
            void testInvalidSyntheticClass() {
                assertThatThrownBy(() -> new WildcardComponentRelationType<>(ComponentInterface.class, SYNTHETIC_CLASS))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContainingAll(SYNTHETIC_CLASS.getName(), "must not be synthetic");
            }

            @Test
            void testTargetTrait() {
                assertThatCode(() -> new WildcardComponentRelationType<>(ComponentInterface.class, TargetWildcard.class)).doesNotThrowAnyException();
            }

            @Test
            void testExclusiveTrait() {
                assertThatCode(() -> new WildcardComponentRelationType<>(ComponentInterface.class, ExclusiveWildcard.class))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContainingAll(ExclusiveWildcard.class.getName(), "cannot be used as a target bound", "marked as a Relationship");
            }

            @Test
            void testEntityRelationshipTrait() {
                assertThatCode(() -> new WildcardComponentRelationType<>(ComponentInterface.class, EntityRelationshipWildcard.class)).isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContainingAll(EntityRelationshipWildcard.class.getName(), "cannot be used as a target bound", "marked as a Relationship");
            }

            @Test
            void testExclusiveEntityRelationshipTrait() {
                assertThatCode(() -> new WildcardComponentRelationType<>(ComponentInterface.class, ExclusiveEntityRelationshipWildcard.class)).isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContainingAll(ExclusiveEntityRelationshipWildcard.class.getName(), "cannot be used as a target bound", "marked as a Relationship");
            }

        }

    }

    @Nested
    class WildcardEntityRelationTypeTest {

        @ParameterizedTest
        @ValueSource(classes = { ComponentInterface.class, AbstractComponent.class, NonFinalComponent.class })
        void testToString(Class<?> relationshipBound) {
            assertThat(wildcardRelation(relationshipBound))
                    .extracting(Object::toString, InstanceOfAssertFactories.STRING)
                    .containsSubsequence("WildcardEntityRelationType", relationshipBound.getSimpleName());
        }

        @ParameterizedTest
        @ValueSource(classes = { EnumComponent.class, Component.class, FinalComponent.class })
        void testRelationshipFinal(Class<?> finalBound) {
            assertThatThrownBy(() -> new WildcardEntityRelationType<>(finalBound))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContainingAll(finalBound.getName(), "cannot be used as a entity relationship bound", "Must not be final");
        }

        @Nested
        class RelationshipTest {

            @ParameterizedTest
            @ValueSource(classes = { ComponentInterface.class, AbstractComponent.class, NonFinalComponent.class })
            void testRelationshipBound(Class<?> relationshipBound) {
                var wildcard = new WildcardEntityRelationType<>(relationshipBound);

                assertThat(wildcard.relationshipBound()).isSameAs(relationshipBound);
            }

            @ParameterizedTest
            @ValueSource(classes = { GenericComponent.class, GenericComponent.class, GenericComponentInterface.class, int.class, int[].class, Integer[].class, Object[].class })
            void testInvalidRelationshipBound(Class<?> relationshipBound) {
                assertThatThrownBy(() -> new WildcardEntityRelationType<>(relationshipBound))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContainingAll(relationshipBound.getName(), "cannot be used as a wildcard");
            }

            @Test
            void testInvalidSyntheticClass() {
                assertThatThrownBy(() -> new WildcardEntityRelationType<>(SYNTHETIC_CLASS))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContainingAll(SYNTHETIC_CLASS.getName(), "must not be synthetic");
            }

            @Test
            void testRelationshipTrait() {
                assertThatCode(() -> new WildcardEntityRelationType<>(RelationshipWildcard.class)).doesNotThrowAnyException();
            }

            @Test
            void testTargetTrait() {
                assertThatThrownBy(() -> new WildcardEntityRelationType<>(TargetWildcard.class))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContainingAll(TargetWildcard.class.getName(), "cannot be used as a relationship bound", "marked as a Target");
            }

            @Test
            void testExclusiveTrait() {
                assertThatCode(() -> new WildcardEntityRelationType<>(ExclusiveWildcard.class)).doesNotThrowAnyException();
            }

            @Test
            void testEntityRelationshipTrait() {
                assertThatCode(() -> new WildcardEntityRelationType<>(EntityRelationshipWildcard.class)).doesNotThrowAnyException();
            }

            @Test
            void testExclusiveEntityRelationshipTrait() {
                assertThatCode(() -> new WildcardEntityRelationType<>(ExclusiveEntityRelationshipWildcard.class)).doesNotThrowAnyException();
            }

        }

    }

    /*
    
    @Nested
    class WildcardEntityRelationFetchTypeTest {
    
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
        @ValueSource(classes = { Component.class, GenericComponent.class, GenericComponent.class, GenericComponentInterface.class, FinalComponent.class, int[].class, Integer[].class, Object[].class })
        void testInvalidRelationshipBound(Class<?> relationshipBound) {
            assertThatThrownBy(() -> new WildcardEntityRelationFetchType<>(relationshipBound, FETCH))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContainingAll(relationshipBound.getName(), "cannot be used as a wildcard");
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
    
    */

}
