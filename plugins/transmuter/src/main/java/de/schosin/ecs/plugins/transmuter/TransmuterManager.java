package de.schosin.ecs.plugins.transmuter;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import de.schosin.ecs.api.Pooled;
import de.schosin.ecs.api.World;
import de.schosin.ecs.api.components.ComponentType.RegularComponentType;
import de.schosin.ecs.api.components.Components.PooledComponents;
import de.schosin.ecs.codegen.EcsCodegen;
import de.schosin.ecs.engine.components.ComponentManager;
import de.schosin.ecs.engine.components.ComponentMapperManager;
import de.schosin.ecs.engine.components.TransmutationManager;
import de.schosin.ecs.engine.components.TransmutationManager.AbstractTransmuter;
import de.schosin.ecs.storage.api.components.Component;

@EcsCodegen
public class TransmuterManager extends BaseTransmuterManager implements TransmuterPlugin {

    private final ComponentManager componentManager;
    private final TransmutationManager transmutationManager;
    private final ComponentMapperManager componentMapperManager;

    private final Map<Class<?>, PooledComponents<?>> mappers = new ConcurrentHashMap<>();

    public TransmuterManager(World world) {
        this.componentManager = world.getSingleton(ComponentManager.class);
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

        protected AbstractAddTransmuter(TransmuterManager manager, Transmuter.Builder builder) {
            super(manager.transmutationManager, convert(manager, builder.getAdd()), convert(manager, builder.getRemove()));

            this.manager = manager;
        }

        private static Component<?>[] convert(TransmuterManager manager, Set<RegularComponentType<?>> classes) {
            return classes.stream().map(manager.componentManager::getComponent).toArray(Component[]::new);
        }

        @Override
        public <T extends Pooled> T getInstance(Class<T> clazz) {
            var mapper = manager.mappers.computeIfAbsent(clazz, key -> manager.componentMapperManager.getPooledComponents(clazz));

            return clazz.cast(mapper.getInstance());
        }

    }

}
