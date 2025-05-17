package de.schosin.ecs.storage.testsuite.components.relations;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.function.BiConsumer;

import org.assertj.core.api.ObjectAssert;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import de.schosin.ecs.api.components.ComponentType.RegularComponentRelationType;
import de.schosin.ecs.api.components.ComponentType.RegularComponentType;
import de.schosin.ecs.api.components.Relation.ComponentRelation;
import de.schosin.ecs.storage.api.components.Component.ComponentRelationComponent;
import de.schosin.ecs.storage.testsuite.components.CommonComponentTest;

public abstract class CommonComponentRelationTest<R1, T1, X1, R2, T2, X2, R3, T3, X3>
        extends CommonComponentTest<ComponentRelation<R1, T1>, X1, ComponentRelation<R2, T2>, X2, ComponentRelation<R3, T3>, X3> {

    protected final BiConsumer<ObjectAssert<?>, ComponentRelation<?, ?>> verifyRelationInstance = verifyRelationInstance();

    @Override
    protected <T> T getInstance(RegularComponentType<T, ?> type) {
        return getInstance(type, 1, 1);
    }

    @SuppressWarnings("unchecked")
    protected <T> T getInstance(RegularComponentType<T, ?> type, int relationship, int target) {
        if (type.equals(type1())) {
            return (T) getInstance1(relationship, target);
        }
        if (type.equals(type2())) {
            return (T) getInstance2(relationship, target);
        }
        if (type.equals(type3())) {
            return (T) getInstance3(relationship, target);
        }

        throw new IllegalArgumentException("Unknown type: " + type);
    }

    protected abstract ComponentRelation<R1, T1> getInstance1(int relationship, int target);

    protected abstract ComponentRelation<R2, T2> getInstance2(int relationship, int target);

    protected abstract ComponentRelation<R3, T3> getInstance3(int relationship, int target);

    protected BiConsumer<ObjectAssert<?>, ComponentRelation<?, ?>> verifyRelationInstance() {
        return ObjectAssert::isEqualTo;
    }

    @Nested
    class ComponentRelationComponentTest extends AbstractTypeTest {

        @ParameterizedTest
        @MethodSource(TYPES)
        void testRelationshipClass(RegularComponentRelationType<?, ?, ?> type) {
            var component = getComponent(type);

            assertThat(component.relationshipClass()).as("relationshipClass matches type relationship").isEqualTo(type.relationship());
        }

        @ParameterizedTest
        @MethodSource(TYPES)
        void testTargetClass(RegularComponentRelationType<?, ?, ?> type) {
            var component = getComponent(type);

            assertThat(component.targetClass()).as("targetClass matches type target").isEqualTo(type.target());
        }

        @ParameterizedTest
        @MethodSource(TYPES)
        @SuppressWarnings({ "unchecked", "rawtypes" })
        void testAddRelation(RegularComponentRelationType<?, ?, ?> type) {
            var component = (ComponentRelationComponent) getComponent(type);
            var relation = getInstance(type);

            var entityId = world.createEntity();
            component.addRelation(entityId, relation.relationship(), relation.target());

            verifyRelationInstance.accept(assertThat(component.getComponent(entityId)).as("get returns equal relation after addRelation"), relation);
        }

        @ParameterizedTest
        @MethodSource(TYPES)
        @SuppressWarnings({ "unchecked", "rawtypes" })
        void testGetInstance(RegularComponentRelationType<?, ?, ?> type) {
            var component = (ComponentRelationComponent) getComponent(type);
            var instance = getInstance(type);

            var relation = component.getInstance(instance.relationship(), instance.target());
            assertThat(relation).as("getInstance returns an equal relation").isEqualTo(instance);
            assertThat(relation.relationship()).as("getInstance returns the same relationship").isSameAs(instance.relationship());
            assertThat(relation.target()).as("getInstance returns the same target").isSameAs(instance.target());
        }

        @ParameterizedTest
        @MethodSource(TYPES)
        @SuppressWarnings({ "unchecked", "rawtypes" })
        void testGetInstance_ReusedAfterFreed(RegularComponentRelationType<?, ?, ?> type) {
            var component = (ComponentRelationComponent) getComponent(type);
            var instance = getInstance(type);

            var relation = component.getInstance(instance.relationship(), instance.target());
            var entityId = world.createEntity(relation);

            world.deleteEntity(entityId);
            world.process();

            var reusedRelation = component.getInstance(instance.relationship(), instance.target());
            assertThat(reusedRelation).as("relation instances should be reused if owning entity deleted").isSameAs(relation);
        }

    }

    @Nested
    class DisplayTest extends AbstractTypeTest {

        @ParameterizedTest
        @MethodSource(TYPES)
        void testDisplayIncludesRelationshipSimpleName(RegularComponentRelationType<?, ?, ?> type) {
            assertThat(getComponent(type).display()).as("component.display() must contain type.relationship().getSimpleName()").contains(type.relationship().getSimpleName());
        }

        @ParameterizedTest
        @MethodSource(TYPES)
        void testDisplayIncludesTargetSimpleName(RegularComponentRelationType<?, ?, ?> type) {
            assertThat(getComponent(type).display()).as("component.display() must contain type.target().getSimpleName()").contains(type.target().getSimpleName());
        }

        @ParameterizedTest
        @MethodSource(TYPES)
        void testDisplayIncludesId(RegularComponentRelationType<?, ?, ?> type) {
            var component = getComponent(type);

            assertThat(component.display()).as("component.display() must contain the id").contains(Integer.toString(component.id()));
        }

    }

    protected <R, T, X> ComponentRelationComponent<R, T, X> getComponent(RegularComponentRelationType<R, T, X> type) {
        return engine.getComponent(type, NO_OP);
    }

    protected <R, T> ComponentRelation<R, T> relation(R relationship, T target) {
        return new ComponentRelationImpl<>(relationship, target);
    }

    private record ComponentRelationImpl<R, T>(R relationship, T target) implements ComponentRelation<R, T> {
    }

}
