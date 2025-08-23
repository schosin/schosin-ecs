package de.schosin.ecs.engine.components.mappers.accessors;

import de.schosin.ecs.api.data.ComponentAccessor;
import de.schosin.ecs.api.data.DataAccessor;
import de.schosin.ecs.utils.collections.Bag;

@SuppressWarnings("rawtypes")
public record IndexedAccessorImpl(int componentIndex) implements ComponentAccessor {

    private static final Bag<IndexedAccessorImpl> INSTANCES = new Bag<>(IndexedAccessorImpl.class, 32);

    public IndexedAccessorImpl {
        // this check allows the JIT the remove the branching in DataAccessorgetComponentByIndex
        if (componentIndex < 0) {
            throw new IllegalArgumentException("componentIndex must not be negative, but was: " + componentIndex);
        }
    }

    @SuppressWarnings("unchecked")
    public static <R> ComponentAccessor<R> getInstance(int componentIndex) {
        var result = INSTANCES.getSafe(componentIndex);
        if (result != null) {
            return result;
        }

        synchronized (INSTANCES) {
            result = INSTANCES.getSafe(componentIndex);
            if (result != null) {
                return result;
            }

            result = new IndexedAccessorImpl(componentIndex);
            INSTANCES.set(componentIndex, result);

            return result;
        }
    }

    @Override
    public Object getComponent(DataAccessor accessor) {
        return accessor.getComponentByIndex(componentIndex);
    }

}
