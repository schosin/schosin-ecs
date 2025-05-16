package de.schosin.ecs.storage.defaultimpl;

import java.util.function.Consumer;

import com.google.auto.service.AutoService;

import de.schosin.ecs.api.Pooled;
import de.schosin.ecs.api.components.ComponentType;
import de.schosin.ecs.api.components.ComponentType.RegularComponentType;
import de.schosin.ecs.storage.api.ComponentStorage;
import de.schosin.ecs.storage.api.StorageEngine;
import de.schosin.ecs.storage.api.StorageWorld;
import de.schosin.ecs.storage.api.components.Component;
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
    public Component<?> getComponent(int componentId) {
        return this.componentStorage.getComponent(componentId);
    }

    @Override
    public <T> Component<T> getComponent(RegularComponentType<T> type, Consumer<RegularComponentType<?>> validate) {
        return this.componentStorage.getComponent(type, validate);
    }

    @Override
    public <T extends Pooled> PooledComponentData<T> getPooledComponent(RegularComponentType<T> type, Consumer<RegularComponentType<?>> validate) {
        return this.componentStorage.getPooledComponent(type, validate);
    }

    @Override
    public ImmutableBag<Component<?>> getComponents() {
        return this.componentStorage.getComponents();
    }

    @Override
    public <T> ImmutableBag<Component<? extends T>> getComponents(ComponentType<T> bound) {
        return this.componentStorage.getComponents(bound);
    }

}
