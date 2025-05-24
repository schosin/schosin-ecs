package de.schosin.ecs.api.components.types;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.stream.Stream;

import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import de.schosin.ecs.api.components.ComponentSet;
import de.schosin.ecs.api.components.Relation;
import de.schosin.ecs.api.components.Relation.EntityRelationship;
import de.schosin.ecs.api.components.Relation.Exclusive;
import de.schosin.ecs.api.components.Relation.Relationship;

public abstract class AbstractComponentTypeTest {

    protected static final ClassType<EnumComponent> FETCH = ComponentType.component(EnumComponent.class);

    protected static final Class<?> SYNTHETIC_CLASS = ((Runnable) () -> {
    }).getClass();

    abstract class CommonComponentTest {

        protected abstract ComponentType<?, ?> type(Class<?> clazz);

        @ParameterizedTest
        @ValueSource(classes = { GenericComponent.class, ComponentInterface.class, AbstractComponent.class, Object.class, int[].class, Integer[].class, Object[].class })
        void testInvalidType(Class<?> clazz) {
            assertThatThrownBy(() -> type(clazz))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContainingAll(clazz.getName(), "cannot be used as a component");
        }

        @ParameterizedTest
        @MethodSource("de.schosin.ecs.api.components.types.ComponentTypeTest#unsupportedTypes")
        void testUnsupportedClassType(Class<?> clazz) {
            assertThatThrownBy(() -> type(clazz))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContainingAll(clazz.getName(), "cannot be used as a component");
        }

        @Test
        void testInvalidSyntheticClass() {
            assertThatThrownBy(() -> type(SYNTHETIC_CLASS))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContainingAll(SYNTHETIC_CLASS.getName(), "must not be synthetic");
        }

        @Test
        void testComponentSet() {
            assertThatThrownBy(() -> type(MyComponentSet.class))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContainingAll(MyComponentSet.class.getName(), "cannot be used as a component", "must not be component sets");
        }

    }

    static Stream<Arguments> unsupportedTypes() {
        return ComponentTypeHelper.UNSUPPORTED_TYPES.stream()
                .map(clazz -> Arguments.of(Named.of(clazz.getSimpleName(), clazz)));
    }

    enum EnumComponent {
        A, B
    }

    record Component(String value) {
    }

    record GenericComponent<T>(T value) {
    }

    interface ComponentInterface {
    }

    @SuppressWarnings("unused")
    interface GenericComponentInterface<T> {
    }

    abstract class AbstractComponent {
    }

    class NonFinalComponent {
    }

    final class FinalComponent {
    }

    enum RelationshipComponent implements Relationship {
    }

    enum ExclusiveComponent implements Exclusive {
    }

    enum EntityRelationshipComponent implements EntityRelationship {
    }

    enum ExclusiveEntityRelationship implements EntityRelationship, Exclusive {
    }

    enum TargetComponent implements Relation.Target {
    }

    interface MyComponentSet extends ComponentSet {
        Component component();
    }

    class MyComponentSetClass implements MyComponentSet {
        @Override
        public int entityId() {
            return -1;
        }

        @Override
        public Component component() {
            return null;
        }
    }

}
