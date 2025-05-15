package de.schosin.ecs.storage.defaultimpl;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import de.schosin.ecs.api.Pooled;
import de.schosin.ecs.api.components.ComponentType;
import de.schosin.ecs.api.components.ComponentType.ClassType;
import de.schosin.ecs.api.components.ComponentType.RegularComponentType;
import de.schosin.ecs.storage.api.ComponentStorage;
import de.schosin.ecs.storage.api.StorageWorld;
import de.schosin.ecs.storage.api.components.Component;
import de.schosin.ecs.storage.api.components.Component.ComponentData;
import de.schosin.ecs.storage.api.components.Component.PooledComponentData;
import de.schosin.ecs.storage.defaultimpl.components.ComponentDataImpl;
import de.schosin.ecs.storage.defaultimpl.components.PooledComponentDataImpl;
import de.schosin.ecs.utils.ReflectionUtils;
import de.schosin.ecs.utils.collections.Bag;
import de.schosin.ecs.utils.collections.Pool;

public class ComponentStorageImpl implements ComponentStorage {

    private final StorageWorld world;

    private final Bag<Component<?>> components = new Bag<>(Component.class, 64);
    private final Map<Class<?>, ComponentData<?>> componentData = new ConcurrentHashMap<>();

    private final AtomicInteger nextComponentId = new AtomicInteger(0);

    public ComponentStorageImpl(StorageWorld world) {
        this.world = world;
    }

    @Override
    public Component<?> getComponent(int componentId) {
        return components.get(componentId);
    }

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public <T> Component<T> getComponent(RegularComponentType<T> type, Consumer<RegularComponentType<?>> validate) {
        return switch (type) {
            case ComponentType.ClassType<T> classType -> Pooled.class.isAssignableFrom(classType.clazz())
                    ? getPooledComponentData((ClassType) classType, validate)
                    : getComponentData(classType, validate);
        };
    }

    @Override
    public <T extends Pooled> PooledComponentData<T> getPooledComponent(RegularComponentType<T> type, Consumer<RegularComponentType<?>> validate) {
        return switch (type) {
            case ComponentType.ClassType<T> classType -> getPooledComponentData(classType, validate);
        };
    }

    @SuppressWarnings("unchecked")
    private <T> ComponentData<T> getComponentData(ClassType<T> classType, Consumer<RegularComponentType<?>> validate) {
        var result = (ComponentData<T>) componentData.get(classType.clazz());
        if (result != null) {
            return result;
        }

        synchronized (componentData) {
            result = (ComponentData<T>) componentData.get(classType.clazz());
            if (result != null) {
                return result;
            }

            validate.accept(classType);

            var component = createComponentData(classType);
            this.componentData.put(classType.clazz(), component);

            world.dispatchComponentAddedEvent(classType, component);

            return component;
        }
    }

    private <T> ComponentData<T> createComponentData(ClassType<T> classType) {
        var data = world.createEntityBag(classType.clazz());

        var metadata = new ComponentDataImpl<>(nextComponentId.getAndIncrement(), classType, data);
        this.components.set(metadata.id(), metadata);

        return metadata;
    }

    @SuppressWarnings("unchecked")
    private <T extends Pooled> PooledComponentData<T> getPooledComponentData(ClassType<T> classType, Consumer<RegularComponentType<?>> validate) {
        var result = (PooledComponentData<T>) componentData.get(classType.clazz());
        if (result != null) {
            return result;
        }

        synchronized (componentData) {
            result = (PooledComponentData<T>) componentData.get(classType.clazz());
            if (result != null) {
                return result;
            }

            validate.accept(classType);

            var component = createPooledComponentData(classType);
            this.componentData.put(classType.clazz(), component);

            world.dispatchComponentAddedEvent(classType, component);

            return component;
        }
    }

    private <T extends Pooled> PooledComponentData<T> createPooledComponentData(ClassType<T> classType) {
        var data = world.createEntityBag(classType.clazz());
        var pool = Pool.unbounded(classType.clazz(), () -> ReflectionUtils.createComponentInstance(classType.clazz()));

        var metadata = new PooledComponentDataImpl<>(nextComponentId.getAndIncrement(), classType, data, pool);
        this.components.set(metadata.id(), metadata);

        return metadata;
    }

    @Override
    public Collection<Component<?>> getComponents() {
        return Collections.unmodifiableCollection(componentData.values());
    }

}
