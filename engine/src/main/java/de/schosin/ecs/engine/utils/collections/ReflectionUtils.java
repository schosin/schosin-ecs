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

    public static <T> T createSingleton(World world, Class<T> clazz) {
        try {
            // Try world constructor
            return clazz.getConstructor(World.class).newInstance(world);
        } catch (NoSuchMethodException ignore) {
            // Try default constructor
            try {
                return clazz.getDeclaredConstructor().newInstance();
            } catch (NoSuchMethodException ex) {
                throw new UnsupportedOperationException("Failed to create singleton of %s. Default constructor not found.".formatted(clazz.getName()), ex);
            } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | SecurityException ex) {
                throw new UnsupportedOperationException("Failed to create singleton of %s: %s".formatted(clazz.getName(), ex.getMessage()), ex);
            }
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | SecurityException ex) {
            throw new UnsupportedOperationException("Failed to create singleton of %s: %s".formatted(clazz.getName(), ex.getMessage()), ex);
        }
    }

}
