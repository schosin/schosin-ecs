package de.schosin.ecs.engine.components.mappers.wildcardrelations;

import static de.schosin.ecs.api.components.types.ComponentType.wildcardRelation;

import java.util.Iterator;

import org.jspecify.annotations.NonNull;

import de.schosin.ecs.api.Pooled;
import de.schosin.ecs.api.components.Relation;
import de.schosin.ecs.api.components.Relation.EntityRelation;
import de.schosin.ecs.api.components.Relation.EntityRelationData;
import de.schosin.ecs.api.components.Result;
import de.schosin.ecs.api.components.mappers.Components;
import de.schosin.ecs.api.components.mappers.WildcardRelations.WildcardEntityFetchRelations;
import de.schosin.ecs.api.components.types.WildcardRelationType.WildcardEntityRelationFetchType;
import de.schosin.ecs.engine.components.ComponentMapperManager;
import de.schosin.ecs.engine.components.ComponentMapperManager.PoolingComponents;
import de.schosin.ecs.utils.collections.Bag;
import de.schosin.ecs.utils.collections.BagIterator;
import de.schosin.ecs.utils.collections.Pool;

public class WildcardEntityFetchRelationsImpl<R, T> implements WildcardEntityFetchRelations<R, T>, PoolingComponents<Result<EntityRelationData<? extends R, T>>> {

    private final WildcardEntityRelations<R> relationMapper;
    private final Components<?, T> dataMapper;

    private final Bag<ResultImpl<R, T>> lent = new Bag<>(ResultImpl.class, 8);

    public WildcardEntityFetchRelationsImpl(WildcardEntityRelationFetchType<R, T> type, ComponentMapperManager componentMapperManager) {
        this.relationMapper = componentMapperManager.getWildcardEntityRelations(wildcardRelation(type.relationshipBound()));
        this.dataMapper = componentMapperManager.getComponents(type.fetch());
    }

    @Override
    public void free(Result<EntityRelationData<? extends R, T>> result) {
        if (result instanceof ResultImpl<R, T> impl) {
            this.lent.removeIdentity(impl);
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
    public Result<EntityRelationData<? extends R, T>> get(int entityId) {
        var relations = relationMapper.get(entityId);
        if (relations.isEmpty()) {
            return Result.empty();
        }

        var result = ResultImpl.getInstance(relations, dataMapper);
        lent.add(result);

        return result;
    }

    @Override
    public boolean remove(int entityId) {
        return relationMapper.remove(entityId);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static class ResultImpl<R, T> implements Result<EntityRelationData<? extends R, T>>, Pooled {

        private static final Pool<ResultImpl<?, ?>> POOL = Pool.unbounded(ResultImpl.class, ResultImpl::new);

        private Result<EntityRelation<?>> result;
        private Components<?, ?> mapper;
        private boolean initialized;

        private final Bag<EntityRelationData<?, ?>> relations = new Bag<>(EntityRelationData.class, 8);

        static <R, T> ResultImpl<R, T> getInstance(Result<EntityRelation<? extends R>> result, Components<?, T> mapper) {
            var instance = (ResultImpl<R, T>) POOL.getInstance();
            instance.result = (Result) result;
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
        public Iterator<EntityRelationData<? extends R, T>> iterator() {
            initialize();

            return new BagIterator<>((Bag) this.relations);
        }

        @NonNull
        @Override
        public EntityRelationData<? extends R, T> get(int i) {
            initialize();

            if (i >= size()) {
                throw new ArrayIndexOutOfBoundsException(i);
            }

            return (EntityRelationData) this.relations.get(i);
        }

        private void initialize() {
            if (initialized) {
                return;
            }

            for (int i = 0, s = result.size(); i < s; i++) {
                var relation = result.get(i);
                var data = mapper.get(relation.target());

                this.relations.add(Relation.create(relation.relationship(), relation.target(), data));
            }

            this.initialized = true;
        }

        @Override
        public void reset() {
            this.result = null;
            this.mapper = null;
            this.initialized = false;

            this.relations.clear();
        }

    }

}
