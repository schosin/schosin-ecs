package de.schosin.ecs.storage.archetype;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import de.schosin.ecs.api.World;
import de.schosin.ecs.storage.archetype.ArchetypeStorageConfig.Variant;

class ArchetypeStorageEngineTest {

    @Nested
    class ConfigTest {

        @AfterEach
        void clearSystemProperty() {
            System.clearProperty(ArchetypeStorageConfig.PROPERTY_CLASS_ID_COUNT);
            System.clearProperty(ArchetypeStorageConfig.PROPERTY_RELATION_COUNT);
            System.clearProperty(ArchetypeStorageConfig.PROPERTY_VARIANT);
        }

        @ParameterizedTest
        @EnumSource(Variant.class)
        void testConfigObject(Variant variant) {
            var config = new ArchetypeStorageConfig(1, 1, variant);

            assertThatCode(() -> World.builder().storageEngine(ArchetypeStorageEngine.class, config).build()).doesNotThrowAnyException();
        }

        @Test
        void testNoConfigObject() {
            assertThatCode(() -> World.builder().storageEngine(ArchetypeStorageEngine.class, null).build()).doesNotThrowAnyException();
        }

        @Test
        void testClassIdCountSystemProperty() {
            System.setProperty(ArchetypeStorageConfig.PROPERTY_CLASS_ID_COUNT, "42");

            assertThatCode(() -> World.builder().storageEngine(ArchetypeStorageEngine.class, null).build()).doesNotThrowAnyException();

            var config = ArchetypeStorageEngine.retrieveStorageConfig(null);
            assertThat(config.classIdCount()).isEqualTo(42);
            assertThat(config.relationCount()).isEqualTo(ArchetypeStorageConfig.DEFAULT_RELATION_COUNT);
            assertThat(config.variant()).isEqualTo(ArchetypeStorageConfig.DEFAULT_VARIANT);
        }

        @Test
        void testRelationCountSystemProperty() {
            System.setProperty(ArchetypeStorageConfig.PROPERTY_RELATION_COUNT, "9001");

            assertThatCode(() -> World.builder().storageEngine(ArchetypeStorageEngine.class, null).build()).doesNotThrowAnyException();

            var config = ArchetypeStorageEngine.retrieveStorageConfig(null);
            assertThat(config.classIdCount()).isEqualTo(ArchetypeStorageConfig.DEFAULT_CLASS_ID_COUNT);
            assertThat(config.relationCount()).isEqualTo(9001);
            assertThat(config.variant()).isEqualTo(ArchetypeStorageConfig.DEFAULT_VARIANT);
        }

        @ParameterizedTest
        @EnumSource(Variant.class)
        void testVariantSystemProperty(Variant variant) {
            System.setProperty(ArchetypeStorageConfig.PROPERTY_VARIANT, variant.name());

            assertThatCode(() -> World.builder().storageEngine(ArchetypeStorageEngine.class, null).build()).doesNotThrowAnyException();

            var config = ArchetypeStorageEngine.retrieveStorageConfig(null);
            assertThat(config.classIdCount()).isEqualTo(ArchetypeStorageConfig.DEFAULT_CLASS_ID_COUNT);
            assertThat(config.relationCount()).isEqualTo(ArchetypeStorageConfig.DEFAULT_RELATION_COUNT);
            assertThat(config.variant()).isEqualTo(variant);
        }

        @ParameterizedTest
        @EnumSource(Variant.class)
        void testAllSystemProperties(Variant variant) {
            System.setProperty(ArchetypeStorageConfig.PROPERTY_CLASS_ID_COUNT, "42");
            System.setProperty(ArchetypeStorageConfig.PROPERTY_RELATION_COUNT, "9001");
            System.setProperty(ArchetypeStorageConfig.PROPERTY_VARIANT, variant.name());

            assertThatCode(() -> World.builder().storageEngine(ArchetypeStorageEngine.class, null).build()).doesNotThrowAnyException();

            var config = ArchetypeStorageEngine.retrieveStorageConfig(null);
            assertThat(config.classIdCount()).isEqualTo(42);
            assertThat(config.relationCount()).isEqualTo(9001);
            assertThat(config.variant()).isEqualTo(variant);
        }

    }

}
