package de.schosin.ecs.storage.testsuite;

import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.api.Test;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;

import de.schosin.ecs.storage.api.StorageEngine;

@Suite
@SuiteDisplayName("Storage Engine Example Suite")
@SelectPackages("de.schosin.ecs.storage.testsuite")
public abstract class StorageEngineTestSuite {

    protected abstract Class<? extends StorageEngine> getImplementationClass();

    @Test
    void testLoad() {
        assertThatCode(StorageEngine::load).doesNotThrowAnyException();
    }

    @Test
    void testLoadStorageEngine() {
        var implementationClass = getImplementationClass();

        assertThatCode(() -> StorageEngine.load(implementationClass)).doesNotThrowAnyException();
    }

}
