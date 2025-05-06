package de.schosin.ecs.engine;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

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

    }

}
