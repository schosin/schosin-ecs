package de.schosin.ecs.engine.components;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import de.schosin.ecs.api.Pooled;
import de.schosin.ecs.api.archetype.Transmuter;
import de.schosin.ecs.api.archetype.Transmuter.Builder;
import de.schosin.ecs.api.archetype.Transmuter.Remove;
import de.schosin.ecs.engine.ChangeManager;
import de.schosin.ecs.engine.entities.EntityManager;
import de.schosin.ecs.engine.utils.collections.ArrayUtils;
import de.schosin.ecs.engine.utils.collections.Bag;

public class TransmutationManager implements Transmuter.Creator {

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
    public Remove createTransmuter(Builder.Remove builder) {
        var transmuter = (Remove) transmuters.get(builder);
        if (transmuter != null) {
            return transmuter;
        }

        return (Remove) transmuters.computeIfAbsent(builder, ignore -> new RemoveImpl(builder));
    }

    @Override
    @SuppressWarnings({ "unchecked" })
    public <T1> Transmuter.Add1<T1> createTransmuter(Builder.Add1<T1> builder) {
        var transmuter = (Transmuter.Add1<T1>) transmuters.get(builder);
        if (transmuter != null) {
            return transmuter;
        }

        return (Transmuter.Add1<T1>) transmuters.computeIfAbsent(builder, ignore -> new Add1Impl<>(builder));
    }

    @Override
    @SuppressWarnings({ "unchecked" })
    public <T1, T2, T3, T4, T5, T6, T7, T8> Transmuter.Add8<T1, T2, T3, T4, T5, T6, T7, T8> createTransmuter(Builder.Add8<T1, T2, T3, T4, T5, T6, T7, T8> builder) {
        var transmuter = (Transmuter.Add8<T1, T2, T3, T4, T5, T6, T7, T8>) transmuters.get(builder);
        if (transmuter != null) {
            return transmuter;
        }

        return (Transmuter.Add8<T1, T2, T3, T4, T5, T6, T7, T8>) transmuters.computeIfAbsent(builder, ignore -> new Add8Impl<>(builder));
    }

    @Override
    @SuppressWarnings({ "unchecked" })
    public <T1, T2> Transmuter.Add2<T1, T2> createTransmuter(Builder.Add2<T1, T2> builder) {
        var transmuter = (Transmuter.Add2<T1, T2>) transmuters.get(builder);
        if (transmuter != null) {
            return transmuter;
        }

        return (Transmuter.Add2<T1, T2>) transmuters.computeIfAbsent(builder, ignore -> new Add2Impl<>(builder));
    }

    @Override
    @SuppressWarnings({ "unchecked" })
    public <T1, T2, T3> Transmuter.Add3<T1, T2, T3> createTransmuter(Builder.Add3<T1, T2, T3> builder) {
        var transmuter = (Transmuter.Add3<T1, T2, T3>) transmuters.get(builder);
        if (transmuter != null) {
            return transmuter;
        }

        return (Transmuter.Add3<T1, T2, T3>) transmuters.computeIfAbsent(builder, ignore -> new Add3Impl<>(builder));
    }

    @Override
    @SuppressWarnings({ "unchecked" })
    public <T1, T2, T3, T4> Transmuter.Add4<T1, T2, T3, T4> createTransmuter(Builder.Add4<T1, T2, T3, T4> builder) {
        var transmuter = (Transmuter.Add4<T1, T2, T3, T4>) transmuters.get(builder);
        if (transmuter != null) {
            return transmuter;
        }

        return (Transmuter.Add4<T1, T2, T3, T4>) transmuters.computeIfAbsent(builder, ignore -> new Add4Impl<>(builder));
    }

    @Override
    @SuppressWarnings({ "unchecked" })
    public <T1, T2, T3, T4, T5> Transmuter.Add5<T1, T2, T3, T4, T5> createTransmuter(Builder.Add5<T1, T2, T3, T4, T5> builder) {
        var transmuter = (Transmuter.Add5<T1, T2, T3, T4, T5>) transmuters.get(builder);
        if (transmuter != null) {
            return transmuter;
        }

        return (Transmuter.Add5<T1, T2, T3, T4, T5>) transmuters.computeIfAbsent(builder, ignore -> new Add5Impl<>(builder));
    }

    @Override
    @SuppressWarnings({ "unchecked" })
    public <T1, T2, T3, T4, T5, T6> Transmuter.Add6<T1, T2, T3, T4, T5, T6> createTransmuter(Builder.Add6<T1, T2, T3, T4, T5, T6> builder) {
        var transmuter = (Transmuter.Add6<T1, T2, T3, T4, T5, T6>) transmuters.get(builder);
        if (transmuter != null) {
            return transmuter;
        }

        return (Transmuter.Add6<T1, T2, T3, T4, T5, T6>) transmuters.computeIfAbsent(builder, ignore -> new Add6Impl<>(builder));
    }

    @Override
    @SuppressWarnings({ "unchecked" })
    public <T1, T2, T3, T4, T5, T6, T7> Transmuter.Add7<T1, T2, T3, T4, T5, T6, T7> createTransmuter(Builder.Add7<T1, T2, T3, T4, T5, T6, T7> builder) {
        var transmuter = (Transmuter.Add7<T1, T2, T3, T4, T5, T6, T7>) transmuters.get(builder);
        if (transmuter != null) {
            return transmuter;
        }

        return (Transmuter.Add7<T1, T2, T3, T4, T5, T6, T7>) transmuters.computeIfAbsent(builder, ignore -> new Add7Impl<>(builder));
    }

    @Override
    @SuppressWarnings({ "unchecked" })
    public <T1, T2, T3, T4, T5, T6, T7, T8> Transmuter.AddN<T1, T2, T3, T4, T5, T6, T7, T8> createTransmuter(Builder.AddN<T1, T2, T3, T4, T5, T6, T7, T8> builder) {
        var transmuter = (Transmuter.AddN<T1, T2, T3, T4, T5, T6, T7, T8>) transmuters.get(builder);
        if (transmuter != null) {
            return transmuter;
        }

        return (Transmuter.AddN<T1, T2, T3, T4, T5, T6, T7, T8>) transmuters.computeIfAbsent(builder, ignore -> new AddNImpl<>(builder));
    }

    private class RemoveImpl extends AbstractTransmuter implements Transmuter.Remove {

        private RemoveImpl(Builder.Remove builder) {
            super(Set.of(), builder.getRemove());
        }

        @Override
        public boolean apply(int entityId) {
            return super.apply(entityId);
        }

    }

    private class Add1Impl<T1> extends AbstractAddTransmuter implements Transmuter.Add1<T1> {

        private Add1Impl(Builder.Add1<T1> builder) {
            super(builder.getAdd(), builder.getRemove());
        }

        @Override
        public boolean apply(int entityId, T1 component1) {
            return super.apply(entityId, component1);
        }

    }

    private class Add2Impl<T1, T2> extends AbstractAddTransmuter implements Transmuter.Add2<T1, T2> {

        private Add2Impl(Builder.Add2<T1, T2> builder) {
            super(builder.getAdd(), builder.getRemove());
        }

        @Override
        public boolean apply(int entityId, T1 component1, T2 component2) {
            return super.apply(entityId, component1, component2);
        }

    }

    private class Add3Impl<T1, T2, T3> extends AbstractAddTransmuter implements Transmuter.Add3<T1, T2, T3> {

        private Add3Impl(Builder.Add3<T1, T2, T3> builder) {
            super(builder.getAdd(), builder.getRemove());
        }

        @Override
        public boolean apply(int entityId, T1 component1, T2 component2, T3 component3) {
            return super.apply(entityId, component1, component2, component3);
        }

    }

    private class Add4Impl<T1, T2, T3, T4> extends AbstractAddTransmuter implements Transmuter.Add4<T1, T2, T3, T4> {

        private Add4Impl(Builder.Add4<T1, T2, T3, T4> builder) {
            super(builder.getAdd(), builder.getRemove());
        }

        @Override
        public boolean apply(int entityId, T1 component1, T2 component2, T3 component3, T4 component4) {
            return super.apply(entityId, component1, component2, component3, component4);
        }

    }

    private class Add5Impl<T1, T2, T3, T4, T5> extends AbstractAddTransmuter implements Transmuter.Add5<T1, T2, T3, T4, T5> {

        private Add5Impl(Builder.Add5<T1, T2, T3, T4, T5> builder) {
            super(builder.getAdd(), builder.getRemove());
        }

        @Override
        public boolean apply(int entityId, T1 component1, T2 component2, T3 component3, T4 component4, T5 component5) {
            return super.apply(entityId, component1, component2, component3, component4, component5);
        }

    }

    private class Add6Impl<T1, T2, T3, T4, T5, T6> extends AbstractAddTransmuter implements Transmuter.Add6<T1, T2, T3, T4, T5, T6> {

        private Add6Impl(Builder.Add6<T1, T2, T3, T4, T5, T6> builder) {
            super(builder.getAdd(), builder.getRemove());
        }

        @Override
        public boolean apply(int entityId, T1 component1, T2 component2, T3 component3, T4 component4, T5 component5, T6 component6) {
            return super.apply(entityId, component1, component2, component3, component4, component5, component6);
        }

    }

    private class Add7Impl<T1, T2, T3, T4, T5, T6, T7> extends AbstractAddTransmuter implements Transmuter.Add7<T1, T2, T3, T4, T5, T6, T7> {

        private Add7Impl(Builder.Add7<T1, T2, T3, T4, T5, T6, T7> builder) {
            super(builder.getAdd(), builder.getRemove());
        }

        @Override
        public boolean apply(int entityId, T1 component1, T2 component2, T3 component3, T4 component4, T5 component5, T6 component6, T7 component7) {
            return super.apply(entityId, component1, component2, component3, component4, component5, component6, component7);
        }

    }

    private class Add8Impl<T1, T2, T3, T4, T5, T6, T7, T8> extends AbstractAddTransmuter implements Transmuter.Add8<T1, T2, T3, T4, T5, T6, T7, T8> {

        private Add8Impl(Builder.Add8<T1, T2, T3, T4, T5, T6, T7, T8> builder) {
            super(builder.getAdd(), builder.getRemove());
        }

        @Override
        public boolean apply(int entityId, T1 component1, T2 component2, T3 component3, T4 component4, T5 component5, T6 component6, T7 component7, T8 component8) {
            return super.apply(entityId, component1, component2, component3, component4, component5, component6, component7, component8);
        }

    }

    private class AddNImpl<T1, T2, T3, T4, T5, T6, T7, T8> extends AbstractAddTransmuter implements Transmuter.AddN<T1, T2, T3, T4, T5, T6, T7, T8> {

        private AddNImpl(Builder.AddN<T1, T2, T3, T4, T5, T6, T7, T8> builder) {
            super(builder.getAdd(), builder.getRemove());
        }

        @Override
        public boolean apply(int entityId, T1 component1, T2 component2, T3 component3, T4 component4, T5 component5, T6 component6, T7 component7, T8 component8, Object... others) {
            return super.apply(entityId, ArrayUtils.concat(Object.class, new Object[] { component1, component2, component3, component4, component5, component6, component7, component8 }, others));
        }

    }

    private abstract class AbstractAddTransmuter extends AbstractTransmuter implements Transmuter.Add {

        protected AbstractAddTransmuter(Set<Class<?>> add, Set<Class<?>> remove) {
            super(add, remove);
        }

        @Override
        public <T extends Pooled> T getInstance(Class<T> clazz) {
            return componentManager.getData(clazz).getInstance();
        }

    }

    private abstract class AbstractTransmuter implements Transmuter {

        protected final Component[] add;
        protected final Component[] remove;

        protected final Bag<ComponentMask> cache = new Bag<>(ComponentMask.class, 64);

        protected AbstractTransmuter(Set<Class<?>> add, Set<Class<?>> remove) {
            this(add.stream().map(componentManager::getData).toArray(Component[]::new), remove.stream().map(componentManager::getData).toArray(Component[]::new));
        }

        protected AbstractTransmuter(Component[] add, Component[] remove) {
            this.add = add;
            this.remove = remove;
        }

        protected boolean apply(int entityId, Object... added) {
            // Retrieve component mask, return early if null (entity does not exist)
            var componentMask = entityManager.getComponentMask(entityId);
            if (componentMask == null) {
                return false;
            }

            // Retrieve pending component mask change if present
            var pendingComponentMaskId = changeManager.getPendingComponentMask(entityId);
            if (pendingComponentMaskId > -1) {
                componentMask = componentMaskManager.getComponentMask(pendingComponentMaskId);
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
            changeManager.updateEntity(entityId, updatedComponentMask);

            return true;
        }

        private final void addComponents(int entityId, Object... components) {
            for (var component : components) {
                var metadata = componentManager.getData(component.getClass());
                changeManager.addComponent(entityId, metadata, component);
            }
        }

        private final void removeComponents(int entityId) {
            for (var metadata : remove) {
                changeManager.removeComponent(entityId, metadata);
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
                result = componentMaskManager.addComponent(result, metadata);
            }
            for (var metadata : remove) {
                result = componentMaskManager.removeComponent(result, metadata);
            }

            return result;
        }

    }

}
