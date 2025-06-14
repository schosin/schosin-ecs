package de.schosin.ecs.benchmark;

import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import de.schosin.ecs.benchmark.utils.collections.BitVectorBenchmark;
import de.schosin.ecs.engine.BagManager;
import de.schosin.ecs.engine.ChangeManager;
import de.schosin.ecs.engine.components.ComponentManager;
import de.schosin.ecs.engine.components.ComponentMapperManager;
import de.schosin.ecs.engine.components.TransmutationManager;
import de.schosin.ecs.engine.entities.EntityManager;
import de.schosin.ecs.plugins.archetype.ArchetypeManager;
import de.schosin.ecs.plugins.composition.manager.CompositionManager;
import de.schosin.ecs.worlds.DefaultWorld;

public abstract class EcsBenchmark extends BaseBenchmark {

    protected DefaultWorld world;

    protected BagManager bagManager;
    protected ComponentManager componentManager;
    protected CompositionManager compositionManager;
    protected EntityManager entityManager;
    protected ArchetypeManager archetypeManager;
    protected ChangeManager changeManager;
    protected TransmutationManager transmutationManager;
    protected ComponentMapperManager componentMapperManager;

    protected void setupWorld(int expectedEntities) {
        this.world = DefaultWorld.builder().expectedEntities(expectedEntities).build();

        try {
            this.bagManager = world.getSingleton(BagManager.class);
            this.componentManager = world.getSingleton(ComponentManager.class);
            this.compositionManager = world.getSingleton(CompositionManager.class);
            this.entityManager = world.getSingleton(EntityManager.class);
            this.archetypeManager = world.getSingleton(ArchetypeManager.class);
            this.changeManager = world.getSingleton(ChangeManager.class);
            this.transmutationManager = world.getSingleton(TransmutationManager.class);
            this.componentMapperManager = world.getSingleton(ComponentMapperManager.class);
        } catch (Exception ex) {
            throw new UnsupportedOperationException("Failed to get managers via reflection: " + ex.getMessage(), ex);
        }
    }

    public static void main(String[] args) throws Exception {
        var options = new OptionsBuilder()
                .include(benchmarkName(BitVectorBenchmark.ContainsBenchmarks.class))
                .include(benchmarkName(BitVectorBenchmark.IterationBenchmarks.class))
                .include(benchmarkName(EngineWorldBenchmark.AddEntityBenchmark.class))
                .include(benchmarkName(EngineWorldBenchmark.RemoveEntityBenchmark.class))
                .include(benchmarkName(EngineWorldBenchmark.AddComponentBenchmark.class))
                .include(benchmarkName(EngineWorldBenchmark.IterateCompositionBenchmark.class))
                .build();

        new Runner(options).run();
    }

}
