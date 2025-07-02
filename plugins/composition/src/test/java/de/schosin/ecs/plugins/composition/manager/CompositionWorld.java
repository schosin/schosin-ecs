package de.schosin.ecs.plugins.composition.manager;

import de.schosin.ecs.api.World;
import de.schosin.ecs.plugins.composition.CompositionPlugin;
import de.schosin.ecs.plugins.wildcards.WildcardPlugin;

public interface CompositionWorld extends World, CompositionPlugin, WildcardPlugin {
}
