package de.schosin.ecs.engine.components.mappers.wildcardrelations;

import static de.schosin.ecs.api.components.types.ComponentType.wildcardRelation;

import java.util.Iterator;

import org.jspecify.annotations.NonNull;

import de.schosin.ecs.api.Pooled;
import de.schosin.ecs.api.components.Relation.EntityRelationData;
import de.schosin.ecs.api.components.Relations.EntityRelationsData;
import de.schosin.ecs.api.components.mappers.Components;
import de.schosin.ecs.api.components.mappers.WildcardRelationMappers.WildcardEntityFetchRelationMapper;
import de.schosin.ecs.api.components.types.WildcardRelationType.WildcardEntityRelationFetchType;
import de.schosin.ecs.api.data.DataAccessor;
import de.schosin.ecs.engine.components.ComponentMapperManager;
import de.schosin.ecs.engine.components.ComponentMapperManager.PoolingComponents;
import de.schosin.ecs.engine.utils.components.EntityRelationDataImpl;
import de.schosin.ecs.utils.collections.Bag;
import de.schosin.ecs.utils.collections.Pool;

public final class WildcardEntityFetchRelationMapperImpl<R, T> implements WildcardEntityFetchRelationMapper<R, T>, PoolingComponents<EntityRelationsData<? extends R, T>> {

    private final WildcardEntityRelationMapper<R> relationMapper;
    private final Components<?, T> dataMapper;

    private final Bag<ResultImpl<? extends R, T>> lent = new Bag<>(ResultImpl.class, 8);

    public WildcardEntityFetchRelationMapperImpl(WildcardEntityRelationFetchType<R, T> type, ComponentMapperManager componentMapperManager) {
        this.relationMapper = componentMapperManager.getComponents(wildcardRelation(type.relationshipBound()));
        this.dataMapper = componentMapperManager.getComponents(type.fetch());
    }

    @Override
    public void free(EntityRelationsData<? extends R, T> result) {
        if (result instanceof ResultImpl<? extends R, T> impl && this.lent.removeIdentity(impl)) {
            ResultImpl.POOL.free(impl);
        }
    }

    @Override
    public void reclaim() {
        var data = lent.getData();
        for (int i = 0, s = lent.getSize(); i < s; i++) {
            ResultImpl.POOL.free(data[i]);
        }

        lent.clear();
    }

    @Override
    public boolean has(int entityId) {
        return relationMapper.has(entityId);
    }

    @Override
    public EntityRelationsData<? extends R, T> get(int entityId) {
        var relations = relationMapper.get(entityId);

        var result = ResultImpl.getInstance(relations, dataMapper);
        lent.add(result);

        return result;
    }

    @Override
    public EntityRelationsData<? extends R, T> get(DataAccessor accessor) {
        var relations = relationMapper.get(accessor);

        var result = ResultImpl.getInstance(relations, dataMapper);
        lent.add(result);

        return result;
    }

    @Override
    public boolean remove(int entityId) {
        return relationMapper.remove(entityId);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static class ResultImpl<R, T> implements EntityRelationsData<R, T>, Pooled {

        private static final Pool<ResultImpl<?, ?>> POOL = Pool.unbounded(ResultImpl.class, ResultImpl::new);

        private EntityRelations<? extends R> result;
        private Components<?, ?> mapper;
        private boolean initialized;

        private final Bag<EntityRelationData<?, ?>> relations = new Bag<>(EntityRelationData.class, 8);

        static <R, T> ResultImpl<? extends R, T> getInstance(EntityRelations<? extends R> result, Components<?, T> mapper) {
            var instance = (ResultImpl<R, T>) POOL.getInstance();
            instance.result = result;
            instance.mapper = mapper;
            instance.initialized = false;
            instance.relations.ensureCapacity(result.size());

            return instance;
        }

        @Override
        public int size() {
            return result.size();
        }

        @Override
        public boolean isEmpty() {
            return result.isEmpty();
        }

        @Override
        public Iterator<EntityRelationData<R, T>> iterator() {
            initialize();
            return (Iterator) this.relations.iterator();
        }

        @NonNull
        @Override
        public EntityRelationData<R, T> get(int i) {
            initialize();

            if (i >= size()) {
                throw new ArrayIndexOutOfBoundsException(i);
            }

            return (EntityRelationData) this.relations.get(i);
        }

        @Override
        public R getRelationship(int target) {
            return null;
        }

        @Override
        public T getData(int target) {
            return null;
        }

        private void initialize() {
            if (initialized) {
                return;
            }

            for (int i = 0, s = result.size(); i < s; i++) {
                var relation = result.get(i);

                this.relations.add(EntityRelationDataImpl.getInstance(relation, mapper::get));
            }

            this.initialized = true;
        }

        @Override
        public void reset() {
            for (int i = 0, s = relations.getSize(); i < s; i++) {
                EntityRelationDataImpl.free(relations.get(i));
            }

            this.result = null;
            this.mapper = null;
            this.initialized = false;

            this.relations.clear();
        }

    }

}
