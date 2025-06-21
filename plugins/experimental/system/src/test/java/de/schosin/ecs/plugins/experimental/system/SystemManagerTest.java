package de.schosin.ecs.plugins.experimental.system;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import de.schosin.ecs.plugins.experimental.system.systems.BaseSystem;

class SystemManagerTest {

    @Nested
    class StandaloneTest extends AbstractTest<CompositionWorld> {
        @Override
        protected SystemPlugin plugin() {
            return SystemPlugin.standalone(world);
        }
    }

    @Nested
    class WorldTest extends AbstractTest<SystemWorld> {
        @Override
        protected SystemPlugin plugin() {
            return world;
        }
    }

    abstract class AbstractTest<T extends CompositionWorld> extends AbstractSystemPluginTest<T> {

        @Nested
        class InvocationTest {

            @Test
            void testAddSystems() {
                var system = new TestSystem();
                plugin.addSystems(system);

                assertThat(system.invocations).isZero();
            }

            @Test
            void testProcess() {
                var system = new TestSystem();
                plugin.addSystems(system);

                plugin.processSystems();

                assertThat(system.invocations).isEqualTo(1);
            }

            @Test
            void testStandalonePlugin() {
                var system1 = new TestSystem();
                var system2 = new TestSystem();

                plugin.addSystems(system1, system2);

                // Call
                plugin.processSystems();

                // Verify
                assertThat(system1.invocations).isEqualTo(1);
                assertThat(system2.invocations).isEqualTo(1);
            }

            private class TestSystem implements BaseSystem {
                private int invocations;

                @Override
                public void process() {
                    this.invocations++;
                }
            }

        }

    }

}
