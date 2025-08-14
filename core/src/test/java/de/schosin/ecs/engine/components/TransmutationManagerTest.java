package de.schosin.ecs.engine.components;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.LinkedHashSet;
import java.util.SequencedSet;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import de.schosin.ecs.api.components.types.ComponentType;
import de.schosin.ecs.api.components.types.ComponentType.RegularComponentType;
import de.schosin.ecs.engine.AbstractWorldTest;
import de.schosin.ecs.engine.components.TransmutationManager.AbstractTransmuter;
import de.schosin.ecs.engine.components.TransmutationManager.Builder;
import de.schosin.ecs.storage.api.StorageEngineException;

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
        assertThatThrownBy(() -> add1.apply(42, new C1())).isInstanceOf(StorageEngineException.class).hasMessageContaining("not present in storage");
        assertThatThrownBy(() -> add2.apply(42, new C2())).isInstanceOf(StorageEngineException.class).hasMessageContaining("not present in storage");
        assertThatThrownBy(() -> remove1.apply(42)).isInstanceOf(StorageEngineException.class).hasMessageContaining("not present in storage");
        assertThatThrownBy(() -> remove2.apply(42)).isInstanceOf(StorageEngineException.class).hasMessageContaining("not present in storage");
    }

    @Test
    void testAddRemove() {
        // Setup
        var entityId = world.createEntity(new C2());
        verifyDoesNotHaveComponents(entityId, C1.class);
        verifyHasComponents(entityId, C2.class);

        verifyArchetypeDoesNotHaveComponents(entityId, C1.class);
        verifyArchetypeHasComponents(entityId, C2.class);

        // Call
        add1.apply(entityId, new C1());
        remove2.apply(entityId);
        world.process();

        // Verify
        verifyHasComponents(entityId, C1.class);
        verifyDoesNotHaveComponents(entityId, C2.class);

        verifyArchetypeHasComponents(entityId, C1.class);
        verifyArchetypeDoesNotHaveComponents(entityId, C2.class);
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
            verifyArchetypeDoesNotHaveComponents(entityId, C1.class);

            // Call
            add1.apply(entityId, new C1());
            world.process();

            // Verify
            verifyHasComponents(entityId, C1.class);
            verifyArchetypeHasComponents(entityId, C1.class);
        }

        @Test
        void testAdd_WhenComponentPresent_ReplacesExisting() {
            // Setup
            var oldC1 = new C1();
            var newC1 = new C1();

            var entityId = world.createEntity(oldC1);
            verifyHasComponents(entityId, C1.class);
            verifyArchetypeHasComponents(entityId, C1.class);

            // Call
            add1.apply(entityId, newC1);
            world.process();

            // Verify
            assertThat(getComponent(entityId, C1.class)).isSameAs(newC1);
            verifyHasComponents(entityId, C1.class);
            verifyArchetypeHasComponents(entityId, C1.class);
        }

        @Test
        void testAdd_WhenPresentComponentMarkedForRemoval_ReplacesExisting() {
            // Setup
            var oldC1 = new C1();
            var newC1 = new C1();

            var entityId = world.createEntity(oldC1);
            verifyHasComponents(entityId, C1.class);
            verifyArchetypeHasComponents(entityId, C1.class);

            remove1.apply(entityId);

            // Call
            add1.apply(entityId, newC1);
            world.process();

            // Verify
            assertThat(getComponent(entityId, C1.class)).isSameAs(newC1);
            verifyHasComponents(entityId, C1.class);
            verifyArchetypeHasComponents(entityId, C1.class);
        }

        @Test
        void testAdd_WhenPresentComponentMarkedForRemoval_DoesNotTriggerCompositionChanges() {
            // Setup
            var oldC1 = new C1();
            var newC1 = new C1();

            var entityId = world.createEntity(oldC1);
            verifyHasComponents(entityId, C1.class);
            verifyArchetypeHasComponents(entityId, C1.class);

            verify(verify -> {
                verify.expectNoMoreUpdated();

                // Call
                remove1.apply(entityId);
                add1.apply(entityId, newC1);
                world.process();

                // Verify
                assertThat(getComponent(entityId, C1.class)).isSameAs(newC1);
                verifyHasComponents(entityId, C1.class);
                verifyArchetypeHasComponents(entityId, C1.class);
            });
        }

        @Test
        void testAdd_NoInstance() {
            // Setup
            var entityId = world.createEntity();
            verifyDoesNotHaveComponents(entityId, C1.class);
            verifyArchetypeDoesNotHaveComponents(entityId, C1.class);

            // Call
            assertThatThrownBy(() -> add1.apply(entityId, (C1) null))
                    .isInstanceOf(StorageEngineException.class)
                    .hasMessageContaining(C1.class.getSimpleName());

            // Verify
            verifyDoesNotHaveComponents(entityId, C1.class);
            verifyArchetypeDoesNotHaveComponents(entityId, C1.class);
        }

        @Test
        void testAdd_NoProcessDoesNotUpdateComposition() {
            // Setup
            var entityId = world.createEntity();
            verifyDoesNotHaveComponents(entityId, C1.class);
            verifyArchetypeDoesNotHaveComponents(entityId, C1.class);

            // Call
            add1.apply(entityId, new C1());

            // Verify
            verifyHasComponents(entityId, C1.class);
            verifyArchetypeDoesNotHaveComponents(entityId, C1.class);
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
            verifyArchetypeDoesNotHaveComponents(entityId, C1.class, C2.class);

            // Call
            add1.apply(entityId, new C1());
            add2.apply(entityId, new C2());
            world.process();

            // Verify
            verifyHasComponents(entityId, C1.class, C2.class);
            verifyArchetypeHasComponents(entityId, C1.class, C2.class);
        }

        @Test
        void testAddNull_Throws() {
            // Setup
            var entityId = world.createEntity();
            verifyDoesNotHaveComponents(entityId, C1.class);
            verifyArchetypeDoesNotHaveComponents(entityId, C1.class);

            // Call
            assertThatThrownBy(() -> add1.apply(entityId, (C1) null))
                    .isInstanceOf(StorageEngineException.class)
                    .hasMessageContaining(C1.class.getSimpleName());

            // Verify
            verifyDoesNotHaveComponents(entityId, C1.class);
            verifyArchetypeDoesNotHaveComponents(entityId, C1.class);
        }

        @Test
        void testAddNull_WhenWorldProcessed_DoesNotAlterComposition() {
            // Setup
            var entityId = world.createEntity();
            verifyDoesNotHaveComponents(entityId, C1.class);
            verifyArchetypeDoesNotHaveComponents(entityId, C1.class);

            assertThatThrownBy(() -> add1.apply(entityId, (C1) null))
                    .isInstanceOf(StorageEngineException.class)
                    .hasMessageContaining(C1.class.getSimpleName());

            // Call
            world.process();

            // Verify
            verifyDoesNotHaveComponents(entityId, C1.class);
            verifyArchetypeDoesNotHaveComponents(entityId, C1.class);
        }

    }

    @Nested
    class RemoveTest {

        @Test
        void testRemove() {
            // Setup
            var entityId = world.createEntity(new C1(), new C2(), new C3());
            verifyHasComponents(entityId, C1.class, C2.class, C3.class);
            verifyArchetypeHasComponents(entityId, C1.class, C2.class, C3.class);

            // Remove 1
            remove1.apply(entityId);
            world.process();

            // Verify
            verifyDoesNotHaveComponents(entityId, C1.class);
            verifyHasComponents(entityId, C2.class, C3.class);

            verifyArchetypeDoesNotHaveComponents(entityId, C1.class);
            verifyArchetypeHasComponents(entityId, C2.class, C3.class);
        }

        @Test
        void testRemoveMultipleComponents() {
            // Setup
            var entityId = world.createEntity(new C1(), new C2(), new C3());
            verifyHasComponents(entityId, C1.class, C2.class, C3.class);
            verifyArchetypeHasComponents(entityId, C1.class, C2.class, C3.class);

            // Remove 2 & 3
            remove2.apply(entityId);
            remove3.apply(entityId);

            world.process();

            // Verify
            verifyHasComponents(entityId, C1.class);
            verifyDoesNotHaveComponents(entityId, C2.class, C3.class);

            verifyArchetypeHasComponents(entityId, C1.class);
            verifyArchetypeDoesNotHaveComponents(entityId, C2.class, C3.class);
        }

        @Test
        void testRemoveMultipleEntities() {
            // Setup
            var entity1 = world.createEntity(new C1(), new C2(), new C3());
            verifyHasComponents(entity1, C1.class, C2.class, C3.class);
            verifyArchetypeHasComponents(entity1, C1.class, C2.class, C3.class);

            var entity2 = world.createEntity(new C1(), new C2(), new C3());
            verifyHasComponents(entity2, C1.class, C2.class, C3.class);
            verifyArchetypeHasComponents(entity2, C1.class, C2.class, C3.class);

            // Remove 2
            remove2.apply(entity1);
            remove2.apply(entity2);

            world.process();

            // Verify
            verifyHasComponents(entity1, C1.class, C3.class);
            verifyDoesNotHaveComponents(entity1, C2.class);

            verifyArchetypeHasComponents(entity1, C1.class, C3.class);
            verifyArchetypeDoesNotHaveComponents(entity1, C2.class);

            verifyHasComponents(entity2, C1.class, C3.class);
            verifyDoesNotHaveComponents(entity2, C2.class);

            verifyArchetypeHasComponents(entity2, C1.class, C3.class);
            verifyArchetypeDoesNotHaveComponents(entity2, C2.class);
        }

        // Verifies "entities.clear()" in ChangeManager  
        @Test
        void testRemove_DoesNotRemoveOnProcessOfReusedEntityId() {
            // Setup
            var entity1 = world.createEntity(new C1(), new C2(), new C3());
            verifyHasComponents(entity1, C1.class, C2.class, C3.class);
            verifyArchetypeHasComponents(entity1, C1.class, C2.class, C3.class);

            // Remove 2 
            remove2.apply(entity1);
            world.process();

            // Delete and create new entity
            world.deleteEntity(entity1);
            world.process();

            var entity2 = world.createEntity(new C1(), new C2(), new C3());
            assertThat(entity2).as("entityId reused").isEqualTo(entity1);

            verifyHasComponents(entity2, C1.class, C2.class, C3.class);
            verifyArchetypeHasComponents(entity2, C1.class, C2.class, C3.class);

            var entity3 = world.createEntity(new C1(), new C2(), new C3());

            // Remove 3
            remove2.apply(entity3); // trigger removals of C2
            remove3.apply(entity2);
            world.process();

            // Verify
            verifyHasComponents(entity2, C1.class, C2.class);
            verifyDoesNotHaveComponents(entity2, C3.class);

            verifyArchetypeHasComponents(entity2, C1.class, C2.class);
            verifyArchetypeDoesNotHaveComponents(entity2, C3.class);

            verifyHasComponents(entity3, C1.class, C3.class);
            verifyDoesNotHaveComponents(entity3, C2.class);

            verifyArchetypeHasComponents(entity3, C1.class, C3.class);
            verifyArchetypeDoesNotHaveComponents(entity3, C2.class);
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
            verifyArchetypeHasComponents(entityId, C1.class, C2.class, C3.class);
        }

        @Test
        void testRemove1_WhenWorldProcessed_ChangesComposition() {
            var entityId = world.createEntity(new C1(), new C2(), new C3());

            // Call
            remove1.apply(entityId);
            world.process();

            // Verify
            verifyArchetypeHasComponents(entityId, C2.class, C3.class);
            verifyArchetypeDoesNotHaveComponents(entityId, C1.class);
        }

        @Test
        void testRemove1Remove2_WhenWorldNotProcessed_DoesNotChangeComposition() {
            var entityId = world.createEntity(new C1(), new C2(), new C3());

            // Call
            remove1.apply(entityId);
            remove2.apply(entityId);

            // Verify
            verifyArchetypeHasComponents(entityId, C1.class, C2.class, C3.class);
        }

        @Test
        void testRemove1Remove2_WhenWorldProcessed_ChangesComposition() {
            var entityId = world.createEntity(new C1(), new C2(), new C3());

            // Call
            remove1.apply(entityId);
            remove2.apply(entityId);
            world.process();

            // Verify
            verifyArchetypeHasComponents(entityId, C3.class);
            verifyArchetypeDoesNotHaveComponents(entityId, C1.class, C2.class);
        }

        @Test
        void testRemove1Remove2Remove3_WhenWorldNotProcessed_DoesNotChangeComposition() {
            var entityId = world.createEntity(new C1(), new C2(), new C3());

            // Call
            remove1.apply(entityId);
            remove2.apply(entityId);
            remove3.apply(entityId);

            // Verify
            verifyArchetypeHasComponents(entityId, C1.class, C2.class, C3.class);
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
            verifyArchetypeDoesNotHaveComponents(entityId, C1.class, C2.class, C3.class);
        }

        @Test
        void testAdd1Remove2_ComponentAccessInCallbacks() {
            // Setup listener
            var mapper1 = world.getComponents(C1.class);
            var mapper2 = world.getComponents(C2.class);

            var removed = new AtomicBoolean(false);
            onBeforeEntityUpdate((archetype, newArchetype, id) -> {
                assertThat(mapper1.get(id)).isNotNull();
                removed.set(true);
            });

            var added = new AtomicBoolean(false);
            onEntityUpdated((archetype, newArchetype, id) -> {
                assertThat(mapper2.get(id)).isNotNull();
                added.set(true);
            });

            // Setup entity
            var entityId = world.createEntity(new C1());

            remove1.apply(entityId);
            add2.apply(entityId, new C2());

            // Call
            world.process();

            // Verify
            assertThat(removed.get()).isTrue();
            assertThat(added.get()).isTrue();

            assertThat(mapper1.get(entityId)).isNull();
            assertThat(mapper2.get(entityId)).isNotNull();
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
            verifyArchetypeDoesNotHaveComponents(entityId, C1.class, C2.class);

            // Call
            transmuter.apply(entityId, new C1(), new C2());

            world.process();

            // Verify
            verifyHasComponents(entityId, C1.class, C2.class);
            verifyArchetypeHasComponents(entityId, C1.class, C2.class);
        }

        @Test
        void testApplyAdd_IncorrectOrder_Throws() {
            var builder = new CustomBuilder().add(component(C1.class), component(C2.class));
            var transmuter = new CustomTransmuter(transmutationManager, builder);

            var entityId = world.createEntity();
            verifyDoesNotHaveComponents(entityId, C1.class, C2.class);
            verifyArchetypeDoesNotHaveComponents(entityId, C1.class, C2.class);

            // Call
            assertThatThrownBy(() -> transmuter.apply(entityId, new C2(), new C1()));
        }

        @Test
        void testApplyAdd_FewerComponentsThanExpected() {
            var builder = new CustomBuilder().add(component(C1.class), component(C2.class));
            var transmuter = new CustomTransmuter(transmutationManager, builder);

            var entityId = world.createEntity();
            verifyDoesNotHaveComponents(entityId, C1.class, C2.class);
            verifyArchetypeDoesNotHaveComponents(entityId, C1.class, C2.class);

            // Call
            assertThatThrownBy(() -> transmuter.apply(entityId, new C1()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContainingAll("Expected 2", "got 1");

            world.process();

            // Verify
            verifyDoesNotHaveComponents(entityId, C1.class, C2.class);
            verifyArchetypeDoesNotHaveComponents(entityId, C1.class, C2.class);
        }

        @Test
        void testApplyAdd_MoreComponentsThanExpected() {
            var builder = new CustomBuilder().add(component(C1.class), component(C2.class));
            var transmuter = new CustomTransmuter(transmutationManager, builder);

            var entityId = world.createEntity();
            verifyDoesNotHaveComponents(entityId, C1.class, C2.class);
            verifyArchetypeDoesNotHaveComponents(entityId, C1.class, C2.class);

            // Call
            assertThatThrownBy(() -> transmuter.apply(entityId, new C1(), new C2(), new C3()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContainingAll("Expected 2", "got 3");

            verifyDoesNotHaveComponents(entityId, C1.class, C2.class);
            verifyArchetypeDoesNotHaveComponents(entityId, C1.class, C2.class);
        }

        @Test
        void testApplyRemove() {
            var builder = new CustomBuilder().remove(component(C1.class), component(C2.class));
            var transmuter = new CustomTransmuter(transmutationManager, builder);

            var entityId = world.createEntity(new C1(), new C2(), new C3());
            verifyHasComponents(entityId, C1.class, C2.class);
            verifyArchetypeHasComponents(entityId, C1.class, C2.class);

            // Call
            transmuter.apply(entityId);

            world.process();

            // Verify
            verifyHasComponents(entityId, C3.class);
            verifyDoesNotHaveComponents(entityId, C1.class, C2.class);

            verifyArchetypeHasComponents(entityId, C3.class);
            verifyArchetypeDoesNotHaveComponents(entityId, C1.class, C2.class);
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
