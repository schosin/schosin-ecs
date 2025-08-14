package de.schosin.ecs.engine.components;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.SequencedSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import de.schosin.ecs.api.Pooled;
import de.schosin.ecs.api.components.types.ComponentType;
import de.schosin.ecs.api.components.types.ComponentType.RegularComponentType;
import de.schosin.ecs.storage.api.StorageEngine;
import de.schosin.ecs.utils.collections.Bag;
import de.schosin.ecs.utils.collections.ImmutableBag;
import de.schosin.ecs.utils.collections.Pool;

public class TransmutationManager {

    public interface Builder {

        SequencedSet<RegularComponentType<?, ?>> getAdd();

        SequencedSet<ComponentType<?, ?>> getRemove();

    }

    private final StorageEngine storageEngine;

    private final Map<BuilderKey, AbstractTransmuter> transmuters = new ConcurrentHashMap<>();

    private final Pool<BuilderKey> internalKeyPool = Pool.unbounded(BuilderKey.class, () -> new BuilderKey(true));
    private final Pool<BuilderKey> keyPool = Pool.unbounded(BuilderKey.class, () -> new BuilderKey(false));

    public TransmutationManager(StorageEngine storageEngine) {
        this.storageEngine = storageEngine;
    }

    public <T> Add<T> getAddTransmuter(RegularComponentType<T, ?> component) {
        return internalKeyPool.withInstance(key -> getTransmuter(key.add(component), () -> new Add<>(component)));
    }

    public Remove getRemoveTransmuter(ComponentType<?, ?> component) {
        return internalKeyPool.withInstance(key -> getTransmuter(key.remove(component), () -> new Remove(component)));
    }

    public <T extends AbstractTransmuter> T getTransmuter(Builder builder, Supplier<T> supplier) {
        return keyPool.withInstance(key -> getTransmuter(key.init(builder), supplier));
    }

    @SuppressWarnings("unchecked")
    private <T extends AbstractTransmuter> T getTransmuter(BuilderKey key, Supplier<T> supplier) {
        var result = (T) transmuters.get(key);
        if (result != null) {
            return result;
        }

        synchronized (transmuters) {
            result = (T) transmuters.get(key);
            if (result != null) {
                return result;
            }

            var transmuter = supplier.get();
            this.transmuters.put(key.copy(), transmuter);

            return transmuter;
        }
    }

    public class Add<T> extends AbstractTransmuter {

        public Add(RegularComponentType<T, ?> add) {
            super(TransmutationManager.this, Set.of(add), Set.of());
        }

        public boolean apply(int entityId, T component) {
            return super.apply(entityId, component);
        }

        @Override
        protected final boolean apply(int entityId, Object... added) {
            throw new UnsupportedOperationException("use public apply");
        }

    }

    public class Remove extends AbstractTransmuter {

        public Remove(ComponentType<?, ?> remove) {
            super(TransmutationManager.this, Set.of(), Set.of(remove));
        }

        public boolean apply(int entityId) {
            return super.apply(entityId);
        }

        @Override
        protected final boolean apply(int entityId, Object... added) {
            throw new UnsupportedOperationException("use public apply");
        }

    }

    public abstract static class AbstractTransmuter {

        private final TransmutationManager manager;

        private final ImmutableBag<RegularComponentType<?, ?>> addTypes;
        private final ImmutableBag<ComponentType<?, ?>> removeTypes;

        protected AbstractTransmuter(TransmutationManager manager, Builder builder) {
            this(manager, builder.getAdd(), builder.getRemove());
        }

        protected AbstractTransmuter(TransmutationManager manager, Set<RegularComponentType<?, ?>> add, Set<ComponentType<?, ?>> remove) {
            this(manager, convert(add), convertRemove(remove));
        }

        private static ImmutableBag<RegularComponentType<?, ?>> convert(Set<RegularComponentType<?, ?>> types) {
            if (types.isEmpty()) {
                return ImmutableBag.emptyBag();
            }

            return ImmutableBag.create(new Bag<>(types.toArray(RegularComponentType<?, ?>[]::new)));
        }

        private static ImmutableBag<ComponentType<?, ?>> convertRemove(Set<ComponentType<?, ?>> types) {
            if (types.isEmpty()) {
                return ImmutableBag.emptyBag();
            }

            return ImmutableBag.create(new Bag<>(types.toArray(ComponentType<?, ?>[]::new)));
        }

        private AbstractTransmuter(TransmutationManager manager, ImmutableBag<RegularComponentType<?, ?>> addTypes, ImmutableBag<ComponentType<?, ?>> removeTypes) {
            this.manager = manager;

            this.addTypes = addTypes;
            this.removeTypes = removeTypes;
        }

        protected boolean apply(int entityId, Object... added) {
            if (addTypes.getSize() != added.length) {
                throw new IllegalArgumentException("Expected %d added components, but got %d".formatted(addTypes.getSize(), added.length));
            }

            var updatedArchetype = manager.storageEngine.modify(entityId, addTypes, added, removeTypes);
            return updatedArchetype != manager.storageEngine.getArchetypeForEntity(entityId);
        }

    }

    private record BuilderKey(boolean internal, SequencedSet<RegularComponentType<?, ?>> add, SequencedSet<ComponentType<?, ?>> remove) implements Pooled {

        public BuilderKey(boolean internal) {
            this(internal, new LinkedHashSet<>(), new LinkedHashSet<>());
        }

        private BuilderKey add(RegularComponentType<?, ?> component) {
            this.add.add(component);
            return this;
        }

        private BuilderKey remove(ComponentType<?, ?> component) {
            this.remove.add(component);
            return this;
        }

        private BuilderKey init(Builder builder) {
            this.add.addAll(builder.getAdd());
            this.remove.addAll(builder.getRemove());

            return this;
        }

        private BuilderKey copy() {
            return new BuilderKey(internal, new LinkedHashSet<>(this.add), new LinkedHashSet<>(this.remove));
        }

        @Override
        public void reset() {
            this.add.clear();
            this.remove.clear();
        }

    }

}
