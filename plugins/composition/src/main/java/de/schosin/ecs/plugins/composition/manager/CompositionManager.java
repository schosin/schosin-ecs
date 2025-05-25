package de.schosin.ecs.plugins.composition.manager;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Spliterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

import org.jspecify.annotations.NonNull;

import de.schosin.ecs.api.World;
import de.schosin.ecs.api.components.mappers.Components;
import de.schosin.ecs.api.components.types.ComponentType;
import de.schosin.ecs.codegen.EcsCodegen;
import de.schosin.ecs.engine.BagManager;
import de.schosin.ecs.engine.components.ComponentMapperManager;
import de.schosin.ecs.engine.components.ComponentMapperManager.PoolingComponents;
import de.schosin.ecs.engine.components.ComponentMask;
import de.schosin.ecs.engine.components.ComponentMaskManager;
import de.schosin.ecs.engine.entities.EntityManager.ComponentsPredicate;
import de.schosin.ecs.engine.events.EventManager;
import de.schosin.ecs.engine.events.builtin.EntitiesEvent.EntitiesInsertedEvent;
import de.schosin.ecs.engine.events.builtin.EntityEvent.EntityInsertedEvent;
import de.schosin.ecs.engine.events.builtin.EntityEvent.EntityRemovedEvent;
import de.schosin.ecs.engine.events.builtin.EntityEvent.EntityUpdatedEvent;
import de.schosin.ecs.plugins.composition.BaseComposition;
import de.schosin.ecs.plugins.composition.Composition;
import de.schosin.ecs.plugins.composition.Composition.Builder;
import de.schosin.ecs.plugins.composition.CompositionPlugin;
import de.schosin.ecs.plugins.composition.Spec;
import de.schosin.ecs.utils.collections.Bag;
import de.schosin.ecs.utils.collections.IntBag;
import de.schosin.ecs.utils.collections.Pool;

@EcsCodegen
public class CompositionManager extends AbstractSpecManager implements CompositionPlugin {

    private final BagManager bagManager;
    private final ComponentMaskManager componentMaskManager;
    private final ComponentMapperManager componentMapperManager;

    private final Map<EngineSpec, CompositionImpl> compositions = new ConcurrentHashMap<>();

    private final Bag<Bag<CompositionImpl>> compositionsByMask = new Bag<>(Bag.class, 64);

    private final Pool<Set<ComponentType<?, ?>>> componentTypeSetPool = Pool.unbounded(Set.class, HashSet::new, Set::clear);
    private final Bag<ComponentMask> fill = new Bag<>(ComponentMask.class, 64);

    public CompositionManager(World world) {
        super(world);

        world.addSingleton(this);

        this.bagManager = world.getSingleton(BagManager.class);
        this.componentMaskManager = world.getSingleton(ComponentMaskManager.class);
        this.componentMapperManager = world.getSingleton(ComponentMapperManager.class);

        var eventManager = world.getSingleton(EventManager.class);
        eventManager.registerEventHandler(EntityInsertedEvent.class, event -> handleInserted(event.entityId(), event.componentMask()));
        eventManager.registerEventHandler(EntitiesInsertedEvent.class, event -> handleInserted(event.entityIds(), event.componentMask()));
        eventManager.registerEventHandler(EntityUpdatedEvent.class, event -> handleUpdated(event.entityId(), event.previousComponentMask(), event.componentMask()));
        eventManager.registerEventHandler(EntityRemovedEvent.class, event -> handleRemoved(event.entityId(), event.componentMask()));
    }

    @Override
    public Composition createComposition(Builder builder) {
        return create(builder, entityManager::getEntities);
    }

    public Composition create(Builder builder, Function<ComponentsPredicate, IntBag> entities) {
        var spec = buildSpec(builder);

        return this.compositions.computeIfAbsent(spec, ignore -> buildComposition(spec, entities));
    }

    private CompositionImpl buildComposition(EngineSpec spec, Function<ComponentsPredicate, IntBag> entities) {
        // Create composition
        var composition = new CompositionImpl(spec, entities.apply(spec), bagManager.createEntityIntBag());

        // Add composition to ComponentMask lookup 
        synchronized (compositionsByMask) {
            fill.clear();
            componentMaskManager.getComponentMasks(composition::isInterested, fill);

            var data = fill.getData();
            for (int i = 0, s = fill.getSize(); i < s; i++) {
                var componentMask = data[i];

                var maskCompositions = getCompositions(componentMask);
                maskCompositions.add(composition);
            }
        }

        return composition;
    }

    private void handleInserted(int entityId, ComponentMask componentMask) {
        var maskCompositions = getCompositions(componentMask);

        var data = maskCompositions.getData();
        for (int i = 0, s = maskCompositions.getSize(); i < s; i++) {
            var composition = data[i];
            if (composition.isInterested(componentMask)) {
                composition.inserted(entityId);
            }
        }
    }

    private void handleInserted(int[] entityIds, ComponentMask componentMask) {
        var maskCompositions = getCompositions(componentMask);

        var data = maskCompositions.getData();
        for (int i = 0, s = maskCompositions.getSize(); i < s; i++) {
            var composition = data[i];
            if (composition.isInterested(componentMask)) {
                for (var entityId : entityIds) {
                    composition.inserted(entityId);
                }
            }
        }
    }

    private void handleUpdated(int entityId, ComponentMask previousComponentMask, ComponentMask componentMask) {
        // Remove from previous composition if no longer interested
        var previousCompositions = getCompositions(previousComponentMask);

        var previousData = previousCompositions.getData();
        for (int i = 0, s = previousCompositions.getSize(); i < s; i++) {
            var composition = previousData[i];

            var beforeInterested = composition.containsEntity(entityId);
            if (beforeInterested && !composition.isInterested(componentMask)) {
                composition.removed(entityId);
            }
        }

        // Add to new composition if not yet contained
        var newCompositions = getCompositions(componentMask);

        var dataData = newCompositions.getData();
        for (int i = 0, s = newCompositions.getSize(); i < s; i++) {
            var composition = dataData[i];

            var beforeInterested = composition.containsEntity(entityId);
            if (!beforeInterested && composition.isInterested(componentMask)) {
                composition.inserted(entityId);
            }
        }
    }

    private void handleRemoved(int entityId, ComponentMask componentMask) {
        var maskCompositions = getCompositions(componentMask);

        var data = maskCompositions.getData();
        for (int i = 0, s = maskCompositions.getSize(); i < s; i++) {
            var composition = data[i];

            if (composition.containsEntity(entityId)) {
                composition.removed(entityId);
            }
        }
    }

    private Bag<CompositionImpl> getCompositions(ComponentMask componentMask) {
        synchronized (compositionsByMask) {
            var result = compositionsByMask.get(componentMask.getId());
            if (result == null) {
                result = new Bag<>(CompositionImpl.class);
                compositionsByMask.set(componentMask.getId(), result);

                // Add interested compositions
                for (var composition : this.compositions.values()) {
                    if (composition.isInterested(componentMask)) {
                        result.add(composition);
                    }
                }
            }

            return result;
        }
    }

    private final class CompositionImpl extends BaseCompositionImpl implements Composition {

        private final EngineSpec spec;

        private final IntBag entities;
        private final IntBag lookup;

        private final IntBag maskCache;

        private IntConsumer inserted;
        private Bag<IntConsumer> moreInserted;

        private IntConsumer removed;
        private Bag<IntConsumer> moreRemoved;

        private final Map<Set<ComponentType<?, ?>>, Composition.Of<?>> retrieves = new ConcurrentHashMap<>();

        private CompositionImpl(EngineSpec spec, IntBag entities, IntBag lookup) {
            this.spec = spec;

            this.entities = entities;
            this.lookup = lookup;

            var data = entities.getData();
            for (int i = 0, s = entities.getSize(); i < s; i++) {
                this.lookup.set(data[i], toLookup(i));
            }

            this.maskCache = new IntBag(64);
        }

        @Override
        protected <T extends Composition.Of<?>> T retrieve(Supplier<T> constructor, ComponentType<?, ?>... components) {
            return componentTypeSetPool.withInstance(componentTypes -> {
                for (var component : components) {
                    componentTypes.add(component);
                }

                return getRetrieveComposition(componentTypes, constructor);
            });
        }

        @Override
        protected Components<?, ?> getComponents(ComponentType<?, ?> type) {
            return componentMapperManager.getComponents(type);
        }

        @SuppressWarnings("unchecked")
        private <T extends Composition.Of<?>> T getRetrieveComposition(Set<ComponentType<?, ?>> componentTypes, Supplier<T> constructor) {
            // Lookup cached
            var result = retrieves.get(componentTypes);
            if (result != null) {
                return (T) result;
            }

            var key = Set.copyOf(componentTypes);
            return (T) retrieves.computeIfAbsent(key, ignore -> constructor.get());
        }

        private boolean containsEntity(int entityId) {
            return this.lookup.get(entityId) != 0;
        }

        private void inserted(int entityId) {
            this.lookup.set(entityId, toLookup(this.entities.getSize()));
            this.entities.add(entityId);

            if (inserted == null) {
                return;
            }

            // Process callbacks
            inserted.accept(entityId);

            if (moreInserted != null) {
                var data = moreInserted.getData();
                for (int i = 0, s = moreInserted.getSize(); i < s; i++) {
                    data[i].accept(entityId);
                }
            }
        }

        private void removed(int entityId) {
            // Retrieve index for fast removal
            var index = this.lookup.get(entityId);
            var lookupIndex = fromLookup(index);

            // Remove entity
            this.lookup.set(entityId, 0);
            this.entities.removeIndex(lookupIndex);

            // Fix lookup due to implementation detail of Bag#removeIndex (moves last element to removed position)
            var moved = this.entities.get(lookupIndex);
            if (moved != 0) {
                this.lookup.set(moved, index);
            }

            if (removed == null) {
                return;
            }

            // Process callbacks
            removed.accept(entityId);

            if (moreRemoved != null) {
                var data = moreRemoved.getData();
                for (int i = 0, s = moreRemoved.getSize(); i < s; i++) {
                    data[i].accept(entityId);
                }
            }
        }

        private int toLookup(int index) {
            return index == 0 ? -1 : index;
        }

        private int fromLookup(int index) {
            return index == -1 ? 0 : index;
        }

        public boolean isInterested(@NonNull ComponentMask componentMask) {
            // Check cached value
            var cached = maskCache.get(componentMask.getId());
            if (cached != 0) {
                return cached == 1;
            }

            // Test spec and cache result
            var result = spec.isInterested(componentMask.getMask());
            maskCache.set(componentMask.getId(), result ? 1 : 2);

            return result;
        }

        @Override
        public boolean isInterested(int entityId) {
            return containsEntity(entityId);
        }

        @Override
        public boolean matches(Spec spec) {
            if (spec == null) {
                throw new NullPointerException("spec was null");
            }

            if (spec instanceof CompositionImpl composition) {
                return this.spec.matches(composition.spec);
            }

            if (spec instanceof AbstractCompositionN composition) {
                return this.spec.matches(((CompositionImpl) composition.composition).spec);
            }

            if (spec instanceof SpecImpl impl) {
                return this.spec.matches(impl.spec);
            }

            throw new IllegalArgumentException("Only compare Specs and Compositions returned by the same world.");
        }

        @Override
        public void inserted(@NonNull IntConsumer callback) {
            if (this.inserted == null) {
                this.inserted = callback;
            } else {
                if (this.moreInserted == null) {
                    this.moreInserted = new Bag<>(IntConsumer.class, 8);
                }

                this.moreInserted.add(callback);
            }
        }

        @Override
        public void removed(@NonNull IntConsumer callback) {
            if (this.removed == null) {
                this.removed = callback;
            } else {
                if (this.moreRemoved == null) {
                    this.moreRemoved = new Bag<>(IntConsumer.class, 8);
                }

                this.moreRemoved.add(callback);
            }
        }

        @Override
        public int getCount() {
            return this.entities.getSize();
        }

        @Override
        public boolean isEmpty() {
            return this.entities.isEmpty();
        }

        @Override
        public void process(@NonNull IntConsumer process) {
            var data = this.entities.getData();
            for (int i = 0, s = this.entities.getSize(); i < s; i++) {
                process.accept(data[i]);
            }
        }

        @Override
        public IntStream stream() {
            return StreamSupport.intStream(spliterator(), false);
        }

        @Override
        public IntStream parallelStream() {
            return StreamSupport.intStream(spliterator(), true);
        }

        private CompositionSpliterator spliterator() {
            return new CompositionSpliterator(this.entities.getData(), 0, -1, this.entities.getSize());
        }

        @Override
        public String toString() {
            return "CompositionImpl [spec=" + this.spec + ", entities=" + this.entities.getSize() + "]";
        }

    }

    abstract static class AbstractCompositionN implements BaseComposition, Spec {

        protected final BaseCompositionImpl composition;

        private final Components<?, ?>[] components;

        protected AbstractCompositionN(BaseCompositionImpl composition, ComponentType<?, ?>... components) {
            this.composition = composition;
            this.components = new Components<?, ?>[components.length];
            for (int i = 0, s = components.length; i < s; i++) {
                this.components[i] = composition.getComponents(components[i]);
            }
        }

        @SuppressWarnings("unchecked")
        protected <T> T get(int entityId, int component) {
            return (T) components[component].get(entityId);
        }

        @SuppressWarnings("unchecked")
        protected void free(int component, Object instance) {
            if (components[component] instanceof PoolingComponents resultComponents) {
                resultComponents.free(instance);
            }
        }

        @Override
        public boolean isInterested(int entityId) {
            return composition.isInterested(entityId);
        }

        @Override
        public boolean matches(Spec spec) {
            return composition.matches(spec);
        }

        @Override
        public void inserted(IntConsumer inserted) {
            composition.inserted(inserted);
        }

        @Override
        public void removed(IntConsumer removed) {
            composition.removed(removed);
        }

        @Override
        public int getCount() {
            return composition.getCount();
        }

        @Override
        public boolean isEmpty() {
            return composition.isEmpty();
        }

        @Override
        public void process(IntConsumer process) {
            composition.process(process);
        }

        @Override
        public IntStream stream() {
            return composition.stream();
        }

        @Override
        public IntStream parallelStream() {
            return composition.parallelStream();
        }

    }

    private static class CompositionSpliterator implements Spliterator.OfInt {

        private final int[] data;
        private final int size;

        private int index;
        private int fence;

        private CompositionSpliterator(int[] data, int index, int fence, int size) {
            this.data = data;
            this.index = index;
            this.fence = fence;
            this.size = size;
        }

        @Override
        public long estimateSize() {
            return getFence() - index;
        }

        @Override
        public int characteristics() {
            return Spliterator.SIZED | Spliterator.DISTINCT | Spliterator.NONNULL | Spliterator.SUBSIZED;
        }

        @Override
        public OfInt trySplit() {
            int hi = getFence(), lo = index, mid = (lo + hi) >>> 1;

            return (lo >= mid)
                    ? null
                    : new CompositionSpliterator(data, lo, index = mid, size);
        }

        @Override
        public boolean tryAdvance(IntConsumer action) {
            int hi = getFence(), i = index;

            if (i < hi) {
                index = i + 1;
                int e = data[i];
                action.accept(e);

                return true;
            }

            return false;
        }

        private int getFence() {
            int hi;
            if ((hi = fence) < 0) {
                hi = fence = size;
            }

            return hi;
        }

    }

}
