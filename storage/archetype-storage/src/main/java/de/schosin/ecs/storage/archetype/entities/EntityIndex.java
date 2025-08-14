package de.schosin.ecs.storage.archetype.entities;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.TreeSet;

import de.schosin.ecs.api.Pooled;
import de.schosin.ecs.api.components.Relation;
import de.schosin.ecs.api.components.types.ComponentType.RegularComponentType;
import de.schosin.ecs.api.data.DataAccessor;
import de.schosin.ecs.storage.api.StorageEngineException;
import de.schosin.ecs.storage.api.StorageWorld;
import de.schosin.ecs.storage.api.components.Component;
import de.schosin.ecs.storage.api.components.Component.RelationComponent;
import de.schosin.ecs.storage.api.entities.Archetype;
import de.schosin.ecs.storage.api.entities.ArchetypeAccessor;
import de.schosin.ecs.storage.api.events.ArchetypeAddedEvent;
import de.schosin.ecs.storage.archetype.ArchetypeStorageConfig;
import de.schosin.ecs.storage.archetype.ArchetypeStorageEngine;
import de.schosin.ecs.storage.archetype.components.ComponentIndex;
import de.schosin.ecs.storage.archetype.entities.archetypes.ArchetypeData;
import de.schosin.ecs.storage.archetype.entities.archetypes.ArchetypeDataSoaImpl;
import de.schosin.ecs.storage.archetype.entities.archetypes.ArchetypeGraphNode;
import de.schosin.ecs.storage.archetype.entities.archetypes.PendingChanges;
import de.schosin.ecs.storage.archetype.utils.results.ComponentRelationResultImpl;
import de.schosin.ecs.storage.archetype.utils.results.EntityRelationResultImpl;
import de.schosin.ecs.utils.collections.Bag;
import de.schosin.ecs.utils.collections.BitVector;
import de.schosin.ecs.utils.collections.ImmutableBag;
import de.schosin.ecs.utils.collections.ImmutableIntBag;
import de.schosin.ecs.utils.collections.Pool;

/**
 * Fast entity to archetype lookup.
 * 
 * Inspired by implementation in Flecs: https://ajmmertens.medium.com/building-an-ecs-storage-in-pictures-642b8bfd6e04 
 */
public class EntityIndex {

    private static final Comparator<Component<?, ?>> COMPONENT_COMPARATOR = Comparator.comparing(Component::id);

    private final StorageWorld world;
    private final ArchetypeStorageConfig config;

    private final ArchetypeStorageEngine storage;
    private final Observers observers;

    private final ComponentIndex componentIndex;
    private final EntityRelationIndex relationIndex;

    private final Bag<ArchetypePointer> lookup;

    private final Bag<ArchetypeData> archetypes = new Bag<>(ArchetypeData.class, 8);
    private final ImmutableBag<Archetype> immutableArchetypes = ImmutableBag.create(archetypes);

    private Bag<ArchetypeData> dirtyArchetypes = new Bag<>(ArchetypeData.class, 8);
    private Bag<ArchetypeData> dirtyArchetypesOverflow = new Bag<>(ArchetypeData.class, 8);

    private final Map<BitVector, ArchetypeData> archetypesLookup = new HashMap<>();
    private final Map<BitVector, ArchetypeGraphNode> graphLookup = new HashMap<>();

    private final ArchetypeGraphNode emptyArchetypeNode;
    private final ArchetypeData emptyArchetype;

    private final Pool<BitVector> bitVectorPool = Pool.unbounded(BitVector.class, BitVector::new, BitVector::clear);

    public EntityIndex(StorageWorld world, ArchetypeStorageConfig config, ArchetypeStorageEngine storage, Observers observers, ComponentIndex componentIndex, EntityRelationIndex relationIndex) {
        this.world = world;
        this.config = config;

        this.storage = storage;
        this.observers = observers;
        this.componentIndex = componentIndex;
        this.relationIndex = relationIndex;

        this.lookup = world.createEntityBag(ArchetypePointer.class);

        this.emptyArchetypeNode = new ArchetypeGraphNode(this.graphLookup.size(), componentIndex, this, new BitVector(), ImmutableBag.emptyBag());
        this.graphLookup.put(emptyArchetypeNode.getComponentIds(), emptyArchetypeNode);

        this.emptyArchetype = this.emptyArchetypeNode.getArchetype();
    }

    public void process() {
        if (this.dirtyArchetypes.isEmpty()) {
            return;
        }

        var attempts = config.processAttempts();

        while (attempts-- > 0) {
            // Double buffering
            var dirtyArchetypes = this.dirtyArchetypes;
            this.dirtyArchetypes = this.dirtyArchetypesOverflow;
            this.dirtyArchetypesOverflow = dirtyArchetypes;

            // Process dirty archetypes
            var data = dirtyArchetypes.getData();
            for (int i = 0, s = dirtyArchetypes.getSize(); i < s; i++) {
                data[i].process();
            }

            // Clear bag
            dirtyArchetypes.clear();

            // Return if no more changes
            if (this.dirtyArchetypes.isEmpty()) {
                return;
            }
        }

        // Throw error if changes remain after configured number of attempts
        throw new StorageEngineException("""
                Processing changes did not finish after %d attemtps. \
                Set propery '%s' to increase this value.

                If this error persists, make sure there are no endless loops caused by inserted/removed callbacks.
                """.formatted(config.processAttempts(), ArchetypeStorageConfig.PROPERTY_PROCESS_ATTEMPTS));
    }

    public void markDirty(ArchetypeData archetype) {
        this.dirtyArchetypes.add(archetype);
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

    public DataAccessor getAccessor(int entityId) {
        var pointer = lookup.getSafe(entityId);
        if (pointer == null || !pointer.isValid()) {
            throw new StorageEngineException("Cannot get accessor for entity %d: Entity not present in storage".formatted(entityId));
        }

        return pointer;
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

    public ArchetypeData getArchetype(ArchetypeGraphNode node) {
        var componentIds = node.getComponentIds();

        var result = this.archetypesLookup.get(componentIds);
        if (result != null) {
            return result;
        }

        synchronized (this.archetypesLookup) {
            result = this.archetypesLookup.get(componentIds);
            if (result != null) {
                return result;
            }

            result = createArchetype(this.archetypesLookup.size(), node);
            this.archetypes.set(result.getId(), result);
            this.archetypesLookup.put(componentIds, result);

            world.dispatchEvent(new ArchetypeAddedEvent(result));
        }

        return result;
    }

    public ArchetypeData getArchetype(RegularComponentType<?, ?>... componentTypes) {
        return switch (componentTypes.length) {
            case 0 -> emptyArchetype;
            case 1 -> {
                var componentType = componentTypes[0];
                yield emptyArchetypeNode.addComponentType(componentType, componentIndex.getId(componentType)).getArchetype();
            }
            default -> {
                var result = emptyArchetypeNode;
                for (int i = 0, s = componentTypes.length; i < s; i++) {
                    var componentType = componentTypes[i];

                    var next = result.addComponentType(componentType, componentIndex.getId(componentType));
                    if (next == result) {
                        throw new StorageEngineException("Cannot create archetype, detected duplicate component type: %s".formatted(componentType));
                    }

                    result = next;
                }

                yield result.getArchetype();
            }
        };
    }

    public ArchetypeGraphNode addToArchetype(ArchetypeGraphNode base, int componentId, RegularComponentType<?, ?> componentType) {
        var key = bitVectorPool.getInstance();
        key.setAll(base.getComponentIds());
        key.set(componentId);

        var result = this.graphLookup.get(key);
        if (result != null) {
            bitVectorPool.free(key);
            return result;
        }

        synchronized (this.graphLookup) {
            result = this.graphLookup.get(key);
            if (result != null) {
                bitVectorPool.free(key);
                return result;
            }

            var componentIds = new BitVector(key);

            var components = new Bag<>(base.getComponents());
            components.add(storage.getComponent(componentType));

            components = sortComponents(components);

            if (components.getSize() != base.getComponents().getSize() + 1) {
                // this indicates a bug and should not be reachable
                throw new IllegalArgumentException("Cannot add '%s' to archetype '%s': Component type already contained".formatted(componentType, base));
            }

            result = new ArchetypeGraphNode(this.graphLookup.size(), componentIndex, this, componentIds, components);
            this.graphLookup.put(componentIds, result);

            bitVectorPool.free(key);
            return result;
        }
    }

    private Bag<Component<?, ?>> sortComponents(Bag<Component<?, ?>> components) {
        var duplicates = new HashSet<RegularComponentType<?, ?>>();

        var set = new TreeSet<>(COMPONENT_COMPARATOR);
        for (int i = 0, s = components.getSize(); i < s; i++) {
            var component = components.get(i);

            if (!set.add(component) && !(component instanceof RelationComponent<?, ?, ?>)) {
                duplicates.add(component.type());
            }
        }

        // Check for duplicates
        if (!duplicates.isEmpty()) {
            throw new StorageEngineException("Detected duplicate component types: %s".formatted(duplicates));
        }

        // Build sorted bag
        var result = new Bag<Component<?, ?>>(Component.class, set.size());
        for (var component : set) {
            result.add(component);
        }

        return result;
    }

    public ArchetypeGraphNode removeFromArchetype(ArchetypeGraphNode base, int componentId, RegularComponentType<?, ?> componentType) {
        var key = bitVectorPool.getInstance();
        key.setAll(base.getComponentIds());
        key.clear(componentId);

        var result = this.graphLookup.get(key);
        if (result != null) {
            bitVectorPool.free(key);
            return result;
        }

        synchronized (this.graphLookup) {
            result = this.graphLookup.get(key);
            if (result != null) {
                bitVectorPool.free(key);
                return result;
            }

            var components = new Bag<>(base.getComponents());
            if (!components.remove(storage.getComponent(componentId))) {
                // this indicates a bug and should not be reachable
                throw new IllegalArgumentException("Cannot remove '%s' from archetype '%s': Component type not contained".formatted(componentType, base));
            }

            var componentIds = new BitVector(key);

            result = new ArchetypeGraphNode(this.graphLookup.size(), componentIndex, this, componentIds, components);
            this.graphLookup.put(componentIds, result);

            bitVectorPool.free(key);
            return result;
        }
    }

    private ArchetypeData createArchetype(int id, ArchetypeGraphNode node) {
        return new ArchetypeDataSoaImpl(id, node, componentIndex, relationIndex, this, observers, config, world);
    }

    public void markDeleted(int entityId) {
        // Lookup pointer
        var pointer = lookup.get(entityId);
        if (pointer == null || !pointer.isValid()) {
            throw new StorageEngineException("Cannot delete entity %d: Entity not present in storage".formatted(entityId));
        }

        // Mark entity as deleted
        pointer.markDeleted();
    }

    public PendingChanges getPendingChanges(int entityId) {
        // Lookup pointer
        var pointer = lookup.get(entityId);
        if (pointer == null || !pointer.isValid()) {
            throw new StorageEngineException("Cannot get pending changes for entity %d: Entity not present in storage".formatted(entityId));
        }

        return pointer.getPendingChanges();
    }

    private ArchetypeData flushChanges(int entityId, ArchetypeData archetype, ArchetypePointer pointer) {
        var newArchetypeNode = pointer.getPendingChanges().getPendingArchetypeNode();
        var newArchetype = newArchetypeNode.getArchetype();

        // Call observers
        observers.triggerEntityBeforeUpdate(archetype, newArchetype, entityId);

        // Move entity to new archetype
        archetype.moveEntity(entityId, newArchetypeNode, pointer.getIndex());

        // Call observers
        observers.triggerEntityUpdated(newArchetype, archetype, entityId);

        return newArchetype;
    }

    public int getEntityIndex(ArchetypeData archetypeData, int entityId) {
        var pointer = lookup.get(entityId);
        if (pointer == null || pointer.getArchetype() != archetypeData) {
            return -1;
        }

        return pointer.getIndex();
    }

    public void setEntityIndex(int entityId, int index) {
        var pointer = lookup.get(entityId);
        pointer.setIndex(index);
    }

    public void setEntityIndex(int entityId, ArchetypeData archetype, int index) {
        var pointer = lookup.get(entityId);
        pointer.updatePointer(archetype, index);
    }

    /**
     * Removes the reference to the entity and returns the index before the removal.
     * 
     * @param entityId id of entity
     * @return index before removal
     */
    public int removeEntityIndex(int entityId) {
        var pointer = lookup.get(entityId);
        var index = pointer.getIndex();

        pointer.invalidate();

        return index;
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

    public void processCreatedEntities(ImmutableIntBag entities) {
        for (int i = 0, s = entities.getSize(); i < s; i++) {
            processCreatedEntity(entities.get(i));
        }
    }

    public void processCreatedEntity(int entityId) {
        var pointer = lookup.get(entityId);

        var attempts = config.creationFlushAttempts();

        while (attempts-- > 0) {
            // Retrieve changes, skip if none
            var changes = pointer.getPendingChanges();
            if (changes == null || changes.isEmpty()) {
                return;
            }

            // Flush changes
            flushChanges(entityId, pointer.getArchetype(), pointer);
        }

        // Throw error if changes remain after configured number of attempts
        throw new StorageEngineException("""
                Processing changes for created entity did not finish after %d attemtps. \
                Set propery '%s' to increase this value.

                If this error persists, make sure there are no endless loops caused by inserted/removed callbacks.
                """.formatted(config.creationFlushAttempts(), ArchetypeStorageConfig.PROPERTY_CREATION_FLUSH_ATTEMPTS));
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

    public void markDeleted() {
        archetype.markDeleted(entityId, index);
    }

    void setPointer(ArchetypeData archetype, int index) {
        this.archetype = archetype;
        this.index = index;

        this.accessor = archetype.getAccessor(index);
    }

    void updatePointer(ArchetypeData archetype, int index) {
        this.archetype = archetype;
        this.index = index;

        this.accessor.free();
        this.accessor = archetype.getAccessor(index);
    }

    void setIndex(int index) {
        this.index = index;
        this.archetype.updateAccessor(index, this.accessor);
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
