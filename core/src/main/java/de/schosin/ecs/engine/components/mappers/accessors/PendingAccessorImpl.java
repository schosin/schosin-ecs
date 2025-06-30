package de.schosin.ecs.engine.components.mappers.accessors;

import de.schosin.ecs.api.data.ComponentAccessor;
import de.schosin.ecs.api.data.DataAccessor;
import de.schosin.ecs.utils.collections.Pool;

public class PendingAccessorImpl<R> implements ComponentAccessor<R> {

    private final int componentId;
    private final Pool<PendingAccessorImpl<R>> pool;

    public PendingAccessorImpl(int componentId, Pool<PendingAccessorImpl<R>> pool) {
        this.componentId = componentId;
        this.pool = pool;
    }

    @Override
    public R getComponent(DataAccessor accessor) {
        return accessor.getPendingComponent(componentId);
    }

    @Override
    public void free() {
        pool.free(this);
    }

}
