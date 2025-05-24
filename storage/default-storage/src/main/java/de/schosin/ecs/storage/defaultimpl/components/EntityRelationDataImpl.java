package de.schosin.ecs.storage.defaultimpl.components;

import java.util.Iterator;
import java.util.Objects;

import org.jspecify.annotations.NonNull;

import de.schosin.ecs.api.Pooled;
import de.schosin.ecs.api.components.Relation;
import de.schosin.ecs.api.components.Relation.EntityRelation;
import de.schosin.ecs.api.components.Result.EntityRelationResult;
import de.schosin.ecs.api.components.types.ComponentType.EntityRelationType;
import de.schosin.ecs.storage.api.StorageWorld;
import de.schosin.ecs.storage.api.components.Component.EntityRelationData;
import de.schosin.ecs.utils.collections.Bag;
import de.schosin.ecs.utils.collections.BagIterator;
import de.schosin.ecs.utils.collections.IntBag;
import de.schosin.ecs.utils.collections.Pool;

public record EntityRelationDataImpl<R>(int id, EntityRelationType<R> type, Bag<EntityRelationResultImpl<R>> components, Bag<IntBag> targetLookup,
        Pool<EntityRelationResultImpl<R>> resultPool) implements EntityRelationData<R> {

    public EntityRelationDataImpl(int id, EntityRelationType<R> type, StorageWorld world) {
        this(id, type, world.createEntityBag(EntityRelationResult.class), world.createEntityBag(IntBag.class),
                Pool.unbounded(EntityRelationResultImpl.class, EntityRelationResultImpl::new));
    }

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
        return this.components.get(entityId) != null;
    }

    @Override
    public EntityRelationResult<R> getComponent(int entityId) {
        return this.components.get(entityId);
    }

    @Override
    public void addRelation(int entityId, R relationship, int target) {
        addComponentUnsafe(entityId, Relation.create(relationship, target));
    }

    @Override
    public void addComponentUnsafe(int entityId, EntityRelation<R> component) {
        var relations = this.components.get(entityId);
        if (relations != null) {
            addRelation(entityId, relations, component);
            return;
        }

        synchronized (this.components) {
            relations = this.components.get(entityId);
            if (relations != null) {
                addRelation(entityId, relations, component);
                return;
            }

            relations = resultPool.getInstance();
            this.components.set(entityId, relations);
        }

        addRelation(entityId, relations, component);
    }

    private void addRelation(int entityId, EntityRelationResultImpl<R> relations, EntityRelation<R> component) {
        synchronized (relations) {
            relations.add(component);

            var entities = this.targetLookup.get(component.target());
            if (entities == null) {
                synchronized (this.targetLookup) {
                    entities = this.targetLookup.get(component.target());
                    if (entities == null) {
                        entities = new IntBag(4);
                        this.targetLookup.set(component.target(), entities);
                    }
                }
            }

            if (!entities.contains(component.target())) {
                entities.add(entityId);
            }
        }
    }

    @Override
    public void removeComponent(int entityId) {
        var component = this.components.get(entityId);
        if (component != null) {
            this.components.set(entityId, null);

            for (int i = 0, s = component.size(); i < s; i++) {
                var relation = component.get(i);

                var entities = this.targetLookup.get(relation.target());
                if (entities != null) {
                    entities.removeValue(entityId);
                }

                Relation.free(relation);
            }

            this.resultPool.free(component);
        }
    }

    @Override
    public void removeTarget(int target, IntBag affectedEntities) {
        var entities = targetLookup.get(target);
        if (entities == null) {
            return;
        }

        var data = entities.getData();
        for (int i = 0, s = entities.getSize(); i < s; i++) {
            var entityId = data[i];

            var result = this.components.get(entityId);
            if (result == null) {
                continue;
            }

            result.removeTarget(target);

            if (result.isEmpty()) {
                this.components.set(entityId, null);
                this.resultPool.free(result);

                if (affectedEntities != null) {
                    affectedEntities.add(entityId);
                }
            }
        }

        entities.clear();
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
        builder.append("ExclusiveEntityRelationDataImpl [id=").append(this.id).append(", type=").append(this.type).append("]");
        return builder.toString();
    }

}

class EntityRelationResultImpl<R> implements EntityRelationResult<R>, Pooled {

    private final Bag<EntityRelation<R>> relations = new Bag<>(EntityRelation.class, 4);
    private final Bag<EntityRelation<R>> targetLookup = new Bag<>(EntityRelation.class, 4);

    public synchronized void add(EntityRelation<R> relation) {
        targetLookup.set(relation.target(), relation);

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

    public void removeTarget(int target) {
        var relation = this.targetLookup.get(target);
        if (relation == null) {
            return;
        }

        this.targetLookup.set(target, null);
        this.relations.remove(relation);
    }

    @Override
    public @NonNull EntityRelation<R> get(int i) {
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
    public R getRelationship(int target) {
        var data = relations.getData();
        for (int i = 0, s = relations.getSize(); i < s; i++) {
            var relation = data[i];
            if (relation.target() == target) {
                return relation.relationship();
            }
        }

        return null;
    }

    @Override
    public Iterator<EntityRelation<R>> iterator() {
        return new BagIterator<>(relations);
    }

    @Override
    public void reset() {
        this.relations.clear();
    }

}
