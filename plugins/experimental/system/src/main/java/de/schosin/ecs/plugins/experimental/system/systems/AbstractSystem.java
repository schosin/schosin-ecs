package de.schosin.ecs.plugins.experimental.system.systems;

/**
 * Abstract base system for the supported system types.
 */
public abstract class AbstractSystem implements BaseSystem {

    @Override
    public final void process() {
        begin();
        processSystem();
        end();
    }

    /**
     * Called before every {@link #processSystem()}.
     */
    public void begin() {
    }

    /**
     * Process system logic.
     */
    protected abstract void processSystem();

    /**
     * Called after every {@link #processSystem()}.
     */
    public void end() {
    }

    @Override
    public final Void call() {
        return BaseSystem.super.call();
    }

}
