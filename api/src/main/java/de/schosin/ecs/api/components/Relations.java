package de.schosin.ecs.api.components;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import org.jspecify.annotations.NonNull;

import de.schosin.ecs.api.Pooled;
import de.schosin.ecs.api.components.Relation.ComponentRelation;
import de.schosin.ecs.api.components.Relation.EntityRelation;
import de.schosin.ecs.api.components.Relation.EntityRelationData;
import de.schosin.ecs.api.components.Relation.Exclusive;
import de.schosin.ecs.api.components.Relations.ComponentRelations;
import de.schosin.ecs.api.components.Relations.EntityRelations;
import de.schosin.ecs.api.components.types.RelationComponentType.ComponentRelationType;
import de.schosin.ecs.api.components.types.RelationComponentType.EntityRelationType;
import de.schosin.ecs.api.components.types.RelationFetchType.EntityRelationFetchType;
import de.schosin.ecs.api.components.types.WildcardRelationType.WildcardComponentRelationType;
import de.schosin.ecs.api.components.types.WildcardRelationType.WildcardEntityRelationFetchType;
import de.schosin.ecs.api.components.types.WildcardRelationType.WildcardEntityRelationType;

public interface Relations<T extends Relation<?>> extends Result<T> {

    /**
     * Creates an instance for a single component relation.
     * 
     * <p>
     * This factory method is intended to be used for creating or modifying entities.
     * An instance can only be used once. For every entity a new instance has to
     * be created.
     * </p>
     */
    static <R, T> ComponentRelations<R, T> create(R relationship, T target) {
        return of(Relation.create(relationship, target));
    }

    /**
     * Creates an instance for a single entity relation.
     * 
     * <p>
     * This factory method is intended to be used for creating or modifying entities.
     * An instance can only be used once. For every entity a new instance has to
     * be created.
     * </p>
     */
    static <R> EntityRelations<R> create(R relationship, int target) {
        return of(Relation.create(relationship, target));
    }

    /**
     * Creates an instance for the component relations.
     * 
     * <p>
     * This factory method is intended to be used for creating or modifying entities.
     * An instance can only be used once. For every entity a new instance has to
     * be created.
     * </p>
     * 
     * <p>
     * Make sure to not assign the same {@link ComponentRelation relation instances} to multiple entities.
     * THe same rules apply as with {@link Pooled components}: When a relation 
     * is removed form an entity or the entity is deleted, the relation will be invalidated.
     * </p>
     */
    @SafeVarargs
    static <R, T> ComponentRelations<R, T> of(ComponentRelation<R, T>... relations) {
        if (relations == null || relations.length == 0) {
            throw new IllegalArgumentException("Relations must not be null or empty");
        }

        return RelationsHelper.create(relations);
    }

    /**
     * Creates an instance for the entity relations.
     * 
     * <p>
     * This factory method is intended to be used for creating or modifying entities.
     * An instance can only be used once. For every entity a new instance has to
     * be created.
     * </p>
     * 
     * <p>
     * Make sure to not assign the same {@link EntntityRelation relation instances} to multiple entities.
     * THe same rules apply as with {@link Pooled components}: When a relation 
     * is removed form an entity or the entity is deleted, the relation will be invalidated.
     * </p>
     */
    @SafeVarargs
    static <R> EntityRelations<R> of(EntityRelation<R>... relations) {
        if (relations == null || relations.length == 0) {
            throw new IllegalArgumentException("Relations must not be empty");
        }

        return RelationsHelper.create(relations);
    }

    /**
     * Returns a relation to its pool, allowing it to be reused.
     */
    static void free(Relations<?> relation) {
        RelationsHelper.free(relation);
    }

    /**
     * Specialized type used by {@link ComponentRelationType} that allows accessing
     * the relationship given an equal target component.
     * 
     * @param <R> type of relationship component
     * @param <T> type of target component
     */
    interface ComponentRelations<R, T> extends Relations<ComponentRelation<R, T>> {

        /**
         * Retrieves the relationship given the target component. The target will be
         * checked by {@link #equals(Object)} against existing relations, returning 
         * its relationship if it is equal.
         * 
         * <p>
         * In the case of {@link WildcardComponentRelationType} this method
         * will always return null, as a target component can have multiple 
         * relationships with the entity.
         * </p>
         * 
         * @param target target component
         * @return relationship component, or null if not present
         */
        R getRelationship(T target);

    }

    /**
     * Specialized type used by {@link EntityRelationType} that allows accessing
     * the relationship given a target entity.
     * 
     * @param <R> type of relationship component
     */
    interface EntityRelations<R> extends Relations<EntityRelation<R>> {

        /**
         * Retrieves the relationship given the target entity.
         * 
         * <p>
         * In the case of {@link WildcardEntityRelationType} this method
         * will always return null, as a target entity can have multiple 
         * relationships with the entity.
         * </p>
         * 
         * @param target target entity
         * @return relationship component, or null if not present
         */
        R getRelationship(int target);

    }

    /**
     * Specialized type used by {@link EntityRelationFetchType} and {@link WildcardEntityRelationFetchType}
     * that allows accessing components of the target entity.
     * 
     * @param <R> type of relationship component
     * @param <T> type of component retrieved from target entity
     */
    interface EntityRelationsData<R, T> extends Relations<EntityRelationData<R, T>> {

        /**
         * Retrieves the relationship given the target entity.
         * 
         * <p>
         * In the case of {@link WildcardEntityRelationType} this method
         * will always return null, as a target entity can have multiple 
         * relationships with the entity.
         * </p>
         * 
         * @param target target entity
         * @return relationship component, or null if not present
         */
        R getRelationship(int target);

        /**
         * Retrieves the data given the target entity.
         * 
         * <p>
         * In the case of {@link WildcardEntityRelationType} this method
         * will always return null, as a target entity can have multiple 
         * relationships with the entity.
         * </p>
         * 
         * @param target target entity
         * @return data of fetch component, or null unknown parent or no data for parent
         */
        T getData(int target);

    }

}

class RelationsHelper {

    private static final List<ComponentRelationsImpl> COMPONENT_RELATIONS = new ArrayList<>(32);
    private static final List<EntityRelationsImpl> ENTITY_RELATIONS = new ArrayList<>(32);

    @SuppressWarnings("unchecked")
    static synchronized <R, T> ComponentRelations<R, T> create(ComponentRelation<R, T>... relations) {
        var result = COMPONENT_RELATIONS.isEmpty() ? new ComponentRelationsImpl() : COMPONENT_RELATIONS.removeLast();

        for (var relation : relations) {
            if (relation.relationship() instanceof Exclusive) {
                throw new IllegalArgumentException("Cannot create ComponentRelations with exclusive relationship: %s".formatted(relation.relationship().getClass().getSimpleName()));
            }

            result.relations.add(relation);
        }

        return result;

    }

    @SuppressWarnings("unchecked")
    static synchronized <R> EntityRelations<R> create(EntityRelation<R>... relations) {
        var result = ENTITY_RELATIONS.isEmpty() ? new EntityRelationsImpl() : ENTITY_RELATIONS.removeLast();

        for (var relation : relations) {
            if (relation.relationship() instanceof Exclusive) {
                throw new IllegalArgumentException("Cannot create EntityRelations with exclusive relationship: %s".formatted(relation.relationship().getClass().getSimpleName()));
            }

            result.relations.add(relation);
        }

        return result;
    }

    static void free(Relations<?> relations) {
        if (relations instanceof ComponentRelationsImpl impl) {
            impl.relations.clear();

            COMPONENT_RELATIONS.add(impl);
            return;
        }

        if (relations instanceof EntityRelationsImpl impl) {
            impl.relations.clear();

            ENTITY_RELATIONS.add(impl);
            return;
        }

        // reaching here is an error, should probably be atleast a warning, better yet IllegalArgumentException
    }

    @SuppressWarnings("rawtypes")
    private static final class ComponentRelationsImpl implements ComponentRelations {

        private final List<ComponentRelation> relations = new ArrayList<>(2);

        @NonNull
        @Override
        public ComponentRelation get(int i) {
            return relations.get(i);
        }

        @Override
        public int size() {
            return relations.size();
        }

        @Override
        public boolean isEmpty() {
            return relations.isEmpty();
        }

        @Override
        public Iterator<ComponentRelation> iterator() {
            return relations.iterator();
        }

        @Override
        public Object getRelationship(Object target) {
            for (int i = 0, s = relations.size(); i < s; i++) {
                var relation = relations.get(i);
                if (Objects.equals(relation.target(), target)) {
                    return relation.relationship();
                }
            }

            return null;
        }

        @Override
        public String toString() {
            var builder = new StringBuilder().append("ComponentRelations(");

            for (int i = 0, s = relations.size(); i < s; i++) {
                if (i > 0) {
                    builder.append(", ");
                }

                builder.append(relations.get(i));
            }

            return builder.append(")").toString();
        }

    }

    @SuppressWarnings("rawtypes")
    private static final class EntityRelationsImpl implements EntityRelations {

        private final List<EntityRelation> relations = new ArrayList<>(2);

        @NonNull
        @Override
        public EntityRelation get(int i) {
            return relations.get(i);
        }

        @Override
        public int size() {
            return relations.size();
        }

        @Override
        public boolean isEmpty() {
            return relations.isEmpty();
        }

        @Override
        public Iterator<EntityRelation> iterator() {
            return relations.iterator();
        }

        @Override
        public Object getRelationship(int target) {
            for (int i = 0, s = relations.size(); i < s; i++) {
                var relation = relations.get(i);
                if (relation.target() == target) {
                    return relation.relationship();
                }
            }

            return null;
        }

        @Override
        public String toString() {
            var builder = new StringBuilder().append("EntityRelations(");

            for (int i = 0, s = relations.size(); i < s; i++) {
                if (i > 0) {
                    builder.append(", ");
                }

                builder.append(relations.get(i));
            }

            return builder.append(")").toString();
        }

    }

}