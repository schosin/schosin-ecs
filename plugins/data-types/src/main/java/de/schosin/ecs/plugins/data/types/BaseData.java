package de.schosin.ecs.plugins.data.types;

import de.schosin.ecs.api.Pooled;
import de.schosin.ecs.codegen.EcsCodegen;
import de.schosin.ecs.utils.collections.ImmutableBag;

@EcsCodegen
public interface BaseData extends Pooled {

    ImmutableBag<Object> getComponents();

    void free();

}
