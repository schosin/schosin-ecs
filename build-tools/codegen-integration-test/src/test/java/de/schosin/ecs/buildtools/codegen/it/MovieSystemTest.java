package de.schosin.ecs.buildtools.codegen.it;

import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.api.Test;

import de.schosin.ecs.api.World;

class MovieSystemTest {

    @Test
    void test() {
        var world = World.builder().build();

        assertThatCode(() -> new MovieSystem(world)).doesNotThrowAnyException();
    }

}
