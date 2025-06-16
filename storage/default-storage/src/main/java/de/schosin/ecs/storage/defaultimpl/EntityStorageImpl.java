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

import org.jspecify.annotations.Nullable;

import de.schosin.ecs.api.components.Relation.ComponentRelation;
import de.schosin.ecs.api.components.Relation.EntityRelation;
import de.schosin.ecs.api.components.Relations;
import de.schosin.ecs.api.components.Relations.ComponentRelations;
import de.schosin.ecs.api.components.Relations.EntityRelations;
import de.schosin.ecs.api.components.types.ComponentType;
import de.schosin.ecs.api.components.types.ComponentType.RegularComponentType;
import de.schosin.ecs.api.components.types.RelationComponentType;
import de.schosin.ecs.api.data.DataAccessor;
import de.schosin.ecs.storage.api.ArchetypeStorage;
import de.schosin.ecs.storage.api.EntityStorage;
import de.schosin.ecs.storage.api.StorageEngineException;
import de.schosin.ecs.storage.api.StorageWorld;
import de.schosin.ecs.storage.api.components.Component;
import de.schosin.ecs.storage.api.components.Component.RelationComponent;
import de.schosin.ecs.storage.api.entities.Archetype;
import de.schosin.ecs.storage.api.entities.ComponentMask;
import de.schosin.ecs.storage.common.PendingChanges;
import de.schosin.ecs.storage.common.results.ComponentRelationResultImpl;
import de.schosin.ecs.storage.common.results.EntityRelationResultImpl;
import de.schosin.ecs.storage.defaultimpl.archetype.ArchetypeImpl;
import de.schosin.ecs.storage.defaultimpl.archetype.ArchetypeManager;
import de.schosin.ecs.storage.defaultimpl.components.DefaultComponent;
import de.schosin.ecs.storage.defaultimpl.entities.ComponentMaskImpl;
import de.schosin.ecs.utils.collections.Bag;
import de.schosin.ecs.utils.collections.BitVector;
import de.schosin.ecs.utils.collections.ImmutableBag;
import de.schosin.ecs.utils.collections.Pool;

public class EntityStorageImpl implements EntityStorage, ArchetypeStorage {

    private static final Comparator<Component<?, ?>> COMPONENT_COMPARATOR = Comparator.comparing(Component::id);

    private final ComponentStorageImpl componentStorage;
    private final ArchetypeManager archetypeManager;

    private final Bag<ComponentMaskImpl> componentMasksById = new Bag<>(ComponentMaskImpl.class, 64);
    private final ImmutableBag<ComponentMask> immutableComponentMasks = ImmutableBag.create(componentMasksById);

    private final Map<BitVector, ComponentMaskImpl> componentMasks = new ConcurrentHashMap<>();

    private final Bag<ComponentMaskImpl> componentMaskByEntity;
    private final Bag<PendingChanges> pendingChanges;

    private final Pool<BitVector> bitvectorPool = Pool.unbounded(BitVector.class, BitVector::new, BitVector::clear);
    private final Pool<Bag<RegularComponentType<?, ?>>> componentTypesPool = Pool.unbounded(Bag.class, () -> new Bag<>(RegularComponentType.class, 8), Bag::clear);
    private final Pool<Bag<Object>> componentPool = Pool.unbounded(Bag.class, () -> new Bag<>(Object.class, 8), Bag::clear);

    public EntityStorageImpl(StorageWorld world, Bag<PendingChanges> pendingChanges, ComponentStorageImpl componentStorage) {
        this.componentStorage = componentStorage;
        this.archetypeManager = new ArchetypeManager(world, componentStorage, this);

        this.componentMaskByEntity = world.createEntityBag(ComponentMaskImpl.class);
        this.pendingChanges = pendingChanges;
    }

    @Override
    public DataAccessor getAccessor(int entityId) {
        var archetype = archetypeManager.getArchetypeForEntity(entityId);
        if (archetype == null) {
            throw new StorageEngineException("Cannot get accessor for entity %d: Entity not present in storage".formatted(entityId));
        }

        return archetype.getAccessor(entityId);
    }

    @Override
    public ComponentMaskImpl getComponentMaskForEntity(int entityId) {
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

    private ComponentMaskImpl removeFromComponentMask(ComponentMask mask, ImmutableBag<? extends ComponentType<?, ?>> componentTypes) {
        var regularComponentTypes = componentTypesPool.getInstance();

        fillRegularComponentTypes(componentTypes, regularComponentTypes);

        var result = removeRegularFromComponentMask(mask, regularComponentTypes);

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

    public void add(ArchetypeImpl archetype, int entityId) {
        var componentMask = archetype.getComponentMask();

        // Set component mask for new entity on existing changes
        var changes = pendingChanges.get(entityId);
        if (changes != null) {
            changes.setComponentMask(componentMask);
        }

        // Track component mask
        this.componentMaskByEntity.set(entityId, componentMask);
        this.archetypeManager.set(entityId, componentMask);
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
        var componentMask = componentMaskByEntity.get(entityId);
        if (componentMask == null) {
            throw new StorageEngineException("Cannot add components to entity %d: Entity not present in storage".formatted(entityId));
        }

        // Get pending changes
        var changes = pendingChanges.get(entityId);
        if (changes == null) {
            changes = new PendingChanges(componentMask);
            pendingChanges.set(entityId, changes);
        }

        // Add components
        for (int i = 0, s = componentTypes.getSize(); i < s; i++) {
            var componentType = componentTypes.get(i);
            var component = components[i];

            if (!changes.add(componentType, component)) {
                addComponent(entityId, componentType, component);
            } else {
                // Discover so that a delayed addition can be removed before flush
                componentStorage.getComponent(componentType);
            }
        }

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
        var componentMask = componentMaskByEntity.get(entityId);
        if (componentMask == null) {
            throw new StorageEngineException("Cannot remove components from entity %d: Entity not present in storage".formatted(entityId));
        }

        return componentTypesPool.withInstance(regularComponentTypes -> {
            fillRegularComponentTypes(componentTypes, regularComponentTypes);

            // Get pending changes
            var changes = pendingChanges.get(entityId);
            if (changes == null) {
                changes = new PendingChanges(componentMask);
                pendingChanges.set(entityId, changes);
            }

            // Remove components
            for (int i = 0, s = regularComponentTypes.getSize(); i < s; i++) {
                changes.remove(regularComponentTypes.get(i));
            }

            var pendingComponentMask = getPendingComponentMask(entityId);
            return pendingComponentMask != null ? pendingComponentMask : componentMask;
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
        this.archetypeManager.remove(entityId);

        // Reset pending changes
        var changes = this.pendingChanges.get(entityId);
        if (changes != null) {
            changes.reset();
        }

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

    @Override
    @Nullable
    public ComponentMask getPendingComponentMask(int entityId) {
        var componentMask = getComponentMaskForEntity(entityId);
        if (componentMask == null) {
            throw new StorageEngineException("Cannot get pending changes for entity %d: Entity not present in storage".formatted(entityId));
        }

        var changes = pendingChanges.get(entityId);
        if (changes == null || changes.isEmpty()) {
            return null;
        }

        componentMask = addToComponentMask(componentMask, changes.getAddedTypes());
        return removeFromComponentMask(componentMask, changes.getRemovedTypes());
    }

    @Override
    public ComponentMask flushChanges(int entityId) {
        var componentMask = getComponentMaskForEntity(entityId);
        if (componentMask == null) {
            throw new StorageEngineException("Cannot get pending changes for entity %d: Entity not present in storage".formatted(entityId));
        }

        var changes = pendingChanges.get(entityId);
        if (changes == null || changes.isEmpty()) {
            throw new StorageEngineException("Cannot flush changes: Entity %d has no pending changes".formatted(entityId));
        }

        var newComponentMask = addToComponentMask(componentMask, changes.getAddedTypes());
        newComponentMask = removeFromComponentMask(newComponentMask, changes.getRemovedTypes());

        // Add components
        var addedTypes = changes.getAddedTypes();
        var added = changes.getAdded();
        for (int i = 0, s = addedTypes.getSize(); i < s; i++) {
            addComponent(entityId, addedTypes.get(i), added.get(i));
        }

        // Remove components
        var removedTypes = changes.getRemovedTypes();
        for (int i = 0, s = removedTypes.getSize(); i < s; i++) {
            var component = (DefaultComponent<?>) componentStorage.getComponent(removedTypes.get(i));
            component.removeComponent(entityId);
        }

        // Track component mask
        this.componentMaskByEntity.set(entityId, newComponentMask);
        this.archetypeManager.set(entityId, newComponentMask);

        // Update component mask on changes, reset state
        changes.setComponentMask(newComponentMask);
        changes.reset();

        return newComponentMask;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void addComponent(int entityId, RegularComponentType<?, ?> componentType, Object component) {
        var mapper = (DefaultComponent) componentStorage.getComponent(componentType);

        switch (component) {
            case ComponentRelationResultImpl relations -> addRelations(mapper, entityId, relations);
            case ComponentRelations<?, ?> relations -> {
                for (int i = 0, s = relations.size(); i < s; i++) {
                    mapper.addComponent(entityId, relations.get(i));
                }

                Relations.free(relations);
            }
            case ComponentRelation<?, ?> relation -> mapper.addComponent(entityId, component);
            case EntityRelationResultImpl relations -> addRelations(mapper, entityId, relations);
            case EntityRelations<?> relations -> {
                for (int i = 0, s = relations.size(); i < s; i++) {
                    mapper.addComponent(entityId, relations.get(i));
                }

                Relations.free(relations);
            }
            case EntityRelation<?> relation -> mapper.addComponent(entityId, component);
            default -> mapper.addComponent(entityId, component);
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void addRelations(DefaultComponent mapper, int entityId, ComponentRelationResultImpl relations) {
        while (!relations.isEmpty()) {
            mapper.addComponent(entityId, relations.removeLast());
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void addRelations(DefaultComponent mapper, int entityId, EntityRelationResultImpl relations) {
        while (!relations.isEmpty()) {
            mapper.addComponent(entityId, relations.removeLast());
        }
    }

    @Override
    public Archetype getArchetypeForEntity(int entityId) {
        return archetypeManager.getArchetypeForEntity(entityId);
    }

    @Override
    public Archetype getArchetypeById(int archetypeId) {
        return archetypeManager.getArchetypeById(archetypeId);
    }

    @Override
    public Archetype getArchetype(RegularComponentType<?, ?>... componentTypes) {
        var bag = componentTypesPool.getInstance();

        for (var type : componentTypes) {
            bag.add(type);
        }

        var componentMask = resolveComponentMask(bag);
        var archetype = archetypeManager.getArchetype(componentMask);

        componentTypesPool.free(bag);

        return archetype;
    }

}
