package de.schosin.ecs.engine.components.mappers.relations;

import de.schosin.ecs.api.components.Relation.EntityRelation;
import de.schosin.ecs.api.components.Relation.Exclusive;
import de.schosin.ecs.api.components.mappers.EntityRelations.ExclusiveEntityRelationMapper;
import de.schosin.ecs.engine.components.TransmutationManager;
import de.schosin.ecs.storage.api.components.Component.ExclusiveEntityRelationData;

public class ExclusiveEntityRelationMapperImpl<R extends Exclusive> extends AbstractEntityRelationMapper<R, EntityRelation<R>, ExclusiveEntityRelationData<R>>
        implements ExclusiveEntityRelationMapper<R> {

    public ExclusiveEntityRelationMapperImpl(ExclusiveEntityRelationData<R> data, TransmutationManager transmutationManager) {
        super(data, transmutationManager);
    }

    @Override
    public EntityRelation<R> add(int entityId, EntityRelation<R> relation) {
        this.add.apply(entityId, relation);

        return relation;
    }

    @Override
    public R getRelationship(int entityId) {
        var relation = get(entityId);
        if (relation == null) {
            return null;
        }

        return relation.relationship();
    }

    @Override
    public int getTarget(int entityId) {
        var relation = get(entityId);
        if (relation == null) {
            return -1;
        }

        return relation.target();
    }

}