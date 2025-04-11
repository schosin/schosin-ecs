package de.schosin.ecs.engine.utils.collections;

import java.lang.reflect.InvocationTargetException;

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

    public static <T> T createSingleton(Class<T> clazz) {
        try {
            return clazz.getDeclaredConstructor().newInstance();
        } catch (NoSuchMethodException ex) {
            throw new UnsupportedOperationException("Failed to create singleton of %s. Default constructor not found.".formatted(clazz.getName()), ex);
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | SecurityException ex) {
            throw new UnsupportedOperationException("Failed to create singleton of %s: %s".formatted(clazz.getName(), ex.getMessage()), ex);
        }
    }

}
