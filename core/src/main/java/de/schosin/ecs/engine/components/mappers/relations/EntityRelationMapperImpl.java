package de.schosin.ecs.engine.components.mappers.relations;

import de.schosin.ecs.api.components.Relation.EntityRelation;
import de.schosin.ecs.api.components.Result.EntityRelationResult;
import de.schosin.ecs.api.components.mappers.EntityRelations.EntityRelationMapper;
import de.schosin.ecs.engine.components.TransmutationManager;
import de.schosin.ecs.storage.api.components.Component.EntityRelationData;

public class EntityRelationMapperImpl<R> extends AbstractEntityRelationMapper<R, EntityRelationResult<R>, EntityRelationData<R>> implements EntityRelationMapper<R> {

    public EntityRelationMapperImpl(EntityRelationData<R> data, TransmutationManager transmutationManager) {
        super(data, transmutationManager);
    }

    @Override
    public EntityRelation<R> add(int entityId, EntityRelation<R> relation) {
        this.add.apply(entityId, relation);

        return relation;
    }

    @Override
    public R getRelationship(int entityId, int target) {
        var relation = data.getComponent(entityId);
        if (relation == null) {
            return null;
        }

        return relation.getRelationship(target);
    }

}
