package de.schosin.ecs.benchmark.others;

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
import de.schosin.ecs.benchmark.others.components.DominionComponents;
import de.schosin.ecs.benchmark.others.components.SchosinComponents;
import de.schosin.ecs.plugins.archetype.Archetype1;
import de.schosin.ecs.plugins.archetype.Archetype3;
import de.schosin.ecs.plugins.archetype.Archetype6;
import de.schosin.ecs.worlds.DefaultWorld;

import dev.dominion.ecs.api.Composition;
import dev.dominion.ecs.api.Entity;
import dev.dominion.ecs.engine.EntityRepository;

public class EntityCreationBenchmark {

    public static void main(String[] args) throws Exception {
        var options = new OptionsBuilder()
                .include(benchmarkName(SchosinEcs.class))
                // .include(benchmarkName(Dominion.class))
                .build();

        new Runner(options).run();
    }

    public static class SchosinEcs extends BaseBenchmark implements SchosinComponents {

        public static void main(String[] args) throws InterruptedException {
            var benchmark = new SchosinEcs();
            benchmark.size = 1000000;
            benchmark.init();

            var bh = new Blackhole("Today's password is swordfish. I understand instantiating Blackholes directly is dangerous.");

            while (true) {
                benchmark.createEntityWith03(bh);
                benchmark.tearDownInvocation();
            }
        }

        @Param({ "1000000" })
        private int size;

        private DefaultWorld world;
        private de.schosin.ecs.plugins.composition.Composition all;

        private Archetype1<Schosin1> archetype1;
        private Archetype3<Schosin1, Schosin2, Schosin3> archetype3;
        private Archetype6<Schosin1, Schosin2, Schosin3, Schosin4, Schosin5, Schosin6> archetype6;

        private Archetype1<Pooled1> pooled1;
        private Archetype3<Pooled1, Pooled2, Pooled3> pooled3;
        private Archetype6<Pooled1, Pooled2, Pooled3, Pooled4, Pooled5, Pooled6> pooled6;

        @Setup(Level.Trial)
        public void init() {
            this.world = DefaultWorld.create();
            this.all = world.createComposition(de.schosin.ecs.plugins.composition.Composition.all());

            this.archetype1 = world.createArchetype(Schosin1.class);
            this.archetype3 = world.createArchetype(Schosin1.class, Schosin2.class, Schosin3.class);
            this.archetype6 = world.createArchetype(Schosin1.class, Schosin2.class, Schosin3.class, Schosin4.class, Schosin5.class, Schosin6.class);

            this.pooled1 = world.createArchetype(Pooled1.class);
            this.pooled3 = world.createArchetype(Pooled1.class, Pooled2.class, Pooled3.class);
            this.pooled6 = world.createArchetype(Pooled1.class, Pooled2.class, Pooled3.class, Pooled4.class, Pooled5.class, Pooled6.class);
        }

        @TearDown(Level.Invocation)
        public void tearDownInvocation() {
            all.process(world::deleteEntity);
            world.process();
        }

        @Benchmark
        public void createEntityWith01(Blackhole bh) {
            bh.consume(archetype1.createBatch(size, () -> new Schosin1()));
        }

        // @Benchmark
        public void createEntityWith01_Pooled(Blackhole bh) {
            bh.consume(pooled1.createBatch(size, () -> pooled1.getInstance(Pooled1.class)));
        }

        @Benchmark
        public void createEntityWith03(Blackhole bh) {
            bh.consume(archetype3.createBatch(size, (i, factory) -> factory.create(new Schosin1(), new Schosin2(), new Schosin3())));
        }

        // @Benchmark
        public void createEntityWith03_Pooled(Blackhole bh) {
            bh.consume(pooled3.createBatch(size, (i, factory) -> factory.create(pooled3.getInstance(Pooled1.class), pooled3.getInstance(Pooled2.class), pooled3.getInstance(Pooled3.class))));
        }

        @Benchmark
        public void createEntityWith06(Blackhole bh) {
            bh.consume(archetype6.createBatch(size, (i, factory) -> factory.create(new Schosin1(), new Schosin2(), new Schosin3(), new Schosin4(), new Schosin5(), new Schosin6())));
        }

        // @Benchmark
        public void createEntityWith06_Pooled(Blackhole bh) {
            bh.consume(pooled6.createBatch(size, (i, factory) -> factory.create(
                    pooled6.getInstance(Pooled1.class), pooled6.getInstance(Pooled2.class), pooled6.getInstance(Pooled3.class),
                    pooled6.getInstance(Pooled4.class), pooled6.getInstance(Pooled5.class), pooled6.getInstance(Pooled6.class))));
        }

    }

    public static class Dominion extends BaseBenchmark implements DominionComponents {

        @Param({ "1000000" })
        private int size;

        EntityRepository entityRepository;
        Composition.Of1<Dominion1> composition1;
        Composition.Of3<Dominion1, Dominion2, Dominion3> composition3;
        Composition.Of6<Dominion1, Dominion2, Dominion3, Dominion4, Dominion5, Dominion6> composition6;
        Entity[] entities;

        @Setup(Level.Trial)
        public void setup() {
            entityRepository = (EntityRepository) new EntityRepository.Factory().create();
            Composition composition = entityRepository.composition();
            composition1 = composition.of(Dominion1.class);
            composition3 = composition.of(Dominion1.class, Dominion2.class, Dominion3.class);
            composition6 = composition.of(Dominion1.class, Dominion2.class, Dominion3.class, Dominion4.class, Dominion5.class, Dominion6.class);
            entities = new Entity[size];
            for (int i = 0; i < size; i++) {
                entities[i] = entityRepository.createEntity();
            }
        }

        @Setup(Level.Invocation)
        public void setupInvocation() {
            for (int i = 0; i < size; i++) {
                entityRepository.deleteEntity(entities[i]);
            }
        }

        @Benchmark
        public void createEntityWith01(Blackhole bh) {
            for (int i = 0; i < size; i++) {
                bh.consume(entities[i] = entityRepository.createPreparedEntity(composition1.withValue(new Dominion1())));
            }
        }

        @Benchmark
        public void createEntityWith03(Blackhole bh) {
            for (int i = 0; i < size; i++) {
                bh.consume(entities[i] = entityRepository.createPreparedEntity(composition3.withValue(new Dominion1(), new Dominion2(), new Dominion3())));
            }
        }

        @Benchmark
        public void createEntityWith06(Blackhole bh) {
            for (int i = 0; i < size; i++) {
                bh.consume(entities[i] = entityRepository
                        .createPreparedEntity(composition6.withValue(new Dominion1(), new Dominion2(), new Dominion3(), new Dominion4(), new Dominion5(), new Dominion6())));
            }
        }

    }

}
