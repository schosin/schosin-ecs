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
            var archetypes = engine.getArchetypes();
            assertThat(archetypes).as("getArchetypes must not return null").isNotNull();
            assertThat(archetypes.getSize()).as("getArchetypes must return bag containing only empty archetype if no archetypes created").isEqualTo(1);

            var emptyArchetype = engine.getArchetype();
            assertThat(archetypes).as("getArchetypes must return bag containing only empty archetype if no archetypes created").containsExactly(emptyArchetype);
        }

        @Test
        void testArchetypes() {
            var emtpyArchetype = engine.getArchetype();
            var archetype1 = engine.getArchetype(component(C1.class));

            var archetypes = engine.getArchetypes();
            assertThat(archetypes).as("getArchetypes must not return null").isNotNull();
            assertThat(archetypes.getSize()).as("getArchetypes must return all archetypes").isEqualTo(2);
            assertThat(archetypes.get(0)).as("getArchetypes must return all archetypes").isIn(emtpyArchetype, archetype1);
            assertThat(archetypes.get(1)).as("getArchetypes must return all archetypes").isIn(emtpyArchetype, archetype1);
        }

        @Test
        void testArchetypesAddedAfterwards() {
            var emtpyArchetype = engine.getArchetype();
            var archetype1 = engine.getArchetype(component(C1.class));

            var archetypes = engine.getArchetypes();
            var archetype2 = engine.getArchetype(component(C2.class));

            assertThat(archetypes).as("getArchetypes must not return null").isNotNull();
            assertThat(archetypes.getSize()).as("getArchetypes must return all archetypes").isEqualTo(3);
            assertThat(archetypes.get(0)).as("getArchetypes must return all archetypes").isIn(emtpyArchetype, archetype1, archetype2);
            assertThat(archetypes.get(1)).as("getArchetypes must return all archetypes").isIn(emtpyArchetype, archetype1, archetype2);
            assertThat(archetypes.get(1)).as("getArchetypes must return all archetypes").isIn(emtpyArchetype, archetype1, archetype2);
        }

        @Test
        void testNoUnnecessaryArchetypesCreated() {
            var emtpyArchetype = engine.getArchetype();
            var archetype = engine.getArchetype(component(C1.class), component(C2.class));

            var archetypes = engine.getArchetypes();
            assertThat(archetypes).as("getArchetype must only return known archetypes").containsExactlyInAnyOrder(emtpyArchetype, archetype);
        }

    }

    @Test
    void testArchetypeInstanceReused() {
        var archetype = engine.getArchetype();
        var archetype1 = engine.getArchetype(component(C1.class));

        assertThat(engine.getArchetype()).as("getArchetype must return the same instance for the same arguments").isSameAs(archetype);
        assertThat(engine.getArchetype(component(C1.class))).as("getArchetype must return the same instance for the same arguments").isSameAs(archetype1);
    }

    @Test
    void testComponentTypeOrder() {
        var archetype12 = engine.getArchetype(component(C1.class), component(C2.class));
        var archetype21 = engine.getArchetype(component(C2.class), component(C1.class));

        assertThat(archetype21).as("getArchetype must return same instance regardless of component type order").isSameAs(archetype12);
    }

    @Test
    void testGetById() {
        var archetype = engine.getArchetype();
        var archetype1 = engine.getArchetype(component(C1.class));

        assertThat(engine.getArchetypeById(archetype.getId())).as("getArchetypeById must return same instance").isSameAs(archetype);
        assertThat(engine.getArchetypeById(archetype1.getId())).as("getArchetypeById must return same instance").isSameAs(archetype1);
    }

    @Test
    void testGetForEntity() {
        var archetype = engine.getArchetype();
        var archetype1 = engine.getArchetype(component(C1.class));

        var entity = world.createEntity();
        var entity1 = world.createEntity(new C1());

        assertThat(engine.getArchetypeForEntity(entity)).as("getArchetypeForEntity returns same instance of matching archetype").isSameAs(archetype);
        assertThat(engine.getArchetypeForEntity(entity1)).as("getArchetypeForEntity returns same instance of matching archetype").isSameAs(archetype1);
    }

    @Test
    void testArchetypeAddedEvent() {
        var events = new ArrayList<ArchetypeAddedEvent>();
        eventManager.registerEventHandler(ArchetypeAddedEvent.class, events::add);

        // Access empty archetype
        var archetype1 = engine.getArchetype(component(C1.class));
        assertThat(events).extracting("archetype").as("accessing an archetype for the first time must dispatch ArchetypeAddedEvent").containsExactly(archetype1);

        // Access empty archetype again
        engine.getArchetype(component(C1.class));
        assertThat(events).extracting("archetype").as("accessing an archetype multiple times must dispatch ArchetypeAddedEvent only once").containsExactly(archetype1);

        // Access archetypes
        var archetype2 = engine.getArchetype(component(C2.class));
        var archetype12 = engine.getArchetype(component(C1.class), component(C2.class));

        assertThat(events).extracting("archetype")
                .as("accessing an archetype multiple times must dispatch ArchetypeAddedEvent only once")
                .containsExactly(archetype1, archetype2, archetype12);
    }

    record C1() {
    }

}
