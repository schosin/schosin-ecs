package de.schosin.ecs.plugins.transmuter;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.SequencedSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import de.schosin.ecs.api.Pooled;
import de.schosin.ecs.api.World;
import de.schosin.ecs.api.components.mappers.ComponentMapper.PooledComponentMapper;
import de.schosin.ecs.api.components.types.ComponentType;
import de.schosin.ecs.api.components.types.ComponentType.RegularComponentType;
import de.schosin.ecs.codegen.EcsCodegen;
import de.schosin.ecs.engine.components.ComponentMapperManager;
import de.schosin.ecs.engine.components.TransmutationManager;
import de.schosin.ecs.engine.components.TransmutationManager.AbstractTransmuter;
import de.schosin.ecs.engine.components.TransmutationManager.Builder;
import de.schosin.ecs.utils.collections.ImmutableBag;
import de.schosin.ecs.utils.collections.Pool;

@EcsCodegen
public class TransmuterManager extends BaseTransmuterManager implements TransmuterPlugin {

    private final TransmutationManager transmutationManager;
    private final ComponentMapperManager componentMapperManager;

    private final Map<BuilderKey, AbstractTransmuter> transmuters = new HashMap<>(); // uses synchronized -> no ConcurrentHashMap
    private final Map<Class<?>, PooledComponentMapper<?>> mappers = new ConcurrentHashMap<>();

    private final Pool<BuilderKey> keyPool = Pool.unbounded(BuilderKey.class, BuilderKey::new);

    public TransmuterManager(World world) {
        this.transmutationManager = world.getSingleton(TransmutationManager.class);
        this.componentMapperManager = world.getSingleton(ComponentMapperManager.class);
    }

    @Override
    public Transmuter.Remove createTransmuter(Transmuter.Builder.Remove builder) {
        return getTransmuter(builder, () -> new RemoveImpl(builder));
    }

    @Override
    @SuppressWarnings("unchecked")
    protected <T extends TransmutationManager.AbstractTransmuter> T getTransmuter(Transmuter.Builder builder, Supplier<T> supplier) {
        var key = keyPool.getInstance().init(builder);

        var result = (T) transmuters.get(key);
        if (result != null) {
            keyPool.free(key);
            return result;
        }

        // key pooled -> no computeIfAbsent possible
        synchronized (transmuters) {
            result = (T) transmuters.get(key);
            if (result != null) {
                keyPool.free(key);
                return result;
            }

            var transmuter = supplier.get();
            this.transmuters.put(key.copy(), transmuter);

            keyPool.free(key);
            return transmuter;
        }
    }

    private final class RemoveImpl extends AbstractTransmuter implements Transmuter.Remove {

        private final ImmutableBag<ComponentType<?, ?>> types;

        private RemoveImpl(Transmuter.Builder.Remove builder) {
            super(transmutationManager);

            this.types = convertRemove(builder.remove);
        }

        @Override
        public boolean apply(int entityId) {
            var updatedArchetype = storageEngine.remove(entityId, types);
            return updatedArchetype != storageEngine.getArchetypeForEntity(entityId);
        }

    }

    abstract static class AbstractAddTransmuter extends AbstractTransmuter implements Transmuter.Add {

        private final TransmuterManager manager;

        private final ImmutableBag<RegularComponentType<?, ?>> addTypes;
        private final ImmutableBag<ComponentType<?, ?>> removeTypes;

        protected AbstractAddTransmuter(BaseTransmuterManager manager, Transmuter.Builder builder) {
            this((TransmuterManager) manager, builder);
        }

        private AbstractAddTransmuter(TransmuterManager manager, Transmuter.Builder builder) {
            super(manager.transmutationManager);

            this.manager = manager;

            this.addTypes = convert(builder.getAdd());
            this.removeTypes = convertRemove(builder.getRemove());
        }

        protected final boolean apply(int entityId, Object... components) {
            var updatedArchetype = storageEngine.modify(entityId, addTypes, components, removeTypes);
            return updatedArchetype != storageEngine.getArchetypeForEntity(entityId);
        }

        @Override
        public final <T extends Pooled> T getInstance(Class<T> clazz) {
            var mapper = manager.mappers.computeIfAbsent(clazz, key -> manager.componentMapperManager.getPooledComponents(clazz));

            return clazz.cast(mapper.getInstance());
        }

    }

    private record BuilderKey(SequencedSet<RegularComponentType<?, ?>> add, SequencedSet<ComponentType<?, ?>> remove) implements Pooled {

        public BuilderKey() {
            this(new LinkedHashSet<>(), new LinkedHashSet<>());
        }

        private BuilderKey init(Builder builder) {
            this.add.addAll(builder.getAdd());
            this.remove.addAll(builder.getRemove());

            return this;
        }

        private BuilderKey copy() {
            return new BuilderKey(new LinkedHashSet<>(this.add), new LinkedHashSet<>(this.remove));
        }

        @Override
        public void reset() {
            this.add.clear();
            this.remove.clear();
        }

    }

}
