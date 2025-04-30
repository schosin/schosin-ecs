package de.schosin.ecs.engine;

import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.jspecify.annotations.NonNull;

import de.schosin.ecs.api.Pooled;
import de.schosin.ecs.api.World;
import de.schosin.ecs.api.archetype.Archetype;
import de.schosin.ecs.api.archetype.Transmuter;
import de.schosin.ecs.api.components.Components;
import de.schosin.ecs.api.components.Components.EnumComponents;
import de.schosin.ecs.api.components.Components.PooledComponents;
import de.schosin.ecs.api.components.Composition;
import de.schosin.ecs.api.components.Spec;
import de.schosin.ecs.api.state.State;
import de.schosin.ecs.api.state.State.PooledState;
import de.schosin.ecs.engine.components.ComponentManager;
import de.schosin.ecs.engine.components.ComponentMapperManager;
import de.schosin.ecs.engine.components.ComponentMaskManager;
import de.schosin.ecs.engine.components.TransmutationManager;
import de.schosin.ecs.engine.compositions.CompositionManager;
import de.schosin.ecs.engine.compositions.SpecManager;
import de.schosin.ecs.engine.entities.ArchetypeManager;
import de.schosin.ecs.engine.entities.EntityManager;
import de.schosin.ecs.engine.entities.StateManager;

public class EngineWorld implements World {

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
        this.stateManager = addSingleton(new StateManager(bagManager, classes));
        this.componentManager = addSingleton(new ComponentManager(bagManager, classes));
        this.componentMaskManager = addSingleton(new ComponentMaskManager(bagManager, componentManager));
        this.compositionManager = addSingleton(new CompositionManager(bagManager, componentManager, componentMaskManager));
        this.entityManager = addSingleton(new EntityManager(this, bagManager, componentManager, componentMaskManager, compositionManager));
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
    public <T1> Archetype.Of1<T1> createArchetype(Class<T1> component1) {
        return archetypeManager.createArchetype(component1);
    }

    @Override
    public <T1, T2> Archetype.Of2<T1, T2> createArchetype(Class<T1> component1, Class<T2> component2) {
        return archetypeManager.createArchetype(component1, component2);
    }

    @Override
    public <T1, T2, T3> Archetype.Of3<T1, T2, T3> createArchetype(Class<T1> component1, Class<T2> component2, Class<T3> component3) {
        return archetypeManager.createArchetype(component1, component2, component3);
    }

    @Override
    public <T1, T2, T3, T4> Archetype.Of4<T1, T2, T3, T4> createArchetype(Class<T1> component1, Class<T2> component2, Class<T3> component3, Class<T4> component4) {
        return archetypeManager.createArchetype(component1, component2, component3, component4);
    }

    @Override
    public <T1, T2, T3, T4, T5> Archetype.Of5<T1, T2, T3, T4, T5> createArchetype(Class<T1> component1, Class<T2> component2, Class<T3> component3, Class<T4> component4, Class<T5> component5) {
        return archetypeManager.createArchetype(component1, component2, component3, component4, component5);
    }

    @Override
    public <T1, T2, T3, T4, T5, T6> Archetype.Of6<T1, T2, T3, T4, T5, T6> createArchetype(Class<T1> component1, Class<T2> component2, Class<T3> component3, Class<T4> component4, Class<T5> component5,
            Class<T6> component6) {

        return archetypeManager.createArchetype(component1, component2, component3, component4, component5, component6);
    }

    @Override
    public <T1, T2, T3, T4, T5, T6, T7> Archetype.Of7<T1, T2, T3, T4, T5, T6, T7> createArchetype(Class<T1> component1, Class<T2> component2, Class<T3> component3, Class<T4> component4,
            Class<T5> component5, Class<T6> component6, Class<T7> component7) {

        return archetypeManager.createArchetype(component1, component2, component3, component4, component5, component6, component7);
    }

    @Override
    public <T1, T2, T3, T4, T5, T6, T7, T8> Archetype.Of8<T1, T2, T3, T4, T5, T6, T7, T8> createArchetype(Class<T1> component1, Class<T2> component2, Class<T3> component3, Class<T4> component4,
            Class<T5> component5, Class<T6> component6, Class<T7> component7, Class<T8> component8) {

        return archetypeManager.createArchetype(component1, component2, component3, component4, component5, component6, component7, component8);
    }

    @Override
    public <T1, T2, T3, T4, T5, T6, T7, T8> Archetype.OfN<T1, T2, T3, T4, T5, T6, T7, T8> createArchetype(Class<T1> component1, Class<T2> component2, Class<T3> component3, Class<T4> component4,
            Class<T5> component5, Class<T6> component6, Class<T7> component7, Class<T8> component8, Class<?>... others) {

        return archetypeManager.createArchetype(component1, component2, component3, component4, component5, component6, component7, component8, others);
    }

    @Override
    public Transmuter.Remove createTransmuter(Transmuter.Builder.Remove builder) {
        return transmutationManager.createTransmuter(builder);
    }

    @Override
    public <T1> Transmuter.Add1<T1> createTransmuter(Transmuter.Builder.Add1<T1> builder) {
        return transmutationManager.createTransmuter(builder);
    }

    @Override
    public <T1, T2> Transmuter.Add2<T1, T2> createTransmuter(Transmuter.Builder.Add2<T1, T2> builder) {
        return transmutationManager.createTransmuter(builder);
    }

    @Override
    public <T1, T2, T3> Transmuter.Add3<T1, T2, T3> createTransmuter(Transmuter.Builder.Add3<T1, T2, T3> builder) {
        return transmutationManager.createTransmuter(builder);
    }

    @Override
    public <T1, T2, T3, T4> Transmuter.Add4<T1, T2, T3, T4> createTransmuter(Transmuter.Builder.Add4<T1, T2, T3, T4> builder) {
        return transmutationManager.createTransmuter(builder);
    }

    @Override
    public <T1, T2, T3, T4, T5> Transmuter.Add5<T1, T2, T3, T4, T5> createTransmuter(Transmuter.Builder.Add5<T1, T2, T3, T4, T5> builder) {
        return transmutationManager.createTransmuter(builder);
    }

    @Override
    public <T1, T2, T3, T4, T5, T6> Transmuter.Add6<T1, T2, T3, T4, T5, T6> createTransmuter(Transmuter.Builder.Add6<T1, T2, T3, T4, T5, T6> builder) {
        return transmutationManager.createTransmuter(builder);
    }

    @Override
    public <T1, T2, T3, T4, T5, T6, T7> Transmuter.Add7<T1, T2, T3, T4, T5, T6, T7> createTransmuter(Transmuter.Builder.Add7<T1, T2, T3, T4, T5, T6, T7> builder) {
        return transmutationManager.createTransmuter(builder);
    }

    @Override
    public <T1, T2, T3, T4, T5, T6, T7, T8> Transmuter.Add8<T1, T2, T3, T4, T5, T6, T7, T8> createTransmuter(Transmuter.Builder.Add8<T1, T2, T3, T4, T5, T6, T7, T8> builder) {
        return transmutationManager.createTransmuter(builder);
    }

    @Override
    public <T1, T2, T3, T4, T5, T6, T7, T8> Transmuter.AddN<T1, T2, T3, T4, T5, T6, T7, T8> createTransmuter(Transmuter.Builder.AddN<T1, T2, T3, T4, T5, T6, T7, T8> builder) {
        return transmutationManager.createTransmuter(builder);
    }

    @Override
    public Spec createSpec(Composition.Builder builder) {
        return specManager.createSpec(builder);
    }

    @Override
    public Composition createComposition(Composition.Builder builder) {
        return compositionManager.create(builder, entityManager::getEntities);
    }

    @Override
    public <T1> Composition.Of1<T1> createComposition(Composition.Builder builder, Class<T1> component1) {
        return compositionManager.create(builder, entityManager::getEntities, component1);
    }

    @Override
    public <T1, T2> Composition.Of2<T1, T2> createComposition(Composition.Builder builder, Class<T1> component1, Class<T2> component2) {
        return compositionManager.create(builder, entityManager::getEntities, component1, component2);
    }

    @Override
    public <T1, T2, T3> Composition.Of3<T1, T2, T3> createComposition(Composition.Builder builder, Class<T1> component1, Class<T2> component2, Class<T3> component3) {
        return compositionManager.create(builder, entityManager::getEntities, component1, component2, component3);
    }

    @Override
    public <T1, T2, T3, T4> Composition.Of4<T1, T2, T3, T4> createComposition(Composition.Builder builder, Class<T1> component1, Class<T2> component2, Class<T3> component3, Class<T4> component4) {
        return compositionManager.create(builder, entityManager::getEntities, component1, component2, component3, component4);
    }

    @Override
    public <T1, T2, T3, T4, T5> Composition.Of5<T1, T2, T3, T4, T5> createComposition(Composition.Builder builder, Class<T1> component1, Class<T2> component2, Class<T3> component3,
            Class<T4> component4, Class<T5> component5) {

        return compositionManager.create(builder, entityManager::getEntities, component1, component2, component3, component4, component5);
    }

    @Override
    public <T1, T2, T3, T4, T5, T6> Composition.Of6<T1, T2, T3, T4, T5, T6> createComposition(Composition.Builder builder, Class<T1> component1, Class<T2> component2, Class<T3> component3,
            Class<T4> component4, Class<T5> component5, Class<T6> component6) {

        return compositionManager.create(builder, entityManager::getEntities, component1, component2, component3, component4, component5, component6);
    }

    @Override
    public <T1, T2, T3, T4, T5, T6, T7> Composition.Of7<T1, T2, T3, T4, T5, T6, T7> createComposition(Composition.Builder builder, Class<T1> component1, Class<T2> component2, Class<T3> component3,
            Class<T4> component4, Class<T5> component5, Class<T6> component6, Class<T7> component7) {

        return compositionManager.create(builder, entityManager::getEntities, component1, component2, component3, component4, component5, component6, component7);
    }

    @Override
    public <T1, T2, T3, T4, T5, T6, T7, T8> Composition.Of8<T1, T2, T3, T4, T5, T6, T7, T8> createComposition(Composition.Builder builder, Class<T1> component1, Class<T2> component2,
            Class<T3> component3, Class<T4> component4, Class<T5> component5, Class<T6> component6, Class<T7> component7, Class<T8> component8) {

        return compositionManager.create(builder, entityManager::getEntities, component1, component2, component3, component4, component5, component6, component7, component8);
    }

}
