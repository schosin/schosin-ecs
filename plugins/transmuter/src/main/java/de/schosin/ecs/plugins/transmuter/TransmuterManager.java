package de.schosin.ecs.plugins.transmuter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import de.schosin.ecs.api.Pooled;
import de.schosin.ecs.api.World;
import de.schosin.ecs.api.components.mappers.ComponentMapper.PooledComponentMapper;
import de.schosin.ecs.codegen.EcsCodegen;
import de.schosin.ecs.engine.components.ComponentMapperManager;
import de.schosin.ecs.engine.components.TransmutationManager;
import de.schosin.ecs.engine.components.TransmutationManager.AbstractTransmuter;

@EcsCodegen
public class TransmuterManager extends BaseTransmuterManager implements TransmuterPlugin {

    private final TransmutationManager transmutationManager;
    private final ComponentMapperManager componentMapperManager;

    private final Map<Class<?>, PooledComponentMapper<?>> mappers = new ConcurrentHashMap<>();

    public TransmuterManager(World world) {
        this.transmutationManager = world.getSingleton(TransmutationManager.class);
        this.componentMapperManager = world.getSingleton(ComponentMapperManager.class);
    }

    @Override
    public Transmuter.Remove createTransmuter(Transmuter.Builder.Remove builder) {
        return transmutationManager.getTransmuter(builder, () -> new RemoveImpl(builder));
    }

    @Override
    protected <T extends AbstractTransmuter> T getTransmuter(Transmuter.Builder builder, Supplier<T> supplier) {
        return transmutationManager.getTransmuter(builder, supplier);
    }

    private class RemoveImpl extends AbstractTransmuter implements Transmuter.Remove {

        private RemoveImpl(Transmuter.Builder.Remove builder) {
            super(transmutationManager, (Builder) builder); // TODO remove cast
        }

        @Override
        public boolean apply(int entityId) {
            return super.apply(entityId);
        }

    }

    abstract static class AbstractAddTransmuter extends AbstractTransmuter implements Transmuter.Add {

        private final TransmuterManager manager;

        protected AbstractAddTransmuter(BaseTransmuterManager manager, Transmuter.Builder builder) {
            this((TransmuterManager) manager, builder);
        }

        private AbstractAddTransmuter(TransmuterManager manager, Transmuter.Builder builder) {
            super(manager.transmutationManager, builder);

            this.manager = manager;
        }

        @Override
        public <T extends Pooled> T getInstance(Class<T> clazz) {
            var mapper = manager.mappers.computeIfAbsent(clazz, key -> manager.componentMapperManager.getPooledComponents(clazz));

            return clazz.cast(mapper.getInstance());
        }

    }

}
