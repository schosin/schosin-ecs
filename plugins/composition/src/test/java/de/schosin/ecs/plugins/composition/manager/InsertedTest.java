package de.schosin.ecs.plugins.composition.manager;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.HashSet;

import org.junit.jupiter.api.Test;

import de.schosin.ecs.plugins.composition.Composition;
import de.schosin.ecs.plugins.composition.manager.components.records.C1;
import de.schosin.ecs.plugins.composition.manager.components.records.C2;
import de.schosin.ecs.utils.collections.IntBag;

public class InsertedTest extends AbstractCompositionPluginTest {

    @Test
    void testInserted_AllComposition() {
        // Setup
        var composition = compositionManager.createComposition(EMPTY);

        var inserted = new IntBag(3);
        composition.inserted(inserted::add);

        // Insert entity
        var entity1 = world.createEntity();
        var entity2 = world.createEntity();
        var entity3 = world.createEntity();

        // Verify
        assertThat(inserted.getSize()).as("size").isEqualTo(3);
        assertThat(inserted.contains(entity1)).as("contains 1").isTrue();
        assertThat(inserted.contains(entity2)).as("contains 2").isTrue();
        assertThat(inserted.contains(entity3)).as("contains 3").isTrue();
    }

    @Test
    void testInserted_InterestedOnly() {
        // Setup
        var composition = compositionManager.createComposition(Composition.all(C1.class));

        var inserted = new IntBag(1);
        composition.inserted(inserted::add);

        // Insert entity
        var entity1 = world.createEntity(new C1(), new C2());
        world.createEntity(new C2());

        // Verify
        assertThat(inserted.getSize()).as("size").isEqualTo(1);
        assertThat(inserted.contains(entity1)).as("contains 42").isTrue();
    }

    @Test
    void testMultipleInserted() {
        // Setup
        var inserted1 = new IntBag(2);
        var inserted2 = new ArrayList<Integer>();
        var inserted3 = new HashSet<Integer>();

        var composition = compositionManager.createComposition(EMPTY);

        // Add callbacks
        composition.inserted(inserted1::add);
        assertThat(inserted1.getSize()).as("size").isZero();

        composition.inserted(inserted2::add);
        assertThat(inserted2).isEmpty();

        composition.inserted(inserted3::add);
        assertThat(inserted3).isEmpty();

        // Insert entity
        var entity1 = world.createEntity();
        var entity2 = world.createEntity();
        var entity3 = world.createEntity();

        // Verify
        assertThat(inserted1.getSize()).as("size").isEqualTo(3);
        assertThat(inserted1.contains(entity1)).as("contains 1").isTrue();
        assertThat(inserted1.contains(entity2)).as("contains 2").isTrue();
        assertThat(inserted1.contains(entity3)).as("contains 3").isTrue();

        assertThat(inserted2).containsExactlyInAnyOrder(entity1, entity2, entity3);

        assertThat(inserted3).containsExactlyInAnyOrder(entity1, entity2, entity3);
    }

}
