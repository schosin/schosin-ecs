package de.schosin.ecs.storage.common.results;

import de.schosin.ecs.api.components.Relation;
import de.schosin.ecs.api.components.types.RelationComponentType;
import de.schosin.ecs.api.components.types.RelationComponentType.RegularComponentRelationType;
import de.schosin.ecs.api.components.types.RelationComponentType.RegularEntityRelationType;

public sealed interface StorageRelationResult<T extends Relation<?>> permits ComponentRelationResultImpl, EntityRelationResultImpl {

    static StorageRelationResult<?> getInstance(RelationComponentType<?, ?, ?> componentType) {
        return switch (componentType) {
            case RegularComponentRelationType<?, ?, ?> type -> ComponentRelationResultImpl.getInstance();
            case RegularEntityRelationType<?, ?> type -> EntityRelationResultImpl.getInstance();
        };
    }

    void add(T relation);

    boolean isEmpty();
    
    T removeLast();
    
}
