package de.schosin.ecs.engine.components.mappers;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class EnumComponentMapperImplTest extends AbstractMapperTest {

    @Test
    void testHasComponent() {
        var entity1 = world.createEntity(EnumComponent.FIRST);
        var entity2 = world.createEntity();

        // Call
        assertThat(firstEnum.has(entity1)).isTrue();
        assertThat(secondEnum.has(entity1)).isTrue();

        assertThat(firstEnum.has(entity2)).isFalse();
        assertThat(secondEnum.has(entity2)).isFalse();
    }

    @Test
    void testGetComponent() {
        var entity1 = world.createEntity(EnumComponent.FIRST);
        var entity2 = world.createEntity();

        // Call
        assertThat(firstEnum.get(entity1)).isSameAs(EnumComponent.FIRST);
        assertThat(secondEnum.get(entity1)).isSameAs(EnumComponent.FIRST);

        assertThat(firstEnum.get(entity2)).isNull();
        assertThat(secondEnum.get(entity2)).isNull();
    }

    @Test
    void testGetDefault() {
        assertThat(firstEnum.getDefault()).isSameAs(EnumComponent.FIRST);
        assertThat(secondEnum.getDefault()).isSameAs(EnumComponent.SECOND);
    }

    @Test
    void testAdd() {
        var entity1 = world.createEntity();
        var entity2 = world.createEntity();

        verify(verify -> {
            verify.expectUpdated(entity1, EnumComponent.class);
            verify.expectUpdated(entity2, EnumComponent.class);
            verify.expectNoMoreUpdated();

            // Call
            assertThat(firstEnum.add(entity1, EnumComponent.FIRST)).isSameAs(EnumComponent.FIRST);
            assertThat(secondEnum.add(entity2, EnumComponent.FIRST)).isSameAs(EnumComponent.FIRST);

            world.process();

            // Verify
            assertThat(firstEnum.get(entity1)).isSameAs(EnumComponent.FIRST);
            assertThat(firstEnum.get(entity2)).isSameAs(EnumComponent.FIRST);

            assertThat(secondEnum.get(entity1)).isSameAs(EnumComponent.FIRST);
            assertThat(secondEnum.get(entity2)).isSameAs(EnumComponent.FIRST);

            verifyHasComponents(entity1, EnumComponent.class);
            verifyComponentMaskHasComponents(entity1, EnumComponent.class);

            verifyHasComponents(entity2, EnumComponent.class);
            verifyComponentMaskHasComponents(entity2, EnumComponent.class);
        });
    }

    @Test
    void testAddDefaultValue() {
        var entity1 = world.createEntity();
        var entity2 = world.createEntity();

        verify(verify -> {
            verify.expectUpdated(entity1, EnumComponent.class);
            verify.expectUpdated(entity2, EnumComponent.class);
            verify.expectNoMoreUpdated();

            // Call
            assertThat(firstEnum.add(entity1)).isSameAs(EnumComponent.FIRST);
            assertThat(secondEnum.add(entity2)).isSameAs(EnumComponent.SECOND);

            world.process();

            // Verify
            assertThat(firstEnum.get(entity1)).isSameAs(EnumComponent.FIRST);
            assertThat(firstEnum.get(entity2)).isSameAs(EnumComponent.SECOND);

            assertThat(secondEnum.get(entity1)).isSameAs(EnumComponent.FIRST);
            assertThat(secondEnum.get(entity2)).isSameAs(EnumComponent.SECOND);

            verifyHasComponents(entity1, EnumComponent.class);
            verifyComponentMaskHasComponents(entity1, EnumComponent.class);

            verifyHasComponents(entity2, EnumComponent.class);
            verifyComponentMaskHasComponents(entity2, EnumComponent.class);
        });
    }

    @Test
    void testRemoveComponent() {
        var entity1 = world.createEntity(EnumComponent.FIRST);
        var entity2 = world.createEntity(EnumComponent.FIRST);
        var entity3 = world.createEntity(EnumComponent.SECOND);
        var entity4 = world.createEntity(EnumComponent.SECOND);

        verify(verify -> {
            verify.expectUpdated(entity1, NO_COMPONENTS);
            verify.expectUpdated(entity2, NO_COMPONENTS);
            verify.expectUpdated(entity3, NO_COMPONENTS);
            verify.expectUpdated(entity4, NO_COMPONENTS);
            verify.expectNoMoreUpdated();

            // Call
            assertThat(firstEnum.remove(entity1)).isTrue();
            assertThat(firstEnum.remove(entity2)).isTrue();

            assertThat(secondEnum.remove(entity3)).isTrue();
            assertThat(secondEnum.remove(entity4)).isTrue();

            world.process();

            // Verify
            assertThat(firstEnum.get(entity1)).isNull();
            assertThat(firstEnum.get(entity2)).isNull();
            assertThat(firstEnum.get(entity3)).isNull();
            assertThat(firstEnum.get(entity4)).isNull();

            verifyDoesNotHaveComponents(entity1, EnumComponent.class);
            verifyComponentMaskDoesNotHaveComponents(entity1, EnumComponent.class);

            verifyDoesNotHaveComponents(entity2, EnumComponent.class);
            verifyComponentMaskDoesNotHaveComponents(entity2, EnumComponent.class);

            verifyDoesNotHaveComponents(entity3, EnumComponent.class);
            verifyComponentMaskDoesNotHaveComponents(entity3, EnumComponent.class);

            verifyDoesNotHaveComponents(entity4, EnumComponent.class);
            verifyComponentMaskDoesNotHaveComponents(entity4, EnumComponent.class);
        });
    }

}
