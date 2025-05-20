package de.schosin.ecs.engine.components;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jspecify.annotations.Nullable;

import de.schosin.ecs.api.components.ComponentType.ComponentRelationType;
import de.schosin.ecs.api.components.ComponentType.EntityRelationType;
import de.schosin.ecs.api.components.ComponentType.ExclusiveComponentRelationType;
import de.schosin.ecs.api.components.ComponentType.ExclusiveEntityRelationType;
import de.schosin.ecs.api.components.Components.ComponentRelationMapper;
import de.schosin.ecs.api.components.Components.EntityRelationMapper;
import de.schosin.ecs.api.components.Components.ExclusiveComponentRelationMapper;
import de.schosin.ecs.api.components.Components.ExclusiveEntityRelationMapper;
import de.schosin.ecs.api.components.Relation.ComponentRelation;
import de.schosin.ecs.api.components.Relation.EntityRelation;
import de.schosin.ecs.api.components.Relation.Exclusive;
import de.schosin.ecs.api.components.Result.ComponentRelationResult;
import de.schosin.ecs.api.components.Result.EntityRelationResult;
import de.schosin.ecs.engine.events.EventManager;
import de.schosin.ecs.engine.events.builtin.EntityEvent.EntityRemovedEvent;
import de.schosin.ecs.storage.api.StorageEngine;
import de.schosin.ecs.storage.api.components.Component;
import de.schosin.ecs.storage.api.components.Component.ComponentRelationData;
import de.schosin.ecs.storage.api.components.Component.EntityRelationComponent;
import de.schosin.ecs.storage.api.components.Component.EntityRelationData;
import de.schosin.ecs.storage.api.components.Component.ExclusiveComponentRelationData;
import de.schosin.ecs.storage.api.components.Component.ExclusiveEntityRelationData;
import de.schosin.ecs.utils.collections.Bag;
import de.schosin.ecs.utils.collections.ImmutableBag;
import de.schosin.ecs.utils.collections.IntBag;

public class RelationMapperManager {

    private final StorageEngine engine;
    private final ComponentManager componentManager;
    private final TransmutationManager transmutationManager;

    private final Map<Class<?>, RelationMappers<?>> componentRelations = new ConcurrentHashMap<>();
    private final Map<Class<?>, ExclusiveRelationMappers<?>> exclusiveComponentRelations = new ConcurrentHashMap<>();

    private final Map<EntityRelationType<?>, EntityRelationMapper<?>> entityRelations = new ConcurrentHashMap<>();
    private final Map<ExclusiveEntityRelationType<?>, ExclusiveEntityRelationMapper<?>> exclusiveEntityRelations = new ConcurrentHashMap<>();

    private final Bag<AbstractEntityRelationMapper<?, ?, ?>> entityRelationMappers = new Bag<>(AbstractEntityRelationMapper.class, 16);

    private ImmutableBag<Component<?, ?>> components;
    private final IntBag affectedEntities = new IntBag(32);

    public RelationMapperManager(StorageEngine engine, EventManager eventManager, ComponentManager componentManager, TransmutationManager transmutationManager) {
        this.engine = engine;
        this.componentManager = componentManager;
        this.transmutationManager = transmutationManager;

        eventManager.registerEventHandler(EntityRemovedEvent.class, this::handleEntityRemovedEvent);
    }

    private void handleEntityRemovedEvent(EntityRemovedEvent event) {
        if (components == null) {
            this.components = engine.getComponents();
        }

        // Remove relations with entity as target
        for (int i = 0, s = components.getSize(); i < s; i++) {
            var mapper = entityRelationMappers.get(components.get(i).id());
            if (mapper == null) {
                continue;
            }

            mapper.data.removeTarget(event.entityId(), affectedEntities);
            handleRemovedRelations(mapper, affectedEntities);
        }
    }

    private void handleRemovedRelations(AbstractEntityRelationMapper<?, ?, ?> mapper, IntBag affectedEntities) {
        if (affectedEntities.isEmpty()) {
            return;
        }

        var data = affectedEntities.getData();
        for (int i = 0, s = affectedEntities.getSize(); i < s; i++) {
            mapper.remove.apply(data[i]);
        }

        affectedEntities.clear();
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
            mapper = new EntityRelationMapperImpl<>(metadata);

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
            mapper = new ExclusiveEntityRelationMapperImpl<>(metadata);

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
                var mapper = new ComponentRelationMapperImpl<>(metadata);

                componentRelationshipByTarget.put(targetClass, mapper);

                return mapper;
            }

        }

    }

    private class ComponentRelationMapperImpl<R, T> extends AbstractComponentRelationMapper<R, T, ComponentRelationResult<R, T>, ComponentRelationData<R, T>> implements ComponentRelationMapper<R, T> {

        public ComponentRelationMapperImpl(ComponentRelationData<R, T> data) {
            super(data);
        }

        @Override
        public R getRelationship(int entityId, T target) {
            var relation = data.getComponent(entityId);
            if (relation == null) {
                return null;
            }

            return relation.getRelationship(target);
        }

        @Override
        public ComponentRelation<R, T> add(int entityId, ComponentRelation<R, T> relation) {
            this.add.apply(entityId, relation);

            return relation;
        }

    }

    private class ExclusiveRelationMappers<R extends Exclusive> {

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
                var mapper = new ExclusiveComponentRelationMapperImpl<>(metadata, this);

                componentRelationships.add(mapper);
                componentRelationshipByTarget.put(targetClass, mapper);

                return mapper;
            }

        }

    }

    private class ExclusiveComponentRelationMapperImpl<R extends Exclusive, T> extends AbstractComponentRelationMapper<R, T, ComponentRelation<R, T>, ExclusiveComponentRelationData<R, T>>
            implements ExclusiveComponentRelationMapper<R, T> {

        private final ExclusiveRelationMappers<R> parent;

        public ExclusiveComponentRelationMapperImpl(ExclusiveComponentRelationData<R, T> data, ExclusiveRelationMappers<R> parent) {
            super(data);

            this.parent = parent;
        }

        @Override
        public ComponentRelation<R, T> add(int entityId, ComponentRelation<R, T> relation) {
            var data = parent.componentRelationships.getData();
            for (int i = 0, s = parent.componentRelationships.getSize(); i < s; i++) {
                var otherMapper = data[i];
                if (otherMapper != this) {
                    otherMapper.remove(entityId);
                }
            }

            this.add.apply(entityId, relation);

            return relation;
        }

        @Override
        public R getRelationship(int entityId) {
            var relation = get(entityId);
            if (relation == null) {
                return null;
            }

            return relation.relationship();
        }

        @Override
        public T getTarget(int entityId) {
            var relation = get(entityId);
            if (relation == null) {
                return null;
            }

            return relation.target();
        }

    }

    private abstract class AbstractComponentRelationMapper<R, T, RR, C extends Component<ComponentRelation<R, T>, RR>> {

        protected C data;

        protected final TransmutationManager.Add<ComponentRelation<R, T>> add;
        protected final TransmutationManager.Remove remove;

        protected AbstractComponentRelationMapper(C data) {
            this.data = data;

            this.add = transmutationManager.getAddTransmuter(data.type());
            this.remove = transmutationManager.getRemoveTransmuter(data.type());
        }

        public boolean has(int entityId) {
            return data.hasComponent(entityId);
        }

        @Nullable
        public RR get(int entityId) {
            return data.getComponent(entityId);
        }

        public boolean remove(int entityId) {
            return this.remove.apply(entityId);
        }

    }

    private class EntityRelationMapperImpl<R> extends AbstractEntityRelationMapper<R, EntityRelationResult<R>, EntityRelationData<R>> implements EntityRelationMapper<R> {

        public EntityRelationMapperImpl(EntityRelationData<R> data) {
            super(data);
        }

        @Override
        public EntityRelation<R> add(int entityId, EntityRelation<R> relation) {
            this.add.apply(entityId, relation);

            return relation;
        }

        @Override
        public R getRelationship(int entityId, int target) {
            var relation = data.getComponent(entityId);
            if (relation == null) {
                return null;
            }

            return relation.getRelationship(target);
        }

    }

    private class ExclusiveEntityRelationMapperImpl<R extends Exclusive> extends AbstractEntityRelationMapper<R, EntityRelation<R>, ExclusiveEntityRelationData<R>>
            implements ExclusiveEntityRelationMapper<R> {

        public ExclusiveEntityRelationMapperImpl(ExclusiveEntityRelationData<R> data) {
            super(data);
        }

        @Override
        public EntityRelation<R> add(int entityId, EntityRelation<R> relation) {
            this.add.apply(entityId, relation);

            return relation;
        }

        @Override
        public R getRelationship(int entityId) {
            var relation = get(entityId);
            if (relation == null) {
                return null;
            }

            return relation.relationship();
        }

        @Override
        public int getTarget(int entityId) {
            var relation = get(entityId);
            if (relation == null) {
                return -1;
            }

            return relation.target();
        }

    }

    private abstract class AbstractEntityRelationMapper<R, RR, C extends EntityRelationComponent<R, RR>> {

        protected C data;

        protected final TransmutationManager.Add<EntityRelation<R>> add;
        protected final TransmutationManager.Remove remove;

        protected AbstractEntityRelationMapper(C data) {
            this.data = data;

            this.add = transmutationManager.getAddTransmuter(data.type());
            this.remove = transmutationManager.getRemoveTransmuter(data.type());
        }

        public boolean has(int entityId) {
            return data.hasComponent(entityId);
        }

        @Nullable
        public RR get(int entityId) {
            return data.getComponent(entityId);
        }

        public boolean remove(int entityId) {
            return this.remove.apply(entityId);
        }

    }

}
