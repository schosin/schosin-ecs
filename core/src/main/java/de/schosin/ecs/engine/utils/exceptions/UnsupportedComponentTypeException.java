package de.schosin.ecs.engine.utils.exceptions;

import de.schosin.ecs.api.components.ComponentType;

public class UnsupportedComponentTypeException extends Exception {
    private static final long serialVersionUID = 1L;

    private final ComponentType<?> componentType;

    public UnsupportedComponentTypeException(ComponentType<?> componentType, String message) {
        super(message);

        this.componentType = componentType;
    }

    public ComponentType<?> getComponentType() {
        return this.componentType;
    }

}
