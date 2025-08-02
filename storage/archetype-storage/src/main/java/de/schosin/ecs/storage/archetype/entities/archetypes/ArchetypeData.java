package de.schosin.ecs.storage.archetype.entities.archetypes;

import de.schosin.ecs.api.components.types.ComponentType.RegularComponentType;
import de.schosin.ecs.storage.api.entities.Archetype;
import de.schosin.ecs.storage.api.entities.ArchetypeAccessor;
import de.schosin.ecs.utils.collections.ImmutableBag;

public sealed interface ArchetypeData extends Archetype permits ArchetypeDataSoaImpl {

    ArchetypeAccessor getAccessor(int entityId);


    void addComponents(int entityId, int index, ImmutableBag<? extends RegularComponentType<?, ?>> componentTypes, Object[] components);

    void removeComponents(int entityId, int index, ImmutableBag<? extends RegularComponentType<?, ?>> componentTypes);

    /**
     * Remove the entity at the given index from the archetype.
     * 
     * @param entityId id of entity
     * @param index index of entity
     * @return id of entity swapped to index position, or -1 if no swap
     */
    int removeEntity(int entityId, int index);

    /**
     * Moves the entity according to its pending changes.
     * 
     * @param entityId id of entity
     * @param index index of entity in this archetype
     * @return id of entity moved to index, or -1 if none moved
     */
    int moveEntity(int entityId, int index);

    /**
     * Returns pending changes.
     * 
     * @param index index of entity
     * @return pending changes or null if none
     */
    PendingChanges getPendingChanges(int index);

}
