package de.schosin.ecs.plugins.composition;

import de.schosin.ecs.api.Plugin;
import de.schosin.ecs.plugins.composition.manager.CompositionManager;

@Plugin(CompositionManager.class)
public interface CompositionPlugin extends Composition.Creator, Spec.SpecCreator {
}
