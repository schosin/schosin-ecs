package de.schosin.ecs.plugins.data.types;

import de.schosin.ecs.api.components.types.ComponentType;
import de.schosin.ecs.api.components.types.CustomDataProcessorType;
import de.schosin.ecs.api.data.DataProcessor;
import de.schosin.ecs.codegen.EcsCodegen;
import de.schosin.ecs.plugins.data.mappers.DataTypeMapper;

@EcsCodegen
public interface BaseDataType<T extends Data, R extends Data, P extends DataProcessor<R>> extends CustomDataProcessorType<T, R, DataTypeMapper<T, R>, P> {

    ComponentType<?, ?>[] getComponentTypes();

}
