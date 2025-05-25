package de.schosin.ecs.engine.components.mappers;

import de.schosin.ecs.api.components.mappers.ComponentMapper;
import de.schosin.ecs.engine.components.TransmutationManager;
import de.schosin.ecs.storage.api.components.Component.ClassComponent;

public class ComponentMapperImpl<T> implements ComponentMapper<T> {

    protected final ClassComponent<T> data;

    private final TransmutationManager.Add<T> add;
    private final TransmutationManager.Remove remove;

    public ComponentMapperImpl(ClassComponent<T> data, TransmutationManager transmutationManager) {
        this.data = data;

        this.add = transmutationManager.getAddTransmuter(data.type());
        this.remove = transmutationManager.getRemoveTransmuter(data.type());
    }

    @Override
    public boolean has(int entityId) {
        return data.hasComponent(entityId);
    }

    @Override
    public T add(int entityId, T component) {
        this.add.apply(entityId, component);

        return component;
    }

    @Override
    public T get(int entityId) {
        return data.getComponent(entityId);
    }

    @Override
    public boolean remove(int entityId) {
        return this.remove.apply(entityId);
    }

}
