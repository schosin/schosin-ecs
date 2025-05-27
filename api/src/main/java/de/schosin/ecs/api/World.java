package de.schosin.ecs.api;

import java.lang.reflect.InvocationTargetException;
import java.util.ServiceLoader;
import java.util.ServiceLoader.Provider;

import org.jspecify.annotations.NonNull;

import de.schosin.ecs.api.Plugin.PluginConfig;
import de.schosin.ecs.api.components.mappers.Components;

public interface World extends Components.Creator {

    String DEFAULT_IMPLEMENTATION = "de.schosin.ecs.engine.WorldBuilder";

    static World.Builder<World> builder() {
        return builder(World.class);
    }

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
        Builder<T> storageEngine(Class<?> storageEngine);

        /**
         * Default loop count used by {@link World#process()} when delegating to {@link World#process(int)}.
         * 
         * @param loops default value
         * @return this instance
         */
        Builder<T> processLoops(int loops);

        /**
         * Singletons to add to this world. Can be retrieved by systems with
         * {@link World#getSingleton(Class)}.
         * 
         * @param singletons singletons to add
         * @return this instance
         */
        Builder<T> singletons(Object... singletons);

        Builder<T> configure(PluginConfig... configs);

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
