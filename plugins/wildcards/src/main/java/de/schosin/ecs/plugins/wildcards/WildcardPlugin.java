package de.schosin.ecs.plugins.wildcards;

import de.schosin.ecs.api.Plugin;
import de.schosin.ecs.api.components.types.ComponentType;
import de.schosin.ecs.plugins.wildcards.mappers.WildcardComponentRelationMapper;
import de.schosin.ecs.plugins.wildcards.mappers.WildcardEntityRelationFetchMapper;
import de.schosin.ecs.plugins.wildcards.mappers.WildcardEntityRelationMapper;

@Plugin(WildcardManager.class)
public interface WildcardPlugin {

    <R, T> WildcardComponentRelationMapper<R, T> getWildcardComponentRelations(Class<R> relationshipBound, Class<T> targetBound);

    <R> WildcardEntityRelationMapper<R> getWildcardEntityRelations(Class<R> relationshipBound);

    <R, T> WildcardEntityRelationFetchMapper<R, T> getWildcardEntityFetchRelations(Class<R> relationshipBound, ComponentType<?, T> fetch);

}
