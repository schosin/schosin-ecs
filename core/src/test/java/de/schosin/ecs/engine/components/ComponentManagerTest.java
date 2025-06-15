package de.schosin.ecs.engine.components;

import static de.schosin.ecs.api.components.types.ComponentType.wildcard;
import static de.schosin.ecs.api.components.types.ComponentType.wildcardRelation;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import de.schosin.ecs.api.components.ComponentSetConfig;
import de.schosin.ecs.api.components.Relation.ComponentRelation;
import de.schosin.ecs.api.components.Relation.EntityRelation;
import de.schosin.ecs.api.components.Relation.Exclusive;
import de.schosin.ecs.api.components.Relations.ComponentRelations;
import de.schosin.ecs.api.components.Relations.EntityRelations;
import de.schosin.ecs.api.components.Result.ComponentResult;
import de.schosin.ecs.api.components.types.ClassType;
import de.schosin.ecs.api.components.types.ComponentType.RegularComponentType;
import de.schosin.ecs.api.components.types.RelationComponentType.ComponentRelationType;
import de.schosin.ecs.api.components.types.RelationComponentType.EntityRelationType;
import de.schosin.ecs.api.components.types.RelationComponentType.ExclusiveComponentRelationType;
import de.schosin.ecs.api.components.types.RelationComponentType.ExclusiveEntityRelationType;
import de.schosin.ecs.api.components.types.RelationFetchType.EntityRelationFetchType;
import de.schosin.ecs.api.components.types.WildcardRelationType.WildcardEntityRelationFetchType;
import de.schosin.ecs.engine.AbstractWorldTest;

public class ComponentManagerTest extends AbstractWorldTest {

    @Nested
    class GetRegularComponentTypesTest {

        @ParameterizedTest
        @MethodSource("regularComponentTypes")
        void testRegularComponentType(RegularComponentType<?, ?> componentType) {
            var componentTypes = componentManager.getRegularComponentTypes(componentType);
            assertThat(componentTypes).containsExactly(componentType);
        }

        @Test
        void testEntityRelationFetchType() {
            var relation1 = componentManager.getComponent(new EntityRelationType<>(C1.class)).type();
            componentManager.getComponent(new EntityRelationType<>(C2.class)).type();

            var componentTypes = componentManager.getRegularComponentTypes(new EntityRelationFetchType<>(C1.class, component(C2.class)));
            assertThat(componentTypes).containsExactly(relation1);
        }

        @Test
        void testWildcard() {
            var class1 = componentManager.getComponent(new ClassType<>(C1.class)).type();
            var class2 = componentManager.getComponent(new ClassType<>(C2.class)).type();
            componentManager.getComponent(new ClassType<>(C3.class)).type();

            var componentTypes = componentManager.getRegularComponentTypes(wildcard(Bound.class));
            assertThat(componentTypes).containsExactlyInAnyOrder(class1, class2);
        }

        @Test
        void testComponentRelationWildcards() {
            var relation11 = componentManager.getComponent(new ComponentRelationType<>(C1.class, C1.class)).type();
            var relation12 = componentManager.getComponent(new ComponentRelationType<>(C1.class, C2.class)).type();
            var exclusive11 = componentManager.getComponent(new ExclusiveComponentRelationType<>(Exclusive1.class, C1.class)).type();

            componentManager.getComponent(new ComponentRelationType<>(C1.class, C3.class));
            componentManager.getComponent(new ExclusiveComponentRelationType<>(Exclusive1.class, C3.class));
            componentManager.getComponent(new ExclusiveComponentRelationType<>(Exclusive2.class, C2.class));

            var componentTypes = componentManager.getRegularComponentTypes(wildcardRelation(Bound.class, Bound.class));
            assertThat(componentTypes).containsExactlyInAnyOrder(relation11, relation12, exclusive11);
        }

        @Test
        void testComponentRelationRelationshipWildcard() {
            var relation12 = componentManager.getComponent(new ComponentRelationType<>(C1.class, C2.class)).type();
            var exclusive22 = componentManager.getComponent(new ExclusiveComponentRelationType<>(Exclusive2.class, C2.class)).type();

            componentManager.getComponent(new ComponentRelationType<>(C1.class, C1.class));
            componentManager.getComponent(new ComponentRelationType<>(C1.class, C3.class));
            componentManager.getComponent(new ExclusiveComponentRelationType<>(Exclusive1.class, C1.class));
            componentManager.getComponent(new ExclusiveComponentRelationType<>(Exclusive1.class, C3.class));

            var componentTypes = componentManager.getRegularComponentTypes(wildcardRelation(Object.class, C2.class));
            assertThat(componentTypes).containsExactlyInAnyOrder(relation12, exclusive22);
        }

        @Test
        void testComponentRelationTargetWildcard() {
            var relation11 = componentManager.getComponent(new ComponentRelationType<>(C1.class, C1.class)).type();
            var relation12 = componentManager.getComponent(new ComponentRelationType<>(C1.class, C2.class)).type();

            componentManager.getComponent(new ComponentRelationType<>(C1.class, C3.class));
            componentManager.getComponent(new ExclusiveComponentRelationType<>(Exclusive2.class, C2.class));
            componentManager.getComponent(new ExclusiveComponentRelationType<>(Exclusive1.class, C1.class));
            componentManager.getComponent(new ExclusiveComponentRelationType<>(Exclusive1.class, C3.class));

            var componentTypes = componentManager.getRegularComponentTypes(wildcardRelation(C1.class, Bound.class));
            assertThat(componentTypes).containsExactlyInAnyOrder(relation11, relation12);
        }

        @Test
        void testEntityRelationWildcard() {
            var relation1 = componentManager.getComponent(new EntityRelationType<>(C1.class)).type();
            var relation2 = componentManager.getComponent(new EntityRelationType<>(C2.class)).type();
            var exclusive1 = componentManager.getComponent(new ExclusiveEntityRelationType<>(Exclusive1.class)).type();

            componentManager.getComponent(new EntityRelationType<>(C3.class));
            componentManager.getComponent(new ExclusiveEntityRelationType<>(Exclusive2.class));

            var componentTypes = componentManager.getRegularComponentTypes(wildcardRelation(Bound.class));
            assertThat(componentTypes).containsExactlyInAnyOrder(relation1, relation2, exclusive1);
        }

        @Test
        void testWildcardEntityRelationFetchType() {
            var relation1 = componentManager.getComponent(new EntityRelationType<>(C1.class)).type();
            var relation2 = componentManager.getComponent(new EntityRelationType<>(C2.class)).type();
            componentManager.getComponent(new EntityRelationType<>(C3.class)).type();

            var componentTypes = componentManager.getRegularComponentTypes(new WildcardEntityRelationFetchType<>(Bound.class, component(C2.class)));
            assertThat(componentTypes).containsExactlyInAnyOrder(relation1, relation2);
        }

        @Test
        void testComponentSet() {
            var componentTypes = componentManager.getRegularComponentTypes(SimpleComponentSet.TYPE);
            assertThat(componentTypes).containsExactlyInAnyOrder(
                    component(C3.class),
                    exclusiveRelation(Exclusive1.class, C2.class), relation(C1.class, C2.class),
                    exclusiveRelation(Exclusive1.class), relation(C1.class));
        }

        @Test
        void testComponentSet_NonRegularComponentTypes() {
            var componentTypes = componentManager.getRegularComponentTypes(ComplexComponentSet.TYPE);
            assertThat(componentTypes).isEmpty();
        }

        @Test
        void testComponentSet_NonRegularComponentTypes_KnownTypes() {
            var class1 = componentManager.getComponent(new ClassType<>(C1.class)).type();
            var class2 = componentManager.getComponent(new ClassType<>(C2.class)).type();
            componentManager.getComponent(new ClassType<>(C3.class)).type();

            var componentTypes = componentManager.getRegularComponentTypes(ComplexComponentSet.TYPE);
            assertThat(componentTypes).containsExactlyInAnyOrder(class1, class2);
        }

        @Test
        void testNestedComponentSet() {
            var class1 = componentManager.getComponent(new ClassType<>(C1.class)).type();
            var class2 = componentManager.getComponent(new ClassType<>(C2.class)).type();
            componentManager.getComponent(new ClassType<>(C3.class)).type();

            var componentTypes = componentManager.getRegularComponentTypes(NestedComponentSet.TYPE);
            assertThat(componentTypes).containsExactlyInAnyOrder(
                    component(C3.class),
                    exclusiveRelation(Exclusive1.class, C2.class), relation(C1.class, C2.class),
                    exclusiveRelation(Exclusive1.class), relation(C1.class),
                    class1, class2);
        }

        private static Stream<RegularComponentType<?, ?>> regularComponentTypes() {
            return Stream.of(
                    component(C1.class),
                    exclusiveRelation(Exclusive1.class, C2.class),
                    relation(C1.class, C2.class),
                    exclusiveRelation(Exclusive1.class),
                    relation(C1.class));
        }

        @SuppressWarnings("unused")
        @ComponentSetConfig("SimpleComponentSet")
        private void simpleComponentSet(int entityId, C3 c3,
                ComponentRelation<Exclusive1, C2> componentRelation, ComponentRelations<C1, C2> componentRelations,
                EntityRelation<Exclusive1> entityRelation, EntityRelations<C1> entityRelations) {
        }

        @SuppressWarnings("unused")
        @ComponentSetConfig("ComplexComponentSet")
        private void complexComponentSet(int entityId, ComponentResult<Bound> components) {
        }

        @SuppressWarnings("unused")
        @ComponentSetConfig("NestedComponentSet")
        private void nestedComponentSet(int entityId, SimpleComponentSet simpleSet, ComplexComponentSet complexSet) {
        }

    }

    public interface Bound {
    }

    public record C1() implements Bound {
    }

    public record C2() implements Bound {
    }

    public record C3() {
    }

    public record Exclusive1() implements Exclusive, Bound {
    }

    record Exclusive2() implements Exclusive {
    }

}
