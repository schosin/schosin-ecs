package de.schosin.ecs.plugins.data.types;

import de.schosin.ecs.api.Pooled;
import de.schosin.ecs.api.components.types.ComponentType;
import de.schosin.ecs.api.components.types.CustomComponentType;
import de.schosin.ecs.codegen.EcsCodegen;
import de.schosin.ecs.plugins.data.mappers.DataTypeMapper;
import de.schosin.ecs.plugins.data.types.BaseDataType.Data;
import de.schosin.ecs.utils.collections.ImmutableBag;

@EcsCodegen
public interface BaseDataType<T extends Data, S extends DataProvider<T>, R extends Data, P extends DataProcessor<R>>
        extends DataProviderType<T, S>, DataProcessorType<R, P>, CustomComponentType<T, R, DataTypeMapper<T, R>> {

    ComponentType<?, ?>[] getComponentTypes();

    interface Data extends Pooled {

        ImmutableBag<Object> getComponents();

        void free();

    }

}
