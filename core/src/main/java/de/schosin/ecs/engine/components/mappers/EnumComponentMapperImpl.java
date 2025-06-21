package de.schosin.ecs.engine.components.mappers;

import org.jspecify.annotations.NonNull;

import de.schosin.ecs.api.components.mappers.ComponentMapper.EnumComponentMapper;
import de.schosin.ecs.api.components.types.ClassType;
import de.schosin.ecs.api.data.DataAccessor;
import de.schosin.ecs.storage.api.entities.Archetype;

public final class EnumComponentMapperImpl<T extends Enum<T>> implements EnumComponentMapper<T>, ComponentConverter.Factory<T> {

    private final ComponentMapperImpl<T> delegate;
    private final T defaultComponent;

    public EnumComponentMapperImpl(ComponentMapperImpl<T> delegate, T defaultComponent) {
        this.delegate = delegate;
        this.defaultComponent = defaultComponent;
    }

    @Override
    public int componentId() {
        return this.delegate.componentId();
    }

    @Override
    public ClassType<T> componentType() {
        return this.delegate.componentType();
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
    public T access(DataAccessor accessor) {
        return this.delegate.access(accessor);
    }

    @Override
    public ComponentConverter<T> getConverter(Archetype archetype) {
        return this.delegate.getConverter(archetype);
    }

    @Override
    public boolean remove(int entityId) {
        return this.delegate.remove(entityId);
    }

}