package de.schosin.ecs.api.data;

/**
 * Interface for accessing components for an entity.
 */
public interface DataAccessor extends AutoCloseable {

    /**
     * Returns the id of the entity.
     */
    int entityId();

    boolean hasComponent(int componentId);

    /**
     * Retrieves the component given its id.
     * 
     * @param <R> type of component
     * @param componentId id of component
     * @return component or null
     */
    <R> R getComponent(int componentId);

    /**
     * INTERNAL API! Not intended for regular use.
     */
    <R> R getComponentByIndex(int componentIndex);

    /**
     * INTERNAL API! Not intended for regular use.
     */
    <R> R getPendingComponent(int componentId);

    /**
     * Returns true if this accessor is valid and can be used. Accessors will be invalidated by calling {@link #free()}.
     */
    boolean isValid();
    
    /**
     * Frees this accessor instance. Must be called after it is not needed anymore 
     * by the code that retrieved this instance.
     * 
     * <p>
     * <b>Warning:</b> Only call this method where the accessor was retrieved. 
     * DO NOT free an accessor that was passed in via a parameter or similar.
     * </p>
     * 
     * <p>
     * Alternatively use {@code try-with-resources}:
     * 
     * {@snippet:
     * try (var accessor = ...) {
     *     // use accessor
     * }
     * }
     * </p>
     */
    void free();

    @Override
    default void close() {
        free();
    }

}
