package de.schosin.ecs.storage.testsuite.components;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import de.schosin.ecs.api.components.ComponentType.ClassType;
import de.schosin.ecs.api.components.ComponentType.RegularComponentType;
import de.schosin.ecs.storage.api.components.Component.ComponentData;

public abstract class CommonClassTypeTest<T1, T2, T3> extends CommonComponentTest<T1, T1, T2, T2, T3, T3> {

    @Override
    @SuppressWarnings("unchecked")
    protected ClassType<T1> type1() {
        return new ClassType<>((Class<T1>) getInstance1().getClass());
    }

    @Override
    @SuppressWarnings("unchecked")
    protected ClassType<T2> type2() {
        return new ClassType<>((Class<T2>) getInstance2().getClass());
    }

    @Override
    @SuppressWarnings("unchecked")
    protected ClassType<T3> type3() {
        return new ClassType<>((Class<T3>) getInstance3().getClass());
    }

    @Override
    @SuppressWarnings("unchecked")
    protected final <T> T getInstance(RegularComponentType<T, ?> type) {
        if (type.equals(type1())) {
            return (T) getInstance1();
        }
        if (type.equals(type2())) {
            return (T) getInstance2();
        }
        if (type.equals(type3())) {
            return (T) getInstance3();
        }

        throw new IllegalArgumentException("Unknown type: " + type);
    }

    protected abstract T1 getInstance1();

    protected abstract T2 getInstance2();

    protected abstract T3 getInstance3();

    @Nested
    class DisplayTest extends AbstractTypeTest {

        @ParameterizedTest
        @MethodSource(TYPES)
        void testDisplayIncludesSimpleName(ClassType<?> type) {
            assertThat(getComponent(type).display()).as("component.display() must contain clazz.getSimpleName()").contains(type.clazz().getSimpleName());
        }

        @ParameterizedTest
        @MethodSource(TYPES)
        void testDisplayIncludesId(ClassType<?> type) {
            var component = getComponent(type);

            assertThat(component.display()).as("component.display() must contain the id").contains(Integer.toString(component.id()));
        }

    }

}

class TestComponent<T> implements ComponentData<T> {

    private final int id;

    TestComponent(int id) {
        this.id = id;
    }

    @Override
    public int id() {
        return id;
    }

    @Override
    public ClassType<T> type() {
        return null;
    }

    @Override
    public String display() {
        return null;
    }

    @Override
    public boolean hasComponent(int entityId) {
        return false;
    }

    @Override
    public T getComponent(int entityId) {
        return null;
    }

    @Override
    public void addComponentUnsafe(int id, T component) {
    }

    @Override
    public void removeComponent(int entityId) {
    }

    @Override
    public Class<T> clazz() {
        return null;
    }

}