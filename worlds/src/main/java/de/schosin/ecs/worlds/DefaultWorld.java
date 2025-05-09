package de.schosin.ecs.worlds;

import de.schosin.ecs.api.World;
import de.schosin.ecs.plugins.archetype.ArchetypePlugin;
import de.schosin.ecs.plugins.composition.CompositionPlugin;
import de.schosin.ecs.plugins.state.StatePlugin;
import de.schosin.ecs.plugins.transmuter.TransmuterPlugin;

public interface DefaultWorld extends World, StatePlugin, ArchetypePlugin, TransmuterPlugin, CompositionPlugin {

    static DefaultWorld create() {
        return builder().build();
    }

    static World.Builder<DefaultWorld> builder() {
        return World.builder(DefaultWorld.class);
    }

}
