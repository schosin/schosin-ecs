package de.schosin.ecs.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class IdManagerTest extends AbstractWorldTest {

    @Test
    void testSharedIdSpace() {
        var entityId = idManager.createEntityId();
        var componentId = idManager.createComponentId();

        assertThat(entityId.id()).isNotEqualTo(componentId.id());
    }

    @Nested
    class EntityIdTest {

        @Test
        void testEntityId() {
            var id = idManager.createEntityId();

            // Verify
            assertThat(id).isNotNull();
            assertThat(id.id()).isNotNegative();
            assertThat(id.flags()).isZero();
        }

        @ParameterizedTest
        @ValueSource(ints = { 1, 512, 2048 })
        void testEntityBagCapacity(int count) {
            var bag = bagManager.createEntityBag(String.class);

            var id = -1;
            for (int i = 0; i < count; i++) {
                id = idManager.createEntityId().id();
            }

            var lastId = id;

            assertThat(bag.getCapacity()).isGreaterThan(lastId);
            assertThatCode(() -> bag.get(lastId)).doesNotThrowAnyException();
        }

        @ParameterizedTest
        @ValueSource(ints = { 1, 512, 2048 })
        void testEntityIntBagCapacity(int count) {
            var bag = bagManager.createEntityIntBag();

            var id = -1;
            for (int i = 0; i < count; i++) {
                id = idManager.createEntityId().id();
            }

            var lastId = id;

            assertThat(bag.getCapacity()).isGreaterThan(lastId);
            assertThatCode(() -> bag.get(lastId)).doesNotThrowAnyException();
        }

    }

    @Nested
    class ComponentIdTest {

        @Test
        void testComponentId() {
            var id = idManager.createComponentId();

            // Verify
            assertThat(id).isNotNull();
            assertThat(id.id()).isNotNegative();
            assertThat(id.flags()).isZero();
        }

        @ParameterizedTest
        @ValueSource(ints = { 1, 512, 2048 })
        void testComponentBagCapacity(int count) {
            var bag = bagManager.createComponentBag(String.class);

            var id = -1;
            for (int i = 0; i < count; i++) {
                id = idManager.createComponentId().id();
            }

            var lastId = id;

            assertThat(bag.getCapacity()).isGreaterThan(lastId);
            assertThatCode(() -> bag.get(lastId)).doesNotThrowAnyException();
        }

        @ParameterizedTest
        @ValueSource(ints = { 1, 512, 2048 })
        void testComponentIntBagCapacity(int count) {
            var bag = bagManager.createComponentIntBag();

            var id = -1;
            for (int i = 0; i < count; i++) {
                id = idManager.createComponentId().id();
            }

            var lastId = id;

            assertThat(bag.getCapacity()).isGreaterThan(lastId);
            assertThatCode(() -> bag.get(lastId)).doesNotThrowAnyException();
        }

    }

}
