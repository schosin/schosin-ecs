package de.schosin.ecs.plugins.composition.manager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;

import de.schosin.ecs.api.components.Relation;
import de.schosin.ecs.plugins.composition.Composition;
import de.schosin.ecs.plugins.composition.manager.components.records.C1;
import de.schosin.ecs.plugins.composition.manager.components.records.C2;
import de.schosin.ecs.plugins.composition.manager.components.records.C3;
import de.schosin.ecs.plugins.composition.manager.components.records.C4;
import de.schosin.ecs.plugins.composition.manager.components.records.ExclusiveRelationship;
import de.schosin.ecs.plugins.composition.manager.components.records.RelationshipComponent;
import de.schosin.ecs.plugins.composition.manager.components.records.Target;

public class ComponentSetTest extends AbstractCompositionPluginTest {

    @Test
    void testRetrieveComponentSet() {
        var componentSet = MyComponentSet.TYPE;
        var composition = world.createComposition(Composition.all(C1.class), componentSet);

        var target1 = world.createEntity();
        var target2 = world.createEntity();

        var entityId = world.createEntity(new C1(),
                Relation.create(new ExclusiveRelationship(1), new Target(10)),
                Relation.create(new RelationshipComponent(2), new Target(20)),
                Relation.create(new RelationshipComponent(3), new Target(30)),
                Relation.create(new ExclusiveRelationship(4), target1),
                Relation.create(new RelationshipComponent(5), target1),
                Relation.create(new RelationshipComponent(6), target2),
                new C2(), new C3(), new C4());

        var processed = new AtomicBoolean(false);
        composition.process((id, c1, componentRelation, componentRelations, entityRelation, entityRelations, c1234) -> {
            assertThat(id).isEqualTo(entityId);

            assertThat(c1).isNotNull();
            assertThat(componentRelation).extracting("relationship.value", "target.value").contains(1, 10);
            assertThat(componentRelations).extracting("relationship.value", "target.value").containsExactlyInAnyOrder(tuple(2, 20), tuple(3, 30));
            assertThat(entityRelation).extracting("relationship.value", "target").contains(4, target1);
            assertThat(entityRelations).extracting("relationship.value", "target").containsExactlyInAnyOrder(tuple(5, target1), tuple(6, target2));
            assertThat(c1234).hasSize(4);

            processed.set(true);
        });

        assertThat(processed.get()).isTrue();
    }

}
