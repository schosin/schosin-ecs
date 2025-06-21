package de.schosin.ecs.plugins.data.types;

import de.schosin.ecs.api.Pooled;
import de.schosin.ecs.codegen.EcsCodegen;

@EcsCodegen
public interface BaseData extends Pooled {

    Object getComponent(int i);

    void free();

}
