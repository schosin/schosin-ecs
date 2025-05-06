package de.schosin.ecs.engine.components;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import de.schosin.ecs.engine.AbstractWorldTest;
import de.schosin.ecs.engine.utils.collections.Bag;
import de.schosin.ecs.engine.utils.collections.BitVector;

class ComponentDataTest extends AbstractWorldTest {

    @Nested
    class ComponentDataImplTest {

        @Test
        void testAdd() {
            var removals = new BitVector();
            var data = new ComponentDataImpl<>(idManager.createComponentId(), C1.class, new Bag<>(C1.class, 64), removals, null);

            assertThat(data.hasComponent(42)).isFalse();
            assertThat(data.getComponent(42)).isNull();

            // Call
            var component = new C1();
            data.addComponent(42, component);

            // Verify
            assertThat(data.hasComponent(42)).isTrue();
            assertThat(data.getComponent(42)).isSameAs(component);
        }

        @Test
        void testAdd_ClearDoesNotRemovedMark() {
            var removals = new BitVector();
            var component = new C1();

            var data = new ComponentDataImpl<>(idManager.createComponentId(), C1.class, new Bag<>(C1.class, 64), removals, null);
            data.addComponent(42, component);
            data.markRemoved(42);

            assertThat(data.hasComponent(42)).isTrue();
            assertThat(data.getComponent(42)).isSameAs(component);
            assertThat(removals.get(42)).isTrue();

            // Call
            var component2 = new C1();
            data.addComponent(42, component2);

            // Verify
            assertThat(data.hasComponent(42)).isTrue();
            assertThat(data.getComponent(42)).isSameAs(component2);
            assertThat(removals.get(42)).isTrue();
        }

        @Test
        void testMarkRemoved() {
            var removals = new BitVector();
            var component = new C1();

            var data = new ComponentDataImpl<>(idManager.createComponentId(), C1.class, new Bag<>(C1.class, 64), removals, null);
            data.addComponent(42, component);

            assertThat(data.hasComponent(42)).isTrue();
            assertThat(data.getComponent(42)).isSameAs(component);
            assertThat(removals.get(42)).isFalse();

            // Call
            data.markRemoved(42);

            // Verify
            assertThat(data.hasComponent(42)).isTrue();
            assertThat(data.getComponent(42)).isSameAs(component);
            assertThat(removals.get(42)).isTrue();
        }

        @Test
        void testUnmarkRemoved() {
            var removals = new BitVector();
            var component = new C1();

            var data = new ComponentDataImpl<>(idManager.createComponentId(), C1.class, new Bag<>(C1.class, 64), removals, null);
            data.addComponent(42, component);
            data.markRemoved(42);

            assertThat(data.hasComponent(42)).isTrue();
            assertThat(data.getComponent(42)).isSameAs(component);
            assertThat(removals.get(42)).isTrue();

            // Call
            data.unmarkRemoved(42);

            // Verify
            assertThat(data.hasComponent(42)).isTrue();
            assertThat(data.getComponent(42)).isSameAs(component);
            assertThat(removals.get(42)).isFalse();
        }

        @Test
        void testApplyRemovals() {
            var removals = new BitVector();
            var component1 = new C1();
            var component42 = new C1();

            var data = new ComponentDataImpl<>(idManager.createComponentId(), C1.class, new Bag<>(C1.class, 64), removals, null);
            data.addComponent(1, component1);
            data.addComponent(42, component42);

            assertThat(data.hasComponent(1)).isTrue();
            assertThat(data.getComponent(1)).isSameAs(component1);
            assertThat(data.hasComponent(42)).isTrue();
            assertThat(data.getComponent(42)).isSameAs(component42);

            // Remove 1
            data.markRemoved(1);
            assertThat(removals.isEmpty()).isFalse();
            assertThat(removals.get(1)).isTrue();
            assertThat(removals.get(42)).isFalse();

            data.applyRemovals();
            assertThat(removals.isEmpty()).isTrue();

            // Verify
            assertThat(data.hasComponent(1)).isFalse();
            assertThat(data.getComponent(1)).isNull();
            assertThat(data.hasComponent(42)).isTrue();
            assertThat(data.getComponent(42)).isSameAs(component42);
        }

        @Test
        void testApplyMultipleRemovals() {
            var removals = new BitVector();
            var component1 = new C1();
            var component2 = new C1();
            var component42 = new C1();

            var data = new ComponentDataImpl<>(idManager.createComponentId(), C1.class, new Bag<>(C1.class, 64), removals, null);
            data.addComponent(1, component1);
            data.addComponent(2, component2);
            data.addComponent(42, component42);

            assertThat(data.hasComponent(1)).isTrue();
            assertThat(data.getComponent(1)).isSameAs(component1);
            assertThat(data.hasComponent(2)).isTrue();
            assertThat(data.getComponent(2)).isSameAs(component2);
            assertThat(data.hasComponent(42)).isTrue();
            assertThat(data.getComponent(42)).isSameAs(component42);

            // Remove 1
            data.markRemoved(1);
            data.markRemoved(42);
            assertThat(removals.isEmpty()).isFalse();
            assertThat(removals.get(1)).isTrue();
            assertThat(removals.get(2)).isFalse();
            assertThat(removals.get(42)).isTrue();

            data.applyRemovals();
            assertThat(removals.isEmpty()).isTrue();

            // Verify
            assertThat(data.hasComponent(1)).isFalse();
            assertThat(data.getComponent(1)).isNull();
            assertThat(data.hasComponent(2)).isTrue();
            assertThat(data.getComponent(2)).isSameAs(component2);
            assertThat(data.hasComponent(42)).isFalse();
            assertThat(data.getComponent(42)).isNull();
        }

        @Test
        void testApplyNoRemovals() {
            var removals = new BitVector();
            var component1 = new C1();
            var component42 = new C1();

            var data = new ComponentDataImpl<>(idManager.createComponentId(), C1.class, new Bag<>(C1.class, 64), removals, null);
            data.addComponent(1, component1);
            data.addComponent(42, component42);

            assertThat(data.hasComponent(1)).isTrue();
            assertThat(data.getComponent(1)).isSameAs(component1);
            assertThat(data.hasComponent(42)).isTrue();
            assertThat(data.getComponent(42)).isSameAs(component42);

            // Remove 1
            data.applyRemovals();

            // Verify
            assertThat(data.hasComponent(1)).isTrue();
            assertThat(data.getComponent(1)).isSameAs(component1);
            assertThat(data.hasComponent(42)).isTrue();
            assertThat(data.getComponent(42)).isSameAs(component42);
        }

    }

    private record C1() {
    }

}
