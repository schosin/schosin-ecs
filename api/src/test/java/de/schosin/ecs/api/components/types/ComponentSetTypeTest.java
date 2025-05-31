package de.schosin.ecs.api.components.types;

import static de.schosin.ecs.api.components.types.ComponentType.component;
import static de.schosin.ecs.api.components.types.ComponentType.componentSet;
import static de.schosin.ecs.api.components.types.ComponentType.exclusiveRelation;
import static de.schosin.ecs.api.components.types.ComponentType.relation;
import static de.schosin.ecs.api.components.types.ComponentType.wildcard;
import static de.schosin.ecs.api.components.types.ComponentType.wildcardRelation;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;

class ComponentSetTypeTest extends AbstractComponentTypeTest<ComponentSetTypeTest.MatchesTestCases> {

    public ComponentSetTypeTest() {
        super(MatchesTestCases.class);
    }

    enum MatchesTestCases implements AbstractComponentTypeTest.MatchesTestCase {

        equalClassType(componentSet(MyComponentSet.class, MyComponentSet.Processor.class), component(Component.class), false),
        otherClassType(componentSet(MyComponentSet.class, MyComponentSet.Processor.class), component(FinalComponent.class), false),
        componentRelation(componentSet(MyComponentSet.class, MyComponentSet.Processor.class), relation(RelationshipComponent.class, TargetComponent.class), false),
        exclusiveComponentRelation(componentSet(MyComponentSet.class, MyComponentSet.Processor.class), exclusiveRelation(ExclusiveComponent.class, TargetComponent.class), false),
        entityRelation(componentSet(MyComponentSet.class, MyComponentSet.Processor.class), relation(EntityRelationshipComponent.class), false),
        exclusiveEntityRelation(componentSet(MyComponentSet.class, MyComponentSet.Processor.class), exclusiveRelation(ExclusiveEntityRelationship.class), false),
        wildcardObject(componentSet(MyComponentSet.class, MyComponentSet.Processor.class), wildcard(Object.class), false),
        componentSet(componentSet(MyComponentSet.class, MyComponentSet.Processor.class), componentSet(MyComponentSet.class, MyComponentSet.Processor.class), true),
        otherComponentSet(componentSet(MyComponentSet.class, MyComponentSet.Processor.class), componentSet(OtherComponentSet.class, OtherComponentSet.Processor.class), false),
        entityFetch(componentSet(MyComponentSet.class, MyComponentSet.Processor.class), relation(EntityRelationshipComponent.class, FETCH), false),
        exclusiveEntityFetch(componentSet(MyComponentSet.class, MyComponentSet.Processor.class), exclusiveRelation(ExclusiveEntityRelationship.class, FETCH), false),
        wildcardComponentRelation(componentSet(MyComponentSet.class, MyComponentSet.Processor.class), wildcardRelation(Object.class, Object.class), false),
        wildcardEntityRelation(componentSet(MyComponentSet.class, MyComponentSet.Processor.class), wildcardRelation(Object.class), false),
        wildcardEntityFetchRelation(componentSet(MyComponentSet.class, MyComponentSet.Processor.class), wildcardRelation(Object.class, component(Component.class)), false);

        private final ComponentSetType<?, ?> type;
        private final ComponentType<?, ?> otherType;
        private final boolean matches;

        private MatchesTestCases(ComponentSetType<?, ?> type, ComponentType<?, ?> otherType, boolean matches) {
            this.type = type;
            this.otherType = otherType;
            this.matches = matches;
        }

        @Override
        public ComponentType<?, ?> type() {
            return type;
        }

        @Override
        public ComponentType<?, ?> otherType() {
            return otherType;
        }

        @Override
        public boolean matches() {
            return matches;
        }

    }

    @Test
    void testToString() {
        assertThat(componentSet(MyComponentSet.class, MyComponentSet.Processor.class))
                .extracting(Object::toString, InstanceOfAssertFactories.STRING)
                .containsSubsequence("ComponentSetType", MyComponentSet.class.getSimpleName());
    }

    @Test
    void testComponentSet() {
        assertThatCode(() -> new ComponentSetType<>(MyComponentSet.class, MyComponentSet.Processor.class)).doesNotThrowAnyException();
    }

}
