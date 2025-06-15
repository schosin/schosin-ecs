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
import de.schosin.ecs.api.components.mappers.ComponentRelationMappers.ComponentRelationMapper;
import de.schosin.ecs.api.components.mappers.ComponentRelationMappers.ExclusiveComponentRelationMapper;
import de.schosin.ecs.api.components.mappers.ComponentSetMapper;
import de.schosin.ecs.api.components.mappers.Components;
import de.schosin.ecs.api.components.mappers.Components.RegularComponents;
import de.schosin.ecs.api.components.mappers.CustomComponentMapper;
import de.schosin.ecs.api.components.mappers.EntityFetchRelationMappers.EntityRelationFetchMapper;
import de.schosin.ecs.api.components.mappers.EntityFetchRelationMappers.ExclusiveEntityRelationFetchMapper;
import de.schosin.ecs.api.components.mappers.EntityRelationMappers.EntityRelationMapper;
import de.schosin.ecs.api.components.mappers.EntityRelationMappers.ExclusiveEntityRelationMapper;
import de.schosin.ecs.api.components.mappers.WildcardRelationMappers.WildcardComponentRelationMapper;
import de.schosin.ecs.api.components.mappers.WildcardRelationMappers.WildcardEntityFetchRelationMapper;
import de.schosin.ecs.api.components.mappers.WildcardRelationMappers.WildcardEntityRelationMapper;
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
import de.schosin.ecs.api.components.types.WildcardRelationType.WildcardComponentRelationType;
import de.schosin.ecs.api.components.types.WildcardRelationType.WildcardEntityRelationFetchType;
import de.schosin.ecs.api.components.types.WildcardRelationType.WildcardEntityRelationType;
import de.schosin.ecs.engine.components.ComponentManager;
import de.schosin.ecs.engine.components.ComponentMapperManager;
import de.schosin.ecs.engine.components.RelationMapperManager;
import de.schosin.ecs.engine.components.TransmutationManager;
import de.schosin.ecs.engine.entities.EntityManager;
import de.schosin.ecs.engine.events.EventManager;
import de.schosin.ecs.engine.events.builtin.ProcessEvent;
import de.schosin.ecs.storage.api.StorageEngine;
import de.schosin.ecs.storage.api.StorageWorld;
import de.schosin.ecs.storage.api.events.StorageEvent;
import de.schosin.ecs.utils.collections.Bag;
import de.schosin.ecs.utils.collections.IntBag;

public class EngineWorld implements World, StorageWorld {

    private record Config(int expectedEntities, int processLoops) {
        public Config(WorldBuilder<?> builder) {
            this(builder.expectedEntities, builder.processLoops);
        }
    }

    public record Classes(Set<Class<?>> components, Set<Class<?>> states) {
    }

    private final Config config;

    private final EventManager eventManager;
    private final SingletonManager singletonManager;
    private final BagManager bagManager;
    private final ComponentManager componentManager;
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
        this.bagManager = addSingleton(new BagManager(config.expectedEntities));
        this.componentManager = addSingleton(new ComponentManager(storageEngine, eventManager, bagManager, classes));
        this.entityManager = addSingleton(new EntityManager(this, storageEngine, bagManager));
        this.changeManager = addSingleton(new ChangeManager(storageEngine, eventManager, entityManager));
        this.transmutationManager = addSingleton(new TransmutationManager(changeManager));
        this.relationMapperManager = addSingleton(new RelationMapperManager(storageEngine, eventManager, bagManager, componentManager, transmutationManager));
        this.componentMapperManager = addSingleton(new ComponentMapperManager(eventManager, bagManager, componentManager, entityManager, transmutationManager, relationMapperManager));

        // Initialized configured singletons
        for (var singleton : builder.singletons.values()) {
            addSingleton(singleton);
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
    public <T, R> Components<T, R> getComponents(ComponentType<T, R> type) {
        return componentMapperManager.getComponents(type);
    }

    @Override
    public <T, R> RegularComponents<T, R> getComponents(RegularComponentType<T, R> type) {
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
    public <R, T> ComponentRelationMapper<R, T> getComponents(ComponentRelationType<R, T> relation) {
        return componentMapperManager.getComponents(relation);
    }

    @Override
    public <R extends Exclusive, T> ExclusiveComponentRelationMapper<R, T> getComponents(ExclusiveComponentRelationType<R, T> relation) {
        return componentMapperManager.getComponents(relation);
    }

    @Override
    public <R> EntityRelationMapper<R> getComponents(EntityRelationType<R> relation) {
        return componentMapperManager.getComponents(relation);
    }

    @Override
    public <R extends Exclusive> ExclusiveEntityRelationMapper<R> getComponents(ExclusiveEntityRelationType<R> relation) {
        return componentMapperManager.getComponents(relation);
    }

    @Override
    public <R, T> EntityRelationFetchMapper<R, T> getComponents(EntityRelationFetchType<R, T> relation) {
        return componentMapperManager.getComponents(relation);
    }

    @Override
    public <R extends Exclusive, T> ExclusiveEntityRelationFetchMapper<R, T> getComponents(ExclusiveEntityRelationFetchType<R, T> relation) {
        return componentMapperManager.getComponents(relation);
    }

    @Override
    public <T extends ComponentSet<?>> ComponentSetMapper<T> getComponents(ComponentSetType<T, ?> type) {
        return componentMapperManager.getComponents(type);
    }

    @Override
    public <R, T> WildcardComponentRelationMapper<R, T> getComponents(WildcardComponentRelationType<R, T> wildcardRelation) {
        return componentMapperManager.getComponents(wildcardRelation);
    }

    @Override
    public <R> WildcardEntityRelationMapper<R> getComponents(WildcardEntityRelationType<R> wildcardRelation) {
        return componentMapperManager.getComponents(wildcardRelation);
    }

    @Override
    public <R, T> WildcardEntityFetchRelationMapper<R, T> getComponents(WildcardEntityRelationFetchType<R, T> wildcardRelation) {
        return componentMapperManager.getComponents(wildcardRelation);
    }

    @Override
    public <T, R, X extends CustomComponentType<T, R, C>, C extends CustomComponentMapper<T, R>> C getComponents(X type) {
        return componentMapperManager.getComponents(type);
    }

    @Override
    public boolean process() {
        var result = process(config.processLoops);
        entityManager.process();

        eventManager.dispatchEvent(ProcessEvent.PROCESS);

        return result;
    }

    @Override
    public boolean process(int loops) {
        var result = changeManager.process(loops);

        eventManager.dispatchEvent(ProcessEvent.PROCESS_STEP);
        componentMapperManager.process();

        return result;
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
    public IntBag createEntityIntBag() {
        return bagManager.createEntityIntBag();
    }

    @Override
    public void dispatchEvent(StorageEvent event) {
        eventManager.dispatchEvent(event);
    }

}
