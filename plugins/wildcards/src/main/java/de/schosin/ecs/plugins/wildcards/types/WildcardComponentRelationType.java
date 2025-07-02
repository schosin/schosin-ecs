package de.schosin.ecs.plugins.wildcards.types;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import de.schosin.ecs.api.components.Relation.ComponentRelation;
import de.schosin.ecs.api.components.types.RelationComponentType.ComponentRelationType;
import de.schosin.ecs.api.components.types.RelationComponentType.ExclusiveComponentRelationType;
import de.schosin.ecs.plugins.wildcards.mappers.WildcardComponentRelationMapper;
import de.schosin.ecs.plugins.wildcards.result.WildcardComponentRelations;

/**
 * Describes a component relation wildcard type. Allows to read all component relations where both the relationship
 * and target are assignable to the respective bounds.
 * 
 * <p>
 * When using {@code Object.class} for both bounds, all component relations (non-exclusive and exclusive) of an entity can be read.
 * </p>
 * 
 * @param <R> type of relationship component bound
 * @param <T> type of target component bound
 */
public record WildcardComponentRelationType<R, T>(Class<R> relationshipBound, Class<T> targetBound)
        implements WildcardRelationType<R, ComponentRelation<R, T>, WildcardComponentRelations<R, T>, WildcardComponentRelationMapper<R, T>> {

    private static final Map<Class<?>, Map<Class<?>, WildcardComponentRelationType<?, ?>>> LOOKUP = new ConcurrentHashMap<>();

    @SuppressWarnings("unchecked")
    static <R, T> WildcardComponentRelationType<R, T> getInstance(Class<R> relationshipBound, Class<T> targetBound) {
        return (WildcardComponentRelationType<R, T>) LOOKUP.computeIfAbsent(relationshipBound, ignore -> new ConcurrentHashMap<>())
                .computeIfAbsent(targetBound, ignore2 -> new WildcardComponentRelationType<>(relationshipBound, targetBound));
    }

    public WildcardComponentRelationType {
        WildcardRelationTypeHelper.validateWildcardComponentRelation(relationshipBound, targetBound);
    }

    @Override
    public boolean matches(RegularComponentType<?, ?> otherType) {
        return switch (otherType) {
            case ComponentRelationType<?, ?> relation -> this.relationshipBound.isAssignableFrom(relation.relationship()) && this.targetBound.isAssignableFrom(relation.target());
            case ExclusiveComponentRelationType<?, ?> relation -> this.relationshipBound.isAssignableFrom(relation.relationship()) && this.targetBound.isAssignableFrom(relation.target());
            default -> false;
        };
    }

    @Override
    public final String toString() {
        return "WildcardComponentRelationType(%s / %s)".formatted(relationshipBound.getSimpleName(), targetBound.getSimpleName());
    }

}
