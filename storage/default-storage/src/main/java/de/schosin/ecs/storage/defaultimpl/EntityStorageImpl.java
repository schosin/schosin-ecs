package de.schosin.ecs.storage.defaultimpl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import de.schosin.ecs.api.components.types.ComponentType;
import de.schosin.ecs.api.components.types.ComponentType.RegularComponentType;
import de.schosin.ecs.api.components.types.RelationComponentType;
import de.schosin.ecs.storage.api.ComponentStorage;
import de.schosin.ecs.storage.api.EntityStorage;
import de.schosin.ecs.storage.api.StorageEngineException;
import de.schosin.ecs.storage.api.StorageWorld;
import de.schosin.ecs.storage.api.components.Component;
import de.schosin.ecs.storage.api.components.Component.RelationComponent;
import de.schosin.ecs.storage.api.entities.ComponentMask;
import de.schosin.ecs.storage.defaultimpl.entities.ComponentMaskImpl;
import de.schosin.ecs.utils.collections.Bag;
import de.schosin.ecs.utils.collections.BitVector;
import de.schosin.ecs.utils.collections.ImmutableBag;
import de.schosin.ecs.utils.collections.Pool;

public class EntityStorageImpl implements EntityStorage {

    private static final Comparator<Component<?, ?>> COMPONENT_COMPARATOR = Comparator.comparing(Component::id);

    private final ComponentStorage componentStorage;

    private final Bag<ComponentMaskImpl> componentMasksById = new Bag<>(ComponentMaskImpl.class, 64);
    private final ImmutableBag<ComponentMask> immutableComponentMasks = ImmutableBag.create(componentMasksById);

    private final Map<BitVector, ComponentMaskImpl> componentMasks = new ConcurrentHashMap<>();

    private final Bag<ComponentMaskImpl> componentMaskByEntity;

    private final Pool<BitVector> bitvectorPool = Pool.unbounded(BitVector.class, BitVector::new, BitVector::clear);
    private final Pool<Bag<RegularComponentType<?, ?>>> componentTypesPool = Pool.unbounded(Bag.class, () -> new Bag<>(RegularComponentType.class, 8), Bag::clear);
    private final Pool<Bag<Object>> componentPool = Pool.unbounded(Bag.class, () -> new Bag<>(Object.class, 8), Bag::clear);

    public EntityStorageImpl(StorageWorld world, ComponentStorage componentStorage) {
        this.componentStorage = componentStorage;

        this.componentMaskByEntity = world.createEntityBag(ComponentMaskImpl.class);
    }

    @Override
    public ComponentMask getComponentMaskForEntity(int entityId) {
        return this.componentMaskByEntity.get(entityId);
    }

    @Override
    public ComponentMask getComponentMaskById(int componentMaskId) {
        return componentMasksById.get(componentMaskId);
    }

    @Override
    public ComponentMask getComponentMask(RegularComponentType<?, ?>... componentTypes) {
        return componentTypesPool.withInstance(bag -> {
            for (var componentType : componentTypes) {
                bag.add(componentType);
            }

            return resolveComponentMask(bag);
        });
    }

    @Override
    public ComponentMaskImpl addToComponentMask(ComponentMask mask, ImmutableBag<? extends RegularComponentType<?, ?>> componentTypes) {
        if (!(mask instanceof ComponentMaskImpl componentMask)) {
            throw new StorageEngineException("Detected unknown component mask. Only use component masks received from the same storage engine: %s".formatted(mask));
        }

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

    @Override
    public ComponentMask removeFromComponentMask(ComponentMask mask, ImmutableBag<? extends ComponentType<?, ?>> componentTypes) {
        return componentTypesPool.withInstance(regularComponentTypes -> {
            fillRegularComponentTypes(componentTypes, regularComponentTypes);

            return removeRegularFromComponentMask(mask, regularComponentTypes);
        });
    }

    private ComponentMaskImpl removeRegularFromComponentMask(ComponentMask mask, ImmutableBag<? extends RegularComponentType<?, ?>> componentTypes) {
        if (!(mask instanceof ComponentMaskImpl result)) {
            throw new StorageEngineException("Detected unknown component mask. Only use component masks received from the same storage engine: %s".formatted(mask));
        }

        if (componentTypes.isEmpty()) {
            return result;
        }

        return componentTypesPool.withInstance(nextComponentTypes -> {
            var componentMask = result;

            for (int i = 0, s = componentTypes.getSize(); i < s; i++) {
                var componentType = componentTypes.get(i);
                var component = componentStorage.getComponent(componentType);

                if (!componentMask.contains(component.id())) {
                    continue;
                }

                var remove = componentMask.getRemove();
                var next = remove.get(component.id());
                if (next == null) {
                    nextComponentTypes.addAll(componentMask.getComponentTypes());
                    nextComponentTypes.remove(componentType);

                    next = resolveComponentMask(nextComponentTypes);
                    remove.set(component.id(), next);

                    nextComponentTypes.clear();
                }

                componentMask = next;
            }

            return componentMask;
        });
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
    public ComponentMask create(int entityId, Object[] components) {
        return componentTypesPool.withInstance(componentTypes -> {
            detectComponentTypes(componentTypes, components);

            var componentMask = resolveComponentMask(componentTypes);
            return createEntity(entityId, componentMask, componentTypes, components);
        });
    }

    @Override
    public ComponentMask create(int entityId, ComponentMask componentMask, Object[] components) {
        return componentTypesPool.withInstance(componentTypes -> {
            detectComponentTypes(componentTypes, components);

            return create(entityId, (ComponentMaskImpl) componentMask, componentTypes, components);
        });
    }

    @Override
    public ComponentMask create(int entityId, ComponentMask mask, ImmutableBag<? extends RegularComponentType<?, ?>> componentTypes, Object[] components) {
        // Validate component mask and types
        validateComponentTypes("Cannot create entity with component mask %d".formatted(mask.getId()), mask, componentTypes, components);

        // Create entity
        return createEntity(entityId, mask, componentTypes, components);
    }

    private ComponentMask createEntity(int entityId, ComponentMask mask, ImmutableBag<? extends RegularComponentType<?, ?>> componentTypes, Object[] components) {
        if (!(mask instanceof ComponentMaskImpl componentMask)) {
            throw new StorageEngineException("Detected unknown component mask. Only use component masks received from the same storage engine: %s".formatted(mask));
        }

        var existing = componentMaskByEntity.get(entityId);
        if (existing != null) {
            throw new StorageEngineException("Cannot create entity %d, already present in storage: %s".formatted(entityId, existing));
        }

        componentMask.addComponents(entityId, componentTypes, components);

        this.componentMaskByEntity.set(entityId, componentMask);

        return componentMask;
    }

    @Override
    public ComponentMask create(int entityId, ComponentMask mask, ImmutableBag<? extends RegularComponentType<?, ?>> componentTypes, ImmutableBag<Object> components) {
        if (!(mask instanceof ComponentMaskImpl componentMask)) {
            throw new StorageEngineException("Detected unknown component mask. Only use component masks received from the same storage engine: %s".formatted(mask));
        }

        // Validate component mask and types 
        validateComponentTypes("Cannot create entity with component mask %d".formatted(mask.getId()), mask, componentTypes, components);

        var existing = componentMaskByEntity.get(entityId);
        if (existing != null) {
            throw new StorageEngineException("Cannot create entity %d, already present in storage: %s".formatted(entityId, existing));
        }

        componentMask.addComponents(entityId, componentTypes, components);

        this.componentMaskByEntity.set(entityId, componentMask);

        return componentMask;
    }

    @Override
    public ComponentMask add(int entityId, Object[] components) {
        return componentTypesPool.withInstance(componentTypes -> {
            detectComponentTypes(componentTypes, components);

            return addComponents(entityId, componentTypes, components);
        });
    }

    @Override
    public ComponentMask add(int entityId, ImmutableBag<? extends RegularComponentType<?, ?>> componentTypes, Object[] components) {
        validateComponentTypes("Cannot add %d components to entity %d".formatted(components.length, entityId), null, componentTypes, components);

        return addComponents(entityId, componentTypes, components);
    }

    private ComponentMask addComponents(int entityId, ImmutableBag<? extends RegularComponentType<?, ?>> componentTypes, Object[] components) {
        var existing = componentMaskByEntity.get(entityId);
        if (existing == null) {
            throw new StorageEngineException("Cannot add components to entity %d: Entity not present in storage".formatted(entityId));
        }

        // Calculate new component mask
        var componentMask = addToComponentMask(existing, componentTypes);

        // Add components
        componentMask.addComponents(entityId, componentTypes, components);

        // Set new component mask
        this.componentMaskByEntity.set(entityId, componentMask);

        return componentMask;
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
        if (componentMask != null) {
            expected.addAll(componentMask.getComponentTypes());
        }

        var unexpected = componentTypesPool.getInstance();
        var relations = componentTypesPool.getInstance();

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

            if (componentMask != null && !expected.remove(expectedType) && !relations.contains(expectedType)) {
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
        var existing = componentMaskByEntity.get(entityId);
        if (existing == null) {
            throw new StorageEngineException("Cannot remove components from entity %d: Entity not present in storage".formatted(entityId));
        }

        return componentTypesPool.withInstance(regularComponentTypes -> {
            fillRegularComponentTypes(componentTypes, regularComponentTypes);

            // Calculate new component mask
            var componentMask = removeRegularFromComponentMask(existing, regularComponentTypes);

            // Remove components
            existing.removeComponents(entityId, regularComponentTypes);

            // Set new component mask
            this.componentMaskByEntity.set(entityId, componentMask);

            return componentMask;
        });
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

    @Override
    public ComponentMask delete(int entityId) {
        var existing = componentMaskByEntity.get(entityId);
        if (existing == null) {
            throw new StorageEngineException("Cannot delete entity %d: Entity not present in storage".formatted(entityId));
        }

        // Remove components
        existing.removeComponents(entityId);

        // Remove entity from storage
        this.componentMaskByEntity.set(entityId, null);

        return existing;
    }

    private void detectComponentTypes(Bag<RegularComponentType<?, ?>> componentTypes, Object[] components) {
        for (var component : components) {
            componentTypes.add(ComponentType.detectComponentType(component));
        }
    }

    private ComponentMaskImpl resolveComponentMask(ImmutableBag<RegularComponentType<?, ?>> componentTypes) {
        return bitvectorPool.withInstance(vector -> {
            // Fill vector
            for (int i = 0, s = componentTypes.getSize(); i < s; i++) {
                var componentId = componentStorage.getComponent(componentTypes.get(i)).id();
                vector.set(componentId);
            }

            // Lookup known component mask
            var result = componentMasks.get(vector);
            if (result != null) {
                return result;
            }

            // Compute compute mask
            vector = new BitVector(vector);
            return componentMasks.computeIfAbsent(vector, mask -> createComponentMask(mask, componentTypes));
        });
    }

    private ComponentMaskImpl createComponentMask(BitVector mask, ImmutableBag<RegularComponentType<?, ?>> componentTypes) {
        var components = new Bag<Component<?, ?>>(Component.class, componentTypes.getSize());
        var lookup = HashMap.<RegularComponentType<?, ?>, Component<?, ?>>newHashMap(componentTypes.getSize());

        for (int i = 0, s = componentTypes.getSize(); i < s; i++) {
            var componentType = componentTypes.get(i);
            var component = componentStorage.getComponent(componentType);

            components.add(component);
            lookup.put(componentType, component);
        }

        var componentMask = new ComponentMaskImpl(componentMasks.size(), mask, sortComponents(components), lookup);
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

}
