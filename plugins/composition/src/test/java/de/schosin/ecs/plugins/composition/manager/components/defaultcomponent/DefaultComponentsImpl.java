package de.schosin.ecs.plugins.composition.manager.components.defaultcomponent;

import java.util.Objects;
import java.util.function.Supplier;

import de.schosin.ecs.api.components.mappers.Components;
import de.schosin.ecs.api.data.ComponentAccessor;
import de.schosin.ecs.api.data.DataAccessor;

public 
final class DefaultComponentsImpl<T> implements DefaultComponents<T>, ComponentAccessor<T> {
    private final Components<T, T> components;
    private final Supplier<T> defaultInstance;

    public DefaultComponentsImpl(Components<T, T> components, Supplier<T> defaultInstance) {
        this.components = components;
        this.defaultInstance = Objects.requireNonNull(defaultInstance, "defaultInstance cannot be null");
    }

    @Override
    public ComponentAccessor<T> getComponentAccessor(DataAccessor accessor) {
        return this;
    }

    @Override
    public T getComponent(DataAccessor accessor) {
        return get(accessor.entityId());
    }

    @Override
    public T get(int entityId) {
        var result = components.get(entityId);

        return result != null ? result : defaultInstance.get();
    }

    @Override
    public boolean has(int entityId) {
        throw new UnsupportedOperationException("irrelevant for test");
    }

    @Override
    public boolean remove(int entityId) {
        throw new UnsupportedOperationException("irrelevant for test");
    }

}
