package de.schosin.ecs.engine.compositions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import de.schosin.ecs.api.archetype.Archetype;
import de.schosin.ecs.api.components.Composition;
import de.schosin.ecs.api.components.Composition.Builder;
import de.schosin.ecs.api.components.Spec;
import de.schosin.ecs.engine.AbstractWorldTest;
import de.schosin.ecs.engine.components.ComponentData;
import de.schosin.ecs.engine.components.ComponentMask;
import de.schosin.ecs.engine.utils.collections.Bag;
import de.schosin.ecs.engine.utils.collections.BitVector;
import de.schosin.ecs.engine.utils.collections.IntBag;

class CompositionManagerTest extends AbstractWorldTest {

    private static final AtomicInteger MASK_ID = new AtomicInteger(0);

    static final BitVector EMPTY_VECTOR = new BitVector();
    static final ComponentMask EMPTY_MASK = mask(EMPTY_VECTOR);

    static final Composition.Builder EMPTY = Composition.all();

    int component1Id;
    int component2Id;

    @BeforeEach
    void setup() {
        MASK_ID.set(0);

        this.component1Id = componentManager.getData(C1.class).id();
        this.component2Id = componentManager.getData(C2.class).id();
    }

    @Test
    void testCaching() {
        // Setup
        var counter = new AtomicInteger(0);
        Function<EngineSpec, IntBag> supplier = spec -> {
            counter.incrementAndGet();
            return new IntBag(1);
        };

        // First call
        var composition = compositionManager.create(EMPTY, supplier);
        assertThat(counter).hasValue(1);

        // Second call 
        var composition2 = compositionManager.create(EMPTY, supplier);
        assertThat(counter).hasValue(1);
        assertThat(composition2).isSameAs(composition);
    }

    @Test
    void testProcess() {
        // Setup
        var entities = new IntBag(4);
        entities.add(1);
        entities.add(4);

        var processed = new IntBag(2);

        var composition = compositionManager.create(EMPTY, spec -> entities);

        // Process
        composition.process(processed::add);

        // Verify
        assertThat(processed.getSize()).as("size").isEqualTo(2);
        assertThat(processed.contains(1)).as("contains 1").isTrue();
        assertThat(processed.contains(4)).as("contains 4").isTrue();
    }

    @Test
    void testStream() {
        // Setup
        var entities = new IntBag(4);
        for (int i = 1; i <= 10; i++) {
            entities.add(i);
        }

        var composition = compositionManager.create(EMPTY, spec -> entities);

        // Process
        var processed = composition.stream().toArray();

        // Verify
        assertThat(processed)
                .hasSize(10)
                .containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
    }

    @Test
    void testStreamParallelized() {
        // Setup
        var entities = new IntBag(4);
        for (int i = 1; i <= 10; i++) {
            entities.add(i);
        }

        var composition = compositionManager.create(EMPTY, spec -> entities);

        // Process
        var processed = composition.stream().parallel().toArray();

        // Verify
        assertThat(processed)
                .hasSize(10)
                .containsExactlyInAnyOrder(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
    }

    @Test
    void testParallelStream() {
        // Setup
        var entities = new IntBag(4);
        for (int i = 1; i <= 10; i++) {
            entities.add(i);
        }

        var composition = compositionManager.create(EMPTY, spec -> entities);

        // Process
        var processed = composition.parallelStream().toArray();

        // Verify
        assertThat(processed)
                .hasSize(10)
                .containsExactlyInAnyOrder(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
    }

    @Nested
    class SpecTest {

        @Nested
        class IsInterestedTest {

            @Nested
            class CompositionTest extends SpecManagerTest.AbstractIsInterestedTest<Composition> {

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
            class Of1Test extends SpecManagerTest.AbstractIsInterestedTest<Composition.Of1<C1>> {

                @Override
                protected Composition.Of1<C1> create(Builder builder) {
                    return world.createComposition(builder, C1.class);
                }

                @Override
                protected boolean isInterested(Composition.Of1<C1> composition, int entityId) {
                    return composition.isInterested(entityId);
                }

            }

            @Nested
            class Of2Test extends SpecManagerTest.AbstractIsInterestedTest<Composition.Of2<C1, C2>> {

                @Override
                protected Composition.Of2<C1, C2> create(Builder builder) {
                    return world.createComposition(builder, C1.class, C2.class);
                }

                @Override
                protected boolean isInterested(Composition.Of2<C1, C2> composition, int entityId) {
                    return composition.isInterested(entityId);
                }

            }

            @Nested
            class Of3Test extends SpecManagerTest.AbstractIsInterestedTest<Composition.Of3<C1, C2, C3>> {

                @Override
                protected Composition.Of3<C1, C2, C3> create(Builder builder) {
                    return world.createComposition(builder, C1.class, C2.class, C3.class);
                }

                @Override
                protected boolean isInterested(Composition.Of3<C1, C2, C3> composition, int entityId) {
                    return composition.isInterested(entityId);
                }

            }

            @Nested
            class Of4Test extends SpecManagerTest.AbstractIsInterestedTest<Composition.Of4<C1, C2, C3, C4>> {

                @Override
                protected Composition.Of4<C1, C2, C3, C4> create(Builder builder) {
                    return world.createComposition(builder, C1.class, C2.class, C3.class, C4.class);
                }

                @Override
                protected boolean isInterested(Composition.Of4<C1, C2, C3, C4> composition, int entityId) {
                    return composition.isInterested(entityId);
                }

            }

            @Nested
            class Of5Test extends SpecManagerTest.AbstractIsInterestedTest<Composition.Of5<C1, C2, C3, C4, C5>> {

                @Override
                protected Composition.Of5<C1, C2, C3, C4, C5> create(Builder builder) {
                    return world.createComposition(builder, C1.class, C2.class, C3.class, C4.class, C5.class);
                }

                @Override
                protected boolean isInterested(Composition.Of5<C1, C2, C3, C4, C5> composition, int entityId) {
                    return composition.isInterested(entityId);
                }

            }

            @Nested
            class Of6Test extends SpecManagerTest.AbstractIsInterestedTest<Composition.Of6<C1, C2, C3, C4, C5, C6>> {

                @Override
                protected Composition.Of6<C1, C2, C3, C4, C5, C6> create(Builder builder) {
                    return world.createComposition(builder, C1.class, C2.class, C3.class, C4.class, C5.class, C6.class);
                }

                @Override
                protected boolean isInterested(Composition.Of6<C1, C2, C3, C4, C5, C6> composition, int entityId) {
                    return composition.isInterested(entityId);
                }

            }

            @Nested
            class Of7Test extends SpecManagerTest.AbstractIsInterestedTest<Composition.Of7<C1, C2, C3, C4, C5, C6, C7>> {

                @Override
                protected Composition.Of7<C1, C2, C3, C4, C5, C6, C7> create(Builder builder) {
                    return world.createComposition(builder, C1.class, C2.class, C3.class, C4.class, C5.class, C6.class, C7.class);
                }

                @Override
                protected boolean isInterested(Composition.Of7<C1, C2, C3, C4, C5, C6, C7> composition, int entityId) {
                    return composition.isInterested(entityId);
                }

            }

            @Nested
            class Of8Test extends SpecManagerTest.AbstractIsInterestedTest<Composition.Of8<C1, C2, C3, C4, C5, C6, C7, C8>> {

                @Override
                protected Composition.Of8<C1, C2, C3, C4, C5, C6, C7, C8> create(Builder builder) {
                    return world.createComposition(builder, C1.class, C2.class, C3.class, C4.class, C5.class, C6.class, C7.class, C8.class);
                }

                @Override
                protected boolean isInterested(Composition.Of8<C1, C2, C3, C4, C5, C6, C7, C8> composition, int entityId) {
                    return composition.isInterested(entityId);
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
                class Of1Test extends AbstractTest<Composition.Of1<C1>> {
                    @Override
                    protected Composition.Of1<C1> createSpec(Composition.Builder builder) {
                        return world.createComposition(builder, C1.class);
                    }
                }

                @Nested
                class Of2Test extends AbstractTest<Composition.Of2<C1, C2>> {
                    @Override
                    protected Composition.Of2<C1, C2> createSpec(Composition.Builder builder) {
                        return world.createComposition(builder, C1.class, C2.class);
                    }
                }

                @Nested
                class Of3Test extends AbstractTest<Composition.Of3<C1, C2, C3>> {
                    @Override
                    protected Composition.Of3<C1, C2, C3> createSpec(Composition.Builder builder) {
                        return world.createComposition(builder, C1.class, C2.class, C3.class);
                    }
                }

                @Nested
                class Of4Test extends AbstractTest<Composition.Of4<C1, C2, C3, C4>> {
                    @Override
                    protected Composition.Of4<C1, C2, C3, C4> createSpec(Composition.Builder builder) {
                        return world.createComposition(builder, C1.class, C2.class, C3.class, C4.class);
                    }
                }

                @Nested
                class Of5Test extends AbstractTest<Composition.Of5<C1, C2, C3, C4, C5>> {
                    @Override
                    protected Composition.Of5<C1, C2, C3, C4, C5> createSpec(Composition.Builder builder) {
                        return world.createComposition(builder, C1.class, C2.class, C3.class, C4.class, C5.class);
                    }
                }

                @Nested
                class Of6Test extends AbstractTest<Composition.Of6<C1, C2, C3, C4, C5, C6>> {
                    @Override
                    protected Composition.Of6<C1, C2, C3, C4, C5, C6> createSpec(Composition.Builder builder) {
                        return world.createComposition(builder, C1.class, C2.class, C3.class, C4.class, C5.class, C6.class);
                    }
                }

                @Nested
                class Of7Test extends AbstractTest<Composition.Of7<C1, C2, C3, C4, C5, C6, C7>> {
                    @Override
                    protected Composition.Of7<C1, C2, C3, C4, C5, C6, C7> createSpec(Composition.Builder builder) {
                        return world.createComposition(builder, C1.class, C2.class, C3.class, C4.class, C5.class, C6.class, C7.class);
                    }
                }

                @Nested
                class Of8Test extends AbstractTest<Composition.Of8<C1, C2, C3, C4, C5, C6, C7, C8>> {
                    @Override
                    protected Composition.Of8<C1, C2, C3, C4, C5, C6, C7, C8> createSpec(Composition.Builder builder) {
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
                class Of1Test extends AbstractCompositionTest<Composition.Of1<C1>> {
                    @Override
                    protected Composition.Of1<C1> createComposition(Composition.Builder builder) {
                        return world.createComposition(builder, C1.class);
                    }
                }

                @Nested
                class Of2Test extends AbstractCompositionTest<Composition.Of2<C1, C2>> {
                    @Override
                    protected Composition.Of2<C1, C2> createComposition(Composition.Builder builder) {
                        return world.createComposition(builder, C1.class, C2.class);
                    }
                }

                @Nested
                class Of3Test extends AbstractCompositionTest<Composition.Of3<C1, C2, C3>> {
                    @Override
                    protected Composition.Of3<C1, C2, C3> createComposition(Composition.Builder builder) {
                        return world.createComposition(builder, C1.class, C2.class, C3.class);
                    }
                }

                @Nested
                class Of4Test extends AbstractCompositionTest<Composition.Of4<C1, C2, C3, C4>> {
                    @Override
                    protected Composition.Of4<C1, C2, C3, C4> createComposition(Composition.Builder builder) {
                        return world.createComposition(builder, C1.class, C2.class, C3.class, C4.class);
                    }
                }

                @Nested
                class Of5Test extends AbstractCompositionTest<Composition.Of5<C1, C2, C3, C4, C5>> {
                    @Override
                    protected Composition.Of5<C1, C2, C3, C4, C5> createComposition(Composition.Builder builder) {
                        return world.createComposition(builder, C1.class, C2.class, C3.class, C4.class, C5.class);
                    }
                }

                @Nested
                class Of6Test extends AbstractCompositionTest<Composition.Of6<C1, C2, C3, C4, C5, C6>> {
                    @Override
                    protected Composition.Of6<C1, C2, C3, C4, C5, C6> createComposition(Composition.Builder builder) {
                        return world.createComposition(builder, C1.class, C2.class, C3.class, C4.class, C5.class, C6.class);
                    }
                }

                @Nested
                class Of7Test extends AbstractCompositionTest<Composition.Of7<C1, C2, C3, C4, C5, C6, C7>> {
                    @Override
                    protected Composition.Of7<C1, C2, C3, C4, C5, C6, C7> createComposition(Composition.Builder builder) {
                        return world.createComposition(builder, C1.class, C2.class, C3.class, C4.class, C5.class, C6.class, C7.class);
                    }
                }

                @Nested
                class Of8Test extends AbstractCompositionTest<Composition.Of8<C1, C2, C3, C4, C5, C6, C7, C8>> {
                    @Override
                    protected Composition.Of8<C1, C2, C3, C4, C5, C6, C7, C8> createComposition(Composition.Builder builder) {
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

    @Nested
    class InsertedTest {

        @Test
        void testInsertedEntities() {
            // Setup
            var composition = compositionManager.create(EMPTY, spec -> new IntBag(4));

            // Insert entity
            compositionManager.inserted(EMPTY_MASK, 42);
            compositionManager.inserted(EMPTY_MASK, 1337);
            compositionManager.inserted(EMPTY_MASK, 9001);

            // Verify
            var entities = new IntBag(3);
            composition.process(entities::add);

            assertThat(entities.getSize()).as("size").isEqualTo(3);
            assertThat(entities.contains(42)).as("contains 42").isTrue();
            assertThat(entities.contains(1337)).as("contains 1337").isTrue();
            assertThat(entities.contains(9001)).as("contains 9001").isTrue();
        }

        @Test
        void testInsertedEntities_InterestedOnly() {
            // Setup
            var all = Composition.all(C1.class);

            var components42 = new BitVector();
            components42.set(component1Id);
            components42.set(component2Id);

            var components1337 = new BitVector();
            components1337.set(component2Id);

            var composition = compositionManager.create(all, spec -> new IntBag(4));

            // Insert entity
            compositionManager.inserted(mask(components42), 42);
            compositionManager.inserted(mask(components1337), 1337);

            // Verify
            var entities = new IntBag(1);
            composition.process(entities::add);

            assertThat(entities.getSize()).as("size").isEqualTo(1);
            assertThat(entities.contains(42)).as("contains 42").isTrue();
        }

        @Test
        void testInsertedCallback() {
            // Setup
            var entities = new IntBag(4);
            var inserted = new IntBag(2);

            var composition = compositionManager.create(EMPTY, spec -> entities);

            // Add callback
            composition.inserted(inserted::add);
            assertThat(inserted.getSize()).as("size").isZero();

            // Insert entity
            compositionManager.inserted(EMPTY_MASK, 42);
            compositionManager.inserted(EMPTY_MASK, 1337);
            compositionManager.inserted(EMPTY_MASK, 9001);

            // Verify
            assertThat(inserted.getSize()).as("size").isEqualTo(3);
            assertThat(inserted.contains(42)).as("contains 42").isTrue();
            assertThat(inserted.contains(1337)).as("contains 1337").isTrue();
            assertThat(inserted.contains(9001)).as("contains 9001").isTrue();
        }

        @Test
        void testMultipleInsertedCallbacks() {
            // Setup
            var entities = new IntBag(4);
            var inserted1 = new IntBag(2);
            var inserted2 = new ArrayList<Integer>();
            var inserted3 = new HashSet<Integer>();

            var composition = compositionManager.create(EMPTY, spec -> entities);

            // Add callbacks
            composition.inserted(inserted1::add);
            assertThat(inserted1.getSize()).as("size").isZero();

            composition.inserted(inserted2::add);
            assertThat(inserted2).isEmpty();

            composition.inserted(inserted3::add);
            assertThat(inserted3).isEmpty();

            // Insert entity
            compositionManager.inserted(EMPTY_MASK, 42);
            compositionManager.inserted(EMPTY_MASK, 1337);
            compositionManager.inserted(EMPTY_MASK, 9001);

            // Verify
            assertThat(inserted1.getSize()).as("size").isEqualTo(3);
            assertThat(inserted1.contains(42)).as("contains 42").isTrue();
            assertThat(inserted1.contains(1337)).as("contains 1337").isTrue();
            assertThat(inserted1.contains(9001)).as("contains 9001").isTrue();

            assertThat(inserted2).containsExactlyInAnyOrder(42, 1337, 9001);

            assertThat(inserted3).containsExactlyInAnyOrder(42, 1337, 9001);
        }

        @Test
        void testInsertedCallback_InterestedOnly() {
            // Setup
            var all = Composition.all(C1.class);

            var components42 = new BitVector();
            components42.set(component1Id);
            components42.set(component2Id);

            var components1337 = new BitVector();
            components1337.set(component2Id);

            var entities = new IntBag(4);
            var inserted = new IntBag(2);

            var composition = compositionManager.create(all, spec -> entities);

            // Add callback
            composition.inserted(inserted::add);
            assertThat(inserted.getSize()).as("size").isZero();

            // Insert entity
            compositionManager.inserted(mask(components42), 42);
            compositionManager.inserted(mask(components1337), 1337);

            // Verify
            assertThat(inserted.getSize()).as("size").isEqualTo(1);
            assertThat(inserted.contains(42)).as("contains 42").isTrue();
        }

    }

    @Nested
    class UpdatedTest {

        @Test
        void testUpdatedEntities_WhenNullPreviousComposition_Throws() {
            var componentMask = componentMaskManager.getComponentMask(C1.class);

            assertThatThrownBy(() -> compositionManager.updated(42, null, componentMask)).isInstanceOf(NullPointerException.class);
        }

        @Test
        void testUpdatedEntities_WhenNullNewComposition_Throws() {
            var componentMask = componentMaskManager.getComponentMask(C1.class);

            assertThatThrownBy(() -> compositionManager.updated(42, componentMask, null)).isInstanceOf(NullPointerException.class);
        }

        @Test
        void testUpdatedEntities_WhenPreviousComposition_RemovesEntities() {
            // Setup
            var entities = new HashSet<Integer>();
            bagManager.ensureEntitySize(10000);

            var composition1 = compositionManager.create(Composition.all(C1.class), spec -> bagManager.createEntityIntBag());
            var composition2 = compositionManager.create(Composition.all(C2.class), spec -> bagManager.createEntityIntBag());

            var componentMask1 = componentMaskManager.getComponentMask(C1.class);
            var componentMask12 = componentMaskManager.getComponentMask(C1.class, C2.class);
            var componentMask2 = componentMaskManager.getComponentMask(C2.class);

            var mask42 = componentMask1;
            var mask1337 = componentMask2;
            var mask9001 = componentMask1;

            compositionManager.inserted(mask42, 42);
            compositionManager.inserted(mask1337, 1337);
            compositionManager.inserted(mask9001, 9001);

            // Insert entity
            compositionManager.updated(42, mask42, componentMask12);
            compositionManager.updated(1337, mask1337, componentMask12);
            compositionManager.updated(9001, mask9001, componentMask2);

            // Verify
            composition1.process(entities::add);
            assertThat(entities).containsExactlyInAnyOrder(42, 1337);

            entities.clear();
            composition2.process(entities::add);
            assertThat(entities).containsExactlyInAnyOrder(42, 1337, 9001);
        }

        @Test
        void testUpdatedEntities_WhenPreviousComposition_CallsRemoved() {
            // Setup
            bagManager.ensureEntitySize(10000);

            var componentMask1 = componentMaskManager.getComponentMask(C1.class);
            var componentMask12 = componentMaskManager.getComponentMask(C1.class, C2.class);
            var componentMask2 = componentMaskManager.getComponentMask(C2.class);
            var componentMask3 = componentMaskManager.getComponentMask(C3.class);

            var composition1 = compositionManager.create(Composition.all(C1.class), spec -> bagManager.createEntityIntBag());
            var composition2 = compositionManager.create(Composition.all(C2.class), spec -> bagManager.createEntityIntBag());
            var composition3 = compositionManager.create(Composition.all(C3.class), spec -> bagManager.createEntityIntBag());

            var mask7 = componentMask2;
            var mask42 = componentMask1;
            var mask1337 = componentMask2;
            var mask9001 = componentMask12;

            compositionManager.inserted(mask7, 7);
            compositionManager.inserted(mask42, 42);
            compositionManager.inserted(mask1337, 1337);
            compositionManager.inserted(mask9001, 9001);

            var removed1 = new HashSet<Integer>();
            composition1.removed(removed1::add);

            var removed2 = new HashSet<Integer>();
            composition2.removed(removed2::add);

            var removed3 = new HashSet<Integer>();
            composition3.removed(removed3::add);

            // Insert entity
            compositionManager.updated(7, mask7, componentMask1);
            compositionManager.updated(42, mask42, componentMask12);
            compositionManager.updated(1337, mask1337, componentMask12);
            compositionManager.updated(9001, mask9001, componentMask3);

            // Verify
            assertThat(removed1).containsExactlyInAnyOrder(9001);
            assertThat(removed2).containsExactlyInAnyOrder(7, 9001);
            assertThat(removed3).isEmpty();
        }

    }

    @Nested
    class RemovedTest {

        @Test
        void testRemovedEntities() {
            // Setup
            var initial = new IntBag(4);
            initial.add(42);
            initial.add(1337);
            initial.add(31337);
            initial.add(9001);

            var componentMask = componentMaskManager.getComponentMask(C1.class);
            var composition = compositionManager.create(EMPTY, spec -> initial);

            // Insert entity
            compositionManager.removed(42, componentMask);
            compositionManager.removed(1337, componentMask);
            compositionManager.removed(9001, componentMask);
            compositionManager.removed(9002, componentMask);

            // Verify
            var entities = new IntBag(4);
            composition.process(entities::add);

            assertThat(entities.getSize()).as("size").isEqualTo(1);
            assertThat(entities.getData()).containsOnly(0, 31337);
        }

        @Test
        void testRemovedCallback() {
            // Setup
            var initial = new IntBag(4);
            initial.add(42);
            initial.add(1337);
            initial.add(31337);

            var removed = new IntBag(2);

            var componentMask = componentMaskManager.getComponentMask(C1.class);
            var composition = compositionManager.create(EMPTY, spec -> initial);

            // Add callback
            composition.removed(removed::add);
            assertThat(removed.getSize()).as("size").isZero();

            // Insert entity
            compositionManager.removed(42, componentMask);
            compositionManager.removed(1337, componentMask);
            compositionManager.removed(9001, componentMask);

            // Verify
            assertThat(removed.getSize()).as("size").isEqualTo(2);
            assertThat(removed.contains(42)).as("contains 42").isTrue();
            assertThat(removed.contains(1337)).as("contains 1337").isTrue();
        }

        @Test
        void testMultipleRemovedCallbacks() {
            // Setup
            var initial = new IntBag(4);
            initial.add(42);
            initial.add(1337);
            initial.add(31337);

            var removed1 = new IntBag(2);
            var removed2 = new ArrayList<Integer>();
            var removed3 = new HashSet<Integer>();

            var componentMask = componentMaskManager.getComponentMask(C1.class);
            var composition = compositionManager.create(EMPTY, spec -> initial);

            // Add callbacks
            composition.removed(removed1::add);
            assertThat(removed1.getSize()).as("size").isZero();

            composition.removed(removed2::add);
            assertThat(removed2).isEmpty();

            composition.removed(removed3::add);
            assertThat(removed3).isEmpty();

            // Insert entity
            compositionManager.removed(42, componentMask);
            compositionManager.removed(1337, componentMask);
            compositionManager.removed(9001, componentMask);

            // Verify
            assertThat(removed1.getSize()).as("size").isEqualTo(2);
            assertThat(removed1.contains(42)).as("contains 42").isTrue();
            assertThat(removed1.contains(1337)).as("contains 1337").isTrue();

            assertThat(removed2).containsExactlyInAnyOrder(42, 1337);

            assertThat(removed3).containsExactlyInAnyOrder(42, 1337);
        }

    }

    @Nested
    class Composition1Test extends AbstractCompositionNTest {

        @Override
        Composition.Of1<C1> composition(Composition.Builder builder) {
            return world.createComposition(builder, C1.class);
        }

        @Test
        void testProcessOtherEntity() {
            composition(builder1).process(expected1[0], (entityId, component1) -> {
                assertThat(component1).isNotNull();
            });

            composition(builder8).process(expected8[0], (entityId, component1) -> {
                assertThat(component1).isNotNull();
            });
        }

        @Test
        void testProcessOneComponent() {
            var composition = composition(builder1);

            var processed = new IntBag(expected1.length);
            composition.process((entityId, component1) -> {
                processed.add(entityId);

                assertThat(component1).isNotNull();
            });

            assertThat(processed.getSize()).isEqualTo(expected1.length);
            assertThat(processed.getData()).containsExactlyInAnyOrder(expected1);
        }

        @Test
        void testProcessAllComponents() {
            var composition = composition(builder8);

            var processed = new IntBag(expected8.length);
            composition.process((entityId, component1) -> {
                processed.add(entityId);

                assertThat(component1).isNotNull();
            });

            assertThat(processed.getSize()).isEqualTo(expected8.length);
            assertThat(processed.getData()).containsExactlyInAnyOrder(expected8);
        }

        @Test
        void testProcessVaryingComponents() {
            var composition = composition(builderAll);

            var processed = new IntBag(expected1.length + expected8.length);
            composition.process((entityId, component1) -> {
                processed.add(entityId);

                assertThat(component1).isNotNull();
            });

            assertThat(processed.getSize()).isEqualTo(expected1.length + expected8.length);
            assertThat(processed.getData()).contains(expected1);
            assertThat(processed.getData()).contains(expected8);
        }

        @Test
        void testInserted() {
            var composition = composition(builder1);

            var inserted = new IntBag(7);
            composition.inserted((entityId, component1) -> {
                inserted.add(entityId);

                assertThat(component1).isNotNull();
            });

            var ids = archetype1.createBatch(7, (i, init) -> init.initialize(new C1()));

            assertThat(inserted.getSize()).isEqualTo(7);
            assertThat(inserted.getData()).containsExactlyInAnyOrder(ids);
        }

        @Test
        void testRemoved() {
            var composition = composition(builder1);

            var removed = new IntBag(expected1.length);
            composition.removed((entityId, component1) -> {
                removed.add(entityId);

                assertThat(component1).isNotNull();
            });

            for (int id : expected1) {
                world.deleteEntity(id);
            }
            world.process();

            assertThat(removed.getSize()).isEqualTo(expected1.length);
            assertThat(removed.getData()).containsExactlyInAnyOrder(expected1);
        }

    }

    @Nested
    class Composition2Test extends AbstractCompositionNTest {

        @Override
        Composition.Of2<C1, C2> composition(Composition.Builder builder) {
            return world.createComposition(builder, C1.class, C2.class);
        }

        @Test
        void testProcessOtherEntity() {
            composition(builder1).process(expected1[0], (entityId, component1, component2) -> {
                assertThat(component1).isNotNull();
                assertThat(component2).isNull();
            });

            composition(builder8).process(expected8[0], (entityId, component1, component2) -> {
                assertThat(component1).isNotNull();
                assertThat(component2).isNotNull();
            });
        }

        @Test
        void testProcessOneComponent() {
            var composition = composition(builder1);

            var processed = new IntBag(expected1.length);
            composition.process((entityId, component1, component2) -> {
                processed.add(entityId);

                assertThat(component1).isNotNull();
                assertThat(component2).isNull();
            });

            assertThat(processed.getSize()).isEqualTo(expected1.length);
            assertThat(processed.getData()).containsExactlyInAnyOrder(expected1);
        }

        @Test
        void testProcessAllComponents() {
            var composition = composition(builder8);

            var processed = new IntBag(expected8.length);
            composition.process((entityId, component1, component2) -> {
                processed.add(entityId);

                assertThat(component1).isNotNull();
                assertThat(component2).isNotNull();
            });

            assertThat(processed.getSize()).isEqualTo(expected8.length);
            assertThat(processed.getData()).containsExactlyInAnyOrder(expected8);
        }

        @Test
        void testProcessVaryingComponents() {
            var composition = composition(builderAll);

            var processed = new IntBag(expected1.length + expected8.length);
            composition.process((entityId, component1, component2) -> {
                processed.add(entityId);

                assertThat(component1).isNotNull();
            });

            assertThat(processed.getSize()).isEqualTo(expected1.length + expected8.length);
            assertThat(processed.getData()).contains(expected1);
            assertThat(processed.getData()).contains(expected8);
        }

        @Test
        void testInserted() {
            var composition = composition(builder1);

            var inserted = new IntBag(7);
            composition.inserted((entityId, component1, component2) -> {
                inserted.add(entityId);

                assertThat(component1).isNotNull();
                assertThat(component2).isNull();
            });

            var ids = archetype1.createBatch(7, (i, init) -> init.initialize(new C1()));

            assertThat(inserted.getSize()).isEqualTo(7);
            assertThat(inserted.getData()).containsExactlyInAnyOrder(ids);
        }

        @Test
        void testRemoved() {
            var composition = composition(builder1);

            var removed = new IntBag(expected1.length);
            composition.removed((entityId, component1, component2) -> {
                removed.add(entityId);

                assertThat(component1).isNotNull();
                assertThat(component2).isNull();
            });

            for (int id : expected1) {
                world.deleteEntity(id);
            }
            world.process();

            assertThat(removed.getSize()).isEqualTo(expected1.length);
            assertThat(removed.getData()).containsExactlyInAnyOrder(expected1);
        }

    }

    @Nested
    class Composition3Test extends AbstractCompositionNTest {

        @Override
        Composition.Of3<C1, C2, C3> composition(Composition.Builder builder) {
            return world.createComposition(builder, C1.class, C2.class, C3.class);
        }

        @Test
        void testProcessOtherEntity() {
            composition(builder1).process(expected1[0], (entityId, component1, component2, component3) -> {
                assertThat(component1).isNotNull();
                assertThat(component2).isNull();
                assertThat(component3).isNull();
            });

            composition(builder8).process(expected8[0], (entityId, component1, component2, component3) -> {
                assertThat(component1).isNotNull();
                assertThat(component2).isNotNull();
                assertThat(component3).isNotNull();
            });
        }

        @Test
        void testProcessOneComponent() {
            var composition = composition(builder1);

            var processed = new IntBag(expected1.length);
            composition.process((entityId, component1, component2, component3) -> {
                processed.add(entityId);

                assertThat(component1).isNotNull();
                assertThat(component2).isNull();
                assertThat(component3).isNull();
            });

            assertThat(processed.getSize()).isEqualTo(expected1.length);
            assertThat(processed.getData()).containsExactlyInAnyOrder(expected1);
        }

        @Test
        void testProcessAllComponents() {
            var composition = composition(builder8);

            var processed = new IntBag(expected8.length);
            composition.process((entityId, component1, component2, component3) -> {
                processed.add(entityId);

                assertThat(component1).isNotNull();
                assertThat(component2).isNotNull();
                assertThat(component3).isNotNull();
            });

            assertThat(processed.getSize()).isEqualTo(expected8.length);
            assertThat(processed.getData()).containsExactlyInAnyOrder(expected8);
        }

        @Test
        void testProcessVaryingComponents() {
            var composition = composition(builderAll);

            var processed = new IntBag(expected1.length + expected8.length);
            composition.process((entityId, component1, component2, component3) -> {
                processed.add(entityId);

                assertThat(component1).isNotNull();
            });

            assertThat(processed.getSize()).isEqualTo(expected1.length + expected8.length);
            assertThat(processed.getData()).contains(expected1);
            assertThat(processed.getData()).contains(expected8);
        }

        @Test
        void testInserted() {
            var composition = composition(builder1);

            var inserted = new IntBag(7);
            composition.inserted((entityId, component1, component2, component3) -> {
                inserted.add(entityId);

                assertThat(component1).isNotNull();
                assertThat(component2).isNull();
                assertThat(component3).isNull();
            });

            var ids = archetype1.createBatch(7, (i, init) -> init.initialize(new C1()));

            assertThat(inserted.getSize()).isEqualTo(7);
            assertThat(inserted.getData()).containsExactlyInAnyOrder(ids);
        }

        @Test
        void testRemoved() {
            var composition = composition(builder1);

            var removed = new IntBag(expected1.length);
            composition.removed((entityId, component1, component2, component3) -> {
                removed.add(entityId);

                assertThat(component1).isNotNull();
                assertThat(component2).isNull();
                assertThat(component3).isNull();
            });

            for (int id : expected1) {
                world.deleteEntity(id);
            }
            world.process();

            assertThat(removed.getSize()).isEqualTo(expected1.length);
            assertThat(removed.getData()).containsExactlyInAnyOrder(expected1);
        }

    }

    @Nested
    class Composition4Test extends AbstractCompositionNTest {

        @Override
        Composition.Of4<C1, C2, C3, C4> composition(Composition.Builder builder) {
            return world.createComposition(builder, C1.class, C2.class, C3.class, C4.class);
        }

        @Test
        void testProcessOtherEntity() {
            composition(builder1).process(expected1[0], (entityId, component1, component2, component3, component4) -> {
                assertThat(component1).isNotNull();
                assertThat(component2).isNull();
                assertThat(component3).isNull();
                assertThat(component4).isNull();
            });

            composition(builder8).process(expected8[0], (entityId, component1, component2, component3, component4) -> {
                assertThat(component1).isNotNull();
                assertThat(component2).isNotNull();
                assertThat(component3).isNotNull();
                assertThat(component4).isNotNull();
            });
        }

        @Test
        void testProcessOneComponent() {
            var composition = composition(builder1);

            var processed = new IntBag(expected1.length);
            composition.process((entityId, component1, component2, component3, component4) -> {
                processed.add(entityId);

                assertThat(component1).isNotNull();
                assertThat(component2).isNull();
                assertThat(component3).isNull();
                assertThat(component4).isNull();
            });

            assertThat(processed.getSize()).isEqualTo(expected1.length);
            assertThat(processed.getData()).containsExactlyInAnyOrder(expected1);
        }

        @Test
        void testProcessAllComponents() {
            var composition = composition(builder8);

            var processed = new IntBag(expected8.length);
            composition.process((entityId, component1, component2, component3, component4) -> {
                processed.add(entityId);

                assertThat(component1).isNotNull();
                assertThat(component2).isNotNull();
                assertThat(component3).isNotNull();
                assertThat(component4).isNotNull();
            });

            assertThat(processed.getSize()).isEqualTo(expected8.length);
            assertThat(processed.getData()).containsExactlyInAnyOrder(expected8);
        }

        @Test
        void testProcessVaryingComponents() {
            var composition = composition(builderAll);

            var processed = new IntBag(expected1.length + expected8.length);
            composition.process((entityId, component1, component2, component3, component4) -> {
                processed.add(entityId);

                assertThat(component1).isNotNull();
            });

            assertThat(processed.getSize()).isEqualTo(expected1.length + expected8.length);
            assertThat(processed.getData()).contains(expected1);
            assertThat(processed.getData()).contains(expected8);
        }

        @Test
        void testInserted() {
            var composition = composition(builder1);

            var inserted = new IntBag(7);
            composition.inserted((entityId, component1, component2, component3, component4) -> {
                inserted.add(entityId);

                assertThat(component1).isNotNull();
                assertThat(component2).isNull();
                assertThat(component3).isNull();
                assertThat(component4).isNull();
            });

            var ids = archetype1.createBatch(7, (i, init) -> init.initialize(new C1()));

            assertThat(inserted.getSize()).isEqualTo(7);
            assertThat(inserted.getData()).containsExactlyInAnyOrder(ids);
        }

        @Test
        void testRemoved() {
            var composition = composition(builder1);

            var removed = new IntBag(expected1.length);
            composition.removed((entityId, component1, component2, component3, component4) -> {
                removed.add(entityId);

                assertThat(component1).isNotNull();
                assertThat(component2).isNull();
                assertThat(component3).isNull();
                assertThat(component4).isNull();
            });

            for (int id : expected1) {
                world.deleteEntity(id);
            }
            world.process();

            assertThat(removed.getSize()).isEqualTo(expected1.length);
            assertThat(removed.getData()).containsExactlyInAnyOrder(expected1);
        }

    }

    @Nested
    class Composition5Test extends AbstractCompositionNTest {

        @Override
        Composition.Of5<C1, C2, C3, C4, C5> composition(Composition.Builder builder) {
            return world.createComposition(builder, C1.class, C2.class, C3.class, C4.class, C5.class);
        }

        @Test
        void testProcessOtherEntity() {
            composition(builder1).process(expected1[0], (entityId, component1, component2, component3, component4, component5) -> {
                assertThat(component1).isNotNull();
                assertThat(component2).isNull();
                assertThat(component3).isNull();
                assertThat(component4).isNull();
                assertThat(component5).isNull();
            });

            composition(builder8).process(expected8[0], (entityId, component1, component2, component3, component4, component5) -> {
                assertThat(component1).isNotNull();
                assertThat(component2).isNotNull();
                assertThat(component3).isNotNull();
                assertThat(component4).isNotNull();
                assertThat(component5).isNotNull();
            });
        }

        @Test
        void testProcessOneComponent() {
            var composition = composition(builder1);

            var processed = new IntBag(expected1.length);
            composition.process((entityId, component1, component2, component3, component4, component5) -> {
                processed.add(entityId);

                assertThat(component1).isNotNull();
                assertThat(component2).isNull();
                assertThat(component3).isNull();
                assertThat(component4).isNull();
                assertThat(component5).isNull();
            });

            assertThat(processed.getSize()).isEqualTo(expected1.length);
            assertThat(processed.getData()).containsExactlyInAnyOrder(expected1);
        }

        @Test
        void testProcessAllComponents() {
            var composition = composition(builder8);

            var processed = new IntBag(expected8.length);
            composition.process((entityId, component1, component2, component3, component4, component5) -> {
                processed.add(entityId);

                assertThat(component1).isNotNull();
                assertThat(component2).isNotNull();
                assertThat(component3).isNotNull();
                assertThat(component4).isNotNull();
                assertThat(component5).isNotNull();
            });

            assertThat(processed.getSize()).isEqualTo(expected8.length);
            assertThat(processed.getData()).containsExactlyInAnyOrder(expected8);
        }

        @Test
        void testProcessVaryingComponents() {
            var composition = composition(builderAll);

            var processed = new IntBag(expected1.length + expected8.length);
            composition.process((entityId, component1, component2, component3, component4, component5) -> {
                processed.add(entityId);

                assertThat(component1).isNotNull();
            });

            assertThat(processed.getSize()).isEqualTo(expected1.length + expected8.length);
            assertThat(processed.getData()).contains(expected1);
            assertThat(processed.getData()).contains(expected8);
        }

        @Test
        void testInserted() {
            var composition = composition(builder1);

            var inserted = new IntBag(7);
            composition.inserted((entityId, component1, component2, component3, component4, component5) -> {
                inserted.add(entityId);

                assertThat(component1).isNotNull();
                assertThat(component2).isNull();
                assertThat(component3).isNull();
                assertThat(component4).isNull();
                assertThat(component5).isNull();
            });

            var ids = archetype1.createBatch(7, (i, init) -> init.initialize(new C1()));

            assertThat(inserted.getSize()).isEqualTo(7);
            assertThat(inserted.getData()).containsExactlyInAnyOrder(ids);
        }

        @Test
        void testRemoved() {
            var composition = composition(builder1);

            var removed = new IntBag(expected1.length);
            composition.removed((entityId, component1, component2, component3, component4, component5) -> {
                removed.add(entityId);

                assertThat(component1).isNotNull();
                assertThat(component2).isNull();
                assertThat(component3).isNull();
                assertThat(component4).isNull();
                assertThat(component5).isNull();
            });

            for (int id : expected1) {
                world.deleteEntity(id);
            }
            world.process();

            assertThat(removed.getSize()).isEqualTo(expected1.length);
            assertThat(removed.getData()).containsExactlyInAnyOrder(expected1);
        }

    }

    @Nested
    class Composition6Test extends AbstractCompositionNTest {

        @Override
        Composition.Of6<C1, C2, C3, C4, C5, C6> composition(Composition.Builder builder) {
            return world.createComposition(builder, C1.class, C2.class, C3.class, C4.class, C5.class, C6.class);
        }

        @Test
        void testProcessOtherEntity() {
            composition(builder1).process(expected1[0], (entityId, component1, component2, component3, component4, component5, component6) -> {
                assertThat(component1).isNotNull();
                assertThat(component2).isNull();
                assertThat(component3).isNull();
                assertThat(component4).isNull();
                assertThat(component5).isNull();
                assertThat(component6).isNull();
            });

            composition(builder8).process(expected8[0], (entityId, component1, component2, component3, component4, component5, component6) -> {
                assertThat(component1).isNotNull();
                assertThat(component2).isNotNull();
                assertThat(component3).isNotNull();
                assertThat(component4).isNotNull();
                assertThat(component5).isNotNull();
                assertThat(component6).isNotNull();
            });
        }

        @Test
        void testProcessOneComponent() {
            var composition = composition(builder1);

            var processed = new IntBag(expected1.length);
            composition.process((entityId, component1, component2, component3, component4, component5, component6) -> {
                processed.add(entityId);

                assertThat(component1).isNotNull();
                assertThat(component2).isNull();
                assertThat(component3).isNull();
                assertThat(component4).isNull();
                assertThat(component5).isNull();
                assertThat(component6).isNull();
            });

            assertThat(processed.getSize()).isEqualTo(expected1.length);
            assertThat(processed.getData()).containsExactlyInAnyOrder(expected1);
        }

        @Test
        void testProcessAllComponents() {
            var composition = composition(builder8);

            var processed = new IntBag(expected8.length);
            composition.process((entityId, component1, component2, component3, component4, component5, component6) -> {
                processed.add(entityId);

                assertThat(component1).isNotNull();
                assertThat(component2).isNotNull();
                assertThat(component3).isNotNull();
                assertThat(component4).isNotNull();
                assertThat(component5).isNotNull();
                assertThat(component6).isNotNull();
            });

            assertThat(processed.getSize()).isEqualTo(expected8.length);
            assertThat(processed.getData()).containsExactlyInAnyOrder(expected8);
        }

        @Test
        void testProcessVaryingComponents() {
            var composition = composition(builderAll);

            var processed = new IntBag(expected1.length + expected8.length);
            composition.process((entityId, component1, component2, component3, component4, component5, component6) -> {
                processed.add(entityId);

                assertThat(component1).isNotNull();
            });

            assertThat(processed.getSize()).isEqualTo(expected1.length + expected8.length);
            assertThat(processed.getData()).contains(expected1);
            assertThat(processed.getData()).contains(expected8);
        }

        @Test
        void testInserted() {
            var composition = composition(builder1);

            var inserted = new IntBag(7);
            composition.inserted((entityId, component1, component2, component3, component4, component5, component6) -> {
                inserted.add(entityId);

                assertThat(component1).isNotNull();
                assertThat(component2).isNull();
                assertThat(component3).isNull();
                assertThat(component4).isNull();
                assertThat(component5).isNull();
                assertThat(component6).isNull();
            });

            var ids = archetype1.createBatch(7, (i, init) -> init.initialize(new C1()));

            assertThat(inserted.getSize()).isEqualTo(7);
            assertThat(inserted.getData()).containsExactlyInAnyOrder(ids);
        }

        @Test
        void testRemoved() {
            var composition = composition(builder1);

            var removed = new IntBag(expected1.length);
            composition.removed((entityId, component1, component2, component3, component4, component5, component6) -> {
                removed.add(entityId);

                assertThat(component1).isNotNull();
                assertThat(component2).isNull();
                assertThat(component3).isNull();
                assertThat(component4).isNull();
                assertThat(component5).isNull();
                assertThat(component6).isNull();
            });

            for (int id : expected1) {
                world.deleteEntity(id);
            }
            world.process();

            assertThat(removed.getSize()).isEqualTo(expected1.length);
            assertThat(removed.getData()).containsExactlyInAnyOrder(expected1);
        }

    }

    @Nested
    class Composition7Test extends AbstractCompositionNTest {

        @Override
        Composition.Of7<C1, C2, C3, C4, C5, C6, C7> composition(Composition.Builder builder) {
            return world.createComposition(builder, C1.class, C2.class, C3.class, C4.class, C5.class, C6.class, C7.class);
        }

        @Test
        void testProcessOtherEntity() {
            composition(builder1).process(expected1[0], (entityId, component1, component2, component3, component4, component5, component6, component7) -> {
                assertThat(component1).isNotNull();
                assertThat(component2).isNull();
                assertThat(component3).isNull();
                assertThat(component4).isNull();
                assertThat(component5).isNull();
                assertThat(component6).isNull();
                assertThat(component7).isNull();
            });

            composition(builder8).process(expected8[0], (entityId, component1, component2, component3, component4, component5, component6, component7) -> {
                assertThat(component1).isNotNull();
                assertThat(component2).isNotNull();
                assertThat(component3).isNotNull();
                assertThat(component4).isNotNull();
                assertThat(component5).isNotNull();
                assertThat(component6).isNotNull();
                assertThat(component7).isNotNull();
            });
        }

        @Test
        void testProcessOneComponent() {
            var composition = composition(builder1);

            var processed = new IntBag(expected1.length);
            composition.process((entityId, component1, component2, component3, component4, component5, component6, component7) -> {
                processed.add(entityId);

                assertThat(component1).isNotNull();
                assertThat(component2).isNull();
                assertThat(component3).isNull();
                assertThat(component4).isNull();
                assertThat(component5).isNull();
                assertThat(component6).isNull();
                assertThat(component7).isNull();
            });

            assertThat(processed.getSize()).isEqualTo(expected1.length);
            assertThat(processed.getData()).containsExactlyInAnyOrder(expected1);
        }

        @Test
        void testProcessAllComponents() {
            var composition = composition(builder8);

            var processed = new IntBag(expected8.length);
            composition.process((entityId, component1, component2, component3, component4, component5, component6, component7) -> {
                processed.add(entityId);

                assertThat(component1).isNotNull();
                assertThat(component2).isNotNull();
                assertThat(component3).isNotNull();
                assertThat(component4).isNotNull();
                assertThat(component5).isNotNull();
                assertThat(component6).isNotNull();
                assertThat(component7).isNotNull();
            });

            assertThat(processed.getSize()).isEqualTo(expected8.length);
            assertThat(processed.getData()).containsExactlyInAnyOrder(expected8);
        }

        @Test
        void testProcessVaryingComponents() {
            var composition = composition(builderAll);

            var processed = new IntBag(expected1.length + expected8.length);
            composition.process((entityId, component1, component2, component3, component4, component5, component6, component7) -> {
                processed.add(entityId);

                assertThat(component1).isNotNull();
            });

            assertThat(processed.getSize()).isEqualTo(expected1.length + expected8.length);
            assertThat(processed.getData()).contains(expected1);
            assertThat(processed.getData()).contains(expected8);
        }

        @Test
        void testInserted() {
            var composition = composition(builder1);

            var inserted = new IntBag(7);
            composition.inserted((entityId, component1, component2, component3, component4, component5, component6, component7) -> {
                inserted.add(entityId);

                assertThat(component1).isNotNull();
                assertThat(component2).isNull();
                assertThat(component3).isNull();
                assertThat(component4).isNull();
                assertThat(component5).isNull();
                assertThat(component6).isNull();
                assertThat(component7).isNull();
            });

            var ids = archetype1.createBatch(7, (i, init) -> init.initialize(new C1()));

            assertThat(inserted.getSize()).isEqualTo(7);
            assertThat(inserted.getData()).containsExactlyInAnyOrder(ids);
        }

        @Test
        void testRemoved() {
            var composition = composition(builder1);

            var removed = new IntBag(expected1.length);
            composition.removed((entityId, component1, component2, component3, component4, component5, component6, component7) -> {
                removed.add(entityId);

                assertThat(component1).isNotNull();
                assertThat(component2).isNull();
                assertThat(component3).isNull();
                assertThat(component4).isNull();
                assertThat(component5).isNull();
                assertThat(component6).isNull();
                assertThat(component7).isNull();
            });

            for (int id : expected1) {
                world.deleteEntity(id);
            }
            world.process();

            assertThat(removed.getSize()).isEqualTo(expected1.length);
            assertThat(removed.getData()).containsExactlyInAnyOrder(expected1);
        }

    }

    @Nested
    class Composition8Test extends AbstractCompositionNTest {

        @Override
        Composition.Of8<C1, C2, C3, C4, C5, C6, C7, C8> composition(Composition.Builder builder) {
            return world.createComposition(builder, C1.class, C2.class, C3.class, C4.class, C5.class, C6.class, C7.class, C8.class);
        }

        @Test
        void testProcessOtherEntity() {
            composition(builder1).process(expected1[0], (entityId, component1, component2, component3, component4, component5, component6, component7, component8) -> {
                assertThat(component1).isNotNull();
                assertThat(component2).isNull();
                assertThat(component3).isNull();
                assertThat(component4).isNull();
                assertThat(component5).isNull();
                assertThat(component6).isNull();
                assertThat(component7).isNull();
                assertThat(component8).isNull();
            });

            composition(builder8).process(expected8[0], (entityId, component1, component2, component3, component4, component5, component6, component7, component8) -> {
                assertThat(component1).isNotNull();
                assertThat(component2).isNotNull();
                assertThat(component3).isNotNull();
                assertThat(component4).isNotNull();
                assertThat(component5).isNotNull();
                assertThat(component6).isNotNull();
                assertThat(component7).isNotNull();
                assertThat(component8).isNotNull();
            });
        }

        @Test
        void testProcessOneComponent() {
            var composition = composition(builder1);

            var processed = new IntBag(expected1.length);
            composition.process((entityId, component1, component2, component3, component4, component5, component6, component7, component8) -> {
                processed.add(entityId);

                assertThat(component1).isNotNull();
                assertThat(component2).isNull();
                assertThat(component3).isNull();
                assertThat(component4).isNull();
                assertThat(component5).isNull();
                assertThat(component6).isNull();
                assertThat(component7).isNull();
                assertThat(component8).isNull();
            });

            assertThat(processed.getSize()).isEqualTo(expected1.length);
            assertThat(processed.getData()).containsExactlyInAnyOrder(expected1);
        }

        @Test
        void testProcessAllComponents() {
            var composition = composition(builder8);

            var processed = new IntBag(expected8.length);
            composition.process((entityId, component1, component2, component3, component4, component5, component6, component7, component8) -> {
                processed.add(entityId);

                assertThat(component1).isNotNull();
                assertThat(component2).isNotNull();
                assertThat(component3).isNotNull();
                assertThat(component4).isNotNull();
                assertThat(component5).isNotNull();
                assertThat(component6).isNotNull();
                assertThat(component7).isNotNull();
                assertThat(component8).isNotNull();
            });

            assertThat(processed.getSize()).isEqualTo(expected8.length);
            assertThat(processed.getData()).containsExactlyInAnyOrder(expected8);
        }

        @Test
        void testProcessVaryingComponents() {
            var composition = composition(builderAll);

            var processed = new IntBag(expected1.length + expected8.length);
            composition.process((entityId, component1, component2, component3, component4, component5, component6, component7, component8) -> {
                processed.add(entityId);

                assertThat(component1).isNotNull();
            });

            assertThat(processed.getSize()).isEqualTo(expected1.length + expected8.length);
            assertThat(processed.getData()).contains(expected1);
            assertThat(processed.getData()).contains(expected8);
        }

        @Test
        void testInserted() {
            var composition = composition(builder1);

            var inserted = new IntBag(7);
            composition.inserted((entityId, component1, component2, component3, component4, component5, component6, component7, component8) -> {
                inserted.add(entityId);

                assertThat(component1).isNotNull();
                assertThat(component2).isNull();
                assertThat(component3).isNull();
                assertThat(component4).isNull();
                assertThat(component5).isNull();
                assertThat(component6).isNull();
                assertThat(component7).isNull();
                assertThat(component8).isNull();
            });

            var ids = archetype1.createBatch(7, (i, init) -> init.initialize(new C1()));

            assertThat(inserted.getSize()).isEqualTo(7);
            assertThat(inserted.getData()).containsExactlyInAnyOrder(ids);
        }

        @Test
        void testRemoved() {
            var composition = composition(builder1);

            var removed = new IntBag(expected1.length);
            composition.removed((entityId, component1, component2, component3, component4, component5, component6, component7, component8) -> {
                removed.add(entityId);

                assertThat(component1).isNotNull();
                assertThat(component2).isNull();
                assertThat(component3).isNull();
                assertThat(component4).isNull();
                assertThat(component5).isNull();
                assertThat(component6).isNull();
                assertThat(component7).isNull();
                assertThat(component8).isNull();
            });

            for (int id : expected1) {
                world.deleteEntity(id);
            }
            world.process();

            assertThat(removed.getSize()).isEqualTo(expected1.length);
            assertThat(removed.getData()).containsExactlyInAnyOrder(expected1);
        }

    }

    abstract class AbstractCompositionNTest {

        final Composition.Builder builder1 = Composition.all(C1.class).none(C8.class);
        final Composition.Builder builder8 = Composition.all(C8.class);
        final Composition.Builder builderAll = Composition.all(C1.class);

        Archetype.Of1<C1> archetype1;
        Archetype.Of8<C1, C2, C3, C4, C5, C6, C7, C8> archetype8;

        int[] expected1;
        int[] expected8;

        @BeforeEach
        void setup() {
            this.archetype1 = world.createArchetype(C1.class);
            this.expected1 = archetype1.createBatch(10, (i, init) -> init.initialize(new C1()));

            this.archetype8 = world.createArchetype(C1.class, C2.class, C3.class, C4.class, C5.class, C6.class, C7.class, C8.class);
            this.expected8 = archetype8.createBatch(7, (i, init) -> init.initialize(new C1(), new C2(), new C3(), new C4(), new C5(), new C6(), new C7(), new C8()));

            world.process();
        }

        abstract Composition composition(Composition.Builder builder);

        @Test
        void testCachedInstance() {
            var composition1 = composition(builder1);
            assertThat(composition(builder1)).isSameAs(composition1);
        }

        @Test
        void testRegularInserted() {
            var inserted = new ArrayList<Integer>();

            var composition = composition(builder1);
            composition.inserted(inserted::add);

            // Call
            var ids = archetype1.createBatch(5, (i, init) -> init.initialize(new C1()));

            // Verify
            assertThat(inserted).hasSize(ids.length);
            for (var id : ids) {
                assertThat(inserted).contains(id);
            }
        }

        @Test
        void testRegularRemoved() {
            var removed = new ArrayList<Integer>();

            var composition = composition(builder1);
            composition.removed(removed::add);

            // Call
            for (var id : expected1) {
                world.deleteEntity(id);
            }
            world.process();

            // Verify 
            assertThat(removed).hasSize(expected1.length);
            for (var id : expected1) {
                assertThat(removed).contains(id);
            }
        }

        @Test
        void testIsEmpty() {
            var composition1 = composition(builder1);
            var composition8 = composition(builder8);
            var compositionAll = composition(builderAll);
            var compositionEmpty = composition(Composition.none(C1.class));

            assertThat(composition1.isEmpty()).isFalse();
            assertThat(composition8.isEmpty()).isFalse();
            assertThat(compositionAll.isEmpty()).isFalse();
            assertThat(compositionEmpty.isEmpty()).isTrue();
        }

        @Test
        void testGetCount() {
            var composition1 = composition(builder1);
            var composition8 = composition(builder8);
            var compositionAll = composition(builderAll);

            assertThat(composition1.getCount()).isEqualTo(expected1.length);
            assertThat(composition8.getCount()).isEqualTo(expected8.length);
            assertThat(compositionAll.getCount()).isEqualTo(expected1.length + expected8.length);
        }

        @Test
        void testStream() {
            var composition1 = composition(builder1);
            var composition8 = composition(builder8);
            var compositionAll = composition(builderAll);

            assertThat(composition1.stream()).hasSize(expected1.length);
            assertThat(composition8.stream()).hasSize(expected8.length);
            assertThat(compositionAll.stream()).hasSize(expected1.length + expected8.length);
        }

        @Test
        void testParallelStream() {
            var composition1 = composition(builder1);
            var composition8 = composition(builder8);
            var compositionAll = composition(builderAll);

            assertThat(composition1.parallelStream()).hasSize(expected1.length);
            assertThat(composition8.parallelStream()).hasSize(expected8.length);
            assertThat(compositionAll.parallelStream()).hasSize(expected1.length + expected8.length);
        }

    }

    static ComponentMask mask(BitVector mask) {
        var lookup = new IntBag(64);
        mask.iterate(componentId -> lookup.set(componentId, 1));

        return new ComponentMask(MASK_ID.getAndIncrement(), mask, new ComponentData<?>[0], lookup, new Bag<>(ComponentMask.class), new Bag<>(ComponentMask.class));
    }

    private record C1() {
    }

    private record C2() {
    }

    private record C3() {
    }

    private record C4() {
    }

    private record C5() {
    }

    private record C6() {
    }

    private record C7() {
    }

    private record C8() {
    }

}
