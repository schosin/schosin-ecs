package de.schosin.ecs.storage.archetype.entities.archetypes;

import java.util.function.IntSupplier;

import de.schosin.ecs.api.components.types.ComponentType.RegularComponentType;
import de.schosin.ecs.storage.api.entities.Archetype;
import de.schosin.ecs.storage.api.entities.ArchetypeAccessor;
import de.schosin.ecs.utils.collections.ImmutableBag;

public sealed interface ArchetypeData extends Archetype permits ArchetypeDataSoaImpl {

    Archetype createEntity(int entityId, Object[] components, ArchetypeComponentProvider componentProvider);

    Archetype createEntities(int count, IntSupplier entityIdSupplier, ComponentsInitializer componentsConsumer, ArchetypeComponentProvider componentProvider);

    void setComponentProvider(ArchetypeComponentProvider componentProvider, ArchetypeData actualArchetype);

    ArchetypeAccessor getAccessor(int index);

    /**
     * Updates previousAccessor with the new index.
     * 
     * <p>
     * The {@code previousAccessor} must have been retrieved from the same archetype using
     * {@link #getAccessor(int)}. Failing to do so will simply update the index, returning 
     * the an accessor for a different entity. 
     * 
     * @param index new index
     * @param previousAccessor previousAccessor
     */
    void updateAccessor(int index, ArchetypeAccessor previousAccessor);

    void addComponents(int entityId, int index, ImmutableBag<? extends RegularComponentType<?, ?>> componentTypes, Object[] components);

    void removeComponents(int entityId, int index, ImmutableBag<? extends RegularComponentType<?, ?>> componentTypes);

    /**
     * Moves the entity according to its pending changes.
     * 
     * @param entityId id of entity
     * @param newArchetypeNode node of new archetype
     * @param index index of entity in this archetype
     */
    void moveEntity(int entityId, ArchetypeGraphNode newArchetypeNode, int index);

    /**
     * Returns pending changes.
     * 
     * @param index index of entity
     * @return pending changes or null if none
     */
    PendingChanges getPendingChanges(int index);

    /**
     * Marks the entity for deletion during the next {@link #process()} call.
     * 
     * @param entityId id of the entity
     * @param index current index of entity in archetype
     */
    void markDeleted(int entityId, int index);

    /**
     * Processes pending deletions and component composition updates of entities.
     */
    void process();

}
