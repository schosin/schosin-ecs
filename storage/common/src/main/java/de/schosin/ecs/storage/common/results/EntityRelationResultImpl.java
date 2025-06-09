package de.schosin.ecs.storage.common.results;

import java.util.Iterator;
import java.util.Objects;

import org.jspecify.annotations.NonNull;

import de.schosin.ecs.api.Pooled;
import de.schosin.ecs.api.components.Relation;
import de.schosin.ecs.api.components.Relation.EntityRelation;
import de.schosin.ecs.api.components.Result.EntityRelationResult;
import de.schosin.ecs.utils.collections.Bag;
import de.schosin.ecs.utils.collections.Pool;

@SuppressWarnings("rawtypes")
public class EntityRelationResultImpl implements EntityRelationResult, Pooled {

    private static final Pool<EntityRelationResultImpl> POOL = Pool.unbounded(EntityRelationResultImpl.class, EntityRelationResultImpl::new);

    private final Bag<EntityRelation<?>> relations = new Bag<>(EntityRelation.class, 4);
    private final Bag<EntityRelation<?>> targetLookup = new Bag<>(EntityRelation.class, 4);

    private EntityRelationResultImpl() {
    }

    public static EntityRelationResultImpl getInstance() {
        return POOL.getInstance();
    }

    public void free() {
        POOL.free(this);
    }

    public synchronized void add(EntityRelation<?> relation) {
        targetLookup.set(relation.target(), relation);

        var data = relations.getData();
        for (int i = 0, s = relations.getSize(); i < s; i++) {
            var existing = data[i];
            if (Objects.equals(existing.target(), relation.target())) {
                relations.set(i, relation);

                return;
            }
        }

        this.relations.add(relation);
    }

    public void removeTarget(int target) {
        var relation = this.targetLookup.get(target);
        if (relation == null) {
            return;
        }

        this.targetLookup.set(target, null);
        this.relations.remove(relation);
    }

    public EntityRelation<?> removeLast() {
        return this.relations.removeLast();
    }

    @NonNull
    @Override
    public EntityRelation<?> get(int i) {
        return this.relations.get(i);
    }

    @Override
    public int size() {
        return relations.getSize();
    }

    @Override
    public boolean isEmpty() {
        return relations.isEmpty();
    }

    @Override
    public Object getRelationship(int target) {
        var data = relations.getData();
        for (int i = 0, s = relations.getSize(); i < s; i++) {
            var relation = data[i];
            if (relation.target() == target) {
                return relation.relationship();
            }
        }

        return null;
    }

    @Override
    public Iterator<? extends EntityRelation<?>> iterator() {
        return relations.iterator();
    }

    @Override
    public void reset() {
        for (int i = 0, s = relations.getSize(); i < s; i++) {
            Relation.free(relations.get(i));
        }

        this.relations.clear();
        this.targetLookup.clear();
    }

}
