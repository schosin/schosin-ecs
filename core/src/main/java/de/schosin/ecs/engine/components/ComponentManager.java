package de.schosin.ecs.engine.components;

import java.util.function.Consumer;

import de.schosin.ecs.api.Pooled;
import de.schosin.ecs.api.components.ComponentType;
import de.schosin.ecs.api.components.ComponentType.ClassType;
import de.schosin.ecs.api.components.ComponentType.RegularComponentType;
import de.schosin.ecs.engine.EngineWorld.Classes;
import de.schosin.ecs.engine.events.EventManager;
import de.schosin.ecs.engine.events.builtin.ComponentAddedEvent.RegularComponentAddedEvent.ClassComponentAddedEvent;
import de.schosin.ecs.engine.utils.ClassUtils;
import de.schosin.ecs.engine.utils.exceptions.UnsupportedComponentTypeException;
import de.schosin.ecs.storage.api.StorageEngine;
import de.schosin.ecs.storage.api.components.Component;
import de.schosin.ecs.storage.api.components.Component.PooledComponentData;
import de.schosin.ecs.utils.collections.BitVector;
import de.schosin.ecs.utils.collections.ImmutableBag;

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

    private final StorageEngine storageEngine;
    private final EventManager eventManager;

    private final Consumer<RegularComponentType<?>> validate;

    public ComponentManager(StorageEngine storageEngine, EventManager eventManager, Classes classes) {
        this.storageEngine = storageEngine;
        this.eventManager = eventManager;

        this.validate = type -> ComponentManager.validateComponent(type, classes);
    }

    public Component<?> getComponent(int componentId) {
        return storageEngine.getComponent(componentId);
    }

    public <T> Component<T> getComponent(RegularComponentType<T> type) {
        return storageEngine.getComponent(type, this.validate);
    }

    public <T extends Pooled> PooledComponentData<T> getPooledComponent(RegularComponentType<T> type) {
        return storageEngine.getPooledComponent(type, this.validate);
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

    public ImmutableBag<Component<?>> getComponents() {
        return storageEngine.getComponents();
    }

    public <T> ImmutableBag<Component<? extends T>> getComponents(ComponentType<T> bound) {
        return storageEngine.getComponents(bound);
    }

    private static boolean validateComponent(RegularComponentType<?> type, Classes classes) {
        return switch (type) {
            case ComponentType.ClassType<?> classType -> validateComponent(classType, classes);
        };
    }

    private static boolean validateComponent(ClassType<?> type, Classes classes) {
        // Validate invalid types
        var classType = ClassUtils.detectType(type.clazz());
        if (!classType.isValidComponent()) {
            throw new IllegalArgumentException("Invalid component '%s' of type '%s'. Allowed types: %s"
                    .formatted(type.clazz().getSimpleName(), classType.name().toLowerCase(), ClassUtils.ClassType.ALLOWED_COMPONENT_TYPES));
        }

        // Validate not a state
        synchronized (classes) {
            if (classes.states().contains(type.clazz())) {
                throw new IllegalArgumentException("Class %s is already used as a state.".formatted(type.clazz().getName()));
            }

            classes.components().add(type.clazz());
        }

        return true;
    }

    public <T> void dispatchComponentAddedEvent(RegularComponentType<T> type, Component<T> component) {
        var event = switch (type) {
            case ComponentType.ClassType<T> clazzType -> ClassComponentAddedEvent.get(clazzType, component);
        };

        eventManager.dispatchEvent(event);
    }

}
