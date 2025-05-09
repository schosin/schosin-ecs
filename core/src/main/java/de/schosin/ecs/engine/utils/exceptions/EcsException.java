package de.schosin.ecs.engine.utils.exceptions;

public abstract class EcsException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public EcsException(String message, Throwable cause) {
        super(message, cause);
    }

    public EcsException(String message) {
        super(message);
    }

    public EcsException(Throwable cause) {
        super(cause);
    }

}
