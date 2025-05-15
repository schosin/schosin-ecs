package de.schosin.ecs.engine;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;

import org.jspecify.annotations.NonNull;

import de.schosin.ecs.api.World;
import de.schosin.ecs.utils.ReflectionUtils;

public class SingletonManager {

    private final World world;

    private final Map<Class<?>, Object> singletons = new ConcurrentHashMap<>();

    public SingletonManager(World world) {
        this.world = world;

        this.singletons.put(SingletonManager.class, this);
    }

    public <T> T addSingleton(@NonNull T singleton) {
        var existing = this.singletons.putIfAbsent(singleton.getClass(), singleton);
        if (existing != null) {
            throw new IllegalArgumentException("This world already contains a singleton of type " + singleton.getClass());
        }

        return singleton;
    }

    @SuppressWarnings("unchecked")
    public synchronized <T> @NonNull T getSingleton(@NonNull Class<T> clazz) throws NoSuchElementException {
        var result = (T) this.singletons.get(clazz);
        if (result != null) {
            return result;
        }

        result = createSingleton(clazz);
        this.singletons.put(clazz, result);

        return result;
    }

    private <T> T createSingleton(Class<T> clazz) {
        return ReflectionUtils.createInstance(world, clazz);
    }

}
