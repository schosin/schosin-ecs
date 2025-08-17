package de.schosin.ecs.plugins.composition.manager;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.schosin.ecs.api.components.mappers.ComponentMapper.PooledComponentMapper;
import de.schosin.ecs.plugins.composition.Composition;
import de.schosin.ecs.plugins.composition.manager.components.records.P1;
import de.schosin.ecs.plugins.composition.manager.components.records.P2;
import de.schosin.ecs.plugins.composition.manager.components.records.P3;

public class CreateEntityMutationsTest extends AbstractCompositionPluginTest {

    PooledComponentMapper<P1> pooled1;
    PooledComponentMapper<P2> pooled2;
    PooledComponentMapper<P3> pooled3;

    @BeforeEach
    void setupMappers() {
        this.pooled1 = world.getPooledComponents(P1.class);
        this.pooled2 = world.getPooledComponents(P2.class);
        this.pooled3 = world.getPooledComponents(P3.class);
    }

    @Test
    void testDeletionDuringCreation() {
        // Setup composition listeners
        var composition1 = world.createComposition(Composition.all(P1.class));
        composition1.inserted(pooled2::add);

        var composition2 = world.createComposition(Composition.all(P2.class));
        composition2.inserted(pooled3::add);

        var composition3 = world.createComposition(Composition.all(P3.class));
        composition3.inserted(world::deleteEntity);

        // Call
        var c1 = pooled1.getInstance();
        var entityId = world.createEntity(c1);

        // Verify
        assertThat(world.isActive(entityId)).isTrue();

        world.process();
        assertThat(world.isActive(entityId)).isFalse();
    }

    @Test
    void testMutationDuringCreation_WhenListenersModifyComponents_ListenersCalledRecursivly() {
        // Setup composition listeners
        var composition1 = world.createComposition(Composition.all(P1.class));
        composition1.inserted(pooled2::add);

        var composition2 = world.createComposition(Composition.all(P2.class));
        composition2.inserted(pooled3::add);

        var composition3 = world.createComposition(Composition.all(P3.class));
        composition3.inserted(pooled1::remove);

        // Call
        var entityId = world.createEntity(pooled1.getInstance());

        // Verify
        verifyHasComposition(entityId, Composition.all(P2.class, P3.class).none(P1.class));

        verifyDoesNotHaveComponents(entityId, P1.class);
        verifyHasComponents(entityId, P2.class, P3.class);
    }

    @Test
    void testMutationAfterCreation_WhenListenersModifyComponents_WorksIfWorldIsProcessed() {
        // Setup composition listeners
        var composition1 = world.createComposition(Composition.all(P1.class));
        composition1.inserted(entityId -> {
            pooled2.add(entityId);

            verifyHasComposition(entityId, Composition.all(P1.class).none(P2.class, P3.class));

            verifyHasComponents(entityId, P1.class, P2.class);
            verifyDoesNotHaveComponents(entityId, P3.class);
        });

        var composition2 = world.createComposition(Composition.all(P2.class));
        composition2.inserted(entityId -> {
            pooled3.add(entityId);

            verifyHasComposition(entityId, Composition.all(P1.class, P2.class).none(P3.class));
            verifyHasComponents(entityId, P1.class, P2.class, P3.class);
        });

        var composition3 = world.createComposition(Composition.all(P3.class));
        composition3.inserted(entityId -> {
            pooled1.remove(entityId);

            verifyHasComposition(entityId, Composition.all(P1.class, P2.class, P3.class));
            verifyHasComponents(entityId, P1.class, P2.class, P3.class);
        });

        var entityId = world.createEntity();

        // Call
        pooled1.add(entityId);
        world.process();

        // Verify
        verifyHasComposition(entityId, Composition.all(P2.class, P3.class).none(P1.class));

        verifyHasComponents(entityId, P2.class, P3.class);
        verifyDoesNotHaveComponents(entityId, P1.class);
    }

    @Test
    void testMutationAfterCreation_WhenListenersModifyComponents_ListenersNotCalledRecirsuvly() {
        // Setup composition listeners
        var composition1 = world.createComposition(Composition.all(P1.class));
        composition1.inserted(pooled2::add);

        var composition2 = world.createComposition(Composition.all(P2.class));
        composition2.inserted(pooled3::add);

        var composition3 = world.createComposition(Composition.all(P3.class));
        composition3.inserted(pooled1::remove);

        var entityId = world.createEntity();

        // Call
        pooled1.add(entityId);

        // Verify
        verifyHasComposition(entityId, Composition.none(P1.class, P2.class, P3.class));

        verifyHasComponents(entityId, P1.class);
        verifyDoesNotHaveComponents(entityId, P2.class);
        verifyDoesNotHaveComponents(entityId, P3.class);
    }

}
