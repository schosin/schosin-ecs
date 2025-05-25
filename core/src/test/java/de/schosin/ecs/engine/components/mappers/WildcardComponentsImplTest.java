package de.schosin.ecs.engine.components.mappers;

import static de.schosin.ecs.api.components.types.ComponentType.WILDCARD;
import static de.schosin.ecs.api.components.types.ComponentType.wildcard;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class WildcardComponentsImplTest extends AbstractMapperTest {

    @Test
    void testHas() {
        var components = world.getComponents(WILDCARD);

        var component1 = new Component1();
        var component2 = new Component2("foo");
        var pooled = new PooledComponent();

        var entityId = world.createEntity(component1, component2, pooled, EnumComponent.FIRST);

        // Call
        assertThat(components.has(entityId)).isTrue();
    }

    @Test
    void testHas_ComponentsKnownBeforehand() {
        var component1 = new Component1();
        var component2 = new Component2("foo");
        var pooled = new PooledComponent();

        var entityId = world.createEntity(component1, component2, pooled, EnumComponent.FIRST);

        var components = world.getComponents(WILDCARD);

        // Call
        assertThat(components.has(entityId)).isTrue();
    }

    @Test
    void testRemove() {
        var components = world.getComponents(WILDCARD);

        var component1 = new Component1();
        var component2 = new Component2("foo");
        var pooled = new PooledComponent();

        var entityId = world.createEntity(component1, component2, pooled, EnumComponent.FIRST);
        verifyHasComponents(entityId, Component1.class, Component2.class, PooledComponent.class, EnumComponent.class);
        verifyComponentMaskHasComponents(entityId, Component1.class, Component2.class, PooledComponent.class, EnumComponent.class);

        // Call
        assertThat(components.remove(entityId)).isTrue();

        world.process();

        // Verify
        verifyDoesNotHaveComponents(entityId, Component1.class, Component2.class, PooledComponent.class, EnumComponent.class);
        verifyComponentMaskDoesNotHaveComponents(entityId, Component1.class, Component2.class, PooledComponent.class, EnumComponent.class);
    }

    @Test
    void testRemove_WhenWorldNotProcessed_DoesNotAlterYet() {
        var components = world.getComponents(WILDCARD);

        var component1 = new Component1();
        var component2 = new Component2("foo");
        var pooled = new PooledComponent();

        var entityId = world.createEntity(component1, component2, pooled, EnumComponent.FIRST);
        verifyHasComponents(entityId, Component1.class, Component2.class, PooledComponent.class, EnumComponent.class);
        verifyComponentMaskHasComponents(entityId, Component1.class, Component2.class, PooledComponent.class, EnumComponent.class);

        // Call
        assertThat(components.remove(entityId)).isTrue();

        // Verify
        verifyHasComponents(entityId, Component1.class, Component2.class, PooledComponent.class, EnumComponent.class);
        verifyComponentMaskHasComponents(entityId, Component1.class, Component2.class, PooledComponent.class, EnumComponent.class);
    }

    @Test
    void testRemove_ComponentsKnownBeforehand() {
        var component1 = new Component1();
        var component2 = new Component2("foo");
        var pooled = new PooledComponent();

        var entityId = world.createEntity(component1, component2, pooled, EnumComponent.FIRST);
        verifyHasComponents(entityId, Component1.class, Component2.class, PooledComponent.class, EnumComponent.class);
        verifyComponentMaskHasComponents(entityId, Component1.class, Component2.class, PooledComponent.class, EnumComponent.class);

        var components = world.getComponents(WILDCARD);

        // Call
        assertThat(components.remove(entityId)).isTrue();

        world.process();

        // Verify
        verifyDoesNotHaveComponents(entityId, Component1.class, Component2.class, PooledComponent.class, EnumComponent.class);
        verifyComponentMaskDoesNotHaveComponents(entityId, Component1.class, Component2.class, PooledComponent.class, EnumComponent.class);
    }

    @Test
    void testRemove_RegularOnly() {
        var components = world.getComponents(wildcard(Regular.class));

        var component1 = new Component1();
        var component2 = new Component2("foo");
        var pooled = new PooledComponent();

        var entityId = world.createEntity(component1, component2, pooled, EnumComponent.FIRST);
        verifyHasComponents(entityId, Component1.class, Component2.class, PooledComponent.class, EnumComponent.class);
        verifyComponentMaskHasComponents(entityId, Component1.class, Component2.class, PooledComponent.class, EnumComponent.class);

        // Call
        assertThat(components.remove(entityId)).isTrue();

        world.process();

        // Verify
        verifyHasComponents(entityId, PooledComponent.class, EnumComponent.class);
        verifyDoesNotHaveComponents(entityId, Component1.class, Component2.class);

        verifyComponentMaskHasComponents(entityId, PooledComponent.class, EnumComponent.class);
        verifyComponentMaskDoesNotHaveComponents(entityId, Component1.class, Component2.class);
    }

    @Test
    void testRemove_RegularOnly_ComponentsKnownBeforehand() {
        var component1 = new Component1();
        var component2 = new Component2("foo");
        var pooled = new PooledComponent();

        var entityId = world.createEntity(component1, component2, pooled, EnumComponent.FIRST);
        verifyHasComponents(entityId, Component1.class, Component2.class, PooledComponent.class, EnumComponent.class);
        verifyComponentMaskHasComponents(entityId, Component1.class, Component2.class, PooledComponent.class, EnumComponent.class);

        var components = world.getComponents(wildcard(Regular.class));

        // Call
        assertThat(components.remove(entityId)).isTrue();

        world.process();

        // Verify
        verifyHasComponents(entityId, PooledComponent.class, EnumComponent.class);
        verifyDoesNotHaveComponents(entityId, Component1.class, Component2.class);

        verifyComponentMaskHasComponents(entityId, PooledComponent.class, EnumComponent.class);
        verifyComponentMaskDoesNotHaveComponents(entityId, Component1.class, Component2.class);
    }

    @Nested
    class GetTest {

        @Test
        void testSize() {
            var components = world.getComponents(WILDCARD);

            var component1 = new Component1();
            var component2 = new Component2("foo");
            var pooled = new PooledComponent();

            var entityId = world.createEntity(component1, component2, pooled, EnumComponent.FIRST);

            // Call
            var result = components.get(entityId);

            // Verify
            assertThat(result.size()).as("size").isEqualTo(4);
        }

        @Test
        void testSize_InterfaceWildcard() {
            var components = world.getComponents(wildcard(Regular.class));

            var component1 = new Component1();
            var component2 = new Component2("foo");
            var pooled = new PooledComponent();

            var entityId = world.createEntity(component1, component2, pooled, EnumComponent.FIRST);

            // Call
            var result = components.get(entityId);

            // Verify
            assertThat(result.size()).as("size").isEqualTo(2);
        }

        @Test
        void testIsEmpty() {
            var components = world.getComponents(WILDCARD);

            var component1 = new Component1();
            var component2 = new Component2("foo");
            var pooled = new PooledComponent();

            var entity1 = world.createEntity(component1, component2, pooled, EnumComponent.FIRST);
            var entity2 = world.createEntity();

            // Call
            var result1 = components.get(entity1);
            var result2 = components.get(entity2);

            // Verify
            assertThat(result1.isEmpty()).as("isEmpty").isFalse();
            assertThat(result2.isEmpty()).as("isEmpty").isTrue();
        }

        @Test
        void testGetByIndex() {
            var components = world.getComponents(WILDCARD);

            var component1 = new Component1();
            var component2 = new Component2("foo");
            var pooled = new PooledComponent();

            var entityId = world.createEntity(component1, component2, pooled, EnumComponent.FIRST);

            // Call
            var result = components.get(entityId);

            // Verify
            assertThat(result.get(0)).as("get(0)").isIn(component1, component2, pooled, EnumComponent.FIRST);
            assertThat(result.get(1)).as("get(1)").isIn(component1, component2, pooled, EnumComponent.FIRST);
            assertThat(result.get(2)).as("get(2)").isIn(component1, component2, pooled, EnumComponent.FIRST);
            assertThat(result.get(3)).as("get(3)").isIn(component1, component2, pooled, EnumComponent.FIRST);

            assertThatThrownBy(() -> result.get(4)).isInstanceOf(ArrayIndexOutOfBoundsException.class);
        }

        @Test
        void testEnhancedForLoop() {
            var components = world.getComponents(WILDCARD);

            var component1 = new Component1();
            var component2 = new Component2("foo");
            var pooled = new PooledComponent();

            var entityId = world.createEntity(component1, component2, pooled, EnumComponent.FIRST);

            // Call
            var result = components.get(entityId);

            // Verify
            for (var component : result) {
                assertThat(component).as("enhanced for loop").isIn(component1, component2, pooled, EnumComponent.FIRST);
            }
        }

        @Test
        void testEnhancedForLoop_InterfaceWildcard() {
            var components = world.getComponents(wildcard(Regular.class));

            var component1 = new Component1();
            var component2 = new Component2("foo");
            var pooled = new PooledComponent();

            var entityId = world.createEntity(component1, component2, pooled, EnumComponent.FIRST);

            // Call
            var result = components.get(entityId);

            // Verify
            for (var component : result) {
                assertThat(component).as("enhanced for loop").isIn(component1, component2);
            }
        }

        @Test
        void testIterator() {
            var components = world.getComponents(WILDCARD);

            var component1 = new Component1();
            var component2 = new Component2("foo");
            var pooled = new PooledComponent();

            var entityId = world.createEntity(component1, component2, pooled, EnumComponent.FIRST);

            // Call
            var result = components.get(entityId);

            // Verify
            for (var iter = result.iterator(); iter.hasNext();) {
                assertThat(iter.next()).as("iterator loop").isIn(component1, component2, pooled, EnumComponent.FIRST);
            }
            for (var iter = result.iterator(); iter.hasNext();) {
                assertThat(iter.next()).as("iterator loop resets automatically").isIn(component1, component2, pooled, EnumComponent.FIRST);
            }
        }

        @Test
        void testIterator_InterfaceWildcard() {
            var components = world.getComponents(wildcard(Regular.class));

            var component1 = new Component1();
            var component2 = new Component2("foo");
            var pooled = new PooledComponent();

            var entityId = world.createEntity(component1, component2, pooled, EnumComponent.FIRST);

            // Call
            var result = components.get(entityId);

            // Verify
            for (var iter = result.iterator(); iter.hasNext();) {
                assertThat(iter.next()).as("iterator loop").isIn(component1, component2);
            }
            for (var iter = result.iterator(); iter.hasNext();) {
                assertThat(iter.next()).as("iterator loop resets automatically").isIn(component1, component2);
            }
        }

        @Test
        void testGetByClass() {
            var components = world.getComponents(WILDCARD);

            var component1 = new Component1();
            var component2 = new Component2("foo");
            var pooled = new PooledComponent();

            var entityId = world.createEntity(component1, component2, pooled, EnumComponent.FIRST);

            // Call
            var result = components.get(entityId);

            // Verify
            assertThat(result.get(Component1.class)).as("get(Component1.class)").isSameAs(component1);
            assertThat(result.get(Component2.class)).as("get(Component2.class)").isSameAs(component2);
            assertThat(result.get(PooledComponent.class)).as("get(PooledComponent.class)").isSameAs(pooled);
            assertThat(result.get(EnumComponent.class)).as("get(EnumComponent.class)").isSameAs(EnumComponent.FIRST);
        }

        @Test
        void testGetByClass_InterfaceWildcard() {
            world.getComponents(Component3.class); // register type so that Components detects component

            var components = world.getComponents(wildcard(Regular.class));

            var component1 = new Component1();
            var component2 = new Component2("foo");
            var pooled = new PooledComponent();

            var entityId = world.createEntity(component1, component2, pooled, EnumComponent.FIRST);

            // Call
            var result = components.get(entityId);

            // Verify
            assertThat(result.get(Component1.class)).as("get(Component1.class)").isSameAs(component1);
            assertThat(result.get(Component2.class)).as("get(Component2.class)").isSameAs(component2);
            assertThat(result.get(Component3.class)).as("get(Component3.class)").isNull();
        }

        @Test
        void testResultReused_WhenWorldProcessedInBetween() {
            var components = world.getComponents(wildcard(Regular.class));

            var entityId = world.createEntity();

            // Call
            var result1 = components.get(entityId);

            world.process();

            // Verify
            var result2 = components.get(entityId);
            assertThat((Object) result2).isSameAs(result1);
        }

        @Test
        void testDifferentResult() {
            var components = world.getComponents(wildcard(Regular.class));

            var entityId = world.createEntity();

            // Call
            var result1 = components.get(entityId);
            var result2 = components.get(entityId);

            // Verify
            assertThat((Object) result2).isNotSameAs(result1);
        }

    }

}
