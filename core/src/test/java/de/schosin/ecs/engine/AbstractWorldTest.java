package de.schosin.ecs.engine;

import org.junit.jupiter.api.BeforeEach;

import de.schosin.ecs.api.World;
import de.schosin.ecs.engine.components.ComponentManager;
import de.schosin.ecs.engine.components.ComponentMapperManager;
import de.schosin.ecs.engine.components.TransmutationManager;
import de.schosin.ecs.engine.entities.EntityManager;
import de.schosin.ecs.engine.events.EventManager;
import de.schosin.ecs.engine.test.AbstractEngineTest;
import de.schosin.ecs.storage.api.StorageEngine;

public abstract class AbstractWorldTest extends AbstractEngineTest {

    protected EngineWorld world;

    protected StorageEngine storageEngine;

    protected EventManager eventManager;
    protected SingletonManager singletonManager;
    protected BagManager bagManager;
    protected ComponentManager componentManager;
    protected EntityManager entityManager;
    protected TransmutationManager transmutationManager;
    protected ComponentMapperManager componentMapperManager;

    @BeforeEach
    final void setupWorld() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        this.world = (EngineWorld) World.builder().build();

        this.storageEngine = world.getSingleton(StorageEngine.class);

        this.eventManager = world.getSingleton(EventManager.class);
        this.singletonManager = world.getSingleton(SingletonManager.class);
        this.bagManager = world.getSingleton(BagManager.class);
        this.componentManager = world.getSingleton(ComponentManager.class);
        this.entityManager = world.getSingleton(EntityManager.class);
        this.transmutationManager = world.getSingleton(TransmutationManager.class);
        this.componentMapperManager = world.getSingleton(ComponentMapperManager.class);

        initializeEngineTest(storageEngine, componentManager, entityManager, eventManager);
    }

    @SuppressWarnings("unchecked")
    protected <T> T getField(Object obj, String name) throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        var field = obj.getClass().getDeclaredField(name);
        field.setAccessible(true);

        return (T) field.get(obj);
    }

}
