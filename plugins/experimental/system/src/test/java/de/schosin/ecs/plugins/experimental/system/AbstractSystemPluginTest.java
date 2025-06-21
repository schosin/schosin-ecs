package de.schosin.ecs.plugins.experimental.system;

import org.junit.jupiter.api.BeforeEach;

import de.schosin.ecs.test.AbstractEcsTest;

public abstract class AbstractSystemPluginTest<T extends CompositionWorld> extends AbstractEcsTest<T> {

    protected SystemPlugin plugin;

    @BeforeEach
    void setupSystemPlugin() {
        this.plugin = plugin();
    }

    protected abstract SystemPlugin plugin();

}
