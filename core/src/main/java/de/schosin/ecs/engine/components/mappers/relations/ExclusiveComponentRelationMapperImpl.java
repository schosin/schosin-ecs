package de.schosin.ecs.engine.components.mappers.relations;

import de.schosin.ecs.api.components.Relation.ComponentRelation;
import de.schosin.ecs.api.components.Relation.Exclusive;
import de.schosin.ecs.api.components.mappers.ComponentRelations.ExclusiveComponentRelationMapper;
import de.schosin.ecs.api.components.types.RelationComponentType.ExclusiveComponentRelationType;
import de.schosin.ecs.engine.components.TransmutationManager;
import de.schosin.ecs.storage.api.components.Component.ExclusiveComponentRelationData;

public final class ExclusiveComponentRelationMapperImpl<R extends Exclusive, T> extends AbstractComponentRelationMapper<R, T, ComponentRelation<R, T>, ExclusiveComponentRelationData<R, T>>
        implements ExclusiveComponentRelationMapper<R, T> {

    public interface RelationshipParent {
        void removeFromOthers(int entityId, ExclusiveComponentRelationMapperImpl<?, ?> mapper);
    }

    private final ExclusiveComponentRelationType<R, T> componentType;
    private final RelationshipParent parent;

    public ExclusiveComponentRelationMapperImpl(ExclusiveComponentRelationData<R, T> data, TransmutationManager transmutationManager, RelationshipParent parent) {
        super(data, transmutationManager);

        this.componentType = data.type();
        this.parent = parent;
    }

    @Override
    public ExclusiveComponentRelationType<R, T> componentType() {
        return componentType;
    }

    @Override
    public ComponentRelation<R, T> add(int entityId, ComponentRelation<R, T> relation) {
        this.parent.removeFromOthers(entityId, this);
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
    public T getTarget(int entityId) {
        var relation = get(entityId);
        if (relation == null) {
            return null;
        }

        return relation.target();
    }

}
