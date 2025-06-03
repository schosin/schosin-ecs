package de.schosin.ecs.engine.components;

import org.jspecify.annotations.NonNull;

import de.schosin.ecs.api.Pooled;
import de.schosin.ecs.api.components.Relation.Exclusive;
import de.schosin.ecs.api.components.types.ClassType;
import de.schosin.ecs.api.components.types.ComponentType;
import de.schosin.ecs.api.components.types.ComponentType.RegularComponentType;
import de.schosin.ecs.api.components.types.RelationComponentType.ComponentRelationType;
import de.schosin.ecs.api.components.types.RelationComponentType.EntityRelationType;
import de.schosin.ecs.api.components.types.RelationComponentType.ExclusiveComponentRelationType;
import de.schosin.ecs.api.components.types.RelationComponentType.ExclusiveEntityRelationType;
import de.schosin.ecs.api.components.types.RelationComponentType.RegularComponentRelationType;
import de.schosin.ecs.api.components.types.RelationComponentType.RegularEntityRelationType;
import de.schosin.ecs.engine.EngineWorld.Classes;
import de.schosin.ecs.engine.events.EventManager;
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
import de.schosin.ecs.storage.api.events.ComponentAddedEvent;
import de.schosin.ecs.utils.collections.BitVector;
import de.schosin.ecs.utils.collections.ImmutableBag;

/**
 * Manages {@link Component component data} for every component class
 * encountered. 
 * 
 * <p>
 * Each new component class is assigned an {@link Component#id}
 * that is used in several places, including as an index into a Bags.
 * </p>
 */
public class ComponentManager {

    private final StorageEngine storageEngine;
    private final Classes classes;

    public ComponentManager(StorageEngine storageEngine, EventManager eventManager, Classes classes) {
        this.storageEngine = storageEngine;
        this.classes = classes;

        eventManager.registerEventHandler(ComponentAddedEvent.class, this::handleComponentAdded);
    }

    public Component<?, ?> getComponent(int componentId) {
        return storageEngine.getComponent(componentId);
    }

    public <T, R> Component<T, R> getComponent(RegularComponentType<T, R> type) {
        return storageEngine.getComponent(type);
    }

    public <T> ClassComponent<T> getComponent(ClassType<T> type) {
        return storageEngine.getComponent(type);
    }

    public <T extends Pooled> PooledComponentData<T> getPooledComponent(ClassType<T> type) {
        return storageEngine.getPooledComponent(type);
    }

    public <R, T, X> ComponentRelationComponent<R, T, X> getComponent(RegularComponentRelationType<R, T, X> type) {
        return storageEngine.getComponent(type);
    }

    public <R, T> ComponentRelationData<R, T> getComponent(ComponentRelationType<R, T> type) {
        return storageEngine.getComponent(type);
    }

    public <R extends Exclusive, T> ExclusiveComponentRelationData<R, T> getComponent(ExclusiveComponentRelationType<R, T> type) {
        return storageEngine.getComponent(type);
    }

    public <R, X> EntityRelationComponent<R, X> getComponent(RegularEntityRelationType<R, X> type) {
        return storageEngine.getComponent(type);
    }

    public <R> EntityRelationData<R> getComponent(EntityRelationType<R> type) {
        return storageEngine.getComponent(type);
    }

    public <R extends Exclusive> ExclusiveEntityRelationData<R> getComponent(ExclusiveEntityRelationType<R> type) {
        return storageEngine.getComponent(type);
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

    public <T> Component<T, ?> getComponent(@NonNull T component) {
        return getComponent(ComponentType.detectComponentType(component));
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
            case ClassType<?> classType -> validateComponentClass(classType.clazz(), classes);
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

    private void handleComponentAdded(ComponentAddedEvent event) {
        validateComponent(event.type(), classes);
    }

}
