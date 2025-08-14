package de.schosin.ecs.storage.testsuite.components.relations;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.function.BiConsumer;

import org.assertj.core.api.ObjectAssert;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import de.schosin.ecs.api.components.Relation.EntityRelation;
import de.schosin.ecs.api.components.types.ComponentType;
import de.schosin.ecs.api.components.types.ComponentType.RegularComponentType;
import de.schosin.ecs.api.components.types.RelationComponentType.RegularEntityRelationType;
import de.schosin.ecs.storage.api.components.Component.EntityRelationComponent;
import de.schosin.ecs.storage.api.components.Component.EntityRelationComponent.RemovedRelationTypeHandler;
import de.schosin.ecs.storage.testsuite.components.CommonComponentTest;
import de.schosin.ecs.utils.collections.Bag;
import de.schosin.ecs.utils.collections.ImmutableBag;

public abstract class CommonEntityRelationTest<R1, X1, R2, X2, R3, X3>
        extends CommonComponentTest<EntityRelation<R1>, X1, EntityRelation<R2>, X2, EntityRelation<R3>, X3> {

    protected static class Handler implements RemovedRelationTypeHandler {

        protected record Data(int entityId, RegularEntityRelationType<?, ?> relationType) {
        }

        protected final Bag<Data> data = new Bag<>(Data.class, 4);

        @Override
        public void removeRelationType(int entityId, RegularEntityRelationType<?, ?> relationType) {
            this.data.add(new Data(entityId, relationType));
        }
    }

    protected final BiConsumer<ObjectAssert<?>, EntityRelation<?>> verifyRelationInstance = verifyRelationInstance();

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

    protected abstract EntityRelation<R1> getInstance1(int relationship, int target);

    protected abstract EntityRelation<R2> getInstance2(int relationship, int target);

    protected abstract EntityRelation<R3> getInstance3(int relationship, int target);

    protected BiConsumer<ObjectAssert<?>, EntityRelation<?>> verifyRelationInstance() {
        return ObjectAssert::isEqualTo;
    }

    @Nested
    class EntityRelationComponentTest extends AbstractTypeTest {

        @ParameterizedTest
        @MethodSource(TYPES)
        void testRelationshipClass(RegularEntityRelationType<?, ?> type) {
            var component = getComponent(type);

            assertThat(component.relationshipClass()).as("relationshipClass matches type relationship").isEqualTo(type.relationship());
        }

    }

    @Nested
    class DisplayTest extends AbstractTypeTest {

        @ParameterizedTest
        @MethodSource(TYPES)
        void testDisplayIncludesRelationshipSimpleName(RegularEntityRelationType<?, ?> type) {
            assertThat(getComponent(type).display()).as("component.display() must contain type.relationship().getSimpleName()").contains(type.relationship().getSimpleName());
        }

        @ParameterizedTest
        @MethodSource(TYPES)
        void testDisplayIncludesId(RegularEntityRelationType<?, ?> type) {
            var component = getComponent(type);

            assertThat(component.display()).as("component.display() must contain the id").contains(Integer.toString(component.id()));
        }

        @ParameterizedTest
        @MethodSource(TYPES)
        void testToStringIncludesRelationshipSimpleName(RegularEntityRelationType<?, ?> type) {
            assertThat(getComponent(type).toString()).as("component.toString() must contain type.relationship().getSimpleName()").contains(type.relationship().getSimpleName());
        }

        @ParameterizedTest
        @MethodSource(TYPES)
        void testToStringIncludesId(RegularEntityRelationType<?, ?> type) {
            var component = getComponent(type);

            assertThat(component.toString()).as("component.toString() must contain the id").contains(Integer.toString(component.id()));
        }

    }

    @Nested
    class FreeRelationTest {

        @Test
        void testFreeRelationInstance() {
            var target = world.createEntity();

            var relation = getInstance1(1, target);
            var entityId = world.createEntity(relation);

            var componentType = ComponentType.detectComponentType(relation);

            // Call
            storageEngine.remove(entityId, ImmutableBag.of(componentType));
            storageEngine.process();

            // Verify
            assertThat(relation.type()).as("removed relation must be returned to Relation.free").isNull();
            assertThat(relation.relationship()).as("removed relation must be returned to Relation.free").isNull();
            assertThat(relation.target()).as("removed relation must be returned to Relation.free").isEqualTo(-1);
        }

    }

    protected <R, X> EntityRelationComponent<R, X> getComponent(RegularEntityRelationType<R, X> type) {
        return storageEngine.getComponent(type);
    }

    record RegularComponent() {
    }

}
