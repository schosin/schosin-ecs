package de.schosin.ecs.engine.components.mappers.fetch;

import static de.schosin.ecs.api.components.types.ComponentType.relation;

import java.util.Iterator;
import java.util.function.IntFunction;

import org.jspecify.annotations.NonNull;

import de.schosin.ecs.api.Pooled;
import de.schosin.ecs.api.components.Relation.EntityRelationData;
import de.schosin.ecs.api.components.Relations.EntityRelationsData;
import de.schosin.ecs.api.components.mappers.Components;
import de.schosin.ecs.api.components.mappers.EntityFetchRelationMappers.EntityRelationFetchMapper;
import de.schosin.ecs.api.components.mappers.EntityRelationMappers.EntityRelationMapper;
import de.schosin.ecs.api.components.types.RelationFetchType.EntityRelationFetchType;
import de.schosin.ecs.api.data.ArchetypeComponentAccessor;
import de.schosin.ecs.api.data.ComponentAccessor;
import de.schosin.ecs.api.data.DataAccessor;
import de.schosin.ecs.engine.components.ComponentMapperManager;
import de.schosin.ecs.engine.components.ComponentMapperManager.ReclaimingComponents;
import de.schosin.ecs.engine.utils.components.EntityRelationDataImpl;
import de.schosin.ecs.storage.api.entities.ArchetypeAccessor;
import de.schosin.ecs.utils.collections.Bag;
import de.schosin.ecs.utils.collections.Pool;

public class EntityRelationFetchMapperImpl<R, T> implements EntityRelationFetchMapper<R, T>, ReclaimingComponents {

    private final IntFunction<DataAccessor> accessor;

    private final EntityRelationMapper<R> relationMapper;
    private final Components<?, T> dataMapper;

    private final Pool<EntityRelationsFetchAccessor<R, T>> pool;
    private final Bag<EntityRelationsFetchAccessor<R, T>> lent = new Bag<>(EntityRelationsFetchAccessor.class, 8);

    public EntityRelationFetchMapperImpl(EntityRelationFetchType<R, T> type, ComponentMapperManager componentMapperManager, IntFunction<DataAccessor> accessor) {
        this.accessor = accessor;

        this.relationMapper = componentMapperManager.getComponents(relation(type.relationship()));
        this.dataMapper = componentMapperManager.getComponents(type.fetch());

        this.pool = Pool.unbounded(EntityRelationsFetchAccessor.class, this::createAccessor);
    }

    private EntityRelationsFetchAccessor<R, T> createAccessor() {
        return new EntityRelationsFetchAccessor<>(relationMapper, dataMapper, pool);
    }

    @Override
    public void reclaim() {
        var data = lent.getData();
        for (int i = 0, s = lent.getSize(); i < s; i++) {
            data[i].free();
        }

        lent.clear();
    }

    @Override
    public boolean has(int entityId) {
        return relationMapper.has(entityId);
    }

    @Override
    public EntityRelationsData<R, T> get(int entityId) {
        var accessor = this.accessor.apply(entityId);

        var componentAccessor = getComponentAccessor(accessor);
        lent.add(componentAccessor);

        return componentAccessor.getComponent(accessor);
    }

    @Override
    public EntityRelationsFetchAccessor<R, T> getComponentAccessor(DataAccessor accessor) {
        return pool.getInstance().init(accessor);
    }

    @Override
    public ArchetypeComponentAccessor<EntityRelationsData<R, T>> getArchetypeComponentAccessor(DataAccessor accessor) {
        // TODO implement
        var todo = true;

        throw new UnsupportedOperationException("getArchetypeComponentAccessor(%s) not implemented yet".formatted(accessor));
    }

    @Override
    public boolean remove(int entityId) {
        return relationMapper.remove(entityId);
    }

    public static final class EntityRelationsFetchAccessor<R, T> implements EntityRelationsData<R, T>, ComponentAccessor<EntityRelationsData<R, T>>, Pooled {

        private final Components<?, EntityRelations<R>> relationMapper;
        private final Components<?, T> dataMapper;

        private final Pool<EntityRelationsFetchAccessor<R, T>> pool;

        private ComponentAccessor<EntityRelations<R>> relationsAccessor;
        private EntityRelations<R> results;
        private boolean initialized;

        private final Bag<EntityRelationData<R, T>> relations = new Bag<>(EntityRelationData.class, 8);

        public EntityRelationsFetchAccessor(Components<?, EntityRelations<R>> relationMapper, Components<?, T> dataMapper, Pool<EntityRelationsFetchAccessor<R, T>> pool) {
            this.relationMapper = relationMapper;
            this.dataMapper = dataMapper;

            this.pool = pool;
        }

        public EntityRelationsFetchAccessor(EntityRelationMapper<R> relationMapper, Components<?, T> dataMapper, Pool<EntityRelationsFetchAccessor<R, T>> pool) {
            this.relationMapper = relationMapper;
            this.dataMapper = dataMapper;

            this.pool = pool;
        }

        public EntityRelationsFetchAccessor<R, T> init(DataAccessor accessor) {
            this.relationsAccessor = relationMapper.getComponentAccessor(accessor);

            return this;
        }

        @Override
        public EntityRelationsData<R, T> getComponent(DataAccessor accessor) {
            this.results = relationsAccessor.getComponent(accessor);
            this.initialized = false;

            for (int i = 0, s = relations.getSize(); i < s; i++) {
                EntityRelationDataImpl.free(relations.get(i));
            }
            this.relations.clear();

            return this;
        }

        @NonNull
        @Override
        public EntityRelationData<R, T> get(int i) {
            initialize();

            if (i >= size()) {
                throw new ArrayIndexOutOfBoundsException(i);
            }

            return this.relations.get(i);
        }

        @Override
        public int size() {
            if (results == null) {
                return 0;
            }

            return results.size();
        }

        @Override
        public boolean isEmpty() {
            return results == null || results.isEmpty();
        }

        @Override
        public Iterator<EntityRelationData<R, T>> iterator() {
            initialize();
            return this.relations.iterator();
        }

        @Override
        public R getRelationship(int target) {
            if (results == null) {
                return null;
            }

            return results.getRelationship(target);
        }

        @Override
        public T getData(int target) {
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

            if (results != null) {
                for (int i = 0, s = results.size(); i < s; i++) {
                    var relation = results.get(i);

                    this.relations.add(EntityRelationDataImpl.getInstance(relation, dataMapper::get));
                }
            }

            this.initialized = true;
        }

        @Override
        public void free() {
            pool.free(this);
        }

        @Override
        public void reset() {
            this.relationsAccessor.free();
            this.relationsAccessor = null;

            this.results = null;

            this.initialized = false;

            for (int i = 0, s = relations.getSize(); i < s; i++) {
                EntityRelationDataImpl.free(relations.get(i));
            }
            this.relations.clear();
        }

        @Override
        public String toString() {
            if (results == null) {
                return "EntityRelationsData(invalidated)";
            }

            var builder = new StringBuilder().append("EntityRelationsData(");
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
