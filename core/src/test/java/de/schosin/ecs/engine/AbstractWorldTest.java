package de.schosin.ecs.engine;

import org.junit.jupiter.api.BeforeEach;

import de.schosin.ecs.api.World;
import de.schosin.ecs.engine.components.ComponentManager;
import de.schosin.ecs.engine.components.ComponentMapperManager;
import de.schosin.ecs.engine.components.ComponentMaskManager;
import de.schosin.ecs.engine.components.TransmutationManager;
import de.schosin.ecs.engine.entities.EntityManager;
import de.schosin.ecs.engine.test.AbstractEngineTest;

public abstract class AbstractWorldTest extends AbstractEngineTest {

    protected EngineWorld world;

    protected SingletonManager singletonManager;
    protected IdManager idManager;
    protected BagManager bagManager;
    protected ComponentManager componentManager;
    protected ComponentMaskManager componentMaskManager;
    protected EntityManager entityManager;
    protected ChangeManager changeManager;
    protected TransmutationManager transmutationManager;
    protected ComponentMapperManager componentMapperManager;

    @BeforeEach
    final void setupWorld() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        this.world = (EngineWorld) World.builder().build();

        this.singletonManager = world.getSingleton(SingletonManager.class);
        this.bagManager = world.getSingleton(BagManager.class);
        this.idManager = world.getSingleton(IdManager.class);
        this.componentManager = world.getSingleton(ComponentManager.class);
        this.componentMaskManager = world.getSingleton(ComponentMaskManager.class);
        this.entityManager = world.getSingleton(EntityManager.class);
        this.changeManager = world.getSingleton(ChangeManager.class);
        this.transmutationManager = world.getSingleton(TransmutationManager.class);
        this.componentMapperManager = world.getSingleton(ComponentMapperManager.class);

        initializeEngineTest(componentManager, entityManager, changeManager);
    }

    @SuppressWarnings("unchecked")
    protected <T> T getField(Object obj, String name) throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        var field = obj.getClass().getDeclaredField(name);
        field.setAccessible(true);

        return (T) field.get(obj);
    }

}
