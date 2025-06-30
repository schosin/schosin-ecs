package de.schosin.ecs.engine.utils.components;

import java.util.Objects;
import java.util.function.IntFunction;

import de.schosin.ecs.api.Pooled;
import de.schosin.ecs.api.components.Relation.EntityRelationData;
import de.schosin.ecs.api.components.types.RelationComponentType.RegularEntityRelationType;
import de.schosin.ecs.utils.collections.Pool;

@SuppressWarnings("rawtypes")
public class EntityRelationDataImpl<R, T> implements EntityRelationData<R, T>, Pooled {

    private static final Pool<EntityRelationDataImpl> POOL = Pool.unbounded(EntityRelationDataImpl.class, EntityRelationDataImpl::new);

    @SuppressWarnings("unchecked")
    public static <R, T> EntityRelationData<R, T> getInstance(EntityRelation<R> relation, IntFunction<T> dataFunction) {
        var instance = POOL.getInstance();
        instance.relation = relation;
        instance.dataFunction = dataFunction;

        return instance;
    }

    public static void free(EntityRelationData<?, ?> relation) {
        if (relation instanceof EntityRelationDataImpl impl) {
            POOL.free(impl);
        }
    }

    private EntityRelation<R> relation;
    private IntFunction<T> dataFunction;

    private T data;

    @Override
    public RegularEntityRelationType<R, ?> type() {
        return relation.type();
    }

    @Override
    public R relationship() {
        return relation.relationship();
    }

    @Override
    public int target() {
        return relation.target();
    }

    @Override
    public T data() {
        if (dataFunction != null) {
            this.data = dataFunction.apply(relation.target());
            this.dataFunction = null;
        }

        return data;
    }

    @Override
    public void reset() {
        this.relation = null;
        this.dataFunction = null;
    }

    // DO NOT REMOVE - easier assertj-tests with extracting(...) requires bean-like accessors
    public Object getRelationship() {
        return relationship();
    }

    // DO NOT REMOVE - easier assertj-tests with extracting(...) requires bean-like accessors
    public int getTarget() {
        return target();
    }

    // DO NOT REMOVE - easier assertj-tests with extracting(...) requires bean-like accessors
    public Object getData() {
        return data();
    }

    @Override
    public String toString() {
        return new StringBuilder()
                .append("EntityRelationDataImpl(")
                .append("relationship = ").append(relationship()).append(", ")
                .append("target = ").append(target()).append(", ")
                .append("data = ").append(data())
                .append(")")
                .toString();
    }

    @Override
    public int hashCode() {
        return Objects.hash(relation);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof EntityRelation other && other.equals(this.relation);
    }

}
