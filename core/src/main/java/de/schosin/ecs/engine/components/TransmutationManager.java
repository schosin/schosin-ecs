package de.schosin.ecs.engine.components;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import de.schosin.ecs.api.components.ComponentType.RegularComponentType;
import de.schosin.ecs.engine.ChangeManager;
import de.schosin.ecs.engine.entities.EntityManager;
import de.schosin.ecs.storage.api.components.Component;
import de.schosin.ecs.utils.collections.Bag;

public class TransmutationManager {

    public interface Builder {

        Set<RegularComponentType<?>> getAdd();

        Set<RegularComponentType<?>> getRemove();

        @Override
        boolean equals(Object obj);

        @Override
        int hashCode();

    }

    private static final Component<?>[] EMPTY = new Component[0];

    private final ChangeManager changeManager;
    private final ComponentManager componentManager;
    private final ComponentMaskManager componentMaskManager;
    private final EntityManager entityManager;

    private final Map<Builder, AbstractTransmuter> transmuters = new ConcurrentHashMap<>();

    public TransmutationManager(ChangeManager changeManager, ComponentManager componentManager, ComponentMaskManager componentMaskManager, EntityManager entityManager) {
        this.changeManager = changeManager;
        this.componentManager = componentManager;
        this.componentMaskManager = componentMaskManager;
        this.entityManager = entityManager;
    }

    @SuppressWarnings("unchecked")
    public <T> Add<T> getAddTransmuter(RegularComponentType<T> component) {
        return (Add<T>) getTransmuter(ImmutableBuilder.add(component), () -> new Add<>(componentManager.getComponent(component)));
    }

    public Remove getRemoveTransmuter(RegularComponentType<?> component) {
        return getTransmuter(ImmutableBuilder.remove(component), () -> new Remove(componentManager.getComponent(component)));
    }

    @SuppressWarnings("unchecked")
    public <T extends AbstractTransmuter> T getTransmuter(Builder builder, Supplier<T> supplier) {
        var result = (T) transmuters.get(builder);
        if (result != null) {
            return result;
        }

        return (T) transmuters.computeIfAbsent(ImmutableBuilder.create(builder), ignore -> supplier.get());
    }

    public class Add<T> extends AbstractTransmuter {

        public Add(Component<?> add) {
            super(TransmutationManager.this, new Component[] { add }, EMPTY);
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

        public Remove(Component<?> remove) {
            super(TransmutationManager.this, EMPTY, new Component[] { remove });
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

        private final Component<?>[] add;
        private final Component<?>[] remove;

        private final Bag<ComponentMask> cache = new Bag<>(ComponentMask.class, 64);

        protected AbstractTransmuter(TransmutationManager manager, Builder builder) {
            this(manager, convert(manager, builder.getAdd()), convert((TransmutationManager) manager, builder.getRemove()));
        }

        private static Component<?>[] convert(TransmutationManager manager, Set<RegularComponentType<?>> classes) {
            return classes.stream().map(manager.componentManager::getComponent).toArray(Component[]::new);
        }

        protected AbstractTransmuter(TransmutationManager manager, Component<?>[] add, Component<?>[] remove) {
            this.manager = manager;

            this.add = add;
            this.remove = remove;
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

        @SuppressWarnings({ "rawtypes", "unchecked" })
        private final void addComponents(int entityId, Object... components) {
            for (var component : components) {
                var metadata = (Component) manager.componentManager.getComponent(component);
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

    private record ImmutableBuilder(Set<RegularComponentType<?>> add, Set<RegularComponentType<?>> remove) implements Builder {

        private static ImmutableBuilder add(RegularComponentType<?> component) {
            return new ImmutableBuilder(Set.of(component), Set.of());
        }

        private static ImmutableBuilder remove(RegularComponentType<?> component) {
            return new ImmutableBuilder(Set.of(), Set.of(component));
        }

        private static ImmutableBuilder create(Builder builder) {
            if (builder instanceof ImmutableBuilder immutable) {
                return immutable;
            }

            return new ImmutableBuilder(Set.copyOf(builder.getAdd()), Set.copyOf(builder.getRemove()));
        }

        @Override
        public Set<RegularComponentType<?>> getAdd() {
            return add;
        }

        @Override
        public Set<RegularComponentType<?>> getRemove() {
            return remove;
        }

    }

}
