package de.schosin.ecs.engine.components.mappers.relations;

import org.jspecify.annotations.Nullable;

import de.schosin.ecs.api.components.Relation.EntityRelation;
import de.schosin.ecs.api.data.DataAccessor;
import de.schosin.ecs.engine.components.TransmutationManager;
import de.schosin.ecs.engine.components.mappers.ComponentConverter;
import de.schosin.ecs.storage.api.components.Component.EntityRelationComponent;
import de.schosin.ecs.storage.api.components.Component.EntityRelationComponent.RemovedRelationTypeHandler;
import de.schosin.ecs.storage.api.entities.Archetype;

public abstract class AbstractEntityRelationMapper<R, RR, C extends EntityRelationComponent<R, RR>> implements ComponentConverter.Factory<RR> {

    protected final C data;
    protected final int componentId;

    protected final TransmutationManager.Add<EntityRelation<R>> add;
    protected final TransmutationManager.Remove remove;

    protected AbstractEntityRelationMapper(C data, TransmutationManager transmutationManager) {
        this.data = data;
        this.componentId = data.id();

        this.add = transmutationManager.getAddTransmuter(data.type());
        this.remove = transmutationManager.getRemoveTransmuter(data.type());
    }

    public final int componentId() {
        return componentId;
    }

    public final boolean has(int entityId) {
        return data.hasComponent(entityId);
    }

    @Nullable
    public final RR get(int entityId) {
        return data.getComponent(entityId);
    }

    public final RR access(DataAccessor accessor) {
        return accessor.getComponent(componentId);
    }

    @Override
    public final ComponentConverter<RR> getConverter(Archetype archetype) {
        var index = archetype.getComponentIndex(componentId);
        return index > -1
                ? ComponentConverter.indexed(index)
                : ComponentConverter.pending(componentId);
    }

    public final boolean remove(int entityId) {
        return this.remove.apply(entityId);
    }

    public final void removeTarget(int entityId, RemovedRelationTypeHandler handler) {
        data.removeTarget(entityId, handler);
    }

}
