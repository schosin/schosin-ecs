package de.schosin.ecs.plugins.experimental.system;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import de.schosin.ecs.api.World;
import de.schosin.ecs.plugins.experimental.system.systems.BaseSystem;
import de.schosin.ecs.test.AbstractEcsTest;

class SystemManagerTest extends AbstractEcsTest<SystemWorld> {

    TestSystem system;

    @Override
    protected SystemWorld createWorld() {
        this.system = new TestSystem();

        return World.builder(SystemWorld.class).configure(SystemConfig.builder(system)).build();
    }

    @Test
    void testWorldCreation() {
        assertThat(system.invocations).isZero();
    }

    @Test
    void testProcess() {
        world.processSystems(1f);

        assertThat(system.invocations).isEqualTo(1);
    }

    @Test
    void testStandalonePlugin() {
        var system1 = new TestSystem();
        var system2 = new TestSystem();

        var world = World.builder().build();
        var systems = SystemPlugin.standalone(world, system1, system2);

        // Call
        systems.processSystems(1f);

        // Verify
        assertThat(system1.invocations).isEqualTo(1);
        assertThat(system2.invocations).isEqualTo(1);
    }

    private class TestSystem implements BaseSystem {
        private int invocations;

        @Override
        public void process(float delta) {
            this.invocations++;
        }
    }

}
