package de.schosin.ecs.plugins.composition.manager;

import static de.schosin.ecs.api.components.types.ComponentType.wildcard;
import static de.schosin.ecs.api.components.types.ComponentType.wildcardRelation;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;
import static org.assertj.core.api.Assertions.tuple;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import de.schosin.ecs.api.Pooled;
import de.schosin.ecs.api.World;
import de.schosin.ecs.api.components.ComponentSetConfig;
import de.schosin.ecs.api.components.Relation;
import de.schosin.ecs.api.components.Relation.ComponentRelation;
import de.schosin.ecs.api.components.Relation.EntityRelation;
import de.schosin.ecs.api.components.Relation.Exclusive;
import de.schosin.ecs.api.components.Relations.ComponentRelations;
import de.schosin.ecs.api.components.Relations.EntityRelations;
import de.schosin.ecs.api.components.Result;
import de.schosin.ecs.api.components.Result.ComponentResult;
import de.schosin.ecs.api.components.mappers.ComponentMapper.PooledComponentMapper;
import de.schosin.ecs.api.components.mappers.Components;
import de.schosin.ecs.api.components.mappers.CustomComponentMapper;
import de.schosin.ecs.api.components.types.ComponentType;
import de.schosin.ecs.api.components.types.CustomComponentType;
import de.schosin.ecs.api.data.DataAccessor;
import de.schosin.ecs.engine.entities.EntityManager.ComponentsPredicate;
import de.schosin.ecs.engine.events.builtin.EntityEvent.BeforeEntityUpdateEvent;
import de.schosin.ecs.engine.events.builtin.EntityEvent.EntityInsertedEvent;
import de.schosin.ecs.engine.events.builtin.EntityEvent.EntityUpdatedEvent;
import de.schosin.ecs.plugins.composition.Composition;
import de.schosin.ecs.plugins.composition.Composition.Builder;
import de.schosin.ecs.plugins.composition.CompositionData;
import de.schosin.ecs.plugins.composition.CompositionData1;
import de.schosin.ecs.plugins.composition.CompositionData2;
import de.schosin.ecs.plugins.composition.CompositionData3;
import de.schosin.ecs.plugins.composition.CompositionData4;
import de.schosin.ecs.plugins.composition.CompositionData5;
import de.schosin.ecs.plugins.composition.CompositionData6;
import de.schosin.ecs.plugins.composition.CompositionData7;
import de.schosin.ecs.plugins.composition.CompositionData8;
import de.schosin.ecs.plugins.composition.Spec;
import de.schosin.ecs.plugins.data.types.DataType;
import de.schosin.ecs.storage.api.StorageEngineException;
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
    void testStream() {
        // Setup
        var entities = new ArrayList<Integer>(4);
        for (int i = 1; i <= 1000; i++) {
            entities.add(world.createEntity());
            entities.add(world.createEntity(new C1()));
            entities.add(world.createEntity(new C2()));
            entities.add(world.createEntity(new C1(), new C2()));
            entities.add(world.createEntity(new C1(), new C2(), new C3()));
        }

        var composition = compositionManager.createComposition(EMPTY);

        // Verify
        assertThat(composition.stream().mapToObj(Integer::valueOf).toList()).containsExactlyInAnyOrderElementsOf(entities);
    }

    @Test
    void testStreamParallelized() {
        // Setup
        var entities = new ArrayList<Integer>(4);
        for (int i = 1; i <= 1000; i++) {
            entities.add(world.createEntity());
            entities.add(world.createEntity(new C1()));
            entities.add(world.createEntity(new C2()));
            entities.add(world.createEntity(new C1(), new C2()));
            entities.add(world.createEntity(new C1(), new C2(), new C3()));
        }

        var composition = compositionManager.createComposition(EMPTY);

        // Verify
        assertThat(composition.stream().parallel().mapToObj(Integer::valueOf).toList()).containsExactlyInAnyOrderElementsOf(entities);
    }

    @Test
    void testParallelStream() {
        // Setup
        var entities = new ArrayList<Integer>(4);
        for (int i = 1; i <= 1000; i++) {
            entities.add(world.createEntity());
            entities.add(world.createEntity(new C1()));
            entities.add(world.createEntity(new C2()));
            entities.add(world.createEntity(new C1(), new C2()));
            entities.add(world.createEntity(new C1(), new C2(), new C3()));
        }

        var composition = compositionManager.createComposition(EMPTY);

        // Verify
        assertThat(composition.parallelStream().mapToObj(Integer::valueOf).toList()).containsExactlyInAnyOrderElementsOf(entities);
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
            var componentMask = storageEngine.getComponentMask(component(C1.class));
            var event = BeforeEntityUpdateEvent.get(42, null, componentMask);

            assertThatThrownBy(() -> eventManager.dispatchEvent(event)).isInstanceOf(NullPointerException.class);
        }

        @Test
        void testUpdatedEntities_WhenNullNewComposition_Throws() {
            var componentMask = storageEngine.getComponentMask(component(C1.class));
            var event = EntityUpdatedEvent.get(42, componentMask, null);

            assertThatThrownBy(() -> eventManager.dispatchEvent(event)).isInstanceOf(NullPointerException.class);
        }

        @Test
        void testUpdatedEntities_WhenPreviousComposition_CallsRemoved() {
            // Setup
            bagManager.ensureEntitySize(10000);

            var componentMask1 = storageEngine.getComponentMask(component(C1.class));
            var componentMask12 = storageEngine.getComponentMask(component(C1.class), component(C2.class));
            var componentMask2 = storageEngine.getComponentMask(component(C2.class));
            var componentMask3 = storageEngine.getComponentMask(component(C3.class));

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
            eventManager.dispatchEvent(BeforeEntityUpdateEvent.get(7, mask7, componentMask1));
            eventManager.dispatchEvent(EntityUpdatedEvent.get(7, mask7, componentMask1));

            eventManager.dispatchEvent(BeforeEntityUpdateEvent.get(42, mask42, componentMask12));
            eventManager.dispatchEvent(EntityUpdatedEvent.get(42, mask42, componentMask12));

            eventManager.dispatchEvent(BeforeEntityUpdateEvent.get(1337, mask1337, componentMask12));
            eventManager.dispatchEvent(EntityUpdatedEvent.get(1337, mask1337, componentMask12));

            eventManager.dispatchEvent(BeforeEntityUpdateEvent.get(9001, mask9001, componentMask3));
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
        void testAddComponentInRemoved_ShouldThrow() {
            var entityId = world.createEntity();

            var mapper1 = world.getPooledComponents(P1.class);

            var composition = world.createComposition(Composition.all());
            composition.removed(mapper1::add);

            // Call
            world.deleteEntity(entityId);

            try {
                world.process();
                // not throwing is okay
            } catch (StorageEngineException ex) {
                assertThat(ex)
                        .isInstanceOf(StorageEngineException.class)
                        .hasMessageContainingAll("entity %d".formatted(entityId), "not present in storage");
            }
        }

        @Test
        void testRemoveComponentInRemoved_ShouldThrow() {
            var entityId = world.createEntity(new P1());

            var mapper1 = world.getPooledComponents(P1.class);

            var composition = world.createComposition(Composition.all());
            composition.removed(mapper1::remove);

            // Call
            world.deleteEntity(entityId);

            try {
                world.process();
                // not throwing is okay
            } catch (StorageEngineException ex) {
                assertThat(ex)
                        .isInstanceOf(StorageEngineException.class)
                        .hasMessageContainingAll("entity %d".formatted(entityId), "not present in storage");
            }
        }

    }

    @Nested
    class Composition1Test extends AbstractCompositionDataTest<CompositionData1<C1>> {

        @Override
        CompositionData1<C1> composition(Composition.Builder builder) {
            return world.createComposition(builder, C1.class);
        }

        @Override
        <R> CompositionData1<R> composition(Builder builder, ComponentType<?, R> first) {
            return world.createComposition(builder, first);
        }

        @Override
        @SuppressWarnings("unchecked")
        <R> void inserted(CompositionData<?> of, ComponentType<?, R> type, TestConsumer<R> consumer) {
            var composition = (CompositionData1<R>) of;
            composition.inserted((entityId, result) -> consumer.consume(entityId, result));
        }

        @Override
        @SuppressWarnings("unchecked")
        <R> void removed(CompositionData<?> of, ComponentType<?, R> type, TestConsumer<R> consumer) {
            var composition = (CompositionData1<R>) of;
            composition.removed((entityId, result) -> consumer.consume(entityId, result));
        }

        @Override
        @SuppressWarnings("unchecked")
        <R> void process(CompositionData<?> of, ComponentType<?, R> type, TestConsumer<R> consumer) {
            var composition = (CompositionData1<R>) of;
            composition.process((entityId, result) -> consumer.consume(entityId, result));
        }

        @Test
        void testProcessOtherEntity() {
            composition(builder1).process(expected1[0], (entityId, c1) -> {
                assertThat(c1).isNotNull();
            });

            composition(builder1).process(expected8[0], (entityId, c1) -> {
                assertThat(c1).isNull();
            });

            composition(builder8).process(expected18[0], (entityId, c1) -> {
                assertThat(c1).isNotNull();
            });
        }

        @Test
        void testProcessOneComponent() {
            var composition = composition(builder1);

            var processed = new IntBag(expected1.length);
            composition.process((entityId, c1) -> {
                processed.add(entityId);

                assertThat(c1).isNotNull();
            });

            assertThat(processed.getSize()).isEqualTo(expected1.length);
            assertThat(processed.getData()).containsExactlyInAnyOrder(expected1);
        }

        @Test
        void testProcessOtherComponents() {
            var composition = composition(builder8);

            var processed = new IntBag(expected8.length);
            composition.process((entityId, c1) -> {
                processed.add(entityId);

                assertThat(c1).isNull();
            });

            assertThat(processed.getSize()).isEqualTo(expected8.length);
            assertThat(processed.getData()).containsExactlyInAnyOrder(expected8);
        }

        @Test
        void testProcessVaryingComponents() {
            var composition = composition(builderAll);

            var processed = new IntBag(expected1.length + expected8.length + expected18.length);
            composition.process((entityId, c1) -> {
                processed.add(entityId);
            });

            assertThat(processed.getSize()).isEqualTo(expected1.length + expected8.length + expected18.length);
            assertThat(processed.getData()).contains(expected1);
            assertThat(processed.getData()).contains(expected8);
            assertThat(processed.getData()).contains(expected18);
        }

        @Test
        void testInserted() {
            var composition = composition(builder1);

            var inserted = new IntBag(7);
            composition.inserted((entityId, c1) -> {
                inserted.add(entityId);

                assertThat(c1).isNotNull();
            });

            var ids = createEntities1(7);

            assertThat(inserted.getSize()).isEqualTo(7);
            assertThat(inserted.getData()).containsExactlyInAnyOrder(ids);
        }

        @Test
        void testRemoved() {
            var composition = composition(builder1);

            var removed = new IntBag(expected1.length);
            composition.removed((entityId, c1) -> {
                removed.add(entityId);

                assertThat(c1).isNotNull();
            });

            for (int id : expected1) {
                world.deleteEntity(id);
            }
            world.process();

            assertThat(removed.getSize()).isEqualTo(expected1.length);
            assertThat(removed.getData()).containsExactlyInAnyOrder(expected1);
        }

        @Test
        void testComponentSet_EntitiesBeforeCompositionCreation() {
            var type = TestComponentSet.TYPE;
            var handler = new ComponentSetHandler();

            var entity = world.createEntity();
            var entity1 = world.createEntity(new C1());
            var entity2 = world.createEntity(new C2());
            var entity12 = world.createEntity(new C1(), new C2());

            var composition = world.createComposition(Composition.one(C1.class, C2.class), type);
            composition.process(handler::handle);

            assertThat(handler.data)
                    .extracting("entityId", "c1", "c2")
                    .doesNotContain(tuple(entity, null, null))
                    .contains(tuple(entity1, new C1(), null))
                    .contains(tuple(entity2, null, new C2()))
                    .contains(tuple(entity12, new C1(), new C2()));
        }

        @Test
        void testComponentSet_EntitiesAfterCompositionCreation() {
            var type = TestComponentSet.TYPE;
            var handler = new ComponentSetHandler();

            var composition = world.createComposition(Composition.one(C1.class, C2.class), type);

            var entity = world.createEntity();
            var entity1 = world.createEntity(new C1());
            var entity2 = world.createEntity(new C2());
            var entity12 = world.createEntity(new C1(), new C2());

            composition.process(handler::handle);

            assertThat(handler.data)
                    .extracting("entityId", "c1", "c2")
                    .doesNotContain(tuple(entity, null, null))
                    .contains(tuple(entity1, new C1(), null))
                    .contains(tuple(entity2, null, new C2()))
                    .contains(tuple(entity12, new C1(), new C2()));
        }

        @Test
        void testNestedDataType() {
            var innerDataType = DataType.get(
                    component(C1.class),
                    relation(C1.class, C2.class));

            var dataType = DataType.get(component(C2.class), innerDataType);
            var composition = world.createComposition(Composition.all(P1.class), dataType);

            var component1 = new C1();
            var component2 = new C2();
            var relation12 = Relation.create(new C1(), new C2());

            var entityId = world.createEntity(new P1(), component1, component2, relation12);

            var processed = new AtomicBoolean(false);
            composition.process((id, c2, innerData) -> {
                assertThat(id).isEqualTo(entityId);

                assertThat(c2).isSameAs(component2);

                assertThat(innerData).isNotNull();
                assertThat(innerData.component1()).isSameAs(component1);
                assertThat(innerData.component2()).containsExactly(relation12);

                processed.set(true);
            });

            assertThat(processed.get()).isTrue();

            assertThat(relation12).extracting("relationship", "target").as("not reset by iteration").doesNotContainNull();
        }

    }

    @Nested
    class Composition2Test extends AbstractCompositionNTest<CompositionData2<C1, C2>> {

        @Override
        CompositionData2<C1, C2> composition(Composition.Builder builder) {
            return world.createComposition(builder, C1.class, C2.class);
        }

        @Override
        <R> CompositionData2<R, C2> composition(Builder builder, ComponentType<?, R> first) {
            return world.createComposition(builder, first, component(C2.class));
        }

        @Override
        @SuppressWarnings("unchecked")
        <R> void inserted(CompositionData<?> of, ComponentType<?, R> type, TestConsumer<R> consumer) {
            var composition = (CompositionData2<R, C2>) of;
            composition.inserted((entityId, result, c2) -> consumer.consume(entityId, result));
        }

        @Override
        @SuppressWarnings("unchecked")
        <R> void removed(CompositionData<?> of, ComponentType<?, R> type, TestConsumer<R> consumer) {
            var composition = (CompositionData2<R, C2>) of;
            composition.removed((entityId, result, c2) -> consumer.consume(entityId, result));
        }

        @Override
        @SuppressWarnings("unchecked")
        <R> void process(CompositionData<?> of, ComponentType<?, R> type, TestConsumer<R> consumer) {
            var composition = (CompositionData2<R, C2>) of;
            composition.process((entityId, result, c2) -> consumer.consume(entityId, result));
        }

        @Test
        void testProcessOtherEntity() {
            composition(builder1).process(expected1[0], (entityId, c1, c2) -> {
                assertThat(c1).isNotNull();
                assertThat(c2).isNull();
            });

            composition(builder8).process(expected18[0], (entityId, c1, c2) -> {
                assertThat(c1).isNotNull();
                assertThat(c2).isNotNull();
            });
        }

        @Test
        void testProcessOneComponent() {
            var composition = composition(builder1);

            var processed = new IntBag(expected1.length);
            composition.process((entityId, c1, c2) -> {
                processed.add(entityId);

                assertThat(c1).isNotNull();
                assertThat(c2).isNull();
            });

            assertThat(processed.getSize()).isEqualTo(expected1.length);
            assertThat(processed.getData()).containsExactlyInAnyOrder(expected1);
        }

        @Test
        void testProcessOtherComponents() {
            var composition = composition(builder8);

            var processed = new IntBag(expected8.length);
            composition.process((entityId, c1, c2) -> {
                processed.add(entityId);

                assertThat(c1).isNull();
                assertThat(c2).isNull();
            });

            assertThat(processed.getSize()).isEqualTo(expected8.length);
            assertThat(processed.getData()).containsExactlyInAnyOrder(expected8);
        }

        @Test
        void testProcessVaryingComponents() {
            var composition = composition(builderAll);

            var processed = new IntBag(expected1.length + expected8.length + expected18.length);
            composition.process((entityId, c1, c2) -> {
                processed.add(entityId);
            });

            assertThat(processed.getSize()).isEqualTo(expected1.length + expected8.length + expected18.length);
            assertThat(processed.getData()).contains(expected1);
            assertThat(processed.getData()).contains(expected8);
            assertThat(processed.getData()).contains(expected18);
        }

        @Test
        void testInserted() {
            var composition = composition(builder1);

            var inserted = new IntBag(7);
            composition.inserted((entityId, c1, c2) -> {
                inserted.add(entityId);

                assertThat(c1).isNotNull();
                assertThat(c2).isNull();
            });

            var ids = createEntities1(7);

            assertThat(inserted.getSize()).isEqualTo(7);
            assertThat(inserted.getData()).containsExactlyInAnyOrder(ids);
        }

        @Test
        void testRemoved() {
            var composition = composition(builder1);

            var removed = new IntBag(expected1.length);
            composition.removed((entityId, c1, c2) -> {
                removed.add(entityId);

                assertThat(c1).isNotNull();
                assertThat(c2).isNull();
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
    class Composition3Test extends AbstractCompositionNTest<CompositionData3<C1, C2, C3>> {

        @Override
        CompositionData3<C1, C2, C3> composition(Composition.Builder builder) {
            return world.createComposition(builder, C1.class, C2.class, C3.class);
        }

        @Override
        <R> CompositionData3<R, C2, C3> composition(Builder builder, ComponentType<?, R> first) {
            return world.createComposition(builder, first, component(C2.class), component(C3.class));
        }

        @Override
        @SuppressWarnings("unchecked")
        <R> void inserted(CompositionData<?> of, ComponentType<?, R> type, TestConsumer<R> consumer) {
            var composition = (CompositionData3<R, C2, C3>) of;
            composition.inserted((entityId, result, c2, c3) -> consumer.consume(entityId, result));
        }

        @Override
        @SuppressWarnings("unchecked")
        <R> void removed(CompositionData<?> of, ComponentType<?, R> type, TestConsumer<R> consumer) {
            var composition = (CompositionData3<R, C2, C3>) of;
            composition.removed((entityId, result, c2, c3) -> consumer.consume(entityId, result));
        }

        @Override
        @SuppressWarnings("unchecked")
        <R> void process(CompositionData<?> of, ComponentType<?, R> type, TestConsumer<R> consumer) {
            var composition = (CompositionData3<R, C2, C3>) of;
            composition.process((entityId, result, c2, c3) -> consumer.consume(entityId, result));
        }

        @Test
        void testProcessOtherEntity() {
            composition(builder1).process(expected1[0], (entityId, c1, c2, c3) -> {
                assertThat(c1).isNotNull();
                assertThat(c2).isNull();
                assertThat(c3).isNull();
            });

            composition(builder8).process(expected18[0], (entityId, c1, c2, c3) -> {
                assertThat(c1).isNotNull();
                assertThat(c2).isNotNull();
                assertThat(c3).isNotNull();
            });
        }

        @Test
        void testProcessOneComponent() {
            var composition = composition(builder1);

            var processed = new IntBag(expected1.length);
            composition.process((entityId, c1, c2, c3) -> {
                processed.add(entityId);

                assertThat(c1).isNotNull();
                assertThat(c2).isNull();
                assertThat(c3).isNull();
            });

            assertThat(processed.getSize()).isEqualTo(expected1.length);
            assertThat(processed.getData()).containsExactlyInAnyOrder(expected1);
        }

        @Test
        void testProcessOtherComponents() {
            var composition = composition(builder8);

            var processed = new IntBag(expected8.length);
            composition.process((entityId, c1, c2, c3) -> {
                processed.add(entityId);

                assertThat(c1).isNull();
                assertThat(c2).isNull();
                assertThat(c3).isNull();
            });

            assertThat(processed.getSize()).isEqualTo(expected8.length);
            assertThat(processed.getData()).containsExactlyInAnyOrder(expected8);
        }

        @Test
        void testProcessVaryingComponents() {
            var composition = composition(builderAll);

            var processed = new IntBag(expected1.length + expected8.length + expected18.length);
            composition.process((entityId, c1, c2, c3) -> {
                processed.add(entityId);
            });

            assertThat(processed.getSize()).isEqualTo(expected1.length + expected8.length + expected18.length);
            assertThat(processed.getData()).contains(expected1);
            assertThat(processed.getData()).contains(expected8);
            assertThat(processed.getData()).contains(expected18);
        }

        @Test
        void testInserted() {
            var composition = composition(builder1);

            var inserted = new IntBag(7);
            composition.inserted((entityId, c1, c2, c3) -> {
                inserted.add(entityId);

                assertThat(c1).isNotNull();
                assertThat(c2).isNull();
                assertThat(c3).isNull();
            });

            var ids = createEntities1(7);

            assertThat(inserted.getSize()).isEqualTo(7);
            assertThat(inserted.getData()).containsExactlyInAnyOrder(ids);
        }

        @Test
        void testRemoved() {
            var composition = composition(builder1);

            var removed = new IntBag(expected1.length);
            composition.removed((entityId, c1, c2, c3) -> {
                removed.add(entityId);

                assertThat(c1).isNotNull();
                assertThat(c2).isNull();
                assertThat(c3).isNull();
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
    class Composition4Test extends AbstractCompositionNTest<CompositionData4<C1, C2, C3, C4>> {

        @Override
        CompositionData4<C1, C2, C3, C4> composition(Composition.Builder builder) {
            return world.createComposition(builder, C1.class, C2.class, C3.class, C4.class);
        }

        @Override
        <R> CompositionData4<R, C2, C3, C4> composition(Builder builder, ComponentType<?, R> first) {
            return world.createComposition(builder, first, component(C2.class), component(C3.class), component(C4.class));
        }

        @Override
        @SuppressWarnings("unchecked")
        <R> void inserted(CompositionData<?> of, ComponentType<?, R> type, TestConsumer<R> consumer) {
            var composition = (CompositionData4<R, C2, C3, C4>) of;
            composition.inserted((entityId, result, c2, c3, c4) -> consumer.consume(entityId, result));
        }

        @Override
        @SuppressWarnings("unchecked")
        <R> void removed(CompositionData<?> of, ComponentType<?, R> type, TestConsumer<R> consumer) {
            var composition = (CompositionData4<R, C2, C3, C4>) of;
            composition.removed((entityId, result, c2, c3, c4) -> consumer.consume(entityId, result));
        }

        @Override
        @SuppressWarnings("unchecked")
        <R> void process(CompositionData<?> of, ComponentType<?, R> type, TestConsumer<R> consumer) {
            var composition = (CompositionData4<R, C2, C3, C4>) of;
            composition.process((entityId, result, c2, c3, c4) -> consumer.consume(entityId, result));
        }

        @Test
        void testProcessOtherEntity() {
            composition(builder1).process(expected1[0], (entityId, c1, c2, c3, c4) -> {
                assertThat(c1).isNotNull();
                assertThat(c2).isNull();
                assertThat(c3).isNull();
                assertThat(c4).isNull();
            });

            composition(builder8).process(expected18[0], (entityId, c1, c2, c3, c4) -> {
                assertThat(c1).isNotNull();
                assertThat(c2).isNotNull();
                assertThat(c3).isNotNull();
                assertThat(c4).isNotNull();
            });
        }

        @Test
        void testProcessOneComponent() {
            var composition = composition(builder1);

            var processed = new IntBag(expected1.length);
            composition.process((entityId, c1, c2, c3, c4) -> {
                processed.add(entityId);

                assertThat(c1).isNotNull();
                assertThat(c2).isNull();
                assertThat(c3).isNull();
                assertThat(c4).isNull();
            });

            assertThat(processed.getSize()).isEqualTo(expected1.length);
            assertThat(processed.getData()).containsExactlyInAnyOrder(expected1);
        }

        @Test
        void testProcessOtherComponents() {
            var composition = composition(builder8);

            var processed = new IntBag(expected8.length);
            composition.process((entityId, c1, c2, c3, c4) -> {
                processed.add(entityId);

                assertThat(c1).isNull();
                assertThat(c2).isNull();
                assertThat(c3).isNull();
                assertThat(c4).isNull();
            });

            assertThat(processed.getSize()).isEqualTo(expected8.length);
            assertThat(processed.getData()).containsExactlyInAnyOrder(expected8);
        }

        @Test
        void testProcessVaryingComponents() {
            var composition = composition(builderAll);

            var processed = new IntBag(expected1.length + expected8.length + expected18.length);
            composition.process((entityId, c1, c2, c3, c4) -> {
                processed.add(entityId);
            });

            assertThat(processed.getSize()).isEqualTo(expected1.length + expected8.length + expected18.length);
            assertThat(processed.getData()).contains(expected1);
            assertThat(processed.getData()).contains(expected8);
            assertThat(processed.getData()).contains(expected18);
        }

        @Test
        void testInserted() {
            var composition = composition(builder1);

            var inserted = new IntBag(7);
            composition.inserted((entityId, c1, c2, c3, c4) -> {
                inserted.add(entityId);

                assertThat(c1).isNotNull();
                assertThat(c2).isNull();
                assertThat(c3).isNull();
                assertThat(c4).isNull();
            });

            var ids = createEntities1(7);

            assertThat(inserted.getSize()).isEqualTo(7);
            assertThat(inserted.getData()).containsExactlyInAnyOrder(ids);
        }

        @Test
        void testRemoved() {
            var composition = composition(builder1);

            var removed = new IntBag(expected1.length);
            composition.removed((entityId, c1, c2, c3, c4) -> {
                removed.add(entityId);

                assertThat(c1).isNotNull();
                assertThat(c2).isNull();
                assertThat(c3).isNull();
                assertThat(c4).isNull();
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
    class Composition5Test extends AbstractCompositionNTest<CompositionData5<C1, C2, C3, C4, C5>> {

        @Override
        CompositionData5<C1, C2, C3, C4, C5> composition(Composition.Builder builder) {
            return world.createComposition(builder, C1.class, C2.class, C3.class, C4.class, C5.class);
        }

        @Override
        <R> CompositionData5<R, C2, C3, C4, C5> composition(Builder builder, ComponentType<?, R> first) {
            return world.createComposition(builder, first, component(C2.class), component(C3.class), component(C4.class), component(C5.class));
        }

        @Override
        @SuppressWarnings("unchecked")
        <R> void inserted(CompositionData<?> of, ComponentType<?, R> type, TestConsumer<R> consumer) {
            var composition = (CompositionData5<R, C2, C3, C4, C5>) of;
            composition.inserted((entityId, result, c2, c3, c4, c5) -> consumer.consume(entityId, result));
        }

        @Override
        @SuppressWarnings("unchecked")
        <R> void removed(CompositionData<?> of, ComponentType<?, R> type, TestConsumer<R> consumer) {
            var composition = (CompositionData5<R, C2, C3, C4, C5>) of;
            composition.removed((entityId, result, c2, c3, c4, c5) -> consumer.consume(entityId, result));
        }

        @Override
        @SuppressWarnings("unchecked")
        <R> void process(CompositionData<?> of, ComponentType<?, R> type, TestConsumer<R> consumer) {
            var composition = (CompositionData5<R, C2, C3, C4, C5>) of;
            composition.process((entityId, result, c2, c3, c4, c5) -> consumer.consume(entityId, result));
        }

        @Test
        void testProcessOtherEntity() {
            composition(builder1).process(expected1[0], (entityId, c1, c2, c3, c4, c5) -> {
                assertThat(c1).isNotNull();
                assertThat(c2).isNull();
                assertThat(c3).isNull();
                assertThat(c4).isNull();
                assertThat(c5).isNull();
            });

            composition(builder8).process(expected18[0], (entityId, c1, c2, c3, c4, c5) -> {
                assertThat(c1).isNotNull();
                assertThat(c2).isNotNull();
                assertThat(c3).isNotNull();
                assertThat(c4).isNotNull();
                assertThat(c5).isNotNull();
            });
        }

        @Test
        void testProcessOneComponent() {
            var composition = composition(builder1);

            var processed = new IntBag(expected1.length);
            composition.process((entityId, c1, c2, c3, c4, c5) -> {
                processed.add(entityId);

                assertThat(c1).isNotNull();
                assertThat(c2).isNull();
                assertThat(c3).isNull();
                assertThat(c4).isNull();
                assertThat(c5).isNull();
            });

            assertThat(processed.getSize()).isEqualTo(expected1.length);
            assertThat(processed.getData()).containsExactlyInAnyOrder(expected1);
        }

        @Test
        void testProcessAllComponents() {
            var composition = composition(builder8);

            var processed = new IntBag(expected8.length);
            composition.process((entityId, c1, c2, c3, c4, c5) -> {
                processed.add(entityId);

                assertThat(c1).isNull();
                assertThat(c2).isNull();
                assertThat(c3).isNull();
                assertThat(c4).isNull();
                assertThat(c5).isNull();
            });

            assertThat(processed.getSize()).isEqualTo(expected8.length);
            assertThat(processed.getData()).containsExactlyInAnyOrder(expected8);
        }

        @Test
        void testProcessVaryingComponents() {
            var composition = composition(builderAll);

            var processed = new IntBag(expected1.length + expected8.length + expected18.length);
            composition.process((entityId, c1, c2, c3, c4, c5) -> {
                processed.add(entityId);
            });

            assertThat(processed.getSize()).isEqualTo(expected1.length + expected8.length + expected18.length);
            assertThat(processed.getData()).contains(expected1);
            assertThat(processed.getData()).contains(expected8);
            assertThat(processed.getData()).contains(expected18);
        }

        @Test
        void testInserted() {
            var composition = composition(builder1);

            var inserted = new IntBag(7);
            composition.inserted((entityId, c1, c2, c3, c4, c5) -> {
                inserted.add(entityId);

                assertThat(c1).isNotNull();
                assertThat(c2).isNull();
                assertThat(c3).isNull();
                assertThat(c4).isNull();
                assertThat(c5).isNull();
            });

            var ids = createEntities1(7);

            assertThat(inserted.getSize()).isEqualTo(7);
            assertThat(inserted.getData()).containsExactlyInAnyOrder(ids);
        }

        @Test
        void testRemoved() {
            var composition = composition(builder1);

            var removed = new IntBag(expected1.length);
            composition.removed((entityId, c1, c2, c3, c4, c5) -> {
                removed.add(entityId);

                assertThat(c1).isNotNull();
                assertThat(c2).isNull();
                assertThat(c3).isNull();
                assertThat(c4).isNull();
                assertThat(c5).isNull();
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
    class Composition6Test extends AbstractCompositionNTest<CompositionData6<C1, C2, C3, C4, C5, C6>> {

        @Override
        CompositionData6<C1, C2, C3, C4, C5, C6> composition(Composition.Builder builder) {
            return world.createComposition(builder, C1.class, C2.class, C3.class, C4.class, C5.class, C6.class);
        }

        @Override
        <R> CompositionData6<R, C2, C3, C4, C5, C6> composition(Builder builder, ComponentType<?, R> first) {
            return world.createComposition(builder, first, component(C2.class), component(C3.class), component(C4.class), component(C5.class), component(C6.class));
        }

        @Override
        @SuppressWarnings("unchecked")
        <R> void inserted(CompositionData<?> of, ComponentType<?, R> type, TestConsumer<R> consumer) {
            var composition = (CompositionData6<R, C2, C3, C4, C5, C6>) of;
            composition.inserted((entityId, result, c2, c3, c4, c5, c6) -> consumer.consume(entityId, result));
        }

        @Override
        @SuppressWarnings("unchecked")
        <R> void removed(CompositionData<?> of, ComponentType<?, R> type, TestConsumer<R> consumer) {
            var composition = (CompositionData6<R, C2, C3, C4, C5, C6>) of;
            composition.removed((entityId, result, c2, c3, c4, c5, c6) -> consumer.consume(entityId, result));
        }

        @Override
        @SuppressWarnings("unchecked")
        <R> void process(CompositionData<?> of, ComponentType<?, R> type, TestConsumer<R> consumer) {
            var composition = (CompositionData6<R, C2, C3, C4, C5, C6>) of;
            composition.process((entityId, result, c2, c3, c4, c5, c6) -> consumer.consume(entityId, result));
        }

        @Test
        void testProcessOtherEntity() {
            composition(builder1).process(expected1[0], (entityId, c1, c2, c3, c4, c5, c6) -> {
                assertThat(c1).isNotNull();
                assertThat(c2).isNull();
                assertThat(c3).isNull();
                assertThat(c4).isNull();
                assertThat(c5).isNull();
                assertThat(c6).isNull();
            });

            composition(builder8).process(expected18[0], (entityId, c1, c2, c3, c4, c5, c6) -> {
                assertThat(c1).isNotNull();
                assertThat(c2).isNotNull();
                assertThat(c3).isNotNull();
                assertThat(c4).isNotNull();
                assertThat(c5).isNotNull();
                assertThat(c6).isNotNull();
            });
        }

        @Test
        void testProcessOneComponent() {
            var composition = composition(builder1);

            var processed = new IntBag(expected1.length);
            composition.process((entityId, c1, c2, c3, c4, c5, c6) -> {
                processed.add(entityId);

                assertThat(c1).isNotNull();
                assertThat(c2).isNull();
                assertThat(c3).isNull();
                assertThat(c4).isNull();
                assertThat(c5).isNull();
                assertThat(c6).isNull();
            });

            assertThat(processed.getSize()).isEqualTo(expected1.length);
            assertThat(processed.getData()).containsExactlyInAnyOrder(expected1);
        }

        @Test
        void testProcessOtherComponents() {
            var composition = composition(builder8);

            var processed = new IntBag(expected8.length);
            composition.process((entityId, c1, c2, c3, c4, c5, c6) -> {
                processed.add(entityId);

                assertThat(c1).isNull();
                assertThat(c2).isNull();
                assertThat(c3).isNull();
                assertThat(c4).isNull();
                assertThat(c5).isNull();
                assertThat(c6).isNull();
            });

            assertThat(processed.getSize()).isEqualTo(expected8.length);
            assertThat(processed.getData()).containsExactlyInAnyOrder(expected8);
        }

        @Test
        void testProcessVaryingComponents() {
            var composition = composition(builderAll);

            var processed = new IntBag(expected1.length + expected8.length + expected18.length);
            composition.process((entityId, c1, c2, c3, c4, c5, c6) -> {
                processed.add(entityId);
            });
            assertThat(processed.getSize()).isEqualTo(expected1.length + expected8.length + expected18.length);
            assertThat(processed.getData()).contains(expected1);
            assertThat(processed.getData()).contains(expected8);
            assertThat(processed.getData()).contains(expected18);
        }

        @Test
        void testInserted() {
            var composition = composition(builder1);

            var inserted = new IntBag(7);
            composition.inserted((entityId, c1, c2, c3, c4, c5, c6) -> {
                inserted.add(entityId);

                assertThat(c1).isNotNull();
                assertThat(c2).isNull();
                assertThat(c3).isNull();
                assertThat(c4).isNull();
                assertThat(c5).isNull();
                assertThat(c6).isNull();
            });

            var ids = createEntities1(7);

            assertThat(inserted.getSize()).isEqualTo(7);
            assertThat(inserted.getData()).containsExactlyInAnyOrder(ids);
        }

        @Test
        void testRemoved() {
            var composition = composition(builder1);

            var removed = new IntBag(expected1.length);
            composition.removed((entityId, c1, c2, c3, c4, c5, c6) -> {
                removed.add(entityId);

                assertThat(c1).isNotNull();
                assertThat(c2).isNull();
                assertThat(c3).isNull();
                assertThat(c4).isNull();
                assertThat(c5).isNull();
                assertThat(c6).isNull();
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
    class Composition7Test extends AbstractCompositionNTest<CompositionData7<C1, C2, C3, C4, C5, C6, C7>> {

        @Override
        CompositionData7<C1, C2, C3, C4, C5, C6, C7> composition(Composition.Builder builder) {
            return world.createComposition(builder, C1.class, C2.class, C3.class, C4.class, C5.class, C6.class, C7.class);
        }

        @Override
        <R> CompositionData7<R, C2, C3, C4, C5, C6, C7> composition(Builder builder, ComponentType<?, R> first) {
            return world.createComposition(builder, first, component(C2.class), component(C3.class), component(C4.class), component(C5.class), component(C6.class), component(C7.class));
        }

        @Override
        @SuppressWarnings("unchecked")
        <R> void inserted(CompositionData<?> of, ComponentType<?, R> type, TestConsumer<R> consumer) {
            var composition = (CompositionData7<R, C2, C3, C4, C5, C6, C7>) of;
            composition.inserted((entityId, result, c2, c3, c4, c5, c6, c7) -> consumer.consume(entityId, result));
        }

        @Override
        @SuppressWarnings("unchecked")
        <R> void removed(CompositionData<?> of, ComponentType<?, R> type, TestConsumer<R> consumer) {
            var composition = (CompositionData7<R, C2, C3, C4, C5, C6, C7>) of;
            composition.removed((entityId, result, c2, c3, c4, c5, c6, c7) -> consumer.consume(entityId, result));
        }

        @Override
        @SuppressWarnings("unchecked")
        <R> void process(CompositionData<?> of, ComponentType<?, R> type, TestConsumer<R> consumer) {
            var composition = (CompositionData7<R, C2, C3, C4, C5, C6, C7>) of;
            composition.process((entityId, result, c2, c3, c4, c5, c6, c7) -> consumer.consume(entityId, result));
        }

        @Test
        void testProcessOtherEntity() {
            composition(builder1).process(expected1[0], (entityId, c1, c2, c3, c4, c5, c6, c7) -> {
                assertThat(c1).isNotNull();
                assertThat(c2).isNull();
                assertThat(c3).isNull();
                assertThat(c4).isNull();
                assertThat(c5).isNull();
                assertThat(c6).isNull();
                assertThat(c7).isNull();
            });

            composition(builder8).process(expected18[0], (entityId, c1, c2, c3, c4, c5, c6, c7) -> {
                assertThat(c1).isNotNull();
                assertThat(c2).isNotNull();
                assertThat(c3).isNotNull();
                assertThat(c4).isNotNull();
                assertThat(c5).isNotNull();
                assertThat(c6).isNotNull();
                assertThat(c7).isNotNull();
            });
        }

        @Test
        void testProcessOneComponent() {
            var composition = composition(builder1);

            var processed = new IntBag(expected1.length);
            composition.process((entityId, c1, c2, c3, c4, c5, c6, c7) -> {
                processed.add(entityId);

                assertThat(c1).isNotNull();
                assertThat(c2).isNull();
                assertThat(c3).isNull();
                assertThat(c4).isNull();
                assertThat(c5).isNull();
                assertThat(c6).isNull();
                assertThat(c7).isNull();
            });

            assertThat(processed.getSize()).isEqualTo(expected1.length);
            assertThat(processed.getData()).containsExactlyInAnyOrder(expected1);
        }

        @Test
        void testProcessOtherComponents() {
            var composition = composition(builder8);

            var processed = new IntBag(expected8.length);
            composition.process((entityId, c1, c2, c3, c4, c5, c6, c7) -> {
                processed.add(entityId);

                assertThat(c1).isNull();
                assertThat(c2).isNull();
                assertThat(c3).isNull();
                assertThat(c4).isNull();
                assertThat(c5).isNull();
                assertThat(c6).isNull();
                assertThat(c7).isNull();
            });

            assertThat(processed.getSize()).isEqualTo(expected8.length);
            assertThat(processed.getData()).containsExactlyInAnyOrder(expected8);
        }

        @Test
        void testProcessVaryingComponents() {
            var composition = composition(builderAll);

            var processed = new IntBag(expected1.length + expected8.length + expected18.length);
            composition.process((entityId, c1, c2, c3, c4, c5, c6, c7) -> {
                processed.add(entityId);
            });

            assertThat(processed.getSize()).isEqualTo(expected1.length + expected8.length + expected18.length);
            assertThat(processed.getData()).contains(expected1);
            assertThat(processed.getData()).contains(expected8);
            assertThat(processed.getData()).contains(expected18);
        }

        @Test
        void testInserted() {
            var composition = composition(builder1);

            var inserted = new IntBag(7);
            composition.inserted((entityId, c1, c2, c3, c4, c5, c6, c7) -> {
                inserted.add(entityId);

                assertThat(c1).isNotNull();
                assertThat(c2).isNull();
                assertThat(c3).isNull();
                assertThat(c4).isNull();
                assertThat(c5).isNull();
                assertThat(c6).isNull();
                assertThat(c7).isNull();
            });

            var ids = createEntities1(7);

            assertThat(inserted.getSize()).isEqualTo(7);
            assertThat(inserted.getData()).containsExactlyInAnyOrder(ids);
        }

        @Test
        void testRemoved() {
            var composition = composition(builder1);

            var removed = new IntBag(expected1.length);
            composition.removed((entityId, c1, c2, c3, c4, c5, c6, c7) -> {
                removed.add(entityId);

                assertThat(c1).isNotNull();
                assertThat(c2).isNull();
                assertThat(c3).isNull();
                assertThat(c4).isNull();
                assertThat(c5).isNull();
                assertThat(c6).isNull();
                assertThat(c7).isNull();
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
    class Composition8Test extends AbstractCompositionNTest<CompositionData8<C1, C2, C3, C4, C5, C6, C7, C8>> {

        @Override
        CompositionData8<C1, C2, C3, C4, C5, C6, C7, C8> composition(Composition.Builder builder) {
            return world.createComposition(builder, C1.class, C2.class, C3.class, C4.class, C5.class, C6.class, C7.class, C8.class);
        }

        @Override
        <R> CompositionData8<R, C2, C3, C4, C5, C6, C7, C8> composition(Builder builder, ComponentType<?, R> first) {
            return world.createComposition(builder, first, component(C2.class), component(C3.class), component(C4.class), component(C5.class), component(C6.class), component(C7.class),
                    component(C8.class));
        }

        @Override
        @SuppressWarnings("unchecked")
        <R> void inserted(CompositionData<?> of, ComponentType<?, R> type, TestConsumer<R> consumer) {
            var composition = (CompositionData8<R, C2, C3, C4, C5, C6, C7, C8>) of;
            composition.inserted((entityId, result, c2, c3, c4, c5, c6, c7, c8) -> consumer.consume(entityId, result));
        }

        @Override
        @SuppressWarnings("unchecked")
        <R> void removed(CompositionData<?> of, ComponentType<?, R> type, TestConsumer<R> consumer) {
            var composition = (CompositionData8<R, C2, C3, C4, C5, C6, C7, C8>) of;
            composition.removed((entityId, result, c2, c3, c4, c5, c6, c7, c8) -> consumer.consume(entityId, result));
        }

        @Override
        @SuppressWarnings("unchecked")
        <R> void process(CompositionData<?> of, ComponentType<?, R> type, TestConsumer<R> consumer) {
            var composition = (CompositionData8<R, C2, C3, C4, C5, C6, C7, C8>) of;
            composition.process((entityId, result, c2, c3, c4, c5, c6, c7, c8) -> consumer.consume(entityId, result));
        }

        @Test
        void testProcessOtherEntity() {
            composition(builder1).process(expected1[0], (entityId, c1, c2, c3, c4, c5, c6, c7, c8) -> {
                assertThat(c1).isNotNull();
                assertThat(c2).isNull();
                assertThat(c3).isNull();
                assertThat(c4).isNull();
                assertThat(c5).isNull();
                assertThat(c6).isNull();
                assertThat(c7).isNull();
                assertThat(c8).isNull();
            });

            composition(builder8).process(expected18[0], (entityId, c1, c2, c3, c4, c5, c6, c7, c8) -> {
                assertThat(c1).isNotNull();
                assertThat(c2).isNotNull();
                assertThat(c3).isNotNull();
                assertThat(c4).isNotNull();
                assertThat(c5).isNotNull();
                assertThat(c6).isNotNull();
                assertThat(c7).isNotNull();
                assertThat(c8).isNotNull();
            });
        }

        @Test
        void testProcessOneComponent() {
            var composition = composition(builder1);

            var processed = new IntBag(expected1.length);
            composition.process((entityId, c1, c2, c3, c4, c5, c6, c7, c8) -> {
                processed.add(entityId);

                assertThat(c1).isNotNull();
                assertThat(c2).isNull();
                assertThat(c3).isNull();
                assertThat(c4).isNull();
                assertThat(c5).isNull();
                assertThat(c6).isNull();
                assertThat(c7).isNull();
                assertThat(c8).isNull();
            });

            assertThat(processed.getSize()).isEqualTo(expected1.length);
            assertThat(processed.getData()).containsExactlyInAnyOrder(expected1);
        }

        @Test
        void testProcessOtherComponents() {
            var composition = composition(builder8);

            var processed = new IntBag(expected8.length);
            composition.process((entityId, c1, c2, c3, c4, c5, c6, c7, c8) -> {
                processed.add(entityId);

                assertThat(c1).isNull();
                assertThat(c2).isNull();
                assertThat(c3).isNull();
                assertThat(c4).isNull();
                assertThat(c5).isNull();
                assertThat(c6).isNull();
                assertThat(c7).isNull();
                assertThat(c8).isNotNull();
            });

            assertThat(processed.getSize()).isEqualTo(expected8.length);
            assertThat(processed.getData()).containsExactlyInAnyOrder(expected8);
        }

        @Test
        void testProcessVaryingComponents() {
            var composition = composition(builderAll);

            var processed = new IntBag(expected1.length + expected8.length + expected18.length);
            composition.process((entityId, c1, c2, c3, c4, c5, c6, c7, c8) -> {
                processed.add(entityId);
            });

            assertThat(processed.getSize()).isEqualTo(expected1.length + expected8.length + expected18.length);
            assertThat(processed.getData()).contains(expected1);
            assertThat(processed.getData()).contains(expected8);
            assertThat(processed.getData()).contains(expected18);
        }

        @Test
        void testInserted() {
            var composition = composition(builder1);

            var inserted = new IntBag(7);
            composition.inserted((entityId, c1, c2, c3, c4, c5, c6, c7, c8) -> {
                inserted.add(entityId);

                assertThat(c1).isNotNull();
                assertThat(c2).isNull();
                assertThat(c3).isNull();
                assertThat(c4).isNull();
                assertThat(c5).isNull();
                assertThat(c6).isNull();
                assertThat(c7).isNull();
                assertThat(c8).isNull();
            });

            var ids = createEntities1(7);

            assertThat(inserted.getSize()).isEqualTo(7);
            assertThat(inserted.getData()).containsExactlyInAnyOrder(ids);
        }

        @Test
        void testRemoved() {
            var composition = composition(builder1);

            var removed = new IntBag(expected1.length);
            composition.removed((entityId, c1, c2, c3, c4, c5, c6, c7, c8) -> {
                removed.add(entityId);

                assertThat(c1).isNotNull();
                assertThat(c2).isNull();
                assertThat(c3).isNull();
                assertThat(c4).isNull();
                assertThat(c5).isNull();
                assertThat(c6).isNull();
                assertThat(c7).isNull();
                assertThat(c8).isNull();
            });

            for (int id : expected1) {
                world.deleteEntity(id);
            }
            world.process();

            assertThat(removed.getSize()).isEqualTo(expected1.length);
            assertThat(removed.getData()).containsExactlyInAnyOrder(expected1);
        }

    }

    abstract class AbstractCompositionDataTest<C extends CompositionData<?>> {

        interface TestConsumer<T> {
            void consume(int index, T result);
        }

        interface ProcessTestImpl<C extends CompositionData<?>> {
            AbstractCompositionDataTest<C> getTest();

            default <R> int[] performImpl(Object[][] components, CompositionData<?> composition, ComponentType<?, R> type, TestConsumer<R> consumer) {
                var test = getTest();
                var world = test.getWorld();

                var s = components.length;
                var entities = new int[s];

                for (int i = 0; i < s; i++) {
                    entities[i] = world.createEntity(components[i]);
                }

                test.process(composition, type, (entityId, result) -> {
                    for (int i = 0; i < s; i++) {
                        if (entityId == entities[i]) {
                            consumer.consume(i, result);
                        }
                    }
                });

                return entities;
            }
        }

        interface InsertedTestImpl<C extends CompositionData<?>> {
            AbstractCompositionDataTest<C> getTest();

            default <R> int[] performImpl(Object[][] components, CompositionData<?> composition, ComponentType<?, R> type, TestConsumer<R> consumer) {
                var test = getTest();
                var world = test.getWorld();

                var s = components.length;
                var entities = new int[s];

                var count = new AtomicInteger(0);

                test.inserted(composition, type, (entityId, result) -> {
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

        interface RemovedTestImpl<C extends CompositionData<?>> {
            AbstractCompositionDataTest<C> getTest();

            default <R> int[] performImpl(Object[][] components, CompositionData<?> composition, ComponentType<?, R> type, TestConsumer<R> consumer) {
                var test = getTest();
                var world = test.getWorld();

                var s = components.length;
                var entities = new int[s];

                for (int i = 0; i < s; i++) {
                    entities[i] = world.createEntity(components[i]);
                    world.deleteEntity(entities[i]);
                }

                test.removed(composition, type, (entityId, result) -> {
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

        final Composition.Builder builder1 = Composition.all(C1.class).none(C8.class);
        final Composition.Builder builder8 = Composition.all(C8.class).none(C1.class);
        final Composition.Builder builderAll = Composition.one(C1.class, C8.class);

        int[] expected1;
        int[] expected4;
        int[] expected8;
        int[] expected18;

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

        int createEntity8() {
            return world.createEntity(new C8());
        }

        int[] createEntities8(int number) {
            var entities = new int[number];

            for (int i = 0; i < number; i++) {
                entities[i] = world.createEntity(new C8());
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
            this.expected8 = createEntities8(5);
            this.expected18 = createEntities18(7);

            world.process();
        }

        abstract C composition(Composition.Builder builder);

        abstract <R> CompositionData<?> composition(Composition.Builder builder, ComponentType<?, R> first);

        abstract <R> void inserted(CompositionData<?> composition, ComponentType<?, R> type, TestConsumer<R> consumer);

        abstract <R> void removed(CompositionData<?> composition, ComponentType<?, R> type, TestConsumer<R> consumer);

        abstract <R> void process(CompositionData<?> composition, ComponentType<?, R> type, TestConsumer<R> consumer);

        private World getWorld() {
            return world;
        }

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
            var compositionEmpty = composition(Composition.none(C1.class, C8.class));

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
            assertThat(compositionAll.getCount()).isEqualTo(expected1.length + expected8.length + expected18.length);
        }

        @Test
        void testStream() {
            var composition1 = composition(builder1);
            var composition8 = composition(builder8);
            var compositionAll = composition(builderAll);

            assertThat(composition1.stream()).hasSize(expected1.length);
            assertThat(composition8.stream()).hasSize(expected8.length);
            assertThat(compositionAll.stream()).hasSize(expected1.length + expected8.length + expected18.length);
        }

        @Test
        void testParallelStream() {
            var composition1 = composition(builder1);
            var composition8 = composition(builder8);
            var compositionAll = composition(builderAll);

            assertThat(composition1.parallelStream()).hasSize(expected1.length);
            assertThat(composition8.parallelStream()).hasSize(expected8.length);
            assertThat(compositionAll.parallelStream()).hasSize(expected1.length + expected8.length + expected18.length);
        }

        @Test
        void testProcess_WithUnprocessedComponentAddition() {
            var entityId = world.createEntity();

            var composition = composition(Composition.all(), component(C1.class));
            var composition1 = composition(Composition.all(C1.class), component(C1.class));
            var compositionNot1 = composition(Composition.none(C1.class), component(C1.class));

            // Verify state before change
            assertThat(composition.isInterested(entityId)).isTrue();
            assertThat(composition.stream()).contains(entityId);

            assertThat(composition1.isInterested(entityId)).isFalse();
            assertThat(composition1.stream()).doesNotContain(entityId);

            assertThat(compositionNot1.isInterested(entityId)).isTrue();
            assertThat(compositionNot1.stream()).contains(entityId);

            // Modify
            var component = new C1();
            world.getComponents(C1.class).add(entityId, component);

            // Verify state before process of change
            assertThat(composition.isInterested(entityId)).isTrue();
            assertThat(composition.stream()).contains(entityId);

            assertThat(composition1.isInterested(entityId)).isFalse();
            assertThat(composition1.stream()).doesNotContain(entityId);

            assertThat(compositionNot1.isInterested(entityId)).isTrue();
            assertThat(compositionNot1.stream()).contains(entityId);

            var found = new AtomicBoolean();
            process(compositionNot1, component(C1.class), (id, c1) -> {
                if (id == entityId) {
                    assertThat(c1).as("read component before process").isNotNull();
                    found.set(true);
                }
            });
            assertThat(found.get()).as("process entity before process").isTrue();

            // Process
            world.process();

            // Verify state after process of change
            assertThat(composition.isInterested(entityId)).isTrue();
            assertThat(composition.stream()).contains(entityId);

            assertThat(composition1.isInterested(entityId)).isTrue();
            assertThat(composition1.stream()).contains(entityId);

            found.set(false);
            process(composition1, component(C1.class), (id, c1) -> {
                if (id == entityId) {
                    assertThat(c1).as("read component after process").isSameAs(component);
                    found.set(true);
                }
            });
            assertThat(found.get()).as("process entity after process").isTrue();

            assertThat(compositionNot1.isInterested(entityId)).isFalse();
            assertThat(compositionNot1.stream()).doesNotContain(entityId);
        }

        @Test
        void testProcess_WithUnprocessedComponentRemoval() {
            var component = new C1();
            var entityId = world.createEntity(component);

            var composition = composition(Composition.all(), component(C1.class));
            var composition1 = composition(Composition.all(C1.class), component(C1.class));
            var compositionNot1 = composition(Composition.none(C1.class), component(C1.class));

            // Verify state before change
            assertThat(composition.isInterested(entityId)).isTrue();
            assertThat(composition.stream()).contains(entityId);

            assertThat(composition1.isInterested(entityId)).isTrue();
            assertThat(composition1.stream()).contains(entityId);

            assertThat(compositionNot1.isInterested(entityId)).isFalse();
            assertThat(compositionNot1.stream()).doesNotContain(entityId);

            // Modify
            world.getComponents(C1.class).remove(entityId);

            // Verify state before process of change
            assertThat(composition.isInterested(entityId)).isTrue();
            assertThat(composition.stream()).contains(entityId);

            assertThat(composition1.isInterested(entityId)).isTrue();
            assertThat(composition1.stream()).contains(entityId);

            var found = new AtomicBoolean();
            process(composition1, component(C1.class), (id, c1) -> {
                if (id == entityId) {
                    assertThat(c1).as("read component").isSameAs(component);
                    found.set(true);
                }
            });
            assertThat(found.get()).as("process entity before process").isTrue();

            assertThat(compositionNot1.isInterested(entityId)).isFalse();
            assertThat(compositionNot1.stream()).doesNotContain(entityId);

            // Process
            world.process();

            // Verify state after process of change
            assertThat(composition.isInterested(entityId)).isTrue();
            assertThat(composition.stream()).contains(entityId);

            assertThat(composition1.isInterested(entityId)).isFalse();
            assertThat(composition1.stream()).doesNotContain(entityId);

            assertThat(compositionNot1.isInterested(entityId)).isTrue();
            assertThat(compositionNot1.stream()).contains(entityId);

            found.set(false);
            process(compositionNot1, component(C1.class), (id, c1) -> {
                if (id == entityId) {
                    assertThat(c1).as("read component after process").isNull();
                    found.set(true);
                }
            });
            assertThat(found.get()).as("process entity after process").isTrue();
        }

        @Nested
        class NonRegularComponentTypesTest {

            @Test
            void testWildcard() {
                var composition = composition(Composition.all(wildcard(C1234.class)));

                var entity2 = world.createEntity(new C2());
                var entity23 = world.createEntity(new C2(), new C3());
                var entity45 = world.createEntity(new C4(), new C5());
                var entity5 = world.createEntity(new C5());

                var expected = new ArrayList<Integer>();
                expected.add(entity2);
                expected.add(entity23);
                expected.add(entity45);
                for (var entityId : expected1) {
                    expected.add(entityId);
                }
                for (var entityId : expected18) {
                    expected.add(entityId);
                }

                // Verify
                assertThat(composition.isEmpty()).isFalse();
                assertThat(composition.getCount()).isEqualTo(expected1.length + expected18.length + 3);

                var processed = new ArrayList<Integer>();
                composition.process(processed::add);

                assertThat(processed)
                        .containsExactlyInAnyOrderElementsOf(expected)
                        .hasSize(expected1.length + expected18.length + 3)
                        .doesNotContain(entity5);
            }

            @Test
            void testWildcardComponentRelation() {
                var composition = composition(Composition.all(wildcardRelation(C1234.class, C8.class)));

                var entity18 = world.createEntity(Relation.create(new C1(), new C8()));
                var entity48 = world.createEntity(Relation.create(new C4(), new C8()));
                var entity58 = world.createEntity(Relation.create(new C5(), new C8()));
                var entity15 = world.createEntity(Relation.create(new C1(), new C5()));
                var entity15and18 = world.createEntity(Relation.create(new C1(), new C5()), Relation.create(new C1(), new C8()));

                // Verify
                assertThat(composition.isEmpty()).isFalse();
                assertThat(composition.getCount()).isEqualTo(3);

                var processed = new ArrayList<Integer>();
                composition.process(processed::add);

                assertThat(processed)
                        .containsExactlyInAnyOrder(entity18, entity48, entity15and18)
                        .doesNotContain(entity15, entity58); // exactly ensures that, but that way those won't be easily optimized away
            }

            @Test
            void testWildcardEntityRelation() {
                var composition = composition(Composition.all(wildcardRelation(C1234.class)));

                var target = world.createEntity();

                var entity1 = world.createEntity(Relation.create(new C1(), target));
                var entity4 = world.createEntity(Relation.create(new C4(), target));
                var entity5 = world.createEntity(Relation.create(new C5(), target));
                var entity5and1 = world.createEntity(Relation.create(new C5(), target), Relation.create(new C1(), target));

                // Verify
                assertThat(composition.isEmpty()).isFalse();
                assertThat(composition.getCount()).isEqualTo(3);

                var processed = new ArrayList<Integer>();
                composition.process(processed::add);

                assertThat(processed)
                        .containsExactlyInAnyOrder(entity1, entity4, entity5and1)
                        .doesNotContain(entity5); // exactly ensures that, but that way those won't be easily optimized away
            }

        }

        @Nested
        class WildcardTypeTest {

            @Nested
            class ProcessTest extends AbstractTest implements ProcessTestImpl<C> {
                @Override
                public AbstractCompositionDataTest<C> getTest() {
                    return AbstractCompositionDataTest.this;
                }

                @Override
                <R> int[] perform(Object[][] components, CompositionData<?> composition, ComponentType<?, R> type, TestConsumer<R> consumer) {
                    return performImpl(components, composition, type, consumer);
                }
            }

            @Nested
            class InsertedTest extends AbstractTest implements InsertedTestImpl<C> {
                @Override
                public AbstractCompositionDataTest<C> getTest() {
                    return AbstractCompositionDataTest.this;
                }

                @Override
                <R> int[] perform(Object[][] components, CompositionData<?> composition, ComponentType<?, R> type, TestConsumer<R> consumer) {
                    return performImpl(components, composition, type, consumer);
                }
            }

            @Nested
            class RemovedTest extends AbstractTest implements RemovedTestImpl<C> {
                @Override
                public AbstractCompositionDataTest<C> getTest() {
                    return AbstractCompositionDataTest.this;
                }

                @Override
                <R> int[] perform(Object[][] components, CompositionData<?> composition, ComponentType<?, R> type, TestConsumer<R> consumer) {
                    return performImpl(components, composition, type, consumer);
                }
            }

            abstract class AbstractTest {

                abstract <R> int[] perform(Object[][] components, CompositionData<?> composition, ComponentType<?, R> type, TestConsumer<R> consumer);

                @Test
                void testResultSize() {
                    var type = wildcard(C1234.class);
                    var composition = composition(Composition.all(), type);

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
                    var composition = composition(Composition.all(), type);

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
                    var composition = composition(Composition.all(), type);

                    var c1 = new C1();
                    var c2 = new C2();

                    var components = new Object[][] {
                            { c1, c2, new C5() },
                            { new C2() }
                    };

                    perform(components, composition, type, (id, result) -> {
                        if (id == 0) {
                            assertThat(result.get(0)).as("get(0)").isIn(c1, c2);
                            assertThat(result.get(1)).as("get(1)").isIn(c1, c2);
                        } else if (id == 1) {
                            assertThat(result.get(0)).as("get(0)").isNotNull();
                        }
                    });
                }

                @Test
                void testResultEnhancedForLoop() {
                    var type = wildcard(C1234.class);
                    var composition = composition(Composition.all(), type);

                    var c1 = new C1();
                    var c2 = new C2();

                    var components = new Object[][] {
                            { c1, c2, new C5() },
                            { new C2() }
                    };

                    perform(components, composition, type, (id, result) -> {
                        if (id == 0) {
                            for (var component : result) {
                                assertThat(component).as("enhanced for loop").isIn(c1, c2);
                            }
                        } else if (id == 1) {
                            for (var component : result) {
                                assertThat(component).as("enhanced for loop").isNotNull();
                            }
                        }
                    });
                }

                @Test
                void testResultIterator() {
                    var type = wildcard(C1234.class);
                    var composition = composition(Composition.all(), type);

                    var c1 = new C1();
                    var c2 = new C2();

                    var components = new Object[][] {
                            { c1, c2, new C5() },
                            { new C2() }
                    };

                    perform(components, composition, type, (id, result) -> {
                        if (id == 0) {
                            for (var iter = result.iterator(); iter.hasNext();) {
                                assertThat(iter.next()).as("iterator loop").isIn(c1, c2);
                            }
                            for (var iter = result.iterator(); iter.hasNext();) {
                                assertThat(iter.next()).as("iterator loop resets automatically").isIn(c1, c2);
                            }
                        } else if (id == 1) {
                            for (var iter = result.iterator(); iter.hasNext();) {
                                assertThat(iter.next()).as("iterator loop").isNotNull();
                            }
                            for (var iter = result.iterator(); iter.hasNext();) {
                                assertThat(iter.next()).as("iterator loop").isNotNull();
                            }

                        }
                    });
                }

                @Test
                void testResultGetByClass() {
                    var type = wildcard(C1234.class);
                    var composition = composition(Composition.all(), type);

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
                    var composition = composition(Composition.all(), type);

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
            class ProcessTest extends AbstractTest implements ProcessTestImpl<C> {
                @Override
                public AbstractCompositionDataTest<C> getTest() {
                    return AbstractCompositionDataTest.this;
                }

                @Override
                <R> int[] perform(Object[][] components, CompositionData<?> composition, ComponentType<?, R> type, TestConsumer<R> consumer) {
                    return performImpl(components, composition, type, consumer);
                }
            }

            @Nested
            class InsertedTest extends AbstractTest implements InsertedTestImpl<C> {
                @Override
                public AbstractCompositionDataTest<C> getTest() {
                    return AbstractCompositionDataTest.this;
                }

                @Override
                <R> int[] perform(Object[][] components, CompositionData<?> composition, ComponentType<?, R> type, TestConsumer<R> consumer) {
                    return performImpl(components, composition, type, consumer);
                }
            }

            @Nested
            class RemovedTest extends AbstractTest implements RemovedTestImpl<C> {
                @Override
                public AbstractCompositionDataTest<C> getTest() {
                    return AbstractCompositionDataTest.this;
                }

                @Override
                <R> int[] perform(Object[][] components, CompositionData<?> composition, ComponentType<?, R> type, TestConsumer<R> consumer) {
                    return performImpl(components, composition, type, consumer);
                }
            }

            abstract class AbstractTest {

                abstract <R> int[] perform(Object[][] components, CompositionData<?> composition, ComponentType<?, R> type, TestConsumer<R> consumer);

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

        @Nested
        class EntityRelationFetchTypeTest {

            @Nested
            class ProcessTest extends AbstractTest implements ProcessTestImpl<C> {
                @Override
                public AbstractCompositionDataTest<C> getTest() {
                    return AbstractCompositionDataTest.this;
                }

                @Override
                <R> int[] perform(Object[][] components, CompositionData<?> composition, ComponentType<?, R> type, TestConsumer<R> consumer) {
                    return performImpl(components, composition, type, consumer);
                }
            }

            @Nested
            class InsertedTest extends AbstractTest implements InsertedTestImpl<C> {
                @Override
                public AbstractCompositionDataTest<C> getTest() {
                    return AbstractCompositionDataTest.this;
                }

                @Override
                <R> int[] perform(Object[][] components, CompositionData<?> composition, ComponentType<?, R> type, TestConsumer<R> consumer) {
                    return performImpl(components, composition, type, consumer);
                }
            }

            @Nested
            class RemovedTest extends AbstractTest implements RemovedTestImpl<C> {
                @Override
                public AbstractCompositionDataTest<C> getTest() {
                    return AbstractCompositionDataTest.this;
                }

                @Override
                <R> int[] perform(Object[][] components, CompositionData<?> composition, ComponentType<?, R> type, TestConsumer<R> consumer) {
                    return performImpl(components, composition, type, consumer);
                }
            }

            abstract class AbstractTest {

                abstract <R> int[] perform(Object[][] components, CompositionData<?> composition, ComponentType<?, R> type, TestConsumer<R> consumer);

                @Test
                void testExclusiveEntityRelationFetchType() {
                    var fetchType = ComponentType.exclusiveRelation(ExclusiveRelationship.class, MyComponentSet.TYPE);
                    var composition = composition(Composition.all(P1.class), fetchType);

                    var nestedTarget1 = world.createEntity();
                    var nestedTarget2 = world.createEntity();

                    var target = world.createEntity(
                            Relation.create(new ExclusiveRelationship(1), new Target(10)),
                            Relation.create(new RelationshipComponent(2), new Target(20)),
                            Relation.create(new RelationshipComponent(3), new Target(30)),
                            Relation.create(new ExclusiveRelationship(4), nestedTarget1),
                            Relation.create(new RelationshipComponent(5), nestedTarget1),
                            Relation.create(new RelationshipComponent(6), nestedTarget2),
                            new C1(), new C2(), new C3(), new C4(), new C5());

                    var entities = new Object[][] {
                            { new P1(), Relation.create(new ExclusiveRelationship(42), target) }
                    };

                    var processed = new AtomicBoolean(false);
                    perform(entities, composition, fetchType, (id, relation) -> {
                        assertThat(id).isEqualTo(0);

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

                @Test
                void testEntityRelationFetchType() {
                    var fetchType = ComponentType.relation(RelationshipComponent.class, MyComponentSet.TYPE);
                    var composition = composition(Composition.all(P1.class), fetchType);

                    var nestedTarget1 = world.createEntity();
                    var nestedTarget2 = world.createEntity();
                    var nestedTarget3 = world.createEntity();

                    var target1 = world.createEntity(
                            Relation.create(new ExclusiveRelationship(1), new Target(10)),
                            Relation.create(new RelationshipComponent(2), new Target(20)),
                            Relation.create(new RelationshipComponent(3), new Target(30)),
                            Relation.create(new ExclusiveRelationship(4), nestedTarget1),
                            Relation.create(new RelationshipComponent(5), nestedTarget1),
                            Relation.create(new RelationshipComponent(6), nestedTarget2),
                            new C1(), new C2(), new C3(), new C4(), new C5());

                    var target2 = world.createEntity(
                            Relation.create(new ExclusiveRelationship(10), new Target(100)),
                            Relation.create(new RelationshipComponent(20), new Target(200)),
                            Relation.create(new RelationshipComponent(30), new Target(300)),
                            Relation.create(new ExclusiveRelationship(40), nestedTarget1),
                            Relation.create(new RelationshipComponent(50), nestedTarget2),
                            Relation.create(new RelationshipComponent(60), nestedTarget3),
                            new C1(), new C2(), new C3(), new C5());

                    var entityId = world.createEntity(new P1(),
                            Relation.create(new RelationshipComponent(42), target1),
                            Relation.create(new RelationshipComponent(42), target2));

                    var processed = new AtomicBoolean(false);
                    process(composition, fetchType, (id, relations) -> {
                        assertThat(id).isEqualTo(entityId);

                        assertThat(relations).isNotNull();
                        assertThat(relations).extracting("target").containsExactlyInAnyOrder(target1, target2);

                        var relation1 = relations.get(0);
                        var relation2 = relations.get(1);
                        if (relation1.target() != target1) {
                            var swap = relation1;
                            relation1 = relation2;
                            relation2 = swap;
                        }

                        var components1 = relation1.data();
                        assertThat(components1).isNotNull();
                        assertThat(components1.c1()).isNotNull();
                        assertThat(components1.componentRelation()).extracting("relationship.value", "target.value").contains(1, 10);
                        assertThat(components1.componentRelations()).extracting("relationship.value", "target.value").containsExactlyInAnyOrder(tuple(2, 20), tuple(3, 30));
                        assertThat(components1.entityRelation()).extracting("relationship.value", "target").contains(4, nestedTarget1);
                        assertThat(components1.entityRelations()).extracting("relationship.value", "target").containsExactlyInAnyOrder(tuple(5, nestedTarget1), tuple(6, nestedTarget2));
                        assertThat(components1.c1234()).hasSize(4);

                        var components2 = relation2.data();
                        assertThat(components2).isNotNull();
                        assertThat(components2.c1()).isNotNull();
                        assertThat(components2.componentRelation()).extracting("relationship.value", "target.value").contains(10, 100);
                        assertThat(components2.componentRelations()).extracting("relationship.value", "target.value").containsExactlyInAnyOrder(tuple(20, 200), tuple(30, 300));
                        assertThat(components2.entityRelation()).extracting("relationship.value", "target").contains(40, nestedTarget1);
                        assertThat(components2.entityRelations()).extracting("relationship.value", "target").containsExactlyInAnyOrder(tuple(50, nestedTarget2), tuple(60, nestedTarget3));
                        assertThat(components2.c1234()).hasSize(3);

                        processed.set(true);
                    });

                    assertThat(processed.get()).isTrue();
                }

                @SuppressWarnings("unused")
                @ComponentSetConfig("MyComponentSet")
                private void myComponentSet(int entityId, C1 c1, ComponentRelation<ExclusiveRelationship, Target> componentRelation,
                        ComponentRelations<RelationshipComponent, Target> componentRelations, EntityRelation<ExclusiveRelationship> entityRelation,
                        EntityRelations<RelationshipComponent> entityRelations, ComponentResult<C1234> c1234) {
                }

            }

        }

        @Nested
        class WildcardComponentRelationTypeTest {

            @Nested
            class ProcessTest extends AbstractTest implements ProcessTestImpl<C> {
                @Override
                public AbstractCompositionDataTest<C> getTest() {
                    return AbstractCompositionDataTest.this;
                }

                @Override
                protected <R> int[] perform(Object[][] components, CompositionData<?> composition, ComponentType<?, R> type, TestConsumer<R> consumer) {
                    return performImpl(components, composition, type, consumer);
                }
            }

            @Nested
            class InsertedTest extends AbstractTest implements InsertedTestImpl<C> {
                @Override
                public AbstractCompositionDataTest<C> getTest() {
                    return AbstractCompositionDataTest.this;
                }

                @Override
                protected <R> int[] perform(Object[][] components, CompositionData<?> composition, ComponentType<?, R> type, TestConsumer<R> consumer) {
                    return performImpl(components, composition, type, consumer);
                }
            }

            @Nested
            class RemovedTest extends AbstractTest implements RemovedTestImpl<C> {
                @Override
                public AbstractCompositionDataTest<C> getTest() {
                    return AbstractCompositionDataTest.this;
                }

                @Override
                protected <R> int[] perform(Object[][] components, CompositionData<?> composition, ComponentType<?, R> type, TestConsumer<R> consumer) {
                    return performImpl(components, composition, type, consumer);
                }

                @Override
                protected void verifyRelationsState(ComponentRelation<?, ?>... relations) {
                    for (var relation : relations) {
                        assertThat(relation).extracting("relationship", "target").as("not reset by iteration").containsOnlyNulls();
                    }
                }
            }

            abstract class AbstractTest {

                protected abstract <R> int[] perform(Object[][] components, CompositionData<?> composition, ComponentType<?, R> type, TestConsumer<R> consumer);

                protected void verifyRelationsState(ComponentRelation<?, ?>... relations) {
                    for (var relation : relations) {
                        assertThat(relation).extracting("relationship", "target").as("not reset by iteration").doesNotContainNull();
                    }
                }

                @Test
                void testWildcardRelationship() {
                    var fetchType = ComponentType.wildcardRelation(Object.class, Target.class);
                    var composition = composition(Composition.all(P1.class), fetchType);

                    var relation11 = Relation.create(new C1(), new Target(1));
                    var relation112 = Relation.create(new C1(), new Target(2));
                    var relation12 = Relation.create(new C1(), new Target2(2));
                    var relation21 = Relation.create(new C2(), new Target(3));
                    var relation22 = Relation.create(new C2(), new Target2(4));
                    var exclusive = Relation.create(new ExclusiveRelationship(1), new Target(1));

                    var entities = new Object[][] {
                            { new P1(), relation11, relation112, relation21, relation12, relation22, exclusive }
                    };

                    var processed = new AtomicBoolean(false);
                    perform(entities, composition, fetchType, (id, relations) -> {
                        assertThat(id).isEqualTo(0);

                        assertThat(relations).isNotNull();
                        assertThat(relations).asInstanceOf(InstanceOfAssertFactories.ITERABLE).containsExactlyInAnyOrder(relation11, relation112, relation21, exclusive);

                        assertThat(relations.size()).isEqualTo(4);
                        assertThat(relations.get(0)).isIn(relation11, relation112, relation21, exclusive);
                        assertThat(relations.get(1)).isIn(relation11, relation112, relation21, exclusive);
                        assertThat(relations.get(2)).isIn(relation11, relation112, relation21, exclusive);
                        assertThat(relations.get(3)).isIn(relation11, relation112, relation21, exclusive);

                        processed.set(true);
                    });

                    assertThat(processed.get()).isTrue();

                    verifyRelationsState(relation11, relation112, relation21, relation12, relation22, exclusive);
                }

                @Test
                void testWildcardTarget() {
                    var fetchType = ComponentType.wildcardRelation(C1.class, Object.class);
                    var composition = composition(Composition.all(P1.class), fetchType);

                    var relation11 = Relation.create(new C1(), new C1());
                    var relation112 = Relation.create(new C1(), new Target(2));
                    var relation12 = Relation.create(new C1(), new C2());
                    var relation21 = Relation.create(new C2(), new C1());
                    var relation22 = Relation.create(new C2(), new C2());
                    var exclusive = Relation.create(new ExclusiveRelationship(1), new Target(1));

                    var entities = new Object[][] {
                            { new P1(), relation11, relation112, relation21, relation12, relation22, exclusive }
                    };

                    var processed = new AtomicBoolean(false);
                    perform(entities, composition, fetchType, (id, relations) -> {
                        assertThat(id).isEqualTo(0);

                        assertThat(relations).isNotNull();
                        assertThat(relations).asInstanceOf(InstanceOfAssertFactories.ITERABLE).containsExactlyInAnyOrder(relation11, relation112, relation12);

                        assertThat(relations.size()).isEqualTo(3);
                        assertThat(relations.get(0)).isIn(relation11, relation112, relation12);
                        assertThat(relations.get(1)).isIn(relation11, relation112, relation12);
                        assertThat(relations.get(2)).isIn(relation11, relation112, relation12);

                        processed.set(true);
                    });

                    assertThat(processed.get()).isTrue();

                    verifyRelationsState(relation11, relation112, relation21, relation12, relation22, exclusive);
                }

                @Test
                void testWildcardBoth() {
                    var fetchType = ComponentType.wildcardRelation(Object.class, Object.class);
                    var composition = composition(Composition.all(P1.class), fetchType);

                    var relation11 = Relation.create(new C1(), new C1());
                    var relation112 = Relation.create(new C1(), new Target(2));
                    var relation12 = Relation.create(new C1(), new C2());
                    var relation21 = Relation.create(new C2(), new C1());
                    var relation22 = Relation.create(new C2(), new C2());
                    var exclusive = Relation.create(new ExclusiveRelationship(1), new Target(1));

                    var entities = new Object[][] {
                            { new P1(), relation11, relation112, relation21, relation12, relation22, exclusive }
                    };

                    var processed = new AtomicBoolean(false);
                    perform(entities, composition, fetchType, (id, relations) -> {
                        assertThat(id).isEqualTo(0);

                        assertThat(relations).isNotNull();
                        assertThat(relations).asInstanceOf(InstanceOfAssertFactories.ITERABLE).containsExactlyInAnyOrder(relation11, relation112, relation12, relation21, relation22, exclusive);

                        assertThat(relations.size()).isEqualTo(6);
                        assertThat(relations.get(0)).isIn(relation11, relation112, relation12, relation21, relation22, exclusive);
                        assertThat(relations.get(1)).isIn(relation11, relation112, relation12, relation21, relation22, exclusive);
                        assertThat(relations.get(2)).isIn(relation11, relation112, relation12, relation21, relation22, exclusive);
                        assertThat(relations.get(3)).isIn(relation11, relation112, relation12, relation21, relation22, exclusive);
                        assertThat(relations.get(4)).isIn(relation11, relation112, relation12, relation21, relation22, exclusive);
                        assertThat(relations.get(5)).isIn(relation11, relation112, relation12, relation21, relation22, exclusive);

                        processed.set(true);
                    });

                    assertThat(processed.get()).isTrue();

                    verifyRelationsState(relation11, relation112, relation21, relation12, relation22, exclusive);
                }

            }

        }

        @Nested
        class WildcardEntityRelationTypeTest {

            @Nested
            class ProcessTest extends AbstractTest implements ProcessTestImpl<C> {
                @Override
                public AbstractCompositionDataTest<C> getTest() {
                    return AbstractCompositionDataTest.this;
                }

                @Override
                protected <R> int[] perform(Object[][] components, CompositionData<?> composition, ComponentType<?, R> type, TestConsumer<R> consumer) {
                    return performImpl(components, composition, type, consumer);
                }
            }

            @Nested
            class InsertedTest extends AbstractTest implements InsertedTestImpl<C> {
                @Override
                public AbstractCompositionDataTest<C> getTest() {
                    return AbstractCompositionDataTest.this;
                }

                @Override
                protected <R> int[] perform(Object[][] components, CompositionData<?> composition, ComponentType<?, R> type, TestConsumer<R> consumer) {
                    return performImpl(components, composition, type, consumer);
                }
            }

            @Nested
            class RemovedTest extends AbstractTest implements RemovedTestImpl<C> {
                @Override
                public AbstractCompositionDataTest<C> getTest() {
                    return AbstractCompositionDataTest.this;
                }

                @Override
                protected <R> int[] perform(Object[][] components, CompositionData<?> composition, ComponentType<?, R> type, TestConsumer<R> consumer) {
                    return performImpl(components, composition, type, consumer);
                }

                @Override
                protected void verifyRelationsState(EntityRelation<?>... relations) {
                    for (var relation : relations) {
                        assertThat(relation.relationship()).isNull();
                        assertThat(relation.target()).isEqualTo(-1);
                    }
                }
            }

            abstract class AbstractTest {

                protected abstract <R> int[] perform(Object[][] components, CompositionData<?> composition, ComponentType<?, R> type, TestConsumer<R> consumer);

                protected void verifyRelationsState(EntityRelation<?>... relations) {
                    for (var relation : relations) {
                        assertThat(relation.relationship()).isNotNull();
                        assertThat(relation.target()).isNotEqualTo(-1);
                    }
                }

                @Test
                void testWildcard() {
                    var fetchType = ComponentType.wildcardRelation(Object.class);
                    var composition = composition(Composition.all(P1.class), fetchType);

                    var target1 = world.createEntity();
                    var target2 = world.createEntity();

                    var relation11 = Relation.create(new C1(), target1);
                    var relation12 = Relation.create(new C1(), target2);
                    var relation21 = Relation.create(new C2(), target1);
                    var relation22 = Relation.create(new C2(), target2);
                    var exclusive = Relation.create(new ExclusiveRelationship(1), target1);

                    var entities = new Object[][] {
                            { new P1(), relation11, relation21, relation12, relation22, exclusive }
                    };

                    var processed = new AtomicBoolean(false);
                    perform(entities, composition, fetchType, (id, relations) -> {
                        assertThat(id).isEqualTo(0);

                        assertThat(relations).isNotNull();
                        assertThat(relations).asInstanceOf(InstanceOfAssertFactories.ITERABLE).containsExactlyInAnyOrder(relation11, relation12, relation21, relation22, exclusive);

                        assertThat(relations.size()).isEqualTo(5);
                        assertThat(relations.get(0)).isIn(relation11, relation12, relation21, relation22, exclusive);
                        assertThat(relations.get(1)).isIn(relation11, relation12, relation21, relation22, exclusive);
                        assertThat(relations.get(2)).isIn(relation11, relation12, relation21, relation22, exclusive);
                        assertThat(relations.get(3)).isIn(relation11, relation12, relation21, relation22, exclusive);
                        assertThat(relations.get(4)).isIn(relation11, relation12, relation21, relation22, exclusive);

                        processed.set(true);
                    });

                    assertThat(processed.get()).isTrue();

                    verifyRelationsState(relation11, relation21, relation12, relation22, exclusive);
                }

            }

        }

        @Nested
        class WildcardEntityRelationFetchTypeTest {

            @Nested
            class ProcessTest extends AbstractTest implements ProcessTestImpl<C> {
                @Override
                public AbstractCompositionDataTest<C> getTest() {
                    return AbstractCompositionDataTest.this;
                }

                @Override
                protected <R> int[] perform(Object[][] components, CompositionData<?> composition, ComponentType<?, R> type, TestConsumer<R> consumer) {
                    return performImpl(components, composition, type, consumer);
                }
            }

            @Nested
            class InsertedTest extends AbstractTest implements InsertedTestImpl<C> {
                @Override
                public AbstractCompositionDataTest<C> getTest() {
                    return AbstractCompositionDataTest.this;
                }

                @Override
                protected <R> int[] perform(Object[][] components, CompositionData<?> composition, ComponentType<?, R> type, TestConsumer<R> consumer) {
                    return performImpl(components, composition, type, consumer);
                }
            }

            @Nested
            class RemovedTest extends AbstractTest implements RemovedTestImpl<C> {
                @Override
                public AbstractCompositionDataTest<C> getTest() {
                    return AbstractCompositionDataTest.this;
                }

                @Override
                protected <R> int[] perform(Object[][] components, CompositionData<?> composition, ComponentType<?, R> type, TestConsumer<R> consumer) {
                    return performImpl(components, composition, type, consumer);
                }

                @Override
                protected void verifyRelationsState(EntityRelation<?>... relations) {
                    for (var relation : relations) {
                        assertThat(relation.relationship()).isNull();
                        assertThat(relation.target()).isEqualTo(-1);
                    }
                }
            }

            abstract class AbstractTest {

                protected abstract <R> int[] perform(Object[][] components, CompositionData<?> composition, ComponentType<?, R> type, TestConsumer<R> consumer);

                protected void verifyRelationsState(EntityRelation<?>... relations) {
                    for (var relation : relations) {
                        assertThat(relation.relationship()).isNotNull();
                        assertThat(relation.target()).isNotEqualTo(-1);
                    }
                }

                @Test
                void testWildcardFetch() {
                    var fetchType = ComponentType.wildcardRelation(Object.class, wildcard(C1234.class));
                    var composition = composition(Composition.all(P1.class), fetchType);

                    var component1 = new C1();
                    var component2 = new C2();
                    var component3 = new C3();
                    var component5 = new C5();

                    var target1 = world.createEntity(component1, component2);
                    var target2 = world.createEntity(component3, component5);

                    var relation11 = Relation.create(new C1(), target1);
                    var relation12 = Relation.create(new C1(), target2);
                    var relation21 = Relation.create(new C2(), target1);
                    var relation22 = Relation.create(new C2(), target2);
                    var exclusive = Relation.create(new ExclusiveRelationship(1), target1);

                    var entities = new Object[][] {
                            { new P1(), relation11, relation21, relation12, relation22, exclusive }
                    };

                    var processed = new AtomicBoolean(false);
                    perform(entities, composition, fetchType, (id, relations) -> {
                        assertThat(id).isEqualTo(0);

                        assertThat(relations).isNotNull();
                        assertThat(relations).asInstanceOf(InstanceOfAssertFactories.ITERABLE).containsExactlyInAnyOrder(relation11, relation12, relation21, relation22, exclusive);

                        assertThat(relations.size()).isEqualTo(5);
                        assertThat(relations.get(0)).isIn(relation11, relation12, relation21, relation22, exclusive);
                        assertThat(relations.get(1)).isIn(relation11, relation12, relation21, relation22, exclusive);
                        assertThat(relations.get(2)).isIn(relation11, relation12, relation21, relation22, exclusive);
                        assertThat(relations.get(3)).isIn(relation11, relation12, relation21, relation22, exclusive);
                        assertThat(relations.get(4)).isIn(relation11, relation12, relation21, relation22, exclusive);

                        for (int i = 0, s = relations.size(); i < s; i++) {
                            var relation = relations.get(i);
                            if (relation.target() == target1) {
                                assertThat(relation.data()).containsExactlyInAnyOrder(component1, component2);
                            } else {
                                assertThat(relation.target()).isEqualTo(target2);
                                assertThat(relation.data()).containsExactly(component3);
                            }
                        }

                        processed.set(true);
                    });

                    assertThat(processed.get()).isTrue();

                    verifyRelationsState(relation11, relation21, relation12, relation22, exclusive);
                }

            }

        }

    }

    abstract class AbstractCompositionNTest<C extends CompositionData<?>> extends AbstractCompositionDataTest<C> {

        @Nested
        class ComponentSetTest {

            @Nested
            class ProcessTest extends AbstractTest implements ProcessTestImpl<C> {
                @Override
                public AbstractCompositionDataTest<C> getTest() {
                    return AbstractCompositionNTest.this;
                }

                @Override
                protected <R> int[] perform(Object[][] components, CompositionData<?> composition, ComponentType<?, R> type, TestConsumer<R> consumer) {
                    return performImpl(components, composition, type, consumer);
                }

                @Test
                void testComponentSet_EntitiesBeforeCompositionCreation() {
                    var type = TestComponentSet.TYPE;
                    var handler = new ComponentSetHandler();

                    var entity = world.createEntity();
                    var entity1 = world.createEntity(new C1());
                    var entity2 = world.createEntity(new C2());
                    var entity12 = world.createEntity(new C1(), new C2());

                    var composition = composition(Composition.one(C1.class, C2.class), type);
                    process(composition, type, handler::handle);

                    assertThat(handler.data)
                            .extracting("entityId", "c1", "c2")
                            .doesNotContain(tuple(entity, null, null))
                            .contains(tuple(entity1, new C1(), null))
                            .contains(tuple(entity2, null, new C2()))
                            .contains(tuple(entity12, new C1(), new C2()));
                }
            }

            @Nested
            class InsertedTest extends AbstractTest implements InsertedTestImpl<C> {
                @Override
                public AbstractCompositionDataTest<C> getTest() {
                    return AbstractCompositionNTest.this;
                }

                @Override
                protected <R> int[] perform(Object[][] components, CompositionData<?> composition, ComponentType<?, R> type, TestConsumer<R> consumer) {
                    return performImpl(components, composition, type, consumer);
                }
            }

            @Nested
            class RemovedTest extends AbstractTest implements RemovedTestImpl<C> {
                @Override
                public AbstractCompositionDataTest<C> getTest() {
                    return AbstractCompositionNTest.this;
                }

                @Override
                protected <R> int[] perform(Object[][] components, CompositionData<?> composition, ComponentType<?, R> type, TestConsumer<R> consumer) {
                    return performImpl(components, composition, type, consumer);
                }

                @Test
                void testComponentSet_EntitiesBeforeCompositionCreation() {
                    var type = TestComponentSet.TYPE;
                    var handler = new ComponentSetHandler();

                    var entity = world.createEntity();
                    var entity1 = world.createEntity(new C1());
                    var entity2 = world.createEntity(new C2());
                    var entity12 = world.createEntity(new C1(), new C2());

                    var composition = composition(Composition.one(C1.class, C2.class), type);
                    removed(composition, type, handler::handle);

                    world.deleteEntity(entity);
                    world.deleteEntity(entity1);
                    world.deleteEntity(entity2);
                    world.deleteEntity(entity12);

                    world.process();

                    assertThat(handler.data)
                            .extracting("entityId", "c1", "c2")
                            .doesNotContain(tuple(entity, null, null))
                            .contains(tuple(entity1, new C1(), null))
                            .contains(tuple(entity2, null, new C2()))
                            .contains(tuple(entity12, new C1(), new C2()));
                }

            }

            abstract class AbstractTest {

                protected abstract <R> int[] perform(Object[][] components, CompositionData<?> composition, ComponentType<?, R> type, TestConsumer<R> consumer);

                @Test
                void testComponentSet_EntitiesAfterCompositionCreation() {
                    var type = TestComponentSet.TYPE;
                    var handler = new ComponentSetHandler();

                    var composition = composition(Composition.one(C1.class, C2.class), type);

                    var entities = new Object[][] {
                            { new C1() },
                            { new C2() },
                            { new C1(), new C2() },
                            {}
                    };

                    perform(entities, composition, type, handler::handle);

                    assertThat(handler.data)
                            .extracting("entityId", "c1", "c2")
                            .containsExactlyInAnyOrder(
                                    tuple(0, new C1(), null),
                                    tuple(1, null, new C2()),
                                    tuple(2, new C1(), new C2()));
                }

            }

        }

        @Nested
        class CustomComponentTypeTest {

            private <T> DefaultComponents<T> factoryFallback(DefaultComponentType<T> type) {
                var components = componentMapperManager.getComponents(type.type());
                return new DefaultComponentsImpl<>(components, type.defaultInstance());
            }

            @Test
            void testDefaultComponentType_FallbackImplementation() {
                componentMapperManager.registerCustomComponentType(DefaultComponentType.class, this::factoryFallback);

                var defaultType = new DefaultComponentType<>(component(D1.class), () -> new D1(-1));
                var composition = composition(Composition.all(P1.class), defaultType);

                var entity1 = world.createEntity(new P1(), new D1(1));
                var entity2 = world.createEntity(new P1());

                var processed = new AtomicInteger();

                process(composition, defaultType, (id, d1) -> {
                    if (id == entity1) {
                        assertThat(d1).isEqualTo(new D1(1));

                        processed.incrementAndGet();
                        return;
                    }

                    assertThat(id).isEqualTo(entity2);
                    assertThat(d1).isEqualTo(new D1(-1));

                    processed.incrementAndGet();
                });

                assertThat(processed.get()).isEqualTo(2);
            }

            @Test
            void testDataType2() {
                var dataType = DataType.get(
                        component(C1.class),
                        relation(C1.class, C2.class));

                var composition = composition(Composition.all(P1.class), dataType);

                var component1 = new C1();
                var relation12 = Relation.create(new C1(), new C2());

                var entityId = world.createEntity(new P1(), component1, relation12);

                var processed = new AtomicBoolean(false);
                process(composition, dataType, (id, data) -> {
                    assertThat(id).isEqualTo(entityId);

                    assertThat(data).isNotNull();
                    assertThat(data.component1()).isSameAs(component1);
                    assertThat(data.component2()).containsExactly(relation12);

                    processed.set(true);
                });

                assertThat(processed.get()).isTrue();

                assertThat(relation12).extracting("relationship", "target").as("not reset by iteration").doesNotContainNull();
            }

            @Test
            void testDataType3() {
                var dataType = DataType.get(
                        component(C1.class),
                        relation(C1.class, C2.class),
                        exclusiveRelation(ExclusiveRelationship.class, C1.class));

                var composition = composition(Composition.all(P1.class), dataType);

                var component1 = new C1();
                var relation12 = Relation.create(new C1(), new C2());
                var exclusive1 = Relation.create(new ExclusiveRelationship(1), new C1());

                var entityId = world.createEntity(new P1(), component1, relation12, exclusive1);

                var processed = new AtomicBoolean(false);
                process(composition, dataType, (id, data) -> {
                    assertThat(id).isEqualTo(entityId);

                    assertThat(data).isNotNull();
                    assertThat(data.component1()).isSameAs(component1);
                    assertThat(data.component2()).containsExactly(relation12);
                    assertThat(data.component3()).isSameAs(exclusive1);

                    processed.set(true);
                });

                assertThat(processed.get()).isTrue();

                assertThat(relation12).extracting("relationship", "target").as("not reset by iteration").doesNotContainNull();
            }

            @Test
            void testDataType4() {
                var dataType = DataType.get(
                        component(C1.class),
                        relation(C1.class, C2.class),
                        exclusiveRelation(ExclusiveRelationship.class, C1.class),
                        relation(C1.class));

                var composition = composition(Composition.all(P1.class), dataType);

                var target1 = world.createEntity();

                var component1 = new C1();
                var relation12 = Relation.create(new C1(), new C2());
                var exclusive1 = Relation.create(new ExclusiveRelationship(1), new C1());
                var relation1 = Relation.create(new C1(), target1);

                var entityId = world.createEntity(new P1(), component1, relation12, exclusive1, relation1);

                var processed = new AtomicBoolean(false);
                process(composition, dataType, (id, data) -> {
                    assertThat(id).isEqualTo(entityId);

                    assertThat(data).isNotNull();
                    assertThat(data.component1()).isSameAs(component1);
                    assertThat(data.component2()).containsExactly(relation12);
                    assertThat(data.component3()).isSameAs(exclusive1);
                    assertThat(data.component4()).containsExactly(relation1);

                    processed.set(true);
                });

                assertThat(processed.get()).isTrue();

                assertThat(relation12).extracting("relationship", "target").as("not reset by iteration").doesNotContainNull();
            }

            @Test
            void testDataType5() {
                var dataType = DataType.get(
                        component(C1.class),
                        relation(C1.class, C2.class),
                        exclusiveRelation(ExclusiveRelationship.class, C1.class),
                        relation(C1.class),
                        exclusiveRelation(ExclusiveRelationship.class));

                var composition = composition(Composition.all(P1.class), dataType);

                var target1 = world.createEntity();
                var target2 = world.createEntity();

                var component1 = new C1();
                var relation12 = Relation.create(new C1(), new C2());
                var exclusive1 = Relation.create(new ExclusiveRelationship(1), new C1());
                var entityRelation1 = Relation.create(new C1(), target1);
                var entityExclusive1 = Relation.create(new ExclusiveRelationship(1), target2);

                var entityId = world.createEntity(new P1(), component1, relation12, exclusive1, entityRelation1, entityExclusive1);

                var processed = new AtomicBoolean(false);
                process(composition, dataType, (id, data) -> {
                    assertThat(id).isEqualTo(entityId);

                    assertThat(data).isNotNull();
                    assertThat(data.component1()).isSameAs(component1);
                    assertThat(data.component2()).containsExactly(relation12);
                    assertThat(data.component3()).isSameAs(exclusive1);
                    assertThat(data.component4()).containsExactly(entityRelation1);
                    assertThat(data.component5()).isSameAs(entityExclusive1);

                    processed.set(true);
                });

                assertThat(processed.get()).isTrue();

                assertThat(relation12).extracting("relationship", "target").as("not reset by iteration").doesNotContainNull();
            }

            @Test
            void testDataType6() {
                var dataType = DataType.get(
                        component(C1.class),
                        relation(C1.class, C2.class),
                        exclusiveRelation(ExclusiveRelationship.class, C1.class),
                        relation(C1.class),
                        exclusiveRelation(ExclusiveRelationship.class),
                        ComponentType.relation(C4.class, component(C3.class)));

                var composition = composition(Composition.all(P1.class), dataType);

                var comonentTarget3 = new C3();

                var target1 = world.createEntity();
                var target2 = world.createEntity();
                var target3 = world.createEntity(comonentTarget3);

                var component1 = new C1();
                var relation12 = Relation.create(new C1(), new C2());
                var exclusive1 = Relation.create(new ExclusiveRelationship(1), new C1());
                var entityRelation1 = Relation.create(new C1(), target1);
                var entityExclusive1 = Relation.create(new ExclusiveRelationship(2), target2);
                var fetchRelation1 = Relation.create(new C4(), target3);

                var entityId = world.createEntity(new P1(), component1, relation12, exclusive1, entityRelation1, entityExclusive1, fetchRelation1);

                var processed = new AtomicBoolean(false);
                process(composition, dataType, (id, data) -> {
                    assertThat(id).isEqualTo(entityId);

                    assertThat(data).isNotNull();
                    assertThat(data.component1()).isSameAs(component1);
                    assertThat(data.component2()).containsExactly(relation12);
                    assertThat(data.component3()).isSameAs(exclusive1);
                    assertThat(data.component4()).containsExactly(entityRelation1);
                    assertThat(data.component5()).isSameAs(entityExclusive1);

                    assertThat(data.component6())
                            .extracting("relationship", "target", "data")
                            .containsExactly(tuple(new C4(), target3, comonentTarget3));

                    processed.set(true);
                });

                assertThat(processed.get()).isTrue();

                assertThat(relation12).extracting("relationship", "target").as("not reset by iteration").doesNotContainNull();
            }

            @Test
            void testDataType7() {
                var dataType = DataType.get(
                        component(C1.class),
                        relation(C1.class, C2.class),
                        exclusiveRelation(ExclusiveRelationship.class, C1.class),
                        relation(C1.class),
                        exclusiveRelation(ExclusiveRelationship.class),
                        ComponentType.relation(C4.class, component(C3.class)),
                        ComponentType.relation(C5.class, MyComponentSet.TYPE));

                var composition = composition(Composition.all(P1.class), dataType);

                var comonentTarget3 = new C3();
                var target4component1 = new C1();
                var target4component2 = new C2();

                var target1 = world.createEntity();
                var target2 = world.createEntity();
                var target3 = world.createEntity(comonentTarget3);
                var target4 = world.createEntity(target4component1, target4component2);

                var component1 = new C1();
                var relation12 = Relation.create(new C1(), new C2());
                var exclusive1 = Relation.create(new ExclusiveRelationship(1), new C1());
                var entityRelation1 = Relation.create(new C1(), target1);
                var entityExclusive1 = Relation.create(new ExclusiveRelationship(2), target2);
                var fetchRelation1 = Relation.create(new C4(), target3);
                var setRelation = Relation.create(new C5(), target4);

                var entityId = world.createEntity(new P1(), component1, relation12, exclusive1, entityRelation1, entityExclusive1, fetchRelation1, setRelation);

                var processed = new AtomicBoolean(false);

                process(composition, dataType, (id, data) -> {
                    assertThat(id).isEqualTo(entityId);

                    assertThat(data).isNotNull();
                    assertThat(data.component1()).isSameAs(component1);
                    assertThat(data.component2()).containsExactly(relation12);
                    assertThat(data.component3()).isSameAs(exclusive1);
                    assertThat(data.component4()).containsExactly(entityRelation1);
                    assertThat(data.component5()).isSameAs(entityExclusive1);

                    assertThat(data.component6())
                            .extracting("relationship", "target", "data")
                            .containsExactly(tuple(new C4(), target3, comonentTarget3));

                    var component7 = data.component7();
                    assertThat(component7).extracting("relationship", "target").containsExactly(tuple(new C5(), target4));

                    var data7 = component7.get(0).data();
                    assertThat(data7.c1()).isSameAs(target4component1);
                    assertThat(data7.c1234())
                            .hasSize(2)
                            .anySatisfy(component -> assertThat(component).isSameAs(target4component1))
                            .anySatisfy(component -> assertThat(component).isSameAs(target4component2));

                    processed.set(true);
                });

                assertThat(processed.get()).isTrue();

                assertThat(relation12).extracting("relationship", "target").as("not reset by iteration").doesNotContainNull();
            }

            @Test
            void testNestedDataType() {
                var innerDataType = DataType.get(
                        component(C1.class),
                        relation(C1.class, C2.class));

                var dataType = DataType.get(component(C2.class), innerDataType);
                var composition = composition(Composition.all(P1.class), dataType);

                var component1 = new C1();
                var component2 = new C2();
                var relation12 = Relation.create(new C1(), new C2());

                var entityId = world.createEntity(new P1(), component1, component2, relation12);

                var processed = new AtomicBoolean(false);
                process(composition, dataType, (id, data) -> {
                    assertThat(id).isEqualTo(entityId);

                    assertThat(data).isNotNull();
                    assertThat(data.component1()).isSameAs(component2);

                    var innerData = data.component2();
                    assertThat(innerData).isNotNull();
                    assertThat(innerData.component1()).isSameAs(component1);
                    assertThat(innerData.component2()).containsExactly(relation12);

                    processed.set(true);
                });

                assertThat(processed.get()).isTrue();

                assertThat(relation12).extracting("relationship", "target").as("not reset by iteration").doesNotContainNull();
            }

        }

    }

    private class ComponentSetHandler {

        record Data(int entityId, C1 c1, C2 c2) {
        }

        private final List<Data> data = new ArrayList<>();

        private void handle(int entityId, TestComponentSet componentSet) {
            this.data.add(new Data(entityId, componentSet.c1(), componentSet.c2()));
        }

        @ComponentSetConfig("TestComponentSet")
        private void handle(int entityId, C1 c1, C2 c2) {
            this.data.add(new Data(entityId, c1, c2));
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
    class ComponentSetTest {

        @Test
        void testRetrieveComponentSet() {
            var componentSet = MyComponentSet.TYPE;
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
            composition.process((id, c1, componentRelation, componentRelations, entityRelation, entityRelations, c1234) -> {
                assertThat(id).isEqualTo(entityId);

                assertThat(c1).isNotNull();
                assertThat(componentRelation).extracting("relationship.value", "target.value").contains(1, 10);
                assertThat(componentRelations).extracting("relationship.value", "target.value").containsExactlyInAnyOrder(tuple(2, 20), tuple(3, 30));
                assertThat(entityRelation).extracting("relationship.value", "target").contains(4, target1);
                assertThat(entityRelations).extracting("relationship.value", "target").containsExactlyInAnyOrder(tuple(5, target1), tuple(6, target2));
                assertThat(c1234).hasSize(4);

                processed.set(true);
            });

            assertThat(processed.get()).isTrue();
        }

    }

    private void verifyHasComposition(int entityId, Composition.Builder builder) {
        var composition = world.createComposition(builder);
        assertThat(composition.isInterested(entityId)).isTrue();
    }

    interface C12 {
    }

    public interface C1234 {
    }

    interface C45 {
    }

    public record C1() implements C12, C1234 {
    }

    public record C2() implements C12, C1234 {
    }

    private record C3() implements C1234 {
    }

    private record C4() implements C1234, C45 {
    }

    private record C5() implements C45 {
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

    public record D1(int value) {
    }

    public record RelationshipComponent(int value) {
    }

    public record ExclusiveRelationship(int value) implements Exclusive {
    }

    public record Target(int value) {
    }

    record Target2(int value) {
    }

    record DefaultComponentType<T>(RegularComponentType<T, T> type, Supplier<T> defaultInstance) implements CustomComponentType<T, T, DefaultComponents<T>> {
        @Override
        public boolean matches(RegularComponentType<?, ?> otherType) {
            return this.type.equals(otherType);
        }
    }

    sealed interface DefaultComponents<T> extends CustomComponentMapper<T, T> {
    }

    final class DefaultComponentsImpl<T> implements DefaultComponents<T> {

        private final Components<T, T> components;
        private final Supplier<T> defaultInstance;

        public DefaultComponentsImpl(Components<T, T> components, Supplier<T> defaultInstance) {
            this.components = components;
            this.defaultInstance = Objects.requireNonNull(defaultInstance, "defaultInstance cannot be null");
        }

        @Override
        public T get(int entityId) {
            var result = components.get(entityId);

            return result != null ? result : defaultInstance.get();
        }

        @Override
        public T get(DataAccessor accessor) {
            var result = components.get(accessor);

            return result != null ? result : defaultInstance.get();
        }

        @Override
        public boolean has(int entityId) {
            throw new UnsupportedOperationException("irrelevant for test");
        }

        @Override
        public boolean remove(int entityId) {
            throw new UnsupportedOperationException("irrelevant for test");
        }

    }

}
