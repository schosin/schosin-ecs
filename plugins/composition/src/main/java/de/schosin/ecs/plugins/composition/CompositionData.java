package de.schosin.ecs.plugins.composition;

import de.schosin.ecs.codegen.EcsCodegen;
import de.schosin.ecs.plugins.data.types.DataProcessor;

@EcsCodegen
public interface CompositionData<P extends DataProcessor<?>> extends Composition {

    void inserted(P processor);

    void removed(P processor);

    void process(P processor);

    void process(int entityId, P processor);

}
