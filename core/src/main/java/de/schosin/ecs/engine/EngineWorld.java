package de.schosin.ecs.engine;

import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.jspecify.annotations.NonNull;

import de.schosin.ecs.api.Pooled;
import de.schosin.ecs.api.World;
import de.schosin.ecs.api.components.ComponentType;
import de.schosin.ecs.api.components.ComponentType.RegularComponentType;
import de.schosin.ecs.api.components.Components;
import de.schosin.ecs.api.components.Components.ComponentMapper;
import de.schosin.ecs.api.components.Components.EnumComponentMapper;
import de.schosin.ecs.api.components.Components.PooledComponentMapper;
import de.schosin.ecs.engine.components.ComponentManager;
import de.schosin.ecs.engine.components.ComponentMapperManager;
import de.schosin.ecs.engine.components.ComponentMaskManager;
import de.schosin.ecs.engine.components.TransmutationManager;
import de.schosin.ecs.engine.entities.EntityManager;
import de.schosin.ecs.engine.events.EventManager;
import de.schosin.ecs.engine.events.builtin.ComponentAddedEvent.RegularComponentAddedEvent.ClassComponentAddedEvent;
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
        this.componentMapperManager = addSingleton(new ComponentMapperManager(eventManager, bagManager, componentManager, transmutationManager));

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
    public <T> @NonNull Components<T> getComponents(ComponentType<T> type) {
        return componentMapperManager.getComponents(type);
    }

    @Override
    public <T> @NonNull ComponentMapper<T> getComponents(RegularComponentType<T> type) {
        return componentMapperManager.getComponents(type);
    }

    @Override
    public <T extends Enum<T>> @NonNull EnumComponentMapper<T> getEnumComponents(@NonNull T defaultComponent) {
        return componentMapperManager.getEnumComponents(defaultComponent);
    }

    @Override
    public <T extends Pooled> PooledComponentMapper<T> getPooledComponents(@NonNull RegularComponentType<T> type) {
        return componentMapperManager.getPooledComponents(type);
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
    public <T> void dispatchComponentAddedEvent(RegularComponentType<T> type, Component<T> component) {
        var event = switch (type) {
            case ComponentType.ClassType<T> classType -> ClassComponentAddedEvent.get(classType, component);
        };

        eventManager.dispatchEvent(event);
    }

}
