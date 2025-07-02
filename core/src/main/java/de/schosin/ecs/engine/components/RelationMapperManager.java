package de.schosin.ecs.engine.components;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import de.schosin.ecs.api.components.Relation.Exclusive;
import de.schosin.ecs.api.components.mappers.ComponentRelationMappers.ComponentRelationMapper;
import de.schosin.ecs.api.components.mappers.ComponentRelationMappers.ExclusiveComponentRelationMapper;
import de.schosin.ecs.api.components.mappers.EntityRelationMappers.EntityRelationMapper;
import de.schosin.ecs.api.components.mappers.EntityRelationMappers.ExclusiveEntityRelationMapper;
import de.schosin.ecs.api.components.types.RelationComponentType.ComponentRelationType;
import de.schosin.ecs.api.components.types.RelationComponentType.EntityRelationType;
import de.schosin.ecs.api.components.types.RelationComponentType.ExclusiveComponentRelationType;
import de.schosin.ecs.api.components.types.RelationComponentType.ExclusiveEntityRelationType;
import de.schosin.ecs.api.components.types.RelationComponentType.RegularEntityRelationType;
import de.schosin.ecs.engine.BagManager;
import de.schosin.ecs.engine.components.mappers.relations.AbstractEntityRelationMapper;
import de.schosin.ecs.engine.components.mappers.relations.ComponentRelationMapperImpl;
import de.schosin.ecs.engine.components.mappers.relations.EntityRelationMapperImpl;
import de.schosin.ecs.engine.components.mappers.relations.ExclusiveComponentRelationMapperImpl;
import de.schosin.ecs.engine.components.mappers.relations.ExclusiveComponentRelationMapperImpl.RelationshipParent;
import de.schosin.ecs.engine.components.mappers.relations.ExclusiveEntityRelationMapperImpl;
import de.schosin.ecs.engine.events.EventManager;
import de.schosin.ecs.engine.events.builtin.EntityEvent.EntityRemovedEvent;
import de.schosin.ecs.storage.api.StorageEngine;
import de.schosin.ecs.storage.api.components.Component;
import de.schosin.ecs.storage.api.components.Component.EntityRelationComponent;
import de.schosin.ecs.storage.api.components.Component.EntityRelationComponent.RemovedRelationTypeHandler;
import de.schosin.ecs.utils.collections.Bag;
import de.schosin.ecs.utils.collections.ImmutableBag;

public class RelationMapperManager implements RemovedRelationTypeHandler {

    private final StorageEngine engine;
    private final ComponentManager componentManager;
    private final TransmutationManager transmutationManager;

    private final Map<Class<?>, RelationMappers<?>> componentRelations = new ConcurrentHashMap<>();
    private final Map<Class<?>, ExclusiveRelationMappers<?>> exclusiveComponentRelations = new ConcurrentHashMap<>();

    private final Map<EntityRelationType<?>, EntityRelationMapper<?>> entityRelations = new ConcurrentHashMap<>();
    private final Map<ExclusiveEntityRelationType<?>, ExclusiveEntityRelationMapper<?>> exclusiveEntityRelations = new ConcurrentHashMap<>();

    private final Bag<AbstractEntityRelationMapper<?, ?, ?>> entityRelationMappers;

    private ImmutableBag<Component<?, ?>> components;

    public RelationMapperManager(StorageEngine engine, EventManager eventManager, BagManager bagManager, ComponentManager componentManager, TransmutationManager transmutationManager) {
        this.engine = engine;
        this.componentManager = componentManager;
        this.transmutationManager = transmutationManager;

        this.entityRelationMappers = bagManager.createComponentBag(AbstractEntityRelationMapper.class);

        eventManager.registerEventHandler(EntityRemovedEvent.class, this::handleEntityRemovedEvent);
    }

    private void handleEntityRemovedEvent(EntityRemovedEvent event) {
        if (components == null) {
            this.components = engine.getComponents();
        }

        // Remove relations with entity as target
        for (int i = components.getSize() - 1; i >= 0; i--) {
            var component = components.get(i);
            if (component instanceof EntityRelationComponent<?, ?> relationComponent) {
                relationComponent.removeTarget(event.entityId(), this);
            }
        }
    }

    @Override
    public void removeRelationType(int entityId, RegularEntityRelationType<?, ?> relationType) {
        switch (relationType) {
            case EntityRelationType<?> type -> getEntityRelationMapper(type).remove(entityId);
            case ExclusiveEntityRelationType<?> type -> getEntityRelationMapper(type).remove(entityId);
        }
    }

    @SuppressWarnings("unchecked")
    public <R, T> ComponentRelationMapper<R, T> getComponentRelationMapper(ComponentRelationType<R, T> type) {
        var relationship = type.relationship();

        var relationships = (RelationMappers<R>) componentRelations.get(relationship);
        if (relationships != null) {
            return relationships.getComponentRelationMapper(type);
        }

        synchronized (componentRelations) {
            relationships = (RelationMappers<R>) componentRelations.get(relationship);
            if (relationships != null) {
                return relationships.getComponentRelationMapper(type);
            }

            relationships = new RelationMappers<>();

            this.componentRelations.put(relationship, relationships);

            return relationships.getComponentRelationMapper(type);
        }
    }

    @SuppressWarnings("unchecked")
    public <R extends Exclusive, T> ExclusiveComponentRelationMapper<R, T> getComponentRelationMapper(ExclusiveComponentRelationType<R, T> type) {
        var relationship = type.relationship();

        var relationships = (ExclusiveRelationMappers<R>) exclusiveComponentRelations.get(relationship);
        if (relationships != null) {
            return relationships.getComponentRelationMapper(type);
        }

        synchronized (exclusiveComponentRelations) {
            relationships = (ExclusiveRelationMappers<R>) exclusiveComponentRelations.get(relationship);
            if (relationships != null) {
                return relationships.getComponentRelationMapper(type);
            }

            relationships = new ExclusiveRelationMappers<>();

            this.exclusiveComponentRelations.put(relationship, relationships);

            return relationships.getComponentRelationMapper(type);
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public <R> EntityRelationMapper<R> getEntityRelationMapper(EntityRelationType<R> type) {
        var mapper = (EntityRelationMapper<R>) entityRelations.get(type);
        if (mapper != null) {
            return mapper;
        }

        synchronized (entityRelations) {
            mapper = (EntityRelationMapper<R>) entityRelations.get(type);
            if (mapper != null) {
                return mapper;
            }

            var metadata = componentManager.getComponent(type);
            mapper = new EntityRelationMapperImpl<>(metadata, transmutationManager);

            this.entityRelationMappers.set(metadata.id(), (AbstractEntityRelationMapper) mapper);
            this.entityRelations.put(type, mapper);

            return mapper;
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public <R extends Exclusive> ExclusiveEntityRelationMapper<R> getEntityRelationMapper(ExclusiveEntityRelationType<R> type) {
        var mapper = (ExclusiveEntityRelationMapper<R>) exclusiveEntityRelations.get(type);
        if (mapper != null) {
            return mapper;
        }

        synchronized (exclusiveEntityRelations) {
            mapper = (ExclusiveEntityRelationMapper<R>) exclusiveEntityRelations.get(type);
            if (mapper != null) {
                return mapper;
            }

            var metadata = componentManager.getComponent(type);
            mapper = new ExclusiveEntityRelationMapperImpl<>(metadata, transmutationManager);

            this.entityRelationMappers.set(metadata.id(), (AbstractEntityRelationMapper) mapper);
            this.exclusiveEntityRelations.put(type, mapper);

            return mapper;
        }
    }

    private class RelationMappers<R> {

        private final Map<Class<?>, ComponentRelationMapper<R, ?>> componentRelationshipByTarget = new ConcurrentHashMap<>();

        @SuppressWarnings("unchecked")
        private <T> ComponentRelationMapper<R, T> getComponentRelationMapper(ComponentRelationType<R, T> type) {
            var targetClass = type.target();

            var target = (ComponentRelationMapper<R, T>) componentRelationshipByTarget.get(targetClass);
            if (target != null) {
                return target;
            }

            synchronized (componentRelationshipByTarget) {
                target = (ComponentRelationMapper<R, T>) componentRelationshipByTarget.get(targetClass);
                if (target != null) {
                    return target;
                }

                var metadata = componentManager.getComponent(type);
                var mapper = new ComponentRelationMapperImpl<>(metadata, transmutationManager);

                componentRelationshipByTarget.put(targetClass, mapper);

                return mapper;
            }

        }

    }

    private class ExclusiveRelationMappers<R extends Exclusive> implements RelationshipParent {

        private final Bag<ExclusiveComponentRelationMapperImpl<R, ?>> componentRelationships = new Bag<>(ExclusiveComponentRelationMapperImpl.class, 32);
        private final Map<Class<?>, ExclusiveComponentRelationMapper<R, ?>> componentRelationshipByTarget = new ConcurrentHashMap<>();

        @SuppressWarnings("unchecked")
        private <T> ExclusiveComponentRelationMapper<R, T> getComponentRelationMapper(ExclusiveComponentRelationType<R, T> type) {
            var targetClass = type.target();

            var target = (ExclusiveComponentRelationMapper<R, T>) componentRelationshipByTarget.get(targetClass);
            if (target != null) {
                return target;
            }

            synchronized (componentRelationshipByTarget) {
                target = (ExclusiveComponentRelationMapper<R, T>) componentRelationshipByTarget.get(targetClass);
                if (target != null) {
                    return target;
                }

                var metadata = componentManager.getComponent(type);
                var mapper = new ExclusiveComponentRelationMapperImpl<>(metadata, transmutationManager, this);

                componentRelationships.add(mapper);
                componentRelationshipByTarget.put(targetClass, mapper);

                return mapper;
            }

        }

        @Override
        public void removeFromOthers(int entityId, ExclusiveComponentRelationMapperImpl<?, ?> mapper) {
            var data = componentRelationships.getData();
            for (int i = 0, s = componentRelationships.getSize(); i < s; i++) {
                var otherMapper = data[i];
                if (otherMapper != mapper) {
                    otherMapper.remove(entityId);
                }
            }
        }

    }

}
