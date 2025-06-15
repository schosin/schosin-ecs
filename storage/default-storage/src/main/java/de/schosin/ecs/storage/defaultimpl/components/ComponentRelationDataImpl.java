package de.schosin.ecs.storage.defaultimpl.components;

import org.jspecify.annotations.NonNull;

import de.schosin.ecs.api.components.Relation.ComponentRelation;
import de.schosin.ecs.api.components.Relations.ComponentRelations;
import de.schosin.ecs.api.components.types.RelationComponentType.ComponentRelationType;
import de.schosin.ecs.storage.api.StorageWorld;
import de.schosin.ecs.storage.api.components.Component.ComponentRelationData;
import de.schosin.ecs.storage.common.PendingChanges;
import de.schosin.ecs.storage.common.results.ComponentRelationResultImpl;
import de.schosin.ecs.utils.collections.Bag;

public record ComponentRelationDataImpl<R, T>(int id, ComponentRelationType<R, T> type, Bag<ComponentRelationResultImpl> components, Bag<PendingChanges> changes)
        implements DefaultComponent<ComponentRelation<R, T>>, ComponentRelationData<R, T> {

    public ComponentRelationDataImpl(int id, ComponentRelationType<R, T> type, StorageWorld world, Bag<PendingChanges> changes) {
        this(id, type, world.createEntityBag(ComponentRelations.class), changes);
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
        return "ComponentRelation(%s / %s, %d)".formatted(type.relationship().getSimpleName(), type.target().getSimpleName(), id());
    }

    @Override
    public boolean hasComponent(int entityId) {
        return getComponent(entityId) != null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public ComponentRelations<R, T> getComponent(int entityId) {
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
        var relations = this.components.get(entityId);
        if (relations != null) {
            relations.add(component);
            return;
        }

        synchronized (this.components) {
            relations = this.components.get(entityId);
            if (relations != null) {
                relations.add(component);
                return;
            }

            relations = ComponentRelationResultImpl.getInstance();
            this.components.set(entityId, relations);
        }

        relations.add(component);
    }

    @Override
    public void removeComponent(int entityId) {
        var component = this.components.get(entityId);
        if (component != null) {
            this.components.set(entityId, null);

            component.free();
        }
    }

    @Override
    public int hashCode() {
        return id;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof ComponentRelationDataImpl<?, ?> data && data.id() == id;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("ComponentRelationDataImpl [id=").append(this.id).append(", type=").append(this.type).append("]");
        return builder.toString();
    }

}
