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
    class GetArcchetypesTest {

        @Test
        void testNoArchetypes() {
            var archetypes = engine.getArchetypes();
            assertThat(archetypes).as("getArchetypes must not return null").isNotNull();
            assertThat(archetypes.getSize()).as("getArchetypes must return empty bag if no archetypes created").isZero();
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
    void testComponentMask() {
        var componentMask = engine.getComponentMask();
        var archetype = engine.getArchetype();

        assertThat(archetype.getComponentMask()).as("archetype.getComponentMask() must be same as returned by engine for same types").isSameAs(componentMask);
    }

    @Test
    void testIdMatchesComponentMask() {
        var archetype = engine.getArchetype();
        assertThat(archetype.getId()).as("archetype.getId() must be equal to archetype.componentMask().getId()").isEqualTo(archetype.getComponentMask().getId());
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
        var archetype = engine.getArchetype();
        assertThat(events).extracting("archetype").as("accessing an archetype for the first time must dispatch ArchetypeAddedEvent").containsExactly(archetype);

        // Access empty archetype again
        engine.getArchetype();
        assertThat(events).extracting("archetype").as("accessing an archetype multiple times must dispatch ArchetypeAddedEvent only once").containsExactly(archetype);

        // Access more archetypes
        var archetype1 = engine.getArchetype(component(C1.class));
        var archetype2 = engine.getArchetype(component(C2.class));
        var archetype12 = engine.getArchetype(component(C1.class), component(C2.class));
        assertThat(events).extracting("archetype")
                .as("accessing an archetype multiple times must dispatch ArchetypeAddedEvent only once")
                .containsExactly(archetype, archetype1, archetype2, archetype12);
    }

    record C1() {
    }

}
