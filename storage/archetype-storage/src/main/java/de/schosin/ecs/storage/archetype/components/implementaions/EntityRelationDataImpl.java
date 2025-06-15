package de.schosin.ecs.storage.archetype.components.implementaions;

import de.schosin.ecs.api.components.Relations.EntityRelations;
import de.schosin.ecs.api.components.types.RelationComponentType.EntityRelationType;
import de.schosin.ecs.storage.api.components.Component.EntityRelationData;
import de.schosin.ecs.storage.archetype.entities.EntityIndex;
import de.schosin.ecs.storage.archetype.entities.EntityRelationIndex;

public record EntityRelationDataImpl<R>(int id, EntityRelationType<R> type, EntityIndex index, EntityRelationIndex relationIndex) implements EntityRelationData<R> {

    @Override
    public Class<R> relationshipClass() {
        return type.relationship();
    }

    @Override
    public String display() {
        return "EntityRelation(%s, %d)".formatted(type.relationship().getSimpleName(), id());
    }

    @Override
    public boolean hasComponent(int entityId) {
        return index.hasComponent(entityId, type);
    }

    @Override
    public EntityRelations<R> getComponent(int entityId) {
        return index.getComponent(entityId, type);
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
        return obj instanceof EntityRelationDataImpl<?> data && data.id() == id;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("EntityRelationDataImpl [id=").append(this.id).append(", type=").append(this.type).append("]");
        return builder.toString();
    }

}
