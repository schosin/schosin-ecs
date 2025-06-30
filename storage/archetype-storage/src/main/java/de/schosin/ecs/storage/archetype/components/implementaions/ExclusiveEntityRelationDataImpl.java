package de.schosin.ecs.storage.archetype.components.implementaions;

import de.schosin.ecs.api.components.Relation.EntityRelation;
import de.schosin.ecs.api.components.Relation.Exclusive;
import de.schosin.ecs.api.components.types.RelationComponentType.ExclusiveEntityRelationType;
import de.schosin.ecs.storage.api.components.Component.ExclusiveEntityRelationData;
import de.schosin.ecs.storage.archetype.entities.EntityIndex;
import de.schosin.ecs.storage.archetype.entities.EntityRelationIndex;

public record ExclusiveEntityRelationDataImpl<R extends Exclusive>(int id, ExclusiveEntityRelationType<R> type, EntityIndex index, EntityRelationIndex relationIndex)
        implements ExclusiveEntityRelationData<R> {

    @Override
    public Class<R> relationshipClass() {
        return type.relationship();
    }

    @Override
    public String display() {
        return "ExclusiveEntityRelation(%s, %d)".formatted(type.relationship().getSimpleName(), id());
    }

    @Override
    public boolean hasComponent(int entityId) {
        return index.hasComponent(entityId, type);
    }

    @Override
    public EntityRelation<R> getComponent(int entityId) {
        return index.getComponent(entityId, type, id);
    }

    @Override
    public void removeTarget(int target, RemovedRelationTypeHandler handler) {
        relationIndex.removeTarget(target, handler, index);
    }

    @Override
    public int hashCode() {
        return id;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof ExclusiveEntityRelationDataImpl<?> data && data.id() == id;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("ExclusiveEntityRelationDataImpl [id=").append(this.id).append(", type=").append(this.type).append("]");
        return builder.toString();
    }

}
