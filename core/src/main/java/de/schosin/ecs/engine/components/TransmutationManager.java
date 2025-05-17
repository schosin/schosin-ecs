package de.schosin.ecs.engine.components;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.SequencedSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import de.schosin.ecs.api.components.ComponentType;
import de.schosin.ecs.api.components.ComponentType.RegularComponentType;
import de.schosin.ecs.engine.ChangeManager;
import de.schosin.ecs.engine.entities.EntityManager;
import de.schosin.ecs.storage.api.components.Component;
import de.schosin.ecs.utils.collections.Bag;
import de.schosin.ecs.utils.collections.ImmutableBag;

public class TransmutationManager {

    public interface Builder {

        SequencedSet<RegularComponentType<?>> getAdd();

        SequencedSet<ComponentType<?>> getRemove();

        @Override
        boolean equals(Object obj);

        @Override
        int hashCode();

    }

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

    public <T> Add<T> getAddTransmuter(RegularComponentType<T> component) {
        return getTransmuter(ImmutableBuilder.add(component), () -> new Add<>(component));
    }

    public Remove getRemoveTransmuter(ComponentType<?> component) {
        return getTransmuter(ImmutableBuilder.remove(component), () -> new Remove(component));
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

        public Add(RegularComponentType<T> add) {
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

        public Remove(ComponentType<?> remove) {
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

        private static final Bag<ImmutableBag<Component<?>>> EMPTY_BAG = new Bag<>(ImmutableBag.class, 0);

        private final TransmutationManager manager;

        private final Component<?>[] add;
        private final ImmutableBag<ImmutableBag<Component<?>>> remove;

        private final Bag<ComponentMask> cache = new Bag<>(ComponentMask.class, 64);

        protected AbstractTransmuter(TransmutationManager manager, Builder builder) {
            this(manager, builder.getAdd(), builder.getRemove());
        }

        protected AbstractTransmuter(TransmutationManager manager, Set<RegularComponentType<?>> add, Set<ComponentType<?>> remove) {
            this(manager, convert(manager, add), convertRemove(manager, remove));
        }

        private static Component<?>[] convert(TransmutationManager manager, Set<RegularComponentType<?>> types) {
            return types.stream().map(manager.componentManager::getComponent).toArray(Component[]::new);
        }

        @SuppressWarnings("unchecked")
        private static ImmutableBag<ImmutableBag<Component<?>>> convertRemove(TransmutationManager manager, Set<ComponentType<?>> types) {
            if (types.isEmpty()) {
                return EMPTY_BAG;
            }

            var bag = new Bag<ImmutableBag<Component<?>>>(ImmutableBag.class, 64);

            for (var type : types) {
                var components = manager.componentManager.getComponents(type);
                bag.add((ImmutableBag<Component<?>>) components);
            }

            return ImmutableBag.create(bag);
        }

        private AbstractTransmuter(TransmutationManager manager, Component<?>[] add, ImmutableBag<ImmutableBag<Component<?>>> remove) {
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
            if (components.length == 0) {
                return;
            }

            for (int i = 0, s = components.length; i < s; i++) {
                var instance = components[i];
                var component = (Component) this.add[i];

                manager.changeManager.addComponent(entityId, component, instance);
            }
        }

        private final void removeComponents(int entityId) {
            for (int i = 0, s = remove.getSize(); i < s; i++) {
                var bags = remove.get(i);

                for (int j = 0, js = bags.getSize(); j < js; j++) {
                    manager.changeManager.removeComponent(entityId, bags.get(j));
                }
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

            for (int i = 0, s = remove.getSize(); i < s; i++) {
                var bags = remove.get(i);

                for (int j = 0, js = bags.getSize(); j < js; j++) {
                    result = manager.componentMaskManager.removeComponent(result, bags.get(j));
                }
            }

            return result;
        }

    }

    private record ImmutableBuilder(SequencedSet<RegularComponentType<?>> add, SequencedSet<ComponentType<?>> remove) implements Builder {

        @SuppressWarnings("rawtypes")
        private static final SequencedSet EMPTY = Collections.unmodifiableSequencedSet(new LinkedHashSet<>());

        @SuppressWarnings("unchecked")
        private static ImmutableBuilder add(RegularComponentType<?> component) {
            return new ImmutableBuilder(sequencedSet(component), EMPTY);
        }

        @SuppressWarnings("unchecked")
        private static ImmutableBuilder remove(ComponentType<?> component) {
            return new ImmutableBuilder(EMPTY, sequencedSet(component));
        }

        private static ImmutableBuilder create(Builder builder) {
            if (builder instanceof ImmutableBuilder immutable) {
                return immutable;
            }

            return new ImmutableBuilder(copyOf(builder.getAdd()), copyOf(builder.getRemove()));
        }

        private static <T> SequencedSet<T> sequencedSet(T item) {
            var result = new LinkedHashSet<T>();
            result.add(item);

            return Collections.unmodifiableSequencedSet(result);
        }

        private static <T> SequencedSet<T> copyOf(SequencedSet<T> set) {
            var result = new LinkedHashSet<T>(set);

            return Collections.unmodifiableSequencedSet(result);
        }

        @Override
        public SequencedSet<RegularComponentType<?>> getAdd() {
            return add;
        }

        @Override
        public SequencedSet<ComponentType<?>> getRemove() {
            return remove;
        }

    }

}
