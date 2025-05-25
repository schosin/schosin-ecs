package de.schosin.ecs.engine.components.mappers;

import org.jspecify.annotations.NonNull;

import de.schosin.ecs.api.components.mappers.ComponentMapper;
import de.schosin.ecs.api.components.mappers.ComponentMapper.EnumComponentMapper;

public class EnumComponentMapperImpl<T extends Enum<T>> implements EnumComponentMapper<T> {

    private final ComponentMapper<T> delegate;
    private final T defaultComponent;

    public EnumComponentMapperImpl(ComponentMapper<T> delegate, T defaultComponent) {
        this.delegate = delegate;
        this.defaultComponent = defaultComponent;
    }

    @Override
    public @NonNull T add(int entityId) {
        return add(entityId, defaultComponent);
    }

    @Override
    public @NonNull T getDefault() {
        return defaultComponent;
    }

    @Override
    public boolean has(int entityId) {
        return this.delegate.has(entityId);
    }

    @Override
    public T add(int entityId, T component) {
        return this.delegate.add(entityId, component);
    }

    @Override
    public T get(int entityId) {
        return this.delegate.get(entityId);
    }

    @Override
    public boolean remove(int entityId) {
        return this.delegate.remove(entityId);
    }

}