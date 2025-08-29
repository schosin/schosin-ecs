package de.schosin.ecs.plugins.wildcards.mappers;

import static de.schosin.ecs.plugins.wildcards.types.WildcardType.wildcardRelation;

import java.util.Iterator;

import org.jspecify.annotations.NonNull;

import de.schosin.ecs.api.Pooled;
import de.schosin.ecs.api.components.Relation.EntityRelationData;
import de.schosin.ecs.api.components.mappers.Components;
import de.schosin.ecs.api.components.mappers.CustomComponentMapper;
import de.schosin.ecs.api.data.ArchetypeComponentAccessor;
import de.schosin.ecs.api.data.ComponentAccessor;
import de.schosin.ecs.api.data.DataAccessor;
import de.schosin.ecs.engine.components.ComponentMapperManager;
import de.schosin.ecs.engine.components.ComponentMapperManager.ReclaimingComponents;
import de.schosin.ecs.engine.entities.EntityManager;
import de.schosin.ecs.engine.utils.components.EntityRelationDataImpl;
import de.schosin.ecs.plugins.wildcards.result.WildcardEntityRelations;
import de.schosin.ecs.plugins.wildcards.result.WildcardEntityRelationsData;
import de.schosin.ecs.plugins.wildcards.types.WildcardEntityRelationFetchType;
import de.schosin.ecs.utils.collections.Bag;
import de.schosin.ecs.utils.collections.Pool;

public class WildcardEntityRelationFetchMapper<R, T> implements CustomComponentMapper<EntityRelationData<R, T>, WildcardEntityRelationsData<R, T>>, ReclaimingComponents {

    private final EntityManager entityManager;

    private final WildcardEntityRelationMapper<R> relationMapper;
    private final Components<?, T> dataMapper;

    private final Bag<WildcardEntityRelationsFetchAccessor<R, T>> lent = new Bag<>(WildcardEntityRelationsFetchAccessor.class, 8);
    private final Pool<WildcardEntityRelationsFetchAccessor<R, T>> pool;

    public WildcardEntityRelationFetchMapper(EntityManager entityManager, WildcardEntityRelationFetchType<R, T> type, ComponentMapperManager componentMapperManager) {
        this.entityManager = entityManager;

        this.relationMapper = componentMapperManager.getComponents(wildcardRelation(type.relationshipBound()));
        this.dataMapper = componentMapperManager.getComponents(type.fetch());

        this.pool = Pool.unbounded(WildcardEntityRelationsFetchAccessor.class, this::createAccessor);
    }

    private WildcardEntityRelationsFetchAccessor<R, T> createAccessor() {
        return new WildcardEntityRelationsFetchAccessor<>(relationMapper, dataMapper, pool);
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
    public WildcardEntityRelationsData<R, T> get(int entityId) {
        var accessor = this.entityManager.getAccessor(entityId);

        var componentAccessor = getComponentAccessor(accessor);
        lent.add(componentAccessor);

        return componentAccessor.getComponent(accessor);
    }

    @Override
    public WildcardEntityRelationsFetchAccessor<R, T> getComponentAccessor(DataAccessor accessor) {
        return pool.getInstance().init(accessor);
    }

    @Override
    public ArchetypeComponentAccessor<WildcardEntityRelationsData<R, T>> getArchetypeComponentAccessor(DataAccessor accessor) {
        // TODO implement
        var todo = true;

        throw new UnsupportedOperationException("getArchetypeComponentAccessor(%s) not implemented yet".formatted(accessor));
    }

    @Override
    public boolean remove(int entityId) {
        return relationMapper.remove(entityId);
    }

    private static final class WildcardEntityRelationsFetchAccessor<R, T> implements WildcardEntityRelationsData<R, T>, ComponentAccessor<WildcardEntityRelationsData<R, T>>, Pooled {

        private final WildcardEntityRelationMapper<R> relationMapper;
        private final Components<?, T> dataMapper;

        private final Pool<WildcardEntityRelationsFetchAccessor<R, T>> pool;

        private ComponentAccessor<WildcardEntityRelations<R>> relationsAccessor;
        private EntityRelations<R> results;
        private boolean initialized;

        private final Bag<EntityRelationData<R, T>> relations = new Bag<>(EntityRelationData.class, 8);

        public WildcardEntityRelationsFetchAccessor(WildcardEntityRelationMapper<R> relationMapper, Components<?, T> dataMapper, Pool<WildcardEntityRelationsFetchAccessor<R, T>> pool) {
            this.relationMapper = relationMapper;
            this.dataMapper = dataMapper;

            this.pool = pool;
        }

        public WildcardEntityRelationsFetchAccessor<R, T> init(DataAccessor accessor) {
            this.relationsAccessor = relationMapper.getComponentAccessor(accessor);

            return this;
        }

        @Override
        public WildcardEntityRelationsData<R, T> getComponent(DataAccessor accessor) {
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
