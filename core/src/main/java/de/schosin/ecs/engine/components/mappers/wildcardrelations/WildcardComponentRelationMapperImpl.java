package de.schosin.ecs.engine.components.mappers.wildcardrelations;

import java.util.Arrays;
import java.util.Iterator;
import java.util.function.IntFunction;

import org.jspecify.annotations.NonNull;

import de.schosin.ecs.api.Pooled;
import de.schosin.ecs.api.components.Relation.ComponentRelation;
import de.schosin.ecs.api.components.Result.ComponentRelationResult;
import de.schosin.ecs.api.components.mappers.ComponentRelationMappers;
import de.schosin.ecs.api.components.mappers.ComponentRelationMappers.ComponentRelationMapper;
import de.schosin.ecs.api.components.mappers.ComponentRelationMappers.ExclusiveComponentRelationMapper;
import de.schosin.ecs.api.components.mappers.WildcardRelationMappers.WildcardComponentRelationMapper;
import de.schosin.ecs.api.data.DataAccessor;
import de.schosin.ecs.engine.components.ComponentMapperManager.PoolingComponents;
import de.schosin.ecs.engine.components.ComponentMapperManager.WildcardMapper;
import de.schosin.ecs.utils.collections.Bag;
import de.schosin.ecs.utils.collections.IntBag;
import de.schosin.ecs.utils.collections.Pool;

public final class WildcardComponentRelationMapperImpl<R, T> implements WildcardComponentRelationMapper<R, T>, PoolingComponents<ComponentRelationResult<R, T>>, WildcardMapper<ComponentRelationMappers<R, T, ?>> {

    private final IntFunction<DataAccessor> accessor;

    private final Bag<ComponentRelationMapper<R, T>> mappers;
    private final IntBag componentIds;

    @SuppressWarnings("rawtypes")
    private final Bag<ExclusiveComponentRelationMapper> exclusiveMappers;
    private final IntBag exclusiveComponentIds;

    private final Pool<ComponentRelationResultImpl> pool = Pool.unbounded(ComponentRelationResultImpl.class, ComponentRelationResultImpl::new);
    private final Bag<ComponentRelationResultImpl> lent = new Bag<>(ComponentRelationResultImpl.class, 8);

    public WildcardComponentRelationMapperImpl(IntFunction<DataAccessor> accessor) {
        this.accessor = accessor;

        this.mappers = new Bag<>(ComponentRelationMapper.class, 4);
        this.componentIds = new IntBag(4);

        this.exclusiveMappers = new Bag<>(ExclusiveComponentRelationMapper.class, 4);
        this.exclusiveComponentIds = new IntBag(4);
    }

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void addMapper(ComponentRelationMappers<R, T, ?> components) {
        switch (components) {
            case ComponentRelationMapper mapper -> {
                this.mappers.add(mapper);
                this.componentIds.add(components.componentId());
            }
            case ExclusiveComponentRelationMapper<?, ?> exclusiveMapper -> {
                this.exclusiveMappers.add(exclusiveMapper);
                this.exclusiveComponentIds.add(components.componentId());
            }
        }
    }

    @Override
    public void free(ComponentRelationResult<R, T> result) {
        if (result instanceof ComponentRelationResultImpl impl && this.lent.removeIdentity(impl)) {
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
        try (var accessor = this.accessor.apply(entityId)) {
            for (int i = 0, s = exclusiveComponentIds.getSize(); i < s; i++) {
                if (accessor.getComponent(exclusiveComponentIds.get(i)) != null) {
                    return true;
                }
            }

            for (int i = 0, s = componentIds.getSize(); i < s; i++) {
                if (accessor.getComponent(componentIds.get(i)) != null) {
                    return true;
                }
            }

            return false;
        }
    }

    @Override
    public ComponentRelationResult<R, T> get(int entityId) {
        return get(accessor.apply(entityId));
    }

    @Override
    public ComponentRelationResult<R, T> get(DataAccessor accessor) {
        var result = pool.getInstance().init(accessor);
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

        var exclusiveData = exclusiveMappers.getData();
        for (int i = 0, s = exclusiveMappers.getSize(); i < s; i++) {
            removed |= exclusiveData[i].remove(entityId);
        }

        return removed;
    }

    /**
     * {@link DataAccessor} based implementation. Calculates the necessary indexes in the first call to {@link #size()}.
     * 
     * <ul>
     * <li>
     * {@link #data}: Holds the componentIds for all indexes until {@link #size}, beginning with exclusive relations
     * </li>
     * <li>
     * {@link #dataIndex}: Holds the indexes for {@link ComponentRelationResult} and -1 for exclusive relations
     * </li>
     * <li>
     * {@link #accessor}: Currently assigned {@link DataAccessor}
     * </li>
     * <li>
     * {@link #exclusiveSize}: Holds the number of exclusive relations. These will be located at the beginning of {@link #data}
     * </li>
     * <li>
     * {@link #size}: Holds the number of all relations
     * </li>
     * <li>
     * {@link #id}: Current index for {@link Iterator} implementation
     * </li>
     * <ul> 
     */
    private class ComponentRelationResultImpl implements ComponentRelationResult<R, T>, Iterator<ComponentRelation<R, T>>, Pooled {

        private final IntBag data = new IntBag(4);
        private final IntBag dataIndex = new IntBag(4);

        private DataAccessor accessor;

        private int exclusiveSize = -1;
        private int size = -1;

        private int id = -1;

        private ComponentRelationResultImpl() {
            Arrays.fill(this.data.getData(), -1);
            Arrays.fill(this.dataIndex.getData(), -1);
        }

        public ComponentRelationResultImpl init(DataAccessor accessor) {
            this.accessor = accessor;

            return this;
        }

        @NonNull
        @Override
        public ComponentRelation<R, T> get(int i) {
            if (i >= size()) {
                throw new ArrayIndexOutOfBoundsException(i);
            }

            if (i < exclusiveSize) {
                return this.accessor.getComponent(this.data.get(i));
            }

            var componentId = this.data.get(i);
            var relations = this.accessor.<ComponentRelationResult<R, T>>getComponent(componentId);

            return relations.get(this.dataIndex.get(i));
        }

        @Override
        public int size() {
            if (size == -1) {
                this.exclusiveSize = 0;
                var size = 0;

                // Place exclusive relations at the beginning of data
                for (int i = 0; i < exclusiveComponentIds.getSize(); i++) {
                    var componentId = exclusiveComponentIds.get(i);

                    if (this.accessor.hasComponent(componentId)) {
                        this.exclusiveSize++;
                        this.data.add(componentId);
                        this.dataIndex.add(-1);
                    }
                }

                // Add non-exclusive relations after exclusive relations
                for (int i = 0; i < componentIds.getSize(); i++) {
                    var componentId = componentIds.get(i);

                    var relations = this.accessor.<ComponentRelationResult<R, T>>getComponent(componentId);
                    if (relations != null) {
                        for (int r = 0, rs = relations.size(); r < rs; r++) {
                            size++;
                            this.data.add(componentId);
                            this.dataIndex.add(r);
                        }
                    }

                }

                this.size = size + exclusiveSize;
            }

            return this.size;
        }

        @Override
        public boolean isEmpty() {
            return size() == 0;
        }

        @Override
        public Iterator<ComponentRelation<R, T>> iterator() {
            this.id = 0;
            return this;
        }

        @Override
        public boolean hasNext() {
            return id < size();
        }

        @Override
        public ComponentRelation<R, T> next() {
            return get(id++);
        }

        @Override
        public R getRelationship(T target) {
            return null;
        }

        @Override
        public void reset() {
            this.data.clear();
            Arrays.fill(this.data.getData(), -1);

            this.dataIndex.clear();
            Arrays.fill(this.dataIndex.getData(), -1);

            this.accessor = null;

            this.exclusiveSize = -1;
            this.size = -1;

            this.id = -1;
        }

    }

}
