package de.schosin.ecs.plugins.composition;

import de.schosin.ecs.api.Plugin;
import de.schosin.ecs.codegen.EcsCodegen;
import de.schosin.ecs.plugins.composition.manager.CompositionManager;

@EcsCodegen
@Plugin(CompositionManager.class)
public interface CompositionPlugin extends CompositionCreator, Spec.SpecCreator {
}
