package de.schosin.ecs.plugins.composition.manager;

import java.util.Map;
import java.util.Spliterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

import org.jspecify.annotations.NonNull;

import de.schosin.ecs.api.World;
import de.schosin.ecs.api.components.ComponentSet;
import de.schosin.ecs.api.components.mappers.Components;
import de.schosin.ecs.api.components.types.ComponentSetType;
import de.schosin.ecs.api.components.types.ComponentType;
import de.schosin.ecs.api.data.DataProcessor;
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
import de.schosin.ecs.plugins.composition.Composition;
import de.schosin.ecs.plugins.composition.Composition.Builder;
import de.schosin.ecs.plugins.composition.CompositionData;
import de.schosin.ecs.plugins.composition.CompositionData1;
import de.schosin.ecs.plugins.composition.CompositionPlugin;
import de.schosin.ecs.plugins.composition.CompositionSet;
import de.schosin.ecs.plugins.composition.Spec;
import de.schosin.ecs.plugins.data.DataTypePlugin;
import de.schosin.ecs.plugins.data.types.BaseDataType.Data;
import de.schosin.ecs.plugins.data.types.DataType;
import de.schosin.ecs.utils.collections.Bag;
import de.schosin.ecs.utils.collections.ImmutableIntBag;
import de.schosin.ecs.utils.collections.IntBag;

@EcsCodegen
public class CompositionManager extends AbstractSpecManager implements CompositionPlugin {

    private final BagManager bagManager;
    private final ComponentMaskManager componentMaskManager;
    private final ComponentMapperManager componentMapperManager;

    private final Map<EngineSpec, CompositionImpl> compositions = new ConcurrentHashMap<>();

    private final Bag<Bag<CompositionImpl>> compositionsByMask = new Bag<>(Bag.class, 64);
    private final Bag<ComponentMask> fill = new Bag<>(ComponentMask.class, 64);

    public CompositionManager(World world, DataTypePlugin dataTypePlugin) {
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

    @Override
    public <R> CompositionData1<R> createComposition(Builder builder, ComponentType<?, R> component) {
        var composition = (CompositionImpl) createComposition(builder);

        return composition.createCompositionData(component);
    }

    @Override
    public <T extends Data, P extends DataProcessor<T>, D extends CompositionData<P>> D createComposition(Builder builder, DataType<?, ?, T, P> dataType) {
        var composition = (CompositionImpl) createComposition(builder);

        return composition.createCompositionData(dataType);
    }

    @Override
    public <T extends ComponentSet<P>, P extends DataProcessor<T>> CompositionSet<P> createComposition(Builder builder, ComponentSetType<T, P> componentSetType) {
        var composition = (CompositionImpl) createComposition(builder);

        return composition.createCompositionData(componentSetType);
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

    private void handleInserted(ImmutableIntBag entityIds, ComponentMask componentMask) {
        var maskCompositions = getCompositions(componentMask);

        var data = maskCompositions.getData();
        for (int i = 0, s = maskCompositions.getSize(); i < s; i++) {
            var composition = data[i];
            if (composition.isInterested(componentMask)) {
                for (int e = 0, es = entityIds.getSize(); e < es; e++) {
                    var entityId = entityIds.get(e);

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

    private final class CompositionImpl implements Composition {

        private final EngineSpec spec;

        private final IntBag entities;
        private final IntBag lookup;

        private final IntBag maskCache;

        private IntConsumer inserted;
        private Bag<IntConsumer> moreInserted;

        private IntConsumer removed;
        private Bag<IntConsumer> moreRemoved;

        private final Map<ComponentType<?, ?>, CompositionData<?>> compositionData = new ConcurrentHashMap<>();

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

        @SuppressWarnings("unchecked")
        private <R> CompositionData1<R> createCompositionData(ComponentType<?, R> component) {
            if (component instanceof DataType<?, ?, ?, ?>) {
                throw new IllegalArgumentException("Cannot pass DataType to createComposition(Builder, ComponentType). Use createComposition(Builder, DataType) instead.");
            }

            var result = (CompositionData1<R>) compositionData.get(component);
            if (result != null) {
                return result;
            }

            synchronized (compositionData) {
                result = (CompositionData1<R>) compositionData.get(component);
                if (result != null) {
                    return result;
                }

                var compositionData = new Composition1<>(this, component);
                this.compositionData.put(component, compositionData);

                return compositionData;
            }
        }

        @SuppressWarnings("unchecked")
        private <T extends Data, P extends DataProcessor<T>, D extends CompositionData<P>> D createCompositionData(DataType<?, ?, T, P> dataType) {
            var result = (D) compositionData.get(dataType);
            if (result != null) {
                return result;
            }

            synchronized (compositionData) {
                result = (D) compositionData.get(dataType);
                if (result != null) {
                    return result;
                }

                var compositionData = (D) CompositionManagerHelper.createCompositionData(this, dataType);
                this.compositionData.put(dataType, compositionData);

                return compositionData;
            }
        }

        @SuppressWarnings("unchecked")
        private <T extends ComponentSet<P>, P extends DataProcessor<T>> CompositionSet<P> createCompositionData(ComponentSetType<T, P> componentSetType) {
            var result = (CompositionSet<P>) compositionData.get(componentSetType);
            if (result != null) {
                return result;
            }

            synchronized (compositionData) {
                result = (CompositionSet<P>) compositionData.get(componentSetType);
                if (result != null) {
                    return result;
                }

                var compositionSet = new ComponentSetComposition<>(this, componentSetType);
                this.compositionData.put(componentSetType, compositionSet);

                return compositionSet;
            }
        }

        protected <R> Components<?, R> getComponents(ComponentType<?, R> type) {
            return componentMapperManager.getComponents(type);
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
            var result = spec.isInterested(componentMask);
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

            if (spec instanceof AbstractComposition composition) {
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

    static class Composition1<R> extends AbstractComposition<R, DataProcessor<R>> implements CompositionData1<R> {

        protected Composition1(Composition composition, ComponentType<?, R> type) {
            super(composition, type);
        }

    }

    static class ComponentSetComposition<T extends ComponentSet<P>, P extends DataProcessor<T>> extends AbstractComposition<T, P> implements CompositionSet<P> {

        protected ComponentSetComposition(Composition composition, ComponentSetType<T, P> type) {
            super(composition, type);
        }

    }

    abstract static class AbstractCompositionN<R extends Data, P extends DataProcessor<R>> extends AbstractComposition<R, P> {

        protected AbstractCompositionN(Composition composition, DataType<?, ?, R, P> dataType) {
            super(composition, dataType);
        }

    }

    abstract static class AbstractComposition<R, P extends DataProcessor<R>> implements CompositionData<P>, Spec {

        protected final CompositionImpl composition;

        private final Components<?, R> mapper;

        protected AbstractComposition(Composition composition, ComponentType<?, R> type) {
            this.composition = (CompositionImpl) composition;
            this.mapper = this.composition.getComponents(type);
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
        public void inserted(P processor) {
            composition.inserted(entityId -> process(entityId, processor));
        }

        @Override
        public void removed(IntConsumer removed) {
            composition.removed(removed);
        }

        @Override
        public void removed(P processor) {
            composition.removed(entityId -> process(entityId, processor));
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
        public void process(P processor) {
            composition.process(entityId -> process(entityId, processor));
        }

        @Override
        @SuppressWarnings("unchecked")
        public void process(int entityId, P processor) {
            var data = mapper.get(entityId);
            processor.process(entityId, data);

            if (data != null && mapper instanceof PoolingComponents pooling) {
                pooling.free(data);
            }
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
