package de.schosin.ecs.engine.components;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import de.schosin.ecs.api.Pooled;
import de.schosin.ecs.api.archetype.Transmuter;
import de.schosin.ecs.api.archetype.Transmuter.Builder;
import de.schosin.ecs.api.archetype.Transmuter.Remove;
import de.schosin.ecs.codegen.EcsCodegen;
import de.schosin.ecs.engine.ChangeManager;
import de.schosin.ecs.engine.entities.EntityManager;
import de.schosin.ecs.engine.utils.collections.Bag;

@EcsCodegen
public class TransmutationManager extends BaseTransmutationManager implements Transmuter.Creator {

    private final ChangeManager changeManager;
    private final ComponentManager componentManager;
    private final ComponentMaskManager componentMaskManager;
    private final EntityManager entityManager;

    private final Map<Transmuter.Builder, Transmuter> transmuters = new ConcurrentHashMap<>();

    public TransmutationManager(ChangeManager changeManager, ComponentManager componentManager, ComponentMaskManager componentMaskManager, EntityManager entityManager) {
        this.changeManager = changeManager;
        this.componentManager = componentManager;
        this.componentMaskManager = componentMaskManager;
        this.entityManager = entityManager;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected <T extends Transmuter> T getTransmuter(Builder builder, Supplier<T> supplier) {
        return (T) transmuters.computeIfAbsent(builder, ignore -> supplier.get());
    }

    @Override
    public Remove createTransmuter(Builder.Remove builder) {
        return getTransmuter(builder, () -> new RemoveImpl(builder));
    }

    private class RemoveImpl extends AbstractTransmuter implements Transmuter.Remove {

        private RemoveImpl(Builder.Remove builder) {
            super(TransmutationManager.this, builder);
        }

        @Override
        public boolean apply(int entityId) {
            return super.apply(entityId);
        }

    }

    abstract static class AbstractAddTransmuter extends AbstractTransmuter implements Transmuter.Add {

        protected AbstractAddTransmuter(BaseTransmutationManager manager, Transmuter.Builder builder) {
            super(manager, builder);
        }

        @Override
        public <T extends Pooled> T getInstance(Class<T> clazz) {
            return manager.componentManager.getData(clazz).getInstance();
        }

    }

    private abstract static class AbstractTransmuter implements Transmuter {

        protected final TransmutationManager manager;

        protected final Component[] add;
        protected final Component[] remove;

        protected final Bag<ComponentMask> cache = new Bag<>(ComponentMask.class, 64);

        protected AbstractTransmuter(BaseTransmutationManager manager, Transmuter.Builder builder) {
            this.manager = (TransmutationManager) manager;

            this.add = builder.getAdd().stream().map(this.manager.componentManager::getData).toArray(Component[]::new);
            this.remove = builder.getRemove().stream().map(this.manager.componentManager::getData).toArray(Component[]::new);
        }

        protected boolean apply(int entityId, Object... added) {
            // Retrieve component mask, return early if null (entity does not exist)
            var componentMask = manager.entityManager.getComponentMask(entityId);
            if (componentMask == null) {
                return false;
            }

            // Retrieve pending component mask change if present
            var pendingComponentMaskId = manager.changeManager.getPendingComponentMask(entityId);
            if (pendingComponentMaskId > -1) {
                componentMask = manager.componentMaskManager.getComponentMask(pendingComponentMaskId);
            }

            // Modify components
            addComponents(entityId, added);
            removeComponents(entityId);

            // Update component mask
            var updatedComponentMask = getNewComponentMask(componentMask);
            if (updatedComponentMask.getId() == componentMask.getId()) {
                return false;
            }

            // Notify entity changes
            manager.changeManager.updateEntity(entityId, updatedComponentMask);

            return true;
        }

        private final void addComponents(int entityId, Object... components) {
            for (var component : components) {
                var metadata = manager.componentManager.getData(component.getClass());
                manager.changeManager.addComponent(entityId, metadata, component);
            }
        }

        private final void removeComponents(int entityId) {
            for (var metadata : remove) {
                manager.changeManager.removeComponent(entityId, metadata);
            }
        }

        private final ComponentMask getNewComponentMask(ComponentMask componentMask) {
            // Retrieve cached value
            var cached = cache.get(componentMask.getId());
            if (cached != null) {
                return cached;
            }

            // Build result
            var result = computeNewComponentMask(componentMask);
            cache.set(componentMask.getId(), result);

            return result;
        }

        private ComponentMask computeNewComponentMask(ComponentMask componentMask) {
            var result = componentMask;

            for (var metadata : add) {
                result = manager.componentMaskManager.addComponent(result, metadata);
            }
            for (var metadata : remove) {
                result = manager.componentMaskManager.removeComponent(result, metadata);
            }

            return result;
        }

    }

}
