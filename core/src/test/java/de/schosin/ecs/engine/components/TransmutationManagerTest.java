package de.schosin.ecs.engine.components;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import de.schosin.ecs.engine.AbstractWorldTest;

class TransmutationManagerTest extends AbstractWorldTest {

    TransmutationManager.Add<C1> add1;
    TransmutationManager.Add<C2> add2;

    TransmutationManager.Remove remove1;
    TransmutationManager.Remove remove2;
    TransmutationManager.Remove remove3;

    @BeforeEach
    void setupTransmuters() {
        this.add1 = transmutationManager.getAddTransmuter(component(C1.class));
        this.add2 = transmutationManager.getAddTransmuter(component(C2.class));

        this.remove1 = transmutationManager.getRemoveTransmuter(component(C1.class));
        this.remove2 = transmutationManager.getRemoveTransmuter(component(C2.class));
        this.remove3 = transmutationManager.getRemoveTransmuter(component(C3.class));
    }

    @Test
    void testUnknownEntity() {
        assertThat(add1.apply(42, new C1())).isFalse();
        assertThat(add2.apply(42, new C2())).isFalse();
        assertThat(remove1.apply(42)).isFalse();
        assertThat(remove2.apply(42)).isFalse();
    }

    @Test
    void testAddRemove() {
        // Setup
        var entityId = world.createEntity(new C2());
        verifyDoesNotHaveComponents(entityId, C1.class);
        verifyHasComponents(entityId, C2.class);

        verifyComponentMaskDoesNotHaveComponents(entityId, C1.class);
        verifyComponentMaskHasComponents(entityId, C2.class);

        // Call
        add1.apply(entityId, new C1());
        remove2.apply(entityId);
        world.process();

        // Verify
        verifyHasComponents(entityId, C1.class);
        verifyDoesNotHaveComponents(entityId, C2.class);

        verifyComponentMaskHasComponents(entityId, C1.class);
        verifyComponentMaskDoesNotHaveComponents(entityId, C2.class);
    }

    @Test
    void testCachedTransmuter() {
        var transmuter = transmutationManager.getAddTransmuter(component(C1.class));

        // Call
        assertThat(transmutationManager.getAddTransmuter(component(C1.class))).isSameAs(transmuter);
    }

    @Nested
    class AddTest {

        @Test
        void testAdd() {
            // Setup
            var entityId = world.createEntity();
            verifyDoesNotHaveComponents(entityId, C1.class);
            verifyComponentMaskDoesNotHaveComponents(entityId, C1.class);

            // Call
            add1.apply(entityId, new C1());
            world.process();

            // Verify
            verifyHasComponents(entityId, C1.class);
            verifyComponentMaskHasComponents(entityId, C1.class);
        }

        @Test
        void testAdd_WhenComponentPresent_ReplacesExisting() {
            // Setup
            var oldC1 = new C1();
            var newC1 = new C1();

            var entityId = world.createEntity(oldC1);
            verifyHasComponents(entityId, C1.class);
            verifyComponentMaskHasComponents(entityId, C1.class);

            // Call
            add1.apply(entityId, newC1);
            world.process();

            // Verify
            assertThat(getComponent(entityId, C1.class)).isSameAs(newC1);
            verifyHasComponents(entityId, C1.class);
            verifyComponentMaskHasComponents(entityId, C1.class);
        }

        @Test
        void testAdd_WhenPresentComponentMarkedForRemoval_ReplacesExisting() {
            // Setup
            var oldC1 = new C1();
            var newC1 = new C1();

            var entityId = world.createEntity(oldC1);
            verifyHasComponents(entityId, C1.class);
            verifyComponentMaskHasComponents(entityId, C1.class);

            remove1.apply(entityId);

            // Call
            add1.apply(entityId, newC1);
            world.process();

            // Verify
            assertThat(getComponent(entityId, C1.class)).isSameAs(newC1);
            verifyHasComponents(entityId, C1.class);
            verifyComponentMaskHasComponents(entityId, C1.class);
        }

        @Test
        void testAdd_WhenPresentComponentMarkedForRemoval_DoesNotTriggerCompositionChanges() {
            // Setup
            var oldC1 = new C1();
            var newC1 = new C1();

            var entityId = world.createEntity(oldC1);
            verifyHasComponents(entityId, C1.class);
            verifyComponentMaskHasComponents(entityId, C1.class);

            verify(verify -> {
                verify.expectNoMoreUpdated();

                // Call
                remove1.apply(entityId);
                add1.apply(entityId, newC1);
                world.process();

                // Verify
                assertThat(getComponent(entityId, C1.class)).isSameAs(newC1);
                verifyHasComponents(entityId, C1.class);
                verifyComponentMaskHasComponents(entityId, C1.class);
            });
        }

        @Test
        void testAdd_NoInstance() {
            // Setup
            var entityId = world.createEntity();
            verifyDoesNotHaveComponents(entityId, C1.class);
            verifyComponentMaskDoesNotHaveComponents(entityId, C1.class);

            // Call
            assertThatThrownBy(() -> add1.apply(entityId, (C1) null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Cannot get component type for null instance");

            // Verify
            verifyDoesNotHaveComponents(entityId, C1.class);
            verifyComponentMaskDoesNotHaveComponents(entityId, C1.class);
        }

        @Test
        void testAdd_NoProcessDoesNotUpdateComposition() {
            // Setup
            var entityId = world.createEntity();
            verifyDoesNotHaveComponents(entityId, C1.class);
            verifyComponentMaskDoesNotHaveComponents(entityId, C1.class);

            // Call
            add1.apply(entityId, new C1());

            // Verify
            verifyHasComponents(entityId, C1.class);
            verifyComponentMaskDoesNotHaveComponents(entityId, C1.class);
        }

        @Test
        void testAdd_CompositionInsertedCallback() {
            // Setup
            var entityId = world.createEntity();

            verify(verify -> {
                verify.expectUpdated(entityId, C1.class);
                verify.expectNoMoreUpdated();

                // Call
                add1.apply(entityId, new C1());
                world.process();
            });
        }

        @Test
        void testAdd_CompositionInsertedCallback_NotCalledIfCompositionUnchanged() {
            // Setup
            var entityId = world.createEntity(new C1());

            verify(verify -> {
                verify.expectNoMoreUpdated();

                // Call
                add1.apply(entityId, new C1());
                world.process();
            });
        }

        @Test
        void testAddMultiple() {
            // Setup
            var entityId = world.createEntity();
            verifyDoesNotHaveComponents(entityId, C1.class, C2.class);
            verifyComponentMaskDoesNotHaveComponents(entityId, C1.class, C2.class);

            // Call
            add1.apply(entityId, new C1());
            add2.apply(entityId, new C2());
            world.process();

            // Verify
            verifyHasComponents(entityId, C1.class, C2.class);
            verifyComponentMaskHasComponents(entityId, C1.class, C2.class);
        }

        @Test
        void testAddNull_Throws() {
            // Setup
            var entityId = world.createEntity();
            verifyDoesNotHaveComponents(entityId, C1.class);
            verifyComponentMaskDoesNotHaveComponents(entityId, C1.class);

            // Call
            assertThatThrownBy(() -> add1.apply(entityId, (C1) null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Cannot get component type for null instance");

            // Verify
            verifyDoesNotHaveComponents(entityId, C1.class);
            verifyComponentMaskDoesNotHaveComponents(entityId, C1.class);
        }

        @Test
        void testAddNull_WhenWorldProcessed_DoesNotAlterComposition() {
            // Setup
            var entityId = world.createEntity();
            verifyDoesNotHaveComponents(entityId, C1.class);
            verifyComponentMaskDoesNotHaveComponents(entityId, C1.class);

            assertThatThrownBy(() -> add1.apply(entityId, (C1) null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Cannot get component type for null instance");

            // Call
            world.process();

            // Verify
            verifyDoesNotHaveComponents(entityId, C1.class);
            verifyComponentMaskDoesNotHaveComponents(entityId, C1.class);
        }

    }

    @Nested
    class RemoveTest {

        @Test
        void testRemove() {
            // Setup
            var entityId = world.createEntity(new C1(), new C2(), new C3());
            verifyHasComponents(entityId, C1.class, C2.class, C3.class);
            verifyComponentMaskHasComponents(entityId, C1.class, C2.class, C3.class);

            // Remove 1
            remove1.apply(entityId);
            world.process();

            // Verify
            verifyDoesNotHaveComponents(entityId, C1.class);
            verifyHasComponents(entityId, C2.class, C3.class);

            verifyComponentMaskDoesNotHaveComponents(entityId, C1.class);
            verifyComponentMaskHasComponents(entityId, C2.class, C3.class);
        }

        @Test
        void testRemoveMultiple() {
            // Setup
            var entityId = world.createEntity(new C1(), new C2(), new C3());
            verifyHasComponents(entityId, C1.class, C2.class, C3.class);
            verifyComponentMaskHasComponents(entityId, C1.class, C2.class, C3.class);

            // Remove 2 & 3
            remove2.apply(entityId);
            remove3.apply(entityId);

            world.process();

            // Verify
            verifyHasComponents(entityId, C1.class);
            verifyDoesNotHaveComponents(entityId, C2.class, C3.class);

            verifyComponentMaskHasComponents(entityId, C1.class);
            verifyComponentMaskDoesNotHaveComponents(entityId, C2.class, C3.class);
        }

        @Test
        void testRemove_CompositionRemovedCalled() {
            // Setup
            var entityId = world.createEntity(new C1(), new C2(), new C3());

            verify(verify -> {
                verify.expectUpdated(entityId, C2.class, C3.class);
                verify.expectNoMoreUpdated();

                // Remove 1
                remove1.apply(entityId);
                world.process();
            });
        }

        @Test
        void testRemove_CompositionRemovedNotCalledIfUnchanged() {
            // Setup
            var entityId = world.createEntity(new C2(), new C3());

            verify(verify -> {
                verify.expectNoMoreUpdated();

                // Remove 1
                remove1.apply(entityId);
                world.process();
            });
        }

    }

    @Nested
    class ConsecutiveMutationsTest {

        @Test
        void testRemove1_WhenWorldNotProcessed_DoesNotChangeComposition() {
            var entityId = world.createEntity(new C1(), new C2(), new C3());

            // Call
            remove1.apply(entityId);

            // Verify
            verifyComponentMaskHasComponents(entityId, C1.class, C2.class, C3.class);
        }

        @Test
        void testRemove1_WhenWorldProcessed_ChangesComposition() {
            var entityId = world.createEntity(new C1(), new C2(), new C3());

            // Call
            remove1.apply(entityId);
            world.process();

            // Verify
            verifyComponentMaskHasComponents(entityId, C2.class, C3.class);
            verifyComponentMaskDoesNotHaveComponents(entityId, C1.class);
        }

        @Test
        void testRemove1Remove2_WhenWorldNotProcessed_DoesNotChangeComposition() {
            var entityId = world.createEntity(new C1(), new C2(), new C3());

            // Call
            remove1.apply(entityId);
            remove2.apply(entityId);

            // Verify
            verifyComponentMaskHasComponents(entityId, C1.class, C2.class, C3.class);
        }

        @Test
        void testRemove1Remove2_WhenWorldProcessed_ChangesComposition() {
            var entityId = world.createEntity(new C1(), new C2(), new C3());

            // Call
            remove1.apply(entityId);
            remove2.apply(entityId);
            world.process();

            // Verify
            verifyComponentMaskHasComponents(entityId, C3.class);
            verifyComponentMaskDoesNotHaveComponents(entityId, C1.class, C2.class);
        }

        @Test
        void testRemove1Remove2Remove3_WhenWorldNotProcessed_DoesNotChangeComposition() {
            var entityId = world.createEntity(new C1(), new C2(), new C3());

            // Call
            remove1.apply(entityId);
            remove2.apply(entityId);
            remove3.apply(entityId);

            // Verify
            verifyComponentMaskHasComponents(entityId, C1.class, C2.class, C3.class);
        }

        @Test
        void testRemove1Remove2Remove3_WhenWorldProcessed_ChangesComposition() {
            var entityId = world.createEntity(new C1(), new C2(), new C3());

            // Call
            remove1.apply(entityId);
            remove2.apply(entityId);
            remove3.apply(entityId);
            world.process();

            // Verify
            verifyComponentMaskDoesNotHaveComponents(entityId, C1.class, C2.class, C3.class);
        }

    }

    public record C1() {
    }

    public record C2() {
    }

    public record C3() {
    }

}
