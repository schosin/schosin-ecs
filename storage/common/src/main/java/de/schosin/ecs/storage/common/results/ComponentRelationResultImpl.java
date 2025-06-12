package de.schosin.ecs.storage.common.results;

import java.util.Iterator;
import java.util.Objects;

import org.jspecify.annotations.NonNull;

import de.schosin.ecs.api.Pooled;
import de.schosin.ecs.api.components.Relation;
import de.schosin.ecs.api.components.Relation.ComponentRelation;
import de.schosin.ecs.api.components.Result.ComponentRelationResult;
import de.schosin.ecs.utils.collections.Bag;
import de.schosin.ecs.utils.collections.Pool;

@SuppressWarnings("rawtypes")
public class ComponentRelationResultImpl implements ComponentRelationResult, Pooled {

    private static final Pool<ComponentRelationResultImpl> POOL = Pool.unbounded(ComponentRelationResultImpl.class, ComponentRelationResultImpl::new);

    private final Bag<ComponentRelation<?, ?>> relations = new Bag<>(ComponentRelation.class, 4);

    private ComponentRelationResultImpl() {
    }

    public static ComponentRelationResultImpl getInstance() {
        return POOL.getInstance();
    }

    public void free() {
        POOL.free(this);
    }

    public synchronized void add(ComponentRelation<?, ?> relation) {
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

    public ComponentRelation<?, ?> removeLast() {
        return this.relations.removeLast();
    }

    @NonNull
    @Override
    public ComponentRelation<?, ?> get(int i) {
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
    public Object getRelationship(Object target) {
        var data = relations.getData();
        for (int i = 0, s = relations.getSize(); i < s; i++) {
            var relation = data[i];
            if (Objects.equals(relation.target(), target)) {
                return relation.relationship();
            }
        }

        return null;
    }

    @Override
    public Iterator<? extends ComponentRelation<?, ?>> iterator() {
        return relations.iterator();
    }

    @Override
    public void reset() {
        for (int i = 0, s = this.relations.getSize(); i < s; i++) {
            Relation.free(this.relations.get(i));
        }

        this.relations.clear();
    }

    @Override
    public String toString() {
        return new StringBuilder()
                .append("ComponentRelationResultImpl(")
                .append("relations = ").append(this.relations)
                .append(")")
                .toString();
    }

}
