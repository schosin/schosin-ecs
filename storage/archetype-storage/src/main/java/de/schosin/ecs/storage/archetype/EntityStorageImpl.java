package de.schosin.ecs.storage.archetype;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.jspecify.annotations.Nullable;

import de.schosin.ecs.api.components.types.ComponentType;
import de.schosin.ecs.api.components.types.ComponentType.RegularComponentType;
import de.schosin.ecs.api.components.types.RelationComponentType;
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
    private final Pool<Bag<Object>> componentPool = Pool.unbounded(Bag.class, () -> new Bag<>(Object.class, 8), Bag::clear);

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
        validateComponentTypes("Cannot add %d components to entity %d".formatted(components.length, entityId), null, componentTypes, components);

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

    private void validateComponentTypes(String context, ArchetypeData archetype, ImmutableBag<? extends RegularComponentType<?, ?>> expectedTypes, Object[] components) {
        var bag = componentPool.getInstance();

        for (int i = 0, s = components.length; i < s; i++) {
            bag.add(components[i]);
        }

        validateComponentTypes(context, archetype, expectedTypes, bag);

        componentPool.free(bag);
    }

    private void validateComponentTypes(String context, ArchetypeData archetype, ImmutableBag<? extends RegularComponentType<?, ?>> expectedTypes, ImmutableBag<Object> components) {
        List<String> errors = null;

        if (expectedTypes.getSize() != components.getSize()) {
            errors = new ArrayList<>();

            errors.add("Expected %d component types, but got %d".formatted(components.getSize(), expectedTypes.getSize()));
        }

        var expected = componentTypesPool.getInstance();
        var unexpected = componentTypesPool.getInstance();
        var relations = componentTypesPool.getInstance();

        if (archetype != null) {
            expected.addAll(archetype.getComponentTypes());
        }

        for (int i = 0, s = components.getSize(); i < s; i++) {
            var expectedType = i < expectedTypes.getSize() ? expectedTypes.get(i) : null;
            var component = components.get(i);

            if (expectedType == null) {
                if (errors == null) {
                    errors = new ArrayList<>();
                }

                errors.add("Unexpected component '%s' at index %d".formatted(component, i));
            } else if (!expectedType.isInstance(component)) {
                if (errors == null) {
                    errors = new ArrayList<>();
                }

                errors.add("Expected component type '%s' at index %d, but was '%s'".formatted(expectedType, i, component));
            }

            if (expectedType instanceof RelationComponentType<?, ?, ?> relationType) {
                relations.add(relationType);
            }

            if (archetype != null && expectedType != null && !expected.remove(expectedType) && !relations.contains(expectedType)) {
                unexpected.add(expectedType);
            }
        }

        if (!expected.isEmpty()) {
            if (errors == null) {
                errors = new ArrayList<>();
            }

            var missingTypes = expected.stream().map(Object::toString).toList();
            errors.add("The following component types are missing: %s".formatted(missingTypes));
        }

        for (int i = components.getSize(), s = expectedTypes.getSize(); i < s; i++) {
            unexpected.add(expectedTypes.get(i));
        }

        if (!unexpected.isEmpty()) {
            if (errors == null) {
                errors = new ArrayList<>();
            }

            var unexpectedTypes = unexpected.stream().map(Object::toString).distinct().toList();
            errors.add("The following component types were unexpected: %s".formatted(unexpectedTypes));
        }

        // Free bags
        componentTypesPool.free(expected);
        componentTypesPool.free(unexpected);
        componentTypesPool.free(relations);

        if (errors != null) {
            var message = errors.stream().map(error -> "- " + error).collect(Collectors.joining(System.lineSeparator()));
            throw new StorageEngineException("%s:%s%s".formatted(context, System.lineSeparator(), message.indent(2)));
        }
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
        validateComponentTypes("Cannot add %d components to entity %d".formatted(add.length, entityId), null, addTypes, add);

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
        if (pendingChanges == null) {
            return null;
        }

        var archetype = pendingChanges.getArchetype();

        var addedTypes = pendingChanges.getAddedTypes();
        for (int i = 0, s = addedTypes.getSize(); i < s; i++) {
            archetype = archetype.addComponentType(addedTypes.get(i));
        }

        var removedTypes = pendingChanges.getRemovedTypes();
        for (int i = 0, s = removedTypes.getSize(); i < s; i++) {
            archetype = archetype.removeComponentType(removedTypes.get(i));
        }

        return archetype;
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
