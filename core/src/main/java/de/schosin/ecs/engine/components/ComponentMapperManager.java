package de.schosin.ecs.engine.components;

import java.util.IdentityHashMap;
import java.util.Map;

import org.jspecify.annotations.NonNull;

import de.schosin.ecs.api.Pooled;
import de.schosin.ecs.api.components.ComponentType;
import de.schosin.ecs.api.components.Components;
import de.schosin.ecs.api.components.Components.EnumComponents;
import de.schosin.ecs.api.components.Components.PooledComponents;
import de.schosin.ecs.engine.BagManager;
import de.schosin.ecs.storage.api.components.Component;
import de.schosin.ecs.storage.api.components.Component.ComponentData;
import de.schosin.ecs.storage.api.components.Component.PooledComponentData;
import de.schosin.ecs.utils.collections.Bag;

public class ComponentMapperManager implements Components.Creator {

    private final ComponentManager componentManager;
    private final TransmutationManager transmutationManager;

    @SuppressWarnings("rawtypes")
    private final Bag<ComponentMapper> components;
    private final Map<Enum<?>, EnumComponentMapper<?>> enumComponents = new IdentityHashMap<>();

    public ComponentMapperManager(BagManager bagManager, ComponentManager componentManager, TransmutationManager transmutationManager) {
        this.componentManager = componentManager;
        this.transmutationManager = transmutationManager;

        this.components = bagManager.createComponentBag(ComponentMapper.class);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public <T> Components<T> getComponents(Class<T> clazz) {
        var metadata = componentManager.getComponent(ComponentType.component(clazz));

        var result = this.components.get(metadata.id());
        if (result != null) {
            return result;
        }

        synchronized (this.components) {
            result = this.components.get(metadata.id());
            if (result != null) {
                return result;
            }

            var mapper = switch (metadata) {
                case Component.PooledComponentData<?> pooled -> (ComponentMapper<T>) new PooledComponentMapper(pooled);
                case Component.ComponentData<T> data -> new ComponentMapper<>(data);
            };

            this.components.set(metadata.id(), mapper);

            return mapper;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Enum<T>> @NonNull EnumComponents<T> getEnumComponents(@NonNull T defaultComponent) {
        var result = (EnumComponents<T>) enumComponents.get(defaultComponent);
        if (result != null) {
            return result;
        }

        synchronized (this.components) {
            result = (EnumComponents<T>) enumComponents.get(defaultComponent);
            if (result != null) {
                return result;
            }

            var delegate = (ComponentMapper<T>) getComponents(defaultComponent.getClass());

            var mapper = new EnumComponentMapper<>(delegate, defaultComponent);
            enumComponents.put(defaultComponent, mapper);

            return mapper;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Pooled> PooledComponents<T> getPooledComponents(Class<T> clazz) {
        var metadata = componentManager.getPooledComponent(ComponentType.component(clazz));

        var result = (PooledComponents<T>) this.components.get(metadata.id());
        if (result != null) {
            return result;
        }

        synchronized (this.components) {
            result = (PooledComponents<T>) this.components.get(metadata.id());
            if (result != null) {
                return result;
            }

            var mapper = new PooledComponentMapper<>(metadata);
            this.components.set(metadata.id(), mapper);

            return mapper;
        }
    }

    private class ComponentMapper<T> implements Components<T> {

        protected final ComponentData<T> data;

        private final TransmutationManager.Add<T> add;
        private final TransmutationManager.Remove remove;

        protected ComponentMapper(ComponentData<T> data) {
            this.data = data;

            this.add = transmutationManager.getAddTransmuter(data.type());
            this.remove = transmutationManager.getRemoveTransmuter(data.type());
        }

        @Override
        public boolean has(int entityId) {
            return data.hasComponent(entityId);
        }

        @Override
        public T add(int entityId, T component) {
            this.add.apply(entityId, component);

            return component;
        }

        @Override
        public T get(int entityId) {
            return data.getComponent(entityId);
        }

        @Override
        public boolean remove(int entityId) {
            return this.remove.apply(entityId);
        }

    }

    private class EnumComponentMapper<T extends Enum<T>> implements EnumComponents<T> {

        private final ComponentMapper<T> delegate;
        private final T defaultComponent;

        public EnumComponentMapper(ComponentMapper<T> delegate, T defaultComponent) {
            this.delegate = delegate;
            this.defaultComponent = defaultComponent;
        }

        @Override
        public @NonNull T add(int entityId) {
            return add(entityId, defaultComponent);
        }

        @Override
        public @NonNull T getDefault() {
            return defaultComponent;
        }

        @Override
        public boolean has(int entityId) {
            return this.delegate.has(entityId);
        }

        @Override
        public T add(int entityId, T component) {
            return this.delegate.add(entityId, component);
        }

        @Override
        public T get(int entityId) {
            return this.delegate.get(entityId);
        }

        @Override
        public boolean remove(int entityId) {
            return this.delegate.remove(entityId);
        }

    }

    private class PooledComponentMapper<T extends Pooled> extends ComponentMapper<T> implements PooledComponents<T> {

        private final PooledComponentData<T> data;

        public PooledComponentMapper(PooledComponentData<T> data) {
            super(data);

            this.data = data;
        }

        @Override
        public @NonNull T add(int entityId) {
            var component = get(entityId);
            if (component != null) {
                return component;
            }

            return add(entityId, getInstance());
        }

        @Override
        public T getInstance() {
            return data.getInstance();
        }

    }

}
