package de.schosin.ecs.plugins.data;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import de.schosin.ecs.api.entities.Entity;
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

    @Nested
    class ArchetypeEntityBagTest {

        @Test
        void testIteration() {
            var type = DataType.get(component(Component1.class), component(Component2.class));

            var entity1 = world.createEntity();
            var entity2 = world.createEntity(new Component1(2));
            var entity3 = world.createEntity(new Component2(3));
            var entity4 = world.createEntity(new Component1(4), new Component2(4));

            var entities = world.getAllEntities().forType(type);

            assertThat(entities.isEmpty()).isFalse();
            assertThat(entities.size()).isEqualTo(4);
            assertThat(entities.contains(entity1)).isTrue();
            assertThat(entities.contains(entity2)).isTrue();
            assertThat(entities.contains(entity3)).isTrue();
            assertThat(entities.contains(entity4)).isTrue();

            assertThat(entities).hasSize(4);
            assertThat(entities).extracting(Entity::id).containsExactlyInAnyOrder(entity1, entity2, entity3, entity4);

            var processed = new ArrayList<Integer>();
            entities.process((id, c1, c2) -> processed.add(id));

            assertThat(processed).containsExactlyInAnyOrder(entity1, entity2, entity3, entity4);
        }

        @Test
        void testComponents() {
            var type = DataType.get(component(Component1.class), component(Component2.class));

            var component1 = new Component1(42);
            var component2 = new Component2(9001);
            var entityId = world.createEntity(component1, component2);

            var entities = world.getAllEntities().forType(type);

            var processed = new ArrayList<Integer>();
            entities.process((id, c1, c2) -> {
                assertThat(id).isSameAs(entityId);
                assertThat(c1).isSameAs(component1);
                assertThat(c2).isSameAs(component2);

                processed.add(id);
            });

            assertThat(processed).containsExactly(entityId);
        }

    }

    record Component1(int value) {
    }

    record Component2(int value) {
    }

    record Component3(int value) {
    }

}
