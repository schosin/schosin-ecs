package de.schosin.ecs.plugins.composition.manager;

import static de.schosin.ecs.plugins.wildcards.types.WildcardType.wildcard;
import static de.schosin.ecs.plugins.wildcards.types.WildcardType.wildcardRelation;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import de.schosin.ecs.plugins.composition.Composition;
import de.schosin.ecs.plugins.composition.Composition.Builder;
import de.schosin.ecs.plugins.composition.CompositionData1;
import de.schosin.ecs.plugins.composition.CompositionData2;
import de.schosin.ecs.plugins.composition.CompositionData3;
import de.schosin.ecs.plugins.composition.CompositionData4;
import de.schosin.ecs.plugins.composition.CompositionData5;
import de.schosin.ecs.plugins.composition.CompositionData6;
import de.schosin.ecs.plugins.composition.CompositionData7;
import de.schosin.ecs.plugins.composition.CompositionData8;
import de.schosin.ecs.plugins.composition.Spec;
import de.schosin.ecs.plugins.composition.manager.components.C1234;
import de.schosin.ecs.plugins.composition.manager.components.records.C1;
import de.schosin.ecs.plugins.composition.manager.components.records.C2;
import de.schosin.ecs.plugins.composition.manager.components.records.C3;
import de.schosin.ecs.plugins.composition.manager.components.records.C4;
import de.schosin.ecs.plugins.composition.manager.components.records.C5;
import de.schosin.ecs.plugins.composition.manager.components.records.C6;
import de.schosin.ecs.plugins.composition.manager.components.records.C7;
import de.schosin.ecs.plugins.composition.manager.components.records.C8;

public class SpecTest extends AbstractCompositionPluginTest {

    @Nested
    class IsInterestedTest {

        @Nested
        class CompositionTest extends AbstractIsInterestedTest<Composition> {

            @Override
            protected Composition create(Builder builder) {
                return world.createComposition(builder);
            }

            @Override
            protected boolean isInterested(Composition composition, int entityId) {
                return composition.isInterested(entityId);
            }

        }

        @Nested
        class Of1Test extends AbstractIsInterestedTest<CompositionData1<C1>> {

            @Override
            protected CompositionData1<C1> create(Builder builder) {
                return world.createComposition(builder, C1.class);
            }

            @Override
            protected boolean isInterested(CompositionData1<C1> composition, int entityId) {
                return composition.isInterested(entityId);
            }

        }

        @Nested
        class Of2Test extends AbstractIsInterestedTest<CompositionData2<C1, C2>> {

            @Override
            protected CompositionData2<C1, C2> create(Builder builder) {
                return world.createComposition(builder, C1.class, C2.class);
            }

            @Override
            protected boolean isInterested(CompositionData2<C1, C2> composition, int entityId) {
                return composition.isInterested(entityId);
            }

        }

        @Nested
        class Of3Test extends AbstractIsInterestedTest<CompositionData3<C1, C2, C3>> {

            @Override
            protected CompositionData3<C1, C2, C3> create(Builder builder) {
                return world.createComposition(builder, C1.class, C2.class, C3.class);
            }

            @Override
            protected boolean isInterested(CompositionData3<C1, C2, C3> composition, int entityId) {
                return composition.isInterested(entityId);
            }

        }

        @Nested
        class Of4Test extends AbstractIsInterestedTest<CompositionData4<C1, C2, C3, C4>> {

            @Override
            protected CompositionData4<C1, C2, C3, C4> create(Builder builder) {
                return world.createComposition(builder, C1.class, C2.class, C3.class, C4.class);
            }

            @Override
            protected boolean isInterested(CompositionData4<C1, C2, C3, C4> composition, int entityId) {
                return composition.isInterested(entityId);
            }

        }

        @Nested
        class Of5Test extends AbstractIsInterestedTest<CompositionData5<C1, C2, C3, C4, C5>> {

            @Override
            protected CompositionData5<C1, C2, C3, C4, C5> create(Builder builder) {
                return world.createComposition(builder, C1.class, C2.class, C3.class, C4.class, C5.class);
            }

            @Override
            protected boolean isInterested(CompositionData5<C1, C2, C3, C4, C5> composition, int entityId) {
                return composition.isInterested(entityId);
            }

        }

        @Nested
        class Of6Test extends AbstractIsInterestedTest<CompositionData6<C1, C2, C3, C4, C5, C6>> {

            @Override
            protected CompositionData6<C1, C2, C3, C4, C5, C6> create(Builder builder) {
                return world.createComposition(builder, C1.class, C2.class, C3.class, C4.class, C5.class, C6.class);
            }

            @Override
            protected boolean isInterested(CompositionData6<C1, C2, C3, C4, C5, C6> composition, int entityId) {
                return composition.isInterested(entityId);
            }

        }

        @Nested
        class Of7Test extends AbstractIsInterestedTest<CompositionData7<C1, C2, C3, C4, C5, C6, C7>> {

            @Override
            protected CompositionData7<C1, C2, C3, C4, C5, C6, C7> create(Builder builder) {
                return world.createComposition(builder, C1.class, C2.class, C3.class, C4.class, C5.class, C6.class, C7.class);
            }

            @Override
            protected boolean isInterested(CompositionData7<C1, C2, C3, C4, C5, C6, C7> composition, int entityId) {
                return composition.isInterested(entityId);
            }

        }

        @Nested
        class Of8Test extends AbstractIsInterestedTest<CompositionData8<C1, C2, C3, C4, C5, C6, C7, C8>> {

            @Override
            protected CompositionData8<C1, C2, C3, C4, C5, C6, C7, C8> create(Builder builder) {
                return world.createComposition(builder, C1.class, C2.class, C3.class, C4.class, C5.class, C6.class, C7.class, C8.class);
            }

            @Override
            protected boolean isInterested(CompositionData8<C1, C2, C3, C4, C5, C6, C7, C8> composition, int entityId) {
                return composition.isInterested(entityId);
            }

        }

        abstract class AbstractIsInterestedTest<T extends Spec> {

            int none;
            int c1;
            int c12;
            int c13;
            int c2;
            int c23;
            int c3;
            int c123;
            int c12345;
            int c5;

            protected abstract T create(Composition.Builder builder);

            protected abstract boolean isInterested(T spec, int entityId);

            @BeforeEach
            void setup() {
                none = world.createEntity();
                c1 = world.createEntity(new C1());
                c12 = world.createEntity(new C1(), new C2());
                c13 = world.createEntity(new C1(), new C3());
                c2 = world.createEntity(new C2());
                c23 = world.createEntity(new C2(), new C3());
                c3 = world.createEntity(new C3());
                c123 = world.createEntity(new C1(), new C2(), new C3());
                c12345 = world.createEntity(new C1(), new C2(), new C3(), new C4(), new C5());
                c5 = world.createEntity(new C5());
            }

            @Test
            void emptySpec() {
                var spec = create(Spec.all());

                assertThat(isInterested(spec, none)).isTrue();
                assertThat(isInterested(spec, c1)).isTrue();
                assertThat(isInterested(spec, c12)).isTrue();
                assertThat(isInterested(spec, c13)).isTrue();
                assertThat(isInterested(spec, c2)).isTrue();
                assertThat(isInterested(spec, c23)).isTrue();
                assertThat(isInterested(spec, c3)).isTrue();
                assertThat(isInterested(spec, c123)).isTrue();
                assertThat(isInterested(spec, c12345)).isTrue();
                assertThat(isInterested(spec, c5)).isTrue();
            }

            @Test
            void allSpec() {
                var spec = create(Spec.all(C1.class));

                assertThat(isInterested(spec, none)).isFalse();
                assertThat(isInterested(spec, c1)).isTrue();
                assertThat(isInterested(spec, c12)).isTrue();
                assertThat(isInterested(spec, c13)).isTrue();
                assertThat(isInterested(spec, c2)).isFalse();
                assertThat(isInterested(spec, c23)).isFalse();
                assertThat(isInterested(spec, c3)).isFalse();
                assertThat(isInterested(spec, c123)).isTrue();
                assertThat(isInterested(spec, c12345)).isTrue();
                assertThat(isInterested(spec, c5)).isFalse();
            }

            @Test
            void allOneSpec() {
                var spec = create(Spec.all(C1.class).one(C2.class));

                assertThat(isInterested(spec, none)).isFalse();
                assertThat(isInterested(spec, c1)).isFalse();
                assertThat(isInterested(spec, c12)).isTrue();
                assertThat(isInterested(spec, c13)).isFalse();
                assertThat(isInterested(spec, c2)).isFalse();
                assertThat(isInterested(spec, c23)).isFalse();
                assertThat(isInterested(spec, c3)).isFalse();
                assertThat(isInterested(spec, c123)).isTrue();
                assertThat(isInterested(spec, c12345)).isTrue();
                assertThat(isInterested(spec, c5)).isFalse();
            }

            @Test
            void allNoneSpec() {
                var spec = create(Spec.all(C1.class).none(C3.class));

                assertThat(isInterested(spec, none)).isFalse();
                assertThat(isInterested(spec, c1)).isTrue();
                assertThat(isInterested(spec, c12)).isTrue();
                assertThat(isInterested(spec, c13)).isFalse();
                assertThat(isInterested(spec, c2)).isFalse();
                assertThat(isInterested(spec, c23)).isFalse();
                assertThat(isInterested(spec, c3)).isFalse();
                assertThat(isInterested(spec, c123)).isFalse();
                assertThat(isInterested(spec, c12345)).isFalse();
                assertThat(isInterested(spec, c5)).isFalse();
            }

            @Test
            void oneSpec() {
                var spec = create(Spec.one(C2.class));

                assertThat(isInterested(spec, none)).isFalse();
                assertThat(isInterested(spec, c1)).isFalse();
                assertThat(isInterested(spec, c12)).isTrue();
                assertThat(isInterested(spec, c13)).isFalse();
                assertThat(isInterested(spec, c2)).isTrue();
                assertThat(isInterested(spec, c23)).isTrue();
                assertThat(isInterested(spec, c3)).isFalse();
                assertThat(isInterested(spec, c123)).isTrue();
                assertThat(isInterested(spec, c12345)).isTrue();
                assertThat(isInterested(spec, c5)).isFalse();
            }

            @Test
            void oneNoneSpec() {
                var spec = create(Spec.one(C2.class).none(C3.class));

                assertThat(isInterested(spec, none)).isFalse();
                assertThat(isInterested(spec, c1)).isFalse();
                assertThat(isInterested(spec, c12)).isTrue();
                assertThat(isInterested(spec, c13)).isFalse();
                assertThat(isInterested(spec, c2)).isTrue();
                assertThat(isInterested(spec, c23)).isFalse();
                assertThat(isInterested(spec, c3)).isFalse();
                assertThat(isInterested(spec, c123)).isFalse();
                assertThat(isInterested(spec, c12345)).isFalse();
                assertThat(isInterested(spec, c5)).isFalse();
            }

            @Test
            void noneSpec() {
                var spec = create(Spec.none(C3.class));

                assertThat(isInterested(spec, none)).isTrue();
                assertThat(isInterested(spec, c1)).isTrue();
                assertThat(isInterested(spec, c12)).isTrue();
                assertThat(isInterested(spec, c13)).isFalse();
                assertThat(isInterested(spec, c2)).isTrue();
                assertThat(isInterested(spec, c23)).isFalse();
                assertThat(isInterested(spec, c3)).isFalse();
                assertThat(isInterested(spec, c123)).isFalse();
                assertThat(isInterested(spec, c12345)).isFalse();
                assertThat(isInterested(spec, c5)).isTrue();
            }

            @Test
            void defaultSpec() {
                var spec = create(Spec.all(C1.class).one(C2.class).none(C3.class));

                assertThat(isInterested(spec, none)).isFalse();
                assertThat(isInterested(spec, c1)).isFalse();
                assertThat(isInterested(spec, c12)).isTrue();
                assertThat(isInterested(spec, c13)).isFalse();
                assertThat(isInterested(spec, c2)).isFalse();
                assertThat(isInterested(spec, c23)).isFalse();
                assertThat(isInterested(spec, c3)).isFalse();
                assertThat(isInterested(spec, c123)).isFalse();
                assertThat(isInterested(spec, c12345)).isFalse();
                assertThat(isInterested(spec, c5)).isFalse();
            }

        }

    }

    @Nested
    class MatchesTest {

        static final Class<?>[] EMPTY = {};

        static final Class<?>[] all = { C1.class };
        static final Class<?>[] one = { C2.class };
        static final Class<?>[] none = { C3.class };

        static final Class<?>[] matchingAll = { C1.class, C2.class };
        static final Class<?>[] matchingOne = { C2.class, C3.class };
        static final Class<?>[] matchingNone = { C3.class, C1.class };

        static final Class<?>[] mismatchingAll = { C2.class };
        static final Class<?>[] mismatchingOne = { C3.class };
        static final Class<?>[] mismatchingNone = { C1.class };

        @Nested
        class MatchesSpecTest extends AbstractTest<Spec> {
            @Override
            protected Spec createSpec(Composition.Builder builder) {
                return world.createSpec(builder);
            }
        }

        @Nested
        class MatchesCompositionTest {

            @Nested
            class CompositionTest extends AbstractTest<Composition> {
                @Override
                protected Composition createSpec(Composition.Builder builder) {
                    return world.createComposition(builder);
                }
            }

            @Nested
            class Of1Test extends AbstractTest<CompositionData1<C1>> {
                @Override
                protected CompositionData1<C1> createSpec(Composition.Builder builder) {
                    return world.createComposition(builder, C1.class);
                }
            }

            @Nested
            class Of2Test extends AbstractTest<CompositionData2<C1, C2>> {
                @Override
                protected CompositionData2<C1, C2> createSpec(Composition.Builder builder) {
                    return world.createComposition(builder, C1.class, C2.class);
                }
            }

            @Nested
            class Of3Test extends AbstractTest<CompositionData3<C1, C2, C3>> {
                @Override
                protected CompositionData3<C1, C2, C3> createSpec(Composition.Builder builder) {
                    return world.createComposition(builder, C1.class, C2.class, C3.class);
                }
            }

            @Nested
            class Of4Test extends AbstractTest<CompositionData4<C1, C2, C3, C4>> {
                @Override
                protected CompositionData4<C1, C2, C3, C4> createSpec(Composition.Builder builder) {
                    return world.createComposition(builder, C1.class, C2.class, C3.class, C4.class);
                }
            }

            @Nested
            class Of5Test extends AbstractTest<CompositionData5<C1, C2, C3, C4, C5>> {
                @Override
                protected CompositionData5<C1, C2, C3, C4, C5> createSpec(Composition.Builder builder) {
                    return world.createComposition(builder, C1.class, C2.class, C3.class, C4.class, C5.class);
                }
            }

            @Nested
            class Of6Test extends AbstractTest<CompositionData6<C1, C2, C3, C4, C5, C6>> {
                @Override
                protected CompositionData6<C1, C2, C3, C4, C5, C6> createSpec(Composition.Builder builder) {
                    return world.createComposition(builder, C1.class, C2.class, C3.class, C4.class, C5.class, C6.class);
                }
            }

            @Nested
            class Of7Test extends AbstractTest<CompositionData7<C1, C2, C3, C4, C5, C6, C7>> {
                @Override
                protected CompositionData7<C1, C2, C3, C4, C5, C6, C7> createSpec(Composition.Builder builder) {
                    return world.createComposition(builder, C1.class, C2.class, C3.class, C4.class, C5.class, C6.class, C7.class);
                }
            }

            @Nested
            class Of8Test extends AbstractTest<CompositionData8<C1, C2, C3, C4, C5, C6, C7, C8>> {
                @Override
                protected CompositionData8<C1, C2, C3, C4, C5, C6, C7, C8> createSpec(Composition.Builder builder) {
                    return world.createComposition(builder, C1.class, C2.class, C3.class, C4.class, C5.class, C6.class, C7.class, C8.class);
                }
            }

        }

        abstract class AbstractTest<T extends Spec> {

            protected abstract T createSpec(Composition.Builder builder);

            @Nested
            class CompositionTest extends AbstractCompositionTest<Composition> {
                @Override
                protected Composition createComposition(Composition.Builder builder) {
                    return world.createComposition(builder);
                }
            }

            @Nested
            class Of1Test extends AbstractCompositionTest<CompositionData1<C1>> {
                @Override
                protected CompositionData1<C1> createComposition(Composition.Builder builder) {
                    return world.createComposition(builder, C1.class);
                }
            }

            @Nested
            class Of2Test extends AbstractCompositionTest<CompositionData2<C1, C2>> {
                @Override
                protected CompositionData2<C1, C2> createComposition(Composition.Builder builder) {
                    return world.createComposition(builder, C1.class, C2.class);
                }
            }

            @Nested
            class Of3Test extends AbstractCompositionTest<CompositionData3<C1, C2, C3>> {
                @Override
                protected CompositionData3<C1, C2, C3> createComposition(Composition.Builder builder) {
                    return world.createComposition(builder, C1.class, C2.class, C3.class);
                }
            }

            @Nested
            class Of4Test extends AbstractCompositionTest<CompositionData4<C1, C2, C3, C4>> {
                @Override
                protected CompositionData4<C1, C2, C3, C4> createComposition(Composition.Builder builder) {
                    return world.createComposition(builder, C1.class, C2.class, C3.class, C4.class);
                }
            }

            @Nested
            class Of5Test extends AbstractCompositionTest<CompositionData5<C1, C2, C3, C4, C5>> {
                @Override
                protected CompositionData5<C1, C2, C3, C4, C5> createComposition(Composition.Builder builder) {
                    return world.createComposition(builder, C1.class, C2.class, C3.class, C4.class, C5.class);
                }
            }

            @Nested
            class Of6Test extends AbstractCompositionTest<CompositionData6<C1, C2, C3, C4, C5, C6>> {
                @Override
                protected CompositionData6<C1, C2, C3, C4, C5, C6> createComposition(Composition.Builder builder) {
                    return world.createComposition(builder, C1.class, C2.class, C3.class, C4.class, C5.class, C6.class);
                }
            }

            @Nested
            class Of7Test extends AbstractCompositionTest<CompositionData7<C1, C2, C3, C4, C5, C6, C7>> {
                @Override
                protected CompositionData7<C1, C2, C3, C4, C5, C6, C7> createComposition(Composition.Builder builder) {
                    return world.createComposition(builder, C1.class, C2.class, C3.class, C4.class, C5.class, C6.class, C7.class);
                }
            }

            @Nested
            class Of8Test extends AbstractCompositionTest<CompositionData8<C1, C2, C3, C4, C5, C6, C7, C8>> {
                @Override
                protected CompositionData8<C1, C2, C3, C4, C5, C6, C7, C8> createComposition(Composition.Builder builder) {
                    return world.createComposition(builder, C1.class, C2.class, C3.class, C4.class, C5.class, C6.class, C7.class, C8.class);
                }
            }

            abstract class AbstractCompositionTest<C extends Composition> {

                protected abstract C createComposition(Composition.Builder builder);

                @Test
                void testInvalidSpecImplementation() {
                    var composition = world.createComposition(Composition.all());

                    assertThatThrownBy(() -> composition.matches(CustomSpec.INSTANCE))
                            .isInstanceOf(IllegalArgumentException.class)
                            .hasMessage("Only compare Specs and Compositions returned by the same world.");
                }

                private static class CustomSpec implements Spec {
                    static final CustomSpec INSTANCE = new CustomSpec();

                    @Override
                    public boolean isInterested(int entityId) {
                        return false;
                    }

                }

                @Test
                void emptySpec() {
                    var composition = createComposition(Composition.all());

                    assertThat(composition.matches(createSpec(builder(null, null, null)))).isTrue();
                    assertThat(composition.matches(createSpec(builder(all, null, null)))).isFalse();
                    assertThat(composition.matches(createSpec(builder(all, one, null)))).isFalse();
                    assertThat(composition.matches(createSpec(builder(all, null, none)))).isFalse();
                    assertThat(composition.matches(createSpec(builder(null, one, null)))).isFalse();
                    assertThat(composition.matches(createSpec(builder(null, one, none)))).isFalse();
                    assertThat(composition.matches(createSpec(builder(null, null, none)))).isFalse();
                    assertThat(composition.matches(createSpec(builder(all, one, none)))).isFalse();
                }

                @Test
                void allSpec() {
                    var composition = createComposition(Composition.all(matchingAll));
                    assertThatThrownBy(() -> composition.matches(null)).isInstanceOf(NullPointerException.class);

                    assertThat(composition.matches(createSpec(builder(null, null, null)))).isTrue();
                    assertThat(composition.matches(createSpec(builder(all, null, null)))).isTrue();
                    assertThat(composition.matches(createSpec(builder(all, one, null)))).isFalse();
                    assertThat(composition.matches(createSpec(builder(all, null, none)))).isFalse();
                    assertThat(composition.matches(createSpec(builder(null, one, null)))).isFalse();
                    assertThat(composition.matches(createSpec(builder(null, one, none)))).isFalse();
                    assertThat(composition.matches(createSpec(builder(null, null, none)))).isFalse();
                    assertThat(composition.matches(createSpec(builder(all, one, none)))).isFalse();
                }

                @Test
                void allSpec_mismatching() {
                    var composition = createComposition(Composition.all(mismatchingAll));
                    assertThatThrownBy(() -> composition.matches(null)).isInstanceOf(NullPointerException.class);

                    assertThat(composition.matches(createSpec(builder(null, null, null)))).isTrue();
                    assertThat(composition.matches(createSpec(builder(all, null, null)))).isFalse();
                    assertThat(composition.matches(createSpec(builder(all, one, null)))).isFalse();
                    assertThat(composition.matches(createSpec(builder(all, null, none)))).isFalse();
                    assertThat(composition.matches(createSpec(builder(null, one, null)))).isFalse();
                    assertThat(composition.matches(createSpec(builder(null, one, none)))).isFalse();
                    assertThat(composition.matches(createSpec(builder(null, null, none)))).isFalse();
                    assertThat(composition.matches(createSpec(builder(all, one, none)))).isFalse();
                }

                @Test
                void allOneSpec() {
                    var composition = createComposition(Composition.all(matchingAll).one(matchingOne));
                    assertThatThrownBy(() -> composition.matches(null)).isInstanceOf(NullPointerException.class);

                    assertThat(composition.matches(createSpec(builder(null, null, null)))).isTrue();
                    assertThat(composition.matches(createSpec(builder(all, null, null)))).isTrue();
                    assertThat(composition.matches(createSpec(builder(all, one, null)))).isTrue();
                    assertThat(composition.matches(createSpec(builder(all, null, none)))).isFalse();
                    assertThat(composition.matches(createSpec(builder(null, one, null)))).isTrue();
                    assertThat(composition.matches(createSpec(builder(null, one, none)))).isFalse();
                    assertThat(composition.matches(createSpec(builder(null, null, none)))).isFalse();
                    assertThat(composition.matches(createSpec(builder(all, one, none)))).isFalse();
                }

                @Test
                void allOneSpec_mismatchingAll() {
                    var composition = createComposition(Composition.all(mismatchingAll).one(matchingOne));
                    assertThatThrownBy(() -> composition.matches(null)).isInstanceOf(NullPointerException.class);

                    assertThat(composition.matches(createSpec(builder(null, null, null)))).isTrue();
                    assertThat(composition.matches(createSpec(builder(all, null, null)))).isFalse();
                    assertThat(composition.matches(createSpec(builder(all, one, null)))).isFalse();
                    assertThat(composition.matches(createSpec(builder(all, null, none)))).isFalse();
                    assertThat(composition.matches(createSpec(builder(null, one, null)))).isTrue();
                    assertThat(composition.matches(createSpec(builder(null, one, none)))).isFalse();
                    assertThat(composition.matches(createSpec(builder(null, null, none)))).isFalse();
                    assertThat(composition.matches(createSpec(builder(all, one, none)))).isFalse();
                }

                @Test
                void allOneSpec_mismatchingOne() {
                    var composition = createComposition(Composition.all(matchingAll).one(mismatchingOne));
                    assertThatThrownBy(() -> composition.matches(null)).isInstanceOf(NullPointerException.class);

                    assertThat(composition.matches(createSpec(builder(null, null, null)))).isTrue();
                    assertThat(composition.matches(createSpec(builder(all, null, null)))).isTrue();
                    assertThat(composition.matches(createSpec(builder(all, one, null)))).isFalse();
                    assertThat(composition.matches(createSpec(builder(all, null, none)))).isFalse();
                    assertThat(composition.matches(createSpec(builder(null, one, null)))).isFalse();
                    assertThat(composition.matches(createSpec(builder(null, one, none)))).isFalse();
                    assertThat(composition.matches(createSpec(builder(null, null, none)))).isFalse();
                    assertThat(composition.matches(createSpec(builder(all, one, none)))).isFalse();
                }

                @Test
                void allNoneSpec() {
                    var composition = createComposition(Composition.all(matchingAll).none(matchingNone));
                    assertThatThrownBy(() -> composition.matches(null)).isInstanceOf(NullPointerException.class);

                    assertThat(composition.matches(createSpec(builder(null, null, null)))).isTrue();
                    assertThat(composition.matches(createSpec(builder(all, null, null)))).isTrue();
                    assertThat(composition.matches(createSpec(builder(all, one, null)))).isFalse();
                    assertThat(composition.matches(createSpec(builder(all, null, none)))).isTrue();
                    assertThat(composition.matches(createSpec(builder(null, one, null)))).isFalse();
                    assertThat(composition.matches(createSpec(builder(null, one, none)))).isFalse();
                    assertThat(composition.matches(createSpec(builder(null, null, none)))).isTrue();
                    assertThat(composition.matches(createSpec(builder(all, one, none)))).isFalse();
                }

                @Test
                void allNoneSpec_mismatchingAll() {
                    var composition = createComposition(Composition.all(mismatchingAll).none(matchingNone));
                    assertThatThrownBy(() -> composition.matches(null)).isInstanceOf(NullPointerException.class);

                    assertThat(composition.matches(createSpec(builder(null, null, null)))).isTrue();
                    assertThat(composition.matches(createSpec(builder(all, null, null)))).isFalse();
                    assertThat(composition.matches(createSpec(builder(all, one, null)))).isFalse();
                    assertThat(composition.matches(createSpec(builder(all, null, none)))).isFalse();
                    assertThat(composition.matches(createSpec(builder(null, one, null)))).isFalse();
                    assertThat(composition.matches(createSpec(builder(null, one, none)))).isFalse();
                    assertThat(composition.matches(createSpec(builder(null, null, none)))).isTrue();
                    assertThat(composition.matches(createSpec(builder(all, one, none)))).isFalse();
                }

                @Test
                void allNoneSpec_mismatchingNone() {
                    var composition = createComposition(Composition.all(matchingAll).none(mismatchingNone));
                    assertThatThrownBy(() -> composition.matches(null)).isInstanceOf(NullPointerException.class);

                    assertThat(composition.matches(createSpec(builder(null, null, null)))).isTrue();
                    assertThat(composition.matches(createSpec(builder(all, null, null)))).isTrue();
                    assertThat(composition.matches(createSpec(builder(all, one, null)))).isFalse();
                    assertThat(composition.matches(createSpec(builder(all, null, none)))).isFalse();
                    assertThat(composition.matches(createSpec(builder(null, one, null)))).isFalse();
                    assertThat(composition.matches(createSpec(builder(null, one, none)))).isFalse();
                    assertThat(composition.matches(createSpec(builder(null, null, none)))).isFalse();
                    assertThat(composition.matches(createSpec(builder(all, one, none)))).isFalse();
                }

                @Test
                void oneSpec() {
                    var composition = createComposition(Composition.one(matchingOne));
                    assertThatThrownBy(() -> composition.matches(null)).isInstanceOf(NullPointerException.class);

                    assertThat(composition.matches(createSpec(builder(null, null, null)))).isTrue();
                    assertThat(composition.matches(createSpec(builder(all, null, null)))).isFalse();
                    assertThat(composition.matches(createSpec(builder(all, one, null)))).isFalse();
                    assertThat(composition.matches(createSpec(builder(all, null, none)))).isFalse();
                    assertThat(composition.matches(createSpec(builder(null, one, null)))).isTrue();
                    assertThat(composition.matches(createSpec(builder(null, one, none)))).isFalse();
                    assertThat(composition.matches(createSpec(builder(null, null, none)))).isFalse();
                    assertThat(composition.matches(createSpec(builder(all, one, none)))).isFalse();
                }

                @Test
                void mismatchingOneSpec() {
                    var composition = createComposition(Composition.one(mismatchingOne));
                    assertThatThrownBy(() -> composition.matches(null)).isInstanceOf(NullPointerException.class);

                    assertThat(composition.matches(createSpec(builder(null, null, null)))).isTrue();
                    assertThat(composition.matches(createSpec(builder(all, null, null)))).isFalse();
                    assertThat(composition.matches(createSpec(builder(all, one, null)))).isFalse();
                    assertThat(composition.matches(createSpec(builder(all, null, none)))).isFalse();
                    assertThat(composition.matches(createSpec(builder(null, one, null)))).isFalse();
                    assertThat(composition.matches(createSpec(builder(null, one, none)))).isFalse();
                    assertThat(composition.matches(createSpec(builder(null, null, none)))).isFalse();
                    assertThat(composition.matches(createSpec(builder(all, one, none)))).isFalse();
                }

                @Test
                void oneNoneSpec() {
                    var composition = createComposition(Composition.one(matchingOne).none(matchingNone));
                    assertThatThrownBy(() -> composition.matches(null)).isInstanceOf(NullPointerException.class);

                    assertThat(composition.matches(createSpec(builder(null, null, null)))).isTrue();
                    assertThat(composition.matches(createSpec(builder(all, null, null)))).isFalse();
                    assertThat(composition.matches(createSpec(builder(all, one, null)))).isFalse();
                    assertThat(composition.matches(createSpec(builder(all, null, none)))).isFalse();
                    assertThat(composition.matches(createSpec(builder(null, one, null)))).isTrue();
                    assertThat(composition.matches(createSpec(builder(null, one, none)))).isTrue();
                    assertThat(composition.matches(createSpec(builder(null, null, none)))).isTrue();
                    assertThat(composition.matches(createSpec(builder(all, one, none)))).isFalse();
                }

                @Test
                void oneNoneSpec_mismatchingOne() {
                    var composition = createComposition(Composition.one(mismatchingOne).none(matchingNone));
                    assertThatThrownBy(() -> composition.matches(null)).isInstanceOf(NullPointerException.class);

                    assertThat(composition.matches(createSpec(builder(null, null, null)))).isTrue();
                    assertThat(composition.matches(createSpec(builder(all, null, null)))).isFalse();
                    assertThat(composition.matches(createSpec(builder(all, one, null)))).isFalse();
                    assertThat(composition.matches(createSpec(builder(all, null, none)))).isFalse();
                    assertThat(composition.matches(createSpec(builder(null, one, null)))).isFalse();
                    assertThat(composition.matches(createSpec(builder(null, one, none)))).isFalse();
                    assertThat(composition.matches(createSpec(builder(null, null, none)))).isTrue();
                    assertThat(composition.matches(createSpec(builder(all, one, none)))).isFalse();
                }

                @Test
                void oneNoneSpec_mismatchingNone() {
                    var composition = createComposition(Composition.one(matchingOne).none(mismatchingNone));
                    assertThatThrownBy(() -> composition.matches(null)).isInstanceOf(NullPointerException.class);

                    assertThat(composition.matches(createSpec(builder(null, null, null)))).isTrue();
                    assertThat(composition.matches(createSpec(builder(all, null, null)))).isFalse();
                    assertThat(composition.matches(createSpec(builder(all, one, null)))).isFalse();
                    assertThat(composition.matches(createSpec(builder(all, null, none)))).isFalse();
                    assertThat(composition.matches(createSpec(builder(null, one, null)))).isTrue();
                    assertThat(composition.matches(createSpec(builder(null, one, none)))).isFalse();
                    assertThat(composition.matches(createSpec(builder(null, null, none)))).isFalse();
                    assertThat(composition.matches(createSpec(builder(all, one, none)))).isFalse();
                }

                @Test
                void noneSpec() {
                    var composition = createComposition(Composition.none(matchingNone));
                    assertThatThrownBy(() -> composition.matches(null)).isInstanceOf(NullPointerException.class);

                    assertThat(composition.matches(createSpec(builder(null, null, null)))).isTrue();
                    assertThat(composition.matches(createSpec(builder(all, null, null)))).isFalse();
                    assertThat(composition.matches(createSpec(builder(all, one, null)))).isFalse();
                    assertThat(composition.matches(createSpec(builder(all, null, none)))).isFalse();
                    assertThat(composition.matches(createSpec(builder(null, one, null)))).isFalse();
                    assertThat(composition.matches(createSpec(builder(null, one, none)))).isFalse();
                    assertThat(composition.matches(createSpec(builder(null, null, none)))).isTrue();
                    assertThat(composition.matches(createSpec(builder(all, one, none)))).isFalse();
                }

                @Test
                void noneSpec_mismatching() {
                    var composition = createComposition(Composition.none(mismatchingNone));
                    assertThatThrownBy(() -> composition.matches(null)).isInstanceOf(NullPointerException.class);

                    assertThat(composition.matches(createSpec(builder(null, null, null)))).isTrue();
                    assertThat(composition.matches(createSpec(builder(all, null, null)))).isFalse();
                    assertThat(composition.matches(createSpec(builder(all, one, null)))).isFalse();
                    assertThat(composition.matches(createSpec(builder(all, null, none)))).isFalse();
                    assertThat(composition.matches(createSpec(builder(null, one, null)))).isFalse();
                    assertThat(composition.matches(createSpec(builder(null, one, none)))).isFalse();
                    assertThat(composition.matches(createSpec(builder(null, null, none)))).isFalse();
                    assertThat(composition.matches(createSpec(builder(all, one, none)))).isFalse();
                }

                @Test
                void defaultSpec() {
                    var composition = createComposition(Composition.all(matchingAll).one(matchingOne).none(matchingNone));
                    assertThatThrownBy(() -> composition.matches(null)).isInstanceOf(NullPointerException.class);

                    assertThat(composition.matches(createSpec(builder(null, null, null)))).isTrue();
                    assertThat(composition.matches(createSpec(builder(all, null, null)))).isTrue();
                    assertThat(composition.matches(createSpec(builder(all, one, null)))).isTrue();
                    assertThat(composition.matches(createSpec(builder(all, null, none)))).isTrue();
                    assertThat(composition.matches(createSpec(builder(null, one, null)))).isTrue();
                    assertThat(composition.matches(createSpec(builder(null, one, none)))).isTrue();
                    assertThat(composition.matches(createSpec(builder(null, null, none)))).isTrue();
                    assertThat(composition.matches(createSpec(builder(all, one, none)))).isTrue();
                }

                @Test
                void defaultSpec_mismatchingAll() {
                    var composition = createComposition(Composition.all(mismatchingAll).one(matchingOne).none(matchingNone));
                    assertThatThrownBy(() -> composition.matches(null)).isInstanceOf(NullPointerException.class);

                    assertThat(composition.matches(createSpec(builder(null, null, null)))).isTrue();
                    assertThat(composition.matches(createSpec(builder(all, null, null)))).isFalse();
                    assertThat(composition.matches(createSpec(builder(all, one, null)))).isFalse();
                    assertThat(composition.matches(createSpec(builder(all, null, none)))).isFalse();
                    assertThat(composition.matches(createSpec(builder(null, one, null)))).isTrue();
                    assertThat(composition.matches(createSpec(builder(null, one, none)))).isTrue();
                    assertThat(composition.matches(createSpec(builder(null, null, none)))).isTrue();
                    assertThat(composition.matches(createSpec(builder(all, one, none)))).isFalse();
                }

                @Test
                void defaultSpec_mismatchingOne() {
                    var composition = createComposition(Composition.all(matchingAll).one(mismatchingOne).none(matchingNone));
                    assertThatThrownBy(() -> composition.matches(null)).isInstanceOf(NullPointerException.class);

                    assertThat(composition.matches(createSpec(builder(null, null, null)))).isTrue();
                    assertThat(composition.matches(createSpec(builder(all, null, null)))).isTrue();
                    assertThat(composition.matches(createSpec(builder(all, one, null)))).isFalse();
                    assertThat(composition.matches(createSpec(builder(all, null, none)))).isTrue();
                    assertThat(composition.matches(createSpec(builder(null, one, null)))).isFalse();
                    assertThat(composition.matches(createSpec(builder(null, one, none)))).isFalse();
                    assertThat(composition.matches(createSpec(builder(null, null, none)))).isTrue();
                    assertThat(composition.matches(createSpec(builder(all, one, none)))).isFalse();
                }

                @Test
                void defaultSpec_mismatchingNone() {
                    var composition = createComposition(Composition.all(matchingAll).one(matchingOne).none(mismatchingNone));
                    assertThatThrownBy(() -> composition.matches(null)).isInstanceOf(NullPointerException.class);

                    assertThat(composition.matches(createSpec(builder(null, null, null)))).isTrue();
                    assertThat(composition.matches(createSpec(builder(all, null, null)))).isTrue();
                    assertThat(composition.matches(createSpec(builder(all, one, null)))).isTrue();
                    assertThat(composition.matches(createSpec(builder(all, null, none)))).isFalse();
                    assertThat(composition.matches(createSpec(builder(null, one, null)))).isTrue();
                    assertThat(composition.matches(createSpec(builder(null, one, none)))).isFalse();
                    assertThat(composition.matches(createSpec(builder(null, null, none)))).isFalse();
                    assertThat(composition.matches(createSpec(builder(all, one, none)))).isFalse();
                }

                @Nested
                class NonRegularComponentTypesTest {

                    @Test
                    void testWildcard() {
                        var composition1 = createComposition(Composition.all(component(C1.class)));
                        var composition4 = createComposition(Composition.all(component(C4.class)));
                        var composition5 = createComposition(Composition.all(component(C5.class)));
                        var composition1234 = createComposition(Composition.all(wildcard(C1234.class)));

                        var spec1 = createSpec(Composition.all(C1.class));
                        var spec4 = createSpec(Composition.all(C4.class));
                        var spec5 = createSpec(Composition.all(C5.class));
                        var spec1234 = createSpec(Composition.all(wildcard(C1234.class)));

                        assertThat(composition1234.matches(spec1)).isFalse();
                        assertThat(composition1234.matches(spec4)).isFalse();
                        assertThat(composition1234.matches(spec5)).isFalse();
                        assertThat(composition1234.matches(spec1234)).isTrue();

                        assertThat(composition1.matches(spec1)).isTrue();
                        assertThat(composition1.matches(spec4)).isFalse();
                        assertThat(composition1.matches(spec5)).isFalse();
                        assertThat(composition1.matches(spec1234)).isTrue();

                        assertThat(composition4.matches(spec1)).isFalse();
                        assertThat(composition4.matches(spec4)).isTrue();
                        assertThat(composition4.matches(spec5)).isFalse();
                        assertThat(composition4.matches(spec1234)).isTrue();

                        assertThat(composition5.matches(spec1)).isFalse();
                        assertThat(composition5.matches(spec4)).isFalse();
                        assertThat(composition5.matches(spec5)).isTrue();
                        assertThat(composition5.matches(spec1234)).isFalse();
                    }

                    @Test
                    void testWildcardComponentRelation() {
                        var composition1 = createComposition(Composition.all(relation(C1.class, C8.class)));
                        var composition4 = createComposition(Composition.all(relation(C4.class, C8.class)));
                        var composition5 = createComposition(Composition.all(relation(C5.class, C8.class)));
                        var composition1234 = createComposition(Composition.all(wildcardRelation(C1234.class, C8.class)));

                        var spec1 = createSpec(Composition.all(relation(C1.class, C8.class)));
                        var spec4 = createSpec(Composition.all(relation(C4.class, C8.class)));
                        var spec5 = createSpec(Composition.all(relation(C5.class, C8.class)));
                        var spec1234 = createSpec(Composition.all(wildcardRelation(C1234.class, C8.class)));

                        assertThat(composition1234.matches(spec1)).isFalse();
                        assertThat(composition1234.matches(spec4)).isFalse();
                        assertThat(composition1234.matches(spec5)).isFalse();
                        assertThat(composition1234.matches(spec1234)).isTrue();

                        assertThat(composition1.matches(spec1)).isTrue();
                        assertThat(composition1.matches(spec4)).isFalse();
                        assertThat(composition1.matches(spec5)).isFalse();
                        assertThat(composition1.matches(spec1234)).isTrue();

                        assertThat(composition4.matches(spec1)).isFalse();
                        assertThat(composition4.matches(spec4)).isTrue();
                        assertThat(composition4.matches(spec5)).isFalse();
                        assertThat(composition4.matches(spec1234)).isTrue();

                        assertThat(composition5.matches(spec1)).isFalse();
                        assertThat(composition5.matches(spec4)).isFalse();
                        assertThat(composition5.matches(spec5)).isTrue();
                        assertThat(composition5.matches(spec1234)).isFalse();
                    }

                    @Test
                    void testWildcardEntityRelation() {
                        var composition1 = createComposition(Composition.all(relation(C1.class)));
                        var composition4 = createComposition(Composition.all(relation(C4.class)));
                        var composition5 = createComposition(Composition.all(relation(C5.class)));
                        var composition1234 = createComposition(Composition.all(wildcardRelation(C1234.class)));

                        var spec1 = createSpec(Composition.all(relation(C1.class)));
                        var spec4 = createSpec(Composition.all(relation(C4.class)));
                        var spec5 = createSpec(Composition.all(relation(C5.class)));
                        var spec1234 = createSpec(Composition.all(wildcardRelation(C1234.class)));

                        assertThat(composition1234.matches(spec1)).isFalse();
                        assertThat(composition1234.matches(spec4)).isFalse();
                        assertThat(composition1234.matches(spec5)).isFalse();
                        assertThat(composition1234.matches(spec1234)).isTrue();

                        assertThat(composition1.matches(spec1)).isTrue();
                        assertThat(composition1.matches(spec4)).isFalse();
                        assertThat(composition1.matches(spec5)).isFalse();
                        assertThat(composition1.matches(spec1234)).isTrue();

                        assertThat(composition4.matches(spec1)).isFalse();
                        assertThat(composition4.matches(spec4)).isTrue();
                        assertThat(composition4.matches(spec5)).isFalse();
                        assertThat(composition4.matches(spec1234)).isTrue();

                        assertThat(composition5.matches(spec1)).isFalse();
                        assertThat(composition5.matches(spec4)).isFalse();
                        assertThat(composition5.matches(spec5)).isTrue();
                        assertThat(composition5.matches(spec1234)).isFalse();
                    }

                }

                private Composition.Builder builder(Class<?>[] all, Class<?>[] one, Class<?>[] none) {
                    return Composition
                            .all(all != null ? all : EMPTY)
                            .one(one != null ? one : EMPTY)
                            .none(none != null ? none : EMPTY);
                }

            }

        }

    }

}
