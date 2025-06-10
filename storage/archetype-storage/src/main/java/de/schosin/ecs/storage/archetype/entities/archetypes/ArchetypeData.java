package de.schosin.ecs.storage.archetype.entities.archetypes;

import de.schosin.ecs.api.components.types.ComponentType.RegularComponentType;
import de.schosin.ecs.storage.api.entities.Archetype;
import de.schosin.ecs.storage.common.PendingChanges;
import de.schosin.ecs.utils.collections.Bag;
import de.schosin.ecs.utils.collections.ImmutableBag;

public interface ArchetypeData extends Archetype {

    boolean contains(long index, RegularComponentType<?, ?> type);

    <R> R getComponent(long index, RegularComponentType<?, R> componentType);

    /**
     * Add an entity, returning its index.
     * 
     * @param id of entity
     * @param componentTypes component types matching components
     * @param components components to add
     * @return index of entity
     */
    long addEntity(int entityId, ImmutableBag<? extends RegularComponentType<?, ?>> componentTypes, ImmutableBag<Object> components);

    long addEntity(int entityId, ImmutableBag<RegularComponentType<?, ?>> componentTypes, Bag<Object> components,
            ImmutableBag<? extends RegularComponentType<?, ?>> componentTypes2, Object[] components2);

    long addEntity(int entityId, ImmutableBag<RegularComponentType<?, ?>> componentTypes, Bag<Object> components,
            ImmutableBag<? extends RegularComponentType<?, ?>> componentTypes2, ImmutableBag<Object> components2);

    void addComponents(int entityId, long index, ImmutableBag<? extends RegularComponentType<?, ?>> componentTypes, Object[] components);

    void removeComponents(int entityId, long index, ImmutableBag<? extends RegularComponentType<?, ?>> componentTypes);

    /**
     * Remove the entity at the given index from the archetype.
     * 
     * @param entityId id of entity
     * @param index index of entity
     * @param fill bag that will contain components of deleted entity
     * @return id of entity swapped to index position, or -1 if no swap
     */
    int removeEntity(int entityId, long index, Bag<Object> fill);

    /**
     * Returns pending changes.
     * 
     * @param index index of entity
     * @return pending changes or null if none
     */
    PendingChanges getPendingChanges(long index);

}
