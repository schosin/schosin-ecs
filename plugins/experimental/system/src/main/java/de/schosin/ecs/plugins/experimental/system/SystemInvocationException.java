package de.schosin.ecs.plugins.experimental.system;

import de.schosin.ecs.plugins.experimental.system.systems.BaseSystem;

public class SystemInvocationException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public SystemInvocationException(Object id, BaseSystem target, Throwable cause) {
        super(format(id, target, cause), cause);
    }

    private static String format(Object id, BaseSystem target, Throwable cause) {
        if (target == null) {
            return "System invocation for group %s failed: %s".formatted(id, cause.getMessage());
        }

        return "System invocation for group %s (system: %s) failed: %s".formatted(id, target, cause.getMessage());
    }

}
