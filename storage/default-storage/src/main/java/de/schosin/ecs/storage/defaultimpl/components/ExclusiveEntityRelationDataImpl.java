package de.schosin.ecs.storage.defaultimpl.components;

import de.schosin.ecs.api.components.Relation;
import de.schosin.ecs.api.components.Relation.EntityRelation;
import de.schosin.ecs.api.components.Relation.Exclusive;
import de.schosin.ecs.api.components.types.ComponentType.ExclusiveEntityRelationType;
import de.schosin.ecs.storage.api.StorageWorld;
import de.schosin.ecs.storage.api.components.Component.ExclusiveEntityRelationData;
import de.schosin.ecs.utils.collections.Bag;
import de.schosin.ecs.utils.collections.IntBag;

public record ExclusiveEntityRelationDataImpl<R extends Exclusive>(int id, ExclusiveEntityRelationType<R> type, Bag<EntityRelation<R>> components, Bag<IntBag> targetLookup)
        implements ExclusiveEntityRelationData<R> {

    public ExclusiveEntityRelationDataImpl(int id, ExclusiveEntityRelationType<R> type, StorageWorld world) {
        this(id, type, world.createEntityBag(EntityRelation.class), world.createEntityBag(IntBag.class));
    }

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
        return this.components.get(entityId) != null;
    }

    @Override
    public EntityRelation<R> getComponent(int entityId) {
        return this.components.get(entityId);
    }

    @Override
    public void addRelation(int entityId, R relationship, int target) {
        addComponentUnsafe(entityId, Relation.create(relationship, target));
    }

    @Override
    public void addComponentUnsafe(int entityId, EntityRelation<R> component) {
        this.components.set(entityId, component);

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

    @Override
    public void removeComponent(int entityId) {
        var component = this.components.get(entityId);
        if (component != null) {
            this.components.set(entityId, null);

            var entities = this.targetLookup.get(component.target());
            if (entities != null) {
                entities.removeValue(entityId);
            }

            Relation.free(component);
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

            if (this.components.get(entityId) == null) {
                continue;
            }

            this.components.set(entityId, null);

            if (affectedEntities != null) {
                affectedEntities.add(entityId);
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
        return obj instanceof ExclusiveEntityRelationDataImpl<?> data && data.id() == id;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("ExclusiveEntityRelationDataImpl [id=").append(this.id).append(", type=").append(this.type).append("]");
        return builder.toString();
    }

}
