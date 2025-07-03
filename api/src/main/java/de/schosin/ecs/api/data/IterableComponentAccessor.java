package de.schosin.ecs.api.data;

public interface IterableComponentAccessor<R, P extends DataProcessor<R>> extends ComponentAccessor<R> {

    void process(IterableAccessor accessor, P processor);

}
