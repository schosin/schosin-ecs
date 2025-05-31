package de.schosin.ecs.engine.components.mappers.fetch;

import static de.schosin.ecs.api.components.types.ComponentType.exclusiveRelation;

import de.schosin.ecs.api.components.Relation;
import de.schosin.ecs.api.components.Relation.EntityRelationData;
import de.schosin.ecs.api.components.Relation.Exclusive;
import de.schosin.ecs.api.components.mappers.Components;
import de.schosin.ecs.api.components.mappers.EntityFetchRelations.ExclusiveEntityRelationFetchMapper;
import de.schosin.ecs.api.components.mappers.EntityRelations.ExclusiveEntityRelationMapper;
import de.schosin.ecs.api.components.types.RelationFetchType.ExclusiveEntityRelationFetchType;
import de.schosin.ecs.engine.components.ComponentMapperManager;

public class ExclusiveEntityRelationFetchMapperImpl<R extends Exclusive, T> implements ExclusiveEntityRelationFetchMapper<R, T> {

    private final ExclusiveEntityRelationMapper<R> relationMapper;
    private final Components<?, T> dataMapper;

    public ExclusiveEntityRelationFetchMapperImpl(ExclusiveEntityRelationFetchType<R, T> type, ComponentMapperManager componentMapperManager) {
        this.relationMapper = componentMapperManager.getComponents(exclusiveRelation(type.relationship()));
        this.dataMapper = componentMapperManager.getComponents(type.fetch());
    }

    @Override
    public boolean has(int entityId) {
        return relationMapper.has(entityId);
    }

    @Override
    public EntityRelationData<R, T> get(int entityId) {
        var relation = relationMapper.get(entityId);
        if (relation == null) {
            return null;
        }

        var data = dataMapper.get(relation.target());
        return Relation.create(relation.relationship(), relation.target(), data);
    }

    @Override
    public boolean remove(int entityId) {
        return relationMapper.remove(entityId);
    }

}