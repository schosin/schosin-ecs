package de.schosin.ecs.engine.components.mappers.relations;

import org.junit.jupiter.api.BeforeEach;

import de.schosin.ecs.api.components.Relation;
import de.schosin.ecs.api.components.Relation.EntityRelation;
import de.schosin.ecs.api.components.mappers.Components;
import de.schosin.ecs.api.components.types.RelationComponentType.RegularEntityRelationType;

public abstract class AbstractEntityRelationsTest extends AbstractRelationsTest {

    abstract class AbstractEntityRelationTest<R1 extends Enum<R1>, R2 extends Enum<R2>, M1 extends Components<EntityRelation<R1>, ?>, M2 extends Components<EntityRelation<R2>, ?>>
            extends AbstractRelationTest<R1, R2, RegularEntityRelationType<R1, ?>, RegularEntityRelationType<R2, ?>, EntityRelation<R1>, M1, EntityRelation<R2>, M2> {

        protected int target1;
        protected int target2;

        protected AbstractEntityRelationTest(RegularEntityRelationType<R1, ?> type1, RegularEntityRelationType<R2, ?> type2) {
            super(type1, type2);
        }

        @BeforeEach
        void setupTargets() {
            this.target1 = world.createEntity();
            this.target2 = world.createEntity();
        }

        @Override
        protected void add1(M1 mapper, int entityId, R1 relationship) {
            add(mapper1, entityId, relationship, target1);
        }

        @Override
        protected void add2(M2 mapper, int entityId, R2 relationship) {
            add(mapper2, entityId, relationship, target2);
        }

        protected abstract <RR> void add(Components<EntityRelation<RR>, ?> mapper, int entityId, RR relationship, int target);

        @Override
        protected EntityRelation<R1> getInstance1(R1 relationship) {
            return Relation.create(relationship, target1);
        }

        @Override
        protected EntityRelation<R2> getInstance2(R2 relationship) {
            return Relation.create(relationship, target2);
        }

    }
    
}
