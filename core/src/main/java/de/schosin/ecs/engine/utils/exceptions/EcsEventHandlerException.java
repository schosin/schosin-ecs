package de.schosin.ecs.engine.utils.exceptions;

import de.schosin.ecs.engine.events.EventHandler;

public class EcsEventHandlerException extends EcsException {
    private static final long serialVersionUID = 1L;

    private final EventHandler<?> eventHandler;

    public EcsEventHandlerException(EventHandler<?> eventHandler, String message) {
        super(message);

        this.eventHandler = eventHandler;
    }

    public EventHandler<?> getEventHandler() {
        return this.eventHandler;
    }

}
