package de.schosin.ecs.api.components.types;

import static de.schosin.ecs.api.components.types.ComponentType.WILDCARD;
import static de.schosin.ecs.api.components.types.ComponentType.component;
import static de.schosin.ecs.api.components.types.ComponentType.componentSet;
import static de.schosin.ecs.api.components.types.ComponentType.exclusiveRelation;
import static de.schosin.ecs.api.components.types.ComponentType.relation;
import static de.schosin.ecs.api.components.types.ComponentType.wildcard;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import de.schosin.ecs.api.components.types.AbstractComponentTypeTest.Component;
import de.schosin.ecs.api.components.types.AbstractComponentTypeTest.ComponentInterface;
import de.schosin.ecs.api.components.types.AbstractComponentTypeTest.ExclusiveComponent;
import de.schosin.ecs.api.components.types.AbstractComponentTypeTest.MyComponentSet;
import de.schosin.ecs.api.components.types.AbstractComponentTypeTest.RelationshipComponent;
import de.schosin.ecs.api.components.types.AbstractComponentTypeTest.TargetComponent;
import de.schosin.ecs.api.components.types.RelationComponentType.ComponentRelationType;
import de.schosin.ecs.api.components.types.RelationComponentType.EntityRelationType;
import de.schosin.ecs.api.components.types.RelationComponentType.ExclusiveComponentRelationType;
import de.schosin.ecs.api.components.types.RelationComponentType.ExclusiveEntityRelationType;

class ComponentTypeTest {

    @Nested
    class FactoryMethodsTest {

        @Test
        void testComponent() {
            assertThat(component(Component.class)).as("must not be refactored to something else").isInstanceOf(ClassType.class);
        }

        @Test
        void testRelation() {
            assertThat(relation(RelationshipComponent.class, TargetComponent.class)).as("must not be refactored to something else").isInstanceOf(ComponentRelationType.class);
        }

        @Test
        void testExclusiveRelation() {
            assertThat(exclusiveRelation(ExclusiveComponent.class, TargetComponent.class)).as("must not be refactored to something else").isInstanceOf(ExclusiveComponentRelationType.class);
        }

        @Test
        void testEntityRelation() {
            assertThat(relation(RelationshipComponent.class)).as("must not be refactored to something else").isInstanceOf(EntityRelationType.class);
        }

        @Test
        void testEntityExclusiveRelation() {
            assertThat(exclusiveRelation(ExclusiveComponent.class)).as("must not be refactored to something else").isInstanceOf(ExclusiveEntityRelationType.class);
        }

        @Test
        void testComponentSet() {
            assertThat(componentSet(MyComponentSet.class)).as("must not be refactored to something else").isInstanceOf(ComponentSetType.class);
        }

        @Test
        void testWildcard() {
            assertThat(wildcard(ComponentInterface.class)).as("must not be refactored to something else").isInstanceOf(Wildcard.class);
        }

        @Test
        void testWildcardConstant() {
            assertThat(WILDCARD).as("must not be refactored to something else").isInstanceOf(Wildcard.class);
        }

    }

}
