package de.schosin.ecs.plugins.experimental.system.systems;

import de.schosin.ecs.api.components.ComponentSet;
import de.schosin.ecs.api.components.types.ComponentSetType;
import de.schosin.ecs.api.components.types.ComponentType;
import de.schosin.ecs.api.data.DataProcessor;
import de.schosin.ecs.engine.utils.components.ComponentSetsHelper;
import de.schosin.ecs.plugins.composition.Composition;
import de.schosin.ecs.plugins.composition.CompositionPlugin;
import de.schosin.ecs.plugins.composition.CompositionSet;

/**
 * Iterating system based on {@link ComponentSetType} and its {@link DataProcessor}.
 * 
 * <p>
 * An implementation of this type must implement the nested type Processor on the {@link ComponentSet}, otherwise a
 * {@link IllegalStateException} is thrown during construction.
 * 
 * {@snippet:
 * class MySystem extends CompositionSystem<MyComponentSet, MyComponentSet.Processor> implements MyComponentSet.Processor {
 *     // constructor, implementation
 * }
 * }
 * </p>
 * 
 * <p>
 * To use {@link CompositionSet#inserted(DataProcessor)} and {@link CompositionSet#removed(DataProcessor)}, you can pass a method
 * reference to {@code this:composition} in the constructor. The signature must match the {@link DataProcessor} signature.
 * 
 * {@snippet:
 * public MySystem(DefaultWorld world) {
 *     super(world, MyComponentSet.TYPE);
 *     
 *     this.composition.inserted(this::handleInserted);
 * }
 * 
 * // signature depends on component set
 * private void handleInserted(int entityId, Position position, Velocity velocity) {
 *     // logic
 * }
 * }
 * </p>
 * 
 * @param <T> type of component set
 * @param <P> type of component set processor
 */
public abstract class ComponentSetSystem<T extends ComponentSet<P>, P extends DataProcessor<T>> extends AbstractSystem {

    private final P processor;
    protected final CompositionSet<P> composition;

    public ComponentSetSystem(CompositionPlugin world, ComponentSetType<T, P> componentType) {
        this(world, componentType, buildComposition(world, componentType));
    }

    @SuppressWarnings("unchecked")
    public ComponentSetSystem(CompositionPlugin world, ComponentSetType<T, P> componentType, Composition.Builder builder) {
        try {
            this.processor = (P) this;
        } catch (ClassCastException ex) {
            throw new IllegalStateException("When extending CompositionSystem, the implementing system must implement the DataProcessor");
        }

        this.composition = world.createComposition(builder, componentType);
    }

    private static <T extends ComponentSet<P>, P extends DataProcessor<T>> Composition.Builder buildComposition(CompositionPlugin world, ComponentSetType<T, P> componentType) {
        var componentTypes = ComponentSetsHelper.getData(componentType.componentSet()).components().stream()
                .<ComponentType<?, ?>>map(ComponentSet.ComponentData::type)
                .toArray(ComponentType<?, ?>[]::new);

        return Composition.all(componentTypes);
    }

    @Override
    protected final void processSystem() {
        this.composition.process(processor);
    }

}
