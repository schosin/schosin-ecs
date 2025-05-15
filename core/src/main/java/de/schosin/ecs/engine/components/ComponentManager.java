package de.schosin.ecs.engine.components;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import de.schosin.ecs.api.Pooled;
import de.schosin.ecs.api.components.ComponentType;
import de.schosin.ecs.api.components.ComponentType.RegularComponentType;
import de.schosin.ecs.engine.BagManager;
import de.schosin.ecs.engine.EngineWorld.Classes;
import de.schosin.ecs.engine.IdManager;
import de.schosin.ecs.engine.IdManager.Id.ComponentId;
import de.schosin.ecs.engine.utils.ClassUtils;
import de.schosin.ecs.engine.utils.ClassUtils.ClassType;
import de.schosin.ecs.engine.utils.exceptions.UnsupportedComponentTypeException;
import de.schosin.ecs.utils.ReflectionUtils;
import de.schosin.ecs.utils.collections.Bag;
import de.schosin.ecs.utils.collections.BitVector;
import de.schosin.ecs.utils.collections.Pool;

/**
 * Manages {@link Component component data} for every component class
 * encountered. 
 * 
 * <p>
 * Each new component class is assigned an {@link Component#id}
 * that is used in several places, including as an index into a Bags,
 * or for optimizing modifications in {@link ComponentMaskManager}.
 * </p>
 */
public class ComponentManager {

    private static final int POOL_LIMIT = 1000000; // TODO configuration or per-class (default method in interface? Annotation? config per-class?)

    private final BagManager bagManager;
    private final IdManager idManager;
    private final Classes classes;

    private final Bag<Component<?>> byId = new Bag<>(Component.class, 64);
    private final Map<Class<?>, ComponentData<?>> byClass = new ConcurrentHashMap<>();

    public ComponentManager(BagManager bagManager, IdManager idManager, Classes classes) {
        this.bagManager = bagManager;
        this.idManager = idManager;
        this.classes = classes;
    }

    public Component<?> getComponent(int componentId) {
        return byId.get(componentId);
    }

    public <T> Component<T> getComponent(RegularComponentType<T> type) {
        return switch (type) {
            case ComponentType.ClassType<T> classType -> getData(classType);
        };
    }

    /**
     * Do no use {@link ComponentType} for accessing components. Use {@link #getComponent(RegularComponentType)} instead.
     * 
     * @throws UnsupportedComponentTypeException operation not supported
     */
    @Deprecated
    public Component<?> getComponent(ComponentType<?> type) throws UnsupportedComponentTypeException {
        throw new UnsupportedComponentTypeException(type, "ComponentType '%s' not allowed, must use RegularComponentType for accessing components.".formatted(type));
    }

    @SuppressWarnings("unchecked")
    public <T> Component<T> getComponent(T component) {
        return switch (component) {
            case null -> throw new IllegalArgumentException("Cannot get component type for null instance");
            default -> getComponent(ComponentType.component((Class<T>) component.getClass()));
        };
    }

    @SuppressWarnings("unchecked")
    private <T> ComponentData<T> getData(ComponentType.ClassType<T> type) {
        var result = (ComponentData<T>) byClass.get(type.clazz());
        if (result != null) {
            return result;
        }

        synchronized (classes) {
            if (classes.states().contains(type.clazz())) {
                throw new IllegalArgumentException("Class %s is already used as a state.".formatted(type.clazz().getName()));
            }

            classes.components().add(type.clazz());
            return (ComponentData<T>) byClass.computeIfAbsent(type.clazz(), ignore -> createMetadata(type, bagManager.getEntitySize()));
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private <T> ComponentDataImpl<T> createMetadata(ComponentType.ClassType<T> type, int bagSize) {
        validateComponentHierarchy(type.clazz());

        var components = bagManager.createEntityBag(type.clazz(), bagSize);
        var removals = new BitVector(bagSize);
        var pool = Pooled.class.isAssignableFrom(type.clazz())
                ? Pool.bounded(POOL_LIMIT, type.clazz(), () -> ReflectionUtils.createComponentInstance(type.clazz()))
                : null;

        var metadata = new ComponentDataImpl(createComponentId(), type, components, removals, pool);
        byId.set(metadata.id(), metadata);

        return metadata;
    }

    private ComponentId createComponentId() {
        return idManager.createComponentId();
    }

    @SuppressWarnings("unused") // I really want exhaustive switch statements
    private void validateComponentHierarchy(Class<?> clazz) {
        // Validate invalid types
        var type = ClassUtils.detectType(clazz);
        if (!type.isValidComponent()) {
            throw new IllegalArgumentException("Invalid component '%s' of type '%s'. Allowed types: %s".formatted(clazz.getSimpleName(), type.name().toLowerCase(), ClassType.ALLOWED_COMPONENT_TYPES));
        }

        // Validate no extends/super of existing component type
        var data = byId.getData();
        for (int i = 0, s = byId.getSize(); i < s; i++) {
            var valid = switch (data[i]) {
                case ComponentData<?> c -> validateComponentHierarchy(clazz, c.clazz());
                case null -> true;
            };
        }
    }

    private boolean validateComponentHierarchy(Class<?> clazz, Class<?> existingClass) {
        if (existingClass.isAssignableFrom(clazz)) {
            throw new IllegalStateException("Extending another component is not supported: %s extends %s".formatted(clazz.getSimpleName(), existingClass.getSimpleName()));
        } else if (clazz.isAssignableFrom(existingClass)) {
            throw new IllegalStateException("Extending another component is not supported: %s extends %s".formatted(existingClass.getSimpleName(), clazz.getSimpleName()));
        }

        return true;
    }

    public void removed(int entityId, ComponentMask componentMask) {
        for (var data : componentMask.getComponents()) {
            data.removeComponent(entityId);
        }
    }

    public void fillVector(BitVector vector, RegularComponentType<?>... components) {
        for (int i = 0, s = components.length; i < s; i++) {
            var componentId = getComponent(components[i]).id();
            vector.set(componentId);
        }
    }

    // public api
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Collection<Component<?>> getComponents() {
        return (Collection) this.byClass.values();
    }

}
