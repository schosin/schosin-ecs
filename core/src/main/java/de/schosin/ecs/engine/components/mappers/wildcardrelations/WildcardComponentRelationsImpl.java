package de.schosin.ecs.engine.components.mappers.wildcardrelations;

import java.util.Iterator;

import org.jspecify.annotations.NonNull;

import de.schosin.ecs.api.Pooled;
import de.schosin.ecs.api.components.Relation.ComponentRelation;
import de.schosin.ecs.api.components.Result;
import de.schosin.ecs.api.components.mappers.ComponentRelations;
import de.schosin.ecs.api.components.mappers.WildcardRelations.WildcardComponentRelations;
import de.schosin.ecs.engine.BagManager;
import de.schosin.ecs.engine.components.ComponentMapperManager.PoolingComponents;
import de.schosin.ecs.engine.components.ComponentMapperManager.WildcardMapper;
import de.schosin.ecs.utils.collections.Bag;
import de.schosin.ecs.utils.collections.Pool;

public class WildcardComponentRelationsImpl<R, T>
        implements WildcardComponentRelations<R, T>, PoolingComponents<Result<ComponentRelation<? extends R, ? extends T>>>, WildcardMapper<ComponentRelations<R, T, ?>> {

    private final Bag<ComponentRelations<R, T, ?>> mappers;

    private final Pool<ComponentRelationResultImpl<R, T>> pool = Pool.unbounded(ComponentRelationResultImpl.class, this::createResultInstance);
    private final Bag<ComponentRelationResultImpl<R, T>> lent = new Bag<>(ComponentRelationResultImpl.class, 8);

    public WildcardComponentRelationsImpl(BagManager bagManager) {
        this.mappers = bagManager.createComponentBag(ComponentRelations.class);
    }

    @Override
    public void addMapper(ComponentRelations<R, T, ?> components) {
        this.mappers.add(components);
    }

    @Override
    public void free(Result<ComponentRelation<? extends R, ? extends T>> result) {
        if (result instanceof ComponentRelationResultImpl<R, T> impl) {
            this.lent.removeIdentity(impl);
            this.pool.free(impl);
        }
    }

    @Override
    public void reclaim() {
        var data = lent.getData();
        for (int i = 0, s = lent.getSize(); i < s; i++) {
            pool.free(data[i]);
        }

        lent.clear();
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
    public Result<ComponentRelation<? extends R, ? extends T>> get(int entityId) {
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

    private ComponentRelationResultImpl<R, T> createResultInstance() {
        return new ComponentRelationResultImpl<>(mappers);
    }

    private static class ComponentRelationResultImpl<R, T> implements Result<ComponentRelation<? extends R, ? extends T>>, Pooled {

        private final Bag<ComponentRelations<R, T, ?>> mappers;
        private final Bag<ComponentRelation<? extends R, ? extends T>> components;

        private int entityId = -1;
        private int size = -1;

        public ComponentRelationResultImpl(Bag<ComponentRelations<R, T, ?>> mappers) {
            this.mappers = mappers;
            this.components = new Bag<>(ComponentRelation.class, 4);
        }

        public ComponentRelationResultImpl<R, T> init(int entityId) {
            this.entityId = entityId;

            return this;
        }

        @Override
        public @NonNull ComponentRelation<? extends R, ? extends T> get(int i) {
            if (i >= size()) {
                throw new ArrayIndexOutOfBoundsException(i);
            }

            return this.components.get(i);
        }

        @Override
        @SuppressWarnings("unchecked")
        public int size() {
            if (size == -1) {
                var data = mappers.getData();
                for (int i = 0, s = mappers.getSize(); i < s; i++) {
                    var component = data[i].get(entityId);
                    if (component == null) {
                        continue;
                    }

                    switch (component) {
                        case ComponentRelation<?, ?> relation -> components.add((ComponentRelation<R, T>) relation);
                        case ComponentRelationResult<?, ?> result -> {
                            for (var relation : result) {
                                components.add((ComponentRelation<R, T>) relation);
                            }
                        }
                        default -> throw new IllegalStateException("Unexpected component of type '%s'".formatted(component.getClass().getName()));
                    }
                }

                this.size = components.getSize();
            }

            return size;
        }

        @Override
        public boolean isEmpty() {
            return size() == 0;
        }

        @Override
        public Iterator<ComponentRelation<? extends R, ? extends T>> iterator() {
            size();
            return this.components.iterator();
        }

        @Override
        public void reset() {
            this.entityId = -1;
            this.size = -1;

            this.components.clear();
        }

    }

}
