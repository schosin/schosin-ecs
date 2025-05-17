package de.schosin.ecs.storage.defaultimpl;

import java.util.function.Consumer;

import com.google.auto.service.AutoService;

import de.schosin.ecs.api.Pooled;
import de.schosin.ecs.api.components.ComponentType;
import de.schosin.ecs.api.components.ComponentType.ClassType;
import de.schosin.ecs.api.components.ComponentType.ComponentRelationType;
import de.schosin.ecs.api.components.ComponentType.ExclusiveComponentRelationType;
import de.schosin.ecs.api.components.ComponentType.RegularComponentType;
import de.schosin.ecs.api.components.Relation.Exclusive;
import de.schosin.ecs.storage.api.ComponentStorage;
import de.schosin.ecs.storage.api.StorageEngine;
import de.schosin.ecs.storage.api.StorageWorld;
import de.schosin.ecs.storage.api.components.Component;
import de.schosin.ecs.storage.api.components.Component.ClassComponent;
import de.schosin.ecs.storage.api.components.Component.ComponentRelationData;
import de.schosin.ecs.storage.api.components.Component.ExclusiveComponentRelationData;
import de.schosin.ecs.storage.api.components.Component.PooledComponentData;
import de.schosin.ecs.utils.collections.ImmutableBag;

@AutoService(StorageEngine.class)
public class DefaultStorageEngine implements StorageEngine {

    private ComponentStorage componentStorage;

    @Override
    public void setWorld(StorageWorld world) {
        this.componentStorage = new ComponentStorageImpl(world);
    }

    @Override
    public Component<?, ?> getComponent(int componentId) {
        return this.componentStorage.getComponent(componentId);
    }

    @Override
    public <T> ClassComponent<T> getComponent(ClassType<T> type, Consumer<RegularComponentType<?, ?>> validate) {
        return this.componentStorage.getComponent(type, validate);
    }

    @Override
    public <T extends Pooled> PooledComponentData<T> getPooledComponent(ClassType<T> type, Consumer<RegularComponentType<?, ?>> validate) {
        return this.componentStorage.getPooledComponent(type, validate);
    }

    @Override
    public <R, T> ComponentRelationData<R, T> getComponent(ComponentRelationType<R, T> type, Consumer<RegularComponentType<?, ?>> validate) {
        return this.componentStorage.getComponent(type, validate);
    }

    @Override
    public <R extends Exclusive, T> ExclusiveComponentRelationData<R, T> getComponent(ExclusiveComponentRelationType<R, T> type, Consumer<RegularComponentType<?, ?>> validate) {
        return this.componentStorage.getComponent(type, validate);
    }

    @Override
    public ImmutableBag<Component<?, ?>> getComponents() {
        return this.componentStorage.getComponents();
    }

    @Override
    public <T> ImmutableBag<Component<? extends T, ?>> getComponents(ComponentType<T, ?> bound) {
        return this.componentStorage.getComponents(bound);
    }

}
