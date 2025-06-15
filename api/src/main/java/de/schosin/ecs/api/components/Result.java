package de.schosin.ecs.api.components;

import java.util.Iterator;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import de.schosin.ecs.api.components.Relation.ComponentRelation;
import de.schosin.ecs.api.components.Relation.EntityRelation;
import de.schosin.ecs.api.components.Relation.EntityRelationData;
import de.schosin.ecs.api.components.types.ComponentType;
import de.schosin.ecs.api.components.types.RelationComponentType.ComponentRelationType;
import de.schosin.ecs.api.components.types.RelationComponentType.EntityRelationType;
import de.schosin.ecs.api.components.types.RelationFetchType.EntityRelationFetchType;
import de.schosin.ecs.api.components.types.Wildcard;
import de.schosin.ecs.api.components.types.WildcardRelationType.WildcardComponentRelationType;
import de.schosin.ecs.api.components.types.WildcardRelationType.WildcardEntityRelationFetchType;
import de.schosin.ecs.api.components.types.WildcardRelationType.WildcardEntityRelationType;

/**
 * Represents a result containing no, one or more components matching
 * the type bound {@code T}. 
 * 
 * <p>
 * <b>Note:</b> This interface extends both {@link Iterable} and {@link Iterator}.
 * When using it as an interable (e.g. in an enhanced for loop), it resets and
 * returns itself.
 * </p>
 * 
 * <p>
 * <b>Attention:</b> APIs may choose to pool these objects. Do not hold onto 
 * instances of Result, as they may be reused. See the documentation of APIs
 * accepting {@link ComponentType} for details on how long an instance of this
 * type can be used and when it will be reclaimed.
 * </p>
 * 
 * @param <T> type bound
 */
public interface Result<T> extends Iterable<T> {

    @SuppressWarnings("unchecked")
    static <T> Result<T> empty() {
        return EmptyResult.INSTANCE;
    }

    /**
     * Specilized type used by {@link Wildcard} that allows accessing a component
     * by its {@link Class}
     * 
     * @param <T> type of component
     */
    interface ComponentResult<T> extends Result<T> {

        /**
         * Retrieves a component that has the given {@code clazz}. The components
         * will be checked by {@code component.getClass() == clazz}.
         *  
         * @param <R> type of component
         * @param clazz class of component
         * @return matching component, or null if not present
         */
        @Nullable
        <R extends T> R get(Class<R> clazz);

    }

    /**
     * Specialized type used by {@link ComponentRelationType} that allows accessing
     * the relationship given an equal target component.
     * 
     * @param <R> type of relationship component
     * @param <T> type of target component
     */
    interface ComponentRelationResult<R, T> extends Result<ComponentRelation<R, T>> {

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
    interface EntityRelationResult<R> extends Result<EntityRelation<R>> {

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
    interface EntityRelationDataResult<R, T> extends Result<EntityRelationData<R, T>> {

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

    /**
     * Retrieves the component by its zero-based index.
     * 
     * @param i zero-based index
     * @return component instance
     * @throws ArrayIndexOutOfBoundsException if {@literal i >= size()}
     */
    @NonNull
    T get(int i);

    /**
     * @return number of components in this result
     */
    int size();

    /**
     * @return true if this result contains no components
     */
    boolean isEmpty();

}

@SuppressWarnings("rawtypes")
enum EmptyResult implements Result {

    INSTANCE;

    private static final Iterator EMPTY = new Iterator() {
        @Override
        public boolean hasNext() {
            return false;
        }

        @Override
        public Object next() {
            throw new UnsupportedOperationException();
        }
    };

    @Override
    public Iterator iterator() {
        return EMPTY;
    }

    @NonNull
    @Override
    public Object get(int i) {
        return null;
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public boolean isEmpty() {
        return true;
    }

}