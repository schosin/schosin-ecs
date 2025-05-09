package de.schosin.ecs.engine.utils.exceptions;

public class EcsPluginException extends EcsWorldCreationException {
    private static final long serialVersionUID = 1L;

    public EcsPluginException(String message, Throwable cause) {
        super(message, cause);
    }

    public EcsPluginException(String message) {
        super(message);
    }

}
