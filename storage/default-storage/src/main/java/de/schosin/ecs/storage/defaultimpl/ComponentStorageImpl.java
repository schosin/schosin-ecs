package de.schosin.ecs.storage.defaultimpl;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import de.schosin.ecs.api.Pooled;
import de.schosin.ecs.api.components.Relation.Exclusive;
import de.schosin.ecs.api.components.types.ClassType;
import de.schosin.ecs.api.components.types.ComponentType;
import de.schosin.ecs.api.components.types.ComponentType.RegularComponentType;
import de.schosin.ecs.api.components.types.RelationComponentType.ComponentRelationType;
import de.schosin.ecs.api.components.types.RelationComponentType.EntityRelationType;
import de.schosin.ecs.api.components.types.RelationComponentType.ExclusiveComponentRelationType;
import de.schosin.ecs.api.components.types.RelationComponentType.ExclusiveEntityRelationType;
import de.schosin.ecs.storage.api.ComponentStorage;
import de.schosin.ecs.storage.api.StorageWorld;
import de.schosin.ecs.storage.api.components.Component;
import de.schosin.ecs.storage.api.components.Component.ClassComponent;
import de.schosin.ecs.storage.api.components.Component.ComponentData;
import de.schosin.ecs.storage.api.components.Component.ComponentRelationData;
import de.schosin.ecs.storage.api.components.Component.EntityRelationData;
import de.schosin.ecs.storage.api.components.Component.ExclusiveComponentRelationData;
import de.schosin.ecs.storage.api.components.Component.ExclusiveEntityRelationData;
import de.schosin.ecs.storage.api.components.Component.PooledComponentData;
import de.schosin.ecs.storage.api.events.ComponentAddedEvent;
import de.schosin.ecs.storage.common.PendingChanges;
import de.schosin.ecs.storage.defaultimpl.components.ComponentDataImpl;
import de.schosin.ecs.storage.defaultimpl.components.ComponentRelationDataImpl;
import de.schosin.ecs.storage.defaultimpl.components.EntityRelationDataImpl;
import de.schosin.ecs.storage.defaultimpl.components.ExclusiveComponentRelationDataImpl;
import de.schosin.ecs.storage.defaultimpl.components.ExclusiveEntityRelationDataImpl;
import de.schosin.ecs.storage.defaultimpl.components.PooledComponentDataImpl;
import de.schosin.ecs.utils.ReflectionUtils;
import de.schosin.ecs.utils.collections.Bag;
import de.schosin.ecs.utils.collections.ImmutableBag;
import de.schosin.ecs.utils.collections.Pool;

public class ComponentStorageImpl implements ComponentStorage {

    private final StorageWorld world;
    private final Bag<PendingChanges> pendingChanges;

    private final Bag<Component<?, ?>> components = new Bag<>(Component.class, 64);

    private final Bag<Component<?, ?>> sortedComponents = new Bag<>(Component.class, 64);
    private final ImmutableBag<Component<?, ?>> immutableComponents = ImmutableBag.create(sortedComponents);

    private final Map<RegularComponentType<?, ?>, Component<?, ?>> componentData = new ConcurrentHashMap<>();

    private final Map<ComponentType<?, ?>, Bag<Component<?, ?>>> bounds = new ConcurrentHashMap<>();
    private final Map<ComponentType<?, ?>, ImmutableBag<Component<?, ?>>> immutableBounds = new ConcurrentHashMap<>();

    private final AtomicInteger nextComponentId = new AtomicInteger(0);

    public ComponentStorageImpl(StorageWorld world, Bag<PendingChanges> pendingChanges) {
        this.world = world;
        this.pendingChanges = pendingChanges;
    }

    @Override
    public Component<?, ?> getComponent(int componentId) {
        return components.get(componentId);
    }

    @Override
    public <T> ClassComponent<T> getComponent(ClassType<T> type) {
        return getComponentData(type);
    }

    @Override
    public <T extends Pooled> PooledComponentData<T> getPooledComponent(ClassType<T> type) {
        return getPooledComponentData(type);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private <T> ClassComponent<T> getComponentData(ClassType<T> classType) {
        if (Pooled.class.isAssignableFrom(classType.clazz())) {
            return getPooledComponent((ClassType) classType);
        }

        var result = (ComponentData<T>) componentData.get(classType);
        if (result != null) {
            return result;
        }

        synchronized (componentData) {
            result = (ComponentData<T>) componentData.get(classType);
            if (result != null) {
                return result;
            }

            var component = createComponentData(classType);

            this.components.set(component.id(), component);
            this.sortedComponents.add(component);
            this.componentData.put(classType, component);

            handleNewComponent(component.id(), classType, component);

            return component;
        }
    }

    private <T> ComponentData<T> createComponentData(ClassType<T> classType) {
        var data = world.createEntityBag(classType.clazz());

        return new ComponentDataImpl<>(nextComponentId.getAndIncrement(), classType, data, pendingChanges);
    }

    @SuppressWarnings("unchecked")
    private <T extends Pooled> PooledComponentData<T> getPooledComponentData(ClassType<T> classType) {
        var result = (PooledComponentData<T>) componentData.get(classType);
        if (result != null) {
            return result;
        }

        synchronized (componentData) {
            result = (PooledComponentData<T>) componentData.get(classType);
            if (result != null) {
                return result;
            }

            var component = createPooledComponentData(classType);

            this.components.set(component.id(), component);
            this.sortedComponents.add(component);
            this.componentData.put(classType, component);

            handleNewComponent(component.id(), classType, component);

            return component;
        }
    }

    private <T extends Pooled> PooledComponentData<T> createPooledComponentData(ClassType<T> classType) {
        var data = world.createEntityBag(classType.clazz());
        var pool = Pool.unbounded(classType.clazz(), () -> ReflectionUtils.createComponentInstance(classType.clazz()));

        return new PooledComponentDataImpl<>(nextComponentId.getAndIncrement(), classType, data, pendingChanges, pool);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R, T> ComponentRelationData<R, T> getComponent(ComponentRelationType<R, T> relationType) {
        var result = (ComponentRelationData<R, T>) componentData.get(relationType);
        if (result != null) {
            return result;
        }

        synchronized (componentData) {
            result = (ComponentRelationData<R, T>) componentData.get(relationType);
            if (result != null) {
                return result;
            }

            var component = createComponentRelationData(relationType);

            this.components.set(component.id(), component);
            this.sortedComponents.add(component);
            this.componentData.put(relationType, component);

            handleNewComponent(component.id(), relationType, component);

            return component;
        }
    }

    private <R, T> ComponentRelationData<R, T> createComponentRelationData(ComponentRelationType<R, T> relationType) {
        return new ComponentRelationDataImpl<>(nextComponentId.getAndIncrement(), relationType, world, pendingChanges);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R extends Exclusive, T> ExclusiveComponentRelationData<R, T> getComponent(ExclusiveComponentRelationType<R, T> relationType) {
        var result = (ExclusiveComponentRelationData<R, T>) componentData.get(relationType);
        if (result != null) {
            return result;
        }

        synchronized (componentData) {
            result = (ExclusiveComponentRelationData<R, T>) componentData.get(relationType);
            if (result != null) {
                return result;
            }

            var component = createExclusiveComponentRelationData(relationType);

            this.components.set(component.id(), component);
            this.sortedComponents.add(component);
            this.componentData.put(relationType, component);

            handleNewComponent(component.id(), relationType, component);

            return component;
        }
    }

    private <R extends Exclusive, T> ExclusiveComponentRelationData<R, T> createExclusiveComponentRelationData(ExclusiveComponentRelationType<R, T> relationType) {
        return new ExclusiveComponentRelationDataImpl<>(nextComponentId.getAndIncrement(), relationType, world, pendingChanges);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <R> EntityRelationData<R> getComponent(EntityRelationType<R> relationType) {
        var result = (EntityRelationData<R>) componentData.get(relationType);
        if (result != null) {
            return result;
        }

        synchronized (componentData) {
            result = (EntityRelationData<R>) componentData.get(relationType);
            if (result != null) {
                return result;
            }

            var component = createEntityRelationData(relationType);

            this.components.set(component.id(), component);
            this.sortedComponents.add(component);
            this.componentData.put(relationType, component);

            handleNewComponent(component.id(), relationType, component);

            return component;
        }
    }

    private <R> EntityRelationData<R> createEntityRelationData(EntityRelationType<R> relationType) {
        return new EntityRelationDataImpl<>(nextComponentId.getAndIncrement(), relationType, world, pendingChanges);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R extends Exclusive> ExclusiveEntityRelationData<R> getComponent(ExclusiveEntityRelationType<R> relationType) {
        var result = (ExclusiveEntityRelationData<R>) componentData.get(relationType);
        if (result != null) {
            return result;
        }

        synchronized (componentData) {
            result = (ExclusiveEntityRelationData<R>) componentData.get(relationType);
            if (result != null) {
                return result;
            }

            var component = createExclusiveEntityRelationData(relationType);

            this.components.set(component.id(), component);
            this.sortedComponents.add(component);
            this.componentData.put(relationType, component);

            handleNewComponent(component.id(), relationType, component);

            return component;
        }
    }

    private <R extends Exclusive> ExclusiveEntityRelationData<R> createExclusiveEntityRelationData(ExclusiveEntityRelationType<R> relationType) {
        return new ExclusiveEntityRelationDataImpl<>(nextComponentId.getAndIncrement(), relationType, world, pendingChanges);
    }

    private <T, R> void handleNewComponent(int componentId, RegularComponentType<T, R> type, Component<T, R> component) {
        // Update bounds
        for (var entry : this.bounds.entrySet()) {
            var componentType = entry.getKey();

            if (componentType.matches(type)) {
                entry.getValue().add(component);
            }
        }

        // Dispatch event
        world.dispatchEvent(ComponentAddedEvent.get(componentId, type, component));
    }

    @Override
    public ImmutableBag<Component<?, ?>> getComponents() {
        return immutableComponents;
    }

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public <T> ImmutableBag<Component<? extends T, ?>> getComponents(ComponentType<T, ?> bound) {
        var result = immutableBounds.get(bound);
        if (result != null) {
            return (ImmutableBag) result;
        }

        synchronized (components) {
            result = immutableBounds.get(bound);
            if (result != null) {
                return (ImmutableBag) result;
            }

            // Create bag
            var bag = new Bag<Component<?, ?>>(Component.class, 32);

            // Fill bag with known components
            var data = this.components.getData();
            for (int i = 0, s = this.components.getSize(); i < s; i++) {
                var component = data[i];

                if (bound.matches(component.type())) {
                    bag.add(component);
                }
            }

            // Add bag and return immutable result
            this.bounds.put(bound, bag);

            var immutableBag = ImmutableBag.create(bag);
            this.immutableBounds.put(bound, immutableBag);

            return (ImmutableBag) immutableBag;
        }
    }

    @Override
    public RegularComponentType<?, ?>[] getRegularComponentTypes(ComponentType<?, ?> bound) {
        if (bound instanceof RegularComponentType<?, ?> regular) {
            return new RegularComponentType<?, ?>[] { regular };
        }

        var components = getComponents(bound);
        if (components.isEmpty()) {
            return new RegularComponentType<?, ?>[0];
        }

        var result = new RegularComponentType<?, ?>[components.getSize()];
        for (int i = 0, s = components.getSize(); i < s; i++) {
            result[i] = components.get(i).type();
        }

        return result;
    }

}
