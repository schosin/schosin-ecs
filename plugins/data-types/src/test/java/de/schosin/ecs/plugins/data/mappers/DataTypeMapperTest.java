package de.schosin.ecs.plugins.data.mappers;

import static org.assertj.core.api.Assertions.assertThat;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import de.schosin.ecs.plugins.data.DataTypeWorld;
import de.schosin.ecs.plugins.data.types.DataType;
import de.schosin.ecs.test.AbstractEcsTest;

class DataTypeMapperTest extends AbstractEcsTest<DataTypeWorld> {

    @Nested
    class DataType2Test {

        @Test
        void testHas() {
            var type = DataType.get(component(Component1.class), component(Component2.class));
            var mapper = world.getComponents(type);

            var entity1 = world.createEntity();
            var entity2 = world.createEntity(new Component1(42));
            var entity3 = world.createEntity(new Component2(9001));
            var entity4 = world.createEntity(new Component1(42), new Component2(9001));

            assertThat(mapper.has(entity1)).isFalse();
            assertThat(mapper.has(entity2)).isTrue();
            assertThat(mapper.has(entity3)).isTrue();
            assertThat(mapper.has(entity4)).isTrue();
        }

        @Test
        void testHasAll() {
            var type = DataType.get(component(Component1.class), component(Component2.class));
            var mapper = world.getComponents(type);

            var entity1 = world.createEntity();
            var entity2 = world.createEntity(new Component1(42));
            var entity3 = world.createEntity(new Component2(9001));
            var entity4 = world.createEntity(new Component1(42), new Component2(9001));

            assertThat(mapper.hasAll(entity1)).isFalse();
            assertThat(mapper.hasAll(entity2)).isFalse();
            assertThat(mapper.hasAll(entity3)).isFalse();
            assertThat(mapper.hasAll(entity4)).isTrue();
        }

        @Test
        void testGet() {
            var type = DataType.get(component(Component1.class), component(Component2.class));
            var mapper = world.getComponents(type);

            var entity1 = world.createEntity();
            var entity2 = world.createEntity(new Component1(42));
            var entity3 = world.createEntity(new Component2(9001));
            var entity4 = world.createEntity(new Component1(42), new Component2(9001));

            assertThat(mapper.get(entity1)).isNotNull(); // "collection" types always return an instance
            assertThat(mapper.get(entity2)).isNotNull();
            assertThat(mapper.get(entity3)).isNotNull();
            assertThat(mapper.get(entity4)).isNotNull();
        }

        @Test
        void testGet_AllComponents() {
            var type = DataType.get(component(Component1.class), component(Component2.class));
            var mapper = world.getComponents(type);

            var component1 = new Component1(42);
            var component2 = new Component2(9001);
            var entityId = world.createEntity(component1, component2);

            var result = mapper.get(entityId);
            assertThat(result).isNotNull();
            assertThat(result.component1()).isSameAs(component1);
            assertThat(result.component2()).isSameAs(component2);
        }

        @Test
        void testReclaim() {
            var type = DataType.get(component(Component1.class), component(Component2.class));
            var mapper = world.getComponents(type);

            var component1 = new Component1(42);
            var component2 = new Component2(9001);
            var entityId = world.createEntity(component1, component2);

            var entity2 = world.createEntity(new Component2(2), new Component1(1));

            var result = mapper.get(entityId);
            assertThat(result).isNotNull();
            assertThat(result.component1()).isSameAs(component1);
            assertThat(result.component2()).isSameAs(component2);

            // Call
            mapper.reclaim();

            // Verify
            assertThat(result).extracting(Object::toString, InstanceOfAssertFactories.STRING).contains("invalidated");

            assertThat(mapper.get(entity2)).isSameAs(result);
            assertThat(result.component1()).isNotSameAs(component1).extracting("value").isEqualTo(1);
            assertThat(result.component2()).isNotSameAs(component2).extracting("value").isEqualTo(2);
        }

        @Test
        void testReclaim_WorldProcess() {
            var type = DataType.get(component(Component1.class), component(Component2.class));
            var mapper = world.getComponents(type);

            var component1 = new Component1(42);
            var component2 = new Component2(9001);
            var entityId = world.createEntity(component1, component2);

            var entity2 = world.createEntity(new Component2(2), new Component1(1));

            var result = mapper.get(entityId);
            assertThat(result).isNotNull();
            assertThat(result.component1()).isSameAs(component1);
            assertThat(result.component2()).isSameAs(component2);

            // Call
            world.process();

            // Verify
            assertThat(result).extracting(Object::toString, InstanceOfAssertFactories.STRING).contains("invalidated");

            assertThat(mapper.get(entity2)).isSameAs(result);
            assertThat(result.component1()).isNotSameAs(component1).extracting("value").isEqualTo(1);
            assertThat(result.component2()).isNotSameAs(component2).extracting("value").isEqualTo(2);
        }

        @Test
        void testRemove() {
            var type = DataType.get(component(Component1.class), component(Component2.class));
            var mapper = world.getComponents(type);

            var entity1 = world.createEntity(new Component3(1));
            var entity2 = world.createEntity(new Component1(42));
            var entity3 = world.createEntity(new Component2(9001));
            var entity4 = world.createEntity(new Component1(42), new Component2(9001));

            verify(verify -> {
                verify.expectUpdated(entity2, NO_COMPONENTS);
                verify.expectUpdated(entity3, NO_COMPONENTS);
                verify.expectUpdated(entity4, NO_COMPONENTS);
                verify.expectNoMoreUpdated();

                // Call
                assertThat(mapper.remove(entity1)).isFalse();
                assertThat(mapper.remove(entity2)).isTrue();
                assertThat(mapper.remove(entity3)).isTrue();
                assertThat(mapper.remove(entity4)).isTrue();

                world.process();

                // Verify
                verifyDoesNotHaveComponents(entity1, Component1.class, Component2.class);
                verifyArchetypeDoesNotHaveComponents(entity1, Component1.class, Component2.class);

                verifyDoesNotHaveComponents(entity2, Component1.class, Component2.class);
                verifyArchetypeDoesNotHaveComponents(entity2, Component1.class, Component2.class);

                verifyDoesNotHaveComponents(entity3, Component1.class, Component2.class);
                verifyArchetypeDoesNotHaveComponents(entity3, Component1.class, Component2.class);

                verifyDoesNotHaveComponents(entity4, Component1.class, Component2.class);
                verifyArchetypeDoesNotHaveComponents(entity4, Component1.class, Component2.class);
            });
        }

    }

    @Nested
    class DataType3Test {

        @Test
        void testGet() {
            var type = DataType.get(component(Component1.class), component(Component2.class), component(Component3.class));
            var mapper = world.getComponents(type);

            var component1 = new Component1(42);
            var component2 = new Component2(9001);
            var component3 = new Component3(9002);
            var entityId = world.createEntity(component1, component2, component3);

            var result = mapper.get(entityId);
            assertThat(result).isNotNull();
            assertThat(result.component1()).isSameAs(component1);
            assertThat(result.component2()).isSameAs(component2);
            assertThat(result.component3()).isSameAs(component3);
        }

    }

    record Component1(int value) {
    }

    record Component2(int value) {
    }

    record Component3(int value) {
    }

}
