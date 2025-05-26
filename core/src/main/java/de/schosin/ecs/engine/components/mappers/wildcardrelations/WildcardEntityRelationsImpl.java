package de.schosin.ecs.engine.components.mappers.wildcardrelations;

import java.util.Iterator;

import org.jspecify.annotations.NonNull;

import de.schosin.ecs.api.Pooled;
import de.schosin.ecs.api.components.Relation.EntityRelation;
import de.schosin.ecs.api.components.Result;
import de.schosin.ecs.api.components.mappers.EntityRelations;
import de.schosin.ecs.api.components.mappers.WildcardRelations.WildcardEntityRelations;
import de.schosin.ecs.engine.BagManager;
import de.schosin.ecs.engine.components.ComponentMapperManager.PoolingComponents;
import de.schosin.ecs.engine.components.ComponentMapperManager.WildcardMapper;
import de.schosin.ecs.utils.collections.Bag;
import de.schosin.ecs.utils.collections.BagIterator;
import de.schosin.ecs.utils.collections.Pool;

public class WildcardEntityRelationsImpl<R> implements WildcardEntityRelations<R>, PoolingComponents<Result<EntityRelation<? extends R>>>, WildcardMapper<EntityRelations<R, ?>> {

    private final Bag<EntityRelations<R, ?>> mappers;

    private final Pool<EntityRelationResultImpl<R>> pool = Pool.unbounded(EntityRelationResultImpl.class, this::createResultInstance);
    private final Bag<EntityRelationResultImpl<R>> lent = new Bag<>(EntityRelationResultImpl.class, 8);

    public WildcardEntityRelationsImpl(BagManager bagManager) {
        this.mappers = bagManager.createComponentBag(EntityRelations.class);
    }

    @Override
    public void addMapper(EntityRelations<R, ?> components) {
        this.mappers.add(components);
    }

    @Override
    public void free(Result<EntityRelation<? extends R>> result) {
        if (result instanceof EntityRelationResultImpl<R> impl) {
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
    public Result<EntityRelation<? extends R>> get(int entityId) {
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

    private EntityRelationResultImpl<R> createResultInstance() {
        return new EntityRelationResultImpl<>(mappers);
    }

    private static class EntityRelationResultImpl<R> implements Result<EntityRelation<? extends R>>, Pooled {

        private final Bag<EntityRelations<R, ?>> mappers;
        private final Bag<EntityRelation<? extends R>> components;

        private final ThreadLocal<BagIterator<EntityRelation<? extends R>>> iterator = ThreadLocal.withInitial(BagIterator::new);

        private int entityId = -1;
        private int size = -1;

        public EntityRelationResultImpl(Bag<EntityRelations<R, ?>> mappers) {
            this.mappers = mappers;
            this.components = new Bag<>(EntityRelation.class, 4);
        }

        public EntityRelationResultImpl<R> init(int entityId) {
            this.entityId = entityId;

            return this;
        }

        @Override
        public @NonNull EntityRelation<? extends R> get(int i) {
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
                        case EntityRelation<?> relation -> components.add((EntityRelation<R>) relation);
                        case EntityRelationResult<?> result -> {
                            for (var relation : result) {
                                components.add((EntityRelation<R>) relation);
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
        public Iterator<EntityRelation<? extends R>> iterator() {
            size();
            return iterator.get().init(this.components);
        }

        @Override
        public void reset() {
            this.entityId = -1;
            this.size = -1;

            this.components.clear();
        }

    }

}
