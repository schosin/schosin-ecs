package de.schosin.ecs.plugins.wildcards.mappers;

import static de.schosin.ecs.plugins.wildcards.types.WildcardType.WILDCARD;
import static de.schosin.ecs.plugins.wildcards.types.WildcardType.wildcard;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Modifier;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import de.schosin.ecs.engine.components.ComponentMapperManager.ReclaimingComponents;

class WildcardClassMapperTest extends AbstractMapperTest {

    @Test
    void testHas() {
        var components = world.getComponents(WILDCARD);

        var entityId = world.createEntity(new OtherComponent());

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
    void testHas_AddComponentToEntityAfterwards() {
        var components = world.getComponents(WILDCARD);

        var entityId = world.createEntity();
        assertThat(components.has(entityId)).isFalse();

        // Call
        world.getComponents(OtherComponent.class).add(entityId, new OtherComponent());
        assertThat(components.has(entityId)).isTrue(); // pending component 

        world.process();
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

    @Nested
    class ReclaimTest {

        @Test
        void testReclaim() {
            var component1 = new Component1();
            var component2 = new Component2("foo");
            var pooled = new PooledComponent();

            var entityId = world.createEntity(component1, component2, pooled, EnumComponent.FIRST);

            var mapper = world.getComponents(WILDCARD);

            // Call
            var result = mapper.get(entityId);
            assertThat(result).asInstanceOf(InstanceOfAssertFactories.ITERABLE).containsExactlyInAnyOrder(component1, component2, pooled, EnumComponent.FIRST);
            assertThat(result.toString()).contains(component1.toString(), component2.toString(), pooled.toString(), EnumComponent.FIRST.toString());

            var reclaimingMapper = assertThat(mapper).asInstanceOf(InstanceOfAssertFactories.type(ReclaimingComponents.class)).actual();
            reclaimingMapper.reclaim();

            // Verify
            assertThat(result.toString()).contains("invalidated");
            assertThat(mapper.get(entityId)).isSameAs(result);
        }

        @Test
        void testReclaim_WorldProcess() {
            var component1 = new Component1();
            var component2 = new Component2("foo");
            var pooled = new PooledComponent();

            var entityId = world.createEntity(component1, component2, pooled, EnumComponent.FIRST);

            var mapper = world.getComponents(WILDCARD);

            // Call
            var result = mapper.get(entityId);
            assertThat(result).asInstanceOf(InstanceOfAssertFactories.ITERABLE).containsExactlyInAnyOrder(component1, component2, pooled, EnumComponent.FIRST);
            assertThat(result.toString()).contains(component1.toString(), component2.toString(), pooled.toString(), EnumComponent.FIRST.toString());

            world.process();

            // Verify
            assertThat(result.toString()).contains("invalidated");
            assertThat(mapper.get(entityId)).isSameAs(result);
        }

    }

    @Nested
    static class TransmutationTest extends AbstractMapperTest {

        @Test
        void ensureTestClassIsStatic() {
            // must be static and extend AbstractWorldTest so that each test has a fresh, empty world not altered by @BeforeEach of outer class
            assertThat(Modifier.isStatic(this.getClass().getModifiers())).as("test class is static").isTrue();
        }

        @Test
        void testInterfaceWildcard() {
            var remove12 = transmutationManager.getRemoveTransmuter(wildcard(C12.class));
            var remove123 = transmutationManager.getRemoveTransmuter(wildcard(C123.class));
            var remove23 = transmutationManager.getRemoveTransmuter(wildcard(C23.class));
            var remove = transmutationManager.getRemoveTransmuter(wildcard(C.class));

            var entity1 = world.createEntity(new C1(), new C2(), new C3());
            var entity2 = world.createEntity(new C1(), new C2(), new C3());
            var entity3 = world.createEntity(new C1(), new C2(), new C3());
            var entity4 = world.createEntity(new C1(), new C2(), new C3());

            // Call
            remove12.apply(entity1);
            remove123.apply(entity2);
            remove23.apply(entity3);
            remove.apply(entity4);

            world.process();

            // Verify
            verifyHasComponents(entity1, C3.class);
            verifyDoesNotHaveComponents(entity1, C1.class, C2.class);
            verifyComponentMaskHasComponents(entity1, C3.class);
            verifyComponentMaskDoesNotHaveComponents(entity1, C1.class, C2.class);

            verifyDoesNotHaveComponents(entity2, C1.class, C2.class, C3.class);
            verifyComponentMaskDoesNotHaveComponents(entity2, C1.class, C2.class, C3.class);

            verifyHasComponents(entity3, C1.class);
            verifyDoesNotHaveComponents(entity3, C2.class, C3.class);
            verifyComponentMaskHasComponents(entity3, C1.class);
            verifyComponentMaskDoesNotHaveComponents(entity3, C2.class, C3.class);

            verifyDoesNotHaveComponents(entity4, C1.class, C2.class, C3.class);
            verifyComponentMaskDoesNotHaveComponents(entity4, C1.class, C2.class, C3.class);
        }

        @Test
        void testInterfaceWildcard_ComponentsKnownBeforehand() {
            componentManager.getComponent(component(C1.class));
            componentManager.getComponent(component(C2.class));
            componentManager.getComponent(component(C3.class));

            var remove12 = transmutationManager.getRemoveTransmuter(wildcard(C12.class));
            var remove123 = transmutationManager.getRemoveTransmuter(wildcard(C123.class));
            var remove23 = transmutationManager.getRemoveTransmuter(wildcard(C23.class));
            var remove = transmutationManager.getRemoveTransmuter(wildcard(C.class));

            var entity1 = world.createEntity(new C1(), new C2(), new C3());
            var entity2 = world.createEntity(new C1(), new C2(), new C3());
            var entity3 = world.createEntity(new C1(), new C2(), new C3());
            var entity4 = world.createEntity(new C1(), new C2(), new C3());

            // Call
            remove12.apply(entity1);
            remove123.apply(entity2);
            remove23.apply(entity3);
            remove.apply(entity4);

            world.process();

            // Verify
            verifyHasComponents(entity1, C3.class);
            verifyDoesNotHaveComponents(entity1, C1.class, C2.class);
            verifyComponentMaskHasComponents(entity1, C3.class);
            verifyComponentMaskDoesNotHaveComponents(entity1, C1.class, C2.class);

            verifyDoesNotHaveComponents(entity2, C1.class, C2.class, C3.class);
            verifyComponentMaskDoesNotHaveComponents(entity2, C1.class, C2.class, C3.class);

            verifyHasComponents(entity3, C1.class);
            verifyDoesNotHaveComponents(entity3, C2.class, C3.class);
            verifyComponentMaskHasComponents(entity3, C1.class);
            verifyComponentMaskDoesNotHaveComponents(entity3, C2.class, C3.class);

            verifyDoesNotHaveComponents(entity4, C1.class, C2.class, C3.class);
            verifyComponentMaskDoesNotHaveComponents(entity4, C1.class, C2.class, C3.class);
        }

        @Test
        void testWildcard() {
            var removeConstant = transmutationManager.getRemoveTransmuter(WILDCARD);
            var removeObject = transmutationManager.getRemoveTransmuter(wildcard(Object.class));

            var entity1 = world.createEntity(new C1(), new C2(), new C3());
            var entity2 = world.createEntity(new C1(), new C2(), new C3());

            // Call
            removeConstant.apply(entity1);
            removeObject.apply(entity2);

            world.process();

            // Verify
            verifyDoesNotHaveComponents(entity1, C1.class, C2.class, C3.class);
            verifyComponentMaskDoesNotHaveComponents(entity1, C1.class, C2.class, C3.class);

            verifyDoesNotHaveComponents(entity2, C1.class, C2.class, C3.class);
            verifyComponentMaskDoesNotHaveComponents(entity2, C1.class, C2.class, C3.class);
        }

        @Test
        void testWildcard_ComponentsKnownBeforehand() {
            componentManager.getComponent(component(C1.class));
            componentManager.getComponent(component(C2.class));
            componentManager.getComponent(component(C3.class));

            var removeConstant = transmutationManager.getRemoveTransmuter(WILDCARD);
            var removeObject = transmutationManager.getRemoveTransmuter(wildcard(Object.class));

            var entity1 = world.createEntity(new C1(), new C2(), new C3());
            var entity2 = world.createEntity(new C1(), new C2(), new C3());

            // Call
            removeConstant.apply(entity1);
            removeObject.apply(entity2);

            world.process();

            // Verify
            verifyDoesNotHaveComponents(entity1, C1.class, C2.class, C3.class);
            verifyComponentMaskDoesNotHaveComponents(entity1, C1.class, C2.class, C3.class);

            verifyDoesNotHaveComponents(entity2, C1.class, C2.class, C3.class);
            verifyComponentMaskDoesNotHaveComponents(entity2, C1.class, C2.class, C3.class);
        }

        interface C {
        }

        interface C12 extends C {
        }

        interface C123 extends C {
        }

        interface C23 extends C {
        }

        public record C1() implements C12, C123 {
        }

        public record C2() implements C12, C123, C23 {
        }

        public record C3() implements C123, C23 {
        }

    }

    record OtherComponent() {
    }

}
