package de.schosin.ecs.plugins.events;

import de.schosin.ecs.api.Plugin;
import de.schosin.ecs.engine.events.EventHandler;

@Plugin(EventPluginImpl.class)
public interface EventPlugin {

    /**
     * Dispatches the event.
     * 
     * @param event event to dispatch
     * @return true if any handlers were invoked
     */
    boolean dispatchEvent(Object event);

    /**
     * Registers the event handler. The passed handler must be an actual
     * class implementing {@link EventHandler}. 
     * The type argument for {@link EventHandler} must not be a generic type
     * parameter. Use {@link #registerEventHandler(Class, EventHandler)} otherwise.
     *   
     * @param <T> type of event
     * @param handler event handler
     */
    <T> void registerEventHandler(EventHandler<T> handler);

    /**
     * Registers the event handler.
     * 
     * @param <T> type of event
     * @param eventType class of event, must not be generic
     * @param handler event handler
     */
    <T> void registerEventHandler(Class<T> eventType, EventHandler<T> handler);

}
