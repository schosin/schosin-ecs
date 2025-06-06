package de.schosin.ecs.plugins.transmuter;

import static de.schosin.ecs.api.components.types.ComponentType.WILDCARD;
import static de.schosin.ecs.api.components.types.ComponentType.wildcard;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import de.schosin.ecs.api.components.Relation;
import de.schosin.ecs.api.components.types.ComponentType;
import de.schosin.ecs.api.components.types.ComponentType.RegularComponentType;
import de.schosin.ecs.codegen.EcsCodegen;
import de.schosin.ecs.engine.events.builtin.EntityEvent.EntityInsertedEvent;
import de.schosin.ecs.engine.events.builtin.EntityEvent.EntityRemovedEvent;
import de.schosin.ecs.engine.events.builtin.EntityEvent.EntityUpdatedEvent;
import de.schosin.ecs.engine.utils.ArrayUtils;
import de.schosin.ecs.plugins.transmuter.Transmuter.Remove;
import de.schosin.ecs.storage.api.StorageEngineException;
import de.schosin.ecs.utils.collections.IntBag;

@EcsCodegen
class TransmuterManagerTest extends BaseTransmuterManagerTest {

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
        assertThatThrownBy(() -> add1.apply(42, new C1())).isInstanceOf(StorageEngineException.class).hasMessageContaining("not present in storage");
        assertThatThrownBy(() -> add1add2.apply(42, new C1(), new C2())).isInstanceOf(StorageEngineException.class).hasMessageContaining("not present in storage");
        assertThatThrownBy(() -> remove2.apply(42)).isInstanceOf(StorageEngineException.class).hasMessageContaining("not present in storage");
        assertThatThrownBy(() -> remove1remove2.apply(42)).isInstanceOf(StorageEngineException.class).hasMessageContaining("not present in storage");
        assertThatThrownBy(() -> add1remove2.apply(42, new C1())).isInstanceOf(StorageEngineException.class).hasMessageContaining("not present in storage");
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
        add1remove2.apply(entityId, new C1());
        world.process();

        // Verify
        verifyHasComponents(entityId, C1.class);
        verifyDoesNotHaveComponents(entityId, C2.class);

        verifyComponentMaskHasComponents(entityId, C1.class);
        verifyComponentMaskDoesNotHaveComponents(entityId, C2.class);
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

            var changed = new ArrayList<Integer>();
            eventManager.registerEventHandler(EntityInsertedEvent.class, event -> changed.add(event.entityId()));
            eventManager.registerEventHandler(EntityUpdatedEvent.class, event -> changed.add(event.entityId()));
            eventManager.registerEventHandler(EntityRemovedEvent.class, event -> changed.add(event.entityId()));

            remove1.apply(entityId);

            // Call
            add1.apply(entityId, newC1);
            world.process();

            // Verify
            assertThat(changed).as("no composition changes triggered").isEmpty();

            assertThat(getComponent(entityId, C1.class)).isSameAs(newC1);
            verifyHasComponents(entityId, C1.class);
            verifyComponentMaskHasComponents(entityId, C1.class);
        }

        @Test
        void testAdd_NoInstance() {
            // Setup
            var entityId = world.createEntity();
            verifyDoesNotHaveComponents(entityId, C1.class);
            verifyComponentMaskDoesNotHaveComponents(entityId, C1.class);

            // Call
            assertThatThrownBy(() -> add1.apply(entityId, null))
                    .isInstanceOf(StorageEngineException.class)
                    .hasMessageContaining(C1.class.getSimpleName());

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
        void testAdd_ChangeManagerUpdatedCallback() throws Exception {
            // Setup
            var entityId = world.createEntity();

            try (var verify = createVerify()) {

            }

            var updated = new IntBag(1);
            eventManager.registerEventHandler(EntityUpdatedEvent.class, event -> {
                assertThat(event.previousComponentMask().getComponents()).isEmpty();
                assertThat(event.componentMask().getComponents()).containsExactly(componentManager.getComponent(component(C1.class)));

                updated.add(event.entityId());
            });

            // Call
            add1.apply(entityId, new C1());
            world.process();

            // Verify
            assertThat(updated.getSize()).as("size").isEqualTo(1);
            assertThat(updated.getData()).contains(entityId);
        }

        @Test
        void testAdd_ChangeManagerUpdatedCallback_NotCalledIfCompositionUnchanged() {
            // Setup
            var entityId = world.createEntity(new C1());

            var updated = new IntBag(1);
            eventManager.registerEventHandler(EntityUpdatedEvent.class, event -> updated.add(event.entityId()));

            // Call
            add1.apply(entityId, new C1());
            world.process();

            // Verify
            assertThat(updated.getSize()).as("size").isEqualTo(0);
            assertThat(updated.getData()).doesNotContain(entityId);
        }

        @Test
        void testAddMultiple() {
            // Setup
            var entityId = world.createEntity();
            verifyDoesNotHaveComponents(entityId, C1.class);
            verifyDoesNotHaveComponents(entityId, C2.class);
            verifyComponentMaskDoesNotHaveComponents(entityId, C1.class, C2.class);

            // Call
            add1add2.apply(entityId, new C1(), new C2());
            world.process();

            // Verify
            verifyHasComponents(entityId, C1.class);
            verifyHasComponents(entityId, C2.class);
            verifyComponentMaskHasComponents(entityId, C1.class, C2.class);
        }

        @Test
        void testAddMultiple_NoInstances() {
            var c1 = new C1();
            var c2 = new C2();

            // Setup
            var entityId = world.createEntity();
            verifyDoesNotHaveComponents(entityId, C1.class);
            verifyDoesNotHaveComponents(entityId, C2.class);
            verifyComponentMaskDoesNotHaveComponents(entityId, C1.class, C2.class);

            // Call
            assertThatThrownBy(() -> add1add2.apply(entityId, null, null))
                    .isInstanceOf(StorageEngineException.class)
                    .hasMessageContaining(C1.class.getSimpleName(), C2.class.getSimpleName());

            assertThatThrownBy(() -> add1add2.apply(entityId, c1, null))
                    .isInstanceOf(StorageEngineException.class)
                    .hasMessageContaining(C2.class.getSimpleName());

            assertThatThrownBy(() -> add1add2.apply(entityId, null, c2))
                    .isInstanceOf(StorageEngineException.class)
                    .hasMessageContaining(C1.class.getSimpleName());

            // Verify
            verifyDoesNotHaveComponents(entityId, C1.class, C2.class);
            verifyComponentMaskDoesNotHaveComponents(entityId, C1.class, C2.class);
        }

        @Test
        void testAddNull_Throws() {
            // Setup
            var entityId = world.createEntity();
            verifyDoesNotHaveComponents(entityId, C1.class);
            verifyComponentMaskDoesNotHaveComponents(entityId, C1.class);

            // Call
            assertThatThrownBy(() -> add1.apply(entityId, null))
                    .isInstanceOf(StorageEngineException.class)
                    .hasMessageContaining(C1.class.getSimpleName());

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

            assertThatThrownBy(() -> add1.apply(entityId, null))
                    .isInstanceOf(StorageEngineException.class)
                    .hasMessageContaining(C1.class.getSimpleName());

            // Call
            world.process();

            // Verify
            verifyDoesNotHaveComponents(entityId, C1.class);
            verifyComponentMaskDoesNotHaveComponents(entityId, C1.class);
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
                verifyHasComponents(entityId, classes);
                verifyComponentMaskHasComponents(entityId, classes);
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
            verifyHasComponents(entityId, C1.class, C2.class, C3.class);
            verifyComponentMaskHasComponents(entityId, C1.class, C2.class, C3.class);

            // Remove 1
            remove1.apply(entityId);
            world.process();

            // Verify
            verifyDoesNotHaveComponents(entityId, C1.class);
            verifyHasComponents(entityId, C2.class, C3.class);

            verifyComponentMaskHasComponents(entityId, C2.class, C3.class);
            verifyComponentMaskDoesNotHaveComponents(entityId, C1.class);
        }

        @Test
        void testRemoveMultiple() {
            // Setup
            var remove23 = world.createTransmuter(Transmuter.remove(C2.class, C3.class));

            var entityId = world.createEntity(new C1(), new C2(), new C3());
            verifyHasComponents(entityId, C1.class, C2.class, C3.class);
            verifyComponentMaskHasComponents(entityId, C1.class, C2.class, C3.class);

            // Remove 2 & 3
            remove23.apply(entityId);
            world.process();

            // Verify
            verifyHasComponents(entityId, C1.class);
            verifyDoesNotHaveComponents(entityId, C2.class, C3.class);

            verifyComponentMaskDoesNotHaveComponents(entityId, C2.class, C3.class);
            verifyComponentMaskHasComponents(entityId, C1.class);
        }

        @Test
        void testRemove_CompositionRemovedCalled() {
            // Setup
            var remove1 = world.createTransmuter(Transmuter.remove(C1.class));
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
            var remove1 = world.createTransmuter(Transmuter.remove(C1.class));
            var entityId = world.createEntity(new C2(), new C3());

            verify(verify -> {
                verify.expectNoMoreUpdated();

                // Remove 1
                remove1.apply(entityId);
                world.process();
            });
        }

        abstract class AbstractRemoveByAddTest {

            @Test
            void testRemove() {
                // Setup
                var entityId = world.createEntity(new D1(), new D2());
                verifyHasComponents(entityId, D1.class, D2.class);
                verifyComponentMaskHasComponents(entityId, D1.class, D2.class);

                verify(verify -> {
                    // Remove 1
                    var added = apply(entityId, D1.class);
                    var expected = ArrayUtils.concat(Class.class, added, D2.class);

                    verify.expectUpdated(entityId, expected);
                    verify.expectNoMoreUpdated();

                    // Process
                    world.process();

                    // Verify
                    verifyDoesNotHaveComponents(entityId, D1.class);
                    verifyHasComponents(entityId, D2.class);
                    verifyComponentMaskHasComponents(entityId, expected);
                });
            }

            @Test
            void testRemoveMultiple() {
                // Setup
                var entityId = world.createEntity(new D1(), new D2());
                verifyHasComponents(entityId, D1.class, D2.class);
                verifyComponentMaskHasComponents(entityId, D1.class, D2.class);

                verify(verify -> {
                    // Remove 2
                    var added = apply(entityId, D1.class, D2.class);

                    verify.expectUpdated(entityId, added);
                    verify.expectNoMoreUpdated();

                    // Process
                    world.process();

                    // Verify
                    verifyDoesNotHaveComponents(entityId, D1.class, D2.class);
                    verifyComponentMaskHasComponents(entityId, added);
                });
            }

            @Test
            void testRemoveWildcard_WhenWorldNotProcessed_DoesNotAlterYet() {
                // Setup
                var entityId = world.createEntity(new D1(), new D2());
                verifyHasComponents(entityId, D1.class, D2.class);
                verifyComponentMaskHasComponents(entityId, D1.class, D2.class);

                verify(verify -> {
                    verify.expectNoMoreUpdated();

                    // Remove 2
                    apply(entityId, WILDCARD);

                    // Verify
                    verifyHasComponents(entityId, D1.class, D2.class);
                    verifyComponentMaskHasComponents(entityId, D1.class, D2.class);
                });
            }

            @Test
            void testRemoveWildcardConstant() {
                // Setup
                var entityId = world.createEntity(new D1(), new D2());
                verifyHasComponents(entityId, D1.class, D2.class);
                verifyComponentMaskHasComponents(entityId, D1.class, D2.class);

                verify(verify -> {
                    // Remove 2
                    var added = apply(entityId, WILDCARD);

                    verify.expectUpdated(entityId, NO_COMPONENTS);
                    verify.expectNoMoreUpdated();

                    // Process
                    world.process();

                    // Verify
                    verifyDoesNotHaveComponents(entityId, added);
                    verifyDoesNotHaveComponents(entityId, D1.class, D2.class);

                    verifyComponentMaskDoesNotHaveComponents(entityId, added);
                    verifyComponentMaskDoesNotHaveComponents(entityId, D1.class, D2.class);
                });
            }

            @Test
            void testRemoveWildcardInterface() {
                // Setup
                var entityId = world.createEntity(new A1(), new A2());
                verifyHasComponents(entityId, A1.class, A2.class);
                verifyComponentMaskHasComponents(entityId, A1.class, A2.class);

                verify(verify -> {
                    // Remove 2
                    var added = apply(entityId, wildcard(A.class));

                    verify.expectUpdated(entityId, added);
                    verify.expectNoMoreUpdated();

                    // Process
                    world.process();

                    // Verify
                    verifyHasComponents(entityId, added);
                    verifyDoesNotHaveComponents(entityId, A1.class, A2.class);

                    verifyComponentMaskHasComponents(entityId, added);
                    verifyComponentMaskDoesNotHaveComponents(entityId, A1.class, A2.class);
                });
            }

            interface A {
            }

            record A1() implements A {
            }

            record A2() implements A {
            }

            protected abstract Class<?>[] apply(int entityId, Class<?>... remove);

            protected abstract Class<?>[] apply(int entityId, ComponentType<?, ?>... remove);

        }

        @Nested
        class Add1Remove extends AbstractRemoveByAddTest {

            private static final Class<?>[] ADDED = { C1.class };

            @Override
            protected Class<?>[] apply(int entityId, Class<?>... remove) {
                return apply(entityId, convert(remove));
            }

            @Override
            protected Class<?>[] apply(int entityId, ComponentType<?, ?>... remove) {
                var transmuter = world.createTransmuter(Transmuter.add(C1.class).remove(remove));
                transmuter.apply(entityId, new C1());

                return ADDED;
            }

        }

        @Nested
        class Add2Remove extends AbstractRemoveByAddTest {

            private static final Class<?>[] ADDED = { C1.class, C2.class };

            @Override
            protected Class<?>[] apply(int entityId, Class<?>... remove) {
                return apply(entityId, convert(remove));
            }

            @Override
            protected Class<?>[] apply(int entityId, ComponentType<?, ?>... remove) {
                var transmuter = world.createTransmuter(Transmuter.add(C1.class, C2.class).remove(remove));
                transmuter.apply(entityId, new C1(), new C2());

                return ADDED;
            }

        }

        @Nested
        class Add3Remove extends AbstractRemoveByAddTest {

            private static final Class<?>[] ADDED = { C1.class, C2.class, C3.class };

            @Override
            protected Class<?>[] apply(int entityId, Class<?>... remove) {
                return apply(entityId, convert(remove));
            }

            @Override
            protected Class<?>[] apply(int entityId, ComponentType<?, ?>... remove) {
                var transmuter = world.createTransmuter(Transmuter.add(C1.class, C2.class, C3.class).remove(remove));
                transmuter.apply(entityId, new C1(), new C2(), new C3());

                return ADDED;
            }
        }

        @Nested
        class Add4Remove extends AbstractRemoveByAddTest {

            private static final Class<?>[] ADDED = { C1.class, C2.class, C3.class, C4.class };

            @Override
            protected Class<?>[] apply(int entityId, Class<?>... remove) {
                return apply(entityId, convert(remove));
            }

            @Override
            protected Class<?>[] apply(int entityId, ComponentType<?, ?>... remove) {
                var transmuter = world.createTransmuter(Transmuter.add(C1.class, C2.class, C3.class, C4.class).remove(remove));
                transmuter.apply(entityId, new C1(), new C2(), new C3(), new C4());

                return ADDED;
            }
        }

        @Nested
        class Add5Remove extends AbstractRemoveByAddTest {

            private static final Class<?>[] ADDED = { C1.class, C2.class, C3.class, C4.class, C5.class };

            @Override
            protected Class<?>[] apply(int entityId, Class<?>... remove) {
                return apply(entityId, convert(remove));
            }

            @Override
            protected Class<?>[] apply(int entityId, ComponentType<?, ?>... remove) {
                var transmuter = world.createTransmuter(Transmuter.add(C1.class, C2.class, C3.class, C4.class, C5.class).remove(remove));
                transmuter.apply(entityId, new C1(), new C2(), new C3(), new C4(), new C5());

                return ADDED;
            }
        }

        @Nested
        class Add6Remove extends AbstractRemoveByAddTest {

            private static final Class<?>[] ADDED = { C1.class, C2.class, C3.class, C4.class, C5.class, C6.class };

            @Override
            protected Class<?>[] apply(int entityId, Class<?>... remove) {
                return apply(entityId, convert(remove));
            }

            @Override
            protected Class<?>[] apply(int entityId, ComponentType<?, ?>... remove) {
                var transmuter = world.createTransmuter(Transmuter.add(C1.class, C2.class, C3.class, C4.class, C5.class, C6.class).remove(remove));
                transmuter.apply(entityId, new C1(), new C2(), new C3(), new C4(), new C5(), new C6());

                return ADDED;
            }
        }

        @Nested
        class Add7Remove extends AbstractRemoveByAddTest {

            private static final Class<?>[] ADDED = { C1.class, C2.class, C3.class, C4.class, C5.class, C6.class, C7.class };

            @Override
            protected Class<?>[] apply(int entityId, Class<?>... remove) {
                return apply(entityId, convert(remove));
            }

            @Override
            protected Class<?>[] apply(int entityId, ComponentType<?, ?>... remove) {
                var transmuter = world.createTransmuter(Transmuter.add(C1.class, C2.class, C3.class, C4.class, C5.class, C6.class, C7.class).remove(remove));
                transmuter.apply(entityId, new C1(), new C2(), new C3(), new C4(), new C5(), new C6(), new C7());

                return ADDED;
            }
        }

        @Nested
        class Add8Remove extends AbstractRemoveByAddTest {

            private static final Class<?>[] ADDED = { C1.class, C2.class, C3.class, C4.class, C5.class, C6.class, C7.class, C8.class };

            @Override
            protected Class<?>[] apply(int entityId, Class<?>... remove) {
                return apply(entityId, convert(remove));
            }

            @Override
            protected Class<?>[] apply(int entityId, ComponentType<?, ?>... remove) {
                var transmuter = world.createTransmuter(Transmuter.add(C1.class, C2.class, C3.class, C4.class, C5.class, C6.class, C7.class, C8.class).remove(remove));
                transmuter.apply(entityId, new C1(), new C2(), new C3(), new C4(), new C5(), new C6(), new C7(), new C8());

                return ADDED;
            }
        }

        @Nested
        class AddNRemove extends AbstractRemoveByAddTest {

            @Override
            protected Class<?>[] apply(int entityId, Class<?>... remove) {
                return apply(entityId, convert(remove));
            }

            @Override
            protected Class<?>[] apply(int entityId, ComponentType<?, ?>... remove) {
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
        void testRemove1_WhenWorldNotProcessed_DoesNotChangeComposition() {
            var entityId = world.createEntity(new C1(), new C2(), new C3());

            // Call
            verify(verify -> {
                verify.expectNoMoreUpdated();

                remove1.apply(entityId);
            });
        }

        @Test
        void testRemove1_WhenWorldProcessed_ChangesComposition() {
            var entityId = world.createEntity(new C1(), new C2(), new C3());

            // Call
            verify(verify -> {
                verify.expectUpdated(entityId, C2.class, C3.class);
                verify.expectNoMoreUpdated();

                remove1.apply(entityId);
                world.process();
            });
        }

        @Test
        void testRemove1Remove2_WhenWorldNotProcessed_DoesNotChangeComposition() {
            var entityId = world.createEntity(new C1(), new C2(), new C3());

            // Call
            verify(verify -> {
                verify.expectNoMoreUpdated();

                remove1.apply(entityId);
                remove2.apply(entityId);
            });
        }

        @Test
        void testRemove1Remove2_WhenWorldProcessed_ChangesComposition() {
            var entityId = world.createEntity(new C1(), new C2(), new C3());

            // Call
            verify(verify -> {
                verify.expectUpdated(entityId, C3.class);
                verify.expectNoMoreUpdated();

                remove1.apply(entityId);
                remove2.apply(entityId);
                world.process();
            });
        }

        @Test
        void testRemove1Remove2Remove3_WhenWorldNotProcessed_DoesNotChangeComposition() {
            var entityId = world.createEntity(new C1(), new C2(), new C3());

            // Call
            verify(verify -> {
                verify.expectNoMoreUpdated();

                remove1.apply(entityId);
                remove2.apply(entityId);
                remove3.apply(entityId);
            });
        }

        @Test
        void testRemove1Remove2Remove3_WhenWorldProcessed_ChangesComposition() {
            var entityId = world.createEntity(new C1(), new C2(), new C3());

            // Call
            verify(verify -> {
                verify.expectUpdated(entityId);
                verify.expectNoMoreUpdated();

                remove1.apply(entityId);
                remove2.apply(entityId);
                remove3.apply(entityId);
                world.process();
            });
        }

    }

    @Nested
    class ComponentRelationTest {

        @Test
        void testAddRelation_WhenNotProcessed() {
            var type = relation(C1.class, C2.class);
            var add = world.createTransmuter(Transmuter.add(type));

            var entityId = world.createEntity();
            verifyDoesNotHaveComponents(entityId, type);
            verifyComponentMaskDoesNotHaveComponents(entityId, type);

            // Call
            add.apply(entityId, Relation.create(new C1(), new C2()));

            // Verify
            verifyHasComponents(entityId, type);
            verifyComponentMaskDoesNotHaveComponents(entityId, type);
        }

        @Test
        void testAddRelation_WhenProcessed_ChangesComponentMask() {
            var type = relation(C1.class, C2.class);
            var mapper = world.getComponents(type);
            var add = world.createTransmuter(Transmuter.add(type));

            var entityId = world.createEntity();
            verifyDoesNotHaveComponents(entityId, type);
            verifyComponentMaskDoesNotHaveComponents(entityId, type);

            // Call
            add.apply(entityId, Relation.create(new C1(), new C2()));

            world.process();

            // Verify
            assertThat(mapper.get(entityId)).hasSize(1);

            verifyHasComponents(entityId, type);
            verifyComponentMaskHasComponents(entityId, type);
        }

        @Test
        void testRemoveRelation_WhenNotProcessed() {
            var type = relation(C1.class, C2.class);
            var remove = world.createTransmuter(Transmuter.remove(type));

            var entityId = world.createEntity(Relation.create(new C1(), new C2()));
            verifyHasComponents(entityId, type);
            verifyComponentMaskHasComponents(entityId, type);

            // Call
            remove.apply(entityId);

            // Verify
            verifyHasComponents(entityId, type);
            verifyComponentMaskHasComponents(entityId, type);
        }

        @Test
        void testRemoveRelation_WhenProcessed_Removes() {
            var type = relation(C1.class, C2.class);
            var remove = world.createTransmuter(Transmuter.remove(type));

            var entityId = world.createEntity(Relation.create(new C1(), new C2()));
            verifyHasComponents(entityId, type);
            verifyComponentMaskHasComponents(entityId, type);

            // Call
            remove.apply(entityId);

            world.process();

            // Verify
            verifyDoesNotHaveComponents(entityId, type);
            verifyComponentMaskDoesNotHaveComponents(entityId, type);
        }

        @Test
        void testRemoveMultipleRelation() {
            var type = relation(C1.class, Target.class);
            var mapper = world.getComponents(type);
            var remove = world.createTransmuter(Transmuter.remove(type));

            var relation1 = Relation.create(new C1(), new Target(1));
            var relation2 = Relation.create(new C1(), new Target(2));

            var entityId = world.createEntity(relation1, relation2);
            assertThat(mapper.get(entityId)).hasSize(2);

            verifyHasComponents(entityId, type);
            verifyComponentMaskHasComponents(entityId, type);

            // Call
            remove.apply(entityId);

            world.process();

            // Verify
            assertThat(mapper.get(entityId)).isNull();

            verifyDoesNotHaveComponents(entityId, type);
            verifyComponentMaskDoesNotHaveComponents(entityId, type);
        }

    }

    @Nested
    class EntityRelationTest {

        @Test
        void testAddRelation_WhenNotProcessed() {
            var type = relation(C1.class);
            var add = world.createTransmuter(Transmuter.add(type));

            var target = world.createEntity();

            var entityId = world.createEntity();
            verifyDoesNotHaveComponents(entityId, type);
            verifyComponentMaskDoesNotHaveComponents(entityId, type);

            // Call
            add.apply(entityId, Relation.create(new C1(), target));

            // Verify
            verifyHasComponents(entityId, type);
            verifyComponentMaskDoesNotHaveComponents(entityId, type);
        }

        @Test
        void testAddRelation_WhenProcessed_ChangesComponentMask() {
            var type = relation(C1.class);
            var mapper = world.getComponents(type);
            var add = world.createTransmuter(Transmuter.add(type));

            var target = world.createEntity();

            var entityId = world.createEntity();
            verifyDoesNotHaveComponents(entityId, type);
            verifyComponentMaskDoesNotHaveComponents(entityId, type);

            // Call
            add.apply(entityId, Relation.create(new C1(), target));

            world.process();

            // Verify
            assertThat(mapper.get(entityId)).hasSize(1);

            verifyHasComponents(entityId, type);
            verifyComponentMaskHasComponents(entityId, type);
        }

        @Test
        void testRemoveRelation_WhenNotProcessed() {
            var type = relation(C1.class);
            var remove = world.createTransmuter(Transmuter.remove(type));

            var target = world.createEntity();

            var entityId = world.createEntity(Relation.create(new C1(), target));
            verifyHasComponents(entityId, type);
            verifyComponentMaskHasComponents(entityId, type);

            // Call
            remove.apply(entityId);

            // Verify
            verifyHasComponents(entityId, type);
            verifyComponentMaskHasComponents(entityId, type);
        }

        @Test
        void testRemoveRelation_WhenProcessed_Removes() {
            var type = relation(C1.class);
            var remove = world.createTransmuter(Transmuter.remove(type));

            var target = world.createEntity();

            var entityId = world.createEntity(Relation.create(new C1(), target));
            verifyHasComponents(entityId, type);
            verifyComponentMaskHasComponents(entityId, type);

            // Call
            remove.apply(entityId);

            world.process();

            // Verify
            verifyDoesNotHaveComponents(entityId, type);
            verifyComponentMaskDoesNotHaveComponents(entityId, type);
        }

        @Test
        void testRemoveMultipleRelation() {
            var type = relation(C1.class);
            var mapper = world.getComponents(type);
            var remove = world.createTransmuter(Transmuter.remove(type));

            var target1 = world.createEntity();
            var target2 = world.createEntity();

            var relation1 = Relation.create(new C1(), target1);
            var relation2 = Relation.create(new C1(), target2);

            var entityId = world.createEntity(relation1, relation2);
            assertThat(mapper.get(entityId)).hasSize(2);

            verifyHasComponents(entityId, type);
            verifyComponentMaskHasComponents(entityId, type);

            // Call
            remove.apply(entityId);

            world.process();

            // Verify
            assertThat(mapper.get(entityId)).isNull();

            verifyDoesNotHaveComponents(entityId, type);
            verifyComponentMaskDoesNotHaveComponents(entityId, type);
        }

    }

    private static RegularComponentType<?, ?>[] convert(Class<?>... classes) {
        return Arrays.stream(classes).map(ComponentType::component).toArray(RegularComponentType<?, ?>[]::new);
    }

    record Target(int value) {
    }

}
