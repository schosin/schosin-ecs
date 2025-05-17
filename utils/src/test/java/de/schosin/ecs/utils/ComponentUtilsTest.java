package de.schosin.ecs.utils;

import static de.schosin.ecs.api.components.ComponentType.component;
import static de.schosin.ecs.api.components.ComponentType.exclusiveRelation;
import static de.schosin.ecs.api.components.ComponentType.relation;
import static de.schosin.ecs.api.components.ComponentType.wildcard;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import de.schosin.ecs.api.components.ComponentType;
import de.schosin.ecs.api.components.ComponentType.RegularComponentType;
import de.schosin.ecs.api.components.Relation;

class ComponentUtilsTest {

    @Nested
    class MatchesRegularComponentTest extends AbstractRegularComponentTypeTest {

        @Override
        protected boolean matches(ComponentType<?, ?> type, RegularComponentType<?, ?> otherType) {
            return ComponentUtils.matches(type, otherType);
        }

    }

    @Nested
    class MatchesComponentTypeTest extends AbstractRegularComponentTypeTest {

        @Override
        protected boolean matches(ComponentType<?, ?> type, RegularComponentType<?, ?> otherType) {
            return ComponentUtils.matches(type, (ComponentType<?, ?>) otherType);
        }

        protected boolean matches(ComponentType<?, ?> type, ComponentType<?, ?> otherType) {
            return ComponentUtils.matches(type, otherType);
        }

        @Nested
        class WildcardTest {

            @Test
            void testRegularMatchesWildcard() {
                var wildcard = wildcard(C12.class);

                assertThat(matches(component(C1.class), wildcard)).isFalse();
                assertThat(matches(component(C2.class), wildcard)).isFalse();
                assertThat(matches(component(C3.class), wildcard)).isFalse();
            }

            @Test
            void testRegularMatchesWildcard_ExtendedComponent() {
                var wildcard = wildcard(C4.class);

                assertThat(matches(component(C4.class), wildcard)).isTrue();
                assertThat(matches(component(C4v2.class), wildcard)).isFalse();
            }

            @Test
            void testWildcardMatchesEqualWildcard() {
                var wildcard = wildcard(C12.class);

                assertThat(matches(wildcard, wildcard(C12.class))).isTrue();
            }

            @Test
            void testWildcardMatchesStricterWildcard() {
                var wildcard = wildcard(C.class);
                var wildcardStrict = wildcard(C12.class);

                assertThat(matches(wildcard, wildcardStrict)).isTrue();
            }

            @Test
            void testWildcardMatchesLessStrictWildcard() {
                var wildcard = wildcard(C.class);
                var wildcardStrict = wildcard(C12.class);

                assertThat(matches(wildcardStrict, wildcard)).isFalse();
            }

            @Test
            void testWildcardMatchesComponentRelation() {
                var wildcard = wildcard(C.class);
                var relation = relation(RelationshipComponent.class, TargetA.class);

                assertThat(matches(wildcard, relation)).isFalse();
            }

            @Test
            void testWildcardMatchesExclusiveComponentRelation() {
                var wildcard = wildcard(C.class);
                var relation = exclusiveRelation(ExclusiveComponent.class, TargetA.class);

                assertThat(matches(wildcard, relation)).isFalse();
            }

            @Test
            void testComponentRelationMatchesWildcard() {
                var wildcard = wildcard(C.class);
                var relation = relation(RelationshipComponent.class, TargetA.class);

                assertThat(matches(relation, wildcard)).isFalse();
            }

            @Test
            void testExclusiveComponentRelationWildcard() {
                var wildcard = wildcard(C.class);
                var relation = exclusiveRelation(ExclusiveComponent.class, TargetA.class);

                assertThat(matches(relation, wildcard)).isFalse();
            }

        }

    }

    abstract class AbstractRegularComponentTypeTest {

        protected abstract boolean matches(ComponentType<?, ?> type, RegularComponentType<?, ?> otherType);

        @Test
        void testNullType() {
            var other = component(C1.class);
            assertThatThrownBy(() -> matches(null, other)).isInstanceOf(NullPointerException.class);
        }

        @Test
        void testNullOther() {
            var type = component(C1.class);
            assertThatThrownBy(() -> matches(type, null)).isInstanceOf(NullPointerException.class);
        }

        @ParameterizedTest
        @ValueSource(classes = { C1.class, C2.class, C3.class })
        void testClassTypeEquality(Class<?> clazz) {
            var type = component(clazz);
            var otherType = C1.class == clazz ? component(C2.class) : component(C1.class);
            var relationType = relation(RelationshipComponent.class, TargetA.class);
            var exclusiveRelationType = exclusiveRelation(ExclusiveComponent.class, TargetA.class);

            assertThat(matches(type, type)).as("same type matches itself").isTrue();
            assertThat(matches(type, component(clazz))).as("equal type matches itself").isTrue();
            assertThat(matches(type, otherType)).as("type does not match other class type").isFalse();
            assertThat(matches(type, relationType)).as("type does not match relation type").isFalse();
            assertThat(matches(type, exclusiveRelationType)).as("type does not match exclusive relation type").isFalse();
        }

        @Test
        void testComponentRelationTypeEquality() {
            var type = relation(RelationshipComponent.class, TargetA.class);
            var otherRelationship = relation(OtherRelationshipComponent.class, TargetA.class);
            var otherTarget = relation(RelationshipComponent.class, TargetB.class);
            var exclusiveRelationship = exclusiveRelation(ExclusiveComponent.class, TargetA.class);
            var classType = component(TargetA.class);

            assertThat(matches(type, type)).as("same type matches itself").isTrue();
            assertThat(matches(type, relation(RelationshipComponent.class, TargetA.class))).as("equal type matches itself").isTrue();
            assertThat(matches(type, otherRelationship)).as("does not match relation with different relationship").isFalse();
            assertThat(matches(type, otherTarget)).as("does not match relation with different target").isFalse();
            assertThat(matches(type, exclusiveRelationship)).as("does not match exclusive relation").isFalse();
            assertThat(matches(type, classType)).as("does not match class type").isFalse();
        }

        @Test
        void testExclusiveComponentRelationTypeEquality() {
            var type = exclusiveRelation(ExclusiveComponent.class, TargetA.class);
            var otherRelationship = exclusiveRelation(OtherExclusiveComponent.class, TargetA.class);
            var otherTarget = exclusiveRelation(ExclusiveComponent.class, TargetB.class);
            var nonExclusiveRelationship = relation(RelationshipComponent.class, TargetA.class);
            var classType = component(TargetA.class);

            assertThat(matches(type, type)).as("same type matches itself").isTrue();
            assertThat(matches(type, exclusiveRelation(ExclusiveComponent.class, TargetA.class))).as("equal type matches itself").isTrue();
            assertThat(matches(type, otherRelationship)).as("does not match relation with different relationship").isFalse();
            assertThat(matches(type, otherTarget)).as("does not match relation with different target").isFalse();
            assertThat(matches(type, nonExclusiveRelationship)).as("does not match non-exclusive relation").isFalse();
            assertThat(matches(type, classType)).as("does not match class type").isFalse();
        }

        @Nested
        class CommonWildcardTest {

            @Test
            void testWildcardMatchesRegular() {
                var wildcard = wildcard(C12.class);

                assertThat(matches(wildcard, component(C1.class))).isTrue();
                assertThat(matches(wildcard, component(C2.class))).isTrue();
                assertThat(matches(wildcard, component(C3.class))).isFalse();
            }

            @Test
            void testWildcardMatchesRegular_ExtendedComponent() {
                var wildcard = wildcard(C4.class);

                assertThat(matches(wildcard, component(C4.class))).isTrue();
                assertThat(matches(wildcard, component(C4v2.class))).isTrue();
            }

            @Test
            void testWildcardMatchesComponentRelation() {
                var wildcard = wildcard(C.class);
                var relation = relation(RelationshipComponent.class, TargetA.class);

                assertThat(matches(wildcard, relation)).isFalse();
            }

            @Test
            void testWildcardMatchesExclusiveComponentRelation() {
                var wildcard = wildcard(C.class);
                var relation = exclusiveRelation(ExclusiveComponent.class, TargetA.class);

                assertThat(matches(wildcard, relation)).isFalse();
            }

        }

    }

    interface C {
    }

    interface C12 extends C {
    }

    interface C123 extends C {
    }

    interface C23 extends C {
    }

    record C1() implements C12, C123 {
    }

    record C2() implements C12, C123, C23 {
    }

    record C3() implements C123, C23 {
    }

    class C4 {
    }

    class C4v2 extends C4 {
    }

    enum RelationshipComponent implements Relation.Relationship {
        INSTANCE
    }

    enum OtherRelationshipComponent implements Relation.Relationship {
        INSTANCE
    }

    enum ExclusiveComponent implements Relation.Exclusive {
        INSTANCE
    }

    enum OtherExclusiveComponent implements Relation.Exclusive {
        INSTANCE
    }

    record TargetA() {
    }

    record TargetB() {
    }

}
