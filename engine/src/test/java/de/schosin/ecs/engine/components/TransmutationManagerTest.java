package de.schosin.ecs.engine.components;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.HashSet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import de.schosin.ecs.api.archetype.Transmuter;
import de.schosin.ecs.api.archetype.Transmuter.Remove;
import de.schosin.ecs.api.components.Composition;
import de.schosin.ecs.codegen.EcsCodegen;
import de.schosin.ecs.engine.utils.collections.IntBag;

@EcsCodegen
class TransmutationManagerTest extends BaseTransmutationManagerTest {

    Transmuter.Add1<C1> add1;
    Transmuter.Add2<C1, C2> add1add2;

    Transmuter.Remove remove1;
    Transmuter.Remove remove2;
    Transmuter.Remove remove1remove2;

    Transmuter.Add1<C1> add1remove2;

    @BeforeEach
    void setupTransmuters() {
        this.add1 = world.createTransmuter(Transmuter.add(C1.class));
        this.remove1 = world.createTransmuter(Transmuter.remove(C1.class));
        this.remove2 = world.createTransmuter(Transmuter.remove(C2.class));

        this.add1remove2 = world.createTransmuter(Transmuter.add(C1.class).remove(C2.class));
        this.add1add2 = world.createTransmuter(Transmuter.add(C1.class, C2.class));
        this.remove1remove2 = world.createTransmuter(Transmuter.remove(C1.class, C2.class));
    }

    @Test
    void testInvalidBuilder() {
        assertThatThrownBy(() -> Transmuter.remove((Class<?>) null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Transmuter.remove(C1.class, (Class<?>) null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Transmuter.remove(C1.class, C2.class, (Class<?>) null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void testUnknownEntity() {
        assertThat(add1.apply(42, new C1())).isFalse();
        assertThat(add1add2.apply(42, new C1(), new C2())).isFalse();
        assertThat(remove2.apply(42)).isFalse();
        assertThat(remove1remove2.apply(42)).isFalse();
        assertThat(add1remove2.apply(42, new C1())).isFalse();
    }

    @Test
    void testAddRemove() {
        // Setup
        var entityId = world.createEntity(new C2());
        verifyDoesNotHaveComponent(entityId, C1.class);
        verifyHasComponent(entityId, C2.class);

        verifyHasComposition(entityId, Composition.none(C1.class).all(C2.class));
        verifyDoesNotHaveComposition(entityId, Composition.all(C1.class).none(C2.class));

        // Call
        add1remove2.apply(entityId, new C1());
        world.process();

        // Verify
        verifyHasComponent(entityId, C1.class);
        verifyDoesNotHaveComponent(entityId, C2.class);

        verifyDoesNotHaveComposition(entityId, Composition.none(C1.class).all(C2.class));
        verifyHasComposition(entityId, Composition.all(C1.class).none(C2.class));
    }

    @Test
    void testGetInstance() {
        assertThat(add1.getInstance(D1.class)).isNotNull();
        assertThat(add1.getInstance(D2.class)).isNotNull();

        assertThat(add1remove2.getInstance(D1.class)).isNotNull();
        assertThat(add1remove2.getInstance(D2.class)).isNotNull();
    }

    @Nested
    class CachedTransmuterTest extends BaseCachedTransmuterTest {

        abstract class AbstractCachedTransmuterTest {

            @Test
            void testCachedTransmuter() {
                var transmuter = getTransmuter();

                // Call
                assertThat(getTransmuter()).isSameAs(transmuter);
            }

            protected abstract Transmuter getTransmuter();

        }

        @Nested
        class RemoveTransmuter extends AbstractCachedTransmuterTest {
            @Override
            protected Transmuter getTransmuter() {
                return world.createTransmuter(Transmuter.remove(C1.class, C2.class));
            }
        }

        @Nested
        class Add2Transmuter extends AbstractCachedTransmuterTest {
            @Override
            protected Transmuter getTransmuter() {
                return world.createTransmuter(Transmuter.add(C1.class, C2.class));
            }
        }

        @Nested
        class Add3Transmuter extends AbstractCachedTransmuterTest {
            @Override
            protected Transmuter getTransmuter() {
                return world.createTransmuter(Transmuter.add(C1.class, C2.class, C3.class));
            }
        }

        @Nested
        class Add4Transmuter extends AbstractCachedTransmuterTest {
            @Override
            protected Transmuter getTransmuter() {
                return world.createTransmuter(Transmuter.add(C1.class, C2.class, C3.class, C4.class));
            }
        }

        @Nested
        class Add5Transmuter extends AbstractCachedTransmuterTest {
            @Override
            protected Transmuter getTransmuter() {
                return world.createTransmuter(Transmuter.add(C1.class, C2.class, C3.class, C4.class, C5.class));
            }
        }

        @Nested
        class Add6Transmuter extends AbstractCachedTransmuterTest {
            @Override
            protected Transmuter getTransmuter() {
                return world.createTransmuter(Transmuter.add(C1.class, C2.class, C3.class, C4.class, C5.class, C6.class));
            }
        }

        @Nested
        class Add7Transmuter extends AbstractCachedTransmuterTest {
            @Override
            protected Transmuter getTransmuter() {
                return world.createTransmuter(Transmuter.add(C1.class, C2.class, C3.class, C4.class, C5.class, C6.class, C7.class));
            }
        }

        @Nested
        class Add8Transmuter extends AbstractCachedTransmuterTest {
            @Override
            protected Transmuter getTransmuter() {
                return world.createTransmuter(Transmuter.add(C1.class, C2.class, C3.class, C4.class, C5.class, C6.class, C7.class, C8.class));
            }
        }

        @Nested
        class AddNTransmuter extends AbstractCachedTransmuterTest {
            @Override
            protected Transmuter getTransmuter() {
                return getTransmuterN();
            }
        }

    }

    @Nested
    class AddTest extends BaseAddTest {

        @Test
        void testAdd() {
            // Setup
            var entityId = world.createEntity();
            verifyDoesNotHaveComponent(entityId, C1.class);
            verifyDoesNotHaveComposition(entityId, Composition.all(C1.class));

            // Call
            add1.apply(entityId, new C1());
            world.process();

            // Verify
            verifyHasComponent(entityId, C1.class);
            verifyHasComposition(entityId, Composition.all(C1.class));
        }

        @Test
        void testAdd_WhenComponentPresent_ReplacesExisting() {
            // Setup
            var oldC1 = new C1();
            var newC1 = new C1();

            var entityId = world.createEntity(oldC1);
            verifyHasComponent(entityId, C1.class);
            verifyHasComposition(entityId, Composition.all(C1.class));

            // Call
            add1.apply(entityId, newC1);
            world.process();

            // Verify
            assertThat(getComponent(entityId, C1.class)).isSameAs(newC1);
            verifyHasComponent(entityId, C1.class);
            verifyHasComposition(entityId, Composition.all(C1.class));
        }

        @Test
        void testAdd_WhenPresentComponentMarkedForRemoval_ReplacesExisting() {
            // Setup
            var oldC1 = new C1();
            var newC1 = new C1();

            var entityId = world.createEntity(oldC1);
            verifyHasComponent(entityId, C1.class);
            verifyHasComposition(entityId, Composition.all(C1.class));

            remove1.apply(entityId);

            // Call
            add1.apply(entityId, newC1);
            world.process();

            // Verify
            assertThat(getComponent(entityId, C1.class)).isSameAs(newC1);
            verifyHasComponent(entityId, C1.class);
            verifyHasComposition(entityId, Composition.all(C1.class));
        }

        @Test
        void testAdd_WhenPresentComponentMarkedForRemoval_DoesNotTriggerCompositionChanges() {
            // Setup
            var oldC1 = new C1();
            var newC1 = new C1();

            var entityId = world.createEntity(oldC1);
            verifyHasComponent(entityId, C1.class);
            verifyHasComposition(entityId, Composition.all(C1.class));

            var changed = new ArrayList<Integer>();
            var composition = world.createComposition(Composition.all(C1.class));
            composition.inserted(changed::add);
            composition.removed(changed::add);

            remove1.apply(entityId);

            // Call
            add1.apply(entityId, newC1);
            world.process();

            // Verify
            assertThat(changed).as("no composition changes triggered").isEmpty();

            assertThat(getComponent(entityId, C1.class)).isSameAs(newC1);
            verifyHasComponent(entityId, C1.class);
            verifyHasComposition(entityId, Composition.all(C1.class));
        }

        @Test
        void testAdd_NoInstance() {
            // Setup
            var entityId = world.createEntity();
            verifyDoesNotHaveComponent(entityId, C1.class);
            verifyDoesNotHaveComposition(entityId, Composition.all(C1.class));

            // Call
            assertThatThrownBy(() -> add1.apply(entityId, null)).isInstanceOf(NullPointerException.class);

            // Verify
            verifyDoesNotHaveComponent(entityId, C1.class);
            verifyDoesNotHaveComposition(entityId, Composition.all(C1.class));
        }

        @Test
        void testAdd_NoProcessDoesNotUpdateComposition() {
            // Setup
            var entityId = world.createEntity();
            verifyDoesNotHaveComponent(entityId, C1.class);
            verifyDoesNotHaveComposition(entityId, Composition.all(C1.class));

            // Call
            add1.apply(entityId, new C1());

            // Verify
            verifyHasComponent(entityId, C1.class);
            verifyDoesNotHaveComposition(entityId, Composition.all(C1.class));
        }

        @Test
        void testAdd_CompositionInsertedCallback() {
            // Setup
            var entityId = world.createEntity();

            var inserted = new IntBag(1);
            var composition = world.createComposition(Composition.all(C1.class));
            composition.inserted(inserted::add);

            // Call
            add1.apply(entityId, new C1());
            world.process();

            // Verify
            assertThat(inserted.getSize()).as("size").isEqualTo(1);
            assertThat(inserted.getData()).contains(entityId);
        }

        @Test
        void testAdd_CompositionInsertedCallback_NotCalledIfCompositionUnchanged() {
            // Setup
            var entityId = world.createEntity(new C1());

            var inserted = new IntBag(1);
            var composition = world.createComposition(Composition.all(C1.class));
            composition.inserted(inserted::add);

            // Call
            add1.apply(entityId, new C1());
            world.process();

            // Verify
            assertThat(inserted.getSize()).as("size").isEqualTo(0);
            assertThat(inserted.getData()).doesNotContain(entityId);
        }

        @Test
        void testAddMultiple() {
            // Setup
            var entityId = world.createEntity();
            verifyDoesNotHaveComponent(entityId, C1.class);
            verifyDoesNotHaveComponent(entityId, C2.class);
            verifyDoesNotHaveComposition(entityId, Composition.all(C1.class, C2.class));

            // Call
            add1add2.apply(entityId, new C1(), new C2());
            world.process();

            // Verify
            verifyHasComponent(entityId, C1.class);
            verifyHasComponent(entityId, C2.class);
            verifyHasComposition(entityId, Composition.all(C1.class, C2.class));
        }

        @Test
        void testAddMultiple_NoInstances() {
            var c1 = new C1();
            var c2 = new C2();

            // Setup
            var entityId = world.createEntity();
            verifyDoesNotHaveComponent(entityId, C1.class);
            verifyDoesNotHaveComponent(entityId, C2.class);
            verifyDoesNotHaveComposition(entityId, Composition.all(C1.class, C2.class));

            // Call
            assertThatThrownBy(() -> add1add2.apply(entityId, null, null)).isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> add1add2.apply(entityId, c1, null)).isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> add1add2.apply(entityId, null, c2)).isInstanceOf(NullPointerException.class);

            // Verify (component1 added due to second call)
            verifyHasComponent(entityId, C1.class);
            verifyDoesNotHaveComponent(entityId, C2.class);
            verifyDoesNotHaveComposition(entityId, Composition.all(C1.class, C2.class));
        }

        @Test
        void testAddNull_Throws() {
            // Setup
            var entityId = world.createEntity();
            verifyDoesNotHaveComponent(entityId, C1.class);
            verifyDoesNotHaveComposition(entityId, Composition.all(C1.class));

            // Call
            assertThatThrownBy(() -> add1.apply(entityId, null))
                    .isExactlyInstanceOf(NullPointerException.class)
                    .hasMessageContainingAll("Cannot invoke", "getClass()", "is null");

            // Verify
            verifyDoesNotHaveComponent(entityId, C1.class);
            verifyDoesNotHaveComposition(entityId, Composition.all(C1.class));
        }

        @Test
        void testAddNull_WhenWorldProcessed_DoesNotAlterComposition() {
            // Setup
            var entityId = world.createEntity();
            verifyDoesNotHaveComponent(entityId, C1.class);
            verifyDoesNotHaveComposition(entityId, Composition.all(C1.class));

            assertThatThrownBy(() -> add1.apply(entityId, null))
                    .isExactlyInstanceOf(NullPointerException.class)
                    .hasMessageContainingAll("Cannot invoke", "getClass()", "is null");

            // Call
            world.process();

            // Verify
            verifyDoesNotHaveComponent(entityId, C1.class);
            verifyDoesNotHaveComposition(entityId, Composition.all(C1.class));
        }

        abstract class AbstractAddTest {

            protected abstract Transmuter.Add getTransmuter();

            @Test
            void testAdd() {
                // Setup
                var entityId = world.createEntity();

                // Call
                var classes = apply(entityId);
                world.process();

                // Verify
                for (var clazz : classes) {
                    verifyHasComponent(entityId, clazz);
                }

                verifyHasComposition(entityId, Composition.all(classes));
            }

            @Test
            void testGetInstance() {
                var transmuter = getTransmuter();
                assertThat(transmuter.getInstance(D1.class)).isNotNull();
                assertThat(transmuter.getInstance(D2.class)).isNotNull();
            }

            /**
             * @param entityId id of entity
             * @return added classes
             */
            protected abstract Class<?>[] apply(int entityId);

        }

        @Nested
        class Add1Test extends AbstractAddTest {
            @Override
            protected Transmuter.Add1<C1> getTransmuter() {
                return world.createTransmuter(Transmuter.add(C1.class));
            }

            @Override
            protected Class<?>[] apply(int entityId) {
                var transmuter = getTransmuter();
                transmuter.apply(entityId, new C1());

                return new Class<?>[] { C1.class };
            }
        }

        @Nested
        class Add2Test extends AbstractAddTest {
            @Override
            protected Transmuter.Add2<C1, C2> getTransmuter() {
                return world.createTransmuter(Transmuter.add(C1.class, C2.class));
            }

            @Override
            protected Class<?>[] apply(int entityId) {
                var transmuter = getTransmuter();
                transmuter.apply(entityId, new C1(), new C2());

                return new Class<?>[] { C1.class, C2.class };
            }
        }

        @Nested
        class Add3Test extends AbstractAddTest {
            @Override
            protected Transmuter.Add3<C1, C2, C3> getTransmuter() {
                return world.createTransmuter(Transmuter.add(C1.class, C2.class, C3.class));
            }

            @Override
            protected Class<?>[] apply(int entityId) {
                var transmuter = getTransmuter();
                transmuter.apply(entityId, new C1(), new C2(), new C3());

                return new Class<?>[] { C1.class, C2.class, C3.class };
            }
        }

        @Nested
        class Add4Test extends AbstractAddTest {
            @Override
            protected Transmuter.Add4<C1, C2, C3, C4> getTransmuter() {
                return world.createTransmuter(Transmuter.add(C1.class, C2.class, C3.class, C4.class));
            }

            @Override
            protected Class<?>[] apply(int entityId) {
                var transmuter = getTransmuter();
                transmuter.apply(entityId, new C1(), new C2(), new C3(), new C4());

                return new Class<?>[] { C1.class, C2.class, C3.class, C4.class };
            }
        }

        @Nested
        class Add5Test extends AbstractAddTest {
            @Override
            protected Transmuter.Add5<C1, C2, C3, C4, C5> getTransmuter() {
                return world.createTransmuter(Transmuter.add(C1.class, C2.class, C3.class, C4.class, C5.class));
            }

            @Override
            protected Class<?>[] apply(int entityId) {
                var transmuter = getTransmuter();
                transmuter.apply(entityId, new C1(), new C2(), new C3(), new C4(), new C5());

                return new Class<?>[] { C1.class, C2.class, C3.class, C4.class, C5.class };
            }
        }

        @Nested
        class Add6Test extends AbstractAddTest {
            @Override
            protected Transmuter.Add6<C1, C2, C3, C4, C5, C6> getTransmuter() {
                return world.createTransmuter(Transmuter.add(C1.class, C2.class, C3.class, C4.class, C5.class, C6.class));
            }

            @Override
            protected Class<?>[] apply(int entityId) {
                var transmuter = getTransmuter();
                transmuter.apply(entityId, new C1(), new C2(), new C3(), new C4(), new C5(), new C6());

                return new Class<?>[] { C1.class, C2.class, C3.class, C4.class, C5.class, C6.class };
            }
        }

        @Nested
        class Add7Test extends AbstractAddTest {
            @Override
            protected Transmuter.Add7<C1, C2, C3, C4, C5, C6, C7> getTransmuter() {
                return world.createTransmuter(Transmuter.add(C1.class, C2.class, C3.class, C4.class, C5.class, C6.class, C7.class));
            }

            @Override
            protected Class<?>[] apply(int entityId) {
                var transmuter = getTransmuter();
                transmuter.apply(entityId, new C1(), new C2(), new C3(), new C4(), new C5(), new C6(), new C7());

                return new Class<?>[] { C1.class, C2.class, C3.class, C4.class, C5.class, C6.class, C7.class };
            }
        }

        @Nested
        class Add8Test extends AbstractAddTest {
            @Override
            protected Transmuter.Add8<C1, C2, C3, C4, C5, C6, C7, C8> getTransmuter() {
                return world.createTransmuter(Transmuter.add(C1.class, C2.class, C3.class, C4.class, C5.class, C6.class, C7.class, C8.class));
            }

            @Override
            protected Class<?>[] apply(int entityId) {
                var transmuter = getTransmuter();
                transmuter.apply(entityId, new C1(), new C2(), new C3(), new C4(), new C5(), new C6(), new C7(), new C8());

                return new Class<?>[] { C1.class, C2.class, C3.class, C4.class, C5.class, C6.class, C7.class, C8.class };
            }
        }

        @Nested
        class AddNRemove extends AbstractAddTest {
            @Override
            protected Transmuter.Add getTransmuter() {
                return getTransmuterN();
            }

            @Override
            protected Class<?>[] apply(int entityId) {
                return applyTransmuterN(entityId);
            }
        }

    }

    @Nested
    class RemoveTest extends BaseRemoveTest {

        @Test
        void testRemove() {
            // Setup
            var remove1 = world.createTransmuter(Transmuter.remove(C1.class));

            var entityId = world.createEntity(new C1(), new C2(), new C3());
            verifyHasComponent(entityId, C1.class);
            verifyHasComponent(entityId, C2.class);
            verifyHasComponent(entityId, C3.class);
            verifyHasComposition(entityId, Composition.all(C1.class, C2.class, C3.class));

            // Remove 1
            remove1.apply(entityId);
            world.process();

            // Verify
            verifyDoesNotHaveComponent(entityId, C1.class);
            verifyHasComponent(entityId, C2.class);
            verifyHasComponent(entityId, C3.class);
            verifyHasComposition(entityId, Composition.all(C2.class, C3.class).none(C1.class));
        }

        @Test
        void testRemoveMultiple() {
            // Setup
            var remove23 = world.createTransmuter(Transmuter.remove(C2.class, C3.class));

            var entityId = world.createEntity(new C1(), new C2(), new C3());
            verifyHasComponent(entityId, C1.class);
            verifyHasComponent(entityId, C2.class);
            verifyHasComponent(entityId, C3.class);
            verifyHasComposition(entityId, Composition.all(C1.class, C2.class, C3.class));

            // Remove 2 & 3
            remove23.apply(entityId);
            world.process();

            // Verify
            verifyHasComponent(entityId, C1.class);
            verifyDoesNotHaveComponent(entityId, C2.class);
            verifyDoesNotHaveComponent(entityId, C3.class);
            verifyHasComposition(entityId, Composition.all(C1.class).none(C2.class, C3.class));
        }

        @Test
        void testRemove_CompositionRemovedCalled() {
            // Setup
            var removed = new HashSet<Integer>();

            var remove1 = world.createTransmuter(Transmuter.remove(C1.class));
            var entityId = world.createEntity(new C1(), new C2(), new C3());

            var composition = world.createComposition(Composition.all(C1.class));
            composition.removed(removed::add);

            // Remove 1
            remove1.apply(entityId);
            world.process();

            // Verify
            assertThat(removed).containsExactlyInAnyOrder(entityId);
        }

        @Test
        void testRemove_CompositionRemovedNotCalledIfUnchanged() {
            // Setup
            var removed = new HashSet<Integer>();

            var remove1 = world.createTransmuter(Transmuter.remove(C1.class));
            var entityId = world.createEntity(new C2(), new C3());

            var composition = world.createComposition(Composition.all(C1.class));
            composition.removed(removed::add);

            // Remove 1
            remove1.apply(entityId);
            world.process();

            // Verify
            assertThat(removed).isEmpty();
        }

        abstract class AbstractRemoveByAddTest {

            @Test
            void testRemove() {
                // Setup
                var entityId = world.createEntity(new D1(), new D2());
                verifyHasComponent(entityId, D1.class);
                verifyHasComponent(entityId, D2.class);
                verifyHasComposition(entityId, Composition.all(D1.class, D2.class));

                // Remove 1
                var added = apply(entityId, D1.class);
                world.process();

                // Verify
                verifyDoesNotHaveComponent(entityId, D1.class);
                verifyHasComponent(entityId, D2.class);
                verifyHasComposition(entityId, Composition.all(D2.class).none(D1.class));

                var componentMask = entityManager.getComponentMask(entityId);
                assertThat(componentMask.getComponents()).hasSize(added + 1);
            }

            @Test
            void testRemoveMultiple() {
                // Setup
                var entityId = world.createEntity(new D1(), new D2());
                verifyHasComponent(entityId, D1.class);
                verifyHasComponent(entityId, D2.class);
                verifyHasComposition(entityId, Composition.all(D1.class, D2.class));

                // Remove 1
                var added = apply(entityId, D1.class, D2.class);
                world.process();

                // Verify
                verifyDoesNotHaveComponent(entityId, D1.class);
                verifyDoesNotHaveComponent(entityId, D2.class);
                verifyHasComposition(entityId, Composition.none(D1.class, D2.class));

                var componentMask = entityManager.getComponentMask(entityId);
                assertThat(componentMask.getComponents()).hasSize(added);
            }

            /**
             * @param entityId id of entity
             * @param remove components to remove
             * @return number of added components
             */
            protected abstract int apply(int entityId, Class<?>... remove);

        }

        @Nested
        class Add1Remove extends AbstractRemoveByAddTest {
            @Override
            protected int apply(int entityId, Class<?>... remove) {
                var transmuter = world.createTransmuter(Transmuter.add(C1.class).remove(remove));
                transmuter.apply(entityId, new C1());

                return 1;
            }
        }

        @Nested
        class Add2Remove extends AbstractRemoveByAddTest {
            @Override
            protected int apply(int entityId, Class<?>... remove) {
                var transmuter = world.createTransmuter(Transmuter.add(C1.class, C2.class).remove(remove));
                transmuter.apply(entityId, new C1(), new C2());

                return 2;
            }
        }

        @Nested
        class Add3Remove extends AbstractRemoveByAddTest {
            @Override
            protected int apply(int entityId, Class<?>... remove) {
                var transmuter = world.createTransmuter(Transmuter.add(C1.class, C2.class, C3.class).remove(remove));
                transmuter.apply(entityId, new C1(), new C2(), new C3());

                return 3;
            }
        }

        @Nested
        class Add4Remove extends AbstractRemoveByAddTest {
            @Override
            protected int apply(int entityId, Class<?>... remove) {
                var transmuter = world.createTransmuter(Transmuter.add(C1.class, C2.class, C3.class, C4.class).remove(remove));
                transmuter.apply(entityId, new C1(), new C2(), new C3(), new C4());

                return 4;
            }
        }

        @Nested
        class Add5Remove extends AbstractRemoveByAddTest {
            @Override
            protected int apply(int entityId, Class<?>... remove) {
                var transmuter = world.createTransmuter(Transmuter.add(C1.class, C2.class, C3.class, C4.class, C5.class).remove(remove));
                transmuter.apply(entityId, new C1(), new C2(), new C3(), new C4(), new C5());

                return 5;
            }
        }

        @Nested
        class Add6Remove extends AbstractRemoveByAddTest {
            @Override
            protected int apply(int entityId, Class<?>... remove) {
                var transmuter = world.createTransmuter(Transmuter.add(C1.class, C2.class, C3.class, C4.class, C5.class, C6.class).remove(remove));
                transmuter.apply(entityId, new C1(), new C2(), new C3(), new C4(), new C5(), new C6());

                return 6;
            }
        }

        @Nested
        class Add7Remove extends AbstractRemoveByAddTest {
            @Override
            protected int apply(int entityId, Class<?>... remove) {
                var transmuter = world.createTransmuter(Transmuter.add(C1.class, C2.class, C3.class, C4.class, C5.class, C6.class, C7.class).remove(remove));
                transmuter.apply(entityId, new C1(), new C2(), new C3(), new C4(), new C5(), new C6(), new C7());

                return 7;
            }
        }

        @Nested
        class Add8Remove extends AbstractRemoveByAddTest {
            @Override
            protected int apply(int entityId, Class<?>... remove) {
                var transmuter = world.createTransmuter(Transmuter.add(C1.class, C2.class, C3.class, C4.class, C5.class, C6.class, C7.class, C8.class).remove(remove));
                transmuter.apply(entityId, new C1(), new C2(), new C3(), new C4(), new C5(), new C6(), new C7(), new C8());

                return 8;
            }
        }

        @Nested
        class AddNRemove extends AbstractRemoveByAddTest {
            @Override
            protected int apply(int entityId, Class<?>... remove) {
                return applyTransmuterN(entityId, remove);
            }
        }

    }

    @Nested
    class ConsecutiveMutationsTest {

        private Remove remove1;
        private Remove remove2;
        private Remove remove3;

        @BeforeEach
        void setupRemove() {
            this.remove1 = world.createTransmuter(Transmuter.remove(C1.class));
            this.remove2 = world.createTransmuter(Transmuter.remove(C2.class));
            this.remove3 = world.createTransmuter(Transmuter.remove(C3.class));
        }

        @Test
        void testInitialSetup() {
            var composition1 = world.createComposition(Composition.all(C1.class));
            var composition12 = world.createComposition(Composition.all(C1.class, C2.class));
            var composition123 = world.createComposition(Composition.all(C1.class, C2.class, C3.class));
            var composition13 = world.createComposition(Composition.all(C1.class, C3.class));
            var composition2 = world.createComposition(Composition.all(C2.class));
            var composition23 = world.createComposition(Composition.all(C2.class, C3.class));
            var composition3 = world.createComposition(Composition.all(C3.class));

            var entityId = world.createEntity(new C1(), new C2(), new C3());

            // Verify
            verifyHasComposition(entityId, composition1);
            verifyHasComposition(entityId, composition12);
            verifyHasComposition(entityId, composition123);
            verifyHasComposition(entityId, composition13);
            verifyHasComposition(entityId, composition2);
            verifyHasComposition(entityId, composition23);
            verifyHasComposition(entityId, composition3);
        }

        @Test
        void testRemove1_WhenWorldNotProcessed_DoesNotChangeComposition() {
            var composition1 = world.createComposition(Composition.all(C1.class));
            var composition12 = world.createComposition(Composition.all(C1.class, C2.class));
            var composition123 = world.createComposition(Composition.all(C1.class, C2.class, C3.class));
            var composition13 = world.createComposition(Composition.all(C1.class, C3.class));
            var composition2 = world.createComposition(Composition.all(C2.class));
            var composition23 = world.createComposition(Composition.all(C2.class, C3.class));
            var composition3 = world.createComposition(Composition.all(C3.class));

            var entityId = world.createEntity(new C1(), new C2(), new C3());

            // Call
            remove1.apply(entityId);

            // Verify
            verifyHasComposition(entityId, composition1);
            verifyHasComposition(entityId, composition12);
            verifyHasComposition(entityId, composition123);
            verifyHasComposition(entityId, composition13);
            verifyHasComposition(entityId, composition2);
            verifyHasComposition(entityId, composition23);
            verifyHasComposition(entityId, composition3);
        }

        @Test
        void testRemove1_WhenWorldProcessed_ChangesComposition() {
            var composition1 = world.createComposition(Composition.all(C1.class));
            var composition12 = world.createComposition(Composition.all(C1.class, C2.class));
            var composition123 = world.createComposition(Composition.all(C1.class, C2.class, C3.class));
            var composition13 = world.createComposition(Composition.all(C1.class, C3.class));
            var composition2 = world.createComposition(Composition.all(C2.class));
            var composition23 = world.createComposition(Composition.all(C2.class, C3.class));
            var composition3 = world.createComposition(Composition.all(C3.class));

            var entityId = world.createEntity(new C1(), new C2(), new C3());

            // Call
            remove1.apply(entityId);
            world.process();

            // Verify
            verifyDoesNotHaveComposition(entityId, composition1);
            verifyDoesNotHaveComposition(entityId, composition12);
            verifyDoesNotHaveComposition(entityId, composition123);
            verifyDoesNotHaveComposition(entityId, composition13);
            verifyHasComposition(entityId, composition2);
            verifyHasComposition(entityId, composition23);
            verifyHasComposition(entityId, composition3);
        }

        @Test
        void testRemove1Remove2_WhenWorldNotProcessed_DoesNotChangeComposition() {
            var composition1 = world.createComposition(Composition.all(C1.class));
            var composition12 = world.createComposition(Composition.all(C1.class, C2.class));
            var composition123 = world.createComposition(Composition.all(C1.class, C2.class, C3.class));
            var composition13 = world.createComposition(Composition.all(C1.class, C3.class));
            var composition2 = world.createComposition(Composition.all(C2.class));
            var composition23 = world.createComposition(Composition.all(C2.class, C3.class));
            var composition3 = world.createComposition(Composition.all(C3.class));

            var entityId = world.createEntity(new C1(), new C2(), new C3());

            // Call
            remove1.apply(entityId);
            remove2.apply(entityId);

            // Verify
            verifyHasComposition(entityId, composition1);
            verifyHasComposition(entityId, composition12);
            verifyHasComposition(entityId, composition123);
            verifyHasComposition(entityId, composition13);
            verifyHasComposition(entityId, composition2);
            verifyHasComposition(entityId, composition23);
            verifyHasComposition(entityId, composition3);
        }

        @Test
        void testRemove1Remove2_WhenWorldProcessed_ChangesComposition() {
            var composition1 = world.createComposition(Composition.all(C1.class));
            var composition12 = world.createComposition(Composition.all(C1.class, C2.class));
            var composition123 = world.createComposition(Composition.all(C1.class, C2.class, C3.class));
            var composition13 = world.createComposition(Composition.all(C1.class, C3.class));
            var composition2 = world.createComposition(Composition.all(C2.class));
            var composition23 = world.createComposition(Composition.all(C2.class, C3.class));
            var composition3 = world.createComposition(Composition.all(C3.class));

            var entityId = world.createEntity(new C1(), new C2(), new C3());

            // Call
            remove1.apply(entityId);
            remove2.apply(entityId);
            world.process();

            // Verify
            verifyDoesNotHaveComposition(entityId, composition1);
            verifyDoesNotHaveComposition(entityId, composition12);
            verifyDoesNotHaveComposition(entityId, composition123);
            verifyDoesNotHaveComposition(entityId, composition13);
            verifyDoesNotHaveComposition(entityId, composition2);
            verifyDoesNotHaveComposition(entityId, composition23);
            verifyHasComposition(entityId, composition3);
        }

        @Test
        void testRemove1Remove2Remove3_WhenWorldNotProcessed_DoesNotChangeComposition() {
            var composition1 = world.createComposition(Composition.all(C1.class));
            var composition12 = world.createComposition(Composition.all(C1.class, C2.class));
            var composition123 = world.createComposition(Composition.all(C1.class, C2.class, C3.class));
            var composition13 = world.createComposition(Composition.all(C1.class, C3.class));
            var composition2 = world.createComposition(Composition.all(C2.class));
            var composition23 = world.createComposition(Composition.all(C2.class, C3.class));
            var composition3 = world.createComposition(Composition.all(C3.class));

            var entityId = world.createEntity(new C1(), new C2(), new C3());

            // Call
            remove1.apply(entityId);
            remove2.apply(entityId);
            remove3.apply(entityId);

            // Verify
            verifyHasComposition(entityId, composition1);
            verifyHasComposition(entityId, composition12);
            verifyHasComposition(entityId, composition123);
            verifyHasComposition(entityId, composition13);
            verifyHasComposition(entityId, composition2);
            verifyHasComposition(entityId, composition23);
            verifyHasComposition(entityId, composition3);
        }

        @Test
        void testRemove1Remove2Remove3_WhenWorldProcessed_ChangesComposition() {
            var composition1 = world.createComposition(Composition.all(C1.class));
            var composition12 = world.createComposition(Composition.all(C1.class, C2.class));
            var composition123 = world.createComposition(Composition.all(C1.class, C2.class, C3.class));
            var composition13 = world.createComposition(Composition.all(C1.class, C3.class));
            var composition2 = world.createComposition(Composition.all(C2.class));
            var composition23 = world.createComposition(Composition.all(C2.class, C3.class));
            var composition3 = world.createComposition(Composition.all(C3.class));

            var entityId = world.createEntity(new C1(), new C2(), new C3());

            // Call
            remove1.apply(entityId);
            remove2.apply(entityId);
            remove3.apply(entityId);
            world.process();

            // Verify
            verifyDoesNotHaveComposition(entityId, composition1);
            verifyDoesNotHaveComposition(entityId, composition12);
            verifyDoesNotHaveComposition(entityId, composition123);
            verifyDoesNotHaveComposition(entityId, composition13);
            verifyDoesNotHaveComposition(entityId, composition2);
            verifyDoesNotHaveComposition(entityId, composition23);
            verifyDoesNotHaveComposition(entityId, composition3);
        }

    }

}
