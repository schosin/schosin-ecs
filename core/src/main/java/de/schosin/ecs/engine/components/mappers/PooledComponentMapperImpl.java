package de.schosin.ecs.engine.components.mappers;

import org.jspecify.annotations.NonNull;

import de.schosin.ecs.api.Pooled;
import de.schosin.ecs.api.components.mappers.ComponentMapper.PooledComponentMapper;
import de.schosin.ecs.engine.components.TransmutationManager;
import de.schosin.ecs.storage.api.components.Component.PooledComponentData;

public class PooledComponentMapperImpl<T extends Pooled> extends ComponentMapperImpl<T> implements PooledComponentMapper<T> {

    private final PooledComponentData<T> data;

    public PooledComponentMapperImpl(PooledComponentData<T> data, TransmutationManager transmutationManager) {
        super(data, transmutationManager);

        this.data = data;
    }

    @NonNull
    @Override
    public T add(int entityId) {
        var component = get(entityId);
        if (component != null) {
            return component;
        }

        return add(entityId, getInstance());
    }

    @Override
    public T getInstance() {
        return data.getInstance();
    }

}
