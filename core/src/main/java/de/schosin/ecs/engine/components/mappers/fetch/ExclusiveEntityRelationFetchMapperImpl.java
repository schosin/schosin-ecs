package de.schosin.ecs.engine.components.mappers.fetch;

import static de.schosin.ecs.api.components.types.ComponentType.exclusiveRelation;

import java.util.function.IntFunction;

import de.schosin.ecs.api.Pooled;
import de.schosin.ecs.api.components.Relation.EntityRelationData;
import de.schosin.ecs.api.components.Relation.Exclusive;
import de.schosin.ecs.api.components.mappers.Components;
import de.schosin.ecs.api.components.mappers.EntityFetchRelationMappers.ExclusiveEntityRelationFetchMapper;
import de.schosin.ecs.api.components.mappers.EntityRelationMappers.ExclusiveEntityRelationMapper;
import de.schosin.ecs.api.components.types.RelationComponentType.RegularEntityRelationType;
import de.schosin.ecs.api.components.types.RelationFetchType.ExclusiveEntityRelationFetchType;
import de.schosin.ecs.api.data.ArchetypeComponentAccessor;
import de.schosin.ecs.api.data.ComponentAccessor;
import de.schosin.ecs.api.data.DataAccessor;
import de.schosin.ecs.engine.components.ComponentMapperManager;
import de.schosin.ecs.engine.components.ComponentMapperManager.ReclaimingComponents;
import de.schosin.ecs.utils.collections.Bag;
import de.schosin.ecs.utils.collections.Pool;

public class ExclusiveEntityRelationFetchMapperImpl<R extends Exclusive, T> implements ExclusiveEntityRelationFetchMapper<R, T>, ReclaimingComponents {

    private final IntFunction<DataAccessor> accessor;

    private final ExclusiveEntityRelationMapper<R> relationMapper;
    private final int componentId;

    private final Components<?, T> dataMapper;

    private final Pool<EntityRelationFetchAccessor<R, T>> accessorPool;
    private final Bag<EntityRelationFetchAccessor<R, T>> lent = new Bag<>(EntityRelationFetchAccessor.class, 8);

    public ExclusiveEntityRelationFetchMapperImpl(ExclusiveEntityRelationFetchType<R, T> type, ComponentMapperManager componentMapperManager, IntFunction<DataAccessor> accessor) {
        this.accessor = accessor;
        this.relationMapper = componentMapperManager.getComponents(exclusiveRelation(type.relationship()));
        this.componentId = relationMapper.componentId();

        this.dataMapper = componentMapperManager.getComponents(type.fetch());
        this.accessorPool = Pool.unbounded(EntityRelationFetchAccessor.class, this::createAccessor);
    }

    private EntityRelationFetchAccessor<R, T> createAccessor() {
        return new EntityRelationFetchAccessor<>(relationMapper, dataMapper, accessorPool);
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
    public EntityRelationData<R, T> get(int entityId) {
        var accessor = this.accessor.apply(entityId);
        if (!accessor.hasComponent(componentId)) {
            return null;
        }

        var componentAccessor = getComponentAccessor(accessor);
        lent.add(componentAccessor);

        return componentAccessor.getComponent(accessor);
    }

    @Override
    public EntityRelationFetchAccessor<R, T> getComponentAccessor(DataAccessor accessor) {
        return accessorPool.getInstance().init(accessor);
    }

    @Override
    public ArchetypeComponentAccessor<EntityRelationData<R, T>> getArchetypeComponentAccessor(DataAccessor accessor) {
        // TODO implement
        var todo = true;

        throw new UnsupportedOperationException("getArchetypeComponentAccessor(%s) not implemented yet".formatted(accessor));
    }

    @Override
    public boolean remove(int entityId) {
        return relationMapper.remove(entityId);
    }

    private static final class EntityRelationFetchAccessor<R extends Exclusive, T> implements EntityRelationData<R, T>, ComponentAccessor<EntityRelationData<R, T>>, Pooled {

        private final ExclusiveEntityRelationMapper<R> relationMapper;
        private final Components<?, T> dataMapper;

        private final Pool<EntityRelationFetchAccessor<R, T>> pool;

        private ComponentAccessor<EntityRelation<R>> relationAccessor;
        private EntityRelation<R> relation;
        private T data;

        public EntityRelationFetchAccessor(ExclusiveEntityRelationMapper<R> relationMapper, Components<?, T> dataMapper, Pool<EntityRelationFetchAccessor<R, T>> pool) {
            this.relationMapper = relationMapper;
            this.dataMapper = dataMapper;
            this.pool = pool;
        }

        private EntityRelationFetchAccessor<R, T> init(DataAccessor accessor) {
            this.relationAccessor = relationMapper.getComponentAccessor(accessor);

            return this;
        }

        @Override
        public EntityRelationData<R, T> getComponent(DataAccessor accessor) {
            this.relation = relationAccessor.getComponent(accessor);
            this.data = null;

            return this;
        }

        @Override
        public RegularEntityRelationType<R, ?> type() {
            return relationMapper.componentType();
        }

        @Override
        public R relationship() {
            return relation.relationship();
        }

        @Override
        public int target() {
            return relation.target();
        }

        @Override
        public T data() {
            if (this.data == null) {
                this.data = dataMapper.get(relation.target());
            }

            return this.data;
        }

        @Override
        public void free() {
            pool.free(this);
        }

        @Override
        public void reset() {
            this.relationAccessor.free();
            this.relationAccessor = null;

            this.relation = null;
            this.data = null;
        }

        @Override
        public String toString() {
            if (relation == null) {
                return "EntityRelationData(invalidated)";
            }

            return new StringBuilder()
                    .append("EntityRelationData(")
                    .append("relationship = ").append(relationship()).append(", ")
                    .append("target = ").append(target()).append(", ")
                    .append("data = ").append(data())
                    .append(")")
                    .toString();
        }

    }

}