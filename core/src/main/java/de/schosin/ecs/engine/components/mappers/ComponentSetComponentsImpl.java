package de.schosin.ecs.engine.components.mappers;

import java.util.Arrays;

import org.jspecify.annotations.NonNull;

import de.schosin.ecs.api.components.ComponentSet;
import de.schosin.ecs.api.components.ComponentSet.ComponentData;
import de.schosin.ecs.api.components.Relation.ComponentRelation;
import de.schosin.ecs.api.components.Relation.EntityRelation;
import de.schosin.ecs.api.components.mappers.ComponentMapper;
import de.schosin.ecs.api.components.mappers.ComponentRelations.ExclusiveComponentRelationMapper;
import de.schosin.ecs.api.components.mappers.ComponentSetMapper;
import de.schosin.ecs.api.components.mappers.Components;
import de.schosin.ecs.api.components.mappers.EntityRelations.ExclusiveEntityRelationMapper;
import de.schosin.ecs.api.components.types.ComponentSetType;
import de.schosin.ecs.engine.components.ComponentMapperManager;
import de.schosin.ecs.engine.components.ComponentMapperManager.PoolingComponents;
import de.schosin.ecs.engine.utils.components.ComponentSetsHelper;
import de.schosin.ecs.engine.utils.components.ComponentSetsHelper.ComponentSetFactory;
import de.schosin.ecs.utils.collections.Bag;
import de.schosin.ecs.utils.collections.Pool;

public class ComponentSetComponentsImpl<T extends ComponentSet<?>> implements ComponentSetMapper<T>, PoolingComponents<T> {

    private final ComponentSetFactory<T> factory;
    private final ComponentData<T, ?, ?>[] componentTypes;
    private final int size;

    private final Components<?, ?>[] mappers;

    private final Bag<T> lent;
    private final Pool<Object[]> pool;

    @SuppressWarnings("unchecked")
    public ComponentSetComponentsImpl(ComponentSetType<T, ?> type, ComponentMapperManager componentMapperManager) {
        this.factory = ComponentSetsHelper.getFactory(type.componentSet());

        this.componentTypes = this.factory.getComponents().toArray(ComponentData[]::new);
        this.size = componentTypes.length;

        this.mappers = new Components<?, ?>[size];

        for (int i = 0; i < size; i++) {
            var componentType = componentTypes[i];

            this.mappers[i] = componentMapperManager.getComponents(componentType.type());
        }

        this.lent = new Bag<>(type.componentSet(), 8);
        this.pool = Pool.unbounded(Object[].class, () -> new Object[size], array -> Arrays.fill(array, null));
    }

    @Override
    public void free(T result) {
        lent.removeIdentity(result);
        result.free();
    }

    @Override
    public void reclaim() {
        var data = lent.getData();
        for (int i = 0, s = lent.getSize(); i < s; i++) {
            data[i].free();
        }

        lent.clear();
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
