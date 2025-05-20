package de.schosin.ecs.engine;

import java.lang.reflect.Constructor;
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
        public PluginData {
            if (!plugin.isAssignableFrom(implementation)) {
                throw new EcsPluginException("Implementation '%s' for plugin '%s' does not implement plugin. Plugin must declare implementation that implements the plugin."
                        .formatted(implementation.getName(), plugin.getName()));
            }
        }

        @Override
        public String toString() {
            return new StringBuilder().append(plugin.getSimpleName()).append("(").append(implementation.getName()).append(")").toString();
        }
    }

    private record PluginInstance(Class<?> plugin, Object instance) {
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

        // Instantiate plugins
        var instantiatedPlugins = instantiatePlugins(world, plugins);

        // Add plugins to definition
        for (var plugin : instantiatedPlugins) {
            definition = definition.method(isMethodOf(plugin.plugin)).intercept(MethodDelegation.to(plugin.instance));
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

    private static SequencedSet<PluginInstance> instantiatePlugins(EngineWorld world, SequencedSet<PluginData> plugins) {
        var work = new LinkedHashSet<>(plugins);
        var instances = new HashMap<Class<?>, Object>();

        instantiatePlugins(world, work, instances);

        var result = new LinkedHashSet<PluginInstance>();

        for (var plugin : plugins) {
            var instance = instances.get(plugin.plugin);
            if (instance == null) {
                throw new EcsWorldCreationException("Failed to instantiate plugin '%s'".formatted(plugin));
            }

            result.add(new PluginInstance(plugin.plugin, instance));
        }

        return result;
    }

    private static void instantiatePlugins(EngineWorld world, LinkedHashSet<PluginData> plugins, HashMap<Class<?>, Object> instances) {
        while (!plugins.isEmpty()) {
            var plugin = plugins.removeFirst();
            var instance = instantiatePlugin(world, plugin, plugins, instances);

            instances.put(plugin.plugin, instance);
            instances.put(plugin.implementation, instance);
        }
    }

    private static Object instantiatePlugin(EngineWorld world, PluginData plugin, LinkedHashSet<PluginData> pendingPlugins, HashMap<Class<?>, Object> instances) {
        var constructors = plugin.implementation.getConstructors();
        if (constructors.length != 1) {
            throw new EcsWorldCreationException("Failed to instantiate plugin '%s': Declares %d public constructors, must be exactly one.".formatted(plugin, constructors.length));
        }

        var constructor = constructors[0];
        var parameters = constructor.getParameters();

        // Default constructor
        if (parameters.length == 0) {
            return instantiate(plugin, constructor);
        }

        // Only world argument
        if (parameters.length == 1 && parameters[0].getType().isAssignableFrom(EngineWorld.class)) {
            return instantiate(plugin, constructor, world);
        }

        // Analyze parameters
        var arguments = new Object[parameters.length];
        for (int i = 0, s = parameters.length; i < s; i++) {
            var parameterType = parameters[i].getType();

            // EngineWorld argument
            if (parameterType.isAssignableFrom(EngineWorld.class)) {
                arguments[i] = world;
                continue;
            }

            // Plugin dependency
            var dependency = instances.get(parameterType);
            if (dependency != null) {
                arguments[i] = dependency;
                continue;
            }

            // Plugin dependency not yet instantiated
            var pendingDependency = pendingPlugins.stream().filter(pending -> pending.plugin == parameterType).findFirst().orElse(null);
            if (pendingDependency != null) {
                pendingPlugins.remove(pendingDependency);

                var instance = arguments[i] = instantiatePlugin(world, pendingDependency, pendingPlugins, instances);

                instances.put(parameterType, instance);
                instances.put(pendingDependency.implementation, instance);

                continue;
            }

            // Plugin not declared by world
            var pluginAnnotation = parameterType.getAnnotation(Plugin.class);
            if (pluginAnnotation != null) {
                var pluginDependency = new PluginData(parameterType, pluginAnnotation.value());
                var instance = arguments[i] = instantiatePlugin(world, pluginDependency, pendingPlugins, instances);

                instances.put(parameterType, instance);
                instances.put(pluginAnnotation.value(), instance);

                continue;
            }

            // Unknown dependency
            throw new EcsWorldCreationException("Plugin '%s' declared parameter of unsupported type '%s'. Only World/EngineWorld and other plugins (interface only) supported."
                    .formatted(plugin, parameterType.getName()));
        }

        return instantiate(plugin, constructor, arguments);
    }

    private static Object instantiate(PluginData plugin, Constructor<?> constructor, Object... arguments) {
        try {
            return constructor.newInstance(arguments);
        } catch (InvocationTargetException ex) {
            var cause = ex.getTargetException() != null ? ex.getTargetException() : ex;

            throw new EcsWorldCreationException("Failed to instantiate plugin '%s' due to %s: %s".formatted(plugin, cause.getClass().getSimpleName(), cause.getMessage()), cause);
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | SecurityException ex) {
            throw new EcsWorldCreationException("Failed to instantiate plugin '%s': %s".formatted(plugin, ex.getMessage()), ex);
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