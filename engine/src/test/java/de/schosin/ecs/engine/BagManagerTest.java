package de.schosin.ecs.engine;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class BagManagerTest {

    BagManager bagManager;

    @BeforeEach
    void setup() {
        this.bagManager = new BagManager();
    }

    @Test
    void testInitialBagCapacity() {
        // Setup
        var entityCapacity = this.bagManager.getEntitySize();
        var componentCapacity = this.bagManager.getComponentSize();

        var entityBag = this.bagManager.createEntityBag(Object.class);
        var entityIntBag = this.bagManager.createEntityIntBag();
        var componentBag = this.bagManager.createComponentBag(Object.class);
        var componentIntBag = this.bagManager.createComponentIntBag();

        // Verify initial capacity
        assertThat(entityBag.getCapacity()).isEqualTo(entityCapacity);
        assertThat(entityIntBag.getCapacity()).isEqualTo(entityCapacity);

        assertThat(componentBag.getCapacity()).isEqualTo(componentCapacity);
        assertThat(componentIntBag.getCapacity()).isEqualTo(componentCapacity);
    }

    @Test
    void testEnsureEntityCapacity() {
        // Setup
        var entityCapacity = this.bagManager.getEntitySize();
        var componentCapacity = this.bagManager.getComponentSize();

        var entityBag = this.bagManager.createEntityBag(Object.class);
        var entityIntBag = this.bagManager.createEntityIntBag();
        var componentBag = this.bagManager.createComponentBag(Object.class);
        var componentIntBag = this.bagManager.createComponentIntBag();

        // Verify initial capacity
        assertThat(entityBag.getCapacity()).isEqualTo(entityCapacity);
        assertThat(entityIntBag.getCapacity()).isEqualTo(entityCapacity);

        assertThat(componentBag.getCapacity()).isEqualTo(componentCapacity);
        assertThat(componentIntBag.getCapacity()).isEqualTo(componentCapacity);

        // Increase entity capacity
        this.bagManager.ensureEntitySize(entityCapacity * 2);

        // Verify only entity capacity increased
        assertThat(entityBag.getCapacity()).isGreaterThanOrEqualTo(entityCapacity * 2);
        assertThat(entityIntBag.getCapacity()).isGreaterThanOrEqualTo(entityCapacity * 2);

        assertThat(componentBag.getCapacity()).isEqualTo(componentCapacity);
        assertThat(componentIntBag.getCapacity()).isEqualTo(componentCapacity);
    }

    @Test
    void testEnsureComponentCapacity() {
        // Setup
        var entityCapacity = this.bagManager.getEntitySize();
        var componentCapacity = this.bagManager.getComponentSize();

        var entityBag = this.bagManager.createEntityBag(Object.class);
        var entityIntBag = this.bagManager.createEntityIntBag();
        var componentBag = this.bagManager.createComponentBag(Object.class);
        var componentIntBag = this.bagManager.createComponentIntBag();

        // Verify initial capacity
        assertThat(entityBag.getCapacity()).isEqualTo(entityCapacity);
        assertThat(entityIntBag.getCapacity()).isEqualTo(entityCapacity);

        assertThat(componentBag.getCapacity()).isEqualTo(componentCapacity);
        assertThat(componentIntBag.getCapacity()).isEqualTo(componentCapacity);

        // Increate component capacity
        this.bagManager.ensureComponentSize(componentCapacity * 3);

        // Verify only entity capacity increased
        assertThat(entityBag.getCapacity()).isEqualTo(entityCapacity);
        assertThat(entityIntBag.getCapacity()).isEqualTo(entityCapacity);

        assertThat(componentBag.getCapacity()).isEqualTo(componentCapacity * 3 + 1);
        assertThat(componentIntBag.getCapacity()).isEqualTo(componentCapacity * 3 + 1);
    }

    @Test
    void testDownsizingDoesNothing() {
        // Setup
        var entityCapacity = this.bagManager.getEntitySize();
        var componentCapacity = this.bagManager.getComponentSize();

        var entityBag = this.bagManager.createEntityBag(Object.class);
        var entityIntBag = this.bagManager.createEntityIntBag();
        var componentBag = this.bagManager.createComponentBag(Object.class);
        var componentIntBag = this.bagManager.createComponentIntBag();

        // Verify initial capacity
        assertThat(entityBag.getCapacity()).isEqualTo(entityCapacity);
        assertThat(entityIntBag.getCapacity()).isEqualTo(entityCapacity);

        assertThat(componentBag.getCapacity()).isEqualTo(componentCapacity);
        assertThat(componentIntBag.getCapacity()).isEqualTo(componentCapacity);

        // Decrease entity and component capacity
        this.bagManager.ensureEntitySize(entityCapacity - 2);
        this.bagManager.ensureComponentSize(componentCapacity - 3);

        // Verify no changes
        assertThat(entityBag.getCapacity()).isEqualTo(entityCapacity);
        assertThat(entityIntBag.getCapacity()).isEqualTo(entityCapacity);

        assertThat(componentBag.getCapacity()).isEqualTo(componentCapacity);
        assertThat(componentIntBag.getCapacity()).isEqualTo(componentCapacity);

    }

    @Nested
    class FreeTest {

        @Test
        void testEntityBag() {
            // Setup
            var capacity = bagManager.getEntitySize();

            var bag1 = bagManager.createEntityBag(Object.class);
            var bag2 = bagManager.createEntityBag(Object.class);
            bagManager.free(bag1);

            // Call
            bagManager.ensureEntitySize(capacity * 2);

            // Verify
            assertThat(bag1.getCapacity()).isEqualTo(capacity);
            assertThat(bag2.getCapacity()).isGreaterThanOrEqualTo(capacity * 2);
        }

        @Test
        void testEntityIntBag() {
            // Setup
            var capacity = bagManager.getEntitySize();

            var bag1 = bagManager.createEntityIntBag();
            var bag2 = bagManager.createEntityIntBag();
            bagManager.free(bag1);

            // Call
            bagManager.ensureEntitySize(capacity * 2);

            // Verify
            assertThat(bag1.getCapacity()).isEqualTo(capacity);
            assertThat(bag2.getCapacity()).isGreaterThanOrEqualTo(capacity * 2);
        }

        @Test
        void testComponentBag() {
            // Setup
            var capacity = bagManager.getComponentSize();

            var bag1 = bagManager.createComponentBag(Object.class);
            var bag2 = bagManager.createComponentBag(Object.class);
            bagManager.free(bag1);

            // Call
            bagManager.ensureComponentSize(capacity * 2);

            // Verify
            assertThat(bag1.getCapacity()).isEqualTo(capacity);
            assertThat(bag2.getCapacity()).isGreaterThanOrEqualTo(capacity * 2);
        }

        @Test
        void testComponentIntBag() {
            // Setup
            var capacity = bagManager.getComponentSize();

            var bag1 = bagManager.createComponentIntBag();
            var bag2 = bagManager.createComponentIntBag();
            bagManager.free(bag1);

            // Call
            bagManager.ensureComponentSize(capacity * 2);

            // Verify
            assertThat(bag1.getCapacity()).isEqualTo(capacity);
            assertThat(bag2.getCapacity()).isGreaterThanOrEqualTo(capacity * 2);
        }

    }

}
