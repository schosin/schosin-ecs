package de.schosin.ecs.storage.defaultimpl.components;

import java.util.Iterator;
import java.util.Objects;

import org.jspecify.annotations.NonNull;

import de.schosin.ecs.api.Pooled;
import de.schosin.ecs.api.components.ComponentType.ComponentRelationType;
import de.schosin.ecs.api.components.Relation.ComponentRelation;
import de.schosin.ecs.api.components.Result.ComponentRelationResult;
import de.schosin.ecs.storage.api.StorageWorld;
import de.schosin.ecs.storage.api.components.Component.ComponentRelationData;
import de.schosin.ecs.utils.collections.Bag;
import de.schosin.ecs.utils.collections.BagIterator;
import de.schosin.ecs.utils.collections.Pool;

public record ComponentRelationDataImpl<R, T>(int id, ComponentRelationType<R, T> type, Bag<ComponentRelationResultImpl<R, T>> components,
        Pool<ComponentRelationResultImpl<R, T>> resultPool, Pool<ComponentRelationImpl<R, T>> pool) implements ComponentRelationData<R, T> {

    public ComponentRelationDataImpl(int id, ComponentRelationType<R, T> type, StorageWorld world) {
        this(id, type, world.createEntityBag(ComponentRelationResult.class),
                Pool.unbounded(ComponentRelationResultImpl.class, ComponentRelationResultImpl::new),
                Pool.unbounded(ComponentRelationImpl.class, ComponentRelationImpl::new));
    }

    @Override
    public int id() {
        return id;
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
        return this.components.get(entityId) != null;
    }

    @Override
    public ComponentRelationResult<R, T> getComponent(int entityId) {
        return this.components.get(entityId);
    }

    @Override
    public void addRelation(int entityId, R relationship, T target) {
        addComponentUnsafe(entityId, pool.getInstance().init(relationship, target));
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

            relations = resultPool.getInstance();
            this.components.set(entityId, relations);
        }

        relations.add(component);
    }

    @Override
    public void removeComponent(int entityId) {
        var component = this.components.get(entityId);
        if (component != null) {
            this.components.set(entityId, null);

            for (int i = 0, s = component.size(); i < s; i++) {
                var relation = component.get(i);

                if (relation instanceof ComponentRelationImpl<R, T> impl) {
                    this.pool.free(impl);
                }
            }

            this.resultPool.free(component);
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
        return obj instanceof ComponentRelationDataImpl<?, ?> data && data.id() == id;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("ComponentRelationDataImpl [id=").append(this.id).append(", type=").append(this.type).append("]");
        return builder.toString();
    }

}

class ComponentRelationImpl<R, T> implements ComponentRelation<R, T>, Pooled {

    private R relationship;
    private T target;

    ComponentRelationImpl<R, T> init(R relationship, T target) {
        this.relationship = relationship;
        this.target = target;

        return this;
    }

    @Override
    public R relationship() {
        return relationship;
    }

    @Override
    public T target() {
        return target;
    }

    @Override
    public void reset() {
        this.relationship = null;
        this.target = null;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("ComponentRelationImpl [relationship=").append(this.relationship).append(", target=").append(this.target).append("]");
        return builder.toString();
    }

    @Override
    public int hashCode() {
        return Objects.hash(relationship, target);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof ComponentRelation<?, ?> other && Objects.equals(this.relationship, other.relationship()) && Objects.equals(this.target, other.target());
    }

}

class ComponentRelationResultImpl<R, T> implements ComponentRelationResult<R, T>, Pooled {

    private final Bag<ComponentRelation<R, T>> relations = new Bag<>(ComponentRelation.class, 4);

    private final ThreadLocal<BagIterator<ComponentRelation<R, T>>> iterator = ThreadLocal.withInitial(BagIterator::new);

    public synchronized void add(ComponentRelation<R, T> relation) {
        var data = relations.getData();
        for (int i = 0, s = relations.getSize(); i < s; i++) {
            var existing = data[i];
            if (Objects.equals(existing.target(), relation.target())) {
                relations.set(i, relation);
                return;
            }
        }

        this.relations.add(relation);
    }

    @Override
    public @NonNull ComponentRelation<R, T> get(int i) {
        return this.relations.get(i);
    }

    @Override
    public int size() {
        return relations.getSize();
    }

    @Override
    public boolean isEmpty() {
        return relations.isEmpty();
    }

    @Override
    public R getRelationship(T target) {
        var data = relations.getData();
        for (int i = 0, s = relations.getSize(); i < s; i++) {
            var relation = data[i];
            if (Objects.equals(relation.target(), target)) {
                return relation.relationship();
            }
        }

        return null;
    }

    @Override
    public Iterator<ComponentRelation<R, T>> iterator() {
        return iterator.get().init(this.relations);
    }

    @Override
    public void reset() {
        this.relations.clear();
    }

}
