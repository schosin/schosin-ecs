package de.schosin.ecs.storage.defaultimpl;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import de.schosin.ecs.api.Pooled;
import de.schosin.ecs.api.components.ComponentType;
import de.schosin.ecs.api.components.ComponentType.ClassType;
import de.schosin.ecs.api.components.ComponentType.ComponentRelationType;
import de.schosin.ecs.api.components.ComponentType.EntityRelationType;
import de.schosin.ecs.api.components.ComponentType.ExclusiveComponentRelationType;
import de.schosin.ecs.api.components.ComponentType.ExclusiveEntityRelationType;
import de.schosin.ecs.api.components.ComponentType.RegularComponentType;
import de.schosin.ecs.api.components.Relation.Exclusive;
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
import de.schosin.ecs.storage.defaultimpl.components.ComponentDataImpl;
import de.schosin.ecs.storage.defaultimpl.components.ComponentRelationDataImpl;
import de.schosin.ecs.storage.defaultimpl.components.EntityRelationDataImpl;
import de.schosin.ecs.storage.defaultimpl.components.ExclusiveComponentRelationDataImpl;
import de.schosin.ecs.storage.defaultimpl.components.ExclusiveEntityRelationDataImpl;
import de.schosin.ecs.storage.defaultimpl.components.PooledComponentDataImpl;
import de.schosin.ecs.utils.ComponentUtils;
import de.schosin.ecs.utils.ReflectionUtils;
import de.schosin.ecs.utils.collections.Bag;
import de.schosin.ecs.utils.collections.ImmutableBag;
import de.schosin.ecs.utils.collections.Pool;

public class ComponentStorageImpl implements ComponentStorage {

    private final StorageWorld world;

    private final Bag<Component<?, ?>> components = new Bag<>(Component.class, 64);

    private final Bag<Component<?, ?>> sortedComponents = new Bag<>(Component.class, 64);
    private final ImmutableBag<Component<?, ?>> immutableComponents = ImmutableBag.create(sortedComponents);

    private final Map<RegularComponentType<?, ?>, Component<?, ?>> componentData = new ConcurrentHashMap<>();

    private final Map<ComponentType<?, ?>, Bag<Component<?, ?>>> bounds = new ConcurrentHashMap<>();
    private final Map<ComponentType<?, ?>, ImmutableBag<Component<?, ?>>> immutableBounds = new ConcurrentHashMap<>();

    private final AtomicInteger nextComponentId = new AtomicInteger(0);

    public ComponentStorageImpl(StorageWorld world) {
        this.world = world;
    }

    @Override
    public Component<?, ?> getComponent(int componentId) {
        return components.get(componentId);
    }

    @Override
    public <T> ClassComponent<T> getComponent(ClassType<T> type, Consumer<RegularComponentType<?, ?>> validate) {
        return getComponentData(type, validate);
    }

    @Override
    public <T extends Pooled> PooledComponentData<T> getPooledComponent(ClassType<T> type, Consumer<RegularComponentType<?, ?>> validate) {
        return getPooledComponentData(type, validate);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private <T> ClassComponent<T> getComponentData(ClassType<T> classType, Consumer<RegularComponentType<?, ?>> validate) {
        if (Pooled.class.isAssignableFrom(classType.clazz())) {
            return getPooledComponent((ClassType) classType, validate);
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

            validate.accept(classType);

            var component = createComponentData(classType);

            this.components.set(component.id(), component);
            this.sortedComponents.add(component);
            this.componentData.put(classType, component);

            handleNewComponent(classType, component);

            return component;
        }
    }

    private <T> ComponentData<T> createComponentData(ClassType<T> classType) {
        var data = world.createEntityBag(classType.clazz());

        return new ComponentDataImpl<>(nextComponentId.getAndIncrement(), classType, data);
    }

    @SuppressWarnings("unchecked")
    private <T extends Pooled> PooledComponentData<T> getPooledComponentData(ClassType<T> classType, Consumer<RegularComponentType<?, ?>> validate) {
        var result = (PooledComponentData<T>) componentData.get(classType);
        if (result != null) {
            return result;
        }

        synchronized (componentData) {
            result = (PooledComponentData<T>) componentData.get(classType);
            if (result != null) {
                return result;
            }

            validate.accept(classType);

            var component = createPooledComponentData(classType);

            this.components.set(component.id(), component);
            this.sortedComponents.add(component);
            this.componentData.put(classType, component);

            handleNewComponent(classType, component);

            return component;
        }
    }

    private <T extends Pooled> PooledComponentData<T> createPooledComponentData(ClassType<T> classType) {
        var data = world.createEntityBag(classType.clazz());
        var pool = Pool.unbounded(classType.clazz(), () -> ReflectionUtils.createComponentInstance(classType.clazz()));

        return new PooledComponentDataImpl<>(nextComponentId.getAndIncrement(), classType, data, pool);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R, T> ComponentRelationData<R, T> getComponent(ComponentRelationType<R, T> relationType, Consumer<RegularComponentType<?, ?>> validate) {
        var result = (ComponentRelationData<R, T>) componentData.get(relationType);
        if (result != null) {
            return result;
        }

        synchronized (componentData) {
            result = (ComponentRelationData<R, T>) componentData.get(relationType);
            if (result != null) {
                return result;
            }

            validate.accept(relationType);

            var component = createComponentRelationData(relationType);

            this.components.set(component.id(), component);
            this.sortedComponents.add(component);
            this.componentData.put(relationType, component);

            handleNewComponent(relationType, component);

            return component;
        }
    }

    private <R, T> ComponentRelationData<R, T> createComponentRelationData(ComponentRelationType<R, T> relationType) {
        return new ComponentRelationDataImpl<>(nextComponentId.getAndIncrement(), relationType, world);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R extends Exclusive, T> ExclusiveComponentRelationData<R, T> getComponent(ExclusiveComponentRelationType<R, T> relationType, Consumer<RegularComponentType<?, ?>> validate) {
        var result = (ExclusiveComponentRelationData<R, T>) componentData.get(relationType);
        if (result != null) {
            return result;
        }

        synchronized (componentData) {
            result = (ExclusiveComponentRelationData<R, T>) componentData.get(relationType);
            if (result != null) {
                return result;
            }

            validate.accept(relationType);

            var component = createExclusiveComponentRelationData(relationType);

            this.components.set(component.id(), component);
            this.sortedComponents.add(component);
            this.componentData.put(relationType, component);

            handleNewComponent(relationType, component);

            return component;
        }
    }

    private <R extends Exclusive, T> ExclusiveComponentRelationData<R, T> createExclusiveComponentRelationData(ExclusiveComponentRelationType<R, T> relationType) {
        return new ExclusiveComponentRelationDataImpl<>(nextComponentId.getAndIncrement(), relationType, world);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <R> EntityRelationData<R> getComponent(EntityRelationType<R> relationType, Consumer<RegularComponentType<?, ?>> validate) {
        var result = (EntityRelationData<R>) componentData.get(relationType);
        if (result != null) {
            return result;
        }

        synchronized (componentData) {
            result = (EntityRelationData<R>) componentData.get(relationType);
            if (result != null) {
                return result;
            }

            validate.accept(relationType);

            var component = createEntityRelationData(relationType);

            this.components.set(component.id(), component);
            this.sortedComponents.add(component);
            this.componentData.put(relationType, component);

            handleNewComponent(relationType, component);

            return component;
        }
    }

    private <R> EntityRelationData<R> createEntityRelationData(EntityRelationType<R> relationType) {
        return new EntityRelationDataImpl<>(nextComponentId.getAndIncrement(), relationType, world);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R extends Exclusive> ExclusiveEntityRelationData<R> getComponent(ExclusiveEntityRelationType<R> relationType, Consumer<RegularComponentType<?, ?>> validate) {
        var result = (ExclusiveEntityRelationData<R>) componentData.get(relationType);
        if (result != null) {
            return result;
        }

        synchronized (componentData) {
            result = (ExclusiveEntityRelationData<R>) componentData.get(relationType);
            if (result != null) {
                return result;
            }

            validate.accept(relationType);

            var component = createExclusiveEntityRelationData(relationType);

            this.components.set(component.id(), component);
            this.sortedComponents.add(component);
            this.componentData.put(relationType, component);

            handleNewComponent(relationType, component);

            return component;
        }
    }

    private <R extends Exclusive> ExclusiveEntityRelationData<R> createExclusiveEntityRelationData(ExclusiveEntityRelationType<R> relationType) {
        return new ExclusiveEntityRelationDataImpl<>(nextComponentId.getAndIncrement(), relationType, world);
    }

    private <T, R> void handleNewComponent(RegularComponentType<T, R> type, Component<T, R> component) {
        // Update bounds
        for (var entry : this.bounds.entrySet()) {
            if (ComponentUtils.matches(entry.getKey(), type)) {
                entry.getValue().add(component);
            }
        }

        // Dispatch event
        world.dispatchComponentAddedEvent(type, component);
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

                if (ComponentUtils.matches(bound, component.type())) {
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

}
