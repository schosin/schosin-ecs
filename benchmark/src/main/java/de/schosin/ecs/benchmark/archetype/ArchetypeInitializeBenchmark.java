package de.schosin.ecs.benchmark.archetype;

import static de.schosin.ecs.benchmark.BaseBenchmark.benchmarkName;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import de.schosin.ecs.benchmark.BaseBenchmark;
import de.schosin.ecs.benchmark.others.components.SchosinComponents;
import de.schosin.ecs.plugins.archetype.BaseArchetype.ArchetypeBatch;
import de.schosin.ecs.plugins.composition.Composition;
import de.schosin.ecs.plugins.composition.EntityInitializer;
import de.schosin.ecs.worlds.DefaultWorld;

public class ArchetypeInitializeBenchmark {

    public static void main(String[] args) throws Exception {
        var options = new OptionsBuilder()
                .include(benchmarkName(OneInitializer.class))
                .build();

        new Runner(options).run();
    }

    public static class OneInitializer extends BaseBenchmark implements SchosinComponents {

        @Param({ "1000000" })
        private int size;

        private DefaultWorld world;
        private Composition all;

        private ArchetypeBatch archetype1;

        public static void main(String[] args) {
            var benchmark = new OneInitializer();
            benchmark.size = 1000000;
            benchmark.init();

            var bh = new Blackhole("Today's password is swordfish. I understand instantiating Blackholes directly is dangerous.");

            while (true) {
                benchmark.compositionInitialization(bh);
                benchmark.tearDownInvocation();
            }
        }

        @Setup(Level.Trial)
        public void init() {
            this.world = DefaultWorld.create();
            this.all = world.createComposition(Composition.all());

            this.archetype1 = world.createArchetype(Schosin1.class).bind((i, factory) -> factory.create(new Schosin1()));

            this.world.initialize(Composition.all(Schosin1.class), EntityInitializer.builder()
                    .with(Schosin2.class, Schosin2::new)
                    .build());

            var schosin2M = world.getComponents(Schosin2.class);
            var composition1 = world.createComposition(Composition.all(Schosin1.class).none(Schosin2.class));
            composition1.inserted(entityId -> schosin2M.add(entityId, new Schosin2()));
        }

        @TearDown(Level.Invocation)
        public void tearDownInvocation() {
            all.process(world::deleteEntity);
            world.process();
        }

        @Benchmark
        public void compositionInitialization(Blackhole bh) {
            bh.consume(archetype1.createBatch(size));
        }

    }

}
