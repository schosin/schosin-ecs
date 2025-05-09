package de.schosin.ecs.plugins.state;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import de.schosin.ecs.api.Pooled;
import de.schosin.ecs.api.World;
import de.schosin.ecs.plugins.state.State.PooledState;

class StateManagerTest {

    StateWorld world;

    @BeforeEach
    void setupWorld() {
        this.world = World.builder(StateWorld.class).build();
    }

    @Test
    void testGetState_WhenPooledClass_ReturnsPooledImplementation() {
        var state = world.getState(PooledTestState.class);
        assertThat(state).isInstanceOf(PooledState.class);
    }

    @Test
    void testGetState_WhenClassUsedAsComponent_Throws() {
        world.getComponents(StateTest.class);

        assertThatThrownBy(() -> world.getState(StateTest.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already used as a component");
    }

    @Test
    void testGetState_WhenClassUsedAsPooledComponent_Throws() {
        world.getPooledComponents(PooledTestState.class);

        assertThatThrownBy(() -> world.getState(PooledTestState.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already used as a component");
    }

    @Test
    void testGetPooledState_WhenClassUsedAsComponent_Throws() {
        world.getPooledComponents(PooledTestState.class);

        assertThatThrownBy(() -> world.getPooledState(PooledTestState.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already used as a component");
    }

    @Nested
    class StateTest {

        @Test
        void testGetState_ReturnsSameInstance() {
            var test = world.getState(TestState.class);

            assertThat(world.getState(TestState.class)).isSameAs(test);
        }

        @Test
        void testGet_WhenNotSet_ReturnsNull() {
            var test = world.getState(TestState.class);

            // Verify
            assertThat(test.get(42)).isNull();
        }

        @Test
        void testGet_WhenSet_ReturnsInstance() {
            var state = new TestState();

            var test = world.getState(TestState.class);
            test.add(42, state);

            // Verify
            assertThat(test.get(42)).isSameAs(state);
        }

        @Test
        void testAdd_WhenAlreadyAssigned_Throws() {
            var state = new TestState().init(42);
            var other = new TestState().init(9001);

            var test = world.getState(TestState.class);
            test.add(42, state);

            // Verify
            assertThatThrownBy(() -> test.add(42, other))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContainingAll("Cannot override existing state ", "42");
        }

        @Test
        void testRemove_WhenNotAssigned_DoesNothing() {
            var test = world.getState(TestState.class);

            assertThatCode(() -> test.remove(42)).doesNotThrowAnyException();
        }

        @Test
        void testRemove_WhenAssigned_Removes() {
            var state = new TestState().init(42);

            var test = world.getState(TestState.class);
            test.add(42, state);

            // Call
            test.remove(42);

            // Verify
            assertThat(test.get(42)).isNull();
        }

        @Test
        void testAdd_WhenEntityDeleted_RemovesState() {
            var state = new TestState().init(42);

            var entityId = world.createEntity();

            var test = world.getState(TestState.class);
            test.add(entityId, state);

            // Call
            world.deleteEntity(entityId);
            world.process();

            // Verify
            assertThat(test.get(entityId)).isNull();
        }

        @Test
        void testAdd_WhenEntityIdReused_CanBeAssignedAgain() {
            var state = new TestState().init(42);
            var state2 = new TestState().init(42);

            var entityId = world.createEntity();

            var test = world.getState(TestState.class);
            test.add(entityId, state);

            // Call
            world.deleteEntity(entityId);
            world.process();

            assertThat(world.createEntity()).isSameAs(entityId);
            assertThatCode(() -> test.add(entityId, state2)).doesNotThrowAnyException();

            // Verify
            assertThat(test.get(entityId)).isSameAs(state2);
        }

    }

    @Nested
    class PooledStateTest {

        @Test
        void testGetPooledState_ReturnsSameInstance() {
            var test = world.getPooledState(PooledTestState.class);

            assertThat(world.getPooledState(PooledTestState.class)).isSameAs(test);
        }

        @Test
        void testGetState_ReturnsSameInstance() {
            var test = world.getPooledState(PooledTestState.class);

            assertThat(world.getState(PooledTestState.class)).isSameAs(test);
        }

        @Test
        void testGet_WhenNotSet_ReturnsNull() {
            var test = world.getPooledState(PooledTestState.class);

            // Verify
            assertThat(test.get(42)).isNull();
        }

        @Test
        void testGet_WhenSet_ReturnsInstance() {
            var state = new PooledTestState();

            var test = world.getPooledState(PooledTestState.class);
            test.add(42, state);

            // Verify
            assertThat(test.get(42)).isSameAs(state);
        }

        @Test
        void testAdd_WhenAlreadyAssigned_Throws() {
            var state = new PooledTestState().init(42);
            var other = new PooledTestState().init(9001);

            var test = world.getPooledState(PooledTestState.class);
            test.add(42, state);

            // Verify
            assertThatThrownBy(() -> test.add(42, other))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContainingAll("Cannot override existing state ", "42");
        }

        @Test
        void testAddPooled_WhenNotAssigned_CreatesAnInstance() {
            var test = world.getPooledState(PooledTestState.class);

            var state = assertThat(test.add(42)).isNotNull().actual();

            // Verify same instance
            assertThat(test.get(42)).isSameAs(state);
        }

        @Test
        void testAddPooled_WhenAlreadyAssigned_ReturnsAssignedInstance() {
            var state = new PooledTestState().init(42);

            var test = world.getPooledState(PooledTestState.class);
            test.add(42, state);

            // Verify
            assertThat(test.add(42)).isSameAs(state);
        }

        @Test
        void testRemove_WhenNotAssigned_DoesNothing() {
            var test = world.getState(PooledTestState.class);

            assertThatCode(() -> test.remove(42)).doesNotThrowAnyException();
        }

        @Test
        void testRemove_WhenAssigned_Removes() {
            var state = new PooledTestState().init(42);

            var test = world.getPooledState(PooledTestState.class);
            test.add(42, state);

            // Call
            test.remove(42);

            // Verify
            assertThat(test.get(42)).isNull();
        }

        @Test
        void testRemove_WhenAssigned_ReusesInstance() {
            var state = new PooledTestState().init(42);

            var test = world.getPooledState(PooledTestState.class);
            test.add(42, state);

            // Call
            test.remove(42);

            // Verify
            assertThat(test.get(42)).isNull();

            // Add pooled
            assertThat(test.add(99)).isSameAs(state);
        }

    }

    public static class PooledTestState extends TestState implements Pooled {

        public PooledTestState init(int data) {
            return (PooledTestState) super.init(data);
        }

        @Override
        public void reset() {
            this.data = -1;
        }

    }

    public static class TestState {

        public int data = -1;

        public TestState init(int data) {
            this.data = data;
            return this;
        }

        @Override
        public String toString() {
            return "TestState [data=" + data + "]";
        }

    }

}
