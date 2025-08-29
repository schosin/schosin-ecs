package de.schosin.ecs.storage.archetype.components.accessors;

import de.schosin.ecs.api.data.ArchetypeComponentAccessor;
import de.schosin.ecs.storage.archetype.entities.archetypes.PendingChanges;
import de.schosin.ecs.utils.collections.Bag;

public record PendingComponentAccessorImpl<T>(int componentId, Bag<PendingChanges> pendingChanges) implements ArchetypeComponentAccessor<T> {

    @Override
    public T getComponent(int index) {
        var changes = pendingChanges.get(index);
        return changes != null ? changes.getComponent(index) : null;
    }

}
