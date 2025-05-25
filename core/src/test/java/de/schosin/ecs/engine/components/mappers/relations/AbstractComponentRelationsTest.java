package de.schosin.ecs.engine.components.mappers.relations;

import de.schosin.ecs.api.components.Relation;
import de.schosin.ecs.api.components.Relation.ComponentRelation;
import de.schosin.ecs.api.components.mappers.Components;
import de.schosin.ecs.api.components.types.RelationComponentType.RegularComponentRelationType;

public class AbstractComponentRelationsTest extends AbstractRelationsTest {

    abstract class AbstractComponentRelationTest<R extends Enum<R>, T1 extends Enum<T1>, T2 extends Enum<T2>, M1 extends Components<ComponentRelation<R, T1>, ?>, M2 extends Components<ComponentRelation<R, T2>, ?>>
            extends AbstractRelationTest<R, R, RegularComponentRelationType<R, T1, ?>, RegularComponentRelationType<R, T2, ?>, ComponentRelation<R, T1>, M1, ComponentRelation<R, T2>, M2> {

        protected final Class<T1> target1Class;
        protected final Class<T2> target2Class;

        protected T1 target1;
        protected T2 target2;

        protected AbstractComponentRelationTest(RegularComponentRelationType<R, T1, ?> type1, RegularComponentRelationType<R, T2, ?> type2) {
            super(type1, type2);

            this.target1Class = type1.target();
            this.target2Class = type2.target();

            this.target1 = target1Class.getEnumConstants()[0];
            this.target2 = target2Class.getEnumConstants()[0];
        }

        @Override
        protected void add1(M1 mapper, int entityId, R relationship) {
            add(mapper1, entityId, relationship, target1);
        }

        @Override
        protected void add2(M2 mapper, int entityId, R relationship) {
            add(mapper2, entityId, relationship, target2);
        }

        protected abstract <RR, T> void add(Components<ComponentRelation<RR, T>, ?> mapper, int entityId, RR relationship, T target);

        @Override
        protected ComponentRelation<R, T1> getInstance1(R relationship) {
            return Relation.create(relationship, target1);
        }

        @Override
        protected ComponentRelation<R, T2> getInstance2(R relationship) {
            return Relation.create(relationship, target2);
        }

    }

}
