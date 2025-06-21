package de.schosin.ecs.engine.components.mappers;

import java.util.IdentityHashMap;
import java.util.Map;

import de.schosin.ecs.api.components.mappers.Components;
import de.schosin.ecs.api.data.DataAccessor;
import de.schosin.ecs.api.data.DataConverter;
import de.schosin.ecs.engine.components.ComponentMapperManager.PoolingComponents;
import de.schosin.ecs.storage.api.entities.Archetype;
import de.schosin.ecs.utils.collections.Bag;

public interface ComponentConverter<R> extends DataConverter<R> {

    static <R> ComponentConverter<R> indexed(int index) {
        return IndexedComponentConverter.getInstance(index);
    }

    static <R> ComponentConverter<R> pending(int componentId) {
        return PendingComponentConverter.getInstance(componentId);
    }

    static <R> ComponentConverter<R> wrapped(Components<?, R> mapper) {
        return WrappedComponentConverter.getInstance(mapper);
    }

    interface Factory<R> {
        ComponentConverter<R> getConverter(Archetype archetype);
    }

    @SuppressWarnings("rawtypes")
    public final class IndexedComponentConverter<R> implements ComponentConverter<R> {

        private static final Bag<IndexedComponentConverter> lookup = new Bag<>(IndexedComponentConverter.class, 8);

        private final int index;

        @SuppressWarnings("unchecked")
        synchronized static <R> ComponentConverter<R> getInstance(int index) {
            var result = lookup.getSafe(index);
            if (result == null) {
                result = new IndexedComponentConverter(index);
                lookup.set(index, result);
            }

            return result;
        }

        private IndexedComponentConverter(int index) {
            this.index = index;
        }

        public int getIndex() {
            return index;
        }

        @Override
        public R getComponent(DataAccessor accessor) {
            return accessor.getComponentByIndex(index);
        }

    }

    @SuppressWarnings("rawtypes")
    public final class PendingComponentConverter<R> implements ComponentConverter<R> {

        private static final Bag<PendingComponentConverter> lookup = new Bag<>(PendingComponentConverter.class, 8);

        private final int componentId;

        @SuppressWarnings("unchecked")
        synchronized static <R> ComponentConverter<R> getInstance(int componentId) {
            var result = lookup.getSafe(componentId);
            if (result == null) {
                result = new PendingComponentConverter(componentId);
                lookup.set(componentId, result);
            }

            return result;
        }

        private PendingComponentConverter(int componentId) {
            this.componentId = componentId;
        }

        @Override
        public R getComponent(DataAccessor accessor) {
            return accessor.getPendingComponent(componentId);
        }

    }

    @SuppressWarnings("rawtypes")
    public final class WrappedComponentConverter<R> implements ComponentConverter<R> {

        private static final Map<Components<?, ?>, WrappedComponentConverter> lookup = new IdentityHashMap<>();

        private final Components<?, ?> mapper;
        private final PoolingComponents<Object> pooling;

        @SuppressWarnings("unchecked")
        synchronized static <R> ComponentConverter<R> getInstance(Components<?, ?> mapper) {
            return lookup.computeIfAbsent(mapper, WrappedComponentConverter::new);
        }

        @SuppressWarnings("unchecked")
        private WrappedComponentConverter(Components<?, ?> mapper) {
            this.mapper = mapper;
            this.pooling = mapper instanceof PoolingComponents pooling ? pooling : null;
        }

        @Override
        @SuppressWarnings("unchecked")
        public R getComponent(DataAccessor accessor) {
            return (R) mapper.access(accessor);
        }

        @Override
        public void free(Object component) {
            if (pooling != null) {
                pooling.free(component);
            }
        }

    }

}
