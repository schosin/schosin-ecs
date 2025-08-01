package de.schosin.ecs.plugins.data;

import java.util.function.IntFunction;

import de.schosin.ecs.api.World;
import de.schosin.ecs.engine.components.ComponentMapperManager;
import de.schosin.ecs.engine.entities.EntityManager;
import de.schosin.ecs.plugins.data.mappers.DataTypeMapper;
import de.schosin.ecs.plugins.data.types.Data;
import de.schosin.ecs.plugins.data.types.DataType;

public class DataTypeManager implements DataTypePlugin {

    private final EntityManager entityManager;
    private final ComponentMapperManager componentMapperManager;

    public DataTypeManager(World world) {
        world.addSingleton(this);

        this.entityManager = world.getSingleton(EntityManager.class);
        this.componentMapperManager = world.getSingleton(ComponentMapperManager.class);

        for (var dataType : DataType.class.getPermittedSubclasses()) {
            componentMapperManager.registerCustomComponentType(dataType.asSubclass(DataType.class), this::createDataTypeMapper);
        }
    }

    private DataTypeMapper<?, ?> createDataTypeMapper(DataType<?, ?, ?> dataType) {
        return new DataTypeMapper<>(dataType, componentMapperManager, entityManager::getAccessor);
    }

    @Override
    public <R extends Data> IntFunction<R> getData(DataType<?, R, ?> type) {
        var mapper = componentMapperManager.getComponents(type);

        return mapper::get;
    }

}
