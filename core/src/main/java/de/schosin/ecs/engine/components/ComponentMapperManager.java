package de.schosin.ecs.engine.components;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jspecify.annotations.NonNull;

import de.schosin.ecs.api.Pooled;
import de.schosin.ecs.api.components.ComponentType;
import de.schosin.ecs.api.components.ComponentType.ClassType;
import de.schosin.ecs.api.components.ComponentType.RegularComponentType;
import de.schosin.ecs.api.components.ComponentType.Wildcard;
import de.schosin.ecs.api.components.Components;
import de.schosin.ecs.api.components.Components.ComponentMapper;
import de.schosin.ecs.api.components.Components.EnumComponentMapper;
import de.schosin.ecs.api.components.Components.PooledComponentMapper;
import de.schosin.ecs.api.components.Result;
import de.schosin.ecs.engine.BagManager;
import de.schosin.ecs.engine.events.EventManager;
import de.schosin.ecs.engine.events.builtin.ComponentAddedEvent;
import de.schosin.ecs.storage.api.components.Component;
import de.schosin.ecs.storage.api.components.Component.ComponentData;
import de.schosin.ecs.storage.api.components.Component.PooledComponentData;
import de.schosin.ecs.utils.ComponentUtils;
import de.schosin.ecs.utils.collections.Bag;
import de.schosin.ecs.utils.collections.Pool;

public class ComponentMapperManager implements Components.Creator {

    public interface ResultComponents<T> {
        void free(Result<T> result);
    }

    private final BagManager bagManager;
    private final ComponentManager componentManager;
    private final TransmutationManager transmutationManager;

    private final ComponentEventHandler eventHandler;

    @SuppressWarnings("rawtypes")
    private final Bag<ComponentMapper> components;
    private final Map<Enum<?>, EnumComponentMapper<?>> enumComponents = new IdentityHashMap<>();

    private final Bag<WildcardComponentsImpl<?>> wildcardComponentMappers;
    private final Map<ComponentType<?>, Components<?>> componentMappers = new ConcurrentHashMap<>();

    public ComponentMapperManager(EventManager eventManager, BagManager bagManager, ComponentManager componentManager, TransmutationManager transmutationManager) {
        this.bagManager = bagManager;
        this.componentManager = componentManager;
        this.transmutationManager = transmutationManager;

        this.eventHandler = new ComponentEventHandler(eventManager);

        this.wildcardComponentMappers = bagManager.createComponentBag(WildcardComponentsImpl.class);
        this.components = bagManager.createComponentBag(ComponentMapper.class);
    }

    public void process() {
        var data = wildcardComponentMappers.getData();
        for (int i = 0, s = wildcardComponentMappers.getSize(); i < s; i++) {
            data[i].process();
        }
    }

    @Override
    public <T> Components<T> getComponents(ComponentType<T> type) {
        return switch (type) {
            case ComponentType.RegularComponentType<T> regular -> getComponents(regular);
            case ComponentType.Wildcard<?> wildcard -> getWildcardComponents(wildcard);
        };
    }

    @Override
    public <T> ComponentMapper<T> getComponents(RegularComponentType<T> type) {
        return switch (type) {
            case ComponentType.ClassType<T> classType -> getClassComponentMapper(classType);
        };
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private <T> ComponentMapper<T> getClassComponentMapper(ClassType<T> type) {
        var metadata = (ComponentData<T>) componentManager.getComponent(type);

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
                case Component.PooledComponentData<?> pooled -> (ComponentMapper<T>) new PooledComponentMapperImpl(pooled);
                case Component.ComponentData<T> data -> new ComponentMapperImpl<>(data);
            };

            this.components.set(metadata.id(), mapper);
            this.componentMappers.put(type, mapper);

            return mapper;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Enum<T>> @NonNull EnumComponentMapper<T> getEnumComponents(@NonNull T defaultComponent) {
        var result = (EnumComponentMapper<T>) enumComponents.get(defaultComponent);
        if (result != null) {
            return result;
        }

        synchronized (this.components) {
            result = (EnumComponentMapper<T>) enumComponents.get(defaultComponent);
            if (result != null) {
                return result;
            }

            var delegate = (ComponentMapperImpl<T>) getComponents(defaultComponent.getClass());

            var mapper = new EnumComponentMapperImpl<>(delegate, defaultComponent);
            enumComponents.put(defaultComponent, mapper);

            return mapper;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Pooled> PooledComponentMapper<T> getPooledComponents(RegularComponentType<T> type) {
        var metadata = componentManager.getPooledComponent(type);

        var result = (PooledComponentMapper<T>) this.components.get(metadata.id());
        if (result != null) {
            return result;
        }

        synchronized (this.components) {
            result = (PooledComponentMapper<T>) this.components.get(metadata.id());
            if (result != null) {
                return result;
            }

            var mapper = new PooledComponentMapperImpl<>(metadata);

            this.components.set(metadata.id(), mapper);
            this.componentMappers.put(type, mapper);

            return mapper;
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private <T> Components<T> getWildcardComponents(Wildcard<?> wildcard) {
        var result = (Components<T>) componentMappers.get(wildcard);
        if (result != null) {
            return result;
        }

        synchronized (this.componentMappers) {
            result = (Components<T>) componentMappers.get(wildcard);
            if (result != null) {
                return result;
            }

            var mapper = new WildcardComponentsImpl(wildcard);

            this.wildcardComponentMappers.add(mapper);
            this.componentMappers.put(wildcard, mapper);

            return mapper;
        }
    }

    private class ComponentMapperImpl<T> implements ComponentMapper<T> {

        protected final ComponentData<T> data;

        private final TransmutationManager.Add<T> add;
        private final TransmutationManager.Remove remove;

        protected ComponentMapperImpl(ComponentData<T> data) {
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

    private class EnumComponentMapperImpl<T extends Enum<T>> implements EnumComponentMapper<T> {

        private final ComponentMapper<T> delegate;
        private final T defaultComponent;

        public EnumComponentMapperImpl(ComponentMapper<T> delegate, T defaultComponent) {
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

    private class PooledComponentMapperImpl<T extends Pooled> extends ComponentMapperImpl<T> implements PooledComponentMapper<T> {

        private final PooledComponentData<T> data;

        public PooledComponentMapperImpl(PooledComponentData<T> data) {
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

    private class WildcardComponentsImpl<T> implements Components<Result<T>>, ResultComponents<T> {

        private final Wildcard<T> type;
        private final Bag<ComponentMapper<? extends T>> mappers;

        private final Pool<ResultImpl<T>> pool = Pool.unbounded(ResultImpl.class, this::createInstance);
        private final Bag<ResultImpl<T>> lent = new Bag<>(ResultImpl.class, 8);

        public WildcardComponentsImpl(Wildcard<T> type) {
            this.type = type;
            this.mappers = bagManager.createComponentBag(ComponentMapper.class);

            eventHandler.handleComponentEvents(type, this);
        }

        @Override
        public void free(Result<T> result) {
            if (result instanceof ResultImpl<T> impl) {
                this.pool.free(impl);
            }
        }

        public void process() {
            var data = lent.getData();
            for (int i = 0, s = lent.getSize(); i < s; i++) {
                pool.free(data[i]);
            }
        }

        private ResultImpl<T> createInstance() {
            return new ResultImpl<>(type.bound(), mappers);
        }

        @Override
        public boolean has(int entityId) {
            var data = mappers.getData();
            for (int i = 0, s = mappers.getSize(); i < s; i++) {
                if (data[i].has(entityId)) {
                    return true;
                }
            }

            return false;
        }

        @Override
        public Result<T> get(int entityId) {
            var result = pool.getInstance().init(entityId);
            lent.add(result);

            return result;
        }

        @Override
        public boolean remove(int entityId) {
            var removed = false;

            var data = mappers.getData();
            for (int i = 0, s = mappers.getSize(); i < s; i++) {
                removed |= data[i].remove(entityId);
            }

            return removed;
        }

    }

    private class ComponentEventHandler {

        private final Map<Wildcard<?>, Bag<WildcardComponentsImpl<?>>> components = new ConcurrentHashMap<>();

        public ComponentEventHandler(EventManager eventManager) {
            eventManager.registerEventHandler(ComponentAddedEvent.class, this::handleComponentAdded);
        }

        @SuppressWarnings({ "unchecked", "rawtypes" })
        private void handleComponentAdded(ComponentAddedEvent event) {
            for (var entry : components.entrySet()) {
                if (ComponentUtils.matches(entry.getKey(), event.type())) {
                    var bags = entry.getValue();

                    var data = bags.getData();
                    for (int i = 0, s = bags.getSize(); i < s; i++) {
                        data[i].mappers.add((ComponentMapper) getComponents(event.type()));
                    }
                }
            }
        }

        @SuppressWarnings({ "unchecked", "rawtypes" })
        public void handleComponentEvents(Wildcard<?> type, WildcardComponentsImpl<?> wildcardComponents) {
            // Add known components
            var components = componentManager.getComponents();
            for (int i = 0, s = components.getSize(); i < s; i++) {
                var component = components.get(i);
                
                if (ComponentUtils.matches(type, component.type())) {
                    wildcardComponents.mappers.add((ComponentMapper) getComponents(component.type()));
                }
            }

            // Add bag to tracked bags
            var bags = this.components.computeIfAbsent(type, ignore -> new Bag<>(WildcardComponentsImpl.class, 32));
            bags.add(wildcardComponents);
        }

    }

}
