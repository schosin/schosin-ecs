package de.schosin.ecs.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assumptions.assumeThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class BagManagerTest extends AbstractWorldTest {

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

    @ParameterizedTest
    @CsvSource({
            "2000, 2048",
            "2047, 2048",
            "2048, 4096"
    })
    void testEnsureEntityCapacity(int size, int expectedCapacity) {
        // Setup
        var entityCapacity = this.bagManager.getEntitySize();
        var componentCapacity = this.bagManager.getComponentSize();

        assumeThat(size).isGreaterThan(entityCapacity);

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
        this.bagManager.ensureEntitySize(size);

        // Verify only entity capacity increased
        assertThat(entityBag.getCapacity()).isGreaterThanOrEqualTo(expectedCapacity);
        assertThat(entityIntBag.getCapacity()).isGreaterThanOrEqualTo(expectedCapacity);

        assertThat(componentBag.getCapacity()).isEqualTo(componentCapacity);
        assertThat(componentIntBag.getCapacity()).isEqualTo(componentCapacity);
    }

    @Test
    void testEnsureEntityCapacityIncreasingByOne() {
        var entityCapacity = this.bagManager.getEntitySize();

        var entityBag = this.bagManager.createEntityBag(Object.class);
        var entityIntBag = this.bagManager.createEntityIntBag();

        for (int i = 0; i < entityCapacity + 10; i++) {
            var entityId = world.createEntity();

            assertThatCode(() -> entityBag.get(entityId)).doesNotThrowAnyException();
            assertThatCode(() -> entityIntBag.get(entityId)).doesNotThrowAnyException();
        }
    }

    @ParameterizedTest
    @CsvSource({
            "100, 128",
            "2000, 2048",
            "2047, 2048",
            "2048, 4096"
    })
    void testEnsureComponentCapacity(int size, int expectedCapacity) {
        // Setup
        var entityCapacity = this.bagManager.getEntitySize();
        var componentCapacity = this.bagManager.getComponentSize();

        assumeThat(size).isGreaterThan(componentCapacity);

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
        this.bagManager.ensureComponentSize(size);

        // Verify only entity capacity increased
        assertThat(entityBag.getCapacity()).isEqualTo(entityCapacity);
        assertThat(entityIntBag.getCapacity()).isEqualTo(entityCapacity);

        assertThat(componentBag.getCapacity()).isGreaterThanOrEqualTo(expectedCapacity);
        assertThat(componentIntBag.getCapacity()).isGreaterThanOrEqualTo(expectedCapacity);
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
