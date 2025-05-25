package de.schosin.ecs.engine.components.mappers;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import de.schosin.ecs.api.World;
import de.schosin.ecs.api.components.mappers.ComponentMapper.PooledComponentMapper;
import de.schosin.ecs.engine.EngineWorld;

class PooledComponentMapperImplTest extends AbstractMapperTest {

    @Test
    void testHasComponent() {
        var entity1 = world.createEntity(new Component1());
        var entity2 = world.createEntity(new PooledComponent());
        var entity3 = world.createEntity(EnumComponent.FIRST);

        // Call
        assertThat(pooledComponent.has(entity1)).isFalse();
        assertThat(pooledComponent.has(entity2)).isTrue();
        assertThat(pooledComponent.has(entity3)).isFalse();
    }

    @Test
    void testGetComponent() {
        var component = new PooledComponent();
        var entityId = world.createEntity(component);

        // Call
        assertThat(pooledComponent.get(entityId)).isSameAs(component);
    }

    @Test
    void testAdd() {
        var entityId = world.createEntity();

        var component = new PooledComponent();
        component.data = "bar";

        verify(verify -> {
            verify.expectUpdated(entityId, PooledComponent.class);
            verify.expectNoMoreUpdated();

            // Call
            assertThat(pooledComponent.add(entityId, component)).isSameAs(component);

            world.process();

            // Verify
            assertThat(pooledComponent.get(entityId)).isSameAs(component);

            verifyHasComponents(entityId, PooledComponent.class);
            verifyComponentMaskHasComponents(entityId, PooledComponent.class);
        });
    }

    @Test
    void testAdd_PooledInstance() {
        var entityId = world.createEntity();

        verify(verify -> {
            verify.expectUpdated(entityId, PooledComponent.class);
            verify.expectNoMoreUpdated();

            // Call
            assertThat(pooledComponent.add(entityId)).isNotNull();

            world.process();

            // Verify
            verifyHasComponents(entityId, PooledComponent.class);
            verifyComponentMaskHasComponents(entityId, PooledComponent.class);
        });
    }

    @Test
    void testAdd_WhenAlreadyContained_ReturnsExisting() {
        var pooled = new PooledComponent();
        var entityId = world.createEntity(pooled);

        // Call
        assertThat(pooledComponent.add(entityId)).isSameAs(pooled);
    }

    @Test
    void testRemoveComponent() {
        var entity1 = world.createEntity(new PooledComponent());
        var entity2 = world.createEntity();

        verify(verify -> {
            verify.expectUpdated(entity1, NO_COMPONENTS);
            verify.expectNoMoreUpdated();

            // Call
            assertThat(pooledComponent.remove(entity1)).isTrue();
            assertThat(pooledComponent.remove(entity2)).isFalse();

            world.process();

            // Verify
            verifyDoesNotHaveComponents(entity1, PooledComponent.class);
            verifyComponentMaskDoesNotHaveComponents(entity1, PooledComponent.class);
        });
    }

    @Test
    void testComponentsDetectsPooled() {
        var world = (EngineWorld) World.builder().build(); // code coverage requires new world due to caching

        var components = world.getComponents(PooledComponent.class);
        assertThat(components).isInstanceOf(PooledComponentMapper.class);

        var pooledComponents = world.getPooledComponents(PooledComponent.class);
        assertThat(pooledComponents).isSameAs(components);
    }

    @Test
    void testPooledIntanceReused() {
        var entityId = world.createEntity();
        verifyDoesNotHaveComponents(entityId, PooledComponent.class);
        verifyComponentMaskDoesNotHaveComponents(entityId, PooledComponent.class);

        // Add
        var instance1 = pooledComponent.add(entityId);
        assertThat(instance1).isNotNull();

        world.process();
        verifyHasComponents(entityId, PooledComponent.class);
        verifyComponentMaskHasComponents(entityId, PooledComponent.class);

        // Remove
        pooledComponent.remove(entityId);

        world.process();
        verifyDoesNotHaveComponents(entityId, PooledComponent.class);
        verifyComponentMaskDoesNotHaveComponents(entityId, PooledComponent.class);

        // Reuse
        var reusedInstance = pooledComponent.add(entityId);
        assertThat(reusedInstance).isSameAs(instance1);

        world.process();
        verifyHasComponents(entityId, PooledComponent.class);
        verifyComponentMaskHasComponents(entityId, PooledComponent.class);
    }

}
