package de.schosin.ecs.engine.components;

import static de.schosin.ecs.api.components.types.ComponentType.exclusiveRelation;
import static de.schosin.ecs.api.components.types.ComponentType.relation;

import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jspecify.annotations.NonNull;

import de.schosin.ecs.api.Pooled;
import de.schosin.ecs.api.components.ComponentSet;
import de.schosin.ecs.api.components.ComponentSet.ComponentData;
import de.schosin.ecs.api.components.Components;
import de.schosin.ecs.api.components.Components.ComponentMapper;
import de.schosin.ecs.api.components.Components.ComponentRelationMapper;
import de.schosin.ecs.api.components.Components.ComponentSetMapper;
import de.schosin.ecs.api.components.Components.EntityRelationFetchMapper;
import de.schosin.ecs.api.components.Components.EntityRelationMapper;
import de.schosin.ecs.api.components.Components.EnumComponentMapper;
import de.schosin.ecs.api.components.Components.ExclusiveComponentRelationMapper;
import de.schosin.ecs.api.components.Components.ExclusiveEntityRelationFetchMapper;
import de.schosin.ecs.api.components.Components.ExclusiveEntityRelationMapper;
import de.schosin.ecs.api.components.Components.PooledComponentMapper;
import de.schosin.ecs.api.components.Components.WildcardComponents;
import de.schosin.ecs.api.components.Relation;
import de.schosin.ecs.api.components.Relation.ComponentRelation;
import de.schosin.ecs.api.components.Relation.EntityRelation;
import de.schosin.ecs.api.components.Relation.EntityRelationData;
import de.schosin.ecs.api.components.Relation.Exclusive;
import de.schosin.ecs.api.components.Result.ComponentResult;
import de.schosin.ecs.api.components.Result.EntityRelationDataResult;
import de.schosin.ecs.api.components.types.ComponentType;
import de.schosin.ecs.api.components.types.ComponentType.ClassType;
import de.schosin.ecs.api.components.types.ComponentType.ComponentRelationType;
import de.schosin.ecs.api.components.types.ComponentType.ComponentSetType;
import de.schosin.ecs.api.components.types.ComponentType.EntityRelationFetchType;
import de.schosin.ecs.api.components.types.ComponentType.EntityRelationType;
import de.schosin.ecs.api.components.types.ComponentType.ExclusiveComponentRelationType;
import de.schosin.ecs.api.components.types.ComponentType.ExclusiveEntityRelationFetchType;
import de.schosin.ecs.api.components.types.ComponentType.ExclusiveEntityRelationType;
import de.schosin.ecs.api.components.types.ComponentType.RegularComponentType;
import de.schosin.ecs.api.components.types.ComponentType.Wildcard;
import de.schosin.ecs.engine.BagManager;
import de.schosin.ecs.engine.events.EventManager;
import de.schosin.ecs.engine.events.builtin.ComponentAddedEvent;
import de.schosin.ecs.engine.utils.components.ComponentSetsHelper;
import de.schosin.ecs.engine.utils.components.ComponentSetsHelper.ComponentSetFactory;
import de.schosin.ecs.storage.api.components.Component;
import de.schosin.ecs.storage.api.components.Component.ClassComponent;
import de.schosin.ecs.storage.api.components.Component.PooledComponentData;
import de.schosin.ecs.utils.ComponentUtils;
import de.schosin.ecs.utils.collections.Bag;
import de.schosin.ecs.utils.collections.BagIterator;
import de.schosin.ecs.utils.collections.Pool;

public class ComponentMapperManager implements Components.Creator {

    public interface PoolingComponents<T> {
        void free(T result);
    }

    private interface ReclaimingComponents {
        void reclaim();
    }

    private final BagManager bagManager;
    private final ComponentManager componentManager;
    private final TransmutationManager transmutationManager;
    private final RelationMapperManager relationMapperManager;

    private final ComponentEventHandler eventHandler;

    private final Bag<Components<?, ?>> components;
    private final Map<Enum<?>, EnumComponentMapper<?>> enumComponents = new IdentityHashMap<>();

    private final Bag<ReclaimingComponents> reclaimingComponents;
    private final Map<ComponentType<?, ?>, Components<?, ?>> componentMappers = new ConcurrentHashMap<>();

    public ComponentMapperManager(EventManager eventManager, BagManager bagManager, ComponentManager componentManager, TransmutationManager transmutationManager,
            RelationMapperManager relationMapperManager) {

        this.bagManager = bagManager;
        this.componentManager = componentManager;
        this.transmutationManager = transmutationManager;
        this.relationMapperManager = relationMapperManager;

        this.eventHandler = new ComponentEventHandler(eventManager);

        this.reclaimingComponents = bagManager.createComponentBag(ReclaimingComponents.class);
        this.components = bagManager.createComponentBag(Components.class);
    }

    public void process() {
        var data = reclaimingComponents.getData();
        for (int i = 0, s = reclaimingComponents.getSize(); i < s; i++) {
            data[i].reclaim();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T, R> Components<T, R> getComponents(ComponentType<T, R> type) {
        return switch (type) {
            case ComponentType.RegularComponentType<T, R> regular -> getComponents(regular);
            case ComponentType.EntityRelationFetchType<?, ?> fetch -> (Components<T, R>) getEntityRelations(fetch);
            case ComponentType.ExclusiveEntityRelationFetchType<?, ?> fetch -> (Components<T, R>) getEntityRelations(fetch);
            case ComponentType.ComponentSetType<?> set -> (Components<T, R>) getComponentSets(set);
            case ComponentType.Wildcard<?> wildcard -> (Components<T, R>) getWildcardComponents(wildcard);
        };
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T, R> Components<T, R> getComponents(RegularComponentType<T, R> type) {
        return (Components<T, R>) switch (type) {
            case ComponentType.ClassType<?> classType -> getComponents(classType);
            case ComponentRelationType<?, ?> relation -> getComponentRelations(relation);
            case ExclusiveComponentRelationType<?, ?> relation -> getComponentRelations(relation);
            case EntityRelationType<?> relation -> getEntityRelations(relation);
            case ExclusiveEntityRelationType<?> relation -> getEntityRelations(relation);
        };
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public <T> ComponentMapper<T> getComponents(@NonNull ClassType<T> type) {
        var metadata = componentManager.getComponent(type);

        var result = (ComponentMapper<T>) this.components.get(metadata.id());
        if (result != null) {
            return result;
        }

        synchronized (this.components) {
            result = (ComponentMapper<T>) this.components.get(metadata.id());
            if (result != null) {
                return result;
            }

            var mapper = (ComponentMapper<T>) switch (metadata) {
                case Component.PooledComponentData<?> pooled -> new PooledComponentMapperImpl(pooled);
                case Component.ComponentData<T> data -> new ComponentMapperImpl<>(data);
            };

            this.components.set(metadata.id(), mapper);
            this.componentMappers.put(type, mapper);

            return mapper;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Enum<T>> @NonNull EnumComponentMapper<T> getEnumComponents(@NonNull T defaultComponent) {
        var result = (EnumComponentMapper<T>) enumComponents.get(defaultComponent);
        if (result != null) {
            return result;
        }

        synchronized (this.components) {
            result = (EnumComponentMapper<T>) enumComponents.get(defaultComponent);
            if (result != null) {
                return result;
            }

            var delegate = (ComponentMapperImpl<T>) getComponents(defaultComponent.getClass());

            var mapper = new EnumComponentMapperImpl<>(delegate, defaultComponent);
            enumComponents.put(defaultComponent, mapper);

            return mapper;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Pooled> PooledComponentMapper<T> getPooledComponents(ClassType<T> type) {
        var metadata = componentManager.getPooledComponent(type);

        var result = (PooledComponentMapper<T>) this.components.get(metadata.id());
        if (result != null) {
            return result;
        }

        synchronized (this.components) {
            result = (PooledComponentMapper<T>) this.components.get(metadata.id());
            if (result != null) {
                return result;
            }

            var mapper = new PooledComponentMapperImpl<>(metadata);

            this.components.set(metadata.id(), mapper);
            this.componentMappers.put(type, mapper);

            return mapper;
        }
    }

    @Override
    public <R, T> ComponentRelationMapper<R, T> getComponentRelations(ComponentRelationType<R, T> relation) {
        return relationMapperManager.getComponentRelationMapper(relation);
    }

    @Override
    public <R extends Exclusive, T> ExclusiveComponentRelationMapper<R, T> getComponentRelations(ExclusiveComponentRelationType<R, T> relation) {
        return relationMapperManager.getComponentRelationMapper(relation);
    }

    @Override
    public <R> EntityRelationMapper<R> getEntityRelations(EntityRelationType<R> relation) {
        return relationMapperManager.getEntityRelationMapper(relation);
    }

    @Override
    public <R extends Exclusive> ExclusiveEntityRelationMapper<R> getEntityRelations(ExclusiveEntityRelationType<R> relation) {
        return relationMapperManager.getEntityRelationMapper(relation);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R, T> EntityRelationFetchMapper<R, T> getEntityRelations(EntityRelationFetchType<R, T> relation) {
        var result = (EntityRelationFetchMapper<R, T>) this.componentMappers.get(relation);
        if (result != null) {
            return result;
        }

        synchronized (this.componentMappers) {
            result = (EntityRelationFetchMapper<R, T>) this.componentMappers.get(relation);
            if (result != null) {
                return result;
            }

            var mapper = new EntityRelationFetchMapperImpl<>(relation);
            this.componentMappers.put(relation, mapper);

            return mapper;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R extends Exclusive, T> ExclusiveEntityRelationFetchMapper<R, T> getEntityRelations(ExclusiveEntityRelationFetchType<R, T> relation) {
        var result = (ExclusiveEntityRelationFetchMapper<R, T>) this.componentMappers.get(relation);
        if (result != null) {
            return result;
        }

        synchronized (this.componentMappers) {
            result = (ExclusiveEntityRelationFetchMapper<R, T>) this.componentMappers.get(relation);
            if (result != null) {
                return result;
            }

            var mapper = new ExclusiveEntityRelationFetchMapperImpl<>(relation);
            this.componentMappers.put(relation, mapper);

            return mapper;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends ComponentSet> ComponentSetMapper<T> getComponentSets(ComponentSetType<T> type) {
        var result = (ComponentSetMapper<T>) componentMappers.get(type);
        if (result != null) {
            return result;
        }

        synchronized (this.componentMappers) {
            result = (ComponentSetMapper<T>) componentMappers.get(type);
            if (result != null) {
                return result;
            }

            var mapper = new ComponentSetComponentsImpl<>(type);

            this.reclaimingComponents.add(mapper);
            this.componentMappers.put(type, mapper);

            return mapper;
        }
    }

    @SuppressWarnings("unchecked")
    private <T> Components<T, ComponentResult<T>> getWildcardComponents(Wildcard<T> wildcard) {
        var result = (Components<T, ComponentResult<T>>) componentMappers.get(wildcard);
        if (result != null) {
            return result;
        }

        synchronized (this.componentMappers) {
            result = (Components<T, ComponentResult<T>>) componentMappers.get(wildcard);
            if (result != null) {
                return result;
            }

            var mapper = new WildcardComponentsImpl<>(wildcard);

            this.reclaimingComponents.add(mapper);
            this.componentMappers.put(wildcard, mapper);

            return mapper;
        }
    }

    private class ComponentMapperImpl<T> implements ComponentMapper<T> {

        protected final ClassComponent<T> data;

        private final TransmutationManager.Add<T> add;
        private final TransmutationManager.Remove remove;

        protected ComponentMapperImpl(ClassComponent<T> data) {
            this.data = data;

            this.add = transmutationManager.getAddTransmuter(data.type());
            this.remove = transmutationManager.getRemoveTransmuter(data.type());
        }

        @Override
        public boolean has(int entityId) {
            return data.hasComponent(entityId);
        }

        @Override
        public T add(int entityId, T component) {
            this.add.apply(entityId, component);

            return component;
        }

        @Override
        public T get(int entityId) {
            return data.getComponent(entityId);
        }

        @Override
        public boolean remove(int entityId) {
            return this.remove.apply(entityId);
        }

    }

    private class EnumComponentMapperImpl<T extends Enum<T>> implements EnumComponentMapper<T> {

        private final ComponentMapper<T> delegate;
        private final T defaultComponent;

        public EnumComponentMapperImpl(ComponentMapper<T> delegate, T defaultComponent) {
            this.delegate = delegate;
            this.defaultComponent = defaultComponent;
        }

        @Override
        public @NonNull T add(int entityId) {
            return add(entityId, defaultComponent);
        }

        @Override
        public @NonNull T getDefault() {
            return defaultComponent;
        }

        @Override
        public boolean has(int entityId) {
            return this.delegate.has(entityId);
        }

        @Override
        public T add(int entityId, T component) {
            return this.delegate.add(entityId, component);
        }

        @Override
        public T get(int entityId) {
            return this.delegate.get(entityId);
        }

        @Override
        public boolean remove(int entityId) {
            return this.delegate.remove(entityId);
        }

    }

    private class PooledComponentMapperImpl<T extends Pooled> extends ComponentMapperImpl<T> implements PooledComponentMapper<T> {

        private final PooledComponentData<T> data;

        public PooledComponentMapperImpl(PooledComponentData<T> data) {
            super(data);

            this.data = data;
        }

        @Override
        public @NonNull T add(int entityId) {
            var component = get(entityId);
            if (component != null) {
                return component;
            }

            return add(entityId, getInstance());
        }

        @Override
        public T getInstance() {
            return data.getInstance();
        }

    }

    private final class EntityRelationFetchMapperImpl<R, T> implements EntityRelationFetchMapper<R, T> {

        private final EntityRelationMapper<R> relationMapper;
        private final Components<?, T> dataMapper;

        public EntityRelationFetchMapperImpl(EntityRelationFetchType<R, T> type) {
            this.relationMapper = getEntityRelations(relation(type.relationship()));
            this.dataMapper = getComponents(type.fetch());
        }

        @Override
        public boolean has(int entityId) {
            return relationMapper.has(entityId);
        }

        @Override
        public EntityRelationDataResult<R, T> get(int entityId) {
            var relations = relationMapper.get(entityId);
            if (relations == null) {
                return null;
            }

            return EntityRelationDataResultImpl.getInstance(relations, dataMapper);
        }

        @Override
        public boolean remove(int entityId) {
            return relationMapper.remove(entityId);
        }

    }

    private final class ExclusiveEntityRelationFetchMapperImpl<R extends Exclusive, T> implements ExclusiveEntityRelationFetchMapper<R, T> {

        private final ExclusiveEntityRelationMapper<R> relationMapper;
        private final Components<?, T> dataMapper;

        public ExclusiveEntityRelationFetchMapperImpl(ExclusiveEntityRelationFetchType<R, T> type) {
            this.relationMapper = getEntityRelations(exclusiveRelation(type.relationship()));
            this.dataMapper = getComponents(type.fetch());
        }

        @Override
        public boolean has(int entityId) {
            return relationMapper.has(entityId);
        }

        @Override
        public EntityRelationData<R, T> get(int entityId) {
            var relation = relationMapper.get(entityId);
            if (relation == null) {
                return null;
            }

            var data = dataMapper.get(relation.target());
            return Relation.create(relation.relationship(), relation.target(), data);
        }

        @Override
        public boolean remove(int entityId) {
            return relationMapper.remove(entityId);
        }

    }

    private class ComponentSetComponentsImpl<T extends ComponentSet> implements ComponentSetMapper<T>, PoolingComponents<T>, ReclaimingComponents {

        private final ComponentSetFactory<T> factory;
        private final ComponentData<T, ?, ?>[] componentTypes;
        private final int size;

        private final Components<?, ?>[] mappers;

        private final Bag<T> lent;
        private final Pool<Object[]> pool;

        @SuppressWarnings("unchecked")
        public ComponentSetComponentsImpl(ComponentSetType<T> type) {
            this.factory = ComponentSetsHelper.getFactory(type.componentSet());

            this.componentTypes = this.factory.getComponents().toArray(ComponentData[]::new);
            this.size = componentTypes.length;

            this.mappers = new Components<?, ?>[size];

            for (int i = 0; i < size; i++) {
                var componentType = componentTypes[i];

                this.mappers[i] = getComponents(componentType.type());
            }

            this.lent = new Bag<>(type.componentSet(), 8);
            this.pool = Pool.unbounded(Object[].class, () -> new Object[size], array -> Arrays.fill(array, null));
        }

        @Override
        public void free(T result) {
            result.free();
        }

        @Override
        public void reclaim() {
            var data = lent.getData();
            for (int i = 0, s = lent.getSize(); i < s; i++) {
                data[i].free();
            }
        }

        @Override
        public boolean has(int entityId) {
            for (int i = 0; i < size; i++) {
                var mapper = mappers[i];

                if (mapper.has(entityId)) {
                    return true;
                }
            }

            return false;
        }

        @Override
        public boolean hasAll(int entityId) {
            for (int i = 0; i < size; i++) {
                var mapper = mappers[i];

                if (!mapper.has(entityId)) {
                    return false;
                }
            }

            return true;
        }

        @Override
        @SuppressWarnings({ "rawtypes", "unchecked" })
        public void add(int entityId, @NonNull T components) {
            for (int i = 0; i < size; i++) {
                var componentType = componentTypes[i];
                var component = componentType.accessor().apply(components);
                if (component == null) {
                    continue;
                }

                switch (mappers[i]) {
                    case ComponentMapper mapper -> mapper.add(entityId, component);
                    case ExclusiveComponentRelationMapper<?, ?> relations -> relations.add(entityId, (ComponentRelation) component);
                    case ExclusiveEntityRelationMapper<?> relations -> relations.add(entityId, (EntityRelation) component);
                    default -> throw new UnsupportedOperationException("Add not supported for component type '%s'".formatted(componentType.type()));
                }
            }

            components.free();
        }

        @Override
        public T get(int entityId) {
            return pool.withInstance(components -> {
                var found = false;

                for (int i = 0; i < size; i++) {
                    var mapper = mappers[i];

                    var component = components[i] = mapper.get(entityId);
                    if (component != null) {
                        found = true;
                    }
                }

                if (!found) {
                    return null;
                }

                var result = factory.getInstance(entityId, components);
                lent.add(result);

                return result;
            });
        }

        @Override
        public boolean remove(int entityId) {
            var removed = false;

            for (int i = 0; i < size; i++) {
                var mapper = mappers[i];

                removed |= mapper.remove(entityId);
            }

            return removed;
        }

    }

    private class WildcardComponentsImpl<T> implements WildcardComponents<T>, PoolingComponents<ComponentResult<T>>, ReclaimingComponents {

        private final Wildcard<T> type;
        private final Bag<ComponentMapper<? extends T>> mappers;

        private final Pool<ComponentResultImpl<T>> pool = Pool.unbounded(ComponentResultImpl.class, this::createInstance);
        private final Bag<ComponentResultImpl<T>> lent = new Bag<>(ComponentResultImpl.class, 8);

        public WildcardComponentsImpl(Wildcard<T> type) {
            this.type = type;
            this.mappers = bagManager.createComponentBag(ComponentMapper.class);

            eventHandler.handleComponentEvents(type, this);
        }

        @Override
        public void free(ComponentResult<T> result) {
            if (result instanceof ComponentResultImpl<T> impl) {
                this.pool.free(impl);
            }
        }

        @Override
        public void reclaim() {
            var data = lent.getData();
            for (int i = 0, s = lent.getSize(); i < s; i++) {
                pool.free(data[i]);
            }
        }

        private ComponentResultImpl<T> createInstance() {
            return new ComponentResultImpl<>(type.bound(), mappers);
        }

        @Override
        public boolean has(int entityId) {
            var data = mappers.getData();
            for (int i = 0, s = mappers.getSize(); i < s; i++) {
                if (data[i].has(entityId)) {
                    return true;
                }
            }

            return false;
        }

        @Override
        public ComponentResult<T> get(int entityId) {
            var result = pool.getInstance().init(entityId);
            lent.add(result);

            return result;
        }

        @Override
        public boolean remove(int entityId) {
            var removed = false;

            var data = mappers.getData();
            for (int i = 0, s = mappers.getSize(); i < s; i++) {
                removed |= data[i].remove(entityId);
            }

            return removed;
        }

    }

    private class ComponentEventHandler {

        private final Map<Wildcard<?>, Bag<WildcardComponentsImpl<?>>> components = new ConcurrentHashMap<>();

        public ComponentEventHandler(EventManager eventManager) {
            eventManager.registerEventHandler(ComponentAddedEvent.class, this::handleComponentAdded);
        }

        @SuppressWarnings({ "unchecked", "rawtypes" })
        private void handleComponentAdded(ComponentAddedEvent event) {
            for (var entry : components.entrySet()) {
                if (ComponentUtils.matches(entry.getKey(), event.type())) {
                    var bags = entry.getValue();

                    var data = bags.getData();
                    for (int i = 0, s = bags.getSize(); i < s; i++) {
                        data[i].mappers.add((ComponentMapper) getComponents(event.type()));
                    }
                }
            }
        }

        @SuppressWarnings({ "unchecked", "rawtypes" })
        public void handleComponentEvents(Wildcard<?> type, WildcardComponentsImpl<?> wildcardComponents) {
            // Add known components
            var components = componentManager.getComponents();
            for (int i = 0, s = components.getSize(); i < s; i++) {
                var component = components.get(i);

                if (ComponentUtils.matches(type, component.type())) {
                    wildcardComponents.mappers.add((ComponentMapper) getComponents(component.type()));
                }
            }

            // Add bag to tracked bags
            var bags = this.components.computeIfAbsent(type, ignore -> new Bag<>(WildcardComponentsImpl.class, 32));
            bags.add(wildcardComponents);
        }

    }

}

@SuppressWarnings({ "unchecked", "rawtypes" })
class EntityRelationDataResultImpl implements EntityRelationDataResult, Pooled {

    private static final Pool<EntityRelationDataResultImpl> POOL = Pool.unbounded(EntityRelationDataResultImpl.class, EntityRelationDataResultImpl::new);

    private EntityRelationResult<?> result;
    private Components<?, ?> mapper;
    private boolean initialized;

    private final Bag<EntityRelationData<?, ?>> relations = new Bag<>(EntityRelationData.class, 8);

    static <R, T> EntityRelationDataResult<R, T> getInstance(EntityRelationResult<R> result, Components<?, T> mapper) {
        var instance = POOL.getInstance();
        instance.result = result;
        instance.mapper = mapper;
        instance.initialized = false;
        instance.relations.ensureCapacity(result.size());

        return instance;
    }

    @Override
    public @NonNull Object get(int i) {
        initialize();

        return this.relations.get(i);
    }

    @Override
    public int size() {
        return result.size();
    }

    @Override
    public boolean isEmpty() {
        return result.isEmpty();
    }

    @Override
    public Iterator iterator() {
        initialize();

        return new BagIterator<>(this.relations);
    }

    @Override
    public Object getRelationship(int target) {
        return result.getRelationship(target);
    }

    @Override
    public Object getData(int target) {
        initialize();

        var data = relations.getData();
        for (int i = 0, s = relations.getSize(); i < s; i++) {
            var relation = data[i];
            if (relation.target() == target) {
                return relation.data();
            }
        }

        return null;
    }

    private void initialize() {
        if (initialized) {
            return;
        }

        for (int i = 0, s = result.size(); i < s; i++) {
            var relation = result.get(i);
            var data = mapper.get(relation.target());

            this.relations.add(Relation.create(relation.relationship(), relation.target(), data));
        }

        this.initialized = true;
    }

    @Override
    public void reset() {
        this.result = null;
        this.mapper = null;
        this.initialized = false;

        this.relations.clear();
    }

}