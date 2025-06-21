package de.schosin.ecs.engine.components.mappers.fetch;

import static de.schosin.ecs.api.components.types.ComponentType.relation;

import java.util.Iterator;
import java.util.function.IntFunction;

import org.jspecify.annotations.NonNull;

import de.schosin.ecs.api.Pooled;
import de.schosin.ecs.api.components.Relation.EntityRelationData;
import de.schosin.ecs.api.components.Relations.EntityRelations;
import de.schosin.ecs.api.components.Relations.EntityRelationsData;
import de.schosin.ecs.api.components.mappers.Components;
import de.schosin.ecs.api.components.mappers.EntityFetchRelationMappers.EntityRelationFetchMapper;
import de.schosin.ecs.api.components.mappers.EntityRelationMappers.EntityRelationMapper;
import de.schosin.ecs.api.components.types.RelationFetchType.EntityRelationFetchType;
import de.schosin.ecs.api.data.DataAccessor;
import de.schosin.ecs.engine.components.ComponentMapperManager;
import de.schosin.ecs.engine.components.ComponentMapperManager.PoolingComponents;
import de.schosin.ecs.engine.utils.components.EntityRelationDataImpl;
import de.schosin.ecs.utils.collections.Bag;
import de.schosin.ecs.utils.collections.Pool;

public class EntityRelationFetchMapperImpl<R, T> implements EntityRelationFetchMapper<R, T>, PoolingComponents<EntityRelationsData<R, T>> {

    private final IntFunction<DataAccessor> accessor;

    private final EntityRelationMapper<R> relationMapper;
    private final int componentId;

    private final Components<?, T> dataMapper;

    public EntityRelationFetchMapperImpl(EntityRelationFetchType<R, T> type, ComponentMapperManager componentMapperManager, IntFunction<DataAccessor> accessor) {
        this.accessor = accessor;

        this.relationMapper = componentMapperManager.getComponents(relation(type.relationship()));
        this.componentId = relationMapper.componentId();

        this.dataMapper = componentMapperManager.getComponents(type.fetch());
    }

    @Override
    public void free(EntityRelationsData<R, T> result) {
        EntityRelationDataResultImpl.free(result);
    }

    @Override
    public boolean has(int entityId) {
        return relationMapper.has(entityId);
    }

    @Override
    public EntityRelationsData<R, T> get(int entityId) {
        return access(accessor.apply(entityId));
    }

    @Override
    public EntityRelationsData<R, T> access(DataAccessor accessor) {
        EntityRelations<R> relations = accessor.getComponent(componentId);
        if (relations == null) {
            return null;
        }

        return EntityRelationDataResultImpl.getInstance(relations, dataMapper);
    }

    @Override
    public boolean remove(int entityId) {
        return relationMapper.remove(entityId);
    }

    public static <R, T> EntityRelationsData<R, T> getEntityRelationsData(EntityRelations<R> relations, Components<?, T> mapper) {
        return EntityRelationDataResultImpl.getInstance(relations, mapper);
    }

    public static <R, T> void freeResult(EntityRelationsData<R, T> result) {
        if (result instanceof EntityRelationDataResultImpl impl) {
            EntityRelationDataResultImpl.POOL.free(impl);
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    static class EntityRelationDataResultImpl implements EntityRelationsData, Pooled {

        static final Pool<EntityRelationDataResultImpl> POOL = Pool.unbounded(EntityRelationDataResultImpl.class, EntityRelationDataResultImpl::new);

        private EntityRelations<?> result;
        private Components<?, ?> mapper;
        private boolean initialized;

        private final Bag<EntityRelationData<?, ?>> relations = new Bag<>(EntityRelationData.class, 8);

        private static <R, T> EntityRelationsData<R, T> getInstance(EntityRelations<R> result, Components<?, T> mapper) {
            var instance = POOL.getInstance();
            instance.result = result;
            instance.mapper = mapper;
            instance.initialized = false;
            instance.relations.ensureCapacity(result.size());

            return instance;
        }

        public static void free(EntityRelationsData relations) {
            if (relations instanceof EntityRelationDataResultImpl impl) {
                POOL.free(impl);
            }
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
