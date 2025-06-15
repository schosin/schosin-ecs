package de.schosin.ecs.engine.components.mappers.relations;

import de.schosin.ecs.api.components.Relation.ComponentRelation;
import de.schosin.ecs.api.components.Result.ComponentRelationResult;
import de.schosin.ecs.api.components.mappers.ComponentRelations.ComponentRelationMapper;
import de.schosin.ecs.api.components.types.RelationComponentType.ComponentRelationType;
import de.schosin.ecs.engine.components.TransmutationManager;
import de.schosin.ecs.storage.api.components.Component.ComponentRelationData;

public final class ComponentRelationMapperImpl<R, T> extends AbstractComponentRelationMapper<R, T, ComponentRelationResult<R, T>, ComponentRelationData<R, T>> implements ComponentRelationMapper<R, T> {

    private final ComponentRelationType<R, T> componentType;

    public ComponentRelationMapperImpl(ComponentRelationData<R, T> data, TransmutationManager transmutationManager) {
        super(data, transmutationManager);

        this.componentType = data.type();
    }

    @Override
    public ComponentRelationType<R, T> componentType() {
        return componentType;
    }

    @Override
    public R getRelationship(int entityId, T target) {
        var relation = data.getComponent(entityId);
        if (relation == null) {
            return null;
        }

        return relation.getRelationship(target);
    }

    @Override
    public ComponentRelation<R, T> add(int entityId, ComponentRelation<R, T> relation) {
        this.add.apply(entityId, relation);

        return relation;
    }

}
