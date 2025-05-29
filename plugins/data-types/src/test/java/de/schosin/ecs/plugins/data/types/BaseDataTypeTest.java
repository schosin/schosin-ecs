package de.schosin.ecs.plugins.data.types;

import static de.schosin.ecs.api.components.types.ComponentType.component;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class BaseDataTypeTest {

    @Nested
    class DataType2Test {

        @Test
        void testGetComponentTypes() {
            var type1 = component(Component1.class);
            var type2 = component(Component2.class);

            var type = DataType.get(type1, type2);
            assertThat(type.getComponentTypes()).containsExactly(type1, type2);
        }

    }

    @Nested
    class DataType3Test {

        @Test
        void testGetComponentTypes() {
            var type1 = component(Component1.class);
            var type2 = component(Component2.class);
            var type3 = component(Component3.class);

            var type = DataType.get(type1, type2, type3);
            assertThat(type.getComponentTypes()).containsExactly(type1, type2, type3);
        }

    }

    record Component1() {
    }

    record Component2() {
    }

    record Component3() {
    }

}
