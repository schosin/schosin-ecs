package de.schosin.ecs.engine.components;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jspecify.annotations.NonNull;

import de.schosin.ecs.api.Pooled;
import de.schosin.ecs.api.components.ComponentSet;
import de.schosin.ecs.api.components.Relation.Exclusive;
import de.schosin.ecs.api.components.Result.ComponentResult;
import de.schosin.ecs.api.components.mappers.ComponentMapper;
import de.schosin.ecs.api.components.mappers.ComponentMapper.EnumComponentMapper;
import de.schosin.ecs.api.components.mappers.ComponentMapper.PooledComponentMapper;
import de.schosin.ecs.api.components.mappers.ComponentRelations.ComponentRelationMapper;
import de.schosin.ecs.api.components.mappers.ComponentRelations.ExclusiveComponentRelationMapper;
import de.schosin.ecs.api.components.mappers.ComponentSetMapper;
import de.schosin.ecs.api.components.mappers.Components;
import de.schosin.ecs.api.components.mappers.Components.RegularComponents;
import de.schosin.ecs.api.components.mappers.CustomComponents;
import de.schosin.ecs.api.components.mappers.CustomComponents.Factory;
import de.schosin.ecs.api.components.mappers.CustomComponents.FactoryAdapter;
import de.schosin.ecs.api.components.mappers.EntityFetchRelations.EntityRelationFetchMapper;
import de.schosin.ecs.api.components.mappers.EntityFetchRelations.ExclusiveEntityRelationFetchMapper;
import de.schosin.ecs.api.components.mappers.EntityRelations.EntityRelationMapper;
import de.schosin.ecs.api.components.mappers.EntityRelations.ExclusiveEntityRelationMapper;
import de.schosin.ecs.api.components.mappers.WildcardRelations.WildcardComponentRelations;
import de.schosin.ecs.api.components.mappers.WildcardRelations.WildcardEntityFetchRelations;
import de.schosin.ecs.api.components.mappers.WildcardRelations.WildcardEntityRelations;
import de.schosin.ecs.api.components.types.ClassType;
import de.schosin.ecs.api.components.types.ComponentSetType;
import de.schosin.ecs.api.components.types.ComponentType;
import de.schosin.ecs.api.components.types.ComponentType.RegularComponentType;
import de.schosin.ecs.api.components.types.CustomComponentType;
import de.schosin.ecs.api.components.types.RelationComponentType.ComponentRelationType;
import de.schosin.ecs.api.components.types.RelationComponentType.EntityRelationType;
import de.schosin.ecs.api.components.types.RelationComponentType.ExclusiveComponentRelationType;
import de.schosin.ecs.api.components.types.RelationComponentType.ExclusiveEntityRelationType;
import de.schosin.ecs.api.components.types.RelationFetchType.EntityRelationFetchType;
import de.schosin.ecs.api.components.types.RelationFetchType.ExclusiveEntityRelationFetchType;
import de.schosin.ecs.api.components.types.Wildcard;
import de.schosin.ecs.api.components.types.WildcardRelationType.WildcardComponentRelationType;
import de.schosin.ecs.api.components.types.WildcardRelationType.WildcardEntityRelationFetchType;
import de.schosin.ecs.api.components.types.WildcardRelationType.WildcardEntityRelationType;
import de.schosin.ecs.engine.BagManager;
import de.schosin.ecs.engine.components.mappers.ComponentMapperImpl;
import de.schosin.ecs.engine.components.mappers.ComponentSetComponentsImpl;
import de.schosin.ecs.engine.components.mappers.EnumComponentMapperImpl;
import de.schosin.ecs.engine.components.mappers.PooledComponentMapperImpl;
import de.schosin.ecs.engine.components.mappers.WildcardComponentsImpl;
import de.schosin.ecs.engine.components.mappers.fetch.EntityRelationFetchMapperImpl;
import de.schosin.ecs.engine.components.mappers.fetch.ExclusiveEntityRelationFetchMapperImpl;
import de.schosin.ecs.engine.components.mappers.wildcardrelations.WildcardComponentRelationsImpl;
import de.schosin.ecs.engine.components.mappers.wildcardrelations.WildcardEntityFetchRelationsImpl;
import de.schosin.ecs.engine.components.mappers.wildcardrelations.WildcardEntityRelationsImpl;
import de.schosin.ecs.engine.events.EventManager;
import de.schosin.ecs.engine.events.builtin.ComponentAddedEvent;
import de.schosin.ecs.storage.api.components.Component;
import de.schosin.ecs.utils.collections.Bag;

public class ComponentMapperManager implements Components.Creator {

    public interface PoolingComponents<T> {
        void free(T result);

        default void reclaim() {
        }
    }

    public interface WildcardMapper<M extends Components<?, ?>> {
        void addMapper(M components);
    }

    private final BagManager bagManager;
    private final ComponentManager componentManager;
    private final TransmutationManager transmutationManager;
    private final RelationMapperManager relationMapperManager;

    private final ComponentEventHandler eventHandler;

    private final Bag<Components<?, ?>> components;
    private final Map<Enum<?>, EnumComponentMapper<?>> enumComponents = new IdentityHashMap<>();

    private final Bag<PoolingComponents<?>> reclaimingComponents;
    private final Map<ComponentType<?, ?>, Components<?, ?>> componentMappers = new ConcurrentHashMap<>();

    private final Map<Class<? extends CustomComponentType<?, ?, ?>>, Factory> factories = new HashMap<>();

    public ComponentMapperManager(EventManager eventManager, BagManager bagManager, ComponentManager componentManager, TransmutationManager transmutationManager,
            RelationMapperManager relationMapperManager) {

        this.bagManager = bagManager;
        this.componentManager = componentManager;
        this.transmutationManager = transmutationManager;
        this.relationMapperManager = relationMapperManager;

        this.eventHandler = new ComponentEventHandler(eventManager);

        this.reclaimingComponents = bagManager.createComponentBag(PoolingComponents.class);
        this.components = bagManager.createComponentBag(Components.class);
    }

    public void process() {
        var data = reclaimingComponents.getData();
        for (int i = 0, s = reclaimingComponents.getSize(); i < s; i++) {
            data[i].reclaim();
        }
    }

    /**
     * Register a {@link CustomComponents.Factory} for a {@link CustomComponentType}.
     * 
     * @param type custom component type
     * @param factory components factory
     * @throws IllegalArgumentException when a factory is already registered for this type
     */
    @SuppressWarnings("rawtypes")
    public synchronized <T extends CustomComponentType, C extends CustomComponents> void registerCustomComponentType(Class<? extends T> type, FactoryAdapter<T, C> factory) {
        registerCustomComponentType(type, (Factory) factory);
    }

    /**
     * Register a {@link CustomComponents.Factory} for a {@link CustomComponentType}.
     * 
     * @param type custom component type
     * @param factory components factory
     * @throws IllegalArgumentException when a factory is already registered for this type
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public synchronized void registerCustomComponentType(Class<? extends CustomComponentType> type, Factory factory) {
        var existing = factories.get(type);
        if (existing != null && existing != factory) {
            throw new IllegalArgumentException("Factory for custom component type '%s' already registered: %s".formatted(type.getName(), existing));
        }

        this.factories.put((Class) type, factory);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T, R> Components<T, R> getComponents(ComponentType<T, R> type) {
        return switch (type) {
            case RegularComponentType<T, R> regular -> getComponents(regular);
            case EntityRelationFetchType<?, ?> fetch -> (Components<T, R>) getComponents(fetch);
            case ExclusiveEntityRelationFetchType<?, ?> fetch -> (Components<T, R>) getComponents(fetch);
            case ComponentSetType<?> set -> (Components<T, R>) getComponents(set);
            case Wildcard<?> wildcard -> (Components<T, R>) getWildcardComponents(wildcard);
            case WildcardComponentRelationType<?, ?> wildcardRelation -> (Components<T, R>) getComponents(wildcardRelation);
            case WildcardEntityRelationType<?> wildcardRelation -> (Components<T, R>) getComponents(wildcardRelation);
            case WildcardEntityRelationFetchType<?, ?> wildcardRelation -> (Components<T, R>) getComponents(wildcardRelation);
            case CustomComponentType<?, ?, ?> custom -> (Components<T, R>) getComponents(custom);
        };
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T, R> RegularComponents<T, R> getComponents(RegularComponentType<T, R> type) {
        return (RegularComponents<T, R>) switch (type) {
            case ClassType<?> classType -> getComponents(classType);
            case ComponentRelationType<?, ?> relation -> getComponents(relation);
            case ExclusiveComponentRelationType<?, ?> relation -> getComponents(relation);
            case EntityRelationType<?> relation -> getComponents(relation);
            case ExclusiveEntityRelationType<?> relation -> getComponents(relation);
        };
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public <T> ComponentMapper<T> getComponents(@NonNull ClassType<T> type) {
        var metadata = componentManager.getComponent(type);

        var result = (ComponentMapper<T>) this.components.get(metadata.id());
        if (result != null) {
            return result;
        }

        synchronized (this.components) {
            result = (ComponentMapper<T>) this.components.get(metadata.id());
            if (result != null) {
                return result;
            }

            var mapper = (ComponentMapper<T>) switch (metadata) {
                case Component.PooledComponentData<?> pooled -> new PooledComponentMapperImpl(pooled, transmutationManager);
                case Component.ComponentData<T> data -> new ComponentMapperImpl<>(data, transmutationManager);
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
    public <T extends Pooled> PooledComponentMapper<T> getPooledComponents(ClassType<T> type) {
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

            var mapper = new PooledComponentMapperImpl<>(metadata, transmutationManager);

            this.components.set(metadata.id(), mapper);
            this.componentMappers.put(type, mapper);

            return mapper;
        }
    }

    @Override
    public <R, T> ComponentRelationMapper<R, T> getComponents(ComponentRelationType<R, T> relation) {
        return relationMapperManager.getComponentRelationMapper(relation);
    }

    @Override
    public <R extends Exclusive, T> ExclusiveComponentRelationMapper<R, T> getComponents(ExclusiveComponentRelationType<R, T> relation) {
        return relationMapperManager.getComponentRelationMapper(relation);
    }

    @Override
    public <R> EntityRelationMapper<R> getComponents(EntityRelationType<R> relation) {
        return relationMapperManager.getEntityRelationMapper(relation);
    }

    @Override
    public <R extends Exclusive> ExclusiveEntityRelationMapper<R> getComponents(ExclusiveEntityRelationType<R> relation) {
        return relationMapperManager.getEntityRelationMapper(relation);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R, T> EntityRelationFetchMapper<R, T> getComponents(EntityRelationFetchType<R, T> relation) {
        var result = (EntityRelationFetchMapper<R, T>) this.componentMappers.get(relation);
        if (result != null) {
            return result;
        }

        synchronized (this.componentMappers) {
            result = (EntityRelationFetchMapper<R, T>) this.componentMappers.get(relation);
            if (result != null) {
                return result;
            }

            var mapper = new EntityRelationFetchMapperImpl<>(relation, this);
            this.componentMappers.put(relation, mapper);

            return mapper;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R extends Exclusive, T> ExclusiveEntityRelationFetchMapper<R, T> getComponents(ExclusiveEntityRelationFetchType<R, T> relation) {
        var result = (ExclusiveEntityRelationFetchMapper<R, T>) this.componentMappers.get(relation);
        if (result != null) {
            return result;
        }

        synchronized (this.componentMappers) {
            result = (ExclusiveEntityRelationFetchMapper<R, T>) this.componentMappers.get(relation);
            if (result != null) {
                return result;
            }

            var mapper = new ExclusiveEntityRelationFetchMapperImpl<>(relation, this);
            this.componentMappers.put(relation, mapper);

            return mapper;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends ComponentSet> ComponentSetMapper<T> getComponents(ComponentSetType<T> type) {
        var result = (ComponentSetMapper<T>) componentMappers.get(type);
        if (result != null) {
            return result;
        }

        synchronized (this.componentMappers) {
            result = (ComponentSetMapper<T>) componentMappers.get(type);
            if (result != null) {
                return result;
            }

            var mapper = new ComponentSetComponentsImpl<>(type, this);

            this.reclaimingComponents.add(mapper);
            this.componentMappers.put(type, mapper);

            return mapper;
        }
    }

    @SuppressWarnings("unchecked")
    private <T> Components<T, ComponentResult<T>> getWildcardComponents(Wildcard<T> wildcard) {
        var result = (Components<T, ComponentResult<T>>) componentMappers.get(wildcard);
        if (result != null) {
            return result;
        }

        synchronized (this.componentMappers) {
            result = (Components<T, ComponentResult<T>>) componentMappers.get(wildcard);
            if (result != null) {
                return result;
            }

            var mapper = new WildcardComponentsImpl<>(wildcard, bagManager);
            eventHandler.registerWildcardMapper(wildcard, mapper);

            this.reclaimingComponents.add(mapper);
            this.componentMappers.put(wildcard, mapper);

            return mapper;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R, T> WildcardComponentRelations<R, T> getComponents(WildcardComponentRelationType<R, T> wildcardRelation) {
        var result = (WildcardComponentRelations<R, T>) componentMappers.get(wildcardRelation);
        if (result != null) {
            return result;
        }

        synchronized (this.componentMappers) {
            result = (WildcardComponentRelations<R, T>) componentMappers.get(wildcardRelation);
            if (result != null) {
                return result;
            }

            var mapper = new WildcardComponentRelationsImpl<R, T>(bagManager);
            eventHandler.registerWildcardMapper(wildcardRelation, mapper);

            this.reclaimingComponents.add(mapper);
            this.componentMappers.put(wildcardRelation, mapper);

            return mapper;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R> WildcardEntityRelations<R> getComponents(WildcardEntityRelationType<R> wildcardRelation) {
        var result = (WildcardEntityRelations<R>) componentMappers.get(wildcardRelation);
        if (result != null) {
            return result;
        }

        synchronized (this.componentMappers) {
            result = (WildcardEntityRelations<R>) componentMappers.get(wildcardRelation);
            if (result != null) {
                return result;
            }

            var mapper = new WildcardEntityRelationsImpl<R>(bagManager);
            eventHandler.registerWildcardMapper(wildcardRelation, mapper);

            this.reclaimingComponents.add(mapper);
            this.componentMappers.put(wildcardRelation, mapper);

            return mapper;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R, T> WildcardEntityFetchRelations<R, T> getComponents(WildcardEntityRelationFetchType<R, T> wildcardRelation) {
        var result = (WildcardEntityFetchRelations<R, T>) componentMappers.get(wildcardRelation);
        if (result != null) {
            return result;
        }

        synchronized (this.componentMappers) {
            result = (WildcardEntityFetchRelations<R, T>) componentMappers.get(wildcardRelation);
            if (result != null) {
                return result;
            }

            var mapper = new WildcardEntityFetchRelationsImpl<>(wildcardRelation, this);

            this.reclaimingComponents.add(mapper);
            this.componentMappers.put(wildcardRelation, mapper);

            return mapper;
        }
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T, R, X extends CustomComponentType<T, R, C>, C extends CustomComponents<T, R>> C getComponents(X type) {
        var result = (C) componentMappers.get(type);
        if (result != null) {
            return result;
        }

        synchronized (this.componentMappers) {
            result = (C) componentMappers.get(type);
            if (result != null) {
                return result;
            }

            var factory = this.factories.get(type.getClass());
            if (factory == null) {
                throw new IllegalArgumentException("No factory registered for custom component type '%s'".formatted(type.getClass().getName()));
            }

            var mapper = factory.createComponents(type);

            if (mapper instanceof PoolingComponents<?> pooling) {
                this.reclaimingComponents.add(pooling);
            }

            this.componentMappers.put(type, mapper);

            return mapper;
        }
    }

    private class ComponentEventHandler {

        @SuppressWarnings("rawtypes")
        private final Map<ComponentType<?, ?>, Bag<WildcardMapper>> components = new ConcurrentHashMap<>();

        public ComponentEventHandler(EventManager eventManager) {
            eventManager.registerEventHandler(ComponentAddedEvent.class, this::handleComponentAdded);
        }

        @SuppressWarnings("unchecked")
        private void handleComponentAdded(ComponentAddedEvent event) {
            for (var entry : components.entrySet()) {
                var componentType = entry.getKey();

                if (componentType.matches(event.type())) {
                    var bags = entry.getValue();

                    var data = bags.getData();
                    for (int i = 0, s = bags.getSize(); i < s; i++) {
                        data[i].addMapper(getComponents(event.type()));
                    }
                }
            }
        }

        @SuppressWarnings({ "unchecked", "rawtypes" })
        public void registerWildcardMapper(ComponentType<?, ?> type, WildcardMapper mapper) {
            // Add known components
            var components = componentManager.getComponents();
            for (int i = 0, s = components.getSize(); i < s; i++) {
                var component = components.get(i);

                if (type.matches(component.type())) {
                    mapper.addMapper(getComponents(component.type()));
                }
            }

            // Add bag to tracked bags
            var bags = this.components.computeIfAbsent(type, ignore -> new Bag<>(WildcardMapper.class, 32));
            bags.add(mapper);
        }

    }

}
