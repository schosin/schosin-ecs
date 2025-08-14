package de.schosin.ecs.storage.testsuite.entities;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import de.schosin.ecs.storage.api.entities.Archetype;
import de.schosin.ecs.storage.testsuite.AbstractStorageEngineTest;

public class ObserverTest extends AbstractStorageEngineTest {

    @Nested
    class EntityCreatedTest {

        @Test
        void testObserver() {
            var emptyArchetype = storageEngine.getArchetype();
            var archetype1 = storageEngine.getArchetype(component(C1.class));

            var data = new ArrayList<EntityData>();
            storageEngine.registerCreated((archetype, entityId) -> data.add(new EntityData(null, archetype, entityId)));

            // Call
            var entity1 = world.createEntity();
            var entity2 = world.createEntity(new C1());

            // Verify
            assertThat(data)
                    .extracting("newArchetype", "entityId")
                    .as("must call observers in order")
                    .containsExactly(
                            tuple(emptyArchetype, entity1),
                            tuple(archetype1, entity2));
        }

    }

    @Nested
    class EntitiesCreatedTest {

        @Test
        void testObserver() {
            var archetype1 = storageEngine.getArchetype(component(C1.class));

            var counter = new AtomicInteger();
            storageEngine.registerBatchCreated((archetype, entities) -> {
                assertThat(archetype).as("must match archetype used for creation").isSameAs(archetype1);
                assertThat(entities.getSize()).as("must contain created entities").isEqualTo(2);
                assertThat(entities.iterator()).toIterable().as("must contain created entities").containsExactlyInAnyOrder(1, 2);

                counter.incrementAndGet();
            });

            // Call
            var ids = new AtomicInteger();
            archetype1.createEntities(2, ids::incrementAndGet, (components, i) -> components[0] = new C1());

            // Verify
            assertThat(counter.get()).isEqualTo(1);
        }

    }

    @Nested
    class EntityUpdateTest {

        @Test
        void testObserver_UnbatchedCreation() {
            var archetype2 = storageEngine.getArchetype(component(C2.class));
            var archetype12 = storageEngine.getArchetype(component(C1.class), component(C2.class));

            // Modify during creation 
            var c2mapper = world.getComponents(C2.class);
            storageEngine.registerCreated((archetype, entityId) -> c2mapper.add(entityId, new C2()));

            // Track updates
            var data = new ArrayList<EntityData>();
            storageEngine.registerBeforeUpdate((archetype, newArchetype, entityId) -> data.add(new EntityData(archetype, newArchetype, entityId, "before")));
            storageEngine.registerUpdated((archetype, oldArchetype, entityId) -> data.add(new EntityData(oldArchetype, archetype, entityId, "after")));

            // Call
            var entity1 = world.createEntity();
            var entity2 = world.createEntity(new C1());

            // Verify
            assertThat(data)
                    .extracting("newArchetype", "entityId", "payload")
                    .as("must call observers in order")
                    .containsExactly(
                            tuple(archetype2, entity1, "before"),
                            tuple(archetype2, entity1, "after"),
                            tuple(archetype12, entity2, "before"),
                            tuple(archetype12, entity2, "after"));
        }

        @Test
        void testObserver_BatchedCreation() {
            var archetype1 = storageEngine.getArchetype(component(C1.class));
            var archetype12 = storageEngine.getArchetype(component(C1.class), component(C2.class));

            // Modify during creation 
            var c2mapper = world.getComponents(C2.class);
            storageEngine.registerBatchCreated((archetype, entities) -> {
                assertThat(entities.getSize()).isEqualTo(2);

                c2mapper.add(entities.get(0), new C2());
                c2mapper.add(entities.get(1), new C2());
            });

            // Track updates
            var data = new ArrayList<EntityData>();
            storageEngine.registerBeforeUpdate((archetype, newArchetype, entityId) -> data.add(new EntityData(archetype, newArchetype, entityId, "before")));
            storageEngine.registerUpdated((archetype, oldArchetype, entityId) -> data.add(new EntityData(oldArchetype, archetype, entityId, "after")));

            // Call
            var ids = new AtomicInteger();
            archetype1.createEntities(2, ids::incrementAndGet, (components, i) -> components[0] = new C1());

            // Verify
            assertThat(data)
                    .extracting("newArchetype", "entityId", "payload")
                    .as("must call observers in order")
                    .containsExactly(
                            tuple(archetype12, 1, "before"), // hardcoded ids because atomic integer will return 1 and 2
                            tuple(archetype12, 1, "after"),
                            tuple(archetype12, 2, "before"),
                            tuple(archetype12, 2, "after"));
        }

    }

    @Nested
    class EntitiesUpdateTest {

        @Test
        void testObserver() {
            var c2mapper = world.getComponents(C2.class);

            var emptyArchetype = storageEngine.getArchetype();
            var archetype1 = storageEngine.getArchetype(component(C1.class));
            var archetype2 = storageEngine.getArchetype(component(C2.class));
            var archetype12 = storageEngine.getArchetype(component(C1.class), component(C2.class));

            var entity1 = world.createEntity();
            var entity2 = world.createEntity(new C1());
            var entity3 = world.createEntity(new C1());

            var counterBefore = new AtomicInteger();
            storageEngine.registerBatchBeforeUpdate((archetype, newArchetype, entities) -> {
                if (archetype == emptyArchetype) {
                    assertThat(newArchetype).as("must have expected new archetype after addition").isSameAs(archetype2);
                    assertThat(entities.iterator()).toIterable().as("must contain created entities").containsExactlyInAnyOrder(entity1);
                } else if (archetype == archetype1) {
                    assertThat(newArchetype).as("must have expected new archetype after addition").isSameAs(archetype12);
                    assertThat(entities.iterator()).toIterable().as("must contain created entities").containsExactlyInAnyOrder(entity2, entity3);
                } else {
                    fail("unexpected archetype: " + archetype);
                }

                counterBefore.incrementAndGet();
            });

            var counterAfter = new AtomicInteger();
            storageEngine.registerBatchUpdated((archetype, oldArchetype, entities) -> {
                if (archetype == archetype2) {
                    assertThat(oldArchetype).as("must have expected old archetype after addition").isSameAs(emptyArchetype);
                    assertThat(entities.iterator()).toIterable().as("must contain created entities").containsExactlyInAnyOrder(entity1);
                } else if (archetype == archetype12) {
                    assertThat(oldArchetype).as("must have expected old archetype after addition").isSameAs(archetype1);
                    assertThat(entities.iterator()).toIterable().as("must contain created entities").containsExactlyInAnyOrder(entity2, entity3);
                } else {
                    fail("unexpected archetype: " + archetype);
                }

                counterAfter.incrementAndGet();
            });

            // Call
            c2mapper.add(entity1, new C2());
            c2mapper.add(entity2, new C2());
            c2mapper.add(entity3, new C2());

            world.process();

            // Verify
            assertThat(counterBefore.get()).isEqualTo(2);
            assertThat(counterAfter.get()).isEqualTo(2);
        }

    }

    @Nested
    class EntitiesDeletedObserverTest {

        @Test
        void testObserver() {
            var emptyArchetype = storageEngine.getArchetype();
            var archetype1 = storageEngine.getArchetype(component(C1.class));

            var entity1 = world.createEntity();
            var entity2 = world.createEntity(new C1());
            var entity3 = world.createEntity(new C1());

            var counter = new AtomicInteger();
            storageEngine.registerDeleted((archetype, entities) -> {
                if (archetype == emptyArchetype) {
                    assertThat(entities.iterator()).toIterable().as("must contain created entities").containsExactlyInAnyOrder(entity1);
                } else if (archetype == archetype1) {
                    assertThat(entities.iterator()).toIterable().as("must contain created entities").containsExactlyInAnyOrder(entity2, entity3);
                } else {
                    fail("unexpected archetype: " + archetype);
                }

                counter.incrementAndGet();
            });

            // Call
            world.deleteEntity(entity1);
            world.deleteEntity(entity2);
            world.deleteEntity(entity3);

            world.process();

            // Verify
            assertThat(counter.get()).isEqualTo(2);
        }

    }

    private record EntityData(Archetype oldArchetype, Archetype newArchetype, int entityId, String payload) {
        public EntityData(Archetype oldArchetype, Archetype newArchetype, int entityId) {
            this(oldArchetype, newArchetype, entityId, null);
        }
    }

    record C1() {
    }

    record C2() {
    }

}
