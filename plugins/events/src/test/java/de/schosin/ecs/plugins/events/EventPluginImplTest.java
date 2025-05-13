package de.schosin.ecs.plugins.events;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;

import org.junit.jupiter.api.Test;

import de.schosin.ecs.test.AbstractEcsTest;

class EventPluginImplTest extends AbstractEcsTest<EventWorld> {

    @Test
    void testEventHandler() {
        var events = new ArrayList<Object>();
        world.registerEventHandler(Object.class, events::add);

        // Call
        world.dispatchEvent("first");
        world.dispatchEvent("second");
        world.dispatchEvent(events);

        // Verify
        assertThat(events).containsExactly("first", "second", events);
    }

}
