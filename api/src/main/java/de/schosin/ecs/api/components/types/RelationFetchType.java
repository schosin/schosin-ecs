package de.schosin.ecs.api.components.types;

import de.schosin.ecs.api.components.Relation;
import de.schosin.ecs.api.components.Relation.EntityRelationData;
import de.schosin.ecs.api.components.Result.EntityRelationDataResult;

public sealed interface RelationFetchType<R, T, X> extends ComponentType<EntityRelationData<R, T>, X> {

    Class<R> relationship();

    record EntityRelationFetchType<R, T>(Class<R> relationship, ComponentType<?, T> fetch) implements RelationFetchType<R, T, EntityRelationDataResult<R, T>> {
        public EntityRelationFetchType {
            RelationComponentTypeHelper.validateNonExclusiveEntityRelationship(relationship);
        }

        @Override
        public final String toString() {
            return "EntityRelationFetchType(%s -> %s)".formatted(relationship.getSimpleName(), fetch);
        }
    }

    record ExclusiveEntityRelationFetchType<R extends Relation.Exclusive, T>(Class<R> relationship, ComponentType<?, T> fetch) implements RelationFetchType<R, T, EntityRelationData<R, T>> {
        public ExclusiveEntityRelationFetchType {
            RelationComponentTypeHelper.validateEntityRelationship(relationship);
        }

        @Override
        public final String toString() {
            return "ExclusiveEntityRelationFetchType(%s -> %s)".formatted(relationship.getSimpleName(), fetch);
        }
    }

}
