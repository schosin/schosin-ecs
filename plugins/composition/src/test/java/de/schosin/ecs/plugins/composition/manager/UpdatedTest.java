package de.schosin.ecs.plugins.composition.manager;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;

import org.junit.jupiter.api.Test;

import de.schosin.ecs.plugins.composition.Composition;
import de.schosin.ecs.plugins.composition.manager.components.records.C1;
import de.schosin.ecs.plugins.composition.manager.components.records.C2;
import de.schosin.ecs.plugins.composition.manager.components.records.C3;

public class UpdatedTest extends AbstractCompositionPluginTest {

    @Test
    void testUpdatedEntities_WhenPreviousComposition_CallsRemoved() {
        // Setup
        bagManager.ensureEntitySize(10000);

        var archetype1 = storageEngine.getArchetype(component(C1.class));
        var archetype12 = storageEngine.getArchetype(component(C1.class), component(C2.class));
        var archetype2 = storageEngine.getArchetype(component(C2.class));
        var archetype3 = storageEngine.getArchetype(component(C3.class));

        var composition1 = compositionManager.createComposition(Composition.all(C1.class));
        var composition2 = compositionManager.createComposition(Composition.all(C2.class));
        var composition3 = compositionManager.createComposition(Composition.all(C3.class));

        var entity7Archetype = archetype2;
        var entity42Archetype = archetype1;
        var entity1337Archetype = archetype2;
        var entity9001Archetype = archetype12;

        compositionManager.handleEntityCreated(entity7Archetype, 7);
        compositionManager.handleEntityCreated(entity42Archetype, 42);
        compositionManager.handleEntityCreated(entity1337Archetype, 1337);
        compositionManager.handleEntityCreated(entity9001Archetype, 9001);

        var removed1 = new HashSet<Integer>();
        composition1.removed(removed1::add);

        var removed2 = new HashSet<Integer>();
        composition2.removed(removed2::add);

        var removed3 = new HashSet<Integer>();
        composition3.removed(removed3::add);

        // Update entity
        compositionManager.handleEntityBeforeUpdate(entity7Archetype, archetype1, 7);
        compositionManager.handleEntityUpdated(archetype1, entity7Archetype, 7);

        compositionManager.handleEntityBeforeUpdate(entity42Archetype, archetype12, 42);
        compositionManager.handleEntityUpdated(archetype12, entity42Archetype, 42);

        compositionManager.handleEntityBeforeUpdate(entity1337Archetype, archetype12, 1337);
        compositionManager.handleEntityUpdated(archetype12, entity1337Archetype, 1337);

        compositionManager.handleEntityBeforeUpdate(entity9001Archetype, archetype3, 9001);
        compositionManager.handleEntityUpdated(archetype3, entity9001Archetype, 9001);

        // Verify
        assertThat(removed1).containsExactlyInAnyOrder(9001);
        assertThat(removed2).containsExactlyInAnyOrder(7, 9001);
        assertThat(removed3).isEmpty();
    }

}
