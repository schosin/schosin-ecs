package de.schosin.ecs.storage.archetype;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.platform.suite.api.SuiteDisplayName;

import de.schosin.ecs.api.World;
import de.schosin.ecs.storage.testsuite.StorageEngineTestSuite;

@SuiteDisplayName("ArchetypeStorage")
class ArchetypeStorageEngineTest extends StorageEngineTestSuite {

    @Nested
    class ConfigTest {

        @AfterEach
        void clearSystemProperty() {
            System.clearProperty(ArchetypeStorageConfig.PROPERTY_CLASS_ID_COUNT);
            System.clearProperty(ArchetypeStorageConfig.PROPERTY_RELATION_COUNT);
            System.clearProperty(ArchetypeStorageConfig.PROPERTY_CREATION_BATCH_SIZE);
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
            assertThat(config.creationBatchSize()).isEqualTo(ArchetypeStorageConfig.DEFAULT_CREATION_BATCH_SIZE);
        }

        @Test
        void testRelationCountSystemProperty() {
            System.setProperty(ArchetypeStorageConfig.PROPERTY_RELATION_COUNT, "9001");

            assertThatCode(() -> World.builder().storageEngine(ArchetypeStorageEngine.class, null).build()).doesNotThrowAnyException();

            var config = ArchetypeStorageEngine.retrieveStorageConfig(null);
            assertThat(config.classIdCount()).isEqualTo(ArchetypeStorageConfig.DEFAULT_CLASS_ID_COUNT);
            assertThat(config.relationCount()).isEqualTo(9001);
            assertThat(config.creationBatchSize()).isEqualTo(ArchetypeStorageConfig.DEFAULT_CREATION_BATCH_SIZE);
        }

        @Test
        void testCreationBatchSize() {
            System.setProperty(ArchetypeStorageConfig.PROPERTY_CREATION_BATCH_SIZE, "1337");

            assertThatCode(() -> World.builder().storageEngine(ArchetypeStorageEngine.class, null).build()).doesNotThrowAnyException();

            var config = ArchetypeStorageEngine.retrieveStorageConfig(null);
            assertThat(config.classIdCount()).isEqualTo(ArchetypeStorageConfig.DEFAULT_CLASS_ID_COUNT);
            assertThat(config.relationCount()).isEqualTo(ArchetypeStorageConfig.DEFAULT_RELATION_COUNT);
            assertThat(config.creationBatchSize()).isEqualTo(1337);
        }

        @Test
        void testAllSystemProperties() {
            System.setProperty(ArchetypeStorageConfig.PROPERTY_CLASS_ID_COUNT, "42");
            System.setProperty(ArchetypeStorageConfig.PROPERTY_RELATION_COUNT, "9001");
            System.setProperty(ArchetypeStorageConfig.PROPERTY_CREATION_BATCH_SIZE, "1337");

            assertThatCode(() -> World.builder().storageEngine(ArchetypeStorageEngine.class, null).build()).doesNotThrowAnyException();

            var config = ArchetypeStorageEngine.retrieveStorageConfig(null);
            assertThat(config.classIdCount()).isEqualTo(42);
            assertThat(config.relationCount()).isEqualTo(9001);
            assertThat(config.creationBatchSize()).isEqualTo(1337);
        }

    }

}
