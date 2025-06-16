package de.schosin.ecs.storage.defaultimpl;

import java.util.function.Predicate;

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
import de.schosin.ecs.api.data.DataAccessor;
import de.schosin.ecs.storage.api.StorageEngine;
import de.schosin.ecs.storage.api.StorageWorld;
import de.schosin.ecs.storage.api.components.Component;
import de.schosin.ecs.storage.api.components.Component.ClassComponent;
import de.schosin.ecs.storage.api.components.Component.ComponentRelationData;
import de.schosin.ecs.storage.api.components.Component.EntityRelationData;
import de.schosin.ecs.storage.api.components.Component.ExclusiveComponentRelationData;
import de.schosin.ecs.storage.api.components.Component.ExclusiveEntityRelationData;
import de.schosin.ecs.storage.api.components.Component.PooledComponentData;
import de.schosin.ecs.storage.api.entities.Archetype;
import de.schosin.ecs.storage.api.entities.ComponentMask;
import de.schosin.ecs.storage.common.PendingChanges;
import de.schosin.ecs.utils.collections.Bag;
import de.schosin.ecs.utils.collections.ImmutableBag;

@AutoService(StorageEngine.class)
public class DefaultStorageEngine implements StorageEngine {

    private ComponentStorageImpl componentStorage;
    private EntityStorageImpl entityStorage;

    @Override
    public void setWorld(StorageWorld world) {
        var pendingChanges = world.createEntityBag(PendingChanges.class);

        this.componentStorage = new ComponentStorageImpl(world, pendingChanges);
        this.entityStorage = new EntityStorageImpl(world, pendingChanges, componentStorage);
    }

    @Override
    public Component<?, ?> getComponent(int componentId) {
        return this.componentStorage.getComponent(componentId);
    }

    @Override
    public <T> ClassComponent<T> getComponent(ClassType<T> type) {
        return this.componentStorage.getComponent(type);
    }

    @Override
    public <T extends Pooled> PooledComponentData<T> getPooledComponent(ClassType<T> type) {
        return this.componentStorage.getPooledComponent(type);
    }

    @Override
    public <R, T> ComponentRelationData<R, T> getComponent(ComponentRelationType<R, T> type) {
        return this.componentStorage.getComponent(type);
    }

    @Override
    public <R extends Exclusive, T> ExclusiveComponentRelationData<R, T> getComponent(ExclusiveComponentRelationType<R, T> type) {
        return this.componentStorage.getComponent(type);
    }

    @Override
    public <R> EntityRelationData<R> getComponent(EntityRelationType<R> type) {
        return this.componentStorage.getComponent(type);
    }

    @Override
    public <R extends Exclusive> ExclusiveEntityRelationData<R> getComponent(ExclusiveEntityRelationType<R> type) {
        return this.componentStorage.getComponent(type);
    }

    @Override
    public ImmutableBag<Component<?, ?>> getComponents() {
        return this.componentStorage.getComponents();
    }

    @Override
    public <T> ImmutableBag<Component<? extends T, ?>> getComponents(ComponentType<T, ?> bound) {
        return this.componentStorage.getComponents(bound);
    }

    @Override
    public RegularComponentType<?, ?>[] getRegularComponentTypes(ComponentType<?, ?> bound) {
        return this.componentStorage.getRegularComponentTypes(bound);
    }

    @Override
    public DataAccessor getAccessor(int entityId) {
        return this.entityStorage.getAccessor(entityId);
    }

    @Override
    public ComponentMask getComponentMaskForEntity(int entityId) {
        return this.entityStorage.getComponentMaskForEntity(entityId);
    }

    @Override
    public ComponentMask getComponentMaskById(int componentMaskId) {
        return this.entityStorage.getComponentMaskById(componentMaskId);
    }

    @Override
    public ComponentMask getComponentMask(RegularComponentType<?, ?>... componentTypes) {
        return this.entityStorage.getComponentMask(componentTypes);
    }

    @Override
    public ImmutableBag<ComponentMask> getComponentMasks() {
        return this.entityStorage.getComponentMasks();
    }

    @Override
    public void getComponentMasks(Predicate<ComponentMask> predicate, Bag<ComponentMask> fill) {
        this.entityStorage.getComponentMasks(predicate, fill);
    }

    @Override
    public ComponentMask create(int entityId, ComponentMask componentMask, Object[] components) {
        return this.entityStorage.create(entityId, componentMask, components);
    }

    @Override
    public ComponentMask create(int entityId, ComponentMask componentMask, ImmutableBag<? extends RegularComponentType<?, ?>> componentTypes, Object[] components) {
        return this.entityStorage.create(entityId, componentMask, componentTypes, components);
    }

    @Override
    public ComponentMask create(int entityId, ComponentMask componentMask, ImmutableBag<? extends RegularComponentType<?, ?>> componentTypes, ImmutableBag<Object> components) {
        return this.entityStorage.create(entityId, componentMask, componentTypes, components);
    }

    @Override
    public ComponentMask add(int entityId, Object[] components) {
        return this.entityStorage.add(entityId, components);
    }

    @Override
    public ComponentMask add(int entityId, ImmutableBag<? extends RegularComponentType<?, ?>> componentTypes, Object[] components) {
        return this.entityStorage.add(entityId, componentTypes, components);
    }

    @Override
    public ComponentMask remove(int entityId, ImmutableBag<? extends ComponentType<?, ?>> componentTypes) {
        return this.entityStorage.remove(entityId, componentTypes);
    }

    @Override
    public ComponentMask modify(int entityId, Object[] add, ImmutableBag<? extends ComponentType<?, ?>> removeTypes) {
        return this.entityStorage.modify(entityId, add, removeTypes);
    }

    @Override
    public ComponentMask modify(int entityId, ImmutableBag<? extends RegularComponentType<?, ?>> addTypes, Object[] add, ImmutableBag<? extends ComponentType<?, ?>> removeTypes) {
        return this.entityStorage.modify(entityId, addTypes, add, removeTypes);
    }

    @Override
    public ComponentMask delete(int entityId) {
        return this.entityStorage.delete(entityId);
    }

    @Override
    public ComponentMask getPendingComponentMask(int entityId) {
        return this.entityStorage.getPendingComponentMask(entityId);
    }

    @Override
    public ComponentMask flushChanges(int entityId) {
        return this.entityStorage.flushChanges(entityId);
    }

    @Override
    public Archetype getArchetypeForEntity(int entityId) {
        return entityStorage.getArchetypeForEntity(entityId);
    }

    @Override
    public Archetype getArchetypeById(int archetypeId) {
        return entityStorage.getArchetypeById(archetypeId);
    }

    @Override
    public Archetype getArchetype(RegularComponentType<?, ?>... componentTypes) {
        return entityStorage.getArchetype(componentTypes);
    }

}
