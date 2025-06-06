package de.schosin.ecs.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.jspecify.annotations.Nullable;

/**
 * Annotation to declare an interface as a plugin, providing its factory
 * or implementation in the {@link #value()} attribute.
 * 
 * <p>
 * A plugin can be used by creating an interface extending {@link World}.
 * To declare a dependency on another plugin, declare it as a constructor
 * argument in the factory or implementation class.
 * </p>
 * 
 * <p>
 * <b>Example</b>
 * 
 * {@snippet:
 *  interface MyWorld extends World, MyPlugin {
 *  }
 * }
 * 
 * This custom world can be passed to {@link World#builder(Class)} to create an
 * instance of this world. 
 * </p>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Plugin {

    /**
     * Implementation class of the plugin interface, or a class implementing {@link Factory}.
     */
    Class<?> value();

    /**
     * Factory of a plugin, providing a way to provide a different implementation based
     * on the {@link World}, other availabl plugins or the storage implementation.
     */
    interface Factory {

        /**
         * Return an instance of the plugin. 
         */
        Object getPlugin();

    }

    /**
     * Marker interface for configuration objects. Implementations of plugins
     * accepting objects of that type will retrieve an instance added via
     * {@link World.Builder#configure(PluginConfig)}.
     * 
     * <p>
     * To mark a config object as optional, annotate the parameter with {@link Nullable}
     * or any other annotation with that exact {@link Class#getSimpleName() simple name}.
     * </p>
     * 
     * <p>
     * Missing configurations will produce an approriate error for the user.
     * </p>
     */
    interface PluginConfig {
    }

}
