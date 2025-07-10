package de.schosin.ecs.storage.archetype;

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
import de.schosin.ecs.storage.api.StorageEngineException;
import de.schosin.ecs.storage.api.StorageWorld;
import de.schosin.ecs.storage.api.components.Component;
import de.schosin.ecs.storage.api.components.Component.ClassComponent;
import de.schosin.ecs.storage.api.components.Component.ComponentRelationData;
import de.schosin.ecs.storage.api.components.Component.EntityRelationData;
import de.schosin.ecs.storage.api.components.Component.ExclusiveComponentRelationData;
import de.schosin.ecs.storage.api.components.Component.ExclusiveEntityRelationData;
import de.schosin.ecs.storage.api.components.Component.PooledComponentData;
import de.schosin.ecs.storage.api.entities.Archetype;
import de.schosin.ecs.storage.archetype.components.ComponentIndex;
import de.schosin.ecs.storage.archetype.entities.EntityIndex;
import de.schosin.ecs.storage.archetype.entities.EntityRelationIndex;
import de.schosin.ecs.utils.collections.ImmutableBag;

@AutoService(StorageEngine.class)
public class ArchetypeStorageEngine implements StorageEngine {

    private ComponentStorageImpl componentStorage;
    private EntityStorageImpl entityStorage;

    @Override
    public void setWorld(StorageWorld world, Object config) {
        var storageConfig = retrieveStorageConfig(config);

        var componentIndex = new ComponentIndex(storageConfig.classIdCount(), storageConfig.relationCount());
        var relationIndex = new EntityRelationIndex(world);
        var entityIndex = new EntityIndex(world, storageConfig, this, componentIndex, relationIndex);

        this.componentStorage = new ComponentStorageImpl(world, componentIndex, entityIndex, relationIndex);
        this.entityStorage = new EntityStorageImpl(entityIndex, componentStorage);
    }

    static ArchetypeStorageConfig retrieveStorageConfig(Object config) {
        // User supplied config
        if (config != null) {
            if (config instanceof ArchetypeStorageConfig archetypeConfig) {
                return archetypeConfig;
            }

            throw new StorageEngineException("ArchetypeStorage requires config object of type ArchetypeStorageConfig, but received: " + config);
        }

        return ArchetypeStorageConfig.getConfig();
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
    public DataAccessor getAccessor(int entityId) {
        return this.entityStorage.getAccessor(entityId);
    }

    @Override
    public Archetype add(int entityId, Object[] components) {
        return this.entityStorage.add(entityId, components);
    }

    @Override
    public Archetype add(int entityId, ImmutableBag<? extends RegularComponentType<?, ?>> componentTypes, Object[] components) {
        return this.entityStorage.add(entityId, componentTypes, components);
    }

    @Override
    public Archetype remove(int entityId, ImmutableBag<? extends ComponentType<?, ?>> componentTypes) {
        return this.entityStorage.remove(entityId, componentTypes);
    }

    @Override
    public Archetype modify(int entityId, Object[] add, ImmutableBag<? extends ComponentType<?, ?>> removeTypes) {
        return this.entityStorage.modify(entityId, add, removeTypes);
    }

    @Override
    public Archetype modify(int entityId, ImmutableBag<? extends RegularComponentType<?, ?>> addTypes, Object[] add, ImmutableBag<? extends ComponentType<?, ?>> removeTypes) {
        return this.entityStorage.modify(entityId, addTypes, add, removeTypes);
    }

    @Override
    public Archetype delete(int entityId) {
        return this.entityStorage.delete(entityId);
    }

    @Override
    public Archetype getPendingArchetype(int entityId) {
        return this.entityStorage.getPendingArchetype(entityId);
    }

    @Override
    public Archetype flushChanges(int entityId) {
        return this.entityStorage.flushChanges(entityId);
    }

    @Override
    public ImmutableBag<Archetype> getArchetypes() {
        return this.entityStorage.getArchetypes();
    }

    @Override
    public Archetype getArchetypeForEntity(int entityId) {
        return this.entityStorage.getArchetypeForEntity(entityId);
    }

    @Override
    public Archetype getArchetypeById(int archetypeId) {
        return this.entityStorage.getArchetypeById(archetypeId);
    }

    @Override
    public Archetype getArchetype(RegularComponentType<?, ?>... componentTypes) {
        return this.entityStorage.getArchetype(componentTypes);
    }

}
