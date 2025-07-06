package de.schosin.ecs.storage.archetype.entities.archetypes;

import de.schosin.ecs.api.components.types.ComponentType.RegularComponentType;
import de.schosin.ecs.storage.api.entities.Archetype;
import de.schosin.ecs.storage.api.entities.ArchetypeAccessor;
import de.schosin.ecs.utils.collections.Bag;
import de.schosin.ecs.utils.collections.BitVector;
import de.schosin.ecs.utils.collections.ImmutableBag;

public sealed interface ArchetypeData extends Archetype permits ArchetypeDataSoaImpl {

    boolean contains(int index, RegularComponentType<?, ?> type);

    <R> R getComponent(int index, RegularComponentType<?, R> componentType, int componentId);

    ArchetypeAccessor getAccessor(int entityId);

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

    /**
     * Returns the archetype if {@code componentType} is addded to the component types of this archetype.
     * Will return this archetype if the component type is already present.
     * 
     * <p>
     * Along with {@link #removeComponentType(RegularComponentType)}, this method is responsible for the
     * archetype graph used to resolve the correct archetype given a specific component type composition.
     * </p>
     * 
     * <p>
     * To resolve the correct archetype, start with the empty archetype and then add the components one-by-one
     * in a loop.
     * </p>
     * 
     * @param componentType added component type
     * @return archetype for all component types of this archetype and the passed one
     */
    ArchetypeData addComponentType(RegularComponentType<?, ?> componentType);

    /**
     * Returns the archetype if {@code componentType} is removed from the component ypes of this archetype.
     * Will return this archetype if the component is not present.
     * 
     * <p>
     * Along with {@link #addComponentType(RegularComponentType)}, this method is responsible for the
     * archetype graph used to resolve the correct archetype given a specific component type composition.
     * </p>
     * 
     * @param componentType removed component type
     * @return archetype for all component types of this archetype minus the passed one
     */
    ArchetypeData removeComponentType(RegularComponentType<?, ?> componentType);
    
    BitVector getComponentIds();

    Bag<ArchetypeData> getAdd();

    Bag<ArchetypeData> getRemove();

}
