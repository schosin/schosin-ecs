package de.schosin.ecs.storage.common;

import static de.schosin.ecs.api.components.types.ComponentType.component;
import static de.schosin.ecs.api.components.types.ComponentType.exclusiveRelation;
import static de.schosin.ecs.api.components.types.ComponentType.relation;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import de.schosin.ecs.api.components.Relation;
import de.schosin.ecs.api.components.Relation.ComponentRelation;
import de.schosin.ecs.api.components.Relation.EntityRelation;
import de.schosin.ecs.api.components.Relation.Exclusive;
import de.schosin.ecs.api.components.Result;
import de.schosin.ecs.api.components.types.ClassType;
import de.schosin.ecs.api.components.types.ComponentType.RegularComponentType;
import de.schosin.ecs.api.components.types.RelationComponentType.ComponentRelationType;
import de.schosin.ecs.api.components.types.RelationComponentType.EntityRelationType;
import de.schosin.ecs.api.components.types.RelationComponentType.ExclusiveComponentRelationType;
import de.schosin.ecs.api.components.types.RelationComponentType.ExclusiveEntityRelationType;
import de.schosin.ecs.storage.api.StorageEngineException;
import de.schosin.ecs.storage.api.entities.ComponentMask;
import de.schosin.ecs.utils.collections.ImmutableBag;

class PendingChangesTest {

    private static final ClassType<C1> type1 = component(C1.class);
    private static final ClassType<C2> type2 = component(C2.class);
    private static final ClassType<C3> type3 = component(C3.class);

    private static final ComponentRelationType<C1, C2> componentRelation12 = relation(C1.class, C2.class);
    private static final ComponentRelationType<C2, C1> componentRelation21 = relation(C2.class, C1.class);
    private static final ComponentRelationType<C2, C2> componentRelation22 = relation(C2.class, C2.class);

    private static final ExclusiveComponentRelationType<E1, C1> exclusiveComponentRelation11 = exclusiveRelation(E1.class, C1.class);
    private static final ExclusiveComponentRelationType<E1, C2> exclusiveComponentRelation12 = exclusiveRelation(E1.class, C2.class);
    private static final ExclusiveComponentRelationType<E2, C1> exclusiveComponentRelation21 = exclusiveRelation(E2.class, C1.class);

    private static final EntityRelationType<C1> entityRelation1 = relation(C1.class);
    private static final EntityRelationType<C2> entityRelation2 = relation(C2.class);
    private static final EntityRelationType<C3> entityRelation3 = relation(C3.class);

    private static final ExclusiveEntityRelationType<E1> exclusiveEntityRelation1 = exclusiveRelation(E1.class);
    private static final ExclusiveEntityRelationType<E2> exclusiveEntityRelation2 = exclusiveRelation(E2.class);
    private static final ExclusiveEntityRelationType<E3> exclusiveEntityRelation3 = exclusiveRelation(E3.class);

    @Test
    void testNoChanges() {
        var componentMask = componentMask();
        var changes = new PendingChanges(componentMask);

        // Verify
        assertThat(changes.getAddedTypes()).isEmpty();
        assertThat(changes.getAdded()).isEmpty();
        assertThat(changes.getRemovedTypes()).isEmpty();
    }

    @Nested
    class ClassComponents {

        @Test
        void testAdd() {
            var componentMask = componentMask();
            var changes = new PendingChanges(componentMask);

            // Call
            var component = new C1(42);
            changes.add(type1, component);

            // Verify
            assertThat(changes.getAddedTypes()).containsExactly(type1);
            assertThat(changes.getAdded()).containsExactly(component);
            assertThat(changes.getRemovedTypes()).isEmpty();
        }

        @Test
        void testAddMultiple() {
            var componentMask = componentMask();
            var changes = new PendingChanges(componentMask);

            // Call
            var component1 = new C1(42);
            changes.add(type1, component1);

            var component2 = new C2(42);
            changes.add(type2, component2);

            // Verify
            assertThat(changes.getAddedTypes()).containsExactly(type1, type2);
            assertThat(changes.getAdded()).containsExactly(component1, component2);
            assertThat(changes.getRemovedTypes()).isEmpty();
        }

        @Test
        void testAdd_ReplaceExisting() {
            var componentMask = componentMask();
            var changes = new PendingChanges(componentMask);

            // Call
            var component1 = new C1(42);
            changes.add(type1, component1);

            var component2 = new C1(9001);
            changes.add(type1, component2);

            // Verify
            assertThat(changes.getAddedTypes()).containsExactly(type1);
            assertThat(changes.getAdded()).containsExactly(component2);
            assertThat(changes.getRemovedTypes()).isEmpty();
        }

        @Nested
        class RemoveTest extends AbstractPendingChangesTest {
            @Override
            protected RegularComponentType<?, ?> type1() {
                return type1;
            }

            @Override
            protected RegularComponentType<?, ?> type2() {
                return type2;
            }

            @Override
            protected RegularComponentType<?, ?> type3() {
                // TODO Auto-generated method stub
                return type3;
            }

            @Override
            protected Object instance1() {
                return new C1();
            }

            @Override
            protected Object instance2() {
                return new C2();
            }
        }

    }

    @Nested
    class NonExclusiveComponentRelations {

        @Test
        void testAdd() {
            var componentMask = componentMask();
            var changes = new PendingChanges(componentMask);

            // Call
            var relation1 = Relation.create(new C1(10), new C2(20));
            changes.add(componentRelation12, relation1);

            var relation2 = Relation.create(new C2(200), new C1(100));
            changes.add(componentRelation21, relation2);

            // Verify
            assertThat(changes.getAddedTypes()).containsExactly(componentRelation12, componentRelation21);
            assertThat(changes.getAdded())
                    .hasSize(2)
                    .anySatisfy(component -> assertThat(component).asInstanceOf(InstanceOfAssertFactories.iterable(ComponentRelation.class)).containsExactly(relation1))
                    .anySatisfy(component -> assertThat(component).asInstanceOf(InstanceOfAssertFactories.iterable(ComponentRelation.class)).containsExactly(relation2));

            assertThat(changes.getRemovedTypes()).isEmpty();
        }

        @Test
        void testAdd_DifferentTarget() {
            var componentMask = componentMask();
            var changes = new PendingChanges(componentMask);

            // Call
            var relation1 = Relation.create(new C1(10), new C2(20));
            changes.add(componentRelation12, relation1);

            var relation2 = Relation.create(new C1(10), new C2(21));
            changes.add(componentRelation12, relation2);

            // Verify
            assertThat(changes.getAddedTypes()).containsExactly(componentRelation12);
            assertThat(changes.getAdded())
                    .hasSize(1)
                    .anySatisfy(component -> assertThat(component).asInstanceOf(InstanceOfAssertFactories.iterable(ComponentRelation.class)).containsExactly(relation1, relation2));

            assertThat(changes.getRemovedTypes()).isEmpty();
        }

        @Test
        void testAdd_EqualTarget() {
            var componentMask = componentMask();
            var changes = new PendingChanges(componentMask);

            // Call
            var relation1 = Relation.create(new C1(10), new C2(20));
            changes.add(componentRelation12, relation1);

            var relation2 = Relation.create(new C1(20), new C2(20));
            changes.add(componentRelation12, relation2);

            // Verify
            assertThat(changes.getAddedTypes()).containsExactly(componentRelation12);
            assertThat(changes.getAdded())
                    .hasSize(1)
                    .anySatisfy(component -> assertThat(component).asInstanceOf(InstanceOfAssertFactories.iterable(ComponentRelation.class)).containsExactly(relation2));

            assertThat(changes.getRemovedTypes()).isEmpty();
        }

        @Nested
        class RemoveTest extends AbstractPendingChangesTest {
            @Override
            protected RegularComponentType<?, ?> type1() {
                return componentRelation12;
            }

            @Override
            protected RegularComponentType<?, ?> type2() {
                return componentRelation21;
            }

            @Override
            protected RegularComponentType<?, ?> type3() {
                return componentRelation22;
            }

            @Override
            protected Object instance1() {
                return Relation.create(new C1(), new C2());
            }

            @Override
            protected Object instance2() {
                return Relation.create(new C2(), new C1());
            }
        }

    }

    @Nested
    class ExclusiveComponentRelations {

        @Test
        void testAdd_DifferentRelationshipType() {
            var componentMask = componentMask();
            var changes = new PendingChanges(componentMask);

            // Call
            var relation1 = Relation.create(E1.INSTANCE1, new C1(10));
            changes.add(exclusiveComponentRelation11, relation1);

            var relation2 = Relation.create(E2.INSTANCE1, new C1(10));
            changes.add(exclusiveComponentRelation21, relation2);

            // Verify
            assertThat(changes.getAddedTypes()).containsExactly(exclusiveComponentRelation11, exclusiveComponentRelation21);
            assertThat(changes.getAdded()).containsExactly(relation1, relation2);
            assertThat(changes.getRemovedTypes()).isEmpty();
        }

        @Test
        void testAdd_DifferentRelationshipInstance() {
            var componentMask = componentMask();
            var changes = new PendingChanges(componentMask);

            // Call
            var relation1 = Relation.create(E1.INSTANCE1, new C1(10));
            changes.add(exclusiveComponentRelation11, relation1);

            var relation2 = Relation.create(E1.INSTANCE2, new C1(11));
            changes.add(exclusiveComponentRelation11, relation2);

            // Verify
            assertThat(changes.getAddedTypes()).containsExactly(exclusiveComponentRelation11);
            assertThat(changes.getAdded()).containsExactly(relation2);
            assertThat(changes.getRemovedTypes()).isEmpty();
        }

        @Test
        void testAdd_DifferentTargetType() {
            var componentMask = componentMask();
            var changes = new PendingChanges(componentMask);

            // Call
            var relation1 = Relation.create(E1.INSTANCE1, new C1(10));
            changes.add(exclusiveComponentRelation11, relation1);

            var relation2 = Relation.create(E1.INSTANCE2, new C2(11));
            changes.add(exclusiveComponentRelation12, relation2);

            // Verify
            assertThat(changes.getAddedTypes()).containsExactly(exclusiveComponentRelation12);
            assertThat(changes.getAdded()).containsExactly(relation2);
            assertThat(changes.getRemovedTypes()).isEmpty();
        }

        @Test
        void testAdd_RemovesMatchingTypesFromComponentMask() {
            var componentMask = componentMask(exclusiveComponentRelation12);
            var changes = new PendingChanges(componentMask);

            // Call
            var relation1 = Relation.create(E1.INSTANCE1, new C1(10));
            changes.add(exclusiveComponentRelation11, relation1);

            // Verify
            assertThat(changes.getAddedTypes()).containsExactly(exclusiveComponentRelation11);
            assertThat(changes.getAdded()).containsExactly(relation1);
            assertThat(changes.getRemovedTypes()).containsExactly(exclusiveComponentRelation12);
        }

        @Nested
        class RemoveTest extends AbstractPendingChangesTest {
            @Override
            protected RegularComponentType<?, ?> type1() {
                return exclusiveComponentRelation11;
            }

            @Override
            protected RegularComponentType<?, ?> type2() {
                return exclusiveComponentRelation21;
            }

            @Override
            protected RegularComponentType<?, ?> type3() {
                return exclusiveComponentRelation12;
            }

            @Override
            protected Object instance1() {
                return Relation.create(E1.INSTANCE1, new C1());
            }

            @Override
            protected Object instance2() {
                return Relation.create(E2.INSTANCE1, new C1());
            }
        }

    }

    @Nested
    class NonExclusiveEntityRelations {

        @Test
        void testAdd() {
            var componentMask = componentMask();
            var changes = new PendingChanges(componentMask);

            // Call
            var relation1 = Relation.create(new C1(10), 20);
            changes.add(entityRelation1, relation1);

            var relation2 = Relation.create(new C2(200), 100);
            changes.add(entityRelation2, relation2);

            // Verify
            assertThat(changes.getAddedTypes()).containsExactly(entityRelation1, entityRelation2);
            assertThat(changes.getAdded())
                    .hasSize(2)
                    .anySatisfy(component -> assertThat(component).asInstanceOf(InstanceOfAssertFactories.iterable(EntityRelation.class)).containsExactly(relation1))
                    .anySatisfy(component -> assertThat(component).asInstanceOf(InstanceOfAssertFactories.iterable(EntityRelation.class)).containsExactly(relation2));

            assertThat(changes.getRemovedTypes()).isEmpty();
        }

        @Test
        void testAdd_DifferentTarget() {
            var componentMask = componentMask();
            var changes = new PendingChanges(componentMask);

            // Call
            var relation1 = Relation.create(new C1(10), 20);
            changes.add(entityRelation1, relation1);

            var relation2 = Relation.create(new C1(10), 21);
            changes.add(entityRelation1, relation2);

            // Verify
            assertThat(changes.getAddedTypes()).containsExactly(entityRelation1);
            assertThat(changes.getAdded())
                    .hasSize(1)
                    .anySatisfy(component -> assertThat(component).asInstanceOf(InstanceOfAssertFactories.iterable(EntityRelation.class)).containsExactly(relation1, relation2));

            assertThat(changes.getRemovedTypes()).isEmpty();
        }

        @Test
        void testAdd_EqualTarget() {
            var componentMask = componentMask();
            var changes = new PendingChanges(componentMask);

            // Call
            var relation1 = Relation.create(new C1(10), 20);
            changes.add(entityRelation1, relation1);

            var relation2 = Relation.create(new C1(20), 20);
            changes.add(entityRelation1, relation2);

            // Verify
            assertThat(changes.getAddedTypes()).containsExactly(entityRelation1);
            assertThat(changes.getAdded())
                    .hasSize(1)
                    .anySatisfy(component -> assertThat(component).asInstanceOf(InstanceOfAssertFactories.iterable(EntityRelation.class)).containsExactly(relation2));

            assertThat(changes.getRemovedTypes()).isEmpty();
        }

        @Nested
        class RemoveTest extends AbstractPendingChangesTest {
            @Override
            protected RegularComponentType<?, ?> type1() {
                return entityRelation1;
            }

            @Override
            protected RegularComponentType<?, ?> type2() {
                return entityRelation2;
            }

            @Override
            protected RegularComponentType<?, ?> type3() {
                return entityRelation3;
            }

            @Override
            protected Object instance1() {
                return Relation.create(new C1(), 10);
            }

            @Override
            protected Object instance2() {
                return Relation.create(new C2(), 20);
            }
        }

    }

    @Nested
    class ExclusiveEntityRelations {

        @Test
        void testAdd_DifferentRelationshipType() {
            var componentMask = componentMask();
            var changes = new PendingChanges(componentMask);

            // Call
            var relation1 = Relation.create(E1.INSTANCE1, 10);
            changes.add(exclusiveEntityRelation1, relation1);

            var relation2 = Relation.create(E2.INSTANCE1, 10);
            changes.add(exclusiveEntityRelation2, relation2);

            // Verify
            assertThat(changes.getAddedTypes()).containsExactly(exclusiveEntityRelation1, exclusiveEntityRelation2);
            assertThat(changes.getAdded()).containsExactly(relation1, relation2);
            assertThat(changes.getRemovedTypes()).isEmpty();
        }

        @Test
        void testAdd_DifferentRelationshipInstance() {
            var componentMask = componentMask();
            var changes = new PendingChanges(componentMask);

            // Call
            var relation1 = Relation.create(E1.INSTANCE1, 10);
            changes.add(exclusiveEntityRelation1, relation1);

            var relation2 = Relation.create(E1.INSTANCE2, 11);
            changes.add(exclusiveEntityRelation1, relation2);

            // Verify
            assertThat(changes.getAddedTypes()).containsExactly(exclusiveEntityRelation1);
            assertThat(changes.getAdded()).containsExactly(relation2);
            assertThat(changes.getRemovedTypes()).isEmpty();
        }

        @Nested
        class RemoveTest extends AbstractPendingChangesTest {
            @Override
            protected RegularComponentType<?, ?> type1() {
                return exclusiveEntityRelation1;
            }

            @Override
            protected RegularComponentType<?, ?> type2() {
                return exclusiveEntityRelation2;
            }

            @Override
            protected RegularComponentType<?, ?> type3() {
                return exclusiveEntityRelation3;
            }

            @Override
            protected Object instance1() {
                return Relation.create(E1.INSTANCE1, 10);
            }

            @Override
            protected Object instance2() {
                return Relation.create(E2.INSTANCE1, 20);
            }
        }

    }

    abstract class AbstractPendingChangesTest {

        protected abstract RegularComponentType<?, ?> type1();

        protected abstract RegularComponentType<?, ?> type2();

        protected abstract RegularComponentType<?, ?> type3();

        protected abstract Object instance1();

        protected abstract Object instance2();

        @Test
        void testIsEmpty() {
            var componentMask = componentMask(type2());
            var changes = new PendingChanges(componentMask);

            assertThat(changes.isEmpty()).isTrue();

            changes.add(type1(), instance1());
            assertThat(changes.isEmpty()).isFalse();

            changes.remove(type1());
            assertThat(changes.isEmpty()).isTrue();

            changes.remove(type2());
            assertThat(changes.isEmpty()).isFalse();
        }

        @Test
        void testAdd_MismatchingType() {
            var componentMask = componentMask();
            var changes = new PendingChanges(componentMask);

            var type = type1();
            var instance = instance2();
            assertThat(type.isInstance(instance)).as("correct test setup by implementing class").isFalse();

            // Call
            assertThatThrownBy(() -> changes.add(type, instance))
                    .isInstanceOf(StorageEngineException.class)
                    .message()
                    .containsSubsequence("Expected ", type.toString(), "but got", instance.toString());
        }

        @Test
        void testAdd_NullType() {
            var componentMask = componentMask();
            var changes = new PendingChanges(componentMask);

            var instance = instance1();

            // Call
            assertThatThrownBy(() -> changes.add(null, instance))
                    .isInstanceOf(StorageEngineException.class)
                    .message()
                    .containsSubsequence("Expected ", "null", "but got", instance.toString());
        }

        @Test
        void testAdd_NullComponent() {
            var componentMask = componentMask();
            var changes = new PendingChanges(componentMask);

            var type = type1();

            // Call
            assertThatThrownBy(() -> changes.add(type, null))
                    .isInstanceOf(StorageEngineException.class)
                    .message()
                    .containsSubsequence("Expected ", type.toString(), "but got", "null");
        }

        @Test
        void testAdd_ArchetypeChange() {
            var componentMask = componentMask();
            var changes = new PendingChanges(componentMask);

            // Call
            assertThat(changes.add(type1(), instance1())).isTrue();
        }

        @Test
        void testAdd_NoArchetypeChange() {
            var componentMask = componentMask(type1());
            var changes = new PendingChanges(componentMask);

            // Call
            assertThat(changes.add(type1(), instance1())).isFalse();
        }

        @Test
        void testReset() {
            var componentMask = componentMask(type3());
            var changes = new PendingChanges(componentMask);

            var component1 = instance1();
            changes.add(type1(), component1);
            changes.remove(type3());

            assertThat(changes.isEmpty()).isFalse();
            assertThat(changes.getAddedTypes()).containsExactly(type1());
            assertThat(changes.getAdded()).hasSize(1);
            assertThat(changes.getRemovedTypes()).containsExactly(type3());

            // Call
            changes.reset();

            // Verify
            assertThat(changes.isEmpty()).isTrue();
            assertThat(changes.getAddedTypes()).isEmpty();
            assertThat(changes.getAdded()).isEmpty();
            assertThat(changes.getRemovedTypes()).isEmpty();
        }

        @Test
        void testRemove_NoOp() {
            var componentMask = componentMask(type1());
            var changes = new PendingChanges(componentMask);

            // Call
            var component2 = instance2();
            changes.add(type2(), component2);

            changes.remove(type3());

            // Verify
            assertThat(changes.getAddedTypes()).containsExactly(type2());
            assertThat(changes.getAdded()).hasSize(1);
            assertThat(changes.getRemovedTypes()).isEmpty();
        }

        @Test
        void testRemove_FromComponentMask() {
            var componentMask = componentMask(type1());
            var changes = new PendingChanges(componentMask);

            // Call
            var component2 = instance2();
            changes.add(type2(), component2);

            changes.remove(type1());

            // Verify
            assertThat(changes.getAddedTypes()).containsExactly(type2());
            assertThat(changes.getAdded()).hasSize(1);
            assertThat(changes.getRemovedTypes()).containsExactly(type1());
        }

        @Test
        void testRemove_FromAddedTypes() {
            var componentMask = componentMask(type2());
            var changes = new PendingChanges(componentMask);

            // Call
            var component1 = instance1();
            changes.add(type1(), component1);

            changes.remove(type1());

            // Verify
            assertThat(changes.getAddedTypes()).isEmpty();
            assertThat(changes.getAdded()).isEmpty();
            assertThat(changes.getRemovedTypes()).isEmpty();
        }

        @Test
        @SuppressWarnings("unchecked")
        void testGetComponent() {
            var componentMask = componentMask();
            var changes = new PendingChanges(componentMask);

            var instance1 = instance1();
            changes.add(type1(), instance1);

            // Verify
            var result = changes.getComponent(type1());
            if (result instanceof Result relationResult) {
                assertThat(relationResult).containsExactly(instance1);
            } else {
                assertThat(result).isSameAs(instance1);
            }

            assertThat(changes.getComponent(type2())).isNull();
            assertThat(changes.getComponent(type3())).isNull();
        }

        @Test
        void testGetComponent_EmptyChanges() {
            var componentMask = componentMask();
            var changes = new PendingChanges(componentMask);

            // Verify
            assertThat(changes.getComponent(type1())).isNull();
            assertThat(changes.getComponent(type2())).isNull();
            assertThat(changes.getComponent(type3())).isNull();
        }

    }

    private ComponentMask componentMask(RegularComponentType<?, ?>... componentTypes) {
        return new MockComponentMask(ImmutableBag.of(componentTypes));
    }

    record C1(int value) {
        C1() {
            this(0);
        }
    }

    record C2(int value) {
        C2() {
            this(0);
        }
    }

    record C3(int value) {
        C3() {
            this(0);
        }
    }

    enum E1 implements Exclusive {
        INSTANCE1, INSTANCE2
    }

    enum E2 implements Exclusive {
        INSTANCE1, INSTANCE2
    }

    enum E3 implements Exclusive {
        INSTANCE1, INSTANCE2
    }

}
