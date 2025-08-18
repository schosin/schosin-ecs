package de.schosin.ecs.plugins.composition;

import java.util.function.IntConsumer;

import de.schosin.ecs.api.Plugin;
import de.schosin.ecs.api.components.types.DataProcessorType;
import de.schosin.ecs.api.data.DataProcessor;
import de.schosin.ecs.codegen.EcsCodegen;
import de.schosin.ecs.plugins.composition.manager.CompositionManager;

@EcsCodegen
@Plugin(CompositionManager.class)
public interface CompositionPlugin extends CompositionCreator, Spec.SpecCreator {

    /**
     * Initialize entities that match the {@code builder} during creation by adding
     * the components if that entity does not already have that component.
     * 
     * <p>
     * If the components require initialization that depends on other components,
     * add the components using this method and initialize them using
     * {@link Composition#inserted(IntConsumer)} or {@link CompositionData#inserted(DataProcessor)}.
     * 
     * <p>
     * This method should be preferred over adding components in {@link Composition#inserted(IntConsumer)}
     * callbacks. This method is able to optimize entity creation by avoiding costly archetype moves and
     * additional {@link Composition#inserted(IntConsumer)} invocations.     * 
     * 
     * @param builder composition spec
     * @param provider provider of components to add to matching entities
     */
    void initialize(Composition.Builder builder, EntityInitializer provider);

    <R, P extends DataProcessor<R>> CompositionData<P> createComposition(Composition.Builder builder, DataProcessorType<?, R, P> componentType);

}
