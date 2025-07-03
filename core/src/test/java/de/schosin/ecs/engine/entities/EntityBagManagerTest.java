package de.schosin.ecs.engine.entities;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import de.schosin.ecs.api.components.ComponentSetConfig;
import de.schosin.ecs.api.entities.Entity;
import de.schosin.ecs.engine.AbstractWorldTest;
import de.schosin.ecs.utils.collections.IntBag;

class EntityBagManagerTest extends AbstractWorldTest {

    @Nested
    class ArchetypeBagTest {

        @Test
        void testAllEntities() {
            var entity1 = world.createEntity();
            var entity2 = world.createEntity(new C1());
            var entity3 = world.createEntity(new C2());
            var entity4 = world.createEntity(new C1(), new C2());

            var entities = world.getAllEntities();

            assertThat(entities.isEmpty()).isFalse();
            assertThat(entities.size()).isEqualTo(4);
            assertThat(entities.contains(entity1)).isTrue();
            assertThat(entities.contains(entity2)).isTrue();
            assertThat(entities.contains(entity3)).isTrue();
            assertThat(entities.contains(entity4)).isTrue();

            assertThat(entities).hasSize(4);
            assertThat(entities).extracting(Entity::id).containsExactlyInAnyOrder(entity1, entity2, entity3, entity4);
        }

        @Test
        void testAllEntities_EntitiesCreatedAfter() {
            var entities = world.getAllEntities();

            var entity1 = world.createEntity();
            var entity2 = world.createEntity(new C1());
            var entity3 = world.createEntity(new C2());
            var entity4 = world.createEntity(new C1(), new C2());

            assertThat(entities.isEmpty()).isFalse();
            assertThat(entities.size()).isEqualTo(4);
            assertThat(entities.contains(entity1)).isTrue();
            assertThat(entities.contains(entity2)).isTrue();
            assertThat(entities.contains(entity3)).isTrue();
            assertThat(entities.contains(entity4)).isTrue();

            assertThat(entities).hasSize(4);
            assertThat(entities).extracting(Entity::id).containsExactlyInAnyOrder(entity1, entity2, entity3, entity4);
        }

        @Test
        void testClass() {
            var entity0 = world.createEntity();
            var entity1 = world.createEntity(new C1());
            var entity12 = world.createEntity(new C1(), new C2());
            var entity2 = world.createEntity(new C2());

            var entities = world.getEntities(component(C1.class));

            assertThat(entities.isEmpty()).isFalse();
            assertThat(entities.size()).isEqualTo(2);
            assertThat(entities.contains(entity0)).isFalse();
            assertThat(entities.contains(entity1)).isTrue();
            assertThat(entities.contains(entity12)).isTrue();
            assertThat(entities.contains(entity2)).isFalse();

            assertThat(entities).hasSize(2);
            assertThat(entities).extracting(Entity::id).containsExactlyInAnyOrder(entity1, entity12);

            for (var entity : entities) {
                assertThat(entity.get(C1.class)).isNotNull();
            }
        }

        @Test
        void testClass_EntitiesCreatedAfter() {
            var entities = world.getEntities(component(C1.class));
            assertThat(entities).isEmpty();

            var entity0 = world.createEntity();
            var entity1 = world.createEntity(new C1());
            var entity12 = world.createEntity(new C1(), new C2());
            var entity2 = world.createEntity(new C2());

            assertThat(entities.isEmpty()).isFalse();
            assertThat(entities.size()).isEqualTo(2);
            assertThat(entities.contains(entity0)).isFalse();
            assertThat(entities.contains(entity1)).isTrue();
            assertThat(entities.contains(entity12)).isTrue();
            assertThat(entities.contains(entity2)).isFalse();

            assertThat(entities).hasSize(2);
            assertThat(entities).extracting(Entity::id).containsExactlyInAnyOrder(entity1, entity12);

            for (var entity : entities) {
                assertThat(entity.get(C1.class)).isNotNull();
            }
        }

        @Test
        void testComponentType() {
            var entity0 = world.createEntity();
            var entity1 = world.createEntity(new C1());
            var entity12 = world.createEntity(new C1(), new C2());
            var entity2 = world.createEntity(new C2());

            var entities = world.getEntities(C1.class);

            assertThat(entities.isEmpty()).isFalse();
            assertThat(entities.size()).isEqualTo(2);
            assertThat(entities.contains(entity0)).isFalse();
            assertThat(entities.contains(entity1)).isTrue();
            assertThat(entities.contains(entity12)).isTrue();
            assertThat(entities.contains(entity2)).isFalse();

            assertThat(entities).hasSize(2);
            assertThat(entities).extracting(Entity::id).containsExactlyInAnyOrder(entity1, entity12);

            for (var entity : entities) {
                assertThat(entity.get(C1.class)).isNotNull();
            }
        }

        @Test
        void testComponentType_EntitiesCreatedAfter() {
            var entities = world.getEntities(C1.class);
            assertThat(entities).isEmpty();

            var entity0 = world.createEntity();
            var entity1 = world.createEntity(new C1());
            var entity12 = world.createEntity(new C1(), new C2());
            var entity2 = world.createEntity(new C2());

            assertThat(entities.isEmpty()).isFalse();
            assertThat(entities.size()).isEqualTo(2);
            assertThat(entities.contains(entity0)).isFalse();
            assertThat(entities.contains(entity1)).isTrue();
            assertThat(entities.contains(entity12)).isTrue();
            assertThat(entities.contains(entity2)).isFalse();

            assertThat(entities).hasSize(2);
            assertThat(entities).extracting(Entity::id).containsExactlyInAnyOrder(entity1, entity12);

            for (var entity : entities) {
                assertThat(entity.get(C1.class)).isNotNull();
            }
        }

        @Nested
        class ProcessableBagTest {

            private final IntBag processed = new IntBag(4);

            @AfterEach
            void clearProcessed() {
                this.processed.clear();
            }

            @Test
            void testProcess_ClassType() {
                var entities = world.getEntities(C1.class);
                assertThat(entities).isEmpty();

                var processable = entities.forType(component(C1.class));

                world.createEntity();
                world.createEntity(new C2());

                var entity1 = world.createEntity(new C1());
                var entity2 = world.createEntity(new C1(), new C2());

                processable.process(this::processComponent);

                assertThat(this.processed.getSize()).isEqualTo(2);
                assertThat(this.processed.getData()).contains(entity1, entity2);
            }

            private void processComponent(int entityId, C1 c1) {
                this.processed.add(entityId);

                assertThat(c1).isNotNull();
            }

            @Test
            void testProcess_ComponentSet() {
                var entities = world.getEntities(C1.class);
                assertThat(entities).isEmpty();

                var processable = entities.forType(ProcessableSet.TYPE);

                world.createEntity();
                world.createEntity(new C2());

                var entity1 = world.createEntity(new C1());
                var entity2 = world.createEntity(new C1(), new C2());

                processable.process(this::processComponentSet);

                assertThat(this.processed.getSize()).isEqualTo(2);
                assertThat(this.processed.getData()).contains(entity1, entity2);
            }

            @SuppressWarnings("unused")
            @ComponentSetConfig("ProcessableSet")
            private void processComponentSet(int entityId, C1 c1, C2 c2) {
                this.processed.add(entityId);

                assertThat(c1).isNotNull();
            }

        }

    }

    record C1() {
    }

    record C2() {
    }

}
