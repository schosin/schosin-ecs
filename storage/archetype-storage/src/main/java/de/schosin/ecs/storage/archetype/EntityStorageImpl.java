package de.schosin.ecs.storage.archetype;

import org.jspecify.annotations.Nullable;

import de.schosin.ecs.api.components.types.ComponentType;
import de.schosin.ecs.api.components.types.ComponentType.RegularComponentType;
import de.schosin.ecs.api.data.DataAccessor;
import de.schosin.ecs.storage.api.ArchetypeStorage;
import de.schosin.ecs.storage.api.ComponentStorage;
import de.schosin.ecs.storage.api.EntityStorage;
import de.schosin.ecs.storage.api.StorageEngineException;
import de.schosin.ecs.storage.api.entities.Archetype;
import de.schosin.ecs.storage.archetype.entities.EntityIndex;
import de.schosin.ecs.storage.archetype.entities.archetypes.ArchetypeData;
import de.schosin.ecs.utils.collections.Bag;
import de.schosin.ecs.utils.collections.ImmutableBag;
import de.schosin.ecs.utils.collections.Pool;

public class EntityStorageImpl implements EntityStorage, ArchetypeStorage {

    private final EntityIndex entityIndex;

    private final ComponentStorage componentStorage;

    private final Pool<Bag<RegularComponentType<?, ?>>> componentTypesPool = Pool.unbounded(Bag.class, () -> new Bag<>(RegularComponentType.class, 8), Bag::clear);

    public EntityStorageImpl(EntityIndex entityIndex, ComponentStorage componentStorage) {
        this.entityIndex = entityIndex;

        this.componentStorage = componentStorage;
    }

    @Override
    public DataAccessor getAccessor(int entityId) {
        return entityIndex.getAccessor(entityId);
    }

    @Override
    public Archetype add(int entityId, Object[] components) {
        var componentTypes = componentTypesPool.getInstance();
        detectComponentTypes(componentTypes, components);

        var result = addComponents(entityId, componentTypes, components);

        componentTypesPool.free(componentTypes);
        return result;
    }

    @Override
    public Archetype add(int entityId, ImmutableBag<? extends RegularComponentType<?, ?>> componentTypes, Object[] components) {
        return addComponents(entityId, componentTypes, components);
    }

    private Archetype addComponents(int entityId, ImmutableBag<? extends RegularComponentType<?, ?>> componentTypes, Object[] components) {
        var archetype = entityIndex.getArchetypeDataForEntity(entityId);
        if (archetype == null) {
            throw new StorageEngineException("Cannot add components to entity %d: Entity not present in storage".formatted(entityId));
        }

        // Add components
        entityIndex.addComponents(entityId, componentTypes, components);

        // Return pending archetype if present
        var pendingArchetype = getPendingArchetype(entityId);
        return pendingArchetype != null ? pendingArchetype : archetype;
    }

    @Override
    public Archetype remove(int entityId, ImmutableBag<? extends ComponentType<?, ?>> componentTypes) {
        var archetype = entityIndex.getArchetypeDataForEntity(entityId);
        if (archetype == null) {
            throw new StorageEngineException("Cannot remove components from entity %d: Entity not present in storage".formatted(entityId));
        }

        // Determine regular component types from removedTypes
        var regularComponentTypes = componentTypesPool.getInstance();
        fillRegularComponentTypes(componentTypes, regularComponentTypes);

        // Remove components
        entityIndex.removeComponents(entityId, regularComponentTypes);

        componentTypesPool.free(regularComponentTypes);

        // Return pending archetype if present
        var pendingArchetype = getPendingArchetype(entityId);
        return pendingArchetype != null ? pendingArchetype : archetype;
    }

    @Override
    public Archetype delete(int entityId) {
        return entityIndex.deleteEntity(entityId);
    }

    @Override
    public Archetype modify(int entityId, Object[] add, ImmutableBag<? extends ComponentType<?, ?>> removeTypes) {
        var addTypes = componentTypesPool.getInstance();
        detectComponentTypes(addTypes, add);

        var result = modifyComponents(entityId, addTypes, add, removeTypes);

        componentTypesPool.free(addTypes);
        return result;
    }

    @Override
    public Archetype modify(int entityId, ImmutableBag<? extends RegularComponentType<?, ?>> addTypes, Object[] add, ImmutableBag<? extends ComponentType<?, ?>> removeTypes) {
        return modifyComponents(entityId, addTypes, add, removeTypes);
    }

    private Archetype modifyComponents(int entityId, ImmutableBag<? extends RegularComponentType<?, ?>> addTypes, Object[] add, ImmutableBag<? extends ComponentType<?, ?>> removeTypes) {
        var archetype = entityIndex.getArchetypeDataForEntity(entityId);
        if (archetype == null) {
            throw new StorageEngineException("Cannot add components to entity %d: Entity not present in storage".formatted(entityId));
        }

        // Discover new component types so that fillRegularComponentTypes will take them into account
        for (int i = 0, s = addTypes.getSize(); i < s; i++) {
            var componentType = addTypes.get(i);
            componentStorage.getComponent(componentType);
        }

        // Determine regular component types from removedTypes
        var regularRemoveTypes = componentTypesPool.getInstance();
        fillRegularComponentTypes(removeTypes, regularRemoveTypes);

        // Modify components
        entityIndex.modifyComponents(entityId, addTypes, add, regularRemoveTypes);

        componentTypesPool.free(regularRemoveTypes);

        // Return pending archetype if present
        var pendingArchetype = getPendingArchetype(entityId);
        return pendingArchetype != null ? pendingArchetype : archetype;
    }

    private void fillRegularComponentTypes(ImmutableBag<? extends ComponentType<?, ?>> componentTypes, Bag<RegularComponentType<?, ?>> regularComponentTypes) {
        for (int i = 0, s = componentTypes.getSize(); i < s; i++) {
            var componentType = componentTypes.get(i);

            if (componentType instanceof RegularComponentType regular) {
                regularComponentTypes.add(regular);
                continue;
            }

            var components = componentStorage.getComponents(componentType);
            for (int c = 0, cs = components.getSize(); c < cs; c++) {
                var component = components.get(c);

                regularComponentTypes.add(component.type());
            }
        }
    }

    private void detectComponentTypes(Bag<RegularComponentType<?, ?>> componentTypes, Object[] components) {
        for (var component : components) {
            componentTypes.add(ComponentType.detectComponentType(component));
        }
    }

    @Override
    @Nullable
    public ArchetypeData getPendingArchetype(int entityId) {
        var pendingChanges = entityIndex.getPendingChanges(entityId);
        if (pendingChanges == null || pendingChanges.isEmpty()) {
            return null;
        }

        return pendingChanges.getPendingArchetypeNode().getArchetype();
    }

    @Override
    public Archetype flushChanges(int entityId) {
        var pendingArchetype = getPendingArchetype(entityId);
        if (pendingArchetype == null) {
            throw new StorageEngineException("Cannot flush changes: Entity %d has no pending changes".formatted(entityId));
        }

        return entityIndex.flushChanges(entityId, pendingArchetype);
    }

    @Override
    public ImmutableBag<Archetype> getArchetypes() {
        return entityIndex.getArchetypes();
    }

    @Override
    public Archetype getArchetypeForEntity(int entityId) {
        return entityIndex.getArchetypeDataForEntity(entityId);
    }

    @Override
    public Archetype getArchetypeById(int archetypeId) {
        return entityIndex.getArchetypeDataById(archetypeId);
    }

    @Override
    public ArchetypeData getArchetype(RegularComponentType<?, ?>... componentTypes) {
        return entityIndex.getArchetype(componentTypes);
    }

}
