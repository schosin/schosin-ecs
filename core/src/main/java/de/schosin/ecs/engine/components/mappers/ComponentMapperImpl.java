package de.schosin.ecs.engine.components.mappers;

import de.schosin.ecs.api.components.mappers.ComponentMapper;
import de.schosin.ecs.api.components.types.ClassType;
import de.schosin.ecs.api.data.ComponentAccessor;
import de.schosin.ecs.api.data.DataAccessor;
import de.schosin.ecs.engine.components.TransmutationManager;
import de.schosin.ecs.engine.components.mappers.accessors.IndexedAccessorImpl;
import de.schosin.ecs.engine.components.mappers.accessors.PendingAccessorImpl;
import de.schosin.ecs.storage.api.components.Component.ClassComponent;
import de.schosin.ecs.storage.api.entities.ArchetypeAccessor;
import de.schosin.ecs.utils.collections.Pool;

public class ComponentMapperImpl<T> implements ComponentMapper<T> {

    protected final ClassComponent<T> data;
    protected final int componentId;
    protected final ClassType<T> componentType;

    private final TransmutationManager.Add<T> add;
    private final TransmutationManager.Remove remove;

    private final Pool<PendingAccessorImpl<T>> pendingAccessors;

    public ComponentMapperImpl(ClassComponent<T> data, TransmutationManager transmutationManager) {
        this.data = data;
        this.componentId = data.id();
        this.componentType = data.type();

        this.add = transmutationManager.getAddTransmuter(data);
        this.remove = transmutationManager.getRemoveTransmuter(componentType);

        this.pendingAccessors = Pool.unbounded(PendingAccessorImpl.class, this::createPendingAccessor);
    }

    private PendingAccessorImpl<T> createPendingAccessor() {
        return new PendingAccessorImpl<>(data.id(), pendingAccessors);
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
    public ComponentAccessor<T> getComponentAccessor(DataAccessor accessor) {
        if (!(accessor instanceof ArchetypeAccessor archetypeAccessor)) {
            throw new IllegalArgumentException("Unexpected IterableAccessor not implementing ArchetypeAccessor: " + accessor);
        }

        var archetype = archetypeAccessor.getArchetype();
        var index = archetype.getComponentIndex(componentId);

        return index > -1
                ? IndexedAccessorImpl.getInstance(index)
                : pendingAccessors.getInstance();
    }

    @Override
    public final boolean remove(int entityId) {
        return this.remove.apply(entityId);
    }

}
