package de.schosin.ecs.worlds;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DefaultWorldTest {

    @Test
    void testCreate() {
        assertThat(DefaultWorld.create()).isNotNull();
    }

    @Test
    void testBuilder() {
        assertThat(DefaultWorld.builder().build()).isNotNull();
    }

}
