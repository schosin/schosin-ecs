package de.schosin.ecs.storage.defaultimpl;

import de.schosin.ecs.storage.api.StorageEngine;
import de.schosin.ecs.storage.testsuite.StorageEngineTestSuite;

class DefaultStorageEngineTest extends StorageEngineTestSuite {

    @Override
    protected Class<? extends StorageEngine> getImplementationClass() {
        return DefaultStorageEngine.class;
    }

}
