package de.schosin.ecs.plugins.data.mappers;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.RecordComponent;
import java.util.Arrays;

import de.schosin.ecs.api.components.mappers.Components;
import de.schosin.ecs.api.components.mappers.CustomComponents;
import de.schosin.ecs.api.components.types.ComponentType;
import de.schosin.ecs.codegen.EcsCodegen;
import de.schosin.ecs.engine.components.ComponentMapperManager;
import de.schosin.ecs.engine.components.ComponentMapperManager.PoolingComponents;
import de.schosin.ecs.plugins.data.types.BaseDataType.Data;
import de.schosin.ecs.plugins.data.types.DataType;
import de.schosin.ecs.utils.collections.Bag;
import de.schosin.ecs.utils.collections.Pool;

@EcsCodegen
public class DataTypeMapper<T extends Data, R extends Data> implements CustomComponents<T, R>, PoolingComponents<T> {

    private final DataType<T, ?, R, ?> dataType;
    private final Components<?, ?>[] mappers;
    private final int size;

    private final Bag<Data> lent;
    private final Pool<Object[]> pool;

    public DataTypeMapper(DataType<T, ?, R, ?> dataType, ComponentMapperManager componentMapperManager) {
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
        lent.removeIdentity(result);
        result.free();
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
    @SuppressWarnings("unchecked")
    public R get(int entityId) {
        return pool.withInstance(components -> {
            var found = false;

            for (int i = 0; i < size; i++) {
                var mapper = mappers[i];

                var component = components[i] = mapper.get(entityId);
                if (component != null) {
                    found = true;
                }
            }

            if (!found) {
                return null;
            }

            var result = (R) DataTypeMapperHelper.getInstance(dataType, components);
            lent.add(result);

            return result;
        });
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
