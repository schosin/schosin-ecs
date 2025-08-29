package de.schosin.ecs.benchmark.others;

import static de.schosin.ecs.benchmark.BaseBenchmark.benchmarkName;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import de.schosin.ecs.api.components.ComponentSetConfig;
import de.schosin.ecs.api.components.mappers.ComponentMapper;
import de.schosin.ecs.benchmark.BaseBenchmark;
import de.schosin.ecs.benchmark.others.components.DominionComponents;
import de.schosin.ecs.benchmark.others.components.SchosinComponents;
import de.schosin.ecs.plugins.composition.Composition;
import de.schosin.ecs.plugins.composition.CompositionData;
import de.schosin.ecs.plugins.composition.CompositionData1;
import de.schosin.ecs.plugins.composition.CompositionData3;
import de.schosin.ecs.plugins.composition.CompositionData6;
import de.schosin.ecs.worlds.DefaultWorld;

import dev.dominion.ecs.engine.EntityRepository;

public class ComponentIterationBenchmark {

    public static void main(String[] args) throws Exception {
        var options = new OptionsBuilder()
                .include(benchmarkName(SchosinEcs.class))
                // .include(benchmarkName(Dominion.class))
                .build();

        new Runner(options).run();
    }

    public static class SchosinEcs extends BaseBenchmark implements SchosinComponents {

        @Param({ "12000000" })
        int size;

        DefaultWorld world;

        Blackhole bh;

        ComponentMapper<Schosin1> mapper1;
        ComponentMapper<Schosin2> mapper2;
        ComponentMapper<Schosin3> mapper3;
        ComponentMapper<Schosin4> mapper4;
        ComponentMapper<Schosin5> mapper5;
        ComponentMapper<Schosin6> mapper6;

        public void setup() {
            world = DefaultWorld.builder().expectedEntities(size).build();

            var batchSize = size / 6;

            var archetype1 = world.createArchetype(Schosin1.class);
            archetype1.createBatch(batchSize, () -> new Schosin1());

            var archetype2 = world.createArchetype(Schosin1.class, Schosin2.class);
            archetype2.createBatch(batchSize, init -> init.create(new Schosin1(), new Schosin2()));

            var archetype3 = world.createArchetype(Schosin1.class, Schosin2.class, Schosin3.class);
            archetype3.createBatch(batchSize, init -> init.create(new Schosin1(), new Schosin2(), new Schosin3()));

            var archetype4 = world.createArchetype(Schosin1.class, Schosin2.class, Schosin3.class, Schosin4.class);
            archetype4.createBatch(batchSize, init -> init.create(new Schosin1(), new Schosin2(), new Schosin3(), new Schosin4()));

            var archetype5 = world.createArchetype(Schosin1.class, Schosin2.class, Schosin3.class, Schosin4.class, Schosin5.class);
            archetype5.createBatch(batchSize, init -> init.create(new Schosin1(), new Schosin2(), new Schosin3(), new Schosin4(), new Schosin5()));

            var archetype6 = world.createArchetype(Schosin1.class, Schosin2.class, Schosin3.class, Schosin4.class, Schosin5.class, Schosin6.class);
            archetype6.createBatch(batchSize, init -> init.create(new Schosin1(), new Schosin2(), new Schosin3(), new Schosin4(), new Schosin5(), new Schosin6()));

            this.mapper1 = world.getComponents(Schosin1.class);
            this.mapper2 = world.getComponents(Schosin2.class);
            this.mapper3 = world.getComponents(Schosin3.class);
            this.mapper4 = world.getComponents(Schosin4.class);
            this.mapper5 = world.getComponents(Schosin5.class);
            this.mapper6 = world.getComponents(Schosin6.class);
        }

        public static class IterationUnpack01 extends SchosinEcs {

            private CompositionData1<Schosin1> composition;

            // @Setup(Level.Trial)
            public void setupComposition(Blackhole bh) {
                setup();

                var builder = Composition.all(Schosin1.class);
                composition = world.createComposition(builder, Schosin1.class);

                this.bh = bh;
            }

            // @Benchmark
            public void iterate() {
                composition.process(this::process);
            }

            @SuppressWarnings("unused")
            private void process(int entityId, Schosin1 c1) {
                // don't consume entityId as Dominion does not have access to it for a single component
                bh.consume(c1);
            }

        }

        public static class IterationUnpack03 extends SchosinEcs {

            private CompositionData3<Schosin1, Schosin2, Schosin3> composition;
            private CompositionData<ComponentSet3.Processor> setComposition;

            @Setup(Level.Trial)
            public void setupComposition(Blackhole bh) {
                setup();

                var builder = Composition.all(Schosin1.class, Schosin2.class, Schosin3.class);
                composition = world.createComposition(builder, Schosin1.class, Schosin2.class, Schosin3.class);
                setComposition = world.createComposition(builder, ComponentSet3.TYPE);

                this.bh = bh;
            }

            // @Benchmark
            public void componentMappers() {
                composition.process(this::processMapper);
            }

            private void processMapper(int entityId) {
                bh.consume(entityId);
                bh.consume(mapper1.get(entityId));
                bh.consume(mapper2.get(entityId));
                bh.consume(mapper3.get(entityId));
            }

            // @Benchmark
            public void dataType() {
                composition.process(this::process);
            }

            @Benchmark
            public void componentSet() {
                setComposition.process(this::process);
            }

            @ComponentSetConfig("ComponentSet3")
            private void process(int entityId, Schosin1 c1, Schosin2 c2, Schosin3 c3) {
                bh.consume(entityId);
                bh.consume(c1);
                bh.consume(c2);
                bh.consume(c3);
            }

        }

        public static class IterationUnpack06 extends SchosinEcs {

            private CompositionData6<Schosin1, Schosin2, Schosin3, Schosin4, Schosin5, Schosin6> composition;
            private CompositionData<ComponentSet6.Processor> setComposition;

            // @Setup(Level.Trial)
            public void setupComposition(Blackhole bh) {
                setup();

                var builder = Composition.all(Schosin1.class, Schosin2.class, Schosin3.class, Schosin4.class, Schosin5.class, Schosin6.class);
                composition = world.createComposition(builder, Schosin1.class, Schosin2.class, Schosin3.class, Schosin4.class, Schosin5.class, Schosin6.class);
                setComposition = world.createComposition(builder, ComponentSet6.TYPE);

                this.bh = bh;
            }

            // @Benchmark
            public void componentMappers() {
                composition.process(this::processMapper);
            }

            private void processMapper(int entityId) {
                bh.consume(entityId);
                bh.consume(mapper1.get(entityId));
                bh.consume(mapper2.get(entityId));
                bh.consume(mapper3.get(entityId));
                bh.consume(mapper4.get(entityId));
                bh.consume(mapper5.get(entityId));
                bh.consume(mapper6.get(entityId));
            }

            // @Benchmark
            public void dataType() {
                composition.process(this::process);
            }

            // @Benchmark
            public void componentSet() {
                setComposition.process(this::process);
            }

            @ComponentSetConfig("ComponentSet6")
            private void process(int entityId, Schosin1 c1, Schosin2 c2, Schosin3 c3, Schosin4 c4, Schosin5 c5, Schosin6 c6) {
                bh.consume(entityId);
                bh.consume(c1);
                bh.consume(c2);
                bh.consume(c3);
                bh.consume(c4);
                bh.consume(c5);
                bh.consume(c6);
            }

        }

    }

    public static class Dominion extends BaseBenchmark implements DominionComponents {

        @Param({ "12000000" })
        int size;

        EntityRepository entityRepository;

        @Setup(Level.Trial)
        public void setup() {
            // System.setProperty("dominion.benchmark.size", "HUGE"); // original benchmark from dominion did not specify size, HUGE would be "millions of entities"
            entityRepository = (EntityRepository) new EntityRepository.Factory().create("benchmark");

            var batchSize = size / 6;
            for (int i = 0; i < batchSize; i++) {
                entityRepository.createEntity(new Dominion1());
                entityRepository.createEntity(new Dominion1(), new Dominion2());
                entityRepository.createEntity(new Dominion1(), new Dominion2(), new Dominion3());
                entityRepository.createEntity(new Dominion1(), new Dominion2(), new Dominion3(), new Dominion4());
                entityRepository.createEntity(new Dominion1(), new Dominion2(), new Dominion3(), new Dominion4(), new Dominion5());
                entityRepository.createEntity(new Dominion1(), new Dominion2(), new Dominion3(), new Dominion4(), new Dominion5(), new Dominion6());
            }
        }

        public static class IterationUnpack01 extends Dominion {

            @Benchmark
            public void iterate(Blackhole bh) {
                var iterator = entityRepository.findCompositionsWith(Dominion1.class).iterator();
                while (iterator.hasNext()) {
                    bh.consume(iterator.next());
                }
            }

        }

        public static class IterationUnpack03 extends Dominion {

            @Benchmark
            public void iterate(Blackhole bh) {
                var iterator = entityRepository.findCompositionsWith(Dominion1.class, Dominion2.class, Dominion3.class).iterator();
                while (iterator.hasNext()) {
                    var result = iterator.next();

                    bh.consume(result.entity());
                    bh.consume(result.comp1());
                    bh.consume(result.comp2());
                    bh.consume(result.comp3());
                }
            }

        }

        public static class IterationUnpack06 extends Dominion {

            @Benchmark
            public void iterate(Blackhole bh) {
                var iterator = entityRepository.findCompositionsWith(Dominion1.class, Dominion2.class, Dominion3.class, Dominion4.class, Dominion5.class, Dominion6.class).iterator();

                while (iterator.hasNext()) {
                    var result = iterator.next();

                    bh.consume(result.entity());
                    bh.consume(result.comp1());
                    bh.consume(result.comp2());
                    bh.consume(result.comp3());
                    bh.consume(result.comp4());
                    bh.consume(result.comp5());
                    bh.consume(result.comp6());
                }
            }

        }

    }

}
