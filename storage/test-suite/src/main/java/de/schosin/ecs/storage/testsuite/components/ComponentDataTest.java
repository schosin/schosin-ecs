package de.schosin.ecs.storage.testsuite.components;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import de.schosin.ecs.api.components.types.ComponentType.ClassType;
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
        return (ComponentData<TT>) engine.getComponent(classType, NO_OP);
    }

    record C1() {
    }

    record C2() {
    }

    record C3() {
    }

}
