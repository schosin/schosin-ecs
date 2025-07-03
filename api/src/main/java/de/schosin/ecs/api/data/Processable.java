package de.schosin.ecs.api.data;

public interface Processable<P extends DataProcessor<?>> {

    void process(P processor);

}
