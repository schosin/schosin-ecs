package de.schosin.ecs.engine.entities;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntSupplier;

import de.schosin.ecs.api.Pooled;
import de.schosin.ecs.api.components.Relation;
import de.schosin.ecs.api.components.Relation.ComponentRelation;
import de.schosin.ecs.api.components.Relation.EntityRelation;
import de.schosin.ecs.api.components.Relations;
import de.schosin.ecs.api.components.types.ComponentType;
import de.schosin.ecs.api.components.types.ComponentType.RegularComponentType;
import de.schosin.ecs.api.components.types.RelationComponentType;
import de.schosin.ecs.api.data.DataAccessor;
import de.schosin.ecs.engine.BagManager;
import de.schosin.ecs.storage.api.StorageEngine;
import de.schosin.ecs.storage.api.entities.Archetype;
import de.schosin.ecs.storage.api.entities.Archetype.ComponentsInitializer;
import de.schosin.ecs.utils.collections.Bag;
import de.schosin.ecs.utils.collections.ImmutableIntBag;
import de.schosin.ecs.utils.collections.IntBag;
import de.schosin.ecs.utils.collections.Pool;

public class EntityManager {

    public interface ComponentsPredicate {
        boolean isInterested(Archetype archetype);
    }

    private static final RegularComponentType<?, ?>[] EMPTY_COMPONENT_TYPES = new RegularComponentType<?, ?>[0];

    private final StorageEngine storageEngine;

    private final BagManager bagManager;

    private final AtomicInteger entityId = new AtomicInteger(1);
    private final Bag<Entity> entities = new Bag<>(Entity.class, 64);
    private final Pool<Entity> pool = Pool.unbounded(Entity.class, () -> new Entity(createEntityId()), Entity::reset);

    private final Pool<IntBag> intBagPool = Pool.unbounded(IntBag.class, () -> new IntBag(1000), IntBag::clear);
    private final Bag<IntBag> lentIntBags = new Bag<>(IntBag.class, 8);

    public EntityManager(StorageEngine storageEngine, BagManager bagManager) {
        this.storageEngine = storageEngine;

        this.bagManager = bagManager;
    }

    public void freeEntityIds(ImmutableIntBag entities) {
        for (int i = 0, s = entities.getSize(); i < s; i++) {
            var entityId = entities.get(i);

            var entity = this.entities.get(entityId);
            if (entity == null) {
                continue;
            }

            this.pool.free(entity);
            this.entities.set(entityId, null);
        }
    }

    public void process() {
        var data = this.lentIntBags.getData();
        for (int i = 0, s = this.lentIntBags.getSize(); i < s; i++) {
            intBagPool.free(data[i]);
        }

        this.lentIntBags.clear();
    }

    public int createEntity(Object... components) {
        // Determine component types and merge relations
        var componentTypes = determineComponentTypes(components);
        var merged = mergeRelations(components, componentTypes);
        if (merged != components) {
            components = merged;
            componentTypes = determineComponentTypes(components);
        }

        // Get archetype, sort components in-place
        var archetype = storageEngine.getArchetype(componentTypes);
        if (components.length > 1) {
            sortComponentsInPlace(components, componentTypes, archetype);
        }

        // Create entity
        var entity = createEntityInstance();

        archetype.createEntity(entity.id, components);

        // Add entity
        this.entities.set(entity.id, entity);

        return entity.id;
    }

    private RegularComponentType<?, ?>[] determineComponentTypes(Object[] components) {
        if (components.length == 0) {
            return EMPTY_COMPONENT_TYPES;
        }

        var componentTypes = new RegularComponentType<?, ?>[components.length];
        for (int i = 0, s = components.length; i < s; i++) {
            componentTypes[i] = ComponentType.detectComponentType(components[i]);
        }

        return componentTypes;
    }

    @SuppressWarnings("unchecked")
    private Object[] mergeRelations(Object[] components, RegularComponentType<?, ?>[] componentTypes) {
        if (components.length < 2) {
            return components;
        }

        // See if any non-exclusive relations are present
        var indices = intBagPool.getInstance();
        for (int i = 0, s = componentTypes.length; i < s; i++) {
            if (componentTypes[i] instanceof RelationComponentType<?, ?, ?>) {
                indices.add(i);
            }
        }

        if (indices.isEmpty()) {
            intBagPool.free(indices);
            return components;
        }

        // Create lookup map based on type to either a single relation, or a list of relations
        var lookup = new HashMap<RegularComponentType<?, ?>, Object>();
        for (int i = 0, s = indices.getSize(); i < s; i++) {
            var index = indices.get(i);
            var relation = components[index];

            lookup.merge(componentTypes[index], relation, (existing, item) -> {
                return switch (existing) {
                    case Relation<?> r -> {
                        yield new ArrayList<Object>(List.of(r, item));
                    }
                    default -> {
                        var list = (List<Object>) existing;

                        list.add(item);
                        yield list;
                    }
                };
            });
        }

        // Return early if only unique relations found
        var newSize = components.length - indices.getSize() + lookup.size();
        if (newSize == components.length) {
            intBagPool.free(indices);
            return components;
        }

        // Build new components array
        var result = new Object[newSize];

        var index = 0;
        for (int i = 0, s = components.length; i < s; i++) {
            if (indices.contains(i)) {
                continue;
            }

            result[index++] = components[i];
        }

        for (var value : lookup.values()) {
            var component = value instanceof List<?> list
                    ? list.get(0) instanceof ComponentRelation<?, ?>
                            ? Relations.of(list.toArray(ComponentRelation[]::new))
                            : Relations.of(list.toArray(EntityRelation[]::new))
                    : value;

            result[index++] = component;
        }

        intBagPool.free(indices);
        return result;
    }

    private void sortComponentsInPlace(Object[] components, RegularComponentType<?, ?>[] componentTypes, Archetype archetype) {
        for (int i = 0, s = components.length; i < s; i++) {
            var index = archetype.getComponentTypes().indexOf(componentTypes[i]);
            if (index != i) {
                swap(components, i, index);
                swap(componentTypes, i, index);

                i--;
            }
        }
    }

    private static void swap(Object[] array, int oldIndex, int newIndex) {
        var item = array[newIndex];
        array[newIndex] = array[oldIndex];
        array[oldIndex] = item;
    }

    public int createEntity(Archetype archetype, Object[] components) {
        // Create entity
        var entity = createEntityInstance();

        archetype.createEntity(entity.id, components);

        // Add entity
        this.entities.set(entity.id, entity);

        return entity.id;
    }

    public ImmutableIntBag createEntities(Archetype archetype, int count, ComponentsInitializer componentsConsumer) {
        // Build supplier of entityIds
        var entityIds = intBagPool.getInstance();
        entityIds.ensureCapacity(count);

        this.lentIntBags.add(entityIds);

        IntSupplier entityIdSupplier = () -> {
            var entity = createEntityInstance();

            entities.set(entity.id, entity);
            entityIds.addSafe(entity.id);

            return entity.id;
        };

        // Create entities
        archetype.createEntities(count, entityIdSupplier, componentsConsumer);

        return entityIds;
    }

    private Entity createEntityInstance() {
        return pool.getInstance();
    }

    private int createEntityId() {
        var entityId = this.entityId.getAndIncrement();
        this.bagManager.ensureEntitySize(entityId);

        return entityId;
    }

    public DataAccessor getAccessor(int entityId) {
        return storageEngine.getAccessor(entityId);
    }

    public boolean isActive(int entityId) {
        return this.entities.get(entityId) != null;
    }

    /**
     * @return archetype of the entity or null if entity does not exist
     */
    public Archetype getArchetype(int entityId) {
        var entity = this.entities.get(entityId);
        if (entity == null) {
            return null;
        }

        return storageEngine.getArchetypeForEntity(entityId);
    }

    private static class Entity implements Pooled {

        private final int id;

        private Entity(int entityId) {
            this.id = entityId;
        }

    }

}