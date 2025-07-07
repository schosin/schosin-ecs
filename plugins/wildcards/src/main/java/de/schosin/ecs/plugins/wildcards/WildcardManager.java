package de.schosin.ecs.plugins.wildcards;

import static de.schosin.ecs.plugins.wildcards.types.WildcardType.wildcardRelation;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import de.schosin.ecs.api.World;
import de.schosin.ecs.api.components.ComponentSet;
import de.schosin.ecs.api.components.mappers.Components;
import de.schosin.ecs.api.components.types.ComponentType;
import de.schosin.ecs.engine.components.ComponentManager;
import de.schosin.ecs.engine.components.ComponentMapperManager;
import de.schosin.ecs.engine.entities.EntityManager;
import de.schosin.ecs.engine.events.EventManager;
import de.schosin.ecs.plugins.wildcards.mappers.WildcardClassMapper;
import de.schosin.ecs.plugins.wildcards.mappers.WildcardComponentRelationMapper;
import de.schosin.ecs.plugins.wildcards.mappers.WildcardEntityRelationFetchMapper;
import de.schosin.ecs.plugins.wildcards.mappers.WildcardEntityRelationMapper;
import de.schosin.ecs.plugins.wildcards.result.WildcardComponentRelations;
import de.schosin.ecs.plugins.wildcards.result.WildcardEntityRelations;
import de.schosin.ecs.plugins.wildcards.result.WildcardEntityRelationsData;
import de.schosin.ecs.plugins.wildcards.result.WildcardResult;
import de.schosin.ecs.plugins.wildcards.types.WildcardClassType;
import de.schosin.ecs.plugins.wildcards.types.WildcardComponentRelationType;
import de.schosin.ecs.plugins.wildcards.types.WildcardEntityRelationFetchType;
import de.schosin.ecs.plugins.wildcards.types.WildcardEntityRelationType;
import de.schosin.ecs.plugins.wildcards.types.WildcardType;
import de.schosin.ecs.storage.api.events.ComponentAddedEvent;
import de.schosin.ecs.utils.collections.Bag;

public class WildcardManager implements WildcardPlugin {

    public interface WildcardMapper<M extends Components<?, ?>> {
        void addMapper(M components);
    }

    private final EntityManager entityManager;
    private final ComponentManager componentManager;
    private final ComponentMapperManager componentMapperManager;

    private final ComponentEventHandler eventHandler;

    public WildcardManager(World world) {
        this.entityManager = world.getSingleton(EntityManager.class);
        this.componentManager = world.getSingleton(ComponentManager.class);

        var eventManager = world.getSingleton(EventManager.class);
        this.eventHandler = new ComponentEventHandler(eventManager);

        this.componentMapperManager = world.getSingleton(ComponentMapperManager.class);
        componentMapperManager.registerCustomComponentType(WildcardClassType.class, this::createWildcardClassMapper);
        componentMapperManager.registerCustomComponentType(WildcardComponentRelationType.class, this::createWildcardComponentRelationMapper);
        componentMapperManager.registerCustomComponentType(WildcardEntityRelationType.class, this::createWildcardEntityRelationMapper);
        componentMapperManager.registerCustomComponentType(WildcardEntityRelationFetchType.class, this::createWildcardEntityRelationFetchMapper);

        registerCustomComponentTypes();
    }

    private <T> WildcardClassMapper<T> createWildcardClassMapper(WildcardClassType<T> type) {
        var mapper = new WildcardClassMapper<T>(entityManager::getAccessor);
        eventHandler.registerWildcardMapper(type, mapper);

        return mapper;
    }

    private <R, T> WildcardComponentRelationMapper<R, T> createWildcardComponentRelationMapper(WildcardComponentRelationType<R, T> type) {
        var mapper = new WildcardComponentRelationMapper<R, T>(entityManager::getAccessor);
        eventHandler.registerWildcardMapper(type, mapper);

        return mapper;
    }

    private <R> WildcardEntityRelationMapper<R> createWildcardEntityRelationMapper(WildcardEntityRelationType<R> type) {
        var mapper = new WildcardEntityRelationMapper<R>(entityManager::getAccessor);
        eventHandler.registerWildcardMapper(type, mapper);

        return mapper;
    }

    private <R, T> WildcardEntityRelationFetchMapper<R, T> createWildcardEntityRelationFetchMapper(WildcardEntityRelationFetchType<R, T> type) {
        return new WildcardEntityRelationFetchMapper<R, T>(entityManager, type, componentMapperManager);
    }

    @Override
    public <R, T> WildcardComponentRelationMapper<R, T> getWildcardComponentRelations(Class<R> relationshipBound, Class<T> targetBound) {
        return componentMapperManager.getComponents(wildcardRelation(relationshipBound, targetBound));
    }

    @Override
    public <R> WildcardEntityRelationMapper<R> getWildcardEntityRelations(Class<R> relationshipBound) {
        return componentMapperManager.getComponents(wildcardRelation(relationshipBound));
    }

    @Override
    public <R, T> WildcardEntityRelationFetchMapper<R, T> getWildcardEntityFetchRelations(Class<R> relationshipBound, ComponentType<?, T> fetch) {
        return componentMapperManager.getComponents(wildcardRelation(relationshipBound, fetch));
    }

    private class ComponentEventHandler {

        @SuppressWarnings("rawtypes")
        private final Map<ComponentType<?, ?>, Bag<WildcardMapper>> components = new ConcurrentHashMap<>();

        private ComponentEventHandler(EventManager eventManager) {
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
                        data[i].addMapper(componentMapperManager.getComponents(event.type()));
                    }
                }
            }
        }

        @SuppressWarnings({ "unchecked", "rawtypes" })
        private void registerWildcardMapper(ComponentType<?, ?> type, WildcardMapper mapper) {
            // Add known components
            var components = componentManager.getComponents();
            for (int i = 0, s = components.getSize(); i < s; i++) {
                var component = components.get(i);

                if (type.matches(component.type())) {
                    mapper.addMapper(componentMapperManager.getComponents(component.type()));
                }
            }

            // Add bag to tracked bags
            var bags = this.components.computeIfAbsent(type, ignore -> new Bag<>(WildcardMapper.class, 32));
            bags.add(mapper);
        }

    }

    private static void registerCustomComponentTypes() {
        ComponentSet.registerComponentType(WildcardClassType.class, WildcardManager::resolveWildcardClassType);
        ComponentSet.registerComponentType(WildcardComponentRelationType.class, WildcardManager::resolveWildcardComponentRelationType);
        ComponentSet.registerComponentType(WildcardEntityRelationType.class, WildcardManager::resolveWildcardEntityRelationType);
        ComponentSet.registerComponentType(WildcardEntityRelationFetchType.class, WildcardManager::resolveWildcardEntityRelationFetchType);
    }

    private static WildcardClassType<?> resolveWildcardClassType(Type type) {
        if (type instanceof ParameterizedType parameterized) {
            var rawType = parameterized.getRawType();
            if (rawType == WildcardResult.class) {
                var bound = (Class<?>) parameterized.getActualTypeArguments()[0];

                return WildcardType.wildcard(bound);
            }
        }

        return null;
    }

    private static WildcardComponentRelationType<?, ?> resolveWildcardComponentRelationType(Type type) {
        if (type instanceof ParameterizedType parameterized) {
            var rawType = parameterized.getRawType();
            if (rawType == WildcardComponentRelations.class) {
                var relationshipBound = (Class<?>) parameterized.getActualTypeArguments()[0];
                var targetBound = (Class<?>) parameterized.getActualTypeArguments()[1];

                return WildcardType.wildcardRelation(relationshipBound, targetBound);
            }
        }

        return null;
    }

    private static WildcardEntityRelationType<?> resolveWildcardEntityRelationType(Type type) {
        if (type instanceof ParameterizedType parameterized) {
            var rawType = parameterized.getRawType();
            if (rawType == WildcardEntityRelations.class) {
                var relationshipBound = (Class<?>) parameterized.getActualTypeArguments()[0];

                return WildcardType.wildcardRelation(relationshipBound);
            }
        }

        return null;
    }

    private static WildcardEntityRelationFetchType<?, ?> resolveWildcardEntityRelationFetchType(Type type) {
        if (type instanceof ParameterizedType parameterized) {
            var rawType = parameterized.getRawType();
            if (rawType == WildcardEntityRelationsData.class) {
                var relationshipBound = (Class<?>) parameterized.getActualTypeArguments()[0];
                var fetchType = ComponentSet.detectComponentType(parameterized.getActualTypeArguments()[1]);

                return WildcardType.wildcardRelation(relationshipBound, fetchType);
            }
        }

        return null;
    }

}
