package de.schosin.ecs.test;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import org.junit.jupiter.api.BeforeEach;

import de.schosin.ecs.api.World;
import de.schosin.ecs.engine.BagManager;
import de.schosin.ecs.engine.ChangeManager;
import de.schosin.ecs.engine.SingletonManager;
import de.schosin.ecs.engine.components.ComponentManager;
import de.schosin.ecs.engine.components.ComponentMapperManager;
import de.schosin.ecs.engine.components.RelationMapperManager;
import de.schosin.ecs.engine.components.TransmutationManager;
import de.schosin.ecs.engine.entities.EntityManager;
import de.schosin.ecs.engine.events.EventManager;
import de.schosin.ecs.engine.test.AbstractEngineTest;
import de.schosin.ecs.storage.api.StorageEngine;

public abstract class AbstractEcsTest<WORLD extends World> extends AbstractEngineTest {

    protected WORLD world;

    protected StorageEngine storageEngine;

    protected EventManager eventManager;
    protected SingletonManager singletonManager;
    protected BagManager bagManager;
    protected ComponentManager componentManager;
    protected EntityManager entityManager;
    protected ChangeManager changeManager;
    protected TransmutationManager transmutationManager;
    protected RelationMapperManager relationMapperManager;
    protected ComponentMapperManager componentMapperManager;

    @BeforeEach
    final void setupWorld() {
        this.world = createWorld();

        this.storageEngine = world.getSingleton(StorageEngine.class);

        this.eventManager = world.getSingleton(EventManager.class);
        this.singletonManager = world.getSingleton(SingletonManager.class);
        this.bagManager = world.getSingleton(BagManager.class);
        this.componentManager = world.getSingleton(ComponentManager.class);
        this.entityManager = world.getSingleton(EntityManager.class);
        this.changeManager = world.getSingleton(ChangeManager.class);
        this.transmutationManager = world.getSingleton(TransmutationManager.class);
        this.relationMapperManager = world.getSingleton(RelationMapperManager.class);
        this.componentMapperManager = world.getSingleton(ComponentMapperManager.class);

        initializeEngineTest(componentManager, entityManager, eventManager);
    }

    @SuppressWarnings("unchecked")
    protected WORLD createWorld() {
        Type type = this.getClass();

        do {
            if (type instanceof ParameterizedType parameterized && AbstractEcsTest.class.equals(parameterized.getRawType())) {
                var worldClazz = (Class<?>) parameterized.getActualTypeArguments()[0];
                if (World.class.isAssignableFrom(worldClazz)) {
                    return World.builder((Class<WORLD>) worldClazz).build();
                }
            }

            type = switch (type) {
                case Class<?> clazz -> clazz.getGenericSuperclass();
                case ParameterizedType p -> p.getRawType();
                default -> null;
            };
        } while (type != null);

        throw new IllegalStateException("Could not detect type of world. Override createWorld().");
    }

}
