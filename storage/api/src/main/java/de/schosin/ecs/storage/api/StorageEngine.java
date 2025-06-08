package de.schosin.ecs.storage.api;

import java.lang.reflect.InvocationTargetException;
import java.util.ServiceLoader;

import de.schosin.ecs.api.Plugin;
import de.schosin.ecs.api.World;

public interface StorageEngine extends ComponentStorage, EntityStorage, ArchetypeStorage {

    /**
     * Sets the world this storage engine is used for. This will be an unproxied instance.
     * 
     * <p>
     * Implementations must support access to all functionalities after this call. 
     * </p>
     * 
     * @param world instance of world
     * @param config configuration object passed by {@link World.Builder#storageEngine(Class, Object)}
     */
    default void setWorld(StorageWorld world, Object config) {
        setWorld(world);
    }

    /**
     * Sets the world this storage engine is used for. This will be an unproxied instance.
     * 
     * <p>
     * Implementations must support access to all functionalities after this call. 
     * </p>
     * 
     * @param world instance of world
     */
    default void setWorld(StorageWorld world) {
    }

    /**
     * If the world uses {@link Plugin plugins}, this method is called once the proxied world
     * has been fully instantiated. This will always be called after {@link #setWorld(StorageWorld)}.
     * 
     * <p>
     * Implementations may support additional features for plugins which can be initialized in this
     * callback. Access to plugins should be done either by casting the world to the plugin, or by
     * {@link World#getSingleton(Class) accessing singletons}.
     * </p>
     * 
     * @param world proxied instance of world
     */
    default void setProxiedWorld(StorageWorld world) {
    }

    static StorageEngine load() {
        var providers = ServiceLoader.load(StorageEngine.class).stream().toList();
        if (providers.isEmpty()) {
            throw new StorageEngineException("No implementation of StorageEngine found. Make sure to include a storage engine implementation.");
        }

        if (providers.size() > 1) {
            throw new StorageEngineException("Multiple implementations of StorageEngine found (%d). Define storage engine using World.Builder or remove additional storage engines from classpath."
                    .formatted(providers.size()));
        }

        var provider = providers.get(0);
        return provider.get();
    }

    static StorageEngine load(Class<? extends StorageEngine> clazz) {
        try {
            var constructor = clazz.getDeclaredConstructor();
            return constructor.newInstance();
        } catch (NoSuchMethodException | SecurityException ex) {
            throw new StorageEngineException("Implementation '%s' of StorageEngine does not declare a public default constructor.".formatted(clazz.getName()), ex);
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
            throw new StorageEngineException("Failed to instantiate implementation '%s' of StorageEngine through its default constructor.".formatted(clazz.getName()), ex);
        }
    }

}
