package de.schosin.ecs.api;

import java.lang.reflect.InvocationTargetException;
import java.util.ServiceLoader;
import java.util.ServiceLoader.Provider;

import org.jspecify.annotations.NonNull;

import de.schosin.ecs.api.Plugin.PluginConfig;
import de.schosin.ecs.api.components.mappers.Components;

/**
 * Base API for working with entities and components.
 * 
 * <p>
 * To create an instance of {@link World} or an interface extending {@link World},
 * use {@link World#builder()} and {@link World#builder(Class)} respectively to retrieve
 * a builder instance. Using {@link Builder#build()} will create an instance of the API.
 * </p>
 * 
 * <p>
 * {@link World#builder(Class)} supports extending the API of {@link World} by creating
 * a custom interface that extends {@link World} as well as plugin interfaces. As a starting
 * point {@link de.schosin.ecs.worlds.DefaultWorld DefaultWorld} from the artifact "{@code de.schosin.ecs:ecs-worlds}"
 * can be used, which includes a default set of plugins that extend the functionality with
 * ways to create and mutate entities (Archetype and Transmuter), query entities (Composition)
 * among other things.
 * </p>
 */
public interface World extends Components.Creator {

    String DEFAULT_IMPLEMENTATION = "de.schosin.ecs.engine.WorldBuilder";

    /**
     * Creates a builder for the base {@link World} API without any plugins.
     * 
     * @return builder for base {@link World}
     */
    static World.Builder<World> builder() {
        return builder(World.class);
    }

    /**
     * Creates a builder for a custom world interface.
     * 
     * <p>
     * The custom world must be an interface, it must extend {@link World} and it
     * must not have any abstract methods itself. The custom world interface may
     * extend plugin interfaces, which have to be annotated with {@link Plugin},
     * pointing to the implementation of the plugin. 
     * </p>
     * 
     * <p>
     * For an example of a custom world, see {@link de.schosin.ecs.worlds.DefaultWorld DefaultWorld} 
     * from the artifact "{@code de.schosin.ecs:ecs-worlds}".
     * </p>
     * 
     * @param <T> type of world
     * @param clazz class of world
     * @return builder for custom world
     */
    @SuppressWarnings("unchecked")
    static <T extends World> World.Builder<T> builder(Class<T> clazz) {
        try {
            return (Builder<T>) Class.forName(DEFAULT_IMPLEMENTATION).getDeclaredConstructor(Class.class).newInstance(clazz);
        } catch (InvocationTargetException ex) {
            if (ex.getTargetException() != null && ex.getTargetException().getClass().getName().startsWith("de.schosin")) {
                throw (RuntimeException) ex.getTargetException();
            }

            throw new IllegalArgumentException("Exception creating builder '%s' instance with class '%s': %s".formatted(DEFAULT_IMPLEMENTATION, clazz, ex.getMessage()), ex);
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | ClassNotFoundException ex) {
            throw new IllegalArgumentException("Exception creating builder '%s' instance with class '%s': %s".formatted(DEFAULT_IMPLEMENTATION, clazz, ex.getMessage()), ex);
        }
    }

    interface Builder<T extends World> {

        /**
         * Override the storage engine used by this world. The storage engine
         * will be loaded via {@link ServiceLoader}.
         * 
         * <p>
         * If this method is not called, a single {@link Provider} is expected
         * to be returned by {@link ServiceLoader#stream()}. If multiple storage
         * engines are present on the classpath, use this method to set the engine to
         * use.
         * </p>
         * 
         * @param storageEngine class of storage engine, must implement StorageEngine
         * @return this instance
         * @throws ClassCastException if class does not implement StorageEngine
         */
        default Builder<T> storageEngine(Class<?> storageEngine) {
            return storageEngine(storageEngine, null);
        }

        /**
         * Override the storage engine used by this world. The storage engine
         * will be loaded via {@link ServiceLoader}.
         * 
         * <p>
         * See the documentation for the used storag engine to see if a configuration
         * object is required or supported.
         * </p>
         * 
         * <p>
         * If this method is not called, a single {@link Provider} is expected
         * to be returned by {@link ServiceLoader#stream()}. If multiple storage
         * engines are present on the classpath, use this method to set the engine to
         * use.
         * </p>
         * 
         * @param storageEngine class of storage engine, must implement StorageEngine
         * @param config configuration object for storage engine
         * @return this instance
         * @throws ClassCastException if class does not implement StorageEngine
         */
        Builder<T> storageEngine(Class<?> storageEngine, Object config);

        /**
         * Configure the maximum count of active entities this world will hold at its peak.
         * 
         * <p>
         * Configuring this value will increase memory usage if the actual number is lower,
         * but can provide better performance at runtime.
         * </p>
         *  
         * @param count expected maximum number of active entities
         * @return this instance
         */
        Builder<T> expectedEntities(int count);

        /**
         * Default loop count used by {@link World#process()} when delegating to {@link World#process(int)}.
         * 
         * @param loops default value
         * @return this builder
         */
        Builder<T> processLoops(int loops);

        /**
         * Singletons to add to this world. Can be retrieved by systems with
         * {@link World#getSingleton(Class)}.
         * 
         * @param singletons singletons to add
         * @return this builder
         */
        Builder<T> singletons(Object... singletons);

        /**
         * Adds a configuration object for a plugin. See the documentation of plugins
         * on if and how to configure it.
         * 
         * @param configs configuration object
         * @return this builder
         */
        Builder<T> configure(PluginConfig... configs);

        /**
         * Creates an instance of the world.
         * 
         * @return instance of world
         */
        T build();

    }

    /**
     * Creates an entity with the given components. 
     * 
     * <p>
     * The returned int is the id of the entity, which is unique during the lifetime of this entity.
     * The id may be reused after this entity has been {@link #deleteEntity(int) deleted}.
     * </p>
     * 
     * @param components components to add to the entity
     * 
     * @return id of the entity
     */
    int createEntity(Object... components);

    /**
     * Marks an entity for deletion. 
     * 
     * <p>
     * The entity will be deleted during the {@link #process()} call. 
     * Interested {@link Composition compositions} will be notified after the deletion.
     * </p>
     *  
     * @param entityId
     */
    void deleteEntity(int entityId);

    /**
     * Returns true if the entity with the given id is active.
     * 
     * @param entityId id of entity
     * @return true if active
     */
    boolean isActive(int entityId);

    /**
     * Adds a singleton to this world. Can be used when the singleton requires functionality of World.
     * 
     * <p>
     * Prefer {@link Builder#singletons(Object...)} instead if the singleton can be created
     * without the world.
     * </p>
     * 
     * @param <T> type of singleton
     * @param singleton singleton to add
     * @return singleton instance
     */
    <T> T addSingleton(@NonNull T singleton);

    /**
     * Adds a singleton to this world. The singleton will be available only via {@code clazz}.
     * 
     * @param <T> type of singleton
     * @param clazz class, superclass or superinterface of singleton
     * @param singleton singleton to add
     * @return singleton instance
     */
    <T> T addSingleton(@NonNull Class<? super T> clazz, @NonNull T singleton);

    /**
     * Returns a singleton instance of the given class. Can be used to share state
     * between systems and other code without passing every singleton around manually.
     * 
     * <p>
     * The singleton instance can be supplied with {@link World.Builder#addSingletons(Object...)} or 
     * {@link World#addSingleton(Object)}.
     * If no singleton is found, a new instance will be created via reflection, requiring a public
     * default constructor or a constructor accepting only a {@link World} argument.
     * If that fails, a {@link UnsupportedOperationException} is thrown. 
     * <b>That should be treated as an error to fix and not to catch</b>.
     * </p>
     * 
     * @param <T> type of singleton
     * @param clazz class of singleton
     * @return singleton instance
     * @throws UnsupportedOperationException if singleton not found and creation failed
     */
    @NonNull
    <T> T getSingleton(@NonNull Class<T> clazz) throws UnsupportedOperationException;

    /**
     * Processes deletions of entities and removals of components. Depending on the implementation additional work may be done in this step.
     * 
     * <p>
     * This method should be called either at the end of a game loop or in between systems. When the processing caused additional changes,
     * this method may return false and the call can either be repeated immedietly or run next time (e.g. next frame). This can happen when
     * {@link Composition#inserted(java.util.function.IntConsumer) inserted} and {@link Composition#removed(java.util.function.IntConsumer) removed}
     * callbacks trigger changes for another composition. 
     * </p>
     * 
     * <p>
     * <b>Attention:</b> If this method is called while another thread is {@link Composition#process(java.util.function.IntConsumer) processing entities},
     * entities might be skipped or the callback might see zeros (0) for the entity id.
     * </p>
     * 
     * @return true if processing finished, false indicates further processing is necessary
     */
    boolean process();

    /**
     * Processes deletions of entities and removals of components. Depending on the implementation additional work may be done in this step.
     * 
     * <p>
     * This method should be called either at the end of a game loop or in between systems. When the processing caused additional changes,
     * this method may return false and the call can either be repeated immedietly or run next time (e.g. next frame). This can happen when
     * {@link Composition#inserted(java.util.function.IntConsumer) inserted} and {@link Composition#removed(java.util.function.IntConsumer) removed}
     * callbacks trigger changes for another composition. 
     * </p>
     * 
     * <p>
     * <b>Attention:</b> If this method is called while another thread is {@link Composition#process(java.util.function.IntConsumer) processing entities},
     * entities might be skipped or the callback might see zeros (0) for the entity id.
     * </p>
     * 
     * @param loops number of times the processing should be repeated if iterations require further processing
     * @return true if processing finished, false indicates further processing is necessary
     */
    boolean process(int loops);

    /**
     * Processes the composition updates for the entity. Should only be used if absolutely necessary, as the same work is done
     * in {@link #process()}.
     * 
     * <p>
     * When an entity is modified by adding or removing components, its composition update and consequently {@link Composition#inserted(java.util.function.IntConsumer)}
     * and {@link Composition#removed(java.util.function.IntConsumer)} calls will be delayed until {@link #process()}. 
     * If that entity is deleted before the next {@link #process()} call, the these calls won't be performed. If these calls are required to clean up some data
     * otherwise left behind, this method can be called to flush the composition updates immedietly after adding or removing components.
     * </p>
     * 
     * <p>
     * <b>Attention:</b> Make sure to only flush the updates if the composition has actually changed. If adding a component, the entity must not have had that component
     * already, and if removing a component, the entity must have had that component.
     * </p>
     * 
     * <p>
     * <b>Note:</b> This method only flushes composition updates, but does not remove components or deletes the entity. 
     * These will be performed at the next {@link #process()}.
     * </p>
     * 
     * @param entityId id of entity
     * @return true if processing finished, false indicates further processing is necessary
     */
    boolean flushEntityUpdates(int entityId);

}
