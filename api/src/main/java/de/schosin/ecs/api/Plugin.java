package de.schosin.ecs.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.jspecify.annotations.Nullable;

/**
 * Annotation to declare an interface as a plugin, providing its implementation
 * in the {@link #value()} attribute.
 * 
 * <p>
 * A plugin can be used by creating an interface extending {@link World} as well as
 * any number of plugin interfaces.
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
     * Implementation class of the plugin interface. Will be constructed reflectivly
     * 
     * @return
     */
    Class<?> value();

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
