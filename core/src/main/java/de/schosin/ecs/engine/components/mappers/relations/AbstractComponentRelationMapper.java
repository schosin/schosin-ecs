package de.schosin.ecs.engine.components.mappers.relations;

import org.jspecify.annotations.Nullable;

import de.schosin.ecs.api.components.Relation.ComponentRelation;
import de.schosin.ecs.engine.components.TransmutationManager;
import de.schosin.ecs.storage.api.components.Component;

abstract class AbstractComponentRelationMapper<R, T, RR, C extends Component<ComponentRelation<R, T>, RR>> {

    protected C data;

    protected final TransmutationManager.Add<ComponentRelation<R, T>> add;
    protected final TransmutationManager.Remove remove;

    protected AbstractComponentRelationMapper(C data, TransmutationManager transmutationManager) {
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

}
