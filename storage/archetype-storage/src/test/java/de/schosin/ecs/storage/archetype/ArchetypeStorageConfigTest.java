package de.schosin.ecs.storage.archetype;

import static de.schosin.ecs.storage.archetype.ArchetypeStorageConfig.DEFAULT_CLASS_ID_COUNT;
import static de.schosin.ecs.storage.archetype.ArchetypeStorageConfig.DEFAULT_CREATION_FLUSH_ATTEMPTS;
import static de.schosin.ecs.storage.archetype.ArchetypeStorageConfig.DEFAULT_PROCESS_ATTEMPTS;
import static de.schosin.ecs.storage.archetype.ArchetypeStorageConfig.DEFAULT_RELATION_COUNT;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import de.schosin.ecs.storage.api.StorageEngineException;

class ArchetypeStorageConfigTest {

    @ParameterizedTest
    @ValueSource(ints = { -1, 0 })
    void testInvalidClassIdCount(int classIdCount) {
        assertThatThrownBy(() -> new ArchetypeStorageConfig(classIdCount, DEFAULT_RELATION_COUNT, DEFAULT_CREATION_FLUSH_ATTEMPTS, DEFAULT_PROCESS_ATTEMPTS))
                .isInstanceOf(StorageEngineException.class)
                .hasMessageContainingAll("classIdCount must be positive");
    }

    @ParameterizedTest
    @ValueSource(ints = { -1, 0 })
    void testInvalidRelationCount(int relationCount) {
        assertThatThrownBy(() -> new ArchetypeStorageConfig(DEFAULT_CLASS_ID_COUNT, relationCount, DEFAULT_CREATION_FLUSH_ATTEMPTS, DEFAULT_PROCESS_ATTEMPTS))
                .isInstanceOf(StorageEngineException.class)
                .hasMessageContainingAll("relationCount must be positive");
    }

    @ParameterizedTest
    @ValueSource(ints = { -1 })
    void testInvalidCreationFlushAttempts(int creationFlushAttempts) {
        assertThatThrownBy(() -> new ArchetypeStorageConfig(DEFAULT_CLASS_ID_COUNT, DEFAULT_RELATION_COUNT, creationFlushAttempts, DEFAULT_PROCESS_ATTEMPTS))
                .isInstanceOf(StorageEngineException.class)
                .hasMessageContainingAll("creationFlushAttempts must be positive or zero");
    }

    @ParameterizedTest
    @ValueSource(ints = { -1, 0 })
    void testInvalidProcessAttempts(int processAttempts) {
        assertThatThrownBy(() -> new ArchetypeStorageConfig(DEFAULT_CLASS_ID_COUNT, DEFAULT_RELATION_COUNT, DEFAULT_CREATION_FLUSH_ATTEMPTS, processAttempts))
                .isInstanceOf(StorageEngineException.class)
                .hasMessageContainingAll("processAttempts must be positive");
    }

}
