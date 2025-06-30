package de.schosin.ecs.engine.components.mappers;

import java.util.Arrays;
import java.util.Iterator;
import java.util.function.IntFunction;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import de.schosin.ecs.api.Pooled;
import de.schosin.ecs.api.components.Result.ComponentResult;
import de.schosin.ecs.api.components.mappers.ComponentMapper;
import de.schosin.ecs.api.components.mappers.WildcardComponentMapper;
import de.schosin.ecs.api.data.ComponentAccessor;
import de.schosin.ecs.api.data.DataAccessor;
import de.schosin.ecs.engine.components.ComponentMapperManager.ReclaimingComponents;
import de.schosin.ecs.engine.components.ComponentMapperManager.WildcardMapper;
import de.schosin.ecs.utils.collections.Bag;
import de.schosin.ecs.utils.collections.IntBag;
import de.schosin.ecs.utils.collections.Pool;

public final class WildcardComponentMapperImpl<T> implements WildcardComponentMapper<T>, ReclaimingComponents, WildcardMapper<ComponentMapper<? extends T>> {

    private final IntFunction<DataAccessor> accessor;

    private final Bag<ComponentMapper<? extends T>> mappers;
    private final IntBag componentIds;
    private final Bag<Class<? extends T>> classes;

    private final Pool<WildcardComponentResultImpl> pool = Pool.unbounded(WildcardComponentResultImpl.class, WildcardComponentResultImpl::new);
    private final Bag<WildcardComponentResultImpl> lent = new Bag<>(WildcardComponentResultImpl.class, 8);

    public WildcardComponentMapperImpl(IntFunction<DataAccessor> accessor) {
        this.accessor = accessor;

        this.mappers = new Bag<>(ComponentMapper.class, 4);
        this.componentIds = new IntBag(4);
        this.classes = new Bag<>(Class.class, 4);
    }

    @Override
    public void addMapper(ComponentMapper<? extends T> mapper) {
        this.mappers.add(mapper);
        this.componentIds.add(mapper.componentId());
        this.classes.add(mapper.componentType().clazz());
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
        try (var accessor = this.accessor.apply(entityId)) {
            for (int i = 0, s = componentIds.getSize(); i < s; i++) {
                if (accessor.getComponent(componentIds.get(i)) != null) {
                    return true;
                }
            }

            return false;
        }
    }

    @Override
    public ComponentResult<T> get(int entityId) {
        var accessor = this.accessor.apply(entityId);

        var componentAccessor = getComponentAccessor(accessor);
        lent.add(componentAccessor);

        return componentAccessor.getComponent(accessor);
    }

    @Override
    public WildcardComponentResultImpl getComponentAccessor(DataAccessor accessor) {
        return pool.getInstance().init(accessor);
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

    private final class WildcardComponentResultImpl implements ComponentResult<T>, Iterator<T>, ComponentAccessor<ComponentResult<T>>, Pooled {

        private final IntBag data = new IntBag(4);

        private DataAccessor accessor;

        private int size = -1;
        private int id = -1;

        private WildcardComponentResultImpl() {
            Arrays.fill(this.data.getData(), -1);
        }

        public WildcardComponentResultImpl init(DataAccessor accessor) {
            this.accessor = accessor;

            return this;
        }

        @Override
        public ComponentResult<T> getComponent(DataAccessor accessor) {
            reset();
            this.accessor = accessor;

            return this;
        }

        @Override
        public void free() {
            pool.free(this);
        }

        @NonNull
        @Override
        public T get(int i) {
            if (i >= size()) {
                throw new ArrayIndexOutOfBoundsException(i);
            }

            return this.accessor.getComponent(this.data.get(i));
        }

        @Nullable
        @Override
        @SuppressWarnings("unchecked")
        public <R extends T> R get(Class<R> clazz) {
            for (int i = 0, s = classes.getSize(); i < s; i++) {
                if (classes.get(i) == clazz) {
                    return (R) this.accessor.getComponent(componentIds.get(i));
                }
            }

            return null;
        }

        @Override
        public int size() {
            if (this.size == -1) {
                this.size = 0;

                for (int i = 0; i < componentIds.getSize(); i++) {
                    var componentId = componentIds.get(i);

                    if (this.accessor.hasComponent(componentId)) {
                        this.size++;
                        this.data.add(componentId);
                    }
                }
            }

            return this.size;
        }

        @Override
        public boolean isEmpty() {
            return size() == 0;
        }

        @Override
        public Iterator<T> iterator() {
            this.id = 0;
            return this;
        }

        @Override
        public boolean hasNext() {
            return id < size();
        }

        @Override
        public T next() {
            return get(id++);
        }

        @Override
        public void reset() {
            this.data.clear();
            Arrays.fill(this.data.getData(), -1);

            this.accessor = null;

            this.size = -1;
            this.id = -1;
        }

        @Override
        public String toString() {
            if (accessor == null) {
                return "ComponentResult(invalidated)";
            }

            var builder = new StringBuilder().append("ComponentResult(");
            for (int i = 0, s = size(); i < s; i++) {
                if (i > 0) {
                    builder.append(", ");
                }

                builder.append(get(i));
            }
            return builder.append(")").toString();
        }

    }

}
