package de.schosin.ecs.engine.components;

import java.util.function.Consumer;

import org.jspecify.annotations.NonNull;

import de.schosin.ecs.api.Pooled;
import de.schosin.ecs.api.components.Relation.ComponentRelation;
import de.schosin.ecs.api.components.Relation.EntityRelation;
import de.schosin.ecs.api.components.Relation.Exclusive;
import de.schosin.ecs.api.components.types.ComponentType;
import de.schosin.ecs.api.components.types.ComponentType.ClassType;
import de.schosin.ecs.api.components.types.ComponentType.ComponentRelationType;
import de.schosin.ecs.api.components.types.ComponentType.EntityRelationType;
import de.schosin.ecs.api.components.types.ComponentType.ExclusiveComponentRelationType;
import de.schosin.ecs.api.components.types.ComponentType.ExclusiveEntityRelationType;
import de.schosin.ecs.api.components.types.ComponentType.RegularComponentRelationType;
import de.schosin.ecs.api.components.types.ComponentType.RegularComponentType;
import de.schosin.ecs.api.components.types.ComponentType.RegularEntityRelationType;
import de.schosin.ecs.engine.EngineWorld.Classes;
import de.schosin.ecs.engine.events.EventManager;
import de.schosin.ecs.engine.events.builtin.ComponentAddedEvent;
import de.schosin.ecs.engine.utils.ClassUtils;
import de.schosin.ecs.engine.utils.exceptions.UnsupportedComponentTypeException;
import de.schosin.ecs.storage.api.StorageEngine;
import de.schosin.ecs.storage.api.components.Component;
import de.schosin.ecs.storage.api.components.Component.ClassComponent;
import de.schosin.ecs.storage.api.components.Component.ComponentRelationComponent;
import de.schosin.ecs.storage.api.components.Component.ComponentRelationData;
import de.schosin.ecs.storage.api.components.Component.EntityRelationComponent;
import de.schosin.ecs.storage.api.components.Component.EntityRelationData;
import de.schosin.ecs.storage.api.components.Component.ExclusiveComponentRelationData;
import de.schosin.ecs.storage.api.components.Component.ExclusiveEntityRelationData;
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

    private final Consumer<RegularComponentType<?, ?>> validate;

    public ComponentManager(StorageEngine storageEngine, EventManager eventManager, Classes classes) {
        this.storageEngine = storageEngine;
        this.eventManager = eventManager;

        this.validate = type -> ComponentManager.validateComponent(type, classes);
    }

    public Component<?, ?> getComponent(int componentId) {
        return storageEngine.getComponent(componentId);
    }

    public <T, R> Component<T, R> getComponent(RegularComponentType<T, R> type) {
        return storageEngine.getComponent(type, this.validate);
    }

    public <T> ClassComponent<T> getComponent(ClassType<T> type) {
        return storageEngine.getComponent(type, this.validate);
    }

    public <T extends Pooled> PooledComponentData<T> getPooledComponent(ClassType<T> type) {
        return storageEngine.getPooledComponent(type, this.validate);
    }

    public <R, T, X> ComponentRelationComponent<R, T, X> getComponent(RegularComponentRelationType<R, T, X> type) {
        return storageEngine.getComponent(type, this.validate);
    }

    public <R, T> ComponentRelationData<R, T> getComponent(ComponentRelationType<R, T> type) {
        return storageEngine.getComponent(type, this.validate);
    }

    public <R extends Exclusive, T> ExclusiveComponentRelationData<R, T> getComponent(ExclusiveComponentRelationType<R, T> type) {
        return storageEngine.getComponent(type, this.validate);
    }

    public <R, X> EntityRelationComponent<R, X> getComponent(RegularEntityRelationType<R, X> type) {
        return storageEngine.getComponent(type, this.validate);
    }

    public <R> EntityRelationData<R> getComponent(EntityRelationType<R> type) {
        return storageEngine.getComponent(type, this.validate);
    }

    public <R extends Exclusive> ExclusiveEntityRelationData<R> getComponent(ExclusiveEntityRelationType<R> type) {
        return storageEngine.getComponent(type, this.validate);
    }

    /**
     * Do no use {@link ComponentType} for accessing components. Use {@link #getComponent(RegularComponentType)} instead.
     * 
     * @throws UnsupportedComponentTypeException operation not supported
     */
    @Deprecated
    public Component<?, ?> getComponent(ComponentType<?, ?> type) throws UnsupportedComponentTypeException {
        throw new UnsupportedComponentTypeException(type, "ComponentType '%s' not allowed, must use RegularComponentType for accessing components.".formatted(type));
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public <T> Component<T, ?> getComponent(@NonNull T component) {
        return switch (component) {
            case null -> throw new IllegalArgumentException("Cannot get component type for null instance");
            case ComponentRelation<?, ?> relation -> Exclusive.class.isAssignableFrom(relation.relationship().getClass())
                    ? getComponent(ComponentType.exclusiveRelation((Class) relation.relationship().getClass(), relation.target().getClass()))
                    : getComponent(ComponentType.relation((Class) relation.relationship().getClass(), relation.target().getClass()));
            case EntityRelation<?> relation -> Exclusive.class.isAssignableFrom(relation.relationship().getClass())
                    ? getComponent(ComponentType.exclusiveRelation((Class) relation.relationship().getClass()))
                    : getComponent(ComponentType.relation((Class) relation.relationship().getClass()));
            default -> getComponent(ComponentType.component((Class<T>) component.getClass()));
        };
    }

    public void removed(int entityId, ComponentMask componentMask) {
        for (var data : componentMask.getComponents()) {
            data.removeComponent(entityId);
        }
    }

    public void fillVector(BitVector vector, RegularComponentType<?, ?>... components) {
        for (int i = 0, s = components.length; i < s; i++) {
            var componentId = getComponent(components[i]).id();
            vector.set(componentId);
        }
    }

    public ImmutableBag<Component<?, ?>> getComponents() {
        return storageEngine.getComponents();
    }

    public <T> ImmutableBag<Component<? extends T, ?>> getComponents(ComponentType<T, ?> bound) {
        return storageEngine.getComponents(bound);
    }

    private static boolean validateComponent(RegularComponentType<?, ?> type, Classes classes) {
        return switch (type) {
            case ComponentType.ClassType<?> classType -> validateComponentClass(classType.clazz(), classes);
            case ComponentRelationType<?, ?> relation -> validateComponentClass(relation.relationship(), classes) && validateComponentClass(relation.target(), classes);
            case ExclusiveComponentRelationType<?, ?> relation -> validateComponentClass(relation.relationship(), classes) && validateComponentClass(relation.target(), classes);
            case EntityRelationType<?> relation -> validateComponentClass(relation.relationship(), classes);
            case ExclusiveEntityRelationType<?> relation -> validateComponentClass(relation.relationship(), classes);
        };
    }

    private static boolean validateComponentClass(Class<?> clazz, Classes classes) {
        // Validate invalid types
        var classType = ClassUtils.detectType(clazz);
        if (!classType.isValidComponent()) {
            throw new IllegalArgumentException("Invalid component '%s' of type '%s'. Allowed types: %s"
                    .formatted(clazz.getSimpleName(), classType.name().toLowerCase(), ClassUtils.ClassType.ALLOWED_COMPONENT_TYPES));
        }

        // Validate not a state
        synchronized (classes) {
            if (classes.states().contains(clazz)) {
                throw new IllegalArgumentException("Class %s is already used as a state.".formatted(clazz.getName()));
            }

            classes.components().add(clazz);
        }

        return true;
    }

    public <T, R> void dispatchComponentAddedEvent(RegularComponentType<T, R> type, Component<T, R> component) {
        eventManager.dispatchEvent(ComponentAddedEvent.get(type, component));
    }

}
