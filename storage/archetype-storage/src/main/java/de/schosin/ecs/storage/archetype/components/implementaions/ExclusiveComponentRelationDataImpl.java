package de.schosin.ecs.storage.archetype.components.implementaions;

import de.schosin.ecs.api.components.Relation.ComponentRelation;
import de.schosin.ecs.api.components.Relation.Exclusive;
import de.schosin.ecs.api.components.types.RelationComponentType.ExclusiveComponentRelationType;
import de.schosin.ecs.storage.api.components.Component.ExclusiveComponentRelationData;
import de.schosin.ecs.storage.archetype.entities.EntityIndex;

public record ExclusiveComponentRelationDataImpl<R extends Exclusive, T>(int id, ExclusiveComponentRelationType<R, T> type, EntityIndex index) implements ExclusiveComponentRelationData<R, T> {

    @Override
    public Class<R> relationshipClass() {
        return type.relationship();
    }

    @Override
    public Class<T> targetClass() {
        return type.target();
    }

    @Override
    public String display() {
        return "ExclusiveComponentRelation(%s / %s, %d)".formatted(type.relationship().getSimpleName(), type.target().getSimpleName(), id());
    }

    @Override
    public boolean hasComponent(int entityId) {
        return index.hasComponent(entityId, type);
    }

    @Override
    public ComponentRelation<R, T> getComponent(int entityId) {
        return index.getComponent(entityId, type, id);
    }

    @Override
    public int hashCode() {
        return id;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof ExclusiveComponentRelationDataImpl<?, ?> data && data.id() == id;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("ExclusiveComponentRelationDataImpl [id=").append(this.id).append(", type=").append(this.type).append("]");
        return builder.toString();
    }

}
