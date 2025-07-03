package de.schosin.ecs.plugins.composition;

import de.schosin.ecs.api.data.DataProcessor;
import de.schosin.ecs.api.data.Processable;
import de.schosin.ecs.codegen.EcsCodegen;

@EcsCodegen
public interface CompositionData<P extends DataProcessor<?>> extends Composition, Processable<P> {

    void inserted(P processor);

    void removed(P processor);

    void process(int entityId, P processor);

}
