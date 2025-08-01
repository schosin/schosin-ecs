package de.schosin.ecs.storage.testsuite.components;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import de.schosin.ecs.api.components.types.ClassType;
import de.schosin.ecs.storage.api.components.Component.ComponentData;
import de.schosin.ecs.storage.testsuite.components.ComponentDataTest.C1;
import de.schosin.ecs.storage.testsuite.components.ComponentDataTest.C2;
import de.schosin.ecs.storage.testsuite.components.ComponentDataTest.C3;

public class ComponentDataTest extends CommonClassTypeTest<C1, C2, C3> {

    @Nested
    class ClazzTest extends AbstractTypeTest {

        @ParameterizedTest
        @MethodSource(TYPES)
        void testClazzMatchesClassType(ClassType<?> type) {
            assertThat(getComponent(type).clazz()).as("component.clazz() is same as argument clazz").isSameAs(type.clazz());
        }

        @Nested
        class ZeroSizedComponents {

            @Test
            void testZeroSizedComponents() {
                var entityId = world.createEntity(ZeroSizedTest.INSTANCE);

                var archetype = engine.getArchetype(component(ZeroSizedTest.class));
                var component = engine.getComponent(component(ZeroSizedTest.class));

                assertThat(engine.getArchetypeForEntity(entityId)).as("getArchetypeForEntity returns expected archetype with zero-sized components").isSameAs(archetype);
                assertThat(component.getComponent(entityId)).as("Component.getComponent returns component instance").isSameAs(ZeroSizedTest.INSTANCE);
            }

            @Test
            void testIterableAccessor() {
                var entityId = world.createEntity(ZeroSizedTest.INSTANCE);

                var archetype = engine.getArchetypeForEntity(entityId);

                var componentId = engine.getComponent(component(ZeroSizedTest.class)).id();
                var componentIndex = archetype.getComponentIndex(componentId);

                var accessor = archetype.getAccessor();
                assertThat(accessor.hasNext()).as("IterableAccessor returns has entity with zero-sized component").isTrue();
                assertThat(accessor.next()).as("IterableAccessor returns has entity with zero-sized component").isEqualTo(entityId);
                assertThat(accessor.<ZeroSizedTest>getComponent(componentId)).as("IterableAccessor returns component instance by componentId").isSameAs(ZeroSizedTest.INSTANCE);
                assertThat(accessor.<ZeroSizedTest>getComponentByIndex(componentIndex)).as("IterableAccessor returns component instance by index").isSameAs(ZeroSizedTest.INSTANCE);

            }

            @Test
            void testDataAccessor() {
                var entityId = world.createEntity(ZeroSizedTest.INSTANCE);

                var archetype = engine.getArchetypeForEntity(entityId);

                var componentId = engine.getComponent(component(ZeroSizedTest.class)).id();
                var componentIndex = archetype.getComponentIndex(componentId);

                var accessor = engine.getAccessor(entityId);
                assertThat(accessor.<ZeroSizedTest>getComponent(componentId)).as("DataAccessor returns component instance by componentId").isSameAs(ZeroSizedTest.INSTANCE);
                assertThat(accessor.<ZeroSizedTest>getComponentByIndex(componentIndex)).as("DataAccessor returns component instance by index").isSameAs(ZeroSizedTest.INSTANCE);
            }

        }

    }

    @Override
    protected C1 getInstance1() {
        return new C1();
    }

    @Override
    protected C2 getInstance2() {
        return new C2();
    }

    @Override
    protected C3 getInstance3() {
        return new C3();
    }

    protected <TT> ComponentData<TT> getComponent(ClassType<TT> classType) {
        return (ComponentData<TT>) engine.getComponent(classType);
    }

    record C1() {
    }

    record C2() {
    }

    record C3() {
    }

    enum ZeroSizedTest {
        INSTANCE
    }

}
