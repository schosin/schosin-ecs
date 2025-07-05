package de.schosin.ecs.plugins.composition;

import de.schosin.ecs.api.Plugin;
import de.schosin.ecs.api.components.types.DataProcessorType;
import de.schosin.ecs.api.data.DataProcessor;
import de.schosin.ecs.codegen.EcsCodegen;
import de.schosin.ecs.plugins.composition.manager.CompositionManager;

@EcsCodegen
@Plugin(CompositionManager.class)
public interface CompositionPlugin extends CompositionCreator, Spec.SpecCreator {

    <R, P extends DataProcessor<R>> CompositionData<P> createComposition(Composition.Builder builder, DataProcessorType<?, R, P> componentType);

}
