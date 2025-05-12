package de.schosin.ecs.plugins.transmuter;

import java.util.Set;
import java.util.function.Supplier;

import de.schosin.ecs.api.Pooled;
import de.schosin.ecs.api.World;
import de.schosin.ecs.codegen.EcsCodegen;
import de.schosin.ecs.engine.components.Component;
import de.schosin.ecs.engine.components.Component.PooledComponent;
import de.schosin.ecs.engine.components.ComponentManager;
import de.schosin.ecs.engine.components.TransmutationManager;
import de.schosin.ecs.engine.components.TransmutationManager.AbstractTransmuter;

@EcsCodegen
public class TransmuterManager extends BaseTransmuterManager implements TransmuterPlugin {

    private final ComponentManager componentManager;
    private final TransmutationManager transmutationManager;

    public TransmuterManager(World world) {
        this.componentManager = world.getSingleton(ComponentManager.class);
        this.transmutationManager = world.getSingleton(TransmutationManager.class);
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

        private static Component<?>[] convert(TransmuterManager manager, Set<Class<?>> classes) {
            return classes.stream().map(manager.componentManager::getComponent).toArray(Component[]::new);
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T extends Pooled> T getInstance(Class<T> clazz) {
            var component = (PooledComponent<T>) manager.componentManager.getComponent(clazz);
            return component.getInstance();
        }

    }

}
