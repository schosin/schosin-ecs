package de.schosin.ecs.storage.defaultimpl.components;

import org.jspecify.annotations.NonNull;

import de.schosin.ecs.api.components.Relation;
import de.schosin.ecs.api.components.Relation.ComponentRelation;
import de.schosin.ecs.api.components.Relation.Exclusive;
import de.schosin.ecs.api.components.types.RelationComponentType.ExclusiveComponentRelationType;
import de.schosin.ecs.storage.api.StorageWorld;
import de.schosin.ecs.storage.api.components.Component.ExclusiveComponentRelationData;
import de.schosin.ecs.storage.common.PendingChanges;
import de.schosin.ecs.utils.collections.Bag;

public record ExclusiveComponentRelationDataImpl<R extends Exclusive, T>(int id, ExclusiveComponentRelationType<R, T> type, Bag<ComponentRelation<R, T>> components, Bag<PendingChanges> changes)
        implements DefaultComponent<ComponentRelation<R, T>>, ExclusiveComponentRelationData<R, T> {

    public ExclusiveComponentRelationDataImpl(int id, ExclusiveComponentRelationType<R, T> type, StorageWorld world, Bag<PendingChanges> changes) {
        this(id, type, world.createEntityBag(ComponentRelation.class), changes);
    }

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
        return getComponent(entityId) != null;
    }

    @Override
    public ComponentRelation<R, T> getComponent(int entityId) {
        var result = this.components.get(entityId);
        if (result != null) {
            return result;
        }

        var changes = this.changes.get(entityId);
        if (changes != null) {
            return changes.getComponent(type);
        }

        return null;
    }

    @Override
    public void addComponentUnsafe(int entityId, @NonNull ComponentRelation<R, T> component) {
        this.components.set(entityId, component);
    }

    @Override
    public void removeComponent(int entityId) {
        var component = this.components.get(entityId);
        if (component != null) {
            this.components.set(entityId, null);

            Relation.free(component);
        }
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
