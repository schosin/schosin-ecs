package de.schosin.ecs.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.StackWalker.Option;
import java.lang.StackWalker.StackFrame;
import java.time.Duration;

import de.schosin.ecs.api.World;
import de.schosin.ecs.plugins.experimental.system.SystemPlugin;
import de.schosin.ecs.storage.api.StorageEngine;

public class Simulation {

    private final Class<?> caller;
    private final Class<?> storageEngine;

    private final Duration duration;
    private final long durationMs;
    private final long sleep;

    private final World world;
    private final SystemPlugin systems;

    private volatile boolean running;
    private Throwable exception;

    Simulation(World world, SystemPlugin systems, Duration duration, long sleep) {
        this.caller = StackWalker.getInstance(Option.RETAIN_CLASS_REFERENCE).walk(stacks -> stacks
                .filter(frame -> frame.getDeclaringClass() != AbstractEcsIT.class && frame.getDeclaringClass() != Simulation.class)
                .findFirst()
                .map(StackFrame::getDeclaringClass)
                .orElseThrow());

        this.storageEngine = world.getSingleton(StorageEngine.class).getClass();

        this.duration = duration;
        this.durationMs = duration.toMillis();
        this.sleep = sleep;

        this.world = world;
        this.systems = systems;
    }

    public boolean isRunning() {
        return this.running;
    }

    public boolean isStopped() {
        return !this.running;
    }

    public Throwable getException() {
        return this.exception;
    }

    public void run() {
        assertThat(running).as("Simulation not already started").isFalse();

        System.out.println("Starting simultation for %s: %s (Storage: %s)".formatted(duration, caller.getSimpleName(), storageEngine.getSimpleName()));

        this.running = true;
        new Thread(this::runSimulation, "simulation-thread").start();
    }

    public void stop() {
        assertThat(running).as("Simulation must be running").isTrue();

        this.running = false;
    }

    private void runSimulation() {
        var start = System.currentTimeMillis();

        while (this.running) {
            try {
                systems.processSystems();
                world.process();

                Thread.sleep(sleep);

                if (System.currentTimeMillis() - start >= durationMs) {
                    this.running = false;
                    System.out.println("Simultation finished normally: %s (Storage: %s)".formatted(caller.getName(), storageEngine.getSimpleName()));
                }
            } catch (Throwable ex) {
                this.running = false;
                this.exception = ex;

                System.err.println("Simultation finished exceptionally: %s (Storage: %s)".formatted(caller.getName(), storageEngine.getSimpleName()));
                ex.printStackTrace();
            }
        }
    }

}
