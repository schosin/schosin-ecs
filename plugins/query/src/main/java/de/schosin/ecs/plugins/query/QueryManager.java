package de.schosin.ecs.plugins.query;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntConsumer;
import java.util.stream.Collectors;

import de.schosin.ecs.engine.EngineWorld;
import de.schosin.ecs.plugins.query.Query.Builder;
import de.schosin.ecs.storage.api.StorageEngine;
import de.schosin.ecs.storage.api.entities.Archetype;
import de.schosin.ecs.utils.collections.Bag;

public class QueryManager implements QueryPlugin {

    private final StorageEngine storageEngine;

    public QueryManager(EngineWorld world) {
        this.storageEngine = world.getSingleton(StorageEngine.class);
    }

    @Override
    public Query createQuery(Builder builder) {
        var conditions = new IdentityHashMap<>(builder.conditions);
        var references = new IdentityHashMap<Object, List<Condition>>();

        var mainConditions = conditions.remove(This.THIS);
        if (mainConditions == null) {
            throw new IllegalStateException("Must have atleast one condition.");
        }

        // Resolve references targets
        for (var mainCondition : mainConditions) {
            if (mainCondition instanceof EntityRelationCondition relation) {
                var relationConditions = conditions.remove(relation.target());
                if (relationConditions == null) {
                    throw new IllegalStateException("Reference '%s' does not have any conditions. Remove unneeded reference.".formatted(relation.target()));
                }

                references.put(relation.target(), relationConditions);
            }
        }

        if (!conditions.isEmpty()) {
            throw new UnsupportedOperationException("nested reference not implemented yet: " + conditions);
        }

        // Resolve archetypes

        return new QueryImpl(mainConditions, references);
    }

    private Spec buildSpec(List<Condition> conditions) {
        throw new UnsupportedOperationException("buildSpec(%s) not implemented yet".formatted(conditions));
    }

    private final class QueryImpl implements Query {

        private final List<Condition> conditions;
        private final IdentityHashMap<Object, List<Condition>> references;

        private final Spec spec;
        private final IdentityHashMap<Object, Spec> referenceSpecs;

        private final Bag<Archetype> archetypes = new Bag<>(Archetype.class, 4);
        private final IdentityHashMap<Object, Bag<Archetype>> referenceArchetypes = new IdentityHashMap<>();

        public QueryImpl(List<Condition> conditions, IdentityHashMap<Object, List<Condition>> references) {
            this.conditions = conditions;
            this.references = references;

            this.spec = buildSpec(conditions);
            this.referenceSpecs = references.entrySet().stream().map(entry -> Map.entry(entry.getKey(), buildSpec(entry.getValue())))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, IdentityHashMap::new));
        }

        private void offer(Archetype archetype) {
            if (spec.isInterested(archetype)) {
                this.archetypes.add(archetype);
            }

            for (var entry : referenceSpecs.entrySet()) {
                if (entry.getValue().isInterested(archetype)) {
                    referenceArchetypes.computeIfAbsent(entry.getKey(), ignore -> new Bag<>(Archetype.class, 4)).add(archetype);
                }
            }
        }

        @Override
        public void process(IntConsumer consumer) {
            /*
             * Goal: Compare performance of composition.process(int, Position) with a query where the position is of a related
             * 
             * Get accessors for all references, lookup by "id", provide a way to have it iterate relations based on entityId 
             */

            for (int i = 0, s = archetypes.getSize(); i < s; i++) {
                var accessor = archetypes.get(i).getEntityData().getAccessor();

                while (accessor.hasNext()) {
                    var entityId = accessor.next();
                    
                    // TODO this can probably be optimized if adding a (relationship, targetId) to entityId automatically adds a reverse relationship. that way archetypes can be filtered as well.
                    // relationship could be marked as Bidirectional, but how to work with the inverse? it would need to be a hidden type, such that Wildcard relations will not match it
                    // OR have it be a zero-sized type that can be matched, but not directly fetched?

                    // TODO have this be a data structure per reference that can be iterated just by passing in entityId
                    // needs a fast way to see if a target is related to an entity by a specific relationship
                    for (var entry : references.entrySet()) {
                        var referenceArchetypes = this.referenceArchetypes.get(entry.getKey());
                        // TODO reference alone is not enough, we also need to regularentityrelationtypes that reference is linked with

                        // TODO somehow merge these together, processing the intersection
                        // requires a fast way to only keep those where entity has entityRelation with target reference
                    }

                    for (var cond : conditions) {
                        if (cond instanceof EntityRelationCondition relation) {
                            var relationArchetypes = referenceArchetypes.get(relation.target());
                        }
                    }
                }

                accessor.free();
            }
        }

    }

    interface Spec {
        boolean isInterested(Archetype archetype);
    }

}
