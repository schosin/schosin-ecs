package de.schosin.ecs.engine.components.mappers.relations;

import org.jspecify.annotations.Nullable;

import de.schosin.ecs.api.components.Relation.EntityRelation;
import de.schosin.ecs.engine.components.TransmutationManager;
import de.schosin.ecs.storage.api.components.Component.EntityRelationComponent;
import de.schosin.ecs.storage.api.components.Component.EntityRelationComponent.RemovedRelationTypeHandler;

public abstract class AbstractEntityRelationMapper<R, RR, C extends EntityRelationComponent<R, RR>> {

    protected C data;

    protected final TransmutationManager.Add<EntityRelation<R>> add;
    protected final TransmutationManager.Remove remove;

    protected AbstractEntityRelationMapper(C data, TransmutationManager transmutationManager) {
        this.data = data;

        this.add = transmutationManager.getAddTransmuter(data.type());
        this.remove = transmutationManager.getRemoveTransmuter(data.type());
    }

    public boolean has(int entityId) {
        return data.hasComponent(entityId);
    }

    @Nullable
    public RR get(int entityId) {
        return data.getComponent(entityId);
    }

    public boolean remove(int entityId) {
        return this.remove.apply(entityId);
    }

    public void removeTarget(int entityId, RemovedRelationTypeHandler handler) {
        data.removeTarget(entityId, handler);
    }

}
