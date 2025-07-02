package de.schosin.ecs.plugins.wildcards.types;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import de.schosin.ecs.api.components.Relation.EntityRelation;
import de.schosin.ecs.api.components.types.RelationComponentType.EntityRelationType;
import de.schosin.ecs.api.components.types.RelationComponentType.ExclusiveEntityRelationType;
import de.schosin.ecs.plugins.wildcards.mappers.WildcardEntityRelationMapper;
import de.schosin.ecs.plugins.wildcards.result.WildcardEntityRelations;

/**
 * Describes an entity relation wildcard type. Allows to read all entity relations where the relationship
 * is assignable to the bound. 
 * 
 * <p>
 * When using {@code Object.class} as the bound, all entity relations (non-exclusive and exclusive) of an entity can be read.
 * </p>
 * 
 * @param <R> type of relationship bound
 */
public record WildcardEntityRelationType<R>(Class<R> relationshipBound)
        implements WildcardRelationType<R, EntityRelation<R>, WildcardEntityRelations<R>, WildcardEntityRelationMapper<R>> {

    private static final Map<Class<?>, WildcardEntityRelationType<?>> LOOKUP = new ConcurrentHashMap<>();

    @SuppressWarnings("unchecked")
    static <R> WildcardEntityRelationType<R> getInstance(Class<R> relationshipBound) {
        return (WildcardEntityRelationType<R>) LOOKUP.computeIfAbsent(relationshipBound, WildcardEntityRelationType::new);
    }

    public WildcardEntityRelationType {
        WildcardRelationTypeHelper.validateWildcardEntityRelation(relationshipBound);
    }

    @Override
    public boolean matches(RegularComponentType<?, ?> otherType) {
        return switch (otherType) {
            case EntityRelationType<?> relation -> this.relationshipBound.isAssignableFrom(relation.relationship());
            case ExclusiveEntityRelationType<?> relation -> this.relationshipBound.isAssignableFrom(relation.relationship());
            default -> false;
        };
    }

    @Override
    public final String toString() {
        return "WildcardEntityRelationType(%s)".formatted(relationshipBound.getSimpleName());
    }

}
