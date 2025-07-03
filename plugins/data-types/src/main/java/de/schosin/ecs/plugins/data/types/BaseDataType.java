package de.schosin.ecs.plugins.data.types;

import de.schosin.ecs.api.components.types.ComponentType;
import de.schosin.ecs.api.components.types.CustomDataProcessorType;
import de.schosin.ecs.api.data.DataProcessor;
import de.schosin.ecs.api.data.DataProvider;
import de.schosin.ecs.api.data.DataProviderType;
import de.schosin.ecs.codegen.EcsCodegen;
import de.schosin.ecs.plugins.data.mappers.DataTypeMapper;

@EcsCodegen
public interface BaseDataType<T extends Data, S extends DataProvider<T>, R extends Data, P extends DataProcessor<R>>
        extends DataProviderType<T, S>, CustomDataProcessorType<T, R, DataTypeMapper<T, R>, P> {

    ComponentType<?, ?>[] getComponentTypes();

}
