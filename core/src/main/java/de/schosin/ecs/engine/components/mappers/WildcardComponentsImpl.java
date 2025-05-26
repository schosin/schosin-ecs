package de.schosin.ecs.engine.components.mappers;

import java.util.Iterator;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import de.schosin.ecs.api.Pooled;
import de.schosin.ecs.api.components.Result.ComponentResult;
import de.schosin.ecs.api.components.mappers.ComponentMapper;
import de.schosin.ecs.api.components.mappers.WildcardComponents;
import de.schosin.ecs.api.components.types.Wildcard;
import de.schosin.ecs.engine.BagManager;
import de.schosin.ecs.engine.components.ComponentMapperManager.PoolingComponents;
import de.schosin.ecs.engine.components.ComponentMapperManager.WildcardMapper;
import de.schosin.ecs.utils.collections.Bag;
import de.schosin.ecs.utils.collections.BagIterator;
import de.schosin.ecs.utils.collections.Pool;

public class WildcardComponentsImpl<T> implements WildcardComponents<T>, PoolingComponents<ComponentResult<T>>, WildcardMapper<ComponentMapper<? extends T>> {

    private final Wildcard<T> type;
    private final Bag<ComponentMapper<? extends T>> mappers;

    private final Pool<WildcardComponentResultImpl<T>> pool = Pool.unbounded(WildcardComponentResultImpl.class, this::createResultInstance);
    private final Bag<WildcardComponentResultImpl<T>> lent = new Bag<>(WildcardComponentResultImpl.class, 8);

    public WildcardComponentsImpl(Wildcard<T> type, BagManager bagManager) {
        this.type = type;
        this.mappers = bagManager.createComponentBag(ComponentMapper.class);
    }

    @Override
    public void addMapper(ComponentMapper<? extends T> mapper) {
        this.mappers.add(mapper);
    }

    @Override
    public void free(ComponentResult<T> result) {
        if (result instanceof WildcardComponentResultImpl<T> impl) {
            this.lent.removeIdentity(impl);
            this.pool.free(impl);
        }
    }

    @Override
    public void reclaim() {
        var data = lent.getData();
        for (int i = 0, s = lent.getSize(); i < s; i++) {
            pool.free(data[i]);
        }

        lent.clear();
    }

    @Override
    public boolean has(int entityId) {
        var data = mappers.getData();
        for (int i = 0, s = mappers.getSize(); i < s; i++) {
            if (data[i].has(entityId)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public ComponentResult<T> get(int entityId) {
        var result = pool.getInstance().init(entityId);
        lent.add(result);

        return result;
    }

    @Override
    public boolean remove(int entityId) {
        var removed = false;

        var data = mappers.getData();
        for (int i = 0, s = mappers.getSize(); i < s; i++) {
            removed |= data[i].remove(entityId);
        }

        return removed;
    }

    private WildcardComponentResultImpl<T> createResultInstance() {
        return new WildcardComponentResultImpl<>(type.bound(), mappers);
    }

    private static class WildcardComponentResultImpl<T> implements ComponentResult<T>, Pooled {

        private final Class<T> clazz;
        private final Bag<ComponentMapper<? extends T>> mappers;
        private final Bag<T> components;

        private final ThreadLocal<BagIterator<T>> iterator = ThreadLocal.withInitial(BagIterator::new);

        private int entityId = -1;
        private int size = -1;

        public WildcardComponentResultImpl(Class<T> clazz, Bag<ComponentMapper<? extends T>> mappers) {
            this.clazz = clazz;
            this.mappers = mappers;
            this.components = new Bag<>(clazz, 4);
        }

        public WildcardComponentResultImpl<T> init(int entityId) {
            this.entityId = entityId;

            return this;
        }

        @NonNull
        @Override
        public T get(int i) {
            if (i >= size()) {
                throw new ArrayIndexOutOfBoundsException(i);
            }

            return this.components.get(i);
        }

        @Nullable
        @Override
        public <R extends T> R get(Class<R> clazz) {
            var s = size(); // initializes components, must be done before getData

            var data = components.getData();
            for (int i = 0; i < s; i++) {
                var component = data[i];
                if (clazz == component.getClass()) {
                    return clazz.cast(component);
                }
            }

            return null;
        }

        @Override
        public int size() {
            if (size == -1) {
                var data = mappers.getData();
                for (int i = 0, s = mappers.getSize(); i < s; i++) {
                    var component = data[i].get(entityId);
                    if (component != null) {
                        components.add(clazz.cast(component));
                    }
                }

                this.size = components.getSize();
            }

            return size;
        }

        @Override
        public boolean isEmpty() {
            return size() == 0;
        }

        @Override
        public Iterator<T> iterator() {
            size();
            return iterator.get().init(this.components);
        }

        @Override
        public void reset() {
            this.entityId = -1;
            this.size = -1;

            this.components.clear();
        }

    }

}
