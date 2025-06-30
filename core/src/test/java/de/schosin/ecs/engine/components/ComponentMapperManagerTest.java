package de.schosin.ecs.engine.components;

import static de.schosin.ecs.api.components.types.ComponentType.wildcard;
import static de.schosin.ecs.api.components.types.ComponentType.wildcardRelation;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import de.schosin.ecs.api.components.ComponentSetConfig;
import de.schosin.ecs.api.components.Relation.Exclusive;
import de.schosin.ecs.api.components.mappers.ComponentMapper;
import de.schosin.ecs.api.components.mappers.ComponentRelationMappers.ComponentRelationMapper;
import de.schosin.ecs.api.components.mappers.ComponentRelationMappers.ExclusiveComponentRelationMapper;
import de.schosin.ecs.api.components.mappers.ComponentSetMapper;
import de.schosin.ecs.api.components.mappers.CustomComponentMapper;
import de.schosin.ecs.api.components.mappers.CustomComponentMapper.FactoryAdapter;
import de.schosin.ecs.api.components.mappers.EntityFetchRelationMappers.EntityRelationFetchMapper;
import de.schosin.ecs.api.components.mappers.EntityFetchRelationMappers.ExclusiveEntityRelationFetchMapper;
import de.schosin.ecs.api.components.mappers.EntityRelationMappers.EntityRelationMapper;
import de.schosin.ecs.api.components.mappers.EntityRelationMappers.ExclusiveEntityRelationMapper;
import de.schosin.ecs.api.components.mappers.WildcardRelationMappers.WildcardComponentRelationMapper;
import de.schosin.ecs.api.components.mappers.WildcardRelationMappers.WildcardEntityFetchRelationMapper;
import de.schosin.ecs.api.components.mappers.WildcardRelationMappers.WildcardEntityRelationMapper;
import de.schosin.ecs.api.components.types.ComponentType;
import de.schosin.ecs.api.components.types.CustomComponentType;
import de.schosin.ecs.api.data.ComponentAccessor;
import de.schosin.ecs.api.data.DataAccessor;
import de.schosin.ecs.engine.AbstractWorldTest;
import de.schosin.ecs.engine.components.ComponentMapperManager.ReclaimingComponents;
import de.schosin.ecs.engine.components.ComponentMapperManager.WildcardMapper;

class ComponentMapperManagerTest extends AbstractWorldTest {

    @ParameterizedTest
    @MethodSource("componentTypes")
    void testComponentMappers(ComponentType<?, ?> componentType, Class<?> expectedMapper) {
        componentMapperManager.registerCustomComponentType(DefaultComponentType.class, this::factory);

        var mapper = world.getComponents(componentType);
        assertThat(mapper).isInstanceOf(expectedMapper);
    }

    @ParameterizedTest
    @MethodSource("componentTypes")
    void testComponentMapperInstancesReused(ComponentType<?, ?> componentType) {
        componentMapperManager.registerCustomComponentType(DefaultComponentType.class, this::factory);

        var mapper = world.getComponents(componentType);
        assertThat(world.getComponents(componentType)).isSameAs(mapper);
    }

    private <T> DefaultComponents<T> factory(DefaultComponentType<T> type) {
        var components = componentMapperManager.getComponents(type.type());
        return new DefaultComponents<>(components, type.defaultInstance());
    }

    static Stream<Arguments> componentTypes() {
        return Stream.of(
                Arguments.arguments(component(Component1.class), ComponentMapper.class),
                Arguments.arguments(relation(Component1.class, Component2.class), ComponentRelationMapper.class),
                Arguments.arguments(exclusiveRelation(Exclusive1.class, Component2.class), ExclusiveComponentRelationMapper.class),
                Arguments.arguments(relation(Component1.class), EntityRelationMapper.class),
                Arguments.arguments(exclusiveRelation(Exclusive1.class), ExclusiveEntityRelationMapper.class),
                Arguments.arguments(relation(Component1.class, component(Component1.class)), EntityRelationFetchMapper.class),
                Arguments.arguments(exclusiveRelation(Exclusive1.class, component(Component1.class)), ExclusiveEntityRelationFetchMapper.class),
                Arguments.arguments(TestSet.TYPE, ComponentSetMapper.class),
                Arguments.arguments(wildcard(Object.class), WildcardMapper.class),
                Arguments.arguments(wildcardRelation(Object.class, Object.class), WildcardComponentRelationMapper.class),
                Arguments.arguments(wildcardRelation(Object.class), WildcardEntityRelationMapper.class),
                Arguments.arguments(wildcardRelation(Object.class, component(Component1.class)), WildcardEntityFetchRelationMapper.class),
                Arguments.arguments(new DefaultComponentType<>(component(Component1.class), Component1::new), DefaultComponents.class));
    }

    @SuppressWarnings("unused")
    @ComponentSetConfig("TestSet")
    static void componentSet(int entityId, Component1 c1) {
    }

    @Nested
    class DefaultComponentTypeTest {

        @Test
        void testComponentMapperInstanceReused() {
            componentMapperManager.registerCustomComponentType(DefaultComponentType.class, this::factory);

            var type = new DefaultComponentType<>(component(Component1.class), () -> new Component1(-1));
            var mapper = world.getComponents(type);

            assertThat(world.getComponents(type)).isSameAs(mapper);
            assertThat(world.getComponents(new DefaultComponentType<>(component(Component1.class), () -> new Component1(-1)))).isNotNull(); // "equal", but not knowable due to the lambda
            assertThat(world.getComponents(new DefaultComponentType<>(component(Component1.class), () -> new Component1(-2)))).isNotSameAs(mapper);
        }

        @Test
        void testNoFactory() {
            var type = new DefaultComponentType<>(component(Component1.class), () -> new Component1(-1));

            assertThatThrownBy(() -> world.getComponents(type))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContainingAll(DefaultComponentType.class.getName(), "No factory registered");
        }

        @Test
        void testMultipleFactories() {
            componentMapperManager.registerCustomComponentType(DefaultComponentType.class, this::factory);

            assertThatThrownBy(() -> componentMapperManager.registerCustomComponentType(DefaultComponentType.class, this::factory))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(DefaultComponentType.class.getName());
        }

        @Test
        @SuppressWarnings("rawtypes")
        void testMultipleFactories_SameInstance() {
            FactoryAdapter<DefaultComponentType, DefaultComponents> factory = this::factory;
            componentMapperManager.registerCustomComponentType(DefaultComponentType.class, factory);

            assertThatCode(() -> componentMapperManager.registerCustomComponentType(DefaultComponentType.class, factory)).doesNotThrowAnyException();
        }

        @Test
        void testReclaimingComponents_Reclaim() {
            componentMapperManager.registerCustomComponentType(DefaultComponentType.class, this::factory);

            var type = new DefaultComponentType<>(component(Component1.class), () -> new Component1(-1));

            var mapper = world.getComponents(type);
            assertThat(mapper.reclaimCalled).isFalse();

            world.process();
            assertThat(mapper.reclaimCalled).isTrue();
        }

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
            assertThat(mapper.getComponent(accessor)).isSameAs(instance);
        }

        @Test
        void testComponentAccessor() {
            componentMapperManager.registerCustomComponentType(DefaultComponentType.class, this::factory);

            var type = new DefaultComponentType<>(component(Component1.class), () -> new Component1(-1));
            var mapper = world.getComponents(type);

            var instance = new Component1(1);
            var entityId = world.createEntity(instance);

            // Verify
            var accessor = entityManager.getAccessor(entityId);
            var componentAccessor = mapper.getComponentAccessor(accessor);
            assertThat(componentAccessor.getComponent(accessor)).isSameAs(instance);
        }

        private <T> DefaultComponents<T> factory(DefaultComponentType<T> type) {
            var components = componentMapperManager.getComponents(type.type());
            return new DefaultComponents<>(components, type.defaultInstance());
        }

    }

    record Component1(int value) {
        Component1() {
            this(0);
        }
    }

    record Component2(int value) {
    }

    enum Exclusive1 implements Exclusive {
        INSTANCE
    }

}

record DefaultComponentType<T>(RegularComponentType<T, T> type, Supplier<T> defaultInstance) implements CustomComponentType<T, T, DefaultComponents<T>> {
}

class DefaultComponents<T> implements CustomComponentMapper<T, T>, ComponentAccessor<T>, ReclaimingComponents {

    private final RegularComponents<T, T> components;
    private final Supplier<T> defaultInstance;

    T freed;
    boolean reclaimCalled;

    public DefaultComponents(RegularComponents<T, T> components, Supplier<T> defaultInstance) {
        this.components = components;
        this.defaultInstance = Objects.requireNonNull(defaultInstance, "defaultInstance cannot be null");
    }

    @Override
    public ComponentAccessor<T> getComponentAccessor(DataAccessor accessor) {
        return this;
    }

    @Override
    public T getComponent(DataAccessor accessor) {
        return get(accessor.entityId());
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
    public boolean remove(int entityId) {
        return components.remove(entityId);
    }

    @Override
    public void free() {
        reclaim();
    }

    @Override
    public void reclaim() {
        reclaimCalled = true;
    }

}
