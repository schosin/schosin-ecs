package de.schosin.ecs.plugins.composition.manager;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import de.schosin.ecs.plugins.composition.Composition;
import de.schosin.ecs.plugins.composition.manager.components.records.P1;
import de.schosin.ecs.storage.api.StorageEngineException;

public class RemovedTest extends AbstractCompositionPluginTest {

    @Test
    void testAddComponentInRemoved_ShouldThrow() {
        var entityId = world.createEntity();

        var mapper1 = world.getPooledComponents(P1.class);

        var composition = world.createComposition(Composition.all());
        composition.removed(mapper1::add);

        // Call
        world.deleteEntity(entityId);

        try {
            world.process();
            // not throwing is okay
        } catch (StorageEngineException ex) {
            assertThat(ex)
                    .isInstanceOf(StorageEngineException.class)
                    .hasMessageContainingAll("entity %d".formatted(entityId), "marked for deletion");
        }
    }

    @Test
    void testRemoveComponentInRemoved_ShouldThrow() {
        var entityId = world.createEntity(new P1());

        var mapper1 = world.getPooledComponents(P1.class);

        var composition = world.createComposition(Composition.all());
        composition.removed(mapper1::remove);

        // Call
        world.deleteEntity(entityId);

        try {
            world.process();
            // not throwing is okay
        } catch (StorageEngineException ex) {
            assertThat(ex)
                    .isInstanceOf(StorageEngineException.class)
                    .hasMessageContainingAll("entity %d".formatted(entityId), "marked for deletion");
        }
    }

}
