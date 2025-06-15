package de.schosin.ecs.engine.components;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;
import java.util.function.Supplier;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import de.schosin.ecs.api.components.mappers.CustomComponents;
import de.schosin.ecs.api.components.types.CustomComponentType;
import de.schosin.ecs.api.data.DataAccessor;
import de.schosin.ecs.engine.AbstractWorldTest;

class ComponentMapperManagerTest extends AbstractWorldTest {

    @Nested
    class DefaultComponentTypeTest {

        @Test
        void testEntityDoesNotHaveComponent() {
            componentMapperManager.registerCustomComponentType(DefaultComponentType.class, this::factory);

            var type = new DefaultComponentType<>(component(Component1.class), () -> new Component1(-1));
            var mapper = world.getComponents(type);

            var entityId = world.createEntity();

            // Verify
            assertThat(mapper.has(entityId)).isFalse();
            assertThat(mapper.get(entityId)).isEqualTo(new Component1(-1));
            assertThat(mapper.remove(entityId)).isFalse();
        }

        @Test
        void testEntityHasComponent() {
            componentMapperManager.registerCustomComponentType(DefaultComponentType.class, this::factory);

            var type = new DefaultComponentType<>(component(Component1.class), () -> new Component1(-1));
            var mapper = world.getComponents(type);

            var instance = new Component1(1);
            var entityId = world.createEntity(instance);

            // Verify
            assertThat(mapper.has(entityId)).isTrue();
            assertThat(mapper.get(entityId)).isEqualTo(new Component1(1));
            assertThat(mapper.remove(entityId)).isTrue();
        }

        @Test
        void testAccessor() {
            componentMapperManager.registerCustomComponentType(DefaultComponentType.class, this::factory);

            var type = new DefaultComponentType<>(component(Component1.class), () -> new Component1(-1));
            var mapper = world.getComponents(type);

            var instance = new Component1(1);
            var entityId = world.createEntity(instance);

            // Verify
            var accessor = entityManager.getAccessor(entityId);
            assertThat(mapper.get(accessor)).isSameAs(instance);
        }

        private <T> DefaultComponents<T> factory(DefaultComponentType<T> type) {
            var components = componentMapperManager.getComponents(type.type());
            return new DefaultComponents<>(components, type.defaultInstance());
        }

        record Component1(int value) {
        }

    }

}

record DefaultComponentType<T>(RegularComponentType<T, T> type, Supplier<T> defaultInstance) implements CustomComponentType<T, T, DefaultComponents<T>> {
}

class DefaultComponents<T> implements CustomComponents<T, T> {

    private final RegularComponents<T, T> components;
    private final int componentId;

    private final Supplier<T> defaultInstance;

    public DefaultComponents(RegularComponents<T, T> components, Supplier<T> defaultInstance) {
        this.components = components;
        this.componentId = components.componentId();

        this.defaultInstance = Objects.requireNonNull(defaultInstance, "defaultInstance cannot be null");
    }

    @Override
    public boolean has(int entityId) {
        return components.has(entityId);
    }

    @Override
    public T get(int entityId) {
        var result = components.get(entityId);

        return result != null ? result : defaultInstance.get();
    }

    @Override
    public T get(DataAccessor accessor) {
        var result = accessor.<T>getComponent(componentId);

        return result != null ? result : defaultInstance.get();
    }

    @Override
    public boolean remove(int entityId) {
        return components.remove(entityId);
    }

}
