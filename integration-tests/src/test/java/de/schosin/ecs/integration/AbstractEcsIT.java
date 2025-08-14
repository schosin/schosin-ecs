package de.schosin.ecs.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.Random;

import org.assertj.core.api.Assertions;

import de.schosin.ecs.plugins.experimental.system.SystemPlugin;
import de.schosin.ecs.test.AbstractEcsTest;

public abstract class AbstractEcsIT extends AbstractEcsTest<SimulationWorld> {

    protected static final Random RNG = new Random(0);

    private static final Duration DEFAULT_DURATION = Duration.ofSeconds(10);
    private static final Duration MARGIN = Duration.ofSeconds(1);

    private static final long DEFAULT_SLEEP = 100L;

    protected Duration getSimulationDuration() {
        return DEFAULT_DURATION;
    }

    protected long getSimultationSleep() {
        return DEFAULT_SLEEP;
    }

    protected Simulation runSimulation(SystemPlugin systems) {
        return runSimulation(systems, getSimulationDuration(), getSimultationSleep());
    }

    protected Simulation runSimulation(SystemPlugin systems, Duration duration) {
        Assertions.setMaxStackTraceElementsDisplayed(100);
        
        return runSimulation(systems, duration, getSimultationSleep());
    }

    protected Simulation runSimulation(SystemPlugin systems, Duration duration, long sleep) {
        var simulation = new Simulation(this.world, systems, duration, sleep);
        simulation.run();

        await().atMost(duration.plus(MARGIN)).until(simulation::isStopped);
        assertThat(simulation.getException()).as("Simulation throws no exception").isNull();

        return simulation;
    }

}
