package de.schosin.ecs.engine.components;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import de.schosin.ecs.api.Pooled;
import de.schosin.ecs.engine.BagManager;
import de.schosin.ecs.engine.EngineWorld.Classes;
import de.schosin.ecs.engine.IdManager;
import de.schosin.ecs.engine.IdManager.Id.ComponentId;
import de.schosin.ecs.engine.utils.ClassUtils;
import de.schosin.ecs.engine.utils.ClassUtils.ClassType;
import de.schosin.ecs.engine.utils.collections.Bag;
import de.schosin.ecs.engine.utils.collections.BitVector;
import de.schosin.ecs.engine.utils.collections.Pool;
import de.schosin.ecs.engine.utils.collections.ReflectionUtils;

/**
 * Manages {@link ComponentData component data} for every component class
 * encountered. 
 * 
 * <p>
 * Each new component class is assigned an {@link ComponentData#id}
 * that is used in several places, including as an index into a Bags,
 * or for optimizing modifications in {@link ComponentMaskManager}.
 * </p>
 */
public class ComponentManager {

    private static final int POOL_LIMIT = 1000000; // TODO configuration or per-class (default method in interface? Annotation? config per-class?)

    private final BagManager bagManager;
    private final IdManager idManager;
    private final Classes classes;

    @SuppressWarnings("rawtypes")
    private final Bag<ComponentDataImpl> byId = new Bag<>(ComponentDataImpl.class, 64);
    private final Map<Class<?>, ComponentDataImpl<?>> byClass = new ConcurrentHashMap<>();

    public ComponentManager(BagManager bagManager, IdManager idManager, Classes classes) {
        this.bagManager = bagManager;
        this.idManager = idManager;
        this.classes = classes;
    }

    public ComponentData<?> getData(int componentId) {
        return byId.get(componentId);
    }

    public <T> ComponentData<T> getData(Class<T> clazz) {
        return getData(clazz, bagManager.getEntitySize());
    }

    @SuppressWarnings("unchecked")
    public <T> ComponentData<T> getData(Class<T> clazz, int bagSize) {
        var result = (ComponentData<T>) byClass.get(clazz);
        if (result != null) {
            return result;
        }

        synchronized (classes) {
            if (classes.states().contains(clazz)) {
                throw new IllegalArgumentException("Class %s is already used as a state.".formatted(clazz.getName()));
            }

            classes.components().add(clazz);
            return (ComponentData<T>) byClass.computeIfAbsent(clazz, ignore -> createMetadata(clazz, bagSize));
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private <T> ComponentDataImpl<T> createMetadata(Class<T> clazz, int bagSize) {
        validateComponentHierarchy(clazz);

        var components = bagManager.createEntityBag(clazz, bagSize);
        var removals = new BitVector(bagSize);
        var pool = Pooled.class.isAssignableFrom(clazz)
                ? Pool.bounded(POOL_LIMIT, clazz, () -> ReflectionUtils.createComponentInstance(clazz))
                : null;

        var metadata = new ComponentDataImpl(createComponentId(), clazz, components, removals, pool);
        byId.set(metadata.id(), metadata);

        return metadata;
    }

    private ComponentId createComponentId() {
        return idManager.createComponentId();
    }

    @SuppressWarnings("unchecked")
    private void validateComponentHierarchy(Class<?> clazz) {
        // Validate invalid types
        var type = ClassUtils.detectType(clazz);
        if (!type.isValidComponent()) {
            throw new IllegalArgumentException("Invalid component '%s' of type '%s'. Allowed types: %s".formatted(clazz.getSimpleName(), type.name().toLowerCase(), ClassType.ALLOWED_COMPONENT_TYPES));
        }

        // Validate no extends/super of existing component type
        var data = byId.getData();
        for (int i = 0, s = byId.getSize(); i < s; i++) {
            var existingClass = data[i].clazz();

            if (existingClass.isAssignableFrom(clazz)) {
                throw new IllegalStateException("Extending another component is not supported: %s extends %s".formatted(clazz.getSimpleName(), existingClass.getSimpleName()));
            } else if (clazz.isAssignableFrom(existingClass)) {
                throw new IllegalStateException("Extending another component is not supported: %s extends %s".formatted(existingClass.getSimpleName(), clazz.getSimpleName()));
            }
        }
    }

    public void removed(int entityId, ComponentMask componentMask) {
        for (var data : componentMask.getComponents()) {
            ((ComponentDataImpl<?>) data).removeComponent(entityId);
        }
    }

    public void fillVector(BitVector vector, Class<?>... components) {
        for (int i = 0, s = components.length; i < s; i++) {
            var componentId = getData(components[i]).id();
            vector.set(componentId);
        }
    }

    // public api
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Collection<ComponentData<?>> getComponents() {
        return (Collection) this.byClass.values();
    }

}
