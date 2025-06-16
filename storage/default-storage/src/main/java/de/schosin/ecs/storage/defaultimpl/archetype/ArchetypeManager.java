package de.schosin.ecs.storage.defaultimpl.archetype;

import de.schosin.ecs.storage.api.ComponentStorage;
import de.schosin.ecs.storage.api.StorageWorld;
import de.schosin.ecs.storage.api.events.ArchetypeAddedEvent;
import de.schosin.ecs.storage.defaultimpl.EntityStorageImpl;
import de.schosin.ecs.storage.defaultimpl.entities.ComponentMaskImpl;
import de.schosin.ecs.utils.collections.Bag;
import de.schosin.ecs.utils.collections.IntBag;

public class ArchetypeManager {

    private final StorageWorld world;
    private final ComponentStorage componentStorage;
    private final EntityStorageImpl entityStorage;

    private final Bag<ArchetypeImpl> archetypes = new Bag<>(ArchetypeImpl.class, 64);
    private final IntBag archetypesByEntityId;

    public ArchetypeManager(StorageWorld world, ComponentStorage componentStorage, EntityStorageImpl entityStorage) {
        this.world = world;
        this.componentStorage = componentStorage;
        this.entityStorage = entityStorage;

        this.archetypesByEntityId = world.createEntityIntBag();
    }

    public void set(int entityId, ComponentMaskImpl componentMask) {
        // remove from old archetype
        remove(entityId);

        // add to new archetype
        var archetype = getArchetype(componentMask);
        var archetypeId = archetype.getId();

        archetype.add(entityId);
        archetypesByEntityId.set(entityId, archetypeId == 0 ? -1 : archetypeId);
    }

    public void remove(int entityId) {
        var archetype = getArchetypeForEntity(entityId);
        if (archetype == null) {
            return;
        }

        archetype.remove(entityId);
        archetypesByEntityId.set(entityId, 0);
    }

    public ArchetypeImpl getArchetypeForEntity(int entityId) {
        var archetypeId = archetypesByEntityId.get(entityId);
        if (archetypeId == 0) {
            return null;
        }

        return archetypes.get(archetypeId == -1 ? 0 : archetypeId);
    }

    public ArchetypeImpl getArchetypeById(int archetypeId) {
        return archetypes.get(archetypeId);
    }

    public ArchetypeImpl getArchetype(ComponentMaskImpl componentMask) {
        var result = archetypes.getSafe(componentMask.getId());
        if (result != null) {
            return result;
        }

        synchronized (archetypes) {
            result = archetypes.getSafe(componentMask.getId());
            if (result != null) {
                return result;
            }

            var archetype = new ArchetypeImpl(componentStorage, entityStorage, componentMask);
            this.archetypes.set(componentMask.getId(), archetype);

            this.world.dispatchEvent(new ArchetypeAddedEvent(componentMask, archetype));

            return archetype;
        }
    }

}
