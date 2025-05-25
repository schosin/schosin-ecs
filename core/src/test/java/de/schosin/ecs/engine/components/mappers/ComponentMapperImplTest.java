package de.schosin.ecs.engine.components.mappers;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ComponentMapperImplTest extends AbstractMapperTest {

    @Test
    void testHasComponent() {
        var entity1 = world.createEntity(new Component1());
        var entity2 = world.createEntity(new Component2("data"));
        var entity3 = world.createEntity(EnumComponent.FIRST);

        // Call
        assertThat(component1.has(entity1)).isTrue();
        assertThat(component2.has(entity1)).isFalse();

        assertThat(component1.has(entity2)).isFalse();
        assertThat(component2.has(entity2)).isTrue();

        assertThat(component1.has(entity3)).isFalse();
        assertThat(component2.has(entity3)).isFalse();
    }

    @Test
    void testGetComponent() {
        var component = new Component1();
        var entity1 = world.createEntity(component);

        var otherComponent = new Component2("data");
        var entity2 = world.createEntity(otherComponent);

        // Call
        assertThat(component1.get(entity1)).isEqualTo(component);
        assertThat(component2.get(entity1)).isNull();

        assertThat(component1.get(entity2)).isNull();
        assertThat(component2.get(entity2)).isEqualTo(otherComponent);
    }

    @Test
    void testAdd() {
        var entityId = world.createEntity();

        var instance1 = new Component1();
        var instance2 = new Component2("foo");

        verify(verify -> {
            verify.expectUpdated(entityId, Component1.class, Component2.class);
            verify.expectNoMoreUpdated();

            // Call
            assertThat(component1.add(entityId, instance1)).isSameAs(instance1);
            assertThat(component2.add(entityId, instance2)).isSameAs(instance2);

            world.process();

            // Verify
            assertThat(component1.get(entityId)).isSameAs(instance1);
            assertThat(component2.get(entityId)).isSameAs(instance2);

            verifyHasComponents(entityId, Component1.class, Component2.class);
            verifyComponentMaskHasComponents(entityId, Component1.class, Component2.class);
        });
    }

    @Test
    void testRemoveComponent() {
        var entityId = world.createEntity(new Component1(), new Component2("data"));

        verify(verify -> {
            verify.expectUpdated(entityId, NO_COMPONENTS);
            verify.expectNoMoreUpdated();

            // Call
            assertThat(component1.remove(entityId)).isTrue();
            assertThat(component2.remove(entityId)).isTrue();

            world.process();

            // Verify
            assertThat(component1.get(entityId)).isNull();
            assertThat(component2.get(entityId)).isNull();

            verifyDoesNotHaveComponents(entityId, Component1.class, Component2.class);
            verifyComponentMaskDoesNotHaveComponents(entityId, Component1.class, Component2.class);
        });
    }

}
