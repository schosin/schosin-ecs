package de.schosin.ecs.engine.components;

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
import de.schosin.ecs.api.components.mappers.EntityFetchRelations.EntityRelationFetchMapper;
import de.schosin.ecs.api.components.mappers.EntityFetchRelations.ExclusiveEntityRelationFetchMapper;
import de.schosin.ecs.api.components.mappers.EntityRelations.EntityRelationMapper;
import de.schosin.ecs.api.components.mappers.EntityRelations.ExclusiveEntityRelationMapper;
import de.schosin.ecs.api.components.types.ClassType;
import de.schosin.ecs.api.components.types.ComponentSetType;
import de.schosin.ecs.api.components.types.ComponentType;
import de.schosin.ecs.api.components.types.ComponentType.RegularComponentType;
import de.schosin.ecs.api.components.types.RelationComponentType.ComponentRelationType;
import de.schosin.ecs.api.components.types.RelationComponentType.EntityRelationType;
import de.schosin.ecs.api.components.types.RelationComponentType.ExclusiveComponentRelationType;
import de.schosin.ecs.api.components.types.RelationComponentType.ExclusiveEntityRelationType;
import de.schosin.ecs.api.components.types.RelationFetchType.EntityRelationFetchType;
import de.schosin.ecs.api.components.types.RelationFetchType.ExclusiveEntityRelationFetchType;
import de.schosin.ecs.api.components.types.Wildcard;
import de.schosin.ecs.engine.BagManager;
import de.schosin.ecs.engine.components.mappers.ComponentMapperImpl;
import de.schosin.ecs.engine.components.mappers.ComponentSetComponentsImpl;
import de.schosin.ecs.engine.components.mappers.EnumComponentMapperImpl;
import de.schosin.ecs.engine.components.mappers.PooledComponentMapperImpl;
import de.schosin.ecs.engine.components.mappers.WildcardComponentsImpl;
import de.schosin.ecs.engine.components.mappers.WildcardComponentsImpl.WildcardComponentSync;
import de.schosin.ecs.engine.components.mappers.fetch.EntityRelationFetchMapperImpl;
import de.schosin.ecs.engine.components.mappers.fetch.ExclusiveEntityRelationFetchMapperImpl;
import de.schosin.ecs.engine.events.EventManager;
import de.schosin.ecs.engine.events.builtin.ComponentAddedEvent;
import de.schosin.ecs.storage.api.components.Component;
import de.schosin.ecs.utils.collections.Bag;

public class ComponentMapperManager implements Components.Creator {

    public interface PoolingComponents<T> {
        void free(T result);
    }

    public interface ReclaimingComponents {
        void reclaim();
    }

    private final BagManager bagManager;
    private final ComponentManager componentManager;
    private final TransmutationManager transmutationManager;
    private final RelationMapperManager relationMapperManager;

    private final ComponentEventHandler eventHandler;

    private final Bag<Components<?, ?>> components;
    private final Map<Enum<?>, EnumComponentMapper<?>> enumComponents = new IdentityHashMap<>();

    private final Bag<ReclaimingComponents> reclaimingComponents;
    private final Map<ComponentType<?, ?>, Components<?, ?>> componentMappers = new ConcurrentHashMap<>();

    public ComponentMapperManager(EventManager eventManager, BagManager bagManager, ComponentManager componentManager, TransmutationManager transmutationManager,
            RelationMapperManager relationMapperManager) {

        this.bagManager = bagManager;
        this.componentManager = componentManager;
        this.transmutationManager = transmutationManager;
        this.relationMapperManager = relationMapperManager;

        this.eventHandler = new ComponentEventHandler(eventManager);

        this.reclaimingComponents = bagManager.createComponentBag(ReclaimingComponents.class);
        this.components = bagManager.createComponentBag(Components.class);
    }

    public void process() {
        var data = reclaimingComponents.getData();
        for (int i = 0, s = reclaimingComponents.getSize(); i < s; i++) {
            data[i].reclaim();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T, R> Components<T, R> getComponents(ComponentType<T, R> type) {
        return switch (type) {
            case RegularComponentType<T, R> regular -> getComponents(regular);
            case EntityRelationFetchType<?, ?> fetch -> (Components<T, R>) getEntityFetchRelations(fetch);
            case ExclusiveEntityRelationFetchType<?, ?> fetch -> (Components<T, R>) getEntityFetchRelations(fetch);
            case ComponentSetType<?> set -> (Components<T, R>) getComponentSets(set);
            case Wildcard<?> wildcard -> (Components<T, R>) getWildcardComponents(wildcard);
        };
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T, R> Components<T, R> getComponents(RegularComponentType<T, R> type) {
        return (Components<T, R>) switch (type) {
            case ClassType<?> classType -> getComponents(classType);
            case ComponentRelationType<?, ?> relation -> getComponentRelations(relation);
            case ExclusiveComponentRelationType<?, ?> relation -> getComponentRelations(relation);
            case EntityRelationType<?> relation -> getEntityRelations(relation);
            case ExclusiveEntityRelationType<?> relation -> getEntityRelations(relation);
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
    public <R, T> ComponentRelationMapper<R, T> getComponentRelations(ComponentRelationType<R, T> relation) {
        return relationMapperManager.getComponentRelationMapper(relation);
    }

    @Override
    public <R extends Exclusive, T> ExclusiveComponentRelationMapper<R, T> getComponentRelations(ExclusiveComponentRelationType<R, T> relation) {
        return relationMapperManager.getComponentRelationMapper(relation);
    }

    @Override
    public <R> EntityRelationMapper<R> getEntityRelations(EntityRelationType<R> relation) {
        return relationMapperManager.getEntityRelationMapper(relation);
    }

    @Override
    public <R extends Exclusive> ExclusiveEntityRelationMapper<R> getEntityRelations(ExclusiveEntityRelationType<R> relation) {
        return relationMapperManager.getEntityRelationMapper(relation);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R, T> EntityRelationFetchMapper<R, T> getEntityFetchRelations(EntityRelationFetchType<R, T> relation) {
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
    public <R extends Exclusive, T> ExclusiveEntityRelationFetchMapper<R, T> getEntityFetchRelations(ExclusiveEntityRelationFetchType<R, T> relation) {
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
    public <T extends ComponentSet> ComponentSetMapper<T> getComponentSets(ComponentSetType<T> type) {
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

            var mapper = new WildcardComponentsImpl<>(wildcard, bagManager, eventHandler);

            this.reclaimingComponents.add(mapper);
            this.componentMappers.put(wildcard, mapper);

            return mapper;
        }
    }

    private class ComponentEventHandler implements WildcardComponentSync {

        private final Map<Wildcard<?>, Bag<WildcardComponentsImpl<?>>> components = new ConcurrentHashMap<>();

        public ComponentEventHandler(EventManager eventManager) {
            eventManager.registerEventHandler(ComponentAddedEvent.class, this::handleComponentAdded);
        }

        @SuppressWarnings({ "unchecked", "rawtypes" })
        private void handleComponentAdded(ComponentAddedEvent event) {
            for (var entry : components.entrySet()) {
                var componentType = entry.getKey();

                if (componentType.matches(event.type())) {
                    var bags = entry.getValue();

                    var data = bags.getData();
                    for (int i = 0, s = bags.getSize(); i < s; i++) {
                        data[i].addMapper((ComponentMapper) getComponents(event.type()));
                    }
                }
            }
        }

        @SuppressWarnings({ "unchecked", "rawtypes" })
        public void addWildcardComponents(Wildcard<?> type, WildcardComponentsImpl<?> wildcardComponents) {
            // Add known components
            var components = componentManager.getComponents();
            for (int i = 0, s = components.getSize(); i < s; i++) {
                var component = components.get(i);

                if (type.matches(component.type())) {
                    wildcardComponents.addMapper((ComponentMapper) getComponents(component.type()));
                }
            }

            // Add bag to tracked bags
            var bags = this.components.computeIfAbsent(type, ignore -> new Bag<>(WildcardComponentsImpl.class, 32));
            bags.add(wildcardComponents);
        }

    }

}
