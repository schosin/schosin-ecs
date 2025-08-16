package de.schosin.ecs.engine.components;

import java.util.Arrays;
import java.util.Map;
import java.util.SequencedSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import de.schosin.ecs.api.components.types.ComponentType;
import de.schosin.ecs.api.components.types.ComponentType.RegularComponentType;
import de.schosin.ecs.storage.api.StorageEngine;
import de.schosin.ecs.storage.api.components.Component;
import de.schosin.ecs.utils.collections.Bag;
import de.schosin.ecs.utils.collections.ImmutableBag;
import de.schosin.ecs.utils.collections.Pool;

public class TransmutationManager {

    public interface Builder {

        SequencedSet<RegularComponentType<?, ?>> getAdd();

        SequencedSet<ComponentType<?, ?>> getRemove();

    }

    private final StorageEngine storageEngine;

    private final Bag<Add<?>> addTransmuters = new Bag<>(Add.class, 32);
    private final Map<ComponentType<?, ?>, Remove> removeTransmuters = new ConcurrentHashMap<>();

    public TransmutationManager(StorageEngine storageEngine) {
        this.storageEngine = storageEngine;
    }

    @SuppressWarnings("unchecked")
    public <T> Add<T> getAddTransmuter(Component<T, ?> component) {
        var componentId = component.id();

        var result = (Add<T>) addTransmuters.getSafe(componentId);
        if (result != null) {
            return result;
        }

        synchronized (addTransmuters) {
            result = (Add<T>) addTransmuters.getSafe(componentId);
            if (result != null) {
                return result;
            }

            result = new Add<>(component.type());
            addTransmuters.set(componentId, result);

            return result;
        }
    }

    public Remove getRemoveTransmuter(ComponentType<?, ?> type) {
        return removeTransmuters.computeIfAbsent(type, Remove::new);
    }

    public final class Add<T> {

        private final ImmutableBag<RegularComponentType<T, ?>> types;

        private final Pool<Object[]> pool = Pool.unbounded(Object[].class, () -> new Object[1], arr -> Arrays.fill(arr, 0, 1, null));

        public Add(RegularComponentType<T, ?> add) {
            this.types = ImmutableBag.of(add);
        }

        public boolean apply(int entityId, T component) {
            var components = pool.getInstance();
            components[0] = component;

            var updatedArchetype = storageEngine.add(entityId, types, components);
            pool.free(components);

            return updatedArchetype != storageEngine.getArchetypeForEntity(entityId);
        }

    }

    public final class Remove {

        private final ImmutableBag<ComponentType<?, ?>> types;

        public Remove(ComponentType<?, ?> remove) {
            this.types = ImmutableBag.of(remove);
        }

        public boolean apply(int entityId) {
            var updatedArchetype = storageEngine.remove(entityId, types);
            return updatedArchetype != storageEngine.getArchetypeForEntity(entityId);
        }

    }

    public abstract static class AbstractTransmuter {

        protected final StorageEngine storageEngine;

        protected AbstractTransmuter(TransmutationManager manager) {
            this.storageEngine = manager.storageEngine;
        }

        protected static ImmutableBag<RegularComponentType<?, ?>> convert(Set<RegularComponentType<?, ?>> types) {
            if (types.isEmpty()) {
                return ImmutableBag.emptyBag();
            }

            return ImmutableBag.create(new Bag<>(types.toArray(RegularComponentType<?, ?>[]::new)));
        }

        protected static ImmutableBag<ComponentType<?, ?>> convertRemove(Set<ComponentType<?, ?>> types) {
            if (types.isEmpty()) {
                return ImmutableBag.emptyBag();
            }

            return ImmutableBag.create(new Bag<>(types.toArray(ComponentType<?, ?>[]::new)));
        }

    }

}
