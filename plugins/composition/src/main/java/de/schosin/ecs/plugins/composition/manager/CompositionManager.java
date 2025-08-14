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
import de.schosin.ecs.api.components.mappers.Components;
import de.schosin.ecs.api.components.types.ComponentSetType;
import de.schosin.ecs.api.components.types.ComponentType;
import de.schosin.ecs.api.components.types.ComponentType.RegularComponentType;
import de.schosin.ecs.api.components.types.DataProcessorType;
import de.schosin.ecs.api.data.DataProcessor;
import de.schosin.ecs.api.data.IterableComponentAccessor;
import de.schosin.ecs.codegen.EcsCodegen;
import de.schosin.ecs.engine.components.ComponentMapperManager;
import de.schosin.ecs.engine.components.ComponentMapperManager.ReclaimingComponents;
import de.schosin.ecs.engine.entities.EntityManager.ComponentsPredicate;
import de.schosin.ecs.engine.events.EventManager;
import de.schosin.ecs.plugins.composition.Composition;
import de.schosin.ecs.plugins.composition.Composition.Builder;
import de.schosin.ecs.plugins.composition.CompositionData;
import de.schosin.ecs.plugins.composition.CompositionData1;
import de.schosin.ecs.plugins.composition.CompositionPlugin;
import de.schosin.ecs.plugins.composition.Spec;
import de.schosin.ecs.plugins.data.DataTypePlugin;
import de.schosin.ecs.plugins.data.types.Data;
import de.schosin.ecs.plugins.data.types.DataType;
import de.schosin.ecs.storage.api.StorageEngine;
import de.schosin.ecs.storage.api.entities.Archetype;
import de.schosin.ecs.storage.api.entities.observer.EntitiesBeforeUpdateObserver;
import de.schosin.ecs.storage.api.entities.observer.EntitiesCreatedObserver;
import de.schosin.ecs.storage.api.entities.observer.EntitiesDeletedObserver;
import de.schosin.ecs.storage.api.entities.observer.EntitiesUpdatedObserver;
import de.schosin.ecs.storage.api.entities.observer.EntityBeforeUpdateObserver;
import de.schosin.ecs.storage.api.entities.observer.EntityCreatedObserver;
import de.schosin.ecs.storage.api.entities.observer.EntityUpdatedObserver;
import de.schosin.ecs.storage.api.events.ArchetypeAddedEvent;
import de.schosin.ecs.utils.collections.Bag;
import de.schosin.ecs.utils.collections.BitVector;
import de.schosin.ecs.utils.collections.ImmutableIntBag;
import de.schosin.ecs.utils.collections.IntBag;

@EcsCodegen
public class CompositionManager extends AbstractSpecManager implements CompositionPlugin,
        EntityCreatedObserver, EntitiesCreatedObserver, EntityBeforeUpdateObserver, EntityUpdatedObserver, EntitiesBeforeUpdateObserver, EntitiesUpdatedObserver, EntitiesDeletedObserver {

    private final StorageEngine storageEngine;

    private final ComponentMapperManager componentMapperManager;

    private final Bag<Archetype> archetypes = new Bag<>(Archetype.class, 64);
    private final Map<EngineSpec, CompositionImpl> compositions = new ConcurrentHashMap<>();

    private final Bag<Bag<CompositionImpl>> compositionsByArchetype = new Bag<>(Bag.class, 64);

    // DataTypePlugin used as argument to initialize the plugin
    public CompositionManager(World world, @SuppressWarnings("unused") DataTypePlugin dataTypePlugin) {
        super(world);

        world.addSingleton(this);

        this.storageEngine = world.getSingleton(StorageEngine.class);

        this.componentMapperManager = world.getSingleton(ComponentMapperManager.class);

        this.archetypes.addAll(storageEngine.getArchetypes());

        var eventManager = world.getSingleton(EventManager.class);
        eventManager.registerEventHandler(ArchetypeAddedEvent.class, this::handleArchetypeAdded);

        storageEngine.registerCreated(this);
        storageEngine.registerBatchCreated(this);
        storageEngine.registerBeforeUpdate(this);
        storageEngine.registerUpdated(this);
        storageEngine.registerBatchBeforeUpdate(this);
        storageEngine.registerBatchUpdated(this);
        storageEngine.registerDeleted(this);
    }

    private void handleArchetypeAdded(ArchetypeAddedEvent event) {
        var archetype = event.archetype();
        this.archetypes.add(archetype);

        for (var composition : compositions.values()) {
            composition.offer(archetype);
        }
    }

    @Override
    public Composition createComposition(Builder builder) {
        return create(builder, entityManager::getEntities);
    }

    @Override
    public <R, P extends DataProcessor<R>> CompositionData<P> createComposition(Composition.Builder builder, DataProcessorType<?, R, P> componentType) {
        var composition = (CompositionImpl) createComposition(builder);

        return composition.createCompositionData(componentType);
    }

    @Override
    public <R> CompositionData1<R> createComposition(Builder builder, ComponentType<?, R> component) {
        var composition = (CompositionImpl) createComposition(builder);

        return composition.createCompositionData(component);
    }

    @Override
    public <T extends Data, P extends DataProcessor<T>, D extends CompositionData<P>> D createComposition(Builder builder, DataType<?, T, P> dataType) {
        var composition = (CompositionImpl) createComposition(builder);

        return composition.createCompositionData(dataType);
    }

    public Composition create(Builder builder, Function<ComponentsPredicate, IntBag> entities) {
        var spec = buildSpec(builder);

        return this.compositions.computeIfAbsent(spec, ignore -> buildComposition(spec, entities));
    }

    private CompositionImpl buildComposition(EngineSpec spec, Function<ComponentsPredicate, IntBag> entities) {
        // Create composition
        var composition = new CompositionImpl(spec, entities);

        // Offer known archetypes to composition
        for (var archetype : archetypes) {
            composition.offer(archetype);
        }

        // Add composition to archetype lookup 
        synchronized (compositionsByArchetype) {
            var archetypes = storageEngine.getArchetypes();
            for (int i = 0, s = archetypes.getSize(); i < s; i++) {
                var archetype = archetypes.get(i);
                if (!composition.isInterested(archetype)) {
                    continue;
                }

                var archetypeCompositions = getCompositions(archetype);
                archetypeCompositions.add(composition);
            }
        }

        return composition;
    }

    @Override
    public final void handleEntityCreated(Archetype archetype, int entityId) {
        var archetypeCompositions = getCompositions(archetype);

        var data = archetypeCompositions.getData();
        for (int i = 0, s = archetypeCompositions.getSize(); i < s; i++) {
            var composition = data[i];
            if (composition.isInterested(archetype)) {
                composition.inserted(entityId);
            }
        }
    }

    @Override
    public final void handleEntitiesCreated(Archetype archetype, ImmutableIntBag entities) {
        var archetypeCompositions = getCompositions(archetype);

        var data = archetypeCompositions.getData();
        for (int i = 0, s = archetypeCompositions.getSize(); i < s; i++) {
            var composition = data[i];
            if (composition.isInterested(archetype)) {
                for (int e = 0, es = entities.getSize(); e < es; e++) {
                    composition.inserted(entities.get(e));
                }
            }
        }
    }

    @Override
    public void handleEntityBeforeUpdate(Archetype archetype, Archetype newArchetype, int entityId) {
        // Remove from previous composition if no longer interested
        var previousCompositions = getCompositions(archetype);

        var previousData = previousCompositions.getData();
        for (int i = 0, s = previousCompositions.getSize(); i < s; i++) {
            var composition = previousData[i];

            var afterInterested = composition.isInterested(newArchetype);
            if (!afterInterested) {
                composition.removed(entityId);
            }
        }
    }

    @Override
    public void handleEntityUpdated(Archetype archetype, Archetype previousArchetype, int entityId) {
        // Add to new composition if not yet contained
        var newCompositions = getCompositions(archetype);

        var dataData = newCompositions.getData();
        for (int i = 0, s = newCompositions.getSize(); i < s; i++) {
            var composition = dataData[i];

            var beforeInterested = composition.isInterested(previousArchetype);
            if (!beforeInterested) {
                composition.inserted(entityId);
            }
        }
    }

    @Override
    public final void handleEntitiesBeforeUpdate(Archetype archetype, Archetype newArchetype, ImmutableIntBag entities) {
        // Remove from previous composition if no longer interested
        var previousCompositions = getCompositions(archetype);

        var previousData = previousCompositions.getData();
        for (int i = 0, s = previousCompositions.getSize(); i < s; i++) {
            var composition = previousData[i];

            var afterInterested = composition.isInterested(newArchetype);
            if (!afterInterested) {
                for (int e = 0, es = entities.getSize(); e < es; e++) {
                    composition.removed(entities.get(e));
                }
            }
        }
    }

    @Override
    public final void handleEntitiesUpdated(Archetype archetype, Archetype previousArchetype, ImmutableIntBag entities) {
        // Add to new composition if not yet contained
        var newCompositions = getCompositions(archetype);

        var dataData = newCompositions.getData();
        for (int i = 0, s = newCompositions.getSize(); i < s; i++) {
            var composition = dataData[i];

            var beforeInterested = composition.isInterested(previousArchetype);
            if (!beforeInterested) {
                for (int e = 0, es = entities.getSize(); e < es; e++) {
                    composition.inserted(entities.get(e));
                }
            }
        }
    }

    @Override
    public final void handleEntitiesDeleted(Archetype archetype, ImmutableIntBag entities) {
        var archetypeCompositions = getCompositions(archetype);

        var data = archetypeCompositions.getData();
        for (int i = 0, s = archetypeCompositions.getSize(); i < s; i++) {
            var composition = data[i];

            for (int e = 0, es = entities.getSize(); e < es; e++) {
                composition.removed(entities.get(e));
            }
        }
    }

    private Bag<CompositionImpl> getCompositions(Archetype archetype) {
        synchronized (compositionsByArchetype) {
            var result = compositionsByArchetype.getSafe(archetype.getId());
            if (result == null) {
                result = new Bag<>(CompositionImpl.class);
                compositionsByArchetype.set(archetype.getId(), result);

                // Add interested compositions
                for (var composition : this.compositions.values()) {
                    if (composition.isInterested(archetype)) {
                        result.add(composition);
                    }
                }
            }

            return result;
        }
    }

    private final class CompositionImpl implements Composition {

        private final EngineSpec spec;
        private final IntBag archetypeCache;

        private final BitVector lookup;

        private final Bag<Archetype> archetypes = new Bag<>(Archetype.class, 8);

        private int count;

        private IntConsumer inserted;
        private Bag<IntConsumer> moreInserted;

        private IntConsumer removed;
        private Bag<IntConsumer> moreRemoved;

        @SuppressWarnings("rawtypes")
        private final Map<ComponentType<?, ?>, AbstractComposition> compositionData = new ConcurrentHashMap<>();

        private CompositionImpl(EngineSpec spec, Function<ComponentsPredicate, IntBag> supplier) {
            this.spec = spec;
            this.archetypeCache = new IntBag(64);

            var entities = supplier.apply(this::isInterested);

            // Determine largest entityId to avoid garbage by BitVector growing
            var largestEntityId = 0;
            for (int i = 0, s = entities.getSize(); i < s; i++) {
                var entityId = entities.get(i);
                if (entityId > largestEntityId) {
                    largestEntityId = entityId;
                }
            }

            this.lookup = new BitVector(largestEntityId);
            for (int i = 0, s = entities.getSize(); i < s; i++) {
                this.lookup.set(entities.get(i));
            }

            this.count = entities.getSize();
        }

        public void offer(Archetype archetype) {
            if (spec.isInterested(archetype)) {
                archetypes.add(archetype);
                archetypeCache.set(archetype.getId(), 1);

                for (var data : compositionData.values()) {
                    data.addArchetype(archetype);
                }
            }
        }

        @SuppressWarnings("unchecked")
        private <R, P extends DataProcessor<R>> CompositionData<P> createCompositionData(DataProcessorType<?, R, P> componentType) {
            if (componentType instanceof DataType<?, ?, ?> dataType) {
                return (CompositionData<P>) createCompositionData(dataType);
            }

            var result = (CompositionData<P>) compositionData.get(componentType);
            if (result != null) {
                return result;
            }

            synchronized (compositionData) {
                result = (CompositionData<P>) compositionData.get(componentType);
                if (result != null) {
                    return result;
                }

                var compositionData = new CompositionDataImpl<>(this, componentType);
                initializeCompositionData(compositionData);

                this.compositionData.put(componentType, compositionData);

                return compositionData;
            }
        }

        @SuppressWarnings("unchecked")
        private <R> CompositionData1<R> createCompositionData(ComponentType<?, R> component) {
            if (component instanceof DataType<?, ?, ?>) {
                throw new IllegalArgumentException("Cannot pass DataType to createComposition(Builder, ComponentType). Use createComposition(Builder, DataType) instead.");
            }
            if (component instanceof ComponentSetType<?, ?>) {
                throw new IllegalArgumentException("Cannot pass ComponentSetType to createComposition(Builder, ComponentType). Use createComposition(Builder, ComponentSetType) instead.");
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

                var compositionData = switch (component) {
                    case RegularComponentType<?, R> regular -> new RegularComposition1<>(this, regular, storageEngine.getComponent(regular).id());
                    default -> new Composition1<>(this, component);
                };

                initializeCompositionData(compositionData);

                this.compositionData.put(component, compositionData);

                return compositionData;
            }
        }

        @SuppressWarnings("unchecked")
        private <R extends Data, P extends DataProcessor<R>, D extends CompositionData<P>> D createCompositionData(DataType<?, R, P> dataType) {
            var result = (D) compositionData.get(dataType);
            if (result != null) {
                return result;
            }

            synchronized (compositionData) {
                result = (D) compositionData.get(dataType);
                if (result != null) {
                    return result;
                }

                var compositionData = CompositionManagerHelper.createCompositionData(this, dataType);
                initializeCompositionData(compositionData);

                this.compositionData.put(dataType, compositionData);

                return (D) compositionData;
            }
        }

        private void initializeCompositionData(AbstractComposition<?, ?> compositionData) {
            synchronized (archetypes) {
                for (int i = 0, s = archetypes.getSize(); i < s; i++) {
                    compositionData.addArchetype(archetypes.get(i));
                }
            }
        }

        protected <R> Components<?, R> getComponents(ComponentType<?, R> type) {
            return componentMapperManager.getComponents(type);
        }

        private boolean containsEntity(int entityId) {
            return this.lookup.get(entityId);
        }

        private void inserted(int entityId) {
            this.lookup.set(entityId);
            this.count++;

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
            // Remove entity
            this.lookup.clear(entityId);
            this.count--;

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

        public boolean isInterested(@NonNull Archetype archetype) {
            // Check cached value
            var cached = archetypeCache.getSafe(archetype.getId());
            if (cached != 0) {
                return cached == 1;
            }

            // Test spec and cache result
            var result = spec.isInterested(archetype);
            archetypeCache.set(archetype.getId(), result ? 1 : 2);

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
            return this.count;
        }

        @Override
        public boolean isEmpty() {
            return this.count == 0;
        }

        @Override
        public void process(@NonNull IntConsumer process) {
            for (int i = 0, s = archetypes.getSize(); i < s; i++) {
                var archetype = archetypes.get(i);
                var entities = archetype.getEntities();

                for (int e = 0, es = entities.getSize(); e < es; e++) {
                    process.accept(entities.get(e));
                }
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
            return new CompositionSpliterator(archetypes);
        }

        @Override
        public String toString() {
            return "CompositionImpl(spec = " + this.spec + ", count = " + this.count + ")";
        }

    }

    private static final class RegularComposition1<R> extends AbstractComposition<R, DataProcessor<R>> implements CompositionData1<R> {

        private final int componentId;

        private final Bag<Archetype> archetypes = new Bag<>(Archetype.class, 4);
        private final Bag<Archetype> pendingArchetypes = new Bag<>(Archetype.class, 4);
        private final IntBag componentIndices = new IntBag(4);

        protected RegularComposition1(Composition composition, RegularComponentType<?, R> componentType, int componentId) {
            super(composition, componentType);

            this.componentId = componentId;
        }

        @Override
        protected final void addArchetype(Archetype archetype) {
            var index = archetype.getComponentIndex(componentId);
            if (index != -1) {
                this.componentIndices.add(index);
                this.archetypes.add(archetype);
            } else {
                this.pendingArchetypes.add(archetype);
            }
        }

        @Override
        public final void process(DataProcessor<R> processor) {
            for (int i = 0, s = archetypes.getSize(); i < s; i++) {
                var accessor = archetypes.get(i).getAccessor();
                var index = componentIndices.get(i);

                while (accessor.hasNext()) {
                    processor.process(accessor.next(), accessor.getComponentByIndex(index));
                }

                accessor.free();
            }

            var componentId = this.componentId;
            for (int i = 0, s = pendingArchetypes.getSize(); i < s; i++) {
                var accessor = pendingArchetypes.get(i).getAccessor();

                while (accessor.hasNext()) {
                    processor.process(accessor.next(), accessor.getPendingComponent(componentId));
                }

                accessor.free();
            }
        }

    }

    private static final class Composition1<R> extends AbstractAccessorComposition<R, DataProcessor<R>> implements CompositionData1<R> {

        private final Components<?, R> mapper;

        protected Composition1(Composition composition, ComponentType<?, R> componentType) {
            super(composition, componentType);

            this.mapper = this.composition.getComponents(componentType);
        }

        @Override
        public final void process(DataProcessor<R> processor) {
            for (int i = 0, s = archetypes.getSize(); i < s; i++) {
                var accessor = archetypes.get(i).getAccessor();
                var components = mapper.getComponentAccessor(accessor);

                while (accessor.hasNext()) {
                    processor.process(accessor.next(), components.getComponent(accessor));
                }

                components.free();
                accessor.free();
            }
        }

    }

    static sealed class CompositionDataImpl<R, P extends DataProcessor<R>> extends AbstractAccessorComposition<R, P> implements CompositionData<P>
            permits CompositionManagerHelper.AbstractCompositionDataN {

        private final Components<?, R> mapper;

        protected CompositionDataImpl(Composition composition, DataProcessorType<?, R, P> componentType) {
            super(composition, componentType);

            this.mapper = this.composition.getComponents(componentType);
        }

        @Override
        @SuppressWarnings("unchecked")
        public final void process(P processor) {
            for (int i = 0, s = archetypes.getSize(); i < s; i++) {
                var accessor = archetypes.get(i).getAccessor();

                var componentAccessor = (IterableComponentAccessor<R, P>) mapper.getComponentAccessor(accessor);
                componentAccessor.process(accessor, processor);
                componentAccessor.free();

                accessor.free();
            }
        }

    }

    private abstract static class AbstractAccessorComposition<R, P extends DataProcessor<R>> extends AbstractComposition<R, P> {

        protected final Bag<Archetype> archetypes = new Bag<>(Archetype.class, 4);

        protected AbstractAccessorComposition(Composition composition, ComponentType<?, R> type) {
            super(composition, type);
        }

        @Override
        protected final void addArchetype(Archetype archetype) {
            this.archetypes.add(archetype);
        }

    }

    private abstract static class AbstractComposition<R, P extends DataProcessor<R>> implements CompositionData<P>, Spec {

        protected final CompositionImpl composition;
        private final Components<?, R> mapper;
        private final ReclaimingComponents reclaiming;

        protected AbstractComposition(Composition composition, ComponentType<?, R> type) {
            this.composition = (CompositionImpl) composition;
            this.mapper = this.composition.getComponents(type);
            this.reclaiming = mapper instanceof ReclaimingComponents reclaiming ? reclaiming : null;
        }

        protected abstract void addArchetype(Archetype archetype);

        @Override
        public final boolean isInterested(int entityId) {
            return composition.isInterested(entityId);
        }

        @Override
        public final boolean matches(Spec spec) {
            return composition.matches(spec);
        }

        @Override
        public final void inserted(IntConsumer inserted) {
            composition.inserted(inserted);
        }

        @Override
        public final void inserted(P processor) {
            composition.inserted(entityId -> process(entityId, processor));
        }

        @Override
        public final void removed(IntConsumer removed) {
            composition.removed(removed);
        }

        @Override
        public final void removed(P processor) {
            composition.removed(entityId -> process(entityId, processor));
        }

        @Override
        public final int getCount() {
            return composition.getCount();
        }

        @Override
        public final boolean isEmpty() {
            return composition.isEmpty();
        }

        @Override
        public final void process(IntConsumer process) {
            composition.process(process);
        }

        @Override
        public final void process(int entityId, P processor) {
            var data = mapper.get(entityId);
            processor.process(entityId, data);

            if (reclaiming != null) {
                reclaiming.reclaim();
            }
        }

        @Override
        public final IntStream stream() {
            return composition.stream();
        }

        @Override
        public final IntStream parallelStream() {
            return composition.parallelStream();
        }

    }

    private static final class CompositionSpliterator implements Spliterator.OfInt {

        private final Bag<ImmutableIntBag> bags;
        private final int size;

        private int currentIndex = 0;
        private int currentEntityIndex = 0;

        public CompositionSpliterator(Bag<Archetype> archetypes) {
            this.bags = new Bag<>(ImmutableIntBag.class, archetypes.getSize());
            this.size = archetypes.getSize();

            for (int i = 0, s = archetypes.getSize(); i < s; i++) {
                this.bags.add(archetypes.get(i).getEntities());
            }
        }

        public CompositionSpliterator(Bag<ImmutableIntBag> bags, int size) {
            this.bags = bags;
            this.size = size;
        }

        @Override
        public long estimateSize() {
            if (this.bags.isEmpty()) {
                return 0L;
            }

            var count = 0L;
            for (int i = currentIndex; i < size; i++) {
                count += this.bags.get(i).getSize() - currentEntityIndex;
            }

            return count;
        }

        @Override
        public int characteristics() {
            return Spliterator.DISTINCT | Spliterator.ORDERED | Spliterator.SIZED | Spliterator.SUBSIZED;
        }

        @Override
        public OfInt trySplit() {
            if (size - currentIndex <= 2) {
                return null;
            }

            var midPoint = currentIndex + size / 2;
            var leftBag = new Bag<>(ImmutableIntBag.class, midPoint - currentIndex);

            for (int i = currentIndex; i < midPoint; i++) {
                leftBag.add(bags.get(i));
            }

            currentIndex = midPoint;

            return new CompositionSpliterator(leftBag, leftBag.getSize());
        }

        @Override
        public boolean tryAdvance(IntConsumer action) {
            while (currentIndex < size) {
                var entities = bags.get(currentIndex);

                if (currentEntityIndex >= entities.getSize()) {
                    currentIndex++;
                    currentEntityIndex = 0;

                    continue;
                }

                var entityId = entities.get(currentEntityIndex++);
                action.accept(entityId);

                return true;
            }

            return false;
        }

    }

}
