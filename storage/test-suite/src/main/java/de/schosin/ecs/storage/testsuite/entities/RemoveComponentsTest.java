package de.schosin.ecs.storage.testsuite.entities;

import static de.schosin.ecs.plugins.wildcards.types.WildcardType.WILDCARD;
import static de.schosin.ecs.plugins.wildcards.types.WildcardType.wildcard;
import static de.schosin.ecs.plugins.wildcards.types.WildcardType.wildcardRelation;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.stream.Stream;

import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import de.schosin.ecs.api.Pooled;
import de.schosin.ecs.api.components.Relation;
import de.schosin.ecs.api.components.Relation.Exclusive;
import de.schosin.ecs.api.components.types.ComponentType;
import de.schosin.ecs.storage.api.StorageEngineException;
import de.schosin.ecs.storage.api.entities.Archetype;
import de.schosin.ecs.storage.testsuite.AbstractStorageEngineTest;
import de.schosin.ecs.utils.collections.ImmutableBag;

public class RemoveComponentsTest extends AbstractStorageEngineTest {

    private static final String COMPONENTS_SOURCE = "de.schosin.ecs.storage.testsuite.entities.RemoveComponentsTest#components";

    public Archetype removeComponents(int entityId, ImmutableBag<? extends ComponentType<?, ?>> componentTypes) {
        return storageEngine.remove(entityId, componentTypes);
    }

    @ParameterizedTest
    @MethodSource(COMPONENTS_SOURCE)
    void testRemoveFromUnknownEntity(Object component) {
        var componentType = ComponentType.detectComponentType(component);

        assertThatThrownBy(() -> removeComponents(42, ImmutableBag.of(componentType)))
                .isInstanceOf(StorageEngineException.class)
                // Depending on implementation ModifyEntityTest can cause "Cannot add" 
                .hasMessageContainingAll("entity 42", "not present in storage");
    }

    @Nested
    class WildcardComponentTypeTest {

        @Test
        void testObjectWildcard() {
            var emptyArchetype = storageEngine.getArchetype();

            var entityId = world.createEntity(new C1(), new C2(), new P1());

            // Call
            var archetype = removeComponents(entityId, ImmutableBag.of(WILDCARD));

            // Verify
            assertThat(archetype).as("object wildcard removes all class type components").isSameAs(emptyArchetype);

            assertThat(world.getComponents(C1.class).get(entityId)).as("does not remove without flush").isNotNull();
            assertThat(world.getComponents(C2.class).get(entityId)).as("does not remove without flush").isNotNull();
            assertThat(world.getComponents(P1.class).get(entityId)).as("does not remove without flush").isNotNull();
        }

        @Test
        void testObjectWildcard_FlushedChanges() {
            var emptyArchetype = storageEngine.getArchetype();

            var entityId = world.createEntity(new C1(), new C2(), new P1());

            // Call
            var archetype = removeComponents(entityId, ImmutableBag.of(WILDCARD));
            storageEngine.flushChanges(entityId);

            // Verify
            assertThat(archetype).as("object wildcard removes all class type components").isSameAs(emptyArchetype);

            assertThat(world.getComponents(C1.class).get(entityId)).as("removes component from storage").isNull();
            assertThat(world.getComponents(C2.class).get(entityId)).as("removes component from storage").isNull();
            assertThat(world.getComponents(P1.class).get(entityId)).as("removes component from storage").isNull();
        }

        @Test
        void testInterfaceWildcard() {
            var expectedArchetype = storageEngine.getArchetype(component(P1.class));

            var entityId = world.createEntity(new C1(), new C2(), new P1());

            // Call
            var archetype = removeComponents(entityId, ImmutableBag.of(wildcard(C12.class)));

            // Verify
            assertThat(archetype).as("interface wildcard removes matching class type components").isSameAs(expectedArchetype);

            assertThat(world.getComponents(C1.class).get(entityId)).as("does not remove without flush").isNotNull();
            assertThat(world.getComponents(C2.class).get(entityId)).as("does not remove without flush").isNotNull();
            assertThat(world.getComponents(P1.class).get(entityId)).as("does not remove without flush").isNotNull();
        }

        @Test
        void testInterfaceWildcard_FlushedChanges() {
            var expectedArchetype = storageEngine.getArchetype(component(P1.class));

            var entityId = world.createEntity(new C1(), new C2(), new P1());

            // Call
            var archetype = removeComponents(entityId, ImmutableBag.of(wildcard(C12.class)));
            storageEngine.flushChanges(entityId);

            // Verify
            assertThat(archetype).as("interface wildcard removes matching class type components").isSameAs(expectedArchetype);

            assertThat(world.getComponents(C1.class).get(entityId)).as("removes component from storage").isNull();
            assertThat(world.getComponents(C2.class).get(entityId)).as("removes component from storage").isNull();
            assertThat(world.getComponents(P1.class).get(entityId)).as("does not remove other components from storage").isNotNull();
        }

        @Test
        void testObjectWildcard_DoesNotRemoveRelations() {
            var entityId = world.createEntity(
                    Relation.create(new C1(), new C2()), Relation.create(E1.INSTANCE, new C2()),
                    Relation.create(new C1(), 2), Relation.create(E1.INSTANCE, 2));

            var expectedArchetype = storageEngine.getArchetypeForEntity(entityId);

            // Call
            var archetype = removeComponents(entityId, ImmutableBag.of(WILDCARD));

            // Verify
            assertThat(archetype).as("object wildcard does not remove relations from storage").isSameAs(expectedArchetype);
            assertThat(storageEngine.getPendingArchetype(entityId)).as("object wildcard does not remove relations from storage").isNull();

            assertThat(world.getComponents(relation(C1.class, C2.class)).get(entityId)).as("does not remove relations from storage").isNotNull();
            assertThat(world.getComponents(exclusiveRelation(E1.class, C2.class)).get(entityId)).as("does not remove relations from storage").isNotNull();
            assertThat(world.getComponents(relation(C1.class)).get(entityId)).as("does not remove relations from storage").isNotNull();
            assertThat(world.getComponents(exclusiveRelation(E1.class)).get(entityId)).as("does not remove relations from storage").isNotNull();
        }

    }

    @Nested
    class WildcardComponentRelationTypeTest {

        @Test
        void testRelationship_ObjectWildcard() {
            var expectedArchetype = storageEngine.getArchetype(
                    relation(C1.class, C1.class),
                    exclusiveRelation(E1.class, C1.class));

            var entityId = world.createEntity(
                    Relation.create(new C1(), new C1()),
                    Relation.create(new C1(), new C2()),
                    Relation.create(new C2(), new C2()),
                    Relation.create(E1.INSTANCE, new C1()),
                    Relation.create(E1.INSTANCE, new C2()));

            // Call
            var archetype = removeComponents(entityId, ImmutableBag.of(wildcardRelation(Object.class, C2.class)));

            // Verify
            assertThat(archetype).as("object wildcard removes all matching component relations").isSameAs(expectedArchetype);

            assertThat(world.getComponents(relation(C1.class, C2.class)).get(entityId)).as("does not remove without flush").isNotNull();
            assertThat(world.getComponents(relation(C2.class, C2.class)).get(entityId)).as("does not remove without flush").isNotNull();
            assertThat(world.getComponents(exclusiveRelation(E1.class, C2.class)).get(entityId)).as("does not remove without flush").isNotNull();
            assertThat(world.getComponents(relation(C1.class, C1.class)).get(entityId)).as("does not remove without flush").isNotNull();
            assertThat(world.getComponents(exclusiveRelation(E1.class, C1.class)).get(entityId)).as("does not remove without flush").isNotNull();
        }

        @Test
        void testRelationship_ObjectWildcard_FlushedChanges() {
            var expectedArchetype = storageEngine.getArchetype(
                    relation(C1.class, C1.class),
                    exclusiveRelation(E1.class, C1.class));

            var entityId = world.createEntity(
                    Relation.create(new C1(), new C1()),
                    Relation.create(new C1(), new C2()),
                    Relation.create(new C2(), new C2()),
                    Relation.create(E1.INSTANCE, new C1()),
                    Relation.create(E1.INSTANCE, new C2()));

            // Call
            var archetype = removeComponents(entityId, ImmutableBag.of(wildcardRelation(Object.class, C2.class)));
            storageEngine.flushChanges(entityId);

            // Verify
            assertThat(archetype).as("object wildcard removes all matching component relations").isSameAs(expectedArchetype);

            assertThat(world.getComponents(relation(C1.class, C2.class)).get(entityId)).as("removes matching component from storage").isNull();
            assertThat(world.getComponents(relation(C2.class, C2.class)).get(entityId)).as("removes matching component from storage").isNull();
            assertThat(world.getComponents(exclusiveRelation(E1.class, C2.class)).get(entityId)).as("removes matching component from storage").isNull();

            assertThat(world.getComponents(relation(C1.class, C1.class)).get(entityId)).as("does not remove non-matching component from storage").isNotNull();
            assertThat(world.getComponents(exclusiveRelation(E1.class, C1.class)).get(entityId)).as("does not remove non-matching component from storage").isNotNull();
        }

        @Test
        void testRelationship_InterfaceWildcard() {
            var expectedArchetype = storageEngine.getArchetype(
                    relation(C1.class, C1.class),
                    exclusiveRelation(E1.class, C1.class),
                    exclusiveRelation(E1.class, C2.class));

            var entityId = world.createEntity(
                    Relation.create(new C1(), new C1()),
                    Relation.create(new C1(), new C2()),
                    Relation.create(new C2(), new C2()),
                    Relation.create(E1.INSTANCE, new C1()),
                    Relation.create(E1.INSTANCE, new C2()));

            // Call
            var archetype = removeComponents(entityId, ImmutableBag.of(wildcardRelation(C12.class, C2.class)));

            // Verify
            assertThat(archetype).as("interface wildcard removes all matching component relations").isSameAs(expectedArchetype);

            assertThat(world.getComponents(relation(C1.class, C2.class)).get(entityId)).as("does not remove without flush").isNotNull();
            assertThat(world.getComponents(relation(C2.class, C2.class)).get(entityId)).as("does not remove without flush").isNotNull();
            assertThat(world.getComponents(relation(C1.class, C1.class)).get(entityId)).as("does not remove without flush").isNotNull();
            assertThat(world.getComponents(exclusiveRelation(E1.class, C2.class)).get(entityId)).as("does not remove without flush").isNotNull();
            assertThat(world.getComponents(exclusiveRelation(E1.class, C1.class)).get(entityId)).as("does not remove without flush").isNotNull();
        }

        @Test
        void testRelationship_InterfaceWildcard_FlushedChanges() {
            var expectedArchetype = storageEngine.getArchetype(
                    relation(C1.class, C1.class),
                    exclusiveRelation(E1.class, C1.class),
                    exclusiveRelation(E1.class, C2.class));

            var entityId = world.createEntity(
                    Relation.create(new C1(), new C1()),
                    Relation.create(new C1(), new C2()),
                    Relation.create(new C2(), new C2()),
                    Relation.create(E1.INSTANCE, new C1()),
                    Relation.create(E1.INSTANCE, new C2()));

            // Call
            var archetype = removeComponents(entityId, ImmutableBag.of(wildcardRelation(C12.class, C2.class)));
            engine.flushChanges(entityId);

            // Verify
            assertThat(archetype).as("interface wildcard removes all matching component relations").isSameAs(expectedArchetype);

            assertThat(world.getComponents(relation(C1.class, C2.class)).get(entityId)).as("removes matching component from storage").isNull();
            assertThat(world.getComponents(relation(C2.class, C2.class)).get(entityId)).as("removes matching component from storage").isNull();

            assertThat(world.getComponents(relation(C1.class, C1.class)).get(entityId)).as("does not remove non-matching component from storage").isNotNull();
            assertThat(world.getComponents(exclusiveRelation(E1.class, C2.class)).get(entityId)).as("does not remove non-matching component from storage").isNotNull();
            assertThat(world.getComponents(exclusiveRelation(E1.class, C1.class)).get(entityId)).as("does not remove non-matching component from storage").isNotNull();
        }

        @Test
        void testTarget_ObjectWildcard() {
            var expectedArchetype = storageEngine.getArchetype(
                    relation(C2.class, C2.class),
                    exclusiveRelation(E1.class, C1.class),
                    exclusiveRelation(E1.class, C2.class));

            var entityId = world.createEntity(
                    Relation.create(new C1(), new C1()),
                    Relation.create(new C1(), new C2()),
                    Relation.create(new C2(), new C2()),
                    Relation.create(E1.INSTANCE, new C1()),
                    Relation.create(E1.INSTANCE, new C2()));

            // Call
            var archetype = removeComponents(entityId, ImmutableBag.of(wildcardRelation(C1.class, Object.class)));

            // Verify
            assertThat(archetype).as("object wildcard removes all matching component relations").isSameAs(expectedArchetype);

            assertThat(world.getComponents(relation(C1.class, C1.class)).get(entityId)).as("does not remove without flush").isNotNull();
            assertThat(world.getComponents(relation(C1.class, C2.class)).get(entityId)).as("does not remove without flush").isNotNull();
            assertThat(world.getComponents(relation(C2.class, C2.class)).get(entityId)).as("does not remove without flush").isNotNull();
            assertThat(world.getComponents(exclusiveRelation(E1.class, C1.class)).get(entityId)).as("does not remove without flush").isNotNull();
            assertThat(world.getComponents(exclusiveRelation(E1.class, C2.class)).get(entityId)).as("does not remove without flush").isNotNull();
        }

        @Test
        void testTarget_ObjectWildcard_FlushedChanges() {
            var expectedArchetype = storageEngine.getArchetype(
                    relation(C2.class, C2.class),
                    exclusiveRelation(E1.class, C1.class),
                    exclusiveRelation(E1.class, C2.class));

            var entityId = world.createEntity(
                    Relation.create(new C1(), new C1()),
                    Relation.create(new C1(), new C2()),
                    Relation.create(new C2(), new C2()),
                    Relation.create(E1.INSTANCE, new C1()),
                    Relation.create(E1.INSTANCE, new C2()));

            // Call
            var archetype = removeComponents(entityId, ImmutableBag.of(wildcardRelation(C1.class, Object.class)));
            storageEngine.flushChanges(entityId);

            // Verify
            assertThat(archetype).as("object wildcard removes all matching component relations").isSameAs(expectedArchetype);

            assertThat(world.getComponents(relation(C1.class, C1.class)).get(entityId)).as("removes matching component from storage").isNull();
            assertThat(world.getComponents(relation(C1.class, C2.class)).get(entityId)).as("removes matching component from storage").isNull();

            assertThat(world.getComponents(relation(C2.class, C2.class)).get(entityId)).as("does not remove non-matching component from storage").isNotNull();
            assertThat(world.getComponents(exclusiveRelation(E1.class, C1.class)).get(entityId)).as("does not remove non-matching component from storage").isNotNull();
            assertThat(world.getComponents(exclusiveRelation(E1.class, C2.class)).get(entityId)).as("does not remove non-matching component from storage").isNotNull();
        }

        @Test
        void testTarget_InterfaceWildcard() {
            var expectedArchetype = storageEngine.getArchetype(
                    relation(C2.class, C2.class),
                    exclusiveRelation(E1.class, C1.class),
                    exclusiveRelation(E1.class, C2.class));

            var entityId = world.createEntity(
                    Relation.create(new C1(), new C1()),
                    Relation.create(new C1(), new C2()),
                    Relation.create(new C2(), new C2()),
                    Relation.create(E1.INSTANCE, new C1()),
                    Relation.create(E1.INSTANCE, new C2()));

            // Call
            var archetype = removeComponents(entityId, ImmutableBag.of(wildcardRelation(C1.class, C12.class)));

            // Verify
            assertThat(archetype).as("interface wildcard removes all matching component relations").isSameAs(expectedArchetype);

            assertThat(world.getComponents(relation(C1.class, C1.class)).get(entityId)).as("does not remove without flush").isNotNull();
            assertThat(world.getComponents(relation(C1.class, C2.class)).get(entityId)).as("does not remove without flush").isNotNull();
            assertThat(world.getComponents(relation(C2.class, C2.class)).get(entityId)).as("does not remove without flush").isNotNull();
            assertThat(world.getComponents(exclusiveRelation(E1.class, C1.class)).get(entityId)).as("does not remove without flush").isNotNull();
            assertThat(world.getComponents(exclusiveRelation(E1.class, C2.class)).get(entityId)).as("does not remove without flush").isNotNull();
        }

        @Test
        void testTarget_InterfaceWildcard_FlushedChanges() {
            var expectedArchetype = storageEngine.getArchetype(
                    relation(C2.class, C2.class),
                    exclusiveRelation(E1.class, C1.class),
                    exclusiveRelation(E1.class, C2.class));

            var entityId = world.createEntity(
                    Relation.create(new C1(), new C1()),
                    Relation.create(new C1(), new C2()),
                    Relation.create(new C2(), new C2()),
                    Relation.create(E1.INSTANCE, new C1()),
                    Relation.create(E1.INSTANCE, new C2()));

            // Call
            var archetype = removeComponents(entityId, ImmutableBag.of(wildcardRelation(C1.class, C12.class)));
            storageEngine.flushChanges(entityId);

            // Verify
            assertThat(archetype).as("interface wildcard removes all matching component relations").isSameAs(expectedArchetype);

            assertThat(world.getComponents(relation(C1.class, C1.class)).get(entityId)).as("removes matching component from storage").isNull();
            assertThat(world.getComponents(relation(C1.class, C2.class)).get(entityId)).as("removes matching component from storage").isNull();

            assertThat(world.getComponents(relation(C2.class, C2.class)).get(entityId)).as("does not remove non-matching component from storage").isNotNull();
            assertThat(world.getComponents(exclusiveRelation(E1.class, C1.class)).get(entityId)).as("does not remove non-matching component from storage").isNotNull();
            assertThat(world.getComponents(exclusiveRelation(E1.class, C2.class)).get(entityId)).as("does not remove non-matching component from storage").isNotNull();
        }

        @Test
        void testObjectWildcards_DoesNotAffectClassComponents() {
            var expectedArchetype = storageEngine.getArchetype(component(C1.class), component(C2.class), component(P1.class));

            var entityId = world.createEntity(
                    new C1(), new C2(), new P1(),
                    Relation.create(new C1(), new C1()),
                    Relation.create(new C1(), new C2()),
                    Relation.create(new C2(), new C2()),
                    Relation.create(E1.INSTANCE, new C1()),
                    Relation.create(E1.INSTANCE, new C2()));

            // Call
            var archetype = removeComponents(entityId, ImmutableBag.of(wildcardRelation(Object.class, Object.class)));

            // Verify
            assertThat(archetype).as("object wildcard does not remove class type components").isSameAs(expectedArchetype);

            assertThat(world.getComponents(C1.class).get(entityId)).as("does not remove class type component from storage").isNotNull();
            assertThat(world.getComponents(C2.class).get(entityId)).as("does not remove class type component from storage").isNotNull();
            assertThat(world.getComponents(P1.class).get(entityId)).as("does not remove class type component from storage").isNotNull();
        }

        @Test
        void testObjectWildcards_DoesNotAffectEntityRelations() {
            var expectedArchetype = storageEngine.getArchetype(relation(C1.class), exclusiveRelation(E1.class));

            var entityId = world.createEntity(
                    Relation.create(new C1(), 42), Relation.create(E1.INSTANCE, 43),
                    Relation.create(new C1(), new C1()),
                    Relation.create(new C1(), new C2()),
                    Relation.create(new C2(), new C2()),
                    Relation.create(E1.INSTANCE, new C1()),
                    Relation.create(E1.INSTANCE, new C2()));

            // Call
            var archetype = removeComponents(entityId, ImmutableBag.of(wildcardRelation(Object.class, Object.class)));

            // Verify
            assertThat(archetype).as("object wildcard does not remove entity relation type components").isSameAs(expectedArchetype);

            assertThat(world.getComponents(relation(C1.class)).get(entityId)).as("does not remove entity relation type component from storage").isNotNull();
            assertThat(world.getComponents(exclusiveRelation(E1.class)).get(entityId)).as("does not remove entity relation type component from storage").isNotNull();
        }

    }

    @Nested
    class WildcardEntityRelationTypeTest {

        @Test
        void testRelationship_ObjectWildcard() {
            var expectedArchetype = storageEngine.getArchetype();

            var entityId = world.createEntity(
                    Relation.create(new C1(), 42),
                    Relation.create(new C2(), 42),
                    Relation.create(E1.INSTANCE, 42));

            // Call
            var archetype = removeComponents(entityId, ImmutableBag.of(wildcardRelation(Object.class)));

            // Verify
            assertThat(archetype).as("object wildcard removes all class type components").isSameAs(expectedArchetype);

            assertThat(world.getComponents(relation(C1.class)).get(entityId)).as("does not remove without flush").isNotNull();
            assertThat(world.getComponents(relation(C2.class)).get(entityId)).as("does not remove without flush").isNotNull();
            assertThat(world.getComponents(exclusiveRelation(E1.class)).get(entityId)).as("does not remove without flush").isNotNull();
        }

        @Test
        void testRelationship_ObjectWildcard_FlushedChanges() {
            var expectedArchetype = storageEngine.getArchetype();

            var entityId = world.createEntity(
                    Relation.create(new C1(), 42),
                    Relation.create(new C2(), 42),
                    Relation.create(E1.INSTANCE, 42));

            // Call
            var archetype = removeComponents(entityId, ImmutableBag.of(wildcardRelation(Object.class)));
            storageEngine.flushChanges(entityId);

            // Verify
            assertThat(archetype).as("object wildcard removes all class type components").isSameAs(expectedArchetype);

            assertThat(world.getComponents(relation(C1.class)).get(entityId)).as("removes matching component from storage").isNull();
            assertThat(world.getComponents(relation(C2.class)).get(entityId)).as("removes matching component from storage").isNull();
            assertThat(world.getComponents(exclusiveRelation(E1.class)).get(entityId)).as("removes matching component from storage").isNull();
        }

        @Test
        void testRelationship_InterfaceWildcard() {
            var expectedArchetype = storageEngine.getArchetype(exclusiveRelation(E1.class));

            var entityId = world.createEntity(
                    Relation.create(new C1(), 42),
                    Relation.create(new C2(), 42),
                    Relation.create(E1.INSTANCE, 42));

            // Call
            var archetype = removeComponents(entityId, ImmutableBag.of(wildcardRelation(C12.class)));

            // Verify
            assertThat(archetype).as("interface wildcard removes all class type components").isSameAs(expectedArchetype);

            assertThat(world.getComponents(relation(C1.class)).get(entityId)).as("does not remove without flush").isNotNull();
            assertThat(world.getComponents(relation(C2.class)).get(entityId)).as("does not remove without flush").isNotNull();
            assertThat(world.getComponents(exclusiveRelation(E1.class)).get(entityId)).as("does not remove without flush").isNotNull();
        }

        @Test
        void testRelationship_InterfaceWildcard_FlushedChanges() {
            var expectedArchetype = storageEngine.getArchetype(exclusiveRelation(E1.class));

            var entityId = world.createEntity(
                    Relation.create(new C1(), 42),
                    Relation.create(new C2(), 42),
                    Relation.create(E1.INSTANCE, 42));

            // Call
            var archetype = removeComponents(entityId, ImmutableBag.of(wildcardRelation(C12.class)));
            storageEngine.flushChanges(entityId);

            // Verify
            assertThat(archetype).as("interface wildcard removes all class type components").isSameAs(expectedArchetype);

            assertThat(world.getComponents(relation(C1.class)).get(entityId)).as("removes matching component from storage").isNull();
            assertThat(world.getComponents(relation(C2.class)).get(entityId)).as("removes matching component from storage").isNull();
            assertThat(world.getComponents(exclusiveRelation(E1.class)).get(entityId)).as("does not remove entity relation type component from storage").isNotNull();
        }

        @Test
        void testObjectWildcard_DoesNotAffectClassComponents() {
            var expectedArchetype = storageEngine.getArchetype(component(C1.class), component(C2.class), component(P1.class));

            var entityId = world.createEntity(
                    new C1(), new C2(), new P1(),
                    Relation.create(new C1(), 42),
                    Relation.create(new C2(), 42),
                    Relation.create(E1.INSTANCE, 42));

            // Call
            var archetype = removeComponents(entityId, ImmutableBag.of(wildcardRelation(Object.class)));

            // Verify
            assertThat(archetype).as("object wildcard does not remove class type components").isSameAs(expectedArchetype);

            assertThat(world.getComponents(C1.class).get(entityId)).as("does not remove class type component from storage").isNotNull();
            assertThat(world.getComponents(C2.class).get(entityId)).as("does not remove class type component from storage").isNotNull();
            assertThat(world.getComponents(P1.class).get(entityId)).as("does not remove class type component from storage").isNotNull();
        }

        @Test
        void testObjectWildcard_DoesNotAffectComponentRelations() {
            var expectedArchetype = storageEngine.getArchetype(relation(C1.class, C2.class), exclusiveRelation(E1.class, C2.class));

            var entityId = world.createEntity(
                    Relation.create(new C1(), new C2()), Relation.create(E1.INSTANCE, new C2()),
                    Relation.create(new C1(), 42),
                    Relation.create(new C2(), 42),
                    Relation.create(E1.INSTANCE, 42));

            // Call
            var archetype = removeComponents(entityId, ImmutableBag.of(wildcardRelation(Object.class)));

            // Verify
            assertThat(archetype).as("object wildcard does not remove component relation type components").isSameAs(expectedArchetype);

            assertThat(world.getComponents(relation(C1.class, C2.class)).get(entityId)).as("does not remove component relation type component from storage").isNotNull();
            assertThat(world.getComponents(exclusiveRelation(E1.class, C2.class)).get(entityId)).as("does not remove component relation type component from storage").isNotNull();
        }

    }

    static Stream<Arguments> components() {
        return Stream.of(
                Arguments.of(Named.of("C1", new C1())),
                Arguments.of(Named.of("P1", new P1())),
                Arguments.of(Named.of("relation(C1, P1)", Relation.create(new C1(), new P1()))),
                Arguments.of(Named.of("relation(E1, P1)", Relation.create(E1.INSTANCE, new P1()))),
                Arguments.of(Named.of("relation(C1, int)", Relation.create(new C1(), 42))),
                Arguments.of(Named.of("relation(E1, int)", Relation.create(E1.INSTANCE, 42))));
    }

    interface C12 {
    }

    record C1() implements C12 {
    }

    record C2() implements C12 {
    }

    record P1() implements Pooled {
    }

    enum E1 implements Exclusive {
        INSTANCE
    }

}
