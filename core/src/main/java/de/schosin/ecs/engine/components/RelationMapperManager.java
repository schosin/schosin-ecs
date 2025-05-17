package de.schosin.ecs.engine.components;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jspecify.annotations.Nullable;

import de.schosin.ecs.api.components.ComponentType.ComponentRelationType;
import de.schosin.ecs.api.components.ComponentType.ExclusiveComponentRelationType;
import de.schosin.ecs.api.components.Components;
import de.schosin.ecs.api.components.Components.ComponentRelationMapper;
import de.schosin.ecs.api.components.Components.ExclusiveComponentRelationMapper;
import de.schosin.ecs.api.components.Relation.ComponentRelation;
import de.schosin.ecs.api.components.Relation.Exclusive;
import de.schosin.ecs.api.components.Result.ComponentRelationResult;
import de.schosin.ecs.storage.api.components.Component;
import de.schosin.ecs.storage.api.components.Component.ComponentRelationData;
import de.schosin.ecs.storage.api.components.Component.ExclusiveComponentRelationData;
import de.schosin.ecs.utils.collections.Bag;

public class RelationMapperManager {

    private final ComponentManager componentManager;
    private final TransmutationManager transmutationManager;

    private final Map<Class<?>, RelationMappers<?>> componentRelations = new ConcurrentHashMap<>();
    private final Map<Class<?>, ExclusiveRelationMappers<?>> exclusiveComponentRelations = new ConcurrentHashMap<>();

    public RelationMapperManager(ComponentManager componentManager, TransmutationManager transmutationManager) {
        this.componentManager = componentManager;
        this.transmutationManager = transmutationManager;
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

        synchronized (componentRelations) {
            relationships = (ExclusiveRelationMappers<R>) exclusiveComponentRelations.get(relationship);
            if (relationships != null) {
                return relationships.getComponentRelationMapper(type);
            }

            relationships = new ExclusiveRelationMappers<>();

            this.exclusiveComponentRelations.put(relationship, relationships);

            return relationships.getComponentRelationMapper(type);
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

    private class ComponentRelationMapperImpl<R, T> implements ComponentRelationMapper<R, T> {

        protected final ComponentRelationData<R, T> data;

        private final TransmutationManager.Add<ComponentRelation<R, T>> add;
        private final TransmutationManager.Remove remove;

        public ComponentRelationMapperImpl(ComponentRelationData<R, T> data) {
            this.data = data;

            this.add = transmutationManager.getAddTransmuter(data.type());
            this.remove = transmutationManager.getRemoveTransmuter(data.type());
        }

        @Override
        public boolean has(int entityId) {
            return data.hasComponent(entityId);
        }

        @Override
        @Nullable
        public ComponentRelationResult<R, T> get(int entityId) {
            return data.getComponent(entityId);
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
        public ComponentRelation<R, T> add(int entityId, R relationship, T target) {
            var relation = this.data.getInstance(relationship, target);
            this.add.apply(entityId, relation);

            return relation;
        }

        @Override
        public boolean remove(int entityId) {
            return this.remove.apply(entityId);
        }

        @Override
        public ComponentRelation<R, T> getInstance(R relationship, T target) {
            return data.getInstance(relationship, target);
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
        public ComponentRelation<R, T> add(int entityId, R relationship, T target) {
            var data = parent.componentRelationships.getData();
            for (int i = 0, s = parent.componentRelationships.getSize(); i < s; i++) {
                var otherMapper = data[i];
                if (otherMapper != this) {
                    otherMapper.remove(entityId);
                }
            }

            var relation = this.data.getInstance(relationship, target);
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

        @Override
        public ComponentRelation<R, T> getInstance(R relationship, T target) {
            return data.getInstance(relationship, target);
        }

    }

    private abstract class AbstractComponentRelationMapper<R, T, RR, C extends Component<ComponentRelation<R, T>, RR>> implements Components<ComponentRelation<R, T>, RR> {

        protected C data;

        protected final TransmutationManager.Add<ComponentRelation<R, T>> add;
        protected final TransmutationManager.Remove remove;

        protected AbstractComponentRelationMapper(C data) {
            this.data = data;

            this.add = transmutationManager.getAddTransmuter(data.type());
            this.remove = transmutationManager.getRemoveTransmuter(data.type());
        }

        @Override
        public boolean has(int entityId) {
            return data.hasComponent(entityId);
        }

        @Override
        @Nullable
        public RR get(int entityId) {
            return data.getComponent(entityId);
        }

        @Override
        public boolean remove(int entityId) {
            return this.remove.apply(entityId);
        }

    }

}
