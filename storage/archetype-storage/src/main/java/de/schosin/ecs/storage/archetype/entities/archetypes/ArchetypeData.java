package de.schosin.ecs.storage.archetype.entities.archetypes;

import de.schosin.ecs.api.components.types.ComponentType.RegularComponentType;
import de.schosin.ecs.api.data.DataAccessor;
import de.schosin.ecs.storage.api.entities.Archetype;
import de.schosin.ecs.storage.common.PendingChanges;
import de.schosin.ecs.utils.collections.Bag;
import de.schosin.ecs.utils.collections.ImmutableBag;

public sealed interface ArchetypeData extends Archetype permits ArchetypeDataSoaImpl {

    boolean contains(int index, RegularComponentType<?, ?> type);

    <R> R getComponent(int index, RegularComponentType<?, R> componentType);

    DataAccessor getAccessor(int entityId);

    int addEntity(int entityId, ImmutableBag<RegularComponentType<?, ?>> componentTypes, Bag<Object> components,
            ImmutableBag<? extends RegularComponentType<?, ?>> componentTypes2, ImmutableBag<Object> components2);

    void addComponents(int entityId, int index, ImmutableBag<? extends RegularComponentType<?, ?>> componentTypes, Object[] components);

    void removeComponents(int entityId, int index, ImmutableBag<? extends RegularComponentType<?, ?>> componentTypes);

    /**
     * Remove the entity at the given index from the archetype.
     * 
     * @param entityId id of entity
     * @param index index of entity
     * @param fill bag that will contain components of deleted entity
     * @return id of entity swapped to index position, or -1 if no swap
     */
    int removeEntity(int entityId, int index, Bag<Object> fill);

    /**
     * Returns pending changes.
     * 
     * @param index index of entity
     * @return pending changes or null if none
     */
    PendingChanges getPendingChanges(int index);

}
