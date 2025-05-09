package de.schosin.ecs.engine;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import de.schosin.ecs.api.World;

public class EngineWorldTest extends AbstractWorldTest {

    @Nested
    class WorldCreationTest {

        @Test
        void testDefault() {
            assertThat(World.builder()).isInstanceOf(WorldBuilder.class);
            assertThat(World.builder().build()).isInstanceOf(EngineWorld.class);
        }

        @Test
        void testConfiguration() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
            var world = (EngineWorld) World.builder()
                    .processLoops(42)
                    .build();

            var config = getField(world, "config");
            assertThat(config)
                    .hasOnlyFields("processLoops") // add assertions for new fields
                    .hasFieldOrPropertyWithValue("processLoops", 42);
        }

    }

}
