package de.schosin.ecs.storage.archetype;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
import de.schosin.ecs.storage.api.StorageWorld;
import de.schosin.ecs.storage.api.components.Component;
import de.schosin.ecs.storage.api.components.Component.ClassComponent;
import de.schosin.ecs.storage.api.components.Component.ComponentData;
import de.schosin.ecs.storage.api.components.Component.ComponentRelationData;
import de.schosin.ecs.storage.api.components.Component.EntityRelationData;
import de.schosin.ecs.storage.api.components.Component.ExclusiveComponentRelationData;
import de.schosin.ecs.storage.api.components.Component.ExclusiveEntityRelationData;
import de.schosin.ecs.storage.api.components.Component.PooledComponentData;
import de.schosin.ecs.storage.api.events.ComponentAddedEvent;
import de.schosin.ecs.storage.archetype.components.ComponentIndex;
import de.schosin.ecs.storage.archetype.components.implementaions.ComponentDataImpl;
import de.schosin.ecs.storage.archetype.components.implementaions.ComponentRelationDataImpl;
import de.schosin.ecs.storage.archetype.components.implementaions.EntityRelationDataImpl;
import de.schosin.ecs.storage.archetype.components.implementaions.ExclusiveComponentRelationDataImpl;
import de.schosin.ecs.storage.archetype.components.implementaions.ExclusiveEntityRelationDataImpl;
import de.schosin.ecs.storage.archetype.components.implementaions.PooledComponentDataImpl;
import de.schosin.ecs.storage.archetype.entities.EntityIndex;
import de.schosin.ecs.storage.archetype.entities.EntityRelationIndex;
import de.schosin.ecs.utils.collections.Bag;
import de.schosin.ecs.utils.collections.ImmutableBag;

public class ComponentStorageImpl implements ComponentStorage {

    private final StorageWorld world;

    private final ComponentIndex componentIndex;
    private final EntityIndex entityIndex;
    private final EntityRelationIndex relationIndex;

    private final Bag<Component<?, ?>> sortedComponents = new Bag<>(Component.class, 64);
    private final ImmutableBag<Component<?, ?>> immutableComponents = ImmutableBag.create(sortedComponents);

    private final Bag<Component<?, ?>> components = new Bag<>(Component.class, 64);

    private final Map<RegularComponentType<?, ?>, Component<?, ?>> componentData = new ConcurrentHashMap<>();

    private final Map<ComponentType<?, ?>, Bag<Component<?, ?>>> bounds = new ConcurrentHashMap<>();
    private final Map<ComponentType<?, ?>, ImmutableBag<Component<?, ?>>> immutableBounds = new ConcurrentHashMap<>();

    public ComponentStorageImpl(StorageWorld world, ComponentIndex componentIndex, EntityIndex entityIndex, EntityRelationIndex relationIndex) {
        this.world = world;

        this.componentIndex = componentIndex;
        this.entityIndex = entityIndex;
        this.relationIndex = relationIndex;
    }

    @Override
    public Component<?, ?> getComponent(int componentId) {
        return components.get(componentId);
    }

    @Override
    public <T> ClassComponent<T> getComponent(ClassType<T> type) {
        return getComponentData(type);
    }

    @Override
    public <T extends Pooled> PooledComponentData<T> getPooledComponent(ClassType<T> type) {
        return getPooledComponentData(type);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private <T> ClassComponent<T> getComponentData(ClassType<T> classType) {
        if (Pooled.class.isAssignableFrom(classType.clazz())) {
            return getPooledComponent((ClassType) classType);
        }

        var result = (ComponentData<T>) componentData.get(classType);
        if (result != null) {
            return result;
        }

        synchronized (componentData) {
            result = (ComponentData<T>) componentData.get(classType);
            if (result != null) {
                return result;
            }

            var component = createComponentData(classType);

            this.components.set(component.id(), component);
            this.sortedComponents.add(component);
            this.componentData.put(classType, component);

            handleNewComponent(component.id(), classType, component);

            return component;
        }
    }

    @SuppressWarnings("unchecked")
    private <T extends Pooled> PooledComponentData<T> getPooledComponentData(ClassType<T> classType) {
        var result = (PooledComponentData<T>) componentData.get(classType);
        if (result != null) {
            return result;
        }

        synchronized (componentData) {
            result = (PooledComponentData<T>) componentData.get(classType);
            if (result != null) {
                return result;
            }

            var component = createPooledComponentData(classType);

            this.components.set(component.id(), component);
            this.sortedComponents.add(component);
            this.componentData.put(classType, component);

            handleNewComponent(component.id(), classType, component);

            return component;
        }
    }

    private <T extends Pooled> PooledComponentData<T> createPooledComponentData(ClassType<T> classType) {
        var pool = componentIndex.getPool(classType.clazz());

        return new PooledComponentDataImpl<>(componentIndex.getId(classType), classType, entityIndex, pool);
    }

    private <T> ComponentData<T> createComponentData(ClassType<T> classType) {
        return new ComponentDataImpl<>(componentIndex.getId(classType), classType, entityIndex);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R, T> ComponentRelationData<R, T> getComponent(ComponentRelationType<R, T> relationType) {
        var result = (ComponentRelationData<R, T>) componentData.get(relationType);
        if (result != null) {
            return result;
        }

        synchronized (componentData) {
            result = (ComponentRelationData<R, T>) componentData.get(relationType);
            if (result != null) {
                return result;
            }

            var component = createComponentRelationData(relationType);

            this.components.set(component.id(), component);
            this.sortedComponents.add(component);
            this.componentData.put(relationType, component);

            handleNewComponent(component.id(), relationType, component);

            return component;
        }
    }

    private <R, T> ComponentRelationData<R, T> createComponentRelationData(ComponentRelationType<R, T> relationType) {
        return new ComponentRelationDataImpl<>(componentIndex.getId(relationType), relationType, entityIndex);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R extends Exclusive, T> ExclusiveComponentRelationData<R, T> getComponent(ExclusiveComponentRelationType<R, T> relationType) {
        var result = (ExclusiveComponentRelationData<R, T>) componentData.get(relationType);
        if (result != null) {
            return result;
        }

        synchronized (componentData) {
            result = (ExclusiveComponentRelationData<R, T>) componentData.get(relationType);
            if (result != null) {
                return result;
            }

            var component = createExclusiveComponentRelationData(relationType);

            this.components.set(component.id(), component);
            this.sortedComponents.add(component);
            this.componentData.put(relationType, component);

            handleNewComponent(component.id(), relationType, component);

            return component;
        }
    }

    private <R extends Exclusive, T> ExclusiveComponentRelationData<R, T> createExclusiveComponentRelationData(ExclusiveComponentRelationType<R, T> relationType) {
        return new ExclusiveComponentRelationDataImpl<>(componentIndex.getId(relationType), relationType, entityIndex);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R> EntityRelationData<R> getComponent(EntityRelationType<R> relationType) {
        var result = (EntityRelationData<R>) componentData.get(relationType);
        if (result != null) {
            return result;
        }

        synchronized (componentData) {
            result = (EntityRelationData<R>) componentData.get(relationType);
            if (result != null) {
                return result;
            }

            var component = createEntityRelationData(relationType);

            this.components.set(component.id(), component);
            this.sortedComponents.add(component);
            this.componentData.put(relationType, component);

            handleNewComponent(component.id(), relationType, component);

            return component;
        }
    }

    private <R> EntityRelationData<R> createEntityRelationData(EntityRelationType<R> relationType) {
        return new EntityRelationDataImpl<>(componentIndex.getId(relationType), relationType, entityIndex, relationIndex);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R extends Exclusive> ExclusiveEntityRelationData<R> getComponent(ExclusiveEntityRelationType<R> type) {
        var result = (ExclusiveEntityRelationData<R>) componentData.get(type);
        if (result != null) {
            return result;
        }

        synchronized (componentData) {
            result = (ExclusiveEntityRelationData<R>) componentData.get(type);
            if (result != null) {
                return result;
            }

            var component = createExclusiveEntityRelationData(type);

            this.components.set(component.id(), component);
            this.sortedComponents.add(component);
            this.componentData.put(type, component);

            handleNewComponent(component.id(), type, component);

            return component;
        }
    }

    private <R extends Exclusive> ExclusiveEntityRelationData<R> createExclusiveEntityRelationData(ExclusiveEntityRelationType<R> relationType) {
        return new ExclusiveEntityRelationDataImpl<>(componentIndex.getId(relationType), relationType, entityIndex, relationIndex);
    }

    private <T, R> void handleNewComponent(int componentId, RegularComponentType<T, R> type, Component<T, R> component) {
        // Update bounds
        for (var entry : this.bounds.entrySet()) {
            var componentType = entry.getKey();

            if (componentType.matches(type)) {
                entry.getValue().add(component);
            }
        }

        // Dispatch event
        world.dispatchEvent(ComponentAddedEvent.get(componentId, type, component));
    }

    @Override
    public ImmutableBag<Component<?, ?>> getComponents() {
        return immutableComponents;
    }

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public <T> ImmutableBag<Component<? extends T, ?>> getComponents(ComponentType<T, ?> bound) {
        var result = immutableBounds.get(bound);
        if (result != null) {
            return (ImmutableBag) result;
        }

        synchronized (components) {
            result = immutableBounds.get(bound);
            if (result != null) {
                return (ImmutableBag) result;
            }

            // Create bag
            var bag = new Bag<Component<?, ?>>(Component.class, 32);

            // Fill bag with known components
            var data = this.components.getData();
            for (int i = 0, s = this.components.getSize(); i < s; i++) {
                var component = data[i];

                if (component != null && bound.matches(component.type())) {
                    bag.add(component);
                }
            }

            // Add bag and return immutable result
            this.bounds.put(bound, bag);

            var immutableBag = ImmutableBag.create(bag);
            this.immutableBounds.put(bound, immutableBag);

            return (ImmutableBag) immutableBag;
        }
    }

}
