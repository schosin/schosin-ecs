package de.schosin.ecs.storage.archetype.components;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import de.schosin.ecs.api.Pooled;
import de.schosin.ecs.api.components.types.ClassType;
import de.schosin.ecs.api.components.types.ComponentType.RegularComponentType;
import de.schosin.ecs.api.components.types.RelationComponentType.RegularComponentRelationType;
import de.schosin.ecs.api.components.types.RelationComponentType.RegularEntityRelationType;
import de.schosin.ecs.storage.api.StorageEngineException;
import de.schosin.ecs.utils.ReflectionUtils;
import de.schosin.ecs.utils.collections.Bag;
import de.schosin.ecs.utils.collections.IntBag;
import de.schosin.ecs.utils.collections.Pool;

public class ComponentIndex {

    private final AtomicInteger nextComponentId = new AtomicInteger(0);

    private final Map<RegularComponentType<?, ?>, Integer> lookup = new HashMap<>();
    private final Bag<RegularComponentType<?, ?>> reverseLookup = new Bag<>(RegularComponentType.class, 64);

    private final Map<Class<? extends Pooled>, Pool<Pooled>> pools = new HashMap<>();

    private final IntBag reservedClassIds;

    private final int relationCount;
    private final Map<Class<?>, IntBag> reservedComponentRelationIds = new HashMap<>();
    private final Map<Class<?>, IntBag> reservedEntityRelationIds = new HashMap<>();

    private final Bag<IntBag> intBags = new Bag<>(IntBag.class, 16);

    public ComponentIndex(int classIdCount, int relationCount) {
        this.relationCount = relationCount;

        this.reservedClassIds = reserveClassIds(classIdCount);
    }

    public IntBag createIntBag() {
        synchronized (this.pools) {
            var maxComponentId = nextComponentId.get() + 1;

            var intBag = new IntBag(maxComponentId);
            Arrays.fill(intBag.getData(), -1);

            for (int i = 0; i < maxComponentId; i++) {
                intBag.set(i, -1);
            }

            this.intBags.add(intBag);
            return intBag;
        }
    }

    @SuppressWarnings("unchecked")
    public <T extends Pooled> Pool<T> getPool(Class<T> clazz) {
        var pool = this.pools.get(clazz);
        if (pool != null) {
            return (Pool<T>) pool;
        }

        synchronized (this.pools) {
            pool = this.pools.get(clazz);
            if (pool != null) {
                return (Pool<T>) pool;
            }

            pool = Pool.unbounded(clazz.asSubclass(Pooled.class), () -> ReflectionUtils.createComponentInstance(clazz));
            this.pools.put(clazz, pool);

            return (Pool<T>) pool;
        }
    }

    public <T extends Pooled> T getInstance(Class<T> clazz) {
        var pool = getPool(clazz);

        return pool.getInstance();
    }

    public void freeInstance(Pooled pooled) {
        var pool = this.pools.get(pooled.getClass());
        if (pool != null) {
            pool.free(pooled);
        }
    }

    public int getExistingId(RegularComponentType<?, ?> componentType) {
        return this.lookup.getOrDefault(componentType, -1);
    }

    public int getId(RegularComponentType<?, ?> componentType) {
        var result = this.lookup.get(componentType);
        if (result != null) {
            return result;
        }

        synchronized (this.lookup) {
            result = this.lookup.get(componentType);
            if (result != null) {
                return result;
            }

            var id = createId(componentType);
            this.lookup.put(componentType, id);
            this.reverseLookup.set(id, componentType);

            for (int i = 0, s = intBags.getSize(); i < s; i++) {
                intBags.get(i).set(id, -1);
            }

            return id;
        }
    }

    @SuppressWarnings("unchecked")
    public <R> RegularComponentType<?, R> getType(int componentId) {
        return (RegularComponentType<?, R>) this.reverseLookup.getSafe(componentId);
    }

    private int createId(RegularComponentType<?, ?> componentType) {
        return switch (componentType) {
            case ClassType<?> type -> reservedClassIds.isEmpty() ? getNextId() : createClassId();
            case RegularComponentRelationType<?, ?, ?> relation -> createComponentRelationId(relation.relationship());
            case RegularEntityRelationType<?, ?> relation -> createEntityRelationId(relation.relationship());
        };
    }

    private synchronized int createClassId() {
        return reservedClassIds.isEmpty() ? getNextId() : reservedClassIds.removeLast();
    }

    private int createComponentRelationId(Class<?> relationship) {
        var reserved = this.reservedComponentRelationIds.get(relationship);
        if (reserved == null) {
            synchronized (this.reservedComponentRelationIds) {
                reserved = this.reservedComponentRelationIds.get(relationship);
                if (reserved == null) {
                    reserved = reserveRelationIds();
                    this.reservedComponentRelationIds.put(relationship, reserved);
                }
            }
        }

        if (reserved.isEmpty()) {
            throw new StorageEngineException("Ids for component relations with relationship '%s' exhausted. Use ArchetypeStorageConfig to configure value (current: %d)".formatted(relationCount));
        }

        synchronized (reserved) {
            if (reserved.isEmpty()) {
                throw new StorageEngineException("Ids for component relations with relationship '%s' exhausted. Use ArchetypeStorageConfig to configure value (current: %d)".formatted(relationCount));
            }

            return reserved.removeLast();
        }
    }

    private int createEntityRelationId(Class<?> relationship) {
        var reserved = this.reservedEntityRelationIds.get(relationship);
        if (reserved == null) {
            synchronized (this.reservedEntityRelationIds) {
                reserved = this.reservedEntityRelationIds.get(relationship);
                if (reserved == null) {
                    reserved = reserveRelationIds();
                    this.reservedEntityRelationIds.put(relationship, reserved);
                }
            }
        }

        if (reserved.isEmpty()) {
            throw new StorageEngineException("Ids for entity relations with relationship '%s' exhausted. Use ArchetypeStorageConfig to configure value (current: %d)".formatted(relationCount));
        }

        synchronized (reserved) {
            if (reserved.isEmpty()) {
                throw new StorageEngineException("Ids for entity relations with relationship '%s' exhausted. Use ArchetypeStorageConfig to configure value (current: %d)".formatted(relationCount));
            }

            return reserved.removeLast();
        }
    }

    private IntBag reserveClassIds(int classIdCount) {
        var result = new IntBag(classIdCount);

        for (int i = 0; i < classIdCount; i++) {
            // Reserve in reverse order so that removeLast returns 0, 1, ...
            result.set(classIdCount - i - 1, getNextId());
        }

        return result;
    }

    private IntBag reserveRelationIds() {
        var result = new IntBag(relationCount);

        for (int i = 0; i < relationCount; i++) {
            // Reserve in reverse order so that removeLast returns 0, 1, ...
            result.set(relationCount - i - 1, getNextId());
        }

        return result;
    }

    private int getNextId() {
        return nextComponentId.getAndIncrement();
    }

}
