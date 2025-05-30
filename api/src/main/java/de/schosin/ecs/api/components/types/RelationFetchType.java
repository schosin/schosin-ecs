package de.schosin.ecs.api.components.types;

import de.schosin.ecs.api.components.Relation;
import de.schosin.ecs.api.components.Relation.EntityRelationData;
import de.schosin.ecs.api.components.Result.EntityRelationDataResult;

/**
 * Describes entity fetch relations.
 * 
 * <ol>
 * <li><b>{@link EntityRelationFetchType}:</b> Describes a non-exclusive entity fetch relation</li>
 * <li><b>{@link ExclusiveEntityRelationFetchType}:</b> Describes an exclusive entity fetch relation</li>
 * </ol>
 * 
 * @param <R> type of relationship
 * @param <T> type of fetched data of target entity
 * @param <X> maps to {@link ComponentType} {@code R} (read operations)
 */
public sealed interface RelationFetchType<R, T, X> extends ComponentType<EntityRelationData<R, T>, X> {

    Class<R> relationship();

    /**
     * Describes a non-exclusive entity fetch relation, consisting of a {@link EntityRelationFetchType#relationship relationship component},
     * and a {@link EntityRelationFetchType#fetch fetch type}. The fetch type describes the component that is fetched for the
     * target entity.
     * 
     * @param <R> type of relationship component, must not extend {@link Relation.Exclusive}
     * @param <T> type of fetched data of target entity
     */
    record EntityRelationFetchType<R, T>(Class<R> relationship, ComponentType<?, T> fetch) implements RelationFetchType<R, T, EntityRelationDataResult<R, T>> {
        public EntityRelationFetchType {
            RelationComponentTypeHelper.validateNonExclusiveEntityRelationship(relationship);
        }

        @Override
        public boolean matches(ComponentType<?, ?> otherType) {
            return this.equals(otherType);
        }

        @Override
        public final String toString() {
            return "EntityRelationFetchType(%s -> %s)".formatted(relationship.getSimpleName(), fetch);
        }
    }

    /**
     * Describes an exclusive entity fetch relation, consisting of a {@link ExclusiveEntityRelationFetchType#relationship relationship component},
     * and a {@link ExclusiveEntityRelationFetchType#fetch fetch type}. The fetch type describes the component that is fetched for the
     * target entity.
     * 
     * @param <R> type of relationship component, must not extend {@link Relation.Exclusive}
     * @param <T> type of fetched data of target entity
     */
    record ExclusiveEntityRelationFetchType<R extends Relation.Exclusive, T>(Class<R> relationship, ComponentType<?, T> fetch) implements RelationFetchType<R, T, EntityRelationData<R, T>> {
        public ExclusiveEntityRelationFetchType {
            RelationComponentTypeHelper.validateEntityRelationship(relationship);
        }

        @Override
        public boolean matches(ComponentType<?, ?> otherType) {
            return this.equals(otherType);
        }

        @Override
        public final String toString() {
            return "ExclusiveEntityRelationFetchType(%s -> %s)".formatted(relationship.getSimpleName(), fetch);
        }
    }

}
