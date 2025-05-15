package de.schosin.ecs.storage.api;

public class StorageEngineException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public StorageEngineException(String message, Throwable cause) {
        super(message, cause);
    }

    public StorageEngineException(String message) {
        super(message);
    }

    public StorageEngineException(Throwable cause) {
        super(cause);
    }

}
