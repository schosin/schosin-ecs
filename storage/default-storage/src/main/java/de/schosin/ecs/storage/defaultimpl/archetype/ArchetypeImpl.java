package de.schosin.ecs.storage.defaultimpl.archetype;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.schosin.ecs.api.Pooled;
import de.schosin.ecs.api.components.Relations;
import de.schosin.ecs.api.components.types.ComponentType.RegularComponentType;
import de.schosin.ecs.api.data.DataAccessor;
import de.schosin.ecs.api.data.IterableAccessor;
import de.schosin.ecs.storage.api.ComponentStorage;
import de.schosin.ecs.storage.api.StorageEngineException;
import de.schosin.ecs.storage.api.components.Component;
import de.schosin.ecs.storage.api.entities.Archetype;
import de.schosin.ecs.storage.api.entities.EntityData;
import de.schosin.ecs.storage.defaultimpl.EntityStorageImpl;
import de.schosin.ecs.storage.defaultimpl.components.DefaultComponent;
import de.schosin.ecs.storage.defaultimpl.entities.ComponentMaskImpl;
import de.schosin.ecs.utils.collections.Bag;
import de.schosin.ecs.utils.collections.ImmutableIntBag;
import de.schosin.ecs.utils.collections.IntBag;
import de.schosin.ecs.utils.collections.Pool;

public class ArchetypeImpl implements Archetype {

    private final ComponentStorage componentStorage;
    private final EntityStorageImpl entityStorage;
    private final ComponentMaskImpl componentMask;
    private final IntBag componentLookup;
    private final int size;

    private final IntBag entities;
    private final ImmutableIntBag immutableEntities;

    private final IntBag lookup;
    private final EntityData entityData;

    private final Map<List<RegularComponentType<?, ?>>, EntityDataImpl> entityDataMap = new HashMap<>();
    private final Pool<List<RegularComponentType<?, ?>>> typesPool = Pool.unbounded(List.class, ArrayList::new, List::clear);

    public ArchetypeImpl(ComponentStorage componentStorage, EntityStorageImpl entityStorage, ComponentMaskImpl componentMask) {
        this.componentStorage = componentStorage;
        this.entityStorage = entityStorage;
        this.componentMask = componentMask;
        this.size = componentMask.getComponentTypes().getSize();

        this.entities = new IntBag(64);
        this.immutableEntities = ImmutableIntBag.create(entities);

        this.lookup = new IntBag(64);

        var componentTypes = new Bag<>(componentMask.getComponentTypes());
        this.entityData = getEntityData(Arrays.copyOf(componentTypes.getData(), componentTypes.getSize()));

        var components = componentMask.getComponents();
        var size = components.getSize();

        var largestComponentId = 0;
        for (int i = 0; i < size; i++) {
            var component = components.get(i);

            if (component.id() > largestComponentId) {
                largestComponentId = component.id();
            }
        }

        this.componentLookup = new IntBag(largestComponentId);
        Arrays.fill(this.componentLookup.getData(), -1);

        for (int i = 0; i < size; i++) {
            this.componentLookup.set(components.get(i).id(), i);
        }
    }

    public void add(int entityId) {
        var size = this.entities.getSize();
        this.lookup.set(entityId, size == 0 ? -1 : size);

        this.entities.add(entityId);
    }

    public void remove(int entityId) {
        this.lookup.set(entityId, 0);

        var lastIndex = entities.getSize() - 1;
        if (entityId == entities.get(lastIndex)) {
            this.entities.removeLast();
            return;
        }

        var swappedEntityId = entities.get(lastIndex);

        var index = this.entities.indexOf(entityId);
        this.entities.removeIndex(index);

        this.lookup.set(swappedEntityId, index == 0 ? -1 : index);
    }

    @Override
    public int getId() {
        return componentMask.getId();
    }

    @Override
    public ComponentMaskImpl getComponentMask() {
        return componentMask;
    }

    @Override
    public int getCount() {
        return entities.getSize();
    }

    @Override
    public boolean contains(int entityId) {
        return this.entities.contains(entityId);
    }

    public DataAccessor getAccessor(int entityId) {
        return entityData.getAccessor(entityId);
    }

    @Override
    public ImmutableIntBag getEntities() {
        return immutableEntities;
    }

    @Override
    public int getComponentIndex(int componentId) {
        var components = componentMask.getComponents();
        for (int i = 0, s = components.getSize(); i < s; i++) {
            if (components.get(i).id() == componentId) {
                return componentId;
            }
        }

        return -1;
    }

    @Override
    public int getComponentIndex(RegularComponentType<?, ?> componentType) {
        var componentTypes = componentMask.getComponentTypes();
        for (int i = 0, s = componentTypes.getSize(); i < s; i++) {
            if (componentTypes.get(i).equals(componentType)) {
                return i;
            }
        }

        return -1;
    }

    @Override
    public EntityData getEntityData() {
        return entityData;
    }

    @Override
    public EntityData getEntityData(RegularComponentType<?, ?>... componentTypes) {
        var key = typesPool.getInstance();
        for (var type : componentTypes) {
            key.add(type);
        }

        var result = entityDataMap.get(key);
        if (result != null) {
            typesPool.free(key);
            return result;
        }

        synchronized (entityDataMap) {
            result = entityDataMap.get(key);
            if (result != null) {
                typesPool.free(key);
                return result;
            }

            result = new EntityDataImpl(componentTypes);
            entityDataMap.put(List.copyOf(key), result);

            typesPool.free(key);
            return result;
        }
    }

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void createEntity(int entityId, Object[] components) {
        var existing = entityStorage.getComponentMaskForEntity(entityId);
        if (existing != null) {
            throw new StorageEngineException("Cannot create entity %d, already present in storage: %s".formatted(entityId, existing));
        }

        // Validate matching length
        if (size != components.length) {
            throw new StorageEngineException("Expected %d components, but got %d".formatted(size, components.length));
        }

        // Add components
        for (int i = 0, s = components.length; i < s; i++) {
            var componentData = (Component & DefaultComponent) componentMask.getComponents().get(i);
            var component = components[i];

            var componentType = componentData.type();
            if (!componentType.isInstance(component)) {
                throw new StorageEngineException("Expected component type '%s' at index %d, but was '%s'".formatted(componentType, i, component));
            }

            if (!(component instanceof Relations<?> relations)) {
                componentData.addComponent(entityId, component);
                continue;
            }

            for (int r = 0, rs = relations.size(); r < rs; r++) {
                componentData.addComponent(entityId, relations.get(r));
            }

            Relations.free(relations);
        }

        // Add to EntityStorage
        entityStorage.add(this, entityId);
    }

    @Override
    public String toString() {
        return new StringBuilder()
                .append("ArchetypeData(")
                .append("count = ").append(this.entities.getSize()).append(", ")
                .append("componentMask = ").append(this.componentMask).append(")")
                .toString();
    }

    private class EntityDataImpl implements EntityData {

        private final int size;
        private final Component<?, ?>[] components;
        private final IntBag componentLookup;

        private final Pool<AccessorImpl> accessors = Pool.unbounded(AccessorImpl.class, AccessorImpl::new);

        public EntityDataImpl(RegularComponentType<?, ?>[] componentTypes) {
            this.size = componentTypes.length;
            this.components = new Component<?, ?>[size];

            var largestComponentId = 0;
            for (int i = 0; i < size; i++) {
                var component = this.components[i] = componentStorage.getComponent(componentTypes[i]);

                if (component.id() > largestComponentId) {
                    largestComponentId = component.id();
                }
            }

            this.componentLookup = new IntBag(largestComponentId);
            Arrays.fill(this.componentLookup.getData(), -1);

            for (int i = 0; i < size; i++) {
                this.componentLookup.set(this.components[i].id(), i);
            }
        }

        @Override
        public int getSize() {
            return entities.getSize();
        }

        @Override
        public int getId(int index) {
            return entities.get(index);
        }

        @Override
        @SuppressWarnings("unchecked")
        public <R> R getComponent(int index) {
            if (size == 0) {
                return null;
            }

            return (R) components[0].getComponent(getId(index));
        }

        @Override
        public IterableAccessor getAccessor() {
            return accessors.getInstance();
        }

        @Override
        public DataAccessor getAccessor(int entityId) {
            var index = lookup.get(entityId);
            if (index == 0) {
                return null;
            }

            var accessor = accessors.getInstance();
            accessor.index = index == -1 ? 0 : index;

            return accessor;
        }

        @Override
        public String toString() {
            return new StringBuilder()
                    .append("EntityDataImpl(archetype = ").append(ArchetypeImpl.this).append(")")
                    .toString();
        }

        private class AccessorImpl implements IterableAccessor, Pooled {

            private int index = -1;

            @Override
            public boolean hasNext() {
                return index < entities.getSize() - 1;
            }

            @Override
            public int next() {
                return entities.get(++index);
            }

            @Override
            public int entityId() {
                return entities.get(index);
            }

            @Override
            public boolean hasComponent(int componentId) {
                return componentStorage.getComponent(componentId).hasComponent(getId(index));
            }

            @Override
            @SuppressWarnings("unchecked")
            public <R> R getComponent(int componentId) {
                if (componentId >= componentLookup.getSize()) {
                    return (R) componentStorage.getComponent(componentId).getComponent(getId(index));
                }

                var componentIndex = componentLookup.get(componentId);
                if (componentIndex == -1) {
                    return (R) componentStorage.getComponent(componentId).getComponent(getId(index));
                }

                return (R) components[componentIndex].getComponent(getId(index));
            }

            @Override
            public <R> R getComponentByIndex(int componentIndex) {
                return getComponent(componentIndex);
            }

            @Override
            @SuppressWarnings("unchecked")
            public <R> R getPendingComponent(int componentId) {
                var lookup = ArchetypeImpl.this.componentLookup;

                if (componentId >= lookup.getSize()) {
                    return (R) componentStorage.getComponent(componentId).getComponent(getId(index));
                }

                var componentIndex = lookup.get(componentId);
                if (componentIndex != -1) {
                    return null;
                }

                return (R) componentStorage.getComponent(componentId).getComponent(getId(index));

            }

            @Override
            public void free() {
                accessors.free(this);
            }

            @Override
            public void reset() {
                this.index = -1;
            }

            @Override
            public String toString() {
                return new StringBuilder()
                        .append("AccessorImpl(index = ").append(index)
                        .append(", size = ").append(size)
                        .append(", archetype = ").append(ArchetypeImpl.this)
                        .append(")").toString();
            }

        }

    }

}
