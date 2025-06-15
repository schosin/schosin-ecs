package de.schosin.ecs.engine.components.mappers.wildcardrelations;

import java.util.Arrays;
import java.util.Iterator;
import java.util.function.IntFunction;

import org.jspecify.annotations.NonNull;

import de.schosin.ecs.api.Pooled;
import de.schosin.ecs.api.components.Relation.EntityRelation;
import de.schosin.ecs.api.components.Result.EntityRelationResult;
import de.schosin.ecs.api.components.mappers.EntityRelationMappers;
import de.schosin.ecs.api.components.mappers.EntityRelationMappers.EntityRelationMapper;
import de.schosin.ecs.api.components.mappers.EntityRelationMappers.ExclusiveEntityRelationMapper;
import de.schosin.ecs.api.components.mappers.WildcardRelationMappers.WildcardEntityRelationMapper;
import de.schosin.ecs.api.data.DataAccessor;
import de.schosin.ecs.engine.components.ComponentMapperManager.PoolingComponents;
import de.schosin.ecs.engine.components.ComponentMapperManager.WildcardMapper;
import de.schosin.ecs.utils.collections.Bag;
import de.schosin.ecs.utils.collections.IntBag;
import de.schosin.ecs.utils.collections.Pool;

public final class WildcardEntityRelationMapperImpl<R> implements WildcardEntityRelationMapper<R>, PoolingComponents<EntityRelationResult<R>>, WildcardMapper<EntityRelationMappers<R, ?>> {

    private final IntFunction<DataAccessor> accessor;

    private final Bag<EntityRelationMapper<R>> mappers;
    private final IntBag componentIds;

    @SuppressWarnings("rawtypes")
    private final Bag<ExclusiveEntityRelationMapper> exclusiveMappers;
    private final IntBag exclusiveComponentIds;

    private final Pool<EntityRelationResultImpl> pool = Pool.unbounded(EntityRelationResultImpl.class, EntityRelationResultImpl::new);
    private final Bag<EntityRelationResultImpl> lent = new Bag<>(EntityRelationResultImpl.class, 8);

    public WildcardEntityRelationMapperImpl(IntFunction<DataAccessor> accessor) {
        this.accessor = accessor;

        this.mappers = new Bag<>(EntityRelationMapper.class, 4);
        this.componentIds = new IntBag(4);

        this.exclusiveMappers = new Bag<>(ExclusiveEntityRelationMapper.class, 4);
        this.exclusiveComponentIds = new IntBag(4);
    }

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void addMapper(EntityRelationMappers<R, ?> components) {
        switch (components) {
            case EntityRelationMapper mapper -> {
                this.mappers.add(mapper);
                this.componentIds.add(components.componentId());
            }
            case ExclusiveEntityRelationMapper<?> exclusiveMapper -> {
                this.exclusiveMappers.add(exclusiveMapper);
                this.exclusiveComponentIds.add(components.componentId());
            }
        }
    }

    @Override
    public void free(EntityRelationResult<R> result) {
        if (result instanceof EntityRelationResultImpl impl && this.lent.removeIdentity(impl)) {
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
    public EntityRelationResult<? extends R> get(int entityId) {
        return get(accessor.apply(entityId));
    }

    @Override
    public EntityRelationResult<? extends R> get(DataAccessor accessor) {
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

    private class EntityRelationResultImpl implements EntityRelationResult<R>, Iterator<EntityRelation<R>>, Pooled {

        private final IntBag data = new IntBag(4);
        private final IntBag dataIndex = new IntBag(4);

        private DataAccessor accessor;

        private int exclusiveSize = -1;
        private int size = -1;

        private int id = -1;

        public EntityRelationResultImpl() {
            Arrays.fill(this.data.getData(), -1);
            Arrays.fill(this.dataIndex.getData(), -1);
        }

        public EntityRelationResultImpl init(DataAccessor accessor) {
            this.accessor = accessor;

            return this;
        }

        @NonNull
        @Override
        public EntityRelation<R> get(int i) {
            if (i >= size()) {
                throw new ArrayIndexOutOfBoundsException(i);
            }

            if (i < exclusiveSize) {
                return this.accessor.getComponent(this.data.get(i));
            }

            var componentId = this.data.get(i);
            var relations = this.accessor.<EntityRelationResult<R>>getComponent(componentId);

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

                    var relations = this.accessor.<EntityRelationResult<R>>getComponent(componentId);
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

            return size;
        }

        @Override
        public boolean isEmpty() {
            return size() == 0;
        }

        @Override
        public Iterator<EntityRelation<R>> iterator() {
            this.id = 0;
            return this;
        }

        @Override
        public boolean hasNext() {
            return id < size();
        }

        @Override
        public EntityRelation<R> next() {
            return get(id++);
        }

        @Override
        public R getRelationship(int target) {
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
