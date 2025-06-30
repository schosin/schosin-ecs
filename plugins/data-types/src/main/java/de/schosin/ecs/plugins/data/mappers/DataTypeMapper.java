package de.schosin.ecs.plugins.data.mappers;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.function.IntFunction;

import de.schosin.ecs.api.components.mappers.Components;
import de.schosin.ecs.api.components.mappers.CustomComponentMapper;
import de.schosin.ecs.api.components.types.ComponentType;
import de.schosin.ecs.api.data.ComponentAccessor;
import de.schosin.ecs.api.data.DataAccessor;
import de.schosin.ecs.codegen.EcsCodegen;
import de.schosin.ecs.engine.components.ComponentMapperManager;
import de.schosin.ecs.engine.components.ComponentMapperManager.ReclaimingComponents;
import de.schosin.ecs.plugins.data.types.Data;
import de.schosin.ecs.plugins.data.types.DataType;
import de.schosin.ecs.utils.collections.Bag;

@EcsCodegen
public class DataTypeMapper<T extends Data, R extends Data> implements CustomComponentMapper<T, R>, ReclaimingComponents {

    private final IntFunction<DataAccessor> accessor;

    private final DataType<T, ?, R, ?> dataType;
    private final Components<?, ?>[] mappers;
    private final int size;

    private final Bag<ComponentAccessor<R>> lent = new Bag<>(ComponentAccessor.class, 8);

    public DataTypeMapper(DataType<T, ?, R, ?> dataType, ComponentMapperManager componentMapperManager, IntFunction<DataAccessor> accessor) {
        this.accessor = accessor;
        this.dataType = dataType;

        this.mappers = Arrays.stream(dataType.getClass().getRecordComponents())
                .map(this::getComponentType)
                .map(componentMapperManager::getComponents)
                .toArray(Components<?, ?>[]::new);

        this.size = mappers.length;
    }

    private ComponentType<?, ?> getComponentType(RecordComponent component) {
        try {
            return (ComponentType<?, ?>) component.getAccessor().invoke(dataType);
        } catch (IllegalAccessException | InvocationTargetException ex) {
            throw new IllegalStateException("Failed to extract record component '%s' as ComponentType: %s".formatted(component.getName(), ex.getMessage()), ex);
        }
    }

    @Override
    public void reclaim() {
        var data = lent.getData();
        for (int i = 0, s = lent.getSize(); i < s; i++) {
            data[i].free();
        }

        lent.clear();
    }

    @Override
    public boolean has(int entityId) {
        for (int i = 0; i < size; i++) {
            var mapper = mappers[i];

            if (mapper.has(entityId)) {
                return true;
            }
        }

        return false;
    }

    public boolean hasAll(int entityId) {
        for (int i = 0; i < size; i++) {
            var mapper = mappers[i];

            if (!mapper.has(entityId)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public R get(int entityId) {
        var accessor = this.accessor.apply(entityId);

        var componentAccessor = getComponentAccessor(accessor);
        lent.add(componentAccessor);

        return componentAccessor.getComponent(accessor);
    }

    @Override
    public ComponentAccessor<R> getComponentAccessor(DataAccessor accessor) {
        return DataTypeMapperHelper.getComponentAccessor(dataType, mappers, accessor);
    }

    @Override
    public boolean remove(int entityId) {
        var removed = false;

        for (int i = 0; i < size; i++) {
            var mapper = mappers[i];

            removed |= mapper.remove(entityId);
        }

        return removed;
    }

}
