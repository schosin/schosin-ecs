package de.schosin.ecs.engine;

import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.jspecify.annotations.NonNull;

import de.schosin.ecs.api.Pooled;
import de.schosin.ecs.api.World;
import de.schosin.ecs.api.components.Components;
import de.schosin.ecs.api.components.Components.EnumComponents;
import de.schosin.ecs.api.components.Components.PooledComponents;
import de.schosin.ecs.api.components.Composition;
import de.schosin.ecs.api.components.Spec;
import de.schosin.ecs.api.state.State;
import de.schosin.ecs.api.state.State.PooledState;
import de.schosin.ecs.codegen.EcsCodegen;
import de.schosin.ecs.engine.components.ComponentManager;
import de.schosin.ecs.engine.components.ComponentMapperManager;
import de.schosin.ecs.engine.components.ComponentMaskManager;
import de.schosin.ecs.engine.components.TransmutationManager;
import de.schosin.ecs.engine.compositions.CompositionManager;
import de.schosin.ecs.engine.compositions.SpecManager;
import de.schosin.ecs.engine.entities.ArchetypeManager;
import de.schosin.ecs.engine.entities.EntityManager;
import de.schosin.ecs.engine.entities.StateManager;

@EcsCodegen
public class EngineWorld extends BaseEngineWorld implements World {

    private record Config(int processLoops) {
        public Config(WorldBuilder builder) {
            this(builder.processLoops);
        }
    }

    public record Classes(Set<Class<?>> components, Set<Class<?>> states) {
    }

    private final Config config;

    private final SingletonManager singletonManager;
    private final BagManager bagManager;
    private final IdManager idManager;
    private final StateManager stateManager;
    private final ComponentManager componentManager;
    private final ComponentMaskManager componentMaskManager;
    private final CompositionManager compositionManager;
    private final EntityManager entityManager;
    private final SpecManager specManager;
    private final ArchetypeManager archetypeManager;
    private final ChangeManager changeManager;
    private final TransmutationManager transmutationManager;
    private final ComponentMapperManager componentMapperManager;

    public EngineWorld(WorldBuilder builder) {
        this.config = new Config(builder);
        var classes = new Classes(ConcurrentHashMap.newKeySet(), ConcurrentHashMap.newKeySet());

        this.singletonManager = new SingletonManager(this);
        this.bagManager = addSingleton(new BagManager());
        this.idManager = addSingleton(new IdManager(bagManager));
        this.stateManager = addSingleton(new StateManager(bagManager, classes));
        this.componentManager = addSingleton(new ComponentManager(bagManager, idManager, classes));
        this.componentMaskManager = addSingleton(new ComponentMaskManager(bagManager, componentManager));
        this.compositionManager = addSingleton(new CompositionManager(bagManager, componentManager, componentMaskManager));
        this.entityManager = addSingleton(new EntityManager(this, idManager, componentManager, componentMaskManager, compositionManager));
        this.specManager = addSingleton(new SpecManager(componentManager, entityManager));
        this.archetypeManager = addSingleton(new ArchetypeManager(componentManager, componentMaskManager, entityManager));
        this.changeManager = addSingleton(new ChangeManager(bagManager, componentManager, componentMaskManager, compositionManager, entityManager));
        this.transmutationManager = addSingleton(new TransmutationManager(changeManager, componentManager, componentMaskManager, entityManager));
        this.componentMapperManager = addSingleton(new ComponentMapperManager(bagManager, componentManager, transmutationManager));

        // Initialized configured singletons
        if (builder.singletons != null) {
            for (var singleton : builder.singletons.values()) {
                addSingleton(singleton);
            }
        }

        initialize(archetypeManager, transmutationManager);
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
    public <T> Components<T> getComponents(Class<T> clazz) {
        return componentMapperManager.getComponents(clazz);
    }

    @Override
    public <T extends Enum<T>> @NonNull EnumComponents<T> getEnumComponents(@NonNull T defaultComponent) {
        return componentMapperManager.getEnumComponents(defaultComponent);
    }

    @Override
    public <T extends Pooled> PooledComponents<T> getPooledComponents(@NonNull Class<T> clazz) {
        return componentMapperManager.getPooledComponents(clazz);
    }

    @Override
    public <T> State<T> getState(@NonNull Class<T> clazz) {
        return stateManager.getState(clazz);
    }

    @Override
    public <T extends Pooled> PooledState<T> getPooledState(@NonNull Class<T> clazz) {
        return stateManager.getPooledState(clazz);
    }

    @Override
    public boolean process() {
        return process(config.processLoops);
    }

    @Override
    public boolean process(int loops) {
        return changeManager.process(loops);
    }

    @Override
    public boolean flushEntityUpdates(int entityId) {
        return changeManager.flushEntityUpdates(entityId, config.processLoops);
    }

    @Override
    public Spec createSpec(Composition.Builder builder) {
        return specManager.createSpec(builder);
    }

    @Override
    public Composition createComposition(Composition.Builder builder) {
        return compositionManager.create(builder, entityManager::getEntities);
    }

}
