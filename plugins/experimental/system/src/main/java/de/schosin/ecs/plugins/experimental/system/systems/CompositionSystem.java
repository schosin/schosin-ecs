package de.schosin.ecs.plugins.experimental.system.systems;

import de.schosin.ecs.api.data.DataProcessor;
import de.schosin.ecs.plugins.composition.CompositionData;

/**
 * Iterating system based on {@link CompositionData} and its {@link DataProcessor}.
 * 
 * <p>
 * An implementation of this type must implement the {@link DataProcessor} passed to the type variable {@code P}, otherwise a
 * {@link IllegalStateException} is thrown during construction.
 * 
 * {@snippet:
 * class MySystem extends CompositionSystem<DataType2.Processor2<Position, Velocity>> implements DataType2.Processor2<Position, Velocity> {
 *     // constructor, implementation
 * }
 * }
 * </p>
 * 
 * <p>
 * To use {@link CompositionData#inserted(DataProcessor)} and {@link CompositionData#removed(DataProcessor)}, you can pass a method
 * reference to {@code this:composition} in the constructor. The signature must match the {@link DataProcessor} signature.
 * 
 * {@snippet:
 * public MySystem(DefaultWorld world) {
 *     super(world.createComposition(Position.class, Velocity.class));
 *     
 *     this.composition.inserted(this::handleInserted);
 * }
 * 
 * private void handleInserted(int entityId, Position position, Velocity velocity) {
 *     // logic
 * }
 * }
 * </p>
 * 
 * @param <P> type of {@link DataProcessor} for composition
 */
public abstract class CompositionSystem<P extends DataProcessor<?>> extends AbstractSystem {

    private final P processor;
    protected final CompositionData<P> composition;

    @SuppressWarnings("unchecked")
    public CompositionSystem(CompositionData<P> composition) {
        try {
            this.processor = (P) this;
        } catch (ClassCastException ex) {
            throw new IllegalStateException("When extending CompositionSystem, the implementing system must implement the DataProcessor");
        }

        this.composition = composition;
    }

    @Override
    protected final void processSystem() {
        this.composition.process(processor);
    }

}
