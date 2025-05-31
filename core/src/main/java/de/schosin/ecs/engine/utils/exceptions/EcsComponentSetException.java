package de.schosin.ecs.engine.utils.exceptions;

import de.schosin.ecs.api.components.ComponentSet;

public class EcsComponentSetException extends EcsException {
    private static final long serialVersionUID = 1L;

    private final Class<? extends ComponentSet<?>> componentSet;

    public EcsComponentSetException(Class<? extends ComponentSet<?>> componentSet, String message) {
        super(message);

        this.componentSet = componentSet;
    }

    public EcsComponentSetException(Class<? extends ComponentSet<?>> componentSet, String message, Throwable cause) {
        super(message, cause);

        this.componentSet = componentSet;
    }

    public Class<? extends ComponentSet<?>> getComponentSet() {
        return this.componentSet;
    }

}
