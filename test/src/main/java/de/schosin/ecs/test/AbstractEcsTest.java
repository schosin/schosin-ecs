package de.schosin.ecs.test;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.HashMap;

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
        var typeVarAssigns = new HashMap<TypeVariable<?>, Type>();

        while (type != null) {
            if (type instanceof ParameterizedType parameterized) {
                var rawType = (Class<?>) parameterized.getRawType();

                var typeParams = rawType.getTypeParameters();
                var actualArgs = parameterized.getActualTypeArguments();
                for (int i = 0, s = typeParams.length; i < s; i++) {
                    typeVarAssigns.put(typeParams[i], actualArgs[i]);
                }

                if (AbstractEcsTest.class.equals(rawType)) {
                    var worldType = actualArgs[0];
                    while (worldType instanceof TypeVariable<?> variable) {
                        worldType = typeVarAssigns.get(variable);
                    }

                    if (worldType instanceof Class<?> worldClass && World.class.isAssignableFrom(worldClass)) {
                        return World.builder((Class<WORLD>) worldClass).build();
                    }

                    break;
                }

                type = rawType.getGenericSuperclass();
            } else if (type instanceof Class<?> clazz) {
                type = clazz.getGenericSuperclass();
            } else {
                break;
            }
        }

        throw new IllegalStateException("Could not detect type of world for %s. Override createWorld().".formatted(this.getClass()));
    }

    @SuppressWarnings("unchecked")
    protected WORLD createWorldOld() {
        Type type = this.getClass();

        var types = new ArrayList<Type>();
        System.out.println("-- Start");
        do {
            types.add(0, type);

            if (type instanceof ParameterizedType parameterized && AbstractEcsTest.class.equals(parameterized.getRawType())) {
                var typeArgument = parameterized.getActualTypeArguments()[0];
                if (typeArgument instanceof Class<?> clazz && World.class.isAssignableFrom(clazz)) {
                    return World.builder((Class<WORLD>) clazz).build();
                }

                if (typeArgument instanceof TypeVariable<?> variable) {
                    for (int i = 0, s = variable.getBounds().length; i < s; i++) {
                        var bound = variable.getBounds()[i];
                        if (bound instanceof Class<?> clazz && World.class.isAssignableFrom(clazz)) {
                            do {
                                var parent = types.remove(0);
                                System.out.println("Parent: " + parent);

                            } while (!types.isEmpty());

                            return World.builder((Class<WORLD>) clazz).build();
                        }
                    }
                }

                break;
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
