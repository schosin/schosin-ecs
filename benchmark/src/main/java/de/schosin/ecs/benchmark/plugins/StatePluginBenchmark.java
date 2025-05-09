package de.schosin.ecs.benchmark.plugins;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import de.schosin.ecs.api.World;
import de.schosin.ecs.benchmark.EcsBenchmark;
import de.schosin.ecs.plugins.state.StateManager;
import de.schosin.ecs.plugins.state.StatePlugin;

public class StatePluginBenchmark extends EcsBenchmark {

    public interface StateWorld extends World, StatePlugin {
    }

    public static class TestState {
    }

    public static void main(String[] args) throws Exception {
        var options = new OptionsBuilder()
                .include(benchmarkName(StatePluginBenchmark.class))
                .build();

        new Runner(options).run();
    }

    @Param({ "1000", "2000" })
    private int count;

    World world;

    StatePlugin plugin;
    StateWorld proxied;

    int[] worldEntities;
    int[] proxiedEntities;

    final TestState testState = new TestState();

    @Setup(Level.Trial)
    public void initTrial() {
        this.world = World.builder().build();

        this.plugin = new StateManager(world);
        this.proxied = World.builder(StateWorld.class).build();

        this.worldEntities = new int[count];
        this.proxiedEntities = new int[count];
    }

    @Setup(Level.Invocation)
    public void initInvocation() {
        for (int i = 0; i < count; i++) {
            world.deleteEntity(this.worldEntities[i]);
            proxied.deleteEntity(this.proxiedEntities[i]);
        }

        world.process();
        proxied.process();

        for (int i = 0; i < count; i++) {
            this.worldEntities[i] = world.createEntity();
            this.proxiedEntities[i] = proxied.createEntity();
        }
    }

    @Benchmark
    public void plugin(Blackhole bh) {
        var testStateM = plugin.getState(TestState.class);

        for (int i = 0; i < count; i++) {
            bh.consume(testStateM.add(worldEntities[i], testState));
        }
    }

    @Benchmark
    public void proxied(Blackhole bh) {
        var testStateM = proxied.getState(TestState.class);

        for (int i = 0; i < count; i++) {
            bh.consume(testStateM.add(proxiedEntities[i], testState));
        }
    }

}
