package de.schosin.ecs.benchmark.composition;

import static de.schosin.ecs.benchmark.BaseBenchmark.benchmarkName;
import static de.schosin.ecs.plugins.wildcards.types.WildcardType.wildcard;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import de.schosin.ecs.api.components.ComponentSetConfig;
import de.schosin.ecs.benchmark.EcsBenchmark;
import de.schosin.ecs.benchmark.others.components.SchosinComponents;
import de.schosin.ecs.plugins.composition.Composition;
import de.schosin.ecs.plugins.composition.CompositionData;
import de.schosin.ecs.plugins.composition.CompositionData1;
import de.schosin.ecs.plugins.composition.CompositionData4;
import de.schosin.ecs.plugins.composition.CompositionData8;
import de.schosin.ecs.plugins.wildcards.result.WildcardResult;

public class CompositionIterationBenchmark implements SchosinComponents {

    public static void main(String[] args) throws RunnerException {
        var options = new OptionsBuilder()
                .include(benchmarkName(BaseCompositionIterationBenchmark.Unpack01.class))
                .include(benchmarkName(BaseCompositionIterationBenchmark.Unpack04.class))
                .include(benchmarkName(BaseCompositionIterationBenchmark.Unpack08.class))
                .include(benchmarkName(WildcardIterationBenchmark.class))
                .build();

        new Runner(options).run();
    }

    public static class WildcardIterationBenchmark extends EcsBenchmark {

        @Param({ "1000000" })
        int entities;

        CompositionData1<WildcardResult<Schosin12>> composition12;
        CompositionData1<WildcardResult<Schosin1234>> composition1234;

        Blackhole bh;

        public static void main(String[] args) {
            var benchmark = new WildcardIterationBenchmark();
            benchmark.entities = 1000000;

            var bh = new Blackhole("Today's password is swordfish. I understand instantiating Blackholes directly is dangerous.");
            benchmark.setupEntities(bh);

            while (true) {
                benchmark.iterate12();
            }
        }

        @Setup(Level.Trial)
        public void setupEntities(Blackhole bh) {
            setupWorld(entities);

            var archetype4 = world.createArchetype(Schosin1.class, Schosin2.class, Schosin3.class, Schosin4.class);
            archetype4.createBatch(entities, init -> init.create(new Schosin1(), new Schosin2(), new Schosin3(), new Schosin4()));

            this.composition12 = world.createComposition(Composition.all(), wildcard(Schosin12.class));
            this.composition1234 = world.createComposition(Composition.all(), wildcard(Schosin1234.class));

            this.bh = bh;
        }

        @Benchmark
        public void iterate12() {
            this.composition12.process(this::process);
        }

        @Benchmark
        public void iterate1234() {
            this.composition1234.process(this::process);
        }

        private <T> void process(int entityId, WildcardResult<T> components) {
            bh.consume(entityId);

            for (var iter = components.iterator(); iter.hasNext();) {
                bh.consume(iter.next());
            }
        }

    }

    public static class BaseCompositionIterationBenchmark extends EcsBenchmark {

        @Param({ "1000000" })
        int entities;

        @Param({ "1", "4", "8" })
        int components;

        Blackhole bh;

        public void setupEntities() {
            setupWorld(entities);

            var batchSize = entities / components;

            var archetype1 = world.createArchetype(Schosin1.class);
            var archetype2 = world.createArchetype(Schosin1.class, Schosin2.class);
            var archetype3 = world.createArchetype(Schosin1.class, Schosin2.class, Schosin3.class);
            var archetype4 = world.createArchetype(Schosin1.class, Schosin2.class, Schosin3.class, Schosin4.class);
            var archetype5 = world.createArchetype(Schosin1.class, Schosin2.class, Schosin3.class, Schosin4.class, Schosin5.class);
            var archetype6 = world.createArchetype(Schosin1.class, Schosin2.class, Schosin3.class, Schosin4.class, Schosin5.class, Schosin6.class);
            var archetype7 = world.createArchetype(Schosin1.class, Schosin2.class, Schosin3.class, Schosin4.class, Schosin5.class, Schosin6.class, Schosin7.class);
            var archetype8 = world.createArchetype(Schosin1.class, Schosin2.class, Schosin3.class, Schosin4.class, Schosin5.class, Schosin6.class, Schosin7.class, Schosin8.class);

            switch (components) {
                case 1 -> {
                    archetype1.createBatch(batchSize, () -> new Schosin1());
                }
                case 4 -> {
                    archetype1.createBatch(batchSize, () -> new Schosin1());
                    archetype2.createBatch(batchSize, init -> init.create(new Schosin1(), new Schosin2()));
                    archetype3.createBatch(batchSize, init -> init.create(new Schosin1(), new Schosin2(), new Schosin3()));
                    archetype4.createBatch(batchSize, init -> init.create(new Schosin1(), new Schosin2(), new Schosin3(), new Schosin4()));
                }
                case 8 -> {
                    archetype1.createBatch(batchSize, () -> new Schosin1());
                    archetype2.createBatch(batchSize, init -> init.create(new Schosin1(), new Schosin2()));
                    archetype3.createBatch(batchSize, init -> init.create(new Schosin1(), new Schosin2(), new Schosin3()));
                    archetype4.createBatch(batchSize, init -> init.create(new Schosin1(), new Schosin2(), new Schosin3(), new Schosin4()));
                    archetype5.createBatch(batchSize, init -> init.create(new Schosin1(), new Schosin2(), new Schosin3(), new Schosin4(), new Schosin5()));
                    archetype6.createBatch(batchSize, init -> init.create(new Schosin1(), new Schosin2(), new Schosin3(), new Schosin4(), new Schosin5(), new Schosin6()));
                    archetype7.createBatch(batchSize, init -> init.create(new Schosin1(), new Schosin2(), new Schosin3(), new Schosin4(), new Schosin5(), new Schosin6(), new Schosin7()));
                    archetype8.createBatch(batchSize,
                            init -> init.create(new Schosin1(), new Schosin2(), new Schosin3(), new Schosin4(), new Schosin5(), new Schosin6(), new Schosin7(), new Schosin8()));
                }
                default -> throw new IllegalArgumentException("Unsupported number of components: " + components);
            }
        }

        public static class Unpack01 extends BaseCompositionIterationBenchmark {

            private CompositionData1<Schosin1> composition;

            @Setup(Level.Trial)
            public void setup(Blackhole bh) {
                setupEntities();

                this.composition = world.createComposition(Composition.all(Schosin1.class), Schosin1.class);

                this.bh = bh;
            }

            @Benchmark
            public void direct() {
                composition.process(this::process);
            }

            private void process(int entityId, Schosin1 c1) {
                bh.consume(entityId);

                bh.consume(c1);
            }

        }

        public static class Unpack04 extends BaseCompositionIterationBenchmark {

            public static void main(String[] args) {
                var benchmark = new Unpack04();
                benchmark.components = 4;
                benchmark.entities = 1000000;

                var bh = new Blackhole("Today's password is swordfish. I understand instantiating Blackholes directly is dangerous.");
                benchmark.setup(bh);

                while (true) {
                    benchmark.componentSet();
                }
            }

            private CompositionData4<Schosin1, Schosin2, Schosin3, Schosin4> compositionData;
            private CompositionData<ComponentSet4.Processor> compositionSet;
            private CompositionData1<WildcardResult<Schosin1234>> compositionWildcard;

            @Setup(Level.Trial)
            public void setup(Blackhole bh) {
                setupEntities();

                this.compositionData = world.createComposition(Composition.all(Schosin1.class), Schosin1.class, Schosin2.class, Schosin3.class, Schosin4.class);
                this.compositionSet = world.createComposition(Composition.all(Schosin1.class), ComponentSet4.TYPE);
                this.compositionWildcard = world.createComposition(Composition.all(Schosin1.class), wildcard(Schosin1234.class));

                this.bh = bh;
            }

            @Benchmark
            public void dataType() {
                compositionData.process(this::process);
            }

            @Benchmark
            public void componentSet() {
                compositionSet.process(this::process);
            }

            @ComponentSetConfig("ComponentSet4")
            private void process(int entityId, Schosin1 c1, Schosin2 c2, Schosin3 c3, Schosin4 c4) {
                bh.consume(entityId);

                bh.consume(c1);
                bh.consume(c2);
                bh.consume(c3);
                bh.consume(c4);
            }

            @Benchmark
            public void wildcardLoop() {
                compositionWildcard.process(this::processLoop);
            }

            private void processLoop(int entityId, WildcardResult<Schosin1234> components) {
                bh.consume(entityId);

                for (int i = 0, s = components.size(); i < s; i++) {
                    bh.consume(components.get(i));
                }
            }

            @Benchmark
            public void wildcardIterator() {
                compositionWildcard.process(this::processIterator);
            }

            private void processIterator(int entityId, WildcardResult<Schosin1234> components) {
                bh.consume(entityId);

                for (var iter = components.iterator(); iter.hasNext();) {
                    bh.consume(iter.next());
                }
            }
        }

        public static class Unpack08 extends BaseCompositionIterationBenchmark {

            private CompositionData8<Schosin1, Schosin2, Schosin3, Schosin4, Schosin5, Schosin6, Schosin7, Schosin8> compositionData;
            private CompositionData<ComponentSet8.Processor> compositionSet;
            private CompositionData1<WildcardResult<Schosin>> compositionWildcard;

            @Setup(Level.Trial)
            public void setup(Blackhole bh) {
                setupEntities();

                this.compositionData = world.createComposition(Composition.all(Schosin1.class),
                        Schosin1.class, Schosin2.class, Schosin3.class, Schosin4.class, Schosin5.class, Schosin6.class, Schosin7.class, Schosin8.class);

                this.compositionSet = world.createComposition(Composition.all(Schosin1.class), ComponentSet8.TYPE);
                this.compositionWildcard = world.createComposition(Composition.all(Schosin1.class), wildcard(Schosin.class));

                this.bh = bh;
            }

            @Benchmark
            public void dataType() {
                compositionData.process(this::process);
            }

            @Benchmark
            public void componentSet() {
                compositionSet.process(this::process);
            }

            @ComponentSetConfig("ComponentSet8")
            private void process(int entityId, Schosin1 c1, Schosin2 c2, Schosin3 c3, Schosin4 c4, Schosin5 c5, Schosin6 c6, Schosin7 c7, Schosin8 c8) {
                bh.consume(entityId);

                bh.consume(c1);
                bh.consume(c2);
                bh.consume(c3);
                bh.consume(c4);
                bh.consume(c5);
                bh.consume(c6);
                bh.consume(c7);
                bh.consume(c8);
            }

            @Benchmark
            public void wildcardLoop() {
                compositionWildcard.process(this::processLoop);
            }

            private void processLoop(int entityId, WildcardResult<Schosin> components) {
                bh.consume(entityId);

                for (int i = 0, s = components.size(); i < s; i++) {
                    bh.consume(components.get(i));
                }
            }

            @Benchmark
            public void wildcardIterator() {
                compositionWildcard.process(this::processIterator);
            }

            private void processIterator(int entityId, WildcardResult<Schosin> components) {
                bh.consume(entityId);

                for (var iter = components.iterator(); iter.hasNext();) {
                    bh.consume(iter.next());
                }
            }

        }

    }

}
