package de.schosin.ecs.plugins.composition.manager;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;

import org.junit.jupiter.api.Test;

import de.schosin.ecs.plugins.composition.Composition;
import de.schosin.ecs.plugins.composition.manager.components.records.C1;

public class WorldTest extends AbstractCompositionPluginTest {

    @Test
    void testInserted_WhenEntityCreated() {
        var inserted = new ArrayList<Integer>();
        var composition = world.createComposition(Composition.all(C1.class));
        composition.inserted(inserted::add);

        // Call
        var entityId = world.createEntity(new C1());

        // Verify
        assertThat(inserted).containsExactly(entityId);
    }

    @Test
    void testInserted_WhenEntityCompositionChanged_NotCalledIfNotProcessed() {
        var mapper = world.getComponents(C1.class);

        var inserted = new ArrayList<Integer>();
        var composition = world.createComposition(Composition.all(C1.class));
        composition.inserted(inserted::add);

        var entityId = world.createEntity();
        assertThat(inserted).isEmpty();

        // Call
        mapper.add(entityId, new C1());

        // Verify
        assertThat(inserted).isEmpty();
    }

    @Test
    void testInserted_WhenEntityCompositionChanged_CalledWhenProcessed() {
        var mapper = world.getComponents(C1.class);

        var inserted = new ArrayList<Integer>();
        var composition = world.createComposition(Composition.all(C1.class));
        composition.inserted(inserted::add);

        var entityId = world.createEntity();
        assertThat(inserted).isEmpty();

        // Call
        mapper.add(entityId, new C1());
        world.process();

        // Verify
        assertThat(inserted).containsExactly(entityId);
    }

    @Test
    void testProcess_WhenEntityQueuedForDeletion_StillProcessesEntity() {
    }

    @Test
    void testRemoved_WhenEntityCompositionChanged_NotCalledIfNotProcessed() {
        var mapper = world.getComponents(C1.class);

        var removed = new ArrayList<Integer>();
        var composition = world.createComposition(Composition.all(C1.class));
        composition.removed(removed::add);

        var entityId = world.createEntity(new C1());

        // Call
        mapper.remove(entityId);

        // Verify
        assertThat(removed).isEmpty();
    }

    @Test
    void testRemoved_WhenEntityCompositionChanged_CalledWhenProcessed() {
        var mapper = world.getComponents(C1.class);

        var removed = new ArrayList<Integer>();
        var composition = world.createComposition(Composition.all(C1.class));
        composition.removed(removed::add);

        var entityId = world.createEntity(new C1());

        // Call
        mapper.remove(entityId);
        world.process();

        // Verify
        assertThat(removed).containsExactly(entityId);
    }

    @Test
    void testRemoved_WhenEntityDeleted_NotCalledIfNotProcessed() {
        var removed = new ArrayList<Integer>();
        var composition = world.createComposition(Composition.all(C1.class));
        composition.removed(removed::add);

        var entityId = world.createEntity(new C1());

        // Call
        world.deleteEntity(entityId);

        // Verify
        assertThat(removed).isEmpty();
    }

    @Test
    void testRemoved_WhenEntityDeleted_CalledWhenProcessed() {
        var removed = new ArrayList<Integer>();
        var composition = world.createComposition(Composition.all(C1.class));
        composition.removed(removed::add);

        var entityId = world.createEntity(new C1());

        // Call
        world.deleteEntity(entityId);
        world.process();

        // Verify
        assertThat(removed).containsExactly(entityId);

    }

}
