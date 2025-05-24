package de.schosin.ecs.engine.components;

import static de.schosin.ecs.api.components.types.ComponentType.WILDCARD;
import static de.schosin.ecs.api.components.types.ComponentType.wildcard;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Modifier;
import java.util.LinkedHashSet;
import java.util.SequencedSet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import de.schosin.ecs.api.components.types.ComponentType;
import de.schosin.ecs.api.components.types.ComponentType.RegularComponentType;
import de.schosin.ecs.engine.AbstractWorldTest;
import de.schosin.ecs.engine.components.TransmutationManager.AbstractTransmuter;
import de.schosin.ecs.engine.components.TransmutationManager.Builder;

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
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("component cannot be null");

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
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("component cannot be null");

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
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("component cannot be null");

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
        void testRemoveMultipleComponents() {
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
        void testRemoveMultipleEntities() {
            // Setup
            var entity1 = world.createEntity(new C1(), new C2(), new C3());
            verifyHasComponents(entity1, C1.class, C2.class, C3.class);
            verifyComponentMaskHasComponents(entity1, C1.class, C2.class, C3.class);

            var entity2 = world.createEntity(new C1(), new C2(), new C3());
            verifyHasComponents(entity2, C1.class, C2.class, C3.class);
            verifyComponentMaskHasComponents(entity2, C1.class, C2.class, C3.class);

            // Remove 2
            remove2.apply(entity1);
            remove2.apply(entity2);

            world.process();

            // Verify
            verifyHasComponents(entity1, C1.class, C3.class);
            verifyDoesNotHaveComponents(entity1, C2.class);

            verifyComponentMaskHasComponents(entity1, C1.class, C3.class);
            verifyComponentMaskDoesNotHaveComponents(entity1, C2.class);

            verifyHasComponents(entity2, C1.class, C3.class);
            verifyDoesNotHaveComponents(entity2, C2.class);

            verifyComponentMaskHasComponents(entity2, C1.class, C3.class);
            verifyComponentMaskDoesNotHaveComponents(entity2, C2.class);
        }

        // Verifies "entities.clear()" in ChangeManager  
        @Test
        void testRemove_DoesNotRemoveOnProcessOfReusedEntityId() {
            // Setup
            var entity1 = world.createEntity(new C1(), new C2(), new C3());
            verifyHasComponents(entity1, C1.class, C2.class, C3.class);
            verifyComponentMaskHasComponents(entity1, C1.class, C2.class, C3.class);

            // Remove 2 
            remove2.apply(entity1);
            world.process();

            // Delete and create new entity
            world.deleteEntity(entity1);
            world.process();

            var entity2 = world.createEntity(new C1(), new C2(), new C3());
            assertThat(entity2).as("entityId reused").isEqualTo(entity1);

            verifyHasComponents(entity2, C1.class, C2.class, C3.class);
            verifyComponentMaskHasComponents(entity2, C1.class, C2.class, C3.class);

            var entity3 = world.createEntity(new C1(), new C2(), new C3());

            // Remove 3
            remove2.apply(entity3); // trigger removals of C2
            remove3.apply(entity2);
            world.process();

            // Verify
            verifyHasComponents(entity2, C1.class, C2.class);
            verifyDoesNotHaveComponents(entity2, C3.class);

            verifyComponentMaskHasComponents(entity2, C1.class, C2.class);
            verifyComponentMaskDoesNotHaveComponents(entity2, C3.class);

            verifyHasComponents(entity3, C1.class, C3.class);
            verifyDoesNotHaveComponents(entity3, C2.class);

            verifyComponentMaskHasComponents(entity3, C1.class, C3.class);
            verifyComponentMaskDoesNotHaveComponents(entity3, C2.class);
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

        @Nested
        static class WildcardTest extends AbstractWorldTest {

            @Test
            void ensureTestClassIsStatic() {
                // must be static and extend AbstractWorldTest so that each test has a fresh, empty world not altered by @BeforeEach of outer class
                assertThat(Modifier.isStatic(this.getClass().getModifiers())).as("test class is static").isTrue();
            }

            @Test
            void testInterfaceWildcard() {
                var remove12 = transmutationManager.getRemoveTransmuter(wildcard(C12.class));
                var remove123 = transmutationManager.getRemoveTransmuter(wildcard(C123.class));
                var remove23 = transmutationManager.getRemoveTransmuter(wildcard(C23.class));
                var remove = transmutationManager.getRemoveTransmuter(wildcard(C.class));

                var entity1 = world.createEntity(new C1(), new C2(), new C3());
                var entity2 = world.createEntity(new C1(), new C2(), new C3());
                var entity3 = world.createEntity(new C1(), new C2(), new C3());
                var entity4 = world.createEntity(new C1(), new C2(), new C3());

                // Call
                remove12.apply(entity1);
                remove123.apply(entity2);
                remove23.apply(entity3);
                remove.apply(entity4);

                world.process();

                // Verify
                verifyHasComponents(entity1, C3.class);
                verifyDoesNotHaveComponents(entity1, C1.class, C2.class);
                verifyComponentMaskHasComponents(entity1, C3.class);
                verifyComponentMaskDoesNotHaveComponents(entity1, C1.class, C2.class);

                verifyDoesNotHaveComponents(entity2, C1.class, C2.class, C3.class);
                verifyComponentMaskDoesNotHaveComponents(entity2, C1.class, C2.class, C3.class);

                verifyHasComponents(entity3, C1.class);
                verifyDoesNotHaveComponents(entity3, C2.class, C3.class);
                verifyComponentMaskHasComponents(entity3, C1.class);
                verifyComponentMaskDoesNotHaveComponents(entity3, C2.class, C3.class);

                verifyDoesNotHaveComponents(entity4, C1.class, C2.class, C3.class);
                verifyComponentMaskDoesNotHaveComponents(entity4, C1.class, C2.class, C3.class);
            }

            @Test
            void testInterfaceWildcard_ComponentsKnownBeforehand() {
                componentManager.getComponent(component(C1.class));
                componentManager.getComponent(component(C2.class));
                componentManager.getComponent(component(C3.class));

                var remove12 = transmutationManager.getRemoveTransmuter(wildcard(C12.class));
                var remove123 = transmutationManager.getRemoveTransmuter(wildcard(C123.class));
                var remove23 = transmutationManager.getRemoveTransmuter(wildcard(C23.class));
                var remove = transmutationManager.getRemoveTransmuter(wildcard(C.class));

                var entity1 = world.createEntity(new C1(), new C2(), new C3());
                var entity2 = world.createEntity(new C1(), new C2(), new C3());
                var entity3 = world.createEntity(new C1(), new C2(), new C3());
                var entity4 = world.createEntity(new C1(), new C2(), new C3());

                // Call
                remove12.apply(entity1);
                remove123.apply(entity2);
                remove23.apply(entity3);
                remove.apply(entity4);

                world.process();

                // Verify
                verifyHasComponents(entity1, C3.class);
                verifyDoesNotHaveComponents(entity1, C1.class, C2.class);
                verifyComponentMaskHasComponents(entity1, C3.class);
                verifyComponentMaskDoesNotHaveComponents(entity1, C1.class, C2.class);

                verifyDoesNotHaveComponents(entity2, C1.class, C2.class, C3.class);
                verifyComponentMaskDoesNotHaveComponents(entity2, C1.class, C2.class, C3.class);

                verifyHasComponents(entity3, C1.class);
                verifyDoesNotHaveComponents(entity3, C2.class, C3.class);
                verifyComponentMaskHasComponents(entity3, C1.class);
                verifyComponentMaskDoesNotHaveComponents(entity3, C2.class, C3.class);

                verifyDoesNotHaveComponents(entity4, C1.class, C2.class, C3.class);
                verifyComponentMaskDoesNotHaveComponents(entity4, C1.class, C2.class, C3.class);
            }

            @Test
            void testWildcard() {
                var removeConstant = transmutationManager.getRemoveTransmuter(WILDCARD);
                var removeObject = transmutationManager.getRemoveTransmuter(wildcard(Object.class));

                var entity1 = world.createEntity(new C1(), new C2(), new C3());
                var entity2 = world.createEntity(new C1(), new C2(), new C3());

                // Call
                removeConstant.apply(entity1);
                removeObject.apply(entity2);

                world.process();

                // Verify
                verifyDoesNotHaveComponents(entity1, C1.class, C2.class, C3.class);
                verifyComponentMaskDoesNotHaveComponents(entity1, C1.class, C2.class, C3.class);

                verifyDoesNotHaveComponents(entity2, C1.class, C2.class, C3.class);
                verifyComponentMaskDoesNotHaveComponents(entity2, C1.class, C2.class, C3.class);
            }

            @Test
            void testWildcard_ComponentsKnownBeforehand() {
                componentManager.getComponent(component(C1.class));
                componentManager.getComponent(component(C2.class));
                componentManager.getComponent(component(C3.class));

                var removeConstant = transmutationManager.getRemoveTransmuter(WILDCARD);
                var removeObject = transmutationManager.getRemoveTransmuter(wildcard(Object.class));

                var entity1 = world.createEntity(new C1(), new C2(), new C3());
                var entity2 = world.createEntity(new C1(), new C2(), new C3());

                // Call
                removeConstant.apply(entity1);
                removeObject.apply(entity2);

                world.process();

                // Verify
                verifyDoesNotHaveComponents(entity1, C1.class, C2.class, C3.class);
                verifyComponentMaskDoesNotHaveComponents(entity1, C1.class, C2.class, C3.class);

                verifyDoesNotHaveComponents(entity2, C1.class, C2.class, C3.class);
                verifyComponentMaskDoesNotHaveComponents(entity2, C1.class, C2.class, C3.class);
            }

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

    @Nested
    class AbstractTransmuterTest {

        @Test
        void testApplyAdd() {
            var builder = new CustomBuilder().add(component(C1.class), component(C2.class));
            var transmuter = new CustomTransmuter(transmutationManager, builder);

            var entityId = world.createEntity();
            verifyDoesNotHaveComponents(entityId, C1.class, C2.class);
            verifyComponentMaskDoesNotHaveComponents(entityId, C1.class, C2.class);

            // Call
            transmuter.apply(entityId, new C1(), new C2());

            world.process();

            // Verify
            verifyHasComponents(entityId, C1.class, C2.class);
            verifyComponentMaskHasComponents(entityId, C1.class, C2.class);
        }

        @Test
        void testApplyAdd_IncorrectOrder_Throws() {
            var builder = new CustomBuilder().add(component(C1.class), component(C2.class));
            var transmuter = new CustomTransmuter(transmutationManager, builder);

            var entityId = world.createEntity();
            verifyDoesNotHaveComponents(entityId, C1.class, C2.class);
            verifyComponentMaskDoesNotHaveComponents(entityId, C1.class, C2.class);

            // Call
            assertThatThrownBy(() -> transmuter.apply(entityId, new C2(), new C1()));
        }

        @Test
        void testApplyAdd_FewerComponentsThanExpected() {
            var builder = new CustomBuilder().add(component(C1.class), component(C2.class));
            var transmuter = new CustomTransmuter(transmutationManager, builder);

            var entityId = world.createEntity();
            verifyDoesNotHaveComponents(entityId, C1.class, C2.class);
            verifyComponentMaskDoesNotHaveComponents(entityId, C1.class, C2.class);

            // Call
            transmuter.apply(entityId, new C1());

            world.process();

            // Verify
            verifyHasComponents(entityId, C1.class);
            verifyDoesNotHaveComponents(entityId, C2.class);

            verifyComponentMaskHasComponents(entityId, C1.class, C2.class);
        }

        @Test
        void testApplyAdd_MoreComponentsThanExpected() {
            var builder = new CustomBuilder().add(component(C1.class), component(C2.class));
            var transmuter = new CustomTransmuter(transmutationManager, builder);

            var entityId = world.createEntity();
            verifyDoesNotHaveComponents(entityId, C1.class, C2.class);
            verifyComponentMaskDoesNotHaveComponents(entityId, C1.class, C2.class);

            // Call
            assertThatThrownBy(() -> transmuter.apply(entityId, new C1(), new C2(), new C3()))
                    .isInstanceOf(ArrayIndexOutOfBoundsException.class)
                    .hasMessage("Index 2 out of bounds for length 2");
        }

        @Test
        void testApplyRemove() {
            var builder = new CustomBuilder().remove(component(C1.class), component(C2.class));
            var transmuter = new CustomTransmuter(transmutationManager, builder);

            var entityId = world.createEntity(new C1(), new C2(), new C3());
            verifyHasComponents(entityId, C1.class, C2.class);
            verifyComponentMaskHasComponents(entityId, C1.class, C2.class);

            // Call
            transmuter.apply(entityId);

            world.process();

            // Verify
            verifyHasComponents(entityId, C3.class);
            verifyDoesNotHaveComponents(entityId, C1.class, C2.class);

            verifyComponentMaskHasComponents(entityId, C3.class);
            verifyComponentMaskDoesNotHaveComponents(entityId, C1.class, C2.class);
        }

        private static class CustomBuilder implements Builder {

            private final SequencedSet<RegularComponentType<?, ?>> add = new LinkedHashSet<>();
            private final SequencedSet<ComponentType<?, ?>> remove = new LinkedHashSet<>();

            private CustomBuilder add(RegularComponentType<?, ?>... types) {
                for (var type : types) {
                    this.add.add(type);
                }

                return this;
            }

            private CustomBuilder remove(ComponentType<?, ?>... types) {
                for (var type : types) {
                    this.remove.add(type);
                }

                return this;
            }

            @Override
            public SequencedSet<RegularComponentType<?, ?>> getAdd() {
                return add;
            }

            @Override
            public SequencedSet<ComponentType<?, ?>> getRemove() {
                return remove;
            }

        }

        private static class CustomTransmuter extends AbstractTransmuter {

            protected CustomTransmuter(TransmutationManager manager, Builder builder) {
                super(manager, builder);
            }

        }

    }

    interface C {
    }

    interface C12 extends C {
    }

    interface C123 extends C {
    }

    interface C23 extends C {
    }

    public record C1() implements C12, C123 {
    }

    public record C2() implements C12, C123, C23 {
    }

    public record C3() implements C123, C23 {
    }

}
