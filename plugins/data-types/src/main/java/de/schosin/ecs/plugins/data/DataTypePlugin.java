package de.schosin.ecs.plugins.data;

import java.util.function.IntFunction;

import de.schosin.ecs.api.Plugin;
import de.schosin.ecs.plugins.data.types.BaseDataType.Data;
import de.schosin.ecs.plugins.data.types.DataType;

@Plugin(DataTypeManager.class)
public interface DataTypePlugin {

    <R extends Data> IntFunction<R> getData(DataType<?, ?, R, ?> type);

}
