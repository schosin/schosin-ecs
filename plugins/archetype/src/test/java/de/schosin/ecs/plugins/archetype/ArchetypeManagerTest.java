package de.schosin.ecs.plugins.archetype;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.ObjIntConsumer;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import de.schosin.ecs.api.Pooled;
import de.schosin.ecs.api.components.Relation;
import de.schosin.ecs.api.components.Relations;
import de.schosin.ecs.api.components.types.ComponentType;
import de.schosin.ecs.api.components.types.ComponentType.RegularComponentType;
import de.schosin.ecs.engine.events.builtin.EntitiesEvent.EntitiesInsertedEvent;
import de.schosin.ecs.engine.events.builtin.EntityEvent.EntityInsertedEvent;
import de.schosin.ecs.storage.api.StorageEngineException;
import de.schosin.ecs.test.AbstractEcsTest;
import de.schosin.ecs.utils.collections.ImmutableIntBag;

class ArchetypeManagerTest extends AbstractEcsTest<ArchetypeWorld> {

    @Nested
    @SuppressWarnings({ "rawtypes", "unchecked" })
    class Archetype1Test extends AbstractArchetypeTest<Archetype1<Object>> {

        @Override
        protected RegularComponentType<?, ?>[] getComponentTypes(RegularComponentType<Object, ?> componentType) {
            return new RegularComponentType<?, ?>[] { componentType };
        }

        @Override
        protected Archetype1<Object> getArchetype(RegularComponentType<?, ?> componentType) {
            return (Archetype1) world.createArchetype(componentType);
        }

        @Override
        protected Object[] createComponents(Archetype1<Object> archetype, int index, Object component) {
            return new Object[] { component };
        }

        @Override
        protected int createEntity(Archetype1<Object> archetype, Object component) {
            return archetype.create(component);
        }

        @Override
        protected ImmutableIntBag createEntities(Archetype1<Object> archetype, ComponentProvider provider) {
            return archetype.createBatch(provider.list.size(), (i, factory) -> {
                var components = provider.list.get(i);
                factory.create(components[0]);
            });
        }

        @Override
        protected int dontCallFactory(Archetype1<Object> archetype) {
            return archetype.create((i, factory) -> {
            });
        }

        @Override
        protected ImmutableIntBag dontCallFactory(int count, Archetype1<Object> archetype) {
            return archetype.createBatch(count, (i, factory) -> {
            });
        }

    }

    @Nested
    @SuppressWarnings({ "rawtypes", "unchecked" })
    class Archetype2Test extends AbstractArchetypeNTest<Archetype2<Object, Object>> {

        @Override
        protected RegularComponentType<?, ?>[] getComponentTypes(RegularComponentType<Object, ?> componentType) {
            return new RegularComponentType<?, ?>[] { componentType, component(D1.class) };
        }

        @Override
        protected Archetype2<Object, Object> getArchetype(RegularComponentType<?, ?> componentType) {
            return (Archetype2) world.createArchetype(componentType, component(D1.class));
        }

        @Override
        protected Archetype2<Object, Object> getArchetype(RegularComponentType<?, ?> componentType1, RegularComponentType<?, ?> componentType2) {
            return (Archetype2) world.createArchetype(componentType1, componentType2);
        }

        @Override
        protected Object[] createComponents(Archetype2<Object, Object> archetype, int index, Object component) {
            return new Object[] { component, new D1(index) };
        }

        @Override
        protected Object[] createComponents(Archetype2<Object, Object> archetype, int index, Object component1, Object component2) {
            return new Object[] { component1, component2 };
        }

        @Override
        protected int createEntity(Archetype2<Object, Object> archetype, Object component) {
            return archetype.create(component, new D1(0));
        }

        @Override
        protected int createEntity(Archetype2<Object, Object> archetype, Object component1, Object component2) {
            return archetype.create(component1, component2);
        }

        @Override
        protected ImmutableIntBag createEntities(Archetype2<Object, Object> archetype, ComponentProvider provider) {
            var expectedIndex = new AtomicInteger(0);

            var result = archetype.createBatch(provider.list.size(), (i, factory) -> {
                assertThat(i).isEqualTo(expectedIndex.getAndIncrement());

                var components = provider.list.get(i);
                factory.create(components[0], components[1]);
            });

            assertThat(expectedIndex.get()).isEqualTo(provider.list.size());

            return result;
        }

        @Override
        protected int dontCallFactory(Archetype2<Object, Object> archetype) {
            return archetype.create(factory -> {
            });
        }

        @Override
        protected ImmutableIntBag dontCallFactory(int count, Archetype2<Object, Object> archetype) {
            return archetype.createBatch(count, factory -> {
            });
        }

    }

    @Nested
    @SuppressWarnings({ "rawtypes", "unchecked" })
    class Archetype3Test extends AbstractArchetypeNTest<Archetype3<Object, Object, Object>> {

        @Override
        protected RegularComponentType<?, ?>[] getComponentTypes(RegularComponentType<Object, ?> componentType) {
            return new RegularComponentType<?, ?>[] { componentType, component(D1.class), component(D2.class) };
        }

        @Override
        protected Archetype3<Object, Object, Object> getArchetype(RegularComponentType<?, ?> componentType) {
            return (Archetype3) world.createArchetype(componentType, component(D1.class), component(D2.class));
        }

        @Override
        protected Archetype3<Object, Object, Object> getArchetype(RegularComponentType<?, ?> componentType1, RegularComponentType<?, ?> componentType2) {
            return (Archetype3) world.createArchetype(componentType1, componentType2, component(D1.class));
        }

        @Override
        protected Object[] createComponents(Archetype3<Object, Object, Object> archetype, int index, Object component) {
            return new Object[] { component, new D1(index), new D2(index) };
        }

        @Override
        protected Object[] createComponents(Archetype3<Object, Object, Object> archetype, int index, Object component1, Object component2) {
            return new Object[] { component1, component2, new D1(index) };
        }

        @Override
        protected int createEntity(Archetype3<Object, Object, Object> archetype, Object component) {
            return archetype.create(component, new D1(0), new D2(0));
        }

        @Override
        protected int createEntity(Archetype3<Object, Object, Object> archetype, Object component1, Object component2) {
            return archetype.create(component1, component2, new D1(0));
        }

        @Override
        protected ImmutableIntBag createEntities(Archetype3<Object, Object, Object> archetype, ComponentProvider provider) {
            var expectedIndex = new AtomicInteger(0);

            var result = archetype.createBatch(provider.list.size(), (i, factory) -> {
                assertThat(i).isEqualTo(expectedIndex.getAndIncrement());

                var components = provider.list.get(i);
                factory.create(components[0], components[1], components[2]);
            });

            assertThat(expectedIndex.get()).isEqualTo(provider.list.size());

            return result;
        }

        @Override
        protected int dontCallFactory(Archetype3<Object, Object, Object> archetype) {
            return archetype.create(factory -> {
            });
        }

        @Override
        protected ImmutableIntBag dontCallFactory(int count, Archetype3<Object, Object, Object> archetype) {
            return archetype.createBatch(count, factory -> {
            });
        }

    }

    @Nested
    @SuppressWarnings({ "rawtypes", "unchecked" })
    class Archetype4Test extends AbstractArchetypeNTest<Archetype4<Object, Object, Object, Object>> {

        @Override
        protected RegularComponentType<?, ?>[] getComponentTypes(RegularComponentType<Object, ?> componentType) {
            return new RegularComponentType<?, ?>[] { componentType, component(D1.class), component(D2.class), component(D3.class) };
        }

        @Override
        protected Archetype4<Object, Object, Object, Object> getArchetype(RegularComponentType<?, ?> componentType) {
            return (Archetype4) world.createArchetype(componentType, component(D1.class), component(D2.class), component(D3.class));
        }

        @Override
        protected Archetype4<Object, Object, Object, Object> getArchetype(RegularComponentType<?, ?> componentType1, RegularComponentType<?, ?> componentType2) {
            return (Archetype4) world.createArchetype(componentType1, componentType2, component(D1.class), component(D2.class));
        }

        @Override
        protected Object[] createComponents(Archetype4<Object, Object, Object, Object> archetype, int index, Object component) {
            return new Object[] { component, new D1(index), new D2(index), new D3(index) };
        }

        @Override
        protected Object[] createComponents(Archetype4<Object, Object, Object, Object> archetype, int index, Object component1, Object component2) {
            return new Object[] { component1, component2, new D1(index), new D2(index) };
        }

        @Override
        protected int createEntity(Archetype4<Object, Object, Object, Object> archetype, Object component) {
            return archetype.create(component, new D1(0), new D2(0), new D3(0));
        }

        @Override
        protected int createEntity(Archetype4<Object, Object, Object, Object> archetype, Object component1, Object component2) {
            return archetype.create(component1, component2, new D1(0), new D2(0));
        }

        @Override
        protected ImmutableIntBag createEntities(Archetype4<Object, Object, Object, Object> archetype, ComponentProvider provider) {
            var expectedIndex = new AtomicInteger(0);

            var result = archetype.createBatch(provider.list.size(), (i, factory) -> {
                assertThat(i).isEqualTo(expectedIndex.getAndIncrement());

                var components = provider.list.get(i);
                factory.create(components[0], components[1], components[2], components[3]);
            });

            assertThat(expectedIndex.get()).isEqualTo(provider.list.size());

            return result;
        }

        @Override
        protected int dontCallFactory(Archetype4<Object, Object, Object, Object> archetype) {
            return archetype.create(factory -> {
            });
        }

        @Override
        protected ImmutableIntBag dontCallFactory(int count, Archetype4<Object, Object, Object, Object> archetype) {
            return archetype.createBatch(count, factory -> {
            });
        }

    }

    @Nested
    @SuppressWarnings({ "rawtypes", "unchecked" })
    class Archetype5Test extends AbstractArchetypeNTest<Archetype5<Object, Object, Object, Object, Object>> {

        @Override
        protected RegularComponentType<?, ?>[] getComponentTypes(RegularComponentType<Object, ?> componentType) {
            return new RegularComponentType<?, ?>[] { componentType, component(D1.class), component(D2.class), component(D3.class), component(D4.class) };
        }

        @Override
        protected Archetype5<Object, Object, Object, Object, Object> getArchetype(RegularComponentType<?, ?> componentType) {
            return (Archetype5) world.createArchetype(componentType, component(D1.class), component(D2.class), component(D3.class), component(D4.class));
        }

        @Override
        protected Archetype5<Object, Object, Object, Object, Object> getArchetype(RegularComponentType<?, ?> componentType1, RegularComponentType<?, ?> componentType2) {
            return (Archetype5) world.createArchetype(componentType1, componentType2, component(D1.class), component(D2.class), component(D3.class));
        }

        @Override
        protected Object[] createComponents(Archetype5<Object, Object, Object, Object, Object> archetype, int index, Object component) {
            return new Object[] { component, new D1(index), new D2(index), new D3(index), new D4(index) };
        }

        @Override
        protected Object[] createComponents(Archetype5<Object, Object, Object, Object, Object> archetype, int index, Object component1, Object component2) {
            return new Object[] { component1, component2, new D1(index), new D2(index), new D3(index) };
        }

        @Override
        protected int createEntity(Archetype5<Object, Object, Object, Object, Object> archetype, Object component) {
            return archetype.create(component, new D1(0), new D2(0), new D3(0), new D4(0));
        }

        @Override
        protected int createEntity(Archetype5<Object, Object, Object, Object, Object> archetype, Object component1, Object component2) {
            return archetype.create(component1, component2, new D1(0), new D2(0), new D3(0));
        }

        @Override
        protected ImmutableIntBag createEntities(Archetype5<Object, Object, Object, Object, Object> archetype, ComponentProvider provider) {
            var expectedIndex = new AtomicInteger(0);

            var result = archetype.createBatch(provider.list.size(), (i, factory) -> {
                assertThat(i).isEqualTo(expectedIndex.getAndIncrement());

                var components = provider.list.get(i);
                factory.create(components[0], components[1], components[2], components[3], components[4]);
            });

            assertThat(expectedIndex.get()).isEqualTo(provider.list.size());

            return result;
        }

        @Override
        protected int dontCallFactory(Archetype5<Object, Object, Object, Object, Object> archetype) {
            return archetype.create(factory -> {
            });
        }

        @Override
        protected ImmutableIntBag dontCallFactory(int count, Archetype5<Object, Object, Object, Object, Object> archetype) {
            return archetype.createBatch(count, factory -> {
            });
        }

    }

    @Nested
    @SuppressWarnings({ "rawtypes", "unchecked" })
    class Archetype6Test extends AbstractArchetypeNTest<Archetype6<Object, Object, Object, Object, Object, Object>> {

        @Override
        protected RegularComponentType<?, ?>[] getComponentTypes(RegularComponentType<Object, ?> componentType) {
            return new RegularComponentType<?, ?>[] { componentType, component(D1.class), component(D2.class), component(D3.class), component(D4.class), component(D5.class) };
        }

        @Override
        protected Archetype6<Object, Object, Object, Object, Object, Object> getArchetype(RegularComponentType<?, ?> componentType) {
            return (Archetype6) world.createArchetype(componentType, component(D1.class), component(D2.class), component(D3.class), component(D4.class), component(D5.class));
        }

        @Override
        protected Archetype6<Object, Object, Object, Object, Object, Object> getArchetype(RegularComponentType<?, ?> componentType1, RegularComponentType<?, ?> componentType2) {
            return (Archetype6) world.createArchetype(componentType1, componentType2, component(D1.class), component(D2.class), component(D3.class), component(D4.class));
        }

        @Override
        protected Object[] createComponents(Archetype6<Object, Object, Object, Object, Object, Object> archetype, int index, Object component) {
            return new Object[] { component, new D1(index), new D2(index), new D3(index), new D4(index), new D5(index) };
        }

        @Override
        protected Object[] createComponents(Archetype6<Object, Object, Object, Object, Object, Object> archetype, int index, Object component1, Object component2) {
            return new Object[] { component1, component2, new D1(index), new D2(index), new D3(index), new D4(index) };
        }

        @Override
        protected int createEntity(Archetype6<Object, Object, Object, Object, Object, Object> archetype, Object component) {
            return archetype.create(component, new D1(0), new D2(0), new D3(0), new D4(0), new D5(0));
        }

        @Override
        protected int createEntity(Archetype6<Object, Object, Object, Object, Object, Object> archetype, Object component1, Object component2) {
            return archetype.create(component1, component2, new D1(0), new D2(0), new D3(0), new D4(0));
        }

        @Override
        protected ImmutableIntBag createEntities(Archetype6<Object, Object, Object, Object, Object, Object> archetype, ComponentProvider provider) {
            var expectedIndex = new AtomicInteger(0);

            var result = archetype.createBatch(provider.list.size(), (i, factory) -> {
                assertThat(i).isEqualTo(expectedIndex.getAndIncrement());

                var components = provider.list.get(i);
                factory.create(components[0], components[1], components[2], components[3], components[4], components[5]);
            });

            assertThat(expectedIndex.get()).isEqualTo(provider.list.size());

            return result;
        }

        @Override
        protected int dontCallFactory(Archetype6<Object, Object, Object, Object, Object, Object> archetype) {
            return archetype.create(factory -> {
            });
        }

        @Override
        protected ImmutableIntBag dontCallFactory(int count, Archetype6<Object, Object, Object, Object, Object, Object> archetype) {
            return archetype.createBatch(count, factory -> {
            });
        }

    }

    @Nested
    @SuppressWarnings({ "rawtypes", "unchecked" })
    class Archetype7Test extends AbstractArchetypeNTest<Archetype7<Object, Object, Object, Object, Object, Object, Object>> {

        @Override
        protected RegularComponentType<?, ?>[] getComponentTypes(RegularComponentType<Object, ?> componentType) {
            return new RegularComponentType<?, ?>[] { componentType, component(D1.class), component(D2.class), component(D3.class), component(D4.class), component(D5.class), component(D6.class) };
        }

        @Override
        protected Archetype7<Object, Object, Object, Object, Object, Object, Object> getArchetype(RegularComponentType<?, ?> componentType) {
            return (Archetype7) world.createArchetype(componentType, component(D1.class), component(D2.class), component(D3.class), component(D4.class), component(D5.class), component(D6.class));
        }

        @Override
        protected Archetype7<Object, Object, Object, Object, Object, Object, Object> getArchetype(RegularComponentType<?, ?> componentType1, RegularComponentType<?, ?> componentType2) {
            return (Archetype7) world.createArchetype(componentType1, componentType2, component(D1.class), component(D2.class), component(D3.class), component(D4.class), component(D5.class));
        }

        @Override
        protected Object[] createComponents(Archetype7<Object, Object, Object, Object, Object, Object, Object> archetype, int index, Object component) {
            return new Object[] { component, new D1(index), new D2(index), new D3(index), new D4(index), new D5(index), new D6(index) };
        }

        @Override
        protected Object[] createComponents(Archetype7<Object, Object, Object, Object, Object, Object, Object> archetype, int index, Object component1, Object component2) {
            return new Object[] { component1, component2, new D1(index), new D2(index), new D3(index), new D4(index), new D5(index) };
        }

        @Override
        protected int createEntity(Archetype7<Object, Object, Object, Object, Object, Object, Object> archetype, Object component) {
            return archetype.create(component, new D1(0), new D2(0), new D3(0), new D4(0), new D5(0), new D6(0));
        }

        @Override
        protected int createEntity(Archetype7<Object, Object, Object, Object, Object, Object, Object> archetype, Object component1, Object component2) {
            return archetype.create(component1, component2, new D1(0), new D2(0), new D3(0), new D4(0), new D5(0));
        }

        @Override
        protected ImmutableIntBag createEntities(Archetype7<Object, Object, Object, Object, Object, Object, Object> archetype, ComponentProvider provider) {
            var expectedIndex = new AtomicInteger(0);

            var result = archetype.createBatch(provider.list.size(), (i, factory) -> {
                assertThat(i).isEqualTo(expectedIndex.getAndIncrement());

                var components = provider.list.get(i);
                factory.create(components[0], components[1], components[2], components[3], components[4], components[5], components[6]);
            });

            assertThat(expectedIndex.get()).isEqualTo(provider.list.size());

            return result;
        }

        @Override
        protected int dontCallFactory(Archetype7<Object, Object, Object, Object, Object, Object, Object> archetype) {
            return archetype.create(factory -> {
            });
        }

        @Override
        protected ImmutableIntBag dontCallFactory(int count, Archetype7<Object, Object, Object, Object, Object, Object, Object> archetype) {
            return archetype.createBatch(count, factory -> {
            });
        }

    }

    @Nested
    @SuppressWarnings({ "rawtypes", "unchecked" })
    class Archetype8Test extends AbstractArchetypeNTest<Archetype8<Object, Object, Object, Object, Object, Object, Object, Object>> {

        @Override
        protected RegularComponentType<?, ?>[] getComponentTypes(RegularComponentType<Object, ?> componentType) {
            return new RegularComponentType<?, ?>[] { componentType, component(D1.class), component(D2.class), component(D3.class), component(D4.class), component(D5.class), component(D6.class),
                    component(D7.class) };
        }

        @Override
        protected Archetype8<Object, Object, Object, Object, Object, Object, Object, Object> getArchetype(RegularComponentType<?, ?> componentType) {
            return (Archetype8) world.createArchetype(componentType, component(D1.class), component(D2.class), component(D3.class), component(D4.class), component(D5.class), component(D6.class),
                    component(D7.class));
        }

        @Override
        protected Archetype8<Object, Object, Object, Object, Object, Object, Object, Object> getArchetype(RegularComponentType<?, ?> componentType1, RegularComponentType<?, ?> componentType2) {
            return (Archetype8) world.createArchetype(componentType1, componentType2, component(D1.class), component(D2.class), component(D3.class), component(D4.class), component(D5.class),
                    component(D6.class));
        }

        @Override
        protected Object[] createComponents(Archetype8<Object, Object, Object, Object, Object, Object, Object, Object> archetype, int index, Object component) {
            return new Object[] { component, new D1(index), new D2(index), new D3(index), new D4(index), new D5(index), new D6(index), new D7(index) };
        }

        @Override
        protected Object[] createComponents(Archetype8<Object, Object, Object, Object, Object, Object, Object, Object> archetype, int index, Object component1, Object component2) {
            return new Object[] { component1, component2, new D1(index), new D2(index), new D3(index), new D4(index), new D5(index), new D6(index) };
        }

        @Override
        protected int createEntity(Archetype8<Object, Object, Object, Object, Object, Object, Object, Object> archetype, Object component) {
            return archetype.create(component, new D1(0), new D2(0), new D3(0), new D4(0), new D5(0), new D6(0), new D7(0));
        }

        @Override
        protected int createEntity(Archetype8<Object, Object, Object, Object, Object, Object, Object, Object> archetype, Object component1, Object component2) {
            return archetype.create(component1, component2, new D1(0), new D2(0), new D3(0), new D4(0), new D5(0), new D6(0));
        }

        @Override
        protected ImmutableIntBag createEntities(Archetype8<Object, Object, Object, Object, Object, Object, Object, Object> archetype, ComponentProvider provider) {
            var expectedIndex = new AtomicInteger(0);

            var result = archetype.createBatch(provider.list.size(), (i, factory) -> {
                assertThat(i).isEqualTo(expectedIndex.getAndIncrement());

                var components = provider.list.get(i);
                factory.create(components[0], components[1], components[2], components[3], components[4], components[5], components[6], components[7]);
            });

            assertThat(expectedIndex.get()).isEqualTo(provider.list.size());

            return result;
        }

        @Override
        protected int dontCallFactory(Archetype8<Object, Object, Object, Object, Object, Object, Object, Object> archetype) {
            return archetype.create(factory -> {
            });
        }

        @Override
        protected ImmutableIntBag dontCallFactory(int count, Archetype8<Object, Object, Object, Object, Object, Object, Object, Object> archetype) {
            return archetype.createBatch(count, factory -> {
            });
        }

    }

    abstract class AbstractArchetypeNTest<A extends BaseArchetype<?>> extends AbstractArchetypeTest<A> {

        protected abstract A getArchetype(RegularComponentType<?, ?> componentType1, RegularComponentType<?, ?> componentType2);

        protected abstract Object[] createComponents(A archetype, int index, Object component1, Object component2);

        protected abstract int createEntity(A archetype, Object component1, Object component2);

        @ParameterizedTest
        @MethodSource("componentsSupplier")
        void testCreateArchetype_DuplicateTypes(IntFunction<Object> componentFunction) {
            var component = componentFunction.apply(0);
            var componentType = ComponentType.detectComponentType(component);

            assertThatThrownBy(() -> getArchetype(componentType, componentType))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContainingAll(componentType.toString(), "duplicates");
        }

    }

    abstract class AbstractArchetypeTest<A extends BaseArchetype<?>> {

        protected abstract RegularComponentType<?, ?>[] getComponentTypes(RegularComponentType<Object, ?> componentType);

        protected abstract A getArchetype(RegularComponentType<?, ?> componentType);

        protected abstract Object[] createComponents(A archetype, int index, Object component);

        protected abstract int createEntity(A archetype, Object component);

        protected abstract ImmutableIntBag createEntities(A archetype, ComponentProvider provider);

        protected abstract ImmutableIntBag dontCallFactory(int count, A archetype);

        protected abstract int dontCallFactory(A archetype);

        @ParameterizedTest
        @MethodSource("componentsSupplier")
        void testCreate(IntFunction<Object> componentFunction) {
            var component = componentFunction.apply(0);
            var componentType = ComponentType.detectComponentType(component);

            var archetype = getArchetype(componentType);

            // Call
            var entityId = createEntity(archetype, component);

            // Verify
            verifyHasComponents(entityId, componentType);
            verifyArchetypeHasComponents(entityId, componentType);
        }

        @ParameterizedTest
        @MethodSource("componentsSupplier")
        @SuppressWarnings("unchecked")
        void testCreate_WithFixedEnums(IntFunction<Object> componentFunction) {
            var component = componentFunction.apply(0);
            var componentType = ComponentType.detectComponentType(component);

            var archetype = (A) getArchetype(componentType).with(E1.INSTANCE, E2.INSTANCE);

            // Call
            var entityId = createEntity(archetype, component);

            // Verify
            verifyHasComponents(entityId, componentType, component(E1.class), component(E2.class));
            verifyArchetypeHasComponents(entityId, componentType, component(E1.class), component(E2.class));
        }

        @ParameterizedTest
        @MethodSource("componentsSupplier")
        void testCreate_InsertedCallbacks(IntFunction<Object> componentFunction) {
            var component = componentFunction.apply(0);
            var componentType = ComponentType.detectComponentType(component);
            var componentTypes = getComponentTypes(componentType);

            var archetype = getArchetype(componentType);

            var called = new AtomicBoolean(false);
            eventManager.registerEventHandler(EntityInsertedEvent.class, event -> {
                assertThat(event.archetype().getComponentTypes()).containsExactlyInAnyOrder(componentTypes);
                called.set(true);
            });

            // Verify
            verify(verify -> {
                verify.expectInserted(componentTypes);

                verify.expectNoMoreInserted();
                verify.expectNoMoreUpdated();
                verify.expectNoMoreRemoved();

                // Call
                createEntity(archetype, component);
            });

            assertThat(called.get()).isTrue();
        }

        @Test
        void testCreate_FactoryNotCalled() {
            var archetype = getArchetype(component(C1.class));

            assertThatThrownBy(() -> dontCallFactory(archetype))
                    .isInstanceOf(StorageEngineException.class)
                    .hasMessageContainingAll(C1.class.getSimpleName(), "index 0", "was 'null'");
        }

        @Test
        void testCreate_NullInstance() {
            var archetype = getArchetype(component(C1.class));

            // Call
            assertThatThrownBy(() -> createEntity(archetype, null))
                    .isInstanceOf(StorageEngineException.class)
                    .hasMessageContainingAll(C1.class.getSimpleName(), "index 0", "was 'null'");
        }

        @Test
        void testCreate_TypeMismatch() {
            var archetype = getArchetype(component(C1.class));

            // Call
            assertThatThrownBy(() -> createEntity(archetype, new C2()))
                    .isInstanceOf(StorageEngineException.class)
                    .message()
                    .containsSubsequence(
                            "Expected", C1.class.getSimpleName(), "index 0",
                            " was ", C2.class.getSimpleName());
        }

        @Test
        void testCreate_WithPooledInstance() {
            var archetype = getArchetype(component(C1.class));

            assertThatThrownBy(() -> archetype.with(new D1()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContainingAll(D1.class.getSimpleName(), "cannot implement Pooled");
        }

        @Test
        void testCreate_WithComponentRelationInstance() {
            var archetype = getArchetype(component(C1.class));
            var relation = Relation.create(new C1(), new C2());

            assertThatThrownBy(() -> archetype.with(relation))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContainingAll(relation.toString(), "cannot implement Relation.");
        }

        @Test
        void testCreate_WithEntityRelationInstance() {
            var archetype = getArchetype(component(C1.class));
            var relation = Relation.create(new C1(), 42);

            assertThatThrownBy(() -> archetype.with(relation))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContainingAll(relation.toString(), "cannot implement Relation.");
        }

        @Test
        void testCreate_WithComponentRelationsInstance() {
            var archetype = getArchetype(component(C1.class));
            var relations = Relations.create(new C1(), 42);

            assertThatThrownBy(() -> archetype.with(relations))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContainingAll(relations.toString(), "cannot implement Relations.");
        }

        @Test
        void testCreate_WithEntityRelationsInstance() {
            var archetype = getArchetype(component(C1.class));
            var relations = Relations.create(new C1(), 42);

            assertThatThrownBy(() -> archetype.with(relations))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContainingAll(relations.toString(), "cannot implement Relations.");
        }

        @ParameterizedTest
        @MethodSource("countComponentsSupplier")
        void testCreateBatch(int count, IntFunction<Object> componentFunction) {
            var componentType = ComponentType.detectComponentType(componentFunction.apply(0));
            var archetype = getArchetype(componentType);

            var componentArrays = IntStream.range(1, count + 1)
                    .mapToObj(i -> createComponents(archetype, i, componentFunction.apply(i)))
                    .toArray(Object[][]::new);

            var provider = new ComponentProvider(componentArrays);

            // Call
            var entityIds = createEntities(archetype, provider);

            // Verify
            assertThat(entityIds.getSize()).isEqualTo(count);

            for (int i = 0; i < count; i++) {
                var entityId = entityIds.get(i);

                verifyHasComponents(entityId, componentType);
                verifyArchetypeHasComponents(entityId, componentType);
            }
        }

        @ParameterizedTest
        @MethodSource("countComponentsSupplier")
        @SuppressWarnings("unchecked")
        void testCreateBatch_WithFixedEnums(int count, IntFunction<Object> componentFunction) {
            var componentType = ComponentType.detectComponentType(componentFunction.apply(0));
            var archetype = (A) getArchetype(componentType).with(E1.INSTANCE, E2.INSTANCE);

            var componentArrays = IntStream.range(1, count + 1)
                    .mapToObj(i -> createComponents(archetype, i, componentFunction.apply(i)))
                    .toArray(Object[][]::new);

            var provider = new ComponentProvider(componentArrays);

            // Call
            var entityIds = createEntities(archetype, provider);

            // Verify
            assertThat(entityIds.getSize()).isEqualTo(count);

            for (int i = 0; i < count; i++) {
                var entityId = entityIds.get(i);

                verifyHasComponents(entityId, componentType, component(E1.class), component(E2.class));
                verifyArchetypeHasComponents(entityId, componentType, component(E1.class), component(E2.class));
            }
        }

        @ParameterizedTest
        @MethodSource("countComponentsSupplier")
        void testCreateBatch_InsertedCallbacks(int count, IntFunction<Object> componentFunction) {
            var componentType = ComponentType.detectComponentType(componentFunction.apply(0));
            var archetype = getArchetype(componentType);
            var componentTypes = getComponentTypes(componentType);

            var componentArrays = IntStream.range(1, count + 1)
                    .mapToObj(i -> createComponents(archetype, i, componentFunction.apply(i)))
                    .toArray(Object[][]::new);

            var provider = new ComponentProvider(componentArrays);

            var called = new AtomicBoolean(false);
            eventManager.registerEventHandler(EntitiesInsertedEvent.class, event -> {
                assertThat(event.archetype().getComponentTypes()).containsExactlyInAnyOrder(componentTypes);
                assertThat(event.entityIds().getSize()).isEqualTo(count);

                called.set(true);
            });

            // Verify
            verify(verify -> {
                for (int i = 0; i < count; i++) {
                    verify.expectInserted(componentTypes);
                }

                verify.expectNoMoreInserted();
                verify.expectNoMoreUpdated();
                verify.expectNoMoreRemoved();

                // Call
                createEntities(archetype, provider);
            });

            assertThat(called.get()).isTrue();
        }

        @ParameterizedTest
        @MethodSource("countComponentsSupplier")
        @SuppressWarnings("unchecked")
        void testCreateBatch_InsertedCallbacks_WithFixedEnums(int count, IntFunction<Object> componentFunction) {
            var componentType = ComponentType.detectComponentType(componentFunction.apply(0));
            var archetype = (A) getArchetype(componentType).with(E1.INSTANCE, E2.INSTANCE);

            var baseComponentTypes = getComponentTypes(componentType);
            var componentTypes = Arrays.copyOf(baseComponentTypes, baseComponentTypes.length + 2);
            componentTypes[baseComponentTypes.length] = component(E1.class);
            componentTypes[baseComponentTypes.length + 1] = component(E2.class);

            var componentArrays = IntStream.range(1, count + 1)
                    .mapToObj(i -> createComponents(archetype, i, componentFunction.apply(i)))
                    .toArray(Object[][]::new);

            var provider = new ComponentProvider(componentArrays);

            var called = new AtomicBoolean(false);
            eventManager.registerEventHandler(EntitiesInsertedEvent.class, event -> {
                assertThat(event.archetype().getComponentTypes()).containsExactlyInAnyOrder(componentTypes);
                assertThat(event.entityIds().getSize()).isEqualTo(count);

                called.set(true);
            });

            // Verify
            verify(verify -> {
                for (int i = 0; i < count; i++) {
                    verify.expectInserted(componentTypes);
                }

                verify.expectNoMoreInserted();
                verify.expectNoMoreUpdated();
                verify.expectNoMoreRemoved();

                // Call
                createEntities(archetype, provider);
            });

            assertThat(called.get()).isTrue();
        }

        @Test
        void testCreateBatch_FactoryNotCalled() {
            var archetype = getArchetype(component(C1.class));

            assertThatThrownBy(() -> dontCallFactory(2, archetype))
                    .isInstanceOf(StorageEngineException.class)
                    .hasMessageContainingAll(C1.class.getSimpleName(), "index 0", "was 'null'");
        }

        @Test
        void testBatchCreate_NullInstance() {
            var archetype = getArchetype(component(C1.class));

            var provider = new ComponentProvider(createComponents(archetype, 1, null));

            // Call
            assertThatThrownBy(() -> createEntities(archetype, provider))
                    .isInstanceOf(StorageEngineException.class)
                    .hasMessageContainingAll(C1.class.getSimpleName(), "index 0", "was 'null'");
        }

        @Test
        void testCreateBatch_TypeMismatch() {
            var archetype = getArchetype(component(C1.class));

            var provider = new ComponentProvider(createComponents(archetype, 1, new C2()));

            // Call
            assertThatThrownBy(() -> createEntities(archetype, provider))
                    .isInstanceOf(StorageEngineException.class)
                    .message()
                    .containsSubsequence(
                            "Expected", C1.class.getSimpleName(), "index 0",
                            " was ", C2.class.getSimpleName());
        }

        @Test
        void testGetInstance() {
            var archetype = getArchetype(component(C1.class));

            assertThat(archetype.getInstance(D1.class)).isNotNull();
            assertThat(archetype.getInstance(D2.class)).isNotNull();
            assertThat(archetype.getInstance(D3.class)).isNotNull();
        }

        static Stream<Arguments> countComponentsSupplier() {
            return IntStream.of(10, 15, 42, 512)
                    .mapToObj(i -> components()
                            .map(args -> Arguments.argumentSet("%d: %s".formatted(i, args.apply(0)), i, args)))
                    .flatMap(Function.identity());
        }

        static Stream<Named<?>> componentsSupplier() {
            return components()
                    .map(supplier -> Named.of(supplier.apply(0).toString(), supplier));
        }

        static Stream<IntFunction<Object>> components() {
            return Stream.of(
                    i -> new C1(i),

                    i -> Relation.create(new C1(i), new C1(i)),
                    i -> Relations.create(new C1(i), new C1(i)),
                    i -> Relations.of(Relation.create(new C1(i), new C1(i)), Relation.create(new C1(i * 10), new C1(i * 10))),
                    i -> Relation.create(E1.INSTANCE, new C1(i)),

                    i -> Relation.create(new C1(i), i),
                    i -> Relations.create(new C1(i), i),
                    i -> Relations.of(Relation.create(new C1(i), i), Relation.create(new C1(i * 10), i * 10)),
                    i -> Relation.create(E1.INSTANCE, i));
        }

    }

    private class ComponentProvider implements ObjIntConsumer<Object[]> {

        private final List<Object[]> list;

        private ComponentProvider(Object[]... components) {
            this.list = Arrays.asList(components);
        }

        @Override
        public void accept(Object[] components, int idx) {
            var data = list.get(idx);

            for (int i = 0, s = components.length; i < s; i++) {
                components[i] = data[i];
            }
        }
    }

    public record C1(int value) {
        public C1() {
            this(0);
        }
    }

    public record C2(int value) {
        public C2() {
            this(0);
        }
    }

    public record C3(int value) {
        public C3() {
            this(0);
        }
    }

    public record C4(int value) {
        public C4() {
            this(0);
        }
    }

    public record C5(int value) {
        public C5() {
            this(0);
        }
    }

    public record C6(int value) {
        public C6() {
            this(0);
        }
    }

    public record C7(int value) {
        public C7() {
            this(0);
        }
    }

    public record C8(int value) {
        public C8() {
            this(0);
        }
    }

    public record D1(int value) implements Pooled {
        public D1() {
            this(0);
        }
    }

    public record D2(int value) implements Pooled {
        public D2() {
            this(0);
        }
    }

    public record D3(int value) implements Pooled {
        public D3() {
            this(0);
        }
    }

    public record D4(int value) implements Pooled {
        public D4() {
            this(0);
        }
    }

    public record D5(int value) implements Pooled {
        public D5() {
            this(0);
        }
    }

    public record D6(int value) implements Pooled {
        public D6() {
            this(0);
        }
    }

    public record D7(int value) implements Pooled {
        public D7() {
            this(0);
        }
    }

    public enum E1 {
        INSTANCE
    }

    public enum E2 {
        INSTANCE
    }

    record Target(int value) {
    }

    record Target2(int value) {
    }

}
