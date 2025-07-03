package de.schosin.ecs.api.entities;

import java.util.function.Consumer;
import java.util.function.ObjIntConsumer;
import java.util.stream.Stream;

import de.schosin.ecs.api.components.ComponentSet;
import de.schosin.ecs.api.components.types.ComponentType;
import de.schosin.ecs.api.components.types.DataProcessorType;
import de.schosin.ecs.api.data.DataProcessor;

public interface ImmutableEntityBag extends Iterable<Entity> {

    int size();

    boolean isEmpty();

    boolean contains(int entityId);

    Entity get(int index);

    void process(Consumer<Entity> consumer);

    void process(ObjIntConsumer<Entity> consumer);

    Stream<Entity> stream();

    /**
     * Returns a view of the bag that supports efficient {@link ImmutableProcessableBag#process(DataProcessor) processing}
     * of the entities and their components.
     * 
     * <p>
     * Intended for use with {@link ComponentSet component sets} and data types ({@code ecs-plugins-data-types}).
     * </p>
     * 
     * @param <R> type of component type
     * @param <P> type of processor
     * @param componentType component type
     * @return processable bag
     */
    <R, P extends DataProcessor<R>> ImmutableProcessableBag<P> forType(DataProcessorType<?, R, P> componentType);

    /**
     * Returns a view of the bag that supports efficient {@link ImmutableProcessableBag#process(DataProcessor) processing}
     * of the entities and their components.
     * 
     * @param <R> type of component type
     * @param componentType component type
     * @return processable bag
     */
    <R> ImmutableProcessableBag<DataProcessor<R>> forType(ComponentType<?, R> componentType);

}
