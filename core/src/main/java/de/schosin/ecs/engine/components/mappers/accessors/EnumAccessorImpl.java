package de.schosin.ecs.engine.components.mappers.accessors;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import de.schosin.ecs.api.data.ComponentAccessor;
import de.schosin.ecs.api.data.DataAccessor;

@SuppressWarnings("rawtypes")
public record EnumAccessorImpl(Enum<?> value) implements ComponentAccessor {

    private static final Map<Enum<?>, EnumAccessorImpl> INSTANCES = new ConcurrentHashMap<>();

    public EnumAccessorImpl {
        Objects.requireNonNull(value, "value cannot be null");

    }

    @SuppressWarnings("unchecked")
    public static <R> ComponentAccessor<R> getInstance(Enum<?> value) {
        return INSTANCES.computeIfAbsent(value, EnumAccessorImpl::new);
    }

    @Override
    public Object getComponent(DataAccessor accessor) {
        return value;
    }

}
