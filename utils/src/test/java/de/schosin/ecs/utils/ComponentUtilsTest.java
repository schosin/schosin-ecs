package de.schosin.ecs.utils;

import static de.schosin.ecs.api.components.ComponentType.component;
import static de.schosin.ecs.api.components.ComponentType.wildcard;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import de.schosin.ecs.api.components.ComponentType;
import de.schosin.ecs.api.components.ComponentType.RegularComponentType;

class ComponentUtilsTest {

    @Nested
    class MatchesRegularComponentTestTest extends AbstractRegularComponentTypeTest {

        @Override
        protected boolean matches(ComponentType<?> type, RegularComponentType<?> otherType) {
            return ComponentUtils.matches(type, otherType);
        }

    }

    @Nested
    class MatchesComponentTypeTest extends AbstractRegularComponentTypeTest {

        @Override
        protected boolean matches(ComponentType<?> type, RegularComponentType<?> otherType) {
            return ComponentUtils.matches(type, (ComponentType<?>) otherType);
        }

        protected boolean matches(ComponentType<?> type, ComponentType<?> otherType) {
            return ComponentUtils.matches(type, otherType);
        }

        @Nested
        class WildcardTest {

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

        }

    }

    abstract class AbstractRegularComponentTypeTest {

        protected abstract boolean matches(ComponentType<?> type, RegularComponentType<?> otherType);

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

            assertThat(matches(type, type)).as("same type matches itself").isTrue();
            assertThat(matches(type, component(clazz))).as("equal type matches itself").isTrue();
            assertThat(matches(type, otherType)).as("type does not match other class type").isFalse();
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

}
