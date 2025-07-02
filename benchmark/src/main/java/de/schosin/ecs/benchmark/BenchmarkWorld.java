package de.schosin.ecs.benchmark;

import de.schosin.ecs.plugins.state.StatePlugin;
import de.schosin.ecs.plugins.transmuter.TransmuterPlugin;
import de.schosin.ecs.plugins.wildcards.WildcardPlugin;
import de.schosin.ecs.worlds.DefaultWorld;

public interface BenchmarkWorld extends DefaultWorld, StatePlugin, TransmuterPlugin, WildcardPlugin {
}
