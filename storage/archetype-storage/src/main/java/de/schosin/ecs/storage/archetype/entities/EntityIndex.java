package de.schosin.ecs.storage.archetype.entities;

import de.schosin.ecs.api.Pooled;
import de.schosin.ecs.api.components.Relation;
import de.schosin.ecs.api.components.types.ComponentType.RegularComponentType;
import de.schosin.ecs.api.data.DataAccessor;
import de.schosin.ecs.storage.api.StorageEngineException;
import de.schosin.ecs.storage.api.StorageWorld;
import de.schosin.ecs.storage.api.entities.Archetype;
import de.schosin.ecs.storage.api.entities.ArchetypeAccessor;
import de.schosin.ecs.storage.api.entities.ComponentMask;
import de.schosin.ecs.storage.api.events.ArchetypeAddedEvent;
import de.schosin.ecs.storage.archetype.ArchetypeStorageConfig;
import de.schosin.ecs.storage.archetype.components.ComponentIndex;
import de.schosin.ecs.storage.archetype.entities.archetypes.ArchetypeData;
import de.schosin.ecs.storage.archetype.entities.archetypes.ArchetypeDataSoaImpl;
import de.schosin.ecs.storage.archetype.entities.archetypes.PendingChanges;
import de.schosin.ecs.storage.archetype.utils.results.ComponentRelationResultImpl;
import de.schosin.ecs.storage.archetype.utils.results.EntityRelationResultImpl;
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
    private final Bag<ArchetypeData> archetypes = new Bag<>(ArchetypeData.class, 8);
    private final ImmutableBag<Archetype> immutableArchetypes = ImmutableBag.create(archetypes);

    private final Pool<Bag<Object>> componentsPool = Pool.unbounded(Bag.class, () -> new Bag<>(Object.class), Bag::clear);

    public EntityIndex(StorageWorld world, ArchetypeStorageConfig config, ComponentIndex componentIndex, EntityRelationIndex relationIndex) {
        this.world = world;
        this.config = config;

        this.componentIndex = componentIndex;
        this.relationIndex = relationIndex;

        this.lookup = world.createEntityBag(ArchetypePointer.class);
    }

    public ArchetypeData getArchetypeDataById(int archetypeId) {
        return archetypes.get(archetypeId);
    }

    public ImmutableBag<Archetype> getArchetypes() {
        return immutableArchetypes;
    }

    public ArchetypeData getArchetypeDataForEntity(int entityId) {
        var pointer = lookup.getSafe(entityId);
        if (pointer == null) {
            return null;
        }

        return pointer.getArchetype();
    }

    public ArchetypeData getArchetype(ComponentMaskImpl componentMask) {
        return determineArchetype(componentMask);
    }

    public DataAccessor getAccessor(int entityId) {
        var pointer = lookup.getSafe(entityId);
        if (pointer == null || !pointer.isValid()) {
            throw new StorageEngineException("Cannot get accessor for entity %d: Entity not present in storage".formatted(entityId));
        }

        return pointer;
    }

    public ComponentMask getComponentMask(int entityId) {
        var archetype = getArchetypeDataForEntity(entityId);
        if (archetype == null) {
            return null;
        }

        return archetype.getComponentMask();
    }

    public boolean hasComponent(int entityId, int componentId) {
        var pointer = lookup.getSafe(entityId);
        if (pointer == null || !pointer.isValid()) {
            return false;
        }

        return pointer.hasComponent(componentId);
    }

    public <R> R getComponent(int entityId, RegularComponentType<?, R> componentType, int componentId) {
        // Lookup pointer
        var pointer = lookup.getSafe(entityId);
        if (pointer == null || !pointer.isValid()) {
            return null; // TODO this should throw, shouldn't it?
        }

        return pointer.getComponent(componentType, componentId);
    }

    public void addComponents(int entityId, ImmutableBag<? extends RegularComponentType<?, ?>> componentTypes, Object[] components) {
        // Lookup pointer
        var pointer = lookup.get(entityId);
        if (pointer == null || !pointer.isValid()) {
            return; // TODO this should throw, shouldn't it?
        }

        pointer.addComponents(componentTypes, components);
    }

    public void removeComponents(int entityId, ImmutableBag<? extends RegularComponentType<?, ?>> removeTypes) {
        // Lookup pointer
        var pointer = lookup.get(entityId);
        if (pointer == null || !pointer.isValid()) {
            return; // TODO this should throw, shouldn't it?
        }

        pointer.removeComponents(removeTypes);
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
        if (pointer == null || !pointer.isValid()) {
            return; // TODO this should throw, shouldn't it?
        }

        pointer.addComponents(addTypes, add);
        pointer.removeComponents(removeTypes);
    }

    public void add(ArchetypeData archetype, int entityId, int index) {
        var pointer = lookup.getSafe(entityId);
        if (pointer == null) {
            pointer = new ArchetypePointer(entityId);
            lookup.set(entityId, pointer);
        }

        pointer.setPointer(archetype, index);
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
            case StructOfArrays -> new ArchetypeDataSoaImpl(componentIndex, relationIndex, this, componentMask, config, world);
        };
    }

    public ComponentMask deleteEntity(int entityId) {
        // Lookup pointer
        var pointer = lookup.get(entityId);
        if (pointer == null || !pointer.isValid()) {
            throw new StorageEngineException("Cannot delete entity %d: Entity not present in storage".formatted(entityId));
        }

        // Remove pending changes
        var changes = getPendingChanges(entityId);
        if (changes != null) {
            changes.reset();
        }

        // Delete entity from archetype
        var componentMask = pointer.getArchetype().getComponentMask();
        removeEntity(pointer, null);

        return componentMask;
    }

    public void removeEntity(ArchetypePointer pointer, Bag<Object> fill) {
        // Remove entity
        var swappedEntityId = pointer.removeEntity(fill);
        if (swappedEntityId > -1) {
            // Update pointer of swapped entity
            var swappedPointer = lookup.get(swappedEntityId);
            swappedPointer.setIndex(pointer.getIndex());
        }

        // Clear pointer data
        pointer.invalidate();
    }

    public PendingChanges getPendingChanges(int entityId) {
        // Lookup pointer
        var pointer = lookup.get(entityId);
        if (pointer == null || !pointer.isValid()) {
            throw new StorageEngineException("Cannot get pending changes for entity %d: Entity not present in storage".formatted(entityId));
        }

        return pointer.getPendingChanges();
    }

    public ComponentMask flushChanges(int entityId, ComponentMaskImpl componentMask) {
        // Lookup pointer
        var pointer = lookup.get(entityId);
        var previousComponentMask = pointer.getArchetype().getComponentMask();

        // Retrieve changes, return early if none
        var changes = pointer.getPendingChanges();

        // Remove entity from current archetype
        var data = componentsPool.getInstance();
        removeEntity(pointer, data);

        // Add to archetype
        var archetype = determineArchetype(componentMask);
        var index = archetype.addEntity(entityId, previousComponentMask.getComponentTypes(), data, changes.getAddedTypes(), changes.getAdded());

        pointer.setPointer(archetype, index);

        // Reset changes, free data
        changes.reset();
        componentsPool.free(data);

        return componentMask;
    }

    public int getEntityIndex(ArchetypeData archetypeData, int entityId) {
        var pointer = lookup.get(entityId);
        if (pointer == null || pointer.getArchetype() != archetypeData) {
            return -1;
        }

        return pointer.getIndex();
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

}

final class ArchetypePointer implements ArchetypeAccessor {

    private final int entityId;

    private ArchetypeData archetype;
    private int index;

    private ArchetypeAccessor accessor;

    ArchetypePointer(int entityId) {
        this.entityId = entityId;
    }

    void addComponents(ImmutableBag<? extends RegularComponentType<?, ?>> componentTypes, Object[] components) {
        archetype.addComponents(entityId, index, componentTypes, components);
    }

    void removeComponents(ImmutableBag<? extends RegularComponentType<?, ?>> componentTypes) {
        archetype.removeComponents(entityId, index, componentTypes);
    }

    PendingChanges getPendingChanges() {
        return archetype.getPendingChanges(index);
    }

    int removeEntity(Bag<Object> fill) {
        return archetype.removeEntity(entityId, index, fill);
    }

    void setPointer(ArchetypeData archetype, int index) {
        this.archetype = archetype;
        this.index = index;

        this.accessor = archetype.getAccessor(entityId);
    }

    void setIndex(int index) {
        this.index = index;

        this.accessor.free();
        this.accessor = archetype.getAccessor(entityId);
    }

    void invalidate() {
        this.archetype = null;
        this.index = -1;

        this.accessor.free();
        this.accessor = null;
    }

    int getIndex() {
        return this.index;
    }

    @Override
    public ArchetypeData getArchetype() {
        return archetype;
    }

    @Override
    public int entityId() {
        return entityId;
    }

    @Override
    public boolean hasComponent(int componentId) {
        return accessor.hasComponent(componentId);
    }

    @Override
    public <R> R getComponent(int componentId) {
        return accessor.getComponent(componentId);
    }

    @Override
    public <R> R getComponent(RegularComponentType<?, R> componentType, int componentId) {
        return accessor.getComponent(componentType, componentId);
    }

    @Override
    public <R> R getComponentByIndex(int componentIndex) {
        return accessor.getComponentByIndex(componentIndex);
    }

    @Override
    public <R> R getPendingComponent(int componentId) {
        return accessor.getPendingComponent(componentId);
    }

    @Override
    public boolean isValid() {
        return this.archetype != null;
    }

    @Override
    public void free() {
        // nothing to do
    }

    @Override
    public String toString() {
        return new StringBuilder()
                .append("ArchetypePointer(index = ").append(this.index).append(", archetype=").append(this.archetype).append(")")
                .toString();
    }

}
