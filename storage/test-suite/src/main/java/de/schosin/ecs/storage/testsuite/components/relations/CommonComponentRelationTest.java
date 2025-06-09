package de.schosin.ecs.storage.testsuite.components.relations;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.function.BiConsumer;

import org.assertj.core.api.ObjectAssert;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import de.schosin.ecs.api.components.Relation;
import de.schosin.ecs.api.components.Relation.ComponentRelation;
import de.schosin.ecs.api.components.types.ComponentType.RegularComponentType;
import de.schosin.ecs.api.components.types.RelationComponentType.RegularComponentRelationType;
import de.schosin.ecs.storage.api.components.Component.ComponentRelationComponent;
import de.schosin.ecs.storage.testsuite.components.CommonComponentTest;
import de.schosin.ecs.utils.collections.ImmutableBag;

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

        @ParameterizedTest
        @MethodSource(TYPES)
        void testToStringIncludesRelationshipSimpleName(RegularComponentRelationType<?, ?, ?> type) {
            assertThat(getComponent(type).toString()).as("component.toString() must contain type.relationship().getSimpleName()").contains(type.relationship().getSimpleName());
        }

        @ParameterizedTest
        @MethodSource(TYPES)
        void testToStringIncludesTargetSimpleName(RegularComponentRelationType<?, ?, ?> type) {
            assertThat(getComponent(type).toString()).as("component.toString() must contain type.target().getSimpleName()").contains(type.target().getSimpleName());
        }

        @ParameterizedTest
        @MethodSource(TYPES)
        void testToStringIncludesId(RegularComponentRelationType<?, ?, ?> type) {
            var component = getComponent(type);

            assertThat(component.toString()).as("component.toString() must contain the id").contains(Integer.toString(component.id()));
        }

    }

    @Nested
    class FreeRelationTest {

        @Test
        void testFreeRelationInstance_WhenNotFlushed() {
            var relation = Relation.create(EnumComponent.INSTANCE, EnumComponent.INSTANCE);
            var entityId = world.createEntity(relation);

            storageEngine.remove(entityId, ImmutableBag.of(relation(EnumComponent.class, EnumComponent.class)));

            assertThat(relation.type()).as("removed relation must not be returned to Relation.free if not flushed").isNotNull();
            assertThat(relation.relationship()).as("removed relation must not be returned to Relation.free if not flushed").isNotNull();
            assertThat(relation.target()).as("removed relation must not be returned to Relation.free if not flushed").isNotNull();
        }

        @Test
        void testFreeRelationInstance_WhenFlushed() {
            var relation = Relation.create(EnumComponent.INSTANCE, EnumComponent.INSTANCE);
            var entityId = world.createEntity(relation);

            storageEngine.remove(entityId, ImmutableBag.of(relation(EnumComponent.class, EnumComponent.class)));
            storageEngine.flushChanges(entityId);

            assertThat(relation.type()).as("removed relation must be returned to Relation.free").isNull();
            assertThat(relation.relationship()).as("removed relation must be returned to Relation.free").isNull();
            assertThat(relation.target()).as("removed relation must be returned to Relation.free").isNull();
        }

    }

    protected <R, T, X> ComponentRelationComponent<R, T, X> getComponent(RegularComponentRelationType<R, T, X> type) {
        return engine.getComponent(type);
    }

    private enum EnumComponent {
        INSTANCE
    }

}
