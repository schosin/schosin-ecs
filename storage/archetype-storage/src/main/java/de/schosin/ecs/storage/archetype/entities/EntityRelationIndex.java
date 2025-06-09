package de.schosin.ecs.storage.archetype.entities;

import de.schosin.ecs.api.components.Relation.EntityRelation;
import de.schosin.ecs.api.components.Result.EntityRelationResult;
import de.schosin.ecs.api.components.types.RelationComponentType.EntityRelationType;
import de.schosin.ecs.api.components.types.RelationComponentType.ExclusiveEntityRelationType;
import de.schosin.ecs.api.components.types.RelationComponentType.RegularEntityRelationType;
import de.schosin.ecs.storage.api.StorageWorld;
import de.schosin.ecs.storage.api.components.Component.EntityRelationComponent.RemovedRelationTypeHandler;
import de.schosin.ecs.storage.common.results.EntityRelationResultImpl;
import de.schosin.ecs.utils.collections.Bag;
import de.schosin.ecs.utils.collections.BitVector;
import de.schosin.ecs.utils.collections.IntBag;

public class EntityRelationIndex {

    private final Bag<IntBag> lookup;
    private final BitVector targets = new BitVector();

    public EntityRelationIndex(StorageWorld world) {
        this.lookup = world.createEntityBag(IntBag.class);
    }

    public void add(int entityId, RegularEntityRelationType<?, ?> relationType, Object component) {
        switch (relationType) {
            case EntityRelationType<?> type -> add(entityId, (EntityRelationResult<?>) component);
            case ExclusiveEntityRelationType<?> type -> add(entityId, (EntityRelation<?>) component);
        }
    }

    private void add(int entityId, EntityRelationResult<?> relations) {
        for (int i = 0, s = relations.size(); i < s; i++) {
            add(entityId, relations.get(i));
        }
    }

    private void add(int entityId, EntityRelation<?> relation) {
        var targetId = relation.target();

        // Add to bit vector for fast lookup
        this.targets.set(targetId);

        // Retrieve related bag
        var related = lookup.get(targetId);
        if (related == null) {
            synchronized (lookup) {
                related = lookup.get(targetId);
                if (related == null) {
                    related = new IntBag(8);
                    lookup.set(targetId, related);
                }
            }
        }

        // Track related entity
        related.add(entityId);
    }

    public void removeTarget(int targetId, RemovedRelationTypeHandler handler, EntityIndex entityIndex) {
        // Check bit vector (bloom filter possibly?)
        if (!this.targets.get(targetId)) {
            return;
        }

        this.targets.clear(targetId);

        // Get related entities
        var related = this.lookup.get(targetId);

        // Add to affected entities, clear related entities for reuse
        synchronized (related) {
            for (int i = 0, s = related.getSize(); i < s; i++) {
                var relatedId = related.get(i);

                var archetypeData = entityIndex.getArchetypeDataForEntity(relatedId);
                if (archetypeData == null) {
                    continue;
                }

                var componentMask = archetypeData.getComponentMask();
                var componentTypes = componentMask.getComponentTypes();
                for (int c = 0, cs = componentTypes.getSize(); c < cs; c++) {
                    if (!(componentTypes.get(c) instanceof RegularEntityRelationType<?, ?> relationType)) {
                        continue;
                    }

                    switch (relationType) {
                        case EntityRelationType<?> type -> {
                            var relations = (EntityRelationResultImpl) entityIndex.getComponent(relatedId, type);
                            if (relations != null) {
                                relations.removeTarget(targetId);

                                if (relations.isEmpty()) {
                                    handler.removeRelationType(relatedId, type);
                                }
                            }
                        }
                        case ExclusiveEntityRelationType<?> type -> handler.removeRelationType(relatedId, type);
                    }
                }
            }

            related.clear();
        }
    }

}
