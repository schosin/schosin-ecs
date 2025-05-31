package de.schosin.ecs.engine.components.mappers.fetch;

import static de.schosin.ecs.api.components.types.ComponentType.relation;

import java.util.Iterator;

import org.jspecify.annotations.NonNull;

import de.schosin.ecs.api.Pooled;
import de.schosin.ecs.api.components.Relation;
import de.schosin.ecs.api.components.Relation.EntityRelationData;
import de.schosin.ecs.api.components.Result.EntityRelationDataResult;
import de.schosin.ecs.api.components.mappers.Components;
import de.schosin.ecs.api.components.mappers.EntityFetchRelations.EntityRelationFetchMapper;
import de.schosin.ecs.api.components.mappers.EntityRelations.EntityRelationMapper;
import de.schosin.ecs.api.components.types.RelationFetchType.EntityRelationFetchType;
import de.schosin.ecs.engine.components.ComponentMapperManager;
import de.schosin.ecs.utils.collections.Bag;
import de.schosin.ecs.utils.collections.Pool;

public class EntityRelationFetchMapperImpl<R, T> implements EntityRelationFetchMapper<R, T> {

    private final EntityRelationMapper<R> relationMapper;
    private final Components<?, T> dataMapper;

    public EntityRelationFetchMapperImpl(EntityRelationFetchType<R, T> type, ComponentMapperManager componentMapperManager) {
        this.relationMapper = componentMapperManager.getComponents(relation(type.relationship()));
        this.dataMapper = componentMapperManager.getComponents(type.fetch());
    }

    @Override
    public boolean has(int entityId) {
        return relationMapper.has(entityId);
    }

    @Override
    public EntityRelationDataResult<R, T> get(int entityId) {
        var relations = relationMapper.get(entityId);
        if (relations == null) {
            return null;
        }

        return EntityRelationDataResultImpl.getInstance(relations, dataMapper);
    }

    @Override
    public boolean remove(int entityId) {
        return relationMapper.remove(entityId);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static class EntityRelationDataResultImpl implements EntityRelationDataResult, Pooled {

        private static final Pool<EntityRelationDataResultImpl> POOL = Pool.unbounded(EntityRelationDataResultImpl.class, EntityRelationDataResultImpl::new);

        private EntityRelationResult<?> result;
        private Components<?, ?> mapper;
        private boolean initialized;

        private final Bag<EntityRelationData<?, ?>> relations = new Bag<>(EntityRelationData.class, 8);

        static <R, T> EntityRelationDataResult<R, T> getInstance(EntityRelationResult<R> result, Components<?, T> mapper) {
            var instance = POOL.getInstance();
            instance.result = result;
            instance.mapper = mapper;
            instance.initialized = false;
            instance.relations.ensureCapacity(result.size());

            return instance;
        }

        @NonNull
        @Override
        public Object get(int i) {
            initialize();

            return this.relations.get(i);
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
        public Iterator iterator() {
            initialize();
            return this.relations.iterator();
        }

        @Override
        public Object getRelationship(int target) {
            return result.getRelationship(target);
        }

        @Override
        public Object getData(int target) {
            initialize();

            var data = relations.getData();
            for (int i = 0, s = relations.getSize(); i < s; i++) {
                var relation = data[i];
                if (relation.target() == target) {
                    return relation.data();
                }
            }

            return null;
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
