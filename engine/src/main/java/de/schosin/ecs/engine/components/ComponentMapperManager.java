package de.schosin.ecs.engine.components;

import org.jspecify.annotations.NonNull;

import de.schosin.ecs.api.Pooled;
import de.schosin.ecs.api.archetype.Transmuter;
import de.schosin.ecs.api.components.Components;
import de.schosin.ecs.api.components.Components.PooledComponents;
import de.schosin.ecs.engine.BagManager;
import de.schosin.ecs.engine.utils.collections.Bag;

public class ComponentMapperManager {

    private final ComponentManager componentManager;
    private final TransmutationManager transmutationManager;

    @SuppressWarnings("rawtypes")
    private final Bag<ComponentMapper> components;

    public ComponentMapperManager(BagManager bagManager, ComponentManager componentManager, TransmutationManager transmutationManager) {
        this.componentManager = componentManager;
        this.transmutationManager = transmutationManager;

        this.components = bagManager.createComponentBag(ComponentMapper.class);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public <T> Components<T> getComponents(Class<T> clazz) {
        var metadata = componentManager.getData(clazz);

        var result = this.components.get(metadata.id());
        if (result != null) {
            return result;
        }

        synchronized (this.components) {
            result = this.components.get(metadata.id());
            if (result != null) {
                return result;
            }

            var mapper = Pooled.class.isAssignableFrom(clazz)
                    ? new PooledComponentMapper(metadata)
                    : new ComponentMapper(metadata);

            this.components.set(metadata.id(), mapper);

            return mapper;
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public <T extends Pooled> PooledComponents<T> getPooledComponents(Class<T> clazz) {
        var metadata = componentManager.getData(clazz);

        var result = (PooledComponents<T>) this.components.get(metadata.id());
        if (result != null) {
            return result;
        }

        synchronized (this.components) {
            result = (PooledComponents<T>) this.components.get(metadata.id());
            if (result != null) {
                return result;
            }

            var mapper = new PooledComponentMapper(metadata);

            this.components.set(metadata.id(), mapper);

            return mapper;
        }
    }

    private class ComponentMapper<T> implements Components<T> {

        protected final ComponentData<T> data;

        private final Transmuter.Add1<T> add;
        private final Transmuter.Remove remove;

        protected ComponentMapper(ComponentData<T> data) {
            this.data = data;

            this.add = transmutationManager.createTransmuter(Transmuter.add(data.clazz()));
            this.remove = transmutationManager.createTransmuter(Transmuter.remove(data.clazz()));
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

    private class PooledComponentMapper<T extends Pooled> extends ComponentMapper<T> implements PooledComponents<T> {

        public PooledComponentMapper(ComponentData<T> data) {
            super(data);
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
