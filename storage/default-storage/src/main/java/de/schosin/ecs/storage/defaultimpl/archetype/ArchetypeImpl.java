package de.schosin.ecs.storage.defaultimpl.archetype;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.schosin.ecs.api.Pooled;
import de.schosin.ecs.api.components.types.ComponentType.RegularComponentType;
import de.schosin.ecs.storage.api.ComponentStorage;
import de.schosin.ecs.storage.api.components.Component;
import de.schosin.ecs.storage.api.entities.Archetype;
import de.schosin.ecs.storage.api.entities.ComponentMask;
import de.schosin.ecs.storage.api.entities.EntityData;
import de.schosin.ecs.utils.collections.ImmutableIntBag;
import de.schosin.ecs.utils.collections.IntBag;
import de.schosin.ecs.utils.collections.Pool;

public class ArchetypeImpl implements Archetype {

    private final ComponentStorage componentStorage;
    private final ComponentMask componentMask;

    private final IntBag entities;
    private final ImmutableIntBag immutableEntities;

    private final Map<List<RegularComponentType<?, ?>>, EntityDataImpl> entityDataMap = new HashMap<>();
    private final Pool<List<RegularComponentType<?, ?>>> typesPool = Pool.unbounded(List.class, ArrayList::new, List::clear);

    public ArchetypeImpl(ComponentStorage componentStorage, ComponentMask componentMask) {
        this.componentStorage = componentStorage;
        this.componentMask = componentMask;

        this.entities = new IntBag(64);
        this.immutableEntities = ImmutableIntBag.create(entities);
    }

    public void add(int entityId) {
        this.entities.add(entityId);
    }

    public void remove(int entityId) {
        this.entities.removeValue(entityId);
    }

    @Override
    public int getId() {
        return componentMask.getId();
    }

    @Override
    public ComponentMask getComponentMask() {
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

    @Override
    public ImmutableIntBag getEntities() {
        return immutableEntities;
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

    private class EntityDataImpl implements EntityData {

        private final int size;
        private final Component<?, ?>[] components;

        private final Pool<AccessorImpl> accessors = Pool.unbounded(AccessorImpl.class, AccessorImpl::new);

        public EntityDataImpl(RegularComponentType<?, ?>[] componentTypes) {
            this.size = componentTypes.length;
            this.components = new Component<?, ?>[size];

            for (int i = 0; i < size; i++) {
                this.components[i] = componentStorage.getComponent(componentTypes[i]);
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
        public Accessor getAccessor() {
            return accessors.getInstance();
        }

        @Override
        public void freeAccessor(Accessor accessor) {
            accessors.free((AccessorImpl) accessor);
        }

        private class AccessorImpl implements Accessor, Pooled {

            private int index = -1;

            @Override
            public boolean hasNext() {
                return ++index < entities.getSize();
            }

            @Override
            public int next() {
                return entities.get(index);
            }

            @Override
            public Object getComponent(int componentIndex) {
                return components[componentIndex].getComponent(getId(index));
            }

            @Override
            public void reset() {
                this.index = -1;
            }

        }

    }

}
