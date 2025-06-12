package de.schosin.ecs.storage.archetype;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import de.schosin.ecs.storage.api.StorageEngineException;

class ArchetypeStorageConfigTest {

    @ParameterizedTest
    @ValueSource(ints = { -1, 0 })
    void testInvalidClassIdCount(int classIdCount) {
        assertThatThrownBy(() -> new ArchetypeStorageConfig(classIdCount, ArchetypeStorageConfig.DEFAULT_RELATION_COUNT, ArchetypeStorageConfig.DEFAULT_VARIANT))
                .isInstanceOf(StorageEngineException.class)
                .hasMessageContainingAll("classIdCount must be positive");
    }

    @ParameterizedTest
    @ValueSource(ints = { -1, 0 })
    void testInvalidRelationCount(int relationCount) {
        assertThatThrownBy(() -> new ArchetypeStorageConfig(ArchetypeStorageConfig.DEFAULT_CLASS_ID_COUNT, relationCount, ArchetypeStorageConfig.DEFAULT_VARIANT))
                .isInstanceOf(StorageEngineException.class)
                .hasMessageContainingAll("relationCount must be positive");
    }

    @Test
    void testNullVariant() {
        assertThatThrownBy(() -> new ArchetypeStorageConfig(ArchetypeStorageConfig.DEFAULT_CLASS_ID_COUNT, ArchetypeStorageConfig.DEFAULT_RELATION_COUNT, null))
                .isInstanceOf(StorageEngineException.class)
                .hasMessageContainingAll("variant must not be null");
    }

}
