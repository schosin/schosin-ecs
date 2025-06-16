package de.schosin.ecs.storage.archetype;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
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
import de.schosin.ecs.storage.api.components.Component;
import de.schosin.ecs.storage.api.components.Component.RelationComponent;
import de.schosin.ecs.storage.api.entities.Archetype;
import de.schosin.ecs.storage.api.entities.ComponentMask;
import de.schosin.ecs.storage.archetype.components.ComponentIndex;
import de.schosin.ecs.storage.archetype.entities.ComponentMaskImpl;
import de.schosin.ecs.storage.archetype.entities.EntityIndex;
import de.schosin.ecs.utils.collections.Bag;
import de.schosin.ecs.utils.collections.BitVector;
import de.schosin.ecs.utils.collections.ImmutableBag;
import de.schosin.ecs.utils.collections.Pool;

public class EntityStorageImpl implements EntityStorage, ArchetypeStorage {

    private static final Comparator<Component<?, ?>> COMPONENT_COMPARATOR = Comparator.comparing(Component::id);

    private final ComponentIndex componentIndex;
    private final EntityIndex entityIndex;

    private final ComponentStorage componentStorage;

    private final Bag<ComponentMaskImpl> componentMasksById = new Bag<>(ComponentMaskImpl.class, 64);
    private final ImmutableBag<ComponentMask> immutableComponentMasks = ImmutableBag.create(componentMasksById);

    private final Map<BitVector, ComponentMaskImpl> componentMasks = new ConcurrentHashMap<>();

    private final Pool<BitVector> bitvectorPool = Pool.unbounded(BitVector.class, BitVector::new, BitVector::clear);
    private final Pool<Bag<RegularComponentType<?, ?>>> componentTypesPool = Pool.unbounded(Bag.class, () -> new Bag<>(RegularComponentType.class, 8), Bag::clear);
    private final Pool<Bag<Object>> componentPool = Pool.unbounded(Bag.class, () -> new Bag<>(Object.class, 8), Bag::clear);

    public EntityStorageImpl(ComponentIndex componentIndex, EntityIndex entityIndex, ComponentStorage componentStorage) {
        this.componentIndex = componentIndex;
        this.entityIndex = entityIndex;

        this.componentStorage = componentStorage;
    }

    @Override
    public DataAccessor getAccessor(int entityId) {
        return entityIndex.getAccessor(entityId);
    }

    @Override
    public ComponentMask getComponentMaskForEntity(int entityId) {
        var data = entityIndex.getArchetypeDataForEntity(entityId);
        if (data == null) {
            return null;
        }

        return data.getComponentMask();
    }

    @Override
    public ComponentMask getComponentMaskById(int componentMaskId) {
        return componentMasksById.get(componentMaskId);
    }

    @Override
    public ComponentMaskImpl getComponentMask(RegularComponentType<?, ?>... componentTypes) {
        var bag = componentTypesPool.getInstance();

        for (var componentType : componentTypes) {
            bag.add(componentType);
        }

        var componentMask = resolveComponentMask(bag);

        componentTypesPool.free(bag);
        return componentMask;
    }

    private ComponentMaskImpl addToComponentMask(ComponentMask mask, ImmutableBag<? extends RegularComponentType<?, ?>> componentTypes) {
        var componentMask = (ComponentMaskImpl) mask;

        if (componentTypes.isEmpty()) {
            return componentMask;
        }

        for (int i = 0, s = componentTypes.getSize(); i < s; i++) {
            var componentType = componentTypes.get(i);
            var component = componentStorage.getComponent(componentType);

            if (componentMask.contains(component.id())) {
                continue;
            }

            var add = componentMask.getAdd();
            add.ensureCapacity(component.id());

            var next = add.get(component.id());
            if (next == null) {
                var nextComponentTypes = new Bag<>(componentMask.getComponentTypes());
                nextComponentTypes.add(componentType);

                next = resolveComponentMask(nextComponentTypes);
                add.set(component.id(), next);
            }

            componentMask = next;
        }

        return componentMask;
    }

    private ComponentMaskImpl removeFromComponentMask(ComponentMask componentMask, ImmutableBag<? extends ComponentType<?, ?>> componentTypes) {
        var regularComponentTypes = componentTypesPool.getInstance();
        fillRegularComponentTypes(componentTypes, regularComponentTypes);

        var result = removeRegularFromComponentMask(componentMask, regularComponentTypes);

        componentTypesPool.free(regularComponentTypes);
        return result;
    }

    private ComponentMaskImpl removeRegularFromComponentMask(ComponentMask mask, ImmutableBag<? extends RegularComponentType<?, ?>> componentTypes) {
        var componentMask = (ComponentMaskImpl) mask;

        if (componentTypes.isEmpty()) {
            return componentMask;
        }

        var nextComponentTypes = componentTypesPool.getInstance();
        var result = componentMask;

        for (int i = 0, s = componentTypes.getSize(); i < s; i++) {
            var componentType = componentTypes.get(i);
            var component = componentStorage.getComponent(componentType);

            if (!result.contains(component.id())) {
                continue;
            }

            var remove = result.getRemove();
            remove.ensureCapacity(component.id());

            var next = remove.get(component.id());
            if (next == null) {
                nextComponentTypes.addAll(result.getComponentTypes());
                nextComponentTypes.remove(componentType);

                next = resolveComponentMask(nextComponentTypes);
                remove.set(component.id(), next);

                nextComponentTypes.clear();
            }

            result = next;
        }

        componentTypesPool.free(nextComponentTypes);
        return result;
    }

    @Override
    public ImmutableBag<ComponentMask> getComponentMasks() {
        return immutableComponentMasks;
    }

    @Override
    public void getComponentMasks(Predicate<ComponentMask> predicate, Bag<ComponentMask> fill) {
        for (int i = 0, s = componentMasksById.getSize(); i < s; i++) {
            var componentMask = componentMasksById.get(i);

            if (predicate.test(componentMask)) {
                fill.add(componentMask);
            }
        }
    }

    @Override
    public ComponentMask create(int entityId, ComponentMask componentMask, Object[] components) {
        var componentTypes = componentTypesPool.getInstance();
        detectComponentTypes(componentTypes, components);

        var result = create(entityId, (ComponentMaskImpl) componentMask, componentTypes, components);

        componentTypesPool.free(componentTypes);
        return result;
    }

    @Override
    public ComponentMask create(int entityId, ComponentMask mask, ImmutableBag<? extends RegularComponentType<?, ?>> componentTypes, Object[] components) {
        // Validate component mask and types
        validateComponentTypes("Cannot create entity with component mask %d".formatted(mask.getId()), mask, componentTypes, components);

        // Create entity
        return createEntity(entityId, mask, componentTypes, components);
    }

    private ComponentMask createEntity(int entityId, ComponentMask mask, ImmutableBag<? extends RegularComponentType<?, ?>> componentTypes, Object[] components) {
        var componentMask = (ComponentMaskImpl) mask;

        var existing = entityIndex.getArchetypeDataForEntity(entityId);
        if (existing != null) {
            throw new StorageEngineException("Cannot create entity %d, already present in storage: %s".formatted(entityId, existing));
        }

        var bag = componentPool.getInstance();
        for (int i = 0, s = components.length; i < s; i++) {
            bag.add(components[i]);
        }

        entityIndex.createEntity(entityId, componentMask, componentTypes, bag);

        componentPool.free(bag);
        return componentMask;
    }

    @Override
    public ComponentMask create(int entityId, ComponentMask mask, ImmutableBag<? extends RegularComponentType<?, ?>> componentTypes, ImmutableBag<Object> components) {
        var componentMask = (ComponentMaskImpl) mask;

        // Validate component mask and types 
        validateComponentTypes("Cannot create entity with component mask %d".formatted(mask.getId()), mask, componentTypes, components);

        var existing = entityIndex.getArchetypeDataForEntity(entityId);
        if (existing != null) {
            throw new StorageEngineException("Cannot create entity %d, already present in storage: %s".formatted(entityId, existing));
        }

        entityIndex.createEntity(entityId, componentMask, componentTypes, components);

        return componentMask;
    }

    @Override
    public ComponentMask add(int entityId, Object[] components) {
        var componentTypes = componentTypesPool.getInstance();
        detectComponentTypes(componentTypes, components);

        var result = addComponents(entityId, componentTypes, components);

        componentTypesPool.free(componentTypes);
        return result;
    }

    @Override
    public ComponentMask add(int entityId, ImmutableBag<? extends RegularComponentType<?, ?>> componentTypes, Object[] components) {
        validateComponentTypes("Cannot add %d components to entity %d".formatted(components.length, entityId), null, componentTypes, components);

        return addComponents(entityId, componentTypes, components);
    }

    private ComponentMask addComponents(int entityId, ImmutableBag<? extends RegularComponentType<?, ?>> componentTypes, Object[] components) {
        var componentMask = entityIndex.getComponentMask(entityId);
        if (componentMask == null) {
            throw new StorageEngineException("Cannot add components to entity %d: Entity not present in storage".formatted(entityId));
        }

        // Add components
        entityIndex.addComponents(entityId, componentTypes, components);

        // Return pending component mask if present
        var pendingComponentMask = getPendingComponentMask(entityId);
        return pendingComponentMask != null ? pendingComponentMask : componentMask;
    }

    private void validateComponentTypes(String context, ComponentMask componentMask, ImmutableBag<? extends RegularComponentType<?, ?>> expectedTypes, Object[] components) {
        var bag = componentPool.getInstance();

        for (int i = 0, s = components.length; i < s; i++) {
            bag.add(components[i]);
        }

        validateComponentTypes(context, componentMask, expectedTypes, bag);

        componentPool.free(bag);
    }

    private void validateComponentTypes(String context, ComponentMask componentMask, ImmutableBag<? extends RegularComponentType<?, ?>> expectedTypes, ImmutableBag<Object> components) {
        List<String> errors = null;

        if (expectedTypes.getSize() != components.getSize()) {
            errors = new ArrayList<>();

            errors.add("Expected %d component types, but got %d".formatted(components.getSize(), expectedTypes.getSize()));
        }

        var expected = componentTypesPool.getInstance();
        var unexpected = componentTypesPool.getInstance();
        var relations = componentTypesPool.getInstance();

        if (componentMask != null) {
            expected.addAll(componentMask.getComponentTypes());
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

            if (componentMask != null && expectedType != null && !expected.remove(expectedType) && !relations.contains(expectedType)) {
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
    public ComponentMask remove(int entityId, ImmutableBag<? extends ComponentType<?, ?>> componentTypes) {
        var componentMask = entityIndex.getComponentMask(entityId);
        if (componentMask == null) {
            throw new StorageEngineException("Cannot remove components from entity %d: Entity not present in storage".formatted(entityId));
        }

        // Determine regular component types from removedTypes
        var regularComponentTypes = componentTypesPool.getInstance();
        fillRegularComponentTypes(componentTypes, regularComponentTypes);

        // Remove components
        entityIndex.removeComponents(entityId, regularComponentTypes);

        componentTypesPool.free(regularComponentTypes);

        // Return pending component mask if present
        var pendingComponentMask = getPendingComponentMask(entityId);
        return pendingComponentMask != null ? pendingComponentMask : componentMask;
    }

    @Override
    public ComponentMask delete(int entityId) {
        return entityIndex.deleteEntity(entityId);
    }

    @Override
    public ComponentMask modify(int entityId, Object[] add, ImmutableBag<? extends ComponentType<?, ?>> removeTypes) {
        var addTypes = componentTypesPool.getInstance();
        detectComponentTypes(addTypes, add);

        var result = modifyComponents(entityId, addTypes, add, removeTypes);

        componentTypesPool.free(addTypes);
        return result;
    }

    @Override
    public ComponentMask modify(int entityId, ImmutableBag<? extends RegularComponentType<?, ?>> addTypes, Object[] add, ImmutableBag<? extends ComponentType<?, ?>> removeTypes) {
        validateComponentTypes("Cannot add %d components to entity %d".formatted(add.length, entityId), null, addTypes, add);

        return modifyComponents(entityId, addTypes, add, removeTypes);
    }

    private ComponentMask modifyComponents(int entityId, ImmutableBag<? extends RegularComponentType<?, ?>> addTypes, Object[] add, ImmutableBag<? extends ComponentType<?, ?>> removeTypes) {
        var componentMask = entityIndex.getComponentMask(entityId);
        if (componentMask == null) {
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

        // Return pending component mask if present
        var pendingComponentMask = getPendingComponentMask(entityId);
        return pendingComponentMask != null ? pendingComponentMask : componentMask;
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

    private ComponentMaskImpl resolveComponentMask(ImmutableBag<RegularComponentType<?, ?>> componentTypes) {
        var vector = bitvectorPool.getInstance();

        // Fill vector
        for (int i = 0, s = componentTypes.getSize(); i < s; i++) {
            var componentId = componentIndex.getId(componentTypes.get(i));
            vector.set(componentId);
        }

        // Lookup known component mask
        var result = componentMasks.get(vector);
        if (result != null) {
            bitvectorPool.free(vector);
            return result;
        }

        // Compute compute mask
        var key = new BitVector(vector);
        result = componentMasks.computeIfAbsent(key, mask -> createComponentMask(mask, componentTypes));

        bitvectorPool.free(vector);
        return result;
    }

    private ComponentMaskImpl createComponentMask(BitVector mask, ImmutableBag<RegularComponentType<?, ?>> componentTypes) {
        var components = new Bag<Component<?, ?>>(Component.class, componentTypes.getSize());

        for (int i = 0, s = componentTypes.getSize(); i < s; i++) {
            var componentType = componentTypes.get(i);
            var component = componentStorage.getComponent(componentType);

            components.add(component);
        }

        var componentMask = new ComponentMaskImpl(componentMasks.size(), mask, sortComponents(components));
        componentMasksById.add(componentMask);

        return componentMask;
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

    @Override
    @Nullable
    public ComponentMaskImpl getPendingComponentMask(int entityId) {
        var pendingChanges = entityIndex.getPendingChanges(entityId);
        if (pendingChanges == null) {
            return null;
        }

        var componentMask = pendingChanges.getComponentMask();
        componentMask = addToComponentMask(componentMask, pendingChanges.getAddedTypes());
        return removeFromComponentMask(componentMask, pendingChanges.getRemovedTypes());
    }

    @Override
    public ComponentMask flushChanges(int entityId) {
        var pendingComponentMask = getPendingComponentMask(entityId);
        if (pendingComponentMask == null) {
            throw new StorageEngineException("Cannot flush changes: Entity %d has no pending changes".formatted(entityId));
        }

        return entityIndex.flushChanges(entityId, pendingComponentMask);
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
    public Archetype getArchetype(RegularComponentType<?, ?>... componentTypes) {
        var componentMask = getComponentMask(componentTypes);

        return entityIndex.getArchetype(componentMask);
    }

}
