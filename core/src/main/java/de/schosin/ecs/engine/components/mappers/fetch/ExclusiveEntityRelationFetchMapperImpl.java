package de.schosin.ecs.engine.components.mappers.fetch;

import static de.schosin.ecs.api.components.types.ComponentType.exclusiveRelation;

import java.util.function.IntFunction;

import de.schosin.ecs.api.components.Relation.EntityRelation;
import de.schosin.ecs.api.components.Relation.EntityRelationData;
import de.schosin.ecs.api.components.Relation.Exclusive;
import de.schosin.ecs.api.components.mappers.Components;
import de.schosin.ecs.api.components.mappers.EntityFetchRelations.ExclusiveEntityRelationFetchMapper;
import de.schosin.ecs.api.components.mappers.EntityRelations.ExclusiveEntityRelationMapper;
import de.schosin.ecs.api.components.types.RelationFetchType.ExclusiveEntityRelationFetchType;
import de.schosin.ecs.api.data.DataAccessor;
import de.schosin.ecs.engine.components.ComponentMapperManager;
import de.schosin.ecs.engine.components.ComponentMapperManager.PoolingComponents;
import de.schosin.ecs.engine.utils.components.EntityRelationDataImpl;

public class ExclusiveEntityRelationFetchMapperImpl<R extends Exclusive, T> implements ExclusiveEntityRelationFetchMapper<R, T>, PoolingComponents<EntityRelationData<R, T>> {

    private final IntFunction<DataAccessor> accessor;

    private final ExclusiveEntityRelationMapper<R> relationMapper;
    private final int componentId;

    private final Components<?, T> dataMapper;

    public ExclusiveEntityRelationFetchMapperImpl(ExclusiveEntityRelationFetchType<R, T> type, ComponentMapperManager componentMapperManager, IntFunction<DataAccessor> accessor) {
        this.accessor = accessor;
        this.relationMapper = componentMapperManager.getComponents(exclusiveRelation(type.relationship()));
        this.componentId = relationMapper.componentId();

        this.dataMapper = componentMapperManager.getComponents(type.fetch());
    }

    @Override
    public void free(EntityRelationData<R, T> result) {
        EntityRelationDataImpl.free(result);
    }

    @Override
    public boolean has(int entityId) {
        return relationMapper.has(entityId);
    }

    @Override
    public EntityRelationData<R, T> get(int entityId) {
        return get(accessor.apply(entityId));
    }

    @Override
    public EntityRelationData<R, T> get(DataAccessor accessor) {
        EntityRelation<R> relation = accessor.getComponent(componentId);
        if (relation == null) {
            return null;
        }

        return EntityRelationDataImpl.getInstance(relation, dataMapper::get);
    }

    @Override
    public boolean remove(int entityId) {
        return relationMapper.remove(entityId);
    }

}