package de.schosin.ecs.engine.components.mappers;

import de.schosin.ecs.api.components.mappers.ComponentMapper;
import de.schosin.ecs.api.components.types.ClassType;
import de.schosin.ecs.api.data.DataAccessor;
import de.schosin.ecs.engine.components.TransmutationManager;
import de.schosin.ecs.storage.api.components.Component.ClassComponent;
import de.schosin.ecs.storage.api.entities.Archetype;

public class ComponentMapperImpl<T> implements ComponentMapper<T>, ComponentConverter.Factory<T> {

    protected final ClassComponent<T> data;
    protected final int componentId;
    protected final ClassType<T> componentType;

    private final TransmutationManager.Add<T> add;
    private final TransmutationManager.Remove remove;

    public ComponentMapperImpl(ClassComponent<T> data, TransmutationManager transmutationManager) {
        this.data = data;
        this.componentId = data.id();
        this.componentType = data.type();

        this.add = transmutationManager.getAddTransmuter(data.type());
        this.remove = transmutationManager.getRemoveTransmuter(data.type());
    }

    @Override
    public final int componentId() {
        return componentId;
    }

    @Override
    public final ClassType<T> componentType() {
        return componentType;
    }

    @Override
    public final boolean has(int entityId) {
        return data.hasComponent(entityId);
    }

    @Override
    public final T add(int entityId, T component) {
        this.add.apply(entityId, component);

        return component;
    }

    @Override
    public final T get(int entityId) {
        return data.getComponent(entityId);
    }

    @Override
    public final T access(DataAccessor accessor) {
        return accessor.<T>getComponent(componentId);
    }

    @Override
    public final boolean remove(int entityId) {
        return this.remove.apply(entityId);
    }

    @Override
    public final ComponentConverter<T> getConverter(Archetype archetype) {
        var index = archetype.getComponentIndex(componentId);
        return index > -1
                ? ComponentConverter.indexed(index)
                : ComponentConverter.pending(componentId);
    }

}
