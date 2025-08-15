package de.schosin.ecs.engine.components.mappers.relations;

import org.jspecify.annotations.Nullable;

import de.schosin.ecs.api.components.Relation.EntityRelation;
import de.schosin.ecs.api.data.ComponentAccessor;
import de.schosin.ecs.api.data.DataAccessor;
import de.schosin.ecs.engine.components.TransmutationManager;
import de.schosin.ecs.engine.components.mappers.accessors.IndexedAccessorImpl;
import de.schosin.ecs.engine.components.mappers.accessors.PendingAccessorImpl;
import de.schosin.ecs.storage.api.components.Component.EntityRelationComponent;
import de.schosin.ecs.storage.api.components.Component.EntityRelationComponent.RemovedRelationTypeHandler;
import de.schosin.ecs.storage.api.entities.ArchetypeAccessor;
import de.schosin.ecs.utils.collections.Pool;

public abstract class AbstractEntityRelationMapper<R, RR, C extends EntityRelationComponent<R, RR>> {

    protected final C data;
    protected final int componentId;

    protected final TransmutationManager.Add<EntityRelation<R>> add;
    protected final TransmutationManager.Remove remove;

    private final Pool<PendingAccessorImpl<RR>> pendingAccessors;

    protected AbstractEntityRelationMapper(C data, TransmutationManager transmutationManager) {
        this.data = data;
        this.componentId = data.id();

        this.add = transmutationManager.getAddTransmuter(data.type());
        this.remove = transmutationManager.getRemoveTransmuter(data.type());

        this.pendingAccessors = Pool.unbounded(PendingAccessorImpl.class, this::createPendingAccessor);
    }

    private PendingAccessorImpl<RR> createPendingAccessor() {
        return new PendingAccessorImpl<>(data.id(), pendingAccessors);
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

    public ComponentAccessor<RR> getComponentAccessor(DataAccessor accessor) {
        if (!(accessor instanceof ArchetypeAccessor archetypeAccessor)) {
            throw new IllegalArgumentException("Unexpected IterableAccessor not implementing ArchetypeAccessor: " + accessor);
        }

        var archetype = archetypeAccessor.getArchetype();
        var index = archetype.getComponentIndex(componentId);

        return index > -1
                ? IndexedAccessorImpl.getInstance(index)
                : pendingAccessors.getInstance();
    }

    public final boolean remove(int entityId) {
        return this.remove.apply(entityId);
    }

    public final void removeTarget(int entityId, RemovedRelationTypeHandler handler) {
        data.removeTarget(entityId, handler);
    }

}
