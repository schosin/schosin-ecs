package de.schosin.ecs.benchmark.others;

import static de.schosin.ecs.benchmark.BaseBenchmark.benchmarkName;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import de.schosin.ecs.api.World;
import de.schosin.ecs.api.components.mappers.ComponentMapper;
import de.schosin.ecs.benchmark.BaseBenchmark;
import de.schosin.ecs.benchmark.BenchmarkWorld;
import de.schosin.ecs.benchmark.others.components.SchosinComponents;
import de.schosin.ecs.plugins.transmuter.Transmuter;

public class ComponentAdditionBenchmark {

    public static void main(String[] args) throws Exception {
        var options = new OptionsBuilder()
                .include(benchmarkName(SchosinEcs.Add1.class))
                .include(benchmarkName(SchosinEcs.Add2.class))
                .include(benchmarkName(SchosinEcs.Add4.class))
                .include(benchmarkName(SchosinEcs.Add6.class))
                .build();

        new Runner(options).run();
    }

    public static class SchosinEcs extends BaseBenchmark implements SchosinComponents {

        @Param({ "1000000" })
        int size;

        BenchmarkWorld world;

        ComponentMapper<Schosin1> mapper1;
        ComponentMapper<Schosin2> mapper2;
        ComponentMapper<Schosin3> mapper3;
        ComponentMapper<Schosin4> mapper4;
        ComponentMapper<Schosin5> mapper5;
        ComponentMapper<Schosin6> mapper6;

        Transmuter.Add2<Schosin1, Schosin2> add2;
        Transmuter.Add4<Schosin1, Schosin2, Schosin3, Schosin4> add4;
        Transmuter.Add6<Schosin1, Schosin2, Schosin3, Schosin4, Schosin5, Schosin6> add6;

        int[] entities;

        @Setup(Level.Trial)
        public void init() {
            this.world = World.builder(BenchmarkWorld.class).build();

            this.mapper1 = world.getComponents(Schosin1.class);
            this.mapper2 = world.getComponents(Schosin2.class);
            this.mapper3 = world.getComponents(Schosin3.class);
            this.mapper4 = world.getComponents(Schosin4.class);
            this.mapper5 = world.getComponents(Schosin5.class);
            this.mapper6 = world.getComponents(Schosin6.class);

            this.add2 = world.createTransmuter(Transmuter.add(Schosin1.class, Schosin2.class));
            this.add4 = world.createTransmuter(Transmuter.add(Schosin1.class, Schosin2.class, Schosin3.class, Schosin4.class));
            this.add6 = world.createTransmuter(Transmuter.add(Schosin1.class, Schosin2.class, Schosin3.class, Schosin4.class, Schosin5.class, Schosin6.class));

            this.entities = new int[size];
        }

        @Setup(Level.Invocation)
        public void initInvocation() {
            for (int i = 0; i < size; i++) {
                this.entities[i] = world.createEntity();
            }
        }

        @TearDown(Level.Invocation)
        public void tearDownInvocation() {
            for (int i = 0; i < size; i++) {
                world.deleteEntity(this.entities[i]);
            }

            world.process();
        }

        public static class Add1 extends SchosinEcs {

            private final Schosin1 s1 = new Schosin1();

            public static void main(String[] args) {
                var benchmark = new Add1();
                benchmark.size = 1000000;

                benchmark.init();

                while (true) {
                    benchmark.initInvocation();
                    benchmark.componentMapper();
                    benchmark.tearDownInvocation();
                }
            }

            @Benchmark
            public void componentMapper() {
                for (int i = 0; i < size; i++) {
                    this.mapper1.add(entities[i], s1);
                }

                world.process();
            }

        }

        public static class Add2 extends SchosinEcs {

            private final Schosin1 s1 = new Schosin1();
            private final Schosin2 s2 = new Schosin2();

            @Benchmark
            public void componentMappers() {
                for (int i = 0; i < size; i++) {
                    var entityId = entities[i];

                    this.mapper1.add(entityId, s1);
                    this.mapper2.add(entityId, s2);
                }

                world.process();
            }

            @Benchmark
            public void transmuter() {
                for (int i = 0; i < size; i++) {
                    this.add2.apply(entities[i], s1, s2);
                }

                world.process();
            }

        }

        public static class Add4 extends SchosinEcs {

            private final Schosin1 s1 = new Schosin1();
            private final Schosin2 s2 = new Schosin2();
            private final Schosin3 s3 = new Schosin3();
            private final Schosin4 s4 = new Schosin4();

            @Benchmark
            public void componentMappers() {
                for (int i = 0; i < size; i++) {
                    var entityId = entities[i];

                    this.mapper1.add(entityId, s1);
                    this.mapper2.add(entityId, s2);
                    this.mapper3.add(entityId, s3);
                    this.mapper4.add(entityId, s4);
                }

                world.process();
            }

            @Benchmark
            public void transmuter() {
                for (int i = 0; i < size; i++) {
                    this.add4.apply(entities[i], s1, s2, s3, s4);
                }

                world.process();
            }

        }

        public static class Add6 extends SchosinEcs {

            private final Schosin1 s1 = new Schosin1();
            private final Schosin2 s2 = new Schosin2();
            private final Schosin3 s3 = new Schosin3();
            private final Schosin4 s4 = new Schosin4();
            private final Schosin5 s5 = new Schosin5();
            private final Schosin6 s6 = new Schosin6();

            public static void main(String[] args) {
                var benchmark = new Add6();
                benchmark.size = 1000000;

                benchmark.init();

                while (true) {
                    benchmark.initInvocation();
                    benchmark.componentMappers();
                    benchmark.tearDownInvocation();
                }
            }

            @Benchmark
            public void componentMappers() {
                for (int i = 0; i < size; i++) {
                    var entityId = entities[i];

                    this.mapper1.add(entityId, s1);
                    this.mapper2.add(entityId, s2);
                    this.mapper3.add(entityId, s3);
                    this.mapper4.add(entityId, s4);
                    this.mapper5.add(entityId, s5);
                    this.mapper6.add(entityId, s6);
                }

                world.process();
            }

            @Benchmark
            public void transmuter() {
                for (int i = 0; i < size; i++) {
                    this.add6.apply(entities[i], s1, s2, s3, s4, s5, s6);
                }

                world.process();
            }

        }

    }

}
