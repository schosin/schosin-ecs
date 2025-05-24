package de.schosin.ecs.storage.defaultimpl;

import java.util.function.Consumer;

import com.google.auto.service.AutoService;

import de.schosin.ecs.api.Pooled;
import de.schosin.ecs.api.components.Relation.Exclusive;
import de.schosin.ecs.api.components.types.ClassType;
import de.schosin.ecs.api.components.types.ComponentType;
import de.schosin.ecs.api.components.types.ComponentType.RegularComponentType;
import de.schosin.ecs.api.components.types.RelationComponentType.ComponentRelationType;
import de.schosin.ecs.api.components.types.RelationComponentType.EntityRelationType;
import de.schosin.ecs.api.components.types.RelationComponentType.ExclusiveComponentRelationType;
import de.schosin.ecs.api.components.types.RelationComponentType.ExclusiveEntityRelationType;
import de.schosin.ecs.storage.api.ComponentStorage;
import de.schosin.ecs.storage.api.StorageEngine;
import de.schosin.ecs.storage.api.StorageWorld;
import de.schosin.ecs.storage.api.components.Component;
import de.schosin.ecs.storage.api.components.Component.ClassComponent;
import de.schosin.ecs.storage.api.components.Component.ComponentRelationData;
import de.schosin.ecs.storage.api.components.Component.EntityRelationData;
import de.schosin.ecs.storage.api.components.Component.ExclusiveComponentRelationData;
import de.schosin.ecs.storage.api.components.Component.ExclusiveEntityRelationData;
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
    public <R> EntityRelationData<R> getComponent(EntityRelationType<R> type, Consumer<RegularComponentType<?, ?>> validate) {
        return this.componentStorage.getComponent(type, validate);
    }

    @Override
    public <R extends Exclusive> ExclusiveEntityRelationData<R> getComponent(ExclusiveEntityRelationType<R> type, Consumer<RegularComponentType<?, ?>> validate) {
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
