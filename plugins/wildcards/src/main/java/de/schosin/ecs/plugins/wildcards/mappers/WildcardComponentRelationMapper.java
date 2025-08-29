package de.schosin.ecs.plugins.wildcards.mappers;

import java.util.Arrays;
import java.util.Iterator;
import java.util.function.IntFunction;

import org.jspecify.annotations.NonNull;

import de.schosin.ecs.api.Pooled;
import de.schosin.ecs.api.components.Relation.ComponentRelation;
import de.schosin.ecs.api.components.mappers.ComponentRelationMappers;
import de.schosin.ecs.api.components.mappers.ComponentRelationMappers.ComponentRelationMapper;
import de.schosin.ecs.api.components.mappers.ComponentRelationMappers.ExclusiveComponentRelationMapper;
import de.schosin.ecs.api.components.mappers.CustomComponentMapper;
import de.schosin.ecs.api.data.ArchetypeComponentAccessor;
import de.schosin.ecs.api.data.ComponentAccessor;
import de.schosin.ecs.api.data.DataAccessor;
import de.schosin.ecs.engine.components.ComponentMapperManager.ReclaimingComponents;
import de.schosin.ecs.plugins.wildcards.WildcardManager.WildcardMapper;
import de.schosin.ecs.plugins.wildcards.result.WildcardComponentRelations;
import de.schosin.ecs.utils.collections.Bag;
import de.schosin.ecs.utils.collections.IntBag;
import de.schosin.ecs.utils.collections.Pool;

public class WildcardComponentRelationMapper<R, T> implements CustomComponentMapper<ComponentRelation<R, T>, WildcardComponentRelations<R, T>>,
        ReclaimingComponents, WildcardMapper<ComponentRelationMappers<R, T, ?>> {

    private final IntFunction<DataAccessor> accessor;

    private final Bag<ComponentRelationMapper<R, T>> mappers = new Bag<>(ComponentRelationMapper.class, 4);
    private final IntBag componentIds = new IntBag(4);

    @SuppressWarnings("rawtypes")
    private final Bag<ExclusiveComponentRelationMapper> exclusiveMappers = new Bag<>(ExclusiveComponentRelationMapper.class, 4);
    private final IntBag exclusiveComponentIds = new IntBag(4);

    private final Pool<WildcardComponentRelationAccessor> pool = Pool.unbounded(WildcardComponentRelationAccessor.class, WildcardComponentRelationAccessor::new);
    private final Bag<WildcardComponentRelationAccessor> lent = new Bag<>(WildcardComponentRelationAccessor.class, 8);

    public WildcardComponentRelationMapper(IntFunction<DataAccessor> accessor) {
        this.accessor = accessor;
    }

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void addMapper(ComponentRelationMappers<R, T, ?> components) {
        switch (components) {
            case ComponentRelationMapper mapper -> {
                this.mappers.add(mapper);
                this.componentIds.add(components.componentId());
            }
            case ExclusiveComponentRelationMapper<?, ?> exclusiveMapper -> {
                this.exclusiveMappers.add(exclusiveMapper);
                this.exclusiveComponentIds.add(components.componentId());
            }
        }
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
        try (var accessor = this.accessor.apply(entityId)) {
            for (int i = 0, s = exclusiveComponentIds.getSize(); i < s; i++) {
                if (accessor.hasComponent(exclusiveComponentIds.get(i))) {
                    return true;
                }
            }

            for (int i = 0, s = componentIds.getSize(); i < s; i++) {
                if (accessor.hasComponent(componentIds.get(i))) {
                    return true;
                }
            }

            return false;
        }
    }

    @Override
    public WildcardComponentRelations<R, T> get(int entityId) {
        var accessor = this.accessor.apply(entityId);

        var componentAccessor = getComponentAccessor(accessor);
        lent.add(componentAccessor);

        return componentAccessor.getComponent(accessor);
    }

    @Override
    public WildcardComponentRelationAccessor getComponentAccessor(DataAccessor accessor) {
        return pool.getInstance();
    }

    @Override
    public ArchetypeComponentAccessor<WildcardComponentRelations<R, T>> getArchetypeComponentAccessor(DataAccessor accessor) {
        // TODO implement
        var todo = true;

        throw new UnsupportedOperationException("getArchetypeComponentAccessor(%s) not implemented yet".formatted(accessor));
    }

    @Override
    public boolean remove(int entityId) {
        var removed = false;

        var data = mappers.getData();
        for (int i = 0, s = mappers.getSize(); i < s; i++) {
            removed |= data[i].remove(entityId);
        }

        var exclusiveData = exclusiveMappers.getData();
        for (int i = 0, s = exclusiveMappers.getSize(); i < s; i++) {
            removed |= exclusiveData[i].remove(entityId);
        }

        return removed;
    }

    private class WildcardComponentRelationAccessor implements WildcardComponentRelations<R, T>, Iterator<ComponentRelation<R, T>>, ComponentAccessor<WildcardComponentRelations<R, T>>, Pooled {

        private final Bag<ComponentAccessor<ComponentRelation<R, T>>> exclusiveAccessors = new Bag<>(ComponentAccessor.class, 4);
        private final Bag<ComponentAccessor<ComponentRelations<R, T>>> accessors = new Bag<>(ComponentAccessor.class, 4);

        private final IntBag accessorIndex = new IntBag(4);
        private final IntBag dataIndex = new IntBag(4);

        private DataAccessor accessor;

        private int exclusiveSize = -1;
        private int totalSize = -1;

        private int id = -1;

        private WildcardComponentRelationAccessor() {
            Arrays.fill(this.accessorIndex.getData(), -1);
            Arrays.fill(this.dataIndex.getData(), -1);
        }

        @Override
        public WildcardComponentRelations<R, T> getComponent(DataAccessor accessor) {
            reset();
            this.accessor = accessor;

            return this;
        }

        @Override
        public @NonNull ComponentRelation<R, T> get(int i) {
            if (i >= size()) {
                throw new ArrayIndexOutOfBoundsException(i);
            }

            if (i < exclusiveSize) {
                return this.exclusiveAccessors.get(i).getComponent(accessor);
            }

            var index = i - exclusiveSize;
            var componentAccessor = this.accessors.get(this.accessorIndex.get(index));
            var relations = componentAccessor.getComponent(accessor);

            return relations.get(this.dataIndex.get(index));
        }

        @Override
        @SuppressWarnings("unchecked")
        public int size() {
            if (totalSize == -1) {
                this.exclusiveSize = 0;

                for (int i = 0, s = exclusiveMappers.getSize(); i < s; i++) {
                    var componentId = exclusiveComponentIds.get(i);

                    if (accessor.hasComponent(componentId)) {
                        this.exclusiveSize++;
                        this.exclusiveAccessors.add(exclusiveMappers.get(i).getComponentAccessor(accessor));
                    }
                }

                var size = 0;
                for (int i = 0, s = mappers.getSize(); i < s; i++) {
                    var componentAccessor = mappers.get(i).getComponentAccessor(accessor);
                    this.accessors.add(componentAccessor);

                    var relations = componentAccessor.getComponent(accessor);
                    if (relations != null) {
                        for (int r = 0, rs = relations.size(); r < rs; r++) {
                            size++;
                            this.accessorIndex.add(i);
                            this.dataIndex.add(r);
                        }
                    }
                }

                this.totalSize = exclusiveSize + size;
            }

            return totalSize;
        }

        @Override
        public boolean isEmpty() {
            return size() == 0;
        }

        @Override
        public Iterator<ComponentRelation<R, T>> iterator() {
            this.id = 0;
            return this;
        }

        @Override
        public boolean hasNext() {
            return id < size();
        }

        @Override
        public ComponentRelation<R, T> next() {
            return get(id++);
        }

        @Override
        public R getRelationship(T target) {
            return null;
        }

        @Override
        public void free() {
            pool.free(this);
        }

        @Override
        public void reset() {
            this.accessorIndex.clear();
            Arrays.fill(this.accessorIndex.getData(), -1);

            this.dataIndex.clear();
            Arrays.fill(this.dataIndex.getData(), -1);

            for (int i = 0; i < exclusiveSize; i++) {
                exclusiveAccessors.get(i).free();
            }
            exclusiveAccessors.clear();

            for (int i = 0, s = accessors.getSize(); i < s; i++) {
                accessors.get(i).free();
            }
            accessors.clear();

            this.accessor = null;

            this.exclusiveSize = -1;
            this.totalSize = -1;

            this.id = -1;
        }

        @Override
        public String toString() {
            if (accessor == null) {
                return "ComponentRelations(invalidated)";
            }

            var builder = new StringBuilder().append("ComponentRelations(");
            for (int i = 0, s = size(); i < s; i++) {
                if (i > 0) {
                    builder.append(", ");
                }

                builder.append(get(i));
            }
            return builder.append(")").toString();
        }

    }

}
