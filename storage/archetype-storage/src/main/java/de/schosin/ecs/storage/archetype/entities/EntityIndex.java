package de.schosin.ecs.storage.archetype.entities;

import de.schosin.ecs.api.Pooled;
import de.schosin.ecs.api.components.Relation;
import de.schosin.ecs.api.components.types.ComponentType.RegularComponentType;
import de.schosin.ecs.api.data.DataAccessor;
import de.schosin.ecs.storage.api.StorageEngineException;
import de.schosin.ecs.storage.api.StorageWorld;
import de.schosin.ecs.storage.api.entities.ComponentMask;
import de.schosin.ecs.storage.api.events.ArchetypeAddedEvent;
import de.schosin.ecs.storage.archetype.ArchetypeStorageConfig;
import de.schosin.ecs.storage.archetype.components.ComponentIndex;
import de.schosin.ecs.storage.archetype.entities.archetypes.ArchetypeData;
import de.schosin.ecs.storage.archetype.entities.archetypes.ArchetypeDataImpl;
import de.schosin.ecs.storage.archetype.entities.archetypes.ArchetypeDataSoaImpl;
import de.schosin.ecs.storage.common.PendingChanges;
import de.schosin.ecs.storage.common.results.ComponentRelationResultImpl;
import de.schosin.ecs.storage.common.results.EntityRelationResultImpl;
import de.schosin.ecs.utils.collections.Bag;
import de.schosin.ecs.utils.collections.ImmutableBag;
import de.schosin.ecs.utils.collections.Pool;

/**
 * Fast entity to archetype lookup.
 * 
 * Inspired by implementation in Flecs: https://ajmmertens.medium.com/building-an-ecs-storage-in-pictures-642b8bfd6e04 
 */
public class EntityIndex {

    private final StorageWorld world;
    private final ArchetypeStorageConfig config;

    private final ComponentIndex componentIndex;
    private final EntityRelationIndex relationIndex;

    private final Bag<ArchetypePointer> lookup;
    private final Bag<ArchetypeData> archetypes;

    private final Pool<Bag<Object>> componentsPool = Pool.unbounded(Bag.class, () -> new Bag<>(Object.class), Bag::clear);

    public EntityIndex(StorageWorld world, ArchetypeStorageConfig config, ComponentIndex componentIndex, EntityRelationIndex relationIndex) {
        this.world = world;
        this.config = config;

        this.componentIndex = componentIndex;
        this.relationIndex = relationIndex;

        this.lookup = world.createEntityBag(ArchetypePointer.class);
        this.archetypes = new Bag<>(ArchetypeData.class, 8);
    }

    public ArchetypeData getArchetypeDataById(int archetypeId) {
        return archetypes.get(archetypeId);
    }

    public ArchetypeData getArchetypeDataForEntity(int entityId) {
        var pointer = lookup.getSafe(entityId);
        if (pointer == null) {
            return null;
        }

        return pointer.archetype;
    }

    public ArchetypeData getArchetype(ComponentMaskImpl componentMask) {
        return determineArchetype(componentMask);
    }

    public DataAccessor getAccessor(int entityId) {
        var pointer = lookup.getSafe(entityId);
        if (pointer == null || pointer.archetype == null) {
            throw new StorageEngineException("Cannot get accessor for entity %d: Entity not present in storage".formatted(entityId));
        }

        return pointer.archetype.getAccessor(entityId);
    }

    public ComponentMask getComponentMask(int entityId) {
        var archetype = getArchetypeDataForEntity(entityId);
        if (archetype == null) {
            return null;
        }

        return archetype.getComponentMask();
    }

    public boolean hasComponent(int entityId, RegularComponentType<?, ?> componentType) {
        var pointer = lookup.getSafe(entityId);
        if (pointer == null || pointer.archetype == null) {
            return false;
        }

        return pointer.archetype.contains(pointer.index, componentType);
    }

    public <R> R getComponent(int entityId, RegularComponentType<?, R> componentType) {
        // Lookup pointer
        var pointer = lookup.getSafe(entityId);
        if (pointer == null || pointer.archetype == null) {
            return null; // TODO this should throw, shouldn't it?
        }

        return pointer.archetype.getComponent(pointer.index, componentType);
    }

    public void addComponents(int entityId, ImmutableBag<? extends RegularComponentType<?, ?>> componentTypes, Object[] components) {
        // Lookup pointer
        var pointer = lookup.get(entityId);
        if (pointer == null || pointer.archetype == null) {
            return; // TODO this should throw, shouldn't it?
        }

        pointer.archetype.addComponents(entityId, pointer.index, componentTypes, components);
    }

    public void removeComponents(int entityId, ImmutableBag<? extends RegularComponentType<?, ?>> removeTypes) {
        // Lookup pointer
        var pointer = lookup.get(entityId);
        if (pointer == null || pointer.archetype == null) {
            return; // TODO this should throw, shouldn't it?
        }

        pointer.archetype.removeComponents(entityId, pointer.index, removeTypes);
    }

    public void modifyComponents(int entityId, ImmutableBag<? extends RegularComponentType<?, ?>> addTypes, Object[] add,
            ImmutableBag<? extends RegularComponentType<?, ?>> removeTypes) {

        // Remove if no added components
        if (add.length == 0) {
            removeComponents(entityId, removeTypes);
            return;
        }

        // Add if no removed components
        if (removeTypes.isEmpty()) {
            addComponents(entityId, addTypes, add);
            return;
        }

        // Lookup pointer
        var pointer = lookup.get(entityId);
        if (pointer == null || pointer.archetype == null) {
            return; // TODO this should throw, shouldn't it?
        }

        pointer.archetype.addComponents(entityId, pointer.index, addTypes, add);
        pointer.archetype.removeComponents(entityId, pointer.index, removeTypes);
    }

    public void add(ArchetypeData archetype, int entityId, int index) {
        var pointer = lookup.getSafe(entityId);
        if (pointer == null) {
            pointer = new ArchetypePointer();
            lookup.set(entityId, pointer);
        }

        pointer.archetype = archetype;
        pointer.index = index;
    }

    public void createEntity(int entityId, ComponentMaskImpl componentMask, ImmutableBag<? extends RegularComponentType<?, ?>> componentTypes, ImmutableBag<Object> components) {
        // Lookup pointer
        var pointer = lookup.getSafe(entityId);
        if (pointer == null) {
            pointer = new ArchetypePointer();
            lookup.set(entityId, pointer);
        }

        // Determine archetype
        pointer.archetype = determineArchetype(componentMask);

        // Add entity to archetype
        pointer.index = pointer.archetype.addEntity(entityId, componentTypes, components);
    }

    private ArchetypeData determineArchetype(ComponentMaskImpl componentMask) {
        var existing = componentMask.getId() < this.archetypes.getSize() ? this.archetypes.get(componentMask.getId()) : null;
        if (existing != null) {
            return existing;
        }

        synchronized (this.archetypes) {
            existing = componentMask.getId() < this.archetypes.getSize() ? this.archetypes.get(componentMask.getId()) : null;
            if (existing != null) {
                return existing;
            }

            var archetype = createArchetype(componentMask);
            this.archetypes.set(componentMask.getId(), archetype);

            this.world.dispatchEvent(new ArchetypeAddedEvent(componentMask, archetype));

            return archetype;
        }
    }

    private ArchetypeData createArchetype(ComponentMaskImpl componentMask) {
        return switch (config.variant()) {
            case ArrayOfStructs -> new ArchetypeDataImpl(componentIndex, relationIndex, this, componentMask, world);
            case StructOfArrays -> new ArchetypeDataSoaImpl(componentIndex, relationIndex, this, componentMask, world);
        };
    }

    public ComponentMask deleteEntity(int entityId) {
        // Lookup pointer
        var pointer = lookup.get(entityId);
        if (pointer == null || pointer.archetype == null) {
            throw new StorageEngineException("Cannot delete entity %d: Entity not present in storage".formatted(entityId));
        }

        // Remove pending changes
        var changes = getPendingChanges(entityId);
        if (changes != null) {
            changes.reset();
        }

        // Delete entity from archetype
        var componentMask = pointer.archetype.getComponentMask();
        removeEntity(entityId, pointer, null);

        return componentMask;
    }

    public void removeEntity(int entityId, ArchetypePointer pointer, Bag<Object> fill) {
        // Remove entity
        var swappedEntityId = pointer.archetype.removeEntity(entityId, pointer.index, fill);
        if (swappedEntityId > -1) {
            // Update pointer of swapped entity
            var swappedPointer = lookup.get(swappedEntityId);
            swappedPointer.index = pointer.index;
        }

        // Clear pointer data
        pointer.archetype = null;
        pointer.index = -1;
    }

    public PendingChanges getPendingChanges(int entityId) {
        // Lookup pointer
        var pointer = lookup.get(entityId);
        if (pointer == null || pointer.archetype == null) {
            throw new StorageEngineException("Cannot get pending changes for entity %d: Entity not present in storage".formatted(entityId));
        }

        return pointer.archetype.getPendingChanges(pointer.index);
    }

    public ComponentMask flushChanges(int entityId, ComponentMaskImpl componentMask) {
        // Lookup pointer
        var pointer = lookup.get(entityId);
        var previousComponentMask = pointer.archetype.getComponentMask();

        // Retrieve changes, return early if none
        var changes = pointer.archetype.getPendingChanges(pointer.index);

        // Remove entity from current archetype
        var data = componentsPool.getInstance();
        removeEntity(entityId, pointer, data);

        // Determine archetype for new component mask
        pointer.archetype = determineArchetype(componentMask);

        // Add to new archetype 
        pointer.index = pointer.archetype.addEntity(entityId, previousComponentMask.getComponentTypes(), data, changes.getAddedTypes(), changes.getAdded());

        // Reset changes, free data
        changes.reset();
        componentsPool.free(data);

        return componentMask;
    }

    public int getEntityIndex(ArchetypeData archetypeData, int entityId) {
        var pointer = lookup.get(entityId);
        if (pointer == null || pointer.archetype != archetypeData) {
            return -1;
        }

        return pointer.index;
    }

    public void freeComponent(Object component) {
        switch (component) {
            case ComponentRelationResultImpl result -> result.free();
            case EntityRelationResultImpl result -> result.free();
            case Relation<?> relation -> Relation.free(relation);
            case Pooled pooled -> componentIndex.freeInstance(pooled);
            default -> {
            }
        }
    }

    private static class ArchetypePointer {

        private ArchetypeData archetype;
        private int index;

        @Override
        public String toString() {
            return new StringBuilder()
                    .append("ArchetypePointer(index = ").append(this.index).append(", archetype=").append(this.archetype).append(")")
                    .toString();
        }

    }

}
