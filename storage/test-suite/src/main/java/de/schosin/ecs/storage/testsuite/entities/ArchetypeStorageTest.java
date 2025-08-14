package de.schosin.ecs.storage.testsuite.entities;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import de.schosin.ecs.storage.api.events.ArchetypeAddedEvent;
import de.schosin.ecs.storage.testsuite.AbstractStorageEngineTest;
import de.schosin.ecs.storage.testsuite.entities.ArchetypeTest.C2;

public class ArchetypeStorageTest extends AbstractStorageEngineTest {

    @Nested
    class GetArchetypesTest {

        @Test
        void testNoArchetypes() {
            var archetypes = storageEngine.getArchetypes();
            assertThat(archetypes).as("getArchetypes must not return null").isNotNull();
            assertThat(archetypes.getSize()).as("getArchetypes must return bag containing only empty archetype if no archetypes created").isEqualTo(1);

            var emptyArchetype = storageEngine.getArchetype();
            assertThat(archetypes).as("getArchetypes must return bag containing only empty archetype if no archetypes created").containsExactly(emptyArchetype);
        }

        @Test
        void testArchetypes() {
            var emtpyArchetype = storageEngine.getArchetype();
            var archetype1 = storageEngine.getArchetype(component(C1.class));

            var archetypes = storageEngine.getArchetypes();
            assertThat(archetypes).as("getArchetypes must not return null").isNotNull();
            assertThat(archetypes.getSize()).as("getArchetypes must return all archetypes").isEqualTo(2);
            assertThat(archetypes.get(0)).as("getArchetypes must return all archetypes").isIn(emtpyArchetype, archetype1);
            assertThat(archetypes.get(1)).as("getArchetypes must return all archetypes").isIn(emtpyArchetype, archetype1);
        }

        @Test
        void testArchetypesAddedAfterwards() {
            var emtpyArchetype = storageEngine.getArchetype();
            var archetype1 = storageEngine.getArchetype(component(C1.class));

            var archetypes = storageEngine.getArchetypes();
            var archetype2 = storageEngine.getArchetype(component(C2.class));

            assertThat(archetypes).as("getArchetypes must not return null").isNotNull();
            assertThat(archetypes.getSize()).as("getArchetypes must return all archetypes").isEqualTo(3);
            assertThat(archetypes.get(0)).as("getArchetypes must return all archetypes").isIn(emtpyArchetype, archetype1, archetype2);
            assertThat(archetypes.get(1)).as("getArchetypes must return all archetypes").isIn(emtpyArchetype, archetype1, archetype2);
            assertThat(archetypes.get(1)).as("getArchetypes must return all archetypes").isIn(emtpyArchetype, archetype1, archetype2);
        }

        @Test
        void testNoUnnecessaryArchetypesCreated() {
            var emtpyArchetype = storageEngine.getArchetype();
            var archetype = storageEngine.getArchetype(component(C1.class), component(C2.class));

            var archetypes = storageEngine.getArchetypes();
            assertThat(archetypes).as("getArchetype must only return known archetypes").containsExactlyInAnyOrder(emtpyArchetype, archetype);
        }

    }

    @Test
    void testArchetypeInstanceReused() {
        var archetype = storageEngine.getArchetype();
        var archetype1 = storageEngine.getArchetype(component(C1.class));

        assertThat(storageEngine.getArchetype()).as("getArchetype must return the same instance for the same arguments").isSameAs(archetype);
        assertThat(storageEngine.getArchetype(component(C1.class))).as("getArchetype must return the same instance for the same arguments").isSameAs(archetype1);
    }

    @Test
    void testComponentTypeOrder() {
        var archetype12 = storageEngine.getArchetype(component(C1.class), component(C2.class));
        var archetype21 = storageEngine.getArchetype(component(C2.class), component(C1.class));

        assertThat(archetype21).as("getArchetype must return same instance regardless of component type order").isSameAs(archetype12);
    }

    @Test
    void testGetById() {
        var archetype = storageEngine.getArchetype();
        var archetype1 = storageEngine.getArchetype(component(C1.class));

        assertThat(storageEngine.getArchetypeById(archetype.getId())).as("getArchetypeById must return same instance").isSameAs(archetype);
        assertThat(storageEngine.getArchetypeById(archetype1.getId())).as("getArchetypeById must return same instance").isSameAs(archetype1);
    }

    @Test
    void testGetForEntity() {
        var archetype = storageEngine.getArchetype();
        var archetype1 = storageEngine.getArchetype(component(C1.class));

        var entity = world.createEntity();
        var entity1 = world.createEntity(new C1());

        assertThat(storageEngine.getArchetypeForEntity(entity)).as("getArchetypeForEntity returns same instance of matching archetype").isSameAs(archetype);
        assertThat(storageEngine.getArchetypeForEntity(entity1)).as("getArchetypeForEntity returns same instance of matching archetype").isSameAs(archetype1);
    }

    @Test
    void testArchetypeAddedEvent() {
        var events = new ArrayList<ArchetypeAddedEvent>();
        eventManager.registerEventHandler(ArchetypeAddedEvent.class, events::add);

        // Access empty archetype
        var archetype1 = storageEngine.getArchetype(component(C1.class));
        assertThat(events).extracting("archetype").as("accessing an archetype for the first time must dispatch ArchetypeAddedEvent").containsExactly(archetype1);

        // Access empty archetype again
        storageEngine.getArchetype(component(C1.class));
        assertThat(events).extracting("archetype").as("accessing an archetype multiple times must dispatch ArchetypeAddedEvent only once").containsExactly(archetype1);

        // Access archetypes
        var archetype2 = storageEngine.getArchetype(component(C2.class));
        var archetype12 = storageEngine.getArchetype(component(C1.class), component(C2.class));

        assertThat(events).extracting("archetype")
                .as("accessing an archetype multiple times must dispatch ArchetypeAddedEvent only once")
                .containsExactly(archetype1, archetype2, archetype12);
    }

    record C1() {
    }

}
