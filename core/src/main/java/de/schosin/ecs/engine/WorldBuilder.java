package de.schosin.ecs.engine;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.SequencedSet;
import java.util.stream.Collectors;

import org.jspecify.annotations.NonNull;

import de.schosin.ecs.api.Plugin;
import de.schosin.ecs.api.World;
import de.schosin.ecs.api.World.Builder;
import de.schosin.ecs.engine.utils.exceptions.EcsPluginException;
import de.schosin.ecs.engine.utils.exceptions.EcsWorldCreationException;
import de.schosin.ecs.storage.api.StorageEngine;
import de.schosin.ecs.storage.api.StorageWorld;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.ByteCodeElement;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.matcher.ElementMatcher.Junction;
import net.bytebuddy.matcher.ElementMatchers;

public class WorldBuilder<T extends World> implements World.Builder<T> {

    private final Class<T> clazz;

    Class<? extends StorageEngine> storageEngine;
    int processLoops = 3;
    Map<Class<?>, Object> singletons;

    public WorldBuilder(Class<T> clazz) {
        if (!clazz.isInterface()) {
            throw new EcsWorldCreationException("World must be an interface.");
        }

        if (!World.class.equals(clazz)) {
            var abstractMethods = Arrays.stream(clazz.getDeclaredMethods())
                    .filter(method -> Modifier.isAbstract(method.getModifiers()))
                    .map(Method::toGenericString)
                    .toList();

            if (!abstractMethods.isEmpty()) {
                throw new EcsWorldCreationException("World '%s' must not declare abstract methods: %s".formatted(clazz.getName(), abstractMethods));
            }
        }

        this.clazz = clazz;
    }

    @Override
    public Builder<T> storageEngine(Class<?> storageEngine) {
        this.storageEngine = storageEngine.asSubclass(StorageEngine.class);
        return this;
    }

    @Override
    public Builder<T> processLoops(int loops) {
        this.processLoops = loops;
        return this;
    }

    @Override
    public Builder<T> singletons(Object... singletons) {
        if (this.singletons == null) {
            this.singletons = new HashMap<>();
        }

        for (var singleton : singletons) {
            if (this.singletons.put(singleton.getClass(), singleton) != null) {
                throw new EcsWorldCreationException("Multiple singletons for class %s passed. Singletons must be unique".formatted(singleton.getClass()));
            }
        }

        return this;
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public T build() {
        var storageEngine = this.storageEngine != null ? StorageEngine.load(this.storageEngine) : StorageEngine.load();
        var world = new EngineWorld(this, storageEngine);

        // Associate world to storage engine
        storageEngine.setWorld(world);

        if (World.class.equals(clazz)) {
            return (T) world;
        }

        var dynamicWorld = DynamicWorldBuilder.createDynamicWorld(world, clazz);
        storageEngine.setProxiedWorld((StorageWorld) dynamicWorld);

        return dynamicWorld;
    }

}

class DynamicWorldBuilder {

    private record PluginData(Class<?> plugin, Class<?> implementation) {
    }

    @SuppressWarnings("unchecked")
    public static <T extends World> T createDynamicWorld(EngineWorld world, Class<T> clazz) {
        var plugins = gatherPlugins(clazz);
        if (plugins.isEmpty()) {
            var dynamicWorld = new ByteBuddy()
                    .subclass(Object.class)
                    .name(clazz.getName() + "$Proxy")
                    .implement(clazz)
                    .implement(StorageWorld.class)
                    .method(isMethodOf(World.class)).intercept(MethodDelegation.to(world))
                    .method(isMethodOf(StorageWorld.class)).intercept(MethodDelegation.to(world))
                    .make()
                    .load(DynamicWorldBuilder.class.getClassLoader())
                    .getLoaded();

            try {
                var proxy = (T) dynamicWorld.getDeclaredConstructor().newInstance();
                injectProxies(world, proxy);

                return proxy;
            } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException ex) {
                throw new EcsWorldCreationException("Failed to create dynamic World '%s': %s".formatted(clazz.getName(), ex.getMessage()), ex);
            }
        }

        return createDynamicPluginWorld(world, clazz, plugins);

    }

    @SuppressWarnings("unchecked")
    private static <T extends World> T createDynamicPluginWorld(EngineWorld world, Class<T> clazz, SequencedSet<PluginData> plugins) {
        // Create builder
        var builder = new ByteBuddy()
                .subclass(Object.class)
                .name(clazz.getName() + "$Proxy")
                .implement(clazz)
                .implement(StorageWorld.class);

        // Add world
        var definition = builder.method(isMethodOf(World.class)).intercept(MethodDelegation.to(world));
        definition = definition.method(isMethodOf(StorageWorld.class)).intercept(MethodDelegation.to(world));

        // Add plugins
        for (var plugin : plugins) {
            var implementation = instantiate(world, plugin);
            if (!plugin.plugin.isInstance(implementation)) {
                throw new EcsPluginException("Implementation '%s' for plugin '%s' does not implement plugin. Plugin must declare implementation that implements the plugin."
                        .formatted(implementation.getClass().getName(), plugin.plugin.getName()));
            }

            definition = definition.method(isMethodOf(plugin.plugin)).intercept(MethodDelegation.to(implementation));
        }

        // Create class 
        var dynamicWorld = definition
                .make()
                .load(DynamicWorldBuilder.class.getClassLoader())
                .getLoaded();

        // Instantiate class
        try {
            var proxy = (T) dynamicWorld.getDeclaredConstructor().newInstance();
            injectProxies(world, proxy);

            return proxy;
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException ex) {
            var pluginNames = plugins.stream().map(plugin -> plugin.plugin.getName()).collect(Collectors.joining(", "));

            throw new EcsWorldCreationException("Failed to create dynamic World '%s' with plugins '%s': %s".formatted(clazz.getName(), pluginNames, ex.getMessage()), ex);
        }
    }

    private static <T extends World> void injectProxies(EngineWorld world, T proxy) {
        injectProxy(world, proxy, "singletonManager", "world");
    }

    private static <T extends World> void injectProxy(Object target, T proxy, String path, String... paths) {
        try {
            var field = target.getClass().getDeclaredField(path);
            field.setAccessible(true);

            Object value = target;

            var i = 0;
            do {
                value = field.get(target);

                field = value.getClass().getDeclaredField(paths[i]);
                field.setAccessible(true);
            } while (++i < paths.length);

            if (!field.getType().isAssignableFrom(EngineWorld.class)) {
                var fullPath = path;
                for (var p : paths) {
                    fullPath += "." + p;
                }

                throw new EcsWorldCreationException("Cannot inject proxy into field '%s' of type '%s': Field type '%s' is not assignable from EngineWorld."
                        .formatted(fullPath, target.getClass().getName(), field.getType()));
            }

            field.set(value, proxy);
        } catch (NoSuchFieldException | IllegalAccessException ex) {
            var fullPath = path;
            for (var p : paths) {
                fullPath += "." + p;
            }

            System.err.println("Failed to inject proxied world into field '%s' of type '%s'".formatted(fullPath, target.getClass().getName()));
            ex.printStackTrace();
        }
    }

    private static Junction<ByteCodeElement> isMethodOf(Class<?> clazz) {
        return ElementMatchers.not(ElementMatchers.isDeclaredBy(Object.class)).and(ElementMatchers.isDeclaredBy(clazz).or(ElementMatchers.isDeclaredBy(ElementMatchers.isSuperTypeOf(clazz))));
    }

    private static Object instantiate(EngineWorld world, PluginData plugin) {
        try {
            return createInstance(world, plugin.plugin, plugin.implementation);
        } catch (InvocationTargetException ex) {
            var cause = ex.getTargetException() != null ? ex.getTargetException() : ex;

            throw new EcsWorldCreationException("Failed to instantiate plugin '%s' (implementation '%s'): %s"
                    .formatted(plugin.plugin.getName(), plugin.implementation.getName(), ex.getMessage()), cause);
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | NoSuchMethodException | SecurityException ex) {
            throw new EcsWorldCreationException("Failed to instantiate plugin '%s' (implementation '%s'): %s".formatted(plugin.plugin.getName(), plugin.implementation.getName(), ex.getMessage()), ex);
        }
    }

    private static Object createInstance(EngineWorld world, Class<?> plugin, Class<?> implementationClass)
            throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {

        // Try to find a constructor accepting EngineWorld or World
        for (var constructor : implementationClass.getConstructors()) {
            var parameters = constructor.getParameters();
            if (parameters.length == 1 && parameters[0].getType().isAssignableFrom(EngineWorld.class)) {
                return constructor.newInstance(world);
            }
        }

        // Try to find a default constructor
        try {
            return implementationClass.getDeclaredConstructor().newInstance();
        } catch (NoSuchMethodException e) {
            // No suitable constructors found
            throw new EcsPluginException("Implementation '%s' for plugin '%s' must have either a constructor accepting World/EngineWorld or a default constructor."
                    .formatted(implementationClass.getName(), plugin.getName()));
        }
    }

    private static SequencedSet<PluginData> gatherPlugins(Class<? extends World> clazz) {
        return gatherPlugins(clazz, new LinkedHashSet<>());
    }

    private static SequencedSet<PluginData> gatherPlugins(Class<?> clazz, SequencedSet<PluginData> result) {
        for (var superinterface : clazz.getInterfaces()) {
            var plugin = superinterface.getAnnotation(Plugin.class);
            if (plugin != null) {
                result.add(new PluginData(superinterface, plugin.value()));
            }

            gatherPlugins(superinterface, result);
        }

        return result;
    }

}