package de.schosin.ecs.storage.defaultimpl.components;

import org.jspecify.annotations.NonNull;

import de.schosin.ecs.api.components.ComponentType.ExclusiveComponentRelationType;
import de.schosin.ecs.api.components.Relation.ComponentRelation;
import de.schosin.ecs.api.components.Relation.Exclusive;
import de.schosin.ecs.storage.api.StorageWorld;
import de.schosin.ecs.storage.api.components.Component.ExclusiveComponentRelationData;
import de.schosin.ecs.utils.collections.Bag;
import de.schosin.ecs.utils.collections.Pool;

public record ExclusiveComponentRelationDataImpl<R extends Exclusive, T>(int id, ExclusiveComponentRelationType<R, T> type, Bag<ComponentRelation<R, T>> components,
        Pool<ComponentRelationImpl<R, T>> pool) implements ExclusiveComponentRelationData<R, T> {

    public ExclusiveComponentRelationDataImpl(int id, ExclusiveComponentRelationType<R, T> type, StorageWorld world) {
        this(id, type, world.createEntityBag(ComponentRelation.class),
                Pool.unbounded(ComponentRelationImpl.class, ComponentRelationImpl::new));
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
        return this.components.get(entityId) != null;
    }

    @Override
    public ComponentRelation<R, T> getComponent(int entityId) {
        return this.components.get(entityId);
    }

    @Override
    public void addRelation(int entityId, R relationship, T target) {
        addComponentUnsafe(entityId, pool.getInstance().init(relationship, target));
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

            if (component instanceof ComponentRelationImpl<R, T> impl) {
                this.pool.free(impl);
            }
        }
    }

    @Override
    public ComponentRelation<R, T> getInstance(R relationship, T target) {
        return pool.getInstance().init(relationship, target);
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
