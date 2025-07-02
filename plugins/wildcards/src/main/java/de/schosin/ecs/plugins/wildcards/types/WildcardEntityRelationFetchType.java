package de.schosin.ecs.plugins.wildcards.types;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import de.schosin.ecs.api.components.Relation.EntityRelationData;
import de.schosin.ecs.api.components.mappers.EntityFetchRelationMappers;
import de.schosin.ecs.api.components.types.ComponentType;
import de.schosin.ecs.api.components.types.RelationComponentType.EntityRelationType;
import de.schosin.ecs.api.components.types.RelationComponentType.ExclusiveEntityRelationType;
import de.schosin.ecs.plugins.wildcards.mappers.WildcardEntityRelationFetchMapper;
import de.schosin.ecs.plugins.wildcards.result.WildcardEntityRelationsData;

/**
 * Describes an entity relation wildcard type similarly to {@link WildcardEntityRelationType}, but also allows to fetch data for
 * the target entities in the same way as {@link EntityFetchRelationMappers} does.
 * 
 * @param <R> type of relationship bound
 * @param <T> type of fetched data for target entities
 */
public record WildcardEntityRelationFetchType<R, T>(Class<R> relationshipBound, ComponentType<?, T> fetch)
        implements WildcardRelationType<R, EntityRelationData<R, T>, WildcardEntityRelationsData<R, T>, WildcardEntityRelationFetchMapper<R, T>> {

    private static final Map<Class<?>, Map<ComponentType<?, ?>, WildcardEntityRelationFetchType<?, ?>>> LOOKUP = new ConcurrentHashMap<>();

    @SuppressWarnings("unchecked")
    static <R, T> WildcardEntityRelationFetchType<R, T> getInstance(Class<R> relationshipBound, ComponentType<?, T> fetch) {
        return (WildcardEntityRelationFetchType<R, T>) LOOKUP.computeIfAbsent(relationshipBound, ignore -> new ConcurrentHashMap<>())
                .computeIfAbsent(fetch, ignore2 -> new WildcardEntityRelationFetchType<>(relationshipBound, fetch));
    }

    public WildcardEntityRelationFetchType {
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
        return "WildcardEntityRelationFetchType(%s -> %s)".formatted(relationshipBound.getSimpleName(), fetch);
    }

}
