package de.schosin.ecs.plugins.composition.manager;

import static de.schosin.ecs.api.components.ComponentType.wildcard;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;
import static org.assertj.core.api.Assertions.tuple;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import de.schosin.ecs.api.Pooled;
import de.schosin.ecs.api.components.ComponentSet;
import de.schosin.ecs.api.components.ComponentType;
import de.schosin.ecs.api.components.Components.PooledComponentMapper;
import de.schosin.ecs.api.components.Relation;
import de.schosin.ecs.api.components.Relation.ComponentRelation;
import de.schosin.ecs.api.components.Relation.EntityRelation;
import de.schosin.ecs.api.components.Relation.Exclusive;
import de.schosin.ecs.api.components.Result;
import de.schosin.ecs.api.components.Result.ComponentRelationResult;
import de.schosin.ecs.api.components.Result.ComponentResult;
import de.schosin.ecs.api.components.Result.EntityRelationResult;
import de.schosin.ecs.engine.entities.EntityManager.ComponentsPredicate;
import de.schosin.ecs.engine.events.builtin.EntityEvent.EntityInsertedEvent;
import de.schosin.ecs.engine.events.builtin.EntityEvent.EntityRemovedEvent;
import de.schosin.ecs.engine.events.builtin.EntityEvent.EntityUpdatedEvent;
import de.schosin.ecs.plugins.composition.BaseComposition;
import de.schosin.ecs.plugins.composition.Composition;
import de.schosin.ecs.plugins.composition.Composition.Builder;
import de.schosin.ecs.plugins.composition.Spec;
import de.schosin.ecs.plugins.composition.manager.CompositionManagerTest.C1;
import de.schosin.ecs.plugins.composition.manager.CompositionManagerTest.C1234;
import de.schosin.ecs.plugins.composition.manager.CompositionManagerTest.ExclusiveRelationship;
import de.schosin.ecs.plugins.composition.manager.CompositionManagerTest.MyComponentSet;
import de.schosin.ecs.plugins.composition.manager.CompositionManagerTest.RelationshipComponent;
import de.schosin.ecs.plugins.composition.manager.CompositionManagerTest.Target;
import de.schosin.ecs.test.AbstractEcsTest;
import de.schosin.ecs.utils.collections.BitVector;
import de.schosin.ecs.utils.collections.IntBag;

public class CompositionManagerTest extends AbstractEcsTest<CompositionWorld> {

    private static final AtomicInteger MASK_ID = new AtomicInteger(0);

    static final BitVector EMPTY_VECTOR = new BitVector();

    static final Builder EMPTY = Composition.all();

    CompositionManager compositionManager;

    int component1Id;
    int C2Id;

    @BeforeEach
    void setup() {
        MASK_ID.set(0);

        this.compositionManager = world.getSingleton(CompositionManager.class);

        this.component1Id = componentManager.getComponent(component(C1.class)).id();
        this.C2Id = componentManager.getComponent(component(C2.class)).id();
    }

    @Test
    void testCaching() {
        // Setup
        var counter = new AtomicInteger(0);
        Function<ComponentsPredicate, IntBag> supplier = spec -> {
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
            class Of1Test extends AbstractIsInterestedTest<Composition.Of1<C1>> {

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
            class Of2Test extends AbstractIsInterestedTest<Composition.Of2<C1, C2>> {

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
            class Of3Test extends AbstractIsInterestedTest<Composition.Of3<C1, C2, C3>> {

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
            class Of4Test extends AbstractIsInterestedTest<Composition.Of4<C1, C2, C3, C4>> {

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
            class Of5Test extends AbstractIsInterestedTest<Composition.Of5<C1, C2, C3, C4, C5>> {

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
            class Of6Test extends AbstractIsInterestedTest<Composition.Of6<C1, C2, C3, C4, C5, C6>> {

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
            class Of7Test extends AbstractIsInterestedTest<Composition.Of7<C1, C2, C3, C4, C5, C6, C7>> {

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
            class Of8Test extends AbstractIsInterestedTest<Composition.Of8<C1, C2, C3, C4, C5, C6, C7, C8>> {

                @Override
                protected Composition.Of8<C1, C2, C3, C4, C5, C6, C7, C8> create(Builder builder) {
                    return world.createComposition(builder, C1.class, C2.class, C3.class, C4.class, C5.class, C6.class, C7.class, C8.class);
                }

                @Override
                protected boolean isInterested(Composition.Of8<C1, C2, C3, C4, C5, C6, C7, C8> composition, int entityId) {
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

                // TODO @Nested
                class Of1Test extends AbstractCompositionTest<Composition.Of1<C1>> {
                    @Override
                    protected Composition.Of1<C1> createComposition(Composition.Builder builder) {
                        return world.createComposition(builder, C1.class);
                    }
                }

                // TODO @Nested
                class Of2Test extends AbstractCompositionTest<Composition.Of2<C1, C2>> {
                    @Override
                    protected Composition.Of2<C1, C2> createComposition(Composition.Builder builder) {
                        return world.createComposition(builder, C1.class, C2.class);
                    }
                }

                // TODO @Nested
                class Of3Test extends AbstractCompositionTest<Composition.Of3<C1, C2, C3>> {
                    @Override
                    protected Composition.Of3<C1, C2, C3> createComposition(Composition.Builder builder) {
                        return world.createComposition(builder, C1.class, C2.class, C3.class);
                    }
                }

                // TODO @Nested
                class Of4Test extends AbstractCompositionTest<Composition.Of4<C1, C2, C3, C4>> {
                    @Override
                    protected Composition.Of4<C1, C2, C3, C4> createComposition(Composition.Builder builder) {
                        return world.createComposition(builder, C1.class, C2.class, C3.class, C4.class);
                    }
                }

                // TODO @Nested
                class Of5Test extends AbstractCompositionTest<Composition.Of5<C1, C2, C3, C4, C5>> {
                    @Override
                    protected Composition.Of5<C1, C2, C3, C4, C5> createComposition(Composition.Builder builder) {
                        return world.createComposition(builder, C1.class, C2.class, C3.class, C4.class, C5.class);
                    }
                }

                // TODO @Nested
                class Of6Test extends AbstractCompositionTest<Composition.Of6<C1, C2, C3, C4, C5, C6>> {
                    @Override
                    protected Composition.Of6<C1, C2, C3, C4, C5, C6> createComposition(Composition.Builder builder) {
                        return world.createComposition(builder, C1.class, C2.class, C3.class, C4.class, C5.class, C6.class);
                    }
                }

                // TODO @Nested
                class Of7Test extends AbstractCompositionTest<Composition.Of7<C1, C2, C3, C4, C5, C6, C7>> {
                    @Override
                    protected Composition.Of7<C1, C2, C3, C4, C5, C6, C7> createComposition(Composition.Builder builder) {
                        return world.createComposition(builder, C1.class, C2.class, C3.class, C4.class, C5.class, C6.class, C7.class);
                    }
                }

                // TODO @Nested
                class Of8Test extends AbstractCompositionTest<Composition.Of8<C1, C2, C3, C4, C5, C6, C7, C8>> {
                    @Override
                    protected Composition.Of8<C1, C2, C3, C4, C5, C6, C7, C8> createComposition(Composition.Builder builder) {
                        return world.createComposition(builder, C1.class, C2.class, C3.class, C4.class, C5.class, C6.class, C7.class, C8.class);
                    }
                }

                abstract class AbstractCompositionTest<C extends BaseComposition> {

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
        void testInserted_AllComposition() {
            // Setup
            var composition = compositionManager.create(EMPTY, spec -> new IntBag(4));

            var inserted = new IntBag(3);
            composition.inserted(inserted::add);

            // Insert entity
            var entity1 = world.createEntity();
            var entity2 = world.createEntity();
            var entity3 = world.createEntity();

            // Verify
            assertThat(inserted.getSize()).as("size").isEqualTo(3);
            assertThat(inserted.contains(entity1)).as("contains 1").isTrue();
            assertThat(inserted.contains(entity2)).as("contains 2").isTrue();
            assertThat(inserted.contains(entity3)).as("contains 3").isTrue();
        }

        @Test
        void testInserted_InterestedOnly() {
            // Setup
            var composition = compositionManager.create(Composition.all(C1.class), spec -> new IntBag(4));

            var inserted = new IntBag(1);
            composition.inserted(inserted::add);

            // Insert entity
            var entity1 = world.createEntity(new C1(), new C2());
            world.createEntity(new C2());

            // Verify
            assertThat(inserted.getSize()).as("size").isEqualTo(1);
            assertThat(inserted.contains(entity1)).as("contains 42").isTrue();
        }

        @Test
        void testMultipleInserted() {
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
            var entity1 = world.createEntity();
            var entity2 = world.createEntity();
            var entity3 = world.createEntity();

            // Verify
            assertThat(inserted1.getSize()).as("size").isEqualTo(3);
            assertThat(inserted1.contains(entity1)).as("contains 1").isTrue();
            assertThat(inserted1.contains(entity2)).as("contains 2").isTrue();
            assertThat(inserted1.contains(entity3)).as("contains 3").isTrue();

            assertThat(inserted2).containsExactlyInAnyOrder(entity1, entity2, entity3);

            assertThat(inserted3).containsExactlyInAnyOrder(entity1, entity2, entity3);
        }

    }

    @Nested
    class UpdatedTest {

        @Test
        void testUpdatedEntities_WhenNullPreviousComposition_Throws() {
            var componentMask = componentMaskManager.getComponentMask(component(C1.class));
            var event = EntityUpdatedEvent.get(42, null, componentMask);

            assertThatThrownBy(() -> eventManager.dispatchEvent(event)).isInstanceOf(NullPointerException.class);
        }

        @Test
        void testUpdatedEntities_WhenNullNewComposition_Throws() {
            var componentMask = componentMaskManager.getComponentMask(component(C1.class));
            var event = EntityUpdatedEvent.get(42, componentMask, null);

            assertThatThrownBy(() -> eventManager.dispatchEvent(event)).isInstanceOf(NullPointerException.class);
        }

        @Test
        void testUpdatedEntities_WhenPreviousComposition_RemovesEntities() {
            // Setup
            var entities = new HashSet<Integer>();
            bagManager.ensureEntitySize(10000);

            var composition1 = compositionManager.create(Composition.all(C1.class), spec -> bagManager.createEntityIntBag());
            var composition2 = compositionManager.create(Composition.all(C2.class), spec -> bagManager.createEntityIntBag());

            var componentMask1 = componentMaskManager.getComponentMask(component(C1.class));
            var componentMask12 = componentMaskManager.getComponentMask(component(C1.class), component(C2.class));
            var componentMask2 = componentMaskManager.getComponentMask(component(C2.class));

            var mask42 = componentMask1;
            var mask1337 = componentMask2;
            var mask9001 = componentMask1;

            eventManager.dispatchEvent(EntityInsertedEvent.get(42, mask42));
            eventManager.dispatchEvent(EntityInsertedEvent.get(1337, mask1337));
            eventManager.dispatchEvent(EntityInsertedEvent.get(9001, mask9001));

            // Update entity
            eventManager.dispatchEvent(EntityUpdatedEvent.get(42, mask42, componentMask12));
            eventManager.dispatchEvent(EntityUpdatedEvent.get(1337, mask1337, componentMask12));
            eventManager.dispatchEvent(EntityUpdatedEvent.get(9001, mask9001, componentMask2));

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

            var componentMask1 = componentMaskManager.getComponentMask(component(C1.class));
            var componentMask12 = componentMaskManager.getComponentMask(component(C1.class), component(C2.class));
            var componentMask2 = componentMaskManager.getComponentMask(component(C2.class));
            var componentMask3 = componentMaskManager.getComponentMask(component(C3.class));

            var composition1 = compositionManager.create(Composition.all(C1.class), spec -> bagManager.createEntityIntBag());
            var composition2 = compositionManager.create(Composition.all(C2.class), spec -> bagManager.createEntityIntBag());
            var composition3 = compositionManager.create(Composition.all(C3.class), spec -> bagManager.createEntityIntBag());

            var mask7 = componentMask2;
            var mask42 = componentMask1;
            var mask1337 = componentMask2;
            var mask9001 = componentMask12;

            eventManager.dispatchEvent(EntityInsertedEvent.get(7, mask7));
            eventManager.dispatchEvent(EntityInsertedEvent.get(42, mask42));
            eventManager.dispatchEvent(EntityInsertedEvent.get(1337, mask1337));
            eventManager.dispatchEvent(EntityInsertedEvent.get(9001, mask9001));

            var removed1 = new HashSet<Integer>();
            composition1.removed(removed1::add);

            var removed2 = new HashSet<Integer>();
            composition2.removed(removed2::add);

            var removed3 = new HashSet<Integer>();
            composition3.removed(removed3::add);

            // Update entity
            eventManager.dispatchEvent(EntityUpdatedEvent.get(7, mask7, componentMask1));
            eventManager.dispatchEvent(EntityUpdatedEvent.get(42, mask42, componentMask12));
            eventManager.dispatchEvent(EntityUpdatedEvent.get(1337, mask1337, componentMask12));
            eventManager.dispatchEvent(EntityUpdatedEvent.get(9001, mask9001, componentMask3));

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

            var componentMask = componentMaskManager.getComponentMask(component(C1.class));
            var composition = compositionManager.create(EMPTY, spec -> initial);

            // Remove entity
            eventManager.dispatchEvent(EntityRemovedEvent.get(42, componentMask));
            eventManager.dispatchEvent(EntityRemovedEvent.get(1337, componentMask));
            eventManager.dispatchEvent(EntityRemovedEvent.get(9001, componentMask));
            eventManager.dispatchEvent(EntityRemovedEvent.get(9002, componentMask));

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

            var componentMask = componentMaskManager.getComponentMask(component(C1.class));
            var composition = compositionManager.create(EMPTY, spec -> initial);

            // Add callback
            composition.removed(removed::add);
            assertThat(removed.getSize()).as("size").isZero();

            // Remove entity
            eventManager.dispatchEvent(EntityRemovedEvent.get(42, componentMask));
            eventManager.dispatchEvent(EntityRemovedEvent.get(1337, componentMask));
            eventManager.dispatchEvent(EntityRemovedEvent.get(9001, componentMask));

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

            var componentMask = componentMaskManager.getComponentMask(component(C1.class));
            var composition = compositionManager.create(EMPTY, spec -> initial);

            // Add callbacks
            composition.removed(removed1::add);
            assertThat(removed1.getSize()).as("size").isZero();

            composition.removed(removed2::add);
            assertThat(removed2).isEmpty();

            composition.removed(removed3::add);
            assertThat(removed3).isEmpty();

            // Remove entity
            eventManager.dispatchEvent(EntityRemovedEvent.get(42, componentMask));
            eventManager.dispatchEvent(EntityRemovedEvent.get(1337, componentMask));
            eventManager.dispatchEvent(EntityRemovedEvent.get(9001, componentMask));

            // Verify
            assertThat(removed1.getSize()).as("size").isEqualTo(2);
            assertThat(removed1.contains(42)).as("contains 42").isTrue();
            assertThat(removed1.contains(1337)).as("contains 1337").isTrue();

            assertThat(removed2).containsExactlyInAnyOrder(42, 1337);

            assertThat(removed3).containsExactlyInAnyOrder(42, 1337);
        }

    }

    @Nested
    class Composition1Test extends AbstractCompositionNTest<Composition.Of1<C1>> {

        @Override
        Composition.Of1<C1> composition(Composition.Builder builder) {
            return world.createComposition(builder, C1.class);
        }

        @Override
        <T, R> Composition.Of1<R> composition(Builder builder, ComponentType<T, R> first) {
            return world.createComposition(builder, first);
        }

        @Override
        @SuppressWarnings("unchecked")
        <T, R> void inserted(Composition.Of<?> of, ComponentType<T, R> type, Composition.Of1.Consumer<R> consumer) {
            var composition = (Composition.Of1<R>) of;
            composition.inserted((entityId, result) -> consumer.consume(entityId, result));
        }

        @Override
        @SuppressWarnings("unchecked")
        <T, R> void removed(Composition.Of<?> of, ComponentType<T, R> type, Composition.Of1.Consumer<R> consumer) {
            var composition = (Composition.Of1<R>) of;
            composition.removed((entityId, result) -> consumer.consume(entityId, result));
        }

        @Override
        @SuppressWarnings("unchecked")
        <T, R> void process(Composition.Of<?> of, ComponentType<T, R> type, Composition.Of1.Consumer<R> consumer) {
            var composition = (Composition.Of1<R>) of;
            composition.process((entityId, result) -> consumer.consume(entityId, result));
        }

        @Test
        void testProcessOtherEntity() {
            composition(builder1).process(expected1[0], (entityId, C1) -> {
                assertThat(C1).isNotNull();
            });

            composition(builder8).process(expected8[0], (entityId, C1) -> {
                assertThat(C1).isNotNull();
            });
        }

        @Test
        void testProcessOneComponent() {
            var composition = composition(builder1);

            var processed = new IntBag(expected1.length);
            composition.process((entityId, C1) -> {
                processed.add(entityId);

                assertThat(C1).isNotNull();
            });

            assertThat(processed.getSize()).isEqualTo(expected1.length);
            assertThat(processed.getData()).containsExactlyInAnyOrder(expected1);
        }

        @Test
        void testProcessAllComponents() {
            var composition = composition(builder8);

            var processed = new IntBag(expected8.length);
            composition.process((entityId, C1) -> {
                processed.add(entityId);

                assertThat(C1).isNotNull();
            });

            assertThat(processed.getSize()).isEqualTo(expected8.length);
            assertThat(processed.getData()).containsExactlyInAnyOrder(expected8);
        }

        @Test
        void testProcessVaryingComponents() {
            var composition = composition(builderAll);

            var processed = new IntBag(expected1.length + expected8.length);
            composition.process((entityId, C1) -> {
                processed.add(entityId);

                assertThat(C1).isNotNull();
            });

            assertThat(processed.getSize()).isEqualTo(expected1.length + expected8.length);
            assertThat(processed.getData()).contains(expected1);
            assertThat(processed.getData()).contains(expected8);
        }

        @Test
        void testInserted() {
            var composition = composition(builder1);

            var inserted = new IntBag(7);
            composition.inserted((entityId, C1) -> {
                inserted.add(entityId);

                assertThat(C1).isNotNull();
            });

            var ids = createEntities1(7);

            assertThat(inserted.getSize()).isEqualTo(7);
            assertThat(inserted.getData()).containsExactlyInAnyOrder(ids);
        }

        @Test
        void testRemoved() {
            var composition = composition(builder1);

            var removed = new IntBag(expected1.length);
            composition.removed((entityId, C1) -> {
                removed.add(entityId);

                assertThat(C1).isNotNull();
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
    class Composition2Test extends AbstractCompositionNTest<Composition.Of2<C1, C2>> {

        @Override
        Composition.Of2<C1, C2> composition(Composition.Builder builder) {
            return world.createComposition(builder, C1.class, C2.class);
        }

        @Override
        <T, R> Composition.Of2<R, C2> composition(Builder builder, ComponentType<T, R> first) {
            return world.createComposition(builder, first, component(C2.class));
        }

        @Override
        @SuppressWarnings("unchecked")
        <T, R> void inserted(Composition.Of<?> of, ComponentType<T, R> type, Composition.Of1.Consumer<R> consumer) {
            var composition = (Composition.Of2<R, C2>) of;
            composition.inserted((entityId, result, c2) -> consumer.consume(entityId, result));
        }

        @Override
        @SuppressWarnings("unchecked")
        <T, R> void removed(Composition.Of<?> of, ComponentType<T, R> type, Composition.Of1.Consumer<R> consumer) {
            var composition = (Composition.Of2<R, C2>) of;
            composition.removed((entityId, result, c2) -> consumer.consume(entityId, result));
        }

        @Override
        @SuppressWarnings("unchecked")
        <T, R> void process(Composition.Of<?> of, ComponentType<T, R> type, Composition.Of1.Consumer<R> consumer) {
            var composition = (Composition.Of2<R, C2>) of;
            composition.process((entityId, result, c2) -> consumer.consume(entityId, result));
        }

        @Test
        void testProcessOtherEntity() {
            composition(builder1).process(expected1[0], (entityId, C1, C2) -> {
                assertThat(C1).isNotNull();
                assertThat(C2).isNull();
            });

            composition(builder8).process(expected8[0], (entityId, C1, C2) -> {
                assertThat(C1).isNotNull();
                assertThat(C2).isNotNull();
            });
        }

        @Test
        void testProcessOneComponent() {
            var composition = composition(builder1);

            var processed = new IntBag(expected1.length);
            composition.process((entityId, C1, C2) -> {
                processed.add(entityId);

                assertThat(C1).isNotNull();
                assertThat(C2).isNull();
            });

            assertThat(processed.getSize()).isEqualTo(expected1.length);
            assertThat(processed.getData()).containsExactlyInAnyOrder(expected1);
        }

        @Test
        void testProcessAllComponents() {
            var composition = composition(builder8);

            var processed = new IntBag(expected8.length);
            composition.process((entityId, C1, C2) -> {
                processed.add(entityId);

                assertThat(C1).isNotNull();
                assertThat(C2).isNotNull();
            });

            assertThat(processed.getSize()).isEqualTo(expected8.length);
            assertThat(processed.getData()).containsExactlyInAnyOrder(expected8);
        }

        @Test
        void testProcessVaryingComponents() {
            var composition = composition(builderAll);

            var processed = new IntBag(expected1.length + expected8.length);
            composition.process((entityId, C1, C2) -> {
                processed.add(entityId);

                assertThat(C1).isNotNull();
            });

            assertThat(processed.getSize()).isEqualTo(expected1.length + expected8.length);
            assertThat(processed.getData()).contains(expected1);
            assertThat(processed.getData()).contains(expected8);
        }

        @Test
        void testInserted() {
            var composition = composition(builder1);

            var inserted = new IntBag(7);
            composition.inserted((entityId, C1, C2) -> {
                inserted.add(entityId);

                assertThat(C1).isNotNull();
                assertThat(C2).isNull();
            });

            var ids = createEntities1(7);

            assertThat(inserted.getSize()).isEqualTo(7);
            assertThat(inserted.getData()).containsExactlyInAnyOrder(ids);
        }

        @Test
        void testRemoved() {
            var composition = composition(builder1);

            var removed = new IntBag(expected1.length);
            composition.removed((entityId, C1, C2) -> {
                removed.add(entityId);

                assertThat(C1).isNotNull();
                assertThat(C2).isNull();
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
    class Composition3Test extends AbstractCompositionNTest<Composition.Of3<C1, C2, C3>> {

        @Override
        Composition.Of3<C1, C2, C3> composition(Composition.Builder builder) {
            return world.createComposition(builder, C1.class, C2.class, C3.class);
        }

        @Override
        <T, R> Composition.Of3<R, C2, C3> composition(Builder builder, ComponentType<T, R> first) {
            return world.createComposition(builder, first, component(C2.class), component(C3.class));
        }

        @Override
        @SuppressWarnings("unchecked")
        <T, R> void inserted(Composition.Of<?> of, ComponentType<T, R> type, Composition.Of1.Consumer<R> consumer) {
            var composition = (Composition.Of3<R, C2, C3>) of;
            composition.inserted((entityId, result, c2, c3) -> consumer.consume(entityId, result));
        }

        @Override
        @SuppressWarnings("unchecked")
        <T, R> void removed(Composition.Of<?> of, ComponentType<T, R> type, Composition.Of1.Consumer<R> consumer) {
            var composition = (Composition.Of3<R, C2, C3>) of;
            composition.removed((entityId, result, c2, c3) -> consumer.consume(entityId, result));
        }

        @Override
        @SuppressWarnings("unchecked")
        <T, R> void process(Composition.Of<?> of, ComponentType<T, R> type, Composition.Of1.Consumer<R> consumer) {
            var composition = (Composition.Of3<R, C2, C3>) of;
            composition.process((entityId, result, c2, c3) -> consumer.consume(entityId, result));
        }

        @Test
        void testProcessOtherEntity() {
            composition(builder1).process(expected1[0], (entityId, C1, C2, C3) -> {
                assertThat(C1).isNotNull();
                assertThat(C2).isNull();
                assertThat(C3).isNull();
            });

            composition(builder8).process(expected8[0], (entityId, C1, C2, C3) -> {
                assertThat(C1).isNotNull();
                assertThat(C2).isNotNull();
                assertThat(C3).isNotNull();
            });
        }

        @Test
        void testProcessOneComponent() {
            var composition = composition(builder1);

            var processed = new IntBag(expected1.length);
            composition.process((entityId, C1, C2, C3) -> {
                processed.add(entityId);

                assertThat(C1).isNotNull();
                assertThat(C2).isNull();
                assertThat(C3).isNull();
            });

            assertThat(processed.getSize()).isEqualTo(expected1.length);
            assertThat(processed.getData()).containsExactlyInAnyOrder(expected1);
        }

        @Test
        void testProcessAllComponents() {
            var composition = composition(builder8);

            var processed = new IntBag(expected8.length);
            composition.process((entityId, C1, C2, C3) -> {
                processed.add(entityId);

                assertThat(C1).isNotNull();
                assertThat(C2).isNotNull();
                assertThat(C3).isNotNull();
            });

            assertThat(processed.getSize()).isEqualTo(expected8.length);
            assertThat(processed.getData()).containsExactlyInAnyOrder(expected8);
        }

        @Test
        void testProcessVaryingComponents() {
            var composition = composition(builderAll);

            var processed = new IntBag(expected1.length + expected8.length);
            composition.process((entityId, C1, C2, C3) -> {
                processed.add(entityId);

                assertThat(C1).isNotNull();
            });

            assertThat(processed.getSize()).isEqualTo(expected1.length + expected8.length);
            assertThat(processed.getData()).contains(expected1);
            assertThat(processed.getData()).contains(expected8);
        }

        @Test
        void testInserted() {
            var composition = composition(builder1);

            var inserted = new IntBag(7);
            composition.inserted((entityId, C1, C2, C3) -> {
                inserted.add(entityId);

                assertThat(C1).isNotNull();
                assertThat(C2).isNull();
                assertThat(C3).isNull();
            });

            var ids = createEntities1(7);

            assertThat(inserted.getSize()).isEqualTo(7);
            assertThat(inserted.getData()).containsExactlyInAnyOrder(ids);
        }

        @Test
        void testRemoved() {
            var composition = composition(builder1);

            var removed = new IntBag(expected1.length);
            composition.removed((entityId, C1, C2, C3) -> {
                removed.add(entityId);

                assertThat(C1).isNotNull();
                assertThat(C2).isNull();
                assertThat(C3).isNull();
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
    class Composition4Test extends AbstractCompositionNTest<Composition.Of4<C1, C2, C3, C4>> {

        @Override
        Composition.Of4<C1, C2, C3, C4> composition(Composition.Builder builder) {
            return world.createComposition(builder, C1.class, C2.class, C3.class, C4.class);
        }

        @Override
        <T, R> Composition.Of4<R, C2, C3, C4> composition(Builder builder, ComponentType<T, R> first) {
            return world.createComposition(builder, first, component(C2.class), component(C3.class), component(C4.class));
        }

        @Override
        @SuppressWarnings("unchecked")
        <T, R> void inserted(Composition.Of<?> of, ComponentType<T, R> type, Composition.Of1.Consumer<R> consumer) {
            var composition = (Composition.Of4<R, C2, C3, C4>) of;
            composition.inserted((entityId, result, c2, c3, c4) -> consumer.consume(entityId, result));
        }

        @Override
        @SuppressWarnings("unchecked")
        <T, R> void removed(Composition.Of<?> of, ComponentType<T, R> type, Composition.Of1.Consumer<R> consumer) {
            var composition = (Composition.Of4<R, C2, C3, C4>) of;
            composition.removed((entityId, result, c2, c3, c4) -> consumer.consume(entityId, result));
        }

        @Override
        @SuppressWarnings("unchecked")
        <T, R> void process(Composition.Of<?> of, ComponentType<T, R> type, Composition.Of1.Consumer<R> consumer) {
            var composition = (Composition.Of4<R, C2, C3, C4>) of;
            composition.process((entityId, result, c2, c3, c4) -> consumer.consume(entityId, result));
        }

        @Test
        void testProcessOtherEntity() {
            composition(builder1).process(expected1[0], (entityId, C1, C2, C3, component4) -> {
                assertThat(C1).isNotNull();
                assertThat(C2).isNull();
                assertThat(C3).isNull();
                assertThat(component4).isNull();
            });

            composition(builder8).process(expected8[0], (entityId, C1, C2, C3, component4) -> {
                assertThat(C1).isNotNull();
                assertThat(C2).isNotNull();
                assertThat(C3).isNotNull();
                assertThat(component4).isNotNull();
            });
        }

        @Test
        void testProcessOneComponent() {
            var composition = composition(builder1);

            var processed = new IntBag(expected1.length);
            composition.process((entityId, C1, C2, C3, component4) -> {
                processed.add(entityId);

                assertThat(C1).isNotNull();
                assertThat(C2).isNull();
                assertThat(C3).isNull();
                assertThat(component4).isNull();
            });

            assertThat(processed.getSize()).isEqualTo(expected1.length);
            assertThat(processed.getData()).containsExactlyInAnyOrder(expected1);
        }

        @Test
        void testProcessAllComponents() {
            var composition = composition(builder8);

            var processed = new IntBag(expected8.length);
            composition.process((entityId, C1, C2, C3, component4) -> {
                processed.add(entityId);

                assertThat(C1).isNotNull();
                assertThat(C2).isNotNull();
                assertThat(C3).isNotNull();
                assertThat(component4).isNotNull();
            });

            assertThat(processed.getSize()).isEqualTo(expected8.length);
            assertThat(processed.getData()).containsExactlyInAnyOrder(expected8);
        }

        @Test
        void testProcessVaryingComponents() {
            var composition = composition(builderAll);

            var processed = new IntBag(expected1.length + expected8.length);
            composition.process((entityId, C1, C2, C3, component4) -> {
                processed.add(entityId);

                assertThat(C1).isNotNull();
            });

            assertThat(processed.getSize()).isEqualTo(expected1.length + expected8.length);
            assertThat(processed.getData()).contains(expected1);
            assertThat(processed.getData()).contains(expected8);
        }

        @Test
        void testInserted() {
            var composition = composition(builder1);

            var inserted = new IntBag(7);
            composition.inserted((entityId, C1, C2, C3, component4) -> {
                inserted.add(entityId);

                assertThat(C1).isNotNull();
                assertThat(C2).isNull();
                assertThat(C3).isNull();
                assertThat(component4).isNull();
            });

            var ids = createEntities1(7);

            assertThat(inserted.getSize()).isEqualTo(7);
            assertThat(inserted.getData()).containsExactlyInAnyOrder(ids);
        }

        @Test
        void testRemoved() {
            var composition = composition(builder1);

            var removed = new IntBag(expected1.length);
            composition.removed((entityId, C1, C2, C3, component4) -> {
                removed.add(entityId);

                assertThat(C1).isNotNull();
                assertThat(C2).isNull();
                assertThat(C3).isNull();
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
    class Composition5Test extends AbstractCompositionNTest<Composition.Of5<C1, C2, C3, C4, C5>> {

        @Override
        Composition.Of5<C1, C2, C3, C4, C5> composition(Composition.Builder builder) {
            return world.createComposition(builder, C1.class, C2.class, C3.class, C4.class, C5.class);
        }

        @Override
        <T, R> Composition.Of5<R, C2, C3, C4, C5> composition(Builder builder, ComponentType<T, R> first) {
            return world.createComposition(builder, first, component(C2.class), component(C3.class), component(C4.class), component(C5.class));
        }

        @Override
        @SuppressWarnings("unchecked")
        <T, R> void inserted(Composition.Of<?> of, ComponentType<T, R> type, Composition.Of1.Consumer<R> consumer) {
            var composition = (Composition.Of5<R, C2, C3, C4, C5>) of;
            composition.inserted((entityId, result, c2, c3, c4, c5) -> consumer.consume(entityId, result));
        }

        @Override
        @SuppressWarnings("unchecked")
        <T, R> void removed(Composition.Of<?> of, ComponentType<T, R> type, Composition.Of1.Consumer<R> consumer) {
            var composition = (Composition.Of5<R, C2, C3, C4, C5>) of;
            composition.removed((entityId, result, c2, c3, c4, c5) -> consumer.consume(entityId, result));
        }

        @Override
        @SuppressWarnings("unchecked")
        <T, R> void process(Composition.Of<?> of, ComponentType<T, R> type, Composition.Of1.Consumer<R> consumer) {
            var composition = (Composition.Of5<R, C2, C3, C4, C5>) of;
            composition.process((entityId, result, c2, c3, c4, c5) -> consumer.consume(entityId, result));
        }

        @Test
        void testProcessOtherEntity() {
            composition(builder1).process(expected1[0], (entityId, C1, C2, C3, component4, component5) -> {
                assertThat(C1).isNotNull();
                assertThat(C2).isNull();
                assertThat(C3).isNull();
                assertThat(component4).isNull();
                assertThat(component5).isNull();
            });

            composition(builder8).process(expected8[0], (entityId, C1, C2, C3, component4, component5) -> {
                assertThat(C1).isNotNull();
                assertThat(C2).isNotNull();
                assertThat(C3).isNotNull();
                assertThat(component4).isNotNull();
                assertThat(component5).isNotNull();
            });
        }

        @Test
        void testProcessOneComponent() {
            var composition = composition(builder1);

            var processed = new IntBag(expected1.length);
            composition.process((entityId, C1, C2, C3, component4, component5) -> {
                processed.add(entityId);

                assertThat(C1).isNotNull();
                assertThat(C2).isNull();
                assertThat(C3).isNull();
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
            composition.process((entityId, C1, C2, C3, component4, component5) -> {
                processed.add(entityId);

                assertThat(C1).isNotNull();
                assertThat(C2).isNotNull();
                assertThat(C3).isNotNull();
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
            composition.process((entityId, C1, C2, C3, component4, component5) -> {
                processed.add(entityId);

                assertThat(C1).isNotNull();
            });

            assertThat(processed.getSize()).isEqualTo(expected1.length + expected8.length);
            assertThat(processed.getData()).contains(expected1);
            assertThat(processed.getData()).contains(expected8);
        }

        @Test
        void testInserted() {
            var composition = composition(builder1);

            var inserted = new IntBag(7);
            composition.inserted((entityId, C1, C2, C3, component4, component5) -> {
                inserted.add(entityId);

                assertThat(C1).isNotNull();
                assertThat(C2).isNull();
                assertThat(C3).isNull();
                assertThat(component4).isNull();
                assertThat(component5).isNull();
            });

            var ids = createEntities1(7);

            assertThat(inserted.getSize()).isEqualTo(7);
            assertThat(inserted.getData()).containsExactlyInAnyOrder(ids);
        }

        @Test
        void testRemoved() {
            var composition = composition(builder1);

            var removed = new IntBag(expected1.length);
            composition.removed((entityId, C1, C2, C3, component4, component5) -> {
                removed.add(entityId);

                assertThat(C1).isNotNull();
                assertThat(C2).isNull();
                assertThat(C3).isNull();
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
    class Composition6Test extends AbstractCompositionNTest<Composition.Of6<C1, C2, C3, C4, C5, C6>> {

        @Override
        Composition.Of6<C1, C2, C3, C4, C5, C6> composition(Composition.Builder builder) {
            return world.createComposition(builder, C1.class, C2.class, C3.class, C4.class, C5.class, C6.class);
        }

        @Override
        <T, R> Composition.Of6<R, C2, C3, C4, C5, C6> composition(Builder builder, ComponentType<T, R> first) {
            return world.createComposition(builder, first, component(C2.class), component(C3.class), component(C4.class), component(C5.class), component(C6.class));
        }

        @Override
        @SuppressWarnings("unchecked")
        <T, R> void inserted(Composition.Of<?> of, ComponentType<T, R> type, Composition.Of1.Consumer<R> consumer) {
            var composition = (Composition.Of6<R, C2, C3, C4, C5, C6>) of;
            composition.inserted((entityId, result, c2, c3, c4, c5, c6) -> consumer.consume(entityId, result));
        }

        @Override
        @SuppressWarnings("unchecked")
        <T, R> void removed(Composition.Of<?> of, ComponentType<T, R> type, Composition.Of1.Consumer<R> consumer) {
            var composition = (Composition.Of6<R, C2, C3, C4, C5, C6>) of;
            composition.removed((entityId, result, c2, c3, c4, c5, c6) -> consumer.consume(entityId, result));
        }

        @Override
        @SuppressWarnings("unchecked")
        <T, R> void process(Composition.Of<?> of, ComponentType<T, R> type, Composition.Of1.Consumer<R> consumer) {
            var composition = (Composition.Of6<R, C2, C3, C4, C5, C6>) of;
            composition.process((entityId, result, c2, c3, c4, c5, c6) -> consumer.consume(entityId, result));
        }

        @Test
        void testProcessOtherEntity() {
            composition(builder1).process(expected1[0], (entityId, C1, C2, C3, component4, component5, component6) -> {
                assertThat(C1).isNotNull();
                assertThat(C2).isNull();
                assertThat(C3).isNull();
                assertThat(component4).isNull();
                assertThat(component5).isNull();
                assertThat(component6).isNull();
            });

            composition(builder8).process(expected8[0], (entityId, C1, C2, C3, component4, component5, component6) -> {
                assertThat(C1).isNotNull();
                assertThat(C2).isNotNull();
                assertThat(C3).isNotNull();
                assertThat(component4).isNotNull();
                assertThat(component5).isNotNull();
                assertThat(component6).isNotNull();
            });
        }

        @Test
        void testProcessOneComponent() {
            var composition = composition(builder1);

            var processed = new IntBag(expected1.length);
            composition.process((entityId, C1, C2, C3, component4, component5, component6) -> {
                processed.add(entityId);

                assertThat(C1).isNotNull();
                assertThat(C2).isNull();
                assertThat(C3).isNull();
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
            composition.process((entityId, C1, C2, C3, component4, component5, component6) -> {
                processed.add(entityId);

                assertThat(C1).isNotNull();
                assertThat(C2).isNotNull();
                assertThat(C3).isNotNull();
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
            composition.process((entityId, C1, C2, C3, component4, component5, component6) -> {
                processed.add(entityId);

                assertThat(C1).isNotNull();
            });

            assertThat(processed.getSize()).isEqualTo(expected1.length + expected8.length);
            assertThat(processed.getData()).contains(expected1);
            assertThat(processed.getData()).contains(expected8);
        }

        @Test
        void testInserted() {
            var composition = composition(builder1);

            var inserted = new IntBag(7);
            composition.inserted((entityId, C1, C2, C3, component4, component5, component6) -> {
                inserted.add(entityId);

                assertThat(C1).isNotNull();
                assertThat(C2).isNull();
                assertThat(C3).isNull();
                assertThat(component4).isNull();
                assertThat(component5).isNull();
                assertThat(component6).isNull();
            });

            var ids = createEntities1(7);

            assertThat(inserted.getSize()).isEqualTo(7);
            assertThat(inserted.getData()).containsExactlyInAnyOrder(ids);
        }

        @Test
        void testRemoved() {
            var composition = composition(builder1);

            var removed = new IntBag(expected1.length);
            composition.removed((entityId, C1, C2, C3, component4, component5, component6) -> {
                removed.add(entityId);

                assertThat(C1).isNotNull();
                assertThat(C2).isNull();
                assertThat(C3).isNull();
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
    class Composition7Test extends AbstractCompositionNTest<Composition.Of7<C1, C2, C3, C4, C5, C6, C7>> {

        @Override
        Composition.Of7<C1, C2, C3, C4, C5, C6, C7> composition(Composition.Builder builder) {
            return world.createComposition(builder, C1.class, C2.class, C3.class, C4.class, C5.class, C6.class, C7.class);
        }

        @Override
        <T, R> Composition.Of7<R, C2, C3, C4, C5, C6, C7> composition(Builder builder, ComponentType<T, R> first) {
            return world.createComposition(builder, first, component(C2.class), component(C3.class), component(C4.class), component(C5.class), component(C6.class), component(C7.class));
        }

        @Override
        @SuppressWarnings("unchecked")
        <T, R> void inserted(Composition.Of<?> of, ComponentType<T, R> type, Composition.Of1.Consumer<R> consumer) {
            var composition = (Composition.Of7<R, C2, C3, C4, C5, C6, C7>) of;
            composition.inserted((entityId, result, c2, c3, c4, c5, c6, c7) -> consumer.consume(entityId, result));
        }

        @Override
        @SuppressWarnings("unchecked")
        <T, R> void removed(Composition.Of<?> of, ComponentType<T, R> type, Composition.Of1.Consumer<R> consumer) {
            var composition = (Composition.Of7<R, C2, C3, C4, C5, C6, C7>) of;
            composition.removed((entityId, result, c2, c3, c4, c5, c6, c7) -> consumer.consume(entityId, result));
        }

        @Override
        @SuppressWarnings("unchecked")
        <T, R> void process(Composition.Of<?> of, ComponentType<T, R> type, Composition.Of1.Consumer<R> consumer) {
            var composition = (Composition.Of7<R, C2, C3, C4, C5, C6, C7>) of;
            composition.process((entityId, result, c2, c3, c4, c5, c6, c7) -> consumer.consume(entityId, result));
        }

        @Test
        void testProcessOtherEntity() {
            composition(builder1).process(expected1[0], (entityId, C1, C2, C3, component4, component5, component6, component7) -> {
                assertThat(C1).isNotNull();
                assertThat(C2).isNull();
                assertThat(C3).isNull();
                assertThat(component4).isNull();
                assertThat(component5).isNull();
                assertThat(component6).isNull();
                assertThat(component7).isNull();
            });

            composition(builder8).process(expected8[0], (entityId, C1, C2, C3, component4, component5, component6, component7) -> {
                assertThat(C1).isNotNull();
                assertThat(C2).isNotNull();
                assertThat(C3).isNotNull();
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
            composition.process((entityId, C1, C2, C3, component4, component5, component6, component7) -> {
                processed.add(entityId);

                assertThat(C1).isNotNull();
                assertThat(C2).isNull();
                assertThat(C3).isNull();
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
            composition.process((entityId, C1, C2, C3, component4, component5, component6, component7) -> {
                processed.add(entityId);

                assertThat(C1).isNotNull();
                assertThat(C2).isNotNull();
                assertThat(C3).isNotNull();
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
            composition.process((entityId, C1, C2, C3, component4, component5, component6, component7) -> {
                processed.add(entityId);

                assertThat(C1).isNotNull();
            });

            assertThat(processed.getSize()).isEqualTo(expected1.length + expected8.length);
            assertThat(processed.getData()).contains(expected1);
            assertThat(processed.getData()).contains(expected8);
        }

        @Test
        void testInserted() {
            var composition = composition(builder1);

            var inserted = new IntBag(7);
            composition.inserted((entityId, C1, C2, C3, component4, component5, component6, component7) -> {
                inserted.add(entityId);

                assertThat(C1).isNotNull();
                assertThat(C2).isNull();
                assertThat(C3).isNull();
                assertThat(component4).isNull();
                assertThat(component5).isNull();
                assertThat(component6).isNull();
                assertThat(component7).isNull();
            });

            var ids = createEntities1(7);

            assertThat(inserted.getSize()).isEqualTo(7);
            assertThat(inserted.getData()).containsExactlyInAnyOrder(ids);
        }

        @Test
        void testRemoved() {
            var composition = composition(builder1);

            var removed = new IntBag(expected1.length);
            composition.removed((entityId, C1, C2, C3, component4, component5, component6, component7) -> {
                removed.add(entityId);

                assertThat(C1).isNotNull();
                assertThat(C2).isNull();
                assertThat(C3).isNull();
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
    class Composition8Test extends AbstractCompositionNTest<Composition.Of8<C1, C2, C3, C4, C5, C6, C7, C8>> {

        @Override
        Composition.Of8<C1, C2, C3, C4, C5, C6, C7, C8> composition(Composition.Builder builder) {
            return world.createComposition(builder, C1.class, C2.class, C3.class, C4.class, C5.class, C6.class, C7.class, C8.class);
        }

        @Override
        <T, R> Composition.Of8<R, C2, C3, C4, C5, C6, C7, C8> composition(Builder builder, ComponentType<T, R> first) {
            return world.createComposition(builder, first, component(C2.class), component(C3.class), component(C4.class), component(C5.class), component(C6.class), component(C7.class),
                    component(C8.class));
        }

        @Override
        @SuppressWarnings("unchecked")
        <T, R> void inserted(Composition.Of<?> of, ComponentType<T, R> type, Composition.Of1.Consumer<R> consumer) {
            var composition = (Composition.Of8<R, C2, C3, C4, C5, C6, C7, C8>) of;
            composition.inserted((entityId, result, c2, c3, c4, c5, c6, c7, c8) -> consumer.consume(entityId, result));
        }

        @Override
        @SuppressWarnings("unchecked")
        <T, R> void removed(Composition.Of<?> of, ComponentType<T, R> type, Composition.Of1.Consumer<R> consumer) {
            var composition = (Composition.Of8<R, C2, C3, C4, C5, C6, C7, C8>) of;
            composition.removed((entityId, result, c2, c3, c4, c5, c6, c7, c8) -> consumer.consume(entityId, result));
        }

        @Override
        @SuppressWarnings("unchecked")
        <T, R> void process(Composition.Of<?> of, ComponentType<T, R> type, Composition.Of1.Consumer<R> consumer) {
            var composition = (Composition.Of8<R, C2, C3, C4, C5, C6, C7, C8>) of;
            composition.process((entityId, result, c2, c3, c4, c5, c6, c7, c8) -> consumer.consume(entityId, result));
        }

        @Test
        void testProcessOtherEntity() {
            composition(builder1).process(expected1[0], (entityId, C1, C2, C3, component4, component5, component6, component7, component8) -> {
                assertThat(C1).isNotNull();
                assertThat(C2).isNull();
                assertThat(C3).isNull();
                assertThat(component4).isNull();
                assertThat(component5).isNull();
                assertThat(component6).isNull();
                assertThat(component7).isNull();
                assertThat(component8).isNull();
            });

            composition(builder8).process(expected8[0], (entityId, C1, C2, C3, component4, component5, component6, component7, component8) -> {
                assertThat(C1).isNotNull();
                assertThat(C2).isNotNull();
                assertThat(C3).isNotNull();
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
            composition.process((entityId, C1, C2, C3, component4, component5, component6, component7, component8) -> {
                processed.add(entityId);

                assertThat(C1).isNotNull();
                assertThat(C2).isNull();
                assertThat(C3).isNull();
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
            composition.process((entityId, C1, C2, C3, component4, component5, component6, component7, component8) -> {
                processed.add(entityId);

                assertThat(C1).isNotNull();
                assertThat(C2).isNotNull();
                assertThat(C3).isNotNull();
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
            composition.process((entityId, C1, C2, C3, component4, component5, component6, component7, component8) -> {
                processed.add(entityId);

                assertThat(C1).isNotNull();
            });

            assertThat(processed.getSize()).isEqualTo(expected1.length + expected8.length);
            assertThat(processed.getData()).contains(expected1);
            assertThat(processed.getData()).contains(expected8);
        }

        @Test
        void testInserted() {
            var composition = composition(builder1);

            var inserted = new IntBag(7);
            composition.inserted((entityId, C1, C2, C3, component4, component5, component6, component7, component8) -> {
                inserted.add(entityId);

                assertThat(C1).isNotNull();
                assertThat(C2).isNull();
                assertThat(C3).isNull();
                assertThat(component4).isNull();
                assertThat(component5).isNull();
                assertThat(component6).isNull();
                assertThat(component7).isNull();
                assertThat(component8).isNull();
            });

            var ids = createEntities1(7);

            assertThat(inserted.getSize()).isEqualTo(7);
            assertThat(inserted.getData()).containsExactlyInAnyOrder(ids);
        }

        @Test
        void testRemoved() {
            var composition = composition(builder1);

            var removed = new IntBag(expected1.length);
            composition.removed((entityId, C1, C2, C3, component4, component5, component6, component7, component8) -> {
                removed.add(entityId);

                assertThat(C1).isNotNull();
                assertThat(C2).isNull();
                assertThat(C3).isNull();
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

    abstract class AbstractCompositionNTest<C extends BaseComposition> {

        interface TestConsumer<T> {
            void consume(int index, T result);
        }

        final Composition.Builder builder1 = Composition.all(C1.class).none(C8.class);
        final Composition.Builder builder8 = Composition.all(C8.class);
        final Composition.Builder builderAll = Composition.all(C1.class);

        int[] expected1;
        int[] expected8;

        int createEntity1() {
            return world.createEntity(new C1());
        }

        int[] createEntities1(int number) {
            var entities = new int[number];

            for (int i = 0; i < number; i++) {
                entities[i] = world.createEntity(new C1());
            }

            return entities;
        }

        int createEntity18() {
            return world.createEntity(new C1(), new C2(), new C3(), new C4(), new C5(), new C6(), new C7(), new C8());
        }

        int[] createEntities18(int number) {
            var entities = new int[number];

            for (int i = 0; i < number; i++) {
                entities[i] = world.createEntity(new C1(), new C2(), new C3(), new C4(), new C5(), new C6(), new C7(), new C8());
            }

            return entities;
        }

        @BeforeEach
        void setup() {
            this.expected1 = createEntities1(10);
            this.expected8 = createEntities18(7);

            world.process();
        }

        abstract C composition(Composition.Builder builder);

        abstract <T, R> Composition.Of<?> composition(Composition.Builder builder, ComponentType<T, R> first);

        abstract <T, R> void inserted(Composition.Of<?> composition, ComponentType<T, R> type, Composition.Of1.Consumer<R> consumer);

        abstract <T, R> void removed(Composition.Of<?> composition, ComponentType<T, R> type, Composition.Of1.Consumer<R> consumer);

        abstract <T, R> void process(Composition.Of<?> composition, ComponentType<T, R> type, Composition.Of1.Consumer<R> consumer);

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
            var ids = createEntities1(5);

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

        @Nested
        class WildcardTypeTest {

            @Nested
            class ProcessTest extends AbstractTest {
                @Override
                <T, R> int[] perform(Object[][] components, Composition.Of<?> composition, ComponentType<T, R> type, TestConsumer<R> consumer) {
                    var s = components.length;
                    var entities = new int[s];

                    for (int i = 0; i < s; i++) {
                        entities[i] = world.createEntity(components[i]);
                    }

                    process(composition, type, (entityId, result) -> {
                        for (int i = 0; i < s; i++) {
                            if (entityId == entities[i]) {
                                consumer.consume(i, result);
                            }
                        }
                    });

                    return entities;
                }
            }

            @Nested
            class InsertedTest extends AbstractTest {
                @Override
                <T, R> int[] perform(Object[][] components, Composition.Of<?> composition, ComponentType<T, R> type, TestConsumer<R> consumer) {
                    var s = components.length;
                    var entities = new int[s];

                    var count = new AtomicInteger(0);

                    inserted(composition, type, (entityId, result) -> {
                        var i = count.getAndIncrement();
                        if (i < s) {
                            consumer.consume(i, result);
                        }
                    });

                    for (int i = 0; i < s; i++) {
                        entities[i] = world.createEntity(components[i]);
                    }

                    return entities;
                }
            }

            @Nested
            class RemovedTest extends AbstractTest {
                @Override
                <T, R> int[] perform(Object[][] components, Composition.Of<?> composition, ComponentType<T, R> type, TestConsumer<R> consumer) {
                    var s = components.length;
                    var entities = new int[s];

                    for (int i = 0; i < s; i++) {
                        entities[i] = world.createEntity(components[i]);
                        world.deleteEntity(entities[i]);
                    }

                    removed(composition, type, (entityId, result) -> {
                        for (int i = 0; i < s; i++) {
                            if (entityId == entities[i]) {
                                consumer.consume(i, result);
                            }
                        }
                    });

                    world.process();

                    return entities;
                }
            }

            abstract class AbstractTest {

                abstract <T, R> int[] perform(Object[][] components, Composition.Of<?> composition, ComponentType<T, R> type, TestConsumer<R> consumer);

                @Test
                void testResultSize() {
                    var type = wildcard(C1234.class);
                    var composition = composition(Composition.all(C1.class), type);

                    var components = new Object[][] {
                            { new C1(), new C2(), new C3(), new C4(), new C5() },
                            { new C1(), new C2(), new C5() },
                            { new C5() }
                    };

                    perform(components, composition, type, (id, result) -> {
                        if (id == 0) {
                            assertThat(result.size()).as("size").isEqualTo(4);
                        } else if (id == 1) {
                            assertThat(result.size()).as("size").isEqualTo(2);
                        } else if (id == 2) {
                            assertThat(result.size()).as("size").isEqualTo(0);
                        }
                    });
                }

                @Test
                void testResultIsEmpty() {
                    var type = wildcard(C1234.class);
                    var composition = composition(Composition.all(C1.class), type);

                    var components = new Object[][] {
                            { new C1(), new C2(), new C3(), new C4(), new C5() },
                            { new C1(), new C2(), new C5() },
                            { new C5() }
                    };

                    perform(components, composition, type, (id, result) -> {
                        if (id == 0) {
                            assertThat(result.isEmpty()).as("isEmpty").isFalse();
                        } else if (id == 1) {
                            assertThat(result.isEmpty()).as("isEmpty").isFalse();
                        } else if (id == 2) {
                            assertThat(result.isEmpty()).as("isEmpty").isTrue();
                        }
                    });
                }

                @Test
                void testResultGetByIndex() {
                    var type = wildcard(C1234.class);
                    var composition = composition(Composition.all(C1.class), type);

                    var c1 = new C1();
                    var c2 = new C2();

                    var components = new Object[][] {
                            { c1, c2, new C5() },
                    };

                    perform(components, composition, type, (id, result) -> {
                        if (id == 0) {
                            assertThat(result.get(0)).as("get(0)").isIn(c1, c2);
                            assertThat(result.get(1)).as("get(1)").isIn(c1, c2);
                        }
                    });
                }

                @Test
                void testResultEnhancedForLoop() {
                    var type = wildcard(C1234.class);
                    var composition = composition(Composition.all(C1.class), type);

                    var c1 = new C1();
                    var c2 = new C2();

                    var components = new Object[][] {
                            { c1, c2, new C5() },
                    };

                    perform(components, composition, type, (id, result) -> {
                        if (id == 0) {
                            for (var component : result) {
                                assertThat(component).as("enhanced for loop").isIn(c1, c2);
                            }
                        }
                    });
                }

                @Test
                void testResultIterator() {
                    var type = wildcard(C1234.class);
                    var composition = composition(Composition.all(C1.class), type);

                    var c1 = new C1();
                    var c2 = new C2();

                    var components = new Object[][] {
                            { c1, c2, new C5() },
                    };

                    perform(components, composition, type, (id, result) -> {
                        if (id == 0) {
                            for (var iter = result.iterator(); iter.hasNext();) {
                                assertThat(iter.next()).as("iterator loop").isIn(c1, c2);
                            }
                            for (var iter = result.iterator(); iter.hasNext();) {
                                assertThat(iter.next()).as("iterator loop resets automatically").isIn(c1, c2);
                            }
                        }
                    });
                }

                @Test
                void testResultGetByClass() {
                    var type = wildcard(C1234.class);
                    var composition = composition(Composition.all(C1.class), type);

                    var c1 = new C1();
                    var c2 = new C2();

                    var components = new Object[][] {
                            { c1, c2, new C5() },
                    };

                    perform(components, composition, type, (id, result) -> {
                        if (id == 0) {
                            assertThat(result.get(C1.class)).as("get(C1.class)").isSameAs(c1);
                            assertThat(result.get(C2.class)).as("get(C2.class)").isSameAs(c2);
                            assertThat(result.get(C3.class)).as("get(C3.class)").isNull();
                            assertThat(result.get(C4.class)).as("get(C4.class)").isNull();
                        }
                    });
                }

                @Test
                void testResultReused() {
                    var type = wildcard(C1234.class);
                    var composition = composition(Composition.all(C1.class), type);

                    var components = new Object[][] {
                            { new C1(), new C5() },
                            { new C1(), new C5() },
                    };

                    var invocations = new AtomicInteger();
                    var results = new IdentityHashMap<Result<C1234>, Boolean>();

                    perform(components, composition, type, (id, result) -> {
                        if (id == 0 || id == 1) {
                            invocations.incrementAndGet();
                            results.put(result, true);
                        }
                    });

                    assertThat(invocations).hasValue(2);
                    assertThat(results).as("Result reused during single-threaded iteration").hasSize(1);
                }

            }

        }

        @Nested
        class RelationTest {

            @Nested
            class ProcessTest extends AbstractTest {
                @Override
                <T, R> int[] perform(Object[][] components, Composition.Of<?> composition, ComponentType<T, R> type, TestConsumer<R> consumer) {
                    var s = components.length;
                    var entities = new int[s];

                    for (int i = 0; i < s; i++) {
                        entities[i] = world.createEntity(components[i]);
                    }

                    process(composition, type, (entityId, result) -> {
                        for (int i = 0; i < s; i++) {
                            if (entityId == entities[i]) {
                                consumer.consume(i, result);
                            }
                        }
                    });

                    return entities;
                }
            }

            @Nested
            class InsertedTest extends AbstractTest {
                @Override
                <T, R> int[] perform(Object[][] components, Composition.Of<?> composition, ComponentType<T, R> type, TestConsumer<R> consumer) {
                    var s = components.length;
                    var entities = new int[s];

                    var count = new AtomicInteger(0);

                    inserted(composition, type, (entityId, result) -> {
                        var i = count.getAndIncrement();
                        if (i < s) {
                            consumer.consume(i, result);
                        }
                    });

                    for (int i = 0; i < s; i++) {
                        entities[i] = world.createEntity(components[i]);
                    }

                    return entities;
                }
            }

            @Nested
            class RemovedTest extends AbstractTest {
                @Override
                <T, R> int[] perform(Object[][] components, Composition.Of<?> composition, ComponentType<T, R> type, TestConsumer<R> consumer) {
                    var s = components.length;
                    var entities = new int[s];

                    for (int i = 0; i < s; i++) {
                        entities[i] = world.createEntity(components[i]);
                        world.deleteEntity(entities[i]);
                    }

                    removed(composition, type, (entityId, result) -> {
                        for (int i = 0; i < s; i++) {
                            if (entityId == entities[i]) {
                                consumer.consume(i, result);
                            }
                        }
                    });

                    world.process();

                    return entities;
                }
            }

            abstract class AbstractTest {

                abstract <T, R> int[] perform(Object[][] components, Composition.Of<?> composition, ComponentType<T, R> type, TestConsumer<R> consumer);

                @Nested
                class ComponentRelationTest {

                    @Nested
                    class MatchTest {

                        @Test
                        void testMatchComponentRelationType() {
                            var type = relation(RelationshipComponent.class, Target.class);
                            var composition = composition(Composition.all(type), type);

                            var relationship = new RelationshipComponent(10);

                            var components = new Object[][] {
                                    { new C1(), relation(relationship, new Target(1)) },
                                    { new C1(), relation(relationship, new Target(2)), relation(relationship, new Target(3)) },
                                    { relation(new ExclusiveRelationship(11), new Target(1)) },
                                    { new C5() }
                            };

                            perform(components, composition, type, (id, result) -> {
                                if (id == 0) {
                                    assertThat(result)
                                            .extracting("relationship.value", "target.value")
                                            .containsExactlyInAnyOrder(
                                                    tuple(10, 1));
                                } else if (id == 1) {
                                    assertThat(result)
                                            .extracting("relationship.value", "target.value")
                                            .containsExactlyInAnyOrder(
                                                    tuple(10, 2),
                                                    tuple(10, 3));
                                } else {
                                    fail("Unexpected entity %d with result '%s'", id, result);
                                }
                            });
                        }

                        @Test
                        void testMatchExclusiveComponentRelationType() {
                            var type = exclusiveRelation(ExclusiveRelationship.class, Target.class);
                            var composition = composition(Composition.all(type), type);

                            var relationship = new ExclusiveRelationship(10);

                            var components = new Object[][] {
                                    { new C1(), relation(relationship, new Target(1)) },
                                    { new C1(), relation(relationship, new Target(2)), relation(relationship, new Target(3)) },
                                    { relation(new RelationshipComponent(11), new Target(1)) },
                                    { new C5() }
                            };

                            perform(components, composition, type, (id, result) -> {
                                if (id == 0) {
                                    assertThat(result)
                                            .extracting("relationship.value", "target.value")
                                            .contains(10, 1);
                                } else if (id == 1) {
                                    assertThat(result)
                                            .extracting("relationship.value", "target.value")
                                            .contains(10, 3);
                                } else {
                                    fail("Unexpected entity %d with result '%s'", id, result);
                                }
                            });
                        }

                    }

                    @Nested
                    class RetrieveTest {

                        @Test
                        void testRetrieveComponentRelations() {
                            var type = relation(RelationshipComponent.class, Target.class);
                            var composition = composition(Composition.all(C1.class), type);

                            var relationship = new RelationshipComponent(10);

                            var components = new Object[][] {
                                    { new C1(), relation(relationship, new Target(1)) },
                                    { new C1(), relation(relationship, new Target(2)), relation(relationship, new Target(3)) },
                                    { new C5() }
                            };

                            perform(components, composition, type, (id, result) -> {
                                if (id == 0) {
                                    assertThat(result)
                                            .extracting("relationship.value", "target.value")
                                            .containsExactlyInAnyOrder(
                                                    tuple(10, 1));
                                } else if (id == 1) {
                                    assertThat(result)
                                            .extracting("relationship.value", "target.value")
                                            .containsExactlyInAnyOrder(
                                                    tuple(10, 2),
                                                    tuple(10, 3));
                                } else if (id == 2) {
                                    assertThat(result).isEmpty();
                                }
                            });
                        }

                        @Test
                        void testRetrieveComponentRelations_DifferentRelationships() {
                            var type = relation(RelationshipComponent.class, Target.class);
                            var composition = composition(Composition.all(C1.class), type);

                            var relationship1 = new RelationshipComponent(10);
                            var relationship2 = new RelationshipComponent(11);

                            var components = new Object[][] {
                                    { new C1(), relation(relationship1, new Target(1)) },
                                    { new C1(), relation(relationship1, new Target(2)), relation(relationship2, new Target(3)) },
                                    { new C5() }
                            };

                            perform(components, composition, type, (id, result) -> {
                                if (id == 0) {
                                    assertThat(result)
                                            .extracting("relationship.value", "target.value")
                                            .containsExactlyInAnyOrder(
                                                    tuple(10, 1));
                                } else if (id == 1) {
                                    assertThat(result)
                                            .extracting("relationship.value", "target.value")
                                            .containsExactlyInAnyOrder(
                                                    tuple(10, 2),
                                                    tuple(11, 3));
                                } else if (id == 2) {
                                    assertThat(result).isEmpty();
                                }
                            });
                        }

                        @Test
                        void testRetrieveExclusiveComponentRelation() {
                            var type = exclusiveRelation(ExclusiveRelationship.class, Target.class);
                            var composition = composition(Composition.all(C1.class), type);

                            var relationship1 = new ExclusiveRelationship(10);
                            var relationship2 = new ExclusiveRelationship(11);

                            var components = new Object[][] {
                                    { new C1(), relation(relationship1, new Target(1)) },
                                    { new C1(), relation(relationship1, new Target(2)), relation(relationship2, new Target(3)) },
                                    { new C5() }
                            };

                            perform(components, composition, type, (id, result) -> {
                                if (id == 0) {
                                    assertThat(result)
                                            .extracting("relationship.value", "target.value")
                                            .contains(10, 1);
                                } else if (id == 1) {
                                    assertThat(result)
                                            .extracting("relationship.value", "target.value")
                                            .contains(11, 3);
                                } else if (id == 2) {
                                    assertThat(result).isNull();
                                }
                            });
                        }

                    }

                }

                @Nested
                class EntityRelationTest {

                    @Nested
                    class MatchTest {

                        @Test
                        void testMatchEntityRelationType() {
                            var type = relation(RelationshipComponent.class);
                            var composition = composition(Composition.all(type), type);

                            var relationship = new RelationshipComponent(10);

                            var target1 = world.createEntity();
                            var target2 = world.createEntity();
                            var target3 = world.createEntity();

                            var components = new Object[][] {
                                    { new C1(), relation(relationship, target1) },
                                    { new C1(), relation(relationship, target2), relation(relationship, target3) },
                                    { relation(new ExclusiveRelationship(11), target1) },
                                    { new C5() }
                            };

                            perform(components, composition, type, (id, result) -> {
                                if (id == 0) {
                                    assertThat(result)
                                            .extracting("relationship.value", "target")
                                            .containsExactlyInAnyOrder(
                                                    tuple(10, target1));
                                } else if (id == 1) {
                                    assertThat(result)
                                            .extracting("relationship.value", "target")
                                            .containsExactlyInAnyOrder(
                                                    tuple(10, target2),
                                                    tuple(10, target3));
                                } else {
                                    fail("Unexpected entity %d with result '%s'", id, result);
                                }
                            });
                        }

                        @Test
                        void testMatchExclusiveEntityRelationType() {
                            var type = exclusiveRelation(ExclusiveRelationship.class);
                            var composition = composition(Composition.all(type), type);

                            var relationship = new ExclusiveRelationship(10);

                            var target1 = world.createEntity();
                            var target2 = world.createEntity();
                            var target3 = world.createEntity();

                            var components = new Object[][] {
                                    { new C1(), relation(relationship, target1) },
                                    { new C1(), relation(relationship, target2), relation(relationship, target3) },
                                    { relation(new RelationshipComponent(11), target1) },
                                    { new C5() }
                            };

                            perform(components, composition, type, (id, result) -> {
                                if (id == 0) {
                                    assertThat(result)
                                            .extracting("relationship.value", "target")
                                            .contains(10, target1);
                                } else if (id == 1) {
                                    assertThat(result)
                                            .extracting("relationship.value", "target")
                                            .contains(10, target3);
                                } else {
                                    fail("Unexpected entity %d with result '%s'", id, result);
                                }
                            });
                        }

                    }

                    @Nested
                    class RetrieveTest {

                        @Test
                        void testRetrieveEntityRelations() {
                            var type = relation(RelationshipComponent.class);
                            var composition = composition(Composition.all(C1.class), type);

                            var relationship = new RelationshipComponent(10);

                            var target1 = world.createEntity();
                            var target2 = world.createEntity();
                            var target3 = world.createEntity();

                            var components = new Object[][] {
                                    { new C1(), relation(relationship, target1) },
                                    { new C1(), relation(relationship, target2), relation(relationship, target3) },
                                    { new C5() }
                            };

                            perform(components, composition, type, (id, result) -> {
                                if (id == 0) {
                                    assertThat(result)
                                            .extracting("relationship.value", "target")
                                            .containsExactlyInAnyOrder(
                                                    tuple(10, target1));
                                } else if (id == 1) {
                                    assertThat(result)
                                            .extracting("relationship.value", "target")
                                            .containsExactlyInAnyOrder(
                                                    tuple(10, target2),
                                                    tuple(10, target3));
                                } else if (id == 2) {
                                    assertThat(result).isEmpty();
                                }
                            });
                        }

                        @Test
                        void testRetrieveEntityRelations_DifferentRelationships() {
                            var type = relation(RelationshipComponent.class);
                            var composition = composition(Composition.all(C1.class), type);

                            var relationship1 = new RelationshipComponent(10);
                            var relationship2 = new RelationshipComponent(11);

                            var target1 = world.createEntity();
                            var target2 = world.createEntity();
                            var target3 = world.createEntity();

                            var components = new Object[][] {
                                    { new C1(), relation(relationship1, target1) },
                                    { new C1(), relation(relationship1, target2), relation(relationship2, target3) },
                                    { new C5() }
                            };

                            perform(components, composition, type, (id, result) -> {
                                if (id == 0) {
                                    assertThat(result)
                                            .extracting("relationship.value", "target")
                                            .containsExactlyInAnyOrder(
                                                    tuple(10, target1));
                                } else if (id == 1) {
                                    assertThat(result)
                                            .extracting("relationship.value", "target")
                                            .containsExactlyInAnyOrder(
                                                    tuple(10, target2),
                                                    tuple(11, target3));
                                } else if (id == 2) {
                                    assertThat(result).isEmpty();
                                }
                            });
                        }

                        @Test
                        void testRetrieveExclusiveEntityRelation() {
                            var type = exclusiveRelation(ExclusiveRelationship.class);
                            var composition = composition(Composition.all(C1.class), type);

                            var relationship1 = new ExclusiveRelationship(10);
                            var relationship2 = new ExclusiveRelationship(11);

                            var target1 = world.createEntity();
                            var target2 = world.createEntity();
                            var target3 = world.createEntity();

                            var components = new Object[][] {
                                    { new C1(), relation(relationship1, target1) },
                                    { new C1(), relation(relationship1, target2), relation(relationship2, target3) },
                                    { new C5() }
                            };

                            perform(components, composition, type, (id, result) -> {
                                if (id == 0) {
                                    assertThat(result)
                                            .extracting("relationship.value", "target")
                                            .contains(10, target1);
                                } else if (id == 1) {
                                    assertThat(result)
                                            .extracting("relationship.value", "target")
                                            .contains(11, target3);
                                } else if (id == 2) {
                                    assertThat(result).isNull();
                                }
                            });
                        }

                    }

                }

            }

        }

    }

    @Nested
    class WorldTest {

        @Test
        void testInserted_WhenEntityCreated() {
            var inserted = new ArrayList<Integer>();
            var composition = world.createComposition(Composition.all(C1.class));
            composition.inserted(inserted::add);

            // Call
            var entityId = world.createEntity(new C1());

            // Verify
            assertThat(inserted).containsExactly(entityId);
        }

        @Test
        void testInserted_WhenEntityCompositionChanged_NotCalledIfNotProcessed() {
            var mapper = world.getComponents(C1.class);

            var inserted = new ArrayList<Integer>();
            var composition = world.createComposition(Composition.all(C1.class));
            composition.inserted(inserted::add);

            var entityId = world.createEntity();
            assertThat(inserted).isEmpty();

            // Call
            mapper.add(entityId, new C1());

            // Verify
            assertThat(inserted).isEmpty();
        }

        @Test
        void testInserted_WhenEntityCompositionChanged_CalledWhenProcessed() {
            var mapper = world.getComponents(C1.class);

            var inserted = new ArrayList<Integer>();
            var composition = world.createComposition(Composition.all(C1.class));
            composition.inserted(inserted::add);

            var entityId = world.createEntity();
            assertThat(inserted).isEmpty();

            // Call
            mapper.add(entityId, new C1());
            world.process();

            // Verify
            assertThat(inserted).containsExactly(entityId);

        }

        @Test
        void testProcess_WhenEntityQueuedForDeletion_StillProcessesEntity() {
        }

        @Test
        void testRemoved_WhenEntityCompositionChanged_NotCalledIfNotProcessed() {
            var mapper = world.getComponents(C1.class);

            var removed = new ArrayList<Integer>();
            var composition = world.createComposition(Composition.all(C1.class));
            composition.removed(removed::add);

            var entityId = world.createEntity(new C1());

            // Call
            mapper.remove(entityId);

            // Verify
            assertThat(removed).isEmpty();
        }

        @Test
        void testRemoved_WhenEntityCompositionChanged_CalledWhenProcessed() {
            var mapper = world.getComponents(C1.class);

            var removed = new ArrayList<Integer>();
            var composition = world.createComposition(Composition.all(C1.class));
            composition.removed(removed::add);

            var entityId = world.createEntity(new C1());

            // Call
            mapper.remove(entityId);
            world.process();

            // Verify
            assertThat(removed).containsExactly(entityId);
        }

        @Test
        void testRemoved_WhenEntityDeleted_NotCalledIfNotProcessed() {
            var removed = new ArrayList<Integer>();
            var composition = world.createComposition(Composition.all(C1.class));
            composition.removed(removed::add);

            var entityId = world.createEntity(new C1());

            // Call
            world.deleteEntity(entityId);

            // Verify
            assertThat(removed).isEmpty();
        }

        @Test
        void testRemoved_WhenEntityDeleted_CalledWhenProcessed() {
            var removed = new ArrayList<Integer>();
            var composition = world.createComposition(Composition.all(C1.class));
            composition.removed(removed::add);

            var entityId = world.createEntity(new C1());

            // Call
            world.deleteEntity(entityId);
            world.process();

            // Verify
            assertThat(removed).containsExactly(entityId);

        }

    }

    @Nested
    class CompositionTest {

        @Nested
        class AllSpecTest extends AbstractSpecTest {

            @Override
            Composition createComposition() {
                return world.createComposition(Composition.all(C1.class, C2.class));
            }

            @Override
            int createInterestedEntity() {
                return world.createEntity(new C1(), new C2());
            }

            @Override
            int createUninterestedEntity() {
                return world.createEntity(new C1(), new C3());
            }

        }

        @Nested
        class OneSpecTest extends AbstractSpecTest {

            @Override
            Composition createComposition() {
                return world.createComposition(Composition.one(C1.class, C2.class));
            }

            @Override
            int createInterestedEntity() {
                return world.createEntity(new C1(), new C3());
            }

            @Override
            int createUninterestedEntity() {
                return world.createEntity(new C3());
            }

        }

        @Nested
        class NoneSpecTest extends AbstractSpecTest {

            @Override
            Composition createComposition() {
                return world.createComposition(Composition.none(C1.class));
            }

            @Override
            int createInterestedEntity() {
                return world.createEntity(new C2(), new C3());
            }

            @Override
            int createUninterestedEntity() {
                return world.createEntity(new C1());
            }

        }

        @Nested
        class ComplexSpecTest extends AbstractSpecTest {

            @Override
            Composition createComposition() {
                return world.createComposition(Composition.one(C1.class, C2.class).none(C3.class));
            }

            @Override
            int createInterestedEntity() {
                return world.createEntity(new C1());
            }

            @Override
            int createUninterestedEntity() {
                return world.createEntity(new C1(), new C3());
            }

        }

        abstract class AbstractSpecTest {

            abstract Composition createComposition();

            abstract int createInterestedEntity();

            abstract int createUninterestedEntity();

            @Test
            void testProcess() {
                // Setup
                var entities = new IntBag(3);

                createUninterestedEntity();
                var interested1 = createInterestedEntity();
                var interested2 = createInterestedEntity();
                createUninterestedEntity();

                var composition = createComposition();

                // Call
                composition.process(entities::add);

                // Verify
                assertThat(entities.getSize()).as("size").isEqualTo(2);
                assertThat(entities.contains(interested1)).as("contains interested1").isTrue();
                assertThat(entities.contains(interested2)).as("contains interested2").isTrue();
            }

            @Test
            void testCreateEntity() {
                // Setup
                var entities = new IntBag(3);

                createUninterestedEntity();
                createInterestedEntity();
                createInterestedEntity();
                createUninterestedEntity();

                var composition = createComposition();

                // Call
                composition.inserted(entities::add);

                createUninterestedEntity();
                var interested3 = createInterestedEntity();
                var interested4 = createInterestedEntity();
                createUninterestedEntity();

                // Verify
                assertThat(entities.getSize()).as("size").isEqualTo(2);
                assertThat(entities.contains(interested3)).as("contains interested3").isTrue();
                assertThat(entities.contains(interested4)).as("contains interested4").isTrue();
            }

            @Test
            void testRemoved() {
                // Setup
                var entities = new IntBag(3);

                var uninterested1 = createUninterestedEntity();
                var interested1 = createInterestedEntity();
                createInterestedEntity();
                createUninterestedEntity();

                var composition = createComposition();

                // Call
                composition.removed(entities::add);

                var interested3 = createInterestedEntity();

                world.deleteEntity(uninterested1);
                world.deleteEntity(interested1);
                world.deleteEntity(interested3);
                world.process();

                // Verify
                assertThat(entities.getSize()).as("size").isEqualTo(2);
                assertThat(entities.contains(interested1)).as("contains interested1").isTrue();
                assertThat(entities.contains(interested3)).as("contains interested3").isTrue();
            }

            @Test
            void testProcessWithRemovedEntity_BeforeWorldProcess() {
                // Setup
                var entities = new IntBag(3);

                createUninterestedEntity();
                var interested1 = createInterestedEntity();
                var interested2 = createInterestedEntity();
                createUninterestedEntity();

                var composition = createComposition();

                // Call
                world.deleteEntity(interested1);
                composition.process(entities::add);

                // Verify
                assertThat(entities.getSize()).as("size").isEqualTo(2);
                assertThat(entities.contains(interested1)).as("contains interested1").isTrue();
                assertThat(entities.contains(interested2)).as("contains interested2").isTrue();
            }

            @Test
            void testProcessWithRemovedEntity_AfterWorldProcess() {
                // Setup
                var entities = new IntBag(3);

                createUninterestedEntity();
                var interested1 = createInterestedEntity();
                var interested2 = createInterestedEntity();
                createUninterestedEntity();

                var composition = createComposition();

                // Call
                world.deleteEntity(interested1);
                world.process();

                composition.process(entities::add);

                // Verify
                assertThat(entities.getSize()).as("size").isEqualTo(1);
                assertThat(entities.contains(interested2)).as("contains interested2").isTrue();
            }

        }

    }

    @Nested
    class CreateEntityMutationsTest {

        PooledComponentMapper<P1> pooled1;
        PooledComponentMapper<P2> pooled2;
        PooledComponentMapper<P3> pooled3;

        @BeforeEach
        void setupMappers() {
            this.pooled1 = world.getPooledComponents(P1.class);
            this.pooled2 = world.getPooledComponents(P2.class);
            this.pooled3 = world.getPooledComponents(P3.class);
        }

        @Test
        void testDeletionDuringCreation() {
            // Setup composition listeners
            var composition1 = world.createComposition(Composition.all(P1.class));
            composition1.inserted(pooled2::add);

            var composition2 = world.createComposition(Composition.all(P2.class));
            composition2.inserted(pooled3::add);

            var composition3 = world.createComposition(Composition.all(P3.class));
            composition3.inserted(world::deleteEntity);

            // Call
            var c1 = pooled1.getInstance();

            assertThatThrownBy(() -> world.createEntity(c1))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("deleted during creation");
        }

        @Test
        void testMutationDuringCreation_WhenListenersModifyComponents_WorksIfWorldIsProcessed() {
            // Setup composition listeners
            var composition1 = world.createComposition(Composition.all(P1.class));
            composition1.inserted(pooled2::add);

            var composition2 = world.createComposition(Composition.all(P2.class));
            composition2.inserted(pooled3::add);

            var composition3 = world.createComposition(Composition.all(P3.class));
            composition3.inserted(pooled1::remove);

            // Call
            var entityId = world.createEntity(pooled1.getInstance());

            assertThat(world.process(1)).isTrue();

            // Verify
            verifyHasComposition(entityId, Composition.all(P2.class, P3.class).none(P1.class));

            verifyDoesNotHaveComponents(entityId, P1.class);
            verifyHasComponents(entityId, P2.class, P3.class);
        }

        @Test
        void testMutationDuringCreation_WhenListenersModifyComponents_ListenersCalledRecursivly() {
            // Setup composition listeners
            var composition1 = world.createComposition(Composition.all(P1.class));
            composition1.inserted(pooled2::add);

            var composition2 = world.createComposition(Composition.all(P2.class));
            composition2.inserted(pooled3::add);

            var composition3 = world.createComposition(Composition.all(P3.class));
            composition3.inserted(pooled1::remove);

            // Call
            var entityId = world.createEntity(pooled1.getInstance());

            // Verify
            verifyHasComposition(entityId, Composition.all(P2.class, P3.class).none(P1.class));

            verifyDoesNotHaveComponents(entityId, P1.class);
            verifyHasComponents(entityId, P2.class, P3.class);
        }

        @Test
        void testMutationAfterCreation_WhenListenersModifyComponents_WorksIfWorldIsProcessed() {
            // Setup composition listeners
            var composition1 = world.createComposition(Composition.all(P1.class));
            composition1.inserted(pooled2::add);

            var composition2 = world.createComposition(Composition.all(P2.class));
            composition2.inserted(pooled3::add);

            var composition3 = world.createComposition(Composition.all(P3.class));
            composition3.inserted(pooled1::remove);

            var entityId = world.createEntity();

            // Call
            pooled1.add(entityId);

            // Verify first process
            assertThat(world.process(1)).isFalse();
            verifyHasComposition(entityId, Composition.all(P1.class).none(P2.class, P3.class));

            verifyHasComponents(entityId, P1.class);
            verifyHasComponents(entityId, P2.class);
            verifyDoesNotHaveComponents(entityId, P3.class);

            // Verify second process
            assertThat(world.process(1)).isFalse();
            verifyHasComposition(entityId, Composition.all(P1.class, P2.class).none(P3.class));

            verifyHasComponents(entityId, P1.class);
            verifyHasComponents(entityId, P2.class);
            verifyHasComponents(entityId, P3.class);

            // Verify third process
            assertThat(world.process(1)).isFalse();
            verifyHasComposition(entityId, Composition.all(P1.class, P2.class, P3.class));

            verifyHasComponents(entityId, P1.class);
            verifyHasComponents(entityId, P2.class);
            verifyHasComponents(entityId, P3.class);

            // Verify last process
            assertThat(world.process(1)).isTrue();
            verifyHasComposition(entityId, Composition.all(P2.class, P3.class).none(P1.class));

            verifyDoesNotHaveComponents(entityId, P1.class);
            verifyHasComponents(entityId, P2.class);
            verifyHasComponents(entityId, P3.class);
        }

        @Test
        void testMutationAfterCreation_WhenListenersModifyComponents_ListenersNotCalledRecirsuvly() {
            // Setup composition listeners
            var composition1 = world.createComposition(Composition.all(P1.class));
            composition1.inserted(pooled2::add);

            var composition2 = world.createComposition(Composition.all(P2.class));
            composition2.inserted(pooled3::add);

            var composition3 = world.createComposition(Composition.all(P3.class));
            composition3.inserted(pooled1::remove);

            var entityId = world.createEntity();

            // Call
            pooled1.add(entityId);

            // Verify
            verifyHasComposition(entityId, Composition.none(P1.class, P2.class, P3.class));

            verifyHasComponents(entityId, P1.class);
            verifyDoesNotHaveComponents(entityId, P2.class);
            verifyDoesNotHaveComponents(entityId, P3.class);
        }

    }

    @Nested
    class EntityRelationFetchTypeTest {

        @Test
        void testEntityRelationFetchType() {
            var fetchType = ComponentType.exclusiveRelation(ExclusiveRelationship.class, ComponentType.componentSet(MyComponentSet.class));
            var composition = world.createComposition(Composition.all(P1.class), fetchType);

            var nestedTarget1 = world.createEntity();
            var nestedTarget2 = world.createEntity();

            var target = world.createEntity(
                    Relation.create(new ExclusiveRelationship(1), new Target(10)),
                    Relation.create(new RelationshipComponent(2), new Target(20)),
                    Relation.create(new RelationshipComponent(3), new Target(30)),
                    Relation.create(new ExclusiveRelationship(4), nestedTarget1),
                    Relation.create(new RelationshipComponent(5), nestedTarget1),
                    Relation.create(new RelationshipComponent(6), nestedTarget2),
                    new C1(), new C2(), new C3(), new C4());

            var entityId = world.createEntity(new P1(), Relation.create(new ExclusiveRelationship(42), target));

            var processed = new AtomicBoolean(false);
            composition.process((id, relation) -> {
                assertThat(id).isEqualTo(entityId);

                assertThat(relation).isNotNull();
                assertThat(relation.target()).isEqualTo(target);

                var components = relation.data();
                assertThat(components).isNotNull();
                assertThat(components.c1()).isNotNull();
                assertThat(components.componentRelation()).extracting("relationship.value", "target.value").contains(1, 10);
                assertThat(components.componentRelations()).extracting("relationship.value", "target.value").containsExactlyInAnyOrder(tuple(2, 20), tuple(3, 30));
                assertThat(components.entityRelation()).extracting("relationship.value", "target").contains(4, nestedTarget1);
                assertThat(components.entityRelations()).extracting("relationship.value", "target").containsExactlyInAnyOrder(tuple(5, nestedTarget1), tuple(6, nestedTarget2));
                assertThat(components.c1234()).hasSize(4);

                processed.set(true);
            });

            assertThat(processed.get()).isTrue();
        }

    }

    @Nested
    class ComponentSetTest {

        @Test
        void testRetrieveComponentSet() {
            var componentSet = ComponentType.componentSet(MyComponentSet.class);
            var composition = world.createComposition(Composition.all(C1.class), componentSet);

            var target1 = world.createEntity();
            var target2 = world.createEntity();

            var entityId = world.createEntity(new C1(),
                    Relation.create(new ExclusiveRelationship(1), new Target(10)),
                    Relation.create(new RelationshipComponent(2), new Target(20)),
                    Relation.create(new RelationshipComponent(3), new Target(30)),
                    Relation.create(new ExclusiveRelationship(4), target1),
                    Relation.create(new RelationshipComponent(5), target1),
                    Relation.create(new RelationshipComponent(6), target2),
                    new C2(), new C3(), new C4());

            var processed = new AtomicBoolean(false);
            composition.process((id, components) -> {
                assertThat(id).isEqualTo(entityId);

                assertThat(components.c1()).isNotNull();
                assertThat(components.componentRelation()).extracting("relationship.value", "target.value").contains(1, 10);
                assertThat(components.componentRelations()).extracting("relationship.value", "target.value").containsExactlyInAnyOrder(tuple(2, 20), tuple(3, 30));
                assertThat(components.entityRelation()).extracting("relationship.value", "target").contains(4, target1);
                assertThat(components.entityRelations()).extracting("relationship.value", "target").containsExactlyInAnyOrder(tuple(5, target1), tuple(6, target2));
                assertThat(components.c1234()).hasSize(4);

                processed.set(true);
            });

            assertThat(processed.get()).isTrue();
        }

    }

    private void verifyHasComposition(int entityId, Composition.Builder builder) {
        var composition = world.createComposition(builder);
        assertThat(composition.isInterested(entityId)).isTrue();
    }

    interface C1234 {
    }

    record C1() implements C1234 {
    }

    record C2() implements C1234 {
    }

    private record C3() implements C1234 {
    }

    private record C4() implements C1234 {
    }

    private record C5() {
    }

    private record C6() {
    }

    private record C7() {
    }

    private record C8() {
    }

    public record P1() implements Pooled {
    }

    public record P2() implements Pooled {
    }

    public record P3() implements Pooled {
    }

    record RelationshipComponent(int value) {
    }

    record ExclusiveRelationship(int value) implements Exclusive {
    }

    record Target(int value) {
    }

    record Target2(int value) {
    }

    public interface MyComponentSet extends ComponentSet {

        ComponentSetData<MyComponentSet> DATA = MyComponentSetImpl.DATA;

        C1 c1();

        ComponentRelation<ExclusiveRelationship, Target> componentRelation();

        ComponentRelationResult<RelationshipComponent, Target> componentRelations();

        EntityRelation<ExclusiveRelationship> entityRelation();

        EntityRelationResult<RelationshipComponent> entityRelations();

        ComponentResult<C1234> c1234();
    }

}

class MyComponentSetImpl implements MyComponentSet {

    static final ComponentSetData<MyComponentSet> DATA = ComponentSet.builder(MyComponentSetImpl::factory)
            .add(new ComponentAccessor<>(MyComponentSet::c1) {})
            .add(new ComponentAccessor<>(MyComponentSet::componentRelation) {})
            .add(new ComponentAccessor<>(MyComponentSet::componentRelations) {})
            .add(new ComponentAccessor<>(MyComponentSet::entityRelation) {})
            .add(new ComponentAccessor<>(MyComponentSet::entityRelations) {})
            .add(new ComponentAccessor<>(MyComponentSet::c1234) {})
            .build();

    private final int entityId;
    private final C1 c1;
    private final ComponentRelation<ExclusiveRelationship, Target> componentRelation;
    private final ComponentRelationResult<RelationshipComponent, Target> componentRelations;
    private final EntityRelation<ExclusiveRelationship> entityRelation;
    private final EntityRelationResult<RelationshipComponent> entityRelations;
    private final ComponentResult<C1234> c1234;

    @SuppressWarnings("unchecked")
    private static MyComponentSet factory(int entityId, Object[] components) {
        return new MyComponentSetImpl(entityId,
                (C1) components[0],
                (ComponentRelation<ExclusiveRelationship, Target>) components[1],
                (ComponentRelationResult<RelationshipComponent, Target>) components[2],
                (EntityRelation<ExclusiveRelationship>) components[3],
                (EntityRelationResult<RelationshipComponent>) components[4],
                (ComponentResult<C1234>) components[5]);
    }

    MyComponentSetImpl(int entityId, C1 c1, ComponentRelation<ExclusiveRelationship, Target> componentRelation, ComponentRelationResult<RelationshipComponent, Target> componentRelations,
            EntityRelation<ExclusiveRelationship> entityRelation, EntityRelationResult<RelationshipComponent> entityRelations, ComponentResult<C1234> c1234) {

        this.entityId = entityId;
        this.c1 = c1;
        this.componentRelation = componentRelation;
        this.componentRelations = componentRelations;
        this.entityRelation = entityRelation;
        this.entityRelations = entityRelations;
        this.c1234 = c1234;
    }

    @Override
    public int entityId() {
        return entityId;
    }

    @Override
    public C1 c1() {
        return c1;
    }

    @Override
    public ComponentRelation<ExclusiveRelationship, Target> componentRelation() {
        return componentRelation;
    }

    @Override
    public ComponentRelationResult<RelationshipComponent, Target> componentRelations() {
        return componentRelations;
    }

    @Override
    public EntityRelation<ExclusiveRelationship> entityRelation() {
        return entityRelation;
    }

    @Override
    public EntityRelationResult<RelationshipComponent> entityRelations() {
        return entityRelations;
    }

    @Override
    public ComponentResult<C1234> c1234() {
        return c1234;
    }

}
