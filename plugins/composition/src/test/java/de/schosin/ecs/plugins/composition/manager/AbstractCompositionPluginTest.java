package de.schosin.ecs.plugins.composition.manager;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;

import de.schosin.ecs.plugins.composition.Composition;
import de.schosin.ecs.plugins.composition.Composition.Builder;
import de.schosin.ecs.plugins.composition.manager.components.records.C1;
import de.schosin.ecs.plugins.composition.manager.components.records.C2;
import de.schosin.ecs.test.AbstractEcsTest;
import de.schosin.ecs.utils.collections.BitVector;

public abstract class AbstractCompositionPluginTest extends AbstractEcsTest<CompositionWorld> {

    protected static final BitVector EMPTY_VECTOR = new BitVector();

    protected static final Builder EMPTY = Composition.all();

    protected CompositionManager compositionManager;

    protected int component1Id;
    protected int C2Id;

    @BeforeEach
    void setup() {
        this.compositionManager = world.getSingleton(CompositionManager.class);

        this.component1Id = componentManager.getComponent(component(C1.class)).id();
        this.C2Id = componentManager.getComponent(component(C2.class)).id();
    }

    protected void verifyHasComposition(int entityId, Composition.Builder builder) {
        var composition = world.createComposition(builder);
        assertThat(composition.isInterested(entityId)).isTrue();
    }

}
