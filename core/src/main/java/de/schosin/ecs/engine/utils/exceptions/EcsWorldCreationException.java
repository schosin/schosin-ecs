package de.schosin.ecs.engine.utils.exceptions;

public class EcsWorldCreationException extends EcsException {
    private static final long serialVersionUID = 1L;

    public EcsWorldCreationException(String message) {
        super(message);
    }

    public EcsWorldCreationException(String message, Throwable cause) {
        super(message, cause);
    }

}
