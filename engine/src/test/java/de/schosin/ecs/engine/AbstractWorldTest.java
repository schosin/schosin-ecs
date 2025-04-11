package de.schosin.ecs.engine;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;

import de.schosin.ecs.api.World;
import de.schosin.ecs.api.components.Composition;
import de.schosin.ecs.engine.components.ComponentManager;
import de.schosin.ecs.engine.components.ComponentMapperManager;
import de.schosin.ecs.engine.components.ComponentMaskManager;
import de.schosin.ecs.engine.components.TransmutationManager;
import de.schosin.ecs.engine.compositions.CompositionManager;
import de.schosin.ecs.engine.entities.ArchetypeManager;
import de.schosin.ecs.engine.entities.EntityManager;
import de.schosin.ecs.engine.utils.collections.IntBag;

public abstract class AbstractWorldTest {

    protected EngineWorld world;

    protected BagManager bagManager;
    protected ComponentManager componentManager;
    protected ComponentMaskManager componentMaskManager;
    protected CompositionManager compositionManager;
    protected EntityManager entityManager;
    protected ArchetypeManager archetypeManager;
    protected ChangeManager changeManager;
    protected TransmutationManager transmutationManager;
    protected ComponentMapperManager componentMapperManager;

    @BeforeEach
    void setupWorld() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        this.world = (EngineWorld) World.builder(WorldBuilder.class.getName()).build();

        this.bagManager = getField("bagManager");
        this.componentManager = getField("componentManager");
        this.componentMaskManager = getField("componentMaskManager");
        this.compositionManager = getField("compositionManager");
        this.entityManager = getField("entityManager");
        this.archetypeManager = getField("archetypeManager");
        this.changeManager = getField("changeManager");
        this.transmutationManager = getField("transmutationManager");
        this.componentMapperManager = getField("componentMapperManager");
    }

    private <T> T getField(String name) throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        return getField(this.world, name);
    }

    @SuppressWarnings("unchecked")
    protected <T> T getField(Object obj, String name) throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        var field = obj.getClass().getDeclaredField(name);
        field.setAccessible(true);

        return (T) field.get(obj);
    }

    protected <T> T getComponent(int entityId, Class<T> clazz) {
        return componentManager.getData(clazz).getComponent(entityId);
    }

    protected void verifyHasComponent(int entityId, Class<?> clazz) {
        var components = componentManager.getData(clazz);
        assertThat(components.hasComponent(entityId)).as("has %s", clazz.getSimpleName()).isTrue();
    }

    protected void verifyDoesNotHaveComponent(int entityId, Class<?> clazz) {
        var components = componentManager.getData(clazz);
        assertThat(components.hasComponent(entityId)).as("does not have %s", clazz.getSimpleName()).isFalse();
    }

    protected void verifyHasComposition(int entityId, Composition.Builder builder) {
        var entities = new IntBag(4);

        var composition = world.createComposition(builder);
        composition.process(entities::add);

        assertThat(entities.getData()).as("has composition " + builder).contains(entityId);
    }

    protected void verifyDoesNotHaveComposition(int entityId, Composition.Builder builder) {
        var entities = new IntBag(4);

        var composition = world.createComposition(builder);
        composition.process(entities::add);

        assertThat(entities.getData()).doesNotContain(entityId);
    }

    protected IntBag getEntities(Composition.Builder builder) {
        var work = new IntBag(4);

        var composition = world.createComposition(builder);
        composition.process(work::add);

        var entities = new IntBag(work.getSize());
        for (int i = 0, s = work.getSize(); i < s; i++) {
            entities.add(work.get(i));
        }

        return entities;
    }

}
