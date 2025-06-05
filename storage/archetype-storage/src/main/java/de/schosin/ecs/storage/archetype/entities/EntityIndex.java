package de.schosin.ecs.storage.archetype.entities;

import de.schosin.ecs.api.Pooled;
import de.schosin.ecs.api.components.Relation;
import de.schosin.ecs.api.components.types.ComponentType.RegularComponentType;
import de.schosin.ecs.storage.api.StorageEngineException;
import de.schosin.ecs.storage.api.StorageWorld;
import de.schosin.ecs.storage.api.entities.ComponentMask;
import de.schosin.ecs.storage.archetype.components.ComponentIndex;
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
    private final ComponentIndex componentIndex;
    private final EntityRelationIndex relationIndex;

    private final Bag<ArchetypePointer> lookup;
    private final Bag<ArchetypeData> archetypes;

    private final Pool<Bag<Object>> componentsPool = Pool.unbounded(Bag.class, () -> new Bag<>(Object.class), Bag::clear);
    private final Pool<Bag<RegularComponentType<?, ?>>> componentTypesPool = Pool.unbounded(Bag.class, () -> new Bag<RegularComponentType<?, ?>>(RegularComponentType.class), Bag::clear);

    public EntityIndex(StorageWorld world, ComponentIndex componentIndex, EntityRelationIndex relationIndex) {
        this.world = world;
        this.componentIndex = componentIndex;
        this.relationIndex = relationIndex;

        this.lookup = world.createEntityBag(ArchetypePointer.class);
        this.archetypes = new Bag<>(ArchetypeData.class, 8);
    }

    public ArchetypeData getArchetypeData(int entityId) {
        var pointer = lookup.getSafe(entityId);
        if (pointer == null) {
            return null;
        }

        return pointer.archetype;
    }

    public ComponentMask getComponentMask(int entityId) {
        var archetype = getArchetypeData(entityId);
        if (archetype == null) {
            return null;
        }

        return archetype.getComponentMask();
    }

    public boolean hasComponent(int entityId, RegularComponentType<?, ?> componentType) {
        var archetype = getArchetypeData(entityId);
        // TODO null should throw, shouldn't it?

        return archetype != null && archetype.contains(componentType);
    }

    public <R> R getComponent(int entityId, RegularComponentType<?, R> componentType) {
        // Lookup pointer
        var pointer = lookup.getSafe(entityId);
        if (pointer == null || pointer.archetype == null) {
            return null; // TODO this should throw, shouldn't it?
        }

        return pointer.archetype.getComponent(pointer.index, componentType);
    }

    public void addComponents(int entityId, ComponentMaskImpl componentMask, ImmutableBag<? extends RegularComponentType<?, ?>> componentTypes, Object[] components) {
        // Lookup pointer
        var pointer = lookup.get(entityId);
        if (pointer == null || pointer.archetype == null) {
            return; // TODO this should throw, shouldn't it?
        }

        var previousComponentMask = pointer.archetype.getComponentMask();

        // If same as current, override existing data / add non-exlusive relations
        if (pointer.archetype.getComponentMask() == componentMask) {
            pointer.archetype.updateComponents(entityId, pointer.index, componentTypes, components);
            return;
        }

        // Remove entity from current archetype
        var data = componentsPool.getInstance();
        removeEntity(entityId, pointer, data);

        // Determine archetype for new component mask
        pointer.archetype = determineArchetype(componentMask);

        // Add to new archetype 
        pointer.index = pointer.archetype.addEntity(entityId, previousComponentMask.getComponentTypes(), data, componentTypes, components);

        componentsPool.free(data);
    }

    public void removeComponents(int entityId, ComponentMaskImpl componentMask, ImmutableBag<? extends RegularComponentType<?, ?>> removeTypes) {
        // Lookup pointer
        var pointer = lookup.get(entityId);
        if (pointer == null || pointer.archetype == null) {
            return; // TODO this should throw, shouldn't it?
        }

        var previousComponentMask = pointer.archetype.getComponentMask();

        // Remove entity from current archetype
        var data = componentsPool.getInstance();
        removeEntity(entityId, pointer, data);

        // Determine archetype for new component mask
        pointer.archetype = determineArchetype(componentMask);

        // Calculate component types
        var componentTypes = componentTypesPool.getInstance();
        componentTypes.addAll(previousComponentMask.getComponentTypes());

        for (int i = 0, s = removeTypes.getSize(); i < s; i++) {
            var componentType = removeTypes.get(i);

            var index = componentTypes.indexOf(componentType);
            if (index > -1) {
                componentTypes.remove(index);

                var component = data.remove(index);
                freeComponent(component);
            }
        }

        // Add to new archetype 
        pointer.index = pointer.archetype.addEntity(entityId, componentTypes, data);

        // Free pooled items
        componentTypesPool.free(componentTypes);
        componentsPool.free(data);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void modifyComponents(int entityId, ComponentMaskImpl componentMask, ImmutableBag<? extends RegularComponentType<?, ?>> addTypes, Object[] add,
            ImmutableBag<? extends RegularComponentType<?, ?>> removeTypes) {

        // Remove if no added components
        if (add.length == 0) {
            removeComponents(entityId, componentMask, removeTypes);
            return;
        }

        // Add if no removed components
        if (removeTypes.isEmpty()) {
            addComponents(entityId, componentMask, addTypes, add);
            return;
        }

        // Lookup pointer
        var pointer = lookup.get(entityId);
        if (pointer == null || pointer.archetype == null) {
            return; // TODO this should throw, shouldn't it?
        }

        var previousComponentMask = pointer.archetype.getComponentMask();

        // Remove entity from current archetype
        var data = componentsPool.getInstance();
        removeEntity(entityId, pointer, data);

        // Determine archetype for new component mask
        pointer.archetype = determineArchetype(componentMask);

        // Remove components
        var componentTypes = componentTypesPool.getInstance();
        componentTypes.addAll(previousComponentMask.getComponentTypes());

        for (int i = 0, s = removeTypes.getSize(); i < s; i++) {
            var removeType = removeTypes.get(i);

            if (((ImmutableBag) addTypes).contains(removeType)) {
                throw new StorageEngineException("Cannot add and remove the same component type for entity %d: %s".formatted(entityId, removeType));
            }

            var index = componentTypes.indexOf(removeType);
            if (index > -1) {
                componentTypes.remove(index);

                var component = data.remove(index);
                freeComponent(component);
            }
        }

        // Add components
        componentTypes.addAll(addTypes);
        for (int i = 0, s = add.length; i < s; i++) {
            data.add(add[i]);
        }

        // Add to new archetype 
        pointer.index = pointer.archetype.addEntity(entityId, componentTypes, data);

        // Free pooled items
        componentTypesPool.free(componentTypes);
        componentsPool.free(data);
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

            return archetype;
        }
    }

    private ArchetypeData createArchetype(ComponentMaskImpl componentMask) {
        return new ArchetypeData(componentIndex, relationIndex, this, componentMask, world);
    }

    public ComponentMask deleteEntity(int entityId) {
        // Lookup pointer
        var pointer = lookup.get(entityId);
        if (pointer == null || pointer.archetype == null) {
            throw new StorageEngineException("Cannot delete entity %d: Entity not present in storage".formatted(entityId));
        }

        // Delete entity from archetype
        var componentMask = pointer.archetype.getComponentMask();
        removeEntity(entityId, pointer, null);

        return componentMask;
    }

    public void removeEntity(int entityId, ArchetypePointer pointer, Bag<Object> fill) {
        // Remove entity
        var swappedEntityId = pointer.archetype.deleteEntity(entityId, pointer.index, fill);
        if (swappedEntityId > -1) {
            // Update pointer of swapped entity
            var swappedPointer = lookup.get(swappedEntityId);
            swappedPointer.index = pointer.index;
        }

        // Clear pointer data
        pointer.archetype = null;
        pointer.index = -1;
    }

    void freeComponent(Object component) {
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
