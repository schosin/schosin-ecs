package de.schosin.ecs.plugins.data;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import de.schosin.ecs.plugins.data.types.DataType;
import de.schosin.ecs.test.AbstractEcsTest;

class DataTypeManagerTest extends AbstractEcsTest<DataTypeWorld> {

    DataTypeManager dataTypeManager;

    @BeforeEach
    void setupManager() {
        this.dataTypeManager = world.getSingleton(DataTypeManager.class);
    }

    @Nested
    class GetDataTest {

        @Test
        void testDataType2() {
            var type = DataType.get(component(Component1.class), component(Component2.class));

            var component1 = new Component1(42);
            var component2 = new Component2(9001);
            var entityId = world.createEntity(component1, component2);

            // Call
            var function = dataTypeManager.getData(type);
            assertThat(function).isNotNull();

            // Verify
            var result = function.apply(entityId);
            assertThat(result).isNotNull();
            assertThat(result.component1()).isSameAs(component1);
            assertThat(result.component2()).isSameAs(component2);
        }

    }

    record Component1(int value) {
    }

    record Component2(int value) {
    }

    record Component3(int value) {
    }

}
