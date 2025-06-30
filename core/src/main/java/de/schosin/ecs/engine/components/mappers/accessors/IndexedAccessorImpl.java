package de.schosin.ecs.engine.components.mappers.accessors;

import de.schosin.ecs.api.Pooled;
import de.schosin.ecs.api.data.ComponentAccessor;
import de.schosin.ecs.api.data.DataAccessor;
import de.schosin.ecs.utils.collections.Pool;

public class IndexedAccessorImpl<R> implements ComponentAccessor<R>, Pooled {

    private final Pool<IndexedAccessorImpl<R>> pool;

    private int componentIndex = -1;

    public IndexedAccessorImpl(Pool<IndexedAccessorImpl<R>> pool) {
        this.pool = pool;
    }

    public IndexedAccessorImpl<R> init(int componentIndex) {
        this.componentIndex = componentIndex;

        return this;
    }

    @Override
    public R getComponent(DataAccessor accessor) {
        return accessor.getComponentByIndex(componentIndex);
    }

    @Override
    public void free() {
        pool.free(this);
    }

    @Override
    public void reset() {
        this.componentIndex = -1;
    }

}
