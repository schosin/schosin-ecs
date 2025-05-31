package de.schosin.ecs.plugins.composition;

import de.schosin.ecs.api.data.DataProcessor;
import de.schosin.ecs.codegen.EcsCodegen;

@EcsCodegen
public interface CompositionData<P extends DataProcessor<?>> extends Composition {

    void inserted(P processor);

    void removed(P processor);

    void process(P processor);

    void process(int entityId, P processor);

}
