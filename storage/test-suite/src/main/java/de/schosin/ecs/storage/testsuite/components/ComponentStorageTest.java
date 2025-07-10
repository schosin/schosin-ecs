package de.schosin.ecs.storage.testsuite.components;

import static de.schosin.ecs.plugins.wildcards.types.WildcardType.WILDCARD;
import static de.schosin.ecs.plugins.wildcards.types.WildcardType.wildcard;
import static de.schosin.ecs.plugins.wildcards.types.WildcardType.wildcardRelation;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import de.schosin.ecs.api.Pooled;
import de.schosin.ecs.api.components.Relation.Exclusive;
import de.schosin.ecs.api.components.types.ClassType;
import de.schosin.ecs.api.components.types.RelationComponentType.ComponentRelationType;
import de.schosin.ecs.api.components.types.RelationComponentType.EntityRelationType;
import de.schosin.ecs.api.components.types.RelationComponentType.ExclusiveComponentRelationType;
import de.schosin.ecs.api.components.types.RelationComponentType.ExclusiveEntityRelationType;
import de.schosin.ecs.plugins.wildcards.types.WildcardClassType;
import de.schosin.ecs.storage.api.components.Component.ComponentData;
import de.schosin.ecs.storage.api.components.Component.PooledComponentData;
import de.schosin.ecs.storage.testsuite.AbstractStorageEngineTest;
import de.schosin.ecs.utils.collections.Bag;

public class ComponentStorageTest extends AbstractStorageEngineTest {

    @Nested
    class GetComponentTest {

        @Test
        void testByClassType() {
            var instance = engine.getComponent(new ClassType<>(C1.class));

            assertThat(instance).as("returned instance is not null").isNotNull();
            assertThat(instance).as("non-pooled ClassType must return ComponentData").isInstanceOf(ComponentData.class);
        }

        @Test
        void testByClassType_PooledComponent() {
            var instance = engine.getComponent(new ClassType<>(P1.class));

            assertThat(instance).as("returned instance is not null").isNotNull();
            assertThat(instance).as("pooled ClassType must return PooledComponentData").isInstanceOf(PooledComponentData.class);
        }

        @Test
        void testById() {
            var instance = engine.getComponent(new ClassType<>(C1.class));

            var result = engine.getComponent(instance.id());
            assertThat(result).as("returned instance is same as previously created instance").isSameAs(instance);
        }

    }

    @Nested
    class GetPooledComponentTest {

        @Test
        void testByClassType() {
            var instance = engine.getPooledComponent(new ClassType<>(P1.class));

            assertThat(instance).as("returned instance is not null").isNotNull();
        }

        @Test
        void testById() {
            var instance = engine.getPooledComponent(new ClassType<>(P1.class));

            var result = engine.getComponent(instance.id());
            assertThat(result).as("returned instance is same as previously created instance").isSameAs(instance);
        }

    }

    @Nested
    class ComponentInstanceReusedTest {

        @Test
        void testInstanceReused_SameComponentTypes() {
            var componentType = new ClassType<>(C1.class);
            var instance1 = engine.getComponent(componentType);
            var instance2 = engine.getComponent(componentType);

            assertThat(instance2).as("getComponent returns same instance for same componentType").isSameAs(instance1);
        }

        @Test
        void testInstanceReused_EqualComponentTypes() {
            var instance1 = engine.getComponent(new ClassType<>(C1.class));
            var instance2 = engine.getComponent(new ClassType<>(C1.class));

            assertThat(instance2).as("getComponent returns same instance for equal componentTypes").isSameAs(instance1);
        }

        @Test
        void testPooledInstanceReused_SameComponentTypes() {
            var componentType = new ClassType<>(P1.class);
            var instance1 = engine.getComponent(componentType);
            var instance2 = engine.getComponent(componentType);
            var instance3 = engine.getPooledComponent(componentType);

            assertThat(instance2).as("getComponent returns same instance for same componentType").isSameAs(instance1);
            assertThat(instance3).as("getPooledComponent returns same instance for same componentType").isSameAs(instance1);
        }

        @Test
        void testPooledInstanceReused_EqualComponentTypes() {
            var instance1 = engine.getComponent(new ClassType<>(P1.class));
            var instance2 = engine.getComponent(new ClassType<>(P1.class));
            var instance3 = engine.getPooledComponent(new ClassType<>(P1.class));

            assertThat(instance2).as("getComponent returns same instance for equal componentTypes").isSameAs(instance1);
            assertThat(instance3).as("getPooledComponent returns same instance for equal componentTypes").isSameAs(instance1);
        }

    }

    @Nested
    class GetComponentsTest {

        @Test
        void testInitiallyEmpty() {
            var components = engine.getComponents();

            assertThat(components).as("result must never be null").isNotNull();
            assertThat(components.isEmpty()).as("result must be empty if no components added").isTrue();
        }

        @Test
        void testSameInstances() {
            var instance1 = engine.getComponent(new ClassType<>(C1.class));
            var instance2 = engine.getComponent(new ClassType<>(C2.class));

            var components = engine.getComponents();

            assertThat(components.getSize()).as("must return all previously added components in any order").isEqualTo(2);
            assertThat(components.get(0)).as("must return all previously added components in any order").isIn(instance1, instance2);
            assertThat(components.get(1)).as("must return all previously added components in any order").isIn(instance1, instance2);
        }

        @Test
        void testDoesNotContainDuplicates() {
            var instance1 = engine.getComponent(new ClassType<>(C1.class));
            engine.getComponent(new ClassType<>(C1.class));

            var components = engine.getComponents();

            assertThat(components.getSize()).as("must not return duplicates if same component retrieved multiple times").isEqualTo(1);
            assertThat(components.get(0)).as("must not return duplicates if same component retrieved multiple times").isIn(instance1);
        }

        @Test
        void testLiveCollection() {
            var components = engine.getComponents();

            var instance1 = engine.getComponent(new ClassType<>(C1.class));
            var instance2 = engine.getComponent(new ClassType<>(C2.class));

            assertThat(components.getSize()).as("must be updated when components added afterwards").isEqualTo(2);
            assertThat(components.get(0)).as("must be updated when components added afterwards").isIn(instance1, instance2);
            assertThat(components.get(1)).as("must be updated when components added afterwards").isIn(instance1, instance2);
        }

        @Test
        void testImmutableBagType() {
            engine.getComponent(new ClassType<>(C1.class));
            engine.getComponent(new ClassType<>(C2.class));

            var components1 = engine.getComponents();
            assertThat(components1).as("must not be a regular Bag").isNotInstanceOf(Bag.class);
        }

        @Test
        void testImmutableBagType_WhenEmpty() {
            var components1 = engine.getComponents();
            assertThat(components1).as("must not be a regular Bag").isNotInstanceOf(Bag.class);
        }

    }

    @Nested
    class GetBoundComponentsTest {

        final WildcardClassType<Bound> bound = wildcard(Bound.class);

        @Test
        void testInitiallyEmpty() {
            var components = engine.getComponents(bound);

            assertThat(components).as("result must never be null").isNotNull();
            assertThat(components.isEmpty()).as("result must be empty if no components added").isTrue();
        }

        @Test
        void testResult_WhenEqualType_ReturnsSameInstance() {
            var components = engine.getComponents(wildcard(Bound.class));

            assertThat(engine.getComponents(wildcard(Bound.class))).as("must return same instance for equal bounds").isSameAs(components);
        }

        @Test
        void testResult_WhenSameType_ReturnsSameInstance() {
            var components = engine.getComponents(bound);

            assertThat(engine.getComponents(bound)).as("must return same instance for same bounds").isSameAs(components);
        }

        @Test
        void testResult_WhenDifferentType_ReturnsDifferentInstance() {
            var components = engine.getComponents(bound);

            assertThat(WILDCARD).as("must return different instance for different bounds").isNotSameAs(components);
        }

        @Test
        void testMultipleCallsReturnSameImmutableBag() {
            var components = engine.getComponents(wildcard(Bound.class));

            assertThat(engine.getComponents(wildcard(Bound.class))).as("must return same instance for equal wildcards").isSameAs(components);
        }

        @Test
        void testComponentsSameInstances() {
            var instance1 = engine.getComponent(new ClassType<>(C1.class));
            var instance2 = engine.getComponent(new ClassType<>(C2.class));
            engine.getComponent(new ClassType<>(C3.class));

            var components = engine.getComponents(bound);

            assertThat(components.getSize()).as("must return all previously added components in any order").isEqualTo(2);
            assertThat(components.get(0)).as("must return all previously added components in any order").isIn(instance1, instance2);
            assertThat(components.get(1)).as("must return all previously added components in any order").isIn(instance1, instance2);
        }

        @Test
        void testDoesNotContainDuplicates() {
            var instance1 = engine.getComponent(new ClassType<>(C1.class));
            engine.getComponent(new ClassType<>(C1.class));

            var components = engine.getComponents(bound);

            assertThat(components.getSize()).as("must not return duplicates if same component retrieved multiple times").isEqualTo(1);
            assertThat(components.get(0)).as("must not return duplicates if same component retrieved multiple times").isIn(instance1);
        }

        @Test
        void testLiveCollection() {
            var components = engine.getComponents(bound);

            var instance1 = engine.getComponent(new ClassType<>(C1.class));
            var instance2 = engine.getComponent(new ClassType<>(C2.class));
            engine.getComponent(new ClassType<>(C3.class));

            assertThat(components)
                    .as("must be updated when components added afterwards").hasSize(2)
                    .as("must be updated when components added afterwards").anySatisfy(component -> assertThat(component).isSameAs(instance1))
                    .as("must be updated when components added afterwards").anySatisfy(component -> assertThat(component).isSameAs(instance2));
        }

        @Test
        void testLiveCollection_ObjectBound() {
            var components = engine.getComponents(wildcard(Object.class));

            var instance1 = engine.getComponent(new ClassType<>(C1.class));
            var instance2 = engine.getComponent(new ClassType<>(C2.class));
            var instance3 = engine.getComponent(new ClassType<>(C3.class));

            assertThat(components)
                    .as("must be updated when components added afterwards").hasSize(3)
                    .as("must be updated when components added afterwards").anySatisfy(component -> assertThat(component).isSameAs(instance1))
                    .as("must be updated when components added afterwards").anySatisfy(component -> assertThat(component).isSameAs(instance2))
                    .as("must be updated when components added afterwards").anySatisfy(component -> assertThat(component).isSameAs(instance3));
        }

        @Test
        void testLiveCollection_ObjectBound_Constant() {
            var components = engine.getComponents(WILDCARD);

            var instance1 = engine.getComponent(new ClassType<>(C1.class));
            var instance2 = engine.getComponent(new ClassType<>(C2.class));
            var instance3 = engine.getComponent(new ClassType<>(C3.class));

            assertThat(components)
                    .as("must be updated when components added afterwards").hasSize(3)
                    .as("must be updated when components added afterwards").anySatisfy(component -> assertThat(component).isSameAs(instance1))
                    .as("must be updated when components added afterwards").anySatisfy(component -> assertThat(component).isSameAs(instance2))
                    .as("must be updated when components added afterwards").anySatisfy(component -> assertThat(component).isSameAs(instance3));
        }

        @Test
        void testImmutableBagType() {
            engine.getComponent(new ClassType<>(C1.class));
            engine.getComponent(new ClassType<>(C2.class));
            engine.getComponent(new ClassType<>(C3.class));

            var components1 = engine.getComponents(bound);
            assertThat(components1).as("must not be a regular Bag").isNotInstanceOf(Bag.class);
        }

        @Test
        void testImmutableBagType_WhenEmpty() {
            var components1 = engine.getComponents(bound);
            assertThat(components1).as("must not be a regular Bag").isNotInstanceOf(Bag.class);
        }

        @Test
        void testComponentRelationWildcards() {
            var components = engine.getComponents(wildcardRelation(Bound.class, Bound.class));

            var relation11 = engine.getComponent(new ComponentRelationType<>(C1.class, C1.class));
            var relation12 = engine.getComponent(new ComponentRelationType<>(C1.class, C2.class));
            var exclusive11 = engine.getComponent(new ExclusiveComponentRelationType<>(Exclusive1.class, C1.class));

            engine.getComponent(new ComponentRelationType<>(C1.class, C3.class));
            engine.getComponent(new ExclusiveComponentRelationType<>(Exclusive1.class, C3.class));
            engine.getComponent(new ExclusiveComponentRelationType<>(Exclusive2.class, C2.class));

            assertThat(components)
                    .as("must be updated when components added afterwards").hasSize(3)
                    .as("must be updated when components added afterwards").anySatisfy(component -> assertThat(component).isSameAs(relation11))
                    .as("must be updated when components added afterwards").anySatisfy(component -> assertThat(component).isSameAs(relation12))
                    .as("must be updated when components added afterwards").anySatisfy(component -> assertThat(component).isSameAs(exclusive11));
        }

        @Test
        void testComponentRelationRelationshipWildcard() {
            var components = engine.getComponents(wildcardRelation(Object.class, C2.class));

            var relation12 = engine.getComponent(new ComponentRelationType<>(C1.class, C2.class));
            var exclusive22 = engine.getComponent(new ExclusiveComponentRelationType<>(Exclusive2.class, C2.class));

            engine.getComponent(new ComponentRelationType<>(C1.class, C1.class));
            engine.getComponent(new ComponentRelationType<>(C1.class, C3.class));
            engine.getComponent(new ExclusiveComponentRelationType<>(Exclusive1.class, C1.class));
            engine.getComponent(new ExclusiveComponentRelationType<>(Exclusive1.class, C3.class));

            assertThat(components)
                    .as("must be updated when components added afterwards").hasSize(2)
                    .as("must be updated when components added afterwards").anySatisfy(component -> assertThat(component).isSameAs(relation12))
                    .as("must be updated when components added afterwards").anySatisfy(component -> assertThat(component).isSameAs(exclusive22));
        }

        @Test
        void testComponentRelationTargetWildcard() {
            var components = engine.getComponents(wildcardRelation(C1.class, Bound.class));

            var relation11 = engine.getComponent(new ComponentRelationType<>(C1.class, C1.class));
            var relation12 = engine.getComponent(new ComponentRelationType<>(C1.class, C2.class));

            engine.getComponent(new ComponentRelationType<>(C1.class, C3.class));
            engine.getComponent(new ExclusiveComponentRelationType<>(Exclusive2.class, C2.class));
            engine.getComponent(new ExclusiveComponentRelationType<>(Exclusive1.class, C1.class));
            engine.getComponent(new ExclusiveComponentRelationType<>(Exclusive1.class, C3.class));

            assertThat(components)
                    .as("must be updated when components added afterwards").hasSize(2)
                    .as("must be updated when components added afterwards").anySatisfy(component -> assertThat(component).isSameAs(relation11))
                    .as("must be updated when components added afterwards").anySatisfy(component -> assertThat(component).isSameAs(relation12));
        }

        @Test
        void testEntityRelationWildcard() {
            var components = engine.getComponents(wildcardRelation(Bound.class));

            var relation1 = engine.getComponent(new EntityRelationType<>(C1.class));
            var relation2 = engine.getComponent(new EntityRelationType<>(C2.class));
            var exclusive1 = engine.getComponent(new ExclusiveEntityRelationType<>(Exclusive1.class));

            engine.getComponent(new EntityRelationType<>(C3.class));
            engine.getComponent(new ExclusiveEntityRelationType<>(Exclusive2.class));

            assertThat(components)
                    .as("must be updated when components added afterwards").hasSize(3)
                    .as("must be updated when components added afterwards").anySatisfy(component -> assertThat(component).isSameAs(relation1))
                    .as("must be updated when components added afterwards").anySatisfy(component -> assertThat(component).isSameAs(relation2))
                    .as("must be updated when components added afterwards").anySatisfy(component -> assertThat(component).isSameAs(exclusive1));
        }

    }

    interface Bound {
    }

    record C1() implements Bound {
    }

    record C2() implements Bound {
    }

    record C3() {
    }

    record Exclusive1() implements Exclusive, Bound {
    }

    record Exclusive2() implements Exclusive {
    }

    record P1() implements Pooled {
    }

}
