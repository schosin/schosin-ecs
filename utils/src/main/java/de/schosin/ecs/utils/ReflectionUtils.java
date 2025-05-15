package de.schosin.ecs.engine.utils.collections;

import java.lang.reflect.InvocationTargetException;

import de.schosin.ecs.api.World;

public class ReflectionUtils {

    public static <T> T createComponentInstance(Class<T> clazz) {
        try {
            return clazz.getDeclaredConstructor().newInstance();
        } catch (NoSuchMethodException ex) {
            throw new UnsupportedOperationException("Failed to create instance of %s. Default constructor not found.".formatted(clazz.getName()), ex);
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | SecurityException ex) {
            throw new UnsupportedOperationException("Failed to create instance of %s: %s".formatted(clazz.getName(), ex.getMessage()), ex);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T createInstance(World world, Class<T> clazz) {
        try {
            // Try to find a constructor accepting EngineWorld or World
            for (var constructor : clazz.getConstructors()) {
                var parameters = constructor.getParameters();
                if (parameters.length == 1 && parameters[0].getType().isAssignableFrom(world.getClass())) {
                    return (T) constructor.newInstance(world);
                }
            }

            // Try default constructor
            return clazz.getDeclaredConstructor().newInstance();
        } catch (NoSuchMethodException ex) {
            throw new UnsupportedOperationException("Failed to create instance of %s. No valid constructor not found.".formatted(clazz.getName()), ex);
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | SecurityException ex) {
            throw new UnsupportedOperationException("Failed to create instance of %s: %s".formatted(clazz.getName(), ex.getMessage()), ex);
        }
    }

}
