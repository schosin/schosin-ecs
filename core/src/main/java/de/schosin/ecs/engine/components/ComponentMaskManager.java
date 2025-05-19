package de.schosin.ecs.engine.components;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

import de.schosin.ecs.api.components.ComponentType.RegularComponentType;
import de.schosin.ecs.engine.BagManager;
import de.schosin.ecs.engine.entities.EntityManager.ComponentsPredicate;
import de.schosin.ecs.storage.api.components.Component;
import de.schosin.ecs.storage.api.components.Component.RelationComponent;
import de.schosin.ecs.utils.collections.Bag;
import de.schosin.ecs.utils.collections.BitVector;
import de.schosin.ecs.utils.collections.Pool;

/**
 * Manages {@link ComponentMask component masks} and their transitions when 
 * {@link #addComponent(ComponentMask, int) adding} or {@link #removeComponent(ComponentMask, int) removing}
 * components.
 * 
 * <p>
 * By giving each {@link ComponentMask component mask} an identity and caching the new masks when adding
 * or removing a component, these operations can be made a lot faster. In addition checking whether a 
 * {@link ComponentsPredicate} is interested in an entity with a given {@link ComponentMask component mask} can be cached
 * for faster entity creation and modification.
 * </p>
 */
public class ComponentMaskManager {

    private final BagManager bagManager;
    private final ComponentManager componentManager;

    private final Map<BitVector, ComponentMask> componentMasks = new ConcurrentHashMap<>();
    private final Bag<ComponentMask> componentMasksById = new Bag<>(ComponentMask.class, 64);

    private final Pool<BitVector> pool = Pool.unbounded(BitVector.class, BitVector::new, BitVector::clear);

    public ComponentMaskManager(BagManager bagManager, ComponentManager componentManager) {
        this.bagManager = bagManager;
        this.componentManager = componentManager;
    }

    public ComponentMask getComponentMask(int componentMaskId) {
        return componentMasksById.get(componentMaskId);
    }

    public ComponentMask getComponentMask(RegularComponentType<?, ?>... components) {
        return pool.withInstance(componentMask -> {
            // Build component bitmask
            componentManager.fillVector(componentMask, components);

            // Lookup cached component mask
            var result = componentMasks.get(componentMask);
            if (result != null) {
                return result;
            }

            // Compute compute mask
            componentMask = new BitVector(componentMask);
            return componentMasks.computeIfAbsent(componentMask, mask -> createComponentMask(mask, componentsFromTypes(components)));
        });
    }

    /**
     * Creates a bitmask for the given components and uses that to lookup or create
     * a unique {@link ComponentMask} for that mask.
     * 
     * @param components components
     * @return unique {@link ComponentMask}
     */
    public ComponentMask getComponentMask(Object... components) {
        return pool.withInstance(componentMask -> {
            // Build component bitmask
            for (int i = 0, s = components.length; i < s; i++) {
                var componentId = componentManager.getComponent(components[i]).id();
                componentMask.set(componentId);
            }

            // Lookup cached mask
            var result = componentMasks.get(componentMask);
            if (result != null) {
                return result;
            }

            // Compute mask if still absent
            componentMask = new BitVector(componentMask);
            return componentMasks.computeIfAbsent(componentMask, mask -> createComponentMask(mask, componentsFromObjects(components)));
        });
    }

    /**
     * Creates a bitmask for the given components and uses that to lookup or create
     * a unique {@link ComponentMask} for that mask.
     * 
     * @param components components
     * @return unique {@link ComponentMask}
     */
    public ComponentMask getComponentMask(Component<?, ?>... components) {
        return pool.withInstance(componentMask -> {
            // Build component bitmask
            for (int i = 0, s = components.length; i < s; i++) {
                var componentId = components[i].id();
                componentMask.set(componentId);
            }

            // Lookup cached mask
            var result = componentMasks.get(componentMask);
            if (result != null) {
                return result;
            }

            // Compute mask if still absent
            componentMask = new BitVector(componentMask);
            return componentMasks.computeIfAbsent(componentMask, mask -> createComponentMask(mask, components));
        });
    }

    private Component<?, ?>[] componentsFromTypes(RegularComponentType<?, ?>[] components) {
        var result = new Component<?, ?>[components.length];
        for (int i = 0, s = components.length; i < s; i++) {
            var metadata = componentManager.getComponent(components[i]);
            result[i] = metadata;
        }

        return result;
    }

    private Component<?, ?>[] componentsFromObjects(Object[] components) {
        var result = new Component<?, ?>[components.length];
        for (int i = 0, s = components.length; i < s; i++) {
            var metadata = componentManager.getComponent(components[i]);
            result[i] = metadata;
        }

        return result;
    }

    private ComponentMask createComponentMask(BitVector componentMask, Component<?, ?>[] components) {
        components = validateComponents(components);

        // Create component mask
        var lookup = bagManager.createComponentIntBag();
        componentMask.iterate(componentId -> lookup.set(componentId, 1));

        var add = bagManager.createComponentBag(ComponentMask.class);
        var remove = bagManager.createComponentBag(ComponentMask.class);

        var result = new ComponentMask(componentMasks.size(), componentMask, components, lookup, add, remove);
        componentMasksById.set(result.getId(), result);

        return result;
    }

    private Component<?, ?>[] validateComponents(Component<?, ?>[] components) {
        var duplicateRelations = 0;

        var set = HashSet.<Component<?, ?>>newHashSet(components.length);
        for (int i = 0, s = components.length; i < s; i++) {
            var component = components[i];

            if (!set.add(component) && component instanceof RelationComponent<?, ?, ?>) {
                duplicateRelations++;
            }
        }

        // Check for duplicates
        if (set.size() != components.length - duplicateRelations) {
            throw new IllegalArgumentException("Detected duplicate component types. %d component types contained %d unique types.".formatted(components.length, set.size()));
        }

        // Return as is if no duplicate relations
        if (duplicateRelations == 0) {
            return components;
        }

        // Deduplicate relations
        return set.toArray(Component<?, ?>[]::new);
    }

    /**
     * Returns the new {@link ComponentMask} that results when the component (componentId) is added
     * to the given {@link ComponentMask componentMask}.
     * 
     * @param componentMask
     * @param componentId
     * @return new component mask
     */
    public ComponentMask addComponent(ComponentMask componentMask, Component<?, ?> metadata) {
        // Return this if unchanged
        var componentId = metadata.id();
        if (componentMask.contains(componentId)) {
            return componentMask;
        }

        // Check cached mapping
        var mapping = componentMask.getAddMapping();

        var result = mapping.get(componentId);
        if (result != null) {
            return result;
        }

        synchronized (componentMask) {
            // Retry cached mapping
            result = mapping.get(componentId);
            if (result != null) {
                return result;
            }

            // Create new component mask
            var newMask = new BitVector(componentMask.getMask());
            newMask.set(componentId);

            var newComponents = Arrays.copyOf(componentMask.getComponents(), componentMask.getComponents().length + 1);
            newComponents[newComponents.length - 1] = metadata;

            result = componentMasks.computeIfAbsent(newMask, key -> createComponentMask(newMask, newComponents));
            mapping.set(componentId, result);

            return result;
        }
    }

    /**
     * Returns the new {@link ComponentMask} that results when the component (componentId) is removed
     * from the given {@link ComponentMask componentMask}.
     * 
     * @param componentMask
     * @param componentId
     * @return new component mask
     */
    public ComponentMask removeComponent(ComponentMask componentMask, Component<?, ?> metadata) {
        var componentId = metadata.id();
        if (!componentMask.contains(componentId)) {
            return componentMask;
        }

        var mapping = componentMask.getRemoveMapping();

        var result = mapping.get(componentId);
        if (result != null) {
            return result;
        }

        synchronized (componentMask) {
            // Retry cached mapping
            result = mapping.get(componentId);
            if (result != null) {
                return result;
            }

            var newMask = new BitVector(componentMask.getMask());
            newMask.clear(componentId);

            var newComponents = Arrays.stream(componentMask.getComponents())
                    .filter(existing -> !existing.equals(metadata))
                    .toArray(Component[]::new);

            result = componentMasks.computeIfAbsent(newMask, key -> createComponentMask(newMask, newComponents));
            mapping.set(componentId, result);

            return result;
        }
    }

    public void getComponentMasks(Predicate<ComponentMask> predicate, Bag<ComponentMask> fill) {
        for (var componentMask : componentMasks.values()) {
            if (predicate.test(componentMask)) {
                fill.add(componentMask);
            }
        }
    }

}
