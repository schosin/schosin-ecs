package de.schosin.ecs.storage.testsuite;

import java.util.function.Consumer;

import de.schosin.ecs.api.World;
import de.schosin.ecs.api.components.ComponentType.RegularComponentType;
import de.schosin.ecs.storage.api.StorageEngine;
import de.schosin.ecs.test.AbstractEcsTest;

public abstract class AbstractStorageEngineTest extends AbstractEcsTest<World> {

    protected static final Consumer<RegularComponentType<?, ?>> NO_OP = AbstractStorageEngineTest::noOp;

    @SuppressWarnings("unused")
    protected static void noOp(RegularComponentType<?, ?> type) {
    }

    protected StorageEngine engine;

    @Override
    protected World createWorld() {
        var world = super.createWorld();
        this.engine = world.getSingleton(StorageEngine.class);

        return world;
    }

}
