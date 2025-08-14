package de.schosin.ecs.engine;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import de.schosin.ecs.api.Pooled;
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
                    .expectedEntities(9001)
                    .processLoops(42)
                    .build();

            var config = getField(world, "config");
            assertThat(config)
                    .hasOnlyFields("expectedEntities", "processLoops") // add assertions for new fields
                    .hasFieldOrPropertyWithValue("expectedEntities", 9001)
                    .hasFieldOrPropertyWithValue("processLoops", 42);
        }

    }

    public record C1() implements Pooled {
    }

    public record C2() implements Pooled {
    }

}
