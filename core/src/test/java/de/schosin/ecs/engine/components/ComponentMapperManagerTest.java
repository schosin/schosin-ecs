package de.schosin.ecs.engine.components;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import de.schosin.ecs.api.components.mappers.Components;
import de.schosin.ecs.api.components.mappers.CustomComponents;
import de.schosin.ecs.api.components.types.CustomComponentType;
import de.schosin.ecs.engine.AbstractWorldTest;

class ComponentMapperManagerTest extends AbstractWorldTest {

    @Nested
    class DefaultComponentTypeTest {

        @Test
        void testEntityDoesNotHaveComponent() {
            componentMapperManager.registerCustomComponentType(DefaultComponentType.class, this::factory2);

            var defaultInstance = new Component1(-1);
            var type = new DefaultComponentType<>(component(Component1.class), defaultInstance);

            var mapper = world.getComponents(type);

            var entityId = world.createEntity();

            // Verify
            assertThat(mapper.has(entityId)).isFalse();
            assertThat(mapper.get(entityId)).isSameAs(defaultInstance);
            assertThat(mapper.remove(entityId)).isFalse();
        }

        @Test
        void testEntityHasComponent() {
            componentMapperManager.registerCustomComponentType(DefaultComponentType.class, this::factory2);

            var defaultInstance = new Component1(-1);
            var type = new DefaultComponentType<>(component(Component1.class), defaultInstance);

            var mapper = world.getComponents(type);

            var instance = new Component1(1);
            var entityId = world.createEntity(instance);

            // Verify
            assertThat(mapper.has(entityId)).isTrue();
            assertThat(mapper.get(entityId)).isSameAs(instance);
            assertThat(mapper.remove(entityId)).isTrue();
        }

        private <T> DefaultComponents<T> factory2(DefaultComponentType<T> type) {
            var components = componentMapperManager.getComponents(type.type());
            return new DefaultComponents<>(components, type.defaultInstance());
        }

        record Component1(int value) {
        }

    }

}

record DefaultComponentType<T>(RegularComponentType<T, T> type, T defaultInstance) implements CustomComponentType<T, T, DefaultComponents<T>> {
}

class DefaultComponents<T> implements CustomComponents<T, T> {

    private final Components<T, T> components;
    private final T defaultInstance;

    public DefaultComponents(Components<T, T> components, T defaultInstance) {
        this.components = components;
        this.defaultInstance = Objects.requireNonNull(defaultInstance, "defaultInstance cannot be null");
    }

    @Override
    public boolean has(int entityId) {
        return components.has(entityId);
    }

    @Override
    public T get(int entityId) {
        var result = components.get(entityId);

        return result != null ? result : defaultInstance;
    }

    @Override
    public boolean remove(int entityId) {
        return components.remove(entityId);
    }

}
