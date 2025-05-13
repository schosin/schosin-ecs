package de.schosin.ecs.plugins.events;

import de.schosin.ecs.api.World;
import de.schosin.ecs.engine.events.EventHandler;
import de.schosin.ecs.engine.events.EventManager;

public class EventPluginImpl implements EventPlugin {

    private final EventManager eventManager;

    public EventPluginImpl(World world) {
        this.eventManager = world.getSingleton(EventManager.class);
    }

    @Override
    public boolean dispatchEvent(Object event) {
        return this.eventManager.dispatchEvent(event);
    }

    @Override
    public <T> void registerEventHandler(EventHandler<T> handler) {
        this.eventManager.registerEventHandler(handler);
    }

    @Override
    public <T> void registerEventHandler(Class<T> eventType, EventHandler<T> handler) {
        this.eventManager.registerEventHandler(eventType, handler);
    }

}
