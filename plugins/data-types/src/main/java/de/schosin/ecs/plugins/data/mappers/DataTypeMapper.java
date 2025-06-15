package de.schosin.ecs.plugins.data.mappers;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.function.IntFunction;

import de.schosin.ecs.api.components.mappers.Components;
import de.schosin.ecs.api.components.mappers.CustomComponents;
import de.schosin.ecs.api.components.types.ComponentType;
import de.schosin.ecs.api.data.DataAccessor;
import de.schosin.ecs.codegen.EcsCodegen;
import de.schosin.ecs.engine.components.ComponentMapperManager;
import de.schosin.ecs.engine.components.ComponentMapperManager.PoolingComponents;
import de.schosin.ecs.plugins.data.types.Data;
import de.schosin.ecs.plugins.data.types.DataType;
import de.schosin.ecs.utils.collections.Bag;
import de.schosin.ecs.utils.collections.Pool;

@EcsCodegen
public class DataTypeMapper<T extends Data, R extends Data> implements CustomComponents<T, R>, PoolingComponents<T> {

    private final IntFunction<DataAccessor> accessor;

    private final DataType<T, ?, R, ?> dataType;
    private final Components<?, ?>[] mappers;
    private final int size;

    private final Bag<Data> lent;
    private final Pool<Object[]> pool;

    public DataTypeMapper(DataType<T, ?, R, ?> dataType, ComponentMapperManager componentMapperManager, IntFunction<DataAccessor> accessor) {
        this.accessor = accessor;
        this.dataType = dataType;

        this.mappers = Arrays.stream(dataType.getClass().getRecordComponents())
                .map(this::getComponentType)
                .map(componentMapperManager::getComponents)
                .toArray(Components<?, ?>[]::new);

        this.size = mappers.length;

        this.lent = new Bag<>(Data.class, 8);
        this.pool = Pool.unbounded(Object[].class, () -> new Object[size], array -> Arrays.fill(array, null));
    }

    private ComponentType<?, ?> getComponentType(RecordComponent component) {
        try {
            return (ComponentType<?, ?>) component.getAccessor().invoke(dataType);
        } catch (IllegalAccessException | InvocationTargetException ex) {
            throw new IllegalStateException("Failed to extract record component '%s' as ComponentType: %s".formatted(component.getName(), ex.getMessage()), ex);
        }
    }

    @Override
    public void free(T result) {
        if (lent.removeIdentity(result)) {
            freeData(result);
        }
    }

    @Override
    public void reclaim() {
        var data = lent.getData();
        for (int i = 0, s = lent.getSize(); i < s; i++) {
            freeData(data[i]);
        }

        lent.clear();
    }

    @SuppressWarnings("unchecked")
    private void freeData(Data result) {
        var components = result.getComponents();
        for (int i = 0; i < size; i++) {
            var mapper = mappers[i];
            if (mapper instanceof PoolingComponents pooling) {
                pooling.free(components.get(i));
            }
        }

        result.free();
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
        return get(accessor.apply(entityId));
    }

    @Override
    public R get(DataAccessor accessor) {
        var components = pool.getInstance();
        var found = false;

        for (int i = 0; i < size; i++) {
            var mapper = mappers[i];

            var component = components[i] = mapper.get(accessor);
            if (component != null) {
                found = true;
            }
        }

        if (!found) {
            pool.free(components);
            return null;
        }

        var result = DataTypeMapperHelper.getInstance(dataType, components);
        lent.add(result);

        pool.free(components);
        return result;
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
