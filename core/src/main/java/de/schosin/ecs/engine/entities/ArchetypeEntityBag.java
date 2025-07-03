package de.schosin.ecs.engine.entities;

import java.lang.ref.Cleaner;
import java.util.Iterator;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.ObjIntConsumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import de.schosin.ecs.api.components.mappers.Components;
import de.schosin.ecs.api.components.types.ComponentType;
import de.schosin.ecs.api.components.types.ComponentType.RegularComponentType;
import de.schosin.ecs.api.components.types.DataProcessorType;
import de.schosin.ecs.api.data.DataProcessor;
import de.schosin.ecs.api.data.IterableAccessor;
import de.schosin.ecs.api.data.IterableComponentAccessor;
import de.schosin.ecs.api.entities.Entity;
import de.schosin.ecs.api.entities.ImmutableEntityBag;
import de.schosin.ecs.api.entities.ImmutableProcessableBag;
import de.schosin.ecs.engine.components.ComponentMapperManager;
import de.schosin.ecs.engine.entities.EntityManager.FreeableEntity;
import de.schosin.ecs.storage.api.entities.Archetype;
import de.schosin.ecs.utils.collections.Bag;

public final class ArchetypeEntityBag implements ImmutableEntityBag {
    private static final Cleaner cleaner = Cleaner.create();

    private final EntityManager entityManager;
    private final ComponentMapperManager componentMapperManager;

    private final Set<RegularComponentType<?, ?>> componentTypes;
    private final Bag<Archetype> archetypes = new Bag<>(Archetype.class, 4);

    public ArchetypeEntityBag(EntityManager entityManager, ComponentMapperManager componentMapperManager, Set<RegularComponentType<?, ?>> componentTypes) {
        this.entityManager = entityManager;
        this.componentMapperManager = componentMapperManager;

        this.componentTypes = componentTypes;
    }

    public void handleArchetype(Archetype archetype) {
        var archetypeComponentTypes = archetype.getComponentMask().getComponentTypes();

        for (var componentType : componentTypes) {
            if (!archetypeComponentTypes.contains(componentType)) {
                return;
            }
        }

        this.archetypes.add(archetype);
    }

    @Override
    public int size() {
        var size = 0;
        for (int i = 0, s = archetypes.getSize(); i < s; i++) {
            size += archetypes.get(i).getCount();
        }

        return size;
    }

    @Override
    public boolean isEmpty() {
        return archetypes.isEmpty() || size() == 0;
    }

    @Override
    public boolean contains(int entityId) {
        for (int i = 0, s = archetypes.getSize(); i < s; i++) {
            if (archetypes.get(i).contains(entityId)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public Entity get(int index) {
        var idx = index;
        for (int i = 0, s = archetypes.getSize(); i < s; i++) {
            var archetype = archetypes.get(i);
            var size = archetype.getCount();

            if (idx < size) {
                var entityId = archetype.getEntityData().getAccessorByIndex(idx).entityId();

                return entityManager.getEntity(entityId);
            }

            idx -= size;
        }

        throw new ArrayIndexOutOfBoundsException(index);
    }

    @Override
    public void process(Consumer<Entity> consumer) {
        for (int i = 0, s = archetypes.getSize(); i < s; i++) {
            var archetype = archetypes.get(i);
            if (archetype.getCount() == 0) {
                continue;
            }

            var accessor = archetype.getEntityData().getAccessor();
            var entity = entityManager.getIterableEntity(accessor);

            while (accessor.hasNext()) {
                accessor.next();
                consumer.accept(entity);
            }

            accessor.free();
            entityManager.freeEntity(entity);
        }
    }

    @Override
    public void process(ObjIntConsumer<Entity> consumer) {
        var idx = 0;
        for (int i = 0, s = archetypes.getSize(); i < s; i++) {
            var archetype = archetypes.get(i);
            if (archetype.getCount() == 0) {
                continue;
            }

            var accessor = archetype.getEntityData().getAccessor();
            var entity = entityManager.getIterableEntity(accessor);

            while (accessor.hasNext()) {
                accessor.next();
                consumer.accept(entity, idx++);
            }

            entityManager.freeEntity(entity);
            accessor.free();
        }
    }

    @Override
    public Iterator<Entity> iterator() {
        if (isEmpty()) {
            return EmptyIterator.INSTANCE;
        }

        return new IteratorImpl();
    }

    @Override
    public Stream<Entity> stream() {
        return StreamSupport.stream(spliterator(), false);
    }

    @Override
    public <R, P extends DataProcessor<R>> ImmutableProcessableBag<P> forType(DataProcessorType<?, R, P> componentType) {
        var mapper = componentMapperManager.getComponents(componentType);

        return new IterableProcessableBag<>(this, mapper);
    }

    @Override
    public <R> ImmutableProcessableBag<DataProcessor<R>> forType(ComponentType<?, R> componentType) {
        var mapper = componentMapperManager.getComponents(componentType);

        return new ProcessableBag<>(this, mapper);
    }

    private final class IteratorImpl implements Iterator<Entity> {

        private final int size;

        private int index;
        private IterableAccessor accessor;
        private FreeableEntity entity;

        private IteratorImpl() {
            this.size = archetypes.getSize();

            // local variable required for cleaner to do its job
            var accessor = this.accessor = archetypes.get(index).getEntityData().getAccessor();
            var entity = this.entity = entityManager.getIterableEntity(accessor);

            cleaner.register(this, accessor::free);
            cleaner.register(this, entity::free);
        }

        @Override
        public boolean hasNext() {
            while (!accessor.hasNext() && index < size - 1) {
                // local variable required for cleaner to do its job
                var accessor = this.accessor = archetypes.get(++index).getEntityData().getAccessor();
                var entity = this.entity = entityManager.getIterableEntity(accessor);

                cleaner.register(this, accessor::free);
                cleaner.register(this, entity::free);
            }

            return accessor.hasNext();
        }

        @Override
        public Entity next() {
            accessor.next();
            return entity;
        }

    }

    private static enum EmptyIterator implements Iterator<Entity> {
        INSTANCE;

        @Override
        public boolean hasNext() {
            return false;
        }

        @Override
        public Entity next() {
            throw new IllegalStateException("next() called when hasNext() returned false");
        }
    }

    private final class ProcessableBag<R, P extends DataProcessor<R>> extends AbstractProcessableBag<R, P> {

        private ProcessableBag(ArchetypeEntityBag bag, Components<?, R> mapper) {
            super(bag, mapper);
        }

        @Override
        public final void process(P processor) {
            for (int i = 0, s = archetypes.getSize(); i < s; i++) {
                var archetype = archetypes.get(i);
                if (archetype.getCount() == 0) {
                    continue;
                }

                var accessor = archetype.getEntityData().getAccessor();
                var componentAccessor = mapper.getComponentAccessor(accessor);

                while (accessor.hasNext()) {
                    processor.process(accessor.next(), componentAccessor.getComponent(accessor));
                }

                componentAccessor.free();
                accessor.free();
            }
        }

    }

    private final class IterableProcessableBag<R, P extends DataProcessor<R>> extends AbstractProcessableBag<R, P> {

        private IterableProcessableBag(ArchetypeEntityBag bag, Components<?, R> mapper) {
            super(bag, mapper);
        }

        @Override
        @SuppressWarnings("unchecked")
        public final void process(P processor) {
            for (int i = 0, s = archetypes.getSize(); i < s; i++) {
                var archetype = archetypes.get(i);
                if (archetype.getCount() == 0) {
                    continue;
                }

                var accessor = archetype.getEntityData().getAccessor();

                var componentAccessor = (IterableComponentAccessor<R, P>) mapper.getComponentAccessor(accessor);
                componentAccessor.process(accessor, processor);
                componentAccessor.free();

                accessor.free();
            }
        }

    }

    private abstract sealed class AbstractProcessableBag<R, P extends DataProcessor<R>> implements ImmutableProcessableBag<P> {

        protected final ArchetypeEntityBag bag;
        protected final Components<?, R> mapper;

        private AbstractProcessableBag(ArchetypeEntityBag bag, Components<?, R> mapper) {
            this.bag = bag;
            this.mapper = mapper;
        }

        @Override
        public final <RR, PP extends DataProcessor<RR>> ImmutableProcessableBag<PP> forType(DataProcessorType<?, RR, PP> componentType) {
            var mapper = componentMapperManager.getComponents(componentType);

            return new IterableProcessableBag<>(bag, mapper);
        }

        @Override
        public final <RR> ImmutableProcessableBag<DataProcessor<RR>> forType(ComponentType<?, RR> componentType) {
            var mapper = componentMapperManager.getComponents(componentType);

            return new ProcessableBag<>(bag, mapper);
        }

        @Override
        public final int size() {
            return bag.size();
        }

        @Override
        public final boolean isEmpty() {
            return bag.isEmpty();
        }

        @Override
        public final boolean contains(int entityId) {
            return bag.contains(entityId);
        }

        @Override
        public final Entity get(int index) {
            return bag.get(index);
        }

        @Override
        public final void process(Consumer<Entity> consumer) {
            bag.process(consumer);
        }

        @Override
        public final void process(ObjIntConsumer<Entity> consumer) {
            bag.process(consumer);
        }

        @Override
        public final Stream<Entity> stream() {
            return bag.stream();
        }

        @Override
        public final Iterator<Entity> iterator() {
            return bag.iterator();
        }

    }

}
