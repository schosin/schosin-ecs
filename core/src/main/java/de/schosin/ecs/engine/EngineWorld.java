package de.schosin.ecs.engine;

import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.jspecify.annotations.NonNull;

import de.schosin.ecs.api.Pooled;
import de.schosin.ecs.api.World;
import de.schosin.ecs.api.components.ComponentSet;
import de.schosin.ecs.api.components.Relation.Exclusive;
import de.schosin.ecs.api.components.mappers.ComponentMapper;
import de.schosin.ecs.api.components.mappers.ComponentMapper.EnumComponentMapper;
import de.schosin.ecs.api.components.mappers.ComponentMapper.PooledComponentMapper;
import de.schosin.ecs.api.components.mappers.ComponentRelations.ComponentRelationMapper;
import de.schosin.ecs.api.components.mappers.ComponentRelations.ExclusiveComponentRelationMapper;
import de.schosin.ecs.api.components.mappers.ComponentSetMapper;
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
import de.schosin.ecs.api.components.types.RelationComponentType.ComponentRelationType;
import de.schosin.ecs.api.components.types.RelationComponentType.EntityRelationType;
import de.schosin.ecs.api.components.types.RelationComponentType.ExclusiveComponentRelationType;
import de.schosin.ecs.api.components.types.RelationComponentType.ExclusiveEntityRelationType;
import de.schosin.ecs.api.components.types.RelationFetchType.EntityRelationFetchType;
import de.schosin.ecs.api.components.types.RelationFetchType.ExclusiveEntityRelationFetchType;
import de.schosin.ecs.api.components.types.WildcardRelationType.WildcardComponentRelationType;
import de.schosin.ecs.api.components.types.WildcardRelationType.WildcardEntityRelationFetchType;
import de.schosin.ecs.api.components.types.WildcardRelationType.WildcardEntityRelationType;
import de.schosin.ecs.engine.components.ComponentManager;
import de.schosin.ecs.engine.components.ComponentMapperManager;
import de.schosin.ecs.engine.components.ComponentMaskManager;
import de.schosin.ecs.engine.components.RelationMapperManager;
import de.schosin.ecs.engine.components.TransmutationManager;
import de.schosin.ecs.engine.entities.EntityManager;
import de.schosin.ecs.engine.events.EventManager;
import de.schosin.ecs.storage.api.StorageEngine;
import de.schosin.ecs.storage.api.StorageWorld;
import de.schosin.ecs.storage.api.components.Component;
import de.schosin.ecs.utils.collections.Bag;

public class EngineWorld implements World, StorageWorld {

    private record Config(int processLoops) {
        public Config(WorldBuilder<?> builder) {
            this(builder.processLoops);
        }
    }

    public record Classes(Set<Class<?>> components, Set<Class<?>> states) {
    }

    private final Config config;

    private final EventManager eventManager;
    private final SingletonManager singletonManager;
    private final BagManager bagManager;
    private final IdManager idManager;
    private final ComponentManager componentManager;
    private final ComponentMaskManager componentMaskManager;
    private final EntityManager entityManager;
    private final ChangeManager changeManager;
    private final TransmutationManager transmutationManager;
    private final RelationMapperManager relationMapperManager;
    private final ComponentMapperManager componentMapperManager;

    public EngineWorld(WorldBuilder<?> builder, StorageEngine storageEngine) {
        this.config = new Config(builder);

        this.singletonManager = new SingletonManager(this);
        singletonManager.addSingleton(StorageEngine.class, storageEngine);

        var classes = addSingleton(new Classes(ConcurrentHashMap.newKeySet(), ConcurrentHashMap.newKeySet()));

        this.eventManager = addSingleton(new EventManager());
        this.bagManager = addSingleton(new BagManager());
        this.idManager = addSingleton(new IdManager(bagManager));
        this.componentManager = addSingleton(new ComponentManager(storageEngine, eventManager, classes));
        this.componentMaskManager = addSingleton(new ComponentMaskManager(bagManager, componentManager));
        this.entityManager = addSingleton(new EntityManager(this, idManager, componentManager, componentMaskManager));
        this.changeManager = addSingleton(new ChangeManager(eventManager, bagManager, componentManager, componentMaskManager, entityManager));
        this.transmutationManager = addSingleton(new TransmutationManager(changeManager, componentManager, componentMaskManager, entityManager));
        this.relationMapperManager = addSingleton(new RelationMapperManager(storageEngine, eventManager, bagManager, componentManager, transmutationManager));
        this.componentMapperManager = addSingleton(new ComponentMapperManager(eventManager, bagManager, componentManager, transmutationManager, relationMapperManager));

        // Initialized configured singletons
        if (builder.singletons != null) {
            for (var singleton : builder.singletons.values()) {
                addSingleton(singleton);
            }
        }
    }

    @Override
    public int createEntity(Object... components) {
        return entityManager.createEntity(components);
    }

    @Override
    public void deleteEntity(int entityId) {
        changeManager.deleteEntity(entityId);
    }

    @Override
    public boolean isActive(int entityId) {
        return entityManager.isActive(entityId);
    }

    @Override
    public <T> T addSingleton(@NonNull T singleton) {
        return singletonManager.addSingleton(singleton);
    }

    @Override
    public <T> @NonNull T getSingleton(@NonNull Class<T> clazz) throws NoSuchElementException {
        return singletonManager.getSingleton(clazz);
    }

    @Override
    public <T, R> de.schosin.ecs.api.components.mappers.Components<T, R> getComponents(ComponentType<T, R> type) {
        return componentMapperManager.getComponents(type);
    }

    @Override
    public <T, R> de.schosin.ecs.api.components.mappers.Components<T, R> getComponents(RegularComponentType<T, R> type) {
        return componentMapperManager.getComponents(type);
    }

    @Override
    public <T> @NonNull ComponentMapper<T> getComponents(ClassType<T> type) {
        return componentMapperManager.getComponents(type);
    }

    @Override
    public <T extends Enum<T>> @NonNull EnumComponentMapper<T> getEnumComponents(@NonNull T defaultComponent) {
        return componentMapperManager.getEnumComponents(defaultComponent);
    }

    @Override
    public <T extends Pooled> PooledComponentMapper<T> getPooledComponents(@NonNull ClassType<T> type) {
        return componentMapperManager.getPooledComponents(type);
    }

    @Override
    public <R, T> ComponentRelationMapper<R, T> getComponentRelations(ComponentRelationType<R, T> relation) {
        return componentMapperManager.getComponentRelations(relation);
    }

    @Override
    public <R extends Exclusive, T> ExclusiveComponentRelationMapper<R, T> getComponentRelations(ExclusiveComponentRelationType<R, T> relation) {
        return componentMapperManager.getComponentRelations(relation);
    }

    @Override
    public <R> EntityRelationMapper<R> getEntityRelations(EntityRelationType<R> relation) {
        return componentMapperManager.getEntityRelations(relation);
    }

    @Override
    public <R extends Exclusive> ExclusiveEntityRelationMapper<R> getEntityRelations(ExclusiveEntityRelationType<R> relation) {
        return componentMapperManager.getEntityRelations(relation);
    }

    @Override
    public <R, T> EntityRelationFetchMapper<R, T> getEntityFetchRelations(EntityRelationFetchType<R, T> relation) {
        return componentMapperManager.getEntityFetchRelations(relation);
    }

    @Override
    public <R extends Exclusive, T> ExclusiveEntityRelationFetchMapper<R, T> getEntityFetchRelations(ExclusiveEntityRelationFetchType<R, T> relation) {
        return componentMapperManager.getEntityFetchRelations(relation);
    }

    @Override
    public <T extends ComponentSet> ComponentSetMapper<T> getComponentSets(ComponentSetType<T> type) {
        return componentMapperManager.getComponentSets(type);
    }

    @Override
    public <R, T> WildcardComponentRelations<R, T> getWildcardComponentRelations(WildcardComponentRelationType<R, T> wildcardRelation) {
        return componentMapperManager.getWildcardComponentRelations(wildcardRelation);
    }

    @Override
    public <R> WildcardEntityRelations<R> getWildcardEntityRelations(WildcardEntityRelationType<R> wildcardRelation) {
        return componentMapperManager.getWildcardEntityRelations(wildcardRelation);
    }

    @Override
    public <R, T> WildcardEntityFetchRelations<R, T> getWildcardEntityFetchRelations(WildcardEntityRelationFetchType<R, T> wildcardRelation) {
        return componentMapperManager.getWildcardEntityFetchRelations(wildcardRelation);
    }

    @Override
    public boolean process() {
        return process(config.processLoops);
    }

    @Override
    public boolean process(int loops) {
        componentMapperManager.process();

        return changeManager.process(loops);
    }

    @Override
    public boolean flushEntityUpdates(int entityId) {
        return changeManager.flushEntityUpdates(entityId, config.processLoops);
    }

    @Override
    public <T> Bag<T> createEntityBag(Class<? super T> clazz) {
        return bagManager.createEntityBag(clazz);
    }

    @Override
    public <T, R> void dispatchComponentAddedEvent(RegularComponentType<T, R> type, Component<T, R> component) {
        componentManager.dispatchComponentAddedEvent(type, component);
    }

}
