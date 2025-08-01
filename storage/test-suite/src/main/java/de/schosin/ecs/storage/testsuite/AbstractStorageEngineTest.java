package de.schosin.ecs.storage.testsuite;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.function.Consumer;

import de.schosin.ecs.api.components.types.ComponentType.RegularComponentType;
import de.schosin.ecs.storage.api.StorageEngine;
import de.schosin.ecs.test.AbstractEcsTest;

public abstract class AbstractStorageEngineTest extends AbstractEcsTest<TestWorld> {

    protected static final Consumer<RegularComponentType<?, ?>> NO_OP = AbstractStorageEngineTest::noOp;

    @SuppressWarnings("unused")
    protected static void noOp(RegularComponentType<?, ?> type) {
    }

    protected StorageEngine engine;

    @Override
    protected TestWorld createWorld() {
        var world = super.createWorld();
        this.engine = world.getSingleton(StorageEngine.class);

        return world;
    }

    @Override
    protected void verifyArchetypeHasComponents(int entityId, RegularComponentType<?, ?>... types) {
        var archetype = engine.getArchetypeForEntity(entityId);
        assertThat(archetype).as("archetype exists").isNotNull();

        for (var type : types) {
            var component = componentManager.getComponent(type);
            assertThat(archetype.getComponents()).as("archetype has %s", type).contains(component);
        }
    }

    @Override
    protected void verifyArchetypeDoesNotHaveComponents(int entityId, RegularComponentType<?, ?>... types) {
        var archetype = engine.getArchetypeForEntity(entityId);
        assertThat(archetype).as("archetype exists").isNotNull();

        for (var type : types) {
            var component = componentManager.getComponent(type);
            assertThat(archetype.getComponents()).as("archetype does not have %s", type).doesNotContain(component);
        }
    }

}
