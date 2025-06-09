package de.schosin.ecs.storage.defaultimpl.components;

import de.schosin.ecs.api.components.Relation.EntityRelation;
import de.schosin.ecs.api.components.Result.EntityRelationResult;
import de.schosin.ecs.api.components.types.RelationComponentType.EntityRelationType;
import de.schosin.ecs.storage.api.StorageWorld;
import de.schosin.ecs.storage.api.components.Component.EntityRelationData;
import de.schosin.ecs.storage.common.PendingChanges;
import de.schosin.ecs.storage.common.results.EntityRelationResultImpl;
import de.schosin.ecs.utils.collections.Bag;
import de.schosin.ecs.utils.collections.IntBag;

public record EntityRelationDataImpl<R>(int id, EntityRelationType<R> type, Bag<EntityRelationResultImpl> components, Bag<PendingChanges> changes, Bag<IntBag> targetLookup)
        implements DefaultComponent<EntityRelation<R>>, EntityRelationData<R> {

    public EntityRelationDataImpl(int id, EntityRelationType<R> type, StorageWorld world, Bag<PendingChanges> changes) {
        this(id, type, world.createEntityBag(EntityRelationResult.class), changes, world.createEntityBag(IntBag.class));
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
        return getComponent(entityId) != null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public EntityRelationResult<R> getComponent(int entityId) {
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

            relations = EntityRelationResultImpl.getInstance();
            this.components.set(entityId, relations);
        }

        addRelation(entityId, relations, component);
    }

    private void addRelation(int entityId, EntityRelationResultImpl relations, EntityRelation<R> component) {
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

            component.free();
        }
    }

    @Override
    public void removeTarget(int target, RemovedRelationTypeHandler handler) {
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
                handler.removeRelationType(entityId, type);
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
        builder.append("EntityRelationDataImpl [id=").append(this.id).append(", type=").append(this.type).append("]");
        return builder.toString();
    }

}
