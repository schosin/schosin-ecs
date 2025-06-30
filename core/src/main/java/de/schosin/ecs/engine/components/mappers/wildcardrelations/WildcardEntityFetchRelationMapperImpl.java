package de.schosin.ecs.engine.components.mappers.wildcardrelations;

import static de.schosin.ecs.api.components.types.ComponentType.wildcardRelation;

import de.schosin.ecs.api.components.Relations.EntityRelationsData;
import de.schosin.ecs.api.components.mappers.Components;
import de.schosin.ecs.api.components.mappers.WildcardRelationMappers.WildcardEntityFetchRelationMapper;
import de.schosin.ecs.api.components.types.WildcardRelationType.WildcardEntityRelationFetchType;
import de.schosin.ecs.api.data.DataAccessor;
import de.schosin.ecs.engine.components.ComponentMapperManager;
import de.schosin.ecs.engine.components.ComponentMapperManager.ReclaimingComponents;
import de.schosin.ecs.engine.components.mappers.fetch.EntityRelationFetchMapperImpl.EntityRelationsFetchAccessor;
import de.schosin.ecs.engine.entities.EntityManager;
import de.schosin.ecs.utils.collections.Bag;
import de.schosin.ecs.utils.collections.Pool;

public final class WildcardEntityFetchRelationMapperImpl<R, T> implements WildcardEntityFetchRelationMapper<R, T>, ReclaimingComponents {

    private final EntityManager entityManager;

    private final WildcardEntityRelationMapper<R> relationMapper;
    private final Components<?, T> dataMapper;

    private final Bag<EntityRelationsFetchAccessor<R, T>> lent = new Bag<>(EntityRelationsFetchAccessor.class, 8);
    private final Pool<EntityRelationsFetchAccessor<R, T>> pool;

    public WildcardEntityFetchRelationMapperImpl(EntityManager entityManager, WildcardEntityRelationFetchType<R, T> type, ComponentMapperManager componentMapperManager) {
        this.entityManager = entityManager;

        this.relationMapper = componentMapperManager.getComponents(wildcardRelation(type.relationshipBound()));
        this.dataMapper = componentMapperManager.getComponents(type.fetch());

        this.pool = Pool.unbounded(EntityRelationsFetchAccessor.class, this::createAccessor);
    }

    private EntityRelationsFetchAccessor<R, T> createAccessor() {
        return new EntityRelationsFetchAccessor<>(relationMapper, dataMapper, pool);
    }

    @Override
    public void reclaim() {
        var data = lent.getData();
        for (int i = 0, s = lent.getSize(); i < s; i++) {
            data[i].free();
        }

        lent.clear();
    }

    @Override
    public boolean has(int entityId) {
        return relationMapper.has(entityId);
    }

    @Override
    public EntityRelationsData<R, T> get(int entityId) {
        var accessor = this.entityManager.getAccessor(entityId);

        var componentAccessor = getComponentAccessor(accessor);
        lent.add(componentAccessor);

        return componentAccessor.getComponent(accessor);
    }

    @Override
    public EntityRelationsFetchAccessor<R, T> getComponentAccessor(DataAccessor accessor) {
        return pool.getInstance().init(accessor);
    }

    @Override
    public boolean remove(int entityId) {
        return relationMapper.remove(entityId);
    }

}
