package de.schosin.ecs.engine.compositions;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Spliterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

import org.jspecify.annotations.NonNull;

import de.schosin.ecs.api.archetype.Transmuter.Builder.AbstractBuilder;
import de.schosin.ecs.api.components.Composition;
import de.schosin.ecs.api.components.Composition.Builder;
import de.schosin.ecs.engine.BagManager;
import de.schosin.ecs.engine.components.ComponentData;
import de.schosin.ecs.engine.components.ComponentManager;
import de.schosin.ecs.engine.components.ComponentMask;
import de.schosin.ecs.engine.utils.collections.Bag;
import de.schosin.ecs.engine.utils.collections.BitVector;
import de.schosin.ecs.engine.utils.collections.IntBag;
import de.schosin.ecs.engine.utils.collections.Pool;

/**
 * This class manages the {@link Composition compositions} that are used to process entities.
 * 
 * <p>
 * Given a {@link AbstractBuilder composition builder} a {@link Spec} is created. This spec is used
 * both as a lookup key and to test whether the composition is interested in an 
 * entity (via {@link ComponentMask} if its mask has not been encountered before.
 * </p>
 * 
 * <p>
 * When entities are {@link #inserted(ComponentMask, int) inserted}, {@link #updated(int, ComponentMask) updated}
 * or {@link #removed(int)} the interested compositions will be called depending on the spec.
 * If a composition did not contain an entity and is interested in an inserted or updated entity, {@link Composition#inserted(IntConsumer)}
 * callbacks will be processed. Likewise when a composition contained an entity is removed or the composition is no longer interested,
 * {@link Composition#removed(IntConsumer)} callbacks will be processed.
 * </p>
 */
public class CompositionManager {

    private final BagManager bagManager;
    private final ComponentManager componentManager;

    private final Map<Spec, CompositionImpl> compositions = new ConcurrentHashMap<>();

    private final Bag<CompositionImpl> bag = new Bag<>(CompositionImpl.class, 64);
    private final Pool<BitVector> bitVectorPool = Pool.unbounded(BitVector.class, BitVector::new, BitVector::clear);

    public CompositionManager(BagManager bagManager, ComponentManager componentManager) {
        this.bagManager = bagManager;
        this.componentManager = componentManager;
    }

    public <T1> Composition.Of1<T1> create(Builder builder, Function<Spec, IntBag> entities, Class<T1> component1) {
        var composition = (CompositionImpl) create(builder, entities);

        return bitVectorPool.withInstance(vector -> {
            componentManager.fillVector(vector, component1);

            return composition.getRetrieveComposition(vector, () -> new Composition1<>(create(builder, entities), component1));
        });
    }

    public <T1, T2> Composition.Of2<T1, T2> create(Builder builder, Function<Spec, IntBag> entities, Class<T1> component1, Class<T2> component2) {
        var composition = (CompositionImpl) create(builder, entities);

        return bitVectorPool.withInstance(vector -> {
            componentManager.fillVector(vector, component1, component2);

            return composition.getRetrieveComposition(vector, () -> new Composition2<>(create(builder, entities), component1, component2));
        });
    }

    public <T1, T2, T3> Composition.Of3<T1, T2, T3> create(Builder builder, Function<Spec, IntBag> entities, Class<T1> component1, Class<T2> component2, Class<T3> component3) {
        var composition = (CompositionImpl) create(builder, entities);

        return bitVectorPool.withInstance(vector -> {
            componentManager.fillVector(vector, component1, component2, component3);

            return composition.getRetrieveComposition(vector, () -> new Composition3<>(create(builder, entities), component1, component2, component3));
        });
    }

    public <T1, T2, T3, T4> Composition.Of4<T1, T2, T3, T4> create(Builder builder, Function<Spec, IntBag> entities, Class<T1> component1, Class<T2> component2, Class<T3> component3,
            Class<T4> component4) {

        var composition = (CompositionImpl) create(builder, entities);

        return bitVectorPool.withInstance(vector -> {
            componentManager.fillVector(vector, component1, component2, component3, component4);

            return composition.getRetrieveComposition(vector, () -> new Composition4<>(create(builder, entities), component1, component2, component3, component4));
        });
    }

    public <T1, T2, T3, T4, T5> Composition.Of5<T1, T2, T3, T4, T5> create(Builder builder, Function<Spec, IntBag> entities, Class<T1> component1, Class<T2> component2, Class<T3> component3,
            Class<T4> component4, Class<T5> component5) {

        var composition = (CompositionImpl) create(builder, entities);

        return bitVectorPool.withInstance(vector -> {
            componentManager.fillVector(vector, component1, component2, component3, component4, component5);

            return composition.getRetrieveComposition(vector, () -> new Composition5<>(create(builder, entities), component1, component2, component3, component4, component5));
        });
    }

    public <T1, T2, T3, T4, T5, T6> Composition.Of6<T1, T2, T3, T4, T5, T6> create(Builder builder, Function<Spec, IntBag> entities, Class<T1> component1, Class<T2> component2, Class<T3> component3,
            Class<T4> component4, Class<T5> component5, Class<T6> component6) {

        var composition = (CompositionImpl) create(builder, entities);

        return bitVectorPool.withInstance(vector -> {
            componentManager.fillVector(vector, component1, component2, component3, component4, component5, component6);

            return composition.getRetrieveComposition(vector, () -> new Composition6<>(create(builder, entities), component1, component2, component3, component4, component5, component6));
        });
    }

    public <T1, T2, T3, T4, T5, T6, T7> Composition.Of7<T1, T2, T3, T4, T5, T6, T7> create(Builder builder, Function<Spec, IntBag> entities, Class<T1> component1, Class<T2> component2,
            Class<T3> component3, Class<T4> component4, Class<T5> component5, Class<T6> component6, Class<T7> component7) {

        var composition = (CompositionImpl) create(builder, entities);

        return bitVectorPool.withInstance(vector -> {
            componentManager.fillVector(vector, component1, component2, component3, component4, component5, component6, component7);

            return composition.getRetrieveComposition(vector, () -> new Composition7<>(create(builder, entities), component1, component2, component3, component4, component5, component6, component7));
        });
    }

    public <T1, T2, T3, T4, T5, T6, T7, T8> Composition.Of8<T1, T2, T3, T4, T5, T6, T7, T8> create(Builder builder, Function<Spec, IntBag> entities, Class<T1> component1, Class<T2> component2,
            Class<T3> component3, Class<T4> component4, Class<T5> component5, Class<T6> component6, Class<T7> component7, Class<T8> component8) {

        var composition = (CompositionImpl) create(builder, entities);

        return bitVectorPool.withInstance(vector -> {
            componentManager.fillVector(vector, component1, component2, component3, component4, component5, component6, component7, component8);

            return composition.getRetrieveComposition(vector, () -> new Composition8<>(composition, component1, component2, component3, component4, component5, component6, component7, component8));
        });
    }

    public Composition create(Composition.Builder builder, Function<Spec, IntBag> entities) {
        var spec = buildSpec(builder);

        return this.compositions.computeIfAbsent(spec, ignore -> buildComposition(spec, entities));
    }

    private CompositionImpl buildComposition(Spec spec, Function<Spec, IntBag> entities) {
        var composition = new CompositionImpl(spec, entities.apply(spec), bagManager.createEntityIntBag());

        synchronized (this.bag) {
            this.bag.add(composition);
        }

        return composition;
    }

    private Spec buildSpec(Composition.Builder builder) {
        BitVector allVector = null;
        if (!builder.getAll().isEmpty()) {
            allVector = new BitVector();
            for (var clazz : builder.getAll()) {
                allVector.set(componentManager.getData(clazz).id());
            }
        }

        BitVector oneVector = null;
        if (!builder.getOne().isEmpty()) {
            oneVector = new BitVector();
            for (var clazz : builder.getOne()) {
                oneVector.set(componentManager.getData(clazz).id());
            }
        }

        BitVector noneVector = null;
        if (!builder.getNone().isEmpty()) {
            noneVector = new BitVector();
            for (var clazz : builder.getNone()) {
                noneVector.set(componentManager.getData(clazz).id());
            }
        }

        return Spec.create(allVector, oneVector, noneVector);
    }

    public void inserted(@NonNull ComponentMask componentMask, int entityId) {
        var data = this.bag.getData();
        for (int i = 0, s = this.bag.getSize(); i < s; i++) {
            var composition = data[i];
            if (composition.isInterested(componentMask)) {
                composition.inserted(entityId);
            }
        }
    }

    public void inserted(@NonNull ComponentMask componentMask, int... entitiyIds) {
        var data = this.bag.getData();
        for (int i = 0, s = this.bag.getSize(); i < s; i++) {
            var composition = data[i];
            if (composition.isInterested(componentMask)) {
                for (var entityId : entitiyIds) {
                    composition.inserted(entityId);
                }
            }
        }
    }

    public void updated(int entityId, @NonNull ComponentMask updatedComponentMask) {
        var data = this.bag.getData();
        for (int i = 0, s = this.bag.getSize(); i < s; i++) {
            var composition = data[i];

            var beforeInterested = composition.containsEntity(entityId);
            var afterInterested = composition.isInterested(updatedComponentMask);

            if (beforeInterested && !afterInterested) {
                composition.removed(entityId);
            } else if (!beforeInterested && afterInterested) {
                composition.inserted(entityId);
            }
        }
    }

    public void removed(int entityId) {
        var data = this.bag.getData();
        for (int i = 0, s = this.bag.getSize(); i < s; i++) {
            var composition = data[i];

            if (composition.containsEntity(entityId)) {
                composition.removed(entityId);
            }
        }
    }

    private final class Composition1<T1> extends AbstractRetrieveComposition implements Composition.Of1<T1> {

        private Composition1(Composition composition, Class<T1> component1) {
            super(composition, component1);
        }

        @Override
        public void process(int entityId, Composition.Of1.@NonNull Consumer<T1> callback) {
            callback.consume(entityId, get(entityId, 0));
        }

        @Override
        public void process(Composition.Of1.@NonNull Consumer<T1> callback) {
            super.process(entityId -> process(entityId, callback));
        }

        @Override
        public void inserted(Composition.Of1.@NonNull Consumer<T1> callback) {
            super.inserted(entityId -> process(entityId, callback));
        }

        @Override
        public void removed(Composition.Of1.@NonNull Consumer<T1> callback) {
            super.removed(entityId -> process(entityId, callback));
        }

    }

    private final class Composition2<T1, T2> extends AbstractRetrieveComposition implements Composition.Of2<T1, T2> {

        private Composition2(Composition composition, Class<T1> component1, Class<T2> component2) {
            super(composition, component1, component2);
        }

        @Override
        public void process(int entityId, Composition.Of2.@NonNull Consumer<T1, T2> callback) {
            callback.consume(entityId, get(entityId, 0), get(entityId, 1));
        }

        @Override
        public void process(Composition.Of2.@NonNull Consumer<T1, T2> callback) {
            super.process(entityId -> process(entityId, callback));
        }

        @Override
        public void inserted(Composition.Of2.@NonNull Consumer<T1, T2> callback) {
            super.inserted(entityId -> process(entityId, callback));
        }

        @Override
        public void removed(Composition.Of2.@NonNull Consumer<T1, T2> callback) {
            super.removed(entityId -> process(entityId, callback));
        }

    }

    private final class Composition3<T1, T2, T3> extends AbstractRetrieveComposition implements Composition.Of3<T1, T2, T3> {

        private Composition3(Composition composition, Class<T1> component1, Class<T2> component2, Class<T3> component3) {
            super(composition, component1, component2, component3);
        }

        @Override
        public void process(int entityId, Composition.Of3.@NonNull Consumer<T1, T2, T3> callback) {
            callback.consume(entityId, get(entityId, 0), get(entityId, 1), get(entityId, 2));
        }

        @Override
        public void process(Composition.Of3.@NonNull Consumer<T1, T2, T3> callback) {
            super.process(entityId -> process(entityId, callback));
        }

        @Override
        public void inserted(Composition.Of3.@NonNull Consumer<T1, T2, T3> callback) {
            super.inserted(entityId -> process(entityId, callback));
        }

        @Override
        public void removed(Composition.Of3.@NonNull Consumer<T1, T2, T3> callback) {
            super.removed(entityId -> process(entityId, callback));
        }

    }

    private final class Composition4<T1, T2, T3, T4> extends AbstractRetrieveComposition implements Composition.Of4<T1, T2, T3, T4> {

        private Composition4(Composition composition, Class<T1> component1, Class<T2> component2, Class<T3> component3, Class<T4> component4) {
            super(composition, component1, component2, component3, component4);
        }

        @Override
        public void process(int entityId, Composition.Of4.@NonNull Consumer<T1, T2, T3, T4> callback) {
            callback.consume(entityId, get(entityId, 0), get(entityId, 1), get(entityId, 2), get(entityId, 3));
        }

        @Override
        public void process(Composition.Of4.@NonNull Consumer<T1, T2, T3, T4> callback) {
            super.process(entityId -> process(entityId, callback));
        }

        @Override
        public void inserted(Composition.Of4.@NonNull Consumer<T1, T2, T3, T4> callback) {
            super.inserted(entityId -> process(entityId, callback));
        }

        @Override
        public void removed(Composition.Of4.@NonNull Consumer<T1, T2, T3, T4> callback) {
            super.removed(entityId -> process(entityId, callback));
        }

    }

    private final class Composition5<T1, T2, T3, T4, T5> extends AbstractRetrieveComposition implements Composition.Of5<T1, T2, T3, T4, T5> {

        private Composition5(Composition composition, Class<T1> component1, Class<T2> component2, Class<T3> component3, Class<T4> component4, Class<T5> component5) {
            super(composition, component1, component2, component3, component4, component5);
        }

        @Override
        public void process(int entityId, Composition.Of5.@NonNull Consumer<T1, T2, T3, T4, T5> callback) {
            callback.consume(entityId, get(entityId, 0), get(entityId, 1), get(entityId, 2), get(entityId, 3), get(entityId, 4));
        }

        @Override
        public void process(Composition.Of5.@NonNull Consumer<T1, T2, T3, T4, T5> callback) {
            super.process(entityId -> process(entityId, callback));
        }

        @Override
        public void inserted(Composition.Of5.@NonNull Consumer<T1, T2, T3, T4, T5> callback) {
            super.inserted(entityId -> process(entityId, callback));
        }

        @Override
        public void removed(Composition.Of5.@NonNull Consumer<T1, T2, T3, T4, T5> callback) {
            super.removed(entityId -> process(entityId, callback));
        }

    }

    private final class Composition6<T1, T2, T3, T4, T5, T6> extends AbstractRetrieveComposition implements Composition.Of6<T1, T2, T3, T4, T5, T6> {

        private Composition6(Composition composition, Class<T1> component1, Class<T2> component2, Class<T3> component3, Class<T4> component4, Class<T5> component5, Class<T6> component6) {
            super(composition, component1, component2, component3, component4, component5, component6);
        }

        @Override
        public void process(int entityId, Composition.Of6.@NonNull Consumer<T1, T2, T3, T4, T5, T6> callback) {
            callback.consume(entityId, get(entityId, 0), get(entityId, 1), get(entityId, 2), get(entityId, 3), get(entityId, 4), get(entityId, 5));
        }

        @Override
        public void process(Composition.Of6.@NonNull Consumer<T1, T2, T3, T4, T5, T6> callback) {
            super.process(entityId -> process(entityId, callback));
        }

        @Override
        public void inserted(Composition.Of6.@NonNull Consumer<T1, T2, T3, T4, T5, T6> callback) {
            super.inserted(entityId -> process(entityId, callback));
        }

        @Override
        public void removed(Composition.Of6.@NonNull Consumer<T1, T2, T3, T4, T5, T6> callback) {
            super.removed(entityId -> process(entityId, callback));
        }

    }

    private final class Composition7<T1, T2, T3, T4, T5, T6, T7> extends AbstractRetrieveComposition implements Composition.Of7<T1, T2, T3, T4, T5, T6, T7> {

        private Composition7(Composition composition, Class<T1> component1, Class<T2> component2, Class<T3> component3, Class<T4> component4, Class<T5> component5, Class<T6> component6,
                Class<T7> component7) {

            super(composition, component1, component2, component3, component4, component5, component6, component7);
        }

        @Override
        public void process(int entityId, Composition.Of7.@NonNull Consumer<T1, T2, T3, T4, T5, T6, T7> callback) {
            callback.consume(entityId, get(entityId, 0), get(entityId, 1), get(entityId, 2), get(entityId, 3), get(entityId, 4), get(entityId, 5), get(entityId, 6));
        }

        @Override
        public void process(Composition.Of7.@NonNull Consumer<T1, T2, T3, T4, T5, T6, T7> callback) {
            super.process(entityId -> process(entityId, callback));
        }

        @Override
        public void inserted(Composition.Of7.@NonNull Consumer<T1, T2, T3, T4, T5, T6, T7> callback) {
            super.inserted(entityId -> process(entityId, callback));
        }

        @Override
        public void removed(Composition.Of7.@NonNull Consumer<T1, T2, T3, T4, T5, T6, T7> callback) {
            super.removed(entityId -> process(entityId, callback));
        }

    }

    private final class Composition8<T1, T2, T3, T4, T5, T6, T7, T8> extends AbstractRetrieveComposition implements Composition.Of8<T1, T2, T3, T4, T5, T6, T7, T8> {

        private Composition8(Composition composition, Class<T1> component1, Class<T2> component2, Class<T3> component3, Class<T4> component4, Class<T5> component5, Class<T6> component6,
                Class<T7> component7, Class<T8> component8) {

            super(composition, component1, component2, component3, component4, component5, component6, component7, component8);
        }

        @Override
        public void process(int entityId, Composition.Of8.@NonNull Consumer<T1, T2, T3, T4, T5, T6, T7, T8> callback) {
            callback.consume(entityId, get(entityId, 0), get(entityId, 1), get(entityId, 2), get(entityId, 3), get(entityId, 4), get(entityId, 5), get(entityId, 6), get(entityId, 7));
        }

        @Override
        public void process(Composition.Of8.@NonNull Consumer<T1, T2, T3, T4, T5, T6, T7, T8> callback) {
            super.process(entityId -> process(entityId, callback));
        }

        @Override
        public void inserted(Composition.Of8.@NonNull Consumer<T1, T2, T3, T4, T5, T6, T7, T8> callback) {
            super.inserted(entityId -> process(entityId, callback));
        }

        @Override
        public void removed(Composition.Of8.@NonNull Consumer<T1, T2, T3, T4, T5, T6, T7, T8> callback) {
            super.removed(entityId -> process(entityId, callback));
        }

    }

    private abstract sealed class AbstractRetrieveComposition implements Composition {

        private final CompositionImpl composition;
        protected final ComponentData<?>[] components;

        private AbstractRetrieveComposition(Composition composition, Class<?>... components) {
            this.composition = (CompositionImpl) composition; // no endless recursion please

            this.components = new ComponentData<?>[components.length];
            for (int i = 0, s = components.length; i < s; i++) {
                this.components[i] = componentManager.getData(components[i]);
            }
        }

        @SuppressWarnings("unchecked")
        protected <T> T get(int entityId, int component) {
            return (T) components[component].getComponent(entityId);
        }

        @Override
        public void inserted(@NonNull IntConsumer inserted) {
            composition.inserted(inserted);
        }

        @Override
        public void removed(@NonNull IntConsumer removed) {
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
        public void process(@NonNull IntConsumer process) {
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

    private static final class CompositionImpl implements Composition {

        private final Spec spec;

        private final IntBag entities;
        private final IntBag lookup;

        private final IntBag maskCache;

        private IntConsumer inserted;
        private List<IntConsumer> moreInserted;

        private IntConsumer removed;
        private List<IntConsumer> moreRemoved;

        private final Map<BitVector, AbstractRetrieveComposition> retrieves = new ConcurrentHashMap<>();

        private CompositionImpl(@NonNull Spec spec, @NonNull IntBag entities, @NonNull IntBag lookup) {
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
        <T extends AbstractRetrieveComposition> T getRetrieveComposition(BitVector vector, Supplier<T> constructor) {
            // Lookup cached
            var result = retrieves.get(vector);
            if (result != null) {
                return (T) result;
            }

            var key = new BitVector(vector);
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
                for (var more : moreInserted) {
                    more.accept(entityId);
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
                for (var more : moreRemoved) {
                    more.accept(entityId);
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
        public void inserted(@NonNull IntConsumer callback) {
            if (this.inserted == null) {
                this.inserted = callback;
            } else {
                if (this.moreInserted == null) {
                    this.moreInserted = new ArrayList<>();
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
                    this.moreRemoved = new ArrayList<>();
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
            return this.entities.getSize() == 0;
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
